/* File:       $Id$
 * Revision:   $Revision$
 * Author:     $Author$
 * Date:       $Date$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2007 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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

import java.io.Serializable;

import dk.netarkivet.common.distribute.Channels;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.harvester.datamodel.JobStatus;
import dk.netarkivet.harvester.distribute.HarvesterMessage;
import dk.netarkivet.harvester.distribute.HarvesterMessageVisitor;
import dk.netarkivet.harvester.harvesting.monitor.HarvestMonitorServer;
import dk.netarkivet.harvester.scheduler.HarvestSchedulerMonitorServer;

/**
 * This message is sent by the {@link HarvestSchedulerMonitorServer} to the
 * {@link HarvestMonitorServer} to notify it that a job ended and should not be
 * monitored anymore, and that any resource used to monitor this job
 * should be freed.
 */
public class JobEndedMessage extends HarvesterMessage
implements Serializable {

    /**
     * The associated job's ID.
     */
    private final long jobId;

    /**
     * The associated job's current status.
     */
    private final JobStatus jobStatus;

    /**
     * Constructs a new message.
     * @param jobId the job ID.
     * @param jobStatus the job's current status.
     */
    public JobEndedMessage(long jobId, JobStatus jobStatus) {
        super(HarvestMonitorServer.CRAWL_PROGRESS_CHANNEL_ID,
                Channels.getError());
        ArgumentNotValid.checkNotNull(jobStatus, "jobStatus");
        this.jobId = jobId;
        if (! (JobStatus.DONE.equals(jobStatus)
                || JobStatus.FAILED.equals(jobStatus))) {
            throw new ArgumentNotValid("Got status '" + jobStatus.name()
                    + "', expected either '" + JobStatus.DONE.name()
                    + "' or '" + JobStatus.FAILED.name() + "'");
        }
        this.jobStatus = jobStatus;
    }

    /**
     * @return the job id
     */
    public long getJobId() {
        return jobId;
    }

    /**
     * @return the job status
     */
    public JobStatus getJobStatus() {
        return jobStatus;
    }

    @Override
    public void accept(HarvesterMessageVisitor v) {
        v.visit(this);
    }

}
