package dk.netarkivet.harvester.datamodel;

import junit.framework.TestCase;

import dk.netarkivet.common.exceptions.ArgumentNotValid;

/** 
 * Unit tests for the {@link TimeUnit} class. 
 */ 
public class TimeUnitTester extends TestCase {
    public TimeUnitTester(String s) {
        super(s);
    }
    
    public void testFromOrdinal() {
        assertEquals(TimeUnit.HOURLY, TimeUnit.fromOrdinal(1));
        assertEquals(TimeUnit.DAILY, TimeUnit.fromOrdinal(2));
        assertEquals(TimeUnit.WEEKLY, TimeUnit.fromOrdinal(3));
        assertEquals(TimeUnit.MONTHLY, TimeUnit.fromOrdinal(4));
        assertEquals(TimeUnit.MINUTE, TimeUnit.fromOrdinal(5));
        try {
            TimeUnit.fromOrdinal(0);
            fail("Should throw ArgumentNotValid when giving arg 0");
        } catch (ArgumentNotValid e) {
            // Expected
        }
        try {
            TimeUnit.fromOrdinal(6);
            fail("Should throw ArgumentNotValid when giving arg 5");
        } catch (ArgumentNotValid e) {
            // Expected
        }
    }
}
