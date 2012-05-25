/* $Id: WARCUtils.java 2185 2011-11-21 17:29:22Z svc $
 * $Date: 2011-11-21 18:29:22 +0100 (ma, 21 nov 2011) $
 * $Revision: 2185 $
 * $Author: svc $
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2011 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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
package dk.netarkivet.common.utils.warc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;

import javax.mail.MethodNotSupportedException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.archive.io.ArchiveRecord;
import org.archive.io.ArchiveRecordHeader;
import org.archive.io.warc.WARCConstants;
import org.archive.io.warc.WARCReader;
import org.archive.io.warc.WARCReaderFactory;
import org.archive.io.warc.WARCRecord;
import org.archive.io.warc.WARCWriter;
import org.archive.util.ArchiveUtils;

import dk.netarkivet.common.Constants;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;

/**
* Various utilities on WARC-records.
* We have borrowed code from wayback.
* @see org.archive.wayback.resourcestore.indexer.WARCRecordToSearchResultAdapter.java
*/
public class WARCUtils {
    
    /** Logging output place. */
    protected static final Log log = LogFactory.getLog(WARCUtils.class);

    /**
     * Create new ARCWriter, writing to arcfile newFile.
     * @param newFile the ARCfile, that the ARCWriter writes to.
     * @return new ARCWriter, writing to arcfile newFile.
     */
    public static WARCWriter createWARCWriter(File newFile) {
        WARCWriter writer;
        PrintStream ps = null;
        try {
            ps = new PrintStream(new FileOutputStream(newFile));
            writer = new WARCWriter(
                    new AtomicInteger(), ps,
                    //This name is used for the first (file metadata) record
                    newFile, 
                    false, //Don't compress
                    //Use current time
                    ArchiveUtils.get14DigitDate(System.currentTimeMillis()),
                    null //No particular file metadata to add
            );
        } catch (IOException e) {
            if (ps != null) {
                ps.close();
            }
            String message = "Could not create WARCWriter to file '"
                    + newFile + "'.\n";
            log.warn(message);
            throw new IOFailure(message, e);
        }
        return writer;
    }

    /** Insert the contents of an ARC file (skipping an optional initial
     *  filedesc: header) in another ARCfile.
     *
     * @param arcFile An ARC file to read.
     * @param writer A place to write the arc records
     * @throws IOFailure if there are problems reading the file.
     */
    public static void insertWARCFile(File arcFile, WARCWriter writer) {
        ArgumentNotValid.checkNotNull(writer, "WARCWriter aw");
        ArgumentNotValid.checkNotNull(arcFile, "File warcFile");
        WARCReader r;

        try {
            r = WARCReaderFactory.get(arcFile);
        } catch (IOException e) {
            String message = "Error while copying ARC records from " + arcFile;
            log.warn(message, e);
            throw new IOFailure(message, e);
        }
        Iterator<ArchiveRecord> it = r.iterator();
        WARCRecord record;
        //it.next(); //Skip ARC file header
        // WARCReaderFactory guarantees the first record exists and is a
        // filedesc, or it would throw exception
        while (it.hasNext()) {
            record = (WARCRecord) it.next();
            copySingleRecord(writer, record);
        }
    }

    /**
     * Writes the given ARCRecord on the given ARCWriter.
     * 
     * Note that the ARCWriter.write method takes the metadata fields as
     * separate arguments instead of accepting an ARCRecordMetaData object. It
     * uses the ArchiveUtils.getDate method to convert an ARCstyle datestring to
     * a Date object.
     * 
     * @see ArchiveUtils#getDate(java.lang.String)
     * @param aw
     *            The ARCWriter to output the record on.
     * @param record
     *            The record to output
     */
    private static void copySingleRecord(WARCWriter aw, WARCRecord record) {
    	/*
        try {
            //Prepare metadata...
            ARCRecordMetaData meta = record.getMetaData();
            String uri = meta.getUrl();
            String mime = meta.getMimetype();
            String ip = meta.getIp();
            // Note the ArchiveUtils.getDate() converts an ARC-style datestring 
            // to a Date object
            long timeStamp = ArchiveUtils.getDate(meta.getDate()).getTime();
            //...and write the given files content into the writer
            // Note ARCRecord extends InputStream            
            aw.write(uri, mime, ip, timeStamp, meta.getLength(), record);
        } catch (Exception e) {
            throw new IOFailure("Error occurred while writing an ARC record"
                    + record, e);
        }
        */
    	throw new UnsupportedOperationException();
    }

    /**
     * Read the contents (payload) of an WARC record into a byte array.
     * 
     * @param record
     *            An WARC record to read from. After reading, the WARC Record 
     *            will no longer have its own data available for reading.
     * @return A byte array containing the payload of the WARC record. Note 
     *         that the size of the payload is calculated by subtracting
     *         the contentBegin value from the length of the record (both values
     *         included in the record header).
     * @throws IOFailure
     *             If there is an error reading the data, or if the record is
     *             longer than Integer.MAX_VALUE (since we can't make bigger
     *             arrays).
     */
    public static byte[] readWARCRecord(WARCRecord record) throws IOFailure {
        ArgumentNotValid.checkNotNull(record, "WARCRecord record");
        if (record.getHeader().getLength() > Integer.MAX_VALUE) {
            throw new IOFailure("WARC Record too long to fit in array: "
                    + record.getHeader().getLength() + " > "
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
            for (; (totalBytes < payloadLength)
                    && ((bytesRead = record.read(buffer)) != -1); totalBytes += bytesRead) {
                System.arraycopy(buffer, 0, tmpbuffer, totalBytes, bytesRead);
            }
        } catch (IOException e) {
            throw new IOFailure("Failure when reading the WARC-record", e);
        }
        
        // Check if the number of bytes read (= totalbytes) matches the
        // size of the buffer.
        if (tmpbuffer.length != totalBytes) {
            // make sure we only return an array with bytes we actualy read
            byte[] truncateBuffer = new byte[totalBytes];
            System.arraycopy(tmpbuffer, 0, truncateBuffer, 0, totalBytes);
            log.debug("Storing " + totalBytes + " bytes. Expected to store: "
                    + tmpbuffer.length);
            return truncateBuffer;
        } else {
            return tmpbuffer;
        }

    }
    
    /**
     * Find out what type of WARC-record this is.
     * @param record a given WARCRecord
     * @return the type of WARCRecord as a String.
     */
    public static String getRecordType(WARCRecord record) {
        ArchiveRecordHeader header = record.getHeader();
        return (String) header.getHeaderValue(WARCConstants.HEADER_KEY_TYPE);
    }
    
}
