package dk.netarkivet.harvester.datamodel;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import junit.framework.TestCase;

public class MinuteFrequencyTest extends TestCase {


    public void testGetFirstEvent() throws Exception {
        MinuteFrequency minuteFrequency = new MinuteFrequency(5);
        Calendar calendar = GregorianCalendar.getInstance();
        calendar.set(2010, Calendar.FEBRUARY, 12, 8, 14);
        Date calendarDate = calendar.getTime();
        Date firstEvent = minuteFrequency.getFirstEvent(calendarDate);
        assertEquals("First event should happen at once.", calendarDate, firstEvent);
    }

    public void testGetNextEvent() throws Exception {
        MinuteFrequency minuteFrequency = new MinuteFrequency(5);
        Calendar calendar = GregorianCalendar.getInstance();
        calendar.set(2010, Calendar.FEBRUARY, 12, 8, 14);
        Date calendarDate = calendar.getTime();
        Date nextEvent = minuteFrequency.getNextEvent(calendarDate);
        calendar.add(Calendar.MINUTE, 5);
        assertEquals("Next event should be five minutes later.", calendar.getTime(), nextEvent);
    }

}
