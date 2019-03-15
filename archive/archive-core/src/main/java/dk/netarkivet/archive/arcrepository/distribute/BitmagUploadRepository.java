package dk.netarkivet.archive.arcrepository.distribute;

import java.io.File;

import dk.netarkivet.common.distribute.arcrepository.UploadRepository;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;

/**
 *
 */
public class BitmagUploadRepository implements UploadRepository {

    @Override public void store(File file) throws IOFailure, ArgumentNotValid {
        JMSBitmagArcRepositoryClient.getInstance().store(file);
    }

    @Override public void close() {
        JMSBitmagArcRepositoryClient.getInstance().close();
    }
}
