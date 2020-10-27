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

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.distribute.arcrepository.Replica;
import dk.netarkivet.common.distribute.arcrepository.ReplicaStoreState;
import dk.netarkivet.common.distribute.arcrepository.ReplicaType;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.IllegalState;
import dk.netarkivet.common.utils.DBUtils;
import dk.netarkivet.common.utils.NotificationType;
import dk.netarkivet.common.utils.NotificationsFactory;

/**
 * Helper methods used by {@link ReplicaCacheDatabase}.
 */
public final class ReplicaCacheHelpers {

    /** The log. */
    protected static Logger log = LoggerFactory.getLogger(ReplicaCacheHelpers.class);

    /** Private constructor to avoid instantiation. */
    private ReplicaCacheHelpers() {
    }

    /**
     * Method for checking whether a replicafileinfo is in the database.
     *
     * @param fileid The id of the file.
     * @param replicaID The id of the replica.
     * @param con An open connection to the archive database
     * @return Whether the replicafileinfo was there or not.
     * @throws IllegalState If more than one copy of the replicafileinfo is placed in the database.
     */
    protected static boolean existsReplicaFileInfoInDB(long fileid, String replicaID, Connection con)
            throws IllegalState {
        // retrieve the amount of times this replicafileinfo
        // is within the database.
        String sql = "SELECT COUNT(*) FROM replicafileinfo WHERE file_id = ? AND replica_id = ?";
        int count = DBUtils.selectIntValue(con, sql, fileid, replicaID);

        // Handle the different cases for count.
        switch (count) {
        case 0:
            return false;
        case 1:
            return true;
        default:
            throw new IllegalState("Cannot handle " + count + " replicafileinfo entries with the id '" + fileid + "'.");
        }
    }

    /**
     * Method for inserting a Replica into the replica table. The replica_guid is automatically given by the database,
     * and the values in the fields replica_id, replica_name and replica_type is created from the replica argument.
     *
     * @param rep The Replica to insert into the replica table.
     * @param con An open connection to the archive database
     * @throws IOFailure If a SQLException is caught.
     */
    protected static void insertReplicaIntoDB(Replica rep, Connection con) throws IOFailure {
        PreparedStatement statement = null;
        try {
            // Make the SQL statement for putting the replica into the database
            // and insert the variables for the entry to the replica table.
            statement = con.prepareStatement("INSERT INTO replica (replica_id, replica_name, replica_type) "
                    + "(SELECT ?,?,? from replica WHERE replica_id=? HAVING count(*) = 0)");
            statement.setString(1, rep.getId());
            statement.setString(2, rep.getName());
            statement.setInt(3, rep.getType().ordinal());
            statement.setString(4, rep.getId());
            log.debug("Executing insert, conditional on {} not already existing in the database.", rep.getId());
            int result = statement.executeUpdate();
            log.debug("Insert statement for {} returned {}", rep.getId(), result);
            con.commit();
        } catch (SQLException e) {
            throw new IOFailure("Cannot add replica '" + rep + "'to the database.", e);
        } finally {
            DBUtils.closeStatementIfOpen(statement);
        }
    }

    /**
     * Method to create a new entry in the file table in the database. The file_id is automatically created by the
     * database, and the argument is used for the filename for this new entry to the table. This will also create a
     * replicafileinfo entry for each replica.
     *
     * @param filename The filename for the new entry in the file table.
     * @param connection An open connection to the archive database
     * @return created file_id for the new entry.
     * @throws IllegalState If the file cannot be inserted into the database.
     */
    protected static long insertFileIntoDB(String filename, Connection connection) throws IllegalState {
        log.debug("Insert file '{}' into database", filename);
        PreparedStatement statement = null;
        try {
            // Make the SQL statement for putting the replica into the database
            // and insert the variables for the entry to the replica table.
            statement = connection.prepareStatement("INSERT INTO file (filename) VALUES ( ? )",
                    Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, filename);

            // execute the SQL statement
            statement.executeUpdate();
            // Retrieve the fileId for the just inserted file.
            ResultSet resultset = statement.getGeneratedKeys();
            resultset.next();
            long fileId = resultset.getLong(1);
            connection.commit();

            // Create replicafileinfo for each replica.
            createReplicaFileInfoEntriesInDB(fileId, connection);
            log.debug("Insert file '{}' into database completed. Assigned fileID={}", filename, fileId);
            return fileId;
        } catch (SQLException e) {
            throw new IllegalState("Cannot add file '" + filename + "' to the database.", e);
        } finally {
            DBUtils.closeStatementIfOpen(statement);
        }
    }

    /**
     * When a new file is inserted into the database, each replica gets a new entry in the replicafileinfo table for
     * this file. The fields for this new entry are set to the following: - file_id = argument. - replica_id = The id of
     * the current replica. - filelist_status = NO_FILELIST_STATUS. - checksum_status = UNKNOWN. - upload_status =
     * NO_UPLOAD_STATUS.
     * <p>
     * The replicafileinfo_guid is automatically created by the database, and the dates are set to null.
     *
     * @param fileId The id for the file.
     * @param con An open connection to the archive database
     * @throws IllegalState If the file could not be entered into the database.
     */
    protected static void createReplicaFileInfoEntriesInDB(long fileId, Connection con) throws IllegalState {
        PreparedStatement statement = null;
        try {
            // init variables
            List<String> repIds = ReplicaCacheHelpers.retrieveIdsFromReplicaTable(con);

            // Make a entry for each replica.
            for (String repId : repIds) {
                // create if it does not exists already.
                if (!existsReplicaFileInfoInDB(fileId, repId, con)) {
                    // Insert with the known values (no dates).
                    statement = DBUtils.prepareStatement(con,
                            "INSERT INTO replicafileinfo (file_id, replica_id, filelist_status, checksum_status, "
                                    + "upload_status ) VALUES ( ?, ?, ?, ?, ? )", fileId, repId,
                            FileListStatus.NO_FILELIST_STATUS.ordinal(), ChecksumStatus.UNKNOWN.ordinal(),
                            ReplicaStoreState.UNKNOWN_UPLOAD_STATE.ordinal());

                    // execute the SQL statement
                    statement.executeUpdate();
                    con.commit();
                    statement.close(); // Important to cleanup!
                }
            }
        } catch (SQLException e) {
            throw new IllegalState("Cannot add replicafileinfo to the database.", e);
        } finally {
            DBUtils.closeStatementIfOpen(statement);
        }
    }

    /**
     * Method for retrieving the replica IDs within the database.
     *
     * @param con An open connection to the archive database
     * @return The list of replicaIds from the replica table in the database.
     */
    protected static List<String> retrieveIdsFromReplicaTable(Connection con) {
        // Make SQL statement for retrieving the replica_ids in the
        // replica table.
        final String sql = "SELECT replica_id FROM replica";

        // execute the SQL statement and return the results.
        return DBUtils.selectStringList(con, sql);
    }

    /**
     * Method for retrieving all the file IDs within the database.
     *
     * @param con An open connection to the archive database
     * @return The list of fileIds from the file table in the database.
     */
    protected static List<String> retrieveIdsFromFileTable(Connection con) {
        // Make SQL statement for retrieving the file_ids in the
        // file table.
        final String sql = "SELECT file_id FROM file";

        // execute the SQL statement and return the results.
        return DBUtils.selectStringList(con, sql);
    }

    /**
     * Method for retrieving all the ReplicaFileInfo GUIDs within the database.
     *
     * @param con An open connection to the archive database
     * @return A list of the replicafileinfo_guid for all entries in the replicafileinfo table.
     */
    protected static List<String> retrieveIdsFromReplicaFileInfoTable(Connection con) {
        // Make SQL statement for retrieving the replicafileinfo_guids in the
        // replicafileinfo table.
        final String sql = "SELECT replicafileinfo_guid FROM replicafileinfo";

        // execute the SQL statement and return the results.
        return DBUtils.selectStringList(con, sql);
    }

    /**
     * Retrieves the file_id for the corresponding filename. An error is thrown if no such file_id is found in the file
     * table, and if more than one instance with the given name is found, then a warning is issued. If more than one is
     * found, then it is logged and only the first is returned.
     *
     * @param filename The entry in the filename list where the corresponding file_id should be found.
     * @param con An open connection to the archive database
     * @return The file_id for the file, or -1 if the file was not found.
     */
    protected static long retrieveIdForFile(String filename, Connection con) {
        // retrieve the file_id of the entry in the file table with the
        // filename filename.
        final String sql = "SELECT file_id FROM file WHERE filename = ?";
        List<Long> files = DBUtils.selectLongList(con, sql, filename);

        switch (files.size()) {
        // if no such file within the database, then return negative value.
        case 0:
            return -1;
        case 1:
            return files.get(0);
            // if more than one file, then log it and return the first found.
        default:
            log.warn("Only one entry in the file table for the name '{}' was expected, but {} was found. "
                    + "The first element is returned.", filename, files.size());
            return files.get(0);
        }
    }

    /**
     * Method for retrieving the replicafileinfo_guid for a specific instance defined from the fileId and the replicaId.
     * If more than one is found, then it is logged and only the first is returned.
     *
     * @param fileId The identifier for the file.
     * @param replicaId The identifier for the replica.
     * @param con An open connection to the archive database
     * @return The identifier for the replicafileinfo, or -1 if not found.
     */
    protected static long retrieveReplicaFileInfoGuid(long fileId, String replicaId, Connection con) {
        // sql for retrieving the replicafileinfo_guid.
        final String sql = "SELECT replicafileinfo_guid FROM replicafileinfo WHERE file_id = ? AND replica_id = ?";
        List<Long> result = DBUtils.selectLongList(con, sql, fileId, replicaId);

        // Handle the different cases for count.
        switch (result.size()) {
        case 0:
            return -1;
        case 1:
            return result.get(0);
        default:
            log.warn(
                    "More than one replicafileinfo with the file id '{}' from replica '{}': {}. The first result returned.",
                    fileId, replicaId, result);
            return result.get(0);
        }
    }

    /**
     * Method for retrieving the list of all the replicafileinfo_guids for a specific replica.
     *
     * @param replicaId The id for the replica to contain the files.
     * @param con An open connection to the archiveDatabase.
     * @return The list of all the replicafileinfo_guid.
     */
    protected static Set<Long> retrieveReplicaFileInfoGuidsForReplica(String replicaId, Connection con) {
        // sql for retrieving the replicafileinfo_guids for the replica.
        final String sql = "SELECT replicafileinfo_guid FROM replicafileinfo "
                + "WHERE replica_id = ? ORDER BY replicafileinfo_guid";
        return DBUtils.selectLongSet(con, sql, replicaId);
    }

    /**
     * Method for retrieving the replica type for a specific replica.
     *
     * @param replicaId The id of the replica.
     * @param con An open connection to the archiveDatabase.
     * @return The type of the replica.
     */
    protected static ReplicaType retrieveReplicaType(String replicaId, Connection con) {
        // The SQL statement for retrieving the replica_type of a replica with
        // the given replica id.
        final String sql = "SELECT replica_type FROM replica WHERE replica_id = ?";
        return ReplicaType.fromOrdinal(DBUtils.selectIntValue(con, sql, replicaId));
    }

    /**
     * Method for retrieving the list of replica, where the file with the given name has the checksum_status 'OK'.
     *
     * @param filename The name of the file.
     * @param con An open connection to the archive database
     * @return The list of replicas where the status for the checksum of the file is OK.
     */
    protected static List<String> retrieveReplicaIdsWithOKChecksumStatus(String filename, Connection con) {
        // The SQL statement to retrieve the replica_id for the entries in the
        // replicafileinfo table for the given fileId and checksum_status = OK
        // FIXME Use joins
        final String sql = "SELECT replica_id FROM replicafileinfo, file "
                + "WHERE replicafileinfo.file_id = file.file_id AND file.filename = ? AND checksum_status = ?";
        return DBUtils.selectStringList(con, sql, filename, ChecksumStatus.OK.ordinal());
    }

    /**
     * Method for retrieving the filename from the entry in the file table which has the fileId as file_id.
     *
     * @param fileId The file_id of the entry in the file table for which to retrieve the filename.
     * @param con An open connection to the archive database
     * @return The filename corresponding to the fileId in the file table.
     */
    protected static String retrieveFilenameForFileId(long fileId, Connection con) {
        // The SQL statement to retrieve the filename for a given file_id
        final String sql = "SELECT filename FROM file WHERE file_id = ?";
        return DBUtils.selectStringValue(con, sql, fileId);
    }

    /**
     * Method for retrieving the filelist_status for the entry in the replicafileinfo table associated with the given
     * filename for the replica identified with a given id.
     *
     * @param filename the filename of the file for which you want a status.
     * @param replicaId The identifier of the replica
     * @param con An open connection to the archive database
     * @return The above mentioned filelist_status of the file
     */
    protected static int retrieveFileListStatusFromReplicaFileInfo(String filename, String replicaId, Connection con) {
        // The SQL statement to retrieve the filelist_status for the given
        // entry in the replica fileinfo table.
        // FIXME Use joins
        final String sql = "SELECT filelist_status FROM replicafileinfo, file "
                + "WHERE file.file_id = replicafileinfo.file_id AND file.filename=? AND replica_id=?";
        return DBUtils.selectIntValue(con, sql, filename, replicaId);
    }

    /**
     * This is used for updating a replicafileinfo instance based on the results of a checksumjob. Updates the following
     * fields for the entry in the replicafileinfo: <br/>
     * - checksum = checksum argument. <br/>
     * - upload_status = completed. <br/>
     * - filelist_status = ok. <br/>
     * - checksum_status = UNKNOWN. <br/>
     * - checksum_checkdatetime = now. <br/>
     * - filelist_checkdatetime = now.
     *
     * @param replicafileinfoId The unique id for the replicafileinfo.
     * @param checksum The new checksum for the entry.
     * @param con An open connection to the archive database
     */
    protected static void updateReplicaFileInfoChecksum(long replicafileinfoId, String checksum, Connection con) {
        PreparedStatement statement = null;
        try {
            // The SQL statement
            final String sql = "UPDATE replicafileinfo SET checksum = ?, upload_status = ?, filelist_status = ?,"
                    + " checksum_status = ?, checksum_checkdatetime = ?, filelist_checkdatetime = ? "
                    + "WHERE replicafileinfo_guid = ?";

            Date now = new Date(Calendar.getInstance().getTimeInMillis());

            // complete the SQL statement.
            statement = DBUtils.prepareStatement(con, sql, checksum, ReplicaStoreState.UPLOAD_COMPLETED.ordinal(),
                    FileListStatus.OK.ordinal(), ChecksumStatus.UNKNOWN.ordinal(), now, now, replicafileinfoId);

            // execute the SQL statement
            statement.executeUpdate();
            con.commit();
        } catch (Exception e) {
            String msg = "Problems updating the replicafileinfo.";
            log.warn(msg);
            throw new IOFailure(msg, e);
        } finally {
            DBUtils.closeStatementIfOpen(statement);
        }
    }

    /**
     * Method for updating the filelist of a replicafileinfo instance. Updates the following fields for the entry in the
     * replicafileinfo: <br/>
     * filelist_status = OK. <br/>
     * filelist_checkdatetime = current time.
     *
     * @param replicafileinfoId The id of the replicafileinfo.
     * @param con An open connection to the archive database
     */
    protected static void updateReplicaFileInfoFilelist(long replicafileinfoId, Connection con) {
        PreparedStatement statement = null;
        try {
            // The SQL statement
            final String sql = "UPDATE replicafileinfo SET filelist_status = ?, filelist_checkdatetime = ? "
                    + "WHERE replicafileinfo_guid = ?";

            Date now = new Date(Calendar.getInstance().getTimeInMillis());

            // complete the SQL statement.
            statement = DBUtils.prepareStatement(con, sql, FileListStatus.OK.ordinal(), now, replicafileinfoId);

            // execute the SQL statement
            statement.executeUpdate();
            con.commit();
        } catch (Exception e) {
            String msg = "Problems updating the replicafileinfo.";
            log.warn(msg);
            throw new IOFailure(msg, e);
        } finally {
            DBUtils.closeStatementIfOpen(statement);
        }
    }

    /**
     * Method for updating the filelist of a replicafileinfo instance. Updates the following fields for the entry in the
     * replicafileinfo: <br/>
     * filelist_status = missing. <br/>
     * filelist_checkdatetime = current time.
     * <p>
     * The replicafileinfo is in the filelist.
     *
     * @param replicafileinfoId The id of the replicafileinfo.
     * @param con An open connection to the archive database
     */
    protected static void updateReplicaFileInfoMissingFromFilelist(long replicafileinfoId, Connection con) {
        PreparedStatement statement = null;
        try {
            // The SQL statement
            final String sql = "UPDATE replicafileinfo "
                    + "SET filelist_status = ?, filelist_checkdatetime = ?, upload_status = ? "
                    + "WHERE replicafileinfo_guid = ?";

            Date now = new Date(Calendar.getInstance().getTimeInMillis());

            // complete the SQL statement.
            statement = DBUtils.prepareStatement(con, sql, FileListStatus.MISSING.ordinal(), now,
                    ReplicaStoreState.UPLOAD_FAILED.ordinal(), replicafileinfoId);

            // execute the SQL statement
            statement.executeUpdate();
            con.commit();
        } catch (Exception e) {
            String msg = "Problems updating the replicafileinfo.";
            log.warn(msg);
            throw new IOFailure(msg, e);
        } finally {
            DBUtils.closeStatementIfOpen(statement);
        }
    }

    /**
     * Method for updating the checksum status of a replicafileinfo instance. Updates the following fields for the entry
     * in the replicafileinfo: <br/>
     * checksum_status = CORRUPT. <br/>
     * checksum_checkdatetime = current time.
     * <p>
     * The replicafileinfo is in the filelist.
     *
     * @param replicafileinfoId The id of the replicafileinfo.
     * @param con An open connection to the archive database
     */
    protected static void updateReplicaFileInfoChecksumCorrupt(long replicafileinfoId, Connection con) {
        PreparedStatement statement = null;
        try {
            // The SQL statement
            final String sql = "UPDATE replicafileinfo "
                    + "SET checksum_status = ?, checksum_checkdatetime = ?, upload_status = ? "
                    + "WHERE replicafileinfo_guid = ?";

            Date now = new Date(Calendar.getInstance().getTimeInMillis());

            // complete the SQL statement.
            statement = DBUtils.prepareStatement(con, sql, ChecksumStatus.CORRUPT.ordinal(), now,
                    ReplicaStoreState.UPLOAD_FAILED.ordinal(), replicafileinfoId);

            // execute the SQL statement
            statement.executeUpdate();
            con.commit();
        } catch (Exception e) {
            String msg = "Problems updating the replicafileinfo.";
            log.warn(msg);
            throw new IOFailure(msg, e);
        } finally {
            DBUtils.closeStatementIfOpen(statement);
        }
    }

    /**
     * Retrieve the guid stored for a filename on a given replica.
     *
     * @param filename a given filename
     * @param replicaId An identifier for a replica.
     * @param con An open connection to the archive database
     * @return the abovementioned guid.
     */
    protected static long retrieveGuidForFilenameOnReplica(String filename, String replicaId, Connection con) {
        // sql for retrieving the replicafileinfo_guid.
        // FIXME Use joins
        final String sql = "SELECT replicafileinfo_guid FROM replicafileinfo, file "
                + "WHERE replicafileinfo.file_id = file.file_id AND file.filename = ? AND replica_id = ?";
        List<Long> result = DBUtils.selectLongList(con, sql, filename, replicaId);
        return result.get(0);
    }

    /**
     * Method for updating the checksum status of a replicafileinfo instance. Updates the following fields for the entry
     * in the replicafileinfo: <br/>
     * checksum_status = UNKNOWN. <br/>
     * checksum_checkdatetime = current time.
     * <p>
     * The replicafileinfo is in the filelist.
     *
     * @param replicafileinfoId The id of the replicafileinfo.
     * @param con An open connection to the archive database
     */
    protected static void updateReplicaFileInfoChecksumUnknown(long replicafileinfoId, Connection con) {
        PreparedStatement statement = null;
        try {
            // The SQL statement
            final String sql = "UPDATE replicafileinfo SET checksum_status = ?, checksum_checkdatetime = ? "
                    + "WHERE replicafileinfo_guid = ?";

            Date now = new Date(Calendar.getInstance().getTimeInMillis());

            // complete the SQL statement.
            statement = DBUtils.prepareStatement(con, sql, ChecksumStatus.UNKNOWN.ordinal(), now, replicafileinfoId);

            // execute the SQL statement
            statement.executeUpdate();
            con.commit();
        } catch (Exception e) {
            String msg = "Problems updating the replicafileinfo.";
            log.warn(msg);
            throw new IOFailure(msg, e);
        } finally {
            DBUtils.closeStatementIfOpen(statement);
        }
    }

    /**
     * Method for updating the checksum status of a replicafileinfo instance. Updates the following fields for the entry
     * in the replicafileinfo: <br/>
     * checksum_status = OK. <br/>
     * upload_status = UPLOAD_COMLPETE. <br/>
     * checksum_checkdatetime = current time. <br/>
     * <br/>
     * The file is required to exist in the replica.
     *
     * @param replicafileinfoId The id of the replicafileinfo.
     * @param con An open connection to the archive database
     */
    protected static void updateReplicaFileInfoChecksumOk(long replicafileinfoId, Connection con) {
        PreparedStatement statement = null;
        try {
            // The SQL statement
            final String sql = "UPDATE replicafileinfo "
                    + "SET checksum_status = ?, checksum_checkdatetime = ?, upload_status = ? "
                    + "WHERE replicafileinfo_guid = ?";
            Date now = new Date(Calendar.getInstance().getTimeInMillis());

            // complete the SQL statement.
            statement = DBUtils.prepareStatement(con, sql, ChecksumStatus.OK.ordinal(), now,
                    ReplicaStoreState.UPLOAD_COMPLETED.ordinal(), replicafileinfoId);

            // execute the SQL statement
            statement.executeUpdate();
            con.commit();
        } catch (Exception e) {
            String msg = "Problems updating the replicafileinfo.";
            log.warn(msg);
            throw new IOFailure(msg, e);
        } finally {
            DBUtils.closeStatementIfOpen(statement);
        }
    }

    /**
     * Method for updating the checksum_updated field for a given replica in the replica table. This is called when a
     * checksum_job has been handled.
     * <p>
     * The following fields for the entry in the replica table: <br/>
     * checksum_updated = now.
     *
     * @param rep The replica which has just been updated.
     * @param con An open connection to the archive database
     */
    protected static void updateChecksumDateForReplica(Replica rep, Connection con) {
        PreparedStatement statement = null;
        try {
            Date now = new Date(Calendar.getInstance().getTimeInMillis());
            final String sql = "UPDATE replica SET checksum_updated = ? WHERE replica_id = ?";
            statement = DBUtils.prepareStatement(con, sql, now, rep.getId());
            statement.executeUpdate();
            con.commit();
        } catch (Exception e) {
            String msg = "Cannot update the checksum_updated for replica '" + rep + "'.";
            log.warn(msg);
            throw new IOFailure(msg, e);
        } finally {
            DBUtils.closeStatementIfOpen(statement);
        }
    }

    /**
     * Method for updating the filelist_updated field for a given replica in the replica table. This is called when a
     * filelist_job or a checksum_job has been handled.
     * <p>
     * The following fields for the entry in the replica table: <br/>
     * filelist_updated = now.
     *
     * @param rep The replica which has just been updated.
     * @param connection An open connection to the archive database
     */
    protected static void updateFilelistDateForReplica(Replica rep, Connection connection) {
        PreparedStatement statement = null;
        try {
            Date now = new Date(Calendar.getInstance().getTimeInMillis());
            final String sql = "UPDATE replica SET filelist_updated = ? WHERE replica_id = ?";
            statement = DBUtils.prepareStatement(connection, sql, now, rep.getId());
            statement.executeUpdate();
            connection.commit();
        } catch (Exception e) {
            String msg = "Cannot update the filelist_updated for replica '" + rep + "'.";
            log.warn(msg);
            throw new IOFailure(msg, e);
        } finally {
            DBUtils.closeStatementIfOpen(statement);
        }
    }

    /**
     * Method for setting the filelist_updated field for a given replica in the replica table to a specified value. This
     * is only called when the admin.data is converted.
     * <p>
     * The following fields for the entry in the replica table: <br/>
     * filelist_updated = date.
     *
     * @param rep The replica which has just been updated.
     * @param date The date for the last filelist update.
     * @param con An open connection to the archive database
     */
    protected static void setFilelistDateForReplica(Replica rep, Date date, Connection con) {
        PreparedStatement statement = null;
        try {
            final String sql = "UPDATE replica SET filelist_updated = ? WHERE replica_id = ?";
            statement = DBUtils.prepareStatement(con, sql, date, rep.getId());
            statement.executeUpdate();
            con.commit();
        } catch (Exception e) {
            String msg = "Cannot update the filelist_updated for replica '" + rep + "'.";
            log.warn(msg);
            throw new IOFailure(msg, e);
        } finally {
            DBUtils.closeStatementIfOpen(statement);
        }
    }

    /**
     * Method for setting the checksum_updated field for a given replica in the replica table to a specified value. This
     * is only called when the admin.data is converted.
     * <p>
     * The following fields for the entry in the replica table: <br/>
     * checksum_updated = date.
     *
     * @param rep The replica which has just been updated.
     * @param date The date for the last checksum update.
     * @param con An open connection to the archive database
     */
    protected static void setChecksumlistDateForReplica(Replica rep, Date date, Connection con) {
        PreparedStatement statement = null;
        try {
            final String sql = "UPDATE replica SET checksum_updated = ? WHERE replica_id = ?";
            statement = DBUtils.prepareStatement(con, sql, date, rep.getId());
            statement.executeUpdate();
            con.commit();
        } catch (Exception e) {
            String msg = "Cannot update the filelist_updated for replica '" + rep + "'.";
            log.warn(msg);
            throw new IOFailure(msg, e);
        } finally {
            DBUtils.closeStatementIfOpen(statement);
        }
    }

    /**
     * Method for testing whether a replica already is within the database.
     *
     * @param rep The replica to find in the database.
     * @param con An open connection to the archive database
     * @return Whether the replica is found in the database.
     */
    protected static boolean existsReplicaInDB(Replica rep, Connection con) {
        // retrieve the amount of times this replica is within the database.
        final String sql = "SELECT COUNT(*) FROM replica WHERE replica_id = ?";
        int count = DBUtils.selectIntValue(con, sql, rep.getId());

        // Handle the different cases for count.
        switch (count) {
        case 0:
            return false;
        case 1:
            return true;
        default:
            throw new IOFailure("Cannot handle " + count + " replicas " + "with id '" + rep.getId() + "'.");
        }
    }

    /**
     * Method for retrieving ReplicaFileInfo entry in the database.
     *
     * @param replicaFileInfoGuid The guid for the specific replicafileinfo.
     * @param con An open connection to the archive database
     * @return The replicafileinfo.
     */
    protected static ReplicaFileInfo getReplicaFileInfo(long replicaFileInfoGuid, Connection con) {
        // retrieve all
        final String sql = "SELECT replicafileinfo_guid, replica_id, file_id, segment_id, checksum, upload_status, "
                + "filelist_status, checksum_status, filelist_checkdatetime, checksum_checkdatetime "
                + "FROM replicafileinfo WHERE replicafileinfo_guid = ?";

        PreparedStatement s = null;

        try {
            s = DBUtils.prepareStatement(con, sql, replicaFileInfoGuid);
            ResultSet res = s.executeQuery();
            res.next();

            // return the corresponding replica file info.
            return new ReplicaFileInfo(res);
        } catch (SQLException e) {
            final String message = "SQL error while selecting ResultsSet by executing statement '" + sql + "'.";
            log.warn(message, e);
            throw new IOFailure(message, e);
        } finally {
            DBUtils.closeStatementIfOpen(s);
        }
    }

    /**
     * Method for retrieving the data for the wanted entries in the replicafileinfo table. All the replicafileinfo
     * entries with no checksum defined is ignored.
     *
     * @param rfiGuids The list of guids for the entries in the replicafileinfo table which is wanted.
     * @param con An open connection to the archive database
     * @return The complete data for these entries in the replicafileinfo table.
     */
    protected static List<ReplicaFileInfo> retrieveReplicaFileInfosWithChecksum(List<Long> rfiGuids, Connection con) {
        ArrayList<ReplicaFileInfo> result = new ArrayList<ReplicaFileInfo>();

        // Extract all the replicafileinfos, but only put the entries with a
        // non-empty checksum into the result list.
        for (long rfiGuid : rfiGuids) {
            ReplicaFileInfo rfi = getReplicaFileInfo(rfiGuid, con);
            if (rfi.getChecksum() != null && !rfi.getChecksum().isEmpty()) {
                result.add(rfi);
            }
        }

        return result;
    }

    /**
     * Method for updating an entry in the replicafileinfo table. This method does not update the
     * 'checksum_checkdatetime' and 'filelist_checkdatetime'.
     *
     * @param replicafileinfoGuid The guid to update.
     * @param checksum The new checksum for the entry.
     * @param state The state for the upload.
     * @param con An open connection to the archive database
     * @throws IOFailure If an error occurs in the database connection.
     */
    protected static void updateReplicaFileInfo(long replicafileinfoGuid, String checksum, ReplicaStoreState state,
            Connection con) throws IOFailure {
        PreparedStatement statement = null;
        try {
            final String sql = "UPDATE replicafileinfo "
                    + "SET checksum = ?, upload_status = ?, filelist_status = ?, checksum_status = ? "
                    + "WHERE replicafileinfo_guid = ?";

            FileListStatus fls;
            ChecksumStatus cs;

            if (state == ReplicaStoreState.UPLOAD_COMPLETED) {
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
            statement = DBUtils.prepareStatement(con, sql, checksum, state.ordinal(), fls.ordinal(), cs.ordinal(),
                    replicafileinfoGuid);
            statement.executeUpdate();
            con.commit();
        } catch (Exception e) {
            String errMsg = "Problems with updating a ReplicaFileInfo";
            log.warn(errMsg);
            throw new IOFailure(errMsg, e);
        } finally {
            DBUtils.closeStatementIfOpen(statement);
        }
    }

    /**
     * Method for updating an entry in the replicafileinfo table. This method updates the 'checksum_checkdatetime' and
     * 'filelist_checkdatetime' with the given date argument.
     *
     * @param replicafileinfoGuid The guid to update.
     * @param checksum The new checksum for the entry.
     * @param date The date for the update.
     * @param state The status for the upload.
     * @param con An open connection to the archive database
     * @throws IOFailure If an error occurs in the connection to the database.
     */
    protected static void updateReplicaFileInfo(long replicafileinfoGuid, String checksum, Date date,
            ReplicaStoreState state, Connection con) throws IOFailure {
        PreparedStatement statement = null;
        try {
            final String sql = "UPDATE replicafileinfo "
                    + "SET checksum = ?, upload_status = ?, filelist_status = ?, checksum_status = ?, "
                    + "checksum_checkdatetime = ?, " + "filelist_checkdatetime = ? WHERE replicafileinfo_guid = ?";

            FileListStatus fls;
            ChecksumStatus cs;

            if (state == ReplicaStoreState.UPLOAD_COMPLETED) {
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
            statement = DBUtils.prepareStatement(con, sql, checksum, state.ordinal(), fls.ordinal(), cs.ordinal(),
                    date, date, replicafileinfoGuid);
            statement.executeUpdate();
            con.commit();
        } catch (Throwable e) {
            String errMsg = "Problems with updating a ReplicaFileInfo";
            log.warn(errMsg);
            throw new IOFailure(errMsg);
        } finally {
            DBUtils.closeStatementIfOpen(statement);
        }
    }

    /**
     * Retrieves the UploadStatus for a specific entry in the replicafileinfo table identified by the file guid and the
     * replica id.
     *
     * @param fileGuid The id of the file.
     * @param repId The id of the replica.
     * @param con An open connection to the archive database
     * @return The upload status of the corresponding replicafileinfo entry.
     */
    protected static ReplicaStoreState retrieveUploadStatus(long fileGuid, String repId, Connection con) {
        // sql query for retrieval of upload status for a specific entry.
        final String sql = "SELECT upload_status FROM replicafileinfo WHERE file_id = ? AND replica_id = ?";
        int us = DBUtils.selectIntValue(con, sql, fileGuid, repId);
        return ReplicaStoreState.fromOrdinal(us);
    }

    /**
     * Retrieves the checksum for a specific entry in the replicafileinfo table identified by the file guid and the
     * replica id.
     *
     * @param fileGuid The guid of the file in the file table.
     * @param repId The id of the replica.
     * @param con An open connection to the archive database
     * @return The checksum of the corresponding replicafileinfo entry.
     */
    protected static String retrieveChecksumForReplicaFileInfoEntry(long fileGuid, String repId, Connection con) {
        // sql query for retrieval of checksum value for an specific entry.
        final String sql = "SELECT checksum FROM replicafileinfo WHERE file_id = ? AND replica_id = ?";
        return DBUtils.selectStringValue(con, sql, fileGuid, repId);
    }

    /**
     * Retrieves the checksum status for a specific entry in the replicafileinfo table identified by the file guid and
     * the replica id.
     *
     * @param fileGuid The guid of the file in the file table.
     * @param repId The id of the replica.
     * @param con An open connection to the archive database
     * @return The checksum status of the corresponding replicafileinfo entry.
     */
    protected static ChecksumStatus retrieveChecksumStatusForReplicaFileInfoEntry(long fileGuid, String repId,
            Connection con) {
        // sql query for retrieval of checksum value for an specific entry.
        String sql = "SELECT checksum_status FROM replicafileinfo WHERE file_id = ? AND replica_id = ?";
        // retrieve the ordinal for the checksum status.
        int statusOrdinal = DBUtils.selectIntValue(con, sql, fileGuid, repId);
        // return the checksum corresponding to the ordinal.
        return ChecksumStatus.fromOrdinal(statusOrdinal);
    }

    /**
     * Method for finding the checksum which is present most times in the list.
     *
     * @param checksums The list of checksum to vote about.
     * @return The most common checksum, or null if several exists.
     */
    protected static String vote(List<String> checksums) {
        log.debug("voting for checksums: {}", checksums.toString());

        // count the occurrences of each unique checksum.
        Map<String, Integer> csMap = new HashMap<String, Integer>();
        for (String cs : checksums) {
            if (csMap.containsKey(cs)) {
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
        for (Map.Entry<String, Integer> entry : csMap.entrySet()) {
            if (entry.getValue() > largestCount) {
                largestCount = entry.getValue();
                checksum = entry.getKey();
                unique = true;
            } else if (entry.getValue() == largestCount) {
                unique = false;
            }
        }

        // if not unique, then log an error and return null!
        if (!unique) {
            log.error("No checksum has the most occurrences in '{}'. A null has been returned!", csMap);
            return null;
        }

        return checksum;
    }

    /**
     * The method for voting about the checksum of a file. <br/>
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
     *
     * @param fileId The id for the file to vote about.
     * @param con An open connection to the archive database
     */
    protected static void fileChecksumVote(long fileId, Connection con) {
        // Get all the replicafileinfo instances for the fileid, though
        // only the ones which have a valid checksum.
        // Check the checksums against each other if they differ,
        // then set to CORRUPT.
        final String sql = "SELECT replicafileinfo_guid FROM replicafileinfo WHERE file_id = ?";
        List<Long> rfiGuids = DBUtils.selectLongList(con, sql, fileId);

        List<ReplicaFileInfo> rfis = retrieveReplicaFileInfosWithChecksum(rfiGuids, con);

        // handle the case, when no replicas has a checksum of the file.
        if (rfis.size() == 0) {
            // issue a warning.
            log.warn("No replicas contains a valid version of the file '{}'.", retrieveFilenameForFileId(fileId, con));

            return;
        }

        // Put all the checksums into a hash set to obtain a set of
        // unique checksums.
        Set<String> hs = new HashSet<String>(rfis.size());
        for (ReplicaFileInfo rfi : rfis) {
            // only accept those files which can be found.
            if (rfi.getFileListState() == FileListStatus.OK) {
                hs.add(rfi.getChecksum());
            }
        }

        // handle the unlikely case, where the file is missing from everywhere!
        if (hs.size() == 0) {
            String errorMsg = "The file '" + retrieveFilenameForFileId(fileId, con) + "' is missing in all replicas";
            log.warn(errorMsg);
            NotificationsFactory.getInstance().notify(errorMsg, NotificationType.WARNING);

            return;
        }

        // if at exactly one unique checksum is found, then no irregularities
        // among the checksums are found.
        if (hs.size() == 1) {
            log.trace("No irregularities found for the file with id '{}'.", fileId);

            // Tell all the replicafileinfo entries that their checksum
            // is ok
            for (ReplicaFileInfo rfi : rfis) {
                // only set OK for those replica where the file is.
                if (rfi.getFileListState() == FileListStatus.OK) {
                    updateReplicaFileInfoChecksumOk(rfi.getGuid(), con);
                }
            }

            // go to next entry in the file table.
            return;
        }

        // Make a list of the checksums for voting.
        List<String> checksums = new ArrayList<String>();
        for (ReplicaFileInfo rfi : rfis) {
            if (rfi.getFileListState() == FileListStatus.OK) {
                checksums.add(rfi.getChecksum());
            }
        }

        // vote to find the unique most common checksum (null if no unique).
        String uniqueChecksum = vote(checksums);

        if (uniqueChecksum != null) {
            // change checksum_status to CORRUPT for the replicafileinfo
            // which
            // does not have the chosen checksum.
            // Set the others replicafileinfo entries to OK.
            for (ReplicaFileInfo rfi : rfis) {
                if (!rfi.getChecksum().equals(uniqueChecksum)) {
                    updateReplicaFileInfoChecksumCorrupt(rfi.getGuid(), con);
                } else {
                    updateReplicaFileInfoChecksumOk(rfi.getGuid(), con);
                }
            }
        } else {
            // Handle the case, when no checksum has most votes.
            String errMsg = "There is no winner of the votes between the replicas for the checksum of file '"
                    + retrieveFilenameForFileId(fileId, con) + "'.";
            log.warn(errMsg);

            // send a notification
            NotificationsFactory.getInstance().notify(errMsg, NotificationType.WARNING);

            // set all replicafileinfo entries to unknown
            for (ReplicaFileInfo rfi : rfis) {
                updateReplicaFileInfoChecksumUnknown(rfi.getGuid(), con);
            }
        }
    }

    /**
     * Add information about one file in a given replica.
     *
     * @param file The name of a file
     * @param replica A replica
     * @param con An open connection to the ArchiveDatabase
     * @return the ReplicaFileInfo ID for the given filename and replica in the database
     */
    protected static long addFileInformation(String file, Replica replica, Connection con) {
        // retrieve the file_id for the file.
        long fileId = ReplicaCacheHelpers.retrieveIdForFile(file, con);
        // If not found, log and create the file in the database.
        if (fileId < 0) {
            log.info("The file '{}' was not found in the database. Thus creating entry for the file.", file);
            // insert the file and retrieve its file_id.
            fileId = ReplicaCacheHelpers.insertFileIntoDB(file, con);
        }

        // retrieve the replicafileinfo_guid for this entry.
        long rfiId = ReplicaCacheHelpers.retrieveReplicaFileInfoGuid(fileId, replica.getId(), con);
        // if not found log and create the replicafileinfo in the database.
        if (rfiId < 0) {
            log.warn("Cannot find the file '{}' for replica '{}'. Thus creating missing entry before updating.", file,
                    replica.getId());
            ReplicaCacheHelpers.createReplicaFileInfoEntriesInDB(fileId, con);
        }

        // update the replicafileinfo of this file:
        // filelist_checkdate, filelist_status, upload_status
        ReplicaCacheHelpers.updateReplicaFileInfoFilelist(rfiId, con);

        return rfiId;
    }

    /**
     * Process checksum information about one file in a given replica. and update the database accordingly.
     *
     * @param filename The name of a file
     * @param checksum The checksum of that file.
     * @param replica A replica
     * @param con An open connection to the ArchiveDatabase
     * @return the ReplicaFileInfo ID for the given filename and replica in the database
     */
    public static long processChecksumline(String filename, String checksum, Replica replica, Connection con) {

        // The ID for the file.
        long fileid = -1;

        // If the file is not within DB, then insert it.
        int count = DBUtils.selectIntValue(con, "SELECT COUNT(*) FROM file WHERE filename = ?", filename);

        if (count == 0) {
            log.info("Inserting the file '{}' into the database.", filename);
            fileid = ReplicaCacheHelpers.insertFileIntoDB(filename, con);
        } else {
            fileid = ReplicaCacheHelpers.retrieveIdForFile(filename, con);
        }

        // If the file does not already exists in the database, create it
        // and retrieve the new ID.
        if (fileid < 0) {
            log.warn("Inserting the file '{}' into the database, again: This should never happen!!!", filename);
            fileid = ReplicaCacheHelpers.insertFileIntoDB(filename, con);
        }

        // Retrieve the replicafileinfo for the file at the replica.
        long rfiId = ReplicaCacheHelpers.retrieveReplicaFileInfoGuid(fileid, replica.getId(), con);

        // Check if there already is an entry in the replicafileinfo table.
        // rfiId is negative if no entry was found.
        if (rfiId < 0) {
            // insert the file into the table.
            ReplicaCacheHelpers.createReplicaFileInfoEntriesInDB(fileid, con);
            log.info("Inserted file '{}' for replica '{}' into replicafileinfo.", filename, replica.toString());
        }

        // Update this table
        ReplicaCacheHelpers.updateReplicaFileInfoChecksum(rfiId, checksum, con);
        log.trace("Updated file '{}' for replica '{}' into replicafileinfo.", filename, replica.toString());

        return rfiId;
    }

}
