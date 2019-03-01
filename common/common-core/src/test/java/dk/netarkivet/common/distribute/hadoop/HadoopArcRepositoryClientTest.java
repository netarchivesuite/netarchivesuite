package dk.netarkivet.common.distribute.hadoop;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.PathFilter;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.junit.Test;

import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.archive.ArchiveRecordBase;

public class HadoopArcRepositoryClientTest {

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


    public static class TestMapper extends Mapper<Text,ArchiveRecordBase,Text,Text> {

        @Override protected void map(Text key, ArchiveRecordBase value, Context context)
                throws IOException, InterruptedException {
            try {
                super.map(key, value, context);
            } catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    public static class TestReducer extends Reducer<Text, Text, Text, Text> {
        @Override protected void reduce(Text key, Iterable<Text> values, Context context)
                throws IOException, InterruptedException {
            super.reduce(key, values, context);
        }
    }
    @Test
    public void hadoopBatch() throws IOException {

        HadoopArcRepositoryClient client = new HadoopArcRepositoryClient();

        Job job = Job.getInstance();

        Configuration conf = job.getConfiguration();

        job.setJobName(this.getClass().getName());
        job.setJarByClass(this.getClass());

        Path inputDir = new Path(".");
        setupArchiveRecordInput(job,conf,inputDir);

        job.setMapOutputKeyClass(Text.class);
        job.setMapOutputValueClass(ArchiveRecordBase.class);



        job.setReducerClass(TestReducer.class);
        job.setMapperClass(TestMapper.class);

        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(Text.class);


        HadoopBatchStatus result = null; //client.hadoopBatch(job, null);
        System.out.println(result);
    }


    private void setupArchiveRecordInput(Job job, Configuration conf, Path inputDir) throws IOException {
        System.out.println("Inputdir=" + inputDir.makeQualified(inputDir.getFileSystem(conf)));
        job.setInputFormatClass(ArchiveInputFormat.class);
        //Filter for filenames
        try {
            WholeFileInputFormat.addInputPath(job, inputDir);
        } catch (IOException e) {
            throw new IOFailure("message", e);
        }
        WholeFileInputFormat.setInputPathFilter(job, MyPathFilter.class);
        WholeFileInputFormat.setInputDirRecursive(job, true);
    }
}