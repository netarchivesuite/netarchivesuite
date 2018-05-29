package dk.netarkivet.harvester.harvesting;

import org.archive.crawler.prefetch.QuotaEnforcer;
import org.archive.modules.CrawlURI;
import org.archive.modules.fetcher.FetchStats;

/**
 * A Heritrix QuotaEnforcer which never enforces quotas on prerequisite uris: dns, robots.txt, and credentials
 */
public class PrerequisiteIgnoringQuotaEnforcer extends QuotaEnforcer {
    @Override protected boolean checkQuotas(CrawlURI curi, FetchStats.HasFetchStats hasStats, int CAT) {
        if (curi.isPrerequisite()) {
            return false;  //False means "do not enforce quota"
        } else {
            return super.checkQuotas(curi, hasStats, CAT);
        }
    }
}
