package dk.netarkivet.common.utils.archive;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.PathFilter;
import org.apache.hadoop.io.MD5Hash;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.MRJobConfig;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.distribute.hadoop.ArchiveInputFormat;
import dk.netarkivet.common.distribute.hadoop.HadoopBatchStatus;
import dk.netarkivet.common.distribute.hadoop.HadoopArcRepositoryClient;
import dk.netarkivet.common.exceptions.IOFailure;

public class ArchiveChecksumJob extends Configured implements Tool {

    public static void main(String[] args) throws Exception {
        System.exit(ToolRunner.run(new ArchiveChecksumJob(), args));
    }

    @Override public int run(String[] args) throws Exception {


        Job job = Job.getInstance();
        Configuration conf = job.getConfiguration();

        job.setJobName(ArchiveChecksumJob.class.getName());
        job.setJarByClass(ArchiveChecksumJob.class);

        //Do not retry failures
        conf.set(MRJobConfig.MAP_MAX_ATTEMPTS, "1");

        //Job is not failed due to failures
        conf.set(MRJobConfig.MAP_FAILURES_MAX_PERCENT, "100");

        //Set up input format
        Path inputDir = new Path(args[0]);
        setupArchiveRecordInput(job, conf, inputDir);

        job.setMapperClass(ChecksumMapper.class);

        job.setMapOutputKeyClass(Text.class);
        job.setMapOutputValueClass(Text.class);

        job.setReducerClass(DuplicateReducer.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(Text.class);

        HadoopArcRepositoryClient client = new HadoopArcRepositoryClient();
        HadoopBatchStatus result = client.hadoopBatch(job, null);

        System.out.println(result);
        try (InputStream resultFile = result.getResultFile().getInputStream()) {
//            IOUtils.copy(resultFile, System.out);
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
            ArchiveInputFormat.addInputPath(job, inputDir);
        } catch (IOException e) {
            throw new IOFailure("message", e);
        }
        ArchiveInputFormat.setInputPathFilter(job, MyPathFilter.class);
        ArchiveInputFormat.setInputDirRecursive(job, true);
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
                String digest;
                digest = MD5Hash.digest(in).toString();
                context.write(new Text(digest), key);
            }
        }
    }

    public static class DuplicateReducer extends Reducer<Text, Text, Text, Text> {
        private static final Logger log = LoggerFactory.getLogger(FileChecksumJob.DuplicateReducer.class);

        @Override
        protected void reduce(Text key, Iterable<Text> values, Context context)
                throws IOException, InterruptedException {

            //DANGER WILL ROBINSON, THE ITERABLE IS AN ITERATOR AND CAN ONLY BE READ ONCE
            List<Text> list = new ArrayList<>();
            Iterator<Text> iterator = values.iterator();
            while (iterator.hasNext()) {
                Text next = iterator.next();
                list.add(new Text(next.toString()));
            }

            if (list.size() > 1) {
                for (Text value : list) {
                    context.write(key, value);
                }
            }

        }
    }

}
