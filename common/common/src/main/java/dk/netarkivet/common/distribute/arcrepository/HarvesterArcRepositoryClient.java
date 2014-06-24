package dk.netarkivet.common.distribute.arcrepository;

import java.io.File;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;

/**
 * Implements the Facade pattern to shield off the methods in
 * JMSArcRepositoryClient not to be used by the harvest system.
 */
public interface HarvesterArcRepositoryClient {
    /** Call on shutdown to release external resources. */
    void close();

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
}
