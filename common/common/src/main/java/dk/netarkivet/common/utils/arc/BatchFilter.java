/* File:                 $Id$
* Revision:         $Revision$
* Author:                $Author$
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
package dk.netarkivet.common.utils.arc;

import org.archive.io.arc.ARCRecord;

import java.awt.datatransfer.MimeTypeParseException;
import java.io.Serializable;
import java.util.regex.Pattern;


/** A filter class for batch entries.  Allows testing whether or not
 * to process an entry without loading the entry data first.
 *
 * accept() is given an ARCRecord rather than a ShareableARCRecord to
 * avoid unnecessary reading and copying of data of records
 * not accepted by filter.
 */
public abstract class BatchFilter implements Serializable {
    /** A default filter: Accepts everything */
    public static final BatchFilter NO_FILTER = new BatchFilter("NO_FILTER") {
            public boolean accept(ARCRecord record) {
                return true;
            }
        };

    private static final String EXCLUDE_FILE_HEADERS_FILEDESC_PREFIX = "filedesc";
    private static final String EXCLUDE_FILE_HEADERS_FILTER_NAME = "EXCLUDE_FILE_HEADERS";
    /** A default filter: Accepts all but the first file */
    public static final BatchFilter EXCLUDE_FILE_HEADERS = new BatchFilter(
            EXCLUDE_FILE_HEADERS_FILTER_NAME) {
            public boolean accept(ARCRecord record) {
                return !record.getMetaData().getUrl().startsWith(EXCLUDE_FILE_HEADERS_FILEDESC_PREFIX);
            }
        };

    private static final String EXCLUDE_HTTP_ENTRIES_HTTP_PREFIX = "http:";
    private static final String ONLY_HTTP_ENTRIES_FILTER_NAME = "ONLY_HTTP_ENTRIES";
    public static final BatchFilter ONLY_HTTP_ENTRIES = new BatchFilter(
            ONLY_HTTP_ENTRIES_FILTER_NAME) {
            public boolean accept(ARCRecord record) {
                return record.getMetaData().getUrl().startsWith(EXCLUDE_HTTP_ENTRIES_HTTP_PREFIX);
            }
        };

    private static final String MIMETYPE_BATCH_FILTER_NAME_PREFIX = "MimetypeBatchFilter-";

    private static final String MIMETYPE_REGEXP = "\\w+/\\w+";
    private static final Pattern MIMETYPE_PATTERN = Pattern.compile(MIMETYPE_REGEXP);

    /** Create a new filter with the given name
     *
      * @param name The name of this filter, for debugging mostly.
     */
    protected BatchFilter(String name) {
        /* TODO: Either use the name or remove it. */
    }

    /**
     * @param mimetype String denoting the mimetype this filter represents
     * @return a BatchFilter that filters out all ARCRecords, that does not have this mimetype
     * @throws java.awt.datatransfer.MimeTypeParseException (if mimetype is invalid)
     */
    public static BatchFilter getMimetypeBatchFilter(final String mimetype)
        throws MimeTypeParseException {
        if (!mimetypeIsOk(mimetype)) {
            throw new MimeTypeParseException("Mimetype argument '" + mimetype +
                "' is invalid");
        }

        return new BatchFilter(MIMETYPE_BATCH_FILTER_NAME_PREFIX + mimetype) {
                public boolean accept(ARCRecord record) {
                    return record.getMetaData().getMimetype().startsWith(mimetype);
                }
            };
    }

    /**
    * Check, if a certain mimetype is valid
    * @param mimetype
    * @return boolean true, if mimetype matches word/word, otherwise false
    */
    public static boolean mimetypeIsOk(String mimetype) {
        return MIMETYPE_PATTERN.matcher(mimetype).matches();
    }

    public abstract boolean accept(ARCRecord record);
}
