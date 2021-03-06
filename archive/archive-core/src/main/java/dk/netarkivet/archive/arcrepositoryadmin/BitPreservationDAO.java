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
import java.sql.Date;

import dk.netarkivet.common.distribute.arcrepository.Replica;
import dk.netarkivet.common.utils.CleanupIF;

/**
 * This is an interface for communicating with bitpreservation databases.
 */
public interface BitPreservationDAO extends CleanupIF {
    /**
     * Given the output of a checksum job, add the results to the database. NOTE: the Checksum version of Replica must
     * be implemented with output in the same form as checksumJobOutput for implementation of bitArchive replicas
     *
     * @param checksumOutput The parsed output of a GetAllChecksumMessage as a File with ChecksumJob lines, i.e.
     * filename##checksum.
     * @param replica The replica this checksum job is for.
     */
    void addChecksumInformation(File checksumOutput, Replica replica);

    /**
     * Given the output of a file list job, add the results to the database. NOTE: the Checksum version of Replica must
     * be implemented with output in the same form as filelistJobOutput for implementation of bitArchive replicas
     *
     * @param filelistOutput A file with a list of filenames for the given replica.
     * @param replica The replica this filelist job is for.
     */
    void addFileListInformation(File filelistOutput, Replica replica);

    /**
     * Return files with upload_status = COMPLETE for the replica, but the filelist_status = MISSING. This is done by
     * querying the database for files with no or different update date from the last known update date for bitarchive,
     * but which are present from admin data.
     *
     * @param replica The replica to check for.
     * @return The list of missing files for a specific replica.
     */
    Iterable<String> getMissingFilesInLastUpdate(Replica replica);

    /**
     * Return files with filelist_status CORRUPT for the replica, but not present in the last missing files job. This is
     * done by querying the database for files with different checksum from the checksum in the last known update date
     * for bitarchive, but which are present from admin data.
     *
     * @param replica The replica to check for.
     * @return The list of wrong files for the replica in the last update.
     */
    Iterable<String> getWrongFilesInLastUpdate(Replica replica);

    /**
     * Return the count of missing files for replica.
     *
     * @param replica The replica to get the count for.
     * @return The count of missing files for a replica.
     */
    long getNumberOfMissingFilesInLastUpdate(Replica replica);

    /**
     * Return the count of corrupt files for replica.
     *
     * @param replica The replica to get the count for.
     * @return The number of wrong files for a replica.
     */
    long getNumberOfWrongFilesInLastUpdate(Replica replica);

    /**
     * Returns the count of files in the replica which is not missing.
     *
     * @param replica The replica to have the files.
     * @return The number of files, which does not have filelist_status = MISSING.
     */
    long getNumberOfFiles(Replica replica);

    /**
     * Get the date for the last file list job.
     *
     * @param replica The replica to get the date for.
     * @return The date of the last missing files update for the replica.
     */
    Date getDateOfLastMissingFilesUpdate(Replica replica);

    /**
     * Get the date for the last file list job.
     *
     * @param replica The replica to get the date for.
     * @return The date of the last wrong file update for the replica.
     */
    Date getDateOfLastWrongFilesUpdate(Replica replica);

    /**
     * Method for retrieving a replica which has the file and the checksum_status = OK.
     *
     * @param filename The name of the file.
     * @return A replica which contains the file, or null if no such replica can be found.
     */
    Replica getBitarchiveWithGoodFile(String filename);

    /**
     * Method for retrieving a replica which has the file and the checksum_status = OK.
     *
     * @param filename The name of the file.
     * @param badReplica A replica which is known to contain a corrupt instance of this file.
     * @return A replica which contains the file, or null if no such replica can be found.
     */
    Replica getBitarchiveWithGoodFile(String filename, Replica badReplica);

    /**
     * Method for updating the status for the files for all the replicas. If the checksums of the archives differ for
     * some replicas, then based on a checksum vote, a specific checksum is chosen as the 'correct' one, and the entries
     * with another checksum that this 'correct' one will be marked as corrupt.
     */
    void updateChecksumStatus();

    /**
     * Method for updating the status for a specific file for all the replicas. If the checksums for the replicas differ
     * for some replica, then based on a checksum vote, a specific checksum is chosen as the 'correct' one, and the
     * entries with another checksum than the 'correct one' will be marked as corrupt. If no winner of the voting is
     * found, the all instances will be chosen to have 'UNKNOWN' checksum status.
     *
     * @param filename The name of the file to update the status for.
     */
    void updateChecksumStatus(String filename);

    /**
     * Method for retrieving the entry in the replicafileinfo table for a given file and replica.
     *
     * @param filename The name of the file for the entry.
     * @param replica The replica of the entry.
     * @return The replicafileinfo entry corresponding to the given filename and replica.
     */
    ReplicaFileInfo getReplicaFileInfo(String filename, Replica replica);

    /**
     * Method for updating a specific entry in the replicafileinfo table.
     *
     * @param filename Name of the file.
     * @param checksum The checksum of the file.
     * @param replica The replica where the file exists.
     */
    void updateChecksumInformationForFileOnReplica(String filename, String checksum, Replica replica);

    /**
     * Method for cleaning up when done.
     */
    void cleanup();
}
