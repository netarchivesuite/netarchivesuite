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
package dk.netarkivet.common.utils.warc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.archive.format.warc.WARCConstants;
import org.archive.format.warc.WARCConstants.WARCRecordType;
import org.archive.io.ArchiveRecord;
import org.archive.io.ArchiveRecordHeader;
import org.archive.io.warc.WARCReader;
import org.archive.io.warc.WARCReaderFactory;
import org.archive.io.warc.WARCRecord;
import org.archive.io.warc.WARCRecordInfo;
import org.archive.io.warc.WARCWriter;
import org.archive.io.warc.WARCWriterPoolSettings;
import org.archive.io.warc.WARCWriterPoolSettingsData;
import org.archive.uid.UUIDGenerator;
import org.archive.util.anvl.ANVLRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.Constants;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.IllegalState;
import dk.netarkivet.common.utils.archive.ArchiveDateConverter;
import dk.netarkivet.common.utils.archive.HeritrixArchiveHeaderWrapper;

/**
 * Various utilities on WARC-records. We have borrowed code from wayback. See
 * org.archive.wayback.resourcestore.indexer.WARCRecordToSearchResultAdapter
 */
public class WARCUtils {

    /** Logging output place. */
    protected static final Logger log = LoggerFactory.getLogger(WARCUtils.class);

    /**
     * Create new WARCWriter, writing to warcfile newFile.
     *
     * @param newFile the WARCfile, that the WARCWriter writes to.
     * @return new WARCWriter, writing to warcfile newFile.
     */
    public static WARCWriter createWARCWriter(File newFile) {
        WARCWriter writer;
        PrintStream ps = null;
        try {
            ps = new PrintStream(new FileOutputStream(newFile));
            /*
            writer = new WARCWriter(new AtomicInteger(), ps,
            // This name is used for the first (file metadata) record
                    newFile, false, // Don't compress
                    // Use current time
                    ArchiveDateConverter.getWarcDateFormat().format(new Date()), null // No particular file metadata to
                                                                                      // add
            );
            */
            WARCWriterPoolSettings settings = new WARCWriterPoolSettingsData(
            		WARCConstants.WARC_FILE_EXTENSION, null, WARCConstants.DEFAULT_MAX_WARC_FILE_SIZE, false,
            		null, null, new UUIDGenerator());
            writer = new WARCWriter(new AtomicInteger(), ps, newFile, settings);
        } catch (IOException e) {
            if (ps != null) {
                ps.close();
            }
            String message = "Could not create WARCWriter to file '" + newFile + "'.\n";
            log.warn(message);
            throw new IOFailure(message, e);
        }
        return writer;
    }

    /**
     * Insert the contents of a WARC file into another WARCFile.
     *
     * @param warcFile An WARC file to read.
     * @param writer A place to write the arc records
     * @throws IOFailure if there are problems reading the file.
     */
    public static void insertWARCFile(File warcFile, WARCWriter writer) {
        ArgumentNotValid.checkNotNull(writer, "WARCWriter aw");
        ArgumentNotValid.checkNotNull(warcFile, "File warcFile");
        WARCReader r;

        try {
            r = WARCReaderFactory.get(warcFile);
        } catch (IOException e) {
            String message = "Error while copying WARC records from " + warcFile;
            log.warn(message, e);
            throw new IOFailure(message, e);
        }
        Iterator<ArchiveRecord> it = r.iterator();
        WARCRecord record;
        while (it.hasNext()) {
            record = (WARCRecord) it.next();
            copySingleRecord(writer, record);
        }
    }

    private static final Set<String> ignoreHeadersMap = new HashSet<String>();

    private static final Map<String, String> headerNamesCaseMap = new HashMap<String, String>();

    static {
        ignoreHeadersMap.add("content-type");
        ignoreHeadersMap.add("reader-identifier");
        ignoreHeadersMap.add("absolute-offset");
        ignoreHeadersMap.add("content-length");
        //ignoreHeadersMap.add("warc-date");
        ignoreHeadersMap.add("warc-record-id");
        ignoreHeadersMap.add("warc-type");
        ignoreHeadersMap.add("warc-target-uri");
        String[] headerNames = {"WARC-Type", "WARC-Record-ID", "WARC-Date", "Content-Length", "Content-Type",
                "WARC-Concurrent-To", "WARC-Block-Digest", "WARC-Payload-Digest", "WARC-IP-Address", "WARC-Refers-To",
                "WARC-Target-URI", "WARC-Truncated", "WARC-Warcinfo-ID", "WARC-Filename", "WARC-Profile",
                "WARC-Identified-Payload-Type", "WARC-Segment-Origin-ID", "WARC-Segment-Number",
                "WARC-Segment-Total-Length"};
        for (int i = 0; i < headerNames.length; ++i) {
            headerNamesCaseMap.put(headerNames[i].toLowerCase(), headerNames[i]);
        }
    }

    /**
     * Writes the given WARCRecord on the given WARCWriter.
     * <p>
     * Creates a new unique UUID for the copied record.
     *
     * @param aw The WARCWriter to output the record on.
     * @param record The record to output
     */
    private static void copySingleRecord(WARCWriter aw, WARCRecord record) {
        try {
            // Prepare metadata...
            HeritrixArchiveHeaderWrapper header = HeritrixArchiveHeaderWrapper.wrapArchiveHeader(null, record);
            String warcType = header.getHeaderStringValue("WARC-Type");

            String url = header.getUrl();
            Date date = header.getDate();
            String dateStr = ArchiveDateConverter.getWarcDateFormat().format(date);
            String mimetype = header.getMimetype();
            String recordIdStr;
            URI recordId;
            try {
                recordIdStr = header.getHeaderStringValue("warc-record-id");
                if (recordIdStr.startsWith("<") && recordIdStr.endsWith(">")) {
                    recordIdStr = recordIdStr.substring(1, recordIdStr.length() - 1);
                }
                recordId = new URI(recordIdStr);
            } catch (URISyntaxException e) {
                throw new IllegalState("Epic fail creating URI from UUID!");
            }

            //ANVLRecord namedFields = new ANVLRecord();

            // Copy to headers from the original WARC record to the new one.
            // Since we store the headers lowercase, we recase them.
            // Non WARC header header are lowercase and loose their case.
            /*
            Iterator<Entry<String, Object>> headerIter = header.getHeaderFields().entrySet().iterator();
            Entry<String, Object> headerEntry;
            String headerName;
            String headerNameCased;
            while (headerIter.hasNext()) {
                headerEntry = headerIter.next();
                if (!ignoreHeadersMap.contains(headerEntry.getKey())) {
                    headerName = headerEntry.getKey();
                    headerNameCased = headerNamesCaseMap.get(headerName);
                    if (headerNameCased != null) {
                        headerName = headerNameCased;
                    }
                    namedFields.addLabelValue(headerName, headerEntry.getValue().toString());
                }
            }
            */

            InputStream in = record;
            // getContentBegin only works for WARC and in H1.44.x!
            Long payloadLength = header.getLength() - record.getHeader().getContentBegin();

            WARCRecordType type = WARCRecordType.valueOf(warcType);
            WARCRecordInfo newRecord = new WARCRecordInfo();
            Iterator<Entry<String, Object>> headerIter = header.getHeaderFields().entrySet().iterator();
            Entry<String, Object> headerEntry;
            String headerName;
            String headerNameCased;
            while (headerIter.hasNext()) {
                headerEntry = headerIter.next();
                if (!ignoreHeadersMap.contains(headerEntry.getKey())) {
                    headerName = headerEntry.getKey();
                    headerNameCased = headerNamesCaseMap.get(headerName);
                    if (headerNameCased != null) {
                        headerName = headerNameCased;
                    }
                    newRecord.addExtraHeader(headerName, headerEntry.getValue().toString());
                }
            }
            newRecord.setType(type);
            newRecord.setUrl(url);
            newRecord.setMimetype(mimetype);
            newRecord.setRecordId(recordId);
            newRecord.setContentStream(in);
            newRecord.setContentLength(payloadLength);
        	aw.writeRecord(newRecord);

            // Write WARC record with type=warcType
        	/*
            if ("metadata".equals(warcType)) {
            	aw.writeMetadataRecord(url, dateStr, mimetype, recordId, namedFields, in, payloadLength);
            } else if ("request".equals(warcType)) {
                aw.writeRequestRecord(url, dateStr, mimetype, recordId, namedFields, in, payloadLength);
            } else if ("resource".equals(warcType)) {
                aw.writeResourceRecord(url, dateStr, mimetype, recordId, namedFields, in, payloadLength);
            } else if ("response".equals(warcType)) {
                aw.writeResponseRecord(url, dateStr, mimetype, recordId, namedFields, in, payloadLength);
            } else if ("revisit".equals(warcType)) {
                aw.writeRevisitRecord(url, dateStr, mimetype, recordId, namedFields, in, payloadLength);
            } else if ("warcinfo".equals(warcType)) {
                aw.writeWarcinfoRecord(dateStr, mimetype, recordId, namedFields, in, payloadLength);
            } else {
                throw new IOFailure("Unknown WARC-Type!");
            }
            */
        } catch (Exception e) {
            throw new IOFailure("Error occurred while writing an WARC record" + record, e);
        }
    }

    /**
     * Read the contents (payload) of an WARC record into a byte array.
     *
     * @param record An WARC record to read from. After reading, the WARC Record will no longer have its own data
     * available for reading.
     * @return A byte array containing the payload of the WARC record. Note that the size of the payload is calculated
     * by subtracting the contentBegin value from the length of the record (both values included in the record header).
     * @throws IOFailure If there is an error reading the data, or if the record is longer than Integer.MAX_VALUE (since
     * we can't make bigger arrays).
     */
    public static byte[] readWARCRecord(WARCRecord record) throws IOFailure {
        ArgumentNotValid.checkNotNull(record, "WARCRecord record");
        if (record.getHeader().getLength() > Integer.MAX_VALUE) {
            throw new IOFailure("WARC Record too long to fit in array: " + record.getHeader().getLength() + " > "
                    + Integer.MAX_VALUE);
        }
        // Calculate the length of the payload.
        // the size of the payload is calculated by subtracting
        // the contentBegin value from the length of the record.

        ArchiveRecordHeader header = record.getHeader();
        long length = header.getLength();

        int payloadLength = (int) (length - header.getContentBegin());

        // read from stream
        byte[] tmpbuffer = new byte[payloadLength];
        byte[] buffer = new byte[Constants.IO_BUFFER_SIZE];
        int bytesRead;
        int totalBytes = 0;
        try {
            for (; (totalBytes < payloadLength) && ((bytesRead = record.read(buffer)) != -1); totalBytes += bytesRead) {
                System.arraycopy(buffer, 0, tmpbuffer, totalBytes, bytesRead);
            }
        } catch (IOException e) {
            throw new IOFailure("Failure when reading the WARC-record", e);
        }

        // Check if the number of bytes read (= totalbytes) matches the
        // size of the buffer.
        if (tmpbuffer.length != totalBytes) {
            // make sure we only return an array with bytes we actually read
            byte[] truncateBuffer = new byte[totalBytes];
            System.arraycopy(tmpbuffer, 0, truncateBuffer, 0, totalBytes);
            log.debug("Storing {} bytes. Expected to store: {}", totalBytes, tmpbuffer.length);
            return truncateBuffer;
        } else {
            return tmpbuffer;
        }

    }

    /**
     * Find out what type of WARC-record this is.
     *
     * @param record a given WARCRecord
     * @return the type of WARCRecord as a String.
     */
    public static String getRecordType(WARCRecord record) {
        ArgumentNotValid.checkNotNull(record, "record");
        ArchiveRecordHeader header = record.getHeader();
        return (String) header.getHeaderValue(WARCConstants.HEADER_KEY_TYPE);
    }

    /**
     * Check if the given filename represents a WARC file.
     *
     * @param filename A given filename
     * @return true, if the filename ends with .warc or .warc.gz
     */
    public static boolean isWarc(String filename) {
        ArgumentNotValid.checkNotNullOrEmpty(filename, "filename");
        String lowercaseFilename = filename.toLowerCase();
        return (lowercaseFilename.endsWith(".warc") || lowercaseFilename.endsWith(".warc.gz"));
    }

}
