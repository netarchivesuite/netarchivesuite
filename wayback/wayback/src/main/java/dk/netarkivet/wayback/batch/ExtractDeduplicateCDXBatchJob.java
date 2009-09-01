/* File:        $Id: License.txt,v $
 * Revision:    $Revision: 1.4 $
 * Author:      $Author: csr $
 * Date:        $Date: 2005/04/11 16:29:16 $
 *
 * Copyright Det Kongelige Bibliotek og Statsbiblioteket, Danmark
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

    //private static final long serialVersionUID = 6791474836852341241L;
    private DeduplicateToCDXAdapter adapter;
    private static final String CRAWL_LOG_URL_PATTERN_STRING = "metadata://(.*)crawl[.]log(.*)";
    private Pattern crawl_log_url_pattern;

    public void initialize(OutputStream os) {
        adapter = new DeduplicateToCDXAdapter();
        crawl_log_url_pattern = Pattern.compile(CRAWL_LOG_URL_PATTERN_STRING);
    }

    public void processRecord(ARCRecord record, OutputStream os) {
        if (crawl_log_url_pattern.matcher(record.getMetaData().getUrl()).matches()) {            
            adapter.adaptStream(record, os);
        } else {
           return;
        }

    }

    public void finish(OutputStream os) {
        //Nothing to finalise
    }
}
