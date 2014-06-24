package dk.netarkivet.harvester.harvesting.distribute;

import dk.netarkivet.harvester.harvesting.distribute.CrawlProgressMessage.CrawlStatus;
import junit.framework.TestCase;

/**
 * Unit tests for the class
 * {@link CrawlProgressMessage}.
 */
public class CrawlProgressMessageTester extends TestCase {
    
    protected void setUp() throws Exception {
    }
    
    public void testConstructor() {
        long harvestId = 2L;
        long jobId = 42L;
        CrawlProgressMessage msg = new CrawlProgressMessage(harvestId, jobId);
        assertEquals(harvestId, msg.getHarvestID());
        assertEquals(jobId, msg.getJobID());
        assertEquals(CrawlStatus.PRE_CRAWL, msg.getStatus());
        assertEquals("", msg.getProgressStatisticsLegend());
    }
}
