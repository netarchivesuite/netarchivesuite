/*$Id$
* $Revision$
* $Date$
* $Author$
*
* The Netarchive Suite - Software to harvest and preserve websites
* Copyright 2004-2012 The Royal Danish Library, the Danish State and
 * University Library, the National Library of France and the Austrian
 * National Library.
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

import dk.netarkivet.common.exceptions.ArgumentNotValid;

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
        assertEquals(JobStatus.FAILED_REJECTED.getLocalizedString(en), "Failed (Rejected for Resubmission)");
      }

    public void testLegalChange() {
        JobStatus status = JobStatus.FAILED_REJECTED;
        assertTrue("Should be legal to change JobStatus from FAILED_REJECTED "
                   + "back to FAILED", status.legalChange(JobStatus.FAILED));
    }
    
    public void testFromOrdinal() {
        assertEquals(JobStatus.NEW, JobStatus.fromOrdinal(0));
        assertEquals(JobStatus.SUBMITTED, JobStatus.fromOrdinal(1));
        assertEquals(JobStatus.STARTED, JobStatus.fromOrdinal(2));
        assertEquals(JobStatus.DONE, JobStatus.fromOrdinal(3));
        assertEquals(JobStatus.FAILED, JobStatus.fromOrdinal(4));
        assertEquals(JobStatus.RESUBMITTED, JobStatus.fromOrdinal(5));
        assertEquals(JobStatus.FAILED_REJECTED, JobStatus.fromOrdinal(6));
        try {
            JobStatus.fromOrdinal(7);
            fail("Should throw ArgumentNotValid on invalid status, but didn't");
        } catch (ArgumentNotValid e) {
            // Expected
        }
    }
    

}
