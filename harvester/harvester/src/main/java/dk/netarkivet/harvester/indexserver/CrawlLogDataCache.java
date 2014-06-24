
package dk.netarkivet.harvester.indexserver;

import java.util.regex.Pattern;

import dk.netarkivet.harvester.harvesting.metadata.MetadataFile;

/**
 * This class implements the low-level cache for crawl log Lucene indexing.
 * It will get the crawl logs for individual jobs as files.
 *
 */

public class CrawlLogDataCache extends RawMetadataCache {
    /**
     * Create a new CrawlLogDataCache.  For a given job ID, this will fetch
     * and cache crawl.log files from metadata files
     * (&lt;ID&gt;-metadata-[0-9]+.arc).
     *
     */
    public CrawlLogDataCache() {
        super("crawllog",
                Pattern.compile(MetadataFile.CRAWL_LOG_PATTERN),
                Pattern.compile("text/plain"));
    }
}
