package dk.netarkivet.archive.indexserver;

import java.util.Set;

import dk.netarkivet.common.distribute.indexserver.RequestType;

public interface IndexRequestServerInterface {

    void setHandler(RequestType cdx, FileBasedCache<Set<Long>> cdxCache);

    void start();

    void close();
}
