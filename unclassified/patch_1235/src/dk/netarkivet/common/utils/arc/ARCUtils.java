/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2007 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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

package dk.netarkivet.common.utils.arc;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.archive.io.ArchiveRecord;
import org.archive.io.arc.ARCReader;
import org.archive.io.arc.ARCReaderFactory;
import org.archive.io.arc.ARCRecord;
import org.archive.io.arc.ARCRecordMetaData;
import org.archive.io.arc.ARCWriter;
import org.archive.util.ArchiveUtils;

import dk.netarkivet.common.Constants;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.SystemUtils;

/**
 * Various utilities that do stuff that ARCWriter does not provide.
 * Also includes method for converting an ARCRecord to a byte array.
 *
 * TODO: Turn this into a wrapper around ARCWriter instead.
 *
 */

public class ARCUtils {
    /** The log. */
    private static Log log = LogFactory.getLog(ARCUtils.class.getName());

    /** Insert the contents of an ARC file (skipping an optional initial
     *  filedesc: header) in another ARCfile.
     *
     * @param arcFile An ARC file to read.
     * @param aw A place to write the arc records
     * @throws IOFailure if there are problems reading the file.
     */
    public static void insertARCFile(File arcFile, ARCWriter aw) {
        ArgumentNotValid.checkNotNull(aw, "ARCWriter aw");
        ArgumentNotValid.checkNotNull(arcFile, "File arcFile");
        ARCReader r;

        try {
            r = ARCReaderFactory.get(arcFile);
        } catch (IOException e) {
            String message = "Error while copying ARC records from " + arcFile;
            log.warn(message, e);
            throw new IOFailure(message, e);
        }
        Iterator<ArchiveRecord> it = r.iterator();
        ARCRecord record = (ARCRecord) it.next(); //Skip ARC file header
        // ARCReaderFactory guarantees the first record exists and is a
        // filedesc, or it would throw exception
        while (it.hasNext()) {
            record = (ARCRecord) it.next();
            copySingleRecord(aw, record);
        }
    }

    /**
     * Writes the given ARCRecord on the given ARCWriter.
     * 
     * Note that the ARCWriter.write method takes the metadata fields as separate arguments 
     * instead of accepting an ARCRecordMetaData object.
     * It uses the ArchiveUtils.getDate method to convert an ARCstyle
     * datestring to a Date object.
     * @see ArchiveUtils#getDate(java.lang.String)
     * @param aw The ARCWriter to output the record on.
     * @param record The record to output
     */
    private static void copySingleRecord(ARCWriter aw, ARCRecord record) {
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
    }

    /**
     * Create new ARCWriter, writing to arcfile newFile.
     * @param newFile the ARCfile, that the ARCWriter writes to.
     * @return new ARCWriter, writing to arcfile newFile.
     */
    public static ARCWriter createARCWriter(File newFile) {
        ARCWriter aw = null;
        PrintStream ps = null;
        try {
            ps = new PrintStream(new FileOutputStream(newFile));
            aw = new ARCWriter(
                    new AtomicInteger(), ps,
                    newFile, //This name is used for the first (file metadata) record
                    false, //Don't compress
                    ArchiveUtils.get14DigitDate(System.currentTimeMillis()), //Use current time
                    null //No particular file metadata to add
            );
        } catch (IOException e) {
            if (ps != null) {
                ps.close();
            }
            String message = "Could not create ARCWriter to file '"
                    + newFile + "'.\n";
            log.warn(message);
            throw new IOFailure(message, e);
        }
        return aw;
    }

    /**
     * Write a file to an ARC file. The writing is done by
     * an existing ARCWriter.
     * An ARCRecord will be added, which contains a header and the contents
     * of the file. The date of the record written will be set to
     * the lastModified value of the file being written.
     * @param aw The ARCWriter doing the writing
     * @param file The file we want to write to the ARC file
     * @param uri The uri for the ARCRecord being written
     * @param mime The mimetype for the ARCRecord being written
     * @throws ArgumentNotValid if any arguments aw and file are null
     *  and arguments uri and mime are null or empty.
     */
    public static void writeFileToARC(ARCWriter aw, File file, String uri,
                                      String mime) {
        ArgumentNotValid.checkNotNull(aw, "ARCWriter aw");
        ArgumentNotValid.checkNotNull(file, "File file");
        ArgumentNotValid.checkNotNullOrEmpty(uri, "String uri");
        ArgumentNotValid.checkNotNullOrEmpty(mime, "String mime");
        
        InputStream is = null;
        try {
            try {
                //Prepare metadata...
                String ip = SystemUtils.getLocalIP();
                long timeStamp = file.lastModified();
                long length = file.length();
                //...and write the CDX file's content into the writer
                is = new FileInputStream(file);
                aw.write(uri, mime, ip, timeStamp, length, is);
            } finally {
                if (is != null) {
                    is.close();
                }
            }
        } catch (IOException e) {
            String msg = "Error writing '" + file + "' to "
                    + aw + " as " + uri;
            log.warn(msg, e);
            throw new IOFailure(msg, e);
        }
    }

    /**
     * Return an ARCWriter suitable for the tools ArcMerge and ArcWrap.
     * @param stream the given PrintStream.
     * @param destinationArcfile the given destination ARC file.
     * @return ARCWriter to be used by tools ArcMerge and ArcWrap
     * @throws IOException
     */
    public static ARCWriter getToolsARCWriter(PrintStream stream,
            File destinationArcfile) throws IOException {
        return
            new ARCWriter(new AtomicInteger(), stream,
                destinationArcfile,
                false, //Don't compress
                // Use current time
                ArchiveUtils.get14DigitDate(System.currentTimeMillis()),
                null // //No particular file metadata to add
                );
    }
    
    /** 
     * Read the contents of an ARC record into a byte array.
     *
     * @param in An ARC record to read from.  After reading, the ARC Record
     * will no longer have its own data available for reading.
     * @return A byte array containing the contents of the ARC record.  Note
     * that the size of this may be different from the size given in the
     * ARC record metadata.
     * @throws IOException If there is an error reading the data, or if the
     * record is longer than Integer.MAX_VALUE (since we can't make bigger
     * arrays).
     */
   public static byte[] readARCRecord(ARCRecord in) throws IOException {
       ArgumentNotValid.checkNotNull(in, "ARCRecord in");
       if (in.getMetaData().getLength() > Integer.MAX_VALUE) {
           throw new IOFailure("ARC Record too long to fit in array: "
                   + in.getMetaData().getLength() + " > " + Integer.MAX_VALUE);
       }
       // read from stream
       // The arcreader has a number of "features" that complicates the read
       //  1) the record at offset 0, returns too large a length
       //  2) readfully does not work
       //  3) ARCRecord.read(buf, offset, length) is broken.
       // TODO verify if these "features" are still around: See bugs #903, #904, #905
       int dataLength = (int) in.getMetaData().getLength();
       byte[] tmpbuffer = new byte[dataLength];
       byte[] buffer = new byte[Constants.IO_BUFFER_SIZE];
       int bytesRead;
       int totalBytes = 0;
       for (; (totalBytes < dataLength)
           && ((bytesRead = in.read(buffer)) != -1); totalBytes += bytesRead) {
           System.arraycopy(buffer, 0, tmpbuffer, totalBytes, bytesRead);
       }
       // Check if the number of bytes read (=i) matches the
       // size of the buffer.
       if (tmpbuffer.length != totalBytes) {
           // make sure we only return an array with bytes we actualy read
           byte[] truncateBuffer = new byte[totalBytes];
           System.arraycopy(tmpbuffer, 0, truncateBuffer, 0, totalBytes);
           return truncateBuffer;
       } else {
           return tmpbuffer;
       }
   }
}
