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

/**
 * Tests the StopReason class.
 */

import java.util.Locale;

import junit.framework.TestCase;

import dk.netarkivet.common.exceptions.UnknownID;
import dk.netarkivet.common.utils.I18n;

public class StopReasonTester extends TestCase {
    
    private static final I18n I18N = new I18n(dk.netarkivet.harvester.Constants.TRANSLATIONS_BUNDLE);
    
    public StopReasonTester(String s) {
        super(s);
    }

    public void setUp() {
    }

    public void tearDown() {
    }

    /** Tests the translation from numbers to stop reasons.
     *
     * DO NOT CHANGE THESE NUMBERS! StopReason is serialised to database using
     * these numbers, and they are never expected to change. That would kill an
     * upgrade with an existing database!
     */
    public void testGetStopReason() {
        //Test that stopreasonNum=0 returns StopReason.DOWNLOAD_COMPLETE
        assertEquals("getStopReason(0) should return StopReason.DOWNLOAD_COMPLETE",
                     StopReason.DOWNLOAD_COMPLETE,
                     StopReason.getStopReason(0));

        //Test that stopreasonNum=1 returns StopReason.OBJECT_LIMIT
        assertEquals("getStopReason(1) should return StopReason.OBJECT_LIMIT",
                     StopReason.OBJECT_LIMIT,
                     StopReason.getStopReason(1));

        //Test that stopreasonNum=2 returns StopReason.SIZE_LIMIT
        assertEquals("getStopReason(2) should return StopReason.SIZE_LIMIT",
                     StopReason.SIZE_LIMIT,
                     StopReason.getStopReason(2));

        //Test that stopreasonNum=3 returns StopReason.CONFIG_SIZE_LIMIT
        assertEquals("getStopReason(3) should return StopReason.CONFIG_SIZE_LIMIT",
                     StopReason.CONFIG_SIZE_LIMIT,
                     StopReason.getStopReason(3));

        //Test that stopreasonNum=4 returns StopReason.DOWNLOAD_UNFINISHED
        assertEquals("getStopReason(4) should return StopReason.DOWNLOAD_UNFINISHED",
                     StopReason.DOWNLOAD_UNFINISHED,
                     StopReason.getStopReason(4));
        
        //Test that stopreasonNum=5 returns StopReason.CONFIG_OBJECT_LIMIT
        assertEquals("getStopReason(5) should return StopReason.CONFIG_OBJECT_LIMIT",
                     StopReason.CONFIG_OBJECT_LIMIT,
                     StopReason.getStopReason(5));

        // Test that stopreasonNum less than 0 and greater than 5 results in IOFailure
        try {
            StopReason.getStopReason(6);
            fail ("UnknownID expected");
        } catch (UnknownID e){
            // UnknownID expected
        }
    }
    
    /** 
     * Test, that the localized String for a given StopReason is correct.
     * We only test with english Locale.  
     */
   public void testGetLocalizedString() {
       Locale l = new Locale("en");
       assertEquals("StopReason.DOWNLOAD_UNFINISHED.getLocalizedString(l) " +
            "should return correct String for english Locale", StopReason.DOWNLOAD_UNFINISHED.getLocalizedString(l), 
            I18N.getString(l,"stopreason.download.unfinished"));
       
       assertEquals("StopReason.DOWNLOAD_COMPLETE.getLocalizedString(l) " +
               "should return correct String for english Locale", StopReason.DOWNLOAD_COMPLETE.getLocalizedString(l), 
               I18N.getString(l,"stopreason.complete"));
       
       assertEquals("StopReason.CONFIG_SIZE_LIMIT.getLocalizedString(l) " +
               "should return correct String for english Locale", StopReason.CONFIG_SIZE_LIMIT.getLocalizedString(l), 
               I18N.getString(l,"stopreason.max.domainconfig.limit.reached"));
       
       assertEquals("StopReason.OBJECT_LIMIT.getLocalizedString(l) " +
               "should return correct String for english Locale", StopReason.OBJECT_LIMIT.getLocalizedString(l), 
               I18N.getString(l,"stopreason.max.objects.limit.reached"));
       
       assertEquals("StopReason.SIZE_LIMIT.getLocalizedString(l) " +
               "should return correct String for english Locale", StopReason.SIZE_LIMIT.getLocalizedString(l), 
               I18N.getString(l,"stopreason.max.bytes.limit.reached"));
       
       // Verify that the unknown case does not break I18n
      assertTrue("getLocalizedString should not break in the unknown case", 
              I18N.getString(l, "stopreason.unknown.0").contains("Unknown reason:")); 
   }
}