package dk.netarkivet.common.utils;

import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.List;

public interface FileResolver {

    /**
     * Return a list of file-paths matching a given pattern. Empty if there
     * are no matching files.
     * @param filepattern
     * @return
     */
    List<Path> getPaths(PathMatcher filepattern);

    /**
     * Return a single path to a given file, or null if the file is not found.
     * @param filename
     * @return
     */
    Path getPath(String filename);

}
