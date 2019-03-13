package dk.netarkivet.archive.arcrepository.distribute;

import dk.netarkivet.common.distribute.arcrepository.BatchStatus;
import dk.netarkivet.common.distribute.arcrepository.ProcessorRepository;
import dk.netarkivet.common.distribute.hadoop.HadoopBatchJob;

/**
 * Created by csr on 3/13/19.
 */
public class HadoopProcessorRepository implements ProcessorRepository<HadoopBatchJob> {
    @Override public BatchStatus batch(HadoopBatchJob job, String replicaId, String... args) {
        throw new RuntimeException("not implemented");
    }

    @Override public void close() {
    }
}
