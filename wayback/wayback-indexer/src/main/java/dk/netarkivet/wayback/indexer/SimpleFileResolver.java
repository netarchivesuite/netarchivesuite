package dk.netarkivet.wayback.indexer;

import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.List;

public class SimpleFileResolver implements FileResolver {

    Path directory;

    public SimpleFileResolver(Path directory) {
       this.directory = directory;
    }

    @Override public List<Path> getPaths(PathMatcher filepattern) {
        throw new RuntimeException("not implemented");
    }

    @Override public Path getPath(String filename) {
        return directory.resolve(filename);
    }
}
