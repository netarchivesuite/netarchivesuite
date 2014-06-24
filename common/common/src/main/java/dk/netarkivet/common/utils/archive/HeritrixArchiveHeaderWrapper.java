package dk.netarkivet.common.utils.archive;

import java.io.File;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.archive.io.ArchiveRecord;
import org.archive.io.ArchiveRecordHeader;
import org.archive.io.arc.ARCRecord;
import org.archive.io.warc.WARCRecord;

import dk.netarkivet.common.exceptions.ArgumentNotValid;

/**
 * Heritrix wrapper implementation of the abstract archive header interface.
 */
@SuppressWarnings({ "unchecked"})
public class HeritrixArchiveHeaderWrapper extends ArchiveHeaderBase {

    /** The logger for this class. */
    private static final Log log = LogFactory.getLog(HeritrixArchiveHeaderWrapper.class);

    /** Reuse the sme WARC <code>DateFormat</code> object. */
    protected DateFormat warcDateFormat = ArchiveDateConverter.getWarcDateFormat();

    /** Reuse the same ARC <code>DateFormat</code> object. */
    protected DateFormat arcDateFormat = ArchiveDateConverter.getArcDateFormat();

    /** Wrapper Heritrix header. */
    protected HeritrixArchiveRecordWrapper recordWrapper;

    /** Original Heritrix header object. */
    protected ArchiveRecordHeader header;

    /** Map of header fields extracted from the Heritrix header.
     *  Only difference is that the keys are normalized to lower case. */
    protected Map<String, Object> headerFields = new HashMap<String, Object>();

    /**
     * Construct a Heritrix record header wrapper object.
     * @param recordWrapper wrapped Heritrix header
     * @param record original Heritrix record
     * @return wrapped Heritrix record header
     */
    public static HeritrixArchiveHeaderWrapper wrapArchiveHeader(HeritrixArchiveRecordWrapper recordWrapper, ArchiveRecord record) {
        //ArgumentNotValid.checkNotNull(recordWrapper, "recordWrapper");
        ArgumentNotValid.checkNotNull(record, "record");
        HeritrixArchiveHeaderWrapper headerWrapper = new HeritrixArchiveHeaderWrapper();
        headerWrapper.recordWrapper = recordWrapper;
        headerWrapper.header = record.getHeader();
        Map<String, Object> heritrixHeaderFields = (Map<String, Object>)headerWrapper.header.getHeaderFields();
        Iterator<Map.Entry<String, Object>> iter = heritrixHeaderFields.entrySet().iterator();
        Map.Entry<String, Object> entry;
        while (iter.hasNext()) {
            entry = iter.next();
            headerWrapper.headerFields.put(entry.getKey().toLowerCase(), entry.getValue());
        }
        if (record instanceof ARCRecord) {
            headerWrapper.bIsArc = true;
        } else if (record instanceof WARCRecord) {
            headerWrapper.bIsWarc = true;
        } else {
            throw new ArgumentNotValid(
                    "Unsupported ArchiveRecord type: "
                    + record.getClass().getName());
        }
        return headerWrapper;
    }

    @Override
    public Object getHeaderValue(String key) {
        return headerFields.get(key.toLowerCase());
    }

    @Override
    public String getHeaderStringValue(String key) {
        Object tmpObj = headerFields.get(key.toLowerCase());
        String str;
        if (tmpObj != null) {
            str = tmpObj.toString();
        } else {
            str = null;
        }
        return str;
    }

    @Override
    public Set<String> getHeaderFieldKeys() {
        return Collections.unmodifiableSet(headerFields.keySet());
    }

    @Override
    public Map<String, Object> getHeaderFields() {
        return Collections.unmodifiableMap(headerFields);
    }

    /*
     * The following fields do not need converting.
     */

    @Override
    public String getVersion() {
        return header.getVersion();
    }

    @Override
    public String getReaderIdentifier() {
        return header.getReaderIdentifier();
    }

    @Override
    public String getRecordIdentifier() {
        return header.getRecordIdentifier();
    }

    @Override
    public String getUrl() {
        return header.getUrl();
    }

    @Override
    public String getIp() {
        Object tmpObj = getHeaderValue("WARC-IP-Address");
        String ip;
        if (tmpObj != null) {
            ip = tmpObj.toString();
        } else {
            ip = null;
        }
        return ip;
    }

    @Override
    public long getOffset() {
        return header.getOffset();
    }

    @Override
    public long getLength() {
        return header.getLength();
    }

    /*
     * Conversion required.
     */

    @Override
    public Date getDate() {
        String dateStr = header.getDate();
        Date date = null;
        try {
            if (bIsArc) {
                date = arcDateFormat.parse(dateStr);
            } else if (bIsWarc) {
                date = warcDateFormat.parse(dateStr);
            }
        } catch (ParseException e) {
            log.info("Archive date could not be parsed" + dateStr + ".");
        }
        return date;
    } 

    @Override
    public String getArcDateStr() {
        String dateStr = header.getDate();
        if (bIsWarc) {
            try {
                Date warcDate = warcDateFormat.parse(dateStr);
                dateStr = arcDateFormat.format(warcDate);
                return dateStr;
            } catch (Exception e) {
                log.info("Archive date could not be parsed" + dateStr + ".");
            }
        }
        return dateStr;
    } 

    @Override
    public String getMimetype() {
        return header.getMimetype();
    }

    @Override
    public File getArchiveFile() {
        return new File(header.getReaderIdentifier());
    }

}
