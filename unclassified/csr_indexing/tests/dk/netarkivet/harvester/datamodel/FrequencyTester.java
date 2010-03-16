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

import junit.framework.TestCase;

import dk.netarkivet.common.exceptions.ArgumentNotValid;

/**
 * Tests of the abstract Frequency class.
 * Currently, only the static method getNewInstance
 * is tested here.
 * The other tests are performed in the subclasses of Frequency.
 */
public class FrequencyTester extends TestCase {
    public FrequencyTester(String s) {
        super(s);
    }

    public void setUp() {
    }

    public void tearDown() {
    }

    /** 
     * Test Frequency.getNewInstance.
     */
    public void testGetNewInstance() {
        
        // test, that negative values for timeunit and numtimeunits
        // throws ArgumentNotValid
        
        int timeunit = -1;
        boolean anytime = true;
        int numtimeunit = 2;
        Integer minut = 6;
        Integer hour = 7;
        Integer dayOfWeek = 1;
        Integer dayOfMonth = 1;
        
        try {
            Frequency.getNewInstance(timeunit, anytime, numtimeunit, minut, hour,
                    dayOfWeek, dayOfMonth);
            fail("ArgumentNotValid exception expected for negative timeunit");
        } catch (ArgumentNotValid e) {
            // Expected
        }
        
        timeunit = TimeUnit.DAILY.ordinal();
        numtimeunit = -1;
        try {
            Frequency.getNewInstance(timeunit, anytime, numtimeunit, minut, hour,
                    dayOfWeek, dayOfMonth);
            fail("ArgumentNotValid exception expected for negative numtimeunits");
        } catch (ArgumentNotValid e) {
            // Expected
        }
  
        
        // check that null Integers are always allowed, if anytime is true
        minut = null; hour = null; dayOfWeek = null; dayOfMonth = null;
        numtimeunit = 2;
        anytime = true;
        try {
            Frequency.getNewInstance(timeunit, anytime, numtimeunit, minut, hour,
                    dayOfWeek, dayOfMonth);
        } catch (ArgumentNotValid e) {
            fail("ArgumentNotValid exception NOT expected "
            		+ "for null Integers when anytime is true");
        }
        
        // Test that Null integers are not allowed in some cases:
        // case 1: null value for minute, if anytime is false
        // case 2: null value for hour, if anytime is false, 
        //         and we are not creating a Hourly frequency
        // case 3: null value for dayofweek, if anytime is false and 
        //         we are creating a Weekly frequency
        // case 4: null value for dayofmonth, if anytime is false,
        //         and we are creating a Monthly frequency
        
        anytime = false;
        // case 1
        minut = null;
        try {
            Frequency.getNewInstance(timeunit, anytime, numtimeunit, minut, hour,
                    dayOfWeek, dayOfMonth);
            fail("ArgumentNotValid exception expected if null minut and anytime is true");
        } catch (ArgumentNotValid e) {
            // Expected
        }
        
       // case 2
        minut = 1;
        hour = null;
        
        timeunit = TimeUnit.HOURLY.ordinal();
        Frequency.getNewInstance(timeunit, anytime, numtimeunit, minut, hour,
                  dayOfWeek, dayOfMonth);
        try {
            timeunit = TimeUnit.DAILY.ordinal();
            Frequency.getNewInstance(timeunit, anytime, numtimeunit, minut, hour,
                    dayOfWeek, dayOfMonth);
            fail("ArgumentNotValid exception expected if null hour,"
                    + "anytime is true, and timeunit is daily");
        } catch (ArgumentNotValid e) {
            // Expected
        }

        // case 3:
        anytime = false;
        minut = 1;
        hour = 1;
        dayOfWeek = null;
        dayOfMonth = 1;
        try {
            timeunit = TimeUnit.HOURLY.ordinal();
        
            Frequency.getNewInstance(timeunit, anytime, numtimeunit, minut, hour,
                    dayOfWeek, dayOfMonth);
            
            timeunit = TimeUnit.DAILY.ordinal();
            Frequency.getNewInstance(timeunit, anytime, numtimeunit, minut, hour,
                    dayOfWeek, dayOfMonth);
            
            timeunit = TimeUnit.MONTHLY.ordinal();
            Frequency.getNewInstance(timeunit, anytime, numtimeunit, minut, hour,
                    dayOfWeek, dayOfMonth);
        } catch (ArgumentNotValid e) {
            fail("ArgumentNotValid exception NOT expected if dayofweek is null,"
                    + "anytime is true, and timeunit is not weekly");
        }

        try {
            timeunit = TimeUnit.WEEKLY.ordinal();
            
            Frequency.getNewInstance(timeunit, anytime, numtimeunit, minut, hour,
                    dayOfWeek, dayOfMonth);
            
            fail("ArgumentNotValid exception expected if dayofweek is null,"
                    + "anytime is true, and timeunit is weekly");
        } catch (ArgumentNotValid e) {
            // Expected
        }
         
        // case 4: null value for dayofmonth, if anytime is false,
        //         and we are creating a Monthly frequency
        
        anytime = false;
        minut = 1;
        hour = 1;
        dayOfWeek = 1;
        dayOfMonth = null;
        try {
            timeunit = TimeUnit.HOURLY.ordinal();
        
            Frequency.getNewInstance(timeunit, anytime, numtimeunit, minut, hour,
                    dayOfWeek, dayOfMonth);
            
            timeunit = TimeUnit.DAILY.ordinal();
            Frequency.getNewInstance(timeunit, anytime, numtimeunit, minut, hour,
                    dayOfWeek, dayOfMonth);
            
            timeunit = TimeUnit.WEEKLY.ordinal();
            Frequency.getNewInstance(timeunit, anytime, numtimeunit, minut, hour,
                    dayOfWeek, dayOfMonth);
        } catch (ArgumentNotValid e) {
            fail("ArgumentNotValid exception NOT expected if dayofmonth is null,"
                    + "anytime is true, and timeunit is not monthly");
        }
        
        try {
            timeunit = TimeUnit.MONTHLY.ordinal();
            
            Frequency.getNewInstance(timeunit, anytime, numtimeunit, minut, hour,
                    dayOfWeek, dayOfMonth);
            
            fail("ArgumentNotValid exception expected if dayofmonth is null,"
                    + "anytime is true, and timeunit is monthly");
        } catch (ArgumentNotValid e) {
            // Expected
        }
        
    }
}
