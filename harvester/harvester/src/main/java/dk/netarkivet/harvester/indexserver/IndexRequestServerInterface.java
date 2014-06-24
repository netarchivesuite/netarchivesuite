package dk.netarkivet.harvester.indexserver;

import java.util.Set;
import dk.netarkivet.common.distribute.indexserver.RequestType;

/**
 * An interface for all IndexRequestServer implementations.
 */
public interface IndexRequestServerInterface {
    
    /**
     * Define a FileBasedCache class to handle the given type of requests.
     * @param type a given request type
     * @param cache the FileBasedCache class to handle this request type
     */
    void setHandler(RequestType type, FileBasedCache<Set<Long>> cache);
    /**
     * The operation to the start the IndexRequestServer.
     */
    void start();
    /**
     * The operation to the close the IndexRequestServer.
     * This closes all resources associated with the IndexRequestServer
     * and shuts down the server. 
     */
    void close();
}
