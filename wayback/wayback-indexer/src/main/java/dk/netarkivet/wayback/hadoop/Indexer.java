package dk.netarkivet.wayback.hadoop;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.hadoop.util.Progressable;

public interface Indexer {

    List<String> indexFile(File file, Progressable progressable) throws IOException;

}
