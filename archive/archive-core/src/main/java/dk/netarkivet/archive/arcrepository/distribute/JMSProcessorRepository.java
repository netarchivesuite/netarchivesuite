package dk.netarkivet.archive.arcrepository.distribute;

import dk.netarkivet.common.distribute.arcrepository.BatchStatus;
import dk.netarkivet.common.distribute.arcrepository.ProcessorRepository;
import dk.netarkivet.common.utils.batch.FileBatchJob;

/**
 *
 */
public class JMSProcessorRepository implements ProcessorRepository<FileBatchJob> {

    private final JMSArcRepositoryClient client;

    public JMSProcessorRepository() {
        client = JMSArcRepositoryClient.getInstance();
    }

    @Override public BatchStatus batch(FileBatchJob job, String replicaId, String... args) {
        return client.batch(job, replicaId, args);
    }

    @Override public void close() {
        client.close();
    }
}
