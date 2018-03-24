/*
 * #%L
 * Netarchivesuite - harvester
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

import java.io.Serializable;

import dk.netarkivet.common.distribute.Channels;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.harvester.datamodel.JobStatus;
import dk.netarkivet.harvester.distribute.HarvesterMessage;
import dk.netarkivet.harvester.distribute.HarvesterMessageVisitor;
import dk.netarkivet.harvester.harvesting.monitor.HarvestMonitor;

/**
 * This message is sent by the HarvestSchedulerMonitorServer to the {@link HarvestMonitor} to notify it that a
 * job ended and should not be monitored anymore, and that any resource used to monitor this job should be freed.
 */
@SuppressWarnings({"serial"})
public class JobEndedMessage extends HarvesterMessage implements Serializable {

    /** The associated job's ID. */
    private final long jobId;

    /** The associated job's current status. */
    private final JobStatus jobStatus;

    /**
     * Constructs a new message.
     *
     * @param jobId the job ID.
     * @param jobStatus the job's current status.
     */
    public JobEndedMessage(long jobId, JobStatus jobStatus) {
        super(HarvestMonitor.HARVEST_MONITOR_CHANNEL_ID, Channels.getError());
        ArgumentNotValid.checkNotNull(jobStatus, "jobStatus");
        this.jobId = jobId;
        if (!(JobStatus.DONE.equals(jobStatus) || JobStatus.FAILED.equals(jobStatus))) {
            throw new ArgumentNotValid("Got status '" + jobStatus.name() + "', expected either '"
                    + JobStatus.DONE.name() + "' or '" + JobStatus.FAILED.name() + "'");
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
