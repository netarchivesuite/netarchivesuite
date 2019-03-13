package dk.netarkivet.archive.arcrepository.distribute;

import dk.netarkivet.common.distribute.arcrepository.BatchStatus;
import dk.netarkivet.common.distribute.arcrepository.ProcessorRepository;
import dk.netarkivet.common.utils.batch.FileBatchJob;

/**
 * Created by csr on 3/13/19.
 */
public class JMSProcessorRepository implements ProcessorRepository<FileBatchJob> {

    @Override public BatchStatus batch(FileBatchJob job, String replicaId, String... args) {
        throw new RuntimeException("not implemented");
    }

    @Override public void close() {
        throw new RuntimeException("not implemented");
    }
}
