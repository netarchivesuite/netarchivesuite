package dk.netarkivet.common.utils;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.LocatedFileStatus;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.RemoteIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.CommonSettings;

/** Utilities for using Hadoop. */
public class HadoopUtils {
    private static final Logger log = LoggerFactory.getLogger(HadoopUtils.class);

    public static final String DEFAULT_FILESYSTEM = "fs.defaultFS";
    public static final String MAPREDUCE_FRAMEWORK = "mapreduce.framework.name";
    public static final String YARN_RESOURCEMANAGER_ADDRESS = "yarn.resourcemanager.address";

    /** Utility class, do not initialise. */
    private HadoopUtils() {
    }

    public static Configuration getConfFromSettings() {
        Configuration conf = new Configuration();
        conf.set(DEFAULT_FILESYSTEM, Settings.get(CommonSettings.HADOOP_DEFAULT_FS));
        conf.set(MAPREDUCE_FRAMEWORK, Settings.get(CommonSettings.HADOOP_MAPRED_FRAMEWORK));
        conf.set(YARN_RESOURCEMANAGER_ADDRESS, Settings.get(CommonSettings.HADOOP_RESOURCEMANAGER_ADDRESS));
        conf.set("fs.hdfs.impl", org.apache.hadoop.hdfs.DistributedFileSystem.class.getName());
        conf.set("fs.file.impl", org.apache.hadoop.fs.LocalFileSystem.class.getName());
        conf.set("dfs.client.use.datanode.hostname", "true");

        final String jarPath = Settings.get(CommonSettings.HADOOP_MAPRED_WAYBACK_UBER_JAR);
        if (jarPath == null || !(new File(jarPath)).exists()) {
            log.warn("Specified jar file {} does not exist.", jarPath);
        }
        conf.set("mapreduce.job.jar", jarPath);
        return conf;
    }

    public static Path initExtractionJobInput(FileSystem fileSystem, UUID uuid) {
        String hadoopInputDir = Settings.get(CommonSettings.HADOOP_MAPRED_CACHE_INPUT_DIR);
        if (hadoopInputDir == null) {
            log.error("Parent input dir specified by {} must not be null.", CommonSettings.HADOOP_MAPRED_CACHE_INPUT_DIR);
            return null;
        }
        return initInputFile(fileSystem, uuid, hadoopInputDir);
    }

    public static Path initInputFile(FileSystem fileSystem, UUID uuid, String hadoopInputDir) {
        try {
            initDir(fileSystem, hadoopInputDir);
        } catch (IOException e) {
            log.error("Failed to init input dir {}", hadoopInputDir, e);
            return null;
        }
        return new Path(hadoopInputDir, uuid.toString());
    }

    public static Path initExtractionJobOutput(FileSystem fileSystem, UUID uuid) {
        String hadoopOutputDir = Settings.get(CommonSettings.HADOOP_MAPRED_CACHE_OUTPUT_DIR);
        if (hadoopOutputDir == null) {
            log.error("Parent output dir specified by {} must not be null.", CommonSettings.HADOOP_MAPRED_CACHE_OUTPUT_DIR);
            return null;
        }
        return initOutputDir(fileSystem, uuid, hadoopOutputDir);
    }

    public static Path initOutputDir(FileSystem fileSystem, UUID uuid, String hadoopOutputDir) {
        try {
            HadoopUtils.initDir(fileSystem, hadoopOutputDir);
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

    /**
     * Collects lines from a jobs output files at a specified path.
     * @param fileSystem The filesystem that the result is collected from.
     * @param outputFolder The output folder to find the job result files in.
     * @return A list of lines collected from all the output files.
     * @throws IOException If the output folder or its contents cannot be read.
     */
    public static List<String> collectOutputLines(FileSystem fileSystem, Path outputFolder) throws IOException {
        List<String> resultLines = new ArrayList<>();
        RemoteIterator<LocatedFileStatus> iterator = fileSystem.listFiles(outputFolder, false);
        while (iterator.hasNext()) {
            Path subPath = iterator.next().getPath();
            if (subPath.getName().startsWith("part-m")) {
                try (BufferedReader in = new BufferedReader(new InputStreamReader(new BufferedInputStream(
                        fileSystem.open(subPath))))) {
                    String line;
                    while ((line = in.readLine()) != null) {
                        resultLines.add(line);
                    }
                }
            }
        }
        return resultLines;
    }

    public static void writeHadoopInputFileLinesToPath(List<java.nio.file.Path> files, java.nio.file.Path inputFilePath)
            throws IOException {
        java.nio.file.Path lastElem = files.get(files.size() - 1);
        for (java.nio.file.Path file : files) {
            String inputLine = "file://" + file.toString() + "\n";
            if (file.equals(lastElem)) {
                inputLine = "file://" + file.toString();
            }
            Files.write(inputFilePath, inputLine.getBytes(), StandardOpenOption.APPEND);
        }
    }
}
