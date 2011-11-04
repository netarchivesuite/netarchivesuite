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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.common.utils.batch.ChecksumJob;
import dk.netarkivet.common.distribute.Channels;
import dk.netarkivet.common.distribute.arcrepository.Replica;
import dk.netarkivet.common.distribute.arcrepository.ReplicaStoreState;
import dk.netarkivet.common.distribute.arcrepository.ReplicaType;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.IllegalState;
import dk.netarkivet.common.exceptions.UnknownID;
import dk.netarkivet.common.utils.DBUtils;
import dk.netarkivet.common.utils.ExceptionUtils;
import dk.netarkivet.common.utils.KeyValuePair;
import dk.netarkivet.common.utils.NotificationsFactory;
import dk.netarkivet.common.utils.StringUtils;

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
    
    /** The database connector handling the database connections. */
    private ReplicaCacheDatabaseConnector dbcon;

    /** The number of entries between logging in either file list or checksum
     * list. */
    private final int LOGGING_ENTRY_INTERVAL = 1000;

    /**
     * Constructor.
     */
    private ReplicaCacheDatabase() {
        // Get a connection to the archive database
        dbcon = ReplicaCacheDatabaseConnector.getInstance();
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
        Connection con = dbcon.getDbConnection();
        // Retrieve the replica IDs from the database.
        List<String> repIds 
            = ReplicaCacheHelpers.retrieveIdsFromReplicaTable(con);
        log.debug("IDs for replicas already in the database: " 
            + StringUtils.conjoin(",", repIds));
        for (Replica rep : replicas) {
            // try removing the id from the temporary list of IDs within the DB.
            // If the remove is not successful, then the replica is already
            // in the database.
            if (!repIds.remove(rep.getId())) {
                // if the replica id cannot be removed from the list, then it
                // does not exist in the database and must be added.
                log.info("Inserting replica '" + rep.toString()
                        + "' in database.");
                ReplicaCacheHelpers.insertReplicaIntoDB(rep, con);
            } else {
                // Otherwise it already exists in the DB.
                log.debug("Replica '" + rep.toString()
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

        // retrieve all
        String sql = "SELECT replicafileinfo_guid, replica_id, "
            + "replicafileinfo.file_id, "
            + "segment_id, checksum, upload_status, filelist_status, "
            + "checksum_status, filelist_checkdatetime, checksum_checkdatetime "
            + "FROM replicafileinfo, file "
            + " WHERE file.file_id = replicafileinfo.file_id"  
            + " AND file.filename=? AND replica_id=?";

        PreparedStatement s = null;

        try {
            s = DBUtils.prepareStatement(dbcon.getDbConnection(), sql,
                    filename, replica.getId());
            ResultSet res = s.executeQuery();
            if (res.next()) {
                // return the corresponding replica file info.
                return new ReplicaFileInfo(res);
            } else {
                return null;
            }            
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
  
        Connection con = dbcon.getDbConnection();
        
        // retrieve the fileId
        long fileId = ReplicaCacheHelpers.retrieveIdForFile(filename, con);

        // Check if a checksum with status OK for the file can be found in
        // the database
        for(Replica rep : Replica.getKnown()) {
            // Return the checksum, if it has a valid status.
            if(ReplicaCacheHelpers.retrieveChecksumStatusForReplicaFileInfoEntry(fileId,
                    rep.getId(), con) == ChecksumStatus.OK) {
                return ReplicaCacheHelpers.retrieveChecksumForReplicaFileInfoEntry(fileId,
                        rep.getId(), con);
            }
        }

        // log that we vote about the file.
        log.debug("No commonly accepted checksum for the file '" + filename
                + "' has previously been found. Voting to achieve one.");

        // retrieves all the UNKNOWN_STATE checksums, and return if unanimous.
        Set<String> checksums = new HashSet<String>();

        for(Replica rep : Replica.getKnown()) {
            if(ReplicaCacheHelpers.retrieveChecksumStatusForReplicaFileInfoEntry(fileId,
                    rep.getId(), con) != ChecksumStatus.CORRUPT) {
                String tmpChecksum 
                    = ReplicaCacheHelpers.retrieveChecksumForReplicaFileInfoEntry(
                        fileId, rep.getId(), con);
                if(tmpChecksum != null) {
                    checksums.add(tmpChecksum);
                } else {
                    log.info("Replica '" + rep.getId() + "' has a null "
                            + "checksum for the file '"
                            + ReplicaCacheHelpers.retrieveFilenameForFileId(
                                    fileId, con) + "'.");
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
            String cs = ReplicaCacheHelpers.retrieveChecksumForReplicaFileInfoEntry(fileId,
                    rep.getId(), con);

            if(cs != null) {
                checksumList.add(cs);
            } else {
                // log when it is second time we find this checksum to be null?
                log.debug("Replica '" + rep.getId() + "' has a null "
                        + "checksum for the file '"
                        + ReplicaCacheHelpers.retrieveFilenameForFileId(
                                fileId, con) + "'.");
            }
        }

        // vote and return the most occurred checksum.
        return ReplicaCacheHelpers.vote(checksumList);
    }

    /**
     * Retrieves the names of all the files in the file table of the database.
     *
     * @return The list of filenames known by the database.
     */
    public Collection<String> retrieveAllFilenames() {
        // make sql query.
        final String sql = "SELECT filename FROM file";

        // Perform the select.
        return DBUtils.selectStringList(dbcon.getDbConnection(), 
                sql, new Object[]{});
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

        Connection con = dbcon.getDbConnection();
        
        // Make query for extracting the upload status.
        String sql = "SELECT upload_status FROM replicafileinfo, file WHERE "
            + "replicafileinfo.file_id = file.file_id AND file.filename = ? "
            + "AND replica_id = ?";
        
        // execute the query.
        int ordinal = DBUtils.selectIntValue(con, sql, filename, replicaId);

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

        Connection con = dbcon.getDbConnection();
        
        // retrieve the guid for the file.
        long fileId = ReplicaCacheHelpers.retrieveIdForFile(filename, con);

        try {
            // init statement.
            PreparedStatement statement = null;

            // Make query for updating the upload status
            if(state == ReplicaStoreState.UPLOAD_COMPLETED) {
                // An UPLOAD_COMPLETE
                // UPLOAD_COMPLETE => filelist_status = OK, checksum_status = OK
                String sql = "UPDATE replicafileinfo SET upload_status = ?, "
                    + "filelist_status = ?, checksum_status = ? "
                    + "WHERE replica_id = ? AND file_id = ?";
                statement = DBUtils.prepareStatement(con, sql,
                        state.ordinal(), FileListStatus.OK.ordinal(),
                        ChecksumStatus.OK.ordinal(), replicaId, fileId);
            } else {
                String sql = "UPDATE replicafileinfo SET upload_status = ? "
                    + "WHERE replica_id = ? AND file_id = ?";
                statement = DBUtils.prepareStatement(con, sql,
                        state.ordinal(), replicaId, fileId);
            }

            // execute the update and commit to database.
            statement.executeUpdate();
            con.commit();
        } catch (SQLException e) {
            String errMsg = "Received the following SQL error while updating "
                + " the database: " + ExceptionUtils.getSQLExceptionCause(e);
            log.warn(errMsg, e);
            throw new IOFailure(errMsg, e);
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
        
        Connection con = dbcon.getDbConnection();
        
        // retrieve the fileId for the filename.
        long fileId;

        // insert into DB, or make sure that it can be inserted.
        if(existsFileInDB(filename)) {
            // retrieve the fileId of the existing file.
            fileId = ReplicaCacheHelpers.retrieveIdForFile(filename, con);

            // Check the entries for this file associated with the replicas.
            for(Replica rep : Replica.getKnown()) {
                // Ensure that the file has not been completely uploaded to a
                // replica.
                ReplicaStoreState us = ReplicaCacheHelpers.retrieveUploadStatus(
                        fileId, rep.getId(), con);

                if(us.equals(ReplicaStoreState.UPLOAD_COMPLETED)) {
                    throw new IllegalState("The file has already been "
                            + "completely uploaded to the replica: " + rep);
                }

                // make sure that it has not been attempted uploaded with
                // another checksum
                String entryCs = ReplicaCacheHelpers.retrieveChecksumForReplicaFileInfoEntry(
                        fileId, rep.getId(), con);

                // throw an exception if the registered checksum differs.
                if(entryCs != null && !checksum.equals(entryCs)) {
                    throw new IllegalState("The file '" + filename + "' with "
                            + "checksum '" + entryCs + "' has attempted being "
                            + "uploaded with the checksum '" + checksum + "'");
                }
            }
        } else {
            fileId = ReplicaCacheHelpers.insertFileIntoDB(filename, con);
        }

        for(Replica rep : Replica.getKnown()) {
            // retrieve the guid for the corresponding replicafileinfo entry
            long guid = ReplicaCacheHelpers.retrieveReplicaFileInfoGuid(
                    fileId, rep.getId(), con);

            // Update with the correct information.
            ReplicaCacheHelpers.updateReplicaFileInfo(guid, checksum,
                    ReplicaStoreState.UNKNOWN_UPLOAD_STATE, con);
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
           
        PreparedStatement statement = null;
        try {
            Connection connection = dbcon.getDbConnection();
            // retrieve the replicafileinfo_guid for this filename .
            long guid = ReplicaCacheHelpers.retrieveGuidForFilenameOnReplica(
                    filename, replica.getId(), connection);
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
        } finally {
            DBUtils.closeStatementIfOpen(statement);
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
       
        PreparedStatement statement = null; 
        try {
            Connection connection = dbcon.getDbConnection();
            // retrieve the replicafileinfo_guid for this filename .
            long guid = ReplicaCacheHelpers.retrieveGuidForFilenameOnReplica(
                    filename, replica.getId(), connection);

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
        } finally {
            DBUtils.closeStatementIfOpen(statement);
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
        return DBUtils.selectStringList(dbcon.getDbConnection(), sql, replicaId,
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
        int count = DBUtils.selectIntValue(
                dbcon.getDbConnection(), sql, filename);

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

        Connection con = dbcon.getDbConnection();
        
        // retrieve the filelist_status for the entry.
        int status = ReplicaCacheHelpers.retrieveFileListStatusFromReplicaFileInfo(
                filename, replica.getId(), con);

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
        Connection con = dbcon.getDbConnection();
        // Get all the fileids
        List<Long> fileIds = DBUtils.selectLongList(con,
                "SELECT file_id FROM file");

        // For each fileid
        for (long fileId : fileIds) {
            ReplicaCacheHelpers.fileChecksumVote(fileId, con);
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
        
        Connection con = dbcon.getDbConnection();
        // retrieve the id and vote!
        Long fileId = ReplicaCacheHelpers.retrieveIdForFile(filename, con);
        ReplicaCacheHelpers.fileChecksumVote(fileId, con);
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

        Connection con = dbcon.getDbConnection();
        
        // Make sure, that the replica exists in the database.
        if (!ReplicaCacheHelpers.existsReplicaInDB(replica, con)) {
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
        List<Long> missingReplicaRFIs = ReplicaCacheHelpers.retrieveReplicaFileInfoGuidsForReplica(
                replica.getId(), con);

        String lastFilename = "";
        String lastChecksum = "";

        int i = 0;
        for (String line : checksumOutput) {
            // log that it is in progress every so often.
            if((i % LOGGING_ENTRY_INTERVAL) == 0) {
                log.info("Processed checksum list entry number " + i
                        + " for replica " + replica);
            }
            i++;

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
                fileid = ReplicaCacheHelpers.insertFileIntoDB(filename, con);
            } else {
                fileid = ReplicaCacheHelpers.retrieveIdForFile(filename, con);
            }

            // If the file does not already exists in the database, create it
            // and retrieve the new ID.
            if (fileid < 0) {
                log.warn("Inserting the file '" + filename + "' into the "
                        + "database, again: This should never happen!!!");
                fileid = ReplicaCacheHelpers.insertFileIntoDB(filename, con);
            }

            // Retrieve the replicafileinfo for the file at the replica.
            long rfiId = ReplicaCacheHelpers.retrieveReplicaFileInfoGuid(
                    fileid, replica.getId(), con);

            // Check if there already is an entry in the replicafileinfo table.
            // rfiId is negative if no entry was found.
            if (rfiId < 0) {
                // insert the file into the table.
                ReplicaCacheHelpers.createReplicaFileInfoEntriesInDB(
                        fileid, con);
                log.info("Inserted file '" + filename + "' for replica '"
                        + replica.toString() + "' into replicafileinfo.");
            }

            // Update this table
            ReplicaCacheHelpers.updateReplicaFileInfoChecksum(
                    rfiId, checksum, con);
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
                ReplicaCacheHelpers.updateReplicaFileInfoMissingFromFilelist(
                        rfi, con);
            }
        }

        // update the checksum updated date for this replica.
        ReplicaCacheHelpers.updateChecksumDateForReplica(replica, con);
        ReplicaCacheHelpers.updateFilelistDateForReplica(replica, con);

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
     * @throws UnknownID If the replica does not already exist in the database.
     */
    @Override
    public void addFileListInformation(List<String> filelist, Replica replica)
            throws ArgumentNotValid, UnknownID {
        ArgumentNotValid.checkNotNull(filelist, "List<String> filelist");
        ArgumentNotValid.checkNotNull(replica, "Replica replica");

        Connection con = dbcon.getDbConnection();
        
        // Make sure, that the replica exists in the database.
        if (!ReplicaCacheHelpers.existsReplicaInDB(replica, con)) {
            String errorMsg = "Cannot add filelist information, since "
                + "the replica '" + replica.toString()
                    + "' does not exist in the database.";
            log.warn(errorMsg);
            throw new UnknownID(errorMsg);
        }

        log.info("Starting processing of " + filelist.size()
                + " filelist entries for replica " + replica.getId());

        // retrieve the list of files already known by this cache.
        List<Long> missingReplicaRFIs = ReplicaCacheHelpers.retrieveReplicaFileInfoGuidsForReplica(
                replica.getId(), con);

        Collections.sort(filelist);

        String lastFileName = "";
        int i = 0;
        for (String file : filelist) {
            // log that it is in progress every so often.
            if((i % LOGGING_ENTRY_INTERVAL) == 0) {
                log.info("Processed file list entry number " + i
                        + " for replica " + replica);
            }
            i++;

            // handle duplicates.
            if(file.equals(lastFileName)) {
                log.warn("There have been found multiple files with the name '"
                        + file + "'");
                continue;
            }
            lastFileName = file;

            // retrieve the file_id for the file.
            long fileId = ReplicaCacheHelpers.retrieveIdForFile(file, con);
            // If not found, log and create the file in the database.
            if (fileId < 0) {
                log.info("The file '" + file + "' was not found in the "
                        + "database. Thus creating entry for the file.");
                // insert the file and retrieve its file_id.
                fileId = ReplicaCacheHelpers.insertFileIntoDB(file, con);
            }

            // retrieve the replicafileinfo_guid for this entry.
            long rfiId = ReplicaCacheHelpers.retrieveReplicaFileInfoGuid(
                    fileId, replica.getId(), con);
            // if not found log and create the replicafileinfo in the database.
            if (rfiId < 0) {
                log.warn("Cannot find the file '" + file + "' for "
                        + "replica '" + replica.getId() + "'. Thus creating "
                        + "missing entry before updating.");
                ReplicaCacheHelpers.createReplicaFileInfoEntriesInDB(fileId, con);
            }

            // remove from replicaRFIs, since it has been found
            missingReplicaRFIs.remove(rfiId);

            // update the replicafileinfo of this file:
            // filelist_checkdate, filelist_status, upload_status
            ReplicaCacheHelpers.updateReplicaFileInfoFilelist(rfiId, con);
        }

        // go through the not found replicafileinfo for this replica to change
        // their filelist_status to missing.
        if(missingReplicaRFIs.size() > 0) {
            log.warn("Found " + missingReplicaRFIs.size() + " missing files "
                    + "for replica '" + replica + "'.");
            for (long rfi : missingReplicaRFIs) {
                // set the replicafileinfo in the database to missing.
                ReplicaCacheHelpers.updateReplicaFileInfoMissingFromFilelist(rfi, con);
            }
        }

        // Update the date for filelist update for this replica.
        ReplicaCacheHelpers.updateFilelistDateForReplica(replica, con);
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
        String result = DBUtils.selectStringValue(dbcon.getDbConnection(), sql,
                replica.getId());

        // return null if the field has no be set for this replica.
        if (result == null) {
            log.debug("The 'filelist_updated' field has not been set, "
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
        String result = DBUtils.selectStringValue(dbcon.getDbConnection(), sql,
                replica.getId());

        // return null if the field has no be set for this replica.
        if (result == null) {
            log.debug("The 'checksum_updated' field has not been set, "
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
        // replicafileinfo table with file_status set to either missing or
        // no_status for the replica.
        String sql = "SELECT COUNT(*) FROM replicafileinfo WHERE replica_id"
                + " = ? AND ( filelist_status = ? OR filelist_status = ?)";
        return DBUtils.selectLongValue(dbcon.getDbConnection(), sql, 
                replica.getId(), FileListStatus.MISSING.ordinal(),
                FileListStatus.NO_FILELIST_STATUS.ordinal());
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
                + "WHERE replica_id = ? AND ( filelist_status = ? "
                + "OR filelist_status = ? )";
        return DBUtils.selectStringList(dbcon.getDbConnection(), sql, 
                replica.getId(), FileListStatus.MISSING.ordinal(),
                FileListStatus.NO_FILELIST_STATUS.ordinal());
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
        return DBUtils.selectLongValue(dbcon.getDbConnection(), sql, 
                replica.getId(), ChecksumStatus.CORRUPT.ordinal());
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
        return DBUtils.selectStringList(dbcon.getDbConnection(), 
                sql, replica.getId(),
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
        return DBUtils.selectLongValue(dbcon.getDbConnection(), sql, 
                replica.getId(), FileListStatus.OK.ordinal());
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
     
        Connection con = dbcon.getDbConnection();
        
        // Retrieve a list of replicas where the the checksum status is OK
        List<String> replicaIds = ReplicaCacheHelpers.retrieveReplicaIdsWithOKChecksumStatus(
                filename, con);

        // go through the list, and return the first valid bitarchive-replica.
        for (String repId : replicaIds) {
            // Retrieve the replica type.
            ReplicaType repType = ReplicaCacheHelpers.retrieveReplicaType(repId, con);

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

        Connection con = dbcon.getDbConnection();
        
        // Then retrieve a list of replicas where the the checksum status is OK
        List<String> replicaIds = ReplicaCacheHelpers.retrieveReplicaIdsWithOKChecksumStatus(
                filename, con);

        // Make sure, that the bad replica is not returned.
        replicaIds.remove(badReplica.getId());

        // go through the list, and return the first valid bitarchive-replica.
        for (String repId : replicaIds) {
            // Retrieve the replica type.
            ReplicaType repType = ReplicaCacheHelpers.retrieveReplicaType(repId, con);

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
            Connection connection = dbcon.getDbConnection();
            
            long guid = ReplicaCacheHelpers.retrieveGuidForFilenameOnReplica(
                    filename, replica.getId(), connection); 

            Date now = new Date(Calendar.getInstance().getTimeInMillis());

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
        
        Connection con = dbcon.getDbConnection();
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

            long fileId = ReplicaCacheHelpers.retrieveIdForFile(filename, con);

            // If the fileId is -1, then the file is not within the file table.
            // Thus insert it and retrieve the id.
            if(fileId == -1) {
                fileId = ReplicaCacheHelpers.insertFileIntoDB(filename, con);
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
                long guid = ReplicaCacheHelpers.retrieveReplicaFileInfoGuid(
                        fileId, replicaId, con);

                // Update the replicaFileInfo with the information.
                ReplicaCacheHelpers.updateReplicaFileInfo(
                        guid, checksum, replicaDate, replicaUploadStatus, con);
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

        Connection con = dbcon.getDbConnection();
        
        // set the date for the replicas.
        for(Replica rep : Replica.getKnown()) {
            ReplicaCacheHelpers.setFilelistDateForReplica(rep, date, con);
            ReplicaCacheHelpers.setChecksumlistDateForReplica(rep, date, con);
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
        return DBUtils.selectLongValue(dbcon.getDbConnection(), sql,
                new Object[0]) == 0L;
    }

    /**
     * Method to print all the tables in the database.
     * @return all the tables as a text string
     */
    public String retrieveAsText() {
        StringBuilder res = new StringBuilder();
        String sql = "";
        Connection connection = dbcon.getDbConnection();
        // Go through the replica table
        List<String> reps = ReplicaCacheHelpers.retrieveIdsFromReplicaTable(
                connection);
        res.append("Replica table: " + reps.size() + "\n");
        res.append("GUID \trepId \trepName \trepType \tfileupdate "
                + "\tchecksumupdated" + "\n");
        res.append("------------------------------------------------"
                + "------------\n");
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
        List<String> fileIds = ReplicaCacheHelpers.retrieveIdsFromFileTable(
                connection);
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
        List<String> rfiIds = ReplicaCacheHelpers.retrieveIdsFromReplicaFileInfoTable(connection);
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
}
