package dk.netarkivet.harvester.webinterface.hadoop;

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
 * Strategy to give a HadoopJob when wanting to extract crawl log lines matching some regex from metadata files.
 * The mapper expects the used Configuration to have this regex set. Otherwise, an all-matching pattern will be used.
 *
 * This type of job is the Hadoop counterpart to running
 * {@link dk.netarkivet.harvester.webinterface.CrawlLogLinesMatchingRegexp}.
 */
public class CrawlLogExtractionStrategy implements HadoopJobStrategy {
	private static final Logger log = LoggerFactory.getLogger(CrawlLogExtractionStrategy.class);
	private final long jobID;
	private final FileSystem fileSystem;
	private final Configuration hadoopConf;

	/**
	 * Constructor.
	 *
	 * @param jobID The ID for the job.
	 * @param fileSystem The Hadoop FileSystem used.
	 */
	public CrawlLogExtractionStrategy(long jobID, FileSystem fileSystem) {
		this.jobID = jobID;
		this.fileSystem = fileSystem;
		this.hadoopConf = fileSystem.getConf();
		int totalMemory = Settings.getInt(CommonSettings.HADOOP_MAP_MEMORY_MB);
		int totalCores = Settings.getInt(CommonSettings.HADOOP_MAP_MEMORY_CORES);
		HadoopJobUtils.setMapMemory(hadoopConf, totalMemory);
		HadoopJobUtils.setMapCoresPerTask(hadoopConf, totalCores);
		HadoopJobUtils.enableMapOnlyUberTask(hadoopConf, totalMemory, totalCores);
		HadoopJobUtils.configureCaching(hadoopConf);
		HadoopJobUtils.setInteractiveQueue(hadoopConf);
	}

	@Override
	public int runJob(Path jobInputFile, Path jobOutputDir) {
		int exitCode;
		try {
			exitCode = ToolRunner.run(new HadoopJobTool(hadoopConf, new CrawlLogExtractionMapper()),
					new String[] { jobInputFile.toString(), jobOutputDir.toString() });
		} catch (Exception e) {
			log.warn("Crawl log extraction job with ID {} failed to run normally.", jobID, e);
			exitCode = 1;
		}
		return exitCode;
	}

	@Override
	public Path createJobInputFile(UUID uuid) {
		Path jobInputFile = HadoopFileUtils.createUniquePathInDir(
				fileSystem, Settings.get(CommonSettings.HADOOP_MAPRED_CRAWLLOG_EXTRACTIONJOB_INPUT_DIR), uuid);
		log.info("Input file for crawl log extraction job '{}' will be '{}'", jobID, jobInputFile);
		return jobInputFile;
	}

	@Override
	public Path createJobOutputDir(UUID uuid) {
		Path jobOutputDir = HadoopFileUtils.createUniquePathInDir(
				fileSystem, Settings.get(CommonSettings.HADOOP_MAPRED_CRAWLLOG_EXTRACTIONJOB_OUTPUT_DIR), uuid);
		log.info("Output directory for crawl log extraction job '{}' is '{}'", jobID, jobOutputDir);
		return jobOutputDir;
	}

	@Override
	public String getJobType() {
		return "CRAWL LOG EXTRACTION";
	}
}
