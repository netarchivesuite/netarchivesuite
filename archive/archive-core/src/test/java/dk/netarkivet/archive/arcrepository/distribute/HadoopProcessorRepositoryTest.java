package dk.netarkivet.archive.arcrepository.distribute;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.PathFilter;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.junit.Test;

import dk.netarkivet.common.distribute.arcrepository.BatchStatus;
import dk.netarkivet.common.exceptions.IOFailure;

public class HadoopProcessorRepositoryTest {

    public static class MyPathFilter extends Configured implements PathFilter {

        @Override public boolean accept(Path path) {
            String name = path.getName();

            try {
                FileSystem fileSystem = FileSystem.get(getConf());

                boolean directory = fileSystem.isDirectory(path);
                if (directory){
                    return true;
                }
                if (!name.equals("JMSBroker.java")){
                    //                    System.out.println(path);
                    return false;
                    //                    return false;
                }
                return name.endsWith(".java");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Test
    public void hadoopBatch() throws IOException, URISyntaxException {



        HadoopProcessorRepository client = new HadoopProcessorRepository();

        Configuration hadoopConfiguration = client.getHadoopConfiguration();
        //Configuration hadoopConfiguration = new Configuration(false);

        MockupHadoopBatchJob batchJob = new MockupHadoopBatchJob(hadoopConfiguration);

        String replicaId = "replicaID1";

        Path inputFile = new Path(new File(Thread.currentThread().getContextClassLoader().getResource("core-site.xml").toURI())
                .getAbsolutePath());

        FileSystem fileSystem = FileSystem.get(hadoopConfiguration);

        Path dst = fileSystem.makeQualified(new Path(replicaId));
        fileSystem.mkdirs(dst);

        fileSystem.copyFromLocalFile(inputFile,  dst);

        batchJob.setFilesToProcess(Arrays.asList(new Path(dst,
                new File(Thread.currentThread().getContextClassLoader().getResource("core-site.xml").toURI()).getName()).toUri()));

        try {
            BatchStatus result = client.batch(batchJob, replicaId);
            System.out.println(result);
        } catch (Exception e){
            e.printStackTrace();
        } finally {
            System.exit(0);
        }

    }

}