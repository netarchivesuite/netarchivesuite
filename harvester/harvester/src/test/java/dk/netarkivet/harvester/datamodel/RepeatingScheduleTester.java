/*$Id$
* $Revision$
* $Date$
* $Author$
*
* The Netarchive Suite - Software to harvest and preserve websites
* Copyright 2004-2009 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
*
* This library is free software; you can redistribute it and/or
* modify it under the terms of the GNU Lesser General Public
* License as published by the Free Software Foundation; either
* version 2.1 of the License, or (at your option) any later version.
*
* This library is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
* Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public
* License along with this library; if not, write to the Free Software
* Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
*/
package dk.netarkivet.harvester.datamodel;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import junit.framework.TestCase;

import dk.netarkivet.common.exceptions.ArgumentNotValid;

/**
 * Tests a repeating schedule.
 */
public class RepeatingScheduleTester extends TestCase {
    public RepeatingScheduleTester(String s) {
        super(s);
    }

    public void setUp() {
    }

    public void tearDown() {
    }
    
    /** Given a repeating schedule that should run yearly 3 times, check
     * that it gives the expected events, and no fourth event.
     * @throws Exception
     */
    public void testGetNextEvent1() throws Exception {
        Calendar cal = new GregorianCalendar(1940, Calendar.APRIL, 9, 9, 30);
        Schedule sched = Schedule.getInstance(cal.getTime(), 3,
                new MonthlyFrequency(12), "Half flag",
                "In rememberance of the German occupation");
        assertTrue("Schedule should be repeating",
                   sched instanceof RepeatingSchedule);
        Date next = sched.getNextEvent(cal.getTime(), 1);
        cal.add(Calendar.MONTH, 12);
        assertEquals("Second event must happen next year.", cal.getTime(), next);
        next = sched.getNextEvent(cal.getTime(), 2);
        cal.add(Calendar.MONTH, 12);
        assertEquals("Third event must happen year after.", cal.getTime(), next);
        next = sched.getNextEvent(cal.getTime(), 3);
        assertNull("Fourth event should not happen.", next);
    }

    /** Given a repeting schedule that should run yearly at noon 3 times, check
     * that it gives the expected events, and no fourth event.
     * @throws Exception
     */
    public void testGetNextEvent2() throws Exception {
        Calendar cal = new GregorianCalendar(1940, Calendar.APRIL, 9, 9, 30);
        Schedule sched = Schedule.getInstance(cal.getTime(), 3,
                new MonthlyFrequency(12, 9, 12, 0), "Full flag",
                "In rememberance of the German occupation");
        assertTrue("Schedule should be repeating",
                   sched instanceof RepeatingSchedule);
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

    /**
     * Test that negative argument on getNextEvent
     * throws ArgumentNotValid exception.
     * @throws Exception
     */
    public void testExceptions() throws Exception {
        Calendar cal = new GregorianCalendar(1940, Calendar.APRIL, 9, 9, 30);
        Schedule sched = Schedule.getInstance(cal.getTime(), 1,
                new MonthlyFrequency(12, 9, 12, 0), "Full flag",
                "In rememberance of the German occupation");
        assertTrue("Schedule should be repeating",
                   sched instanceof RepeatingSchedule);
        try {
            sched.getNextEvent(cal.getTime(), -1);
            fail("Expected argument not valid on negative argument");
        } catch (ArgumentNotValid e) {
            //Expected
        }
    }
    
    /** Given a repeating schedule check, that given the date of previous event is null,
     * the date of the next event is also null.
     */
    public void testGetNextEvent3() throws Exception {
        Calendar cal = new GregorianCalendar(1940, Calendar.APRIL, 9, 9, 30);
        Schedule sched = Schedule.getInstance(cal.getTime(), 1,
                new MonthlyFrequency(12, 9, 12, 0), "Full flag",
                "In rememberance of the German occupation");
        assertTrue("Schedule should be repeating",
                sched instanceof RepeatingSchedule);
        
        assertNull("Null expected", sched.getNextEvent(null, 0));
    }
    
    
}