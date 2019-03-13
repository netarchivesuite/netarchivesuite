package dk.netarkivet.common.distribute.arcrepository;

import java.io.File;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;

/**
 * Interface that isolates the functionality for a "read-only" client to a repository.
 */
public interface ReaderRepository extends ExceptionlessAutoCloseable {
    /**
     * Gets a single ARC record out of the ArcRepository.
     *
     * @param arcfile The name of a file containing the desired record.
     * @param index The offset of the desired record in the file
     * @return a BitarchiveRecord-object, or null if request times out or object is not found.
     * @throws ArgumentNotValid If the get operation failed.
     */
    BitarchiveRecord get(String arcfile, long index) throws ArgumentNotValid;

    /**
     * Retrieves a file from an ArcRepository and places it in a local file.
     *
     * @param arcfilename Name of the arcfile to retrieve.
     * @param replica The bitarchive to retrieve the data from.
     * @param toFile Filename of a place where the file fetched can be put.
     * @throws IOFailure if there are problems getting a reply or the file could not be found.
     */
    void getFile(String arcfilename, Replica replica, File toFile);
}
