package dk.netarkivet.viewerproxy.webinterface.hadoop;

import java.util.UUID;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.util.ToolRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.utils.hadoop.HadoopFileUtils;
import dk.netarkivet.common.utils.hadoop.HadoopJobStrategy;
import dk.netarkivet.common.utils.hadoop.HadoopJobTool;
import dk.netarkivet.common.utils.hadoop.HadoopJobUtils;

/**
 * Strategy to extract CDX lines from metadata files.
 *
 * This type of job is the Hadoop counterpart to running {@link dk.netarkivet.common.utils.cdx.ArchiveExtractCDXJob}.
 */
public class MetadataCDXExtractionStrategy implements HadoopJobStrategy {
    private final Logger log = LoggerFactory.getLogger(MetadataCDXExtractionStrategy.class);
    private final long jobID;
    private final FileSystem fileSystem;
    private final Configuration hadoopConf;

    /**
     * Constructor.
     *
     * @param jobID The ID for the job.
     * @param fileSystem The Hadoop FileSystem used.
     */
    public MetadataCDXExtractionStrategy(long jobID, FileSystem fileSystem) {
        this.jobID = jobID;
        this.fileSystem = fileSystem;
        hadoopConf = fileSystem.getConf();
        HadoopJobUtils.setMapMemory(hadoopConf, 4096);
        HadoopJobUtils.setMapCoresPerTask(hadoopConf, 2);
        HadoopJobUtils.enableMapOnlyUberTask(hadoopConf, 4096, 2);
    }

    @Override
    public int runJob(Path jobInputFile, Path jobOutputDir) {
        int exitCode;
        try {
            exitCode = ToolRunner.run(new HadoopJobTool(hadoopConf, new MetadataCDXMapper()),
                    new String[] {jobInputFile.toString(), jobOutputDir.toString()});
        } catch (Exception e) {
            log.warn("Metadata CDX extraction job with ID {} failed to run normally.", jobID, e);
            exitCode = 1;
        }
        return exitCode;
    }

    @Override
    public Path createJobInputFile(UUID uuid) {
        Path jobInputFile = HadoopFileUtils.createUniquePathInDir(
                fileSystem, Settings.get(CommonSettings.HADOOP_MAPRED_METADATA_CDX_EXTRACTIONJOB_INPUT_DIR), uuid);
        log.info("Input file for metadata CDX extraction job '{}' will be '{}'", jobID, jobInputFile);
        return jobInputFile;
    }

    @Override
    public Path createJobOutputDir(UUID uuid) {
        Path jobOutputDir = HadoopFileUtils.createUniquePathInDir(
                fileSystem, Settings.get(CommonSettings.HADOOP_MAPRED_METADATA_CDX_EXTRACTIONJOB_OUTPUT_DIR), uuid);
        log.info("Output directory for metadata CDX extraction job '{}' is '{}'", jobID, jobOutputDir);
        return jobOutputDir;
    }

    @Override
    public String getJobType() {
        return "METADATA CDX EXTRACTION";
    }
}
