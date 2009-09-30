/* File:        $Id: License.txt,v $
 * Revision:    $Revision: 1.4 $
 * Author:      $Author: csr $
 * Date:        $Date: 2005/04/11 16:29:16 $
 *
 * Copyright 2004-2009 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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

import java.io.OutputStream;
import java.util.regex.Pattern;
import org.archive.io.arc.ARCRecord;
import dk.netarkivet.common.utils.arc.ARCBatchJob;

/**
 * This batch batch job takes deduplication records from a crawl log in a
 * metadata arcfile and converts them to cdx records for use in wayback.
 *
 * @author csr
 * @since Aug 28, 2009
 */

public class ExtractDeduplicateCDXBatchJob extends ARCBatchJob {

    /**
     * A utility which has methods for converting a deduplicate crawl-log
     * entry to a CDX entry.
     */
    private DeduplicateToCDXAdapter adapter;

    /**
     * A regular expression representing the url in a metadata arcfile of a
     * crawl log entry
     */
    private static final String CRAWL_LOG_URL_PATTERN_STRING =
            "metadata://(.*)crawl[.]log(.*)";

    /**
     * A Patteren representing a compiled expression representing the url in
     * a metadata arcfile of a crawl log entry
     */
    private Pattern crawl_log_url_pattern;

    /**
     * Initializes various fields of this class
     * @param os unused parameter
     */
    @Override
    public void initialize(OutputStream os) {
        adapter = new DeduplicateToCDXAdapter();
        crawl_log_url_pattern = Pattern.compile(CRAWL_LOG_URL_PATTERN_STRING);
    }

    /**
     * If the ARCRecord is a crawl-log entry then any duplicate entries in the
     * crawl log are converted to CDX entries and written to the output.
     * Otherwise this method returns without doing anything.
     * @param record The ARCRecord to be processed
     * @param os the stream to which output is written
     */
    @Override
    public void processRecord(ARCRecord record, OutputStream os) {
        if (crawl_log_url_pattern
                .matcher(record.getMetaData().getUrl()).matches()) {
            adapter.adaptStream(record, os);
        } else {
           return;
        }

    }

    /**
     * Does nothing
     * @param os
     */
    @Override
    public void finish(OutputStream os) {
        //Nothing to finalise
    }
}
