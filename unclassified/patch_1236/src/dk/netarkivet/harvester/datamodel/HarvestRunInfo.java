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
import java.util.HashMap;
import java.util.Map;

/**
 * Information on a single run of a harvest.
 *
 */

public class HarvestRunInfo {
    /** Which harvest def ID this is a run of. */
    private final long harvestID;
    /** The name of the harvest def.*/
    private final String harvestName;
    /** Which run this is of the harvest. */
    private final int runNr;
    /** When the first job for this harvest started. */
    private Date startDate;
    /** When the last job for this harvest ended. */
    private Date endDate;
    /** How many bytes were harvested total. */
    private long bytesHarvested;
    /** How many documents were harvested total. */
    private long docsHarvested;
    /** Status of the jobs used to run this harvest. */
    private final Map<JobStatus, Integer> jobCounts
            = new HashMap<JobStatus, Integer>();

    HarvestRunInfo(long harvestID, String harvestName, int runNr) {
        this.harvestID = harvestID;
        this.harvestName = harvestName;
        this.runNr = runNr;
    }

    public long getHarvestID() {
        return harvestID;
    }

    public String getHarvestName() {
        return harvestName;
    }

    public int getRunNr() {
        return runNr;
    }

    public Date getStartDate() {
        return startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public long getBytesHarvested() {
        return bytesHarvested;
    }

    public long getDocsHarvested() {
        return docsHarvested;
    }

    /** Get the total number of jobs created for this run.
     *  @return the total number of jobs created for this run.
     */
    public int getJobCount() {
        int count = 0;
        for (int c : jobCounts.values()) {
            count += c;
        }
        return count;
    }

    /** Get the number of jobs for this run that are in a specific status.
     *  @param status the specific status
     *  @return the number of jobs for this run that are in a specific status.
     */
    public int getJobCount(JobStatus status) {
        if (jobCounts.containsKey(status)) {
            return jobCounts.get(status);
        } else {
            return 0;
        }
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public void setBytesHarvested(long bytesHarvested) {
        this.bytesHarvested = bytesHarvested;
    }

    public void setDocsHarvested(long docsHarvested) {
        this.docsHarvested = docsHarvested;
    }

    public void setStatusCount(JobStatus status, int count) {
        jobCounts.put(status, count);
    }
}
