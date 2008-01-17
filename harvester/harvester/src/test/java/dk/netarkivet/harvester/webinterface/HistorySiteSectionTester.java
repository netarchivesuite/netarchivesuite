package dk.netarkivet.harvester.webinterface;
/**
 * lc forgot to comment this!
 */

import junit.framework.TestCase;

import dk.netarkivet.common.exceptions.ArgumentNotValid;


public class HistorySiteSectionTester extends TestCase {
    public HistorySiteSectionTester(String s) {
        super(s);
    }

    public void setUp() {
    }

    public void tearDown() {
    }

    public void testMakeHarvestRunLink() {
        assertEquals("Should get correctly formatted link for run",
                "<a href=\"/History/Harveststatus-perharvestrun.jsp?"
                        + "harvestID=10&amp;harvestNum=5\">5</a>",
                HarvestStatus.makeHarvestRunLink(10L, 5));
        try {
            HarvestStatus.makeHarvestRunLink(-1, 3);
            fail("Should die on negative harvest");
        } catch (ArgumentNotValid e) {
            //expected
        }
        try {
            HarvestStatus.makeHarvestRunLink(1, -3);
            fail("Should die on negative harvest");
        } catch (ArgumentNotValid e) {
            //expected
        }
    }
}