/* File:        $Id: WaybackCDXExtractionBatchJob.java 2566 2012-12-05 15:08:14Z svc $
 * Revision:    $Revision: 2566 $
 * Author:      $Author: svc $
 * Date:        $Date: 2012-12-05 16:08:14 +0100 (Wed, 05 Dec 2012) $
 *
 * Copyright 2004-2012 The Royal Danish Library, the Danish State and
 * University Library, the National Library of France and the Austrian
 * National Library.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 */

package dk.netarkivet.wayback.batch;

import java.io.IOException;
import java.io.OutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.archive.io.warc.WARCRecord;
import org.archive.wayback.UrlCanonicalizer;
import org.archive.wayback.core.CaptureSearchResult;
import org.archive.wayback.resourceindex.cdx.SearchResultToCDXLineAdapter;

import dk.netarkivet.common.Constants;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.batch.WARCBatchFilter;
import dk.netarkivet.common.utils.warc.WARCBatchJob;
import dk.netarkivet.wayback.batch.copycode.NetarchiveSuiteWARCRecordToSearchResultAdapter;

/**
 * Returns a cdx file using the appropriate format for wayback, including
 * canonicalisation of urls. The returned files are unsorted.
 *
 */
public class WaybackCDXExtractionWARCBatchJob extends WARCBatchJob {
   /**
     * Logger for this class.
     */
    private final Log log = LogFactory.getLog(getClass().getName());

    /**
     * Utility for converting an WArcRecord to a CaptureSearchResult
     * (wayback's representation of a CDX record).
     */
    private NetarchiveSuiteWARCRecordToSearchResultAdapter aToSAdapter;

    /**
     * Utility for converting a wayback CaptureSearchResult to a String
     * representing a line in a CDX file.
     */
    private SearchResultToCDXLineAdapter srToCDXAdapter;

     /**
     * Constructor which set timeout to one day.
     */
    public WaybackCDXExtractionWARCBatchJob() {
        batchJobTimeout = Constants.ONE_DAY_IN_MILLIES;
    }
    
    /** 
     * Set the filter, so only response records are 
     * currently processed.
     */
    @Override
    public WARCBatchFilter getFilter() {
        return WARCBatchFilter.EXCLUDE_NON_RESPONSE_RECORDS;
    }

    /**
     * Alternate constructor, where a timeout can be set.
     * @param timeout specific timeout period
     */
    public WaybackCDXExtractionWARCBatchJob(long timeout) {
        batchJobTimeout = timeout;
    }

    /**
     *  Initializes the private fields of this class. Some of these are
     *  relatively heavy objects, so it is important that they are only
     *  initialised once.
     * @param os unused argument
     */
    @Override
    public void initialize(OutputStream os) {
        log.info("Starting a " + this.getClass().getName());
        aToSAdapter = new NetarchiveSuiteWARCRecordToSearchResultAdapter();
        UrlCanonicalizer uc = UrlCanonicalizerFactory
                .getDefaultUrlCanonicalizer();
        aToSAdapter.setCanonicalizer(uc);
        srToCDXAdapter = new SearchResultToCDXLineAdapter();
    }

    /**
     * Does nothing except log the end of the job.
     * @param os unused argument.
     */
    public void finish(OutputStream os) {
        log.info("Finishing the " + this.getClass().getName());
        //No cleanup required
    }

    /**
     * For each response WARCRecord it writes one CDX line (including newline) to the output.
     * If an warcrecord cannot be converted to a CDX record for any reason then
     * any resulting exception is caught and logged.
     * @param record the WARCRecord to be indexed.
     * @param os the OutputStream to which output is written.
     */
    @Override
    public void processRecord(WARCRecord record, OutputStream os) {
        CaptureSearchResult csr = null;
        try {
            csr = aToSAdapter.adapt(record);
        } catch (Exception e) {
            log.warn(e);
        }
        try {
            if (csr != null) {
                os.write(srToCDXAdapter.adapt(csr).getBytes());
                os.write("\n".getBytes());
            }
        } catch (IOException e) {
            throw new IOFailure("Write error in batch job", e);
        } catch (Exception e) {
            log.warn(e);
        }
    }
}
