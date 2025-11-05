package dk.netarkivet.viewerproxy.webinterface.hadoop;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Iterator;
import java.util.Random;
import java.util.regex.Pattern;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.LocalFileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import org.archive.io.ArchiveReader;
import org.archive.io.ArchiveRecord;
import org.archive.io.arc.ARCReaderFactory;
import org.archive.io.warc.WARCReaderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.utils.batch.FileBatchJob;
import dk.netarkivet.common.utils.hadoop.HadoopFileUtils;
import dk.netarkivet.harvester.webinterface.CrawlLogLinesMatchingRegexp;

/**
 * Hadoop Mapper for extracting crawllog lines from metadata files.
 * Expects the Configuration provided for the job to have a regex set, which is used to filter for relevant lines.
 * If no regex is set an all-matching regex will be used.
 */
public class CrawlLogExtractionMapper extends Mapper<LongWritable, Text, NullWritable, Text> {
	private static final Logger log = LoggerFactory.getLogger(CrawlLogExtractionMapper.class);

	/**
	 * Mapping method.
	 *
	 * @param linenumber The linenumber. Is ignored.
	 * @param archiveFilePath The path to the archive file.
	 * @param context Context used for writing output.
	 * @throws IOException If it fails to generate the CDX indexes.
	 */
	@Override
	protected void map(LongWritable linenumber, Text archiveFilePath, Context context) throws IOException,
			InterruptedException {
		boolean cacheHdfs = context.getConfiguration().getBoolean(CommonSettings.HADOOP_ENABLE_HDFS_CACHE, true);
		// reject empty or null warc paths.
		if (archiveFilePath == null || archiveFilePath.toString().trim().isEmpty()) {
			log.warn("Encountered empty path in job {}", context.getJobID().toString());
			return;
		}
		Path path = new Path(archiveFilePath.toString());
		Configuration conf = context.getConfiguration();
		Pattern crawlLogRegex = conf.getPattern("regex", Pattern.compile(".*"));

		log.info("Extracting crawl log lines matching regex: {}", crawlLogRegex);
		log.info("Extracting directly to hadoop context.");
		final FileSystem fileSystem = path.getFileSystem(conf);
		if (!(fileSystem instanceof LocalFileSystem)) {
			final String status = "Crawl log extraction only implemented for LocalFileSystem. Cannot extract from " + path;
			context.setStatus(status);
			System.err.println(status);
		} else {
			LocalFileSystem localFileSystem = ((LocalFileSystem) fileSystem);
			if (cacheHdfs) {
				try {
					extractCrawlLogLinesWithHdfs(localFileSystem.pathToFile(path), crawlLogRegex, context);
				} catch (IOException e) {
					log.warn("Extracting crawl log via hdfs failed for {} so trying with local file.", path, e);
					extractCrawlLogLines(localFileSystem.pathToFile(path), crawlLogRegex, context);
				}
			} else {
				extractCrawlLogLines(localFileSystem.pathToFile(path), crawlLogRegex, context);
			}
		}
	}

	private void extractCrawlLogLinesWithHdfs(File file, Pattern regex, Context context) throws IOException, InterruptedException {
		log.info("Executing experimental copy to hdfs.");
		Path dst = HadoopFileUtils.cacheFile(file, context.getConfiguration(), context);
		FileSystem hdfsFileSystem = FileSystem.get(context.getConfiguration());
		try (FSDataInputStream inputStream = hdfsFileSystem.open(dst)) {
			ArchiveReader archiveRecords = null;
			if (WARCReaderFactory.isWARCSuffix(file.getName())) {
				archiveRecords = WARCReaderFactory.get(file.getName(), inputStream, true);
			} else {
				archiveRecords = ARCReaderFactory.get(file.getName(), inputStream, true);
			}
			for (Iterator<ArchiveRecord> recordIterator = archiveRecords.iterator(); recordIterator.hasNext();) {
				context.progress();
				try (ArchiveRecord archiveRecord = recordIterator.next()) {
					String url = archiveRecord.getHeader().getUrl();
					log.info("Processing record with url {}", url);
					if (url != null && url.contains("crawl/logs/crawl.log")) {
						log.info("Processing crawl log with regex {}.", regex.pattern());
						BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(archiveRecord));
						String line;
						while ((line = bufferedReader.readLine()) != null) {
							if (regex.equals(".*") || regex.matcher(line).matches()) {
								context.write(NullWritable.get(), new Text(line));
							}
						}
					}
				}
			}
		}
	}

	/**
	 * Extract the crawl logs from a file matching the provided regex
	 * 
	 * @param file File to look for crawl logs in.
	 * @param regex The regex to match lines with.
	 * @param context
	 * @return A list of crawl log lines extracted from the file.
	 */
	private void extractCrawlLogLines(File file, Pattern regex, Context context) throws IOException, InterruptedException {
		FileBatchJob batchJob = new CrawlLogLinesMatchingRegexp(regex.pattern());
		byte[] array = new byte[7]; // length is bounded by 7
		new Random().nextBytes(array);
		String generatedString = new String(array, Charset.forName("UTF-8"));
		java.nio.file.Path tempOutput = Files.createTempFile(generatedString, "crawllog");
		log.info("Extracting crawl log from {} to {}.", file.getAbsolutePath(), tempOutput);
		OutputStream outputStream = Files.newOutputStream(tempOutput);
		batchJob.processFile(file, outputStream);
		try {
			outputStream.flush();
		} catch (IOException e) {
			log.warn("Error when trying to flush batch job output stream", e);
		}
		log.info("Buffering output from {} of size {} bytes.", tempOutput, Files.size(tempOutput));
		Files.lines(tempOutput).forEach(line -> {
			try {
				context.write(NullWritable.get(), new Text(line));
			} catch (IOException | InterruptedException e) {
				throw new RuntimeException(e);
			}
		});
		Files.delete(tempOutput);
	}
}
