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
 * Tests a monthly frequency.
 */
public class MonthlyFrequencyTester extends TestCase {
    public MonthlyFrequencyTester(String s) {
        super(s);
    }

    public void setUp() {
    }

    public void tearDown() {
    }

    /** 
     * Test value is monthly.
     */
    public void testTimeunitIsMonthly() {
    	MonthlyFrequency freq = new MonthlyFrequency(20);
        assertEquals("Timeunit must be monthly.", 
        		     freq.ordinal(), 
        		     TimeUnit.MONTHLY.ordinal()
        );
        assertEquals("Check TimeUnit monthly", 
        		      TimeUnit.MONTHLY, 
        		      TimeUnit.fromOrdinal(TimeUnit.MONTHLY.ordinal())
        );
    }
    
    /** Given a frequency that can start any time, check that first event is
     * immediate.
     * @throws Exception
     */
    public void testGetFirstEvent1() throws Exception {
        MonthlyFrequency freq = new MonthlyFrequency(4); // Every four months, anytime
        Calendar cal = GregorianCalendar.getInstance();
        cal.set(2005, Calendar.AUGUST, 25, 22, 42);
        Date d1 = cal.getTime();
        Date d2 = freq.getFirstEvent(d1);
        assertEquals("First event should happen at once.", d1, d2);
    }

    /** Given a frequency that can start 5th of month 4:22, check that next
     * event is at first correct time.
     * @throws Exception
     */
    public void testGetFirstEvent2() throws Exception {
        MonthlyFrequency freq = new MonthlyFrequency(4, 5, 4, 22); // Every four months, on the day hour and minute
        Calendar cal = new GregorianCalendar(2005, Calendar.AUGUST, 25, 22, 42);
        Date d1 = cal.getTime();
        cal.add(Calendar.MINUTE, ((60 + 22) - 42));
        cal.add(Calendar.HOUR, ((24 + 4) - (22 + 1))); // It is now Aug. 26th
        cal.add(Calendar.DAY_OF_YEAR, ((31 + 5) - (25 + 1)));
        Date d3 = cal.getTime();
        Date d2 = freq.getFirstEvent(d1);
        assertEquals("First event should happen on the 5th day, on the 22nd minute of the 4th hour.", d3, d2);
    }

    /** Given a frequency that can start any time, check that next event is
     * after appropriate period.
     * @throws Exception
     */
    public void testGetNextEvent1() throws Exception {
        MonthlyFrequency freq = new MonthlyFrequency(4); // Every four months, anytime
        Calendar cal = new GregorianCalendar(2005, Calendar.AUGUST, 25, 22, 42);
        Date d1 = cal.getTime();
        cal.add(Calendar.MONTH, 4);
        Date d3 = cal.getTime();
        Date d4 = freq.getNextEvent(d1);
        assertEquals("Second event should happen four months later", d3, d4);
    }

    /** Given a frequency that can start 5th of month 4:22, check that next
     * event is after appropriate period.
     * @throws Exception
     */
    public void testGetNextEvent2() throws Exception {
        MonthlyFrequency freq = new MonthlyFrequency(4, 5, 5, 23); // Every four months, on the day hour and minute
        Calendar cal = new GregorianCalendar(2005, Calendar.AUGUST, 25, 22, 42);
        Date d1 = cal.getTime();
        cal.add(Calendar.MONTH, 4);
        cal.add(Calendar.MINUTE, ((60 + 23) - 42));
        cal.add(Calendar.HOUR, ((24 + 5) - (22 + 1))); // It is now the 26th
        cal.add(Calendar.DATE, ((31 + 5) - (25 + 1)));
        Date d3 = cal.getTime();
        Date d4 = freq.getNextEvent(d1);
        assertEquals("Second event should happen four months later, on 5th day, on the 23rd minute of the 5th hour", d3, d4);
    }

    /** Given a frequency that can start 5th of month 4:22, check that next
     * event is after appropriate period, even given a date that is 5th of month
     * at 4:22.
     * @throws Exception
     */
    public void testGetNextEvent3() throws Exception {
        MonthlyFrequency freq = new MonthlyFrequency(4, 5, 5, 23); // Every four months, on the day hour and minute
        Calendar cal = new GregorianCalendar(2005, Calendar.AUGUST, 5, 5, 23);
        Date d1 = cal.getTime();
        Calendar cal2 = new GregorianCalendar(2005, Calendar.DECEMBER, 5, 5, 23);
        Date d3 = cal2.getTime();
        Date d4 = freq.getNextEvent(d1);
        assertEquals("Second event should happen four months later", d3, d4);
    }

    /** Given a frequency that can start 31st of month 12:00, check that this
     * will be on the 29th in February, and 31st in March.
     * @throws Exception
     */
    public void testGetNextEvent4() throws Exception {
        MonthlyFrequency freq = new MonthlyFrequency(1, 31, 12, 0); // Every four months, on the day hour and minute
        Calendar cal = new GregorianCalendar(2005, Calendar.JANUARY, 31, 12, 0);
        Date d1 = cal.getTime();
        Calendar cal2 = new GregorianCalendar(2005, Calendar.FEBRUARY, 28, 12, 0);
        Date d2 = cal2.getTime();
        Calendar cal3 = new GregorianCalendar(2005, Calendar.MARCH, 31, 12, 0);
        Date d3 = cal3.getTime();
        Date d4 = freq.getFirstEvent(d1);
        assertEquals("First event should happen 31st jan", d1, d4);
        Date d5 = freq.getNextEvent(d4);
        assertEquals("Second event should happen 29th of feb", d2, d5);
        Date d6 = freq.getNextEvent(d5);
        assertEquals("Third event should happen 31st of mar", d3, d6);
    }

    /** Given a frequency that can start 31st of month any time, check that this
     * will be on the 29th in February, and 29th in March.
     * @throws Exception
     */
    public void testGetNextEvent5() throws Exception {
        MonthlyFrequency freq = new MonthlyFrequency(1); // Every four months, on the day hour and minute
        Calendar cal = new GregorianCalendar(2004, Calendar.JANUARY, 31, 12, 0);
        Date d1 = cal.getTime();
        Calendar cal2 = new GregorianCalendar(2004, Calendar.FEBRUARY, 29, 12, 0);
        Date d2 = cal2.getTime();
        Calendar cal3 = new GregorianCalendar(2004, Calendar.MARCH, 29, 12, 0);
        Date d3 = cal3.getTime();
        Date d4 = freq.getFirstEvent(d1);
        assertEquals("First event should happen 31st jan", d1, d4);
        Date d5 = freq.getNextEvent(d4);
        assertEquals("Second event should happen 29th of feb", d2, d5);
        Date d6 = freq.getNextEvent(d5);
        assertEquals("Third event should happen 31st of mar", d3, d6);
    }

    /** Given a frequency that can start 31st of month 12:00, check that this
     * will be on the 28th in February, and 31st in March.
     * @throws Exception
     */
    public void testGetNextEvent6() throws Exception {
        MonthlyFrequency freq = new MonthlyFrequency(1, 31, 12, 0); // Every four months, on the day hour and minute
        Calendar cal = new GregorianCalendar(2005, Calendar.JANUARY, 30, 15, 0);
        Date d = cal.getTime();
        Calendar cal1 = new GregorianCalendar(2005, Calendar.JANUARY, 31, 12, 0);
        Date d1 = cal1.getTime();
        Calendar cal2 = new GregorianCalendar(2005, Calendar.FEBRUARY, 28, 12, 0);
        Date d2 = cal2.getTime();
        Calendar cal3 = new GregorianCalendar(2005, Calendar.MARCH, 31, 12, 0);
        Date d3 = cal3.getTime();
        Date d4 = freq.getFirstEvent(d);
        assertEquals("First event should happen 31st jan", d1, d4);
        Date d5 = freq.getNextEvent(d4);
        assertEquals("Second event should happen 29th of feb", d2, d5);
        Date d6 = freq.getNextEvent(d5);
        assertEquals("Third event should happen 31st of mar", d3, d6);
    }

    /** Test validity of arguments (correct number of units), correct time,
     * correct date.
     */
    public void testValidityOfArguments() throws Exception {
        try {
            new MonthlyFrequency(-1);
            fail("should throw exception on negative number of units");
        } catch (ArgumentNotValid e) {
            //Expected
        }

        try {
            new MonthlyFrequency(0);
            fail("should throw exception on zero number of units");
        } catch (ArgumentNotValid e) {
            //Expected
        }

        try {
            new MonthlyFrequency(-1, 5, 5, 23);
            fail("should throw exception on negative number of units");
        } catch (ArgumentNotValid e) {
            //Expected
        }

        try {
            new MonthlyFrequency(0, 5, 5, 23);
            fail("should throw exception on zero number of units");
        } catch (ArgumentNotValid e) {
            //Expected
        }

        try {
            new MonthlyFrequency(1, 32, 5, 23);
            fail("should throw exception on illegal date");
        } catch (ArgumentNotValid e) {
            //Expected
        }

        try {
            new MonthlyFrequency(1, 0, 5, 23);
            fail("should throw exception on illegal date");
        } catch (ArgumentNotValid e) {
            //Expected
        }

        try {
            new MonthlyFrequency(1, 21, 24, 23);
            fail("should throw exception on illegal time");
        } catch (ArgumentNotValid e) {
            //Expected
        }

        try {
            new MonthlyFrequency(1, 32, -1, 23);
            fail("should throw exception on illegal time");
        } catch (ArgumentNotValid e) {
            //Expected
        }

        try {
            new MonthlyFrequency(1, 32, 0, -1);
            fail("should throw exception on illegal time");
        } catch (ArgumentNotValid e) {
            //Expected
        }

        try {
            new MonthlyFrequency(1, 32, 0, 60);
            fail("should throw exception on illegal time");
        } catch (ArgumentNotValid e) {
            //Expected
        }

        MonthlyFrequency freq = new MonthlyFrequency(4, 5, 5, 23); // Every four months, on the day hour and minute
        try {
            freq.getFirstEvent(null);
            fail("should throw exception");
        } catch (ArgumentNotValid e) {
            //Expected
        }
        try {
            freq.getNextEvent(null);
            fail("should throw exception");
        } catch (ArgumentNotValid e) {
            //Expected
        }
    }

}