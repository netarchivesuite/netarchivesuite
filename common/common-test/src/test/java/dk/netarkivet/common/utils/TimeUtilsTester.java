package dk.netarkivet.common.utils;

import java.util.Calendar;

import junit.framework.TestCase;

/**
 * Unit tests for the TimeUtils class.
 *
 */
public class TimeUtilsTester extends TestCase {
    public TimeUtilsTester(String s) {
        super(s);
    }

    public void setUp() {
    }

    public void tearDown() {
    }

    public void testExponentialBackoffSleep() throws Exception {
        long timeStart = System.currentTimeMillis();
        TimeUtils.exponentialBackoffSleep(2, Calendar.MILLISECOND);
        long waited = System.currentTimeMillis() - timeStart;
        assertTrue("Should have waited at least 2^2 millis, but only waited "
                + waited,
                waited >= 4);

        timeStart = System.currentTimeMillis();
        TimeUtils.exponentialBackoffSleep(3, Calendar.MILLISECOND);
        waited = System.currentTimeMillis() - timeStart;
        assertTrue("Should have waited at least 2^3 millis, but only waited "
                + waited,
                waited >= 8);

        timeStart = System.currentTimeMillis();
        TimeUtils.exponentialBackoffSleep(0, Calendar.SECOND);
        waited = System.currentTimeMillis() - timeStart;
        assertTrue("Should have waited at least 2^0 seconds, but only waited "
                + waited,
                waited >= 1000);
    }
    
    /**
     * Tests whether the readableTimeInterval function converts numbers correctly.
     */
    public void testReadability() {
        // Test whether it works.
        assertEquals(TimeUtils.readableTimeInterval(1L), "1 millisecond");
        
        // Test conversion between millisecond and seconds.
        assertEquals(TimeUtils.readableTimeInterval(5000L), "5 seconds");

        // Test conversion between millisecond and minutes.
        assertEquals(TimeUtils.readableTimeInterval(900000L), "15 minutes");

        // Test conversion between millisecond and hours.
        assertEquals(TimeUtils.readableTimeInterval(50400000), "14 hours");

        // Test conversion between millisecond and days.
        assertEquals(TimeUtils.readableTimeInterval(172800000L), "2 days");

        // Test conversion between millisecond and weeks.
        assertEquals(TimeUtils.readableTimeInterval(604800000L), "1 week");

        // 2,5 hours should be returned in minutes
        assertEquals(TimeUtils.readableTimeInterval(9000000L), "150 minutes");
    }
}
