/*
 * #%L
 * Netarchivesuite - common
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
package dk.netarkivet.common.distribute.arcrepository;

import java.io.File;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.batch.BatchJob;
import dk.netarkivet.common.utils.batch.FileBatchJob;

/**
 * This is the most complete repository client as it requires storage, reading, and processing
 * methods as well as additonal methods appropriate to preservation actions.
 */
public interface PreservationArcRepositoryClient<J extends BatchJob, U extends UploadRepository, R extends ReaderRepository>
        extends AutoCloseable,
        ProcessorRepository<J>,
        ExceptionlessAutoCloseable,
        UploadRepository,
        ReaderRepository {

    /**
     * Updates the administrative data in the ArcRepository for a given file and bitarchive replica.
     *
     * @param fileName The name of a file stored in the ArcRepository.
     * @param replicaId The id if the replica that the administrative data for fileName is wrong for.
     * @param newval What the administrative data will be updated to.
     */
    @Deprecated
    void updateAdminData(String fileName, String replicaId, ReplicaStoreState newval);

    /**
     * Updates the checksum kept in the ArcRepository for a given file. It is the responsibility of the ArcRepository
     * implementation to ensure that this checksum matches that of the underlying files.
     *
     * @param filename The name of a file stored in the ArcRepository.
     * @param checksum The new checksum.
     */
    @Deprecated
    void updateAdminChecksum(String filename, String checksum);

    /**
     * Remove a file from one part of the ArcRepository, retrieving a copy for security purposes. This is typically used
     * when repairing a file that has been corrupted.
     *
     * @param fileName The name of the file to remove.
     * @param replicaId The replica id from which to remove the file.
     * @param checksum The checksum of the file to be removed.
     * @param credentials A string that shows that the user is allowed to perform this operation.
     * @return A local copy of the file removed.
     */
    @Deprecated
    File removeAndGetFile(String fileName, String replicaId, String checksum, String credentials);

    /**
     * Retrieves all the checksum from the replica through a GetAllChecksumMessage.
     * <p>
     * This is the checksum archive alternative to running a ChecksumBatchJob.
     *
     * @param replicaId The id of the replica from which the checksums should be retrieved.
     * @return A list of ChecksumEntries which is the results of the GetAllChecksumMessage.
     * @see .GetAllChecksumsMessage
     */
    @Deprecated
    File getAllChecksums(String replicaId);

    /**
     * Retrieves the checksum of a specific file.
     * <p>
     * This is the checksum archive alternative to running a ChecksumJob limited to a specific file.
     *
     * @param replicaId The name of the replica to send the message.
     * @param filename The name of the file for whom the checksum should be retrieved.
     * @return The checksum of the file in the replica. Or null if an error occurred.
     */
    @Deprecated
    String getChecksum(String replicaId, String filename);

    /**
     * Retrieves the names of all the files in the replica through a GetAllFilenamesMessage.
     * <p>
     * This is the checksum archive alternative to running a FilelistBatchJob.
     *
     * @param replicaId The id of the replica from which the list of filenames should be retrieved.
     * @return A list of all the filenames within the archive of the given replica.
     * see GetAllFilenamesMessage
     */
    @Deprecated
    File getAllFilenames(String replicaId);

    /**
     * Method for correcting a file in a replica.
     * <p>
     * This is the checksum archive method for correcting a file entry in the archive. The bitarchive uses
     * 'removeAndGetFile' followed by a 'store'.
     *
     * @param replicaId The identification of the replica.
     * @param checksum The checksum of the corrupt entry in the archive. It is important to validate that the checksum
     * actually is wrong before correcting the entry.
     * @param file The new file to replace the old one.
     * @param credentials The password for allowing to remove a file entry in the archive.
     * @return The corrupted file from the archive.
     */
    @Deprecated
    File correct(String replicaId, String checksum, File file, String credentials);

}
