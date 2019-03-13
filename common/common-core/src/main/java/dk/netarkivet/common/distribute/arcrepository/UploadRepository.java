package dk.netarkivet.common.distribute.arcrepository;

import java.io.File;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;

/**
 * Interface that isolates the functionality for uploading to a repository.
 */
public interface UploadRepository extends ExceptionlessAutoCloseable {
    /**
     * Store the given file in the ArcRepository. After storing, the file is deleted.
     *
     * @param file A file to be stored. Must exist.
     * @throws IOFailure thrown if store is unsuccessful, or failed to clean up files after the store operation.
     * @throws ArgumentNotValid if file parameter is null or file is not an existing file.
     */
    void store(File file) throws IOFailure, ArgumentNotValid;
}
