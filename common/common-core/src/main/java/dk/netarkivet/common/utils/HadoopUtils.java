package dk.netarkivet.common.utils;

import static dk.netarkivet.common.distribute.arcrepository.bitrepository.BitmagArcRepositoryClient.BITREPOSITORY_KEYFILENAME;
import static dk.netarkivet.common.distribute.arcrepository.bitrepository.BitmagArcRepositoryClient.BITREPOSITORY_SETTINGS_DIR;
import static dk.netarkivet.common.distribute.arcrepository.bitrepository.BitmagArcRepositoryClient.BITREPOSITORY_STORE_MAX_PILLAR_FAILURES;
import static dk.netarkivet.common.distribute.arcrepository.bitrepository.BitmagArcRepositoryClient.BITREPOSITORY_USEPILLAR;

import java.io.File;

import org.apache.hadoop.conf.Configuration;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.distribute.arcrepository.bitrepository.Bitrepository;

public class HadoopUtils {
    public static final String DEFAULT_FILESYSTEM = "fs.defaultFS";
    public static final String MAPREDUCE_FRAMEWORK = "mapreduce.framework.name";
    public static final String YARN_RESOURCEMANAGER_ADDRESS = "yarn.resourcemanager.address";

    // TODO: Probably put these in settings.xml too
    public static final String HDFS_URI_SCHEME = "hdfs://node1:8020";
    public static final String HADOOP_INPUT_FOLDER_PATH = "/user/vagrant/input/";
    public static final String HADOOP_OUTPUT_FOLDER_PATH = "/user/vagrant/output/";
    public static final String HADOOP_FULL_INPUT_FOLDER_PATH = HDFS_URI_SCHEME + HADOOP_INPUT_FOLDER_PATH;
    public static final String HADOOP_FULL_OUTPUT_FOLDER_PATH = HDFS_URI_SCHEME + HADOOP_OUTPUT_FOLDER_PATH;

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
        return conf;
    }

    /**
     * TODO: move this somewhere else where it makes sense
     * Init a Bitrepository from settings
     * @return Initialised Bitrepository
     */
    public static Bitrepository initBitrep() {
        File configDir = Settings.getFile(BITREPOSITORY_SETTINGS_DIR);
        String keyfilename = Settings.get(BITREPOSITORY_KEYFILENAME);
        int maxStoreFailures = Settings.getInt(BITREPOSITORY_STORE_MAX_PILLAR_FAILURES);
        String usepillar = Settings.get(BITREPOSITORY_USEPILLAR);

        return new Bitrepository(configDir, keyfilename, maxStoreFailures, usepillar);
    }
}
