/* File:     $Id$
 * Revision: $Revision$
 * Author:   $Author$
 * Date:     $Date$
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Document;
import org.dom4j.Node;

import dk.netarkivet.common.Constants;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.utils.XmlUtils;
import dk.netarkivet.harvester.HarvesterSettings;
import dk.netarkivet.harvester.datamodel.HeritrixTemplate;

/**
 * A HeritrixLauncher object wraps around an instance of the web crawler
 * Heritrix. The object is constructed with the necessary information to do a
 * crawl. The crawl is performed when doOneCrawl() is called. doOneCrawl()
 * monitors progress and returns when the crawl is finished or must be stopped
 * because it has stalled.
 */
public class HeritrixLauncher {
    /** Class encapsulating placement of various files. */
    private HeritrixFiles files;

    /** the arguments passed to the HeritricController constructor. */
    private Object[] args;

    /** The CrawlController used. */
    private HeritrixController heritrixController;

    /**
     * The period to wait in milliseconds before checking if Heritrix has done
     * anything.
     */
    private static final int WAIT_PERIOD = 20000;

    //Attributes regarding deduplication.

    /** Xpath for the deduplicator index directory node in order.xml documents. */
    static final String DEDUPLICATOR_INDEX_LOCATION_XPATH
            = HeritrixTemplate.DEDUPLICATOR_XPATH
              + "/string[@name='index-location']";

    /**
     * Xpath for the boolean telling if the deduplicator is enabled in order.xml
     * documents.
     */
    static final String DEDUPLICATOR_ENABLED
            = HeritrixTemplate.DEDUPLICATOR_XPATH + "/boolean[@name='enabled']";

    /** The class logger. */
    final Log log = LogFactory.getLog(getClass());

    /** Number of milliseconds in a second. */
    private static final int MILLIS_PER_SECOND = 1000;
    /** How long to wait before aborting a request from a webserver. */
    private static long timeOutInMillisReceivedData =
            Long.parseLong(Settings.get(
                    HarvesterSettings.CRAWLER_TIMEOUT_NON_RESPONDING))
            * MILLIS_PER_SECOND;

    /** How long to wait without any activity before aborting the harvest. */
    private static long timeOutInMillis =
            Long.parseLong(Settings.get(
                    HarvesterSettings.INACTIVITY_TIMEOUT_IN_SECS))
            * MILLIS_PER_SECOND;
    /** Xpath for the 'disk-path' in the order.xml . */
    private static final String DISK_PATH_XPATH =
            "//crawl-order/controller"
            + "/string[@name='disk-path']";
    /** Xpath for the arcfile 'prefix' in the order.xml . */
    private static final String ARCFILE_PREFIX_XPATH =
            "//crawl-order/controller"
            + "/map[@name='write-processors']"
            + "/newObject/string[@name='prefix']";
    /** Xpath for the ARCs dir in the order.xml. */
    private static final String ARCSDIR_XPATH =
            "//crawl-order/controller"
            + "/map[@name='write-processors']"
            + "/newObject/stringList[@name='path']/string";
    /** Xpath for the 'seedsfile' in the order.xml. */
    private static final String SEEDS_FILE_XPATH =
            "//crawl-order/controller"
            + "/newObject[@name='scope']"
            + "/string[@name='seedsfile']";

    /**
     * Private HeritrixLaucher constructor. Sets up the HeritrixLauncher from
     * the given order file and seedsfile.
     *
     * @param files Object encapsulating location of Heritrix crawldir and
     *              configuration files.
     *
     * @throws ArgumentNotValid If either seedsfile or orderfile does not
     *                          exist.
     */
    private HeritrixLauncher(HeritrixFiles files)
            throws ArgumentNotValid {
        if (!files.getOrderXmlFile().isFile()) {
            throw new ArgumentNotValid(
                    "File '" + files.getOrderXmlFile().getName()
                    + "' must exist in order for Heritrix to run. "
                    + "This filepath does not refer to existing file: "
                    + files.getOrderXmlFile().getAbsolutePath());
        }
        if (!files.getSeedsTxtFile().isFile()) {
            throw new ArgumentNotValid(
                    "File '" + files.getSeedsTxtFile().getName()
                    + "' must exist in order for Heritrix to run. "
                    + "This filepath does not refer to existing file: "
                    + files.getSeedsTxtFile().getAbsolutePath());
        }
        this.files = files;
        this.args = new Object[]{files};
    }

    /**
     * Get instance of this class.
     *
     * @param files Object encapsulating location of Heritrix crawldir and
     *              configuration files
     *
     * @return HeritrixLauncher object
     *
     * @throws ArgumentNotValid If either order.xml or seeds.txt does not exist,
     *                          or argument files is null.
     */
    public static HeritrixLauncher getInstance(HeritrixFiles files)
            throws ArgumentNotValid {
        ArgumentNotValid.checkNotNull(files, "HeritrixFiles files");
        return new HeritrixLauncher(files);
    }

    /**
     * Generic constructor to allow HeritrixLauncher to use any implementation
     * of HeritrixController.
     *
     * @param args the arguments to be passed to the constructor or non-static
     *             factory method of the HeritrixController class specified in
     *             settings
     */
    public HeritrixLauncher(Object... args) {
        this.args = args;
    }

    /**
     * This method launches heritrix in the following way:</br> 1. copies the
     * orderfile and the seedsfile to current working directory. </br> 2. sets
     * up the newly created copy of the orderfile </br> 3. starts the crawler
     * </br> 4. stops the crawler (Either when heritrix has finished crawling,
     * or when heritrix is forcefully stopped due to inactivity). </p> The exit
     * from the while-loop depends on Heritrix calling the crawlEnded() method,
     * when the crawling is finished. This method is called from the
     * HarvestControllerServer.onDoOneCrawl() method.
     *
     * @throws IOFailure - if the order.xml is invalid if unable to initialize
     *                   Heritrix CrawlController if Heritrix process
     *                   interrupted
     */
    public void doCrawl() throws IOFailure {
        setupOrderfile();
        heritrixController
                = HeritrixControllerFactory.getDefaultHeritrixController(args);
        try {
            // Initialize Heritrix settings according to the order.xml
            heritrixController.initialize();
            log.debug("Starting crawl..");
            heritrixController.requestCrawlStart();
            if (heritrixController.atFinish()) {
                heritrixController.beginCrawlStop();
            } else {
                doCrawlLoop();
            }
        } catch (IOFailure e) {
            log.warn("Error during initialisation of crawl", e);
            throw (e);
        } catch (Exception e) {
            log.warn("Exception during crawl", e);
            throw new RuntimeException("Exception during crawl", e);
        } finally {
            if (heritrixController != null) {
                heritrixController.cleanup();
            }
        }
        log.debug("Heritrix is finished crawling...");
    }

    /**
     * Monitors the crawling performed by Heritrix. Regularly checks whether any
     * progress is made. If no progress has been made for too long, the crawl is
     * ended.
     *
     * @throws IOFailure if the call to HeritrixController.requestCrawlStop()
     *                   fails. Other failures in calls to the controller are
     *                   caught and logged.
     */
    private void doCrawlLoop() throws IOFailure {
        String errorMessage = "Non-fatal I/O error while communicating with"
                              + " Heritrix during crawl";
        long lastNonZeroActiveQueuesTime = System.currentTimeMillis();
        long lastTimeReceivedData = System.currentTimeMillis();
        boolean crawlIsEnded = false;
        try {
            crawlIsEnded = heritrixController.crawlIsEnded();
        } catch (IOFailure e) {
            log.debug(errorMessage, e);
        }
        while (!crawlIsEnded) {
            String harvestInformation = null;
            String progressStats = null;
            try {
                harvestInformation = heritrixController.getHarvestInformation();
                progressStats = heritrixController.getProgressStats();
            } catch (IOFailure e) {
                log.debug(errorMessage, e);
            }
            log.info("Job ID: " + files.getJobID()
                     + ", Harvest ID: " + files.getHarvestID()
                     + ", " + harvestInformation
                     + "\n"
                     + ((progressStats == null) ? "" : progressStats));
            // Note that we don't check for timeout while paused.
            int processedKBPerSec = 0;
            boolean paused = false;
            try {
                processedKBPerSec
                        = heritrixController.getCurrentProcessedKBPerSec();
                paused = heritrixController.isPaused();
            } catch (IOFailure e) {
                log.debug(errorMessage, e);
            }
            if (processedKBPerSec > 0 || paused) {
                lastTimeReceivedData = System.currentTimeMillis();
            }
            int activeToeCount = 0;
            paused = false;
            try {
                activeToeCount = heritrixController.getActiveToeCount();
                paused = heritrixController.isPaused();
            } catch (IOFailure e) {
                log.debug(errorMessage, e);
            }
            if (activeToeCount > 0 || paused) {
                lastNonZeroActiveQueuesTime = System.currentTimeMillis();
            }
            if ((lastNonZeroActiveQueuesTime + timeOutInMillis
                 < System.currentTimeMillis())
                || (lastTimeReceivedData + timeOutInMillisReceivedData
                    < System.currentTimeMillis())) {
                final double noActiveQueuesTimeoutInSeconds =
                        timeOutInMillis / 1000.0;
                final double noDataReceivedTimeoutInSeconds =
                        timeOutInMillisReceivedData / 1000.0;
                long queuedUriCount = 0;
                try {
                    queuedUriCount = heritrixController.getQueuedUriCount();
                } catch (IOFailure e) {
                    log.debug(errorMessage, e);
                }
                log.warn("Aborting crawl because of inactivity. "
                         + "No active queues for the last "
                         + ((System.currentTimeMillis()
                             - lastNonZeroActiveQueuesTime) / 1000.0)
                         + " seconds (timeout is "
                         + noActiveQueuesTimeoutInSeconds
                         + " seconds).  No traffic for the last "
                         + ((System.currentTimeMillis()
                             - lastTimeReceivedData) / 1000.0)
                         + " seconds (timeout is "
                         + noDataReceivedTimeoutInSeconds
                         + " seconds). URLs in queue:"
                         + queuedUriCount);
                // The following is the only controller command exception we
                // don't catch here. Otherwise we might loop forever.
                heritrixController.requestCrawlStop(
                        "Aborting because of inactivity");
            }

            //Optimization: don't wait if ended since beginning of the loop
            try {
                crawlIsEnded = heritrixController.crawlIsEnded();
            } catch (IOFailure e) {
                log.debug(errorMessage, e);
            }
            if (!crawlIsEnded) {
                try {
                    /* Wait for heritrix to do something.
                    * WAIT_PERIOD is the interval between checks of whether
                    * we have passed timeouts. Note that timeouts are defined
                    * in the settings, while WAIT_PERIOD (being less relevant
                    * to the user) is defined in this class.
                    */
                    synchronized (this) {
                        wait(WAIT_PERIOD);
                    }
                } catch (InterruptedException e) {
                    log.trace("Waiting thread awoken: " + e.getMessage());
                }
            }
        } // end of while (!crawlIsEnded)
    }

    /**
     * This method prepares the orderfile used by the Heritrix crawler. </p> 1.
     * alters the orderfile in the following-way: (overriding whatever is in the
     * orderfile)</br> <ol> <li>sets the disk-path to the outputdir specified in
     * HeritrixFiles.</li> <li>sets the seedsfile to the seedsfile specified in
     * HeritrixFiles.</li> <li>sets the prefix of the arcfiles to unique prefix
     * defined in HeritrixFiles</li> <li>checks that the arcs-file dir is 'arcs'
     * - to ensure that we know where the arc-files are when crawl
     * finishes</li>
     *
     * <li>if deduplication is enabled, sets the node pointing to index
     * directory for deduplication (see step 3)</li> </ol> 2. saves the
     * orderfile back to disk</p>
     *
     * 3. if deduplication is enabled in the order.xml, it writes the absolute
     * path of the lucene index used by the deduplication processor.
     *
     * @throws IOFailure - When the orderfile could not be saved to disk When a
     *                   specific node is not found in the XML-document When the
     *                   SAXReader cannot parse the XML
     */
    public void setupOrderfile() throws IOFailure {
        Document doc = XmlUtils.getXmlDoc(files.getOrderXmlFile());
        XmlUtils.setNode(doc, DISK_PATH_XPATH,
                         files.getCrawlDir().getAbsolutePath());

        XmlUtils.setNode(doc, ARCFILE_PREFIX_XPATH, files.getArcFilePrefix());

        XmlUtils.setNode(doc, SEEDS_FILE_XPATH,
                         files.getSeedsTxtFile().getAbsolutePath());
        XmlUtils.setNode(doc, ARCSDIR_XPATH, Constants.ARCDIRECTORY_NAME);

        if (isDeduplicationEnabledInTemplate(doc)) {
            XmlUtils.setNode(doc, DEDUPLICATOR_INDEX_LOCATION_XPATH,
                             files.getIndexDir().getAbsolutePath());
        }

        files.writeOrderXml(doc);
    }

    /**
     * Return true if the given order.xml file has deduplication enabled.
     *
     * @param doc An order.xml document
     *
     * @return True if Deduplicator is enabled.
     */
    public static boolean isDeduplicationEnabledInTemplate(Document doc) {
        ArgumentNotValid.checkNotNull(doc, "Document doc");
        Node xpathNode = doc.selectSingleNode(DEDUPLICATOR_ENABLED);
        return xpathNode != null
               && xpathNode.getText().trim().equals("true");

    }
}

