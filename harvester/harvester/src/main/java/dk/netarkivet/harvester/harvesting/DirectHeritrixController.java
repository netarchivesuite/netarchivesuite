/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
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

package dk.netarkivet.harvester.harvesting;

import javax.management.InvalidAttributeValueException;
import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.archive.crawler.event.CrawlStatusListener;
import org.archive.crawler.framework.CrawlController;
import org.archive.crawler.framework.StatisticsTracking;
import org.archive.crawler.framework.exceptions.InitializationException;
import org.archive.crawler.settings.XMLSettingsHandler;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;

/** This class encapsulates one full run of Heritrix by grabbing hold of a
 * CrawlController class. It implements the CrawlController interface.
 * @deprecated The JMXHeritrixController offers an implementation that's
 * better on almost all counts.
 *  */
public class DirectHeritrixController implements HeritrixController {
    /**
     * Has Heritrix finished crawling, yet. This field is set in the method
     * SimpleCrawlStatusListener.crawlEnded()
     */
    private AtomicBoolean crawlIsEnded = new AtomicBoolean(false);

    /** the controller object, which initializes, starts, and stops
     * a Heritrix crawl job.
     */
    CrawlController myController = new CrawlController();

    /** The set of files that Heritrix needs Heritrix */
    private HeritrixFiles files;

    /** Create a new DirectHeritrixController object with a given set of files.
     *
     * @param files Files for Heritrix to use.
     */
    protected DirectHeritrixController(HeritrixFiles files) {
        ArgumentNotValid.checkNotNull(files, "HeritrixFiles files");
        this.files = files;
    }

    /**
     * @see HeritrixController#initialize()
     */
    public void initialize() {
        try {
            XMLSettingsHandler handler =
                    new XMLSettingsHandler(files.getOrderXmlFile());
            handler.initialize();
            myController.initialize(handler);
        } catch (InvalidAttributeValueException e) {
            throw new IOFailure("Not valid order.xml-file", e);
        } catch (InitializationException e) {
            throw new IOFailure(
                    "Could not initialize Heritrix CrawlController", e);
        }
        // Add a crawlstatus listener.
        SimpleCrawlStatusListener listener = new SimpleCrawlStatusListener();
        addCrawlStatusListener(listener);
    }

    /**
     * @see HeritrixController#requestCrawlStart()
     */
    public void requestCrawlStart() {
        try {
            myController.requestCrawlStart();
        } catch (Exception e) {
            throw new IOFailure("Error requesting crawl start", e);
        }
    }

    /**
     * @see HeritrixController#atFinish()
     */
    public boolean atFinish() {
        return myController.atFinish();
    }

    /**
     * @see HeritrixController#beginCrawlStop()
     */
    public void beginCrawlStop() {
        myController.beginCrawlStop();
    }

    /**
     * @see HeritrixController#getActiveToeCount()
     */
    public int getActiveToeCount() {
        return myController.getActiveToeCount();
    }

    /**
     * @see HeritrixController#requestCrawlStop(String)
     */
    public void requestCrawlStop(String reason) {
        myController.requestCrawlStop(reason);
    }

    /**
     * Add a listener to this crawlController.
     * This is currently only needed to known when the
     * crawler finished.
     * @param listener The listener for crawlstatus messages.
     * @see HeritrixController#crawlIsEnded()
     */
    public void addCrawlStatusListener(CrawlStatusListener listener) {
    	ArgumentNotValid.checkNotNull(listener, "listener");
        myController.addCrawlStatusListener(listener);
    }
    /** @see {@link HeritrixController#getQueuedUriCount()}*/
    public long getQueuedUriCount() {
        return myController.getFrontier().queuedUriCount();
    }

    /**
     * @see HeritrixController#getCurrentProcessedKBPerSec()
     */
    public int getCurrentProcessedKBPerSec() {
        if (myController.getStatistics() == null) {
            return 0;
        }
        return myController.getStatistics().currentProcessedKBPerSec();
    }

    /**
     * @see HeritrixController#getProgressStats()
     */
    public String getProgressStats() {
        final StatisticsTracking statistics = myController.getStatistics();
        if (statistics == null) {
            return "No statistics available";
        }
        return statistics.progressStatisticsLegend() + "\n"
                       + statistics.getProgressStatisticsLine();
    }

    /**
     * @see HeritrixController#isPaused()
     */
    public boolean isPaused() {
        return false;
    }

    /** Returns true if the crawl has ended, either because Heritrix finished
     * or because we terminated it.
     *
     * This implementation returns true,
     * after the CrawlController has ended a crawl and is about to exit, when
     * it sends a crawlEnded(String sExitMessage) to all listeners.
     *
     * @return True if Heritrix is entirely done and cleanup can start.
     */
    public boolean crawlIsEnded() {
        return crawlIsEnded.get();
    }

    /**
     * @see HeritrixController#cleanup()
     */
    public void cleanup() {
    }

    /**
     * This version just returns a string that tells the harvester is running
     * inline.
     * TODO Make this method respond after how the Harvester is really doing, and not
     * just respond ("Running inline"). 
     * @return running inline.
     */
    public String getHarvestInformation() {
        return "(running inline)";
    }

    /**
     * Class for handling callbacks from Heritrix. Except for logging, all that
     * happens is that - the constructor sets the value of crawlIsEnded to false
     * - crawlEnded() callback method sets the value of crawlIsEnded to true.
     * Note that the callbacks that occur are performed by a "foreign" thread,
     * initiated by Heritrix.
     * That is the reason that the crawlIsEnded field is an AtomicBoolean.
     *
     * @see org.archive.crawler.event.CrawlStatusListener
     */
    class SimpleCrawlStatusListener implements CrawlStatusListener {
        private Log log = LogFactory.getLog(SimpleCrawlStatusListener.class);

        /**
         * Sets the value of crawlIsEnded to false
         */
        public SimpleCrawlStatusListener() {
            crawlIsEnded.set(false);
        }

        /**
		 * Fired by the crawler, when the crawl has started.
		 *
		 * @param s Message to attach
		 * @see CrawlStatusListener#crawlStarted(java.lang.String)
		 */
        public void crawlStarted(String s) {
            log.debug("Crawl started: " + s);
        }

        /**
         * Fired by the crawler when the crawl is about to end (no reports
         * written yet....toe threads might still be running....).
         *
         * @param s Message to attach
         * @see CrawlStatusListener#crawlEnding(java.lang.String)
         */
        public void crawlEnding(String s) {
            log.debug("Crawl ending: " + s);
        }

        /**
         * Sets the value of crawlIsEnded to true
         *
         * Fired by the crawler when the crawl is ended.
         *
         * @param s Message to attach
         * @see CrawlStatusListener#crawlEnded(java.lang.String)
         */
        public void crawlEnded(String s) {
            log.info("Crawl ended: " + s);
            crawlIsEnded.set(true);
        }

        /**
         * Fired by the crawler when the crawl is about to pause (toe threads
         * might still be running.....).
         *
         * @param s Message to attach
         * @see CrawlStatusListener#crawlPausing(java.lang.String)
         */
        public void crawlPausing(String s) {
            log.debug("Crawl pausing: " + s);
        }

        /**
         * Fired by the crawler when the crawl is paused.
         *
         * @param s Message to attach
         * @see CrawlStatusListener#crawlPaused(java.lang.String)
         */
        public void crawlPaused(String s) {
            log.debug("Crawl paused: " + s);
        }

        /**
         * Fired by the crawler when the crawl is resuming.
         *
         * @param s Message to attach
         * @see CrawlStatusListener#crawlResuming(java.lang.String)
         */
        public void crawlResuming(String s) {
            log.debug("Crawl resuming: " + s);
        }

        /**
         * Called by CrawlController when checkpointing.  Allows checkpointing
         * of local data.
         *
         * @param checkpointDir Checkpoint dir. Write checkpoint state here.
         * @throws Exception A fatal exception. Any exceptions that are let out
         *                   of this checkpoint are assumed fatal and terminate
         *                   further checkpoint processing.
         * @see CrawlStatusListener#crawlCheckpoint(java.io.File)
         */
        public void crawlCheckpoint(File checkpointDir) throws Exception {
            log.debug("Crawl checkpoint to " + checkpointDir);
            // TODO: Should we store some of the parent class' information here?
        }
    }
}
