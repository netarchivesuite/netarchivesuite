
package dk.netarkivet.harvester.indexserver;

import java.util.Set;

import dk.netarkivet.common.exceptions.NotImplementedException;

/**
 * A CrawlLogIndexCache that takes in all entries in the crawl log.
 *
 * @see CrawlLogIndexCache
 *
 */
public class FullCrawlLogIndexCache extends CrawlLogIndexCache {
    /** Create a new FullCrawlLogIndexCache, creating Lucene indexes. */
    public FullCrawlLogIndexCache() {
        super("fullcrawllogindex", false, ".*");
    }

    @Override
    public void requestIndex(Set<Long> jobSet, Long harvestId) {
        throw new NotImplementedException(
        "This feature is not implemented for this type of cache");
    }

}
