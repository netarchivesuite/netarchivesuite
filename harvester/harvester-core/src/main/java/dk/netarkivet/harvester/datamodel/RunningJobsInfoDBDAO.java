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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.UnknownID;
import dk.netarkivet.common.utils.DBUtils;
import dk.netarkivet.common.utils.ExceptionUtils;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.harvester.HarvesterSettings;
import dk.netarkivet.harvester.harvesting.distribute.CrawlProgressMessage.CrawlStatus;
import dk.netarkivet.harvester.harvesting.frontier.FrontierReportFilter;
import dk.netarkivet.harvester.harvesting.frontier.FrontierReportLine;
import dk.netarkivet.harvester.harvesting.frontier.InMemoryFrontierReport;
import dk.netarkivet.harvester.harvesting.monitor.StartedJobInfo;

/**
 * Class implementing the persistence of running job infos.
 */
public class RunningJobsInfoDBDAO extends RunningJobsInfoDAO {

    /** The logger. */
    private static final Logger log = LoggerFactory.getLogger(RunningJobsInfoDBDAO.class);

    /** Max length of urls stored in tables. */
    private static final int MAX_URL_LENGTH = 1000;

    /**
     * Defines the order of columns in the runningJobsMonitor table. Used in SQL queries.
     */
    private static enum HM_COLUMN {
        jobId, harvestName, elapsedSeconds, hostUrl, progress, queuedFilesCount, totalQueuesCount, activeQueuesCount, retiredQueuesCount, exhaustedQueuesCount, alertsCount, downloadedFilesCount, currentProcessedKBPerSec, processedKBPerSec, currentProcessedDocsPerSec, processedDocsPerSec, activeToeCount, status, tstamp;

        /**
         * Returns the rank in an SQL query (ordinal + 1).
         *
         * @return ordinal() + 1
         */
        int rank() {
            return ordinal() + 1;
        }

        /**
         * Returns the SQL substring that lists columns according to their ordinal.
         *
         * @return the SQL substring that lists columns in proper order.
         */
        static String getColumnsInOrder() {
            StringBuffer columns = new StringBuffer();
            for (HM_COLUMN c : values()) {
                columns.append(c.name() + ", ");
            }
            return columns.substring(0, columns.lastIndexOf(","));
        }
    }

    /**
     * Date of last history record per job.
     */
    private static Map<Long, Long> lastSampleDateByJobId = new HashMap<Long, Long>();

    /**
     * Rate in milliseconds at which history records should be sampled for a running job.
     */
    private static final long HISTORY_SAMPLE_RATE = 1000 * Settings
            .getLong(HarvesterSettings.HARVEST_MONITOR_HISTORY_SAMPLE_RATE);

    /**
     * The constructor of RunningJobsInfoDBDAO. Attempts to update/install the necessary database tables, if they need
     * to be updated.
     */
    public RunningJobsInfoDBDAO() {
        Connection connection = HarvestDBConnection.get();
        try {
            /**
             * Update if necessary the current version of the tables 'runningJobsHistory', 'runningJobsMonitor' and
             * 'frontierReportMonitor'.
             */
            HarvesterDatabaseTables.checkVersion(connection, HarvesterDatabaseTables.RUNNINGJOBSHISTORY);
            HarvesterDatabaseTables.checkVersion(connection, HarvesterDatabaseTables.RUNNINGJOBSMONITOR);
            HarvesterDatabaseTables.checkVersion(connection, HarvesterDatabaseTables.FRONTIERREPORTMONITOR);
        } finally {
            HarvestDBConnection.release(connection);
        }
    }

    /**
     * Stores a {@link StartedJobInfo} record to the persistent storage. The record is stored in the monitor table, and
     * if the elapsed time since the last history sample is equal or superior to the history sample rate, also to the
     * history table.
     *
     * @param startedJobInfo the record to store.
     */
    @Override
    public synchronized void store(StartedJobInfo startedJobInfo) {
        ArgumentNotValid.checkNotNull(startedJobInfo, "StartedJobInfo startedJobInfo");

        Connection c = HarvestDBConnection.get();

        try {
            PreparedStatement stm = null;

            // First is there a record in the monitor table?
            boolean update = false;
            try {
                stm = c.prepareStatement("SELECT jobId FROM runningJobsMonitor WHERE jobId=? AND harvestName=?");
                stm.setLong(1, startedJobInfo.getJobId());
                stm.setString(2, startedJobInfo.getHarvestName());

                // One row expected, as per PK definition
                update = stm.executeQuery().next();

            } catch (SQLException e) {
                String message = "SQL error checking running jobs monitor table" + "\n"
                        + ExceptionUtils.getSQLExceptionCause(e);
                log.warn(message, e);
                throw new IOFailure(message, e);
            }

            try {
                // Update or insert latest progress information for this job
                c.setAutoCommit(false);

                StringBuffer sql = new StringBuffer();

                if (update) {
                    sql.append("UPDATE runningJobsMonitor SET ");

                    StringBuffer columns = new StringBuffer();
                    // FIXME Seriously, construct an identical SQL string every time and use an enum...?!
                    for (HM_COLUMN setCol : HM_COLUMN.values()) {
                        columns.append(setCol.name() + "=?, ");
                    }
                    sql.append(columns.substring(0, columns.lastIndexOf(",")));
                    sql.append(" WHERE jobId=? AND harvestName=?");
                } else {
                    sql.append("INSERT INTO runningJobsMonitor (");
                    sql.append(HM_COLUMN.getColumnsInOrder());
                    sql.append(") VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
                }

                stm = c.prepareStatement(sql.toString());
                stm.setLong(HM_COLUMN.jobId.rank(), startedJobInfo.getJobId());
                stm.setString(HM_COLUMN.harvestName.rank(), startedJobInfo.getHarvestName());
                stm.setLong(HM_COLUMN.elapsedSeconds.rank(), startedJobInfo.getElapsedSeconds());
                stm.setString(HM_COLUMN.hostUrl.rank(), startedJobInfo.getHostUrl());
                stm.setDouble(HM_COLUMN.progress.rank(), startedJobInfo.getProgress());
                stm.setLong(HM_COLUMN.queuedFilesCount.rank(), startedJobInfo.getQueuedFilesCount());
                stm.setLong(HM_COLUMN.totalQueuesCount.rank(), startedJobInfo.getTotalQueuesCount());
                stm.setLong(HM_COLUMN.activeQueuesCount.rank(), startedJobInfo.getActiveQueuesCount());
                stm.setLong(HM_COLUMN.retiredQueuesCount.rank(), startedJobInfo.getRetiredQueuesCount());
                stm.setLong(HM_COLUMN.exhaustedQueuesCount.rank(), startedJobInfo.getExhaustedQueuesCount());
                stm.setLong(HM_COLUMN.alertsCount.rank(), startedJobInfo.getAlertsCount());
                stm.setLong(HM_COLUMN.downloadedFilesCount.rank(), startedJobInfo.getDownloadedFilesCount());
                stm.setLong(HM_COLUMN.currentProcessedKBPerSec.rank(), startedJobInfo.getCurrentProcessedKBPerSec());
                stm.setLong(HM_COLUMN.processedKBPerSec.rank(), startedJobInfo.getProcessedKBPerSec());
                stm.setDouble(HM_COLUMN.currentProcessedDocsPerSec.rank(),
                        startedJobInfo.getCurrentProcessedDocsPerSec());
                stm.setDouble(HM_COLUMN.processedDocsPerSec.rank(), startedJobInfo.getProcessedDocsPerSec());
                stm.setInt(HM_COLUMN.activeToeCount.rank(), startedJobInfo.getActiveToeCount());
                stm.setInt(HM_COLUMN.status.rank(), startedJobInfo.getStatus().ordinal());
                stm.setTimestamp(HM_COLUMN.tstamp.rank(), new Timestamp(startedJobInfo.getTimestamp().getTime()));

                if (update) {
                    stm.setLong(HM_COLUMN.values().length + 1, startedJobInfo.getJobId());
                    stm.setString(HM_COLUMN.values().length + 2, startedJobInfo.getHarvestName());
                }

                stm.executeUpdate();

                c.commit();
            } catch (SQLException e) {
                String message = "SQL error storing started job info " + startedJobInfo + " in monitor table" + "\n"
                        + ExceptionUtils.getSQLExceptionCause(e);
                log.warn(message, e);
                throw new IOFailure(message, e);
            } finally {
                DBUtils.closeStatementIfOpen(stm);
                DBUtils.rollbackIfNeeded(c, "store started job info", startedJobInfo);
            }

            // Should we store an history record?
            Long lastHistoryStore = lastSampleDateByJobId.get(startedJobInfo.getJobId());

            long time = System.currentTimeMillis();
            boolean shouldSample = lastHistoryStore == null || time >= lastHistoryStore + HISTORY_SAMPLE_RATE;

            if (!shouldSample) {
                return; // we're done
            }
            log.debug("Adding history Record for job {} to runningJobsHistory table", startedJobInfo.getJobId());
            try {
                c.setAutoCommit(false);

                stm = c.prepareStatement("INSERT INTO runningJobsHistory (" + HM_COLUMN.getColumnsInOrder()
                        + ") VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
                stm.setLong(HM_COLUMN.jobId.rank(), startedJobInfo.getJobId());
                stm.setString(HM_COLUMN.harvestName.rank(), startedJobInfo.getHarvestName());
                stm.setLong(HM_COLUMN.elapsedSeconds.rank(), startedJobInfo.getElapsedSeconds());
                stm.setString(HM_COLUMN.hostUrl.rank(), startedJobInfo.getHostUrl());
                stm.setDouble(HM_COLUMN.progress.rank(), startedJobInfo.getProgress());
                stm.setLong(HM_COLUMN.queuedFilesCount.rank(), startedJobInfo.getQueuedFilesCount());
                stm.setLong(HM_COLUMN.totalQueuesCount.rank(), startedJobInfo.getTotalQueuesCount());
                stm.setLong(HM_COLUMN.activeQueuesCount.rank(), startedJobInfo.getActiveQueuesCount());
                stm.setLong(HM_COLUMN.retiredQueuesCount.rank(), startedJobInfo.getRetiredQueuesCount());
                stm.setLong(HM_COLUMN.exhaustedQueuesCount.rank(), startedJobInfo.getExhaustedQueuesCount());
                stm.setLong(HM_COLUMN.alertsCount.rank(), startedJobInfo.getAlertsCount());
                stm.setLong(HM_COLUMN.downloadedFilesCount.rank(), startedJobInfo.getDownloadedFilesCount());
                stm.setLong(HM_COLUMN.currentProcessedKBPerSec.rank(), startedJobInfo.getCurrentProcessedKBPerSec());
                stm.setLong(HM_COLUMN.processedKBPerSec.rank(), startedJobInfo.getProcessedKBPerSec());
                stm.setDouble(HM_COLUMN.currentProcessedDocsPerSec.rank(),
                        startedJobInfo.getCurrentProcessedDocsPerSec());
                stm.setDouble(HM_COLUMN.processedDocsPerSec.rank(), startedJobInfo.getProcessedDocsPerSec());
                stm.setInt(HM_COLUMN.activeToeCount.rank(), startedJobInfo.getActiveToeCount());
                stm.setInt(HM_COLUMN.status.rank(), startedJobInfo.getStatus().ordinal());
                stm.setTimestamp(HM_COLUMN.tstamp.rank(), new Timestamp(startedJobInfo.getTimestamp().getTime()));

                stm.executeUpdate();

                c.commit();
            } catch (SQLException e) {
                String message = "SQL error storing started job info " + startedJobInfo + " in history table" + "\n"
                        + ExceptionUtils.getSQLExceptionCause(e);
                log.warn(message, e);
                throw new IOFailure(message, e);
            } finally {
                DBUtils.closeStatementIfOpen(stm);
                DBUtils.rollbackIfNeeded(c, "store started job info", startedJobInfo);
            }

            // Remember last sampling date
            lastSampleDateByJobId.put(startedJobInfo.getJobId(), time);
        } finally {
            HarvestDBConnection.release(c);
        }
    }

    /**
     * Returns an array of all progress records chronologically sorted for the given job ID.
     *
     * @param jobId the job id.
     * @return an array of all progress records chronologically sorted for the given job ID.
     */
    @Override
    public StartedJobInfo[] getFullJobHistory(long jobId) {
        Connection c = HarvestDBConnection.get();
        PreparedStatement stm = null;
        try {
            stm = c.prepareStatement("SELECT " + HM_COLUMN.getColumnsInOrder() + " FROM runningJobsHistory"
                    + " WHERE jobId=?" + " ORDER BY elapsedSeconds ASC");
            stm.setLong(1, jobId);

            ResultSet rs = stm.executeQuery();
            List<StartedJobInfo> infosForJob = listFromResultSet(rs);

            return (StartedJobInfo[]) infosForJob.toArray(new StartedJobInfo[infosForJob.size()]);

        } catch (SQLException e) {
            String message = "SQL error querying runningJobsHistory for job ID " + jobId + " from database" + "\n"
                    + ExceptionUtils.getSQLExceptionCause(e);
            log.warn(message, e);
            throw new IOFailure(message, e);
        } finally {
            DBUtils.closeStatementIfOpen(stm);
            HarvestDBConnection.release(c);
        }
    }

    /**
     * Returns the most recent record for every job, partitioned by harvest definition name.
     *
     * @return the full listing of started job information, partitioned by harvest definition name.
     */
    @Override
    public Map<String, List<StartedJobInfo>> getMostRecentByHarvestName() {
        Connection c = HarvestDBConnection.get();

        Map<String, List<StartedJobInfo>> infoMap = new TreeMap<String, List<StartedJobInfo>>();
        Statement stm = null;
        try {
            stm = c.createStatement();
            ResultSet rs = stm.executeQuery("SELECT " + HM_COLUMN.getColumnsInOrder() + " FROM runningJobsMonitor");

            while (rs.next()) {
                long jobId = rs.getLong(HM_COLUMN.jobId.rank());
                String harvestName = rs.getString(HM_COLUMN.harvestName.rank());

                List<StartedJobInfo> infosForHarvest = infoMap.get(harvestName);
                if (infosForHarvest == null) {
                    infosForHarvest = new LinkedList<StartedJobInfo>();
                    infoMap.put(harvestName, infosForHarvest);
                }

                StartedJobInfo sji = new StartedJobInfo(harvestName, jobId);

                sji.setElapsedSeconds(rs.getLong(HM_COLUMN.elapsedSeconds.rank()));
                sji.setHostUrl(rs.getString(HM_COLUMN.hostUrl.rank()));
                sji.setProgress(rs.getDouble(HM_COLUMN.progress.rank()));
                sji.setQueuedFilesCount(rs.getLong(HM_COLUMN.queuedFilesCount.rank()));
                sji.setTotalQueuesCount(rs.getLong(HM_COLUMN.totalQueuesCount.rank()));
                sji.setActiveQueuesCount(rs.getLong(HM_COLUMN.activeQueuesCount.rank()));
                sji.setRetiredQueuesCount(rs.getLong(HM_COLUMN.retiredQueuesCount.rank()));
                sji.setExhaustedQueuesCount(rs.getLong(HM_COLUMN.exhaustedQueuesCount.rank()));
                sji.setAlertsCount(rs.getLong(HM_COLUMN.alertsCount.rank()));
                sji.setDownloadedFilesCount(rs.getLong(HM_COLUMN.downloadedFilesCount.rank()));
                sji.setCurrentProcessedKBPerSec(rs.getLong(HM_COLUMN.currentProcessedKBPerSec.rank()));
                sji.setProcessedKBPerSec(rs.getLong(HM_COLUMN.processedKBPerSec.rank()));
                sji.setCurrentProcessedDocsPerSec(rs.getDouble(HM_COLUMN.currentProcessedDocsPerSec.rank()));
                sji.setProcessedDocsPerSec(rs.getDouble(HM_COLUMN.processedDocsPerSec.rank()));
                sji.setActiveToeCount(rs.getInt(HM_COLUMN.activeToeCount.rank()));
                sji.setStatus(CrawlStatus.values()[rs.getInt(HM_COLUMN.status.rank())]);
                sji.setTimestamp(new Date(rs.getTimestamp(HM_COLUMN.tstamp.rank()).getTime()));

                infosForHarvest.add(sji);
            }

            return infoMap;

        } catch (SQLException e) {
            String message = "SQL error querying runningJobsMonitor" + "\n" + ExceptionUtils.getSQLExceptionCause(e);
            log.warn(message, e);
            throw new IOFailure(message, e);
        } finally {
            DBUtils.closeStatementIfOpen(stm);
            HarvestDBConnection.release(c);
        }

    }

    /**
     * Returns the ids of jobs for which history records exist as an immutable set.
     *
     * @return the ids of jobs for which history records exist.
     */
    @Override
    public Set<Long> getHistoryRecordIds() {
        Connection c = HarvestDBConnection.get();
        Set<Long> jobIds = new TreeSet<Long>();
        Statement stm = null;
        try {
            stm = c.createStatement();
            ResultSet rs = stm.executeQuery("SELECT DISTINCT " + HM_COLUMN.jobId + " FROM runningJobsMonitor");

            while (rs.next()) {
                jobIds.add(rs.getLong(HM_COLUMN.jobId.name()));
            }
            stm.close();

            stm = c.createStatement();
            rs = stm.executeQuery("SELECT DISTINCT " + HM_COLUMN.jobId + " FROM runningJobsHistory");

            while (rs.next()) {
                jobIds.add(rs.getLong(HM_COLUMN.jobId.name()));
            }
            stm.close();

            stm = c.createStatement();
            rs = stm.executeQuery("SELECT DISTINCT " + HM_COLUMN.jobId + " FROM frontierReportMonitor");

            while (rs.next()) {
                jobIds.add(rs.getLong(HM_COLUMN.jobId.name()));
            }

            return Collections.unmodifiableSet(jobIds);
        } catch (SQLException e) {
            String message = "SQL error querying running jobs history" + "\n" + ExceptionUtils.getSQLExceptionCause(e);
            log.warn(message, e);
            throw new IOFailure(message, e);
        } finally {
            DBUtils.closeStatementIfOpen(stm);
            HarvestDBConnection.release(c);
        }
    }

    /**
     * Returns an array of chronologically sorted progress records for the given job ID, starting at a given crawl time,
     * and limited to a given number of record.
     *
     * @param jobId the job id.
     * @param startTime the crawl time (in seconds) to begin.
     * @param limit the maximum number of records to fetch.
     * @return an array of chronologically sorted progress records for the given job ID, starting at a given crawl time,
     * and limited to a given number of record.
     */
    @Override
    public StartedJobInfo[] getMostRecentByJobId(long jobId, long startTime, int limit) {

        ArgumentNotValid.checkNotNull(jobId, "jobId");
        ArgumentNotValid.checkNotNull(startTime, "startTime");
        ArgumentNotValid.checkNotNull(limit, "limit");

        Connection c = HarvestDBConnection.get();
        PreparedStatement stm = null;
        try {
            stm = c.prepareStatement("SELECT " + HM_COLUMN.getColumnsInOrder() + " FROM runningJobsHistory"
                    + " WHERE jobId=? AND elapsedSeconds >= ?" + " ORDER BY elapsedSeconds DESC" + " "
                    + DBSpecifics.getInstance().getOrderByLimitAndOffsetSubClause(limit, 0));
            stm.setLong(1, jobId);
            stm.setLong(2, startTime);

            ResultSet rs = stm.executeQuery();
            List<StartedJobInfo> infosForJob = listFromResultSet(rs);

            return (StartedJobInfo[]) infosForJob.toArray(new StartedJobInfo[infosForJob.size()]);

        } catch (SQLException e) {
            String message = "SQL error querying runningJobsHistory for job ID " + jobId + " from database" + "\n"
                    + ExceptionUtils.getSQLExceptionCause(e);
            log.warn(message, e);
            throw new IOFailure(message, e);
        } finally {
            DBUtils.closeStatementIfOpen(stm);
            HarvestDBConnection.release(c);
        }
    }

    /**
     * Returns the most recent progress record for the given job ID.
     *
     * @param jobId the job id.
     * @return the most recent progress record for the given job ID.
     */
    @Override
    public StartedJobInfo getMostRecentByJobId(long jobId) {
        Connection c = HarvestDBConnection.get();
        Statement stm = null;
        try {
            stm = c.createStatement();
            ResultSet rs = stm.executeQuery("SELECT " + HM_COLUMN.getColumnsInOrder() + " FROM runningJobsMonitor"
                    + " WHERE jobId=" + jobId);

            if (rs.next()) {
                String harvestName = rs.getString(HM_COLUMN.harvestName.rank());
                StartedJobInfo sji = new StartedJobInfo(harvestName, jobId);

                sji.setElapsedSeconds(rs.getLong(HM_COLUMN.elapsedSeconds.rank()));
                sji.setHostUrl(rs.getString(HM_COLUMN.hostUrl.rank()));
                sji.setProgress(rs.getDouble(HM_COLUMN.progress.rank()));
                sji.setQueuedFilesCount(rs.getLong(HM_COLUMN.queuedFilesCount.rank()));
                sji.setTotalQueuesCount(rs.getLong(HM_COLUMN.totalQueuesCount.rank()));
                sji.setActiveQueuesCount(rs.getLong(HM_COLUMN.activeQueuesCount.rank()));
                sji.setRetiredQueuesCount(rs.getLong(HM_COLUMN.retiredQueuesCount.rank()));
                sji.setExhaustedQueuesCount(rs.getLong(HM_COLUMN.exhaustedQueuesCount.rank()));
                sji.setAlertsCount(rs.getLong(HM_COLUMN.alertsCount.rank()));
                sji.setDownloadedFilesCount(rs.getLong(HM_COLUMN.downloadedFilesCount.rank()));
                sji.setCurrentProcessedKBPerSec(rs.getLong(HM_COLUMN.currentProcessedKBPerSec.rank()));
                sji.setProcessedKBPerSec(rs.getLong(HM_COLUMN.processedKBPerSec.rank()));
                sji.setCurrentProcessedDocsPerSec(rs.getDouble(HM_COLUMN.currentProcessedDocsPerSec.rank()));
                sji.setProcessedDocsPerSec(rs.getDouble(HM_COLUMN.processedDocsPerSec.rank()));
                sji.setActiveToeCount(rs.getInt(HM_COLUMN.activeToeCount.rank()));
                sji.setStatus(CrawlStatus.values()[rs.getInt(HM_COLUMN.status.rank())]);
                sji.setTimestamp(new Date(rs.getTimestamp(HM_COLUMN.tstamp.rank()).getTime()));
                log.debug("getMostRecentByJobId for {}:{}", jobId, sji);
                return sji;
            }

        } catch (SQLException e) {
            String message = "SQL error querying runningJobsMonitor" + "\n" + ExceptionUtils.getSQLExceptionCause(e);
            log.warn(message, e);
            throw new IOFailure(message, e);
        } finally {
            DBUtils.closeStatementIfOpen(stm);
            HarvestDBConnection.release(c);
        }

        throw new UnknownID("No running job with ID " + jobId);
    }

    /**
     * Removes all records pertaining to the given job ID from the persistent storage.
     *
     * @param jobId the job id.
     * @return the number of deleted records.
     */
    @Override
    public int removeInfoForJob(long jobId) {
        ArgumentNotValid.checkNotNull(jobId, "jobId");

        Connection c = HarvestDBConnection.get();
        PreparedStatement stm = null;

        int deleteCount = 0;
        try {
            // Delete from monitor table
            c.setAutoCommit(false);
            stm = c.prepareStatement("DELETE FROM runningJobsMonitor WHERE jobId=?");
            stm.setLong(1, jobId);
            deleteCount = stm.executeUpdate();
            c.commit();
            stm.close();
            // Delete from history table
            c.setAutoCommit(false);
            stm = c.prepareStatement("DELETE FROM runningJobsHistory WHERE jobId=?");
            stm.setLong(1, jobId);
            deleteCount += stm.executeUpdate();
            c.commit();
        } catch (SQLException e) {
            String message = "SQL error deleting from history records for job ID " + jobId + "\n"
                    + ExceptionUtils.getSQLExceptionCause(e);
            log.warn(message, e);
            throw new IOFailure(message, e);
        } finally {
            DBUtils.closeStatementIfOpen(stm);
            DBUtils.rollbackIfNeeded(c, "removeInfoForJob", jobId);
            HarvestDBConnection.release(c);
        }

        return deleteCount;

    }

    /**
     * Enum class containing all fields in the frontierReportMonitor table.
     */
    private static enum FR_COLUMN {
        jobId, filterId, tstamp, domainName, currentSize, totalEnqueues, sessionBalance, lastCost, averageCost, // See
        // NAS-2168
        // Often
        // contains
        // the
        // illegal
        // value
        // 4.9E-324
        lastDequeueTime, wakeTime, totalSpend, totalBudget, errorCount, lastPeekUri, lastQueuedUri;

        /**
         * @return the rank of a member of the enum class.
         */
        int rank() {
            return ordinal() + 1;
        }

        /**
         * Returns the SQL substring that lists columns according to their ordinal.
         *
         * @return the SQL substring that lists columns in proper order.
         */
        static String getColumnsInOrder() {
            String columns = "";
            for (FR_COLUMN c : values()) {
                columns += c.name() + ", ";
            }
            return columns.substring(0, columns.lastIndexOf(","));
        }
    }

    ;

    /**
     * Store frontier report data to the persistent storage.
     *
     * @param report the report to store
     * @param filterId the id of the filter that produced the report
     * @param jobId The ID of the job responsible for this report
     * @return the update count
     */
    public int storeFrontierReport(String filterId, InMemoryFrontierReport report, Long jobId) {
        ArgumentNotValid.checkNotNull(report, "report");
        ArgumentNotValid.checkNotNull(jobId, "jobId");

        Connection c = HarvestDBConnection.get();
        PreparedStatement stm = null;
        try {

            // First drop existing rows
            try {
                c.setAutoCommit(false);

                stm = c.prepareStatement("DELETE FROM frontierReportMonitor WHERE jobId=? AND filterId=?");
                stm.setLong(1, jobId);
                stm.setString(2, filterId);

                stm.executeUpdate();

                c.commit();
            } catch (SQLException e) {
                String message = "SQL error dropping records for job ID " + jobId + " and filterId " + filterId + "\n"
                        + ExceptionUtils.getSQLExceptionCause(e);
                log.warn(message, e);
                return 0;
            } finally {
                DBUtils.closeStatementIfOpen(stm);
                DBUtils.rollbackIfNeeded(c, "storeFrontierReport delete", jobId);
            }

            // Now batch insert report lines
            try {
                c.setAutoCommit(false);

                stm = c.prepareStatement("INSERT INTO frontierReportMonitor(" + FR_COLUMN.getColumnsInOrder()
                        + ") VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");

                for (FrontierReportLine frl : report.getLines()) {
                    stm.setLong(FR_COLUMN.jobId.rank(), jobId);
                    stm.setString(FR_COLUMN.filterId.rank(), filterId);
                    stm.setTimestamp(FR_COLUMN.tstamp.rank(), new Timestamp(report.getTimestamp()));
                    stm.setString(FR_COLUMN.domainName.rank(), frl.getDomainName());
                    stm.setLong(FR_COLUMN.currentSize.rank(), frl.getCurrentSize());
                    stm.setLong(FR_COLUMN.totalEnqueues.rank(), frl.getTotalEnqueues());
                    stm.setLong(FR_COLUMN.sessionBalance.rank(), frl.getSessionBalance());
                    stm.setDouble(FR_COLUMN.lastCost.rank(), frl.getLastCost());
                    stm.setDouble(FR_COLUMN.averageCost.rank(),
                            correctNumericIfIllegalAverageCost(frl.getAverageCost()));
                    stm.setString(FR_COLUMN.lastDequeueTime.rank(), frl.getLastDequeueTime());
                    stm.setString(FR_COLUMN.wakeTime.rank(), frl.getWakeTime());
                    stm.setLong(FR_COLUMN.totalSpend.rank(), frl.getTotalSpend());
                    stm.setLong(FR_COLUMN.totalBudget.rank(), frl.getTotalBudget());
                    stm.setLong(FR_COLUMN.errorCount.rank(), frl.getErrorCount());

                    // URIs are to be truncated to 1000 characters
                    // (see SQL scripts)
                    DBUtils.setStringMaxLength(stm, FR_COLUMN.lastPeekUri.rank(), frl.getLastPeekUri(), MAX_URL_LENGTH,
                            frl, "lastPeekUri");
                    DBUtils.setStringMaxLength(stm, FR_COLUMN.lastQueuedUri.rank(), frl.getLastQueuedUri(),
                            MAX_URL_LENGTH, frl, "lastQueuedUri");

                    stm.addBatch();
                }

                int[] updCounts = stm.executeBatch();
                int updCountTotal = 0;
                for (int count : updCounts) {
                    updCountTotal += count;
                }

                c.commit();

                return updCountTotal;
            } catch (SQLException e) {
                String message = "SQL error writing records for job ID " + jobId + " and filterId " + filterId + "\n"
                        + ExceptionUtils.getSQLExceptionCause(e);
                log.warn(message, e);
                return 0;
            } finally {
                DBUtils.closeStatementIfOpen(stm);
                DBUtils.rollbackIfNeeded(c, "storeFrontierReport insert", jobId);
            }

        } finally {
            HarvestDBConnection.release(c);
        }
    }

    /**
     * Correct the given double if it is equal to 4.9E-324. Part of fix for NAS-2168
     *
     * @param value A given double
     * @return 0.0 if value is 4.9E-324, otherwise the value as is
     */
    private double correctNumericIfIllegalAverageCost(double value) {
        if (value == 4.9E-324) {
            log.warn("Found illegal double value '" + 4.9E-324 + "'. Changed it to 0.0");
            return 0.0;
        } else {
            return value;
        }
    }

    /**
     * Returns the list of the available frontier report types.
     *
     * @return the list of the available frontier report types.
     * @see FrontierReportFilter#getFilterId()
     */
    public String[] getFrontierReportFilterTypes() {
        List<String> filterIds = new ArrayList<String>();

        Connection c = HarvestDBConnection.get();
        PreparedStatement stm = null;
        try {
            stm = c.prepareStatement("SELECT DISTINCT filterId FROM frontierReportMonitor");

            ResultSet rs = stm.executeQuery();
            while (rs.next()) {
                filterIds.add(rs.getString(1));
            }

        } catch (SQLException e) {
            String message = "SQL error fetching filter IDs" + "\n" + ExceptionUtils.getSQLExceptionCause(e);
            log.warn(message, e);
        } finally {
            DBUtils.closeStatementIfOpen(stm);
            HarvestDBConnection.release(c);
        }

        return filterIds.toArray(new String[filterIds.size()]);
    }

    /**
     * Retrieve a frontier report from a job id and a given filter class.
     *
     * @param jobId the job id
     * @param filterId the id of the filter that produced the report
     * @return a frontier report
     */
    public InMemoryFrontierReport getFrontierReport(long jobId, String filterId) {

        ArgumentNotValid.checkNotNull(jobId, "jobId");
        ArgumentNotValid.checkNotNull(filterId, "filterId");

        InMemoryFrontierReport report = new InMemoryFrontierReport(Long.toString(jobId));

        Connection c = HarvestDBConnection.get();
        PreparedStatement stm = null;
        try {
            stm = c.prepareStatement("SELECT " + FR_COLUMN.getColumnsInOrder() + " FROM frontierReportMonitor"
                    + " WHERE jobId=? AND filterId=?");
            stm.setLong(1, jobId);
            stm.setString(2, filterId);

            ResultSet rs = stm.executeQuery();

            // Process first line to get report timestamp
            if (rs.next()) {
                report.setTimestamp(rs.getTimestamp(FR_COLUMN.tstamp.rank()).getTime());
                report.addLine(getLine(rs));

                while (rs.next()) {
                    report.addLine(getLine(rs));
                }
            }

        } catch (SQLException e) {
            String message = "SQL error fetching report for job ID " + jobId + " and filterId " + filterId + "\n"
                    + ExceptionUtils.getSQLExceptionCause(e);
            log.warn(message, e);
        } finally {
            DBUtils.closeStatementIfOpen(stm);
            HarvestDBConnection.release(c);
        }

        return report;
    }
    
    /**
     * Retrieve a frontier report from a job id, with limited results and possibility to sort by totalenqueues DESC
     *
     * @param jobId the job id
     * @param limit the limit of result to query
     * @param filterId the id of the filter that produced the report
     * @param sort if true, sort the results by totalenqueues DESC
     * @return a frontier report
     */
    public InMemoryFrontierReport getFrontierReport(long jobId, String filterId, int limit, boolean sort) {

        ArgumentNotValid.checkNotNull(jobId, "jobId");
        ArgumentNotValid.checkNotNull(limit, "limit");
        ArgumentNotValid.checkNotNull(sort, "sort");
        ArgumentNotValid.checkNotNull(filterId, "filterId");

        InMemoryFrontierReport report = new InMemoryFrontierReport(Long.toString(jobId));

        Connection c = HarvestDBConnection.get();
        PreparedStatement stm = null;
        try {
        	String sqlString = "SELECT " + FR_COLUMN.getColumnsInOrder() + " FROM frontierReportMonitor"
                    + " WHERE jobId=?  AND filterId=? ";
        	if(sort) {
        		sqlString = sqlString + " ORDER BY totalenqueues DESC ";
        	}
        	if(limit > 0) {
        		sqlString = sqlString + " LIMIT ? ";
        	}
            stm = c.prepareStatement(sqlString);
            stm.setLong(1, jobId);
            stm.setString(2, filterId);
            if(limit > 0) {
            	stm.setInt(3, limit);
            }

            ResultSet rs = stm.executeQuery();

            // Process first line to get report timestamp
            if (rs.next()) {
                report.setTimestamp(rs.getTimestamp(FR_COLUMN.tstamp.rank()).getTime());
                report.addLine(getLine(rs));

                while (rs.next()) {
                    report.addLine(getLine(rs));
                }
            }

        } catch (SQLException e) {
            String message = "SQL error fetching report for job ID " + jobId + " and limit " + limit + "\n"
                    + ExceptionUtils.getSQLExceptionCause(e);
            log.warn(message, e);
        } finally {
            DBUtils.closeStatementIfOpen(stm);
            HarvestDBConnection.release(c);
        }

        return report;
    }
    
    /**
     * Retrieve a frontier report from a job id, with limited results and possibility to sort by totalenqueues DESC
     *
     * @param jobId the job id
     * @param limit the limit of result to query
     * @param sort if true, sort the results by totalenqueues DESC
     * @return a frontier report
     */
    public InMemoryFrontierReport getFrontierReport(long jobId, int limit, boolean sort) {
        ArgumentNotValid.checkNotNull(jobId, "jobId");
        ArgumentNotValid.checkNotNull(limit, "limit");
        ArgumentNotValid.checkNotNull(sort, "sort");

        InMemoryFrontierReport report = new InMemoryFrontierReport(Long.toString(jobId));

        Connection c = HarvestDBConnection.get();
        PreparedStatement stm = null;
        try {
        	String sqlString = "SELECT " + FR_COLUMN.getColumnsInOrder() + " FROM frontierReportMonitor"
                    + " WHERE jobId=? ";
        	if(sort) {
        		sqlString = sqlString + " ORDER BY totalenqueues DESC ";
        	}
        	if(limit > 0) {
        		sqlString = sqlString + " LIMIT ? ";
        	}
            stm = c.prepareStatement(sqlString);
            stm.setLong(1, jobId);
            stm.setInt(2, limit);

            ResultSet rs = stm.executeQuery();

            // Process first line to get report timestamp
            if (rs.next()) {
                report.setTimestamp(rs.getTimestamp(FR_COLUMN.tstamp.rank()).getTime());
                report.addLine(getLine(rs));

                while (rs.next()) {
                    report.addLine(getLine(rs));
                }
            }

        } catch (SQLException e) {
            String message = "SQL error fetching report for job ID " + jobId + " and limit " + limit + "\n"
                    + ExceptionUtils.getSQLExceptionCause(e);
            log.warn(message, e);
        } finally {
            DBUtils.closeStatementIfOpen(stm);
            HarvestDBConnection.release(c);
        }

        return report;
    }

    /**
     * Deletes all frontier report data pertaining to the given job id from the persistent storage.
     *
     * @param jobId the job id
     * @return the update count
     */
    public int deleteFrontierReports(long jobId) {
        ArgumentNotValid.checkNotNull(jobId, "jobId");

        Connection c = HarvestDBConnection.get();
        PreparedStatement stm = null;
        try {
            c.setAutoCommit(false);

            stm = c.prepareStatement("DELETE FROM frontierReportMonitor WHERE jobId=?");
            stm.setLong(1, jobId);

            int delCount = stm.executeUpdate();

            c.commit();

            return delCount;
        } catch (SQLException e) {
            String message = "SQL error deleting report lines for job ID " + jobId + "\n"
                    + ExceptionUtils.getSQLExceptionCause(e);
            log.warn(message, e);
            return 0;
        } finally {
            DBUtils.closeStatementIfOpen(stm);
            DBUtils.rollbackIfNeeded(c, "deleteFrontierReports", jobId);
            HarvestDBConnection.release(c);
        }
    }

    /**
     * Get a frontierReportLine from the resultSet.
     *
     * @param rs the resultset with data from table frontierReportMonitor
     * @return a frontierReportLine from the resultSet.
     * @throws SQLException If unable to get data from resultSet
     */
    private FrontierReportLine getLine(ResultSet rs) throws SQLException {
        FrontierReportLine line = new FrontierReportLine();

        line.setAverageCost(rs.getDouble(FR_COLUMN.averageCost.rank()));
        line.setCurrentSize(rs.getLong(FR_COLUMN.currentSize.rank()));
        line.setDomainName(rs.getString(FR_COLUMN.domainName.rank()));
        line.setErrorCount(rs.getLong(FR_COLUMN.errorCount.rank()));
        line.setLastCost(rs.getDouble(FR_COLUMN.lastCost.rank()));
        line.setLastDequeueTime(rs.getString(FR_COLUMN.lastDequeueTime.rank()));
        line.setLastPeekUri(rs.getString(FR_COLUMN.lastPeekUri.rank()));
        line.setLastQueuedUri(rs.getString(FR_COLUMN.lastQueuedUri.rank()));
        line.setSessionBalance(rs.getLong(FR_COLUMN.sessionBalance.rank()));
        line.setTotalBudget(rs.getLong(FR_COLUMN.totalBudget.rank()));
        line.setTotalEnqueues(rs.getLong(FR_COLUMN.totalEnqueues.rank()));
        line.setTotalSpend(rs.getLong(FR_COLUMN.totalSpend.rank()));
        line.setWakeTime(rs.getString(FR_COLUMN.wakeTime.rank()));

        return line;
    }

    /**
     * Get a list of StartedJobInfo objects from a resultset of entries from runningJobsHistory table.
     *
     * @param rs a resultset with entries from table runningJobsHistory.
     * @return a list of StartedJobInfo objects from the resultset
     * @throws SQLException If any problems reading data from the resultset
     */
    private List<StartedJobInfo> listFromResultSet(ResultSet rs) throws SQLException {
        List<StartedJobInfo> list = new LinkedList<StartedJobInfo>();
        while (rs.next()) {
            StartedJobInfo sji = new StartedJobInfo(rs.getString(HM_COLUMN.harvestName.rank()),
                    rs.getLong(HM_COLUMN.jobId.rank()));
            sji.setElapsedSeconds(rs.getLong(HM_COLUMN.elapsedSeconds.rank()));
            sji.setHostUrl(rs.getString(HM_COLUMN.hostUrl.rank()));
            sji.setProgress(rs.getDouble(HM_COLUMN.progress.rank()));
            sji.setQueuedFilesCount(rs.getLong(HM_COLUMN.queuedFilesCount.rank()));
            sji.setTotalQueuesCount(rs.getLong(HM_COLUMN.totalQueuesCount.rank()));
            sji.setActiveQueuesCount(rs.getLong(HM_COLUMN.activeQueuesCount.rank()));
            sji.setExhaustedQueuesCount(rs.getLong(HM_COLUMN.exhaustedQueuesCount.rank()));
            sji.setAlertsCount(rs.getLong(HM_COLUMN.alertsCount.rank()));
            sji.setDownloadedFilesCount(rs.getLong(HM_COLUMN.downloadedFilesCount.rank()));
            sji.setCurrentProcessedKBPerSec(rs.getLong(HM_COLUMN.currentProcessedKBPerSec.rank()));
            sji.setProcessedKBPerSec(rs.getLong(HM_COLUMN.processedKBPerSec.rank()));
            sji.setCurrentProcessedDocsPerSec(rs.getDouble(HM_COLUMN.currentProcessedDocsPerSec.rank()));
            sji.setProcessedDocsPerSec(rs.getDouble(HM_COLUMN.processedDocsPerSec.rank()));
            sji.setActiveToeCount(rs.getInt(HM_COLUMN.activeToeCount.rank()));
            sji.setStatus(CrawlStatus.values()[rs.getInt(HM_COLUMN.status.rank())]);
            sji.setTimestamp(new Date(rs.getTimestamp(HM_COLUMN.tstamp.rank()).getTime()));

            list.add(sji);
        }
        return list;
    }

}
