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

/**
 * Tests the JobPriority class.
 */

import java.util.Locale;

import junit.framework.TestCase;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.utils.I18n;

public class JobPriorityTester extends TestCase {
    
    private static final I18n I18N =
    	new I18n(dk.netarkivet.harvester.Constants.TRANSLATIONS_BUNDLE);
    
    public JobPriorityTester(String s) {
        super(s);
    }

    public void setUp() {
    }

    public void tearDown() {
    }

    /** Tests the translation from numbers to job Priorities.
     *
     * DO NOT CHANGE THESE NUMBERS! JobPriority is serialised to database using
     * these numbers, and they are never expected to change. That would kill an
     * upgrade with an existing database!
     */
    public void testFromOrdinal() {
        //Test that jobpriorityNum=0 returns JobPriority.LOWPRIORITY
        assertEquals("fromOrdinal(0) should return JobPriority.LOWPRIORITY",
        		JobPriority.LOWPRIORITY,
                JobPriority.fromOrdinal(0));

        //Test that jobpriorityNum=1 returns JobPriority.HIGHPRIORITY
        assertEquals("fromOrdinal(1) should return JobPriority.HIGHPRIORITY",
        		JobPriority.HIGHPRIORITY,
        		JobPriority.fromOrdinal(1));

        // Test that stopreasonNum less than 0 and greater than 4 results in IOFailure
        try {
            JobPriority.fromOrdinal(2);
            fail ("ArgumentNotValid expected");
        } catch (ArgumentNotValid e){
            // ArgumentNotValid expected
        }
    }
    
    /** 
     * Test, that the localized String for a given JobPriority is correct.
     * We only test with english Locale.  
     */
   public void testGetLocalizedString() {
       Locale l = new Locale("en");
       //JobPriority.HIGHPRIORITY.
       //JobPriority.LOWPRIORITY
       assertEquals("JobPriority.HIGHPRIORITY.getLocalizedString(l) " +
            "should return correct String for english Locale", JobPriority.HIGHPRIORITY.getLocalizedString(l), 
            I18N.getString(l,"partial.harvest"));
       
       assertEquals("JobPriority.LOWPRIORITY.getLocalizedString(l) " +
               "should return correct String for english Locale", JobPriority.LOWPRIORITY.getLocalizedString(l), 
               I18N.getString(l,"full.harvest"));
       
       // Verify that the unknown case does not break I18n
      assertTrue("getLocalizedString should not break in the unknown case", 
              I18N.getString(l, "unknown.harvest.type.0").contains("Unknown harvest type ")); 
   }
}