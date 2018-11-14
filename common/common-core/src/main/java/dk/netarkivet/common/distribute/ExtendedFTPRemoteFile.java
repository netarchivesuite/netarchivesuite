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

package dk.netarkivet.common.distribute;

import static dk.netarkivet.common.CommonSettings.FTP_DATATIMEOUT_SETTINGS;
import static dk.netarkivet.common.CommonSettings.FTP_RETRIES_SETTINGS;
import static dk.netarkivet.common.CommonSettings.FTP_SERVER_NAME;
import static dk.netarkivet.common.CommonSettings.FTP_SERVER_PORT;
import static dk.netarkivet.common.CommonSettings.FTP_USER_NAME;
import static dk.netarkivet.common.CommonSettings.FTP_USER_PASSWORD;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import org.apache.commons.io.IOUtils;
import org.apache.commons.net.io.CopyStreamException;
import org.archive.io.ArchiveRecord;
import org.archive.io.arc.ARCRecord;
import org.archive.io.warc.WARCRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.NotificationType;
import dk.netarkivet.common.utils.NotificationsFactory;
import dk.netarkivet.common.utils.Settings;

/**
 * This class extends the functionality of FTPRemoteFile by allowing local input to be taken from an ArchiveRecord. It
 * has factory methods which return an instance of FTPRemoteFile when a File is used as input so that behavior is
 * effectively delegated to that class when required.
 */
@SuppressWarnings({"serial"})
public class ExtendedFTPRemoteFile implements RemoteFile {

    /** A named logger for this class. */
    private static final transient Logger log = LoggerFactory.getLogger(ExtendedFTPRemoteFile.class);

    /** The record to be read from the archive. */
    private transient ArchiveRecord record;

    /**
     * The name to be used for the original record. ArchiveRecords do not necessarily possess natural names so a guid is
     * used. For arcfiles, this is not guaranteed to be the same across multiple fetches of the same record
     */
    private String name;

    
    /** How many times we will retry upload, download, and logon. */
    private static final transient int FTP_RETRIES = Settings.getInt(FTP_RETRIES_SETTINGS);

    /** How large a data timeout on our FTP connections. */
    private static final transient int FTP_DATATIMEOUT = Settings.getInt(FTP_DATATIMEOUT_SETTINGS);

    /** The default place in classpath where the settings file can be found. */
    private static final String DEFAULT_SETTINGS_CLASSPATH = "dk/netarkivet/common/distribute/FTPRemoteFileSettings.xml";

    /*
     * The static initialiser is called when the class is loaded. It will add default values for all settings defined in
     * this class, by loading them from a settings.xml file in classpath.
     */
    static {
        Settings.addDefaultClasspathSettings(DEFAULT_SETTINGS_CLASSPATH);
    }
    
    /** The name that we use for the file on the FTP server. */
    private final String ftpFileName;

	private FTPConnectionManager connectionManager;

    /**
     * Create an instance of this class connected to an ARC or WARC record. Unfortunately the reflection we use to find
     * the factory method cannot find this method directly because the runtime-class of the parameter is not
     * ArchiveRecord. Therefore we also define the two specific overloaded factory methods for ARCRecords and
     * WARCRecord.
     *
     * @param record the record
     * @return the instance
     */
    public static RemoteFile getInstance(ArchiveRecord record) {
        return new ExtendedFTPRemoteFile(record);
    }

    /**
     * Create an instance of this class connected to an ARCRecord.
     *
     * @param record the record
     * @return the instance
     */
    public static RemoteFile getInstance(ARCRecord record) {
        return getInstance((ArchiveRecord) record);
    }

    /**
     * Create an instance of this class connected to a WARCRecord.
     *
     * @param record the record
     * @return the instance
     */
    public static RemoteFile getInstance(WARCRecord record) {
        return getInstance((ArchiveRecord) record);
    }

    /**
     * This method returns an instance of FTPRemoteFile using the factory method with the same signature in that class.
     *
     * @param localFile File object for the remote file
     * @param useChecksums If true, checksums will be used to check transfers.
     * @param fileDeletable If true, this file will be deleted after upload to FTP.
     * @param multipleDownloads If true, the file will not be removed from FTP server automatically after first
     * download.
     * @return FTPRemoteFile object
     * @throws IOFailure if FTPRemoteFile creation fails
     */
    public static RemoteFile getInstance(File localFile, Boolean useChecksums, Boolean fileDeletable,
            Boolean multipleDownloads) throws IOFailure {
        ArgumentNotValid.checkNotNull(localFile, "File remoteFile");
        return FTPRemoteFile.getInstance(localFile, useChecksums, fileDeletable, multipleDownloads, null);
    }

    /**
     * This method returns an instance of FTPRemoteFile using the factory method with the same signature in that class.
     *
     * @param localFile File object for the remote file
     * @param useChecksums If true, checksums will be used to check transfers.
     * @param fileDeletable If true, this file will be deleted after upload to FTP.
     * @param multipleDownloads If true, the file will not be removed from FTP server automatically after first
     * download.
     * @return FTPRemoteFile object
     * @throws IOFailure if FTPRemoteFile creation fails
     */
    public static RemoteFile getInstance(File localFile, Boolean useChecksums, Boolean fileDeletable,
            Boolean multipleDownloads, RemoteFileSettings connectionParams) throws IOFailure {
        ArgumentNotValid.checkNotNull(localFile, "File remoteFile");
        return FTPRemoteFile.getInstance(localFile, useChecksums, fileDeletable, multipleDownloads, connectionParams);
    }

    @Override
    public void copyTo(File destFile) {
        ArgumentNotValid.checkNotNull(destFile, "File destFile");
        destFile = destFile.getAbsoluteFile();
        if ((!destFile.isFile() || !destFile.canWrite())
                && (!destFile.getParentFile().isDirectory() || !destFile.getParentFile().canWrite())) {
            throw new ArgumentNotValid("Destfile '" + destFile + "' does not point to a writable file for "
                    + "remote file '" + toString() + "'");
        }
        if (log.isDebugEnabled()) {
            log.debug("Writing {} to {}", toString(), destFile.getAbsolutePath());
        }
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(destFile);
            appendTo(fos);
        } catch (Exception e) {
            FileUtils.remove(destFile);
            throw new IOFailure("IO trouble transferring file", e);
        } finally {
            IOUtils.closeQuietly(fos);
        }
    }

    @Override
    public void appendTo(OutputStream out) {
        ArgumentNotValid.checkNotNull(out, "OutputStream out");
        connectionManager.logOn();
        try {
            if (!connectionManager.getFTPClient().retrieveFile(ftpFileName, out)) {
                final String msg = "Append operation from '" + ftpFileName + "' failed: " + connectionManager.getFtpErrorMessage();
                log.warn(msg);
                throw new IOFailure(msg);
            }
            out.flush();
        } catch (IOException e) {
            String msg = "Append operation from '" + ftpFileName + "' failed ";
            if (e instanceof CopyStreamException) {
                CopyStreamException realException = (CopyStreamException) e;
                msg += "(real cause = " + realException.getIOException() + ")";
            }
            log.warn(msg, e);
            throw new IOFailure(msg, e);
        } finally {
        	connectionManager.logOut();
            cleanup();
        }
    }

    @Override
    public InputStream getInputStream() {
    	connectionManager.logOn();
        try {
            InputStream in = connectionManager.getFTPClient().retrieveFileStream(ftpFileName);
            return new FilterInputStream(in) {
                public void close() throws IOException {
                    try {
                        super.close();
                    } finally {
                    	connectionManager.logOut();
                        cleanup();
                    }
                }
            };
        } catch (IOException e) {
            String msg = "Creating inputstream from '" + ftpFileName + "' failed ";
            if (e instanceof CopyStreamException) {
                CopyStreamException realException = (CopyStreamException) e;
                msg += "(real cause = " + realException.getIOException() + ")";
            }
            log.warn(msg, e);
            throw new IOFailure(msg, e);
        }
    }

    @Override
    public String getName() {
        return name;
    }

    /**
     * Checksums are not available in this implementation. Returns null.
     *
     * @return null
     */
    @Override
    public String getChecksum() {
        return null;
    }

    /**
     * The cleanup to be effected is deletion of the intermediate file from the ftp server.
     */
    @Override
    public void cleanup() {
        log.debug("Deleting file '{}' from ftp server", ftpFileName);
        try {
        	connectionManager.logOn();
        	connectionManager.getFTPClient().deleteFile(ftpFileName);
        } catch (Exception e) {
            log.warn("Error while deleting ftp file '{}' for file '{}'", ftpFileName, name, e);
        } finally {
            // try to disconnect before returning from method
            try {
            	connectionManager.logOut();
            } catch (Exception e) {
                log.warn("Unexpected error while logging out ", e);
            }
        }
        log.debug("File '{}' deleted from ftp server. Cleanup finished.", ftpFileName);
    }

    /**
     * For an ARCRecord, this is the length of the record as defined in the header. For a WARCRecods, this is the
     * payload length, defined as the difference between the total record length and the size of the header.
     *
     * @return the length of the record content in bytes.
     */
    @Override
    public long getSize() {
        if (record instanceof ARCRecord) {
            return record.getHeader().getLength();
        } else if (record instanceof WARCRecord) {
            // The length of the payload of the warc-record is not getLength(),
            // but getLength minus getContentBegin(), which is the number of
            // bytes used for the record-header!
            return record.getHeader().getLength() - record.getHeader().getContentBegin();
        } else {
            throw new ArgumentNotValid("Unknown type of ArchiveRecord: " + record.getClass());
        }
    }

    /**
     * Creates a RemoteFile instance by uploading the content of the given record to a file on the ftp server.
     *
     * @param record The record to be copied.
     */
    private ExtendedFTPRemoteFile(ArchiveRecord record) {
        this.record = record;
        this.name = UUID.randomUUID().toString();
        this.ftpFileName = this.name;
        if (log.isDebugEnabled()) {
            log.debug("Created {} with name {}", this.getClass().getName(), toString());
        }

        this.connectionManager = new FTPConnectionManager(
        		Settings.get(FTP_USER_NAME), 
        		Settings.get(FTP_USER_PASSWORD), 
        		Settings.get(FTP_SERVER_NAME), 
        		Settings.getInt(FTP_SERVER_PORT), 
        		Settings.getInt(FTP_RETRIES_SETTINGS), 
        		Settings.getInt(FTP_DATATIMEOUT_SETTINGS));
        
        connectionManager.logOn();
        boolean success = false;
        int tried = 0;
        String message = null;
        while (!success && tried < FTP_RETRIES) {
            tried++;
            try {
                success = connectionManager.getFTPClient().storeFile(ftpFileName, record);
                if (!success) {
                    log.debug("FTP store failed attempt '{}' of " + FTP_RETRIES + ": {}", tried, connectionManager.getFtpErrorMessage());
                }
            } catch (IOException e) {
                message = "Write operation to '" + ftpFileName + "' failed on attempt " + tried + " of "
                        + FTP_RETRIES;
                if (e instanceof CopyStreamException) {
                    CopyStreamException realException = (CopyStreamException) e;
                    message += "(real cause = " + realException.getIOException() + ")";
                }
                log.debug(message, e);
            }
        }
        if (!success) {
            final String msg = "Failed to upload '" + name + "' after " + tried + " attempts. Reason for last failure: " +  message;
            log.warn(msg);
            // Send an Notification because of this
            NotificationsFactory.getInstance().notify(msg, NotificationType.ERROR);
            throw new IOFailure(msg);
        }
        log.debug("Completed writing the file '{}'", ftpFileName);
        try {
            if (record != null) {
                record.close();
            }
        } catch (IOException e) {
            // not a serious bug
            log.warn("Problem closing inputstream: ", e);
        }
        connectionManager.logOut();
        log.debug("Ftp logout");
    }

    /**
     * A human readable description of the object which should be sufficient to identify and track it.
     *
     * @return description of this object.
     */
    public String toString() {
        return record.getHeader().getRecordIdentifier() + "_" + record.getHeader().getOffset() + "_" + "(" + name + ")";
    }
}
