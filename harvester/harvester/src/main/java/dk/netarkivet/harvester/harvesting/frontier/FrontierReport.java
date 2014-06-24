package dk.netarkivet.harvester.harvesting.frontier;



/**
 * Common interface for an Heritrix frontier report wrapper.
 *
 */
public interface FrontierReport {

    /**
     * @return the jobName
     */
    String getJobName();

    /**
     * @return the creation timestamp
     */
    long getTimestamp();

    /**
     * Add a line to the report.
     * @param line line to add.
     */
    void addLine(FrontierReportLine line);

    /**
     * Returns the line of the frontier report corresponding to the
     * queue for the given domain name.
     * @param domainName the domain name.
     * @return null if no queue for this domain name exists, otherwise the line
     * of the frontier report corresponding to the queue for the
     * given domain name.
     */
    FrontierReportLine getLineForDomain(String domainName);

}
