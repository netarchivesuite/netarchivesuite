package dk.netarkivet.common.distribute.hadoop;

import java.io.IOException;
import java.util.Date;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.PathFilter;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.junit.Test;

import dk.netarkivet.common.exceptions.IOFailure;

public class HadoopViewerArcRepositoryClientTest {

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


    private void setupInput(Job job, Configuration conf, Path inputDir) throws IOException {
        System.out.println("Inputdir=" + inputDir.makeQualified(inputDir.getFileSystem(conf)));
        job.setInputFormatClass(TextInputFormat.class);
        //Filter for filenames
        try {
            FileInputFormat.addInputPath(job, inputDir);
        } catch (IOException e) {
            throw new IOFailure("message", e);
        }
        FileInputFormat.setInputPathFilter(job, MyPathFilter.class);
        FileInputFormat.setInputDirRecursive(job, true);
    }


    public static class TestMapper<KEYIN, VALUEIN, KEYOUT, VALUEOUT> extends Mapper<KEYIN, VALUEIN, KEYOUT, VALUEOUT> {

        @Override protected void map(KEYIN key, VALUEIN value, Context context)
                throws IOException, InterruptedException {
            try {
                super.map(key, value, context);
            } catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    public static class TestReducer<KEYIN,VALUEIN,KEYOUT,VALUEOUT> extends Reducer<KEYIN,VALUEIN,KEYOUT,VALUEOUT> {
        @Override protected void reduce(KEYIN key, Iterable<VALUEIN> values, Context context)
                throws IOException, InterruptedException {
            super.reduce(key, values, context);
        }
    }
    @Test
    public void hadoopBatch() throws IOException {

        HadoopViewerArcRepositoryClient client = new HadoopViewerArcRepositoryClient();

        Job job = Job.getInstance();

        Configuration conf = job.getConfiguration();

        job.setJobName(this.getClass().getName());
        job.setJarByClass(this.getClass());


        job.setReducerClass(TestReducer.class);
        job.setMapperClass(TestMapper.class);

        Path inputDir = new Path(".");
        setupInput(job, conf, inputDir);

        HadoopBatchStatus result = client.hadoopBatch(job, null);
        System.out.println(result);
    }
}