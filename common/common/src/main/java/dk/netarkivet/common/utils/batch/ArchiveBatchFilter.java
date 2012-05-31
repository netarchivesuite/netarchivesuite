package dk.netarkivet.common.utils.batch;

import java.awt.datatransfer.MimeTypeParseException;
import java.io.Serializable;
import java.util.regex.Pattern;

import org.archive.io.ArchiveRecord;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.NotImplementedException;
import dk.netarkivet.common.utils.archive.ArchiveRecordBase;
import dk.netarkivet.common.utils.archive.HeritrixArchiveRecordWrapper;

/** A filter class for batch entries.  Allows testing whether or not
 * to process an entry without loading the entry data first.
 *
 * accept() is given an ARCRecord rather than a ShareableARCRecord to
 * avoid unnecessary reading and copying of data of records
 * not accepted by filter.
 */
public abstract class ArchiveBatchFilter implements Serializable {

	/**
	 * UID.
	 */
	private static final long serialVersionUID = 1409369446818539939L;

    /** The name of the BatchFilter. */
    protected String name;

    /** Create a new filter with the given name.
     *
     * @param name The name of this filter, for debugging mostly.
     */
    protected ArchiveBatchFilter(String name) {
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
     * Check if a given record is accepted (not filtered out) by this filter.
     * @param record a given ARCRecord
     * @return true, if the given record is accepted by this filter
     */
    public abstract boolean accept(ArchiveRecordBase record);

    /** A default filter: Accepts everything. */
    public static final ArchiveBatchFilter NO_FILTER = new ArchiveBatchFilter("NO_FILTER") {
		private static final long serialVersionUID = 3394083354432326555L;
		public boolean accept(ArchiveRecordBase record) {
			return true;
		}
    };

    private static final String EXCLUDE_FILE_HEADERS_FILEDESC_PREFIX = "filedesc";
    private static final String EXCLUDE_FILE_HEADERS_FILTER_NAME = "EXCLUDE_FILE_HEADERS";

    /** A default filter: Accepts all but the first file */
    public static final ArchiveBatchFilter EXCLUDE_FILE_HEADERS = new ArchiveBatchFilter(
            EXCLUDE_FILE_HEADERS_FILTER_NAME) {
            public boolean accept(ArchiveRecordBase record) {
                //return !record.getMetaData().getUrl().startsWith(EXCLUDE_FILE_HEADERS_FILEDESC_PREFIX);
                String warcType = record.getHeader().getHeaderStringValue("WARC-Type");
            	return "response".equalsIgnoreCase(warcType);
            }
    };


    private static final String EXCLUDE_HTTP_ENTRIES_HTTP_PREFIX = "http:";
    private static final String ONLY_HTTP_ENTRIES_FILTER_NAME = "ONLY_HTTP_ENTRIES";

    public static final ArchiveBatchFilter ONLY_HTTP_ENTRIES = new ArchiveBatchFilter(
        ONLY_HTTP_ENTRIES_FILTER_NAME) {
        public boolean accept(ArchiveRecordBase record) {
            //return record.getMetaData().getUrl().startsWith(EXCLUDE_HTTP_ENTRIES_HTTP_PREFIX);
            throw new NotImplementedException("This filter has not yet been implemented");
        }
    };

    private static final String MIMETYPE_BATCH_FILTER_NAME_PREFIX = "MimetypeBatchFilter-";


    /**
     * @param mimetype String denoting the mimetype this filter represents
     * @return a BatchFilter that filters out all ARCRecords, that does not have this mimetype
     * @throws java.awt.datatransfer.MimeTypeParseException (if mimetype is invalid)
     */
    public static ArchiveBatchFilter getMimetypeBatchFilter(final String mimetype)
        throws MimeTypeParseException {
        if (!mimetypeIsOk(mimetype)) {
            throw new MimeTypeParseException("Mimetype argument '" + mimetype +
                "' is invalid");
        }
        return new ArchiveBatchFilter(MIMETYPE_BATCH_FILTER_NAME_PREFIX + mimetype) {
                public boolean accept(ArchiveRecordBase record) {
                    return record.getHeader().getMimetype().startsWith(mimetype);
                    //return record.getMetaData().getMimetype().startsWith(mimetype);
                }
            };
    }

    private static final String MIMETYPE_REGEXP = "\\w+/\\w+";
    private static final Pattern MIMETYPE_PATTERN = Pattern.compile(MIMETYPE_REGEXP);

    /**
     * Check, if a certain mimetype is valid
     * @param mimetype
     * @return boolean true, if mimetype matches word/word, otherwise false
     */
    public static boolean mimetypeIsOk(String mimetype) {
        return MIMETYPE_PATTERN.matcher(mimetype).matches();
    }

}
