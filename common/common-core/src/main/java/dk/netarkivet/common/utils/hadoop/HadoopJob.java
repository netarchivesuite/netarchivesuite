package dk.netarkivet.common.utils.hadoop;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.util.ToolRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.utils.FileResolver;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.utils.SettingsFactory;
import dk.netarkivet.common.utils.SimpleFileResolver;

public class HadoopJob {
    private static final Logger log = LoggerFactory.getLogger(HadoopJob.class);

    private final Configuration hadoopConf;
    private final Path jobInputFile;
    private final Path jobOutputDir;
    private final JobType jobType;
    private String filenamePattern = ".*";
    private boolean setupFailed = false;

    public HadoopJob(Configuration hadoopConf, Path jobInputPath,
            Path jobOutputDir, JobType jobType) {
        this.hadoopConf = hadoopConf;
        this.jobInputFile = jobInputPath;
        this.jobOutputDir = jobOutputDir;
        this.jobType = jobType;
    }

    /**
     * Prepare the job input by getting the relevant files to process from the fileresolver, writing their paths to a
     * temp file, and copying this file to the input path.
     * @param jobID The ID of the job that is being run.
     * @param fileSystem The Hadoop FileSystem used.
     */
    public void prepareJobInput(long jobID, FileSystem fileSystem) {
        java.nio.file.Path localInputTempFile = HadoopFileUtils.makeLocalInputTempFile();
        FileResolver fileResolver = SettingsFactory.getInstance(CommonSettings.FILE_RESOLVER_CLASS);
        if (fileResolver instanceof SimpleFileResolver) {
            String pillarParentDir = Settings.get(CommonSettings.HADOOP_MAPRED_INPUT_FILES_PARENT_DIR);
            ((SimpleFileResolver) fileResolver).setDirectory(Paths.get(pillarParentDir));
        }
        List<java.nio.file.Path> filePaths = fileResolver.getPaths(Pattern.compile(filenamePattern));
        try {
            HadoopJobUtils.writeHadoopInputFileLinesToInputFile(filePaths, localInputTempFile);
        } catch (IOException e) {
            log.error("Failed writing filepaths to '{}' for {} job '{}'", localInputTempFile, jobType, jobID);
            setupFailed = true;
            return;
        }
        log.info("Copying file with input paths '{}' to hdfs path '{}' for {} job '{}'.",
                localInputTempFile, jobInputFile, jobType, jobID);
        try {
            fileSystem.copyFromLocalFile(true, new Path(localInputTempFile.toAbsolutePath().toString()),
                    jobInputFile);
        } catch (IOException e) {
            log.error("Failed copying local input '{}' to '{}' for job '{}'", localInputTempFile, jobInputFile, jobID);
            setupFailed = true;
        }
    }

    public int run(long jobID, Mapper<LongWritable, Text, NullWritable, Text> mapper) {
        int exitCode;
        try {
            log.info("Starting {} job for jobID {}", jobType, jobID);
            exitCode = ToolRunner.run(new HadoopJobTool(hadoopConf, mapper),
                    new String[] {jobInputFile.toString(), jobOutputDir.toString()});
        } catch (Exception e) {
            log.warn("{} job with ID {} failed to run normally.", jobType, jobID, e);
            exitCode = 1;
        }
        return exitCode;
    }

    public Configuration getHadoopConf() {
        return hadoopConf;
    }

    public JobType getJobType() {
        return jobType;
    }

    public Path getJobInputFile() {
        return jobInputFile;
    }

    public Path getJobOutputDir() {
        return jobOutputDir;
    }

    public void processOnlyFilesMatching(String fileSearchPattern) {
        this.filenamePattern = fileSearchPattern;
    }

    public String getFilenamePattern() {
        return filenamePattern;
    }

    public boolean hasJobsetupFailed() {
        return setupFailed;
    }
}
