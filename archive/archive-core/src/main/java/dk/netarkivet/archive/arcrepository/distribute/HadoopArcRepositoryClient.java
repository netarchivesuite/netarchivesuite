package dk.netarkivet.archive.arcrepository.distribute;

import java.io.File;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.distribute.RemoteFile;
import dk.netarkivet.common.distribute.arcrepository.BatchStatus;
import dk.netarkivet.common.distribute.arcrepository.BitarchiveRecord;
import dk.netarkivet.common.distribute.arcrepository.ProcessorRepository;
import dk.netarkivet.common.distribute.arcrepository.ReaderRepository;
import dk.netarkivet.common.distribute.arcrepository.Replica;
import dk.netarkivet.common.distribute.arcrepository.ReplicaStoreState;
import dk.netarkivet.common.distribute.arcrepository.UploadRepository;
import dk.netarkivet.common.distribute.hadoop.HadoopBatchJob;
import dk.netarkivet.common.distribute.hadoop.HadoopRemoteFile;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;

public class HadoopArcRepositoryClient extends
        ComposedArcRepositoryClient<HadoopBatchJob, BitmagUploadRepository, JMSReaderRepository> {

    private static final Logger log = LoggerFactory.getLogger(HadoopArcRepositoryClient.class);

    public HadoopArcRepositoryClient(
            ProcessorRepository<HadoopBatchJob> processorRepository,
            UploadRepository uploadRepository,
            ReaderRepository readerRepository) {
        super(processorRepository, uploadRepository, readerRepository);
    }


    //TODO move the following to an implementation of ProcessorRepository<HadoopBatchJob> and make it work
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

}
