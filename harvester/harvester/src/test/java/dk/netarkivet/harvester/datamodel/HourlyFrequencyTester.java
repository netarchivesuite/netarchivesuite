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
 * Tests an hourly frequency.
 */
public class HourlyFrequencyTester extends TestCase {
    public HourlyFrequencyTester(String s) {
        super(s);
    }

    public void setUp() {

    }

    public void tearDown() {

    }

    /** 
     * Test value is hourly.
     */
    public void testTimeunitIsHourly() {
    	HourlyFrequency freq = new HourlyFrequency(20);
        assertEquals("Timeunit must be hourly.", 
        		     freq.ordinal(), 
        		     TimeUnit.HOURLY.ordinal()
        );
        assertEquals("Check TimeUnit hourly", 
        		      TimeUnit.HOURLY, 
        		      TimeUnit.fromOrdinal(TimeUnit.HOURLY.ordinal())
        );
    }

    /** Given a frequency that can start any time, check that first event is
     * immediate.
     * @throws Exception
     */
    public void testGetFirstEvent1() throws Exception {
        HourlyFrequency freq
            = new HourlyFrequency(3); // Every three hours, anytime
        Calendar cal = GregorianCalendar.getInstance();
        cal.set(2005, Calendar.APRIL, 5, 22, 42);
        Date d1 = cal.getTime();
        Date d2 = freq.getFirstEvent(d1);
        assertEquals("First event should happen at once.", d1, d2);
    }

    /** Given a frequency that can start a the 22nd minute, check that first
     * event is at the 22nd minute.
     * @throws Exception
     */
    public void testGetFirstEvent2() throws Exception {
        HourlyFrequency freq
            = new HourlyFrequency(3, 22); // Every three hours, on the minute
        Calendar cal = GregorianCalendar.getInstance();
        cal.set(2005, Calendar.APRIL, 5, 22, 42);
        Date d1 = cal.getTime();
        cal.add(Calendar.MINUTE, ((60 + 22) - 42));
        Date d3 = cal.getTime();
        Date d2 = freq.getFirstEvent(d1);
        assertEquals("First event should happen on the 22nd minute.", d2, d3);
    }

    /** Given a frequency that can start any time, check that next event is
     * after correct period.
     * @throws Exception
     */
    public void testGetNextEvent1() throws Exception {
        HourlyFrequency freq
            = new HourlyFrequency(3); // Every three hours, anytime
        Calendar cal = GregorianCalendar.getInstance();
        cal.set(2005, Calendar.APRIL, 5, 22, 42);
        Date d1 = cal.getTime();
        cal.add(Calendar.HOUR, 3);
        Date d3 = cal.getTime();
        Date d4 = freq.getNextEvent(d1);
        assertEquals("Second event should happen three hours later", d3, d4);
    }

    /** Given a frequency that can start at 23rd minute, check that next event
     * is after correct period on 23rd minute.
     * @throws Exception
     */
    public void testGetNextEvent2() throws Exception {
        HourlyFrequency freq
            = new HourlyFrequency(3, 23); // Every three hours, on the minute
        Calendar cal = GregorianCalendar.getInstance();
        cal.set(2005, Calendar.APRIL, 5, 22, 42);
        Date d1 = cal.getTime();
        cal.add(Calendar.HOUR, 3);
        cal.add(Calendar.MINUTE, ((60 + 23)-42));
        Date d3 = cal.getTime();
        Date d4 = freq.getNextEvent(d1);
        assertEquals("Second event should happen three hours later"
                + ", on the 23rd minute", d3, d4);
    }

    /** Given a frequency that can start at 23rd minute, check that next event
     * is after correct period on 23rd minute given a time that is on the 23rd
     * minute.
     * @throws Exception
     */
    public void testGetNextEvent3() throws Exception {
        HourlyFrequency freq
            = new HourlyFrequency(3, 22); // Every three hours, on the minute
        Calendar cal = new GregorianCalendar(2005, Calendar.APRIL, 5, 3, 22);
        Date d1 = cal.getTime();
        Calendar cal2 = new GregorianCalendar(2005, Calendar.APRIL, 5, 6, 22);
        Date d3 = cal2.getTime();
        Date d2 = freq.getNextEvent(d1);
        assertEquals("Second event should happen three hours later", d2, d3);
    }
    
    /** Test validity of arguments.
     *  @throws Exception
     */
    public void testValidityOfArguments() throws Exception {
        try {
            new HourlyFrequency(-1);
            fail("should throw exception on negative number of units");
        } catch (ArgumentNotValid e) {
            //Expected
        }

        try {
            new HourlyFrequency(0);
            fail("should throw exception on zero number of units");
        } catch (ArgumentNotValid e) {
            //Expected
        }

        try {
            new HourlyFrequency(-1, 23);
            fail("should throw exception on negative number of units");
        } catch (ArgumentNotValid e) {
            //Expected
        }

        try {
            new HourlyFrequency(0, 23);
            fail("should throw exception on zero number of units");
        } catch (ArgumentNotValid e) {
            //Expected
        }

        try {
            new HourlyFrequency(1, -1);
            fail("should throw exception on illegal time");
        } catch (ArgumentNotValid e) {
            //Expected
        }

        try {
            new HourlyFrequency(1, 60);
            fail("should throw exception on illegal time");
        } catch (ArgumentNotValid e) {
            //Expected
        }


        HourlyFrequency freq = new HourlyFrequency(4, 23);
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
