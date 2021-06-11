package dk.netarkivet.common.utils.hadoop;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.UUID;

import javax.security.auth.login.Configuration;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.InvalidRequestException;
import org.apache.hadoop.fs.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.utils.Settings;

/** Utilities for file actions related to Hadoop. */
public class HadoopFileUtils {
    private static final Logger log = LoggerFactory.getLogger(HadoopFileUtils.class);

    private static boolean CACHING_ENABLED = true;
    private static String CACHE_PATH = "/user/nat-nas-devel/netarkivet_cache";
    private static int MAX_CACHE_DAYS = 0; //if zero, always delete

    /**
     * Given a file on a local file system, return a cached version of the same file on
     * a hdfs file system.
     * @param file
     * @return a hdfs path to the file
     * @throws InvalidRequestException if caching is not enabled
     */
    public static Path cacheFile(File file, Configuration conf) throws InvalidRequestException {
        throw new InvalidRequestException("not implemented");
    }


    /**
     * Creates and returns a unique path under a given directory.
     * @param fileSystem The used filesystem
     * @param dir A path to the parent directory to create the Path under
     * @param uuid The UUID used to name the Path
     * @return A Hadoop path representing a unique file/directory or null if an error is encountered
     */
    public static Path createUniquePathInDir(FileSystem fileSystem, String dir, UUID uuid) {
        try {
            initDir(fileSystem, dir);
        } catch (IOException e) {
            log.error("Failed to create output dir '{}'", dir, e);
            return null;
        }
        return new Path(dir, uuid.toString());
    }

    /**
     * Initializes the given directory on the filesystem by deleting any existing file on the direct path
     * and making all parent dirs in the directory path.
     * @param fileSystem The filesystem on which the actions are executed.
     * @param hadoopDir The directory path to initialize.
     * @throws IOException If any action on the filesystem fails.
     */
    public static void initDir(FileSystem fileSystem, String hadoopDir) throws IOException {
        Path hadoopDirPath = new Path(hadoopDir);
        if (fileSystem.exists(hadoopDirPath) && !fileSystem.isDirectory(hadoopDirPath)) {
            log.warn("'{}' already exists and is a file. Deleting and creating directory.", hadoopDirPath);
            fileSystem.delete(hadoopDirPath, true);
        } else {
            log.info("Creating dir '{}'", hadoopDirPath);
        }
        fileSystem.mkdirs(hadoopDirPath);
    }

    public static java.nio.file.Path makeLocalInputTempFile() {
        java.nio.file.Path localInputTempFile = null;
        try {
            localInputTempFile = Files.createTempFile(null, null);
        } catch (IOException e) {
            log.error("Failed writing to/creating file.", e);
        }
        return localInputTempFile;
    }
}
