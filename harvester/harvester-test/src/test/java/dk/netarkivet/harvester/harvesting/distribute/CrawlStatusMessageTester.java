/*
 * #%L
 * Netarchivesuite - harvester - test
 * %%
 * Copyright (C) 2005 - 2018 The Royal Danish Library, 
 *             the National Library of France and the Austrian National Library.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 2.1 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */

package dk.netarkivet.harvester.harvesting.distribute;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;

import org.junit.Test;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.harvester.datamodel.JobStatus;
import dk.netarkivet.testutils.Serial;

/**
 * Unit tests for CrawlStatusMessage.
 */
public class CrawlStatusMessageTester {

    /**
     * Test that we can call the constructor for finished jobs
     */
    @Test
    public void testJobFinishedCTOR() {
        CrawlStatusMessage csm;
        csm = new CrawlStatusMessage(12l, JobStatus.DONE, null);
        assertEquals("Don't get back the id we entered", 12l, csm.getJobID());
        assertEquals("Don't get back the status we entered", JobStatus.DONE, csm.getStatusCode());
        csm = new CrawlStatusMessage(12l, JobStatus.FAILED, null);
        assertEquals("Don't get back the id we entered", 12l, csm.getJobID());
        assertEquals("Don't get back the status we entered", JobStatus.FAILED, csm.getStatusCode());
    }

    /**
     * CTOR should fail if not given a null JobStatus, or a negative JobId
     */
    @Test
    public void testJobFinishedCTORFails() {
        try {
            new CrawlStatusMessage(12L, null, null);
            fail("CTOR with RemoteFile should fail if JobStatus is null");
        } catch (ArgumentNotValid e) {
            // expected
        }
        try {
            new CrawlStatusMessage(-1L, JobStatus.NEW, null);
            fail("CTOR with RemoteFile should fail if jobid < 0");
        } catch (ArgumentNotValid e) {
            // expected
        }
    }

    /**
     * Test that we can call the constructor for unfinished jobs
     */
    @Test
    public void testJobNotFinishedCTOR() {
        CrawlStatusMessage csm;
        csm = new CrawlStatusMessage(12l, JobStatus.NEW);
        assertEquals("Don't get back the id we entered", 12l, csm.getJobID());
        assertEquals("Don't get back the status we entered", JobStatus.NEW, csm.getStatusCode());
        csm = new CrawlStatusMessage(12l, JobStatus.STARTED);
        assertEquals("Don't get back the id we entered", 12l, csm.getJobID());
        assertEquals("Don't get back the status we entered", JobStatus.STARTED, csm.getStatusCode());
        csm = new CrawlStatusMessage(12l, JobStatus.SUBMITTED);
        assertEquals("Don't get back the id we entered", 12l, csm.getJobID());
        assertEquals("Don't get back the status we entered", JobStatus.SUBMITTED, csm.getStatusCode());
    }

    @Test
    public void testJobNotFinishedCTORFails() {
        try {
            new CrawlStatusMessage(-1L, JobStatus.DONE);
            fail("CTOR without RemoteFile should fail if jobid is negative");
        } catch (ArgumentNotValid e) {
            // expected
        }
        try {
            new CrawlStatusMessage(12l, null);
            fail("CTOR without RemoteFile should fail if JobStatus is null");
        } catch (ArgumentNotValid e) {
            // expected
        }

    }

    /**
     * Test that class is serializable
     */
    @Test
    public void testSerializable() throws IOException, ClassNotFoundException {
        CrawlStatusMessage csm = new CrawlStatusMessage(0l, JobStatus.DONE, null);
        CrawlStatusMessage csm2 = (CrawlStatusMessage) Serial.serial(csm);
        assertEquals("Deserialization error for CrawlStatusMessage", relevantState(csm), relevantState(csm2));
    }

    /**
     * Returns a string representation of the information to be serialized in a CrawlStatusMessage
     *
     * @param csm the CrawlstatusMessage
     * @return the string representation
     */
    public String relevantState(CrawlStatusMessage csm) {
        return "" + csm.getJobID() + " " + csm.getStatusCode();
    }

}
