package dk.netarkivet.common.utils;

import java.nio.file.Path;
import java.util.List;

public interface FileResolver {

    /**
     * Return a list of file-paths matching a given pattern. Empty if there
     * are no matching files.
     * @param filepattern String to match.
     * @return The list of matching file-paths.
     */
    List<Path> getPaths(String filepattern);

    /**
     * Return a single path to a given file, or null if the file is not found.
     * @param filename The filename to resolve.
     * @return Path representing the file.
     */
    Path getPath(String filename);

}
