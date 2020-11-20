package dk.netarkivet.wayback.hadoop;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

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
import org.archive.wayback.core.CaptureSearchResult;
import org.archive.wayback.resourceindex.cdx.CDXLineToSearchResultAdapter;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.cdx.CDXRecord;
import dk.netarkivet.common.utils.hadoop.HadoopJob;
import dk.netarkivet.common.utils.hadoop.HadoopJobUtils;
import dk.netarkivet.testutils.StringAsserts;
import dk.netarkivet.testutils.preconfigured.MoveTestFiles;

public class CDXMapperTester {
    private final File WORKING_DIR = new File("../../harvester/harvester-test/tests/dk/netarkivet/viewerproxy/data/working");
    private final File BASE_DIR = new File("../../harvester/harvester-test/tests/dk/netarkivet/viewerproxy/webinterface/data");
    private final File ORIGINALS_DIR = new File(BASE_DIR, "originals");
    private final File WARC_ORIGINALS_DIR = new File(BASE_DIR, "warc-originals");
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

    private void setupTestFiles() {
        mtf = new MoveTestFiles(ORIGINALS_DIR, WORKING_DIR);
        mtf.setUp();
        // There is probably a better solution, but need 2 working dirs if using MoveTestFiles since it deletes working dir on setupUp()
        for (File file : WARC_ORIGINALS_DIR.listFiles()) {
            FileUtils.copyFile(file, new File(WORKING_DIR, file.getName()));
        }
    }

    @Test
    public void testCDXIndexWARCMetadataFileNoDedup() throws Exception {
        String outputURI = "hdfs://localhost:" + hdfsCluster.getNameNodePort() + "/" + UUID.randomUUID().toString();
        // Write the input lines to the the input file
        File testFile = new File(WORKING_DIR, "2-metadata-1.warc");
        File jobInputFile = File.createTempFile("tmp", UUID.randomUUID().toString());
        org.apache.commons.io.FileUtils.writeStringToFile(jobInputFile, "file://" + testFile.getAbsolutePath());
        jobInputFile.deleteOnExit();

        // Start the job
        try {
            Tool job = new HadoopJob(conf, new CDXMapper());
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
    public void testCDXIndexARCMetadataFileDedup() throws Exception {
        String outputURI = "hdfs://localhost:" + hdfsCluster.getNameNodePort() + "/" + UUID.randomUUID().toString();
        // Write the input lines to the the input file
        File testFile = new File("../wayback-test/tests/dk/netarkivet/wayback/data/originals/12345-metadata-4.arc");
        File jobInputFile = File.createTempFile("tmp", UUID.randomUUID().toString());
        org.apache.commons.io.FileUtils.writeStringToFile(jobInputFile, "file://" + testFile.getAbsolutePath());
        jobInputFile.deleteOnExit();

        // Start the job
        try {
            Tool job = new HadoopJob(conf, new CDXMapper());
            int exitCode = ToolRunner.run(conf, job,
                    new String[] {"file://" + jobInputFile.toString(), outputURI});
            Assert.assertEquals(0, exitCode); // job success

            List<String> cdxLines = HadoopJobUtils.collectOutputLines(fileSystem, new Path(outputURI));
            assertTrue("Expect some results", cdxLines.size() > 2);
            //cdxLines.forEach(System.out::println);

            CDXLineToSearchResultAdapter adapter = new CDXLineToSearchResultAdapter();
            for (String cdx_line : cdxLines) {
                CaptureSearchResult csr = adapter.adapt(cdx_line);
                assertNotNull("Expect a mime type for every result", csr.getMimeType());
            }
        } finally {
            fileSystem.delete(new Path(outputURI), true);
        }
    }

    @Test
    public void testCDXIndexARCMetadataFileNoDedup() throws Exception {
        String outputURI = "hdfs://localhost:" + hdfsCluster.getNameNodePort() + "/" + UUID.randomUUID().toString();
        // Write the input lines to the the input file
        File testFile = new File(WORKING_DIR, "2-metadata-1.arc");
        File jobInputFile = File.createTempFile("tmp", UUID.randomUUID().toString());
        org.apache.commons.io.FileUtils.writeStringToFile(jobInputFile, "file://" + testFile.getAbsolutePath());
        jobInputFile.deleteOnExit();

        // Start the job
        try {
            Tool job = new HadoopJob(conf, new CDXMapper());
            int exitCode = ToolRunner.run(conf, job,
                    new String[] {"file://" + jobInputFile.toString(), outputURI});
            Assert.assertEquals(0, exitCode); // job success

            List<String> cdxLines = HadoopJobUtils.collectOutputLines(fileSystem, new Path(outputURI));
            cdxLines.forEach(System.out::println);
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

    @Test
    public void testCDXIndexStandardARCFile() throws Exception {
        File testFile = new File("../wayback-test/tests/dk/netarkivet/wayback/data/originals/arcfile_withredirects.arc");
        String outputURI = "hdfs://localhost:" + hdfsCluster.getNameNodePort() + "/" + UUID.randomUUID().toString();
        File jobInputFile = File.createTempFile("tmp", UUID.randomUUID().toString());
        org.apache.commons.io.FileUtils.writeStringToFile(jobInputFile, "file://" + testFile.getAbsolutePath());
        jobInputFile.deleteOnExit();

        // Start the job
        try {
            Tool job = new HadoopJob(conf, new CDXMapper());
            int exitCode = ToolRunner.run(conf, job,
                    new String[] {"file://" + jobInputFile.toString(), outputURI});
            assertEquals(0, exitCode); // job success

            List<String> cdxLines = HadoopJobUtils.collectOutputLines(fileSystem, new Path(outputURI));
            //cdxLines.forEach(System.out::println);
            assertEquals(111, cdxLines.size());
            StringAsserts.assertStringMatches("First line should be netarkivet.dk dns",
                    "^dns:www.netarkivet.dk", cdxLines.get(0).split("\n")[0]);
            StringAsserts.assertStringMatches("Last line should be emediate.dk",
                    "^ad1.emediate.dk/eas\\?cu=4416;cre=mu;js=y;target=_blank;cat=byggeri",
                    cdxLines.get(cdxLines.size()-1).split("\n")[0]);
        } finally {
            fileSystem.delete(new Path(outputURI), true);
        }
    }

    @Test
    public void testCDXIndexStandardWARCFile() throws Exception {
        File testFile = new File("../wayback-test/tests/dk/netarkivet/wayback/data/originals/warcfile_withredirects.warc");
        String outputURI = "hdfs://localhost:" + hdfsCluster.getNameNodePort() + "/" + UUID.randomUUID().toString();
        File jobInputFile = File.createTempFile("tmp", UUID.randomUUID().toString());
        org.apache.commons.io.FileUtils.writeStringToFile(jobInputFile, "file://" + testFile.getAbsolutePath());
        jobInputFile.deleteOnExit();

        // Start the job
        try {
            Tool job = new HadoopJob(conf, new CDXMapper());
            int exitCode = ToolRunner.run(conf, job,
                    new String[] {"file://" + jobInputFile.toString(), outputURI});
            assertEquals(0, exitCode); // job success

            List<String> cdxLines = HadoopJobUtils.collectOutputLines(fileSystem, new Path(outputURI));
            //cdxLines.forEach(System.out::println);
            assertEquals(291, cdxLines.size());
            StringAsserts.assertStringMatches("First line should be netarkivet.dk dns",
                    "^dns:www.netarkivet.dk", cdxLines.get(0).split("\n")[0]);
            StringAsserts.assertStringMatches("Last line should be netarkivet with query",
                    "^netarkivet.dk/\\?p=100", cdxLines.get(cdxLines.size()-1).split("\n")[0]);
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
