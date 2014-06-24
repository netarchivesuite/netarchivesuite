package dk.netarkivet.harvester.webinterface;

import junit.framework.TestCase;
import dk.netarkivet.common.exceptions.ArgumentNotValid;

/**
 * Unit-test for the HistorySiteSection class.
 * FIXME Does not currently test HistorySiteSection functionality.
 */
public class HistorySiteSectionTester extends TestCase {
    public HistorySiteSectionTester(String s) {
        super(s);
    }

    public void setUp() {
    }

    public void tearDown() {
    }
    
    /**
     * Tests the HarvestStatus.makeHarvestRunLink() method.
     */
    public void testMakeHarvestRunLink() {
        long harvestID = 10L;
        int harvestRun = 5;
        assertEquals("Should get correctly formatted link for run",
                "<a href=\"/History/Harveststatus-perharvestrun.jsp?"
                        + "HARVEST_ID=" + harvestID 
                        + "&amp;HARVEST_RUN=" + harvestRun
                        + "&amp;JOB_STATUS=" 
                        + HarvestStatusQuery.JOBSTATUS_ALL
                        + "\">" + harvestRun + "</a>",
                HarvestStatus.makeHarvestRunLink(harvestID, harvestRun));
        try {
            harvestID = -1L;
            harvestRun = 3;
            HarvestStatus.makeHarvestRunLink(harvestID, harvestRun);
            fail("Should die on negative harvestID");
        } catch (ArgumentNotValid e) {
            //expected
        }
        try {
            harvestID = 1L;
            harvestRun = -3;
            HarvestStatus.makeHarvestRunLink(1, -3);
            fail("Should die on negative harvestRun");
        } catch (ArgumentNotValid e) {
            //expected
        }
    }
}