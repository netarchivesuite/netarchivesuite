/*$Id$
* $Revision$
* $Date$
* $Author$
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

import junit.framework.TestCase;

import dk.netarkivet.common.exceptions.ArgumentNotValid;

/**
 * Tests a daily frequency.
 */
public class DailyFrequencyTester extends TestCase {
    public DailyFrequencyTester(String s) {
        super(s);
    }

    public void setUp() {

    }

    public void tearDown() {

    }

    /** 
     * Test value is daily.
     */
    public void testTimeunitIsDaily() {
    	DailyFrequency freq = new DailyFrequency(20);
        assertEquals("Timeunit must be daily.", 
        		     freq.ordinal(), 
        		     TimeUnit.DAILY.ordinal()
        );
        assertEquals("Check TimeUnit daily", 
        		      TimeUnit.DAILY, 
        		      TimeUnit.fromOrdinal(TimeUnit.DAILY.ordinal())
        );
    }

    /** Given a frequency that can start any time, check that first event is
     * immediate.
     * @throws Exception
     */
    public void testGetFirstEvent1() throws Exception {
        DailyFrequency freq = new DailyFrequency(4); // Every four days, anytime
        Calendar cal = GregorianCalendar.getInstance();
        cal.set(2005, Calendar.JUNE, 12, 22, 42);
        Date d1 = cal.getTime();
        Date d2 = freq.getFirstEvent(d1);
        assertEquals("First event should happen at once.", d1, d2);
    }

    /** Given a frequency that can start at 4:22, check that first event starts
     * first time it is 4:22.
     * @throws Exception
     */
    public void testGetFirstEvent2() throws Exception {
        DailyFrequency freq = new DailyFrequency(4, 4, 22); // Every four days, on the hour and minute
        Calendar cal = GregorianCalendar.getInstance();
        cal.set(2005, Calendar.JUNE, 12, 22, 42);
        Date d1 = cal.getTime();
        cal.add(Calendar.MINUTE, ((60 + 22) - 42));
        cal.add(Calendar.HOUR, ((24 + 4) - (22 + 1)));
        Date d3 = cal.getTime();
        Date d2 = freq.getFirstEvent(d1);
        assertEquals("First event should happen on the 22nd minute of the 4th hour.", d3, d2);
    }

    /** Given a frequency that can start any time, check that next event starts
     * after the correct period.
     * @throws Exception
     */
    public void testGetNextEvent1() throws Exception {
        DailyFrequency freq = new DailyFrequency(4); // Every four days, anytime
        Calendar cal = new GregorianCalendar(2005, Calendar.JUNE, 12, 22, 42);
        Date d1 = cal.getTime();
        cal.add(Calendar.DAY_OF_MONTH, 4);
        Date d3 = cal.getTime();
        Date d4 = freq.getNextEvent(d1);
        assertEquals("Second event should happen four days later", d3, d4);
    }

    /** Given a frequency that can start 5:23, check that next event starts
     * at 5:23 after the correct period.
     * @throws Exception
     */
    public void testGetNextEvent2() throws Exception {
        DailyFrequency freq = new DailyFrequency(4, 5, 23); // Every four days, on the hour and minute
        Calendar cal = GregorianCalendar.getInstance();
        cal.set(2005, Calendar.JUNE, 12, 22, 42);
        Date d1 = cal.getTime();
        cal.add(Calendar.DAY_OF_MONTH, 4);
        cal.add(Calendar.MINUTE, ((60 + 23) - 42));
        cal.add(Calendar.HOUR, ((24 + 5) - (22 + 1)));
        Date d3 = cal.getTime();
        Date d4 = freq.getNextEvent(d1);
        assertEquals("Second event should happen four days later, on the 23rd minute of the 5th hour", d3, d4);
    }

    /** Given a frequency that can start 5:23, check that next event starts
     * at 5:23 after the correct period, given a time that is actually 5:23.
     * @throws Exception
     */
    public void testGetNextEvent3() throws Exception {
        DailyFrequency freq = new DailyFrequency(4, 5, 23); // Every four days, on the hour and minute
        Calendar cal = new GregorianCalendar(2005, Calendar.JUNE, 12, 5, 23);
        Date d1 = cal.getTime();
        Calendar cal2 = new GregorianCalendar(2005, Calendar.JUNE, 16, 5, 23);
        Date d3 = cal2.getTime();
        Date d4 = freq.getNextEvent(d1);
        assertEquals("Second event should happen four days later, on the 23rd minute of the 5th hour", d3, d4);
    }

    /** Test validity of arguments.
     * @throws Exception
     */
    public void testValidityOfArguments() throws Exception {
        try {
            new DailyFrequency(-1);
            fail("should throw exception on negative number of units");
        } catch (ArgumentNotValid e) {
            //Expected
        }

        try {
            new DailyFrequency(0);
            fail("should throw exception on zero number of units");
        } catch (ArgumentNotValid e) {
            //Expected
        }

        try {
            new DailyFrequency(-1, 5, 23);
            fail("should throw exception on negative number of units");
        } catch (ArgumentNotValid e) {
            //Expected
        }

        try {
            new DailyFrequency(0, 5, 23);
            fail("should throw exception on zero number of units");
        } catch (ArgumentNotValid e) {
            //Expected
        }

        try {
            new DailyFrequency(1, 24, 23);
            fail("should throw exception on illegal time");
        } catch (ArgumentNotValid e) {
            //Expected
        }

        try {
            new DailyFrequency(1, -1, 23);
            fail("should throw exception on illegal time");
        } catch (ArgumentNotValid e) {
            //Expected
        }

        try {
            new DailyFrequency(1, 0, -1);
            fail("should throw exception on illegal time");
        } catch (ArgumentNotValid e) {
            //Expected
        }

        try {
            new DailyFrequency(1, 0, 60);
            fail("should throw exception on illegal time");
        } catch (ArgumentNotValid e) {
            //Expected
        }

        DailyFrequency freq = new DailyFrequency(4, 5, 23);
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