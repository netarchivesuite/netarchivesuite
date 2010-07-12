/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
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

package dk.netarkivet.harvester.harvesting.controller;

import java.util.Collection;

import javax.management.openmbean.CompositeData;
import javax.management.openmbean.TabularData;
import javax.management.remote.JMXConnector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.archive.util.JmxUtils;

import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.IllegalState;
import dk.netarkivet.common.utils.JMXUtils;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.utils.SystemUtils;
import dk.netarkivet.common.utils.TimeUtils;
import dk.netarkivet.harvester.HarvesterSettings;
import dk.netarkivet.harvester.harvesting.HeritrixFiles;

/**
 * This implementation of the HeritrixController interface starts Heritrix
 * as a separate process and uses JMX to communicate with it.  Each instance
 * executes exactly one process that runs exactly one crawl job.
 */
public class JMXHeritrixController extends AbstractJMXHeritrixController {
    /** The logger for this class. */
    private static final Log log = LogFactory.getLog(
            JMXHeritrixController.class);

    /* The below commands and attributes are copied from 
     * org.archive.crawler.admin.CrawlJob.
     * @see <A href="http://crawler.archive.org/xref/org/archive/crawler/admin/CrawlJob.html">
     *  org.archive.crawler.admin.CrawlJob</A>
     *  
     * These strings are currently not visible from outside the Heritrix class.
     * See http://webteam.archive.org/jira/browse/HER-1285
     */
    /** The command to submit a new crawljob to the Crawlcontroller. */
    private static final String ADD_JOB_COMMAND = "addJob";
    /** The command to retrieve progress statistics for the currently 
     * running job. */
    private static final String PROGRESS_STATISTICS_COMMAND
            = "progressStatistics";
    /** The command to retrieve a progress statistics legend for the currently 
     * running job. */
    private static final String PROGRESS_STATISTICS_LEGEND_COMMAND
            = "progressStatisticsLegend";
    /** The attribute for the current download rate in kbytes for the
     * currently running job. */
    private static final String CURRENT_KB_RATE_ATTRIBUTE = "CurrentKbRate";
    /** The attribute for the number of currently running process-threads. */
    private static final String THREAD_COUNT_ATTRIBUTE = "ThreadCount";
    /** The attribute for the number of discovered URIs for the
     * currently running job. */
    private static final String DISCOVERED_COUNT_ATTRIBUTE = "DiscoveredCount";
    /** The attribute for the number of downloaded URIs for the
     * currently running job. */
    private static final String DOWNLOADED_COUNT_ATTRIBUTE = "DownloadedCount";
    /** The attribute for the status for the currently running job. */
    private static final String STATUS_ATTRIBUTE = "Status";


    /* The below commands and attributes are copied from
     * org.archive.crawler.Heritrix
     * @see <A href="http://crawler.archive.org/apidocs/org/archive/crawler/Heritrix.html">
     *  org.archive.crawler.Heritrix</A>
     *
     * These strings are currently not visible from outside the Heritrix class.
     * See http://webteam.archive.org/jira/browse/HER-1285
     */
    /* Note: The Heritrix JMX interface has two apparent ways to stop crawling:
     * stopCrawling and terminateCurrentJob.  stopCrawling merely makes Heritrix
     * not start any more jobs, but the old jobs continue.  Note that if we
     * start using more than one job at a time, terminateCurrentJob will only
     * stop one job.
     */
    /** Command to start crawling. */
    private static final String START_CRAWLING_COMMAND = "startCrawling";
    /** Make the currently active (selected?) job stop. */
    private static final String TERMINATE_CURRENT_JOB_COMMAND
            = "terminateCurrentJob";
    /** Command for returning list of pending jobs. */
    private static final String PENDING_JOBS_COMMAND = "pendingJobs";
    /** Command for returning list of completed jobs. */
    private static final String COMPLETED_JOBS_COMMAND = "completedJobs";
    /** Command for shutting down Heritrix.  */
    private static final String SHUTDOWN_COMMAND = "shutdown";

    /** The part of the Job MBean name that designates the unique id.  For some
     * reason, this is not included in the normal Heritrix definitions in
     * JmxUtils, otherwise we wouldn't have to define it. 
     * I have committed a feature request: 
     * http://webteam.archive.org/jira/browse/HER-1618
     */
    private static final String UID_PROPERTY = "uid";

    /** The name that Heritrix gives to the job we ask it to create.  This
     * is part of the name of the MBean for that job, but we can only retrieve
     * the name after the MBean has been created. */
    private String jobName;

    /** The header line (legend) for the statistics report. */
    private String progressStatisticsLegend;

    /* The possible values of a request of the status attribute.
     * Copied from private values in
     * {@link org.archive.crawler.framework.CrawlController} 
     * 
     * These strings are currently not visible from outside the 
     * CrawlController class.
     * See http://webteam.archive.org/jira/browse/HER-1285
     */
    /** The 'NASCENT' status. */
    //private static final String NASCENT_STATUS = "NASCENT";
    /** The 'RUNNING' status. */
    //private static final String RUNNING_STATUS = "RUNNING";
    /** The 'PAUSED' status. */
    private static final String PAUSED_STATUS = "PAUSED";
    /** The 'PAUSING' status. */
    private static final String PAUSING_STATUS = "PAUSING";
    /** The 'CHECKPOINTING' status. */
    //private static final String CHECKPOINTING_STATUS = "CHECKPOINTING";
    /** The 'STOPPING' status. */
    //private static final String STOPPING_STATUS = "STOPPING";
    /** The 'FINISHED' status. */
    private static final String FINISHED_STATUS = "FINISHED";
    /** The 'STARTED status. */
    //private static final String STARTED_STATUS = "STARTED";
    /** The 'PREPARING' status. */
    //private static final String PREPARING_STATUS = "PREPARING";
    /** The 'Illegal State' status. */
    private static final String ILLEGAL_STATUS = "Illegal State";

    /** Create a JMXHeritrixController object.
     *
     * @param files Files that are used to set up Heritrix.
     */
    public JMXHeritrixController(HeritrixFiles files) {
        super(files);
    }

    /**
     * @throws IOFailure If Heritrix dies before initialization,
     * or we encounter any problems during the initialization.
     * @see HeritrixController#initialize()  */
    public void initialize() {
        if (processHasExited()) {
            String errMsg = "Heritrix process of " + this
            + " died before initialization";
            log.warn(errMsg);
            throw new IOFailure(errMsg);
        }
        // We want to be sure there are no jobs when starting, in case we got
        // an old Heritrix or somebody added jobs behind our back.
        TabularData doneJobs =
                (TabularData) executeHeritrixCommand(COMPLETED_JOBS_COMMAND);
        TabularData pendingJobs =
                (TabularData) executeHeritrixCommand(PENDING_JOBS_COMMAND);
        if (doneJobs != null && doneJobs.size() > 0 ||
            pendingJobs != null && pendingJobs.size() > 0) {
            throw new IllegalState(
                    "This Heritrix instance is in a illegalState! "
                    + "This instance has either old done jobs ("
                                + doneJobs + "), or old pending jobs ("
                                + pendingJobs + ").");
        }
        // From here on, we can assume there's only the one job we make.
        // We'll use the arc file prefix to name the job, since the prefix
        // already contains the harvest id and job id.
        HeritrixFiles files = getHeritrixFiles();
        executeHeritrixCommand(ADD_JOB_COMMAND,
                             files.getOrderXmlFile().getAbsolutePath(),
                             files.getArcFilePrefix(), getJobDescription(),
                             files.getSeedsTxtFile().getAbsolutePath());
        jobName = getJobName();
        initializeProgressStatisticsLegend();
    }

    /**
     * @throws IOFailure if unable to communicate with Heritrix
     * @see HeritrixController#requestCrawlStart()
     */
    public void requestCrawlStart() {
        executeHeritrixCommand(START_CRAWLING_COMMAND);
    }

    /** @see HeritrixController#atFinish() */
    public boolean atFinish() {
        return crawlIsEnded();
    }

    /**
     * @throws IOFailure if unable to communicate with Heritrix
     * @see HeritrixController#beginCrawlStop() */
    public void beginCrawlStop() {
        executeHeritrixCommand(TERMINATE_CURRENT_JOB_COMMAND);
    }

    /** @see HeritrixController#getActiveToeCount()  */
    public int getActiveToeCount() {
        Integer activeToeCount =
                (Integer) getCrawlJobAttribute(THREAD_COUNT_ATTRIBUTE);
        if (activeToeCount == null) {
            return 0;
        }
        return activeToeCount;
    }

    /** @see HeritrixController#requestCrawlStop(String) */
    public void requestCrawlStop(String reason) {
        if (!atFinish()) {
            beginCrawlStop();
        }
    }

    /**
     * @see HeritrixController#getQueuedUriCount()
     * */
    public long getQueuedUriCount() {
        /* Implementation note:  This count is not as precise as what
         * StatisticsTracker could provide, but it's presently only used in
         * a warning in the HeritrixLauncher.doCrawlLoop() method.
         */
        Long discoveredUris
                = (Long) getCrawlJobAttribute(DISCOVERED_COUNT_ATTRIBUTE);
        Long downloadedUris
                = (Long) getCrawlJobAttribute(DOWNLOADED_COUNT_ATTRIBUTE);
        if (discoveredUris == null) {
            return 0;
        }
        if (downloadedUris == null) {
            return discoveredUris;
        }
        return discoveredUris - downloadedUris;
    }

    /** @see HeritrixController#getCurrentProcessedKBPerSec() */
    public int getCurrentProcessedKBPerSec() {
        Long currentDownloadRate =
                (Long) getCrawlJobAttribute(CURRENT_KB_RATE_ATTRIBUTE);
        if (currentDownloadRate == null) {
            return 0;
        }
        return currentDownloadRate.intValue();
    }

    /** @see HeritrixController#getProgressStats() */
    public String getProgressStats() {
        String status = (String) getCrawlJobAttribute(STATUS_ATTRIBUTE);

        if (status == null) {
            status = "NO STATUS";
        }

        String progressStatistics =
                (String) executeCrawlJobCommand(PROGRESS_STATISTICS_COMMAND);

        if (progressStatistics == null) {
            progressStatistics = "No progress statistics available";
        } else {
            // Since progressStatisticsLegend acts as a latch, we can check
            // for non-null even though it gets assigned asynchronously.
            if (progressStatisticsLegend != null) {
                progressStatistics = progressStatisticsLegend + '\n'
                                     + progressStatistics;
            }
        }
        return status + " " + progressStatistics;
    }

    /** Store the statistics legend line (asynchronously). */
    private void initializeProgressStatisticsLegend() {
        new Thread() {
            public void run() {
                progressStatisticsLegend = (String) executeCrawlJobCommand(
                        PROGRESS_STATISTICS_LEGEND_COMMAND);
            }
        }.start();
    }

    /** @see HeritrixController#isPaused()  */
    public boolean isPaused() {
        String status = (String) getCrawlJobAttribute(STATUS_ATTRIBUTE);
        log.debug("Heritrix state: '" + status + "'");
        // Either Pausing or Paused in case of not null
        return status != null
               && (status.equals(PAUSED_STATUS) 
                       || status.equals(PAUSING_STATUS));
    }

    /** Check if the crawl has ended, either because Heritrix finished
     * of its own, or because we terminated it.
     *
     * @return True if the crawl has ended, either because Heritrix finished
     * or because we terminated it. Otherwise we return false.
     * @see HeritrixController#crawlIsEnded()
     */
    public synchronized boolean crawlIsEnded() {
        // End of crawl can be seen in one of three ways:
        // 1) The Heritrix process has exited.
        // 2) The job has been moved to the completed jobs list in Heritrix.
        // 3) The job is in one of the FINISHED states.
        if (processHasExited()) {
            return true;
        }
        TabularData jobs =
                (TabularData) executeHeritrixCommand(
                        COMPLETED_JOBS_COMMAND);
        if (jobs != null && jobs.size() > 0) {
            for (CompositeData value
                    : (Collection<CompositeData>) jobs.values()) {
                String thisJobID = value.get(JmxUtils.NAME)
                                   + "-" + value.get(UID_PROPERTY);
                if (thisJobID.equals(jobName)) {
                    return true;
                }
            }
        }
        String status = (String) getCrawlJobAttribute(STATUS_ATTRIBUTE);
        return status == null
                || status.equals(FINISHED_STATUS)
                || status.equals(ILLEGAL_STATUS);
    }

    /** 
     * Cleanup after an Heritrix process.
     * This entails sending the shutdown command to the Heritrix process,
     * and killing it forcefully, if it is still alive after waiting 
     * the period of time specified by the CommonSettings.PROCESS_TIMEOUT
     * setting.
     * 
     * @see HeritrixController#cleanup() */
    public void cleanup() {
        try {
            executeHeritrixCommand(SHUTDOWN_COMMAND);
        } catch (IOFailure e) {
            log.error("JMX error while cleaning up Heritrix controller", e);
        }
        
        waitForHeritrixProcessExit();
    }

    /**
     * Return the URL for monitoring this instance.
     * @return the URL for monitoring this instance.
     */
    public String getHarvestInformation() {
        return "http://" + SystemUtils.getLocalHostName() + ":" + getGUIPort();
    }

    /** Get the name of the one job we let this Heritrix run.  The handling
     * of done jobs depends on Heritrix not being in crawl.  This call
     * may take several seconds to finish.
     *
     * @return The name of the one job that Heritrix has.
     * @throws IOFailure if the job created failed to initialize or didn't
     * appear in time.
     * @throws IllegalState if more than one job in done list,
     *  or more than one pending job
     */
    private String getJobName() {
        /* This is called just after we've told Heritrix to create a job.
         * It may take a while before the job is actually created, so we have
         * to wait around a bit.
         */
        TabularData pendingJobs = null;
        TabularData doneJobs;
        int retries = 0;
        while (retries++ < JMXUtils.getMaxTries()) {
            // If the job turns up in Heritrix' pending jobs list, it's ready
            pendingJobs =
                    (TabularData) executeHeritrixCommand(
                            PENDING_JOBS_COMMAND);
            if (pendingJobs != null && pendingJobs.size() > 0) {
                break; // It's ready, we can move on.
            }

            // If there's an error in the job configuration, the job will be put
            // in Heritrix' completed jobs list.
            doneJobs = (TabularData) executeHeritrixCommand(
                    COMPLETED_JOBS_COMMAND);
            if (doneJobs != null && doneJobs.size() >= 1) {
                // Since we haven't allowed Heritrix to start any crawls yet,
                //  the only way the job could have ended and then put into 
                //  the list of completed jobs is by error.
                if (doneJobs.size() > 1) {
                    throw new IllegalState("More than one job in done list: "
                                        + doneJobs);
                } else {
                    CompositeData job = JMXUtils.getOneCompositeData(doneJobs);
                    throw new IOFailure("Job " + job + " failed: "
                                        + job.get(STATUS_ATTRIBUTE));
                }
            }
            if (retries < JMXUtils.getMaxTries()) {
                TimeUtils.exponentialBackoffSleep(retries);
            }
        }
        // If all went well, we now have exactly one job in the pending
        // jobs list.
        if (pendingJobs == null || pendingJobs.size() == 0) {
            throw new IOFailure("Heritrix has not created a job after "
                                + (Math.pow(2, JMXUtils.getMaxTries()) / 1000)
                                + " seconds, giving up.");
        } else if (pendingJobs.size() > 1) {
            throw new IllegalState("More than one pending job: " + pendingJobs);
        } else {
            // Note that we may actually get through to here even if the job
            // is malformed.  The job will then die as soon as we tell it to
            // start crawling.
            CompositeData job = JMXUtils.getOneCompositeData(pendingJobs);
            String name = job.get(JmxUtils.NAME)
                          + "-" + job.get(UID_PROPERTY);
            log.info("Heritrix created a job with name " + name);
            return name;
        }
    }

    /** Get the name to use for logging on to Heritrix' JMX with full control.
     * The name cannot be set by the user.
     *
     * @return Name to use when connecting to Heritrix JMX
     */
    private String getJMXAdminName() {
        String jmxUsername = Settings.get(
                HarvesterSettings.HERITRIX_JMX_USERNAME);
        log.debug("The JMX username used for connecting to "
                + "the Heritrix GUI is: " + "'" + jmxUsername + "'.");
        return jmxUsername;
        }

    /** Get the password to use to access the Heritrix JMX as the user returned
     * by getJMXAdminName().  This password can be set in a file pointed to
     * in settings.xml. 
     * @return Password for accessing Heritrix JMX
     */
    private String getJMXAdminPassword() {
        return Settings.get(HarvesterSettings.HERITRIX_JMX_PASSWORD);
    }

    /** Get the port to use for Heritrix JMX, as set in settings.xml.
     *
     * @return Port that Heritrix will expose its JMX interface on.
     */
    private int getJMXPort() {
        return Settings.getInt(HarvesterSettings.HERITRIX_JMX_PORT);
    }

    /** Get the port to use for Heritrix GUI, as set in settings.xml.
     *
     * @return Port that Heritrix will expose its web interface on.
     */
    private int getGUIPort() {
        return Settings.getInt(HarvesterSettings.HERITRIX_GUI_PORT);
    }

    /** Execute a command for the Heritrix process we're running.
     *
     * @param command The command to execute.
     * @param arguments Any arguments to the command.  These arguments can
     * only be of String type.
     * @return Whatever object was returned by the JMX invocation.
     */
    private Object executeHeritrixCommand(String command, String... arguments) {
        return JMXUtils.executeCommand(getHeritrixJMXConnector(),
                                       getHeritrixBeanName(),
                                       command, arguments);
    }

    /** Execute a command for the Heritrix job.  This must only be called after
     * initialize() has been run.
     *
     * @param command The command to execute.
     * @param arguments Any arguments to the command.  These arguments can
     * only be of String type.
     * @return Whatever object was returned by the JMX invocation.
     */
    private Object executeCrawlJobCommand(String command, String... arguments) {
        return JMXUtils.executeCommand(getHeritrixJMXConnector(),
                                       getCrawlJobBeanName(),
                                       command, arguments);
    }

    /** Get an attribute of the Heritrix process we're running.
     *
     * @param attribute The attribute to get.
     * @return The value of the attribute.
     */
    private Object getHeritrixAttribute(String attribute) {
        return JMXUtils.getAttribute(getHeritrixJMXConnector(),
                                     getHeritrixBeanName(),
                                     attribute);
    }

    /** Get an attribute of the Heritrix job.  This must only be called after
     * initialize() has been run.
     *
     * @param attribute The attribute to get.
     * @return The value of the attribute.
     */
    private Object getCrawlJobAttribute(String attribute) {
        return JMXUtils.getAttribute(getHeritrixJMXConnector(),
                                     getCrawlJobBeanName(),
                                     attribute);
    }

    /** Get the name for the main bean of the Heritrix instance.
     *
     * @return Bean name, to be passed into JMXUtils#getBeanName(String)
     */
    private String getHeritrixBeanName() {
        return "org.archive.crawler:"
               + JmxUtils.NAME + "=Heritrix,"
               + JmxUtils.TYPE + "=CrawlService,"
               + JmxUtils.JMX_PORT + "=" + getJMXPort() + ","
               + JmxUtils.GUI_PORT + "=" + getGUIPort() + ","
               + JmxUtils.HOST + "=" + getHostName();

    }

    /** Get the name for the bean of a single job.  This bean does not exist
     * until after a job has been created using initialize().
     *
     * @return Bean name, to be passed into JMXUtils#getBeanName(String)
     */
    private String getCrawlJobBeanName() {
        return "org.archive.crawler:"
               + JmxUtils.NAME + "=" + jobName + ","
               + JmxUtils.TYPE + "=CrawlService.Job,"
               + JmxUtils.JMX_PORT + "=" + getJMXPort() + ","
               + JmxUtils.MOTHER + "=Heritrix,"
              + JmxUtils.HOST + "=" + getHostName();
    }

    /** Get the JMX connector to Heritrix.
     *
     * @return A connector that connects to a local Heritrix instance.
     */
    private JMXConnector getHeritrixJMXConnector() {
        return JMXUtils.getJMXConnector(SystemUtils.LOCALHOST, getJMXPort(),
                                        getJMXAdminName(),
                                        getJMXAdminPassword());
    }
}
