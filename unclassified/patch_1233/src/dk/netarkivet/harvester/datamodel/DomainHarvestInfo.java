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
 * DomainConfigPair class for extracted information on harvests on a specific domain.
 *
 */

public class DomainHarvestInfo {
    private final String domain;
    private final int jobID;
    private final long harvestID;
    private final int harvestNum;
    private final String harvestName;
    private final String configName;
    private final Date startDate;
    private final Date endDate;
    private final long bytesDownloaded;
    private final long docsDownloaded;
    private final StopReason reason;

    DomainHarvestInfo(String domain, int jobID, String harvestName,
                      long harvestID, int harvestNum, String configName,
                      Date startDate, Date endDate, long bytesDownloaded, long docsDownloaded,
                      StopReason reason) {
        this.domain = domain;
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

    public String getDomain() {
        return domain;
    }

    public int getJobID() {
        return jobID;
    }

    public String getHarvestName() {
        return harvestName;
    }

    public long getHarvestID() {
        return harvestID;
    }
    public int getHarvestNum() {
        return harvestNum;
    }

    public String getConfigName() {
        return configName;
    }

    public Date getStartDate() {
        return startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public long getBytesDownloaded() {
        return bytesDownloaded;
    }

    public long getDocsDownloaded() {
        return docsDownloaded;
    }

    public StopReason getStopReason() {
        return reason;
    }
}

