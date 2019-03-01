package dk.netarkivet.common.distribute.hadoop;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.util.Tool;

import dk.netarkivet.common.distribute.RemoteFile;
import dk.netarkivet.common.utils.batch.BatchJob;

public abstract class HadoopBatchJob extends Configured implements BatchJob {

    private Job job;
    private Path outputDir;

    protected Pattern filenamePattern;


    @Override
    public void setFilesToProcess(Pattern compile) {
        setFilenamePattern(compile);
    }

    public Pattern getFilenamePattern() {
        return filenamePattern;
    }

    public void setFilenamePattern(Pattern filenamePattern) {
        this.filenamePattern = filenamePattern;
    }


    private List<URI> filesToProcess;

    public List<URI> getFilesToProcess() {
        return filesToProcess;
    }

    public void setFilesToProcess(List<URI> filesToProcess) {
        this.filesToProcess = filesToProcess;
    }

    int noOfFilesProcessed = 0;

    @Override
    public int getNoOfFilesProcessed() {
        return noOfFilesProcessed;
    }

    @Override
    public void setNoOfFilesProcessed(int noOfFilesProcessed) {
        this.noOfFilesProcessed = noOfFilesProcessed;
    }


    Collection<URI> filesFailed;

    @Override
    public Collection<URI> getFilesFailed() {
        return filesFailed;
    }

    public void setFilesFailed(Collection<URI> filesFailed) {
        this.filesFailed = filesFailed;
    }


    long batchJobTimeout;

    @Override public long getBatchJobTimeout() {
        return batchJobTimeout;
    }

    @Override public void setBatchJobTimeout(long batchJobTimeout) {
        this.batchJobTimeout = batchJobTimeout;
    }


    public abstract RemoteFile getOuputFile() throws IOException;

    @Override
    public void initialize(OutputStream os) {

    }

    @Override
    public void finish(OutputStream os) {

    }

    private List<ExceptionOccurrence> exceptions;

    /**
     * Get the list of exceptions that have occurred during processing.
     *
     * @return List of exceptions together with information on where they happened.
     */
    public List<ExceptionOccurrence> getExceptions() {
        return exceptions;
    }

}
