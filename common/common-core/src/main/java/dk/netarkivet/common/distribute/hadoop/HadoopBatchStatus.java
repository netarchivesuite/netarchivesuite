package dk.netarkivet.common.distribute.hadoop;

import java.io.File;
import java.io.OutputStream;
import java.net.URI;
import java.util.Collection;
import java.util.List;

import dk.netarkivet.common.distribute.RemoteFile;
import dk.netarkivet.common.distribute.arcrepository.BatchStatus;
import dk.netarkivet.common.exceptions.IllegalState;
import dk.netarkivet.common.utils.batch.BatchJob;

public class HadoopBatchStatus implements BatchStatus {

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

    private RemoteFile logFile;

    /** A list of exceptions caught during the execution of the batchJob. */
    private final List<BatchJob.ExceptionOccurrence> exceptions;

    public HadoopBatchStatus(boolean success, int noOfFilesProcessed, Collection<URI> filesFailed,
            String bitArchiveAppId,
            RemoteFile resultFile, RemoteFile logFile,
            List<BatchJob.ExceptionOccurrence> exceptions) {
        this.success = success;
        this.noOfFilesProcessed = noOfFilesProcessed;
        this.filesFailed = filesFailed;
        this.bitArchiveAppId = bitArchiveAppId;
        this.resultFile = resultFile;
        this.logFile = logFile;
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

    public List<BatchJob.ExceptionOccurrence> getExceptions() {
        return exceptions;
    }




    @Override public synchronized void copyResults(File targetFile) throws IllegalState {
        if (!hasResultFile()){
            throw new IllegalState("Resultfile not found");
        }
        getResultFile().copyTo(targetFile);
        getResultFile().cleanup();

    }

    @Override public synchronized void appendResults(OutputStream outputStream) throws IllegalState {
        if (!hasResultFile()){
            throw new IllegalState("Resultfile not found");
        }
        getResultFile().appendTo(outputStream);
        getResultFile().cleanup();
    }

    @Override public boolean hasResultFile() {
        return getResultFile().exists();
    }


}
