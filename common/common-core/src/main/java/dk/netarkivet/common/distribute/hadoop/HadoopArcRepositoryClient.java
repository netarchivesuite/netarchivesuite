package dk.netarkivet.common.distribute.hadoop;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.LocalFileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.PathFilter;
import org.apache.hadoop.mapred.TIPStatus;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.MRJobConfig;
import org.apache.hadoop.mapreduce.TaskReport;
import org.apache.hadoop.mapreduce.TaskType;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.distribute.RemoteFile;
import dk.netarkivet.common.distribute.arcrepository.ArcRepositoryClient;
import dk.netarkivet.common.distribute.arcrepository.BatchStatus;
import dk.netarkivet.common.distribute.arcrepository.BitarchiveRecord;
import dk.netarkivet.common.distribute.arcrepository.Replica;
import dk.netarkivet.common.distribute.arcrepository.ReplicaStoreState;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.batch.BatchJob;
import dk.netarkivet.common.utils.batch.FileBatchJob;

public class HadoopArcRepositoryClient implements ArcRepositoryClient {

    private static final Logger log = LoggerFactory.getLogger(HadoopArcRepositoryClient.class);

    @Override public BitarchiveRecord get(String arcfile, long index) throws ArgumentNotValid {
        return null;
    }

    @Override public void getFile(String arcfilename, Replica replica, File toFile) {

    }

    @Override public void store(File file) throws IOFailure, ArgumentNotValid {

    }

    @Override public BatchStatus batch(BatchJob job, String replicaId, String... args) {
        return null;
    }

    @Override public void updateAdminData(String fileName, String bitarchiveId, ReplicaStoreState newval) {

    }

    @Override public void updateAdminChecksum(String filename, String checksum) {

    }

    @Override public File removeAndGetFile(String fileName, String bitarchiveId, String checksum, String credentials) {
        return null;
    }

    @Override public File getAllChecksums(String replicaId) {
        return null;
    }

    @Override public String getChecksum(String replicaId, String filename) {
        return null;
    }

    @Override public File getAllFilenames(String replicaId) {
        return null;
    }

    @Override public File correct(String replicaId, String checksum, File file, String credentials) {
        return null;
    }

    @Override public void close() {

    }

    public HadoopBatchStatus hadoopBatch(Job job,
            String replicaId) {
        //We want to do as much work as possible and return results for the successes, along with a list of the files that
        //failed to process.

        job.setOutputFormatClass(TextOutputFormat.class);
        Path outputDir = new Path("target/temp" + new Date().getTime());
        TextOutputFormat.setOutputPath(job, outputDir);

        boolean success;
        try {
            success = job.waitForCompletion(true);
        } catch (IOException | InterruptedException | ClassNotFoundException e) {
            throw new IOFailure("message", e);
        }
        int num_files = Integer.parseInt(job.getConfiguration().get(FileInputFormat.NUM_INPUT_FILES));

        try {
            Arrays.stream(job.getTaskReports(TaskType.MAP))
                    .filter(task -> task.getCurrentStatus() == TIPStatus.FAILED)
                    .map(TaskReport::getDiagnostics)
                    .forEachOrdered(errors -> log.error(Arrays.asList(errors).toString()));
        } catch (IOException | InterruptedException e) {
            throw new IOFailure("message",e);
        }
        //TODO info about failed tasks

        FileSystem srcFS;
        try {
            srcFS = FileSystem.get(job.getConfiguration());
        } catch (IOException e){
            throw new IOFailure("message", e);
        }
        RemoteFile resultFile = getResultFile(outputDir, srcFS);

        return new HadoopBatchStatus(
                success,
                num_files,
                null,
                replicaId,
                resultFile,
                null);
    }

    private RemoteFile getResultFile(Path outputDir, FileSystem srcFS) {
        return new HadoopRemoteFile(outputDir, srcFS);
    }

}
