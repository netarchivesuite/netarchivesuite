package dk.netarkivet.common.utils;

import java.io.File;
import java.io.FilenameFilter;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SimpleFileResolver implements FileResolver {

    Path directory;

    public SimpleFileResolver(Path directory) {
       this.directory = directory;
    }

    @Override public List<Path> getPaths(PathMatcher filepattern) {
        File[] dirContents = new File(directory.toString()).listFiles(
            new FilenameFilter() {
                @Override public boolean accept(File dir, String name) {
                    return filepattern.matches(new File(dir, name).toPath());
                }
            }
        );
        if (dirContents == null) {
            return Collections.emptyList();
        }
        List<Path> result = new ArrayList<>();
        for (File file : dirContents) {
            if (file.isFile()) {
                result.add(Paths.get(file.getAbsolutePath()));
            }
        }
        return result;
    }

    @Override public Path getPath(String filename) {
        return directory.resolve(filename);
    }
}
