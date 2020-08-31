package dk.netarkivet.wayback.hadoop;

import static dk.netarkivet.common.utils.HadoopUtils.DEFAULT_FILESYSTEM;
import static dk.netarkivet.common.utils.HadoopUtils.MAPREDUCE_FRAMEWORK;
import static dk.netarkivet.common.utils.HadoopUtils.YARN_RESOURCEMANAGER_ADDRESS;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.util.ToolRunner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.optional.ssh.Scp;
import org.junit.Before;
import org.junit.Test;

import dk.netarkivet.common.utils.hadoop.HadoopJob;

public class CDXJobTest {

    private Configuration conf;

    //The location on the hadoop server where all the warc-files are to be found
    private String bitmagDir = "/home/vagrant";

    //Local directory containing warc-files to be indexed
    private String datadir = "src/test/testdata";
    private List<String> filenames = new ArrayList<>();

    //hdfs directory for the output
    private Path outputDir = new Path("/output");

    private FileSystem hdfs;

    /**
     * Initialises hdfs and copies data-file to (non-hdfs) filesystem on vagrant machine
     * @throws IOException
     */
    @Before
    public void setUp() throws IOException {
        initHdfs();
        deployTestdata();
    }

    private void deployTestdata() {
        for (File file: new File(datadir).listFiles()) {
            //if (file.getName().endsWith("arc.gz") || file.getName().endsWith("arc")) {
            if (file.getName().endsWith("arc.gz")) {

                filenames.add(file.getName());
                Scp scp = new Scp();
                scp.setHost("node1");
                scp.setUsername("vagrant");
                scp.setPassword("vagrant");
                scp.setRemoteTodir("vagrant:vagrant@node1:");
                scp.setProject(new Project());
                scp.setTrust(true);
                scp.setLocalFile(file.getAbsolutePath());
                scp.execute();
            }
        }
    }

    private void initHdfs() throws IOException {
        System.setProperty("HADOOP_USER_NAME", "vagrant");
        conf = new Configuration();
        conf.set(DEFAULT_FILESYSTEM, "hdfs://node1:8020");
        conf.set(MAPREDUCE_FRAMEWORK, "yarn");
        conf.set(YARN_RESOURCEMANAGER_ADDRESS, "node1:8032");
        conf.set("fs.hdfs.impl", org.apache.hadoop.hdfs.DistributedFileSystem.class.getName());
        conf.set("fs.file.impl", org.apache.hadoop.fs.LocalFileSystem.class.getName());
        conf.set("dfs.client.use.datanode.hostname", "true");
        hdfs = FileSystem.get(conf);
        hdfs.delete(outputDir);
    }

    /**
     * In this test, we run an indexing job on files which lie in hadoop cluster, but not in the hdfs filesystem.
     * The job input file, a list of files to process, does lie in hdfs.
     */
    @Test
    public void runNonhdfs() throws Exception {
        Path hadoopInputPath = new Path("/inputfile");
        hdfs.delete(hadoopInputPath);
        java.nio.file.Path localInputTempfile = buildInputFile();
        hdfs.copyFromLocalFile(false, new Path(localInputTempfile.toAbsolutePath().toString()), hadoopInputPath);
        File jarFile = new File("/home/csr/projects/netarchivesuite/wayback/wayback-indexer/target/wayback-indexer-5.7-IIPCH3-SNAPSHOT-withdeps.jar");
        conf.set("mapreduce.job.jar", jarFile.getAbsolutePath());
        ToolRunner.run(new HadoopJob(conf, new CDXMap()), new String[]{hadoopInputPath.toString(), outputDir.toString()});
        getAndPrintOutput();
    }

    private void getAndPrintOutput() throws IOException {
        java.nio.file.Path tempOutputDir = Files.createTempDirectory(null);
        hdfs.copyToLocalFile(outputDir, new Path(tempOutputDir.toAbsolutePath().toString()));
        Files.walk(tempOutputDir).filter(f -> f.getFileName().toString().startsWith("part-")).forEach(f -> {
            try {
                System.out.println(FileUtils.readFileToString(f.toFile()));
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private java.nio.file.Path buildInputFile() throws IOException {
        java.nio.file.Path localInputTempfile = Files.createTempFile(null, null);
        String fileData = "";
        for (String filename: filenames) {
            fileData += "file://" + bitmagDir + "/" + filename + "\n";
        }
        Files.write(localInputTempfile, fileData.getBytes());
        return localInputTempfile;
    }

}