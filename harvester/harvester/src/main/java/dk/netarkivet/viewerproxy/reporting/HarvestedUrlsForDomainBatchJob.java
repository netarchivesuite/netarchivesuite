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

package dk.netarkivet.viewerproxy.reporting;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;

import org.archive.io.arc.ARCRecord;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.DomainUtils;
import dk.netarkivet.common.utils.FixedUURI;
import dk.netarkivet.common.utils.arc.ARCBatchJob;

/**
 * Batchjob that extracts lines referring to a specific domain from a crawl log.
 * The batch job should be restricted to run on metadata files for a specific
 * job only, using the {@link #processOnlyFilesMatching(String)} construct.
 */
public class HarvestedUrlsForDomainBatchJob extends ARCBatchJob {
    /** Metadata URL for crawl logs. */
    private static final String SETUP_URL_FORMAT
            = "metadata://netarkivet.dk/crawl/logs/crawl.log";

    /** The domain to extract crawl.log lines for. */
    final String domain;

    /**
     * Initialise the batch job.
     *
     * @param domain The domain to get crawl.log lines for.
     */
    public HarvestedUrlsForDomainBatchJob(String domain) {
        this.domain = domain;
    }

    /**
     * Does nothing, no initialisation is needed.
     * @param os Not used.
     */
    public void initialize(OutputStream os) {
    }

    /**
     * Process a record in a file. Will do nothing, unless the record is a
     * crawl log. If the record is a crawl log, copy lines concerning the given
     * domain to result.
     * @param record The record to process.
     * @param os The output stream for the result.
     *
     * @throws ArgumentNotValid on null parameters
     * @throws IOFailure on trouble processing the record.
     */
    public void processRecord(ARCRecord record, OutputStream os) {
        ArgumentNotValid.checkNotNull(record, "ARCRecord record");
        ArgumentNotValid.checkNotNull(os, "OutputStream os");
        if (!record.getHeader().getUrl().startsWith(SETUP_URL_FORMAT)) {
            return;
        }
        BufferedReader arcreader
                = new BufferedReader(new InputStreamReader(record));
        try {
            for(String line = arcreader.readLine(); line != null;
                line = arcreader.readLine()) {
                String[] parts = line.split("\\s+");
                if (parts.length > 3 && DomainUtils.domainNameFromHostname(
                        new FixedUURI(parts[3], true).getReferencedHost()).equals(domain)) {
                    os.write(line.getBytes("UTF-8"));
                    os.write('\n');
                } else if (parts.length > 5 && !parts[5].equals("-")
                           && DomainUtils.domainNameFromHostname(
                        new FixedUURI(parts[5], true).getReferencedHost()).equals(domain)) {
                    os.write(line.getBytes("UTF-8"));
                    os.write('\n');
                }
            }
        } catch (IOException e) {
            throw new IOFailure("Unable to process arc record", e);
        }
    }

    /**
     * Does nothing, no finishing is needed.
     * @param os Not used.
     */
    public void finish(OutputStream os) {
    }
}
