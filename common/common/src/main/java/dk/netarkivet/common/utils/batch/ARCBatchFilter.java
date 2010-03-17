/* File:    $Id$
* Revision: $Revision$
* Author:   $Author$
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
package dk.netarkivet.common.utils.batch;

import org.archive.io.arc.ARCRecord;

import dk.netarkivet.common.exceptions.ArgumentNotValid;

import java.awt.datatransfer.MimeTypeParseException;
import java.io.Serializable;
import java.util.regex.Pattern;


/** A filter class for batch entries.  Allows testing whether or not
 * to process an entry without loading the entry data first.
 * The class in itself is abstract but contains implementation of several
 * filters.
 */
public abstract class ARCBatchFilter implements Serializable {
    
    /** The name of the BatchFilter. */
    private String name;
    
    /** A default filter: Accepts everything. */
    public static final ARCBatchFilter NO_FILTER = new ARCBatchFilter("NO_FILTER") {
            public boolean accept(ARCRecord record) {
                return true;
            }
        };
    
    /** The ARCRecord url for the filedesc record (the header record of every 
     * ARC File).
     */    
    private static final String FILE_HEADERS_FILEDESC_PREFIX
        = "filedesc";
    /** The name of the filter that filters out the filedesc record. */
    private static final String EXCLUDE_FILE_HEADERS_FILTER_NAME
        = "EXCLUDE_FILE_HEADERS";
    /** A default filter: Accepts all but the first file. */
    public static final ARCBatchFilter EXCLUDE_FILE_HEADERS = new ARCBatchFilter(
            EXCLUDE_FILE_HEADERS_FILTER_NAME) {
            public boolean accept(ARCRecord record) {
                return !record.getMetaData().getUrl().startsWith(
                        FILE_HEADERS_FILEDESC_PREFIX);
            }
        };

    /** Prefix for the url in HTTP records. */    
    private static final String HTTP_ENTRIES_HTTP_PREFIX = "http:";
    /** The name of th filter accepting only HTTP entries. */
    private static final String ONLY_HTTP_ENTRIES_FILTER_NAME
        = "ONLY_HTTP_ENTRIES";
    /**
     * Filter that only accepts records where the url starts with http.
     */
    public static final ARCBatchFilter ONLY_HTTP_ENTRIES = new ARCBatchFilter(
            ONLY_HTTP_ENTRIES_FILTER_NAME) {
            public boolean accept(ARCRecord record) {
                return record.getMetaData().getUrl().startsWith(
                        HTTP_ENTRIES_HTTP_PREFIX);
            }
        };
    
    /** The name-prefix for mimetype filters. */    
    private static final String MIMETYPE_BATCH_FILTER_NAME_PREFIX
        = "MimetypeBatchFilter-";
    /** Regexp for mimetypes. */
    private static final String MIMETYPE_REGEXP = "\\w+/\\w+";
    /** Pattern for mimetypes. */
    private static final Pattern MIMETYPE_PATTERN = Pattern.compile(
            MIMETYPE_REGEXP);

    /** Create a new filter with the given name.
     *
      * @param name The name of this filter, for debugging mostly.
     */
    protected ARCBatchFilter(String name) {
        ArgumentNotValid.checkNotNullOrEmpty(name, "String name");
        this.name = name;
    }

    /**
     * Get the name of the filter.
     * @return the name of the filter.
     */
    protected String getName() {
        return this.name;
    }
    
    /**
     * @param mimetype String denoting the mimetype this filter represents
     * @return a BatchFilter that filters out all ARCRecords, that does not 
     *  have this mimetype
     * @throws MimeTypeParseException If mimetype is invalid
     */
    public static ARCBatchFilter getMimetypeBatchFilter(final String mimetype)
        throws MimeTypeParseException {
        ArgumentNotValid.checkNotNullOrEmpty(mimetype, "String mimetype");
        if (!mimetypeIsOk(mimetype)) {
            throw new MimeTypeParseException("Mimetype argument '" + mimetype
                + "' is invalid");
        }

        return new ARCBatchFilter(MIMETYPE_BATCH_FILTER_NAME_PREFIX + mimetype) {
                public boolean accept(ARCRecord record) {
                    return record.getMetaData().getMimetype().startsWith(
                            mimetype);
                }
            };
    }

    /**
    * Check, if a certain mimetype is valid.
    * @param mimetype a given mimetype
    * @return boolean true, if mimetype matches word/word, otherwise false
    */
    public static boolean mimetypeIsOk(String mimetype) {
        ArgumentNotValid.checkNotNullOrEmpty(mimetype, "String mimetype");
        return MIMETYPE_PATTERN.matcher(mimetype).matches();
    }

    /**
     * Check if a given record is accepted (not filtered out) by this filter.
     * @param record a given ARCRecord
     * @return true, if the given record is accepted by this filter
     */
    public abstract boolean accept(ARCRecord record);
}
