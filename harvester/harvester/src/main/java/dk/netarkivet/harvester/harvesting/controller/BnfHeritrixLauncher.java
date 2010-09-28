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
package dk.netarkivet.harvester.harvesting.controller;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.harvester.HarvesterSettings;
import dk.netarkivet.harvester.harvesting.HeritrixFiles;
import dk.netarkivet.harvester.harvesting.HeritrixLauncher;
import dk.netarkivet.harvester.harvesting.distribute.CrawlProgressMessage;
import dk.netarkivet.harvester.harvesting.frontier.FrontierReportAnalyzer;
import dk.netarkivet.harvester.harvesting.monitor.HarvestMonitorServer;

/**
 * BnF specific Heritrix launcher, that forces the use of
 * {@link BnfHeritrixController}. Every turn of the crawl control loop, asks the
 * Heritrix controller to generate a progress report as a
 * {@link CrawlProgressMessage} and then send this message on the JMS bus to
 * be consumed by the {@link HarvestMonitorServer} instance.
 */
public class BnfHeritrixLauncher extends HeritrixLauncher {

    /**
     * This class is an executor for scheduled frontier report analysis tasks.
     * @see ScheduledThreadPoolExecutor
     */
    private static class FrontierReportAnalysisExecutor
    extends ScheduledThreadPoolExecutor {

        public FrontierReportAnalysisExecutor() {
            // We need only 1 thread
            super(1);
        }

        @Override
        protected void afterExecute(Runnable task, Throwable t) {
            if (t != null) {
                log.error("Error during frontier report generation", t);
            }
        }

    }

    /**
     * This class is an executor for scheduled crawl control tasks.
     * @see ScheduledThreadPoolExecutor
     */
    private static class CrawlControlExecutor
    extends ScheduledThreadPoolExecutor {

        public CrawlControlExecutor() {
            // We need only 1 thread
            super(1);
        }

        @Override
        protected void afterExecute(Runnable task, Throwable t) {
            if (t != null) {
                log.error("Error during crawl control", t);
            }
        }



    }

    /**
     * This class executes a crawl control task, e.g. queries the crawler for
     * progress summary, sends the adequate JMS message to the monitor, and
     * checks whether the crawl is finished, in which case crawl control will
     * be ended.
     *
     * These tasks are scheduled by a {@link CrawlControlExecutor}.
     */
    private class CrawlControl implements Runnable {

        public Boolean crawlIsFinished = false;

        @Override
        public void run() {
            CrawlProgressMessage cpm;
            try {
                cpm = heritrixController.getCrawlProgress();
            } catch (IOFailure iof) {
                // Log a warning and retry
                log.warn("IOFailure while getting crawl progress", iof);
                return;
            }

            getJMSConnection().send(cpm);

            if (cpm.crawlIsFinished()) {
                synchronized (crawlIsFinished) {
                 // Crawl is over, set flag
                    crawlIsFinished = true;
                    return;
                }
            }

            HeritrixFiles files = getHeritrixFiles();
            log.info("Job ID: " + files.getJobID() + ", Harvest ID: "
                    + files.getHarvestID() + ", " + cpm.getHostUrl() + "\n"
                    + cpm.getProgressStatisticsLegend() + "\n"
                    + cpm.getJobStatus().getStatus() + " "
                    + cpm.getJobStatus().getProgressStatistics());
        }

        /**
         * @return the crawlIsFinished
         */
        protected boolean crawlIsFinished() {
            synchronized (crawlIsFinished) {
                return crawlIsFinished;
            }
        }
    }

    /** The class logger. */
    final static Log log = LogFactory.getLog(BnfHeritrixLauncher.class);

    /**
     * Wait time in milliseconds (10s).
     */
    private final static int SLEEP_TIME_MS = 10 * 60 * 1000;

    /**
     * Frequency in seconds for generating the full harvest report.
     * Also serves as delay before the first generation occurs.
     */
    final static long FRONTIER_REPORT_GEN_FREQUENCY =
        Settings.getLong(HarvesterSettings.FRONTIER_REPORT_WAIT_TIME);

    /** The CrawlController used. */
    private BnfHeritrixController heritrixController;

    private ScheduledFuture<?> crawlControlHandle = null;

    private ScheduledFuture<?> frontierReportGeneratorHandle = null;

    private BnfHeritrixLauncher(HeritrixFiles files) throws ArgumentNotValid {
        super(files);
    }

    /**
     * Get instance of this class.
     *
     * @param files
     *            Object encapsulating location of Heritrix crawldir and
     *            configuration files
     *
     * @return {@link BnfHeritrixLauncher} object
     *
     * @throws ArgumentNotValid
     *             If either order.xml or seeds.txt does not exist, or argument
     *             files is null.
     */
    public static BnfHeritrixLauncher getInstance(HeritrixFiles files)
            throws ArgumentNotValid {
        ArgumentNotValid.checkNotNull(files, "HeritrixFiles files");
        return new BnfHeritrixLauncher(files);
    }

    /**
     * Initializes an Heritrix controller, then launches the Heritrix instance.
     * Then starts the crawl control loop:
     * <ol>
     * <li>Waits the amount of time configured in
     * {@link HarvesterSettings#CRAWL_LOOP_WAIT_TIME}.</li>
     * <li>Obtains crawl progress information as a {@link CrawlProgressMessage}
     * from the Heritrix controller</li>
     * <li>Sends the progress message via JMS</li>
     * <li>If the crawl if reported as finished, end loop.</li>
     * </ol>
     */
    public void doCrawl() throws IOFailure {
        setupOrderfile();
        heritrixController = new BnfHeritrixController(getHeritrixFiles());

        ScheduledFuture<?> crawlControlHandle = null;
        ScheduledFuture<?> frontierReportGeneratorHandle = null;
        try {
            // Initialize Heritrix settings according to the order.xml
            heritrixController.initialize();
            log.debug("Starting crawl..");
            heritrixController.requestCrawlStart();

            // Schedule full frontier report generation
            FrontierReportAnalysisExecutor frontierReportSched =
                new FrontierReportAnalysisExecutor();

            frontierReportGeneratorHandle =
                frontierReportSched.scheduleAtFixedRate(
                        new FrontierReportAnalyzer(heritrixController),
                        FRONTIER_REPORT_GEN_FREQUENCY,
                        FRONTIER_REPORT_GEN_FREQUENCY,
                        TimeUnit.SECONDS);

            CrawlControl crawlControl = new CrawlControl();
            CrawlControlExecutor crawlCtrlSched = new CrawlControlExecutor();
            crawlControlHandle =
                crawlCtrlSched.scheduleWithFixedDelay(
                        crawlControl,
                        CRAWL_CONTROL_WAIT_PERIOD,
                        CRAWL_CONTROL_WAIT_PERIOD,
                        TimeUnit.SECONDS);

            while (! crawlControl.crawlIsFinished()) {
                // Wait a bit
                try {
                    synchronized (this) {
                        wait(SLEEP_TIME_MS);
                    }
                } catch (InterruptedException e) {
                    log.trace("Waiting thread awoken: " + e.getMessage());
                }
            }

        } catch (IOFailure e) {
            log.warn("Error during initialisation of crawl", e);
            throw (e);
        } catch (Exception e) {
            log.warn("Exception during crawl", e);
            throw new RuntimeException("Exception during crawl", e);
        } finally {
            // Stop the frontier report generation schedule
            if (frontierReportGeneratorHandle != null) {
                frontierReportGeneratorHandle.cancel(true);
            }

            // Stop the crawl control
            if (crawlControlHandle != null) {
                crawlControlHandle.cancel(true);
            }

            if (heritrixController != null) {
                heritrixController.cleanup(getHeritrixFiles().getCrawlDir());
            }
        }
        log.debug("Heritrix is finished crawling...");
    }
}
