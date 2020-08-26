package dk.netarkivet.wayback.hadoop;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.NLineInputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.util.Tool;

/**
 * A simple Hadoop map-only job that when given an input file containing new-line separated file paths
 * creates cdx-indexes and outputs these to new files in the given output path
 */
public class CDXJob extends Configured implements Tool {

    public CDXJob(Configuration conf) {
        super(conf);
    }

    /**
     * Method for running the job.
     * @param args Expects two strings representing the job's in- and output paths (Tool interface dictates String[])
     * @return An exitcode to report back if the job succeeded.
     * @throws InterruptedException, IOException, ClassNotFoundException
     */
    @Override
    public int run(String[] args) throws InterruptedException, IOException, ClassNotFoundException {
        Path inputPath = new Path(args[0]);
        Path outputPath = new Path(args[1]);
        Configuration conf = getConf();
        Job job = Job.getInstance(conf, this.getClass().getName());

        //job.setJarByClass(this.getClass());
        job.setInputFormatClass(NLineInputFormat.class);
        job.setOutputFormatClass(TextOutputFormat.class);
        NLineInputFormat.addInputPath(job, inputPath);
        TextOutputFormat.setOutputPath(job, outputPath);
        job.setMapperClass(CDXMap.class);
        job.setNumReduceTasks(0); // Ensure job is map-only

        // How many files should each node process at a time (how many lines are read from the input file)
        NLineInputFormat.setNumLinesPerSplit(job, 5);

        // In- and output types
        job.setMapOutputKeyClass(NullWritable.class);
        job.setMapOutputValueClass(Text.class);
        job.setOutputKeyClass(NullWritable.class);
        job.setOutputValueClass(Text.class);

        return job.waitForCompletion(true) ? 0 : 1;
    }
}
