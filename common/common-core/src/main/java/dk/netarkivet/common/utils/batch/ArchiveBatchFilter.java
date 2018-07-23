/*
 * #%L
 * Netarchivesuite - common
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
package dk.netarkivet.common.utils.batch;

import java.awt.datatransfer.MimeTypeParseException;
import java.io.Serializable;
import java.util.regex.Pattern;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.utils.archive.ArchiveRecordBase;

/**
 * A filter class for batch entries. Allows testing whether or not to process an entry without loading the entry data
 * first.
 * <p>
 * accept() is given an ArchiveRecord to avoid unnecessary reading and copying of data of records not accepted by
 * filter.
 */
@SuppressWarnings({"serial"})
public abstract class ArchiveBatchFilter implements Serializable {

    /** The name of the BatchFilter. */
    protected String name;

    /**
     * Create a new filter with the given name.
     *
     * @param name The name of this filter, for debugging mostly.
     */
    protected ArchiveBatchFilter(String name) {
        ArgumentNotValid.checkNotNullOrEmpty(name, "String name");
        this.name = name;
    }

    /**
     * Get the name of the filter.
     *
     * @return the name of the filter.
     */
    protected String getName() {
        return this.name;
    }

    /**
     * Check if a given record is accepted (not filtered out) by this filter.
     *
     * @param record a given archive record
     * @return true, if the given archive record is accepted by this filter
     */
    public abstract boolean accept(ArchiveRecordBase record);

    /** A default filter: Accepts everything. */
    public static final ArchiveBatchFilter NO_FILTER = new ArchiveBatchFilter("NO_FILTER") {
        @Override
        public boolean accept(ArchiveRecordBase record) {
            return true;
        }
    };

    /**
     * The ARCRecord url for the filedesc record (the header record of every ARC File).
     */
    private static final String ARC_FILE_FILEDESC_HEADER_PREFIX = "filedesc";

    /** The name of the filter that filters out the filedesc record and/or non-response records. */
    private static final String EXCLUDE_NON_RESPONSE_RECORDS_FILTER_NAME = "EXCLUDE_NON_RESPONSE_RECORDS";

    /** The name of the filter that filters out the filedesc record and/or non-warcinfo records */
    private static final String EXCLUDE_WARCINFO_AND_FILEDESC_RECORDS_FILTER_NAME = "EXCLUDE_WARCINFO_AND_FILEDESC_RECORDS";

    /** A default filter: Accepts only response records. */
    public static final ArchiveBatchFilter EXCLUDE_NON_RESPONSE_RECORDS = new ArchiveBatchFilter(
            EXCLUDE_NON_RESPONSE_RECORDS_FILTER_NAME) {
        @Override
        public boolean accept(ArchiveRecordBase record) {
            if (record.bIsArc) {
                return !record.getHeader().getUrl().startsWith(ARC_FILE_FILEDESC_HEADER_PREFIX);
            }
            if (record.bIsWarc) {
                String warcType = record.getHeader().getHeaderStringValue("WARC-Type");
                return "response".equalsIgnoreCase(warcType);
            }
            return false;
        }
    };

    /** A default filter: Accepts only response records. */
    public static final ArchiveBatchFilter EXCLUDE_NON_WARCINFO_RECORDS = new ArchiveBatchFilter(
            EXCLUDE_WARCINFO_AND_FILEDESC_RECORDS_FILTER_NAME) {
        @Override
        public boolean accept(ArchiveRecordBase record) {
            if (record.bIsArc) {
                return !record.getHeader().getUrl().startsWith(ARC_FILE_FILEDESC_HEADER_PREFIX);
            }
            if (record.bIsWarc) {
                String warcType = record.getHeader().getHeaderStringValue("WARC-Type");
                return !"warcinfo".equalsIgnoreCase(warcType);
            }
            return false;
        }
    };

    /** Prefix for the url in HTTP records. */
    private static final String EXCLUDE_HTTP_ENTRIES_HTTP_PREFIX = "http:";
    /** The name of the filter accepting only HTTP entries. */
    private static final String ONLY_HTTP_ENTRIES_FILTER_NAME = "ONLY_HTTP_ENTRIES";

    /**
     * Filter that only accepts records where the url starts with http.
     */
    public static final ArchiveBatchFilter ONLY_HTTP_ENTRIES = new ArchiveBatchFilter(ONLY_HTTP_ENTRIES_FILTER_NAME) {
        @Override
        public boolean accept(ArchiveRecordBase record) {
            return record.getHeader().getUrl().startsWith(EXCLUDE_HTTP_ENTRIES_HTTP_PREFIX);
        }
    };

    private static final String MIMETYPE_BATCH_FILTER_NAME_PREFIX = "MimetypeBatchFilter-";

    /**
     * Note that the mimetype of the WARC responserecord is not (necessarily) the same as its payload.
     *
     * @param mimetype String denoting the mimetype this filter represents
     * @return a BatchFilter that filters out all ARCRecords, that does not have this mimetype
     * @throws java.awt.datatransfer.MimeTypeParseException (if mimetype is invalid)
     */
    public static ArchiveBatchFilter getMimetypeBatchFilter(final String mimetype) throws MimeTypeParseException {
        if (!mimetypeIsOk(mimetype)) {
            throw new MimeTypeParseException("Mimetype argument '" + mimetype + "' is invalid");
        }
        return new ArchiveBatchFilter(MIMETYPE_BATCH_FILTER_NAME_PREFIX + mimetype) {
            @Override
            public boolean accept(ArchiveRecordBase record) {
                return record.getHeader().getMimetype().startsWith(mimetype);
            }
        };
    }

    /** Regexp for mimetypes. */
    private static final String MIMETYPE_REGEXP = "\\w+/\\w+";
    /** Pattern for mimetypes. */
    private static final Pattern MIMETYPE_PATTERN = Pattern.compile(MIMETYPE_REGEXP);

    /**
     * Check, if a certain mimetype is valid
     *
     * @param mimetype
     * @return boolean true, if mimetype matches word/word, otherwise false
     */
    public static boolean mimetypeIsOk(String mimetype) {
        return MIMETYPE_PATTERN.matcher(mimetype).matches();
    }

}
