package dk.netarkivet.wayback.hadoop;

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

public class CDXStrategy implements HadoopJobStrategy {

    private final Logger log = LoggerFactory.getLogger(CDXStrategy.class);
    private final long jobID;
    private final FileSystem fileSystem;
    private final Configuration hadoopConf;
    private final String filename;

    public CDXStrategy(long jobID, FileSystem fileSystem) {
        this.jobID = jobID;
        this.fileSystem = fileSystem;
        hadoopConf = fileSystem.getConf();
        int totalMemory = Settings.getInt(CommonSettings.HADOOP_MAP_MEMORY_MB);
        int totalCores = Settings.getInt(CommonSettings.HADOOP_MAP_MEMORY_CORES);
        HadoopJobUtils.setMapMemory(hadoopConf, totalMemory);
        HadoopJobUtils.setMapCoresPerTask(hadoopConf, totalCores);
        HadoopJobUtils.enableMapOnlyUberTask(hadoopConf, totalMemory, totalCores);
        HadoopJobUtils.configureCaching(hadoopConf);
        HadoopJobUtils.setBatchQueue(hadoopConf);
        filename = hadoopConf.get("cdx_filename");
    }

    @Override public int runJob(Path jobInputFile, Path jobOutputDir) {
        int exitCode;
        try {
            log.info("Running CDX file job");
            exitCode = ToolRunner.run(new HadoopJobTool(hadoopConf, new CDXMapper()),
                    new String[] {jobInputFile.toString(), jobOutputDir.toString()});
        } catch (Exception e) {
            log.warn("Metadata extraction job with ID {} failed to run normally.", jobID, e);
            exitCode = 1;
        }
        return exitCode;
    }

    @Override public Path createJobInputFile(UUID uuid) {
        Path jobInputFile = HadoopFileUtils.createUniquePathInDir(
                fileSystem, Settings.get(CommonSettings.HADOOP_MAPRED_CDXJOB_INPUT_DIR), uuid);
        log.info("Input file for metadata extraction job '{}' will be '{}'", jobID, jobInputFile);
        return jobInputFile;
    }

    @Override public Path createJobOutputDir(UUID uuid) {
        Path jobOutputDir = HadoopFileUtils.createUniquePathInDir(
                fileSystem, Settings.get(CommonSettings.HADOOP_MAPRED_CDXJOB_OUTPUT_DIR), uuid);
        log.info("Output directory for metadata extraction job '{}' is '{}'", jobID, jobOutputDir);
        return jobOutputDir;
    }

    @Override public String getJobType() {
        return "CDX Indexer For " + filename;
    }
}
