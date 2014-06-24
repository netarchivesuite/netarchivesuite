package dk.netarkivet.harvester.harvesting.frontier;

import java.io.Serializable;

import dk.netarkivet.common.exceptions.ArgumentNotValid;

/**
 * Base abstract implementation of an Heritrix frontier report wrapper.
 *
 */
@SuppressWarnings({ "serial"})
abstract class AbstractFrontierReport implements FrontierReport, Serializable {

    /**
     * The Heritrix job name.
     */
    private String jobName;

    /**
     * The report generation timestamp.
     */
    private long timestamp;

    /**
     * Default empty contrcutor.
     */
    AbstractFrontierReport() {

    }

    /**
     * Initializes an empty Heritrix frontier report wrapper object.
     * @param jobName the Heritrix job name
     */
    public AbstractFrontierReport(String jobName) {
        ArgumentNotValid.checkNotNullOrEmpty(jobName, "jobName");
        this.jobName = jobName;
        this.timestamp = System.currentTimeMillis();
    }

    @Override
    public String getJobName() {
        return jobName;
    }

    @Override
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * @param jobName the jobName to set
     */
    protected void setJobName(String jobName) {
        this.jobName = jobName;
    }

    /**
     * @param timestamp the timestamp to set
     */
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public abstract void addLine(FrontierReportLine line);

    @Override
    public abstract FrontierReportLine getLineForDomain(String domainName);

}
