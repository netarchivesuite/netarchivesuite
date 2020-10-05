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

import org.archive.io.warc.WARCRecord;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.utils.archive.HeritrixArchiveRecordWrapper;

/**
 * A filter class for batch entries. Allows testing whether or not to process an entry without loading the entry data
 * first. The class in itself is abstract but contains implementation of several filters.
 */
@SuppressWarnings({"serial"})
public abstract class WARCBatchFilter implements Serializable {

    /** The name of the BatchFilter. */
    private String name;

    /** A default filter: Accepts everything. */
    public static final WARCBatchFilter NO_FILTER = new WARCBatchFilter("NO_FILTER") {
        public boolean accept(WARCRecord record) {
            return true;
        }
    };

    /** The name of the filter that filters out non response records. */
    private static final String EXCLUDE_NON_RESPONSE_RECORDS_FILTER_NAME = "EXCLUDE_NON_RESPONSE_RECORDS";

    /** A default filter: Accepts on response records. */
    public static final WARCBatchFilter EXCLUDE_NON_RESPONSE_RECORDS = new WARCBatchFilter(
            EXCLUDE_NON_RESPONSE_RECORDS_FILTER_NAME) {
        public boolean accept(WARCRecord record) {
            HeritrixArchiveRecordWrapper recordWrapper = new HeritrixArchiveRecordWrapper(record);
            String warcType = recordWrapper.getHeader().getHeaderStringValue("WARC-Type");
            return "response".equalsIgnoreCase(warcType);
        }
    };

    /** Prefix for the url in HTTP records. */
    private static final String HTTP_ENTRIES_HTTP_PREFIX = "http:";
    /** The name of the filter accepting only HTTP entries. */
    private static final String ONLY_HTTP_ENTRIES_FILTER_NAME = "ONLY_HTTP_ENTRIES";

    /**
     * Filter that only accepts records where the url starts with http.
     */
    public static final WARCBatchFilter ONLY_HTTP_ENTRIES = new WARCBatchFilter(ONLY_HTTP_ENTRIES_FILTER_NAME) {
        public boolean accept(WARCRecord record) {
            HeritrixArchiveRecordWrapper recordWrapper = new HeritrixArchiveRecordWrapper(record);
            return recordWrapper.getHeader().getUrl().startsWith(HTTP_ENTRIES_HTTP_PREFIX);
        }
    };

    /**
     * Create a new filter with the given name.
     *
     * @param name The name of this filter, for debugging mostly.
     */
    protected WARCBatchFilter(String name) {
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
     * Note that the mimetype of the WARC responserecord is not (necessarily) the same as its payload.
     *
     * @param mimetype String denoting the mimetype this filter represents
     * @return a BatchFilter that filters out all WARCRecords, that does not have this mimetype
     * @throws MimeTypeParseException If mimetype is invalid
     */
    public static WARCBatchFilter getMimetypeBatchFilter(final String mimetype) throws MimeTypeParseException {
        ArgumentNotValid.checkNotNullOrEmpty(mimetype, "String mimetype");
        if (!mimetypeIsOk(mimetype)) {
            throw new MimeTypeParseException("Mimetype argument '" + mimetype + "' is invalid");
        }
        return new WARCBatchFilter(MIMETYPE_BATCH_FILTER_NAME_PREFIX + mimetype) {
            public boolean accept(WARCRecord record) {
                HeritrixArchiveRecordWrapper recordWrapper = new HeritrixArchiveRecordWrapper(record);
                return recordWrapper.getHeader().getMimetype().startsWith(mimetype);
            }
        };
    }

    /** The name-prefix for mimetype filters. */
    private static final String MIMETYPE_BATCH_FILTER_NAME_PREFIX = "MimetypeBatchFilter-";
    /** Regexp for mimetypes. */
    private static final String MIMETYPE_REGEXP = "\\w+/\\w+";
    /** Pattern for mimetypes. */
    private static final Pattern MIMETYPE_PATTERN = Pattern.compile(MIMETYPE_REGEXP);

    /**
     * Check, if a certain mimetype is valid.
     *
     * @param mimetype a given mimetype
     * @return boolean true, if mimetype matches word/word, otherwise false
     */
    public static boolean mimetypeIsOk(String mimetype) {
        ArgumentNotValid.checkNotNullOrEmpty(mimetype, "String mimetype");
        return MIMETYPE_PATTERN.matcher(mimetype).matches();
    }

    /**
     * Check if a given record is accepted (not filtered out) by this filter.
     *
     * @param record a given WARCRecord
     * @return true, if the given record is accepted by this filter
     */
    public abstract boolean accept(WARCRecord record);

}
