package dk.netarkivet.harvester.indexserver.hadoop;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileUtil;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.DistributedFileSystem;
import org.apache.hadoop.hdfs.MiniDFSCluster;
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

import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.ZipUtils;
import dk.netarkivet.common.utils.hadoop.GetMetadataMapper;
import dk.netarkivet.common.utils.hadoop.HadoopJob;
import dk.netarkivet.common.utils.hadoop.HadoopJobUtils;
import dk.netarkivet.harvester.harvesting.metadata.MetadataFile;
import dk.netarkivet.harvester.indexserver.TestInfo;
import dk.netarkivet.testutils.preconfigured.MoveTestFiles;

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
            Tool job = new HadoopJob(conf, new GetMetadataMapper());
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
            Tool job = new HadoopJob(conf, new GetMetadataMapper());
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
