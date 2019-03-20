package dk.netarkivet.archive.arcrepository.distribute;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.ClassUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.distribute.hadoop.HadoopBatchJob;
import dk.netarkivet.common.distribute.hadoop.WholeFileInputFormat;
import dk.netarkivet.common.exceptions.IOFailure;

public class MockupHadoopBatchJob extends HadoopBatchJob {

    protected static final Logger log = LoggerFactory.getLogger(MockupHadoopBatchJob.class);

    public MockupHadoopBatchJob(Configuration hadoopConf) {
        try {
            job = Job.getInstance(hadoopConf);

            job.setJobName(this.getClass().getName());
            job.setJarByClass(this.getClass());

            job.setInputFormatClass(WholeFileInputFormat.class);

            job.setMapperClass(TestMapper.class);

            job.setMapOutputKeyClass(Text.class);
            job.setMapOutputValueClass(Text.class);

            job.setReducerClass(TestReducer.class);

            job.setOutputKeyClass(Text.class);
            job.setOutputValueClass(Text.class);

        } catch (IOException e) {
            throw new IOFailure("message", e);
        }

        outputDir = new Path(UUID.randomUUID().toString());
    }

    @Override public void initialize(OutputStream os) {
        FileOutputFormat.setOutputPath(job, outputDir);
        try {
            addJarToClasspath(getHadoopJob(), new File(ClassUtil.findContainingJar(job.getInputFormatClass())));
            addJarToClasspath(getHadoopJob(), new File(ClassUtil.findContainingJar(job.getMapperClass())));
            addJarToClasspath(getHadoopJob(), new File(ClassUtil.findContainingJar(job.getReducerClass())));
        } catch (IOException | ClassNotFoundException e) {
            throw new IOFailure("Message", e);
        }
    }

    @Override public void finish(OutputStream os) {

    }


}
