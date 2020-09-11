package dk.netarkivet.harvester.indexserver.hadoop;

import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.ZipUtils;
import dk.netarkivet.common.utils.hadoop.GetMetadataArchiveMapper;
import dk.netarkivet.common.utils.hadoop.HadoopJob;
import dk.netarkivet.harvester.harvesting.metadata.MetadataFile;
import dk.netarkivet.harvester.indexserver.TestInfo;
import dk.netarkivet.testutils.preconfigured.MoveTestFiles;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileUtil;
import org.apache.hadoop.fs.LocatedFileStatus;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.RemoteIterator;
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

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class GetMetadataArchiveMapperTester {
    private MoveTestFiles mtf;
    private File metadataDir;
    private MiniDFSCluster hdfsCluster;
    private File baseDir;
    private Configuration conf;
    private MiniYARNCluster miniCluster;
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
        miniCluster = new MiniYARNCluster("name", 1, 1, 1);
        miniCluster.init(conf);
        miniCluster.start();
        // System.out.println("YARN started");
    }

    @After
    public void tearDown() throws IOException {
        miniCluster.stop();
        hdfsCluster.shutdown();
        FileUtil.fullyDelete(baseDir);
        mtf.tearDown();
        FileUtils.removeRecursively(TestInfo.WORKING_DIR);
    }


    /**
     * Test that a Hadoop job with a GetMetadataArchiveMapper produces the correct metadata lines when given
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
            Tool job = new HadoopJob(conf, new GetMetadataArchiveMapper());
            int exitCode = ToolRunner.run(conf, job,
                    new String[] {"file://" + jobInputFile.toString(), outputURI});
            Assert.assertEquals(0, exitCode); // job success

            List<String> metadataLines = collectHadoopResults(fileSystem, new Path(outputURI));
            Assert.assertEquals(624, metadataLines.size());
            //metadataLines.forEach(System.out::println);
        } finally {
            fileSystem.delete(new Path(outputURI), true);
        }
    }

    /**
     * Test that a Hadoop job with a GetMetadataArchiveMapper produces the correct metadata lines when given
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
            Tool job = new HadoopJob(conf, new GetMetadataArchiveMapper());
            int exitCode = ToolRunner.run(conf, job,
                    new String[] {"file://" + jobInputFile.toString(), outputURI});
            Assert.assertEquals(0, exitCode); // job success

            List<String> metadataLines = collectHadoopResults(fileSystem, new Path(outputURI));
            Assert.assertEquals(612, metadataLines.size());
            //metadataLines.forEach(System.out::println);
        } finally {
            fileSystem.delete(new Path(outputURI), true);
        }
    }


    public List<String> collectHadoopResults(DistributedFileSystem fileSystem, Path outputFolderPath) throws IOException {
        List<String> metadataLines = new ArrayList<>();
        boolean foundResult = false;
        RemoteIterator<LocatedFileStatus> iterator = fileSystem.listFiles(outputFolderPath, true);
        while (iterator.hasNext()) {
            LocatedFileStatus next = iterator.next();
            Path nextPath = next.getPath();

            if (nextPath.getName().startsWith("part-m")){
                foundResult = true;

                try (BufferedReader in = new BufferedReader(new InputStreamReader(new BufferedInputStream(fileSystem.open(nextPath))))) {
                    String line;
                    while ((line = in.readLine()) != null) {
                        metadataLines.add(line);
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        Assert.assertTrue(foundResult);
        return metadataLines;
    }

    public void setupTestFiles() {
        TestInfo.WORKING_DIR.mkdir();
        metadataDir = new File(TestInfo.WORKING_DIR, "metadata");
        metadataDir.mkdir();
        mtf = new MoveTestFiles(TestInfo.METADATA_DIR, metadataDir);
        mtf.setUp();
    }

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
