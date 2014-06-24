package dk.netarkivet.harvester.indexserver;

import dk.netarkivet.common.distribute.indexserver.RequestType;
import dk.netarkivet.common.utils.CleanupIF;

import java.util.HashSet;
import java.util.Set;

/** Index server.
 * Handles request for lucene indexes of crawl logs and cdx indexes of jobs,
 * using two multifilebasedcache objects as handlers.
 *
 * The server sets up handlers for three kinds of indexes (as defined by
 * RequestType):
 * A CDX index, where each index is one file, gzip-compressed.
 * A DEDUP_CRAWL_LOG index, where each index is multiple files, gzip-compressed,
 * making up a Lucene index of non-HTML files.
 * A FULL_CRAWL_LOG index, where each index is multiple files, gzip-compressed,
 * making up a Lucene index of all files.
 * */
public class IndexServer implements CleanupIF{
    /** The remote server that hands us indexes. */
    private IndexRequestServerInterface remoteServer;
    /** The singleton instance of this class. */
    private static IndexServer instance;

    /** Instantiates the two handlers, and starts listening for requests. */
    protected IndexServer() {
        FileBasedCache<Set<Long>> cdxCache = new CDXIndexCache();
        FileBasedCache<Set<Long>> dedupCrawlLogCache
                = new DedupCrawlLogIndexCache();
        FileBasedCache<Set<Long>> fullCrawlLogCache
                = new FullCrawlLogIndexCache();
        //prompt the empty indices to pre-generated
        Set<Long> emptySet = new HashSet<Long>();
        cdxCache.getIndex(emptySet);
        dedupCrawlLogCache.getIndex(emptySet);
        fullCrawlLogCache.getIndex(emptySet);
                 
        remoteServer = IndexRequestServerFactory.getInstance();

        remoteServer.setHandler(RequestType.CDX, cdxCache);
        remoteServer.setHandler(RequestType.DEDUP_CRAWL_LOG,
                dedupCrawlLogCache);
        remoteServer.setHandler(RequestType.FULL_CRAWL_LOG,
                fullCrawlLogCache);
        remoteServer.start();
    }

    /** Get the unique index server instance.
     *
     * @return The instance;
     */
    public static synchronized IndexServer getInstance() {
        if (instance == null) {
            instance = new IndexServer();
        }
        return instance;
    }

    /** Close the server.
     */
    public void cleanup() {
        remoteServer.close();
        instance = null;
    }
}
