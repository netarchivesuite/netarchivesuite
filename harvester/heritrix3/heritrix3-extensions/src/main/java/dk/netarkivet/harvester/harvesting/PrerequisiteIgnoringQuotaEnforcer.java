package dk.netarkivet.harvester.harvesting;

import org.archive.crawler.prefetch.QuotaEnforcer;
import org.archive.modules.CrawlURI;
import org.archive.modules.fetcher.FetchStats;

/**
 * Created by csr on 5/29/18.
 */
public class PrerequisiteIgnoringQuotaEnforcer extends QuotaEnforcer {
    @Override protected boolean checkQuotas(CrawlURI curi, FetchStats.HasFetchStats hasStats, int CAT) {
        if (curi.isPrerequisite()) {
            return false;
        } else {
            return super.checkQuotas(curi, hasStats, CAT);
        }
    }
}
