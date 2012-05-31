package dk.netarkivet.common.utils.archive;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.archive.io.ArchiveRecord;
import org.archive.io.ArchiveRecordHeader;

public class HeritrixArchiveHeaderWrapper extends ArchiveHeaderBase {

	protected ArchiveRecordHeader header;

	protected Map<String, Object> headerFields = new HashMap<String, Object>();

	public static HeritrixArchiveHeaderWrapper wrapArchiveHeader(ArchiveRecord record) {
		HeritrixArchiveHeaderWrapper headerWrapper = new HeritrixArchiveHeaderWrapper();
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

	public String getDate() {
		return header.getDate();
	} 

	public long getLength() {
		return header.getLength();
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

	public String getMimetype() {
		return header.getMimetype();
	}

	public String getVersion() {
		return header.getVersion();
	}

	public long getOffset() {
		return header.getOffset();
	}

	public String getReaderIdentifier() {
		return header.getReaderIdentifier();
	}

	public String getRecordIdentifier() {
		return header.getRecordIdentifier();
	}

}
