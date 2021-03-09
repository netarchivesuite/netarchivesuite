package dk.netarkivet.viewerproxy.webinterface.hadoop;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.UUID;

import org.apache.hadoop.conf.Configuration;
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
import org.junit.Ignore;
import org.junit.Test;

import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.cdx.CDXRecord;
import dk.netarkivet.common.utils.hadoop.HadoopJobTool;
import dk.netarkivet.common.utils.hadoop.HadoopJobUtils;
import dk.netarkivet.testutils.StringAsserts;
import dk.netarkivet.testutils.preconfigured.MoveTestFiles;
import dk.netarkivet.viewerproxy.webinterface.TestInfo;

@Ignore
public class MetadataCDXMapperTester {
    private final File WORKING_DIR = new File(TestInfo.DATA_DIR, "working");
    private final File WARC_FILE = new File(WORKING_DIR, "2-metadata-1.warc");
    private final File ARC_FILE = new File(WORKING_DIR, "2-metadata-1.arc");
    private MoveTestFiles mtf;
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

    public void setupTestFiles() {
        mtf = new MoveTestFiles(TestInfo.ORIGINALS_DIR, WORKING_DIR);
        mtf.setUp();
        // There is probably a better solution, but would need 2 working dirs if using MoveTestFiles since it deletes working dir on setupUp()
        for (File file : TestInfo.WARC_ORIGINALS_DIR.listFiles()) {
            FileUtils.copyFile(file, new File(WORKING_DIR, file.getName()));
        }
    }

    @Test
    public void testCDXIndexWARCMetadataFile() throws Exception {
        String outputURI = "hdfs://localhost:" + hdfsCluster.getNameNodePort() + "/" + UUID.randomUUID().toString();
        // Write the input lines to the the input file
        File jobInputFile = File.createTempFile("tmp", UUID.randomUUID().toString());
        org.apache.commons.io.FileUtils.writeStringToFile(jobInputFile, "file://" + WARC_FILE.getAbsolutePath());
        jobInputFile.deleteOnExit();

        // Start the job
        try {
            Tool job = new HadoopJobTool(conf, new MetadataCDXMapper());
            int exitCode = ToolRunner.run(conf, job,
                    new String[] {"file://" + jobInputFile.toString(), outputURI});
            Assert.assertEquals(0, exitCode); // job success

            List<String> cdxLines = HadoopJobUtils.collectOutputLines(fileSystem, new Path(outputURI));
            List<CDXRecord> recordsForJob = HadoopJobUtils.getCDXRecordListFromCDXLines(cdxLines);
            assertEquals("Should return the expected number of records", 20, recordsForJob.size());
            StringAsserts.assertStringMatches("First record should be the crawl-manifest",
                    "^metadata://netarkivet.dk/crawl/setup/crawl-manifest.txt.*", recordsForJob.get(0).getURL());
            StringAsserts.assertStringMatches("Last record should be cdx", "^metadata://netarkivet.dk/crawl/index/cdx.*",
                    recordsForJob.get(recordsForJob.size() - 1).getURL());
        } finally {
            fileSystem.delete(new Path(outputURI), true);
        }
    }

    @Test
    public void testCDXIndexARCMetadataFile() throws Exception {
        String outputURI = "hdfs://localhost:" + hdfsCluster.getNameNodePort() + "/" + UUID.randomUUID().toString();
        // Write the input lines to the the input file
        File jobInputFile = File.createTempFile("tmp", UUID.randomUUID().toString());
        org.apache.commons.io.FileUtils.writeStringToFile(jobInputFile, "file://" + ARC_FILE.getAbsolutePath());
        jobInputFile.deleteOnExit();

        // Start the job
        try {
            Tool job = new HadoopJobTool(conf, new MetadataCDXMapper());
            int exitCode = ToolRunner.run(conf, job,
                    new String[] {"file://" + jobInputFile.toString(), outputURI});
            Assert.assertEquals(0, exitCode); // job success

            List<String> cdxLines = HadoopJobUtils.collectOutputLines(fileSystem, new Path(outputURI));
            //cdxLines.forEach(System.out::println);
            List<CDXRecord> recordsForJob = HadoopJobUtils.getCDXRecordListFromCDXLines(cdxLines);
            assertEquals("Should return the expected number of records", 18, recordsForJob.size());
            StringAsserts.assertStringMatches("First record should be preharvester metadata dedup",
                    "^metadata://netarkivet.dk/crawl/setup/duplicatereductionjobs.*", recordsForJob.get(0).getURL());
            StringAsserts.assertStringMatches("Last record should be cdx", "^metadata://netarkivet.dk/crawl/index/cdx.*",
                    recordsForJob.get(recordsForJob.size() - 1).getURL());
        } finally {
            fileSystem.delete(new Path(outputURI), true);
        }
    }

    @After
    public void tearDown() throws IOException {
        miniYarnCluster.stop();
        hdfsCluster.shutdown(true);
        fileSystem.close();
        mtf.tearDown();
    }
}
