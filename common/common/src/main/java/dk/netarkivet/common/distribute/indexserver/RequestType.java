package dk.netarkivet.common.distribute.indexserver;

/** Types of requests we can handle in an index server.
 * Currently we support CDX files and lucene indices of crawl.logs.
 */
public enum RequestType {
    CDX,
    DEDUP_CRAWL_LOG,
    FULL_CRAWL_LOG
}
