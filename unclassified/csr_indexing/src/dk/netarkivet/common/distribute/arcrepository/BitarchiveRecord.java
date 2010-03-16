/* $Id$
 * $Date$
 * $Revision$
 * $Author$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2009 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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
package dk.netarkivet.common.distribute.arcrepository;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.archive.io.arc.ARCRecord;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.distribute.RemoteFile;
import dk.netarkivet.common.distribute.RemoteFileFactory;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.IllegalState;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.utils.arc.ARCUtils;

/**
 * Class to hold the result of a lookup operation in the bitarchive:
 *    The metadata information associated with the record
 *    The actual byte content
 *    The name of the file the data were retrieved from
 *    If length of record exceeds value of
 *    Settings.BITARCHIVE_LIMIT_FOR_RECORD_DATATRANSFER_IN_FILE
 *    The record is stored in a RemoteFile.
 *
 */
public class BitarchiveRecord implements Serializable {

    /** The file the data were retrieved from. */
    private String fileName;

    /** The actual data. */
    private byte[] objectBuffer;

    /** The offset of the ARCRecord contained. */
    private long offset;

    /** The length of the ARCRecord contained. */
    private long length;

    /** The actual data as a remote file.*/
    private RemoteFile objectAsRemoteFile;

    /** Is the data stored in a RemoteFile. */
    private boolean isStoredAsRemoteFile = false;
    
    /** Set after deleting RemoteFile. */
    private boolean hasRemoteFileBeenDeleted = false;
    
    /** How large the ARCRecord can before saving as RemoteFile. */
    private final long LIMIT_FOR_SAVING_DATA_IN_OBJECT_BUFFER
        = Settings.getLong(CommonSettings
            .BITARCHIVE_LIMIT_FOR_RECORD_DATATRANSFER_IN_FILE);

    /** the log. */
    private static final transient Log log
        = LogFactory.getLog(BitarchiveRecord.class.getName());

    /**
     * Creates a BitarchiveRecord from the a ARCRecord.
     * 
     * The filename of the original ARC is read from the ARCRecord itself.
     * If the length of the record is higher than Settings
     * .BITARCHIVE_LIMIT_FOR_RECORD_DATATRANSFER_IN_FILE
     *  the data is stored in a RemoteFile, otherwise the data is stored in
     *  a byte array.
     * @param record the ARCRecord that the data should come from.  We do not
     * close the ARCRecord.
   */
    public BitarchiveRecord(ARCRecord record) {
        ArgumentNotValid.checkNotNull(record, "ARCRecord record");
        fileName = record.getMetaData().getArcFile().getName();
        offset = record.getMetaData().getOffset();
        length = record.getMetaData().getLength();
        if (length > LIMIT_FOR_SAVING_DATA_IN_OBJECT_BUFFER) {
            // copy arc-data to local file and create a RemoteFile based on this
            log.info("ARCRecord exceeds limit of "
                    + LIMIT_FOR_SAVING_DATA_IN_OBJECT_BUFFER
                    + " bytes. Length is " + length
                    + " bytes, Storing as "
                    + Settings.get(CommonSettings.REMOTE_FILE_CLASS));
            File localTmpFile = null;
            try {
                localTmpFile = File.createTempFile("BitarchiveRecord-"
                        + fileName, ".tmp", FileUtils.getTempDir());
                record.dump(new FileOutputStream(localTmpFile));
                objectAsRemoteFile = RemoteFileFactory.getMovefileInstance(
                        localTmpFile);
                isStoredAsRemoteFile = true;
            } catch (IOException e) {
                throw new IOFailure("Unable to store record(" + fileName
                        + "," + offset + ") as remotefile", e);
            }
        } else { // Store data in objectbuffer
            try {
                objectBuffer = ARCUtils.readARCRecord(record);
                log.debug("Bytes stored in objectBuffer: "
                        + objectBuffer.length);
            } catch (IOException e) {
                throw new ExceptionInInitializerError(e);
            }
        }
    }

    /**
     * Returns the file that this information was loaded from.
     * @return the file that this ARC record comes from.
     */
    public String getFile() {
        return fileName;
    }

    /**
     * Returns the length of the ARCRecord contained.
     * @return the length of the ARCRecord contained
     */
    public long getLength(){
        return length;
    }

    /**
     * Retrieve the data in the record.
     * If data is in RemoteFile, this operation deletes the RemoteFile.
     * @throws IllegalState if remotefile already deleted
     * @return the data from the ARCRecord as an InputStream.
     */
    public InputStream getData() {
        InputStream result = null;
        if (isStoredAsRemoteFile) {
            if (hasRemoteFileBeenDeleted) {
                throw new IllegalState("RemoteFile has already been deleted");
            }
            log.info("Reading " + length + " bytes from RemoteFile");
            InputStream rfInputStream = objectAsRemoteFile.getInputStream();
            result = new FilterInputStream(rfInputStream) {
                public void close() throws IOException {
                    super.close();
                    objectAsRemoteFile.cleanup();
                    hasRemoteFileBeenDeleted = true;
                }
            };
        } else {
            log.debug("Reading " + length + " bytes from objectBuffer");
            result = new ByteArrayInputStream(objectBuffer);
        }
        return result;
    }

    /**
     * Deliver the data in the record to a given OutputStream.
     * If data is in RemoteFile, this operation deletes the RemoteFile
     * @param out deliver the data to this outputstream
     * @throws IOFailure
     *             if any IOException occurs reading or writing the data
     * @throws IllegalState if remotefile already deleted
     */
    public void getData(OutputStream out) {
        ArgumentNotValid.checkNotNull(out, "OutputStream out");
        if (isStoredAsRemoteFile) {
            if (hasRemoteFileBeenDeleted) {
                throw new IllegalState("RemoteFile has already been deleted");
            }
            try {
                log.debug("Reading " + length + " bytes from RemoteFile");
                objectAsRemoteFile.appendTo(out);
            } finally {
                log.trace("Deleting the RemoteFile '"
                        + objectAsRemoteFile.getName() + "'.");
                objectAsRemoteFile.cleanup();
                hasRemoteFileBeenDeleted = true;
            }
        } else {
            try {
                log.debug("Reading " + length + " bytes from objectBuffer");
                out.write(objectBuffer, 0, objectBuffer.length);
            } catch (IOException e) {
                throw new IOFailure("Unable to write data from "
                        + "objectBuffer to the outputstream", e);
            }
        }
    }
}
