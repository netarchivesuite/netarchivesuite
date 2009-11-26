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
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301 
 *  USA
 */
package dk.netarkivet.archive.arcrepository.bitpreservation;

import java.sql.Date;
import java.util.List;

import dk.netarkivet.common.distribute.arcrepository.Replica;
import dk.netarkivet.common.utils.CleanupIF;

/**
 * This is an interface for communicating with bitpreservation databases.
 */
public interface BitPreservationDAO extends CleanupIF {
    /** Given the output of a checksum job, add the results to the database.
     * NOTE: the Checksum version of Replica must be implemented with output
     *       in the same form as checksumJobOutput for implementation of
     *       bitArchive replicas
     *       
     * @param checksumOutput The parsed output of a checksum job or the output
     * from a GetAllChecksumMessage.
     * @param replica The replica this checksum job is for.
     */
    void addChecksumInformation(List<ChecksumEntry> checksumOutput, 
            Replica replica);
    
    /** Given the output of a file list job, add the results to the database.
     * NOTE: the Checksum version of Replica must be implemented with output
     *       in the same form as filelistJobOutput for implementation of
     *       bitArchive replicas
     *       
     * @param filelistOutput The list of filenames for the given replica.
     * @param replica The replica this filelist job is for.
     */
    void addFileListInformation(List<String> filelistOutput, Replica replica);

    /** Return files with upload_status = COMPLETE for the replica, but the
     * filelist_status = MISSING.
     * This is done by querying the database for files with no or different 
     * update date from the last known update date for bitarchive, but which 
     * are present from admin data.
     * 
     * @param replica The replica to check for.
     * @return The list of missing files for a specific replica.
     */
    Iterable<String> getMissingFilesInLastUpdate(Replica replica);

    /** Return files with filelist_status CORRUPT for the replica, but not 
     * present in the last missing files job.
     * This is done by querying the database for files with different checksum 
     * from the checksum in the last known update date for bitarchive, but 
     * which are present from admin data.
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
     * @return The number of files, which does not have 
     * filelist_status = MISSING.
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
     * Method for retrieving a replica which has the file and the 
     * checksum_status = OK.
     * 
     * @param filename The name of the file.
     * @return A replica which contains the file, or null if no such replica 
     * can be found.
     */
    Replica getBitarchiveWithGoodFile(String filename);

    /**
     * Method for retrieving a replica which has the file and the 
     * checksum_status = OK.
     * 
     * @param filename The name of the file.
     * @param badReplica A replica which is known to contain a corrupt instance
     * of this file.
     * @return A replica which contains the file, or null if no such replica 
     * can be found.
     */
    Replica getBitarchiveWithGoodFile(String filename, Replica badReplica);

    /**
     * Method for updating the status for the files for all the replicas.
     * If the checksums of the archives differ for some replicas, then based on
     * a checksum vote, a specific checksum is chosen as the 'correct' one, and
     * the entries with another checksum that this 'correct' one will be marked
     * as corrupt.
     */
    void updateChecksumStatus();
    
    /**
     * Method for cleaning up when done.
     */
    void cleanup();
}
