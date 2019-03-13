package dk.netarkivet.common.distribute.arcrepository;

import dk.netarkivet.common.utils.batch.BatchJob;

/**
 * Interface that isolates the mass-processing functionality of a repository.
 */
public interface ProcessorRepository<J extends BatchJob> extends ExceptionlessAutoCloseable {

    /**
     * Runs a batch batch job on each file in the ArcRepository.
     *
     * @param job An object that implements the FileBatchJob interface. The initialize() method will be called before
     * processing and the finish() method will be called afterwards. The process() method will be called with each File
     * entry. An optional function postProcess() allows handling the combined results of the batchjob, e.g. summing the
     * results, sorting, etc.
     * @param replicaId The archive to execute the job on.
     * @param args The arguments for the batchjob.
     * @return The status of the batch job after it ended.
     */
    BatchStatus batch(J job, String replicaId, String... args);

}
