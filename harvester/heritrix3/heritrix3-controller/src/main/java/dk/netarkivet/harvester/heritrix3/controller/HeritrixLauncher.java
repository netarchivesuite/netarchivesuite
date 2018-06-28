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
package dk.netarkivet.harvester.heritrix3.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.distribute.JMSConnectionFactory;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.HarvestingAbort;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.harvester.HarvesterSettings;
import dk.netarkivet.harvester.harvesting.distribute.CrawlProgressMessage;
import dk.netarkivet.harvester.harvesting.monitor.HarvestMonitor;
import dk.netarkivet.harvester.heritrix3.Heritrix3Files;
import dk.netarkivet.harvester.heritrix3.Heritrix3Settings;
import dk.netarkivet.harvester.heritrix3.HeritrixLauncherAbstract;

/**
 * BnF specific Heritrix3 launcher, that forces the use of {@link HeritrixController}. Every turn of the crawl control
 * loop, asks the Heritrix3 controller to generate a progress report as a {@link CrawlProgressMessage} and then send this
 * message on the JMS bus to be consumed by the {@link HarvestMonitor} instance.
 */
public class HeritrixLauncher extends HeritrixLauncherAbstract {

    /** The class logger. */
    private static final Logger log = LoggerFactory.getLogger(HeritrixLauncher.class);

    /** Frequency in seconds for generating the full harvest report. Also serves as delay before the first generation
     *  occurs. */
    static final long FRONTIER_REPORT_GEN_FREQUENCY = Settings.getLong(Heritrix3Settings.FRONTIER_REPORT_WAIT_TIME);

    /** The CrawlController used. */
    private HeritrixController heritrixController;

    private String jobName;

    /** Is the heritrix3 crawl finished. */
    private boolean crawlIsOver = false;

    /**
     * Private constructor for this class.
     *
     * @param files the files needed by Heritrix to launch a job.
     * @throws ArgumentNotValid
     */
    private HeritrixLauncher(Heritrix3Files files, String jobName) throws ArgumentNotValid {
        super(files);
        this.jobName = jobName;
    }

    /**
     * Get instance of this class.
     *
     * @param files Object encapsulating location of Heritrix crawldir and configuration files
     * @return {@link HeritrixLauncher} object
     * @throws ArgumentNotValid If either order.xml or seeds.txt does not exist, or argument files is null.
     */
    public static HeritrixLauncher getInstance(Heritrix3Files files, String jobName) throws ArgumentNotValid {
        ArgumentNotValid.checkNotNull(files, "Heritrix3Files files");
        return new HeritrixLauncher(files, jobName); // The launching takes place here
    } 

    /**
     * Initializes an Heritrix3controller, then launches the Heritrix3 instance. Then starts the crawl control loop:
     * <ol>
     * <li>Waits the amount of time configured in {@link HarvesterSettings#CRAWL_LOOP_WAIT_TIME}.</li>
     * <li>Obtains crawl progress information as a {@link CrawlProgressMessage} from the Heritrix controller</li>
     * <li>Sends a progress message via JMS</li>
     * <li>If the crawl is reported as finished, end loop.</li>
     * </ol>
     */
    public void doCrawl() throws IOFailure {
        setupOrderfile(getHeritrixFiles());
        heritrixController = new HeritrixController(getHeritrixFiles(), jobName);
        
        try {
            // Initialize Heritrix settings according to the crawler-beans.cxml file.
            heritrixController.initialize();
            log.debug("Setup and start new h3 crawl");
            heritrixController.requestCrawlStart();
                
            log.info("Starting periodic CrawlControl with CRAWL_CONTROL_WAIT_PERIOD={} seconds", CRAWL_CONTROL_WAIT_PERIOD);            
          
            while (!crawlIsOver) {
                CrawlControl cc = new CrawlControl();
                cc.run();
                FrontierReportAnalyzer fra = new FrontierReportAnalyzer(heritrixController);
                fra.run();
                if (!crawlIsOver) {
                    try {
                    Thread.sleep(CRAWL_CONTROL_WAIT_PERIOD*1000L);
                    } catch (InterruptedException e) {
                        log.warn("Wait interrupted: " + e);
                    }
                }
            }
            log.info("CrawlJob is now over");
        } catch (IOFailure e) {
            log.warn("Error during initialisation of crawl", e);
            throw (e);
        } catch (Exception e) {
            log.warn("Exception during crawl", e);
            throw new RuntimeException("Exception during crawl", e);
        } finally {
            if (heritrixController != null) {
                heritrixController.cleanup(getHeritrixFiles().getCrawlDir());
            }
        }
        log.debug("Heritrix3 has finished crawling...");
    }

    /**
     * This class executes a crawl control task, e.g. queries the crawler for progress summary, sends the adequate JMS
     * message to the monitor, and checks whether the crawl is finished, in which case crawl control will be ended.
     * <p>
     */
    private class CrawlControl implements Runnable {
       
        @Override
        public void run() {
            CrawlProgressMessage cpm = null;
            try {
                cpm = heritrixController.getCrawlProgress();
            } catch (IOFailure e) {
                // Log a warning and retry
                log.warn("IOFailure while getting crawl progress", e);
                return;
            } catch (HarvestingAbort e) {
                log.warn("Got HarvestingAbort exception while getting crawl progress. Means crawl is over", e);
                crawlIsOver = true;
                return;
            }
            JMSConnectionFactory.getInstance().send(cpm);

            Heritrix3Files files = getHeritrixFiles();
            if (cpm.crawlIsFinished()) {
                log.info("Job ID {}: crawl is finished.", files.getJobID());
                crawlIsOver = true;
                return;
            }
            
            log.info("Job ID: " + files.getJobID() + ", Harvest ID: " + files.getHarvestID() + ", " + cpm.getHostUrl()
                    + "\n" + cpm.getProgressStatisticsLegend() + "\n" + cpm.getJobStatus().getStatus() + " "
                    + cpm.getJobStatus().getProgressStatistics());
        }

    }

}
