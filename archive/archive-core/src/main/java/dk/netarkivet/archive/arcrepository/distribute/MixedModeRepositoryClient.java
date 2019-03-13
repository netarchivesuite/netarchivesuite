package dk.netarkivet.archive.arcrepository.distribute;

import dk.netarkivet.common.utils.batch.FileBatchJob;

/**
 * This class should replace JMSBitmagArcRepositoryClient once we have implemented the three client interfaces it
 * is composed of.
 */
public class MixedModeRepositoryClient extends
        ComposedArcRepositoryClient<FileBatchJob, BitmagUploadRepository, JMSReaderRepository> {

    public MixedModeRepositoryClient() {
        super (new JMSProcessorRepository(), new BitmagUploadRepository(), new JMSReaderRepository());
    }

}
