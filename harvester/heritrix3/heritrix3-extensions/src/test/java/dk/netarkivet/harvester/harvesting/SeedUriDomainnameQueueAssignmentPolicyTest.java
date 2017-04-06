package dk.netarkivet.harvester.harvesting;

import static org.junit.Assert.*;

import org.archive.modules.CrawlURI;
import org.archive.net.UURI;
import org.archive.net.UURIFactory;
import org.junit.Test;

/**
 * Created by csr on 4/6/17.
 */
public class SeedUriDomainnameQueueAssignmentPolicyTest {
    @Test
    public void getClassKey() throws Exception {
       SeedUriDomainnameQueueAssignmentPolicy policy = new SeedUriDomainnameQueueAssignmentPolicy();
        policy.setDeferToPrevious(true);
        String url1 = "http://www.ssup.dk";
        String url2 = "http://www.nemmehjemmesider.dk/designs/responsive03/stylesheets/layout.css";
        UURI uuri1 = UURIFactory.getInstance(url1);
        UURI uuri2 = UURIFactory.getInstance(url2);
        CrawlURI curi1 = new CrawlURI(uuri1);
        CrawlURI curi2 = new CrawlURI(uuri2, "E", uuri1, null);
        assertEquals(policy.getClassKey(curi1), policy.getClassKey(curi2));
    }

}