/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2012 The Royal Danish Library, the Danish State and
 * University Library, the National Library of France and the Austrian
 * National Library.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
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

import org.archive.io.ArchiveRecord;
import org.archive.io.ArchiveRecordHeader;
import org.archive.io.arc.ARCRecord;
import org.archive.io.warc.WARCRecord;

import dk.netarkivet.common.exceptions.ArgumentNotValid;

public class HeritrixArchiveHeaderWrapper extends ArchiveHeaderBase {

	protected DateFormat warcDateFormat = ArchiveDateConverter.getWarcDateFormat();

	protected DateFormat arcDateFormat = ArchiveDateConverter.getArcDateFormat();

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
			// TODO maybe log?
		}
		return date;
	} 

	public String getArcDateStr() {
		if (bIsArc) {
			return header.getDate();
		} else if (bIsWarc) {
			try {
				String dateStr = header.getDate();
				Date warcDate = warcDateFormat.parse(dateStr);
				dateStr = arcDateFormat.format(warcDate);
				return dateStr;
			} catch (Exception e) {
				// TODO maybe log?
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
