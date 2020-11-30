package dk.netarkivet.viewerproxy.webinterface.hadoop;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.LocalFileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.utils.batch.FileBatchJob;
import dk.netarkivet.viewerproxy.webinterface.CrawlLogLinesMatchingRegexp;

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
        // reject empty or null warc paths.
        if (archiveFilePath == null || archiveFilePath.toString().trim().isEmpty()) {
            log.warn("Encountered empty path in job {}", context.getJobID().toString());
            return;
        }
        Path path = new Path(archiveFilePath.toString());
        List<String> crawlLogLines;

        /*try (InputStream in = new BufferedInputStream(path.getFileSystem(context.getConfiguration()).open(path))) {
            crawlLogLines = extractCrawlLogLines(in, archiveFilePath.toString());
        }*/
        log.info("Extracting crawl log lines for domain"); // TODO set domain on config?
        final FileSystem fileSystem = path.getFileSystem(context.getConfiguration());
        LocalFileSystem localFileSystem = ((LocalFileSystem) fileSystem);

        Configuration conf = context.getConfiguration();
        FileBatchJob batchJob = new CrawlLogLinesMatchingRegexp(conf.get("regex"));
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        batchJob.processFile(localFileSystem.pathToFile(path), baos);
        baos.flush();
        crawlLogLines = Arrays.asList(baos.toString().split("\\n"));
        for (String crawlLog : crawlLogLines) {
            context.write(NullWritable.get(), new Text(crawlLog));
        }
    }
}
