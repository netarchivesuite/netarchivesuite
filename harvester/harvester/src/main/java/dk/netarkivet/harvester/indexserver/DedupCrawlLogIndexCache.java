
package dk.netarkivet.harvester.indexserver;

import java.util.Set;

import dk.netarkivet.common.exceptions.NotImplementedException;

/**
 * A cache of crawl log indices appropriate for the Icelandic deduplicator
 * code, excluding all text entries.
 *
 * @see CrawlLogIndexCache
 *
 */
public class DedupCrawlLogIndexCache extends CrawlLogIndexCache {
    /**
     * Constructor.
     * Calls the constructor of the inherited class with the correct values.
     */
    public DedupCrawlLogIndexCache() {
        super("dedupcrawllogindex", true, "^text/.*");
    }

    @Override
    public void requestIndex(Set<Long> jobSet, Long harvestId) {
        throw new NotImplementedException(
        "This feature is not implemented for the cdxIndexCache");        
    }
}

