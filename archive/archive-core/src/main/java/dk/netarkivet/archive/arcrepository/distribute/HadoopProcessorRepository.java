package dk.netarkivet.archive.arcrepository.distribute;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.io.output.CountingOutputStream;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapred.TIPStatus;
import org.apache.hadoop.mapreduce.InputFormat;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.TaskReport;
import org.apache.hadoop.mapreduce.TaskType;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.util.ClassUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.distribute.FileRemoteFile;
import dk.netarkivet.common.distribute.arcrepository.BatchStatus;
import dk.netarkivet.common.distribute.arcrepository.ProcessorRepository;
import dk.netarkivet.common.distribute.hadoop.HadoopBatchJob;
import dk.netarkivet.common.distribute.hadoop.HadoopBatchStatus;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.batch.BatchJob;

/**
 *
 */
public class HadoopProcessorRepository implements ProcessorRepository<HadoopBatchJob> {

    protected static final Logger log = LoggerFactory.getLogger(HadoopProcessorRepository.class);

    @Override public BatchStatus batch(HadoopBatchJob batchJob, String replicaId, String... args) {

        Job hadoopJob = batchJob.getHadoopJob();

        try {
            Class<? extends InputFormat<?, ?>> inputFormatClass = hadoopJob.getInputFormatClass();
            if (FileInputFormat.class.isAssignableFrom(inputFormatClass)) {
                for (URI filesToProcess : batchJob.getFilesToProcess()) {
                    FileInputFormat.addInputPath(hadoopJob, new Path(filesToProcess));
                }
            }

            HadoopBatchJob.addJarToClasspath(hadoopJob, new File(hadoopJob.getJar()));
            HadoopBatchJob.addJarToClasspath(hadoopJob, new File(ClassUtil.findContainingJar(HadoopBatchJob.class)));

            List<BatchJob.ExceptionOccurrence> exceptions = new ArrayList<>();
            File errorLog = new File("errorLog");
            boolean success = runJob(batchJob, exceptions, errorLog);

            return new HadoopBatchStatus(
                    success,
                    batchJob.getFilesToProcess().size(),
                    null,
                    replicaId,
                    batchJob.getOuputFile(),
                    new FileRemoteFile(errorLog),
                    exceptions);

        } catch (IOException | ClassNotFoundException e) {
            throw new IOFailure("message", e);
        }

    }

    private boolean runJob(HadoopBatchJob batchJob,
            List<BatchJob.ExceptionOccurrence> exceptions,
            File resultFile) {

        boolean success = true;

        PrintStream stdOut = System.out;
        PrintStream stdErr = System.err;

        try (CountingOutputStream errorLog = new CountingOutputStream(new FileOutputStream(resultFile))) {

            System.setOut(new PrintStream(errorLog));
            System.setErr(new PrintStream(errorLog));

            try {
                //First we try to initialise and collect the exceptions

                try {
                    batchJob.initialize(errorLog);
                } catch (Exception e) {
                    exceptions.add(new BatchJob.ExceptionOccurrence(true, errorLog.getByteCount(), e));
                    success = false;
                }

                //If the initialization succeeded, try the running
                if (success) {
                    try {
                        success = batchJob.process(errorLog);


                        try {
                            success= batchJob.getHadoopJob().waitForCompletion(true);
                        } catch (Exception e) {
                            throw new IOFailure("Message", e);
                        }


                    } catch (Exception e) {
                        exceptions.add(new BatchJob.ExceptionOccurrence(new File("TODO"), 1,
                                errorLog.getByteCount(), e));
                        success = false;
                    }
                }

                try {
                    batchJob.finish(errorLog);
                } catch (Exception e) {
                    exceptions.add(new BatchJob.ExceptionOccurrence(false, errorLog.getByteCount(), e));
                    success = false;
                }

                Job job = batchJob.getHadoopJob();
                try {
                    String jobStatus = job.getStatus().toString();

                    //TODO more info about failed tasks
                    List<String[]> diagnostics = Arrays.stream(job.getTaskReports(TaskType.MAP))
//                            .filter(task -> task.getCurrentStatus() == TIPStatus.FAILED)
                            .map(TaskReport::getDiagnostics).collect(Collectors.toList());
                    for (String[] diagnostic : diagnostics) {
                        log.error(Arrays.asList(diagnostic).toString());
                    }
                } catch (IOException | InterruptedException e) {
                    throw new IOFailure("message", e);
                }

            } finally {
                System.setOut(stdOut);
                System.setErr(stdErr);
            }

        } catch (IOException e) {
            exceptions.add(new BatchJob.ExceptionOccurrence(false, BatchJob.ExceptionOccurrence.UNKNOWN_OFFSET, e));
            success = false;
        }
        return success;
    }

    @Override public void close() {
    }

    public Configuration getHadoopConfiguration() throws URISyntaxException {

        System.setProperty("HADOOP_USER_NAME", "dkm_eld");

        Configuration conf = new Configuration(true);
        conf.set("hdp.version", "2.6.0.3-8");

        conf.addResource("yarn-site.xml");
        conf.addResource("hdfs-site.xml");
        conf.addResource("mapred-site.xml");

        conf.set(Job.WORKING_DIR, "/user/dkm_eld/");

        conf.reloadConfiguration();
        return conf;
    }

    /**
     * Hackiest hack of https://stackoverflow.com/a/496849
     *
     * @param newenv
     * @throws Exception
     */
    public static void set(Map<String, String> newenv) throws Exception {
        Class[] classes = Collections.class.getDeclaredClasses();
        Map<String, String> env = System.getenv();
        for (Class cl : classes) {
            if ("java.util.Collections$UnmodifiableMap".equals(cl.getName())) {
                Field field = cl.getDeclaredField("m");
                field.setAccessible(true);
                Object obj = field.get(env);
                Map<String, String> map = (Map<String, String>) obj;
                map.clear();
                map.putAll(newenv);
            }
        }
    }

}
