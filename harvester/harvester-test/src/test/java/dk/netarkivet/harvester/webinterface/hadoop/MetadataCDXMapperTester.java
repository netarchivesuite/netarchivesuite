package dk.netarkivet.harvester.webinterface.hadoop;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

import dk.netarkivet.viewerproxy.webinterface.hadoop.MetadataCDXMapper;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import dk.netarkivet.common.hadoop.HadoopMiniClusterTestCase;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.cdx.CDXRecord;
import dk.netarkivet.common.utils.hadoop.HadoopJobTool;
import dk.netarkivet.common.utils.hadoop.HadoopJobUtils;
import dk.netarkivet.testutils.StringAsserts;
import dk.netarkivet.testutils.preconfigured.MoveTestFiles;
import dk.netarkivet.harvester.webinterface.TestInfo;

public class MetadataCDXMapperTester extends HadoopMiniClusterTestCase {
    private final File WORKING_DIR = new File(TestInfo.BASE_DIR, "working");
    private final File WARC_FILE = new File(WORKING_DIR, "2-metadata-1.warc");
    private final File ARC_FILE = new File(WORKING_DIR, "2-metadata-1.arc");
    private MoveTestFiles mtf;

    @Before
    public void setUp() throws IOException {
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
            StringAsserts.assertStringContains("First record should be the crawl-manifest",
                    "metadata://netarkivet.dk/crawl/setup/crawl-manifest.txt", recordsForJob.get(0).getURL());
            StringAsserts.assertStringContains("Last record should be cdx", "metadata://netarkivet.dk/crawl/index/cdx",
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
            String firstRecordURL = recordsForJob.get(0).getURL();
            StringAsserts.assertStringContains("First record should be preharvester metadata dedup",
                    "metadata://netarkivet.dk/crawl/setup/duplicatereductionjobs", firstRecordURL);
            String lastRecordURL = recordsForJob.get(recordsForJob.size() - 1).getURL();
            StringAsserts.assertStringContains("Last record should be cdx", "metadata://netarkivet.dk/crawl/index/cdx",
                    lastRecordURL);
        } finally {
            fileSystem.delete(new Path(outputURI), true);
        }
    }

    @After
    public void tearDown() throws IOException {
        mtf.tearDown();
    }
}
