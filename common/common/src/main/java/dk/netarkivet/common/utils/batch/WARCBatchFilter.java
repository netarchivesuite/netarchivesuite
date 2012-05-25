package dk.netarkivet.common.utils.batch;

import java.awt.datatransfer.MimeTypeParseException;
import java.io.Serializable;
import java.util.regex.Pattern;

import org.archive.io.warc.WARCRecord;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.utils.warc.HeritrixArchiveRecordHeader;

public abstract class WARCBatchFilter implements Serializable {

    /**
	 * UID.
	 */
	private static final long serialVersionUID = 1371298946366194533L;

	/** The name of the BatchFilter. */
    private String name;
    
    /** A default filter: Accepts everything. */
    public static final WARCBatchFilter NO_FILTER = new WARCBatchFilter("NO_FILTER") {
            public boolean accept(WARCRecord record) {
                return true;
            }
        };
    
    /** The WARCRecord url for the filedesc record (the header record of every 
     * ARC File).
     */    
    /*
    private static final String FILE_HEADERS_FILEDESC_PREFIX
        = "filedesc";
    */
    /** The name of the filter that filters out the filedesc record. */
    private static final String EXCLUDE_FILE_HEADERS_FILTER_NAME
        = "EXCLUDE_FILE_HEADERS";

    /** A default filter: Accepts all but the first file. */
    public static final WARCBatchFilter EXCLUDE_FILE_HEADERS = new WARCBatchFilter(
            EXCLUDE_FILE_HEADERS_FILTER_NAME) {
            public boolean accept(WARCRecord record) {
                HeritrixArchiveRecordHeader header = new HeritrixArchiveRecordHeader(record);
                String warcType = header.getHeaderStringValue("WARC-Type");
            	return "response".equalsIgnoreCase(warcType);
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
    /*
    public static final WARCBatchFilter ONLY_HTTP_ENTRIES = new WARCBatchFilter(
            ONLY_HTTP_ENTRIES_FILTER_NAME) {
            public boolean accept(WARCRecord record) {
                return record.getMetaData().getUrl().startsWith(
                        HTTP_ENTRIES_HTTP_PREFIX);
            }
        };
    */
    
    /** The name-prefix for mimetype filters. */    
    private static final String MIMETYPE_BATCH_FILTER_NAME_PREFIX
        = "MimetypeBatchFilter-";
    /** Regexp for mimetypes. */
    private static final String MIMETYPE_REGEXP = "\\w+/\\w+";
    /** Pattern for mimetypes. */
    private static final Pattern MIMETYPE_PATTERN = Pattern.compile(
            MIMETYPE_REGEXP);

    /** Create a new filter with the given name.
     * @param name The name of this filter, for debugging mostly.
     */
    protected WARCBatchFilter(String name) {
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
     * @return a BatchFilter that filters out all WARCRecords, that does not 
     *  have this mimetype
     * @throws MimeTypeParseException If mimetype is invalid
     */
    /*
    public static WARCBatchFilter getMimetypeBatchFilter(final String mimetype)
        throws MimeTypeParseException {
        ArgumentNotValid.checkNotNullOrEmpty(mimetype, "String mimetype");
        if (!mimetypeIsOk(mimetype)) {
            throw new MimeTypeParseException("Mimetype argument '" + mimetype
                + "' is invalid");
        }

        return new WARCBatchFilter(MIMETYPE_BATCH_FILTER_NAME_PREFIX + mimetype) {
                public boolean accept(WARCRecord record) {
                    return record.getMetaData().getMimetype().startsWith(
                            mimetype);
                }
            };
    }
    */

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
     * @param record a given WARCRecord
     * @return true, if the given record is accepted by this filter
     */
    public abstract boolean accept(WARCRecord record);

}
