package dk.netarkivet.wayback.hadoop;

import java.io.File;
import java.io.IOException;
import java.util.List;

public interface Indexer {

    List<String> indexFile(File file) throws IOException;

}
