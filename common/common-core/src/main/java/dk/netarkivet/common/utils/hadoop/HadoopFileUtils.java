package dk.netarkivet.common.utils.hadoop;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.UUID;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.utils.Settings;

/** Utilities for file actions related to Hadoop. */
public class HadoopFileUtils {
    private static final Logger log = LoggerFactory.getLogger(HadoopFileUtils.class);

    /**
     * Initializes the input file and all parent dirs for a metadata extraction job
     * @param fileSystem The used filesystem
     * @param uuid The UUID used to name the input file
     * @return A Hadoop path representing the newly initialized input file or null if an error is encountered
     */
    public static Path initExtractionJobInput(FileSystem fileSystem, UUID uuid) {
        String hadoopInputDir = Settings.get(CommonSettings.HADOOP_MAPRED_CACHE_INPUT_DIR);
        if (hadoopInputDir == null) {
            log.error("Parent input dir specified by {} must not be null.", CommonSettings.HADOOP_MAPRED_CACHE_INPUT_DIR);
            return null;
        }
        return initInputFile(fileSystem, hadoopInputDir, uuid);
    }

    /**
     * Initializes the output dir and all its parent dirs for a metadata extraction job
     * @param fileSystem The used filesystem
     * @param uuid The UUID used to name the output dir
     * @return A Hadoop path representing the newly initialized output dir or null if an error is encountered
     */
    public static Path initExtractionJobOutput(FileSystem fileSystem, UUID uuid) {
        String hadoopOutputDir = Settings.get(CommonSettings.HADOOP_MAPRED_CACHE_OUTPUT_DIR);
        if (hadoopOutputDir == null) {
            log.error("Parent output dir specified by {} must not be null.", CommonSettings.HADOOP_MAPRED_CACHE_OUTPUT_DIR);
            return null;
        }
        return initOutputDir(fileSystem, hadoopOutputDir, uuid);
    }

    /**
     * Initializes and returns a job input file under a given path
     * @param fileSystem The used filesystem
     * @param hadoopInputDir A path to the parent directory to init the file under
     * @param uuid The UUID used to name the file
     * @return A Hadoop path representing the newly initialized input file or null if an error is encountered
     */
    public static Path initInputFile(FileSystem fileSystem, String hadoopInputDir, UUID uuid) {
        try {
            initDir(fileSystem, hadoopInputDir);
        } catch (IOException e) {
            log.error("Failed to init input dir {}", hadoopInputDir, e);
            return null;
        }
        return new Path(hadoopInputDir, uuid.toString());
    }

    /**
     * Initializes and returns a job output dir under a given path
     * @param fileSystem The used filesystem
     * @param hadoopOutputDir A path to the parent directory to init the new dir under
     * @param uuid The UUID used to name the dir
     * @return A Hadoop path representing the newly initialized output dir or null if an error is encountered
     */
    public static Path initOutputDir(FileSystem fileSystem, String hadoopOutputDir, UUID uuid) {
        try {
            initDir(fileSystem, hadoopOutputDir);
        } catch (IOException e) {
            log.error("Failed to init output dir {}", hadoopOutputDir, e);
            return null;
        }
        return new Path(hadoopOutputDir, uuid.toString());
    }

    /**
     * Initializes the given directory on the filesystem by deleting any existing directory and its files
     * on the direct path and (re)making the full directory path.
     * @param fileSystem The filesystem on which the actions are executed.
     * @param hadoopDir The directory path to initialize.
     * @throws IOException If any action on the filesystem fails.
     */
    public static void initDir(FileSystem fileSystem, String hadoopDir) throws IOException {
        Path hadoopDirPath = new Path(hadoopDir);
        if (fileSystem.exists(hadoopDirPath) && !fileSystem.isDirectory(hadoopDirPath)) {
            log.warn("{} already exists and is a file. Deleting and creating directory.", hadoopDirPath);
            fileSystem.delete(hadoopDirPath, true);
        } else {
            log.info("Creating dir {}", hadoopDirPath);
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
