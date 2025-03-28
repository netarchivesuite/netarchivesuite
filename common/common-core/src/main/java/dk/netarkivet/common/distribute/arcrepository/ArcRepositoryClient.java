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
import dk.netarkivet.common.utils.batch.FileBatchJob;

/**
 * Generic interface defining all methods that an ArcRepository provides. Typically, an application using this will only
 * see one of the restricted superinterfaces.
 */
public interface ArcRepositoryClient extends HarvesterArcRepositoryClient, ViewerArcRepositoryClient,
        PreservationArcRepositoryClient {

    /** Call on shutdown to release external resources. */
    @Override
    void close();

    /**
     * Gets a single ARC record out of the ArcRepository.
     *
     * @param arcfile The name of a file containing the desired record.
     * @param index The offset of the desired record in the file
     * @return a BitarchiveRecord-object, or null if request times out or object is not found.
     * @throws IOFailure If the get operation failed.
     * @throws ArgumentNotValid if the given arcfile is null or empty string, or the given index is negative.
     */
    @Override
    BitarchiveRecord get(String arcfile, long index) throws ArgumentNotValid;

    /**
     * Retrieves a file from an ArcRepository and places it in a local file.
     *
     * @param arcfilename Name of the arcfile to retrieve.
     * @param replica The bitarchive to retrieve the data from. On implementations with only one replica, null may be
     * used.
     * @param toFile Filename of a place where the file fetched can be put.
     * @throws IOFailure if there are problems getting a reply or the file could not be found.
     */
    @Override
    void getFile(String arcfilename, Replica replica, File toFile);

    /**
     * Store the given file in the ArcRepository. After storing, the file is deleted.
     *
     * @param file A file to be stored. Must exist.
     * @throws IOFailure thrown if store is unsuccessful, or failed to clean up files after the store operation.
     * @throws ArgumentNotValid if file parameter is null or file is not an existing file.
     */
    @Override
    void store(File file) throws IOFailure, ArgumentNotValid;

    /**
     * Runs a batch batch job on each file in the ArcRepository.
     *
     * @param job An object that implements the FileBatchJob interface. The initialize() method will be called before
     * processing and the finish() method will be called afterwards. The process() method will be called with each File
     * entry. An optional function postProcess() allows handling the combined results of the batchjob, e.g. summing the
     * results, sorting, etc.
     * @param replicaId The archive to execute the job on.
     * @param args The arguments for the batchjob.
     * @return The status of the batch job after it ended.
     */
    BatchStatus batch(FileBatchJob job, String replicaId, String... args);

    /**
     * Updates the administrative data in the ArcRepository for a given file and replica.
     *
     * @param fileName The name of a file stored in the ArcRepository.
     * @param bitarchiveId The id of the replica that the administrative data for fileName is wrong for.
     * @param newval What the administrative data will be updated to.
     */
    @Override
    @Deprecated
    void updateAdminData(String fileName, String bitarchiveId, ReplicaStoreState newval);

    /**
     * Updates the checksum kept in the ArcRepository for a given file. It is the responsibility of the ArcRepository
     * implementation to ensure that this checksum matches that of the underlying files.
     *
     * @param filename The name of a file stored in the ArcRepository.
     * @param checksum The new checksum.
     */
    @Override
    @Deprecated
    void updateAdminChecksum(String filename, String checksum);

    /**
     * Remove a file from one part of the ArcRepository, retrieving a copy for security purposes. This is typically used
     * when repairing a file that has been corrupted.
     *
     * @param fileName The name of the file to remove.
     * @param bitarchiveId The id of the replica from which to remove the file.
     * @param checksum The checksum of the file to be removed.
     * @param credentials A string that shows that the user is allowed to perform this operation.
     * @return A local copy of the file removed.
     */
    @Override
    @Deprecated
    File removeAndGetFile(String fileName, String bitarchiveId, String checksum, String credentials);

}
