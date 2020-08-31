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

/** Hadoop Mapper for extracting metadata lines from archive files. */
public class GetMetadataArchiveMapper extends Mapper<LongWritable, Text, NullWritable, Text> {
    /** The logger for this class. */
    private static final Logger log = LoggerFactory.getLogger(GetMetadataArchiveMapper.class);

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
     * @param linenumber The current linenumber of the input file (is ignored).
     * @param filePath The path to the input file.
     * @param context Context used for writing output.
     * @throws IOException Thrown by a bunch of stuff TODO do try-catch and logging
     * @throws InterruptedException Thrown if job is interrupted while writing output
     */
    @Override
    protected void map(LongWritable linenumber, Text filePath, Context context) throws IOException,
            InterruptedException {

        // reject empty or null file paths.
        if(filePath == null || filePath.toString().trim().isEmpty()) {
            return;
        }

        Path path = new Path(filePath.toString());
        try (InputStream in = new BufferedInputStream(path.getFileSystem(context.getConfiguration()).open(path))) {
            List<String> res = new ArrayList<>();
            ArchiveReader archiveReader = ArchiveReaderFactory.get(filePath.toString(), in, true);
            for (ArchiveRecord archiveRecord : archiveReader) {
                ArchiveRecordBase record = ArchiveRecordBase.wrapArchiveRecord(archiveRecord);
                ArchiveHeaderBase header = record.getHeader();

                if (header.getUrl() == null) {
                    continue;
                }
                log.info(header.getUrl() + " - " + header.getMimetype());
                if (urlMatcher.matcher(header.getUrl()).matches() && mimeMatcher.matcher(header.getMimetype()).matches()) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(record.getInputStream()));
                    for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                        res.add(line);
                    }
                }
                // TODO - maybe also refactor to separate class
            }
            for (String line : res) {
                context.write(NullWritable.get(), new Text(line));
            }
        }
    }
}
