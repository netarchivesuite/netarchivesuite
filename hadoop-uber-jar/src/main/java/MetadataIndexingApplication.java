import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.HdfsConfiguration;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapreduce.MRConfig;
import org.apache.hadoop.security.UserGroupInformation;
import org.apache.hadoop.util.ToolRunner;
import org.apache.hadoop.yarn.conf.YarnConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.utils.LoggingOutputStream;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.utils.hadoop.GetMetadataMapper;
import dk.netarkivet.common.utils.hadoop.HadoopFileUtils;
import dk.netarkivet.common.utils.hadoop.HadoopJob;
import dk.netarkivet.common.utils.hadoop.HadoopJobStrategy;
import dk.netarkivet.common.utils.hadoop.HadoopJobTool;
import dk.netarkivet.common.utils.hadoop.MetadataExtractionStrategy;

public class MetadataIndexingApplication {

    private static final Logger log = LoggerFactory.getLogger(MetadataIndexingApplication.class);


    // /user/nat-csr/9385-metadata-1.warc.gz

    public static void main(String[] args) throws IOException, InterruptedException {

        UserGroupInformation ugi = UserGroupInformation.loginUserFromKeytabAndReturnUGI("nat-csr@KBHPC.KB.DK", "/home/colin/nat-csr.keytab");
        ugi.doAs( (PrivilegedExceptionAction<Integer>)() -> {
            Configuration conf = new JobConf(new YarnConfiguration(new HdfsConfiguration()));
            conf.set("mapreduce.job.am-access-disabled","true");
            conf.set("yarn.timeline-service.enabled", "false");
            conf.set(MRConfig.FRAMEWORK_NAME, MRConfig.YARN_FRAMEWORK_NAME);
            conf.setPattern(GetMetadataMapper.URL_PATTERN,  Pattern.compile(".*"));
            conf.setPattern(GetMetadataMapper.MIME_PATTERN,  Pattern.compile(".*"));
            final String jarPath = Settings.get(CommonSettings.HADOOP_MAPRED_UBER_JAR);
            if (jarPath == null || !(new File(jarPath)).exists()) {
                log.warn("Specified jar file {} does not exist.", jarPath);
                throw new RuntimeException("Jar file " + jarPath + " does not exist.");
            }
            conf.set("mapreduce.job.jar", jarPath);
            FileSystem fileSystem = FileSystem.newInstance(conf);
            Long id = 0L;
            HadoopJobStrategy jobStrategy = new MetadataExtractionStrategy(id, fileSystem);
            HadoopJob job = new HadoopJob(id, jobStrategy);
            UUID uuid = UUID.randomUUID();
            Path jobInputFile = jobStrategy.createJobInputFile(uuid);
            Path jobOutputDir = jobStrategy.createJobOutputDir(uuid);
            job.setJobInputFile(jobInputFile);
            job.setJobOutputDir(jobOutputDir);
            java.nio.file.Path localInputTempFile = HadoopFileUtils.makeLocalInputTempFile();
            log.info("Local input path is " + localInputTempFile);
            List<java.nio.file.Path> filePaths = new ArrayList<>();
            for (String filepath: args) {
                log.info("Adding file " + filepath + " to input.");
                filePaths.add(localInputTempFile.getFileSystem().getPath(filepath));
            }
            writeHdfsInputFileLinesToInputFile(filePaths, localInputTempFile);
            log.info("Putting local input file in hdfs at " + jobInputFile);
            fileSystem.copyFromLocalFile(true, new Path(localInputTempFile.toAbsolutePath().toString()),
                    jobInputFile);
            //fileSystem.open(jobInputFile);
            //job.run();
            ToolRunner.run(new HadoopJobTool(conf, new GetMetadataMapper()),
                    new String[] {jobInputFile.toString(), jobOutputDir.toString()});
            return 0;
        } );
    }

    public static void writeHdfsInputFileLinesToInputFile(List<java.nio.file.Path> files,
            java.nio.file.Path inputFilePath) throws IOException {
        if (files.size() == 0) {
            return;
        }
        java.nio.file.Path lastElem = files.get(files.size() - 1);
        for (java.nio.file.Path file : files) {
            String inputLine = "hdfs://" + file.toString() + "\n";
            if (file.equals(lastElem)) {
                // Not writing newline on last line to avoid a mapper being spawned on no input
                inputLine = "hdfs://" + file.toString();
            }
            Files.write(inputFilePath, inputLine.getBytes(), StandardOpenOption.APPEND);
        }
    }

}
