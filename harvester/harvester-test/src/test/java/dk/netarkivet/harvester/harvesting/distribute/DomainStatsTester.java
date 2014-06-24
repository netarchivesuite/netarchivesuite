package dk.netarkivet.harvester.harvesting.distribute;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.harvester.datamodel.StopReason;
import junit.framework.Assert;
import junit.framework.TestCase;

/**
 * Unit tests for the class
 * dk.netarkivet.harvester.harvesting.distribute.DomainStats.
 */
public class DomainStatsTester extends TestCase {

    private final long negativeInitObjectCount = -100;
    private final long positiveInitObjectCount = 100;
    private final long negativeInitByteCount = -100;
    private final long positiveInitByteCount = 100;
    private final StopReason downloadComplete = StopReason.DOWNLOAD_COMPLETE;
    private final StopReason nullStopreason = null;
    private DomainStats domainstats;
    
    protected void setUp() throws Exception {
        domainstats = new DomainStats(positiveInitObjectCount,
                positiveInitByteCount, downloadComplete);
    }

    /** test the DomainStats constructor. */
    public void testDomainStats() {
        try {
            new DomainStats(negativeInitObjectCount,
                    positiveInitByteCount, downloadComplete);
            fail("Should throw ArgumentNotValid exception on negative"
                    + " objectCount");
        } catch (ArgumentNotValid e) {
            // Expected
        }
        
        try {
            new DomainStats(positiveInitObjectCount,
                    negativeInitByteCount, downloadComplete);
            fail("Should throw ArgumentNotValid exception on negative"
                    + " byteCount");
        } catch (ArgumentNotValid e) {
            // Expected
        }
 
        try {
            new DomainStats(positiveInitObjectCount,
                    positiveInitByteCount, nullStopreason);
            fail("Should throw ArgumentNotValid exception on null stopreason");
        } catch (ArgumentNotValid e) {
            // Expected
        }
        
        try {
            new DomainStats(positiveInitObjectCount,
                    positiveInitByteCount, downloadComplete);
        } catch (ArgumentNotValid e) {
            fail("Should not throw ArgumentNotValid exception on "
                    + "correct arguments.");
        }
    }
    
    /** Test the setByteCount method. */
    public void testSetByteCount() {
        final long bytes = 500L;
        domainstats.setByteCount(bytes);
        Assert.assertEquals(bytes, domainstats.getByteCount());
    }
    /** Test the setObjectCount method. */
    public void testSetObjectCount() {
        final long objects = 200L;
        domainstats.setObjectCount(objects);
        Assert.assertEquals(objects, domainstats.getObjectCount());
    }
    
    /** Test the setStopReason method. */
    public void testSetStopReason() {
        StopReason aStopreason = StopReason.DOWNLOAD_UNFINISHED;
        domainstats.setStopReason(aStopreason);
        Assert.assertEquals(aStopreason, domainstats.getStopReason());

    }

}
