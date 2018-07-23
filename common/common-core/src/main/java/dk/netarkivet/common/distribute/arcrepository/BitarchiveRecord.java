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
package dk.netarkivet.common.distribute.arcrepository;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;

import org.archive.io.ArchiveRecord;
import org.archive.io.arc.ARCRecord;
import org.archive.io.warc.WARCRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.distribute.RemoteFile;
import dk.netarkivet.common.distribute.RemoteFileFactory;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.IllegalState;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.utils.arc.ARCUtils;
import dk.netarkivet.common.utils.warc.WARCUtils;

/**
 * Class to hold the result of a lookup operation in the bitarchive: The metadata information associated with the record
 * The actual byte content The name of the file the data were retrieved from If length of record exceeds value of
 * Settings.BITARCHIVE_LIMIT_FOR_RECORD_DATATRANSFER_IN_FILE The record is stored in a RemoteFile.
 */
@SuppressWarnings({"serial"})
public class BitarchiveRecord implements Serializable {

    /** the log. */
    private static final transient Logger log = LoggerFactory.getLogger(BitarchiveRecord.class);

    /** The file the data were retrieved from. */
    private String fileName;

    /** The actual data. */
    private byte[] objectBuffer;

    /** The offset of the ArchiveRecord contained. */
    private long offset;

    /** The length of the ArchiveRecord contained. */
    private long length;

    /** The actual data as a remote file. */
    private RemoteFile objectAsRemoteFile;

    /** Is the data stored in a RemoteFile. */
    private boolean isStoredAsRemoteFile = false;

    /** Set after deleting RemoteFile. */
    private boolean hasRemoteFileBeenDeleted = false;

    /** How large the ARCRecord can before saving as RemoteFile. */
    private final long LIMIT_FOR_SAVING_DATA_IN_OBJECT_BUFFER = Settings
            .getLong(CommonSettings.BITARCHIVE_LIMIT_FOR_RECORD_DATATRANSFER_IN_FILE);

    /**
     * Creates a BitarchiveRecord from the a ArchiveRecord, which can be either a ARCRecord or WARCRecord. Note that
     * record metadata is not included with the BitarchiveRecord, only the payload of the record.
     * <p>
     * If the length of the record is higher than Settings .BITARCHIVE_LIMIT_FOR_RECORD_DATATRANSFER_IN_FILE the data is
     * stored in a RemoteFile, otherwise the data is stored in a byte array.
     *
     * @param record the ArchiveRecord that the data should come from. We do not close the ArchiveRecord.
     * @param filename The filename of the ArchiveFile
     */
    public BitarchiveRecord(ArchiveRecord record, String filename) {
        ArgumentNotValid.checkNotNull(record, "ArchiveRecord record");
        ArgumentNotValid.checkNotNull(filename, "String filename");
        this.fileName = filename;
        this.offset = record.getHeader().getOffset();
        if (record instanceof ARCRecord) {
            length = record.getHeader().getLength();
        } else if (record instanceof WARCRecord) {
            // The length of the payload of the warc-record is not getLength(),
            // but getLength minus getContentBegin(), which is the number of
            // bytes used for the record-header!
            length = record.getHeader().getLength() - record.getHeader().getContentBegin();
        } else {
            throw new ArgumentNotValid("Unknown type of ArchiveRecord");
        }
        if (length > LIMIT_FOR_SAVING_DATA_IN_OBJECT_BUFFER) {
            // copy arc-data to local file and create a RemoteFile based on this
            log.info("Record exceeds limit of {} bytes. Length is {} bytes, Storing as instance of {}",
                    LIMIT_FOR_SAVING_DATA_IN_OBJECT_BUFFER, length, Settings.get(CommonSettings.REMOTE_FILE_CLASS));
            if (RemoteFileFactory.isExtendedRemoteFile()) {
                objectAsRemoteFile = RemoteFileFactory.getExtendedInstance(record);
                isStoredAsRemoteFile = true;
            } else {
                File localTmpFile = null;
                try {
                    localTmpFile = File.createTempFile("BitarchiveRecord-" + fileName, ".tmp", FileUtils.getTempDir());
                    record.dump(new FileOutputStream(localTmpFile));
                    objectAsRemoteFile = RemoteFileFactory.getMovefileInstance(localTmpFile);
                    isStoredAsRemoteFile = true;
                } catch (IOException e) {
                    throw new IOFailure("Unable to store record(" + fileName + "," + offset + ") as remotefile", e);
                }
            }
        } else { // Store data in objectbuffer
            try {
                if (record instanceof ARCRecord) {
                    objectBuffer = ARCUtils.readARCRecord((ARCRecord) record);
                } else if (record instanceof WARCRecord) {
                    objectBuffer = WARCUtils.readWARCRecord((WARCRecord) record);
                }
                log.debug("Bytes stored in objectBuffer: {}", objectBuffer.length);
            } catch (IOException e) {
                throw new ExceptionInInitializerError(e);
            }
        }
    }

    /**
     * Returns the file that this information was loaded from.
     *
     * @return the file that this ARC record comes from.
     */
    public String getFile() {
        return fileName;
    }

    /**
     * Returns the length of the ARCRecord contained.
     *
     * @return the length of the ARCRecord contained
     */
    public long getLength() {
        return length;
    }

    /**
     * Retrieve the data in the record. If data is in RemoteFile, this operation deletes the RemoteFile.
     *
     * @return the data from the ARCRecord as an InputStream.
     * @throws IllegalState if remotefile already deleted
     */
    public InputStream getData() {
        InputStream result = null;
        if (isStoredAsRemoteFile) {
            if (hasRemoteFileBeenDeleted) {
                throw new IllegalState("RemoteFile has already been deleted");
            }
            log.info("Reading {} bytes from RemoteFile", length);
            InputStream rfInputStream = objectAsRemoteFile.getInputStream();
            result = new FilterInputStream(rfInputStream) {
                public void close() throws IOException {
                    super.close();
                    objectAsRemoteFile.cleanup();
                    hasRemoteFileBeenDeleted = true;
                }
            };
        } else {
            log.debug("Reading {} bytes from objectBuffer", length);
            result = new ByteArrayInputStream(objectBuffer);
        }
        return result;
    }

    /**
     * Deliver the data in the record to a given OutputStream. If data is in RemoteFile, this operation deletes the
     * RemoteFile
     *
     * @param out deliver the data to this outputstream
     * @throws IOFailure if any IOException occurs reading or writing the data
     * @throws IllegalState if remotefile already deleted
     */
    public void getData(OutputStream out) {
        ArgumentNotValid.checkNotNull(out, "OutputStream out");
        if (isStoredAsRemoteFile) {
            if (hasRemoteFileBeenDeleted) {
                throw new IllegalState("RemoteFile has already been deleted");
            }
            try {
                log.debug("Reading {} bytes from RemoteFile", length);
                objectAsRemoteFile.appendTo(out);
            } finally {
                log.trace("Deleting the RemoteFile '{}'.", objectAsRemoteFile.getName());
                objectAsRemoteFile.cleanup();
                hasRemoteFileBeenDeleted = true;
            }
        } else {
            try {
                log.debug("Reading {} bytes from objectBuffer", length);
                out.write(objectBuffer, 0, objectBuffer.length);
            } catch (IOException e) {
                throw new IOFailure("Unable to write data from " + "objectBuffer to the outputstream", e);
            }
        }
    }

}
