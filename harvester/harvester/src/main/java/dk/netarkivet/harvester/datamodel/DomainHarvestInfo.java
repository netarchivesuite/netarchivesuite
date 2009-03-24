/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
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

package dk.netarkivet.harvester.datamodel;

import java.util.Date;

/**
 * DomainConfigPair class for extracted information on harvests on a specific
 * domain.
 */
public class DomainHarvestInfo {
    /** The name of the domain. */
    private final String domainName;
    /** The Id of the job. */
    private final long jobID;
    /** The Id of the harvestdefinition, which this job was generated from. */
    private final long harvestID;
    /** The number of the harvest. */
    private final int harvestNum;
    /** The name of the harvestdefinition. */
    private final String harvestName;
    /** The name of the configuration. */
    private final String configName;
    /** The date when the harvestjob started. */
    private final Date startDate;
    /** The date when the harvestjob finished. */
    private final Date endDate;
    /** How many bytes were downloaded by this job for this domain. */
    private final long bytesDownloaded;
    /** How many documents(URIs) were downloaded by this job for this domain. */
    private final long docsDownloaded;
    /** The reason why the harvestjob stopped harvesting any more URIs
     * from this domain.
     */
    private final StopReason reason;
    
    /**
     * Constructor for a DomainHarvestInfo object.
     * @param domainName The given domain
     * @param jobID The Id of the job that harvested this domain
     * @param harvestName The name of the harvestdefinition behind the job
     * @param harvestID The ID of the harvestdefinition behind the job
     * @param harvestNum The number of the harvest
     * @param configName The name of the configuration
     * @param startDate The date when the harvestjob started
     * @param endDate The date when the harvestjob finished
     * @param bytesDownloaded How many bytes were downloaded by this job for
     *                        this domain
     * @param docsDownloaded How many documents(URIs) were downloaded by
     *                       this job for this domain.
     * @param reason The reason why the harvestjob stopped harvesting any more
     * URIs from this domain.
     */
    DomainHarvestInfo(String domainName, long jobID, String harvestName,
                      long harvestID, int harvestNum, String configName, 
                      Date startDate, Date endDate, 
                      long bytesDownloaded, long docsDownloaded, 
                      StopReason reason) {
        this.domainName = domainName;
        this.jobID = jobID;
        this.harvestID = harvestID;
        this.harvestName = harvestName;
        this.harvestNum = harvestNum;
        this.configName = configName;
        this.startDate = startDate;
        this.endDate = endDate;
        this.bytesDownloaded = bytesDownloaded;
        this.docsDownloaded = docsDownloaded;
        this.reason = reason;
    }

    /**
     * Get the domain Name.
     * @return the domain Name.
     */
    public String getDomain() {
        return domainName;
    }  
    
    /**
     * Get the Id of the job that harvested this domain.
     * @return the Id of the job that harvested this domain
     */
    public long getJobID() {
        return jobID;
    }

    /**
     * Get the name of the harvestdefinition behind the job.
     * @return The name of the harvestdefinition behind the job
     */
    public String getHarvestName() {
        return harvestName;
    }

    /**
     * Get the ID of the harvestdefinition behind the job.
     * @return The ID of the harvestdefinition behind the job
     */
    public long getHarvestID() {
        return harvestID;
    }
 
    /**
     * Get the number of the harvest.
     * @return The number of the harvest
     */
    public int getHarvestNum() {
        return harvestNum;
    }
    
    /**
     * Get the name of the configuration.
     * @return The name of the configuration
     */
    public String getConfigName() {
        return configName;
    }
    
    /**
     * Get the date when the harvestjob started.
     * @return Get the date when the harvestjob started.
     */
    public Date getStartDate() {
        return startDate;
    }

    /**
     * Get the date when the harvestjob finished.
     * @return Get the date when the harvestjob finished.
     */
    public Date getEndDate() {
        return endDate;
    }

    /**
     * Get the number of bytes that were downloaded by this job for this domain.
     * @return The number of bytes that were downloaded by this job for this
     * domain.
     */
    public long getBytesDownloaded() {
        return bytesDownloaded;
    }

    /**
     * Get the number of documents (URIs) that were downloaded by this job
     * for this domain.
     * @return The number of documents (URIs) that were downloaded by this job
     * for this domain.
     */
    public long getDocsDownloaded() {
        return docsDownloaded;
    }

    /**
     * Get the reason why the harvestjob stopped harvesting any more
     * URIs from this domain.
     * @return The reason why the harvestjob stopped harvesting any more
     * URIs from this domain.
     */
    public StopReason getStopReason() {
        return reason;
    }
}
