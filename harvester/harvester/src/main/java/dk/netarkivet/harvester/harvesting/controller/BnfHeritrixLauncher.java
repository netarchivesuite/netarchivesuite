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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.harvester.HarvesterSettings;
import dk.netarkivet.harvester.harvesting.HeritrixFiles;
import dk.netarkivet.harvester.harvesting.HeritrixLauncher;
import dk.netarkivet.harvester.harvesting.distribute.CrawlProgressMessage;
import dk.netarkivet.harvester.harvesting.monitor.HarvestMonitorServer;

/**
 * BnF specific Heritrix launcher, that forces the use of 
 * {@link BnfHeritrixController}. Every turn of the crawl control loop, asks the
 * Heritrix controller to generate a progress report as a 
 * {@link CrawlProgressMessage} and then send this message on the JMS bus to
 * be consulmed by the {@link HarvestMonitorServer} instance.
 */
public class BnfHeritrixLauncher extends HeritrixLauncher {

    /** The class logger. */
    final Log log = LogFactory.getLog(getClass());

    /** The CrawlController used. */
    private BnfHeritrixController heritrixController;

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
     * <li>Obtains carwl progress information as a {@link CrawlProgressMessage}
     * from the Heritrix controller</li>
     * <li>Sends the progress message via JMS</li>
     * <li>If the crawl if reported as finished, end loop.</li>
     * </ol>
     */
    public void doCrawl() throws IOFailure {
        setupOrderfile();
        heritrixController = new BnfHeritrixController(getHeritrixFiles());
        try {
            // Initialize Heritrix settings according to the order.xml
            heritrixController.initialize();
            log.debug("Starting crawl..");
            heritrixController.requestCrawlStart();

            while (true) {

                // First we wait the configured amount of time
                waitSomeTime();

                CrawlProgressMessage cpm;
                try {
                    cpm = heritrixController.getCrawlProgress();
                } catch (IOFailure iof) {
                    // Log a warning and retry
                    log.warn("IOFailure while getting crawl progress", iof);
                    continue;
                }

                getJMSConnection().send(cpm);

                if (cpm.crawlIsFinished()) {
                    // Crawl is over, exit the loop
                    break;
                }

                HeritrixFiles files = getHeritrixFiles();
                log.info("Job ID: " + files.getJobID() + ", Harvest ID: "
                        + files.getHarvestID() + ", " + cpm.getHostUrl() + "\n"
                        + cpm.getProgressStatisticsLegend() + "\n"
                        + cpm.getJobStatus().getStatus() + " "
                        + cpm.getJobStatus().getProgressStatistics());

            }

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
        log.debug("Heritrix is finished crawling...");
    }
}
