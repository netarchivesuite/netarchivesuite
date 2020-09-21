package dk.netarkivet.wayback.hadoop;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.LocalFileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Hadoop Mapper for creating the CDX indexes.
 *
 * The input is a key (not used) and a Text line, which we assume is the path to an WARC file.
 * The output is an exit code (not used), and the generated CDX lines.
 */
public class CDXMapper extends Mapper<LongWritable, Text, NullWritable, Text> {

    private static final Logger log = LoggerFactory.getLogger(CDXMapper.class);

    /** The CDX indexer.*/
    private CDXIndexer cdxIndexer = new CDXIndexer();
    private DedupIndexer dedupIndexer = new DedupIndexer();

    /**
     * Mapping method.
     *
     * @param linenumber  The linenumber. Is ignored.
     * @param warcPath The path to the WARC file.
     * @param context Context used for writing output.
     * @throws IOException If it fails to generate the CDX indexes.
     */
    @Override
    protected void map(LongWritable linenumber, Text warcPath, Context context) throws IOException,
            InterruptedException {
        // reject empty or null warc paths.
        if(warcPath == null || warcPath.toString().trim().isEmpty()) {
            log.warn("Encountered empty path in job {}", context.getJobID().toString());
            return;
        }

        Path path = new Path(warcPath.toString());
        List<String> cdxIndexes;
        Indexer indexer;
        if (path.getName().contains("metadata")) {
            log.info("Indexing metadata file '{}'", path);
            indexer = new DedupIndexer();
            final FileSystem fileSystem = path.getFileSystem(context.getConfiguration());
            if (!(fileSystem instanceof LocalFileSystem)) {
                final String status = "Metadata indexing only implemented for LocalFileSystem. Cannot index " + path;
                context.setStatus(status);
                System.err.println(status);
                cdxIndexes = new ArrayList<>();
            } else {
                LocalFileSystem localFileSystem = ((LocalFileSystem) fileSystem);
                cdxIndexes = indexer.indexFile(localFileSystem.pathToFile(path));
            }
        } else {
            log.info("Indexing archive file '{}'", path);
            try (InputStream in = new BufferedInputStream(path.getFileSystem(context.getConfiguration()).open(path))) {
                cdxIndexes = cdxIndexer.index(in, warcPath.toString());
            }
        }
        for (String cdxIndex : cdxIndexes) {
            context.write(NullWritable.get(), new Text(cdxIndex));
        }
    }
}
