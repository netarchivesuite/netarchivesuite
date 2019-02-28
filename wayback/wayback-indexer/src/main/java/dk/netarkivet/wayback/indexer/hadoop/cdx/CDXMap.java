package dk.netarkivet.wayback.indexer.hadoop.cdx;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;

/**
 * Hadoop Mapper for creating the CDX indexes.
 *
 * The input is a key (not used) and a Text line, which we assume is the path to an WARC file.
 * The output is an exit code (not used), and the generated CDX lines.
 */
public class CDXMap extends Mapper<LongWritable, Text, Text, Text> {

    /** The CDX indexer.*/
    private CDXIndexer indexer = new CDXIndexer();

    /**
     * Mapping method.
     *
     * @param key  The key. Is ignored.
     * @param warcPath The path to the WARC file.
     * @param context ??
     * @throws IOException If it fails to generate the CDX indexes.
     */
    @Override
    protected void map(LongWritable key, Text warcPath, Context context) throws IOException, InterruptedException {
        // reject empty or null warc paths.
        if(warcPath == null || warcPath.toString().trim().isEmpty()) {
            return;
        }

        Path path = new Path(warcPath.toString());
        try (InputStream in = path.getFileSystem(context.getConfiguration()).open(path)) {
            List<String> cdxIndexes = indexer.index(in, warcPath.toString());
            for (String cdxIndex : cdxIndexes) {
                context.write(warcPath, new Text(cdxIndex));
            }
        }
    }

}
