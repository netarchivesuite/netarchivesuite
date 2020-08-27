package dk.netarkivet.wayback.indexer;

import java.nio.file.Path;

public interface FileResolver {

    public Path getPath(String filename);

}
