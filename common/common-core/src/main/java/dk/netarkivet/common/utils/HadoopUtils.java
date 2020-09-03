package dk.netarkivet.common.utils;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
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
}
