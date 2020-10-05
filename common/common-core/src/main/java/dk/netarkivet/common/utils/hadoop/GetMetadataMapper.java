package dk.netarkivet.common.utils.hadoop;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import org.archive.io.ArchiveReader;
import org.archive.io.ArchiveReaderFactory;
import org.archive.io.ArchiveRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.utils.archive.ArchiveHeaderBase;
import dk.netarkivet.common.utils.archive.ArchiveRecordBase;

/** Hadoop Mapper for extracting metadata entries from metadata files. */
public class GetMetadataMapper extends Mapper<LongWritable, Text, NullWritable, Text> {
    /** The logger for this class. */
    private static final Logger log = LoggerFactory.getLogger(GetMetadataMapper.class);

    /** A regular expression object that matches everything. */
    private final Pattern MATCH_ALL_PATTERN = Pattern.compile(".*");
    /** The pattern for matching the urls. */
    private Pattern urlMatcher;
    /** The pattern for the mimetype matcher. */
    private Pattern mimeMatcher;

    /**
     * Setup method that is provided by default for a Hadoop Mapper.
     * Initializes the patterns for matching the metadata records.
     *
     * @param context The job context. Used for getting the provided Configuration.
     * @throws IOException Thrown by the super class' setup method.
     * @throws InterruptedException Thrown by the super class' setup method.
     */
    @Override
    protected void setup(Context context) throws IOException, InterruptedException {
        super.setup(context);
        Configuration conf = context.getConfiguration();
        urlMatcher = conf.getPattern("url.pattern", MATCH_ALL_PATTERN);
        mimeMatcher = conf.getPattern("mime.pattern", MATCH_ALL_PATTERN);
    }

    /**
     * Mapping method.
     *
     * @param lineNumber The current line number of the input file (is ignored).
     * @param filePath The path to the input file.
     * @param context Context used for writing output.
     */
    @Override
    protected void map(LongWritable lineNumber, Text filePath, Context context) {

        // reject empty or null file paths.
        if(filePath == null || filePath.toString().trim().isEmpty()) {
            return;
        }

        Path path = new Path(filePath.toString());
        try (FileSystem fs = path.getFileSystem(context.getConfiguration())) {
            try (InputStream in = new BufferedInputStream(fs.open(path))) {
                try (ArchiveReader archiveReader = ArchiveReaderFactory.get(filePath.toString(), in, true)) {
                    for (ArchiveRecord archiveRecord : archiveReader) {
                        ArchiveRecordBase record = ArchiveRecordBase.wrapArchiveRecord(archiveRecord);
                        ArchiveHeaderBase header = record.getHeader();

                        if (header.getUrl() == null) {
                            continue;
                        }
                        log.info(header.getUrl() + " - " + header.getMimetype());
                        boolean recordHeaderMatchesPatterns = urlMatcher.matcher(header.getUrl()).matches()
                                && mimeMatcher.matcher(header.getMimetype()).matches();
                        if (recordHeaderMatchesPatterns) {
                            writeRecordMetadataLinesToContext(record, path, context);
                        }
                    }
                } catch (IOException e) {
                    log.warn("Failed creating archiveReader from archive file located at '{}'", filePath.toString());
                }
            } catch (IOException e) {
                log.error("Could not read input file at '{}'.", path.toString());
            }
        } catch (IOException e) {
            log.error("Could not get FileSystem from configuration", e);
        }
    }

    /**
     * Reads a record line by line and writes the metadata lines to output
     *
     * @param record The current record.
     * @param path Path for the input file the job is run on.
     * @param context The mapping context.
     */
    private void writeRecordMetadataLinesToContext(ArchiveRecordBase record, Path path, Context context) {
        int lineCount = 0;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(record.getInputStream()))) {
            for (String metadataLine = reader.readLine(); metadataLine != null; metadataLine = reader.readLine()) {
                context.write(NullWritable.get(), new Text(metadataLine));
                lineCount++;
            }
        } catch (Exception e) {
            log.warn("Failed writing metadata line #{} for input file '{}'.", lineCount, path.toString());
        }
    }
}
