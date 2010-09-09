/* File:       $Id$
 * Revision:   $Revision$
 * Author:     $Author$
 * Date:       $Date$
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
package dk.netarkivet.harvester.harvesting.monitor;

import java.io.File;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.jms.MessageListener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.common.distribute.ChannelID;
import dk.netarkivet.common.distribute.Channels;
import dk.netarkivet.common.distribute.JMSConnectionFactory;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.lifecycle.ComponentLifeCycle;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.harvester.HarvesterSettings;
import dk.netarkivet.harvester.datamodel.JobDAO;
import dk.netarkivet.harvester.datamodel.JobStatus;
import dk.netarkivet.harvester.datamodel.RunningJobsInfoDAO;
import dk.netarkivet.harvester.distribute.HarvesterMessageHandler;
import dk.netarkivet.harvester.harvesting.distribute.CrawlProgressMessage;
import dk.netarkivet.harvester.harvesting.distribute.FrontierReportMessage;
import dk.netarkivet.harvester.harvesting.frontier.ExhaustedQueuesFilter;
import dk.netarkivet.harvester.harvesting.frontier.InMemoryFrontierReport;
import dk.netarkivet.harvester.harvesting.frontier.RetiredQueuesFilter;
import dk.netarkivet.harvester.harvesting.frontier.TopTotalEnqueuesFilter;
import dk.netarkivet.harvester.scheduler.HarvestJobManager;

/**
 * Listens for {@link CrawlProgressMessage}s on the proper JMS channel, and
 * stores information to be presented in the monitoring console.
 */
public class HarvestMonitorServer
extends HarvesterMessageHandler
implements MessageListener, ComponentLifeCycle {

    /** The logger for this class. */
    private static final Log LOG = LogFactory
            .getLog(HarvestMonitorServer.class);

    /**
     * Singleton instance of the monitor.
     */
    private static HarvestMonitorServer instance;

    /**
     * The JMS channel on which to listen for {@link CrawlProgressMessage}s.
     */
    public static final ChannelID CRAWL_PROGRESS_CHANNEL_ID =
        Channels.getHarvestMonitorServerChannel();

    /**
     * The JMS channel on which to listen for {@link FrontierReportMessage}s.
     */
    public static final ChannelID FRONTIER_CHANNEL_ID =
        Channels.getFrontierReportMonitorServerChannel();

    private Map<Long, StartedJobHistoryChartGen> chartGenByJobId =
        new HashMap<Long, StartedJobHistoryChartGen>();

    private HarvestMonitorServer() {
        JMSConnectionFactory.getInstance().setListener(
                CRAWL_PROGRESS_CHANNEL_ID, this);
        JMSConnectionFactory.getInstance().setListener(
                FRONTIER_CHANNEL_ID, this);
    }

    /**
     * Does nothing.
     * TODO this class should not be a singleton (see {@link HarvestJobManager})
     */
    @Override
    public void start() {

    }

    /**
     * Close down the HarvestMonitorServer singleton. This removes the
     * HarvestMonitorServer as listener to the JMS scheduler
     * and frontier channels, closes the persistence container, and resets
     * the singleton.
     *
     */
    @Override
    public void shutdown() {
        JMSConnectionFactory.getInstance().removeListener(
                CRAWL_PROGRESS_CHANNEL_ID, this);
        JMSConnectionFactory.getInstance().removeListener(
                FRONTIER_CHANNEL_ID, this);
        instance = null;
    }

    /**
     * @return the singleton instance for this class.
     */
    public static HarvestMonitorServer getInstance() {
        if (instance == null) {
            instance = new HarvestMonitorServer();
        }
        return instance;
    }

    @Override
    public void visit(CrawlProgressMessage msg) {
        ArgumentNotValid.checkNotNull(msg, "msg");

        Long jobId = Long.valueOf(msg.getJobID());

        JobStatus jobStatus = JobDAO.getInstance().read(jobId).getStatus();
        if (!JobStatus.STARTED.equals(jobStatus)) {
            return;
        }

        StartedJobInfo info = StartedJobInfo.build(msg);
        RunningJobsInfoDAO.getInstance().store(info);

        // Start a chart generator if none has been started yet
        if (chartGenByJobId.get(jobId) == null) {
            chartGenByJobId.put(jobId, new StartedJobHistoryChartGen(jobId));
        }
    }

    /**
     * Returns the delay in seconds after which a harvest monitor webpage should
     * refresh itself.
     * This delay is set by overriding the value of the
     * {@link HarvesterSettings#HARVEST_MONITOR_REFRESH_INTERVAL} property.
     * @return the delay in seconds after which a harvest monitor webpage should
     * refresh itself
     */
    public static final int getAutoRefreshDelay() {
        return Settings.getInt(
                HarvesterSettings.HARVEST_MONITOR_REFRESH_INTERVAL);
    }

    /**
     * Returns a configurable number of the most recent running job info records
     * available for the given job ID.
     * @param jobId
     * @return the most recent running job info records available
     * for the given job ID.
     * @see HarvesterSettings#HARVEST_MONITOR_DISPLAYED_HISTORY_SIZE
     */
    public static StartedJobInfo[] getMostRecentRunningJobInfos(long jobId) {
        int displayedHistorySize = Settings.getInt(
                HarvesterSettings.HARVEST_MONITOR_DISPLAYED_HISTORY_SIZE);
       return RunningJobsInfoDAO.getInstance().getMostRecentByJobId(
               jobId,
               0, // for now. TODO pagination
               displayedHistorySize);
    }

    @Override
    public void visit(FrontierReportMessage msg) {
        ArgumentNotValid.checkNotNull(msg, "msg");

        int insertCount = RunningJobsInfoDAO.getInstance().storeFrontierReport(
                msg.getFilterId(),
                msg.getReport());
        LOG.info("Stored frontier report " + msg.getReport().getJobName()
                + "-" + msg.getFilterId()
                + "' (" + msg.getReport().getSize() + " lines)"
                + ": inserted " + insertCount + " lines in the DB");
    }

    /**
     * Retrieves the latest frontier report stored for the given job ID.
     * @param jobId the job id
     * @return a frontier report
     */
    public InMemoryFrontierReport getFrontierReport(long jobId) {
        // Right now there's only one filter and it's not user controlled.
        return RunningJobsInfoDAO.getInstance().getFrontierReport(
                jobId,
                new TopTotalEnqueuesFilter().getFilterId());
    }

    public InMemoryFrontierReport getFrontierRetiredQueues(long jobId) {
        return RunningJobsInfoDAO.getInstance().getFrontierReport(
                jobId,
                new RetiredQueuesFilter().getFilterId());
    }

    public InMemoryFrontierReport getFrontierExhaustedQueues(long jobId) {
        return RunningJobsInfoDAO.getInstance().getFrontierReport(
                jobId,
                new ExhaustedQueuesFilter().getFilterId());
    }

    /**
     * Notifies the monitor that a job ended, and that all progress data
     * should be wiped.
     * @param jobId the job id
     */
    public void notifyJobEnded(long jobId, JobStatus newStatus) {

        // Delete records in the DB
        RunningJobsInfoDAO dao = RunningJobsInfoDAO.getInstance();
        int delCount = dao.removeInfoForJob(jobId);
        LOG.info("Deleted " + delCount + " running job info records"
                + " for job ID " + jobId
                + " on transition to status " + newStatus.name());

        // Stop chart generation
        StartedJobHistoryChartGen gen = chartGenByJobId.get(jobId);
        if (gen != null) {
            gen.cleanup();
        }
    }

    /**
     * Default chart image.
     */
    private static final String EMPTY_CHART_FILE = "empty-history.png";

    /**
     * Returns the path of the chart image file, relative to the
     * webapp directory. If no chart is available, returns a default empty
     * image.
     * @param jobId the job id
     * @return the path of the chart image file, relative to the
     * webapp directory.
     */
    public String getChartFilePath(long jobId) {
        StartedJobHistoryChartGen gen = chartGenByJobId.get(jobId);
        if (gen != null) {
            File chartFile = gen.getChartFile();
            if (chartFile == null) {
                return EMPTY_CHART_FILE;
            }
            return chartFile.getName();
        }
        return EMPTY_CHART_FILE;
    }

    /**
     * Sets the chart generator locale.
     * @param jobId
     * @param loc
     */
    public void setChartLocale(long jobId, Locale loc) {
        StartedJobHistoryChartGen gen = chartGenByJobId.get(jobId);
        if (gen != null) {
            gen.setLocale(loc);
        }
    }

}
