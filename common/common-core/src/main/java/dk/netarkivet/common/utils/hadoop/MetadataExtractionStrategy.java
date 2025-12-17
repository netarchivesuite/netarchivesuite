package dk.netarkivet.common.utils.hadoop;

import java.util.UUID;
import java.util.regex.Pattern;

import dk.netarkivet.common.exceptions.IOFailure;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.util.ToolRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.utils.Settings;

/**
 * Strategy to give a HadoopJob when wanting to extract selected content from metadata files matching specific
 * URL- and MIME-patterns. The mapper expects the used Configuration to have these patterns set before use.
 * Otherwise, it will use all-matching patterns.
 *
 * This type of job is the Hadoop counterpart to running
 * {@link dk.netarkivet.common.utils.archive.GetMetadataArchiveBatchJob}.
 */
public class MetadataExtractionStrategy implements HadoopJobStrategy {
    private final Logger log = LoggerFactory.getLogger(MetadataExtractionStrategy.class);
    private final long jobID;
    private final FileSystem fileSystem;
    private final Configuration hadoopConf;
    private final Pattern urlPattern;
    private final Pattern mimePattern;

    /**
     * Constructor.
     *
     * @param jobID The ID for the job.
     * @param fileSystem The Hadoop FileSystem used.
     */
    public MetadataExtractionStrategy(long jobID, FileSystem fileSystem) {
        this.jobID = jobID;
        this.fileSystem = fileSystem;
        hadoopConf = fileSystem.getConf();
        int totalMemory = Settings.getInt(CommonSettings.HADOOP_MAP_MEMORY_MB);
        int totalCores = Settings.getInt(CommonSettings.HADOOP_MAP_MEMORY_CORES);
        HadoopJobUtils.setMapMemory(hadoopConf, totalMemory);
        HadoopJobUtils.setMapCoresPerTask(hadoopConf, totalCores);
        HadoopJobUtils.enableMapOnlyUberTask(hadoopConf, totalMemory, totalCores);
        HadoopJobUtils.configureCaching(hadoopConf);
        urlPattern = hadoopConf.getPattern(GetMetadataMapper.URL_PATTERN, Pattern.compile(".*"));
        mimePattern = hadoopConf.getPattern(GetMetadataMapper.MIME_PATTERN, Pattern.compile(".*"));
        HadoopJobUtils.setBatchQueue(hadoopConf);
    }

    @Override
    public void runJob(Path jobInputFile, Path jobOutputDir) throws HadoopException {
        int exitCode;
        try {
            log.info("URL/MIME patterns used for metadata extraction job {} are '{}' and '{}'",
                     jobID, urlPattern, mimePattern);
            exitCode = ToolRunner.run(new HadoopJobTool(hadoopConf, new GetMetadataMapper()),
                                      new String[]{jobInputFile.toString(), jobOutputDir.toString()});
        } catch (Exception e) {
            throw new HadoopException("Metadata extraction job with ID " + jobID + " failed to run normally.", e);
        }
        if (exitCode != 0) {
            throw new HadoopException("Metadata extraction job with ID " + jobID + " failed with exit code " + exitCode);
        }
    }

    @Override
    public Path createJobInputFile(UUID uuid) {
        Path jobInputFile = HadoopFileUtils.createUniquePathInDir(
                fileSystem, Settings.get(CommonSettings.HADOOP_MAPRED_METADATA_EXTRACTIONJOB_INPUT_DIR), uuid);
        log.info("Input file for metadata extraction job '{}' will be '{}'", jobID, jobInputFile);
        return jobInputFile;
    }

    @Override
    public Path createJobOutputDir(UUID uuid) {
        Path jobOutputDir = HadoopFileUtils.createUniquePathInDir(
                fileSystem, Settings.get(CommonSettings.HADOOP_MAPRED_METADATA_EXTRACTIONJOB_OUTPUT_DIR), uuid);
        log.info("Output directory for metadata extraction job '{}' is '{}'", jobID, jobOutputDir);
        return jobOutputDir;
    }

    @Override
    public String getJobType() {
        return "METADATA EXTRACTION";
    }
}
