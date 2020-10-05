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
import java.util.regex.Pattern;

import dk.netarkivet.common.utils.archive.ArchiveBatchJob;
import dk.netarkivet.common.utils.archive.ArchiveRecordBase;

/**
 * This batch batch job takes deduplication records from a crawl log in a metadata arcfile and converts them to cdx
 * records for use in wayback.
 */
@SuppressWarnings({"serial"})
public class DeduplicationCDXExtractionBatchJob extends ArchiveBatchJob {

    /**
     * A utility which has methods for converting a deduplicate crawl-log entry to a CDX entry.
     */
    private DeduplicateToCDXAdapter adapter;

    /**
     * A regular expression representing the url in a metadata arcfile of a crawl log entry.
     */
    private static final String CRAWL_LOG_URL_PATTERN_STRING = "metadata://(.*)crawl[.]log(.*)";

    /**
     * A Pattern representing a compiled expression representing the url in a metadata arcfile of a crawl log entry.
     */
    private Pattern crawlLogUrlPattern;

    /**
     * Initializes various fields of this class.
     *
     * @param os unused parameter
     */
    @Override
    public void initialize(OutputStream os) {
        adapter = new DeduplicateToCDXAdapter();
        crawlLogUrlPattern = Pattern.compile(CRAWL_LOG_URL_PATTERN_STRING);
    }

    /**
     * If the ArchiveRecord is a crawl-log entry then any duplicate entries in the crawl log are converted to CDX
     * entries and written to the output. Otherwise this method returns without doing anything. If the ArchiveRecord is
     * a WarcRecord, and the record is the warcinfo, the record is skipped.
     *
     * @param record The ArchiveRecord to be processed
     * @param os the stream to which output is written
     */
    @Override
    public void processRecord(ArchiveRecordBase record, OutputStream os) {
        if (record.bIsWarc && record.getHeader().getHeaderStringValue("warc-type").equalsIgnoreCase("warcinfo")) {
            // Skip the warc-info record
            return;
        }
        if (crawlLogUrlPattern.matcher(record.getHeader().getUrl()).matches()) {
            adapter.adaptStream(record.getInputStream(), os);
        } else {
            return;
        }
    }

    /**
     * Does nothing.
     *
     * @param os an outputstream
     */
    @Override
    public void finish(OutputStream os) {
        // Nothing to finalise
    }

}
