package dk.netarkivet.harvester.indexserver.hadoop;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.FileUtil;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.DistributedFileSystem;
import org.apache.hadoop.hdfs.HdfsConfiguration;
import org.apache.hadoop.hdfs.MiniDFSCluster;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.security.UserGroupInformation;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.apache.hadoop.yarn.conf.YarnConfiguration;
import org.apache.hadoop.yarn.server.MiniYARNCluster;
import org.apache.hadoop.yarn.server.resourcemanager.scheduler.ResourceScheduler;
import org.apache.hadoop.yarn.server.resourcemanager.scheduler.fifo.FifoScheduler;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.utils.ZipUtils;
import dk.netarkivet.common.utils.hadoop.GetMetadataMapper;
import dk.netarkivet.common.utils.hadoop.HadoopJobTool;
import dk.netarkivet.common.utils.hadoop.HadoopJobUtils;
import dk.netarkivet.harvester.harvesting.metadata.MetadataFile;
import dk.netarkivet.harvester.indexserver.TestInfo;
import dk.netarkivet.testutils.preconfigured.MoveTestFiles;
import sun.security.krb5.KrbException;

public class GetMetadataMapperTester {
    private MoveTestFiles mtf;
    private File metadataDir;
    private MiniDFSCluster hdfsCluster;
    private File baseDir;
    private Configuration conf;
    private MiniYARNCluster miniYarnCluster;
    private DistributedFileSystem fileSystem;


    @Before
    public void setUp() throws IOException {
        setupTestFiles();
        baseDir = Files.createTempDirectory("test_hdfs").toFile().getAbsoluteFile();
        conf = new YarnConfiguration();
        conf.set(MiniDFSCluster.HDFS_MINIDFS_BASEDIR, baseDir.getAbsolutePath());
        MiniDFSCluster.Builder builder = new MiniDFSCluster.Builder(conf);
        hdfsCluster = builder.build();

        fileSystem = hdfsCluster.getFileSystem();
        // System.out.println("HDFS started");

        conf.setInt(YarnConfiguration.RM_SCHEDULER_MINIMUM_ALLOCATION_MB, 64);
        conf.setClass(YarnConfiguration.RM_SCHEDULER,
                FifoScheduler.class, ResourceScheduler.class);
        miniYarnCluster = new MiniYARNCluster("name", 1, 1, 1);
        miniYarnCluster.init(conf);
        miniYarnCluster.start();
        // System.out.println("YARN started");
    }

    @After
    public void tearDown() throws IOException {
        miniYarnCluster.stop();
        hdfsCluster.shutdown();
        FileUtil.fullyDelete(baseDir);
        mtf.tearDown();
        FileUtils.removeRecursively(TestInfo.WORKING_DIR);
    }


    /**
     * Test that a Hadoop job with a GetMetadataMapper produces the correct metadata lines when given
     * a crawl log url pattern and 'text/plain' mime pattern.
     */
    @Test
    public void testMetadataCrawlLogJob() throws Exception {
        String outputURI = "hdfs://localhost:" + hdfsCluster.getNameNodePort() + "/" + UUID.randomUUID().toString();
        File[] files = getTestFiles();
        java.nio.file.Path jobInputFile = Files.createTempFile("", UUID.randomUUID().toString());
        Files.write(jobInputFile, Arrays.asList("file://" + files[0].getAbsolutePath(), "file://" + files[1].getAbsolutePath()));
        jobInputFile.toFile().deleteOnExit();

        conf.set("url.pattern", MetadataFile.CRAWL_LOG_PATTERN);
        conf.set("mime.pattern", "text/plain");

        try {
            Tool job = new HadoopJobTool(conf, new GetMetadataMapper());
            int exitCode = ToolRunner.run(conf, job,
                    new String[] {"file://" + jobInputFile.toString(), outputURI});
            Assert.assertEquals(0, exitCode); // job success

            List<String> metadataLines = HadoopJobUtils.collectOutputLines(fileSystem, new Path(outputURI));
            Assert.assertEquals(624, metadataLines.size());
            //metadataLines.forEach(System.out::println);
        } finally {
            fileSystem.delete(new Path(outputURI), true);
        }
    }

    /**
     * Test that a Hadoop job with a GetMetadataMapper produces the correct metadata lines when given
     * a cdx entry url pattern and 'application/x-cdx' mime pattern.
     */
    @Test
    public void testMetadataCDXJob() throws Exception {
        String outputURI = "hdfs://localhost:" + hdfsCluster.getNameNodePort() + "/" + UUID.randomUUID().toString();
        File[] files = getTestFiles();
        java.nio.file.Path jobInputFile = Files.createTempFile("", UUID.randomUUID().toString());
        Files.write(jobInputFile, Arrays.asList("file://" + files[0].getAbsolutePath(), "file://" + files[1].getAbsolutePath()));
        jobInputFile.toFile().deleteOnExit();

        conf.set("url.pattern", MetadataFile.CDX_PATTERN);
        conf.set("mime.pattern", "application/x-cdx");

        try {
            Tool job = new HadoopJobTool(conf, new GetMetadataMapper());
            int exitCode = ToolRunner.run(conf, job,
                    new String[] {"file://" + jobInputFile.toString(), outputURI});
            Assert.assertEquals(0, exitCode); // job success

            List<String> metadataLines = HadoopJobUtils.collectOutputLines(fileSystem, new Path(outputURI));
            Assert.assertEquals(612, metadataLines.size());
            //metadataLines.forEach(System.out::println);
        } finally {
            fileSystem.delete(new Path(outputURI), true);
        }
    }

    /*@Test
    public void testKerberizedMetadataJob() throws KrbException, IOException {
        Settings.set(CommonSettings.HADOOP_KERBEROS_PRINCIPAL, "nat-rbkr@KBHPC.KB.DK");
        Settings.set(CommonSettings.HADOOP_KERBEROS_KEYTAB, "/home/rbkr/nat-rbkr.keytab");
        Settings.set(CommonSettings.HADOOP_KERBEROS_CONF, "/etc/krb5.conf");


        HadoopJobUtils.doKerberosLogin();
        Configuration conf = new JobConf(new YarnConfiguration(new HdfsConfiguration()));
        conf.set("mapreduce.job.am-access-disabled","true");
        conf.set("hadoop.security.authentication", "kerberos");
        //conf.set("fs.defaultFS", "hdfs://narchive-t-hdfs01.kb.dk");
        conf.set("hadoop.security.authorization", "true");

        try (FileSystem fileSystem = FileSystem.newInstance(conf)) {
            FileStatus[] fileStatuses = fileSystem.listStatus(new Path("/user/nat-rbkr"));
            Arrays.stream(fileStatuses).forEach(System.out::println);
        }
    }*/

    /**
     * Prepare the input files for test by moving them to a temporary 'working' directory.
     */
    public void setupTestFiles() {
        TestInfo.WORKING_DIR.mkdir();
        metadataDir = new File(TestInfo.WORKING_DIR, "metadata");
        mtf = new MoveTestFiles(TestInfo.METADATA_DIR, metadataDir);
        mtf.setUp();
    }

    /**
     * Unzip the compressed test files and return their insides.
     * @return The non-compressed archive files
     */
    public File[] getTestFiles() {
        File zipOne = new File(metadataDir, "1-metadata-1.warc.zip");
        File zipTwo = new File(metadataDir, "1-metadata-1.arc.zip");
        ZipUtils.unzip(zipOne, TestInfo.WORKING_DIR);
        ZipUtils.unzip(zipTwo, TestInfo.WORKING_DIR);
        File warcFile = new File(TestInfo.WORKING_DIR, "1-metadata-1.warc");
        File arcFile = new File(TestInfo.WORKING_DIR, "1-metadata-1.arc");
        return new File[] {warcFile, arcFile};
    }
}
