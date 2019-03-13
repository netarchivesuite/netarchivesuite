package dk.netarkivet.archive.arcrepository.distribute;

import dk.netarkivet.common.distribute.arcrepository.ProcessorRepository;
import dk.netarkivet.common.utils.batch.FileBatchJob;

/**
 * Created by csr on 3/13/19.
 */
public class MixedModeClient extends
        ComposedArcRepositoryClient<FileBatchJob, BitmagUploadRepository, JMSReaderRepository> {
    public MixedModeClient(
            ProcessorRepository<FileBatchJob> processorRepository,
            BitmagUploadRepository uploadRepository,
            JMSReaderRepository readerRepository) {
        super(processorRepository, uploadRepository, readerRepository);
    }
}
