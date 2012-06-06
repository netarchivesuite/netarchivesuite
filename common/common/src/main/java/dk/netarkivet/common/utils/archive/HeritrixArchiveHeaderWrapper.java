package dk.netarkivet.common.utils.archive;

import java.io.File;
import java.text.DateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.archive.io.ArchiveRecord;
import org.archive.io.ArchiveRecordHeader;

public class HeritrixArchiveHeaderWrapper extends ArchiveHeaderBase {

	protected HeritrixArchiveRecordWrapper recordWrapper;

	protected ArchiveRecordHeader header;

	protected Map<String, Object> headerFields = new HashMap<String, Object>();

	public static HeritrixArchiveHeaderWrapper wrapArchiveHeader(HeritrixArchiveRecordWrapper recordWrapper, ArchiveRecord record) {
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
        return headerWrapper;
	}

	public Object getHeaderValue(String key) {
		return headerFields.get(key.toLowerCase());
	}

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

	public Set<String> getHeaderFieldKeys() {
		return Collections.unmodifiableSet(headerFields.keySet());
	}

	public Map<String, Object> getHeaderFields() {
		return Collections.unmodifiableMap(headerFields);
	}

	/*
	 * The following fields do not need converting.
	 */

	public String getVersion() {
		return header.getVersion();
	}

	public String getReaderIdentifier() {
		return header.getReaderIdentifier();
	}

	public String getRecordIdentifier() {
		return header.getRecordIdentifier();
	}

	public String getUrl() {
		return header.getUrl();
	}

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

	public long getOffset() {
		return header.getOffset();
	}

	public long getLength() {
		return header.getLength();
	}

	/*
	 * Conversion required.
	 */

	public String getDate() {
		if (recordWrapper.bIsArc) {
			return header.getDate();
		} else if (recordWrapper.bIsWarc) {
			try {
				String dateStr = header.getDate();
				DateFormat warcDateFormat = ArchiveDateConverter.getWarcDateFormat();
				Date warcDate = warcDateFormat.parse(dateStr);
				DateFormat arcDateFormat = ArchiveDateConverter.getArcDateFormat();
				dateStr = arcDateFormat.format(warcDate);
				return dateStr;
			} catch (Exception e) {
				return null;
			}
		}
		return null;
	} 

	public String getMimetype() {
		return header.getMimetype();
	}

	public File getArchiveFile() {
		return new File(header.getReaderIdentifier());
	}

}
