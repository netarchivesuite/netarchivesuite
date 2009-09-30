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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Serializable;

import org.apache.commons.httpclient.URIException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.net.UURI;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.DomainUtils;
import dk.netarkivet.common.utils.FixedUURI;
import dk.netarkivet.harvester.datamodel.StopReason;
import dk.netarkivet.harvester.harvesting.distribute.DomainHarvestReport;
import dk.netarkivet.harvester.harvesting.distribute.DomainStats;

/**
 * Class responsible for generating a domain harvest report from
 * crawl logs created by
 * Heritrix and presenting the relevant information to clients.
 *
 */
public class HeritrixDomainHarvestReport extends DomainHarvestReport
    implements Serializable {

    /** The logger for this class. */
    private static final Log log = LogFactory.getLog(
            HeritrixDomainHarvestReport.class);
    /** The default reason why we stopped harvesting this domain.
     * This value is set by looking for a CRAWL ENDED in the crawl.log.
     */
    private StopReason defaultStopReason;

     /**
     * The constructor gets the data in a crawl.log file,
     * and parses the file. The crawl.log is described in the Heritrix
     * user-manual, section 8.2.1:
     * http://crawler.archive.org/articles/user_manual.html#logs
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
     * @param defaultStopReason the default stopreason
     * @throws IOFailure If unable to read reportFile
     */
    public HeritrixDomainHarvestReport(File reportFile, StopReason defaultStopReason) {
        ArgumentNotValid.checkNotNull(reportFile, "reportFile");
        this.defaultStopReason  = defaultStopReason;
        parseCrawlLog(reportFile);
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
                    log.debug(message, e);
                }
            }
        } catch (IOException e) {
            String msg = "Unable to open/read crawl.log file '"
                         + file.getAbsolutePath() + "'.";
            log.warn(msg, e);
            throw new IOFailure(msg, e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    log.debug("Unable to close " + file, e);
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
        DomainStats dhi = domainstats.get(domainName);
        if (dhi == null) {
            dhi = new DomainStats(0L, 0L, defaultStopReason);
            domainstats.put(domainName, dhi);
        }

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
