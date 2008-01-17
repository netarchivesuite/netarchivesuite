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
 * A simple tuple to deliver information on the status of jobs.
 *
 */

public class JobStatusInfo {
    private final long jobID;
    private final JobStatus status;
    private final long harvestDefinitionID;
    private final String harvestDefinition;
    private final int harvestNum;
    private final String harvestErrors;
    private final String uploadErrors;
    private final String orderXMLname;
    private final int configCount;
    private final Date startDate;
    private final Date endDate;

    JobStatusInfo(long jobID, JobStatus status,
                  long harvestDefinitionID, String harvestDefinition,
                  int harvestNum, String harvestErrors, String uploadErrors,
                  String orderXMLname, int domainCount,
                  Date startDate, Date endDate) {
        this.jobID = jobID;
        this.status = status;
        this.harvestDefinitionID = harvestDefinitionID;
        this.harvestDefinition = harvestDefinition;
        this.harvestNum = harvestNum;
        this.harvestErrors = harvestErrors;
        this.uploadErrors = uploadErrors;
        this.orderXMLname = orderXMLname;
        this.configCount = domainCount;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public long getJobID() {
        return jobID;
    }

    public JobStatus getStatus() {
        return status;
    }

    public long getHarvestDefinitionID() {
        return harvestDefinitionID;
    }

    public String getHarvestDefinition() {
        return harvestDefinition;
    }

    public int getHarvestNum() {
        return harvestNum;
    }

    public String getHarvestErrors() {
        return harvestErrors;
    }

    public String getUploadErrors() {
        return uploadErrors;
    }

    public String getOrderXMLname() {
        return orderXMLname;
    }

    public int getConfigCount() {
        return configCount;
    }

    public Date getStartDate() {
        return startDate;
    }

    public Date getEndDate() {
        return endDate;
    }
}
