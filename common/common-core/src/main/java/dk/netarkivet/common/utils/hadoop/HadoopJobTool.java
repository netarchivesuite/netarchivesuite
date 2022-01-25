package dk.netarkivet.common.utils.hadoop;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.lib.input.NLineInputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.util.Tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A simple generic Hadoop map-only tool that runs a given mapper on the passed input file
 * containing new-line separated file paths and outputs the job's resulting files in the passed output path
 */
public class HadoopJobTool extends Configured implements Tool {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private Mapper<LongWritable, Text, NullWritable, Text> mapper;

    public HadoopJobTool(Configuration conf, Mapper<LongWritable, Text, NullWritable, Text> mapper) {
        super(conf);
        this.mapper = mapper;
    }

    /**
     * Method for running the tool/job.
     * @param args Expects two strings representing the job's in- and output paths (Tool interface dictates String[])
     * @return An exitcode to report back if the job succeeded.
     * @throws InterruptedException, IOException, ClassNotFoundException
     */
    @Override
    public int run(String[] args) throws InterruptedException, IOException, ClassNotFoundException {
        log.info("Entered run method of HadoopJobTool");
        Path inputPath = new Path(args[0]);
        Path outputPath = new Path(args[1]);
        Configuration conf = getConf();
        try (Job job = Job.getInstance(conf)) {
            job.setJobName("HadoopJob using " + mapper.getClass().getSimpleName());

            //job.setJarByClass(this.getClass());
            job.setInputFormatClass(NLineInputFormat.class);
            job.setOutputFormatClass(TextOutputFormat.class);
            NLineInputFormat.addInputPath(job, inputPath);
            TextOutputFormat.setOutputPath(job, outputPath);
            job.setMapperClass(mapper.getClass());
            job.setNumReduceTasks(0); // Ensure job is map-only

            // How many files should each node process at a time (how many lines are read from the input file)
            NLineInputFormat.setNumLinesPerSplit(job, 5);

            // In- and output types
            job.setMapOutputKeyClass(NullWritable.class);
            job.setMapOutputValueClass(Text.class);
            job.setOutputKeyClass(NullWritable.class);
            job.setOutputValueClass(Text.class);

            log.info("Calling waitForCompletion");
            boolean success = job.waitForCompletion(true);
            if (!success){
                log.error("Job {} failed, state is {}."
                                + "See more info at {}",
                        job.getJobID(),
                        job.toString(),
                        job.getHistoryUrl());
            }
            return success ? 0 : 1;
        }
    }
}
