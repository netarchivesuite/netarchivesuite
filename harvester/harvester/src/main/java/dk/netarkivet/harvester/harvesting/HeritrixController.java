/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2009 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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

import dk.netarkivet.common.exceptions.IOFailure;

/**
 * This interface encapsulates the direct access to Heritrix, allowing
 * for accessing in various ways (direct class access or JMX).  Heritrix is
 * expected to perform one crawl for each instance of an implementing class.
 *
 */
public interface HeritrixController {

    /** Initialize a new CrawlController for executing a Heritrix crawl.
     * This does not start the crawl.
     *
     */
    void initialize();

    /** Request that Heritrix start crawling.  When this method returns,
     * either Heritrix has failed in the early stages, or the crawljob has
     * been successfully created.  Actual crawling will commence at some
     * point hereafter.
     * @throws IOFailure If something goes wrong during startup.
     */
    void requestCrawlStart() throws IOFailure;

    /** Tell Heritrix to stop crawling.  Heritrix may take a while to actually
     * stop, so you cannot assume that crawling is stopped when this method
     * returns.
     */
    void beginCrawlStop();

    /** Request that crawling stops.
     * This makes a call to beginCrawlStop(), unless the crawler
     * is already shutting down. In that case it does nothing.
     *
     * @param reason A human-readable reason the crawl is being stopped.
     */
    void requestCrawlStop(String reason);

    /** Query whether Heritrix is in a state where it can finish crawling.
     *  Returns true if no uris remain to be harvested, or it has met
     *  either the maxbytes limit, the document limit,
     *  or the time-limit for the current harvest.
     *
     * @return True if Heritrix thinks it is time to stop crawling.
     */
    boolean atFinish();

    /** Returns true if the crawl has ended, either because Heritrix finished
     * or because we terminated it.
     *
     * @return True if the CrawlEnded event has happened in Heritrix,
     * indicating that all crawls have stopped.
     */
    boolean crawlIsEnded();

    /**
     * Get the number of currently active ToeThreads (crawler threads).
     * @return Number of ToeThreads currently active within Heritrix.
     */
    int getActiveToeCount();

    /** Get the number of URIs currently on the queue to be processed.  This
     * number may not be exact and should only be used in informal texts.
     *
     * @return How many URIs Heritrix have lined up for processing.
     */
    long getQueuedUriCount();

    /**
     * Get an estimate of the rate, in kb, at which documents
     * are currently being processed by the crawler.
     * @see org.archive.crawler.framework.StatisticsTracking#currentProcessedKBPerSec()
     * @return Number of KB data downloaded by Heritrix over an undefined
     * interval up to now.
     */
    int getCurrentProcessedKBPerSec();

    /** Get a human-readable set of statistics on the progress of the crawl.
     *  The statistics is
     *  discovered uris, queued uris, downloaded uris, doc/s(avg), KB/s(avg),
     *  dl-failures, busy-thread, mem-use-KB, heap-size-KB, congestion,
     *  max-depth, avg-depth.
     *  If no statistics are available, the string "No statistics available"
     *  is returned.
     *  Note: this method may disappear in the future.
     *
     * @return Some ascii-formatted statistics on the progress of the crawl.
     */
    String getProgressStats();

    /** Returns true if the crawler has been paused, and thus not
     * supposed to fetch anything.  Heritrix may still be fetching stuff, as
     * it takes some time for it to go into full pause mode.  This method can
     * be used as an indicator that we should not be worried if Heritrix
     * appears to be idle.
     *
     * @return True if the crawler has been paused, e.g. by using the
     * Heritrix GUI.
     */
    boolean isPaused();

    /** Release any resources kept by the class.
     */
    void cleanup();

    /**
     * Get harvest information. An example of this can be an URL pointing
     * to the GUI of a running Heritrix process.
     * @return information about the harvest process.
     */
    String getHarvestInformation();
}
