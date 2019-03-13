package dk.netarkivet.archive.arcrepository.distribute;

import dk.netarkivet.common.distribute.arcrepository.BatchStatus;
import dk.netarkivet.common.distribute.arcrepository.ProcessorRepository;
import dk.netarkivet.common.distribute.hadoop.HadoopBatchJob;

/**
 *
 */
public class HadoopProcessorRepository implements ProcessorRepository<HadoopBatchJob> {

    @Override public BatchStatus batch(HadoopBatchJob job, String replicaId, String... args) {

        throw new RuntimeException("not implemented");
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

    //private RemoteFile getResultFile(Path outputDir, FileSystem srcFS) {
    //     return new HadoopRemoteFile(outputDir, srcFS);
    //}


    @Override public void close() {
    }
}
