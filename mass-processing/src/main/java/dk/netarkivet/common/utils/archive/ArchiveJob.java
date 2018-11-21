package dk.netarkivet.common.utils.archive;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.stream.StreamSupport;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
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
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

import dk.netarkivet.common.distribute.hadoop.ArchiveInputFormat;
import dk.netarkivet.common.distribute.hadoop.HadoopBatchStatus;
import dk.netarkivet.common.distribute.hadoop.HadoopViewerArcRepositoryClient;
import dk.netarkivet.common.distribute.hadoop.WholeFileInputFormat;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.InputStreamUtils;

public class ArchiveJob extends Configured implements Tool {

    public static void main(String[] args) throws Exception {
        System.exit(ToolRunner.run(new ArchiveJob(), args));
    }

    @Override public int run(String[] args) throws Exception {

        HadoopViewerArcRepositoryClient client = new HadoopViewerArcRepositoryClient();

        Job job = Job.getInstance();
        Configuration conf = job.getConfiguration();

        job.setJobName(ArchiveJob.class.getName());
        job.setJarByClass(ArchiveJob.class);

        //Set up input format
        Path inputDir = new Path(args[0]);
        setupArchiveRecordInput(job, conf, inputDir);

        job.setMapperClass(ChecksumMapper.class);
        job.setMapOutputKeyClass(Text.class);
        job.setMapOutputValueClass(ArchiveRecordBase.class);

        job.setReducerClass(DuplicateReducer.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(Text.class);

        HadoopBatchStatus result = client.hadoopBatch(job, null);

        System.out.println(result);
        try (InputStream resultFile = result.getResultFile().getInputStream()) {
            IOUtils.copy(resultFile, System.out);
        } finally {
            result.getResultFile().cleanup();
        }

        return result.isSuccess() ? 0 : 1;

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

    public static class MyPathFilter extends Configured implements PathFilter {

        @Override public boolean accept(Path path) {
            String name = path.getName();

            try {
                FileSystem fileSystem = FileSystem.get(getConf());

                if (fileSystem.isDirectory(path)) {
                    return true;
                }
                return name.endsWith(".warc") || name.endsWith(".warc.gz") || name.endsWith(".arc") || name
                        .endsWith(".arc.gz");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static class ChecksumMapper extends Mapper<Text, ArchiveRecordBase, Text, Text> {

        @Override protected void map(Text key, ArchiveRecordBase value, Context context)
                throws IOException, InterruptedException {
            try (InputStream in = value.getInputStream()) {
                String digest = DigestUtils.md5Hex(in);
                context.write(new Text(digest), key);
            }
        }
    }

    public static class DuplicateReducer extends Reducer<Text, Text, Text, Text> {

        @Override protected void reduce(Text key, Iterable<Text> values, Context context)
                throws IOException, InterruptedException {
            long size = StreamSupport.stream(values.spliterator(), false).count();
            if (size > 1) {
                for (Text value : values) {
                    context.write(key, value);
                }
            }
        }
    }

}
