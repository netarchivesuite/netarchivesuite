package dk.netarkivet.common.distribute.hadoop;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapreduce.Job;

import dk.netarkivet.common.distribute.RemoteFile;
import dk.netarkivet.common.utils.batch.BatchJob;

public abstract class HadoopBatchJob extends Configured implements BatchJob {

    protected Job job;
    protected Path outputDir;

    protected Pattern filenamePattern;

    public static void addJarToClasspath(Job hadoopJob, File jarfile) throws IOException {
        //The job jar locally
        String localJobJar = jarfile.getAbsoluteFile().toURI().toString();

        String[] oldClasspath = hadoopJob.getConfiguration().getStrings("tmpjars");
        if (!ArrayUtils.contains(oldClasspath, localJobJar)) {
            String[] newClassPath = ArrayUtils.add(oldClasspath, localJobJar);
            hadoopJob.getConfiguration().setStrings("tmpjars", newClassPath);
        }
    }

    public static FileSystem getFileSystem(Job hadoopJob) throws IOException {
        FileSystem hdfs;
        hdfs = FileSystem.newInstance(hadoopJob.getConfiguration());

        return hdfs;
    }

    public static Path getRunFolder(Job hadoopJob, FileSystem hdfs) throws IOException {
        Path runFolder = new Path(UUID.randomUUID().toString());
        hdfs.mkdirs(runFolder);

        return runFolder;
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

    @Override
    public void setFilesToProcess(Pattern compile) {
        setFilenamePattern(compile);
    }

    public Job getHadoopJob() {
        return job;
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

    public RemoteFile getOuputFile() throws IOException{
        return new HadoopRemoteFile(outputDir, FileSystem.get(job.getConfiguration()));
    }

    /**
     * Use this method to load the remaining libraries with code like
     *
     * HadoopBatchJob.addJarToClasspath(getHadoopJob(), new File(ClassUtil.findContainingJar(HadoopBatchJob.class)));
     *
     * @param os the OutputStream to which output should be written
     */
    @Override
    public void initialize(OutputStream os) {

    }

    @Override public boolean process(OutputStream os) {
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
