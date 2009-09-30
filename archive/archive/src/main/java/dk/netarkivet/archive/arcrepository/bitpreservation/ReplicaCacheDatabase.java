/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2009 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package dk.netarkivet.archive.arcrepository.bitpreservation;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.archive.arcrepositoryadmin.AdminData;
import dk.netarkivet.archive.arcrepositoryadmin.ReadOnlyAdminData;
import dk.netarkivet.common.distribute.arcrepository.Replica;
import dk.netarkivet.common.distribute.arcrepository.ReplicaStoreState;
import dk.netarkivet.common.distribute.arcrepository.ReplicaType;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.IllegalState;
import dk.netarkivet.common.exceptions.UnknownID;
import dk.netarkivet.common.utils.DBUtils;
import dk.netarkivet.common.utils.NotificationsFactory;

/**
 * Method for storing the bitpreservation cache in a database.
 * 
 * This method uses the 'admin.data' file for retrieving the upload status.
 */
public class ReplicaCacheDatabase implements BitPreservationDAO {
    /** The log.*/
    protected static final Log log
            = LogFactory.getLog(ReplicaCacheDatabase.class.getName());

    /** The current instance.*/
    private static ReplicaCacheDatabase instance;
    
    /** The connection to the database.*/
    Connection dbConnection;
    
    /**
     * Constructor.
     */
    private ReplicaCacheDatabase() {
        // Initialise the database
        dbConnection = DBConnect.getDBConnection();

        // initialise the database.
        initialiseDB();
    }
    
    /**
     * Method for retrieving the current instance of this class.
     * 
     * @return The current instance.
     */
    public static ReplicaCacheDatabase getInstance() {
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
            if (!repIds.remove(rep.getId())) {
                // if the replica id cannot be removed from the list, then it
                // does not exist in the database and must be added.
                log.info("Inserting replica '" + rep.toString()
                        + "' to database.");
                insertReplicaIntoDB(rep);
            } else {
                // Otherwise it already exists in the DB.
                log.info("Replica '" + rep.toString()
                        + "' already inserted in database.");
            }
        }

        // check whether unknown replicas is already within the database.
        if (repIds.size() > 0) {
            log.warn("The database contain ID of the following "
                    + "replicas, which has not defined in the settings: "
                    + repIds);
        }
    }
    
    /**
     * Method for checking whether a replicafileinfo is within the database.
     * 
     * @param fileid The id of the file.
     * @param replicaID The id of the replica.
     * @return Whether the replicafileinfo was there or not.
     * @throws IOFailure If more than one copy of the replicafileinfo is 
     * placed within the database.
     */
    private boolean existsReplicaFileInfoInDB(long fileid, String replicaID)
            throws IOFailure {
        // retrieve the amount of times this replicafileinfo
        // is within the database.
        String sql = "SELECT COUNT(*) FROM replicafileinfo WHERE file_id = ? "
                + "AND replica_id = ?";
        int count = DBUtils
                .selectIntValue(dbConnection, sql, fileid, replicaID);

        // Handle the different cases for count.
        switch (count) {
        case 0:
            return false;
        case 1:
            return true;
        default:
            throw new IOFailure("Cannot handle " + count + " files "
                    + "with the name '" + fileid + "'.");
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
            PreparedStatement statement = null;
            dbConnection.setAutoCommit(false);
            
            // Make the SQL statement for putting the replica into the database
            // and insert the variables for the entry to the replica table.
            statement = dbConnection.prepareStatement(
                    "INSERT INTO replica "
                    + "(replica_id, replica_name, replica_type) "
                    + "VALUES ( ?, ?, ?)");
            statement.setString(1, rep.getId());
            statement.setString(2, rep.getName());
            statement.setInt(3, rep.getType().ordinal());
            
            // execute the SQL statement
            statement.executeUpdate();
            dbConnection.commit();
        } catch (SQLException e) {
            String msg = "Cannot add replica to the database.";
            log.warn(msg);
            throw new IOFailure(msg, e);
        }
    }
    
    /**
     * Method to create a new entry in the file table in the database.
     * The file_id is automatically created by the database, and the argument
     * is used for the filename for this new entry to the table. 
     * This will also create a replicafileinfo entry for each replica.
     * 
     * @param filename The filename for the new entry in the file table.
     */
    private void insertFileIntoDB(String filename) {
        try {
            PreparedStatement statement = null;
            dbConnection.setAutoCommit(false);
            
            // Make the SQL statement for putting the replica into the database
            // and insert the variables for the entry to the replica table.
            statement = dbConnection.prepareStatement(
                    "INSERT INTO file "
                    + "(filename) "
                    + "VALUES ( ? )");
            statement.setString(1, filename);
            
            // execute the SQL statement
            statement.executeUpdate();
            dbConnection.commit();
            
            // Create the replicafileinfo for each replica.
            long fileId = retrieveIdForFile(filename);
            createReplicaFileInfoEntriesInDB(fileId);
        } catch (SQLException e) {
            String msg = "Cannot add replica to the database.";
            log.warn(msg);
            throw new IOFailure(msg, e);
        }
    }
    
    /**
     * When a new file is inserted into the database, each replica gets
     * a new entry in the replicafileinfo table for this file.
     * The fields for the new entry are set to the following:
     * - file_id = argument.
     * - replica_id = each replica_id.
     * - filelist_status = NO_FILELIST_STATUS.
     * - checksum_status = UNKNOWN.
     * - upload_status = NO_UPLOAD_STATUS.
     * 
     * The replicafileinfo_guid is automatically created by the database,
     * and the dates are set to null.
     * 
     * @param fileId The id for the file.
     */
    private void createReplicaFileInfoEntriesInDB(long fileId) {
        try {
            // init variables
            List<String> repIds = retrieveIdsFromReplicaTable();

            PreparedStatement statement = null;
            dbConnection.setAutoCommit(false);

            // Make a entry for each replica.
            for (String repId : repIds) {
                // create if is does not exists already.
                if (!existsReplicaFileInfoInDB(fileId, repId)) {
                    // Only update the relevant fields.
                    statement = DBUtils.prepareStatement(
                                    dbConnection,
                                    "INSERT INTO replicafileinfo "
                                    + "(file_id, replica_id, filelist_status, "
                                    + "checksum_status, upload_status ) VALUES "
                                            + "( ?, ?, ?, ?, ? )", fileId,
                                    repId, FileListStatus.NO_FILELIST_STATUS
                                            .ordinal(), ChecksumStatus.UNKNOWN
                                            .ordinal(),
                                    UploadStatus.NO_UPLOAD_STATUS.ordinal());

                    // execute the SQL statement
                    statement.executeUpdate();
                    dbConnection.commit();
                }
            }
        } catch (SQLException e) {
            String msg = "Cannot add replicafileinfo to the database.";
            log.warn(msg);
            throw new IOFailure(msg, e);
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
        return DBUtils.selectStringList(dbConnection, sql);
    }
    
    /**
     * Method for retrieving all the file IDs within the database.
     * 
     * @return The list of fileIds from the file table in the database.
     */
    private List<String> retrieveIdsFromFileTable() {
        // Make SQL statement for retrieving the replica_ids in the
        // replica table.
        String sql = "SELECT file_id FROM file";

        // execute the SQL statement and return the results.
        return DBUtils.selectStringList(dbConnection, sql);
    }
    
    /**
     * Method for retrieving all the ReplicaFileInfo GUIDs within the database.
     * 
     * @return A list of the replicafileinfo_guid for all entries in the 
     * replicafileinfo table.
     */
    private List<String> retrieveIdsFromReplicaFileInfoTable() {
        // Make SQL statement for retrieving the replica_ids in the
        // replica table.
        String sql = "SELECT replicafileinfo_guid FROM replicafileinfo";

        // execute the SQL statement and return the results.
        return DBUtils.selectStringList(dbConnection, sql);
    }

    /**
     * Retrieves the file_id for the corresponding filename.
     * An error is thrown if no such file_if is found in the file table, and if 
     * more than one instance with the given name is found, then a warning
     * is issued.
     * 
     * @param filename The entry in the filename list where the corresponding
     * file_id should be found.
     * @return The file_id for the file, or -1 if the file was not found.
     */
    private long retrieveIdForFile(String filename) {
        // retrieve the amount of times this replica is within the database.
        String sql = "SELECT file_id FROM file WHERE filename = ?";
        List<Long> files = DBUtils.selectLongList(dbConnection, sql, filename);

        switch (files.size()) {
        // if no such file within the database, then return negative value.
        case 0:
            return -1;
        case 1:
            return files.get(0);
            // if more than one file, then log it and return the first found.
        default:
            log.warn("Only one entry in the file table with the name '"
                    + filename + "' was expected, but " + files.size()
                    + " was found.");
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
        // sql for retrieving thie replicafileinfo_guid.
        String sql = "SELECT replicafileinfo_guid FROM replicafileinfo WHERE "
                + "file_id = ? AND replica_id = ?";
        List<Long> result = DBUtils.selectLongList(dbConnection, sql, fileId,
                replicaId);

        // Handle the different cases for count.
        switch (result.size()) {
        case 0:
            return -1;
        case 1:
            return result.get(0);
        default:
            log.warn("More than one replicafileinfo with the file id '"
                    + fileId + "' from replica '" + replicaId + "': " + result);
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
    private List<Long> retrieveReplicaFileInfoGuidsForReplica(String replicaId) {
        // sql for retrieving the replicafileinfo_guid for the replica.
        String sql = "SELECT replicafileinfo_guid FROM replicafileinfo WHERE "
                + "replica_id = ?";
        return DBUtils.selectLongList(dbConnection, sql, replicaId);
    }
    
    /**
     * Method for retrieving the list of all the replicafileinfo_guids for a
     * specific file.
     * 
     * @param fileId The id for the file.
     * @return The list of all the replicafileinfo_guids.
     */
    private List<Long> retrieveReplicaFileInfoGuidsForFile(long fileId) {
        // sql for retrieving the replicafileinfo_guid for the replica.
        String sql = "SELECT replicafileinfo_guid FROM replicafileinfo WHERE "
                + "file_id = ?";
        return DBUtils.selectLongList(dbConnection, sql, fileId);
    }
    
    /**
     * Method for retrieving a list of all the file_ids in the file table.
     * 
     * @return The list of all the file_ids in the file table.
     */
    private List<Long> retrieveAllFileIds() {
        // sql for retrieving all the file_ids in the file table.
        String sql = "SELECT file_id FROM file";
        return DBUtils.selectLongList(dbConnection, sql);
    }

    /**
     * Method for retrieving the replica type for a specific replica.
     * 
     * @param replicaId The id of the replica.
     * @return The type of the replica.
     */
    private int retrieveReplicaType(String replicaId) {
        // The SQL statement for retrieving the replica status.
        String sql = "SELECT replica_type FROM replica WHERE replica_id = ?";
        return DBUtils.selectIntValue(dbConnection, sql, replicaId);
    }
    
    /**
     * Method for retrieving the list of replica, where the file with fileId
     * has the checksum_status 'OK'.
     *  
     * @param fileId The if for the file. 
     * @return The list of replicas where the status for the checksum of the 
     * file is OK.
     */
    private List<String> retrieveReplicaIdsWithOKChecksumStatus(long fileId) {
        // The SQL statement to retrieve the replica_id for the entries in the
        // replicafileinfo table for the given fileId and checksum_status = OK
        String sql = "SELECT replica_id FROM replicafileinfo WHERE "
                + "file_id = ? AND checksum_status = ?";
        return DBUtils.selectStringList(dbConnection, sql, fileId,
                ChecksumStatus.OK.ordinal());
    }
    
    /**
     * Method to retrieve the id of the replica for a given entry in the 
     * replicafileinfo table.
     * 
     * @param replicafileinfoGuid The replicafileinfo_guid for the entry in 
     * the replicafileinfo table.
     * @return The id of the replica for the given replicafileinfo entry.
     */
    private String retrieveReplicaIdFromReplicaFileInfo(long replicafileinfoGuid) {
        // The SQL statement to retrieve the replica_id for the given entry in
        // the replicafileinfo table.
        String sql = "SELECT replica_id FROM replicafileinfo WHERE "
                + "replicafileinfo_guid = ?";
        return DBUtils
                .selectStringValue(dbConnection, sql, replicafileinfoGuid);
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
        return DBUtils.selectStringValue(dbConnection, sql, fileId);
    }
    
    /**
     * Method for retrieving the file_id from a replicafileinfo based on the 
     * replicafileinfo_guid.
     * 
     * @param replicafileinfoGuid The guid for the replicafileinfo entry from
     * which the file_id should be retrieved.
     * @return The file_id for the replicafileinfo entry corresponding to the 
     * given replicafileinfo_guid.
     */
    private long retrieveFileIDFromReplicaFileInfo(long replicafileinfoGuid) {
        // The SQL statement to retrieve the file_id for the given entry in the
        // replicafileinfo table.
        String sql = "SELECT file_id FROM replicafileinfo WHERE "
                + "replicafileinfo_guid = ?";
        return DBUtils.selectLongValue(dbConnection, sql, replicafileinfoGuid);
    }
    
    /**
     * Method for retrieving the filelist_status for a given entry in the
     * replicafileinfo table.
     * 
     * @param replicafileinfoGuid The identification for the replicafileinfo
     * entry.
     * @return The filelist_status for the given entry. 
     */
    private int retrieveFileListStatusFromReplicaFileInfo(
            long replicafileinfoGuid) {
        // The SQL statement to retrieve the filelist_status for the given
        // entry in the replica fileinfo table.
        String sql = "SELECT filelist_status FROM replicafileinfo WHERE "
                + "replicafileinfo_guid = ?";
        return DBUtils.selectIntValue(dbConnection, sql, replicafileinfoGuid);

    }

    /**
     * This is used for updating a replicafileinfo instance based on the 
     * results of a checksumjob.
     * Updates the following fields for the entry in the replicafileinfo:
     * - checksum = checksum argument.
     * - filelist_status = ok.
     * - checksum_status = UNKNOWN.
     * - checksum_checkdatetime = now.
     * - filelist_checkdatetime = now.
     * 
     * @param replicafileinfoId The unique id for the replicafileinfo.
     * @param checksum The new checksum for the entry.
     */
    private void updateReplicaFileInfoChecksum(long replicafileinfoId,
            String checksum) {
        try {
            // The SQL statement
            String sql = "UPDATE replicafileinfo SET checksum = ?, "
                    + "filelist_status = ?, checksum_status = ?, "
                    + "checksum_checkdatetime = ?, filelist_checkdatetime = ? "
                    + "WHERE replicafileinfo_guid = ?";

            // init.
            PreparedStatement statement = null;
            dbConnection.setAutoCommit(false);
            Date now = new Date(Calendar.getInstance().getTimeInMillis());

            // complete the SQL statement.
            statement = DBUtils.prepareStatement(dbConnection, sql, checksum,
                    FileListStatus.OK.ordinal(), ChecksumStatus.UNKNOWN
                            .ordinal(), now, now, replicafileinfoId);

            // execute the SQL statement
            statement.executeUpdate();
            dbConnection.commit();
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
                    + "filelist_checkdatetime = ? WHERE replicafileinfo_guid = ?";

            // init.
            PreparedStatement statement = null;
            dbConnection.setAutoCommit(false);
            Date now = new Date(Calendar.getInstance().getTimeInMillis());

            // complete the SQL statement.
            statement = DBUtils.prepareStatement(dbConnection, sql,
                    FileListStatus.OK.ordinal(), now, replicafileinfoId);

            // execute the SQL statement
            statement.executeUpdate();
            dbConnection.commit();
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
    private void updateReplicaFileInfoMissingFromFilelist(long replicafileinfoId) {
        try {
            // The SQL statement
            String sql = "UPDATE replicafileinfo SET filelist_status = ?, "
                    + "filelist_checkdatetime = ? WHERE replicafileinfo_guid = ?";

            // init.
            PreparedStatement statement = null;
            dbConnection.setAutoCommit(false);
            Date now = new Date(Calendar.getInstance().getTimeInMillis());

            // complete the SQL statement.
            statement = DBUtils.prepareStatement(dbConnection, sql,
                    FileListStatus.MISSING.ordinal(), now, replicafileinfoId);

            // execute the SQL statement
            statement.executeUpdate();
            dbConnection.commit();
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
                    + "checksum_checkdatetime = ? WHERE replicafileinfo_guid = ?";

            // init.
            PreparedStatement statement = null;
            dbConnection.setAutoCommit(false);
            Date now = new Date(Calendar.getInstance().getTimeInMillis());

            // complete the SQL statement.
            statement = DBUtils.prepareStatement(dbConnection, sql,
                    ChecksumStatus.CORRUPT.ordinal(), now, replicafileinfoId);

            // execute the SQL statement
            statement.executeUpdate();
            dbConnection.commit();
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
                    + "checksum_checkdatetime = ? WHERE replicafileinfo_guid = ?";

            // init.
            PreparedStatement statement = null;
            dbConnection.setAutoCommit(false);
            Date now = new Date(Calendar.getInstance().getTimeInMillis());

            // complete the SQL statement.
            statement = DBUtils.prepareStatement(dbConnection, sql,
                    ChecksumStatus.UNKNOWN.ordinal(), now, replicafileinfoId);

            // execute the SQL statement
            statement.executeUpdate();
            dbConnection.commit();
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
     * <br/> checksum_checkdatetime = current time. 
     * 
     * The replicafileinfo is in the filelist.
     * 
     * @param replicafileinfoId The id of the replicafileinfo.
     */
    private void updateReplicaFileInfoChecksumOk(long replicafileinfoId) {
        try {
            // The SQL statement
            String sql = "UPDATE replicafileinfo SET checksum_status = ?, "
                    + "checksum_checkdatetime = ? WHERE replicafileinfo_guid = ?";

            // init.
            PreparedStatement statement = null;
            dbConnection.setAutoCommit(false);
            Date now = new Date(Calendar.getInstance().getTimeInMillis());

            // complete the SQL statement.
            statement = DBUtils.prepareStatement(dbConnection, sql,
                    ChecksumStatus.OK.ordinal(), now, replicafileinfoId);

            // execute the SQL statement
            statement.executeUpdate();
            dbConnection.commit();
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
            dbConnection.setAutoCommit(false);
            Date now = new Date(Calendar.getInstance().getTimeInMillis());
            String sql = "UPDATE replica SET checksum_updated = ? WHERE "
                    + "replica_id = ?";
            statement = DBUtils.prepareStatement(dbConnection, sql, now, rep
                    .getId());
            statement.executeUpdate();
            dbConnection.commit();
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
            dbConnection.setAutoCommit(false);
            Date now = new Date(Calendar.getInstance().getTimeInMillis());
            String sql = "UPDATE replica SET filelist_updated = ? WHERE "
                    + "replica_id = ?";
            statement = DBUtils.prepareStatement(dbConnection, sql, now, rep
                    .getId());
            statement.executeUpdate();
            dbConnection.commit();
        } catch (Exception e) {
            String msg = "Cannot update the filelist_updated for replica '"
                    + rep + "'.";
            log.warn(msg);
            throw new IOFailure(msg, e);
        }
    }
    
    /**
     * Method for updating the upload_status for a specific replicafileinfo 
     * entry.
     * This method contacts the admin data to retrieve the upload_status for 
     * the file in the replica.
     * 
     * TODO Chose when to update the upload status.
     * 
     * @param replicafileinfoGuid The identification for the replicafileinfo 
     * entry, which should have the upload_status updated.
     */
    private void updateUploadStatus(long replicafileinfoGuid) {
        // gain access to the admin data
        ReadOnlyAdminData admin = AdminData.getReadOnlyInstance();

        // Retrieve the replica id and the filename.
        String repId = retrieveReplicaIdFromReplicaFileInfo(
                replicafileinfoGuid);
        String fileName = retrieveFilenameForFileId(
                retrieveFileIDFromReplicaFileInfo(replicafileinfoGuid));

        // retrieve the upload status from the admin data.
        ReplicaStoreState uploadStatus;
        try {
            uploadStatus = admin.getState(fileName, repId);
        } catch (UnknownID e) {
            log.warn("Cannot retrieve the upload status for replicafileinfo '"
                    + replicafileinfoGuid + "' for replica '" + repId
                    + "' with file '" + fileName + "'.");
            return;
        }

        // The SQL statement for updating the upload status for the entry
        // in the replicafileinfo table.
        String sql = "UPDATE replicafileinfo SET upload_status = ? WHERE "
                + "replicafileinfo = ?";

        try {
            PreparedStatement statement = null;
            dbConnection.setAutoCommit(false);
            statement = DBUtils.prepareStatement(dbConnection, sql,
                    uploadStatus.ordinal(), replicafileinfoGuid);
            statement.executeUpdate();
            dbConnection.commit();
        } catch (SQLException e) {
            String msg = "";
            log.warn(msg, e);
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
        int count = DBUtils.selectIntValue(dbConnection, sql, rep.getId());

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
     * Checks whether a file is already in the file table in the database.
     * 
     * @param filename The name of the file in the database.
     * @return Whether the file was found in the database.
     * @throws IOFailure If more than one entry with the given filename was 
     * found.
     */
    private boolean existsFileInDB(String filename) throws IOFailure {
        // retrieve the amount of times this replica is within the database.
        String sql = "SELECT COUNT(*) FROM file WHERE filename = ?";
        int count = DBUtils.selectIntValue(dbConnection, sql, filename);

        // Handle the different cases for count.
        switch (count) {
        case 0:
            return false;
        case 1:
            return true;
        default:
            throw new IOFailure("Cannot handle " + count + " files "
                    + "with the name '" + filename + "'.");
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
        String sql = "SELECT * FROM replicafileinfo WHERE replicafileinfo_guid "
                + "= ?";

        PreparedStatement s = null;

        try {
            s = DBUtils
                    .prepareStatement(dbConnection, sql, replicaFileInfoGuid);
            ResultSet res = s.executeQuery();
            res.next();
            int guidCol = res.findColumn("replicafileinfo_guid");
            int repIdCol = res.findColumn("replica_id");
            int fileIdCol = res.findColumn("file_id");
            int segIdCol = res.findColumn("segment_id");
            int csCol = res.findColumn("checksum");
            int usCol = res.findColumn("upload_status");
            int fsCol = res.findColumn("filelist_status");
            int fDateCol = res.findColumn("filelist_checkdatetime");
            int cDateCol = res.findColumn("checksum_checkdatetime");

            return new ReplicaFileInfo(res.getLong(guidCol), res
                    .getString(repIdCol), res.getLong(fileIdCol), res
                    .getLong(segIdCol), res.getString(csCol),
                    res.getInt(usCol), res.getInt(fsCol),
                    res.getDate(fDateCol), res.getDate(cDateCol));
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
        // valid checksum into the result list.
        for (long rfiGuid : rfiGuids) {
            ReplicaFileInfo rfi = getReplicaFileInfo(rfiGuid);
            if (rfi.checksum != null && !rfi.checksum.isEmpty()) {
                result.add(rfi);
            }
        }

        return result;
    }
    
    /**
     * Method for retrieving the filelist_status for a replicafileinfo entry.
     * 
     * @param filename The name of the file.
     * @param replica The replica where the file should be.
     * @return The filelist_status for the file in the replica.
     */
    public FileListStatus retrieveFileListStatus(String filename,
            Replica replica) {
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
     * replicafileinfo entries. </br>
     * </br>
     * For each file in the database, the checksum vote is made in the 
     * following way. </br>
     * Each entry in the replicafileinfo table containing the file is retrieved.
     * All the unique checksums are retrieved, e.g. if a checksum is found more
     * than one, then it is ignored. </br>
     * If only one unique checksum is found, then if must be the correct one, 
     * and all the replicas with this file will have their checksum_status set 
     * to 'OK'. </br>
     * If more than one checksum is found, then a vote for the correct checksum
     * is performed. This is done by counting the amount of time each of the 
     * unique checksum is found among the replicafileinfo entries for the 
     * current file. The checksum with most votes is chosen as the correct one,
     * and the checksum_status for all the replicafileinfo entries with this 
     * checksum is set to 'OK', whereas the replicafileinfo entries with a 
     * different checksum is set to 'CORRUPT'. </br>  
     * If no winner is found then a warning and a notification is issued, and 
     * the checksum_status for all the replicafileinfo entries with for the 
     * current file is set to 'UNKNOWN'. </br>
     */
    public void updateChecksumStatus() {
        // Get all the fileids
        List<Long> fileIds = retrieveAllFileIds();

        // For each fileid
        for (long fileId : fileIds) {
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

                continue;
            }

            // Put all the checksums into a hash set to obtain a set of
            // unique checksums.
            Set<String> hs = new HashSet<String>(rfis.size());
            for (ReplicaFileInfo rfi : rfis) {
                hs.add(rfi.checksum);
            }

            // if at most one unique checksum is found, then no irregularities
            // among the checksums are found.
            if (hs.size() <= 1) {
                log.trace("No irregularities found for the file with id '"
                        + fileId + "'.");

                // Tell all the replicafileinfo entries that their checksum
                // is ok
                for (ReplicaFileInfo rfi : rfis) {
                    updateReplicaFileInfoChecksumOk(rfi.guid);
                }

                // go to next entry in the file table.
                continue;
            }

            // else count the amount of times each checksum is found.
            int[] csCount = new int[hs.size()];
            String[] uniqueCs = hs.toArray(new String[0]);
            for (ReplicaFileInfo rfi : rfis) {
                for (int i = 0; i < hs.size(); i++) {
                    if (rfi.checksum.equals(uniqueCs[i])) {
                        csCount[i]++;
                    }
                }
            }

            // find the one with the largest unique amount of checksums.
            // save the index and whether it is unique largest amount.
            int largest = 0;
            boolean unique = false;
            int index = -1;
            for (int i = 0; i < csCount.length; i++) {
                if (csCount[i] > largest) {
                    // new largest found, set relevant variables.
                    largest = csCount[i];
                    unique = true;
                    index = i;
                } else if (csCount[i] == largest) {
                    // If they have the same value then add to indices.
                    unique = false;
                }
            }

            if (unique) {
                // change checksum_status to CORRUPT for the replicafileinfo
                // which
                // does not have the chosen checksum.
                // Set the others replciafileinfo entries to OK.
                for (ReplicaFileInfo rfi : rfis) {
                    if (!rfi.checksum.equals(uniqueCs[index])) {
                        updateReplicaFileInfoChecksumCorrupt(rfi.guid);
                    } else {
                        updateReplicaFileInfoChecksumOk(rfi.guid);
                    }
                }
            } else {
                // Handle the case, when no checksum has most votes.
                String errMsg = "There is no winner of the votes between "
                        + "the replicas for the checksum of file '"
                        + retrieveFilenameForFileId(fileId) + "'.";
                // if it is not unique, then set all to corrupt.
                log.error(errMsg);

                // send a notification
                NotificationsFactory.getInstance().errorEvent(errMsg);

                // set all replicafileinfo entries to unknown
                for (ReplicaFileInfo rfi : rfis) {
                    updateReplicaFileInfoChecksumUnknown(rfi.guid);
                }
            }
        }
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
    public void addChecksumInformation(List<ChecksumEntry> checksumOutput,
            Replica replica) {
        // validate arguments
        ArgumentNotValid.checkNotNull(checksumOutput,
                "List<ChecksumEntry> checksumjobOutput");
        ArgumentNotValid.checkNotNull(replica, "Replica replica");

        // Make sure, that the replica exists in the database.
        if (!existsReplicaInDB(replica)) {
            String msg = "Cannot add checksum information, since the replica '"
                    + replica.toString()
                    + "' does not exist within the database.";
            log.warn(msg);
            throw new IOFailure(msg);
        }

        for (ChecksumEntry entry : checksumOutput) {
            // parse the input.
            String filename = entry.getFilename();
            String checksum = entry.getChecksum();

            // If the file is not within DB, then insert it.
            if (!existsFileInDB(filename)) {
                log.info("Inserting the file '" + filename + "' into the "
                        + "database.");
                insertFileIntoDB(filename);
            }

            // Retrieve the ID for the file.
            long fileid = retrieveIdForFile(filename);

            // If the file does not already exists in the database, create it
            // and retrieve the new ID.
            if (fileid < 0) {
                insertFileIntoDB(filename);
                fileid = retrieveIdForFile(filename);
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
        }

        // update the checksum updated date for this replica.
        updateChecksumDateForReplica(replica);
        updateFilelistDateForReplica(replica);
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
     */
    @Override
    public void addFileListInformation(List<String> filelist, Replica replica) {
        ArgumentNotValid.checkNotNull(filelist, "List<String> filelist");

        // retrieve the list of files already known by this cache.
        List<Long> missingReplicaRFIs = retrieveReplicaFileInfoGuidsForReplica(
                replica.getId());

        for (String file : filelist) {
            // retrieve the file_id for the file.
            long fileId = retrieveIdForFile(file);
            // If not found, log and create the file in the database.
            if (fileId < 0) {
                log.info("The file '" + file + "' was not found in the "
                        + "database. Thus creating entry for the file.");
                // insert the file and retrieve its file_id.
                insertFileIntoDB(file);
                fileId = retrieveIdForFile(file);
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

    @Override
    public Date getDateOfLastMissingFilesUpdate(Replica replica) {
        // sql for retrieving thie replicafileinfo_guid.
        String sql = "SELECT filelist_updated FROM replica WHERE "
                + "replica_id = ?";
        String result = DBUtils.selectStringValue(dbConnection, sql, replica
                .getId());

        // return null if the field has no be set for this replica.
        if (result == null) {
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
     * @return The date for the last checksum update.
     */
    @Override
    public Date getDateOfLastWrongFilesUpdate(Replica replica) {
        // The SQL statement for retrieving the date for last updating of
        // checksum for the replica.
        String sql = "SELECT checksum_updated FROM replica WHERE "
                + "replica_id = ?";
        String result = DBUtils.selectStringValue(dbConnection, sql, replica
                .getId());

        // return null if the field has no be set for this replica.
        if (result == null) {
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
     */
    @Override
    public long getNumberOfMissingFilesInLastUpdate(Replica replica) {
        // The SQL statement to retrieve the number of entries in the
        // replicafileinfo table with file_status set to missing for the
        // replica.
        String sql = "SELECT COUNT(*) FROM replicafileinfo WHERE replica_id"
                + " = ? AND filelist_status = ?";
        return DBUtils.selectLongValue(dbConnection, sql, replica.getId(),
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
     */
    @Override
    public Iterable<String> getMissingFilesInLastUpdate(Replica replica) {
        // The SQL statement to retrieve the filenames of the missing
        // replicafileinfo to the given replica.
        String sql = "SELECT filename FROM replicafileinfo LEFT OUTER JOIN "
                + "file ON replicafileinfo.file_id = file.file_id WHERE replica_id"
                + " = ? AND filelist_status = ?";
        return DBUtils.selectStringList(dbConnection, sql, replica.getId(),
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
     */
    @Override
    public long getNumberOfWrongFilesInLastUpdate(Replica replica) {
        // The SQL statement to retrieve the number of corrupted entries in
        // the replicafileinfo table for the given replica.
        String sql = "SELECT COUNT(*) FROM replicafileinfo WHERE replica_id"
                + " = ? AND checksum_status = ?";
        return DBUtils.selectLongValue(dbConnection, sql, replica.getId(),
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
     */
    @Override
    public Iterable<String> getWrongFilesInLastUpdate(Replica replica) {
        // The SQL statement to retrieve the filenames for the corrupted files
        // in the replicafileinfo table for the given replica.
        String sql = "SELECT filename FROM replicafileinfo LEFT OUTER JOIN "
                + "file ON replicafileinfo.file_id = file.file_id WHERE replica_id"
                + " = ? AND checksum_status = ?";
        return DBUtils.selectStringList(dbConnection, sql, replica.getId(),
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
     */
    @Override
    public long getNumberOfFiles(Replica replica) {
        // The SQL statement to retrieve the amount of entries in the
        // replicafileinfo table for the replica which does not have the
        // filelist_status set to MISSING.
        String sql = "SELECT COUNT(*) FROM replicafileinfo WHERE replica_id "
                + " = ? AND filelist_status = ?";
        return DBUtils.selectLongValue(dbConnection, sql, replica.getId(),
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
     */
    @Override
    public Replica getBitarchiveWithGoodFile(String filename) {
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

        log.warn("No bitarchive replica was found which contains the file '"
                + filename + "'.");

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
     * @return A bitarchive which contains a valid version of the file, or null
     * if no such bitarchive exists.
     */
    @Override
    public Replica getBitarchiveWithGoodFile(String filename, Replica badReplica) {
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

        log.warn("No bitarchive replica was found which contains the file '"
                + filename + "'.");

        // If not bitarchive containing the file with a OK checksum_status.
        // then return null.
        return null;
    }

    /**
     * Method to print all the tables in the database.
     * FIXME This is only used during implementation. Kill me afterwards!
     */
    public void print() {
        String sql = "";

        // Go through the replica table
        List<String> reps = retrieveIdsFromReplicaTable();
        System.out.println("Replica table: " + reps.size());
        System.out.println("GUID \trepId \trepName \trepType \tfileupdate "
                + "\tchecksumupdated");
        System.out.println("------------------------------------------------"
                + "------------");
        for (String repId : reps) {
            // retrieve the replica_name
            sql = "SELECT replica_guid FROM replica WHERE replica_id = ?";
            String repGUID = DBUtils
                    .selectStringValue(dbConnection, sql, repId);
            // retrieve the replica_name
            sql = "SELECT replica_name FROM replica WHERE replica_id = ?";
            String repName = DBUtils
                    .selectStringValue(dbConnection, sql, repId);
            // retrieve the replica_type
            sql = "SELECT replica_type FROM replica WHERE replica_id = ?";
            int repType = DBUtils.selectIntValue(dbConnection, sql, repId);
            // retrieve the date for last updated
            sql = "SELECT filelist_updated FROM replica WHERE replica_id = ?";
            String filelistUpdated = DBUtils.selectStringValue(dbConnection,
                    sql, repId);
            // retrieve the date for last updated
            sql = "SELECT checksum_updated FROM replica WHERE replica_id = ?";
            String checksumUpdated = DBUtils.selectStringValue(dbConnection,
                    sql, repId);

            // Print
            System.out.println(repGUID + "\t" + repId + "\t" + repName + "\t"
                    + ReplicaType.fromOrdinal(repType).name() + "\t"
                    + filelistUpdated + "\t" + checksumUpdated);
        }
        System.out.println();

        // Go through the file table
        List<String> fileIds = retrieveIdsFromFileTable();
        System.out.println("File table : " + fileIds.size());
        System.out.println("fileId \tfilename");
        System.out.println("--------------------");
        for (String fileId : fileIds) {
            // retrieve the file_name
            sql = "SELECT filename FROM file WHERE file_id = ?";
            String fileName = DBUtils.selectStringValue(dbConnection, sql,
                    fileId);

            // Print
            System.out.println(fileId + " \t " + fileName);
        }
        System.out.println();

        // Go through the replicafileinfo table
        List<String> rfiIds = retrieveIdsFromReplicaFileInfoTable();
        System.out.println("ReplicaFileInfo table : " + rfiIds.size());
        System.out.println("GUID \trepId \tfileId \tchecksum \t"
                + "us \t\tfls \tcss \tfilelistCheckdate \t"
                + "checksumCheckdate");
        System.out.println("------------------------------------------------"
                + "---------------------------------------------------------");
        for (String rfiGUID : rfiIds) {
            // retrieve the replica_id
            sql = "SELECT replica_id FROM replicafileinfo WHERE "
                    + "replicafileinfo_guid = ?";
            String replicaId = DBUtils.selectStringValue(dbConnection, sql,
                    rfiGUID);
            // retrieve the file_id
            sql = "SELECT file_id FROM replicafileinfo WHERE "
                    + "replicafileinfo_guid = ?";
            String fileId = DBUtils.selectStringValue(dbConnection, sql,
                    rfiGUID);
            // retrieve the checksum
            sql = "SELECT checksum FROM replicafileinfo WHERE "
                    + "replicafileinfo_guid = ?";
            String checksum = DBUtils.selectStringValue(dbConnection, sql,
                    rfiGUID);
            // retrieve the upload_status
            sql = "SELECT upload_status FROM replicafileinfo WHERE "
                    + "replicafileinfo_guid = ?";
            int uploadStatus = DBUtils.selectIntValue(dbConnection, sql,
                    rfiGUID);
            // retrieve the filelist_status
            sql = "SELECT filelist_status FROM replicafileinfo WHERE "
                    + "replicafileinfo_guid = ?";
            int filelistStatus = DBUtils.selectIntValue(dbConnection, sql,
                    rfiGUID);
            // retrieve the checksum_status
            sql = "SELECT checksum_status FROM replicafileinfo WHERE "
                    + "replicafileinfo_guid = ?";
            int checksumStatus = DBUtils.selectIntValue(dbConnection, sql,
                    rfiGUID);
            // retrieve the filelist_checkdatetime
            sql = "SELECT filelist_checkdatetime FROM replicafileinfo WHERE "
                    + "replicafileinfo_guid = ?";
            String filelistCheckdatetime = DBUtils.selectStringValue(
                    dbConnection, sql, rfiGUID);
            // retrieve the checksum_checkdatetime
            sql = "SELECT checksum_checkdatetime FROM replicafileinfo WHERE "
                    + "replicafileinfo_guid = ?";
            String checksumCheckdatetime = DBUtils.selectStringValue(
                    dbConnection, sql, rfiGUID);

            // Print
            System.out.println(rfiGUID + " \t" + replicaId + "\t" + fileId
                    + "\t" + checksum + "\t"
                    + UploadStatus.fromOrdinal(uploadStatus).name() + "  \t"
                    + FileListStatus.fromOrdinal(filelistStatus).name() + "\t"
                    + ChecksumStatus.fromOrdinal(checksumStatus).name() + "\t"
                    + filelistCheckdatetime + "\t" + checksumCheckdatetime);
        }
        System.out.println();
    }

    @Override
    public void cleanup() {
        instance = null;
    }
}
