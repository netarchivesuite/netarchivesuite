package dk.netarkivet.harvester.harvesting.distribute;

import dk.netarkivet.common.distribute.Channels;
import dk.netarkivet.harvester.distribute.HarvesterMessage;
import dk.netarkivet.harvester.distribute.HarvesterMessageVisitor;
import dk.netarkivet.harvester.harvesting.frontier.FrontierReportFilter;
import dk.netarkivet.harvester.harvesting.frontier.InMemoryFrontierReport;
import dk.netarkivet.harvester.harvesting.monitor.HarvestMonitor;

/**
 * Sends a frontier report to the {@link HarvestMonitor}.
 *
 */
@SuppressWarnings({ "serial"})
public class FrontierReportMessage extends HarvesterMessage {

    /**
     * The id of the filter that generated this report.
     */
    private String filterId;

    /**
     * The report.
     */
    private InMemoryFrontierReport report;
    
    /** The ID of the job, this message represents. */
    private Long jobID;

    /**
     * Builds a frontier report wrapper message.
     * @param filter the filter that generated the report.
     * @param report the report to wrap.
     */
    public FrontierReportMessage(
            FrontierReportFilter filter,
            InMemoryFrontierReport report,
            Long jobID) {
        super(HarvestMonitor.HARVEST_MONITOR_CHANNEL_ID, Channels.getError());
        this.filterId = filter.getFilterId();
        this.report = report;
        this.jobID = jobID;
    }

    @Override
    public void accept(HarvesterMessageVisitor v) {
        v.visit(this);
    }

    /**
     * @return the filter id
     */
    public String getFilterId() {
        return filterId;
    }

    /**
     * @return the report
     */
    public InMemoryFrontierReport getReport() {
        return report;
    }
    
    public Long getJobID() {
        return jobID;
    }

}
