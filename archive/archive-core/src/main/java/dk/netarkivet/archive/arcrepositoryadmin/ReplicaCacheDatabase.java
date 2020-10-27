/*
 * #%L
 * Netarchivesuite - archive
 * %%
 * Copyright (C) 2005 - 2018 The Royal Danish Library, 
 *             the National Library of France and the Austrian National Library.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 2.1 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */
package dk.netarkivet.archive.arcrepositoryadmin;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
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
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.apache.commons.io.LineIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.KeyValuePair;
import dk.netarkivet.common.utils.NotificationType;
import dk.netarkivet.common.utils.NotificationsFactory;
import dk.netarkivet.common.utils.StringUtils;
import dk.netarkivet.common.utils.TimeUtils;
import dk.netarkivet.common.utils.batch.ChecksumJob;

/**
 * Method for storing the bitpreservation cache in a database.
 * <p>
 * This method uses the 'admin.data' file for retrieving the upload status.
 */
public final class ReplicaCacheDatabase implements BitPreservationDAO {

    /** The log. */
    protected static final Logger log = LoggerFactory.getLogger(ReplicaCacheDatabase.class);

    /** The current instance. */
    private static ReplicaCacheDatabase instance;

    /**
     * The number of entries between logging in either file list or checksum list. This also controls how often the
     * database connection is renewed in methods {@link #addChecksumInformation(File, Replica)} and
     * {@link #addFileListInformation(File, Replica)}, where the operations can take hours, and seems to leak memory.
     */
    private final int LOGGING_ENTRY_INTERVAL = 1000;

    /** Waiting time in seconds before attempting to initialise the database again. */
    private final int WAIT_BEFORE_INIT_RETRY = 30;

    /** Number of DB INIT retries. */
    private final int INIT_DB_RETRIES = 3;

    /**
     * Constructor. throws IllegalState if unable to initialize the database.
     */
    private ReplicaCacheDatabase() {
        // Get a connection to the archive database
        Connection con = ArchiveDBConnection.get();
        try {
            int retries = 0;
            boolean initialized = false;
            while (retries < INIT_DB_RETRIES && !initialized) {
                retries++;
                try {
                    initialiseDB(con);
                    initialized = true;
                    log.info("Initialization of database successful");
                    return;
                } catch (IOFailure e) {
                    if (retries < INIT_DB_RETRIES) {
                        log.info("Initialization failed. Probably because another application is calling the same "
                                + "method now. Retrying after a minimum of {} seconds: ", WAIT_BEFORE_INIT_RETRY, e);
                        waitSome();
                    } else {
                        throw new IllegalState("Unable to initialize the database.");
                    }
                }
            }
        } finally {
            ArchiveDBConnection.release(con);
        }
    }

    /**
     * Wait a while.
     */
    private void waitSome() {
        Random rand = new Random();
        try {
            Thread.sleep(WAIT_BEFORE_INIT_RETRY * TimeUtils.SECOND_IN_MILLIS + rand.nextInt(WAIT_BEFORE_INIT_RETRY));
        } catch (InterruptedException e1) {
            // Ignored
        }
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
     * Method for initialising the database. This basically makes sure that all the replicas are within the database,
     * and that no unknown replicas have been defined.
     *
     * @param connection An open connection to the archive database
     */
    protected void initialiseDB(Connection connection) {
        // retrieve the list of replicas.
        Collection<Replica> replicas = Replica.getKnown();
        // Retrieve the replica IDs currently in the database.
        List<String> repIds = ReplicaCacheHelpers.retrieveIdsFromReplicaTable(connection);
        log.debug("IDs for replicas already in the database: {}", StringUtils.conjoin(",", repIds));
        for (Replica rep : replicas) {
            // try removing the id from the temporary list of IDs within the DB.
            // If the remove is not successful, then the replica is already
            // in the database.
            if (!repIds.remove(rep.getId())) {
                // if the replica id cannot be removed from the list, then it
                // does not exist in the database and must be added.
                log.info("Inserting replica '{}' in database.", rep.toString());
                ReplicaCacheHelpers.insertReplicaIntoDB(rep, connection);
            } else {
                // Otherwise it already exists in the DB.
                log.debug("Replica '{}' already inserted in database.", rep.toString());
            }
        }

        // If unknown replica ids are found, then throw exception.
        if (repIds.size() > 0) {
            throw new IllegalState("The database contain identifiers for the following replicas, which are not "
                    + "defined in the settings: " + repIds);
        }
    }

    /**
     * Method for retrieving the entry in the replicafileinfo table for a given file and replica.
     *
     * @param filename The name of the file for the entry.
     * @param replica The replica of the entry.
     * @return The replicafileinfo entry corresponding to the given filename and replica.
     * @throws ArgumentNotValid If the filename is either null or empty, or if the replica is null.
     */
    public ReplicaFileInfo getReplicaFileInfo(String filename, Replica replica) throws ArgumentNotValid {
        ArgumentNotValid.checkNotNullOrEmpty(filename, "String filename");
        ArgumentNotValid.checkNotNull(replica, "Replica replica");

        // retrieve replicafileinfo for the given filename
        // FIXME Use joins!
        String sql = "SELECT replicafileinfo_guid, replica_id, " + "replicafileinfo.file_id, "
                + "segment_id, checksum, upload_status, filelist_status, "
                + "checksum_status, filelist_checkdatetime, checksum_checkdatetime " + "FROM replicafileinfo, file "
                + " WHERE file.file_id = replicafileinfo.file_id" + " AND file.filename=? AND replica_id=?";

        PreparedStatement s = null;
        Connection con = ArchiveDBConnection.get();
        try {
            s = DBUtils.prepareStatement(con, sql, filename, replica.getId());
            ResultSet res = s.executeQuery();
            if (res.next()) {
                // return the corresponding replica file info.
                return new ReplicaFileInfo(res);
            } else {
                return null;
            }
        } catch (SQLException e) {
            final String message = "SQL error while selecting ResultsSet by executing statement '" + sql + "'.";
            log.warn(message, e);
            throw new IOFailure(message, e);
        } finally {
            DBUtils.closeStatementIfOpen(s);
            ArchiveDBConnection.release(con);
        }

    }

    /**
     * Method for retrieving the checksum for a specific file. Since a file is not directly attached with a checksum,
     * the checksum of a file must be found by having the replicafileinfo entries for the file vote about it.
     *
     * @param filename The name of the file, whose checksum are to be found.
     * @return The checksum of the file, or a Null if no validated checksum can be found.
     * @throws ArgumentNotValid If the filename is either null or the empty string.
     */
    public String getChecksum(String filename) throws ArgumentNotValid {
        ArgumentNotValid.checkNotNullOrEmpty(filename, "String filename");

        Connection con = ArchiveDBConnection.get();
        try {
            // retrieve the fileId
            long fileId = ReplicaCacheHelpers.retrieveIdForFile(filename, con);

            // Check if a checksum with status OK for the file can be found in
            // the database
            for (Replica rep : Replica.getKnown()) {
                // Return the checksum, if it has a valid status.
                if (ReplicaCacheHelpers.retrieveChecksumStatusForReplicaFileInfoEntry(fileId, rep.getId(), con) == ChecksumStatus.OK) {
                    return ReplicaCacheHelpers.retrieveChecksumForReplicaFileInfoEntry(fileId, rep.getId(), con);
                }
            }

            // log that we vote about the file.
            log.debug("No commonly accepted checksum for the file '{}' has previously been found. "
                    + "Voting to achieve one.", filename);

            // retrieves all the UNKNOWN_STATE checksums, and return if unanimous.
            Set<String> checksums = new HashSet<String>();

            for (Replica rep : Replica.getKnown()) {
                if (ReplicaCacheHelpers.retrieveChecksumStatusForReplicaFileInfoEntry(fileId, rep.getId(), con) != ChecksumStatus.CORRUPT) {
                    String tmpChecksum = ReplicaCacheHelpers.retrieveChecksumForReplicaFileInfoEntry(fileId,
                            rep.getId(), con);
                    if (tmpChecksum != null) {
                        checksums.add(tmpChecksum);
                    } else {
                        log.info("Replica '{}' has a null checksum for the file '{}'.", rep.getId(),
                                ReplicaCacheHelpers.retrieveFilenameForFileId(fileId, con));
                    }
                }
            }

            // check if unanimous (thus exactly one!)
            if (checksums.size() == 1) {
                // return the first and only value.
                return checksums.iterator().next();
            }

            // If no checksums are found, then return null.
            if (checksums.size() == 0) {
                log.warn("No checksums found for file '{}'.", filename);
                return null;
            }

            log.info("No unanimous checksum found for file '{}'.", filename);
            // put all into a list for voting
            List<String> checksumList = new ArrayList<String>();
            for (Replica rep : Replica.getKnown()) {
                String cs = ReplicaCacheHelpers.retrieveChecksumForReplicaFileInfoEntry(fileId, rep.getId(), con);

                if (cs != null) {
                    checksumList.add(cs);
                } else {
                    // log when it is second time we find this checksum to be null?
                    log.debug("Replica '{}' has a null checksum for the file '{}'.", rep.getId(),
                            ReplicaCacheHelpers.retrieveFilenameForFileId(fileId, con));
                }
            }

            // vote and return the most frequent checksum.
            return ReplicaCacheHelpers.vote(checksumList);

        } finally {
            ArchiveDBConnection.release(con);
        }
    }

    /**
     * Retrieves the names of all the files in the file table of the database.
     *
     * @return The list of filenames known by the database.
     */
    public Collection<String> retrieveAllFilenames() {
        Connection con = ArchiveDBConnection.get();
        // make sql query.
        final String sql = "SELECT filename FROM file";
        try {
            // Perform the select.
            return DBUtils.selectStringList(con, sql, new Object[] {});
        } finally {
            ArchiveDBConnection.release(con);
        }
    }

    /**
     * Retrieves the ReplicaStoreState for the entry in the replicafileinfo table, which refers to the given file and
     * replica.
     *
     * @param filename The name of the file in the filetable.
     * @param replicaId The id of the replica.
     * @return The ReplicaStoreState for the specified entry.
     * @throws ArgumentNotValid If the replicaId or the filename are eihter null or the empty string.
     */
    public ReplicaStoreState getReplicaStoreState(String filename, String replicaId) throws ArgumentNotValid {
        ArgumentNotValid.checkNotNullOrEmpty(filename, "String filename");
        ArgumentNotValid.checkNotNullOrEmpty(replicaId, "String replicaId");

        Connection con = ArchiveDBConnection.get();

        // Make query for extracting the upload status.
        // FIXME Use joins.
        String sql = "SELECT upload_status FROM replicafileinfo, file WHERE "
                + "replicafileinfo.file_id = file.file_id AND file.filename = ? " + "AND replica_id = ?";
        try {
            // execute the query.
            int ordinal = DBUtils.selectIntValue(con, sql, filename, replicaId);

            // return the corresponding ReplicaStoreState.
            return ReplicaStoreState.fromOrdinal(ordinal);
        } finally {
            ArchiveDBConnection.release(con);
        }
    }

    /**
     * Sets the ReplicaStoreState for the entry in the replicafileinfo table.
     *
     * @param filename The name of the file in the filetable.
     * @param replicaId The id of the replica.
     * @param state The ReplicaStoreState for the specified entry.
     * @throws ArgumentNotValid If the replicaId or the filename are eihter null or the empty string. Or if the
     * ReplicaStoreState is null.
     */
    public void setReplicaStoreState(String filename, String replicaId, ReplicaStoreState state)
            throws ArgumentNotValid {
        ArgumentNotValid.checkNotNullOrEmpty(filename, "String filename");
        ArgumentNotValid.checkNotNullOrEmpty(replicaId, "String replicaId");
        ArgumentNotValid.checkNotNull(state, "ReplicaStoreState state");

        Connection con = ArchiveDBConnection.get();
        PreparedStatement statement = null;
        try {
            // retrieve the guid for the file.
            long fileId = ReplicaCacheHelpers.retrieveIdForFile(filename, con);

            // Make query for updating the upload status
            if (state == ReplicaStoreState.UPLOAD_COMPLETED) {
                // An UPLOAD_COMPLETE
                // UPLOAD_COMPLETE => filelist_status = OK, checksum_status = OK
                String sql = "UPDATE replicafileinfo SET upload_status = ?, "
                        + "filelist_status = ?, checksum_status = ? " + "WHERE replica_id = ? AND file_id = ?";
                statement = DBUtils.prepareStatement(con, sql, state.ordinal(), FileListStatus.OK.ordinal(),
                        ChecksumStatus.OK.ordinal(), replicaId, fileId);
            } else {
                String sql = "UPDATE replicafileinfo SET upload_status = ? WHERE replica_id = ? AND file_id = ?";
                statement = DBUtils.prepareStatement(con, sql, state.ordinal(), replicaId, fileId);
            }

            // execute the update and commit to database.
            statement.executeUpdate();
            con.commit();
        } catch (SQLException e) {
            String errMsg = "Received the following SQL error while updating  the database: "
                    + ExceptionUtils.getSQLExceptionCause(e);
            log.warn(errMsg, e);
            throw new IOFailure(errMsg, e);
        } finally {
            DBUtils.closeStatementIfOpen(statement);
            ArchiveDBConnection.release(con);
        }
    }

    /**
     * Creates a new entry for the filename for each replica, and give it the given checksum and set the upload_status =
     * UNKNOWN_UPLOAD_STATUS.
     *
     * @param filename The name of the file.
     * @param checksum The checksum of the file.
     * @throws ArgumentNotValid If the filename or the checksum is either null or the empty string.
     * @throws IllegalState If the file exists with another checksum on one of the replicas. Or if the file has already
     * been completely uploaded to one of the replicas.
     */
    public void insertNewFileForUpload(String filename, String checksum) throws ArgumentNotValid, IllegalState {
        ArgumentNotValid.checkNotNullOrEmpty(filename, "String filename");
        ArgumentNotValid.checkNotNullOrEmpty(checksum, "String checkums");

        Connection con = ArchiveDBConnection.get();
        // retrieve the fileId for the filename.
        long fileId;

        try {
            // insert into DB, or make sure that it can be inserted.
            if (existsFileInDB(filename)) {
                // retrieve the fileId of the existing file.
                fileId = ReplicaCacheHelpers.retrieveIdForFile(filename, con);

                // Check the entries for this file associated with the replicas.
                for (Replica rep : Replica.getKnown()) {
                    // Ensure that the file has not been completely uploaded to a
                    // replica.
                    ReplicaStoreState us = ReplicaCacheHelpers.retrieveUploadStatus(fileId, rep.getId(), con);

                    if (us.equals(ReplicaStoreState.UPLOAD_COMPLETED)) {
                        throw new IllegalState("The file has already been completely uploaded to the replica: " + rep);
                    }

                    // make sure that it has not been attempted uploaded with
                    // another checksum
                    String entryCs = ReplicaCacheHelpers.retrieveChecksumForReplicaFileInfoEntry(fileId, rep.getId(),
                            con);

                    // throw an exception if the registered checksum differs.
                    if (entryCs != null && !checksum.equals(entryCs)) {
                        throw new IllegalState("The file '" + filename + "' with checksum '" + entryCs
                                + "' has attempted being uploaded with the checksum '" + checksum + "'");
                    }
                }
            } else {
                fileId = ReplicaCacheHelpers.insertFileIntoDB(filename, con);
            }

            for (Replica rep : Replica.getKnown()) {
                // retrieve the guid for the corresponding replicafileinfo entry
                long guid = ReplicaCacheHelpers.retrieveReplicaFileInfoGuid(fileId, rep.getId(), con);

                // Update with the correct information.
                ReplicaCacheHelpers.updateReplicaFileInfo(guid, checksum, ReplicaStoreState.UNKNOWN_UPLOAD_STATE, con);
            }
        } finally {
            ArchiveDBConnection.release(con);
        }
    }

    /**
     * Method for inserting an entry into the database about a file upload has begun for a specific replica. It is not
     * tested whether the entry has another checksum or another UploadStatus.
     *
     * @param filename The name of the file.
     * @param replica The replica for the replicafileinfo.
     * @param state The new ReplicaStoreState for the entry.
     * @throws ArgumentNotValid If the filename is either null or the empty string. Or if the replica or the status is
     * null.
     */
    public void changeStateOfReplicafileinfo(String filename, Replica replica, ReplicaStoreState state)
            throws ArgumentNotValid {
        ArgumentNotValid.checkNotNullOrEmpty(filename, "String filename");
        ArgumentNotValid.checkNotNull(replica, "Replica rep");
        ArgumentNotValid.checkNotNull(state, "ReplicaStoreState state");

        PreparedStatement statement = null;
        Connection connection = null;
        try {
            connection = ArchiveDBConnection.get();
            // retrieve the replicafileinfo_guid for this filename .
            long guid = ReplicaCacheHelpers.retrieveGuidForFilenameOnReplica(filename, replica.getId(), connection);
            statement = connection.prepareStatement("UPDATE replicafileinfo SET upload_status = ? "
                    + "WHERE replicafileinfo_guid = ?");
            statement.setLong(1, state.ordinal());
            statement.setLong(2, guid);

            // Perform the update.
            statement.executeUpdate();
            connection.commit();
        } catch (SQLException e) {
            throw new IllegalState("Cannot update status and checksum of a replicafileinfo in the database.", e);
        } finally {
            DBUtils.closeStatementIfOpen(statement);
            if (connection != null) {
                ArchiveDBConnection.release(connection);
            }
        }
    }

    /**
     * Method for inserting an entry into the database about a file upload has begun for a specific replica. It is not
     * tested whether the entry has another checksum or another UploadStatus.
     *
     * @param filename The name of the file.
     * @param checksum The new checksum for the entry.
     * @param replica The replica for the replicafileinfo.
     * @param state The new ReplicaStoreState for the entry.
     * @throws ArgumentNotValid If the filename or the checksum is either null or the empty string. Or if the replica or
     * the status is null.
     * @throws IllegalState If an sql exception is thrown.
     */
    public void changeStateOfReplicafileinfo(String filename, String checksum, Replica replica, ReplicaStoreState state)
            throws ArgumentNotValid, IllegalState {
        ArgumentNotValid.checkNotNullOrEmpty(filename, "String filename");
        ArgumentNotValid.checkNotNullOrEmpty(checksum, "String checksum");
        ArgumentNotValid.checkNotNull(replica, "Replica rep");
        ArgumentNotValid.checkNotNull(state, "ReplicaStoreState state");

        PreparedStatement statement = null;
        Connection connection = null;
        try {
            connection = ArchiveDBConnection.get();
            // retrieve the replicafileinfo_guid for this filename .
            long guid = ReplicaCacheHelpers.retrieveGuidForFilenameOnReplica(filename, replica.getId(), connection);

            statement = connection.prepareStatement("UPDATE replicafileinfo SET upload_status = ?, checksum = ? "
                    + "WHERE replicafileinfo_guid = ?");
            statement.setLong(1, state.ordinal());
            statement.setString(2, checksum);
            statement.setLong(3, guid);

            // Perform the update.
            statement.executeUpdate();
            connection.commit();
        } catch (SQLException e) {
            throw new IllegalState("Cannot update status and checksum of a replicafileinfo in the database.", e);
        } finally {
            DBUtils.closeStatementIfOpen(statement);
            ArchiveDBConnection.release(connection);
        }
    }

    /**
     * Retrieves the names of all the files in the given replica which has the specified UploadStatus.
     *
     * @param replicaId The id of the replica which contain the files.
     * @param state The ReplicaStoreState for the wanted files.
     * @return The list of filenames for the entries in the replica which has the specified UploadStatus.
     * @throws ArgumentNotValid If the UploadStatus is null or if the replicaId is either null or the empty string.
     */
    public Collection<String> retrieveFilenamesForReplicaEntries(String replicaId, ReplicaStoreState state)
            throws ArgumentNotValid {
        ArgumentNotValid.checkNotNull(state, "ReplicaStoreState state");
        ArgumentNotValid.checkNotNullOrEmpty(replicaId, "String replicaId");
        Connection con = ArchiveDBConnection.get();
        final String sql = "SELECT filename FROM replicafileinfo "
                + "LEFT OUTER JOIN file ON replicafileinfo.file_id = file.file_id "
                + "WHERE replica_id = ? AND upload_status = ?";
        try {
            return DBUtils.selectStringList(con, sql, replicaId, state.ordinal());
        } finally {
            ArchiveDBConnection.release(con);
        }
    }

    /**
     * Checks whether a file is already in the file table in the database.
     *
     * @param filename The name of the file in the database.
     * @return Whether the file was found in the database.
     * @throws IllegalState If more than one entry with the given filename was found.
     */
    public boolean existsFileInDB(String filename) throws IllegalState {
        // retrieve the amount of times this replica is within the database.
        Connection con = ArchiveDBConnection.get();
        final String sql = "SELECT COUNT(*) FROM file WHERE filename = ?";
        try {
            int count = DBUtils.selectIntValue(con, sql, filename);

            // Handle the different cases for count.
            switch (count) {
            case 0:
                return false;
            case 1:
                return true;
            default:
                throw new IllegalState("Cannot handle " + count + " files " + "with the name '" + filename + "'.");
            }
        } finally {
            ArchiveDBConnection.release(con);
        }
    }

    /**
     * Method for retrieving the filelist_status for a replicafileinfo entry.
     *
     * @param filename The name of the file.
     * @param replica The replica where the file should be.
     * @return The filelist_status for the file in the replica.
     * @throws ArgumentNotValid If the replica is null or the filename is either null or the empty string.
     */
    public FileListStatus retrieveFileListStatus(String filename, Replica replica) throws ArgumentNotValid {
        ArgumentNotValid.checkNotNullOrEmpty(filename, "String filename");
        ArgumentNotValid.checkNotNull(replica, "Replica replica");

        Connection con = ArchiveDBConnection.get();
        try {
            // retrieve the filelist_status for the entry.
            int status = ReplicaCacheHelpers.retrieveFileListStatusFromReplicaFileInfo(filename, replica.getId(), con);
            // Return the corresponding FileListStatus
            return FileListStatus.fromOrdinal(status);
        } finally {
            ArchiveDBConnection.release(con);
        }

    }

    /**
     * SQL used to update the checksum status of straightforward cases. See complete description for method below.
     */
    public static final String updateChecksumStatusSql = "" + "UPDATE replicafileinfo SET checksum_status = "
            + ChecksumStatus.OK.ordinal() + " " + "WHERE checksum_status != " + ChecksumStatus.OK.ordinal()
            + " AND file_id IN ( " + "  SELECT file_id " + "  FROM ( "
            + "    SELECT file_id, COUNT(file_id) AS checksums, SUM(replicas) replicas " + "    FROM ( "
            + "      SELECT file_id, COUNT(checksum) AS replicas, checksum " + "      FROM replicafileinfo "
            + "      WHERE filelist_status != " + FileListStatus.MISSING.ordinal() + " AND checksum IS NOT NULL "
            + "      GROUP BY file_id, checksum " + "    ) AS ss1 " + "    GROUP BY file_id " + "  ) AS ss2 "
            + "  WHERE checksums = 1 " + ")";

    /**
     * SQL used to select those files whose check status has to be voted on. See complete description for method below.
     */
    public static final String selectForFileChecksumVotingSql = "" + "SELECT file_id " + "FROM ( "
            + "  SELECT file_id, COUNT(file_id) AS checksums, SUM(replicas) replicas " + "  FROM ( "
            + "    SELECT file_id, COUNT(checksum) AS replicas, checksum " + "    FROM replicafileinfo "
            + "    WHERE filelist_status != " + FileListStatus.MISSING.ordinal() + " AND checksum IS NOT NULL "
            + "    GROUP BY file_id, checksum " + "  ) AS ss1 " + "  GROUP BY file_id " + ") AS ss2 "
            + "WHERE checksums > 1 ";

    /**
     * This method is used to update the status for the checksums for all replicafileinfo entries. <br/>
     * <br/>
     * For each file in the database, the checksum vote is made in the following way. <br/>
     * Each entry in the replicafileinfo table containing the file is retrieved. All the unique checksums are retrieved,
     * e.g. if a checksum is found more than one, then it is ignored. <br/>
     * If only one unique checksum is found, then if must be the correct one, and all the replicas with this file will
     * have their checksum_status set to 'OK'. <br/>
     * If more than one checksum is found, then a vote for the correct checksum is performed. This is done by counting
     * the amount of time each of the unique checksum is found among the replicafileinfo entries for the current file.
     * The checksum with most votes is chosen as the correct one, and the checksum_status for all the replicafileinfo
     * entries with this checksum is set to 'OK', whereas the replicafileinfo entries with a different checksum is set
     * to 'CORRUPT'. <br/>
     * If no winner is found then a warning and a notification is issued, and the checksum_status for all the
     * replicafileinfo entries with for the current file is set to 'UNKNOWN'. <br/>
     */
    public void updateChecksumStatus() {
        log.info("UpdateChecksumStatus operation commencing");
        Connection con = ArchiveDBConnection.get();
        boolean autoCommit = true;
        try {
            autoCommit = con.getAutoCommit();
            // Set checksum_status to 'OK' where there is the same
            // checksum across all replicas.
            DBUtils.executeSQL(con, updateChecksumStatusSql);

            // Get all the fileids that need processing.
            // Previously: "SELECT file_id FROM file"
            Iterator<Long> fileIdsIterator = DBUtils.selectLongIterator(con, selectForFileChecksumVotingSql);
            // For each fileid
            while (fileIdsIterator.hasNext()) {
                long fileId = fileIdsIterator.next();
                ReplicaCacheHelpers.fileChecksumVote(fileId, con);
            }
        } catch (SQLException e) {
            throw new IOFailure("Error getting auto commit.\n" + ExceptionUtils.getSQLExceptionCause(e), e);
        } finally {
            try {
                con.setAutoCommit(autoCommit);
            } catch (SQLException e) {
                log.error("Could not change auto commit back to default!");
            }
            ArchiveDBConnection.release(con);
        }
        log.info("UpdateChecksumStatus operation completed!");
    }

    /**
     * Method for updating the status for a specific file for all the replicas. If the checksums for the replicas differ
     * for some replica, then based on a checksum vote, a specific checksum is chosen as the 'correct' one, and the
     * entries with another checksum than the 'correct one' will be marked as corrupt.
     *
     * @param filename The name of the file to update the status for.
     * @throws ArgumentNotValid If the filename is either null or the empty string.
     */
    @Override
    public void updateChecksumStatus(String filename) throws ArgumentNotValid {
        ArgumentNotValid.checkNotNullOrEmpty(filename, "String filename");

        Connection con = ArchiveDBConnection.get();
        try {
            // retrieve the id and vote!
            Long fileId = ReplicaCacheHelpers.retrieveIdForFile(filename, con);
            ReplicaCacheHelpers.fileChecksumVote(fileId, con);
        } finally {
            ArchiveDBConnection.release(con);
        }
    }

    /**
     * Given the output of a checksum job, add the results to the database.
     * <p>
     * The following fields in the table are updated for each corresponding entry in the replicafileinfo table: <br/>
     * - checksum = the given checksum. <br/>
     * - filelist_status = ok. <br/>
     * - filelist_checkdatetime = now. <br/>
     * - checksum_checkdatetime = now.
     *
     * @param checksumOutputFile The output of a checksum job in a file
     * @param replica The replica this checksum job is for.
     */
    @Override
    public void addChecksumInformation(File checksumOutputFile, Replica replica) {
        // validate arguments
        ArgumentNotValid.checkNotNull(checksumOutputFile, "File checksumOutputFile");
        ArgumentNotValid.checkNotNull(replica, "Replica replica");

        // Sort the checksumOutputFile file.
        File sortedResult = new File(checksumOutputFile.getParent(), checksumOutputFile.getName() + ".sorted");
        FileUtils.sortFile(checksumOutputFile, sortedResult);
        final long datasize = FileUtils.countLines(sortedResult);

        Set<Long> missingReplicaRFIs = null;
        Connection con = ArchiveDBConnection.get();
        LineIterator lineIterator = null;
        try {
            // Make sure, that the replica exists in the database.
            if (!ReplicaCacheHelpers.existsReplicaInDB(replica, con)) {
                String msg = "Cannot add checksum information, since the replica '" + replica.toString()
                        + "' does not exist within the database.";
                log.warn(msg);
                throw new IOFailure(msg);
            }

            log.info("Starting processing of {} checksum entries for replica {}", datasize, replica.getId());

            // retrieve the list of files already known by this cache.
            // TODO This does not scale! Should the datastructure
            // (missingReplicaRFIs) be disk-bound in some way, or optimized
            // in some way, e.g. using it.unimi.dsi.fastutil.longs.LongArrayList
            missingReplicaRFIs = ReplicaCacheHelpers.retrieveReplicaFileInfoGuidsForReplica(replica.getId(), con);

            // Initialize the String iterator
            lineIterator = new LineIterator(new FileReader(sortedResult));

            String lastFilename = "";
            String lastChecksum = "";

            int i = 0;
            while (lineIterator.hasNext()) {
                String line = lineIterator.next();
                // log that it is in progress every so often.
                if ((i % LOGGING_ENTRY_INTERVAL) == 0) {
                    log.info("Processed checksum list entry number {} for replica {}", i, replica);
                    // Close connection, and open another one
                    // to avoid memory-leak (NAS-2003)
                    ArchiveDBConnection.release(con);
                    con = ArchiveDBConnection.get();
                    log.debug("Databaseconnection has now been renewed");
                }
                ++i;

                // parse the input.
                final KeyValuePair<String, String> entry = ChecksumJob.parseLine(line);
                final String filename = entry.getKey();
                final String checksum = entry.getValue();

                // check for duplicates
                if (filename.equals(lastFilename)) {
                    // if different checksums, then
                    if (!checksum.equals(lastChecksum)) {
                        // log and send notification
                        String errMsg = "Unidentical duplicates of file '" + filename + "' with the checksums '"
                                + lastChecksum + "' and '" + checksum + "'. First instance used.";
                        log.warn(errMsg);
                        NotificationsFactory.getInstance().notify(errMsg, NotificationType.WARNING);
                    } else {
                        // log about duplicate identical
                        log.debug("Duplicates of the file '{}' found with the same checksum '{}'.", filename, checksum);
                    }

                    // avoid overhead of inserting duplicates twice.
                    continue;
                }

                // set these value to be the old values in next iteration.
                lastFilename = filename;
                lastChecksum = checksum;

                // Process the current (filename + checksum) combo for this replica
                // Remove the returned replicafileinfo guid from the missing entries.
                missingReplicaRFIs.remove(ReplicaCacheHelpers.processChecksumline(filename, checksum, replica, con));
            }
        } catch (IOException e) {
            throw new IOFailure("Unable to read checksum entries from file", e);
        } finally {
            ArchiveDBConnection.release(con);
            LineIterator.closeQuietly(lineIterator);
        }

        con = ArchiveDBConnection.get();
        try {
            // go through the not found replicafileinfo for this replica to change
            // their filelist_status to missing.
            if (missingReplicaRFIs.size() > 0) {
                log.warn("Found {} missing files for replica '{}'.", missingReplicaRFIs.size(), replica);
                for (long rfi : missingReplicaRFIs) {
                    // set the replicafileinfo in the database to missing.
                    ReplicaCacheHelpers.updateReplicaFileInfoMissingFromFilelist(rfi, con);
                }
            }

            // update the checksum updated date for this replica.
            ReplicaCacheHelpers.updateChecksumDateForReplica(replica, con);
            ReplicaCacheHelpers.updateFilelistDateForReplica(replica, con);

            log.info("Finished processing of {} checksum entries for replica {}", datasize, replica.getId());
        } finally {
            ArchiveDBConnection.release(con);
        }
    }

    /**
     * Method for adding the results from a list of filenames on a replica. This list of filenames should return the
     * list of all the files within the database.
     * <p>
     * For each file in the FileListJob the following fields are set for the corresponding entry in the replicafileinfo
     * table: <br/>
     * - filelist_status = ok. <br/>
     * - filelist_checkdatetime = now.
     * <p>
     * For each entry in the replicafileinfo table for the replica which are missing in the results from the FileListJob
     * the following fields are assigned the following values: <br/>
     * - filelist_status = missing. <br/>
     * - filelist_checkdatetime = now.
     *
     * @param filelistFile The list of filenames either parsed from a FilelistJob or the result from a
     * GetAllFilenamesMessage.
     * @param replica The replica, which the FilelistBatchjob has run upon.
     * @throws ArgumentNotValid If the filelist or the replica is null.
     * @throws UnknownID If the replica does not already exist in the database.
     */
    @Override
    public void addFileListInformation(File filelistFile, Replica replica) throws ArgumentNotValid, UnknownID {
        ArgumentNotValid.checkNotNull(filelistFile, "File filelistFile");
        ArgumentNotValid.checkNotNull(replica, "Replica replica");

        // Sort the filelist file.
        File sortedResult = new File(filelistFile.getParent(), filelistFile.getName() + ".sorted");
        FileUtils.sortFile(filelistFile, sortedResult);
        final long datasize = FileUtils.countLines(sortedResult);

        Connection con = ArchiveDBConnection.get();
        Set<Long> missingReplicaRFIs = null;
        LineIterator lineIterator = null;
        try {
            // Make sure, that the replica exists in the database.
            if (!ReplicaCacheHelpers.existsReplicaInDB(replica, con)) {
                String errorMsg = "Cannot add filelist information, since the replica '" + replica.toString()
                        + "' does not exist in the database.";
                log.warn(errorMsg);
                throw new UnknownID(errorMsg);
            }

            log.info("Starting processing of {} filelist entries for replica {}", datasize, replica.getId());

            // retrieve the list of files already known by this cache.
            // TODO This does not scale! Should this datastructure
            // (missingReplicaRFIs) be disk-bound in some way.
            missingReplicaRFIs = ReplicaCacheHelpers.retrieveReplicaFileInfoGuidsForReplica(replica.getId(), con);

            // Initialize String iterator
            lineIterator = new LineIterator(new FileReader(sortedResult));

            String lastFileName = "";
            int i = 0;
            while (lineIterator.hasNext()) {
                String file = lineIterator.next();
                // log that it is in progress every so often.
                if ((i % LOGGING_ENTRY_INTERVAL) == 0) {
                    log.info("Processed file list entry number {} for replica {}", i, replica);
                    // Close connection, and open another one
                    // to avoid memory-leak (NAS-2003)
                    ArchiveDBConnection.release(con);
                    con = ArchiveDBConnection.get();
                    log.debug("Databaseconnection has now been renewed");
                }
                ++i;

                // handle duplicates.
                if (file.equals(lastFileName)) {
                    log.warn("There have been found multiple files with the name '{}'", file);
                    continue;
                }

                lastFileName = file;
                // Add information for one file, and remove the ReplicaRFI from the
                // set of missing ones.
                missingReplicaRFIs.remove(ReplicaCacheHelpers.addFileInformation(file, replica, con));
            }
        } catch (IOException e) {
            throw new IOFailure("Unable to read the filenames from file", e);
        } finally {
            ArchiveDBConnection.release(con);
            LineIterator.closeQuietly(lineIterator);
        }

        con = ArchiveDBConnection.get();
        try {
            // go through the not found replicafileinfo for this replica to change
            // their filelist_status to missing.
            if (missingReplicaRFIs.size() > 0) {
                log.warn("Found {} missing files for replica '{}'.", missingReplicaRFIs.size(), replica);
                for (long rfi : missingReplicaRFIs) {
                    // set the replicafileinfo in the database to missing.
                    ReplicaCacheHelpers.updateReplicaFileInfoMissingFromFilelist(rfi, con);
                }
            }
            // Update the date for filelist update for this replica.
            ReplicaCacheHelpers.updateFilelistDateForReplica(replica, con);
        } finally {
            ArchiveDBConnection.release(con);
        }
    }

    /**
     * Get the date for the last file list job.
     *
     * @param replica The replica to get the date for.
     * @return The date of the last missing files update for the replica. A null is returned if no last missing files
     * update has been performed.
     * @throws ArgumentNotValid If the replica is null.
     * @throws IllegalArgumentException If the Date of the Timestamp cannot be instantiated.
     */
    @Override
    public Date getDateOfLastMissingFilesUpdate(Replica replica) throws ArgumentNotValid, IllegalArgumentException {
        ArgumentNotValid.checkNotNull(replica, "Replica replica");
        Connection con = ArchiveDBConnection.get();
        String result = null;
        try {
            // sql for retrieving this replicafileinfo_guid.
            String sql = "SELECT filelist_updated FROM replica WHERE replica_id = ?";
            result = DBUtils.selectStringValue(con, sql, replica.getId());
        } finally {
            ArchiveDBConnection.release(con);
        }
        // return null if the field has no be set for this replica.
        if (result == null) {
            log.debug("The 'filelist_updated' field has not been set, as no missing files update has been performed yet.");
            return null;
        } else {
            // Parse the timestamp into a date.
            return new Date(Timestamp.valueOf(result).getTime());
        }
    }

    /**
     * Method for retrieving the date for the last update for corrupted files.
     * <p>
     * This method does not contact the replicas, it only retrieves the data from the last time the checksum was
     * retrieved.
     *
     * @param replica The replica to find the date for the latest update for corruption of files.
     * @return The date for the last checksum update. A null is returned if no wrong files update has been performed for
     * this replica.
     * @throws ArgumentNotValid If the replica is null.
     * @throws IllegalArgumentException If the Date of the Timestamp cannot be instantiated.
     */
    @Override
    public Date getDateOfLastWrongFilesUpdate(Replica replica) throws ArgumentNotValid, IllegalArgumentException {
        ArgumentNotValid.checkNotNull(replica, "Replica replica");
        Connection con = ArchiveDBConnection.get();
        String result = null;
        try {
            // The SQL statement for retrieving the date for last update of
            // checksum for the replica.
            final String sql = "SELECT checksum_updated FROM replica WHERE replica_id = ?";
            result = DBUtils.selectStringValue(con, sql, replica.getId());
        } finally {
            ArchiveDBConnection.release(con);
        }
        // return null if the field has no be set for this replica.
        if (result == null) {
            log.debug("The 'checksum_updated' field has not been set, as no wrong files update has been performed yet.");
            return null;
        } else {
            // Parse the timestamp into a date.
            return new Date(Timestamp.valueOf(result).getTime());
        }
    }

    /**
     * Method for retrieving the number of files missing from a specific replica.
     * <p>
     * This method does not contact the replica directly, it only retrieves the count of missing files from the last
     * filelist update.
     *
     * @param replica The replica to find the number of missing files for.
     * @return The number of missing files for the replica.
     * @throws ArgumentNotValid If the replica is null.
     */
    @Override
    public long getNumberOfMissingFilesInLastUpdate(Replica replica) throws ArgumentNotValid {
        ArgumentNotValid.checkNotNull(replica, "Replica replica");
        Connection con = ArchiveDBConnection.get();
        // The SQL statement to retrieve the number of entries in the
        // replicafileinfo table with file_status set to either missing or
        // no_status for the replica.
        // FIXME Consider using a UNION instead of OR.
        final String sql = "SELECT COUNT(*) FROM replicafileinfo "
                + "WHERE replica_id = ? AND ( filelist_status = ? OR filelist_status = ?)";
        try {
            return DBUtils.selectLongValue(con, sql, replica.getId(), FileListStatus.MISSING.ordinal(),
                    FileListStatus.NO_FILELIST_STATUS.ordinal());
        } finally {
            ArchiveDBConnection.release(con);
        }
    }

    /**
     * Method for retrieving the list of the names of the files which was missing for the replica in the last filelist
     * update.
     * <p>
     * This method does not contact the replica, it only uses the database to find the files, which was missing during
     * the last filelist update.
     *
     * @param replica The replica to find the list of missing files for.
     * @return A list containing the names of the files which are missing in the given replica.
     * @throws ArgumentNotValid If the replica is null.
     */
    @Override
    public Iterable<String> getMissingFilesInLastUpdate(Replica replica) throws ArgumentNotValid {
        ArgumentNotValid.checkNotNull(replica, "Replica replica");
        Connection con = ArchiveDBConnection.get();
        // The SQL statement to retrieve the filenames of the missing
        // replicafileinfo to the given replica.
        final String sql = "SELECT filename FROM replicafileinfo "
                + "LEFT OUTER JOIN file ON replicafileinfo.file_id = file.file_id "
                + "WHERE replica_id = ? AND ( filelist_status = ? OR filelist_status = ? )";
        try {
            return DBUtils.selectStringList(con, sql, replica.getId(), FileListStatus.MISSING.ordinal(),
                    FileListStatus.NO_FILELIST_STATUS.ordinal());
        } finally {
            ArchiveDBConnection.release(con);
        }
    }

    /**
     * Method for retrieving the amount of files with a incorrect checksum within a replica.
     * <p>
     * This method does not contact the replica, it only uses the database to count the amount of files which are
     * corrupt.
     *
     * @param replica The replica to find the number of corrupted files for.
     * @return The number of corrupted files.
     * @throws ArgumentNotValid If the replica is null.
     */
    @Override
    public long getNumberOfWrongFilesInLastUpdate(Replica replica) throws ArgumentNotValid {
        ArgumentNotValid.checkNotNull(replica, "Replica");
        Connection con = ArchiveDBConnection.get();
        // The SQL statement to retrieve the number of corrupted entries in
        // the replicafileinfo table for the given replica.
        final String sql = "SELECT COUNT(*) FROM replicafileinfo WHERE replica_id = ? AND checksum_status = ?";
        try {
            return DBUtils.selectLongValue(con, sql, replica.getId(), ChecksumStatus.CORRUPT.ordinal());
        } finally {
            ArchiveDBConnection.release(con);
        }
    }

    /**
     * Method for retrieving the list of the files in the replica which have a incorrect checksum. E.g. the
     * checksum_status is set to CORRUPT.
     * <p>
     * This method does not contact the replica, it only uses the local database.
     *
     * @param replica The replica to find the list of corrupted files for.
     * @return The list of files which have wrong checksums.
     * @throws ArgumentNotValid If the replica is null.
     */
    @Override
    public Iterable<String> getWrongFilesInLastUpdate(Replica replica) throws ArgumentNotValid {
        ArgumentNotValid.checkNotNull(replica, "Replica replica");
        Connection con = ArchiveDBConnection.get();
        // The SQL statement to retrieve the filenames for the corrupted files
        // in the replicafileinfo table for the given replica.
        String sql = "SELECT filename FROM replicafileinfo "
                + "LEFT OUTER JOIN file ON replicafileinfo.file_id = file.file_id "
                + "WHERE replica_id = ? AND checksum_status = ?";
        try {
            return DBUtils.selectStringList(con, sql, replica.getId(), ChecksumStatus.CORRUPT.ordinal());
        } finally {
            ArchiveDBConnection.release(con);
        }
    }

    /**
     * Method for retrieving the number of files within a replica. This count all the files which are not missing from
     * the replica, thus all entries in the replicafileinfo table which has the filelist_status set to OK. It is ignored
     * whether the files has a correct checksum.
     * <p>
     * This method does not contact the replica, it only uses the local database.
     *
     * @param replica The replica to count the number of files for.
     * @return The number of files within the replica.
     * @throws ArgumentNotValid If the replica is null.
     */
    @Override
    public long getNumberOfFiles(Replica replica) throws ArgumentNotValid {
        ArgumentNotValid.checkNotNull(replica, "Replica replica");
        Connection con = ArchiveDBConnection.get();
        // The SQL statement to retrieve the amount of entries in the
        // replicafileinfo table for the replica which have the
        // filelist_status set to OK.
        String sql = "SELECT COUNT(*) FROM replicafileinfo WHERE replica_id  = ? AND filelist_status = ?";
        try {
            return DBUtils.selectLongValue(con, sql, replica.getId(), FileListStatus.OK.ordinal());
        } finally {
            ArchiveDBConnection.release(con);
        }
    }

    /**
     * Method for finding a replica with a valid version of a file. This method is used in order to find a replica from
     * which a file should be retrieved, during the process of restoring a corrupt file on another replica.
     * <p>
     * This replica must of the type bitarchive, since a file cannot be retrieved from a checksum replica.
     *
     * @param filename The name of the file which needs to have a valid version in a bitarchive.
     * @return A bitarchive which contains a valid version of the file, or null if no such bitarchive exists.
     * @throws ArgumentNotValid If the filename is null or the empty string.
     */
    @Override
    public Replica getBitarchiveWithGoodFile(String filename) throws ArgumentNotValid {
        ArgumentNotValid.checkNotNullOrEmpty(filename, "String filename");

        Connection con = ArchiveDBConnection.get();
        try {
            // Retrieve a list of replicas where the the checksum status is OK
            List<String> replicaIds = ReplicaCacheHelpers.retrieveReplicaIdsWithOKChecksumStatus(filename, con);

            // go through the list, and return the first valid bitarchive-replica.
            for (String repId : replicaIds) {
                // Retrieve the replica type.
                ReplicaType repType = ReplicaCacheHelpers.retrieveReplicaType(repId, con);

                // If the replica is of type BITARCHIVE then return it.
                if (repType.equals(ReplicaType.BITARCHIVE)) {
                    log.trace("The replica with id '{}' is the first bitarchive replica which contains the file '{}' "
                            + "with a valid checksum.", repId, filename);
                    return Replica.getReplicaFromId(repId);
                }
            }
        } finally {
            ArchiveDBConnection.release(con);
        }

        // Notify the administrator about that no proper bitarchive was found.
        NotificationsFactory.getInstance().notify(
                "No bitarchive replica " + "was found which contains the file '" + filename + "'.",
                NotificationType.WARNING);

        // If no bitarchive exists that contains the file with a OK checksum_status.
        // then return null.
        return null;
    }

    /**
     * Method for finding a replica with a valid version of a file. This method is used in order to find a replica from
     * which a file should be retrieved, during the process of restoring a corrupt file on another replica.
     * <p>
     * This replica must of the type bitarchive, since a file cannot be retrieved from a checksum replica.
     *
     * @param filename The name of the file which needs to have a valid version in a bitarchive.
     * @param badReplica The Replica which has a bad copy of the given file
     * @return A bitarchive which contains a valid version of the file, or null if no such bitarchive exists (in which
     * case, a notification is sent)
     * @throws ArgumentNotValid If the replica is null or the filename is either null or the empty string.
     */
    @Override
    public Replica getBitarchiveWithGoodFile(String filename, Replica badReplica) throws ArgumentNotValid {
        ArgumentNotValid.checkNotNull(badReplica, "Replica badReplica");
        ArgumentNotValid.checkNotNullOrEmpty(filename, "String filename");

        Connection con = ArchiveDBConnection.get();
        try {
            // Then retrieve a list of replicas where the the checksum status is
            // OK
            List<String> replicaIds = ReplicaCacheHelpers.retrieveReplicaIdsWithOKChecksumStatus(filename, con);

            // Make sure, that the bad replica is not returned.
            replicaIds.remove(badReplica.getId());

            // go through the list, and return the first valid
            // bitarchive-replica.
            for (String repId : replicaIds) {
                // Retrieve the replica type.
                ReplicaType repType = ReplicaCacheHelpers.retrieveReplicaType(repId, con);

                // If the replica is of type BITARCHIVE then return it.
                if (repType.equals(ReplicaType.BITARCHIVE)) {
                    log.trace(
                            "The replica with id '{}' is the first bitarchive replica which contains the file '{}' with a valid checksum.",
                            repId, filename);
                    return Replica.getReplicaFromId(repId);
                }
            }
        } finally {
            ArchiveDBConnection.release(con);
        }
        // Notify the administrator about that no proper bitarchive was found, and log the incidence
        final String msg = "No bitarchive replica " + "was found which contains the file '" + filename + "'.";
        log.warn(msg);
        NotificationsFactory.getInstance().notify(msg, NotificationType.WARNING);

        return null;
    }

    /**
     * Method for updating a specific entry in the replicafileinfo table. Based on the filename, checksum and replica it
     * is verified whether a file is missing, corrupt or valid.
     *
     * @param filename Name of the file.
     * @param checksum The checksum of the file. Is allowed to be null, if no file is found.
     * @param replica The replica where the file exists.
     * @throws ArgumentNotValid If the filename is null or the empty string, or if the replica is null.
     */
    @Override
    public void updateChecksumInformationForFileOnReplica(String filename, String checksum, Replica replica)
            throws ArgumentNotValid {
        ArgumentNotValid.checkNotNullOrEmpty(filename, "String filename");
        ArgumentNotValid.checkNotNull(replica, "Replica replica");

        PreparedStatement statement = null;
        Connection connection = null;
        try {
            connection = ArchiveDBConnection.get();

            long guid = ReplicaCacheHelpers.retrieveGuidForFilenameOnReplica(filename, replica.getId(), connection);

            Date now = new Date(Calendar.getInstance().getTimeInMillis());

            // handle differently whether a checksum was retrieved.
            if (checksum == null) {
                // Set to MISSING! and do not update the checksum
                // (cannot insert null).
                String sql = "UPDATE replicafileinfo "
                        + "SET filelist_status = ?, checksum_status = ?, filelist_checkdatetime = ? "
                        + "WHERE replicafileinfo_guid = ?";
                statement = DBUtils.prepareStatement(connection, sql, FileListStatus.MISSING.ordinal(),
                        ChecksumStatus.UNKNOWN.ordinal(), now, guid);
            } else {
                String sql = "UPDATE replicafileinfo "
                        + "SET checksum = ?, filelist_status = ?, filelist_checkdatetime = ? "
                        + "WHERE replicafileinfo_guid = ?";
                statement = DBUtils.prepareStatement(connection, sql, checksum, FileListStatus.OK.ordinal(), now, guid);
            }
            statement.executeUpdate();
            connection.commit();
        } catch (Exception e) {
            throw new IOFailure("Could not update single checksum entry.", e);
        } finally {
            DBUtils.closeStatementIfOpen(statement);
            if (connection != null) {
                ArchiveDBConnection.release(connection);
            }
        }
    }

    /**
     * Method for inserting a line of Admin.Data into the database. It is assumed that it is a '0.4' admin.data line.
     *
     * @param line The line to insert into the database.
     * @return Whether the line was valid.
     * @throws ArgumentNotValid If the line is null. If it is empty, then it is logged.
     */
    public boolean insertAdminEntry(String line) throws ArgumentNotValid {
        ArgumentNotValid.checkNotNull(line, "String line");

        Connection con = ArchiveDBConnection.get();
        log.trace("Insert admin entry begun");
        final int lengthFirstPart = 4;
        final int lengthOtherParts = 3;
        try {
            // split into parts. First contains
            String[] split = line.split(" , ");

            // Retrieve the basic entry data.
            String[] entryData = split[0].split(" ");

            // Check if enough elements
            if (entryData.length < lengthFirstPart) {
                log.warn("Bad line in Admin.data: {}", line);
                return false;
            }

            String filename = entryData[0];
            String checksum = entryData[1];

            long fileId = ReplicaCacheHelpers.retrieveIdForFile(filename, con);

            // If the fileId is -1, then the file is not within the file table.
            // Thus insert it and retrieve the id.
            if (fileId == -1) {
                fileId = ReplicaCacheHelpers.insertFileIntoDB(filename, con);
            }
            log.trace("Step 1 completed (file created in database).");
            // go through the replica specifics.
            for (int i = 1; i < split.length; i++) {
                String[] repInfo = split[i].split(" ");

                // check if correct size
                if (repInfo.length < lengthOtherParts) {
                    log.warn("Bad replica information '{}' in line '{}'", split[i], line);
                    continue;
                }

                // retrieve the data for this replica
                String replicaId = Channels.retrieveReplicaFromIdentifierChannel(repInfo[0]).getId();
                ReplicaStoreState replicaUploadStatus = ReplicaStoreState.valueOf(repInfo[1]);
                Date replicaDate = new Date(Long.parseLong(repInfo[2]));

                // retrieve the guid of the replicafileinfo.
                long guid = ReplicaCacheHelpers.retrieveReplicaFileInfoGuid(fileId, replicaId, con);

                // Update the replicaFileInfo with the information.
                ReplicaCacheHelpers.updateReplicaFileInfo(guid, checksum, replicaDate, replicaUploadStatus, con);
            }
        } catch (IllegalState e) {
            log.warn("Received IllegalState exception while parsing.", e);
            return false;
        } finally {
            ArchiveDBConnection.release(con);
        }
        log.trace("Insert admin entry finished");
        return true;
    }

    /**
     * Method for setting a specific value for the filelistdate and the checksumlistdate for all the replicas.
     *
     * @param date The new date for the checksumlist and filelist for all the replicas.
     * @throws ArgumentNotValid If the date is null.
     */
    public void setAdminDate(Date date) throws ArgumentNotValid {
        ArgumentNotValid.checkNotNull(date, "Date date");

        Connection con = ArchiveDBConnection.get();
        try {
            // set the date for the replicas.
            for (Replica rep : Replica.getKnown()) {
                ReplicaCacheHelpers.setFilelistDateForReplica(rep, date, con);
                ReplicaCacheHelpers.setChecksumlistDateForReplica(rep, date, con);
            }
        } finally {
            ArchiveDBConnection.release(con);
        }
    }

    /**
     * Method for telling whether the database is empty. The database is empty if it does not contain any files.
     * <p>
     * The database will not be entirely empty, since the replicas are put into the replica table during the
     * instantiation of this class, but if the file table is empty, then the replicafileinfo table is also empty, and
     * the database will be considered empty.
     *
     * @return Whether the file list is empty.
     */
    public boolean isEmpty() {
        // The SQL statement to retrieve the amount of entries in the
        // file table. No arguments (represented by empty Object array).
        final String sql = "SELECT COUNT(*) FROM file";
        Connection con = ArchiveDBConnection.get();
        try {
            return DBUtils.selectLongValue(con, sql, new Object[0]) == 0L;
        } finally {
            ArchiveDBConnection.release(con);
        }
    }

    /**
     * Method to print all the tables in the database.
     *
     * @return all the tables as a text string
     */
    public String retrieveAsText() {
        StringBuilder res = new StringBuilder();
        String sql = "";
        Connection connection = ArchiveDBConnection.get();
        // Go through the replica table
        List<String> reps = ReplicaCacheHelpers.retrieveIdsFromReplicaTable(connection);
        res.append("Replica table: " + reps.size() + "\n");
        res.append("GUID \trepId \trepName \trepType \tfileupdate \tchecksumupdated" + "\n");
        res.append("------------------------------------------------------------\n");
        for (String repId : reps) {
            // retrieve the replica_name
            sql = "SELECT replica_guid FROM replica WHERE replica_id = ?";
            String repGUID = DBUtils.selectStringValue(connection, sql, repId);
            // retrieve the replica_name
            sql = "SELECT replica_name FROM replica WHERE replica_id = ?";
            String repName = DBUtils.selectStringValue(connection, sql, repId);
            // retrieve the replica_type
            sql = "SELECT replica_type FROM replica WHERE replica_id = ?";
            int repType = DBUtils.selectIntValue(connection, sql, repId);
            // retrieve the date for last updated
            sql = "SELECT filelist_updated FROM replica WHERE replica_id = ?";
            String filelistUpdated = DBUtils.selectStringValue(connection, sql, repId);
            // retrieve the date for last updated
            sql = "SELECT checksum_updated FROM replica WHERE replica_id = ?";
            String checksumUpdated = DBUtils.selectStringValue(connection, sql, repId);

            // Print
            res.append(repGUID + "\t" + repId + "\t" + repName + "\t" + ReplicaType.fromOrdinal(repType).name() + "\t"
                    + filelistUpdated + "\t" + checksumUpdated + "\n");
        }
        res.append("\n");

        // Go through the file table
        List<String> fileIds = ReplicaCacheHelpers.retrieveIdsFromFileTable(connection);
        res.append("File table : " + fileIds.size() + "\n");
        res.append("fileId \tfilename" + "\n");
        res.append("--------------------" + "\n");
        for (String fileId : fileIds) {
            // retrieve the file_name
            sql = "SELECT filename FROM file WHERE file_id = ?";
            String fileName = DBUtils.selectStringValue(connection, sql, fileId);

            // Print
            res.append(fileId + " \t " + fileName + "\n");
        }
        res.append("\n");

        // Go through the replicafileinfo table
        List<String> rfiIds = ReplicaCacheHelpers.retrieveIdsFromReplicaFileInfoTable(connection);
        res.append("ReplicaFileInfo table : " + rfiIds.size() + "\n");
        res.append("GUID \trepId \tfileId \tchecksum \tus \t\tfls \tcss \tfilelistCheckdate \tchecksumCheckdate\n");
        res.append("---------------------------------------------------------------------------------------------------------\n");
        for (String rfiGUID : rfiIds) {
            // FIXME Replace with one SELECT instead of one SELECT for each row! DOH!
            // retrieve the replica_id
            sql = "SELECT replica_id FROM replicafileinfo WHERE replicafileinfo_guid = ?";
            String replicaId = DBUtils.selectStringValue(connection, sql, rfiGUID);
            // retrieve the file_id
            sql = "SELECT file_id FROM replicafileinfo WHERE replicafileinfo_guid = ?";
            String fileId = DBUtils.selectStringValue(connection, sql, rfiGUID);
            // retrieve the checksum
            sql = "SELECT checksum FROM replicafileinfo WHERE replicafileinfo_guid = ?";
            String checksum = DBUtils.selectStringValue(connection, sql, rfiGUID);
            // retrieve the upload_status
            sql = "SELECT upload_status FROM replicafileinfo WHERE replicafileinfo_guid = ?";
            int uploadStatus = DBUtils.selectIntValue(connection, sql, rfiGUID);
            // retrieve the filelist_status
            sql = "SELECT filelist_status FROM replicafileinfo WHERE replicafileinfo_guid = ?";
            int filelistStatus = DBUtils.selectIntValue(connection, sql, rfiGUID);
            // retrieve the checksum_status
            sql = "SELECT checksum_status FROM replicafileinfo WHERE replicafileinfo_guid = ?";
            int checksumStatus = DBUtils.selectIntValue(connection, sql, rfiGUID);
            // retrieve the filelist_checkdatetime
            sql = "SELECT filelist_checkdatetime FROM replicafileinfo WHERE replicafileinfo_guid = ?";
            String filelistCheckdatetime = DBUtils.selectStringValue(connection, sql, rfiGUID);
            // retrieve the checksum_checkdatetime
            sql = "SELECT checksum_checkdatetime FROM replicafileinfo WHERE replicafileinfo_guid = ?";
            String checksumCheckdatetime = DBUtils.selectStringValue(connection, sql, rfiGUID);

            // Print
            res.append(rfiGUID + " \t" + replicaId + "\t" + fileId + "\t" + checksum + "\t"
                    + ReplicaStoreState.fromOrdinal(uploadStatus).name() + "  \t"
                    + FileListStatus.fromOrdinal(filelistStatus).name() + "\t"
                    + ChecksumStatus.fromOrdinal(checksumStatus).name() + "\t" + filelistCheckdatetime + "\t"
                    + checksumCheckdatetime + "\n");
        }
        res.append("\n");

        return res.toString();
    }

    /**
     * Method for cleaning up.
     */
    @Override
    public synchronized void cleanup() {
        instance = null;
    }

}
