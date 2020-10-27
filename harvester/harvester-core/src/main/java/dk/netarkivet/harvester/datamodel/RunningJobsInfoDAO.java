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

import java.util.List;
import java.util.Map;
import java.util.Set;

import dk.netarkivet.harvester.harvesting.frontier.FrontierReportFilter;
import dk.netarkivet.harvester.harvesting.frontier.InMemoryFrontierReport;
import dk.netarkivet.harvester.harvesting.monitor.StartedJobInfo;

/**
 * Abstract class for handling the persistence of running job infos.
 *
 * @see StartedJobInfo
 */
public abstract class RunningJobsInfoDAO implements DAO {

    /** The singleton instance of this class. */
    private static RunningJobsInfoDAO instance;

    /**
     * Constructor used when creating singleton. Do not call directly.
     */
    protected RunningJobsInfoDAO() {

    }

    /**
     * Gets the JobDAO singleton.
     *
     * @return the JobDAO singleton
     */
    public static synchronized RunningJobsInfoDAO getInstance() {
        if (instance == null) {
            instance = new RunningJobsInfoDBDAO();
        }
        return instance;
    }

    /**
     * Stores a {@link StartedJobInfo} record to the persistent storage. The record is stored in the monitor table, and
     * if the elapsed time since the last history sample is equal or superior to the history sample rate, also to the
     * history table.
     *
     * @param startedJobInfo the record to store.
     */
    public abstract void store(StartedJobInfo startedJobInfo);

    /**
     * Returns the most recent record for every job, partitioned by harvest definition name.
     *
     * @return the full listing of started job information, partitioned by harvest definition name.
     */
    public abstract Map<String, List<StartedJobInfo>> getMostRecentByHarvestName();

    /**
     * Returns an array of all progress records chronologically sorted for the given job ID.
     *
     * @param jobId the job id.
     * @return an array of all progress records chronologically sorted for the given job ID.
     */
    public abstract StartedJobInfo[] getFullJobHistory(long jobId);

    /**
     * Returns an array of progress records chronologically sorted for the given job ID, starting at a given crawl time,
     * and limited to a given number of records.
     *
     * @param jobId the job id.
     * @param startTime the crawl time (in seconds) to begin.
     * @param limit the maximum number of records to fetch.
     * @return an array of progress records chronologically sorted for the given job ID, starting at a given crawl time,
     * and limited to a given number of record.
     */
    public abstract StartedJobInfo[] getMostRecentByJobId(long jobId, long startTime, int limit);

    /**
     * Returns the most recent progress record for the given job ID.
     *
     * @param jobId the job id.
     * @return the most recent progress record for the given job ID.
     */
    public abstract StartedJobInfo getMostRecentByJobId(long jobId);

    /**
     * Removes all monitor and history records pertaining to the given job ID from the persistent storage.
     *
     * @param jobId the job id.
     * @return the number of deleted records.
     */
    public abstract int removeInfoForJob(long jobId);

    /**
     * Store frontier report data to the persistent storage.
     *
     * @param report the report to store
     * @param filterId the id of the filter that produced the report
     * @param jobId The ID of the harvestjob responsible for this report
     * @return the update count
     */
    public abstract int storeFrontierReport(String filterId, InMemoryFrontierReport report, Long jobId);

    /**
     * Returns the list of the available frontier report types.
     *
     * @return the list of the available frontier report types.
     * @see FrontierReportFilter#getFilterId()
     */
    public abstract String[] getFrontierReportFilterTypes();

    /**
     * Retrieve a frontier report from a job id and a given filter class.
     *
     * @param jobId the job id
     * @param filterId the id of the filter that produced the report
     * @return a frontier report
     */
    public abstract InMemoryFrontierReport getFrontierReport(long jobId, String filterId);
    
    /**
     * Retrieve a frontier report from a job id, with limited results and possibility to sort by totalenqueues DESC
     *
     * @param jobId the job id
     * @param limit the limit of result to query
     * @param sort if true, sort the results by totalenqueues DESC
     * @return a frontier report
     */
    public abstract InMemoryFrontierReport getFrontierReport(long jobId, int limit, boolean sort);
    
    /**
     * Retrieve a frontier report from a job id, with limited results and possibility to sort by totalenqueues DESC
     *
     * @param jobId the job id
     * @param limit the limit of result to query
     * @param filterId the id of the filter that produced the report
     * @param sort if true, sort the results by totalenqueues DESC
     * @return a frontier report
     */
    public abstract InMemoryFrontierReport getFrontierReport(long jobId, String filterId, int limit, boolean sort);

    /**
     * Deletes all frontier report data pertaining to the given job id from the persistent storage.
     *
     * @param jobId the job id
     * @return the update count
     */
    public abstract int deleteFrontierReports(long jobId);

    /**
     * Returns the ids of jobs for which history records exist, as an immutable set.
     *
     * @return the ids of jobs for which history records exist.
     */
    public abstract Set<Long> getHistoryRecordIds();

}
