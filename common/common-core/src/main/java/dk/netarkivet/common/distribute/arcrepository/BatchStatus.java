package dk.netarkivet.common.distribute.arcrepository;

import java.io.File;
import java.io.OutputStream;
import java.net.URI;
import java.util.Collection;
import java.util.List;

import dk.netarkivet.common.distribute.RemoteFile;
import dk.netarkivet.common.exceptions.IllegalState;
import dk.netarkivet.common.utils.batch.BatchJob;

public interface BatchStatus {
    /**
     * Get the number of files processed by the batch job. This counts all files whether failed or not.
     *
     * @return number of files passed to processFile
     */
    int getNoOfFilesProcessed();

    /**
     * Get the File objects for the files that failed.
     *
     * @return A collection containing the files that processFile returned false on.
     */
    Collection<URI> getFilesFailed();

    /**
     * Get the appId (internal string) for the bitarchive that these are the results from. Set to ALL_BITARCHIVE_APPS if
     * this it the combined status.
     *
     * @return A uniquely identifying string that should *not* be parsed
     */
    String getBitArchiveAppId();

    /**
     * Get the file containing results from a batch job. This may be null, if the batch job resulted in errors.
     *
     * @return A remote file containing results in some job-specific format.
     */
    RemoteFile getResultFile();

    /**
     * Get the list of exceptions that happened during the batch job.
     *
     * @return List of exceptions with information on where they occurred.
     */
    List<BatchJob.ExceptionOccurrence> getExceptions();

    /**
     * Copy the results of a batch job into a local file. This deletes the file from the remote server as appropriate.
     * Note that this method or appendResults can only be called once on a given object. If hasResultFile() returns
     * true, this method is safe to call.
     *
     * @param targetFile File to copy the results into. This file will be overridden if hasResultFile() returns true;
     * @throws IllegalState If the results have already been copied, or there are no results to copy due to errors.
     */
    void copyResults(File targetFile) throws IllegalState;

    /**
     * Append the results of a batch job into a stream. This deletes the results file from the remote server, so this or
     * copyResults can only be called once. If hasResultFile() returns true, this method is safe to call.
     *
     * @param stream A stream to append results to.
     * @throws IllegalState If the results have already been copied, or there are no results to copy due to errors.
     */
    void appendResults(OutputStream stream) throws IllegalState;

    /**
     * Returns true if this object has a result file. There is no result file if no bitarchives succeeded in processing
     * any files, or if the result file sent has already been deleted (e.g., by calling copyResults or appendResults).
     *
     * @return True if this object has a result file.
     */
    boolean hasResultFile();
}
