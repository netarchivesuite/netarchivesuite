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
package dk.netarkivet.harvester.harvesting.report;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.httpclient.URIException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.net.UURI;

import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.DomainUtils;
import dk.netarkivet.common.utils.FixedUURI;
import dk.netarkivet.common.utils.StringUtils;
import dk.netarkivet.harvester.datamodel.Domain;
import dk.netarkivet.harvester.datamodel.DomainDAO;
import dk.netarkivet.harvester.datamodel.HarvestInfo;
import dk.netarkivet.harvester.datamodel.Job;
import dk.netarkivet.harvester.datamodel.NumberUtils;
import dk.netarkivet.harvester.datamodel.StopReason;
import dk.netarkivet.harvester.harvesting.ContentSizeAnnotationPostProcessor;
import dk.netarkivet.harvester.harvesting.HeritrixFiles;
import dk.netarkivet.harvester.harvesting.distribute.DomainStats;

/**
 * Class responsible for generating a domain harvest report from
 * crawl logs created by
 * Heritrix and presenting the relevant information to clients.
 *
 */
public class LegacyHarvestReport extends AbstractHarvestReport {

    /** The logger for this class. */
    private static final Log LOG = LogFactory.getLog(
            LegacyHarvestReport.class);

    /**
     * The constructor gets the data in a crawl.log file,
     * and parses the file. The crawl.log is described in the Heritrix
     * user-manual, section 8.2.1:
     * http://crawler.archive.org/articles/user_manual/analysis.html#logs
     * Note: Invalid lines are logged and then ignored.
     *
     * Each url listed in the file is assigned to a domain,
     * the total object count and byte count per domain is calculated.
     * Finally, a StopReason is found for each domain:
     *  When the response is CrawlURI.S_BLOCKED_BY_QUOTA (
     *  currently = -5003), the StopReason is set to
     *  StopReason.SIZE_LIMIT,
     *      if the annotation equals "Q:group-max-all-kb"  or
     *  StopReason.OBJECT_LIMIT,
     *      if the annotation equals "Q:group-max-fetch-successes".
     *
     *
     * @param reportFile a crawl.log
     * @throws IOFailure If unable to read reportFile
     */
    public LegacyHarvestReport(HeritrixFiles hFiles) {
        super(hFiles);
    }

    public LegacyHarvestReport() {
        super();
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
                } catch (IOFailure e) {
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
    private void processHarvestLine(String line) {
        //A legal crawl log line has at least 11 parts, + optional annotations
        String[] parts = line.split("\\s+", 12);
        if (parts.length < 11) {
            throw new IOFailure("Not enough fields for line in crawl.log: '"
                    + line + "'.");
        }

        //Get the domain name from the URL in the fourth field
        String hostName;
        // This errormessage is shared by the two exceptions thrown below.
        String errorMsg = "Unparsable URI in field 4 of crawl.log: '"
            + parts[3] + "'.";
        try {
            UURI uuri = new FixedUURI(parts[3], false);
            hostName = uuri.getReferencedHost();
        } catch (URIException e) {
            throw new IOFailure(errorMsg);
        }
        if (hostName == null) {
            throw new IOFailure(errorMsg);
        }
        String domainName;
        domainName = DomainUtils.domainNameFromHostname(hostName);

        //Get the response code for the URL in the second field
        long response;
        try {
            response = Long.parseLong(parts[1]);
        } catch (NumberFormatException e) {
            throw new IOFailure("Unparsable response code in field 2 of "
                                + "crawl.log: '" + parts[1] + "'.");
        }

        //Get the byte count from annotation field "content-size"
        //and the stop reason from annotation field if status code is -5003
        StopReason defaultStopReason = getDefaultStopReason();
        StopReason stopReason = defaultStopReason;
        long byteCounter = 0;
        if (parts.length > 11) { // test if any annotations exist
            String[] annotations = parts[11].split(",");
            for (String annotation : annotations) {
                if (annotation.trim().startsWith(
                        ContentSizeAnnotationPostProcessor
                            .CONTENT_SIZE_ANNOTATION_PREFIX)) {
                    try {
                        byteCounter = Long.parseLong(annotation.substring(
                                ContentSizeAnnotationPostProcessor
                                    .CONTENT_SIZE_ANNOTATION_PREFIX.length()));
                    } catch (NumberFormatException e) {
                        throw new IOFailure("Unparsable annotation in "
                                            + "field 12 of crawl.log: '"
                                            + parts[11]
                                            + "'.");
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

    /**
     * Pre-processing happens when the report is built just at the end of the
     * crawl, before the ARC files upload.
     */
    @Override
    public void preProcess(HeritrixFiles files) {

        LOG.info("Starting pre-processing of harvest report for job "
                + files.getJobID());
        long startTime = System.currentTimeMillis();

        File crawlLog = files.getCrawlLog();
        if (! crawlLog.isFile() || ! crawlLog.canRead()) {
            String errorMsg = "Not a file or not readable: "
                              + crawlLog.getAbsolutePath();
            LOG.error(errorMsg);
            throw new IOFailure(errorMsg);
        }
        parseCrawlLog(files.getCrawlLog());

        long time = System.currentTimeMillis() - startTime;
        LOG.info("Finished pre-processing of harvest report for job "
                + files.getJobID() + ", operation took "
                + StringUtils.formatDuration(time));
    }

    /**
     * Post-processing happens on the scheduler side when ARC files
     * have been uploaded.
     */
    @Override
    public void postProcess(Job job) {

        LOG.info("Starting post-processing of harvest report for job "
                + job.getJobID());
        long startTime = System.currentTimeMillis();

        // Get the map from domain names to domain configurations
        Map<String, String> configurationMap = job.getDomainConfigurationMap();

        // For each domain harvested, check if it corresponds to a
        // domain configuration for this Job and if so add a new HarvestInfo
        // to the DomainHistory of the corresponding Domain object.
        // TODO:  Information about the domains harvested by the crawler
        // without a domain configuration for this job is deleted!
        // Should this information be saved in some way (perhaps stored
        // in metadata.arc-files?)

        final Set<String> domainNames = new HashSet<String>();
        domainNames.addAll(getDomainNames());
        domainNames.retainAll(configurationMap.keySet());
        final DomainDAO dao = DomainDAO.getInstance();
        for (String domainName : domainNames) {
            Domain domain = dao.read(domainName);

            // Retrieve crawl data from log and add it to HarvestInfo
            StopReason stopReason = getStopReason(domainName);
            long countObjectRetrieved = getObjectCount(domainName);
            long bytesReceived = getByteCount(domainName);

            //If StopReason is SIZE_LIMIT, we check if it's the harvests' size
            //limit, or rather a configuration size limit.

            //A harvest is considered to have hit the configuration limit if
            //1) The limit is lowest, or
            //2) The number of harvested bytes is greater than the limit

            // Note: Even though the per-config-byte-limit might have changed
            // between the time we calculated the job and now, it's okay we
            // compare with the new limit, since it gives us the most accurate
            // result for whether we want to harvest any more.
            if (stopReason == StopReason.SIZE_LIMIT) {
                long maxBytesPerDomain = job.getMaxBytesPerDomain();
                long configMaxBytes = domain.getConfiguration(
                        configurationMap.get(domainName)).getMaxBytes();
                if (NumberUtils.compareInf(configMaxBytes, maxBytesPerDomain)
                    <= 0
                    || NumberUtils.compareInf(configMaxBytes, bytesReceived)
                       <= 0) {
                    stopReason = StopReason.CONFIG_SIZE_LIMIT;
                }
            } else if (stopReason == StopReason.OBJECT_LIMIT) {
                long maxObjectsPerDomain = job.getMaxObjectsPerDomain();
                long configMaxObjects = domain.getConfiguration(
                        configurationMap.get(domainName)).getMaxObjects();
                if (NumberUtils.compareInf(configMaxObjects, maxObjectsPerDomain)
                    <= 0) {
                    stopReason = StopReason.CONFIG_OBJECT_LIMIT;
                }
            }
            // Create the HarvestInfo object
            HarvestInfo hi = new HarvestInfo(
                    job.getOrigHarvestDefinitionID(), job.getJobID(),
                    domain.getName(), configurationMap.get(domain.getName()),
                    new Date(), bytesReceived, countObjectRetrieved,
                    stopReason);

            // Add HarvestInfo to Domain and make data persistent
            // by updating DAO
            domain.getHistory().addHarvestInfo(hi);
            dao.update(domain);
        }

        long time = System.currentTimeMillis() - startTime;
        LOG.info("Finished post-processing of harvest report for job "
                + job.getJobID() + ", operation took "
                + StringUtils.formatDuration(time / 1000));

    }



}
