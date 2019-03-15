package dk.netarkivet.archive.arcrepository.distribute;

import java.io.File;

import dk.netarkivet.common.distribute.arcrepository.BitarchiveRecord;
import dk.netarkivet.common.distribute.arcrepository.ReaderRepository;
import dk.netarkivet.common.distribute.arcrepository.Replica;
import dk.netarkivet.common.exceptions.ArgumentNotValid;

/**
 * Created by csr on 3/13/19.
 */
public class JMSReaderRepository implements ReaderRepository {

    private final JMSArcRepositoryClient client = JMSArcRepositoryClient.getInstance();

    @Override public BitarchiveRecord get(String arcfile, long index) throws ArgumentNotValid {
        return client.get(arcfile, index);
    }

    @Override public void getFile(String arcfilename, Replica replica, File toFile) {
        client.getFile(arcfilename, replica, toFile);
    }

    @Override public void close() {
        client.close();
    }
}
