import dk.netarkivet.common.utils.hadoop.GetMetadataMapper;
import dk.netarkivet.common.utils.hadoop.HadoopJob;
import dk.netarkivet.common.utils.hadoop.HadoopJobStrategy;
import dk.netarkivet.common.utils.hadoop.HadoopJobTool;
import dk.netarkivet.common.utils.hadoop.HadoopJobUtils;
import dk.netarkivet.common.utils.hadoop.MetadataExtractionStrategy;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.util.ToolRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

public class MetadataIndexingApplication {
    private static final Logger log = LoggerFactory.getLogger(MetadataIndexingApplication.class);

    private static void usage() {
        System.out.println("Usage: java MetadataIndexingApplication <inputFile>");
    }

    /**
     * Start a hadoop job that fetches seeds reports out of metadata files. The single input argument
     * is a path to the input file in the local file system. The input file is a newline-separated
     * list of metadata paths to be processed. The lines input paths may be any combination of "file://"
     * and "hdfs://" URIs.
     *
     * @param args
     * @throws IOException
     * @throws InterruptedException
     */
    public static void main(String[] args) throws Exception {

        if (args.length < 1) {
            usage();
            System.exit(1);
        }
        String localInputFileString = args[0];
        if (localInputFileString == null || localInputFileString.length() == 0) {
            usage();
            System.exit(1);
        }
        File localInputFile = new File(localInputFileString);
        if (!localInputFile.exists() && localInputFile.isFile()) {
            System.out.println("No such file " + localInputFile.getAbsolutePath());
            usage();
            System.exit(2);
        }

        HadoopJobUtils.doKerberosLogin();
        Configuration conf = HadoopJobUtils.getConf();
        conf.setPattern(GetMetadataMapper.URL_PATTERN,  Pattern.compile("metadata://[^/]*/crawl/reports/seeds-report.txt.*"));
        conf.setPattern(GetMetadataMapper.MIME_PATTERN,  Pattern.compile(".*"));

        try (FileSystem fileSystem = FileSystem.newInstance(conf)) {
            long id = 0L;
            HadoopJobStrategy jobStrategy = new MetadataExtractionStrategy(id, fileSystem);
            HadoopJob job = new HadoopJob(id, jobStrategy);
            UUID uuid = UUID.randomUUID();
            Path jobInputFile = jobStrategy.createJobInputFile(uuid);
            job.setJobInputFile(jobInputFile);
            log.info("Putting local input file in hdfs at " + jobInputFile);
            fileSystem.copyFromLocalFile(false, new Path(localInputFile.getAbsolutePath()),
                    jobInputFile);
            Path jobOutputDir = jobStrategy.createJobOutputDir(uuid);
            job.setJobOutputDir(jobOutputDir);
            ToolRunner.run(new HadoopJobTool(conf, new GetMetadataMapper()),
                    new String[] {jobInputFile.toString(), jobOutputDir.toString()});
        }
    }
}
