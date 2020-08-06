package dk.netarkivet.wayback.hadoop;

import static dk.netarkivet.common.utils.HadoopUtils.DEFAULT_FILESYSTEM;
import static dk.netarkivet.common.utils.HadoopUtils.MAPREDUCE_FRAMEWORK;
import static dk.netarkivet.common.utils.HadoopUtils.YARN_RESOURCEMANAGER_ADDRESS;
import static dk.netarkivet.common.utils.HadoopUtils.getConfFromSettings;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.avro.generic.GenericData;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IOUtils;
import org.apache.hadoop.util.ToolRunner;
import org.hibernate.id.GUIDGenerator;
import org.junit.Before;
import org.junit.Test;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.tools.ToolRunnerBase;
import dk.netarkivet.common.utils.Settings;

public class CDXJobTest {

    private Configuration conf;

    //The location on the hadoop server where all the warc-files are to be found
    private String bitmagDir = "/kbhpillar/collection-netarkivet/";

    //The warc-files to be indexed
    private String[] files = new String[]{"10-4-20161218234343407-00000-kb-test-har-003.kb.dk.warc.gz"};

    //hdfs directory for the output
    private Path outputDir = new Path("/output");

    private FileSystem hdfs;

    @Before
    public void setUp() throws IOException {
        System.setProperty("HADOOP_USER_NAME", "vagrant");
        conf = new Configuration();
        conf.set(DEFAULT_FILESYSTEM, "hdfs://node1:8020");
        conf.set(MAPREDUCE_FRAMEWORK, "yarn");
        conf.set(YARN_RESOURCEMANAGER_ADDRESS, "node1:8032");
        conf.set("fs.hdfs.impl", org.apache.hadoop.hdfs.DistributedFileSystem.class.getName());
        conf.set("fs.file.impl", org.apache.hadoop.fs.LocalFileSystem.class.getName());
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
        java.nio.file.Path localInputTempfile = Files.createTempFile(null, null);
        for (String filename: files) {
            Files.write(localInputTempfile, ("file://" + bitmagDir + "/" + filename + "\n").getBytes());
        }
        hdfs.copyFromLocalFile(false, new Path(localInputTempfile.toAbsolutePath().toString()), hadoopInputPath);
        File jarFile = new File("/home/csr/projects/netarchivesuite/wayback/wayback-indexer/target/wayback-indexer-5.7-IIPCH3-SNAPSHOT-withdeps.jar");
        conf.set("mapreduce.job.jar", jarFile.getAbsolutePath());
        ToolRunner.run(new CDXJob(conf), new String[]{hadoopInputPath.toString(), outputDir.toString()});
    }

    @Test
    public void run() throws Exception {
        System.setProperty("HADOOP_USER_NAME", "vagrant");
        conf = new Configuration();
        conf.set(DEFAULT_FILESYSTEM, "hdfs://node1:8020");
        conf.set(MAPREDUCE_FRAMEWORK, "yarn");
        conf.set(YARN_RESOURCEMANAGER_ADDRESS, "node1:8032");
        conf.set("fs.hdfs.impl", org.apache.hadoop.hdfs.DistributedFileSystem.class.getName());
        conf.set("fs.file.impl", org.apache.hadoop.fs.LocalFileSystem.class.getName());

        //Local path to the input data
        java.nio.file.Path localInputDataDir = Paths.get("", "src", "test", "testdata");

        //Unique names for the input and output directories on the hdfs system
        String hdfsInputDir = UUID.randomUUID().toString();
        String hdfsOutputDir = UUID.randomUUID().toString();

        hdfs = FileSystem.get(conf);
        final Path inputPath = new Path("/" + hdfsInputDir);
        hdfs.mkdirs(inputPath);
        File [] localDataFiles = localInputDataDir.toFile().listFiles(new FilenameFilter() {    //the files to be processed
            @Override public boolean accept(File dir, String name) {
                return name.endsWith(".arc.gz") || name.endsWith(".warc.gz");
            }
        });
        List<Path> hdfsPaths = new ArrayList<>();  //the list of input paths for the hadoop job
        for (File localDataFile: localDataFiles) {
            Path hdfsDataFilePath = new Path (inputPath, localDataFile.getName()); //the remote file to copy to
            hdfsPaths.add(hdfsDataFilePath);
            hdfs.copyFromLocalFile(false, new Path(localDataFile.getAbsolutePath()), hdfsDataFilePath);
        }
        Path hadoopInputFile = new Path(inputPath, "inputfile");  //the input file for the hadoop job
        java.nio.file.Path tempFile = Files.createTempFile(null, null);
        //FSDataOutputStream outputStream = hdfs.create(hadoopInputFile);  //create the hadoop input file
        for (Path hdfsPath: hdfsPaths) {
            Files.write(tempFile, ("hdfs://" + hdfsPath.toString() + "\n").getBytes());
            //outputStream.writeUTF("hdfs://" + hdfsPath.toString());
            //outputStream.writeUTF("\n");
        }
        //outputStream.close();
        hdfs.copyFromLocalFile(false, new Path(tempFile.toAbsolutePath().toString()), hadoopInputFile);
        File jarFile = new File("/home/csr/projects/netarchivesuite/wayback/wayback-indexer/target/wayback-indexer-5.7-IIPCH3-SNAPSHOT-withdeps.jar");
        conf.set("mapreduce.job.jar", jarFile.getAbsolutePath());
        ToolRunner.run(new CDXJob(conf), new String[]{hadoopInputFile.toString(), "/" + hdfsOutputDir});
        hdfs.copyToLocalFile(new Path("hdfs:///"+hdfsOutputDir), new Path("newdir"));
        //hdfs.delete(inputPath, true);
    }
}