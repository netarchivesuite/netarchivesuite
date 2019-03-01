package dk.netarkivet.common.distribute.hadoop;

import java.io.File;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.distribute.RemoteFile;
import dk.netarkivet.common.distribute.arcrepository.ArcRepositoryClient;
import dk.netarkivet.common.distribute.arcrepository.BatchStatus;
import dk.netarkivet.common.distribute.arcrepository.BitarchiveRecord;
import dk.netarkivet.common.distribute.arcrepository.Replica;
import dk.netarkivet.common.distribute.arcrepository.ReplicaStoreState;
import dk.netarkivet.common.distribute.arcrepository.ViewerArcRepositoryClient;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;

public class HadoopArcRepositoryClient implements ArcRepositoryClient<HadoopBatchJob>,
        ViewerArcRepositoryClient<HadoopBatchJob> {

    private static final Logger log = LoggerFactory.getLogger(HadoopArcRepositoryClient.class);

    @Override public BitarchiveRecord get(String arcfile, long index) throws ArgumentNotValid {
        return null;
    }

    @Override public void getFile(String arcfilename, Replica replica, File toFile) {

    }

    @Override public void store(File file) throws IOFailure, ArgumentNotValid {

   }

    @Override public BatchStatus batch(HadoopBatchJob job, String replicaId, String... args) {
        return null;
    }
    //
//    @Override public BatchStatus batch(BatchJob job, String replicaId, String... args) {
//        ArgumentNotValid.checkNotNull(job, "BatchJob job");
//        ArgumentNotValid.checkNotNullOrEmpty(replicaId, "String replicaId");
//
//        if (job instanceof HadoopBatchJob) {
//            HadoopBatchJob hadoopJob = (HadoopBatchJob) job;
//
//            OutputStream os = null;
//            File resultFile;
//            try {
//                resultFile = File.createTempFile("batch", replicaId, FileUtils.getTempDir());
//
//                os = new FileOutputStream(resultFile);
//
//
//                //TODO calc files in a better way
//                List<File> files = new ArrayList<File>();
//
//                final FilenameFilter filenameFilter = new FilenameFilter() {
//                    public boolean accept(File dir, String name) {
//                        Pattern filenamePattern = job.getFilenamePattern();
//                        return new File(dir, name).isFile()
//                                && (filenamePattern == null || filenamePattern.matcher(name).matches());
//                    }
//                };
//
////                for (File dir : storageDirs) {
////                    File[] filesInDir = dir.listFiles(filenameFilter);
////                    if (filesInDir != null) {
////                        files.addAll(Arrays.asList(filesInDir));
////                    }
////                }
//
//                hadoopBatch(hadoopJob,os, files, args);
//
//            } catch (IOException e) {
//                throw new IOFailure("Cannot perform batch '" + job + "'", e);
//            } finally {
//                if (os != null) {
//                    try {
//                        os.close();
//                    } catch (IOException e) {
//                        log.warn("Error closing batch output stream '{}'", os, e);
//                    }
//                }
//            }
//            return new BatchStatus(replicaId, job.getFilesFailed(), job.getNoOfFilesProcessed(), new FileRemoteFile(
//                    resultFile), job.getExceptions());
//
//        } else {
//            throw new ClassCastException();
//        }
//
//    }

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
/*
    public HadoopBatchStatus hadoopBatch(HadoopBatchJob job,
            OutputStream errorLog, List<Path> files, String... args) {
        //We want to do as much work as possible and return results for the successes, along with a list of the files that
        //failed to process.


        boolean success;
        try {
            job.initialize(errorLog, args);

            job.process();

            job.finish(errorLog);
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
    }*/

    private RemoteFile getResultFile(Path outputDir, FileSystem srcFS) {
        return new HadoopRemoteFile(outputDir, srcFS);
    }

}
