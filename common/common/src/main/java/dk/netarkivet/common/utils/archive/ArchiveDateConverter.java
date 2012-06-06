package dk.netarkivet.common.utils.archive;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

public class ArchiveDateConverter {

    /** ARC date format string as speficied in the ARC documentation. */
	public static final String ARC_DATE_FORMAT = "yyyyMMddHHmmss";

	/** WARC date format string as specified by the WARC ISO standard. */
	public static final String WARC_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";

	/** ARC <code>DateFormat</code> as specified in the ARC documentation. */
    private final DateFormat arcDateFormat;

    /** WARC <code>DateFormat</code> as speficied in the WARC ISO standard. */
    private final DateFormat warcDateFormat;

    /** Basic <code>DateFormat</code> is not thread safe. */
    private static final ThreadLocal<ArchiveDateConverter> DateParserTL =
        new ThreadLocal<ArchiveDateConverter>() {
        public ArchiveDateConverter initialValue() {
            return new ArchiveDateConverter();
        }
    };

    /**
     * Creates a new <code>DateParser</code>.
     */
    private ArchiveDateConverter() {
        arcDateFormat = new SimpleDateFormat(ARC_DATE_FORMAT);
        arcDateFormat.setLenient(false);
        arcDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        warcDateFormat = new SimpleDateFormat(WARC_DATE_FORMAT);
        warcDateFormat.setLenient(false);
        warcDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    public static DateFormat getArcDateFormat() {
    	return DateParserTL.get().arcDateFormat;
    }

    public static DateFormat getWarcDateFormat() {
    	return DateParserTL.get().warcDateFormat;
    }

}
