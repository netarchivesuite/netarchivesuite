package dk.netarkivet.wayback.indexer.hadoop;

import java.io.File;
import java.io.OutputStream;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.util.Tool;

import dk.netarkivet.common.utils.batch.BatchJob;

public class HadoopJob extends Configured implements Tool, BatchJob {


    Pattern filenamePattern;


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



    int noOfFilesProcessed = 0;

    @Override
    public int getNoOfFilesProcessed() {
        return noOfFilesProcessed;
    }

    @Override
    public void setNoOfFilesProcessed(int noOfFilesProcessed) {
        this.noOfFilesProcessed = noOfFilesProcessed;
    }


    Collection<File> filesFailed;

    @Override
    public Collection<File> getFilesFailed() {
        return filesFailed;
    }

    public void setFilesFailed(Collection<File> filesFailed) {
        this.filesFailed = filesFailed;
    }


    long batchJobTimeout;

    @Override public long getBatchJobTimeout() {
        return batchJobTimeout;
    }

    @Override public void setBatchJobTimeout(long batchJobTimeout) {
        this.batchJobTimeout = batchJobTimeout;
    }


    @Override
    public int run(String[] args) throws Exception {
        return 0;
    }


    @Override
    public void initialize(OutputStream os) {

    }

    @Override
    public boolean processFile(File file, OutputStream os) {
        return false;
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
