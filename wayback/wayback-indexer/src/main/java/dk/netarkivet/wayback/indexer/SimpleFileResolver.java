package dk.netarkivet.wayback.indexer;

import java.nio.file.Path;

public class SimpleFileResolver implements FileResolver {

    Path directory;

    public SimpleFileResolver(Path directory) {
       this.directory = directory;
    }

    @Override public Path getPath(String filename) {
        return directory.resolve(filename);
    }
}
