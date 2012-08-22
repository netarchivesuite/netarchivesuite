package dk.netarkivet.common.utils.warc;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.archive.io.warc.WARCWriter;
import org.archive.util.anvl.ANVLRecord;

public class WARCWriterNAS extends WARCWriter {

    private static final Logger logger = 
            Logger.getLogger(WARCWriter.class.getName());

    /**
     * Constructor.
     * Takes a stream. Use with caution. There is no upperbound check on size.
     * Will just keep writing.  Only pass Streams that are bounded. 
     * @param serialNo  used to generate unique file name sequences
     * @param out Where to write.
     * @param f File the <code>out</code> is connected to.
     * @param cmprs Compress the content written.
     * @param a14DigitDate If null, we'll write current time.
     * @throws IOException
     */
    public WARCWriterNAS(final AtomicInteger serialNo,
    		final OutputStream out, final File f,
    		final boolean cmprs, final String a14DigitDate,
            final List<String> warcinfoData)
    throws IOException {
    	super(serialNo, out, f, cmprs, a14DigitDate, warcinfoData);
    }

    /**
     * Constructor.
     *
     * @param dirs Where to drop files.
     * @param prefix File prefix to use.
     * @param cmprs Compress the records written. 
     * @param maxSize Maximum size for ARC files written.
     * @param suffix File tail to use.  If null, unused.
     * @param warcinfoData File metadata for warcinfo record.
     */
    public WARCWriterNAS(final AtomicInteger serialNo,
    		final List<File> dirs, final String prefix, 
            final String suffix, final boolean cmprs,
            final long maxSize, final List<String> warcinfoData) {
        super(serialNo, dirs, prefix, suffix, cmprs, maxSize, warcinfoData);
    }

    @Override
    protected void writeRecord(final String type, final String url,
    		final String create14DigitDate, final String mimetype,
    		final URI recordId, ANVLRecord xtraHeaders,
            final InputStream contentStream, final long contentLength, boolean enforceLength)
    throws IOException {
    	if (!TYPES_LIST.contains(type)) {
    		throw new IllegalArgumentException("Unknown record type: " + type);
    	}
    	if (contentLength == 0 &&
                (xtraHeaders == null || xtraHeaders.size() <= 0)) {
    		throw new IllegalArgumentException("Cannot write record " +
    		    "of content-length zero and base headers only.");
    	}

    	String header;
    	try {
    		header = createRecordHeader(type, url,
    				create14DigitDate, mimetype, recordId, xtraHeaders,
    				contentLength);

    	} catch (IllegalArgumentException e) {
    		logger.log(Level.SEVERE,"could not write record type: " + type
    				+ "for URL: " + url, e);
    		return;
    	}

        try {
            preWriteRecordTasks();
            // TODO: Revisit endcoding of header.
            write(header.getBytes(WARC_HEADER_ENCODING));

            // Write out the header/body separator.
            write(CRLF_BYTES);

            if (contentStream != null && contentLength > 0) {
            	copyFrom(contentStream, contentLength, enforceLength);
            }

            // Write out the two blank lines at end of all records, per spec
            write(CRLF_BYTES);
            write(CRLF_BYTES);
        } finally {
            postWriteRecordTasks();
        }
    }

}
