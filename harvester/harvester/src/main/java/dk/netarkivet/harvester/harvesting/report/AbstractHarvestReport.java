/* File:       $Id$
* Revision:    $Revision$
* Author:      $Author$
* Date:        $Date$
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
package dk.netarkivet.harvester.harvesting.report;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.httpclient.URIException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.net.UURI;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.DomainUtils;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.FixedUURI;
import dk.netarkivet.common.utils.StringUtils;
import dk.netarkivet.harvester.datamodel.Job;
import dk.netarkivet.harvester.datamodel.StopReason;
import dk.netarkivet.harvester.harvesting.ContentSizeAnnotationPostProcessor;
import dk.netarkivet.harvester.harvesting.HeritrixFiles;
import dk.netarkivet.harvester.harvesting.distribute.DomainStats;

/**
 * Base implementation for a harvest report.
 *
 */
public abstract class AbstractHarvestReport implements HarvestReport {

    /**
     * Strings found in the progress-statistics.log, used to devise the
     * default stop reason for domains.
     */
    public static enum ProgressStatisticsConstants {

        /**
         * String in crawl.log, that Heritrix writes
         *  as the last entry in the progress-statistics.log.
         */
        ORDERLY_FINISH("CRAWL ENDED"),

        /**
         * String which shows that the harvest was deliberately aborted from
         * the Heritrix GUI or forcibly stopped by the Netarchive Suite
         * software due to an inactivity timeout.
         */
        HARVEST_ABORTED("Ended by operator");
        
        /** The pattern associated with a given enum value. */
        private final String pattern;
        /**
         * Constructor for this enum class.
         * @param pattern The pattern associated with a given enum value.
         */
        ProgressStatisticsConstants(String pattern) {
            this.pattern = pattern;
        }
    }

    /** Datastructure holding the domain-information contained in one
     *  harvest.
     */
    private final Map<String, DomainStats> domainstats =
        new HashMap<String, DomainStats>();

    // Used at construction tile only, does not need to be serialized.
    private transient HeritrixFiles heritrixFiles;

    /**
     * The default reason why we stopped harvesting this domain.
     * This value is set by looking for a CRAWL ENDED in the crawl.log.
     */
    private StopReason defaultStopReason;

    /** The logger for this class. */
    private static final Log LOG =
        LogFactory.getLog(AbstractHarvestReport.class);


    /**
     * Default constructor that does nothing.
     * The real construction is supposed to be done
     * in the subclasses by filling out the domainStats map with crawl results.
     */
    public AbstractHarvestReport() {
    }

    /**
     * Constructor from Heritrix report files. Subclasses might use a different
     * set of Heritrix reports.
     * @param files the set of Heritrix reports.
     */
    public AbstractHarvestReport(HeritrixFiles files) {
        ArgumentNotValid.checkNotNull(files, "files");
        this.heritrixFiles = files;
        this.defaultStopReason = findDefaultStopReason(
                heritrixFiles.getProgressStatisticsLog());
        preProcess(heritrixFiles);
    }

    /**
     * Pre-processing happens when the report is built just at the end of the
     * crawl, before the ARC files upload.
     */
    @Override
    public void preProcess(HeritrixFiles files) {
        if (LOG.isInfoEnabled()) {
            LOG.info("Starting pre-processing of harvest report for job "
                    + files.getJobID());
        }
        long startTime = System.currentTimeMillis();

        File crawlLog = files.getCrawlLog();
        if (!crawlLog.isFile() || !crawlLog.canRead()) {
            String errorMsg = "Not a file or not readable: "
                + crawlLog.getAbsolutePath();
            throw new IOFailure(errorMsg);
        }
        parseCrawlLog(files.getCrawlLog());

        if (LOG.isInfoEnabled()) {
            long time = System.currentTimeMillis() - startTime;
            LOG.info("Finished pre-processing of harvest report for job "
                    + files.getJobID() + ", operation took "
                    + StringUtils.formatDuration(time));
        }
    }

    /**
     * Post-processing happens on the scheduler side when ARC files
     * have been uploaded.
     */
    @Override
    public abstract void postProcess(Job job);

    @Override
    public StopReason getDefaultStopReason() {
        return defaultStopReason;
    }

    /**
     * Returns the set of domain names
     * that are contained in hosts-report.txt
     * (i.e. host names mapped to domains)
     *
     * @return a Set of Strings
     */
    public final Set<String> getDomainNames() {
        return Collections.unmodifiableSet(domainstats.keySet());
    }

    /**
     * Get the number of objects found for the given domain.
     *
     * @param domainName A domain name (as given by getDomainNames())
     * @return How many objects were collected for that domain
     * @throws ArgumentNotValid if null or empty domainName
     */
    public final Long getObjectCount(String domainName) {
        ArgumentNotValid.checkNotNullOrEmpty(domainName, "domainName");
        final DomainStats domainStats = domainstats.get(domainName);
        if (domainStats != null) {
            return domainStats.getObjectCount();
        }
        return null;
    }

    /**
     * Get the number of bytes downloaded for the given domain.
     *
     * @param domainName A domain name (as given by getDomainNames())
     * @return How many bytes were collected for that domain
     * @throws ArgumentNotValid if null or empty domainName
     */
    public final Long getByteCount(String domainName) {
        ArgumentNotValid.checkNotNullOrEmpty(domainName, "domainName");
        final DomainStats domainStats = domainstats.get(domainName);
        if (domainStats != null) {
            return domainStats.getByteCount();
        }
        return null;
    }

    /**
     * Get the StopReason for the given domain.
     * @param domainName A domain name (as given by getDomainNames())
     * @return the StopReason for the given domain.
     * @throws ArgumentNotValid if null or empty domainName
     */
    public final StopReason getStopReason(String domainName) {
        ArgumentNotValid.checkNotNullOrEmpty(domainName, "domainName");
        final DomainStats domainStats = domainstats.get(domainName);
        if (domainStats != null) {
            return domainStats.getStopReason();
        }
        return null;
    }

    /**
     * @return the heritrixFiles
     */
    protected HeritrixFiles getHeritrixFiles() {
        return heritrixFiles;
    }

    /**
     * Attempts to get an already existing {@link DomainStats} object for that
     * domain, and if not found creates one with zero values.
     * @param domainName the name of the domain to get DomainStats for.
     * @return a DomainStats object for the given domain-name.
     */
    protected DomainStats getOrCreateDomainStats(String domainName) {
        DomainStats dhi = domainstats.get(domainName);
        if (dhi == null) {
            dhi = new DomainStats(0L, 0L, defaultStopReason);
            domainstats.put(domainName, dhi);
        }

        return dhi;
    }

    /**
     * Find out whether we stopped normally in progress statistics log.
     * @param logFile A progress-statistics.log file.
     * @return StopReason.DOWNLOAD_COMPLETE for progress statistics ending with
     * CRAWL ENDED, StopReason.DOWNLOAD_UNFINISHED otherwise or if file does
     * not exist.
     * @throws ArgumentNotValid on null argument.
     */
    public static StopReason findDefaultStopReason(File logFile)
            throws ArgumentNotValid {
        ArgumentNotValid.checkNotNull(logFile, "File logFile");
        if (!logFile.exists()) {
            return StopReason.DOWNLOAD_UNFINISHED;
        }
        String lastLine = FileUtils.readLastLine(logFile);
        if (lastLine.contains(
                ProgressStatisticsConstants.ORDERLY_FINISH.pattern)) {
            if (lastLine.contains(
                    ProgressStatisticsConstants.HARVEST_ABORTED.pattern)) {
               return StopReason.DOWNLOAD_UNFINISHED;
            } else {
               return StopReason.DOWNLOAD_COMPLETE;
            }
        } else {
            return StopReason.DOWNLOAD_UNFINISHED;
        }
    }

    /**
     * Computes the domain-name/byte-count and domain-name/object-count
     * and domain-name/stopreason maps
     * for a crawl.log.
     *
     * @param file the local file to pe processed
     * @throws IOFailure if there is problem reading the file
     */
    private void parseCrawlLog(File file) throws IOFailure {
        BufferedReader in = null;

        try {
            in = new BufferedReader(new FileReader(file));
            String line;
            int lineCnt = 0;
            while ((line = in.readLine()) != null) {
                ++lineCnt;
                try {
                    processHarvestLine(line);
                } catch (ArgumentNotValid e) {
                    final String message = "Invalid line in '"
                                           + file.getAbsolutePath()
                                           + "' line " + lineCnt + ": '"
                                           + line + "'. Ignoring.";
                    LOG.debug(message, e);
                }
            }
        } catch (IOException e) {
            String msg = "Unable to open/read crawl.log file '"
                         + file.getAbsolutePath() + "'.";
            LOG.warn(msg, e);
            throw new IOFailure(msg, e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    LOG.debug("Unable to close " + file, e);
                    // Can't throw here, as would destroy the real exception
                }
            }
        }
    }

    /**
     * Processes a harvest-line, updating the object and byte maps.
     *
     * @param line the line to process.
     */
    private void processHarvestLine(String line) throws ArgumentNotValid {
        //A legal crawl log line has at least 11 parts, + optional annotations
        final int MIN_CRAWL_LOG_PARTS = 11;
        final int MAX_PARTS = 12;
        final int ANNOTATION_PART_INDEX = 11;
        String[] parts = line.split("\\s+", MAX_PARTS);
        if (parts.length < MIN_CRAWL_LOG_PARTS) {
            throw new ArgumentNotValid(
                    "Not enough fields for line in crawl.log: '" + line + "'.");
        }

        //Get the domain name from the URL in the fourth field
        String hostName;
        // This errormessage is shared by the two exceptions thrown below.
        String errorMsg =
            "Unparsable URI in field 4 of crawl.log: '" + parts[3] + "'.";
        try {
            UURI uuri = new FixedUURI(parts[3], false);
            hostName = uuri.getReferencedHost();
        } catch (URIException e) {
            throw new ArgumentNotValid(errorMsg, e);
        }
        if (hostName == null) {
            throw new ArgumentNotValid(errorMsg);
        }
        String domainName;
        domainName = DomainUtils.domainNameFromHostname(hostName);

        //Get the response code for the URL in the second field
        long response;
        try {
            response = Long.parseLong(parts[1]);
        } catch (NumberFormatException e) {
            throw new ArgumentNotValid("Unparsable response code in field 2 of "
                                + "crawl.log: '" + parts[1] + "'.");
        }

        //Get the byte count from annotation field "content-size"
        //and the stop reason from annotation field if status code is -5003
        StopReason stopReason = getDefaultStopReason();
        long byteCounter = 0;
        if (parts.length > MIN_CRAWL_LOG_PARTS) { 
            // test if any annotations exist
            String[] annotations = parts[ANNOTATION_PART_INDEX].split(",");
            for (String annotation : annotations) {
                if (annotation.trim().startsWith(
                        ContentSizeAnnotationPostProcessor
                            .CONTENT_SIZE_ANNOTATION_PREFIX)) {
                    try {
                        byteCounter = Long.parseLong(annotation.substring(
                                ContentSizeAnnotationPostProcessor
                                    .CONTENT_SIZE_ANNOTATION_PREFIX.length()));
                    } catch (NumberFormatException e) {
                        throw new ArgumentNotValid("Unparsable annotation in "
                                            + "field 12 of crawl.log: '"
                                            + parts[ANNOTATION_PART_INDEX]
                                            + "'.", e);
                    }
                }
                if (response == CrawlURI.S_BLOCKED_BY_QUOTA) {
                    if (annotation.trim().equals("Q:group-max-all-kb")) {
                        stopReason = StopReason.SIZE_LIMIT;
                    } else if (annotation.trim()
                            .equals("Q:group-max-fetch-successes")) {
                        stopReason = StopReason.OBJECT_LIMIT;
                    }
                }
            }
        }

        //Update stats for domain
        DomainStats dhi = getOrCreateDomainStats(domainName);

        //Only count harvested URIs
        if (response >= 0) {
            long oldObjectCount = dhi.getObjectCount();
            dhi.setObjectCount(oldObjectCount + 1);
            long oldByteCount = dhi.getByteCount();
            dhi.setByteCount(oldByteCount + byteCounter);
        }
        //Only if reason not set
        if (dhi.getStopReason() == defaultStopReason) {
            dhi.setStopReason(stopReason);
        }
    }

}
