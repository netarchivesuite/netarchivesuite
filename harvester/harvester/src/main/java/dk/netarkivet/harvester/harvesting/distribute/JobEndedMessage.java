package dk.netarkivet.harvester.harvesting.distribute;

import java.io.Serializable;

import dk.netarkivet.common.distribute.Channels;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.harvester.datamodel.JobStatus;
import dk.netarkivet.harvester.distribute.HarvesterMessage;
import dk.netarkivet.harvester.distribute.HarvesterMessageVisitor;
import dk.netarkivet.harvester.harvesting.monitor.HarvestMonitor;
import dk.netarkivet.harvester.scheduler.HarvestSchedulerMonitorServer;

/**
 * This message is sent by the {@link HarvestSchedulerMonitorServer} to the
 * {@link HarvestMonitor} to notify it that a job ended and should not be
 * monitored anymore, and that any resource used to monitor this job
 * should be freed.
 */
@SuppressWarnings({ "serial"})
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
        super(HarvestMonitor.HARVEST_MONITOR_CHANNEL_ID,
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
