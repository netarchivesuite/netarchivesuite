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
package dk.netarkivet.harvester.harvesting.monitor;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.jms.MessageListener;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.distribute.ChannelID;
import dk.netarkivet.common.distribute.JMSConnectionFactory;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.utils.CleanupIF;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.harvester.HarvesterSettings;
import dk.netarkivet.harvester.datamodel.JobDAO;
import dk.netarkivet.harvester.datamodel.JobStatus;
import dk.netarkivet.harvester.datamodel.RunningJobsInfoDAO;
import dk.netarkivet.harvester.distribute.HarvesterChannels;
import dk.netarkivet.harvester.distribute.HarvesterMessageHandler;
import dk.netarkivet.harvester.harvesting.distribute.CrawlProgressMessage;
import dk.netarkivet.harvester.harvesting.distribute.FrontierReportMessage;
import dk.netarkivet.harvester.harvesting.distribute.JobEndedMessage;
import dk.netarkivet.harvester.harvesting.frontier.ExhaustedQueuesFilter;
import dk.netarkivet.harvester.harvesting.frontier.InMemoryFrontierReport;
import dk.netarkivet.harvester.harvesting.frontier.RetiredQueuesFilter;
import dk.netarkivet.harvester.harvesting.frontier.TopTotalEnqueuesFilter;

/**
 * Listens for {@link CrawlProgressMessage}s on the proper JMS channel, and stores information to be presented in the
 * monitoring console.
 */
public class HarvestMonitor extends HarvesterMessageHandler implements MessageListener, CleanupIF {

    /** The logger for this class. */
    private static final Logger LOG = LoggerFactory.getLogger(HarvestMonitor.class);

    /** Singleton instance of the monitor. */
    private static HarvestMonitor instance;
    /** Harvest Monitor refresh Interval. */
    private int refreshInterval;

    /** The JMS channel on which to listen for {@link CrawlProgressMessage}s. */
    public static final ChannelID HARVEST_MONITOR_CHANNEL_ID = HarvesterChannels.getHarvestMonitorChannel();

    private Map<Long, StartedJobHistoryChartGen> chartGenByJobId = new HashMap<Long, StartedJobHistoryChartGen>();

    private Set<Long> runningJobs = new TreeSet<Long>();

    private HarvestMonitor() {
    	refreshInterval = Settings.getInt(HarvesterSettings.HARVEST_MONITOR_REFRESH_INTERVAL);
    	LOG.info("Initializing HarvestMonitor with refreshInterval={} seconds", refreshInterval);
    	
        // Perform initial cleanup (in case apps crashed)
        cleanOnStartup();

        // Register for listening JMS messages
        JMSConnectionFactory.getInstance().setListener(HARVEST_MONITOR_CHANNEL_ID, this);
        LOG.info("Started listening to queue {}", HARVEST_MONITOR_CHANNEL_ID);
    }

    /**
     * Close down the HarvestMonitor singleton. This removes the HarvestMonitor as listener to the JMS scheduler and
     * frontier channels, closes the persistence container, and resets the singleton.
     *
     * @see CleanupIF#cleanup()
     */
    public void cleanup() {
        JMSConnectionFactory.getInstance().removeListener(HARVEST_MONITOR_CHANNEL_ID, this);

        for (StartedJobHistoryChartGen chartGen : chartGenByJobId.values()) {
            chartGen.cleanup();
        }

        instance = null;
    }

    /**
     * @return the singleton instance for this class.
     */
    public static synchronized HarvestMonitor getInstance() {
        if (instance == null) {
            instance = new HarvestMonitor();
        }
        return instance;
    }

    @Override
    public void visit(CrawlProgressMessage msg) {
        ArgumentNotValid.checkNotNull(msg, "msg");
        Long jobId = Long.valueOf(msg.getJobID());
        
        JobStatus jobStatus = JobDAO.getInstance().read(jobId).getStatus();
        if (!JobStatus.STARTED.equals(jobStatus)) {
        	LOG.warn("Receiving CrawlProgressMessage for job {} registered as state {} instead of STARTED. Ignoring message", jobId, jobStatus);
            return;
        }
        
        StartedJobInfo info = StartedJobInfo.build(msg);
        LOG.trace("Received CrawlProgressMessage for jobId {}: {}", jobId, info);
        RunningJobsInfoDAO.getInstance().store(info);

        runningJobs.add(jobId);

        // Start a chart generator if none has been started yet
        if (chartGenByJobId.get(jobId) == null) {
            chartGenByJobId.put(jobId, new StartedJobHistoryChartGen(jobId));
        }
    }

    /**
     * Cleans up the database on transitions to status DONE and FAILED.
     *
     * @param msg a {@link JobEndedMessage}
     */
    @Override
    public void visit(JobEndedMessage msg) {
        ArgumentNotValid.checkNotNull(msg, "msg");

        JobStatus newStatus = msg.getJobStatus();
        long jobId = msg.getJobId();

        // Delete records in the DB
        RunningJobsInfoDAO dao = RunningJobsInfoDAO.getInstance();
        int delCount = dao.removeInfoForJob(jobId);
        LOG.info("Processing JobEndedMessage. Deleted {} running job info records for job ID {} on transition to status {}", delCount, jobId,
                newStatus.name());

        runningJobs.remove(jobId);

        // Stop chart generation
        StartedJobHistoryChartGen gen = chartGenByJobId.get(jobId);
        if (gen != null) {
            gen.cleanup();
        }
    }

    /**
     * Returns the delay in seconds after which a harvest monitor webpage should refresh itself. This delay is set by
     * overriding the value of the {@link HarvesterSettings#HARVEST_MONITOR_REFRESH_INTERVAL} property.
     *
     * @return the delay in seconds after which a harvest monitor webpage should refresh itself
     */
    public static final int getAutoRefreshDelay() {
        return Settings.getInt(HarvesterSettings.HARVEST_MONITOR_REFRESH_INTERVAL);
    }

    /**
     * Returns a configurable number of the most recent running job info records available for the given job ID.
     *
     * @param jobId
     * @return the most recent running job info records available for the given job ID.
     * @see HarvesterSettings#HARVEST_MONITOR_DISPLAYED_HISTORY_SIZE
     */
    public static StartedJobInfo[] getMostRecentRunningJobInfos(long jobId) {
        int displayedHistorySize = Settings.getInt(HarvesterSettings.HARVEST_MONITOR_DISPLAYED_HISTORY_SIZE);
        // for now. TODO pagination
        return RunningJobsInfoDAO.getInstance().getMostRecentByJobId(jobId, 0, displayedHistorySize);
    }

    /**
     * Returns the most recent running job info record available for the given job ID.
     *
     * @param jobId
     * @return the most recent running job info records available for the given job ID.
     */
    public static StartedJobInfo getMostRecentRunningJobInfo(long jobId) {
        return RunningJobsInfoDAO.getInstance().getMostRecentByJobId(jobId);
    }

    @Override
    public void visit(FrontierReportMessage msg) {
        ArgumentNotValid.checkNotNull(msg, "msg");

        int insertCount = RunningJobsInfoDAO.getInstance().storeFrontierReport(msg.getFilterId(), msg.getReport(),
                msg.getJobID());
        if (LOG.isInfoEnabled() && insertCount > 0) {
            LOG.info("Stored frontier report {}-{}' ({} lines): inserted {} lines in the DB", msg.getReport()
                    .getJobName(), msg.getFilterId(), msg.getReport().getSize(), insertCount);
        }
    }

    /**
     * Retrieves the latest frontier report stored for the given job ID.
     *
     * @param jobId the job id
     * @return a frontier report
     */
    public static InMemoryFrontierReport getFrontierReport(long jobId) {
        // Right now there's only one filter and it's not user controlled.
        return RunningJobsInfoDAO.getInstance().getFrontierReport(jobId, new TopTotalEnqueuesFilter().getFilterId());
    }
    
    /**
     * Retrieve a frontier report from a job id, with limited results and possibility to sort by totalenqueues DESC
     *
     * @param jobId the job id
     * @param limit the limit of result to query
     * @param sort if true, sort the results by totalenqueues DESC
     * @return a frontier report
     */
    public static InMemoryFrontierReport getFrontierReport(long jobId, boolean sort) {
    	int displayedHistorySize = 100; //default value
    	try {
    		displayedHistorySize = Settings.getInt(HarvesterSettings.HARVEST_MONITOR_DISPLAYED_FRONTIER_QUEUE_SIZE);
    	} catch (Exception e) {
    		//nothing
    	}
        // Right now there's only one filter and it's not user controlled.
        return RunningJobsInfoDAO.getInstance().getFrontierReport(jobId, displayedHistorySize, sort);
    }
    
    /**
     * Retrieve a frontier report from a job id, with limited results and possibility to sort by totalenqueues DESC
     *
     * @param jobId the job id
     * @param limit the limit of result to query
     * @param sort if true, sort the results by totalenqueues DESC
     * @return a frontier report
     */
    public static InMemoryFrontierReport getFrontierActiveAndInactiveQueuesReport(long jobId, boolean sort) {
    	int displayedHistorySize = 100; //default value
    	try {
    		displayedHistorySize = Settings.getInt(HarvesterSettings.HARVEST_MONITOR_DISPLAYED_FRONTIER_QUEUE_SIZE);
    	} catch (Exception e) {
    		//nothing
    	}
        // Right now there's only one filter and it's not user controlled.
        return RunningJobsInfoDAO.getInstance().getFrontierReport(jobId, new TopTotalEnqueuesFilter().getFilterId(),
        		displayedHistorySize, sort);
    }

    /**
     * Retrieves the latest frontier extract report stored for the given job ID, that contains only retired queues.
     *
     * @param jobId the job id
     * @return a frontier report that contains only retired queues.
     */
    public static InMemoryFrontierReport getFrontierRetiredQueues(long jobId) {
        return RunningJobsInfoDAO.getInstance().getFrontierReport(jobId, new RetiredQueuesFilter().getFilterId());
    }

    /**
     * Retrieves the latest frontier extract report stored for the given job ID, that contains only exhausted queues.
     *
     * @param jobId the job id
     * @return a frontier report that contains only exhausted queues.
     */
    public static InMemoryFrontierReport getFrontierExhaustedQueues(long jobId) {
        return RunningJobsInfoDAO.getInstance().getFrontierReport(jobId, new ExhaustedQueuesFilter().getFilterId());
    }

    /** Default chart image. */
    private static final String EMPTY_CHART_FILE = "empty-history.png";

    /**
     * Returns the path of the chart image file, relative to the webapp directory. If no chart is available, returns a
     * default empty image.
     *
     * @param jobId the job id
     * @return the path of the chart image file, relative to the webapp directory.
     */
    public static String getChartFilePath(long jobId) {
        if (instance == null) {
            return EMPTY_CHART_FILE;
        }
        StartedJobHistoryChartGen gen = instance.chartGenByJobId.get(jobId);
        if (gen != null) {
            File chartFile = gen.getChartFile();
            if (chartFile == null) {
                return EMPTY_CHART_FILE;
            }
            return chartFile.getName();
        }
        return EMPTY_CHART_FILE;
    }

    private void cleanOnStartup() {
        Set<Long> idsToRemove = new TreeSet<Long>();

        RunningJobsInfoDAO dao = RunningJobsInfoDAO.getInstance();
        idsToRemove.addAll(dao.getHistoryRecordIds());
        Iterator<Long> startedJobIds = JobDAO.getInstance().getAllJobIds(JobStatus.STARTED);
        while (startedJobIds.hasNext()) {
            // don't remove records for jobs still in status STARTED
            idsToRemove.remove(startedJobIds.next());
        }

        int delCount = 0;
        for (long jobId : idsToRemove) {
            delCount += dao.removeInfoForJob(jobId);
            delCount += dao.deleteFrontierReports(jobId);
        }
        if (delCount > 0) {
            LOG.info("Cleaned up {} obsolete history records for finished jobs {}", delCount, StringUtils.join(idsToRemove, ","));
        }
    }

    public Set<Long> getRunningJobs() {
    	return Collections.unmodifiableSet(runningJobs);
    }

}
