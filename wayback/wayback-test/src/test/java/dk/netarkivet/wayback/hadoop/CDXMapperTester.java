package dk.netarkivet.wayback.hadoop;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.archive.wayback.core.CaptureSearchResult;
import org.archive.wayback.resourceindex.cdx.CDXLineToSearchResultAdapter;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import dk.netarkivet.common.hadoop.HadoopMiniClusterTestCase;
import dk.netarkivet.common.utils.hadoop.HadoopJobTool;
import dk.netarkivet.common.utils.hadoop.HadoopJobUtils;
import dk.netarkivet.testutils.StringAsserts;
import dk.netarkivet.testutils.preconfigured.MoveTestFiles;

public class CDXMapperTester extends HadoopMiniClusterTestCase {
    private final File BASE_DIR = new File("tests/dk/netarkivet/wayback/data");
    private final File ORIGINALS_DIR = new File(BASE_DIR, "originals/");
    private final File WORKING_DIR = new File(BASE_DIR, "working");
    private MoveTestFiles mtf;

    @Before
    public void setUp() throws IOException {
        mtf = new MoveTestFiles(ORIGINALS_DIR, WORKING_DIR);
        mtf.setUp();
    }

    @Test
    public void testDedupCDXIndexARCMetadataFile() throws Exception {
        String outputURI = "hdfs://localhost:" + hdfsCluster.getNameNodePort() + "/" + UUID.randomUUID().toString();
        // Write the input lines to the the input file
        File testFile = new File(WORKING_DIR, "12345-metadata-4.arc");
        File jobInputFile = File.createTempFile("tmp", UUID.randomUUID().toString());
        org.apache.commons.io.FileUtils.writeStringToFile(jobInputFile, "file://" + testFile.getAbsolutePath());
        jobInputFile.deleteOnExit();

        // Start the job
        try {
            Tool job = new HadoopJobTool(conf, new CDXMapper());
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
    public void testCDXIndexStandardARCFile() throws Exception {
        File testFile = new File(WORKING_DIR, "arcfile_withredirects.arc");
        String outputURI = "hdfs://localhost:" + hdfsCluster.getNameNodePort() + "/" + UUID.randomUUID().toString();
        File jobInputFile = File.createTempFile("tmp", UUID.randomUUID().toString());
        org.apache.commons.io.FileUtils.writeStringToFile(jobInputFile, "file://" + testFile.getAbsolutePath());
        jobInputFile.deleteOnExit();

        // Start the job
        try {
            Tool job = new HadoopJobTool(conf, new CDXMapper());
            int exitCode = ToolRunner.run(conf, job,
                    new String[] {"file://" + jobInputFile.toString(), outputURI});
            assertEquals(0, exitCode); // job success

            List<String> cdxLines = HadoopJobUtils.collectOutputLines(fileSystem, new Path(outputURI));
            //cdxLines.forEach(System.out::println);
            assertEquals(111, cdxLines.size());
            StringAsserts.assertStringContains("First line should be netarkivet.dk dns",
                    "dns:www.netarkivet.dk", cdxLines.get(0).split("\n")[0]);
            StringAsserts.assertStringContains("Last line should be emediate.dk",
                    "ad1.emediate.dk/eas?cu=4416;cre=mu;js=y;target=_blank;cat=byggeri",
                    cdxLines.get(cdxLines.size()-1).split("\n")[0]);
        } finally {
            fileSystem.delete(new Path(outputURI), true);
        }
    }

    @Test
    public void testCDXIndexStandardWARCFile() throws Exception {
        File testFile = new File(WORKING_DIR,"warcfile_withredirects.warc");
        String outputURI = "hdfs://localhost:" + hdfsCluster.getNameNodePort() + "/" + UUID.randomUUID().toString();
        File jobInputFile = File.createTempFile("tmp", UUID.randomUUID().toString());
        org.apache.commons.io.FileUtils.writeStringToFile(jobInputFile, "file://" + testFile.getAbsolutePath());
        jobInputFile.deleteOnExit();

        // Start the job
        try {
            Tool job = new HadoopJobTool(conf, new CDXMapper());
            int exitCode = ToolRunner.run(conf, job,
                    new String[] {"file://" + jobInputFile.toString(), outputURI});
            assertEquals(0, exitCode); // job success

            List<String> cdxLines = HadoopJobUtils.collectOutputLines(fileSystem, new Path(outputURI));
            //cdxLines.forEach(System.out::println);
            assertEquals(291, cdxLines.size());
            String firstLine = cdxLines.get(0).split("\n")[0];
            StringAsserts.assertStringContains("First line should be netarkivet.dk dns",
                    "dns:www.netarkivet.dk", firstLine);
            String lastLine = cdxLines.get(cdxLines.size() - 1).split("\n")[0];
            StringAsserts.assertStringContains("Last line should be netarkivet with query",
                    "netarkivet.dk/?p=100", lastLine);
        } finally {
            fileSystem.delete(new Path(outputURI), true);
        }
    }

    @After
    public void tearDown() throws IOException {
        mtf.tearDown();
    }
}
