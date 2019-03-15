package dk.netarkivet.archive.arcrepository.distribute;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapred.TIPStatus;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.TaskReport;
import org.apache.hadoop.mapreduce.TaskType;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.distribute.RemoteFile;
import dk.netarkivet.common.distribute.arcrepository.BatchStatus;
import dk.netarkivet.common.distribute.arcrepository.ProcessorRepository;
import dk.netarkivet.common.distribute.hadoop.HadoopBatchJob;
import dk.netarkivet.common.distribute.hadoop.HadoopBatchStatus;
import dk.netarkivet.common.distribute.hadoop.HadoopRemoteFile;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.batch.BatchJob;

/**
 *
 */
public class HadoopProcessorRepository implements ProcessorRepository<HadoopBatchJob> {

    protected static final Logger log = LoggerFactory.getLogger(HadoopProcessorRepository.class);


    @Override public BatchStatus batch(HadoopBatchJob batchJob, String replicaId, String... args) {

        Job hadoopJob = batchJob.getHadoopJob();

        //We want to do as much work as possible and return results for the successes, along with a list of the files that
        //failed to process.

        boolean success = true;


        List<BatchJob.ExceptionOccurrence> exceptions = new ArrayList<>();

        FileSystem srcFS;
        try {
            srcFS = FileSystem.get(hadoopJob.getConfiguration());
        } catch (IOException e){
            throw new IOFailure("message", e);
        }


        Path newResultFileTODO = new Path("newResultFileTODO");

        if (success) {
            success = runJob(batchJob, exceptions, srcFS, newResultFileTODO);
        }

        HadoopRemoteFile hadoopRemoteResultFile = new HadoopRemoteFile(newResultFileTODO, srcFS);


        int num_files = -1;
        try {
            if (hadoopJob.getInputFormatClass().isInstance(FileInputFormat.class)) {
                num_files = Integer.parseInt(hadoopJob.getConfiguration().get(FileInputFormat.NUM_INPUT_FILES));
            }
        } catch (ClassNotFoundException e) {
            success = false;
            throw new IOFailure("message",e);
        }

        try {
            //TODO info about failed tasks
            Arrays.stream(hadoopJob.getTaskReports(TaskType.MAP))
                    .filter(task -> task.getCurrentStatus() == TIPStatus.FAILED)
                    .map(TaskReport::getDiagnostics)
                    .forEachOrdered(errors -> log.error(Arrays.asList(errors).toString()));
        } catch (IOException | InterruptedException e) {
            success = false;
            throw new IOFailure("message",e);
        }

        HadoopBatchStatus jobStatus = new HadoopBatchStatus(
                success,
                num_files,
                null,
                replicaId,
                hadoopRemoteResultFile,
                exceptions);

        return jobStatus;
    }

    private boolean runJob(HadoopBatchJob batchJob, List<BatchJob.ExceptionOccurrence> exceptions,
            FileSystem srcFS, Path newResultFileTODO) {
        boolean success = true;
        try (ByteArrayOutputStream errorLog = new ByteArrayOutputStream();) {
          try {

              //First we try to initialise and collect the exceptions
              try {
                  batchJob.initialize(errorLog);
              } catch (Exception e){
                  exceptions.add(new BatchJob.ExceptionOccurrence(true, errorLog.size(),e));
                  success = false;
              }

              //If the initialization succeeded, try the running
              if (success) {
                  try {
                      success = batchJob.process(errorLog);
                  } catch (Exception e){
                      exceptions.add(new BatchJob.ExceptionOccurrence(null, BatchJob.ExceptionOccurrence.UNKNOWN_OFFSET,
                              errorLog.size(),e));
                      success = false;
                  }
              }

              if (success) {
                  try {
                      batchJob.finish(errorLog);
                  } catch (Exception e){
                      exceptions.add(new BatchJob.ExceptionOccurrence(false, errorLog.size(),e));
                      success = false;
                  }
              }
          } finally {
              try (OutputStream resultFile = srcFS.create(newResultFileTODO);
                      ByteArrayInputStream inputStream = new ByteArrayInputStream(errorLog.toByteArray())) {
                  IOUtils.copyLarge(inputStream, resultFile);
              }
          }
        } catch (IOException e) {
            exceptions.add(new BatchJob.ExceptionOccurrence(false, BatchJob.ExceptionOccurrence.UNKNOWN_OFFSET,e));
            success = false;
        }
        return success;
    }

    private RemoteFile getResultFile(Path outputDir, FileSystem srcFS) {
         return new HadoopRemoteFile(outputDir, srcFS);
    }


    @Override public void close() {
    }
}
