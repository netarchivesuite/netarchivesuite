/* File:        $Id: RunningJobsInfoDBDAO.java 2783 2013-08-19 09:50:16Z svc $
 * Revision:    $Revision: 2783 $
 * Date:        $Date: 2013-08-19 11:50:16 +0200 (Mon, 19 Aug 2013) $
 * Author:      $Author: svc $
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2012 The Royal Danish Library, the Danish State and
 * University Library, the National Library of France and the Austrian
 * National Library.
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
package dk.netarkivet.harvester.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.UnknownID;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.harvester.HarvesterSettings;
import dk.netarkivet.harvester.dao.spec.DBSpecifics;
import dk.netarkivet.harvester.harvesting.distribute.CrawlProgressMessage.CrawlStatus;
import dk.netarkivet.harvester.harvesting.frontier.FrontierReportFilter;
import dk.netarkivet.harvester.harvesting.frontier.FrontierReportLine;
import dk.netarkivet.harvester.harvesting.frontier.InMemoryFrontierReport;
import dk.netarkivet.harvester.harvesting.monitor.StartedJobInfo;

/**
 * Class implementing the persistence of running job infos.
 *
 */
public class RunningJobsInfoDBDAO extends RunningJobsInfoDAO 
implements ResultSetExtractor<List<StartedJobInfo>> {

	/** Max length of urls stored in tables. */
	private static final int MAX_URL_LENGTH = 1000;

	/**
	 * Defines the order of columns in the runningJobsMonitor table.
	 * Used in SQL queries.
	 */
	private static enum HM_COLUMN {
		jobId,
		harvestName,
		elapsedSeconds,
		hostUrl,
		progress,
		queuedFilesCount,
		totalQueuesCount,
		activeQueuesCount,
		retiredQueuesCount,
		exhaustedQueuesCount,
		alertsCount,
		downloadedFilesCount,
		currentProcessedKBPerSec,
		processedKBPerSec,
		currentProcessedDocsPerSec,
		processedDocsPerSec,
		activeToeCount,
		status,
		tstamp;

		/**
		 * Returns the SQL substring that lists columns according
		 * to their ordinal.
		 * @return the SQL substring that lists columns in proper order.
		 */
		static String getColumnsInOrder(boolean param) {
			StringBuffer columns = new StringBuffer();
			for (HM_COLUMN c : values()) {
				columns.append((param ? ":" : "") + c.name() + ", ");
			}
			return columns.substring(0, columns.lastIndexOf(","));
		}
	}

	/** The logger. */
	private final Log log = LogFactory.getLog(getClass());

	/**
	 * Date of last history record per job.
	 */
	private static Map<Long, Long> lastSampleDateByJobId =
			new HashMap<Long, Long>();

	/**
	 * Rate in milliseconds at which history records should be sampled
	 * for a running job.
	 */
	private static final long HISTORY_SAMPLE_RATE =
			1000 * Settings.getLong(
					HarvesterSettings.HARVEST_MONITOR_HISTORY_SAMPLE_RATE);

	@Override
	public List<StartedJobInfo> extractData(ResultSet rs)
			throws SQLException, DataAccessException {
		return listFromResultSet(rs);
	}

	/**
	 * Stores a {@link StartedJobInfo} record to the persistent storage.
	 * The record is stored in the monitor table, and if the elapsed time since
	 * the last history sample is equal or superior to the history sample rate,
	 * also to the history table.
	 * @param startedJobInfo the record to store.
	 */
	@Override
	public void store(StartedJobInfo startedJobInfo) {
		ArgumentNotValid.checkNotNull(
				startedJobInfo, "StartedJobInfo startedJobInfo");
		executeTransaction("doStore", StartedJobInfo.class, startedJobInfo);
	}
	
	@SuppressWarnings("unused")
	private synchronized void doStore(StartedJobInfo startedJobInfo) {

		// First is there a record in the monitor table?
		boolean update = query("SELECT jobId FROM runningJobsMonitor"
				+ " WHERE jobId=:jobId AND harvestName=:harvestName",
				new ParameterMap(
						"jobId", startedJobInfo.getJobId(),
						"harvestName", startedJobInfo.getHarvestName()),
						new ResultSetExtractor<Boolean>() {
					@Override
					public Boolean extractData(ResultSet rs)
							throws SQLException, DataAccessException {
						return rs.next();
					}

				});

		StringBuffer sql = new StringBuffer();

		if (update) {
			sql.append("UPDATE runningJobsMonitor SET ");

			StringBuffer columns = new StringBuffer();
			for (HM_COLUMN setCol : HM_COLUMN.values()) {
				String name = setCol.name();
				columns.append(name + "=:" + name + ", ");
			}
			sql.append(columns.substring(0, columns.lastIndexOf(",")));
			sql.append(" WHERE jobId=:jobId AND harvestName=:harvestName");
		} else {
			sql.append("INSERT INTO runningJobsMonitor (");
			sql.append(HM_COLUMN.getColumnsInOrder(false));
			sql.append(") VALUES (");
			sql.append(HM_COLUMN.getColumnsInOrder(true));
			sql.append(")");
		}

		ParameterMap params = new ParameterMap(
				HM_COLUMN.jobId.name(), startedJobInfo.getJobId(),
				HM_COLUMN.harvestName.name(), startedJobInfo.getHarvestName(),
				HM_COLUMN.elapsedSeconds.name(), startedJobInfo.getElapsedSeconds(),
				HM_COLUMN.hostUrl.name(), startedJobInfo.getHostUrl(),
				HM_COLUMN.progress.name(), startedJobInfo.getProgress(),
				HM_COLUMN.queuedFilesCount.name(), startedJobInfo.getQueuedFilesCount(),
				HM_COLUMN.totalQueuesCount.name(), startedJobInfo.getTotalQueuesCount(),
				HM_COLUMN.activeQueuesCount.name(), startedJobInfo.getActiveQueuesCount(),
				HM_COLUMN.retiredQueuesCount.name(), startedJobInfo.getRetiredQueuesCount(),
				HM_COLUMN.exhaustedQueuesCount.name(), startedJobInfo.getExhaustedQueuesCount(),
				HM_COLUMN.alertsCount.name(), startedJobInfo.getAlertsCount(),
				HM_COLUMN.downloadedFilesCount.name(), startedJobInfo.getDownloadedFilesCount(),
				HM_COLUMN.currentProcessedKBPerSec.name(), 
				startedJobInfo.getCurrentProcessedKBPerSec(),
				HM_COLUMN.processedKBPerSec.name(), startedJobInfo.getProcessedKBPerSec(),
				HM_COLUMN.currentProcessedDocsPerSec.name(), 
				startedJobInfo.getCurrentProcessedDocsPerSec(),
				HM_COLUMN.processedDocsPerSec.name(), startedJobInfo.getProcessedDocsPerSec(),
				HM_COLUMN.activeToeCount.name(), startedJobInfo.getActiveToeCount(),
				HM_COLUMN.status.name(), startedJobInfo.getStatus().ordinal(),
				HM_COLUMN.tstamp.name(), startedJobInfo.getTimestamp());

		executeUpdate(sql.toString(), params);

		// Should we store an history record?
		Long lastHistoryStore =
				lastSampleDateByJobId.get(startedJobInfo.getJobId());

		long time  = System.currentTimeMillis();
		boolean shouldSample = 
				lastHistoryStore == null || time >= lastHistoryStore + HISTORY_SAMPLE_RATE;

				if (!shouldSample) {
					return;  // we're done
				}

				executeUpdate(
						"INSERT INTO runningJobsHistory ("
								+ HM_COLUMN.getColumnsInOrder(false)
								+ ") VALUES (" + HM_COLUMN.getColumnsInOrder(true) + ")",
								params);
	}

	/**
	 * Returns an array of all progress records chronologically sorted for the
	 * given job ID.
	 * @param jobId the job id.
	 * @return an array of all progress records chronologically sorted for the
	 * given job ID.
	 */
	@Override
	public StartedJobInfo[] getFullJobHistory(long jobId) {
		List<StartedJobInfo> infosForJob = query(
				"SELECT " + HM_COLUMN.getColumnsInOrder(false)
				+ " FROM runningJobsHistory"
				+ " WHERE jobId=:jobId"
				+ " ORDER BY elapsedSeconds ASC",
				new ParameterMap("jobId", jobId),
				this);
		return (StartedJobInfo[]) infosForJob.toArray(
				new StartedJobInfo[infosForJob.size()]);
	}

	/**
	 * Returns the most recent record for every job, partitioned by harvest
	 * definition name.
	 * @return the full listing of started job information, partitioned by
	 *         harvest definition name.
	 */
	@Override
	public Map<String, List<StartedJobInfo>> getMostRecentByHarvestName() {

		return query(
				"SELECT " + HM_COLUMN.getColumnsInOrder(false) + " FROM runningJobsMonitor",
				new ResultSetExtractor<Map<String, List<StartedJobInfo>>>() {
					@Override
					public Map<String, List<StartedJobInfo>> extractData(ResultSet rs) 
							throws SQLException, DataAccessException {
						Map<String, List<StartedJobInfo>> infoMap = 
								new TreeMap<String, List<StartedJobInfo>>();

						while (rs.next()) {
							long jobId = rs.getLong(HM_COLUMN.jobId.name());
							String harvestName = rs.getString(HM_COLUMN.harvestName.name());

							List<StartedJobInfo> infosForHarvest = infoMap.get(harvestName);
							if (infosForHarvest == null) {
								infosForHarvest = new LinkedList<StartedJobInfo>();
								infoMap.put(harvestName, infosForHarvest);
							}

							StartedJobInfo sji = new StartedJobInfo(harvestName, jobId);

							sji.setElapsedSeconds(
									rs.getLong(HM_COLUMN.elapsedSeconds.name()));
							sji.setHostUrl(rs.getString(HM_COLUMN.hostUrl.name()));
							sji.setProgress(rs.getDouble(HM_COLUMN.progress.name()));
							sji.setQueuedFilesCount(
									rs.getLong(HM_COLUMN.queuedFilesCount.name()));
							sji.setTotalQueuesCount(
									rs.getLong(HM_COLUMN.totalQueuesCount.name()));
							sji.setActiveQueuesCount(
									rs.getLong(HM_COLUMN.activeQueuesCount.name()));
							sji.setRetiredQueuesCount(
									rs.getLong(HM_COLUMN.retiredQueuesCount.name()));
							sji.setExhaustedQueuesCount(
									rs.getLong(HM_COLUMN.exhaustedQueuesCount.name()));
							sji.setAlertsCount(
									rs.getLong(HM_COLUMN.alertsCount.name()));
							sji.setDownloadedFilesCount(
									rs.getLong(HM_COLUMN.downloadedFilesCount.name()));
							sji.setCurrentProcessedKBPerSec(
									rs.getLong(
											HM_COLUMN.currentProcessedKBPerSec.name()));
							sji.setProcessedKBPerSec(
									rs.getLong(HM_COLUMN.processedKBPerSec.name()));
							sji.setCurrentProcessedDocsPerSec(
									rs.getDouble(
											HM_COLUMN.currentProcessedDocsPerSec.name()));
							sji.setProcessedDocsPerSec(
									rs.getDouble(HM_COLUMN.processedDocsPerSec.name()));
							sji.setActiveToeCount(
									rs.getInt(HM_COLUMN.activeToeCount.name()));
							sji.setStatus(
									CrawlStatus.values()[rs.getInt(HM_COLUMN.status.name())]);
							sji.setTimestamp(new Date(
									rs.getTimestamp(HM_COLUMN.tstamp.name()).getTime()));

							infosForHarvest.add(sji);
						}
						return infoMap;
					}

				});
	}

	/**
	 * Returns the ids of jobs for which history records exist as an
	 * immutable set.
	 * @return the ids of jobs for which history records exist.
	 */
	@Override
	public Set<Long> getHistoryRecordIds() {

		ResultSetExtractor<Set<Long>> extractor = new ResultSetExtractor<Set<Long>>() {
			@Override
			public Set<Long> extractData(ResultSet rs)
					throws SQLException, DataAccessException {
				Set<Long> jobIds = new TreeSet<Long>();
				while (rs.next()) {
					jobIds.add(rs.getLong(HM_COLUMN.jobId.name()));
				}
				return jobIds;
			}			
		}; 

		Set<Long> jobIds = query(
				"SELECT DISTINCT " + HM_COLUMN.jobId + " FROM runningJobsMonitor",
				extractor);

		jobIds.addAll(query(
				"SELECT DISTINCT " + HM_COLUMN.jobId + " FROM frontierReportMonitor",
				extractor));

		return Collections.unmodifiableSet(jobIds);
	}

	/**
	 * Returns an array of chronologically sorted progress records for the
	 * given job ID, starting at a given crawl time, and limited to a given
	 * number of record.
	 * @param jobId the job id.
	 * @param startTime the crawl time (in seconds) to begin.
	 * @param limit the maximum number of records to fetch.
	 * @return an array of chronologically sorted progress records for the
	 * given job ID, starting at a given crawl time, and limited to a given
	 * number of record.
	 */
	@Override
	public StartedJobInfo[] getMostRecentByJobId(
			long jobId, long startTime, int limit) {

		ArgumentNotValid.checkNotNull(jobId, "jobId");
		ArgumentNotValid.checkNotNull(startTime, "startTime");
		ArgumentNotValid.checkNotNull(limit, "limit");

		List<StartedJobInfo> infosForJob = query(
				"SELECT " + HM_COLUMN.getColumnsInOrder(false)
				+ " FROM runningJobsHistory"
				+ " WHERE jobId=? AND elapsedSeconds >= ?"
				+ " ORDER BY elapsedSeconds DESC"
				+ " " + DBSpecifics.getInstance().getOrderByLimitAndOffsetSubClause(limit, 0),
				this);

		return (StartedJobInfo[]) infosForJob.toArray(
				new StartedJobInfo[infosForJob.size()]);
	}

	/**
	 * Returns the most recent progress record for the given job ID.
	 * @param jobId the job id.
	 * @return the most recent progress record for the given job ID.
	 */
	@Override
	public StartedJobInfo getMostRecentByJobId(final long jobId) {

		return query("SELECT " + HM_COLUMN.getColumnsInOrder(false)
				+ " FROM runningJobsMonitor"
				+ " WHERE jobId=:jobId",
				new ParameterMap("jobId", jobId),
				new ResultSetExtractor<StartedJobInfo>() {
			@Override
			public StartedJobInfo extractData(ResultSet rs)
					throws SQLException, DataAccessException {
				if (!rs.next()) {
					throw new UnknownID("No running job with ID " + jobId);
				}
				String harvestName = rs.getString(HM_COLUMN.harvestName.name());
				StartedJobInfo sji = new StartedJobInfo(harvestName, jobId);

				sji.setElapsedSeconds(
						rs.getLong(HM_COLUMN.elapsedSeconds.name()));
				sji.setHostUrl(rs.getString(HM_COLUMN.hostUrl.name()));
				sji.setProgress(rs.getDouble(HM_COLUMN.progress.name()));
				sji.setQueuedFilesCount(
						rs.getLong(HM_COLUMN.queuedFilesCount.name()));
				sji.setTotalQueuesCount(
						rs.getLong(HM_COLUMN.totalQueuesCount.name()));
				sji.setActiveQueuesCount(
						rs.getLong(HM_COLUMN.activeQueuesCount.name()));
				sji.setRetiredQueuesCount(
						rs.getLong(HM_COLUMN.retiredQueuesCount.name()));
				sji.setExhaustedQueuesCount(
						rs.getLong(HM_COLUMN.exhaustedQueuesCount.name()));
				sji.setAlertsCount(
						rs.getLong(HM_COLUMN.alertsCount.name()));
				sji.setDownloadedFilesCount(
						rs.getLong(HM_COLUMN.downloadedFilesCount.name()));
				sji.setCurrentProcessedKBPerSec(
						rs.getLong(
								HM_COLUMN.currentProcessedKBPerSec.name()));
				sji.setProcessedKBPerSec(
						rs.getLong(HM_COLUMN.processedKBPerSec.name()));
				sji.setCurrentProcessedDocsPerSec(
						rs.getDouble(
								HM_COLUMN.currentProcessedDocsPerSec.name()));
				sji.setProcessedDocsPerSec(
						rs.getDouble(HM_COLUMN.processedDocsPerSec.name()));
				sji.setActiveToeCount(
						rs.getInt(HM_COLUMN.activeToeCount.name()));
				sji.setStatus(
						CrawlStatus.values()[
						                     rs.getInt(HM_COLUMN.status.name())]);
				sji.setTimestamp(new Date(
						rs.getTimestamp(HM_COLUMN.tstamp.name()).getTime()));

				return sji;
			}

		});
	}

	/**
	 * Removes all records pertaining to the given job ID from the persistent
	 * storage.
	 * @param jobId the job id.
	 * @return the number of deleted records.
	 */
	@Override
	public int removeInfoForJob(long jobId) {
		ArgumentNotValid.checkNotNull(jobId, "jobId");
		return (Integer) executeTransaction("doRemoveInfoForJob", Long.class, jobId);
	}
	
	@SuppressWarnings("unused")
	private synchronized int doRemoveInfoForJob(Long jobId) {
		int deleteCount = 0;
		
		ParameterMap pJobId = new ParameterMap("id", jobId);
		
		// Delete from monitor table
		deleteCount += executeUpdate("DELETE FROM runningJobsMonitor WHERE jobId=:id", pJobId);

		// Delete from history table
		deleteCount += executeUpdate("DELETE FROM runningJobsHistory WHERE jobId=:id", pJobId);

		return deleteCount;
	}

	/** Enum class containing all fields in the frontierReportMonitor
	 * table.
	 */
	private static enum FR_COLUMN {
		jobId,
		filterId,
		tstamp,
		domainName,
		currentSize,
		totalEnqueues,
		sessionBalance,
		lastCost,
		averageCost, // See NAS-2168 Often contains the illegal value 4.9E-324
		lastDequeueTime,
		wakeTime,
		totalSpend,
		totalBudget,
		errorCount,
		lastPeekUri,
		lastQueuedUri;

		/**
		 * Returns the SQL substring that lists columns according
		 * to their ordinal.
		 * @return the SQL substring that lists columns in proper order.
		 */
		static String getColumnsInOrder(boolean param) {
			String columns = "";
			for (FR_COLUMN c : values()) {
				columns += (param ? ":" : "") + c.name() + ", ";
			}
			return columns.substring(0, columns.lastIndexOf(","));
		}
	};

	/**
	 * Store frontier report data to the persistent storage.
	 * @param report the report to store
	 * @param filterId the id of the filter that produced the report
	 * @param jobId The ID of the job responsible for this report
	 * @return the update count
	 */
	public int storeFrontierReport(
			String filterId,
			InMemoryFrontierReport report,
			Long jobId) {
		ArgumentNotValid.checkNotNull(report, "report");
		ArgumentNotValid.checkNotNull(jobId, "jobId");
		
		return (Integer) executeTransaction(
				"doStoreFrontierReport", 
				String.class, filterId,
				InMemoryFrontierReport.class, report,
				Long.class, jobId);
	}
	
	@SuppressWarnings("unused")
	private synchronized int doStoreFrontierReport(
			String filterId,
			InMemoryFrontierReport report,
			Long jobId) {

		// First drop existing rows
		executeUpdate("DELETE FROM frontierReportMonitor"
				+ " WHERE jobId=:jobId AND filterId=:filterId",
				new ParameterMap(
						"jobId", jobId,
						"filterId", filterId));

		// Now batch insert report lines
		ArrayList<ParameterMap> pmaps = new ArrayList<ParameterMap>();
		for (FrontierReportLine frl : report.getLines()) {
			pmaps.add(new ParameterMap( 
					FR_COLUMN.jobId.name(), jobId,
					FR_COLUMN.filterId.name(), filterId,
					FR_COLUMN.tstamp.name(), new Date(report.getTimestamp()),
					FR_COLUMN.domainName.name(), frl.getDomainName(),
					FR_COLUMN.currentSize.name(), frl.getCurrentSize(),
					FR_COLUMN.totalEnqueues.name(), frl.getTotalEnqueues(),
					FR_COLUMN.sessionBalance.name(), frl.getSessionBalance(),
					FR_COLUMN.lastCost.name(), frl.getLastCost(),
					FR_COLUMN.averageCost.name(), 
						correctNumericIfIllegalAverageCost(frl.getAverageCost()),
					FR_COLUMN.lastDequeueTime.name(), frl.getLastDequeueTime(),
					FR_COLUMN.wakeTime.name(), frl.getWakeTime(),
					FR_COLUMN.totalSpend.name(), frl.getTotalSpend(),
					FR_COLUMN.totalBudget.name(), frl.getTotalBudget(),
					FR_COLUMN.errorCount.name(), frl.getErrorCount(),
					// URIs are to be truncated to 1000 characters
					// (see SQL scripts)
					FR_COLUMN.lastPeekUri.name(), getMaxLengthStringValue(
							frl, "lastPeekUri", frl.getLastPeekUri(), MAX_URL_LENGTH),
					FR_COLUMN.lastQueuedUri.name(), getMaxLengthStringValue(
							frl, "lastQueuedUri", frl.getLastQueuedUri(), MAX_URL_LENGTH)));
		}
		
		int[] updCounts = executeBatchUpdate("INSERT INTO frontierReportMonitor("
				+ FR_COLUMN.getColumnsInOrder(false)
				+ ") VALUES (" + FR_COLUMN.getColumnsInOrder(true) + ")",
				pmaps);
		int updCountTotal = 0;
		for (int count : updCounts) {
			updCountTotal  += count;
		}

		return updCountTotal;
	}

	/**
	 * Correct the given double if it is equal to 4.9E-324. 
	 * Part of fix for NAS-2168
	 * @param value A given double
	 * @return 0.0 if value is 4.9E-324, otherwise the value as is
	 */
	private double correctNumericIfIllegalAverageCost(double value) {
		if (value == 4.9E-324) {
			log.warn("Found illegal double value '" + 4.9E-324 
					+ "'. Changed it to 0.0");
			return 0.0;
		} else {
			return value;
		}
	}

	/**
	 * Returns the list of the available frontier report types.
	 * @see FrontierReportFilter#getFilterId()
	 * @return the list of the available frontier report types.
	 */
	public String[] getFrontierReportFilterTypes() {
		List<String> filterIds = query(
				"SELECT DISTINCT filterId FROM frontierReportMonitor",
				new ResultSetExtractor<List<String>>() {
					@Override
					public List<String> extractData(ResultSet rs)
							throws SQLException, DataAccessException {
						List<String> filterIds = new ArrayList<String>();
						while (rs.next()) {
							filterIds.add(rs.getString(1));
						}
						return filterIds;
					}
			
				});

		return filterIds.toArray(new String[filterIds.size()]);
	}

	/**
	 * Retrieve a frontier report from a job id and a given filter class.
	 * @param jobId the job id
	 * @param filterId the id of the filter that produced the report
	 * @return a frontier report
	 */
	public InMemoryFrontierReport getFrontierReport(
			final long jobId,
			final String filterId) {

		ArgumentNotValid.checkNotNull(jobId, "jobId");
		ArgumentNotValid.checkNotNull(filterId, "filterId");

		return query("SELECT " + FR_COLUMN.getColumnsInOrder(false) + " FROM frontierReportMonitor"
				+ " WHERE jobId=:jobId AND filterId=:filterId",
				new ParameterMap("jobId", jobId, "filterId", filterId),
				new ResultSetExtractor<InMemoryFrontierReport>() {
					@Override
					public InMemoryFrontierReport extractData(ResultSet rs)
							throws SQLException, DataAccessException {
						InMemoryFrontierReport report =
								new InMemoryFrontierReport(Long.toString(jobId));
						// Process first line to get report timestamp
						if (rs.next()) {
							report.setTimestamp(
									rs.getTimestamp(FR_COLUMN.tstamp.name()).getTime());
							report.addLine(getLine(rs));

							while (rs.next()) {
								report.addLine(getLine(rs));
							}
						}
						return report;
					}
			
				});
	}

	/**
	 * Deletes all frontier report data pertaining to the given job id from
	 * the persistent storage.
	 * @param jobId the job id
	 * @return the update count
	 */
	public int deleteFrontierReports(long jobId) {
		ArgumentNotValid.checkNotNull(jobId, "jobId");
		return (Integer) executeTransaction("doDeleteFrontierReports", Long.class, jobId);
	}
	
	@SuppressWarnings("unused")
	private synchronized int doDeleteFrontierReports(long jobId) {
		return executeUpdate(
				"DELETE FROM frontierReportMonitor WHERE jobId=:jobId",
				new ParameterMap("jobId", jobId));
	}

	/**
	 * Get a frontierReportLine from the resultSet.
	 * @param rs the resultset with data from table frontierReportMonitor
	 * @return a frontierReportLine from the resultSet.
	 * @throws SQLException If unable to get data from resultSet
	 */
	private FrontierReportLine getLine(ResultSet rs) throws SQLException {
		FrontierReportLine line = new FrontierReportLine();

		line.setAverageCost(rs.getDouble(FR_COLUMN.averageCost.name()));
		line.setCurrentSize(rs.getLong(FR_COLUMN.currentSize.name()));
		line.setDomainName(rs.getString(FR_COLUMN.domainName.name()));
		line.setErrorCount(rs.getLong(FR_COLUMN.errorCount.name()));
		line.setLastCost(rs.getDouble(FR_COLUMN.lastCost.name()));
		line.setLastDequeueTime(rs.getString(FR_COLUMN.lastDequeueTime.name()));
		line.setLastPeekUri(rs.getString(FR_COLUMN.lastPeekUri.name()));
		line.setLastQueuedUri(rs.getString(FR_COLUMN.lastQueuedUri.name()));
		line.setSessionBalance(rs.getLong(FR_COLUMN.sessionBalance.name()));
		line.setTotalBudget(rs.getLong(FR_COLUMN.totalBudget.name()));
		line.setTotalEnqueues(rs.getLong(FR_COLUMN.totalEnqueues.name()));
		line.setTotalSpend(rs.getLong(FR_COLUMN.totalSpend.name()));
		line.setWakeTime(rs.getString(FR_COLUMN.wakeTime.name()));

		return line;
	}

	/**
	 * Get a list of StartedJobInfo objects from a resultset of entries 
	 * from runningJobsHistory table.
	 * @param rs a resultset with entries from table runningJobsHistory.
	 * @return a list of StartedJobInfo objects from the resultset
	 * @throws SQLException If any problems reading data from the resultset
	 */
	private List<StartedJobInfo> listFromResultSet(ResultSet rs) 
			throws SQLException {
		List<StartedJobInfo> list = new LinkedList<StartedJobInfo>();
		while (rs.next()) {
			StartedJobInfo sji = new StartedJobInfo(
					rs.getString(HM_COLUMN.harvestName.name()),
					rs.getLong(HM_COLUMN.jobId.name()));
			sji.setElapsedSeconds(
					rs.getLong(HM_COLUMN.elapsedSeconds.name()));
			sji.setHostUrl(rs.getString(HM_COLUMN.hostUrl.name()));
			sji.setProgress(rs.getDouble(HM_COLUMN.progress.name()));
			sji.setQueuedFilesCount(
					rs.getLong(HM_COLUMN.queuedFilesCount.name()));
			sji.setTotalQueuesCount(
					rs.getLong(HM_COLUMN.totalQueuesCount.name()));
			sji.setActiveQueuesCount(
					rs.getLong(HM_COLUMN.activeQueuesCount.name()));
			sji.setExhaustedQueuesCount(
					rs.getLong(HM_COLUMN.exhaustedQueuesCount.name()));
			sji.setAlertsCount(rs.getLong(HM_COLUMN.alertsCount.name()));
			sji.setDownloadedFilesCount(
					rs.getLong(HM_COLUMN.downloadedFilesCount.name()));
			sji.setCurrentProcessedKBPerSec(
					rs.getLong(HM_COLUMN.currentProcessedKBPerSec.name()));
			sji.setProcessedKBPerSec(
					rs.getLong(HM_COLUMN.processedKBPerSec.name()));
			sji.setCurrentProcessedDocsPerSec(
					rs.getDouble(
							HM_COLUMN.currentProcessedDocsPerSec.name()));
			sji.setProcessedDocsPerSec(
					rs.getDouble(HM_COLUMN.processedDocsPerSec.name()));
			sji.setActiveToeCount(
					rs.getInt(HM_COLUMN.activeToeCount.name()));
			sji.setStatus(
					CrawlStatus.values()[
					                     rs.getInt(HM_COLUMN.status.name())]);
			sji.setTimestamp(new Date(
					rs.getTimestamp(HM_COLUMN.tstamp.name()).getTime()));

			list.add(sji);
		}
		return list;
	}

}
