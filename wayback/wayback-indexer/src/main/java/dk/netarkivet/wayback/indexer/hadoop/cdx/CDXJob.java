package dk.netarkivet.wayback.indexer.hadoop.cdx;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.NLineInputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;

public class CDXJob extends HadoopJob {

    private Job job;


    @Override
    public void initialize(OutputStream os) {
        Configuration conf = new Configuration();
        try {
            job = Job.getInstance(conf, this.getClass().getName());
        } catch (IOException e){
            throw new RuntimeException(e);
            //TODO collect exceptions
        }
        job.setJarByClass(this.getClass());



        //TODO probably better if we can give it a folder or glob rather than a file of files
        job.setInputFormatClass(NLineInputFormat.class);
        NLineInputFormat.setNumLinesPerSplit(job, 1);


        job.setMapperClass(CDXMap.class);

        job.setMapOutputKeyClass(Text.class);
        job.setMapOutputValueClass(Text.class);

        job.setCombinerClass(CDXCombiner.class);

        job.setReducerClass(CDXReduce.class);

        job.setNumReduceTasks(1);

        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(Text.class);

        job.setOutputFormatClass(TextOutputFormat.class);
        TextOutputFormat.setOutputPath(job, new Path(args[1]));

    }

    @Override public boolean processFile(File file, OutputStream os) {
        NLineInputFormat.addInputPath(job, new Path(args[0]));

        try {
            return job.waitForCompletion(true);
        } catch (IOException | InterruptedException | ClassNotFoundException e){

        }
    }

    @Override
    public void finish(OutputStream os) {
        super.finish(os);
    }
}
