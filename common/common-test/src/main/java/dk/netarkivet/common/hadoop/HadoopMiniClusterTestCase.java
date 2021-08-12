package dk.netarkivet.common.hadoop;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hdfs.DistributedFileSystem;
import org.apache.hadoop.hdfs.MiniDFSCluster;
import org.apache.hadoop.yarn.conf.YarnConfiguration;
import org.apache.hadoop.yarn.server.MiniYARNCluster;
import org.apache.hadoop.yarn.server.resourcemanager.scheduler.ResourceScheduler;
import org.apache.hadoop.yarn.server.resourcemanager.scheduler.fifo.FifoScheduler;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.utils.Settings;

/** Overhead class for Hadoop tests that need to use a mini cluster. */
public class HadoopMiniClusterTestCase {
    protected static MiniDFSCluster hdfsCluster;
    protected static Configuration conf;
    protected static MiniYARNCluster miniYarnCluster;
    protected static DistributedFileSystem fileSystem;

    @BeforeClass
    public static void initCluster() throws IOException {
        setupConfiguration();
        startDFSCluster();
        startYarnCluster();

        fileSystem = hdfsCluster.getFileSystem();
    }

    private static void setupConfiguration() throws IOException {
        conf = new YarnConfiguration();

        File baseDir = Files.createTempDirectory("test_hdfs").toFile().getAbsoluteFile();
        baseDir.deleteOnExit();
        conf.set(MiniDFSCluster.HDFS_MINIDFS_BASEDIR, baseDir.getAbsolutePath());

        conf.setInt(YarnConfiguration.RM_SCHEDULER_MINIMUM_ALLOCATION_MB, 64);
        conf.setClass(YarnConfiguration.RM_SCHEDULER,
                FifoScheduler.class, ResourceScheduler.class);
        conf.setBoolean(CommonSettings.HADOOP_ENABLE_HDFS_CACHE, false);
        conf.set(CommonSettings.METADATAFILE_REGEX_SUFFIX, Settings.get(CommonSettings.METADATAFILE_REGEX_SUFFIX));

    }

    private static void startDFSCluster() throws IOException {
        MiniDFSCluster.Builder builder = new MiniDFSCluster.Builder(conf);
        hdfsCluster = builder.build();
    }

    private static void startYarnCluster() {
        miniYarnCluster = new MiniYARNCluster("name", 1, 1, 1);
        miniYarnCluster.init(conf);
        miniYarnCluster.start();
    }

    @AfterClass
    public static void tearDownCluster() throws IOException {
        miniYarnCluster.stop();
        hdfsCluster.shutdown(true);
        fileSystem.close();
    }
}
