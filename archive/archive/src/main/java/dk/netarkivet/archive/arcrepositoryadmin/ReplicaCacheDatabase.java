/* File:     $Id$
 * Revision: $Revision$
 * Author:   $Author$
 * Date:     $Date$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2010 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301 
 *  USA
 */
package dk.netarkivet.archive.arcrepositoryadmin;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.archive.arcrepository.bitpreservation.ChecksumJob;
import dk.netarkivet.common.distribute.Channels;
import dk.netarkivet.common.distribute.arcrepository.Replica;
import dk.netarkivet.common.distribute.arcrepository.ReplicaStoreState;
import dk.netarkivet.common.distribute.arcrepository.ReplicaType;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.IllegalState;
import dk.netarkivet.common.utils.DBUtils;
import dk.netarkivet.common.utils.KeyValuePair;
import dk.netarkivet.common.utils.NotificationsFactory;

/**
 * Method for storing the bitpreservation cache in a database.
 * 
 * This method uses the 'admin.data' file for retrieving the upload status.
 * 
 * TODO this file is extremely large (more than 2000 lines) and should be 
 * shortened.
 */
public final class ReplicaCacheDatabase implements BitPreservationDAO {
    /** The log.*/
    protected static Log log 
            = LogFactory.getLog(ReplicaCacheDatabase.class.getName());

    /** The current instance.*/
    private static ReplicaCacheDatabase instance;
    
    /** The connection to the database.*/
    private Connection dbConnection;
    
    /** max number of database retries. */
    private final int maxdatabaseRetries = 5;
    /** max time to wait between retries. */
    private final int delaybetweenretries = 5*60*1000;
    
    /**
     * Constructor.
     */
    private ReplicaCacheDatabase() {
        // Initialise the database based on settings.
        dbConnection = DBConnect.getDBConnection(DBConnect.getArchiveUrl());

        // initialise the database.
        initialiseDB();
    }
    
    /**
     * Method for retrieving the current instance of this class.
     * 
     * @return The current instance.
     */
    public static synchronized ReplicaCacheDatabase getInstance() {
        if (instance == null) {
            instance = new ReplicaCacheDatabase();
        }
        return instance;
    }

    /**
     * Method for initialising the database.
     * This basically makes sure that all the replicas are within the database,
     * and that no unknown replicas have been defined.
     */
    protected void initialiseDB() {
        // retrieve the list of replicas.
        Collection<Replica> replicas = Replica.getKnown();
        // Retrieve the replica IDs from the database.
        List<String> repIds = retrieveIdsFromReplicaTable();

        for (Replica rep : replicas) {
            // try removing the id from the temporary list of IDs within the DB.
            // If the remove is not successful, then the replica is already
            // in the database.
            if (!repIds.remove(rep.getId())) {
                // if the replica id cannot be removed from the list, then it
                // does not exist in the database and must be added.
                log.info("Inserting replica '" + rep.toString()
                        + "' in database.");
                insertReplicaIntoDB(rep);
            } else {
                // Otherwise it already exists in the DB.
                log.info("Replica '" + rep.toString()
                        + "' already inserted in database.");
            }
        }

        // If unknown replica ids are found, then throw exception.
        if (repIds.size() > 0) {
            throw new IllegalState("The database contain identifiers for the "
                    + "following replicas, which has not defined in the "
                    + "settings: " + repIds);
        }
    }
    
    /**
     * Method for checking whether a replicafileinfo is in the database.
     * 
     * @param fileid The id of the file.
     * @param replicaID The id of the replica.
     * @return Whether the replicafileinfo was there or not.
     * @throws IllegalState If more than one copy of the replicafileinfo is 
     * placed in the database.
     */
    private boolean existsReplicaFileInfoInDB(long fileid, String replicaID)
            throws IllegalState {
        // retrieve the amount of times this replicafileinfo
        // is within the database.
        String sql = "SELECT COUNT(*) FROM replicafileinfo WHERE file_id = ? "
                + "AND replica_id = ?";
        int count = DBUtils.selectIntValue(getDbConnection(), sql, fileid, 
                replicaID);

        // Handle the different cases for count.
        switch (count) {
        case 0:
            return false;
        case 1:
            return true;
        default:
            throw new IllegalState("Cannot handle " + count 
                    + " replicafileinfo entries "
                    + "with the id '" + fileid + "'.");
        }
    }

    /**
     * Method for inserting a Replica into the replica table.
     * The replica_guid is automatically given by the database, and the 
     * values in the fields replica_id, replica_name and replica_type is 
     * created from the replica argument.
     * 
     * @param rep The Replica to insert into the replica table.
     * @throws IllegalState If a SQLException is caught.
     */
    private void insertReplicaIntoDB(Replica rep) throws IllegalState {
        try {
            Connection connection = getDbConnection();
            PreparedStatement statement = null;
            
            // Make the SQL statement for putting the replica into the database
            // and insert the variables for the entry to the replica table.
            statement = connection.prepareStatement(
                    "INSERT INTO replica "
                    + "(replica_id, replica_name, replica_type) "
                    + "VALUES ( ?, ?, ?)");
            statement.setString(1, rep.getId());
            statement.setString(2, rep.getName());
            statement.setInt(3, rep.getType().ordinal());
            
            // execute the SQL statement
            statement.executeUpdate();
            connection.commit();
        } catch (SQLException e) {
            throw new IOFailure("Cannot add replica '" + rep 
                    + "'to the database.", e);
        }
    }
    
    /**
     * Method to create a new entry in the file table in the database.
     * The file_id is automatically created by the database, and the argument
     * is used for the filename for this new entry to the table. 
     * This will also create a replicafileinfo entry for each replica.
     * 
     * @param filename The filename for the new entry in the file table.
     * @throws IllegalState If the file cannot be inserted into the database.
     * @return created file_id for the new entry.
     */
    private long insertFileIntoDB(String filename) throws IllegalState {
        log.debug("Insert file '" + filename + "' into database");
        try {
            Connection connection = getDbConnection();
            PreparedStatement statement = null;
            
            // Make the SQL statement for putting the replica into the database
            // and insert the variables for the entry to the replica table.
            statement = connection.prepareStatement(
                    "INSERT INTO file "
                    + "(filename) "
                    + "VALUES ( ? )");
            statement.setString(1, filename);
            
            // execute the SQL statement
            statement.executeUpdate();
            connection.commit();
            
            // Create the replicafileinfo for each replica.
            long fileId = retrieveIdForFile(filename);
            createReplicaFileInfoEntriesInDB(fileId);
            log.debug("Insert file '" + filename + "' into database completed");
            return fileId;
        } catch (SQLException e) {
            throw new IllegalState("Cannot add file '" + filename
                    + "' to the database.", e);
        }
    }
    
    /**
     * When a new file is inserted into the database, each replica gets
     * a new entry in the replicafileinfo table for this file.
     * The fields for this new entry are set to the following:
     * - file_id = argument.
     * - replica_id = The id of the current replica.
     * - filelist_status = NO_FILELIST_STATUS.
     * - checksum_status = UNKNOWN.
     * - upload_status = NO_UPLOAD_STATUS.
     * 
     * The replicafileinfo_guid is automatically created by the database,
     * and the dates are set to null.
     * 
     * @param fileId The id for the file.
     * @throws IllegalState If the file could not be entered into the database.
     */
    private void createReplicaFileInfoEntriesInDB(long fileId) 
            throws IllegalState {
        try {
            // init variables
            List<String> repIds = retrieveIdsFromReplicaTable();

            PreparedStatement statement = null;
            Connection connection = getDbConnection();

            // Make a entry for each replica.
            for (String repId : repIds) {
                // create if it does not exists already.
                if (!existsReplicaFileInfoInDB(fileId, repId)) {
                    // Insert with the known values (no dates).
                    statement = DBUtils.prepareStatement(
                            connection,
                            "INSERT INTO replicafileinfo "
                            + "(file_id, replica_id, filelist_status, "
                            + "checksum_status, upload_status ) VALUES "
                            + "( ?, ?, ?, ?, ? )", fileId, repId, 
                            FileListStatus.NO_FILELIST_STATUS.ordinal(), 
                            ChecksumStatus.UNKNOWN.ordinal(),
                            ReplicaStoreState.UNKNOWN_UPLOAD_STATE.ordinal());

                    // execute the SQL statement
                    statement.executeUpdate();
                    connection.commit();
                }
            }
        } catch (SQLException e) {
            throw new IllegalState("Cannot add replicafileinfo to the "
                    + "database.", e);
        }
    }
    
    /**
     * Method for retrieving the replica IDs within the database.
     * 
     * @return The list of replicaIds from the replica table in the database.
     */
    private List<String> retrieveIdsFromReplicaTable() {
        // Make SQL statement for retrieving the replica_ids in the
        // replica table.
        String sql = "SELECT replica_id FROM replica";

        // execute the SQL statement and return the results.
        return DBUtils.selectStringList(getDbConnection(), sql);
    }
    
    /**
     * Method for retrieving all the file IDs within the database.
     * 
     * @return The list of fileIds from the file table in the database.
     */
    private List<String> retrieveIdsFromFileTable() {
        // Make SQL statement for retrieving the file_ids in the
        // file table.
        String sql = "SELECT file_id FROM file";

        // execute the SQL statement and return the results.
        return DBUtils.selectStringList(getDbConnection(), sql);
    }
    
    /**
     * Method for retrieving all the ReplicaFileInfo GUIDs within the database.
     * 
     * @return A list of the replicafileinfo_guid for all entries in the 
     * replicafileinfo table.
     */
    private List<String> retrieveIdsFromReplicaFileInfoTable() {
        // Make SQL statement for retrieving the replicafileinfo_guids in the
        // replicafileinfo table.
        String sql = "SELECT replicafileinfo_guid FROM replicafileinfo";

        // execute the SQL statement and return the results.
        return DBUtils.selectStringList(getDbConnection(), sql);
    }

    /**
     * Retrieves the file_id for the corresponding filename.
     * An error is thrown if no such file_id is found in the file table, and if 
     * more than one instance with the given name is found, then a warning
     * is issued.
     * If more than one is found, then it is logged and only the first is 
     * returned.
     * 
     * @param filename The entry in the filename list where the corresponding
     * file_id should be found.
     * @return The file_id for the file, or -1 if the file was not found.
     */
    private long retrieveIdForFile(String filename) {
        // retrieve the file_id of the entry in the file table with the 
        // filename filename.
        String sql = "SELECT file_id FROM file WHERE filename = ?";
        List<Long> files = DBUtils.selectLongList(
                getDbConnection(), sql, filename);

        switch (files.size()) {
        // if no such file within the database, then return negative value.
        case 0:
            return -1;
        case 1:
            return files.get(0);
            // if more than one file, then log it and return the first found.
        default:
            log.warn("Only one entry in the file table for the name '"
                    + filename + "' was expected, but " + files.size()
                    + " was found. The first element is returned.");
            return files.get(0);
        }
    }
    
    /**
     * Method for retrieving the replicafileinfo_guid for a specific instance
     * defined from the fileId and the replicaId.
     * If more than one is found, then it is logged and only the first is 
     * returned.
     * 
     * @param fileId The identifier for the file.
     * @param replicaId The identifier for the replica.
     * @return The identifier for the replicafileinfo, or -1 if not found.
     */
    private long retrieveReplicaFileInfoGuid(long fileId, String replicaId) {
        // sql for retrieving the replicafileinfo_guid.
        String sql = "SELECT replicafileinfo_guid FROM replicafileinfo WHERE "
                + "file_id = ? AND replica_id = ?";
        List<Long> result = DBUtils.selectLongList(getDbConnection(), sql, 
                fileId, replicaId);

        // Handle the different cases for count.
        switch (result.size()) {
        case 0:
            return -1;
        case 1:
            return result.get(0);
        default:
            log.warn("More than one replicafileinfo with the file id '"
                    + fileId + "' from replica '" + replicaId + "': " + result
                    + ". The first result returned.");
            return result.get(0);
        }
    }
    
    /**
     * Method for retrieving the list of all the replicafileinfo_guids for a 
     * specific replica.
     * 
     * @param replicaId The id for the replica to contain the files.
     * @return The list of all the replicafileinfo_guid.
     */
    private List<Long> retrieveReplicaFileInfoGuidsForReplica(
            String replicaId) {
        // sql for retrieving the replicafileinfo_guids for the replica.
        String sql = "SELECT replicafileinfo_guid FROM replicafileinfo WHERE "
                + "replica_id = ?";
        return DBUtils.selectLongList(getDbConnection(), sql, replicaId);
    }
    
    /**
     * Method for retrieving the list of all the replicafileinfo_guids for a
     * specific file.
     * 
     * @param fileId The id for the file.
     * @return The list of all the replicafileinfo_guids.
     */
    private List<Long> retrieveReplicaFileInfoGuidsForFile(long fileId) {
        // sql for retrieving the replicafileinfo_guids for the fileId.
        String sql = "SELECT replicafileinfo_guid FROM replicafileinfo WHERE "
                + "file_id = ?";
        return DBUtils.selectLongList(getDbConnection(), sql, fileId);
    }
    
    /**
     * Method for retrieving a list of all the file_ids in the file table.
     * 
     * @return The list of all the file_ids in the file table.
     */
    private List<Long> retrieveAllFileIds() {
        // sql for retrieving all the file_ids in the file table.
        String sql = "SELECT file_id FROM file";
        return DBUtils.selectLongList(getDbConnection(), sql);
    }

    /**
     * Method for retrieving the replica type for a specific replica.
     * 
     * @param replicaId The id of the replica.
     * @return The type of the replica.
     */
    private int retrieveReplicaType(String replicaId) {
        // The SQL statement for retrieving the replica_type of a replica with
        // the given replica id.
        String sql = "SELECT replica_type FROM replica WHERE replica_id = ?";
        return DBUtils.selectIntValue(getDbConnection(), sql, replicaId);
    }
    
    /**
     * Method for retrieving the list of replica, where the file with fileId
     * has the checksum_status 'OK'.
     *  
     * @param fileId The id for the file. 
     * @return The list of replicas where the status for the checksum of the 
     * file is OK.
     */
    private List<String> retrieveReplicaIdsWithOKChecksumStatus(long fileId) {
        // The SQL statement to retrieve the replica_id for the entries in the
        // replicafileinfo table for the given fileId and checksum_status = OK
        String sql = "SELECT replica_id FROM replicafileinfo WHERE "
                + "file_id = ? AND checksum_status = ?";
        return DBUtils.selectStringList(getDbConnection(), sql, fileId,
                ChecksumStatus.OK.ordinal());
    }
    
    /**
     * Method for retrieving the filename from the entry in the file table 
     * which has the fileId as file_id.
     * 
     * @param fileId The file_id of the entry in the file table for which to
     * retrieve the filename.
     * @return The filename corresponding to the fileId in the file table.
     */
    private String retrieveFilenameForFileId(long fileId) {
        // The SQL statement to retrieve the filename for a given file_id
        String sql = "SELECT filename FROM file WHERE file_id = ?";
        return DBUtils.selectStringValue(getDbConnection(), sql, fileId);
    }
    
    /**
     * Method for retrieving the filelist_status for the entry in the
     * replicafileinfo table with the given replicafileinfo_guid.
     * 
     * @param replicafileinfoGuid The identification for the replicafileinfo
     * entry.
     * @return The filelist_status for the entry with the replciafileinfo_guid. 
     */
    private int retrieveFileListStatusFromReplicaFileInfo(
            long replicafileinfoGuid) {
        // The SQL statement to retrieve the filelist_status for the given
        // entry in the replica fileinfo table.
        String sql = "SELECT filelist_status FROM replicafileinfo WHERE "
                + "replicafileinfo_guid = ?";
        return DBUtils.selectIntValue(getDbConnection(), sql, 
                replicafileinfoGuid);
    }

    /**
     * This is used for updating a replicafileinfo instance based on the 
     * results of a checksumjob.
     * Updates the following fields for the entry in the replicafileinfo:
     * <br/>- checksum = checksum argument.
     * <br/>- upload_status = completed.
     * <br/>- filelist_status = ok.
     * <br/>- checksum_status = UNKNOWN.
     * <br/>- checksum_checkdatetime = now.
     * <br/>- filelist_checkdatetime = now.
     * 
     * @param replicafileinfoId The unique id for the replicafileinfo.
     * @param checksum The new checksum for the entry.
     */
    private void updateReplicaFileInfoChecksum(long replicafileinfoId,
            String checksum) {
        try {
            // The SQL statement
            String sql = "UPDATE replicafileinfo SET checksum = ?, "
                + "upload_status = ?, filelist_status = ?, checksum_status "
                + "= ?, checksum_checkdatetime = ?, filelist_checkdatetime = ?"
                + " WHERE replicafileinfo_guid = ?";

            // init.
            PreparedStatement statement = null;
            Connection connection = getDbConnection();
            Date now = new Date(Calendar.getInstance().getTimeInMillis());

            // complete the SQL statement.
            statement = DBUtils.prepareStatement(connection, sql, checksum,
                    ReplicaStoreState.UPLOAD_COMPLETED.ordinal(), 
                    FileListStatus.OK.ordinal(), ChecksumStatus.UNKNOWN
                            .ordinal(), now, now, replicafileinfoId);

            // execute the SQL statement
            statement.executeUpdate();
            connection.commit();
        } catch (Exception e) {
            String msg = "Problems updating the replicafileinfo.";
            log.warn(msg);
            throw new IOFailure(msg, e);
        }
    }
    
    /**
     * Method for updating the filelist of a replicafileinfo instance.
     * Updates the following fields for the entry in the replicafileinfo:
     * <br/> filelist_status = OK.
     * <br/> filelist_checkdatetime = current time. 
     * 
     * @param replicafileinfoId The id of the replicafileinfo.
     */
    private void updateReplicaFileInfoFilelist(long replicafileinfoId) {
        try {
            // The SQL statement
            String sql = "UPDATE replicafileinfo SET filelist_status = ?, "
                    + "filelist_checkdatetime = ? "
                    + "WHERE replicafileinfo_guid = ?";

            // init.
            PreparedStatement statement = null;
            Connection connection = getDbConnection();
            Date now = new Date(Calendar.getInstance().getTimeInMillis());

            // complete the SQL statement.
            statement = DBUtils.prepareStatement(connection, sql,
                    FileListStatus.OK.ordinal(), now, replicafileinfoId);

            // execute the SQL statement
            statement.executeUpdate();
            connection.commit();
        } catch (Exception e) {
            String msg = "Problems updating the replicafileinfo.";
            log.warn(msg);
            throw new IOFailure(msg, e);
        }
    }
    
    /**
     * Method for updating the filelist of a replicafileinfo instance.
     * Updates the following fields for the entry in the replicafileinfo:
     * <br/> filelist_status = missing.
     * <br/> filelist_checkdatetime = current time. 
     * 
     * The replicafileinfo is in the filelist.
     * 
     * @param replicafileinfoId The id of the replicafileinfo.
     */
    private void updateReplicaFileInfoMissingFromFilelist(
            long replicafileinfoId) {
        try {
            // The SQL statement
            String sql = "UPDATE replicafileinfo SET filelist_status = ?, "
                    + "filelist_checkdatetime = ?, upload_status = ? "
                    + "WHERE replicafileinfo_guid = ?";

            // init.
            PreparedStatement statement = null;
            Connection connection = getDbConnection();
            Date now = new Date(Calendar.getInstance().getTimeInMillis());

            // complete the SQL statement.
            statement = DBUtils.prepareStatement(connection, sql,
                    FileListStatus.MISSING.ordinal(), now, 
                    ReplicaStoreState.UPLOAD_FAILED.ordinal(),
                    replicafileinfoId);

            // execute the SQL statement
            statement.executeUpdate();
            connection.commit();
        } catch (Exception e) {
            String msg = "Problems updating the replicafileinfo.";
            log.warn(msg);
            throw new IOFailure(msg, e);
        }
    }

    /**
     * Method for updating the checksum status of a replicafileinfo instance.
     * Updates the following fields for the entry in the replicafileinfo:
     * <br/> checksum_status = CORRUPT.
     * <br/> checksum_checkdatetime = current time. 
     * 
     * The replicafileinfo is in the filelist.
     * 
     * @param replicafileinfoId The id of the replicafileinfo.
     */
    private void updateReplicaFileInfoChecksumCorrupt(long replicafileinfoId) {
        try {
            // The SQL statement
            String sql = "UPDATE replicafileinfo SET checksum_status = ?, "
                    + "checksum_checkdatetime = ?, upload_status = ? "
                    + "WHERE replicafileinfo_guid = ?";

            // init.
            PreparedStatement statement = null;
            Connection connection = getDbConnection();
            Date now = new Date(Calendar.getInstance().getTimeInMillis());

            // complete the SQL statement.
            statement = DBUtils.prepareStatement(connection, sql,
                    ChecksumStatus.CORRUPT.ordinal(), now, 
                    ReplicaStoreState.UPLOAD_FAILED.ordinal(),
                    replicafileinfoId);

            // execute the SQL statement
            statement.executeUpdate();
            connection.commit();
        } catch (Exception e) {
            String msg = "Problems updating the replicafileinfo.";
            log.warn(msg);
            throw new IOFailure(msg, e);
        }
    }

    /**
     * Method for updating the checksum status of a replicafileinfo instance.
     * Updates the following fields for the entry in the replicafileinfo:
     * <br/> checksum_status = UNKNOWN.
     * <br/> checksum_checkdatetime = current time. 
     * 
     * The replicafileinfo is in the filelist.
     * 
     * @param replicafileinfoId The id of the replicafileinfo.
     */
    private void updateReplicaFileInfoChecksumUnknown(long replicafileinfoId) {
        try {
            // The SQL statement
            String sql = "UPDATE replicafileinfo SET checksum_status = ?, "
                    + "checksum_checkdatetime = ? "
                    + "WHERE replicafileinfo_guid = ?";

            // init.
            PreparedStatement statement = null;
            Connection connection = getDbConnection();
            Date now = new Date(Calendar.getInstance().getTimeInMillis());

            // complete the SQL statement.
            statement = DBUtils.prepareStatement(connection, sql,
                    ChecksumStatus.UNKNOWN.ordinal(), now, replicafileinfoId);

            // execute the SQL statement
            statement.executeUpdate();
            connection.commit();
        } catch (Exception e) {
            String msg = "Problems updating the replicafileinfo.";
            log.warn(msg);
            throw new IOFailure(msg, e);
        }
    }
    
    /**
     * Method for updating the checksum status of a replicafileinfo instance.
     * Updates the following fields for the entry in the replicafileinfo:
     * <br/> checksum_status = OK.
     * <br/> upload_status = UPLOAD_COMLPETE.
     * <br/> checksum_checkdatetime = current time. 
     * <br/><br/>
     * The file is required to exist in the replica.
     * 
     * @param replicafileinfoId The id of the replicafileinfo.
     */
    private void updateReplicaFileInfoChecksumOk(long replicafileinfoId) {
        try {
            // The SQL statement
            String sql = "UPDATE replicafileinfo SET checksum_status = ?, "
                    + "checksum_checkdatetime = ?, upload_status = ? "
                    + "WHERE replicafileinfo_guid = ?";

            // init.
            PreparedStatement statement = null;
            Connection connection = getDbConnection();
            Date now = new Date(Calendar.getInstance().getTimeInMillis());

            // complete the SQL statement.
            statement = DBUtils.prepareStatement(connection, sql,
                    ChecksumStatus.OK.ordinal(), now, 
                    ReplicaStoreState.UPLOAD_COMPLETED.ordinal(),
                    replicafileinfoId);

            // execute the SQL statement
            statement.executeUpdate();
            connection.commit();
        } catch (Exception e) {
            String msg = "Problems updating the replicafileinfo.";
            log.warn(msg);
            throw new IOFailure(msg, e);
        }
    }

    /**
     * Method for updating the checksum_updated field for a given replica
     * in the replica table.
     * This is called when a checksum_job has been handled.
     * 
     * The following fields for the entry in the replica table:
     * <br/> checksum_updated = now.
     * 
     * @param rep The replica which has just been updated.
     */
    private void updateChecksumDateForReplica(Replica rep) {
        try {
            PreparedStatement statement = null;
            Connection connection = getDbConnection();
            Date now = new Date(Calendar.getInstance().getTimeInMillis());
            String sql = "UPDATE replica SET checksum_updated = ? WHERE "
                    + "replica_id = ?";
            statement = DBUtils.prepareStatement(connection, sql, now, rep
                    .getId());
            statement.executeUpdate();
            connection.commit();
        } catch (Exception e) {
            String msg = "Cannot update the checksum_updated for replica '"
                    + rep + "'.";
            log.warn(msg);
            throw new IOFailure(msg, e);
        }
    }
    
    /**
     * Method for updating the filelist_updated field for a given replica
     * in the replica table.
     * This is called when a filelist_job or a checksum_job has been handled.
     * 
     * The following fields for the entry in the replica table:
     * <br/> filelist_updated = now.
     * 
     * @param rep The replica which has just been updated.
     */
    private void updateFilelistDateForReplica(Replica rep) {
        try {
            PreparedStatement statement = null;
            Connection connection = getDbConnection();
            Date now = new Date(Calendar.getInstance().getTimeInMillis());
            String sql = "UPDATE replica SET filelist_updated = ? WHERE "
                    + "replica_id = ?";
            statement = DBUtils.prepareStatement(connection, sql, now, rep
                    .getId());
            statement.executeUpdate();
            connection.commit();
        } catch (Exception e) {
            String msg = "Cannot update the filelist_updated for replica '"
                    + rep + "'.";
            log.warn(msg);
            throw new IOFailure(msg, e);
        }
    }
    
    /**
     * Method for setting the filelist_updated field for a given replica
     * in the replica table to a specified value.
     * This is only called when the admin.data is converted.
     * 
     * The following fields for the entry in the replica table:
     * <br/> filelist_updated = date.
     * 
     * @param rep The replica which has just been updated.
     * @param date The date for the last filelist update.
     */
    private void setFilelistDateForReplica(Replica rep, Date date) {
        try {
            PreparedStatement statement = null;
            Connection connection = getDbConnection();
            String sql = "UPDATE replica SET filelist_updated = ? WHERE "
                    + "replica_id = ?";
            statement = DBUtils.prepareStatement(connection, sql, date, rep
                    .getId());
            statement.executeUpdate();
            connection.commit();
        } catch (Exception e) {
            String msg = "Cannot update the filelist_updated for replica '"
                    + rep + "'.";
            log.warn(msg);
            throw new IOFailure(msg, e);
        }
    }

    /**
     * Method for setting the checksum_updated field for a given replica
     * in the replica table to a specified value.
     * This is only called when the admin.data is converted.
     * 
     * The following fields for the entry in the replica table:
     * <br/> checksum_updated = date.
     * 
     * @param rep The replica which has just been updated.
     * @param date The date for the last checksum update.
     */
    private void setChecksumlistDateForReplica(Replica rep, Date date) {
        try {
            PreparedStatement statement = null;
            Connection connection = getDbConnection();
            String sql = "UPDATE replica SET checksum_updated = ? WHERE "
                    + "replica_id = ?";
            statement = DBUtils.prepareStatement(connection, sql, date, rep
                    .getId());
            statement.executeUpdate();
            connection.commit();
        } catch (Exception e) {
            String msg = "Cannot update the filelist_updated for replica '"
                    + rep + "'.";
            log.warn(msg);
            throw new IOFailure(msg, e);
        }
    }

    /**
     * Method for testing whether a replica already is within the database.
     * 
     * @param rep The replica to find in the database.
     * @return Whether the replica is found in the database.
     */
    private boolean existsReplicaInDB(Replica rep) {
        // retrieve the amount of times this replica is within the database.
        String sql = "SELECT COUNT(*) FROM replica WHERE replica_id = ?";
        int count = DBUtils.selectIntValue(getDbConnection(), sql, rep.getId());

        // Handle the different cases for count.
        switch (count) {
        case 0:
            return false;
        case 1:
            return true;
        default:
            throw new IOFailure("Cannot handle " + count + " replicas "
                    + "with id '" + rep.getId() + "'.");
        }
    }   
    
    /**
     * Method for retrieving ReplicaFileInfo entry in the database.
     * 
     * @param replicaFileInfoGuid The guid for the specific replicafileinfo.
     * @return The replicafileinfo.
     */
    private ReplicaFileInfo getReplicaFileInfo(long replicaFileInfoGuid) {
        // retrieve all
        String sql = "SELECT replicafileinfo_guid, replica_id, file_id, "
            + "segment_id, checksum, upload_status, filelist_status, "
            + "checksum_status, filelist_checkdatetime, checksum_checkdatetime "
            + "FROM replicafileinfo WHERE replicafileinfo_guid = ?";

        PreparedStatement s = null;

        try {
            s = DBUtils.prepareStatement(getDbConnection(), sql, 
                    replicaFileInfoGuid);
            ResultSet res = s.executeQuery();
            res.next();

            // return the corresponding replica file info.
            return new ReplicaFileInfo(res.getLong(1), res.getString(2), 
             res.getLong(3), res.getLong(4), res.getString(5), res.getInt(6),
             res.getInt(7), res.getInt(8), res.getDate(9), res.getDate(10));
        } catch (SQLException e) {
            final String message = "SQL error while selecting ResultsSet "
                    + "by executing statement '" + sql + "'.";
            log.warn(message, e);
            throw new IOFailure(message, e);
        } finally {
            DBUtils.closeStatementIfOpen(s);
        }
    }
    
    /**
     * Method for retrieving the data for the wanted entries in the
     * replicafileinfo table. All the replicafileinfo entries with no checksum
     * defined is ignored. 
     * 
     * @param rfiGuids The list of guids for the entries in the replicafileinfo
     * table which is wanted.
     * @return The complete data for these entries in the replicafileinfo table.
     */
    private List<ReplicaFileInfo> retrieveReplicaFileInfosWithChecksum(
            List<Long> rfiGuids) {
        ArrayList<ReplicaFileInfo> result = new ArrayList<ReplicaFileInfo>();

        // Extract all the replicafileinfos, but only put the entries with a
        // non-empty checksum into the result list.
        for (long rfiGuid : rfiGuids) {
            ReplicaFileInfo rfi = getReplicaFileInfo(rfiGuid);
            if (rfi.getChecksum() != null && !rfi.getChecksum().isEmpty()) {
                result.add(rfi);
            }
        }

        return result;
    }
    
    /**
     * Method for updating an entry in the replicafileinfo table. 
     * This method does not update the 'checksum_checkdatetime'
     * and 'filelist_checkdatetime'. 
     * 
     * @param replicafileinfoGuid The guid to update.
     * @param checksum The new checksum for the entry.
     * @param state The state for the upload.
     * @throws IOFailure If an error occurs in the database connection. 
     */
    private void updateReplicaFileInfo(long replicafileinfoGuid, 
            String checksum, ReplicaStoreState state) throws IOFailure {
        try {
            String sql = "UPDATE replicafileinfo SET checksum = ?, "
                + "upload_status = ?, filelist_status = ?, "
                + "checksum_status = ? WHERE replicafileinfo_guid = ?";

            // init.
            PreparedStatement statement = null;
            Connection connection = getDbConnection();
            
            FileListStatus fls;
            ChecksumStatus cs;

            if(state == ReplicaStoreState.UPLOAD_COMPLETED) {
                fls = FileListStatus.OK;
                cs = ChecksumStatus.OK;
            } else if (state == ReplicaStoreState.UPLOAD_FAILED) {
                fls = FileListStatus.MISSING;
                cs = ChecksumStatus.UNKNOWN;
            } else {
                fls = FileListStatus.NO_FILELIST_STATUS;
                cs = ChecksumStatus.UNKNOWN;
            }

            // complete the SQL statement.
            statement = DBUtils.prepareStatement(connection, sql, checksum,
                    state.ordinal(), fls.ordinal(), cs.ordinal(), 
                    replicafileinfoGuid);
            statement.executeUpdate();
            connection.commit();
        } catch (Exception e) {
            String errMsg = "Problems with updating a ReplicaFileInfo";
            log.warn(errMsg);
            throw new IOFailure(errMsg, e);
        }
    }
    
    /**
     * Method for updating an entry in the replicafileinfo table. 
     * This method updates the 'checksum_checkdatetime'
     * and 'filelist_checkdatetime' with the given date argument.
     *  
     * @param replicafileinfoGuid The guid to update.
     * @param checksum The new checksum for the entry.
     * @param date The date for the update.
     * @param state The status for the upload.
     * @throws IOFailure If an error occurs in the connection to the database.
     */
    private void updateReplicaFileInfo(long replicafileinfoGuid, 
            String checksum, Date date, ReplicaStoreState state) 
            throws IOFailure {
        try {
            String sql = "UPDATE replicafileinfo SET checksum = ?, "
                + "upload_status = ?, filelist_status = ?, "
                + "checksum_status = ?, checksum_checkdatetime = ?, "
                + "filelist_checkdatetime = ? WHERE replicafileinfo_guid = ?";

            // init.
            PreparedStatement statement = null;
            Connection connection = getDbConnection();

            FileListStatus fls;
            ChecksumStatus cs;

            if(state == ReplicaStoreState.UPLOAD_COMPLETED) {
                fls = FileListStatus.OK;
                cs = ChecksumStatus.OK;
            } else if (state == ReplicaStoreState.UPLOAD_FAILED) {
                fls = FileListStatus.MISSING;
                cs = ChecksumStatus.UNKNOWN;
            } else {
                fls = FileListStatus.NO_FILELIST_STATUS;
                cs = ChecksumStatus.UNKNOWN;
            }

            // complete the SQL statement.
            statement = DBUtils.prepareStatement(connection, sql, checksum,
                    state.ordinal(), fls.ordinal(), cs.ordinal(), date, date, 
                    replicafileinfoGuid);
            statement.executeUpdate();
            connection.commit();
        } catch (Exception e) {
            String errMsg = "Problems with updating a ReplicaFileInfo";
            log.warn(errMsg);
            throw new IOFailure(errMsg);
        }
    }
    
    /**
     * Retrieves the UploadStatus for a specific entry in the replicafileinfo 
     * table identified by the file guid and the replica id.
     * 
     * @param fileGuid The id of the file.
     * @param repId The id of the replica.
     * @return The upload status of the corresponding replicafileinfo entry.
     */
    private ReplicaStoreState retrieveUploadStatus(long fileGuid, 
            String repId) {
        // sql query for retrieval of upload status for a specific entry. 
        String sql = "SELECT upload_status FROM replicafileinfo WHERE "
            + "file_id = ? AND replica_id = ?";
        int us = DBUtils.selectIntValue(getDbConnection(), sql, fileGuid, 
                repId);
        return ReplicaStoreState.fromOrdinal(us);
    }
    
    /**
     * Retrieves the checksum for a specific entry in the replicafileinfo table
     * identified by the file guid and the replica id.
     *  
     * @param fileGuid The guid of the file in the file table.
     * @param repId The id of the replica.
     * @return The checksum of the corresponding replicafileinfo entry.  
     */
    private String retrieveChecksumForReplicaFileInfoEntry(long fileGuid, 
            String repId) {
        // sql query for retrieval of checksum value for an specific entry.
        String sql = "SELECT checksum FROM replicafileinfo WHERE file_id = ? "
            + "AND replica_id = ?";
        return DBUtils.selectStringValue(getDbConnection(), sql, fileGuid, 
                repId);
    }
    
    /**
     * Retrieves the checksum status for a specific entry in the 
     * replicafileinfo table identified by the file guid and the replica id.
     *  
     * @param fileGuid The guid of the file in the file table.
     * @param repId The id of the replica.
     * @return The checksum status of the corresponding replicafileinfo entry.  
     */
    private ChecksumStatus retrieveChecksumStatusForReplicaFileInfoEntry(
            long fileGuid, String repId) {
        // sql query for retrieval of checksum value for an specific entry.
        String sql = "SELECT checksum_status FROM replicafileinfo WHERE "
            + "file_id = ? AND replica_id = ?";
        // retrieve the ordinal for the checksum status.
        int statusOrdinal = DBUtils.selectIntValue(getDbConnection(), sql,
                fileGuid, repId);
        // return the checksum corresponding to the ordinal.
        return ChecksumStatus.fromOrdinal(statusOrdinal);
    }
    
    /**
     * Method for finding the checksum which are present most times in the 
     * list.
     * 
     * @param checksums The list of checksum to vote about.
     * @return The most common checksum, or null if several exists.
     */
    private String vote(List<String> checksums) {
        log.debug("voting for checksums: " + checksums.toString());
        
        // count the occurrences of each unique checksum.
        Map<String, Integer> csMap = new HashMap<String, Integer>();
        for(String cs : checksums) {
            if(csMap.containsKey(cs)) {
                // count one more!
                Integer count = csMap.get(cs) + 1;
                csMap.put(cs, count);
            } else {
                csMap.put(cs, 1);
            }
        }
        
        // find the checksum with the largest count.
        int largestCount = -1;
        boolean unique = false;
        String checksum = null;
        for(Map.Entry<String, Integer> entry : csMap.entrySet()) {
            if(entry.getValue() > largestCount) {
                largestCount = entry.getValue();
                checksum = entry.getKey();
                unique = true;
            } else if(entry.getValue() == largestCount) {
                unique = false;
            }
        }
        
        // if not unique, then log an error and return null!
        if(!unique) {
            log.error("No checksum has the most occurrences in '"
                    + csMap + "'. A null has been returned!");
            return null;
        }
        
        return checksum;
    }
    
    /**
     * The method for voting about the checksum of a file. <br/>
     * Each entry in the replicafileinfo table containing the file is retrieved.
     * All the unique checksums are retrieved, e.g. if a checksum is found more
     * than one, then it is ignored. <br/>
     * If only one unique checksum is found, then if must be the correct one, 
     * and all the replicas with this file will have their checksum_status set 
     * to 'OK'. <br/>
     * If more than one checksum is found, then a vote for the correct checksum
     * is performed. This is done by counting the amount of time each of the 
     * unique checksum is found among the replicafileinfo entries for the 
     * current file. The checksum with most votes is chosen as the correct one,
     * and the checksum_status for all the replicafileinfo entries with this 
     * checksum is set to 'OK', whereas the replicafileinfo entries with a 
     * different checksum is set to 'CORRUPT'. <br/>  
     * If no winner is found then a warning and a notification is issued, and 
     * the checksum_status for all the replicafileinfo entries with for the 
     * current file is set to 'UNKNOWN'. <br/>
     * 
     * @param fileId The id for the file to vote about.
     */
    private void fileChecksumVote(long fileId) {
        // Get all the replicafileinfo instances for the fileid, though
        // only the ones which have a valid checksum.
        // Check the checksums against each other if they differ,
        // then set to CORRUPT.
        List<Long> rfiGuids = retrieveReplicaFileInfoGuidsForFile(fileId);
        List<ReplicaFileInfo> rfis = retrieveReplicaFileInfosWithChecksum(
                rfiGuids);

        // handle the case, when no replicas has a checksum of the file.
        if (rfis.size() == 0) {
            // issue a warning.
            log.warn("No replicas contains a valid version of the file '"
                    + retrieveFilenameForFileId(fileId) + "'.");

            return;
        }

        // Put all the checksums into a hash set to obtain a set of
        // unique checksums.
        Set<String> hs = new HashSet<String>(rfis.size());
        for (ReplicaFileInfo rfi : rfis) {
            // only accept those files which can be found.
            if(rfi.getFileListState() == FileListStatus.OK) {
                hs.add(rfi.getChecksum());
            }
        }
        
        // handle the unlikely case, where the file is missing from everywhere!
        if(hs.size() == 0) {
            String errorMsg = "The file '" + retrieveFilenameForFileId(fileId) 
            + "' is missing in all replicas";
            log.warn(errorMsg);
            NotificationsFactory.getInstance().errorEvent(errorMsg);
            
            return;
        }

        // if at exactly one unique checksum is found, then no irregularities
        // among the checksums are found.
        if (hs.size() == 1) {
            log.trace("No irregularities found for the file with id '"
                    + fileId + "'.");

            // Tell all the replicafileinfo entries that their checksum
            // is ok
            for (ReplicaFileInfo rfi : rfis) {
                // only set OK for those replica where the file is.
                if(rfi.getFileListState() == FileListStatus.OK) {
                    updateReplicaFileInfoChecksumOk(rfi.getGuid());
                }
            }

            // go to next entry in the file table.
            return;
        }

        // Make a list of the checksums for voting.
        List<String> checksums = new ArrayList<String>();
        for (ReplicaFileInfo rfi : rfis) {
            if(rfi.getFileListState() == FileListStatus.OK) {
                checksums.add(rfi.getChecksum());
            }
        }
        
        // vote to find the unique most common checksum (null if no unique).
        String uniqueChecksum = vote(checksums);
        
        if (uniqueChecksum != null) {
            // change checksum_status to CORRUPT for the replicafileinfo
            // which
            // does not have the chosen checksum.
            // Set the others replciafileinfo entries to OK.
            for (ReplicaFileInfo rfi : rfis) {
                if (!rfi.getChecksum().equals(uniqueChecksum)) {
                    updateReplicaFileInfoChecksumCorrupt(rfi.getGuid());
                } else {
                    updateReplicaFileInfoChecksumOk(rfi.getGuid());
                }
            }
        } else {
            // Handle the case, when no checksum has most votes.
            String errMsg = "There is no winner of the votes between "
                    + "the replicas for the checksum of file '"
                    + retrieveFilenameForFileId(fileId) + "'.";
            log.error(errMsg);

            // send a notification
            NotificationsFactory.getInstance().errorEvent(errMsg);

            // set all replicafileinfo entries to unknown
            for (ReplicaFileInfo rfi : rfis) {
                updateReplicaFileInfoChecksumUnknown(rfi.getGuid());
            }
        }
    }
    
    /**
     * Method for retrieving the entry in the replicafileinfo table for a
     * given file and replica.
     * 
     * @param filename The name of the file for the entry.
     * @param replica The replica of the entry. 
     * @return The replicafileinfo entry corresponding to the given filename 
     * and replica.
     * @throws ArgumentNotValid If the filename is either null or empty, or if
     * the replica is null.
     */
    public ReplicaFileInfo getReplicaFileInfo(String filename, Replica replica) 
            throws ArgumentNotValid {
        ArgumentNotValid.checkNotNullOrEmpty(filename, "String filename");
        ArgumentNotValid.checkNotNull(replica, "Replica replica");
        
        // get the file id.
        long fileId = retrieveIdForFile(filename);
        // get the replicafileinfo guid.
        long guid = retrieveReplicaFileInfoGuid(fileId, replica.getId());
        
        return getReplicaFileInfo(guid);
    }
    
    /**
     * Method for retrieving the checksum for a specific file.
     * Since a file is not directly attached with a checksum, the checksum of 
     * a file must be found by having the replicafileinfo entries for the file
     * vote about it.
     * 
     * @param filename The name of the file, whose checksum are to be found.
     * @return The checksum of the file, or a Null if no validated checksum
     * can be found.
     * @throws ArgumentNotValid If teh filename is either null or the empty 
     * string.
     */
    public String getChecksum(String filename) throws ArgumentNotValid {
        ArgumentNotValid.checkNotNullOrEmpty(filename, "String filename");
        
        // retrieve the fileId
        long fileId = retrieveIdForFile(filename);
        
        // Check if a checksum with status OK for the file can be found in 
        // the database
        for(Replica rep : Replica.getKnown()) {
            // Return the checksum, if it has a valid status.
            if(retrieveChecksumStatusForReplicaFileInfoEntry(fileId, 
                    rep.getId()) == ChecksumStatus.OK) {
                return retrieveChecksumForReplicaFileInfoEntry(fileId, 
                        rep.getId());
            }
        }
        
        // log that we vote about the file.
        log.debug("No commonly accepted checksum for the file '" + filename 
                + "' has previously been found. Voting to achieve one.");
        
        // retrieves all the UNKNOWN_STATE checksums, and return if unanimous.
        Set<String> checksums = new HashSet<String>();
        
        for(Replica rep : Replica.getKnown()) {
            if(retrieveChecksumStatusForReplicaFileInfoEntry(fileId, 
                    rep.getId()) != ChecksumStatus.CORRUPT) {
                String tmpChecksum = retrieveChecksumForReplicaFileInfoEntry(
                        fileId, rep.getId());
                if(tmpChecksum != null) {
                    checksums.add(tmpChecksum);
                } else {
                    log.info("Replica '" + rep.getId() + "' has a null "
                            + "checksum for the file '" 
                            + retrieveFilenameForFileId(fileId) + "'.");
                }
            }
        }
        
        // check if unanimous (thus exactly one!)
        if(checksums.size() == 1) {
            // return the first and only value.
            return checksums.iterator().next();
        }
        
        // If no checksums are found, then return null.
        if(checksums.size() == 0) {
            log.warn("No checksums found for file '" + filename + "'.");
            return null;
        }
        
        log.info("No unanimous checksum found for file '" + filename + "'");
        // put all into a list for voting
        List<String> checksumList = new ArrayList<String>();
        for(Replica rep : Replica.getKnown()) {
            String cs = retrieveChecksumForReplicaFileInfoEntry(fileId, 
                    rep.getId());
            
            if(cs != null) {
                checksumList.add(cs);
            } else {
                // log when it is second time we find this checksum to be null?
                log.debug("Replica '" + rep.getId() + "' has a null "
                        + "checksum for the file '" 
                        + retrieveFilenameForFileId(fileId) + "'.");
            }
        }
        
        // vote and return the most occurred checksum. 
        return vote(checksumList);
    }
    
    /**
     * Retrieves the names of all the files in the file table of the database.
     *   
     * @return The list of filenames known by the database.
     */
    public Collection<String> retrieveAllFilenames() {
        // make sql query.
        String sql = "SELECT filename FROM file";

        // Perform the select.
        return DBUtils.selectStringList(getDbConnection(), sql, new Object[]{});
    }
    
    /**
     * Retrieves the ReplicaStoreState for the entry in the 
     * replicafileinfo table, which refers to the given file and replica.
     * 
     * @param filename The name of the file in the filetable.
     * @param replicaId The id of the replica.
     * @return The ReplicaStoreState for the specified entry.
     * @throws ArgumentNotValid If the replicaId or the filename are eihter 
     * null or the empty string.
     */
    public ReplicaStoreState getReplicaStoreState(String filename, String 
            replicaId) throws ArgumentNotValid {
        ArgumentNotValid.checkNotNullOrEmpty(filename, "String filename");
        ArgumentNotValid.checkNotNullOrEmpty(replicaId, "String replicaId");
        
        // retrieve the guid for the file.
        long fileId = retrieveIdForFile(filename);
        
        // Make query for extracting the upload status.
        String sql = "SELECT upload_status FROM replicafileinfo WHERE "
            + "file_id = ? AND replica_id = ?";
        // execute the query.
        int ordinal = DBUtils.selectIntValue(getDbConnection(), sql, fileId, 
                replicaId);

        // return the corresponding ReplicaStoreState.
        return ReplicaStoreState.fromOrdinal(ordinal);
    }
    
    /**
     * Sets the ReplicaStoreState for the entry in the replicafileinfo table.
     * 
     * @param filename The name of the file in the filetable.
     * @param replicaId The id of the replica.
     * @param state The ReplicaStoreState for the specified entry.
     * @throws ArgumentNotValid If the replicaId or the filename are eihter 
     * null or the empty string. Or if the ReplicaStoreState is null.
     */
    public void setReplicaStoreState(String filename, String replicaId, 
            ReplicaStoreState state) throws ArgumentNotValid {
        ArgumentNotValid.checkNotNullOrEmpty(filename, "String filename");
        ArgumentNotValid.checkNotNullOrEmpty(replicaId, "String replicaId");
        ArgumentNotValid.checkNotNull(state, "ReplicaStoreState state");
        
        // retrieve the guid for the file.
        long fileId = retrieveIdForFile(filename);
        
        try {
            // Make query for extracting the upload status.
            String sql = "UPDATE replicafileinfo SET upload_status = ? "
                + "WHERE replica_id = ? AND file_id = ?";

            // init statement.
            PreparedStatement statement = null;
            Connection connection = getDbConnection();

            // complete the SQL statement.
            statement = DBUtils.prepareStatement(connection, sql, 
                    state.ordinal(), replicaId, fileId);
            
            // execute the update and commit to database.
            statement.executeUpdate();
            connection.commit();
        } catch (SQLException e) {
            String errMsg = "Problems with updating a ReplicaFileInfo";
            log.warn(errMsg);
            throw new IOFailure(errMsg);
        }
    }
    
    /**
     * Creates a new entry for the filename for each replica, and give it the
     * given checksum and set the upload_status = UNKNOWN_UPLOAD_STATUS.
     * 
     * @param filename The name of the file.
     * @param checksum The checksum of the file.
     * @throws ArgumentNotValid If the filename or the checksum is either null
     * or the empty string.
     * @throws IllegalState If the file exists with another checksum on one of
     * the replicas. Or if the file has already been completely uploaded to
     * one of the replicas.
     */
    public void insertNewFileForUpload(String filename, String checksum) 
            throws ArgumentNotValid, IllegalState {
        ArgumentNotValid.checkNotNullOrEmpty(filename, "String filename");
        ArgumentNotValid.checkNotNullOrEmpty(checksum, "String checkums");
        
        // retrieve the fileId for the filename.
        long fileId;
        
        // insert into DB, or make sure that it can be inserted.
        if(existsFileInDB(filename)) {
            // retrieve the fileId of the existing file.
            fileId = retrieveIdForFile(filename);
            
            // Check whether the entries for the replicas.
            for(Replica rep : Replica.getKnown()) {
                // Ensure that the file has not been completely uploaded to a 
                // replica.
                ReplicaStoreState us = retrieveUploadStatus(fileId, 
                        rep.getId());
                
                if(us.equals(ReplicaStoreState.UPLOAD_COMPLETED)) {
                    throw new IllegalState("The file has already been "
                            + "completely uploaded to the replica: " + rep);
                }
                
                // make sure that it has not been attempted uploaded with 
                // another checksum
                String entryCs = retrieveChecksumForReplicaFileInfoEntry(
                        fileId, rep.getId());
                
                // throw an exception if the registered checksum differs.
                if(entryCs != null && !checksum.equals(entryCs)) {
                    throw new IllegalState("The file '" + filename + "' with "
                            + "checksum '" + entryCs + "' has attempted being "
                            + "uploaded with the checksum '" + checksum + "'");
                }
            }
        } else {
            fileId = insertFileIntoDB(filename);
        }
        
        for(Replica rep : Replica.getKnown()) {
            // retrieve the guid for the corresponding replicafileinfo entry
            long guid = retrieveReplicaFileInfoGuid(fileId, rep.getId());
            
            // Update with the correct information.
            updateReplicaFileInfo(guid, checksum, 
                    ReplicaStoreState.UNKNOWN_UPLOAD_STATE);
        }
    }
    
    /**
     * Method for inserting an entry into the database about a file upload has
     * begun for a specific replica.
     * It is not tested whether the entry has another checksum or another 
     * UploadStatus.
     * 
     * @param filename The name of the file.
     * @param replica The replica for the replicafileinfo.
     * @param state The new ReplicaStoreState for the entry.
     * @throws ArgumentNotValid If the filename is either null or the empty 
     * string. Or if the replica or the status is null.
     */
    public void changeStateOfReplicafileinfo(String filename, Replica replica, 
            ReplicaStoreState state) throws ArgumentNotValid {
        ArgumentNotValid.checkNotNullOrEmpty(filename, "String filename");
        ArgumentNotValid.checkNotNull(replica, "Replica rep");
        ArgumentNotValid.checkNotNull(state, "ReplicaStoreState state");
        
        // Retrieve the file_id for the file.
        long fileId = retrieveIdForFile(filename);

        // retrieve the replicafileinfo_guid for the entry.
        long guid = retrieveReplicaFileInfoGuid(fileId, replica.getId());

        try {
            PreparedStatement statement = null;
            Connection connection = getDbConnection();
            
            statement = connection.prepareStatement("UPDATE replicafileinfo "
                    + "SET upload_status = ? WHERE replicafileinfo_guid = ?");
            statement.setLong(1, state.ordinal());
            statement.setLong(2, guid);
            
            // Perform the update.
            statement.executeUpdate();
            connection.commit();
        } catch (SQLException e) {
            throw new IllegalState("Cannot update status and checksum of " 
                    + "a replicafileinfo in the database.", e);
        }
    }
    
    /**
     * Method for inserting an entry into the database about a file upload has
     * begun for a specific replica.
     * It is not tested whether the entry has another checksum or another 
     * UploadStatus.
     * 
     * @param filename The name of the file.
     * @param checksum The new checksum for the entry.
     * @param replica The replica for the replicafileinfo.
     * @param state The new ReplicaStoreState for the entry.
     * @throws ArgumentNotValid If the filename or the checksum is either null 
     * or the empty string. Or if the replica or the status is null.
     * @throws IllegalState If an sql exception is thrown.
     */
    public void changeStateOfReplicafileinfo(String filename, String checksum, 
            Replica replica, ReplicaStoreState state) throws ArgumentNotValid, 
            IllegalState {
        ArgumentNotValid.checkNotNullOrEmpty(filename, "String filename");
        ArgumentNotValid.checkNotNullOrEmpty(checksum, "String checksum");
        ArgumentNotValid.checkNotNull(replica, "Replica rep");
        ArgumentNotValid.checkNotNull(state, "ReplicaStoreState state");
        
        // Retrieve the file_id for the file.
        long fileId = retrieveIdForFile(filename);

        // retrieve the replicafileinfo_guid for the entry.
        long guid = retrieveReplicaFileInfoGuid(fileId, replica.getId());

        try {
            PreparedStatement statement = null;
            Connection connection = getDbConnection();
            
            statement = connection.prepareStatement("UPDATE replicafileinfo "
                    + "SET upload_status = ?, checksum = ? WHERE "
                    + "replicafileinfo_guid = ?");
            statement.setLong(1, state.ordinal());
            statement.setString(2, checksum);
            statement.setLong(3, guid);
            
            // Perform the update.
            statement.executeUpdate();
            connection.commit();
        } catch (SQLException e) {
            throw new IllegalState("Cannot update status and checksum of " 
                    + "a replicafileinfo in the database.", e);
        }
    }

    /**
     * Retrieves the names of all the files in the given replica which has 
     * the specified UploadStatus.
     *   
     * @param replicaId The id of the replica which contain the files.
     * @param state The ReplicaStoreState for the wanted files.
     * @return The list of filenames for the entries in the replica which has
     * the specified UploadStatus.
     * @throws ArgumentNotValid If the UploadStatus is null or if the replicaId
     * is either null or the empty string.
     */
    public Collection<String> retrieveFilenamesForReplicaEntries(
            String replicaId, ReplicaStoreState state) throws ArgumentNotValid {
        ArgumentNotValid.checkNotNull(state, "ReplicaStoreState state");
        ArgumentNotValid.checkNotNullOrEmpty(replicaId, "String replicaId");
        
        String sql = "SELECT filename FROM replicafileinfo LEFT OUTER "
            + "JOIN file ON replicafileinfo.file_id = file.file_id WHERE "
            + "replica_id = ? AND upload_status = ?";

        // Perform the update.
        return DBUtils.selectStringList(getDbConnection(), sql, replicaId,
                state.ordinal());
    }
    
    /**
     * Checks whether a file is already in the file table in the database.
     * 
     * @param filename The name of the file in the database.
     * @return Whether the file was found in the database.
     * @throws IllegalState If more than one entry with the given filename was 
     * found.
     */
    public boolean existsFileInDB(String filename) throws IllegalState {
        // retrieve the amount of times this replica is within the database.
        String sql = "SELECT COUNT(*) FROM file WHERE filename = ?";
        int count = DBUtils.selectIntValue(getDbConnection(), sql, filename);

        // Handle the different cases for count.
        switch (count) {
        case 0:
            return false;
        case 1:
            return true;
        default:
            throw new IllegalState("Cannot handle " + count + " files "
                    + "with the name '" + filename + "'.");
        }
    }
    
    /**
     * Method for retrieving the filelist_status for a replicafileinfo entry.
     * 
     * @param filename The name of the file.
     * @param replica The replica where the file should be.
     * @return The filelist_status for the file in the replica.
     * @throws ArgumentNotValid If the replica is null or the filename is 
     * either null or the empty string.
     */
    public FileListStatus retrieveFileListStatus(String filename,
            Replica replica) throws ArgumentNotValid {
        ArgumentNotValid.checkNotNullOrEmpty(filename, "String filename");
        ArgumentNotValid.checkNotNull(replica, "Replica replica");
        
        // retrieve the file_id for the file.
        long fileId = retrieveIdForFile(filename);

        // retrieve the replicafileinfo_guid for the entry.
        long guid = retrieveReplicaFileInfoGuid(fileId, replica.getId());

        // retrieve the filelist_status for the entry.
        int status = retrieveFileListStatusFromReplicaFileInfo(guid);

        // Return the corresponding FileListStatus
        return FileListStatus.fromOrdinal(status);
    }
    
    /**
     * This method is used to update the status for the checksums for all 
     * replicafileinfo entries. <br/>
     * <br/>
     * For each file in the database, the checksum vote is made in the 
     * following way. <br/>
     * Each entry in the replicafileinfo table containing the file is retrieved.
     * All the unique checksums are retrieved, e.g. if a checksum is found more
     * than one, then it is ignored. <br/>
     * If only one unique checksum is found, then if must be the correct one, 
     * and all the replicas with this file will have their checksum_status set 
     * to 'OK'. <br/>
     * If more than one checksum is found, then a vote for the correct checksum
     * is performed. This is done by counting the amount of time each of the 
     * unique checksum is found among the replicafileinfo entries for the 
     * current file. The checksum with most votes is chosen as the correct one,
     * and the checksum_status for all the replicafileinfo entries with this 
     * checksum is set to 'OK', whereas the replicafileinfo entries with a 
     * different checksum is set to 'CORRUPT'. <br/>  
     * If no winner is found then a warning and a notification is issued, and 
     * the checksum_status for all the replicafileinfo entries with for the 
     * current file is set to 'UNKNOWN'. <br/>
     */
    public void updateChecksumStatus() {
        // Get all the fileids
        List<Long> fileIds = retrieveAllFileIds();

        // For each fileid
        for (long fileId : fileIds) {
            fileChecksumVote(fileId);
        }
    }
    
    /**
     * Method for updating the status for a specific file for all the replicas.
     * If the checksums for the replicas differ for some replica, then based on 
     * a checksum vote, a specific checksum is chosen as the 'correct' one, and
     * the entries with another checksum than the 'correct one' will be marked
     * as corrupt.
     *  
     * @param filename The name of the file to update the status for.
     * @throws ArgumentNotValid If the filename is either null or the empty 
     * string.
     */
    @Override
    public void updateChecksumStatus(String filename) throws ArgumentNotValid {
        ArgumentNotValid.checkNotNullOrEmpty(filename, "String filename");
        
        // retrieve the id and vote!
        Long fileId = retrieveIdForFile(filename);
        fileChecksumVote(fileId);
    }
    
    /** Given the output of a checksum job, add the results to the database.
     *
     * The following fields in the table are updated for each corresponding 
     * entry in the replicafileinfo table:
     * <br/> - checksum = the given checksum. 
     * <br/> - filelist_status = ok.
     * <br/> - filelist_checkdatetime = now.
     * <br/> - checksum_checkdatetime = now.
     * 
     * @param checksumOutput The output of a checksum job.
     * @param replica The replica this checksum job is for.
     */
    @Override
    public void addChecksumInformation(List<String> checksumOutput,
            Replica replica) {
        // validate arguments
        ArgumentNotValid.checkNotNull(checksumOutput,
                "List<ChecksumEntry> checksumOutput");
        ArgumentNotValid.checkNotNull(replica, "Replica replica");

        // Make sure, that the replica exists in the database.
        if (!existsReplicaInDB(replica)) {
            String msg = "Cannot add checksum information, since the replica '"
                    + replica.toString()
                    + "' does not exist within the database.";
            log.warn(msg);
            throw new IOFailure(msg);
        }

        log.info("Starting processing of " + checksumOutput.size() 
                + " checksum entries for replica " + replica.getId());
        
        // Sort for finding duplicates.
        Collections.sort(checksumOutput);
        
        // retrieve the list of files already known by this cache.
        List<Long> missingReplicaRFIs = retrieveReplicaFileInfoGuidsForReplica(
                replica.getId());
        
        String lastFilename = "";
        String lastChecksum = "";
        
        for (String line : checksumOutput) {
            // parse the input.
            KeyValuePair<String, String> entry = ChecksumJob.parseLine(line);
            String filename = entry.getKey();
            String checksum = entry.getValue();
            
            // check for duplicates
            if(filename.equals(lastFilename)) {
                // if different checksums, then
                if(!checksum.equals(lastChecksum)) {
                    // log and send notification
                    String errMsg = "Unidentical duplicates of file '" 
                        + filename + "' with the checksums '" + lastChecksum
                        + "' and '" + checksum + "'. First instance used.";
                    log.error(errMsg);
                    NotificationsFactory.getInstance().errorEvent(errMsg);
                } else {
                    // log about duplicate identical 
                    log.debug("Duplicates of the file '" + filename + "' found "
                            + "with the same checksum '" + checksum + "'.");
                }
                
                // avoid overhead of inserting duplicates twice.
                continue;
            }
            
            // set these value to be the old values in next iteration.
            lastFilename = filename;
            lastChecksum = checksum;
            
            // The ID for the file.
            long fileid = -1;

            // If the file is not within DB, then insert it.
            if (!existsFileInDB(filename)) { 
                log.info("Inserting the file '" + filename + "' into the "
                        + "database.");
                fileid = insertFileIntoDB(filename);
            } else {
                fileid = retrieveIdForFile(filename);
            }

            // If the file does not already exists in the database, create it
            // and retrieve the new ID.
            if (fileid < 0) {
                log.warn("Inserting the file '" + filename + "' into the "
                        + "database, again: This should never happen!!!");
                fileid = insertFileIntoDB(filename);
            }

            // Retrieve the replicafileinfo for the file at the replica.
            long rfiId = retrieveReplicaFileInfoGuid(fileid, replica.getId());

            // Check if there already is an entry in the replicafileinfo table.
            // rfiId is negative if no entry was found.
            if (rfiId < 0) {
                // insert the file into the table.
                createReplicaFileInfoEntriesInDB(fileid);
                log.info("Inserted file '" + filename + "' for replica '"
                        + replica.toString() + "' into replicafileinfo.");
            }

            // Update this table
            updateReplicaFileInfoChecksum(rfiId, checksum);
            log.trace("Updated file '" + filename + "' for replica '"
                    + replica.toString() + "' into replicafileinfo.");
            
            // remove the replicafileinfo guid from the missing entries.
            missingReplicaRFIs.remove(rfiId);
        }
        
        // go through the not found replicafileinfo for this replica to change
        // their filelist_status to missing.
        if(missingReplicaRFIs.size() > 0) {
            log.warn("Found " + missingReplicaRFIs.size() + " missing files "
                    + "for replica '" + replica + "'.");
            for (long rfi : missingReplicaRFIs) {
                // set the replicafileinfo in the database to missing.
                updateReplicaFileInfoMissingFromFilelist(rfi);
            }
        }
        
        // update the checksum updated date for this replica.
        updateChecksumDateForReplica(replica);
        updateFilelistDateForReplica(replica);
        
        log.info("Finished processing of " + checksumOutput.size() 
                + " checksum entries for replica " + replica.getId());
    }

    /**
     * Method for adding the results from a list of filenames on a replica.
     * This list of filenames should return the list of all the files within 
     * the database. 
     * 
     * For each file in the FileListJob the following fields are set for the 
     * corresponding entry in the replicafileinfo table: 
     * <br/> - filelist_status = ok.
     * <br/> - filelist_checkdatetime = now.
     * 
     * For each entry in the replicafileinfo table for the replica which are 
     * missing in the results from the FileListJob the following fields are 
     * assigned the following values:
     * <br/> - filelist_status = missing.
     * <br/> - filelist_checkdatetime = now.
     * 
     * @param filelist The list of filenames either parsed from a FilelistJob 
     * or the result from a GetAllFilenamesMessage.
     * @param replica The replica, which the FilelistBatchjob has run upon.
     * @throws ArgumentNotValid If the filelist or the replica is null.
     */
    @Override
    public void addFileListInformation(List<String> filelist, Replica replica) 
            throws ArgumentNotValid {
        ArgumentNotValid.checkNotNull(filelist, "List<String> filelist");
        ArgumentNotValid.checkNotNull(replica, "Replica replica");

        // retrieve the list of files already known by this cache.
        List<Long> missingReplicaRFIs = retrieveReplicaFileInfoGuidsForReplica(
                replica.getId());
        
        Collections.sort(filelist);

        String lastFileName = "";
        for (String file : filelist) {
            // handle duplicates.
            if(file.equals(lastFileName)) {
                log.warn("There have been found multiple files with the name '" 
                        + file + "'");
                continue;
            }
            lastFileName = file;
            
            // retrieve the file_id for the file.
            long fileId = retrieveIdForFile(file);
            // If not found, log and create the file in the database.
            if (fileId < 0) {
                log.info("The file '" + file + "' was not found in the "
                        + "database. Thus creating entry for the file.");
                // insert the file and retrieve its file_id.
                fileId = insertFileIntoDB(file);
            }

            // retrieve the replicafileinfo_guid for this entry.
            long rfiId = retrieveReplicaFileInfoGuid(fileId, replica.getId());
            // if not found log and create the replicafileinfo in the database.
            if (rfiId < 0) {
                log.warn("Cannot find the file '" + file + "' for "
                        + "replica '" + replica.getId() + "'. Thus creating "
                        + "missing entry before updating.");
                createReplicaFileInfoEntriesInDB(fileId);
            }

            // remove from replicaRFIs, since it has been found
            missingReplicaRFIs.remove(rfiId);

            // update the replicafileinfo of this file:
            // filelist_checkdate, filelist_status, upload_status
            updateReplicaFileInfoFilelist(rfiId);
        }

        // go through the not found replicafileinfo for this replica to change
        // their filelist_status to missing.
        for (long rfi : missingReplicaRFIs) {
            // set the replicafileinfo in the database to missing.
            updateReplicaFileInfoMissingFromFilelist(rfi);
        }

        // Update the date for filelist update for this replica.
        updateFilelistDateForReplica(replica);
    }

    /**
     * Get the date for the last file list job.
     * 
     * @param replica The replica to get the date for.
     * @return The date of the last missing files update for the replica.
     * A null is returned if no last missing files update has been performed.
     * @throws ArgumentNotValid If the replica is null.
     * @throws IllegalArgumentException If the Date of the Timestamp cannot be
     * instanciated.
     */
    @Override
    public Date getDateOfLastMissingFilesUpdate(Replica replica) throws 
            ArgumentNotValid, IllegalArgumentException {
        ArgumentNotValid.checkNotNull(replica, "Replica replica");
        // sql for retrieving thie replicafileinfo_guid.
        String sql = "SELECT filelist_updated FROM replica WHERE "
                + "replica_id = ?";
        String result = DBUtils.selectStringValue(getDbConnection(), sql, 
                replica.getId());

        // return null if the field has no be set for this replica.
        if (result == null) {
            log.warn("The 'filelist_updated' field has not been set, " 
                    + "as no missing files update has been performed yet.");
            return null;
        } else {
            // Parse the timestamp into a date.
            return new Date(Timestamp.valueOf(result).getTime());
        }
    }

    /**
     * Method for retrieving the date for the last update for corrupted files.
     * 
     * This method does not contact the replicas, it only retrieves the data
     * from the last time the checksum was retrieved. 
     * 
     * @param replica The replica to find the date for the latest update for
     * corruption of files. 
     * @return The date for the last checksum update. A null is returned if no
     * wrong files update has been performed for this replica.
     * @throws ArgumentNotValid If the replica is null.
     * @throws IllegalArgumentException If the Date of the Timestamp cannot be
     * instanciated.
     */
    @Override
    public Date getDateOfLastWrongFilesUpdate(Replica replica) throws 
            ArgumentNotValid, IllegalArgumentException {
        ArgumentNotValid.checkNotNull(replica, "Replica replica");
        
        // The SQL statement for retrieving the date for last updating of
        // checksum for the replica.
        String sql = "SELECT checksum_updated FROM replica WHERE "
                + "replica_id = ?";
        String result = DBUtils.selectStringValue(getDbConnection(), sql, 
                replica.getId());

        // return null if the field has no be set for this replica.
        if (result == null) {
            log.warn("The 'checksum_updated' field has not been set, " 
                    + "as no wrong files update has been performed yet.");
            return null;
        } else {
            // Parse the timestamp into a date.
            return new Date(Timestamp.valueOf(result).getTime());
        }
    }

    /**
     * Method for retrieving the number of files missing from a specific
     * replica.
     * 
     * This method does not contact the replica directly, it only retrieves
     * the count of missing files from the last filelist update.
     * 
     * @param replica The replica to find the number of missing files for.
     * @return The number of missing files for the replica.
     * @throws ArgumentNotValid If the replica is null.
     */
    @Override
    public long getNumberOfMissingFilesInLastUpdate(Replica replica) throws 
            ArgumentNotValid {
        ArgumentNotValid.checkNotNull(replica, "Replica replica");
        
        // The SQL statement to retrieve the number of entries in the
        // replicafileinfo table with file_status set to missing for the
        // replica.
        String sql = "SELECT COUNT(*) FROM replicafileinfo WHERE replica_id"
                + " = ? AND filelist_status = ?";
        return DBUtils.selectLongValue(getDbConnection(), sql, replica.getId(),
                FileListStatus.MISSING.ordinal());
    }

    /**
     * Method for retrieving the list of the names of the files which was 
     * missing for the replica in the last filelist update.
     * 
     * This method does not contact the replica, it only uses the database to
     * find the files, which was missing during the last filelist update.
     * 
     * @param replica The replica to find the list of missing files for.
     * @return A list containing the names of the files which are missing
     * in the given replica.
     * @throws ArgumentNotValid If the replica is null.
     */
    @Override
    public Iterable<String> getMissingFilesInLastUpdate(Replica replica) throws
            ArgumentNotValid {
        ArgumentNotValid.checkNotNull(replica, "Replica replica");
        // The SQL statement to retrieve the filenames of the missing
        // replicafileinfo to the given replica.
        String sql = "SELECT filename FROM replicafileinfo LEFT OUTER JOIN "
                + "file ON replicafileinfo.file_id = file.file_id "
                + "WHERE replica_id = ? AND filelist_status = ?";
        return DBUtils.selectStringList(getDbConnection(), sql, replica.getId(),
                FileListStatus.MISSING.ordinal());
    }

    /**
     * Method for retrieving the amount of files with a incorrect checksum 
     * within a replica.
     * 
     * This method does not contact the replica, it only uses the database to
     * count the amount of files which are corrupt.
     * 
     * @param replica The replica to find the number of corrupted files for.
     * @return The number of corrupted files.
     * @throws ArgumentNotValid If the replica is null.
     */
    @Override
    public long getNumberOfWrongFilesInLastUpdate(Replica replica) throws 
            ArgumentNotValid {
        ArgumentNotValid.checkNotNull(replica, "Replica");
        // The SQL statement to retrieve the number of corrupted entries in
        // the replicafileinfo table for the given replica.
        String sql = "SELECT COUNT(*) FROM replicafileinfo WHERE replica_id"
                + " = ? AND checksum_status = ?";
        return DBUtils.selectLongValue(getDbConnection(), sql, replica.getId(),
                ChecksumStatus.CORRUPT.ordinal());
    }

    /**
     * Method for retrieving the list of the files in the replica which have 
     * a incorrect checksum. E.g. the checksum_status is set to CORRUPT.
     * 
     * This method does not contact the replica, it only uses the local 
     * database.
     * 
     * @param replica The replica to find the list of corrupted files for.
     * @return The list of files which have wrong checksums.
     * @throws ArgumentNotValid If the replica is null. 
     */
    @Override
    public Iterable<String> getWrongFilesInLastUpdate(Replica replica) throws
            ArgumentNotValid {
        ArgumentNotValid.checkNotNull(replica, "Replica replica");
        
        // The SQL statement to retrieve the filenames for the corrupted files
        // in the replicafileinfo table for the given replica.
        String sql = "SELECT filename FROM replicafileinfo LEFT OUTER JOIN "
                + "file ON replicafileinfo.file_id = file.file_id "
                + "WHERE replica_id = ? AND checksum_status = ?";
        return DBUtils.selectStringList(getDbConnection(), sql, replica.getId(),
                ChecksumStatus.CORRUPT.ordinal());
    }

    /**
     * Method for retrieving the number of files within a replica.
     * This count all the files which are not missing from the replica, thus
     * all entries in the replicafileinfo table which has the filelist_status
     * set to OK. It is ignored whether the files has a correct checksum.  
     * 
     * This method does not contact the replica, it only uses the local 
     * database.
     * 
     * @param replica The replica to count the number of files for.
     * @return The number of files within the replica.
     * @throws ArgumentNotValid If the replica is null.
     */
    @Override
    public long getNumberOfFiles(Replica replica) throws ArgumentNotValid {
        ArgumentNotValid.checkNotNull(replica, "Replica replica");
        
        // The SQL statement to retrieve the amount of entries in the
        // replicafileinfo table for the replica which have the
        // filelist_status set to OK.
        String sql = "SELECT COUNT(*) FROM replicafileinfo WHERE replica_id "
                + " = ? AND filelist_status = ?";
        return DBUtils.selectLongValue(getDbConnection(), sql, replica.getId(),
                FileListStatus.OK.ordinal());
    }
    
    /**
     * Method for finding a replica with a valid version of a file. This method
     * is used in order to find a replica from which a file should be retrieved,
     * during the process of restoring a corrupt file on another replica.
     * 
     * This replica must of the type bitarchive, since a file cannot be 
     * retrieved from a checksum replica.
     * 
     * @param filename The name of the file which needs to have a valid version
     * in a bitarchive.
     * @return A bitarchive which contains a valid version of the file, or null
     * if no such bitarchive exists.
     * @throws ArgumentNotValid If the filename is null or the empty string.
     */
    @Override
    public Replica getBitarchiveWithGoodFile(String filename) throws 
            ArgumentNotValid {
        ArgumentNotValid.checkNotNullOrEmpty(filename, "String filename");
        // First retrieve the file id for the filename
        long fileId = retrieveIdForFile(filename);

        // Then retrieve a list of replicas where the the checksum status is OK
        List<String> replicaIds = retrieveReplicaIdsWithOKChecksumStatus(
                fileId);

        // go through the list, and return the first valid bitarchive-replica.
        for (String repId : replicaIds) {
            // Retrieve the replica type.
            ReplicaType repType = ReplicaType
                    .fromOrdinal(retrieveReplicaType(repId));

            // If the replica is of type BITARCHIVE then return it.
            if (repType.equals(ReplicaType.BITARCHIVE)) {
                log.trace("The replica with id '" + repId + "' is the first "
                        + "bitarchive replica which contains the file '"
                        + filename + "' with a valid checksum.");
                return Replica.getReplicaFromId(repId);
            }
        }

        // Notify the administator about that no proper bitarchive was found. 
        NotificationsFactory.getInstance().errorEvent("No bitarchive replica "
                + "was found which contains the file '" + filename + "'.");

        // If not bitarchive containing the file with a OK checksum_status.
        // then return null.
        return null;
    }

    /**
     * Method for finding a replica with a valid version of a file. This method
     * is used in order to find a replica from which a file should be retrieved,
     * during the process of restoring a corrupt file on another replica.
     * 
     * This replica must of the type bitarchive, since a file cannot be 
     * retrieved from a checksum replica.
     * 
     * @param filename The name of the file which needs to have a valid version
     * in a bitarchive.
     * @param badReplica The Replica which has a bad copy of the given file
     * @return A bitarchive which contains a valid version of the file, or null
     * if no such bitarchive exists.
     * @throws ArgumentNotValid If the replica is null or the filename is either
     * null or the empty string. 
     */
    @Override
    public Replica getBitarchiveWithGoodFile(String filename, Replica 
            badReplica) throws ArgumentNotValid {
        ArgumentNotValid.checkNotNull(badReplica, "Replica badReplica");
        ArgumentNotValid.checkNotNullOrEmpty(filename, "String filename");
        
        // First retrieve the file id for the filename
        long fileId = retrieveIdForFile(filename);

        // Then retrieve a list of replicas where the the checksum status is OK
        List<String> replicaIds = retrieveReplicaIdsWithOKChecksumStatus(
                fileId);

        // Make sure, that the bad replica is not returned.
        replicaIds.remove(badReplica.getId());
        
        // go through the list, and return the first valid bitarchive-replica.
        for (String repId : replicaIds) {
            // Retrieve the replica type.
            ReplicaType repType = ReplicaType.fromOrdinal(
                    retrieveReplicaType(repId));

            // If the replica is of type BITARCHIVE then return it.
            if (repType.equals(ReplicaType.BITARCHIVE)) {
                log.trace("The replica with id '" + repId + "' is the first "
                        + "bitarchive replica which contains the file '"
                        + filename + "' with a valid checksum.");
                return Replica.getReplicaFromId(repId);
            }
        }

        // Notify the administator about that no proper bitarche was found. 
        NotificationsFactory.getInstance().errorEvent("No bitarchive replica "
                + "was found which contains the file '" + filename + "'.");

        // If not bitarchive containing the file with a OK checksum_status.
        // then return null.
        return null;
    }

    /**
     * Method for updating a specific entry in the replicafileinfo table. Based
     * on the filename, checksum and replica it is verified whether a file
     * is missing, corrupt or valid. 
     * 
     * @param filename Name of the file.
     * @param checksum The checksum of the file. Is allowed to be null, if no
     * file is found.
     * @param replica The replica where the file exists.
     * @throws ArgumentNotValid If the filename is null or the empty string, or
     * if the replica is null.
     */
    @Override
    public void updateChecksumInformationForFileOnReplica(String filename, 
            String checksum, Replica replica) throws ArgumentNotValid {
        ArgumentNotValid.checkNotNullOrEmpty(filename, "String filename");
        ArgumentNotValid.checkNotNull(replica, "Replica replica");
        // The checksum can be null!

        // retrieve the guid.
        PreparedStatement statement = null;
        try {
            long fileId = retrieveIdForFile(filename);
            long guid = retrieveReplicaFileInfoGuid(fileId, replica.getId());
            Date now = new Date(Calendar.getInstance().getTimeInMillis());

            Connection connection = getDbConnection();
            
            // handle differently whether a checksum was retrieved.
            if(checksum == null) {
                // Set to MISSING! and do not update the checksum 
                // (cannot insert null).
                String sql = "UPDATE replicafileinfo SET "
                    + "filelist_status = ?, checksum_status = ?, "
                    + "filelist_checkdatetime = ? "
                    + "WHERE replicafileinfo_guid = ?";
                statement = DBUtils.prepareStatement(connection, sql, 
                        FileListStatus.MISSING.ordinal(), 
                        ChecksumStatus.UNKNOWN.ordinal(), now, guid);
            } else {
                String sql = "UPDATE replicafileinfo SET checksum = ?, "
                    + "filelist_status = ?, filelist_checkdatetime = ? "
                    + "WHERE replicafileinfo_guid = ?";
                statement = DBUtils.prepareStatement(connection, sql, checksum, 
                        FileListStatus.OK.ordinal(), now, guid);
            }
            statement.executeUpdate();
            connection.commit();
        } catch (Exception e) {
            throw new IOFailure("Could not update single checksum entry.", e);
        } finally {
            // remember to close the statement.
            DBUtils.closeStatementIfOpen(statement);
        }
    }
 
    
    /**
     * Method for inserting a line of Admin.Data into the database.
     * It is assumed that it is a '0.4' admin.data line. 
     * 
     * @param line The line to insert into the database.
     * @return Whether the line was valid.
     * @throws ArgumentNotValid If the line is null. If it is empty, then it is
     * logged.
     */
    public boolean insertAdminEntry(String line) throws ArgumentNotValid {
        ArgumentNotValid.checkNotNull(line, "String line");
        log.trace("Insert admin entry begun");
        final int lengthFirstPart = 4;
        final int lengthOtherParts = 3;
        try {
            // split into parts. First contains 
            String[] split = line.split(" , ");

            // Retrieve the basic entry data.  
            String[] entryData = split[0].split(" ");
            
            // Check if enough elements
            if(entryData.length < lengthFirstPart) {
                log.warn("Bad line in Admin.data: " + line);
                return false;
            }
            
            String filename = entryData[0];
            String checksum = entryData[1];
            
            long fileId = retrieveIdForFile(filename);
            
            // If the fileId is -1, then the file is not within the file table.
            // Thus insert it and retrieve the id.
            if(fileId == -1) {
                fileId = insertFileIntoDB(filename);
            }
            log.trace("Step 1 completed (file created in database).");
            // go through the replica specifics.
            for(int i = 1; i < split.length; i++) {
                String[] repInfo = split[i].split(" ");
                
                // check if correct size
                if(repInfo.length < lengthOtherParts) {
                    log.warn("Bad replica information '" + split[i] 
                            + "' in line '" + line + "'");
                    continue;
                }
                
                //retrieve the data for this replica 
                String replicaId = 
                    Channels.retrieveReplicaFromIdentifierChannel(
                            repInfo[0]).getId();
                ReplicaStoreState replicaUploadStatus = 
                    ReplicaStoreState.valueOf(repInfo[1]);
                Date replicaDate = new Date(Long.parseLong(repInfo[2])); 
                
                // retrieve the guid of the replicafileinfo.
                long guid = retrieveReplicaFileInfoGuid(fileId, replicaId);
                
                // Update the replicaFileInfo with the information.
                updateReplicaFileInfo(guid, checksum, replicaDate, 
                        replicaUploadStatus);
            }
        } catch (IllegalState e) {
            log.warn("Received IllegalState exception while parsing.", e);
            return false;
        }
        log.trace("Insert admin entry finished");
        return true;
    }
    
    /**
     * Method for setting a specific value for the filelistdate and 
     * the checksumlistdate for all the replicas. 
     * 
     * @param date The new date for the checksumlist and filelist for all the 
     * replicas.
     * @throws ArgumentNotValid If the date is null.
     */
    public void setAdminDate(Date date) throws ArgumentNotValid {
        ArgumentNotValid.checkNotNull(date, "Date date");
        
        // set the date for the replicas.
        for(Replica rep : Replica.getKnown()) {
            setFilelistDateForReplica(rep, date);
            setChecksumlistDateForReplica(rep, date);
        }
    }
    
    /**
     * Method for telling whether the database is empty.
     * The database is empty if it does not contain any files.
     * 
     * The database will not be entirely empty, since the replicas are put into
     * the replica table during the instantiation of this class, but if the 
     * file table is empty, then the replicafileinfo table is also empty, and
     * the database will be considered empty.
     * 
     * @return Whether the file list is empty. 
     */
    public boolean isEmpty() {
        // The SQL statement to retrieve the amount of entries in the
        // file table. No arguments (represented by empty Object array).
        String sql = "SELECT COUNT(*) FROM file";
        return DBUtils.selectLongValue(getDbConnection(), sql, 
                new Object[0]) == 0L;
    }
    
    /**
     * Method to print all the tables in the database.
     * @return all the tables as a text string
     */
    public String retrieveAsText() {
        StringBuilder res = new StringBuilder();
        String sql = "";

        // Go through the replica table
        List<String> reps = retrieveIdsFromReplicaTable();
        res.append("Replica table: " + reps.size() + "\n");
        res.append("GUID \trepId \trepName \trepType \tfileupdate "
                + "\tchecksumupdated" + "\n");
        res.append("------------------------------------------------"
                + "------------\n");
        Connection connection = getDbConnection();
        for (String repId : reps) {
            // retrieve the replica_name
            sql = "SELECT replica_guid FROM replica WHERE replica_id = ?";
            String repGUID = DBUtils
                    .selectStringValue(connection, sql, repId);
            // retrieve the replica_name
            sql = "SELECT replica_name FROM replica WHERE replica_id = ?";
            String repName = DBUtils
                    .selectStringValue(connection, sql, repId);
            // retrieve the replica_type
            sql = "SELECT replica_type FROM replica WHERE replica_id = ?";
            int repType = DBUtils.selectIntValue(connection, sql, repId);
            // retrieve the date for last updated
            sql = "SELECT filelist_updated FROM replica WHERE replica_id = ?";
            String filelistUpdated = DBUtils.selectStringValue(connection,
                    sql, repId);
            // retrieve the date for last updated
            sql = "SELECT checksum_updated FROM replica WHERE replica_id = ?";
            String checksumUpdated = DBUtils.selectStringValue(connection,
                    sql, repId);

            // Print
            res.append(repGUID + "\t" + repId + "\t" + repName + "\t"
                    + ReplicaType.fromOrdinal(repType).name() + "\t"
                    + filelistUpdated + "\t" + checksumUpdated + "\n");
        }
        res.append("\n");

        // Go through the file table
        List<String> fileIds = retrieveIdsFromFileTable();
        res.append("File table : " + fileIds.size() + "\n");
        res.append("fileId \tfilename" + "\n");
        res.append("--------------------" + "\n");
        for (String fileId : fileIds) {
            // retrieve the file_name
            sql = "SELECT filename FROM file WHERE file_id = ?";
            String fileName = DBUtils.selectStringValue(connection, sql,
                    fileId);

            // Print
            res.append(fileId + " \t " + fileName + "\n");
        }
        res.append("\n");

        // Go through the replicafileinfo table
        List<String> rfiIds = retrieveIdsFromReplicaFileInfoTable();
        res.append("ReplicaFileInfo table : " + rfiIds.size() + "\n");
        res.append("GUID \trepId \tfileId \tchecksum \t"
                + "us \t\tfls \tcss \tfilelistCheckdate \t"
                + "checksumCheckdate" + "\n");
        res.append("---------------------------------------------------------"
                + "------------------------------------------------" + "\n");
        for (String rfiGUID : rfiIds) {
            // retrieve the replica_id
            sql = "SELECT replica_id FROM replicafileinfo WHERE "
                    + "replicafileinfo_guid = ?";
            String replicaId = DBUtils.selectStringValue(connection, sql,
                    rfiGUID);
            // retrieve the file_id
            sql = "SELECT file_id FROM replicafileinfo WHERE "
                    + "replicafileinfo_guid = ?";
            String fileId = DBUtils.selectStringValue(connection, sql,
                    rfiGUID);
            // retrieve the checksum
            sql = "SELECT checksum FROM replicafileinfo WHERE "
                    + "replicafileinfo_guid = ?";
            String checksum = DBUtils.selectStringValue(connection, sql,
                    rfiGUID);
            // retrieve the upload_status
            sql = "SELECT upload_status FROM replicafileinfo WHERE "
                    + "replicafileinfo_guid = ?";
            int uploadStatus = DBUtils.selectIntValue(connection, sql,
                    rfiGUID);
            // retrieve the filelist_status
            sql = "SELECT filelist_status FROM replicafileinfo WHERE "
                    + "replicafileinfo_guid = ?";
            int filelistStatus = DBUtils.selectIntValue(connection, sql,
                    rfiGUID);
            // retrieve the checksum_status
            sql = "SELECT checksum_status FROM replicafileinfo WHERE "
                    + "replicafileinfo_guid = ?";
            int checksumStatus = DBUtils.selectIntValue(connection, sql,
                    rfiGUID);
            // retrieve the filelist_checkdatetime
            sql = "SELECT filelist_checkdatetime FROM replicafileinfo WHERE "
                    + "replicafileinfo_guid = ?";
            String filelistCheckdatetime = DBUtils.selectStringValue(
                    connection, sql, rfiGUID);
            // retrieve the checksum_checkdatetime
            sql = "SELECT checksum_checkdatetime FROM replicafileinfo WHERE "
                    + "replicafileinfo_guid = ?";
            String checksumCheckdatetime = DBUtils.selectStringValue(
                    connection, sql, rfiGUID);

            // Print
            res.append(rfiGUID + " \t" + replicaId + "\t" + fileId
                    + "\t" + checksum + "\t"
                    + ReplicaStoreState.fromOrdinal(uploadStatus).name() 
                    + "  \t" + FileListStatus.fromOrdinal(filelistStatus).name()
                    + "\t" + ChecksumStatus.fromOrdinal(checksumStatus).name() 
                    + "\t" + filelistCheckdatetime + "\t" 
                    + checksumCheckdatetime + "\n");
        }
        res.append("\n");
        
        return res.toString();
    }

    /**
     * Method for cleaning up.
     */
    @Override
    public void cleanup() {
        instance = null;
    }
    
    /**
     * Get the current database connection. Check first, if the connection 
     * is valid. If the connection is terminated. Try to reestablish the 
     * connection a number of times before giving up.
     * @throws IOFailure If the database connection is down and 
     * we are unable to reestablish the connection.
     * @return the current database connection.
     */
    private synchronized Connection getDbConnection() throws IOFailure {
        int secondsToWait = 5;
        try {
            if (dbConnection.isValid(secondsToWait)) {
                return this.dbConnection;
            }
        } catch (SQLException e) {
            log.warn(
                    "Exception thrown while testing the connection to the "
                    + "database", e);
        }
        // The connection is terminated (maybe server is shutdown?)
        // Try to reestablish the connection a number of times.
        Connection newConnection = null;
        boolean establishedConnection = false;
        int tries = 0;
        
        while (!establishedConnection && tries <= maxdatabaseRetries) {
            tries++;
            try {
                newConnection = DBConnect.getDBConnection(
                        DBConnect.getArchiveUrl());
                establishedConnection = true;
                this.dbConnection = newConnection;
            } catch (IOFailure e) {
                if (tries == maxdatabaseRetries){
                    log.warn("Final attempt to reestablish "
                            + "connection to database failed.", e);
                    throw new IOFailure(
                            "Reestablishing of dbconnection failed", e);
                } else {
                    log.info("Another attempt to reestablish "
                            + "connection to database failed. Will try " 
                            + (maxdatabaseRetries - tries) 
                            + " more attempt(s) before giving up"
                            , e);
                    try {
                        Thread.sleep(delaybetweenretries);
                    } catch (InterruptedException e1) {
                        // ignore this exception
                        log.trace("Interruption ignored.", e1);
                    }
                }
            }
        }
        return this.dbConnection;
    }
}
