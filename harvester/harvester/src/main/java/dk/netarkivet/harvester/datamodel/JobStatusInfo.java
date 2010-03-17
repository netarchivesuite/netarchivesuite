/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2010 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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
 * A simple tuple to deliver information on the status of jobs.
 *
 */
public class JobStatusInfo {
    /** The ID of the job. */
    private final long jobID;
    /** The current status of the Job. */
    private final JobStatus status;
    /** The Id of the harvestdefinition behind this job. */
    private final long harvestDefinitionID;
    /** The name of the harvestdefinition behind this job. */
    private final String harvestDefinition;
    /** The number of times a harvestdefinition has been performed. */
    private final int harvestNum;
    /** Any errors encountered during the actual harvest. */
    private final String harvestErrors;
    /** Any errors encountered during the upload of the result files. */
    private final String uploadErrors;
    /** The name of the Heritrix Template used by this job. */
    private final String orderXMLname;
    /** The number of domain-configurations used for this job. */
    private final int configCount;
    /** The time when this job was submitted. */
    private final Date submittedDate;
    /** The time when this job started. */
    private final Date startDate;
    /** The time when this job finished. */
    private final Date endDate;
    /**
     * The ID of the job this job was resubmitted as.
     */
    private final Long resubmittedAsJobWithID;
    
    /**
     * Constructor for the JobStatusInfo class. 
     * @param jobID The ID of the job
     * @param status The current status of the Job
     * @param harvestDefinitionID The Id of the harvestdefinition behind
     * this job
     * @param harvestDefinition The name of the harvestdefinition behind
     * this job
     * @param harvestNum The number of times a harvestdefinition has been
     * performed
     * @param harvestErrors Any errors encountered during the actual harvest
     * @param uploadErrors Any errors encountered during the upload of the
     * result files
     * @param orderXMLname The name of the Heritrix Template used by this job
     * @param domainCount The number of domain-configurations used for this job
     * @param submittedDate The time when this job was submitted
     * @param startDate The time when this job started
     * @param endDate The time when this job finished
     * @param resubmittedAsJobWithID The id of the job this job was resubmitted
     *  as (possibly null)
     */
    JobStatusInfo(long jobID, JobStatus status,
                  long harvestDefinitionID, String harvestDefinition,
                  int harvestNum, String harvestErrors, String uploadErrors,
                  String orderXMLname, int domainCount, Date submittedDate,
                  Date startDate, Date endDate, Long resubmittedAsJobWithID) {
        this.jobID = jobID;
        this.status = status;
        this.harvestDefinitionID = harvestDefinitionID;
        this.harvestDefinition = harvestDefinition;
        this.harvestNum = harvestNum;
        this.harvestErrors = harvestErrors;
        this.uploadErrors = uploadErrors;
        this.orderXMLname = orderXMLname;
        this.configCount = domainCount;
        this.submittedDate = submittedDate;
        this.startDate = startDate;
        this.endDate = endDate;
        this.resubmittedAsJobWithID = resubmittedAsJobWithID;
        
    }

    /**
     * @return the ID of the job.
     */
    public long getJobID() {
        return jobID;
    }
    
    /**
     * @return the current status of the Job
     */
    public JobStatus getStatus() {
        return status;
    }
    
    /**
     * @return The Id of the harvestdefinition behind the job
     */
    public long getHarvestDefinitionID() {
        return harvestDefinitionID;
    }
    
    /**
     * @return The name of the harvestdefinition behind the job.
     */
    public String getHarvestDefinition() {
        return harvestDefinition;
    }
    
    /**
     * @return the harvest number
     */
    public int getHarvestNum() {
        return harvestNum;
    }

    /**
     * @return Any errors encountered during the actual harvest
     */
    public String getHarvestErrors() {
        return harvestErrors;
    }

    /**
     * @return Any errors encountered during the upload of the result files.
     */
    public String getUploadErrors() {
        return uploadErrors;
    }

    /**
     * @return The name of the Heritrix Template used by the job.
     */
    public String getOrderXMLname() {
        return orderXMLname;
    }

    /**
     * @return The number of domain-configurations used for the job.
     */
    public int getConfigCount() {
        return configCount;
    }

    /**
     * @return The time when the job was submitted
     */
    public Date getSubmittedDate() {
        return submittedDate;
    }

    
    /**
     * @return The time when the job started
     */
    public Date getStartDate() {
        return startDate;
    }

    /**
     * @return The time when the job finished
     */
    public Date getEndDate() {
        return endDate;
    }
    
    /**
     * @return the ID of the job this job was resubmitted as. If null this
     * job has not been resubmitted.
     */
    public Long getResubmittedAsJob() {
        return this.resubmittedAsJobWithID;
    }
}
