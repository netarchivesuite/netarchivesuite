package dk.netarkivet.common.utils.archive;

import java.io.File;
import java.util.Date;
import java.util.Map;
import java.util.Set;

/**
 * Utility class for presenting the same interface record header API for both
 * ARC and WARC record headers.
 */
public abstract class ArchiveHeaderBase {

    /** Is this record from an ARC file. */
    public boolean bIsArc;

    /** Is this record from a WARC file. */
    public boolean bIsWarc;

    /**
     * Return a header value object.
     * @param key header key
     * @return header value object
     */
    public abstract Object getHeaderValue(String key);

    /**
     * Return a header value string.
     * @param key header key
     * @return header value string
     */
    public abstract String getHeaderStringValue(String key);

    /**
     * Return a <code>Set</code> of header keys.
     * @return <code>Set</code> of header keys.
     */
    public abstract Set<String> getHeaderFieldKeys();

    /**
     * Return a <code>Map</code> of all header key/value pairs.
     * @return <code>Map</code> of all header key/value pairs.
     */
    public abstract Map<String, Object> getHeaderFields();

    /**
     * Return the header date as a <code>Date</code> object.
     * @return header date as a  <code>Date</code> object
     */
    public abstract Date getDate();

    /**
     * Return the header date in the ARC string format for use in CDX output.
     * @return header date in the ARC string format
     */
    public abstract String getArcDateStr();

    /**
     * Get the record length from the header.
     * @return the record length
     */
    public abstract long getLength();

    /**
     * Get the URL from the header.
     * @return the URL from the header
     */
    public abstract String getUrl();

    /**
     * Get the IP-Address from the header.
     * @return the IP-Address from the header
     */
    public abstract String getIp();

    /**
     * Get the content-type from the header and not the payload.
     * @return the content-type from the header
     */
    public abstract String getMimetype();

    /**
     * Get record version.
     * @return record version
     */
    public abstract String getVersion();

    /**
     * Get record offset.
     * @return record offset
     */
    public abstract long getOffset();

    /**
     * Return the reader identifier.
     * @return reader identifier
     */
    public abstract String getReaderIdentifier();

    /**
     * Return the record identifier.
     * @return record identifier
     */
    public abstract String getRecordIdentifier();

    /**
     * Return the archive <code>File</code< object.
     * @return archive <code>File</code< object
     */
    public abstract File getArchiveFile();

}
