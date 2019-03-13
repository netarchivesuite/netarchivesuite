package dk.netarkivet.archive.arcrepository.distribute;

import java.io.File;

import dk.netarkivet.common.distribute.arcrepository.UploadRepository;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;

/**
 * Created by csr on 3/13/19.
 */
public class BitmagUploadRepository implements UploadRepository {
    @Override public void store(File file) throws IOFailure, ArgumentNotValid {
        throw new RuntimeException("not implemented");
    }

    @Override public void close() {
        throw new RuntimeException("not implemented");
    }
}
