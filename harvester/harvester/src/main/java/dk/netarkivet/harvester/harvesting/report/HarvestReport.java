package dk.netarkivet.harvester.harvesting.report;

import java.io.Serializable;
import java.util.Set;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.harvester.datamodel.Job;
import dk.netarkivet.harvester.datamodel.StopReason;
import dk.netarkivet.harvester.harvesting.HeritrixFiles;

/**
 * Base interface for a post-crawl harvest report.
 */
public interface HarvestReport extends Serializable {

    /**
     * Returns the default stop reason initially assigned to every domain.
     */
    StopReason getDefaultStopReason();

    /**
     * Returns the set of domain names
     * that are contained in hosts-report.txt
     * (i.e. host names mapped to domains)
     *
     * @return a Set of Strings
     */
    Set<String> getDomainNames();

    /**
     * Get the number of objects found for the given domain.
     *
     * @param domainName A domain name (as given by getDomainNames())
     * @return How many objects were collected for that domain
     * @throws ArgumentNotValid if null or empty domainName
     */
    Long getObjectCount(String domainName) throws ArgumentNotValid;

    /**
     * Get the number of bytes downloaded for the given domain.
     *
     * @param domainName A domain name (as given by getDomainNames())
     * @return How many bytes were collected for that domain
     * @throws ArgumentNotValid if null or empty domainName
     */
    Long getByteCount(String domainName) throws ArgumentNotValid;

    /**
     * Get the StopReason for the given domain.
     * @param domainName A domain name (as given by getDomainNames())
     * @return the StopReason for the given domain.
     * @throws ArgumentNotValid if null or empty domainName
     */
    StopReason getStopReason(String domainName) throws ArgumentNotValid;

    /**
     * Pre-processing happens when the report is built just at the end of the
     * crawl, before the ARC files upload.
     */
    void preProcess(HeritrixFiles files);

    /**
     * Post-processing happens on the scheduler side when ARC files
     * have been uploaded.
     */
    void postProcess(Job job);

}
