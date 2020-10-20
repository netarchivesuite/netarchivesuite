package dk.netarkivet.common.utils;

import java.io.File;
import java.io.FilenameFilter;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A simple file resolver for resolving local files against a parent directory to get
 * Path objects representing these files
 */
public class SimpleFileResolver implements FileResolver {
    private static final Logger log = LoggerFactory.getLogger(SimpleFileResolver.class);
    Path directory;

    public SimpleFileResolver(Path directory) {
        this.directory = directory;
    }

    @Override public List<Path> getPaths(PathMatcher filepattern) {
        File[] dirContents = new File(directory.toString()).listFiles(
            new FilenameFilter() {
                @Override public boolean accept(File dir, String name) {
                    return filepattern.matches(new File(name).toPath());
                }
            }
        );
        if (dirContents == null) {
            log.debug("No files found in directory '{}'. Returning empty list.", directory);
            return Collections.emptyList();
        }
        List<Path> result = new ArrayList<>();
        for (File file : dirContents) {
            if (file.isFile()) {
                Path filePath = getPath(file.getName());
                log.debug("Adding path '{}'", filePath);
                result.add(filePath);
            }
        }
        return result;
    }

    @Override public Path getPath(String filename) {
        Path path = directory.resolve(filename);
        if (!path.toFile().exists()) {
            return null;
        }
        return path;
    }
}
