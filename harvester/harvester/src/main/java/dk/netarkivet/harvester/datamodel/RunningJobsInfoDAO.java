/* File:        $Id$
 * Revision:    $Revision$
 * Date:        $Date$
 * Author:      $Author$
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

import java.util.List;
import java.util.Map;

import dk.netarkivet.harvester.harvesting.frontier.FrontierReportFilter;
import dk.netarkivet.harvester.harvesting.frontier.InMemoryFrontierReport;
import dk.netarkivet.harvester.harvesting.monitor.StartedJobInfo;

/**
 * Abstract class for handling the persistence of running job infos.
 *
 * @see StartedJobInfo
 *
 */
public abstract class RunningJobsInfoDAO {

    /**
     * The singleton instance of this class.
     */
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
     * Stores a {@link StartedJobInfo} record to the persistent storage.
     * The record is stored in the monitor table, and if the elapsed time since
     * the last history sample is equal or superior to the history sample rate,
     * also to the history table.
     * @param startedJobInfo the record to store.
     */
    public abstract void store(StartedJobInfo startedJobInfo);

    /**
     * Returns the most recent record for every job, partitioned by harvest
     * definition name.
     * @return the full listing of started job information, partitioned by
     *         harvest definition name.
     */
    public abstract Map<String, List<StartedJobInfo>>
        getMostRecentByHarvestName();

    /**
     * Returns an array of all progress records chronologically sorted for the
     * given job ID.
     * @param jobId the job id.
     * @return an array of all progress records chronologically sorted for the
     * given job ID.
     */
    public abstract StartedJobInfo[] getFullJobHistory(long jobId);

    /**
     * Returns an array of progress records chronologically sorted for the
     * given job ID, starting at a given crawl time, and limited to a given
     * number of records.
     * @param jobId the job id.
     * @param startTime the crawl time (in seconds) to begin.
     * @param limit the maximum number of records to fetch.
     * @return an array of progress records chronologically sorted for the
     * given job ID, starting at a given crawl time, and limited to a given
     * number of record.
     */
    public abstract StartedJobInfo[] getMostRecentByJobId(
            long jobId,
            long startTime,
            int limit);

    /**
     * Returns the most recent progress record for the given job ID.
     * @param jobId the job id.
     * @return the most recent progress record for the given job ID.
     */
    public abstract StartedJobInfo getMostRecentByJobId(long jobId);

    /**
     * Removes all monitor and history records pertaining to the given job ID
     * from the persistent storage.
     * @param jobId the job id.
     * @return the number of deleted records.
     */
    public abstract int removeInfoForJob(long jobId);

    /**
     * Store frontier report data to the persistent storage.
     * @param report the report to store
     * @param filterId the id of the filter that produced the report
     * @return the update count
     */
    public abstract int storeFrontierReport(
            String filterId, InMemoryFrontierReport report);

    /**
     * Returns the list of the available frontier report types.
     * @see FrontierReportFilter#getFilterId()
     * @return the list of the available frontier report types.
     */
    public abstract String[] getFrontierReportFilterTypes();

    /**
     * Retrieve a frontier report from a job id and a given filter class.
     * @param jobId the job id
     * @param filterId the id of the filter that produced the report
     * @return a frontier report
     */
    public abstract InMemoryFrontierReport getFrontierReport(
            long jobId, String filterId);

    /**
     * Deletes all frontier report data pertaining to the given job id from
     * the persistent storage.
     * @param jobId the job id
     * @return the update count
     */
    public abstract int deleteFrontierReports(long jobId);

}
