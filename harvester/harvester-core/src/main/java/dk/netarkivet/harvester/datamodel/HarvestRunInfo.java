/*
 * #%L
 * Netarchivesuite - harvester
 * %%
 * Copyright (C) 2005 - 2018 The Royal Danish Library, 
 *             the National Library of France and the Austrian National Library.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 2.1 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */

package dk.netarkivet.harvester.datamodel;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Information on a single run of a harvest.
 */
public class HarvestRunInfo {

    /** Which harvest def ID this is a run of. */
    private final long harvestID;
    /** The name of the harvest def. */
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
    private final Map<JobStatus, Integer> jobCounts = new HashMap<JobStatus, Integer>();

    /**
     * Constructor used to read harvest run information from database.
     *
     * @param harvestID the ID of the harvest job
     * @param harvestName the name of the harvest job
     * @param runNr the run number of this harvest job
     */
    HarvestRunInfo(long harvestID, String harvestName, int runNr) {
        this.harvestID = harvestID;
        this.harvestName = harvestName;
        this.runNr = runNr;
    }

    /** @return the harvest id of this job */
    public long getHarvestID() {
        return harvestID;
    }

    /** @return the harvest name of this job */
    public String getHarvestName() {
        return harvestName;
    }

    /** @return the harvest run number of this job */
    public int getRunNr() {
        return runNr;
    }

    /** @return the date when this job started */
    public Date getStartDate() {
        return startDate;
    }

    /** @return the date when this job ended */
    public Date getEndDate() {
        return endDate;
    }

    /** @return bytes harvested by the job */
    public long getBytesHarvested() {
        return bytesHarvested;
    }

    /** @return documents harvested by the job */
    public long getDocsHarvested() {
        return docsHarvested;
    }

    /**
     * Get the total number of jobs created for this run.
     *
     * @return the total number of jobs created for this run.
     */
    public int getJobCount() {
        int count = 0;
        for (int c : jobCounts.values()) {
            count += c;
        }
        return count;
    }

    /**
     * Get the number of jobs for this run that are in a specific status.
     *
     * @param status the specific status
     * @return the number of jobs for this run that are in a specific status.
     */
    public int getJobCount(JobStatus status) {
        if (jobCounts.containsKey(status)) {
            return jobCounts.get(status);
        } else {
            return 0;
        }
    }

    /**
     * Set the start Date for this harvest job.
     *
     * @param startDate the start date
     */
    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    /**
     * Set the end Date for this harvest job.
     *
     * @param endDate The end date
     */
    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    /**
     * Set the number of bytes harvested by this job.
     *
     * @param bytesHarvested number of bytes harvested
     */
    public void setBytesHarvested(long bytesHarvested) {
        this.bytesHarvested = bytesHarvested;
    }

    /**
     * Set the number of documents harvested by this job.
     *
     * @param docsHarvested number of documents harvested
     */
    public void setDocsHarvested(long docsHarvested) {
        this.docsHarvested = docsHarvested;
    }

    /**
     * Update the count for a specific jobstatus.
     *
     * @param status a certain JobStatus
     * @param count the new count for this JobStatus.
     */
    public void setStatusCount(JobStatus status, int count) {
        jobCounts.put(status, count);
    }

}
