/*$Id$
* $Revision$
* $Date$
* $Author$
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
package dk.netarkivet.common.distribute.arcrepository;

import java.io.File;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.batch.FileBatchJob;

/**
 * Implements the Facade pattern to shield off the methods in
 * JMSArcRepositoryClient not to be used by the bit preservation system.
 */
public interface PreservationArcRepositoryClient  {
    /** Call on shutdown to release external resources. */
    void close();

    /**
     * Gets a single ARC record out of the ArcRepository.
     *
     * @param arcfile The name of a file containing the desired record.
     * @param index   The offset of the desired record in the file
     * @return a BitarchiveRecord-object, or null if request times out or object
     * is not found.
     * @exception ArgumentNotValid If the get operation failed.
     */
    BitarchiveRecord get(String arcfile, long index) throws ArgumentNotValid;

    /**
     * Retrieves a file from an ArcRepository and places it in a local file.

     * @param arcfilename Name of the arcfile to retrieve.
     * @param replica The bitarchive to retrieve the data from.
     * @param toFile Filename of a place where the file fetched can be put.
     * @throws IOFailure if there are problems getting a reply or the file
     * could not be found.
     */
    void getFile(String arcfilename, Replica replica, File toFile);

    /**
     * Store the given file in the ArcRepository.  After storing, the file is
     * deleted.
     *
     * @param file A file to be stored. Must exist.
     * @throws IOFailure thrown if store is unsuccesful, or failed to clean
     * up files after the store operation.
     * @throws ArgumentNotValid if file parameter is null or file is not an
     *                          existing file.
     */
    void store(File file) throws IOFailure, ArgumentNotValid;

    /**
     * Runs a batch batch job on each file in the ArcRepository.
     *
     * @param job An object that implements the FileBatchJob interface. The
     *  initialize() method will be called before processing and the finish()
     *  method will be called afterwards. The process() method will be called
     *  with each File entry.
     *
     * @param replicaId The archive to execute the job on.
     * @return The status of the batch job after it ended.
     */
    BatchStatus batch(FileBatchJob job, String replicaId);

    /** Updates the administrative data in the ArcRepository for a given
     * file and bitarchive replica.
     *
     * @param fileName The name of a file stored in the ArcRepository.
     * @param replicaId The id if the replica that the administrative
     * data for fileName is wrong for.
     * @param newval What the administrative data will be updated to.
     */
    void updateAdminData(String fileName, String replicaId,
                         ReplicaStoreState newval);

    /** Updates the checksum kept in the ArcRepository for a given
     * file. It is the responsibility of the ArcRepository implementation to
     * ensure that this checksum matches that of the underlying files.
     *
     * @param filename The name of a file stored in the ArcRepository.
     * @param checksum The new checksum.
     */
    void updateAdminChecksum(String filename, String checksum);

    /** Remove a file from one part of the ArcRepository, retrieveing a copy
     * for security purposes.  This is typically used when repairing a file
     * that has been corrupted.
     *
     * @param fileName The name of the file to remove.
     * @param replicaId The replica id from which to remove the file.
     * @param checksum The checksum of the file to be removed.
     * @param credentials A string that shows that the user is allowed to
     * perform this operation.
     * @return A local copy of the file removed.
     */
    File removeAndGetFile(String fileName, String replicaId,
                          String checksum, String credentials);
    
    /**
     * Retrieves all the checksum from the replica through a 
     * GetAllChecksumMessage.
     * 
     * This is the checksum archive alternative to running a ChecksumBatchJob. 
     * 
     * @param replicaId The id of the replica from which the checksums should 
     * be retrieved.
     * @return A list of ChecksumEntries which is the results of the 
     * GetAllChecksumMessage.
     * @see dk.netarkivet.archive.checksum.distribute.GetAllChecksumsMessage
     */
    File getAllChecksums(String replicaId);
    
    /**
     * Retrieves the checksum of a specific file.
     * 
     * This is the checksum archive alternative to running a ChecksumJob 
     * limited to a specific file.
     * 
     * @param replicaId The name of the replica to send the message.
     * @param filename The name of the file for whom the checksum should be
     * retrieved.
     * @return The checksum of the file in the replica. Or null if an 
     * error occurred.
     */
    String getChecksum(String replicaId, String filename);
    
    /**
     * Retrieves the names of all the files in the replica through a 
     * GetAllFilenamesMessage.
     * 
     * This is the checksum archive alternative to running a FilelistBatchJob. 
     * 
     * @param replicaId The id of the replica from which the list of filenames
     * should be retrieved.
     * @return A list of all the filenames within the archive of the given 
     * replica.
     * @see dk.netarkivet.archive.checksum.distribute.GetAllFilenamesMessage
     */
    File getAllFilenames(String replicaId);
    
    /**
     * Method for correcting a file in a replica.
     * 
     * This is the checksum archive method for correcting a file entry in the 
     * archive. The bitarchive uses 'removeAndGetFile' followed by a 'store'.
     * 
     * @param replicaId The identification of the replica.
     * @param file The new file to replace the old one.
     * @param credentials The password for allowing to remove a file entry in
     * the archive. 
     */
    void correct(String replicaId, String checksum, File file, 
	    String credentials);
}
