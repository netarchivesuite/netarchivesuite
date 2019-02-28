package dk.netarkivet.common.distribute.hadoop;

import java.net.URI;
import java.util.Collection;
import java.util.List;

import dk.netarkivet.common.distribute.RemoteFile;

public class HadoopBatchStatus {

    private boolean success;

    /** The total number of files processed so far. */
    private final int noOfFilesProcessed;
    /** A list of files that the batch job could not process. */
    private final Collection<URI> filesFailed;
    /**
     * The application ID identifying the bitarchive, that run this batch job. */
    private final String bitArchiveAppId;
    /** The file containing the result of the batch job. */
    private RemoteFile resultFile;

    /** A list of exceptions caught during the execution of the batchJob. */
    private final List<Exception> exceptions;

    public HadoopBatchStatus(boolean success, int noOfFilesProcessed, Collection<URI> filesFailed,
            String bitArchiveAppId,
            RemoteFile resultFile, List<Exception> exceptions) {
        this.success = success;
        this.noOfFilesProcessed = noOfFilesProcessed;
        this.filesFailed = filesFailed;
        this.bitArchiveAppId = bitArchiveAppId;
        this.resultFile = resultFile;
        this.exceptions = exceptions;
    }

    @Override public String toString() {
        return "HadoopBatchStatus{" +
                "success=" + success +
                ", noOfFilesProcessed=" + noOfFilesProcessed +
                ", filesFailed=" + filesFailed +
                ", bitArchiveAppId='" + bitArchiveAppId + '\'' +
                ", resultFile=" + resultFile +
                ", exceptions=" + exceptions +
                '}';
    }

    public boolean isSuccess() {
        return success;
    }

    public int getNoOfFilesProcessed() {
        return noOfFilesProcessed;
    }

    public Collection<URI> getFilesFailed() {
        return filesFailed;
    }

    public String getBitArchiveAppId() {
        return bitArchiveAppId;
    }

    public RemoteFile getResultFile() {
        return resultFile;
    }

    public List<Exception> getExceptions() {
        return exceptions;
    }
}
