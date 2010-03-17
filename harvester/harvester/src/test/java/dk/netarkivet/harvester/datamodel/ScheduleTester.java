/* File:    $Id$
 * Version: $Revision$
 * Date:    $Date$
 * Author:  $Author$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2010 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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
import java.util.TimeZone;

import dk.netarkivet.common.exceptions.ArgumentNotValid;


/**
 * Unit-tests for the Schedule class.
 */
public class ScheduleTester extends DataModelTestCase {
    public ScheduleTester(String testName) {
        super(testName);
    }

    public void setUp() throws Exception {
        super.setUp();
    }

    public void tearDown() throws Exception {
        super.tearDown();
    }

    public void testValidityOfArguments() {
        try {
            Date startDate = new Date(System.currentTimeMillis());
            new TimedSchedule(startDate, null, TestInfo.FREQUENCY, null, "");
            fail("Null not valid schedule name");
        } catch (ArgumentNotValid e) {
            // expected
        }

        try {
            Date startDate = new Date(System.currentTimeMillis());
            new TimedSchedule(startDate, null, null, TestInfo.DEFAULT_SCHEDULE_NAME, "");
            fail("Null not valid schedule name");
        } catch (ArgumentNotValid e) {
            // expected
        }

        try {
            Date startDate = new Date(System.currentTimeMillis());
            new TimedSchedule(startDate, null, TestInfo.FREQUENCY, "", "");
            fail("Empty string not a valid schedule name");
        } catch (ArgumentNotValid e) {
            // expected
        }

        try {
            Date startDate = new Date(System.currentTimeMillis());
            new TimedSchedule(startDate, null, TestInfo.FREQUENCY, TestInfo.DEFAULT_SCHEDULE_NAME, null);
            fail("Empty string not a valid schedule name");
        } catch (ArgumentNotValid e) {
            // expected
        }

        Schedule sched = Schedule.getInstance(null, null,
                new MonthlyFrequency(12), "Half flag",
                "In rememberance of the German occupation");
        try {
            sched.getFirstEvent(null);
        } catch (ArgumentNotValid e) {
            // expected
        }
    }

    /** Test the first event happens at the given time if the given time is
     * given as the first possible.
     * @throws Exception
     */
    public void testGetFirstEvent1() throws Exception {
        Calendar cal = new GregorianCalendar(1940, Calendar.APRIL, 9, 9, 30);
        Schedule sched = Schedule.getInstance(cal.getTime(), null,
                new MonthlyFrequency(12), "Half flag",
                "In rememberance of the German occupation");
        assertTrue("Schedule should be timed", sched instanceof TimedSchedule);
        Date first = sched.getFirstEvent(cal.getTime());
        assertEquals("First event must happen immediately.", cal.getTime(), first);
    }

    /** Test the first event happens at first time matching frequency
     * requirements if the given time is given as the first possible.
     * @throws Exception
     */
    public void testGetFirstEvent2() throws Exception {
        Calendar cal = new GregorianCalendar(1940, Calendar.APRIL, 9, 9, 30);
        Schedule sched = Schedule.getInstance(cal.getTime(), null,
                new MonthlyFrequency(12, 9, 12, 0), "Full flag",
                "In rememberance of the German occupation");
        assertTrue("Schedule should be timed", sched instanceof TimedSchedule);
        Date first = sched.getFirstEvent(cal.getTime());
        cal.set(Calendar.HOUR, 12);
        cal.set(Calendar.MINUTE, 00);
        assertEquals("First event must happen at noon.", cal.getTime(), first);
    }

    /** Tests that first event happens now if no first time is given.
     * @throws Exception
     */
    public void testGetFirstEvent3() throws Exception {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.MILLISECOND, 0);
        Schedule sched = Schedule.getInstance(null, null,
                new MonthlyFrequency(12), "Half flag",
                "In rememberance of the German occupation");
        assertTrue("Schedule should be timed", sched instanceof TimedSchedule);
        Date first = sched.getFirstEvent(cal.getTime());
        assertEquals("First event must happen immediately.", cal.getTime(), first);
    }

    /** Tests that first event happens at first possible time allowed by
     * frequency requirements if no first time is given.
     * @throws Exception
     */
    public void testGetFirstEvent4() throws Exception {
        Schedule sched = Schedule.getInstance(null, null,
                new MonthlyFrequency(12, 9, 12, 0), "Full flag",
                "In rememberance of the German occupation");
        assertTrue("Schedule should be timed", sched instanceof TimedSchedule);
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.MONTH, Calendar.MAY);
        cal.set(Calendar.DAY_OF_MONTH, 10);
        cal.set(Calendar.HOUR_OF_DAY, 10);
        cal.set(Calendar.MINUTE, 37);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date first = sched.getFirstEvent(cal.getTime());
        cal.set(Calendar.MONTH, Calendar.JUNE);
        cal.set(Calendar.DAY_OF_MONTH, 9);
        cal.set(Calendar.HOUR_OF_DAY, 12);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        assertEquals("First event must happen at noon.", cal.getTime(), first);
    }

    /** Tests that first event happens at given first time
     *  if now is before then.
     *  @throws Exception
     */
    public void testGetFirstEvent5() throws Exception {
        Calendar cal = new GregorianCalendar(1940, Calendar.APRIL, 9, 9, 30);
        Calendar cal2 = new GregorianCalendar(1943, Calendar.APRIL, 9, 9, 29);
        Calendar cal3 = new GregorianCalendar(1935, Calendar.APRIL, 9, 9, 29);
        Schedule sched = Schedule.getInstance(cal.getTime(), cal2.getTime(),
                new MonthlyFrequency(12), "Half flag",
                "In rememberance of the German occupation");
        assertTrue("Schedule should be timed", sched instanceof TimedSchedule);
        Date first = sched.getFirstEvent(cal3.getTime());
        assertEquals("First event must happen at correct time.", cal.getTime(), first);
    }

    /** Tests that first event happens at given first time
     *  if now is before then but at time allowed by frequency requirements.
     *  @throws Exception
     */
    public void testGetFirstEvent6() throws Exception {
        Calendar cal = new GregorianCalendar(1940, Calendar.APRIL, 9, 9, 30);
        Calendar cal2 = new GregorianCalendar(1943, Calendar.APRIL, 9, 9, 29);
        Calendar cal3 = new GregorianCalendar(1935, Calendar.APRIL, 9, 9, 29);
        Schedule sched = Schedule.getInstance(cal.getTime(), cal2.getTime(),
                new MonthlyFrequency(12, 9, 12, 0), "Full flag",
                "In rememberance of the German occupation");
        assertTrue("Schedule should be timed", sched instanceof TimedSchedule);
        Date first = sched.getFirstEvent(cal3.getTime());
        cal.set(Calendar.HOUR_OF_DAY, 12);
        cal.set(Calendar.MINUTE, 00);
        assertEquals("First event must happen at noon.", cal.getTime(), first);
    }



    public void testEquals() {
        // Three identical schedules:
        Schedule sch = TestInfo.getDefaultSchedule();
        Schedule sch_2 = TestInfo.getDefaultSchedule();
        Schedule sch_3 = TestInfo.getDefaultSchedule();

        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT+1"));
        cal.setTimeInMillis(0);
        cal.set(2005, Calendar.APRIL, 1, 0, 0, 0);
        Date startDate = new Date(cal.getTime().getTime());
        cal.set(2007, Calendar.FEBRUARY, 1, 0, 0, 0);
        Date endDate = new Date(cal.getTime().getTime());

        // A different schedule:
        Schedule sch_4 = new TimedSchedule(startDate, endDate, new HourlyFrequency(1), "NonDefaultSchedule", "");

        //  reflexive: for any non-null reference value x, x.equals(x) should return true.
        assertTrue("Schedule.equals() is not reflexive", sch.equals(sch));

        // symmetric: for any non-null reference values x and y, x.equals(y) should return true if and only if y.equals(x) returns true.
        if (sch.equals(sch_2)) {
            assertTrue("Schedule.equals() is not symmetric", sch_2.equals(sch));
            assertFalse("Schedule.equals() is not symmetric", sch.equals(sch_4));
        } else {
            fail("Symmetry: Schedule sch does not equal Schedule sch_2");
        }

        // transitive: for any non-null reference values x, y, and z, if x.equals(y) returns true and y.equals(z) returns true, then x.equals(z) should return true.
        if (sch.equals(sch_2) && sch_2.equals(sch_3)) {
            assertTrue("Schedule.equals() is not transitive", sch.equals(sch_3));
        } else {
            fail("Transitivity failure...");
        }

        //  consistent: for any non-null reference values x and y, multiple invocations of x.equals(y) consistently return true or consistently return false, provided no information used in equals comparisons on the objects is modified.
        for (int i = 0; i < 10; i++) {
            assertTrue("Schedule.equals() is not consistent", sch.equals(sch_2));
            assertFalse("Schedule.equals() is not consistent", sch.equals(sch_4));
        }

        // null-property:
        assertFalse("Schedule.equals() does not satisfy null-rule", sch.equals(null));
    }

    public void testHashCode() {

        // Two identical schedules:
        Schedule sch = TestInfo.getDefaultSchedule();
        Schedule sch_2 = TestInfo.getDefaultSchedule();

        // Equality property:
        if (sch.equals(sch_2)) {
            assertTrue("Equal schedules do not have identical hashcodes", sch.hashCode() == sch_2.hashCode());
        } else {
            fail("The two schedules are not equal");
        }

        // Consistency:
        for (int i = 0; i < 10; i++) {
            if (i > 0) {
                int prevHashCode = sch.hashCode();
                assertTrue("Schedule.hashCode() is not consistent", prevHashCode == sch.hashCode());
            }
        }
    }

}
