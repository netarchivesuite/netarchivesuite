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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.archive.io.arc.ARCRecord;
import org.archive.io.arc.ARCRecordMetaData;

import dk.netarkivet.common.utils.ReadOnlyByteArray;


/**
 *
 * Class for reading a record in an ARC file.
 * This class wraps around an org.archive.io.arc.ARCRecord.
 * The main functionality added is that this class can
 * return InputStreams to the record object at will, allowing
 * multiple consumers to share the object.
 * A ShareableARCRecord can also tell which file the current
 * record was found in (useful for indexing).
 * 
 */
public class ShareableARCRecord {
    /** The stream to read data from. */
    private ARCRecord in;

    /** The file the record comes from. */
    private File file;

    /** Internal buffer for the content. */
    private byte[] objectBuffer;

    /**
     * Creates a ShareableARCRecord from an ARCRecord and
     * the File in which the ARCRecord was found.
     * @param record - an ARCRecord that the return object
     * should wrap around.  Note that this object does not close
     * the record, but holds on to the record.  The record can
     * be closed after readAll(), getDataReadOnly() or getObjectAsInputStream()
     * has been called.
     * @param fromFile - the ARC file in which the ARCRecord was
     * found.
     */
    public ShareableARCRecord(ARCRecord record, File fromFile) {
        if ((record == null) || (fromFile == null)) {
            throw new NullPointerException(
                "Null parameter passed to constructor");
        }

        file = fromFile;
        in = record;
    }

    /**
     * Returns the file that this ARC record comes from.
     * @return the file that this ARC record comes from.
     */
    public File getFile() {
        return file;
    }

    /**
     * Returns the metadata of this ARC record.
     * @return the metadata belonging to this ARC record.
     */
    public ARCRecordMetaData getMetaData() {
        return in.getMetaData();
    }

    /** Reads all the data on the internal input stream into objectBuffer,
     * if not already done.
     * @throws IOException if reading fails or if the length of the record
     * is greater than Integer.MAX_VALUE (since Java doesn't allow larger
     * arrays).
     */
    private synchronized void fillObjectBuffer() throws IOException {
        if (objectBuffer == null) {
            objectBuffer = ARCUtils.readARCRecord(in);
        }
    }

    /** Reads all the data in the ARCRecord's InputStream.
     * A "fresh copy" of the data is made, so the caller
     * is free to manipulate the returned array at will.
     * @return an array of bytes read from the ARC file.  This may be
     * quite large.
     * @throws IOException if reading fails
     */
    public byte[] readAll() throws IOException {
        fillObjectBuffer();

        byte[] result = new byte[objectBuffer.length];
        System.arraycopy(objectBuffer, 0, result, 0, objectBuffer.length);

        return result;
    }

    /** Reads all the data in the ARCRecord's InputStream.  The data is not
     * copied, but the call ensures that the data has been read into memory.
     * @return a read-only array of bytes read from the ARC file.  This may be
     * quite large.
     * @throws IOException if reading fails
     */
    public ReadOnlyByteArray getDataReadOnly() throws IOException {
        fillObjectBuffer();

        return new ReadOnlyByteArray(objectBuffer);
    }

    /**
     * Allows the caller to read the record object itself.
     * Multiple calls to this method return different InputStreams,
     * so that many calling objects may read without interference.
     * @return an InputStream representing the object in the ARCRecord.
     * @throws IOException if there are problems reading the object
     */
    public InputStream getObjectAsInputStream() throws IOException {
        fillObjectBuffer();

        return new ByteArrayInputStream(objectBuffer);
    }

    /**
     * Returns the ARC record around which the ShareableARCRecord is based.
     * Note that if record data are read from the ARCRecord, the
     * ShareableARCRecord is no longer able to fulfill its contracts,
     * as data will be gone from the inputstream.
     *
     * @return the ARCRecord for this ShareableARC record.
     */
    public ARCRecord getARCRecord() {
        return in;
    }
}
