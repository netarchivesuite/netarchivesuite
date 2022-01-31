package dk.netarkivet.harvester.indexserver.hadoop;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import dk.netarkivet.common.hadoop.HadoopMiniClusterTestCase;
import dk.netarkivet.common.utils.ZipUtils;
import dk.netarkivet.common.utils.hadoop.GetMetadataMapper;
import dk.netarkivet.common.utils.hadoop.HadoopJobTool;
import dk.netarkivet.common.utils.hadoop.HadoopJobUtils;
import dk.netarkivet.harvester.harvesting.metadata.MetadataFile;
import dk.netarkivet.harvester.indexserver.TestInfo;
import dk.netarkivet.testutils.preconfigured.MoveTestFiles;

public class GetMetadataMapperTester extends HadoopMiniClusterTestCase {
    private MoveTestFiles mtf;
    private final File METADATA_DIR = new File(TestInfo.WORKING_DIR, "metadata");

    // Prepare the input files for test by moving them to a temporary 'working' directory.
    @Before
    public void setUp() throws IOException {
        TestInfo.WORKING_DIR.mkdir();
        mtf = new MoveTestFiles(TestInfo.METADATA_DIR, METADATA_DIR);
        mtf.setUp();
    }

    @After
    public void tearDown() throws IOException {
        mtf.tearDown();
    }


    /**
     * Test that a Hadoop job with a GetMetadataMapper produces the correct metadata lines when given
     * a crawl log url pattern and 'text/plain' mime pattern.
     */
    //@Test
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
    //@Test
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

    /**
     * Unzip the compressed test files and return their insides.
     * @return The non-compressed archive files
     */
    private File[] getTestFiles() {
        File zipOne = new File(METADATA_DIR, "1-metadata-1.warc.zip");
        File zipTwo = new File(METADATA_DIR, "1-metadata-1.arc.zip");
        ZipUtils.unzip(zipOne, TestInfo.WORKING_DIR);
        ZipUtils.unzip(zipTwo, TestInfo.WORKING_DIR);
        File warcFile = new File(TestInfo.WORKING_DIR, "1-metadata-1.warc");
        File arcFile = new File(TestInfo.WORKING_DIR, "1-metadata-1.arc");
        return new File[] {warcFile, arcFile};
    }
}
