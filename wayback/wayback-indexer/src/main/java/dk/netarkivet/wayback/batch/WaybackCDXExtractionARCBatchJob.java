/*
 * #%L
 * Netarchivesuite - wayback
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

package dk.netarkivet.wayback.batch;

import java.io.OutputStream;

import org.archive.io.arc.ARCRecord;
import org.archive.wayback.UrlCanonicalizer;
import org.archive.wayback.core.CaptureSearchResult;
import org.archive.wayback.resourceindex.cdx.SearchResultToCDXLineAdapter;
import org.archive.wayback.resourcestore.indexer.ARCRecordToSearchResultAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.Constants;
import dk.netarkivet.common.utils.arc.ARCBatchJob;
import dk.netarkivet.common.utils.batch.ARCBatchFilter;

/**
 * Returns a cdx file using the appropriate format for wayback, including canonicalisation of urls. The returned files
 * are unsorted.
 */
@SuppressWarnings({"deprecation", "serial"})
public class WaybackCDXExtractionARCBatchJob extends ARCBatchJob {

    /** Logger for this class. */
    private static final Logger log = LoggerFactory.getLogger(WaybackCDXExtractionARCBatchJob.class);

    /** Utility for converting an ArcRecord to a CaptureSearchResult (wayback's representation of a CDX record). */
    private ARCRecordToSearchResultAdapter aToSAdapter;

    /** Utility for converting a wayback CaptureSearchResult to a String representing a line in a CDX file. */
    private SearchResultToCDXLineAdapter srToCDXAdapter;

    /**
     * Constructor which set timeout to one day.
     */
    public WaybackCDXExtractionARCBatchJob() {
        batchJobTimeout = Constants.ONE_DAY_IN_MILLIES;
    }

    /**
     * Constructor.
     *
     * @param timeout specific timeout period
     */
    public WaybackCDXExtractionARCBatchJob(long timeout) {
        batchJobTimeout = timeout;
    }

    /**
     * Initializes the private fields of this class. Some of these are relatively heavy objects, so it is important that
     * they are only initialised once.
     *
     * @param os unused argument
     */
    @Override
    public void initialize(OutputStream os) {
        log.info("Starting a {}", this.getClass().getName());
        aToSAdapter = new ARCRecordToSearchResultAdapter();
        UrlCanonicalizer uc = UrlCanonicalizerFactory.getDefaultUrlCanonicalizer();
        aToSAdapter.setCanonicalizer(uc);
        srToCDXAdapter = new SearchResultToCDXLineAdapter();
    }

    /**
     * Does nothing except log the end of the job.
     *
     * @param os unused argument.
     */
    public void finish(OutputStream os) {
        log.info("Finishing the {}", this.getClass().getName());
        // No cleanup required
    }

    @Override
    public ARCBatchFilter getFilter() {
        return ARCBatchFilter.EXCLUDE_FILE_HEADERS;
    }

    /**
     * For each ARCRecord writes one CDX line (including newline) to the output. If an arcrecord cannot be converted to
     * a CDX record for any reason then any resulting exception is caught and logged.
     *
     * @param record the ARCRecord to be indexed.
     * @param os the OutputStream to which output is written.
     */
    @Override
    public void processRecord(ARCRecord record, OutputStream os) {
        CaptureSearchResult csr = null;
        log.debug("Entered {} for '{}'", this.getClass().getName(), record.getHeaderString());
        try {
            log.debug("Adapting Record '{}'", record.getHeader());
            csr = aToSAdapter.adapt(record);
            log.debug("Adapted Record '{}' to '{}'", record.getHeader(), csr);
        } catch (Exception e) {
            log.error("Exception processing ARC record:", e);
            return;
        }
        try {
            if (csr != null) {
                log.debug("Adapting Search Result'{}'", csr);
                String cdx = srToCDXAdapter.adapt(csr);
                os.write(cdx.getBytes());
                os.write("\n".getBytes());
                log.debug("Adapted Search Result '{}' + to '{}'", csr, cdx);
            } else {
                log.info("Could not parse '{}'", record.getHeaderString());
            }
        } catch (Exception e) {
            log.error("Exception processing ARC record:", e);
        }
    }

}
