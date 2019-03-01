package dk.netarkivet.wayback.indexer.hadoop.cdx;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.PathFilter;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;

import dk.netarkivet.common.distribute.hadoop.HadoopBatchJob;
import dk.netarkivet.common.distribute.hadoop.HadoopRemoteFile;
import dk.netarkivet.common.distribute.hadoop.WholeFileInputFormat;
import dk.netarkivet.common.exceptions.IOFailure;

public class CDXBatchJob extends HadoopBatchJob {

    private Job job;
    private Path outputDir;

    @Override
    public HadoopRemoteFile getOuputFile() throws IOException {
        return new HadoopRemoteFile(outputDir,FileSystem.get(job.getConfiguration()));
    }

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

        job.setInputFormatClass(WholeFileInputFormat.class);
        try {
            setupWholeFileInput(job,getConf());
        } catch (IOException e){
            throw new RuntimeException(e);
            //TODO collect exceptions
        }

        job.setMapperClass(CDXMap.class);

        job.setMapOutputKeyClass(Text.class);
        job.setMapOutputValueClass(Text.class);

        job.setCombinerClass(CDXCombiner.class);

        job.setReducerClass(CDXReduce.class);

        job.setNumReduceTasks(1);

        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(Text.class);

        job.setOutputFormatClass(TextOutputFormat.class);
        outputDir = new Path(UUID.randomUUID().toString());
        TextOutputFormat.setOutputPath(job, outputDir);


    }


    private void setupWholeFileInput(Job job, Configuration conf) throws IOException {
        job.setInputFormatClass(WholeFileInputFormat.class);
        for (URI filesToProcess : getFilesToProcess()) {
            Path pathToProcess = new Path(filesToProcess);
            WholeFileInputFormat.addInputPath(job, pathToProcess);
        }
        conf.setPattern("fileNamePattern", getFilenamePattern());
        WholeFileInputFormat.setInputPathFilter(job, MyPathFilter.class);
        WholeFileInputFormat.setInputDirRecursive(job, true);
    }



    public static class MyPathFilter extends Configured implements PathFilter {

        private Pattern fileNamePattern = null;

        @Override public boolean accept(Path path) {
            String name = path.getName();

            if (fileNamePattern == null){
                fileNamePattern = getConf().getPattern("fileNamePattern", Pattern.compile(EVERYTHING_REGEXP));
            }
            try {
                FileSystem fileSystem = FileSystem.get(getConf());

                if (fileSystem.isDirectory(path)) {
                    return true;
                }

                return fileNamePattern.matcher(name).matches();
            } catch (IOException e) {
                throw new IOFailure("message", e);
            }
        }
    }

    @Override
    public boolean process(OutputStream os) {

        try {
            return job.waitForCompletion(true);
        } catch (IOException | InterruptedException | ClassNotFoundException e){
            throw new RuntimeException(e);
        }
    }

    @Override
    public void finish(OutputStream os) {
        super.finish(os);
    }
}
