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
import org.archive.io.ArchiveRecord;
import org.archive.io.arc.ARCRecord;
import org.archive.io.warc.WARCRecord;

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
 * Class to hold the result of a lookup operation in the bitarchive:
 *    The metadata information associated with the record
 *    The actual byte content
 *    The name of the file the data were retrieved from
 *    If length of record exceeds value of
 *    Settings.BITARCHIVE_LIMIT_FOR_RECORD_DATATRANSFER_IN_FILE
 *    The record is stored in a RemoteFile.
 *
 */
@SuppressWarnings({ "serial"})
public class BitarchiveRecord implements Serializable {

    /** The file the data were retrieved from. */
    private String fileName;

    /** The actual data. */
    private byte[] objectBuffer;

    /** The offset of the ArchiveRecord contained. */
    private long offset;

    /** The length of the ArchiveRecord contained. */
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
     * Creates a BitarchiveRecord from the a ArchiveRecord, which can be either
     * a ARCRecord or WARCRecord. Note that record metadata is not included with
     * the BitarchiveRecord, only the payload of the record.
     * 
     * If the length of the record is higher than Settings
     * .BITARCHIVE_LIMIT_FOR_RECORD_DATATRANSFER_IN_FILE
     *  the data is stored in a RemoteFile, otherwise the data is stored in
     *  a byte array.
     * @param record the ArchiveRecord that the data should come from.  We do not
     * close the ArchiveRecord.
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
            log.info("Record exceeds limit of "
                     + LIMIT_FOR_SAVING_DATA_IN_OBJECT_BUFFER
                     + " bytes. Length is " + length
                     + " bytes, Storing as instance of "
                     + Settings.get(CommonSettings.REMOTE_FILE_CLASS));
            if (RemoteFileFactory.isExtendedRemoteFile()) {
                objectAsRemoteFile = RemoteFileFactory.getExtendedInstance(record);
                isStoredAsRemoteFile = true;
            }  else {
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
            }
        } else { // Store data in objectbuffer
            try {
                if (record instanceof ARCRecord) {
                    objectBuffer = ARCUtils.readARCRecord((ARCRecord) record);
                } else if (record instanceof WARCRecord) {
                    objectBuffer = WARCUtils.readWARCRecord((WARCRecord) record);
                }
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
