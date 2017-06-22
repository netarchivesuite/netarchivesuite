package dk.netarkivet.harvester.harvesting;

import static org.junit.Assert.*;

import org.archive.modules.CrawlURI;
import org.archive.modules.extractor.Hop;
import org.archive.modules.extractor.LinkContext;
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
        String url1 = "http://www.ssup.dk";
        String url2 = "http://www.nemmehjemmesider.dk/designs/responsive03/stylesheets/layout.css";
        UURI uuri1 = UURIFactory.getInstance(url1);
        UURI uuri2 = UURIFactory.getInstance(url2);

        CrawlURI curi1 = new CrawlURI(uuri1);
        curi1.setSeed(true);
        curi1.setSourceTag(url1);
        CrawlURI curi2 = curi1.createCrawlURI(uuri2, LinkContext.EMBED_MISC, Hop.EMBED);
        assertEquals(policy.getClassKey(curi1), policy.getClassKey(curi2));
    }

    @Test
    public void getClassKeyTestChain() throws Exception {
        SeedUriDomainnameQueueAssignmentPolicy policy = new SeedUriDomainnameQueueAssignmentPolicy();
        String url1 = "http://www.ssup.dk";
        UURI uuri1 = UURIFactory.getInstance(url1);
        UURI uuri2 = UURIFactory.getInstance("http://www.f2.dk");
        UURI uuri3 = UURIFactory.getInstance("http://www.f3.dk");
        UURI uuri4 = UURIFactory.getInstance("http://www.f4.dk");
        UURI uuri5 = UURIFactory.getInstance("http://www.f5.dk");
        UURI uuri6 = UURIFactory.getInstance("http://www.f6.dk");
        UURI uuri7 = UURIFactory.getInstance("http://www.f7.dk");


        CrawlURI curi1 = new CrawlURI(uuri1);
        curi1.setSeed(true);
        curi1.setSourceTag(url1);
        //LRLEPI
        CrawlURI curi2 = curi1.createCrawlURI(uuri2, null, Hop.NAVLINK);
        CrawlURI curi3 = curi2.createCrawlURI(uuri3, null, Hop.REFER );
        CrawlURI curi4 = curi3.createCrawlURI(uuri4, null, Hop.NAVLINK);
        CrawlURI curi5 = curi4.createCrawlURI(uuri5, null, Hop.EMBED);
        CrawlURI curi6 = curi5.createCrawlURI(uuri6, null, Hop.PREREQ);
        CrawlURI curi7 = curi6.createCrawlURI(uuri7, null, Hop.INFERRED);
        assertEquals(policy.getClassKey(curi7), policy.getClassKey(curi1));
    }


}