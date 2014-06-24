package dk.netarkivet.harvester.datamodel;

/**
 * Tests a timed schedule
 */

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import junit.framework.TestCase;

import dk.netarkivet.common.exceptions.ArgumentNotValid;


public class TimedScheduleTester extends TestCase {
    public TimedScheduleTester(String s) {
        super(s);
    }

    public void setUp() {
    }

    public void tearDown() {
    }

    /**
     * Test that a schedule with a null startDate starts running immediately
     */
    public void testNullStartDate() {
        // Run a job on the 1st of January every year from 2001 to 2005.
        Calendar cal = new GregorianCalendar(2001, Calendar.JANUARY, 1, 9, 30);
        Calendar cal2 = new GregorianCalendar(2005, Calendar.JULY, 23, 9, 30);
        Schedule sched = Schedule.getInstance(null, cal2.getTime(),
                new MonthlyFrequency(12), "Tilykke", "med");
        int totalCount = 0;
        Date first = sched.getFirstEvent(cal.getTime());
        assertNotNull("Should have non-null first time", first);
        if (first != null ) totalCount++;
        for (int i = 0; i<10; i++) {
            Date next = sched.getNextEvent(cal.getTime(), 0);
            cal.add(Calendar.MONTH,12);
            if (next != null) totalCount++;
        }
        assertEquals("Should have scheduled 5 times", 5, totalCount);
    }


    /**
     * test that a never-ending schedule (endDate==null) in fact never ends
     */
    public void testNeverEndingSchedule() {
        Calendar cal = new GregorianCalendar(1940, Calendar.APRIL, 9, 9, 30);
        Schedule sched = Schedule.getInstance(cal.getTime(), null,
                new MonthlyFrequency(12), "Half flag",
                "In rememberance of the German occupation");
        Date first = sched.getFirstEvent(cal.getTime());
        assertNotNull("First date should not be null", first);
        for (int i=0; i<10; i++) {
            cal.add(Calendar.MONTH,12);
            Date next = sched.getNextEvent(cal.getTime(), 0);
            assertNotNull("Next date should never be null",next);
        }
    }


    /** Given a timed schedule that should run yearly and end just before
     * the fourth event, test that we get the expected three events
     * @throws Exception
     */
    public void testGetNextEvent1() throws Exception {
        Calendar cal = new GregorianCalendar(1940, Calendar.APRIL, 9, 9, 30);
        Calendar cal2 = new GregorianCalendar(1943, Calendar.APRIL, 9, 9, 29);
        Schedule sched = Schedule.getInstance(cal.getTime(), cal2.getTime(),
                new MonthlyFrequency(12), "Half flag",
                "In rememberance of the German occupation");
        assertTrue("Schedule should be timed", sched instanceof TimedSchedule);
        Date next = sched.getNextEvent(cal.getTime(), 1);
        cal.add(Calendar.MONTH, 12);
        assertEquals("Second event must happen next year.", cal.getTime(), next);
        next = sched.getNextEvent(cal.getTime(), 2);
        cal.add(Calendar.MONTH, 12);
        assertEquals("Third event must happen year after.", cal.getTime(), next);
        next = sched.getNextEvent(cal.getTime(), 3);
        assertNull("Fourth event should not happen.", next);
    }

    /** Given a timed schedule that should run yearly at noon and end just
     * before the fourth event, test that we get the expected three events
     * @throws Exception
     */
    public void testGetNextEvent2() throws Exception {
        Calendar cal = new GregorianCalendar(1940, Calendar.APRIL, 9, 9, 30);
        Calendar cal2 = new GregorianCalendar(1943, Calendar.APRIL, 9, 11, 59);
        Schedule sched = Schedule.getInstance(cal.getTime(), cal2.getTime(),
                new MonthlyFrequency(12, 9, 12, 0), "Full flag",
                "In rememberance of the German occupation");
        assertTrue("Schedule should be timed", sched instanceof TimedSchedule);
        Date next = sched.getNextEvent(cal.getTime(), 1);
        cal.set(Calendar.HOUR, 12);
        cal.set(Calendar.MINUTE, 00);
        cal.add(Calendar.MONTH, 12);
        assertEquals("Second event must happen next year.", cal.getTime(), next);
        next = sched.getNextEvent(cal.getTime(), 2);
        cal.add(Calendar.MONTH, 12);
        assertEquals("Third event must happen year after.", cal.getTime(), next);
        next = sched.getNextEvent(cal.getTime(), 3);
        assertNull("Fourth event should not happen.", next);
    }

    /** Test of invalid arguments
     * @throws Exception
     */
    public void testExceptions() throws Exception {
        Calendar cal = new GregorianCalendar(1940, Calendar.APRIL, 9, 9, 30);
        Calendar cal2 = new GregorianCalendar(1943, Calendar.APRIL, 9, 9, 29);
        Schedule sched = Schedule.getInstance(cal.getTime(), cal2.getTime(),
                new MonthlyFrequency(12, 9, 12, 0), "Full flag",
                "In rememberance of the German occupation");
        assertTrue("Schedule should be timed",
                   sched instanceof TimedSchedule);
        try {
            sched.getNextEvent(cal2.getTime(), -1);
            fail("Expected argument not valid on negative argument");
        } catch (ArgumentNotValid e) {
            //Expected
        }
    }
}
