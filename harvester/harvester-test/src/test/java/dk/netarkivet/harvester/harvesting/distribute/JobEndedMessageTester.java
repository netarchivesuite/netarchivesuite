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

import org.junit.Test;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.harvester.datamodel.JobStatus;

public class JobEndedMessageTester {

    @Test
    public void testJobEndedConstructor() {
        JobEndedMessage msg = new JobEndedMessage(42L, JobStatus.DONE);
        assertEquals(JobStatus.DONE, msg.getJobStatus());
        msg = new JobEndedMessage(42L, JobStatus.FAILED);
        assertEquals(42L, msg.getJobId());
        assertEquals(JobStatus.FAILED, msg.getJobStatus());
        try {
            new JobEndedMessage(42L, JobStatus.STARTED);
            fail("Should throw ArgumentNotValid given states " + "other than DONE and FAILED");
        } catch (ArgumentNotValid e) {
            // Expected
        }

    }
}
