/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
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

package dk.netarkivet.harvester.harvesting.distribute;

import java.io.IOException;

import junit.framework.TestCase;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.harvester.datamodel.JobStatus;
import dk.netarkivet.testutils.Serial;

/**
 * Unit tests for CrawlStatusMessage
 *
 */

public class CrawlStatusMessageTester extends TestCase {



    /**
     * Test that we can call the constructor for finished jobs
     */
    public void testJobFinishedCTOR() {
        CrawlStatusMessage csm;
        csm = new CrawlStatusMessage(
                12l, JobStatus.DONE, null);
        assertEquals("Don't get back the id we entered", 12l, csm.getJobID());
        assertEquals("Don't get back the status we entered", JobStatus.DONE,
                csm.getStatusCode());
        csm = new CrawlStatusMessage(
                12l, JobStatus.FAILED, null);
        assertEquals("Don't get back the id we entered", 12l, csm.getJobID());
        assertEquals("Don't get back the status we entered", JobStatus.FAILED,
                csm.getStatusCode());
    }

    /**
     * CTOR should fail if not given a null JobStatus, or a negative JobId
     */
    public void testJobFinishedCTORFails() {
        try {
            new CrawlStatusMessage(
                    12L, null, null);
            fail("CTOR with RemoteFile should fail if JobStatus is null") ;
        } catch (ArgumentNotValid e ) {
            //expected
        }
        try {
            new CrawlStatusMessage(
                    -1L, JobStatus.NEW, null);
            fail("CTOR with RemoteFile should fail if jobid < 0");
        } catch (ArgumentNotValid e ) {
            //expected
        }
    }

    /**
     * Test that we can call the constructor for unfinished jobs
     */
    public void testJobNotFinishedCTOR() {
        CrawlStatusMessage csm;
        csm = new CrawlStatusMessage(
                12l, JobStatus.NEW);
        assertEquals("Don't get back the id we entered", 12l, csm.getJobID());
        assertEquals("Don't get back the status we entered", JobStatus.NEW,
                csm.getStatusCode());
        csm = new CrawlStatusMessage(
                12l, JobStatus.STARTED);
        assertEquals("Don't get back the id we entered", 12l, csm.getJobID());
        assertEquals("Don't get back the status we entered", JobStatus.STARTED,
                csm.getStatusCode());
        csm = new CrawlStatusMessage(
                12l, JobStatus.SUBMITTED);
        assertEquals("Don't get back the id we entered", 12l, csm.getJobID());
        assertEquals("Don't get back the status we entered", JobStatus.SUBMITTED,
                csm.getStatusCode());
    }

    public void testJobNotFinishedCTORFails() {
        CrawlStatusMessage csm;
        try {
            csm = new CrawlStatusMessage(
                    -1L, JobStatus.DONE);
            fail("CTOR without RemoteFile should fail if jobid is negative") ;
        } catch (ArgumentNotValid e ) {
            //expected
        }
        try {
            csm = new CrawlStatusMessage(
                    12l, null);
            fail("CTOR without RemoteFile should fail if JobStatus is null") ;
        } catch (ArgumentNotValid e ) {
            //expected
        }

    }


    /**
     * Test that class is serializable
     */
    public void testSerializable() throws IOException, ClassNotFoundException {
        CrawlStatusMessage csm = new CrawlStatusMessage(
                0l, JobStatus.DONE, null);
        CrawlStatusMessage csm2 = (CrawlStatusMessage) Serial.serial(csm);
        assertEquals("Deserialization error for CrawlStatusMessage",
                relevantState(csm), relevantState(csm2));
    }

    /**
     * Returns a string representation of the information to be serialized in
     * a CrawlStatusMessage
     * @param csm the CrawlstatusMessage
     * @return  the string representation
     */
    public String relevantState(CrawlStatusMessage csm){
        return ""+csm.getJobID()+" "+csm.getStatusCode();
    }

}
