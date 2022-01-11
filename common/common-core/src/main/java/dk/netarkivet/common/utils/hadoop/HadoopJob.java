package dk.netarkivet.common.utils.hadoop;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.service.FileResolver;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.utils.SettingsFactory;
import dk.netarkivet.common.utils.service.SimpleFileResolver;

/**
 * Wrapper for a Hadoop job to prepare/handle a job.
 */
public class HadoopJob {
    private static final Logger log = LoggerFactory.getLogger(HadoopJob.class);

    private final HadoopJobStrategy jobStrategy;
    private final String jobType;
    private final long jobID;

    public void setJobInputFile(Path jobInputFile) {
        this.jobInputFile = jobInputFile;
    }

    public void setJobOutputDir(Path jobOutputDir) {
        this.jobOutputDir = jobOutputDir;
    }

    private Path jobInputFile;
    private Path jobOutputDir;
    private String filenamePattern = ".*";
    private int fileCount = 0;

    /**
     * Constructor.
     *
     * @param jobID The id of the current job.
     * @param jobStrategy Strategy specifying
     */
    public HadoopJob(long jobID, HadoopJobStrategy jobStrategy) {
        this.jobID = jobID;
        this.jobStrategy = jobStrategy;
        jobType = jobStrategy.getJobType();
    }

    /**
     * Prepare the job output and input by getting the relevant files to process from the fileresolver,
     * writing their paths to a temp file, and copying this file to the input path.
     * By default uses an all-matching pattern for the file resolver, so use {@link #processOnlyFilesMatching(String)}
     * first to get files matching a specific pattern.
     *
     * @param fileSystem The Hadoop FileSystem used.
     */
    public void prepareJobInputOutput(FileSystem fileSystem) {
        UUID uuid = UUID.randomUUID();
        jobInputFile = jobStrategy.createJobInputFile(uuid);
        jobOutputDir = jobStrategy.createJobOutputDir(uuid);
        if (jobInputFile == null || jobOutputDir == null) {
            log.error("Failed initializing input/output for {} job '{}' with uuid '{}'",
                    jobType, jobID, uuid);
            throw new IOFailure("Failed preparing job: failed initializing job input/output directory");
        }

        java.nio.file.Path localInputTempFile = HadoopFileUtils.makeLocalInputTempFile();
        FileResolver fileResolver = SettingsFactory.getInstance(CommonSettings.FILE_RESOLVER_CLASS);
        if (fileResolver instanceof SimpleFileResolver) {
            String pillarParentDir = Settings.get(CommonSettings.HADOOP_MAPRED_INPUT_FILES_PARENT_DIR);
            ((SimpleFileResolver) fileResolver).setDirectory(Paths.get(pillarParentDir));
        }
        List<java.nio.file.Path> filePaths = fileResolver.getPaths(Pattern.compile(filenamePattern));
        fileCount = filePaths.size();
        log.info("{} found {} file(s) matching pattern '{}' to add to input file for {} job {}",
                fileResolver.getClass().getName(), fileCount, filenamePattern, jobType, jobID);
        if (fileCount == 0) {
            log.warn("Zero input files found for job {}, {}. Proceeding with caution.", jobType, jobID);
        }
        try {
            HadoopJobUtils.writeHadoopInputFileLinesToInputFile(filePaths, localInputTempFile);
        } catch (IOException e) {
            log.error("Failed writing filepaths to '{}' for {} job '{}'",
                    localInputTempFile, jobType, jobID);
            throw new IOFailure("Failed preparing job: failed to write job input to input file", e);
        }
        log.info("Copying file with input paths '{}' to job input path '{}' for {} job '{}'.",
                localInputTempFile, jobInputFile, jobType, jobID);
        try {
            fileSystem.copyFromLocalFile(true, new Path(localInputTempFile.toAbsolutePath().toString()),
                    jobInputFile);
        } catch (IOException e) {
            log.error("Failed copying local input '{}' to job input path '{}' on filesystem '{}' for job '{}'",
                    localInputTempFile, jobInputFile, fileSystem, jobID);
            throw new IOFailure("Failed preparing job: failed copying input to job input path", e);
        }
    }

    /**
     * Runs a Hadoop job according to the used strategy, configuration, and the settings in
     * {@link dk.netarkivet.common.utils.hadoop.HadoopJobTool}.
     */
    public void run() {
        log.info("Starting {} job for jobID {} on {} file(s) matching pattern '{}'",
                jobType, jobID, fileCount, filenamePattern);
        int exitCode = jobStrategy.runJob(jobInputFile, jobOutputDir);
        if (exitCode == 0) {
            log.info("{} job with jobID {} was a success!", jobType, jobID);
        } else {
            log.warn("{} job with ID {} failed with exit code '{}'", jobType, jobID, exitCode);
            throw new IOFailure("Hadoop job failed with exit code "+exitCode);
        }
    }

    /**
     * Changes the pattern used when getting the files for the job's input.
     *
     * @param filenamePattern Pattern to use when matching filenames.
     */
    public void processOnlyFilesMatching(String filenamePattern) {
        this.filenamePattern = filenamePattern;
    }

    /**
     * Get the output directory for the job.
     * @return Path representing output directory.
     */
    public Path getJobOutputDir() {
        return jobOutputDir;
    }

    /**
     * Get what type of job is being run.
     * @return The job type set by the job strategy used.
     */
    public String getJobType() {
        return jobType;
    }
}
