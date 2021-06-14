package dk.netarkivet.common.utils.hadoop;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.UUID;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.FileUtil;
import org.apache.hadoop.fs.InvalidRequestException;
import org.apache.hadoop.fs.LocalFileSystem;
import org.apache.hadoop.fs.LocatedFileStatus;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.RemoteIterator;
import org.apache.hadoop.mapreduce.Mapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.utils.Settings;

/** Utilities for file actions related to Hadoop. */
public class HadoopFileUtils {
    private static final Logger log = LoggerFactory.getLogger(HadoopFileUtils.class);

    /**
     * Given a file on a local file system, return a cached version of the same file on
     * a hdfs file system.
     * @param file
     * @return a hdfs path to the file
     * @throws IOException if caching not enabled or fails otherwise
     */
    public static Path cacheFile(File file, Configuration conf) throws IOException {
        if (!conf.getBoolean(CommonSettings.HADOOP_ENABLE_HDFS_CACHE, false)) {
            throw new InvalidRequestException("Hdfs caching not enabled.");
        }
        FileSystem hdfsFileSystem = FileSystem.get(conf);
        Path cachePath = new Path(conf.get(CommonSettings.HADOOP_HDFS_CACHE_DIR));
        log.info("Creating the cache directory at {} if necessary.", cachePath);
        hdfsFileSystem.mkdirs(cachePath);
        cleanCache(conf);
        Path dst = new Path(cachePath, file.getName());
        log.info("Caching {} to {}.", file.getAbsolutePath(), dst);
        if (!hdfsFileSystem.exists(dst)) {
            FileUtil.copy(file, hdfsFileSystem, dst, false, conf);
        } else {
            log.info("Cached copy found - copying not necessary.");
        }
        return dst;
    }

     public static void cleanCache(Configuration configuration) throws IOException {
         log.info("Cleaning hdfs cache");
         long currentTime = System.currentTimeMillis();
         int days = configuration.getInt(CommonSettings.HADOOP_CACHE_DAYS, 0);
         long maxAgeMillis = days *24L*3600L*1000L;
         Path cachePath = new Path(configuration.get(CommonSettings.HADOOP_HDFS_CACHE_DIR));;
         log.info("Scanning {} for files older than {} days.", cachePath, days);
         FileSystem fileSystem = FileSystem.get(configuration);
         fileSystem.mkdirs(cachePath);
         RemoteIterator<LocatedFileStatus> locatedFileStatusRemoteIterator = fileSystem
                 .listFiles(cachePath, false);
         while (locatedFileStatusRemoteIterator.hasNext()) {
             LocatedFileStatus locatedFileStatus = locatedFileStatusRemoteIterator.next();
             long modTime = locatedFileStatus.getModificationTime();
             if (days == 0 || (currentTime - modTime) > maxAgeMillis ) {
                 log.info("Deleting {}.", locatedFileStatus.getPath());
                 fileSystem.delete(locatedFileStatus.getPath(), false);
             } else {
                 log.info("Not deleting {}.", locatedFileStatus.getPath());
             }
         }
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

    public static Path replaceWithCachedPathIfEnabled(Mapper.Context context, Path path)
            throws IOException {
        boolean cachingEnabled = context.getConfiguration().getBoolean(CommonSettings.HADOOP_ENABLE_HDFS_CACHE, false);
        boolean isLocal = path.getFileSystem(context.getConfiguration()) instanceof LocalFileSystem;
        if (isLocal && cachingEnabled) {
            log.info("Replacing {} with hdfs cached version.", path);
            File localFile = ((LocalFileSystem) path.getFileSystem(context.getConfiguration())).pathToFile(path);
            path = cacheFile(localFile, context.getConfiguration());
            log.info("New input path is {}.", path);
        }
        return path;
    }
}
