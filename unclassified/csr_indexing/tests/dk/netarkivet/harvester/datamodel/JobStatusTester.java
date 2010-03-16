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

import java.util.Locale;

import junit.framework.TestCase;

/**
 * Tests of the JobStatus class.
 * Currently, only the static method getLocalizedString
 * is tested here.
 */
public class JobStatusTester extends TestCase {
    public JobStatusTester(String s) {
        super(s);
    }

    public void setUp() {
    }

    public void tearDown() {
    }

    /** 
     * Test getLocalizedString.
     */
    public void testGetLocalizedString() {
        Locale en = new Locale("en");
        assertEquals(JobStatus.NEW.getLocalizedString(en), "New");
        assertEquals(JobStatus.DONE.getLocalizedString(en), "Done");
        assertEquals(JobStatus.SUBMITTED.getLocalizedString(en), "Submitted");
        assertEquals(JobStatus.STARTED.getLocalizedString(en), "Started");
        assertEquals(JobStatus.FAILED.getLocalizedString(en), "Failed");
        assertEquals(JobStatus.RESUBMITTED.getLocalizedString(en), "Resubmitted");       
      }
}
