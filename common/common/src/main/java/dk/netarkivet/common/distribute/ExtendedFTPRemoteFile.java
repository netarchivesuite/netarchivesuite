/* File:   $Id$
 * Revision: $Revision$
 * Author:   $Author$
 * Date:     $Date$
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
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 *  USA
 */

package dk.netarkivet.common.distribute;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.UUID;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.utils.SystemUtils;
import dk.netarkivet.common.utils.TimeUtils;
import static dk.netarkivet.common.CommonSettings.*;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.io.CopyStreamException;
import org.archive.io.ArchiveRecord;
import org.archive.io.arc.ARCRecord;
import org.archive.io.warc.WARCRecord;

/**
 * This class extends the functionality of FTPRemoteFile by allowing local input
 * to be taken from an ArchiveRecord. It has factory methods which return an
 * instance of FTPRemoteFile when a File is used as input so that behaviour is
 * effectively delegated to that class when required.
 */
public class ExtendedFTPRemoteFile implements RemoteFile {

    /**
     * A named logger for this class.
     */
    private static final transient Log log =
            LogFactory.getLog(ExtendedFTPRemoteFile.class.getName());

    /**
     * The record to be read from the archive.
     */
    private transient ArchiveRecord record;

    /**
     * The name to be used for the original record. ArchiveRecords do not
     * necessarily possess natural names so a guid is used. For arcfiles, this
     * is not guaranteed to be the same across multiple fetches of the same record
     */
    private String name;

    /**
     * Ftp-connection information.
     */
    private String ftpServerName;
    /** The ftp-server port. */
    private final int ftpServerPort;
    /** The username used to connect to the ftp-server. */
    private final String ftpUserName;
    /** The password used to connect to the ftp-server. */
    private final String ftpUserPassword;

     /**
     * How many times we will retry upload, download, and logon.
     */
    private static final transient int FTP_RETRIES
            = Settings.getInt(FTP_RETRIES_SETTINGS);

/**
     * How large a data timeout on our FTP connections.
     */
    private static final transient int FTP_DATATIMEOUT
            = Settings.getInt(FTP_DATATIMEOUT_SETTINGS);


     /**
     * The FTP client object for the current connection.
     */
    private transient FTPClient currentFTPClient;

    /**

     /**
     * The name that we use for the file on the FTP server.
     */
    private final String ftpFileName;

    /**
     * Create an instance of this class connected to an ARC or WARC record.
     * Unfortunately the reflection we use to find the factory method cannot
     * find this method directly because the runtime-class of the parameter
     * is not ArchiveRecord. Therefore we also define the two specific overloaded
     * factory methods for ARCRecords and WARCRecord.
     * @param record  the record
     * @return  the instance
     */
    public static RemoteFile getInstance(ArchiveRecord record) {
        return new ExtendedFTPRemoteFile(record);
    }

     /**
     * Create an instance of this class connected to an ARCRecord.
     * @param record  the record
     * @return  the instance
     */
    public static RemoteFile getInstance(ARCRecord record) {
        return getInstance((ArchiveRecord) record);
    }

     /**
     * Create an instance of this class connected to a WARCRecord.
     * @param record  the record
     * @return  the instance
     */
    public static RemoteFile getInstance(WARCRecord record) {
        return getInstance((ArchiveRecord) record);
    }

    /**
     * This method returns an instance of FTPRemoteFile using the
     * factory method with the same signature in that class.
      *
     * @param localFile         File object for the remote file
     * @param useChecksums      If true, checksums will be used to check
     *                          transfers.
     * @param fileDeletable     If true, this file will be deleted after upload
     *                          to FTP.
     * @param multipleDownloads If true, the file will not be removed from FTP
     *                          server automatically after first download.
     * @return FTPRemoteFile object
     * @throws IOFailure if FTPRemoteFile creation fails
     */
    public static RemoteFile getInstance(File localFile,
                                               Boolean useChecksums,
                                               Boolean fileDeletable,
                                               Boolean multipleDownloads)
            throws IOFailure {
        ArgumentNotValid.checkNotNull(localFile, "File remoteFile");
        return FTPRemoteFile.getInstance(localFile, useChecksums,
                                 fileDeletable, multipleDownloads, null);
    }

    /**
     * This method returns an instance of FTPRemoteFile using the
     * factory method with the same signature in that class.
      *
     * @param localFile         File object for the remote file
     * @param useChecksums      If true, checksums will be used to check
     *                          transfers.
     * @param fileDeletable     If true, this file will be deleted after upload
     *                          to FTP.
     * @param multipleDownloads If true, the file will not be removed from FTP
     *                          server automatically after first download.
     * @return FTPRemoteFile object
     * @throws IOFailure if FTPRemoteFile creation fails
     */
    public static RemoteFile getInstance(File localFile,
            Boolean useChecksums,
            Boolean fileDeletable,
            Boolean multipleDownloads,
            RemoteFileSettings connectionParams)
                    throws IOFailure {
        ArgumentNotValid.checkNotNull(localFile, "File remoteFile");
        return FTPRemoteFile.getInstance(localFile, useChecksums,
                fileDeletable, multipleDownloads, connectionParams);
    }

    @Override
    public void copyTo(File destFile) {
        ArgumentNotValid.checkNotNull(destFile, "File destFile");
        destFile = destFile.getAbsoluteFile();
        if ((!destFile.isFile() || !destFile.canWrite())
            && (!destFile.getParentFile().isDirectory()
                || !destFile.getParentFile().canWrite())) {
            throw new ArgumentNotValid("Destfile '" + destFile
                                       + "' does not point to a writable file for remote file '"
                                       + toString() + "'");
        }
        log.debug("Writing " + toString() + " to " + destFile.getAbsolutePath());
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(destFile);
            appendTo(fos);
        } catch (Exception e) {
            FileUtils.remove(destFile);
            throw new IOFailure("IO trouble transferring file", e);
        } finally {
            try {
                fos.close();
            } catch (IOException e) {
                 log.warn("Error closing output stream", e);
            }
        }
    }

    @Override
    public void appendTo(OutputStream out) {
        ArgumentNotValid.checkNotNull(out, "OutputStream out");
        logOn();
        try{
            if (!currentFTPClient.retrieveFile(ftpFileName, out)) {
                final String msg = "Append operation from '"
                                   + ftpFileName + "' failed: " + getFtpErrorMessage();
                log.warn(msg);
                throw new IOFailure(msg);
            }
            out.flush();
        } catch (IOException e) {
            String msg = "Append operation from '" + ftpFileName
                         + "' failed ";
            if (e instanceof CopyStreamException) {
                CopyStreamException realException = (CopyStreamException) e;
                msg += "(real cause = " + realException.getIOException() + ")";
            }
            log.warn(msg, e);
            throw new IOFailure(msg, e);
        } finally {
            logOut();
            cleanup();
        }
    }

    @Override
    public InputStream getInputStream() {
        logOn();
        try {
            InputStream in = currentFTPClient.retrieveFileStream(ftpFileName);
            return new FilterInputStream(in) {
                public void close() throws IOException {
                    try {
                        super.close();
                    } finally {
                        logOut();
                        cleanup();
                    }
                }
            };
        } catch (IOException e) {
            String msg = "Creating inputstream from '" + ftpFileName
                         + "' failed ";
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
     * @return null
     */
    @Override
    public String getChecksum() {
        return null;
    }

    /**
     * The cleanup to be effected is deletion of the intermediate file from the
     * ftp server.
     */
    @Override
    public void cleanup() {
        log.debug("Deleting file '" + ftpFileName + "' from ftp server");
        try {
            logOn();
            currentFTPClient.deleteFile(ftpFileName);
        } catch (Exception e) {
            log.warn("Error while deleting ftp file '" + ftpFileName
                     + "' for file '" + name + "'", e);
        } finally {
            // try to disconnect before returning from method
            try {
                logOut();
            } catch (Exception e) {
                log.warn("Unexpected error while logging out ", e);
            }
        }
        log.debug("File '" + ftpFileName + "' deleted from ftp server. "
                + "Cleanup finished.");
    }

    /**
     * For an ARCRecord, this is the length of the record as defined in the
     * header. For a WARCRecods, this is the payload length, defined as the
     * difference between the total record length and the size of the header.
     * @return the length of the record content in bytes.
     *
     */
    @Override
    public long getSize() {
         if (record instanceof ARCRecord) {
            return record.getHeader().getLength();
        } else if (record instanceof WARCRecord) {
            // The length of the payload of the warc-record is not getLength(),
            // but getLength minus getContentBegin(), which is the number of
            // bytes used for the record-header!
            return  record.getHeader().getLength() - record.getHeader().getContentBegin();
        } else {
            throw new ArgumentNotValid("Unknown type of ArchiveRecord: " + record.getClass());
        }
    }

    /**
     * Creates a RemoteFile instance by uploading the content of the given
     * record to a file on the ftp server.
     * @param record The record to be copied.
     */
    private ExtendedFTPRemoteFile(ArchiveRecord record) {
        this.record = record;
        this.name = UUID.randomUUID().toString();
        this.ftpFileName = this.name;
        log.debug("Created " + this.getClass().getName() + " with name " +
                  toString());

        this.ftpServerName = Settings.get(FTP_SERVER_NAME);
        this.ftpServerPort = Settings.getInt(FTP_SERVER_PORT);
        this.ftpUserName = Settings.get(FTP_USER_NAME);
        this.ftpUserPassword = Settings.get(FTP_USER_PASSWORD);
        if (ftpServerName.equalsIgnoreCase("localhost")) {
            ftpServerName = SystemUtils.getLocalHostName();
            log.debug("ftpServerName set to localhost on machine: "
                      + SystemUtils.getLocalHostName() + ", resetting to "
                      + ftpServerName);
        }
        logOn();
        boolean success = false;
        int tried = 0;
        while (!success && tried < FTP_RETRIES) {
            tried++;
            try {
                success = currentFTPClient.storeFile(ftpFileName, record);
                if (!success) {
                    log.debug("FTP store failed attempt '" + tried
                              + "' of " + FTP_RETRIES
                              + ": " + getFtpErrorMessage());
                }
            } catch (IOException e) {
                String message = "Write operation to '"
                                 + ftpFileName
                                 + "' failed on attempt " + tried
                                 + " of " + FTP_RETRIES;
                if (e instanceof CopyStreamException) {
                    CopyStreamException realException
                            = (CopyStreamException) e;
                    message += "(real cause = "
                               + realException.getIOException() + ")";
                }
                log.debug(message, e);
            }
        }
        if (!success) {
            final String msg = "Failed to upload '" + name
                               + "' after "
                               + tried + " attempts";
            log.warn(msg);
            throw new IOFailure(msg);
        }
        log.debug("Completed writing the file '" + ftpFileName + "'");
        try {
            if (record != null) {
                record.close();
            }
        } catch (IOException e) {
            log.warn("Problem closing inputstream: " + e);
            // not a serious bug
        }
        logOut();
        log.debug("Ftp logout");
    }

    /**
     * A human readbale description of the object which should be sufficient to
     * identify and track it.
     * @return description of this object.
     */
    public String toString() {
        return record.getHeader().getRecordIdentifier() + "_" +
               record.getHeader().getOffset() + "_" + "(" + name + ")";
    }

    //TODO This code is copied from FTPRemoteFile. A better solution would
    //be to have some sort of helper class - e.g. an FTPConnectionManager - with
    //a single copy of this code.
     /**
     * Create FTPClient and log on to ftp-server, if not already connected to
     * ftp-server.  Attempts to set binary mode and passive mode.
     * Will try to login up to FTP_RETRIES times, if login fails.
     */
    private void logOn() {
        if (currentFTPClient != null && currentFTPClient.isConnected()) {
            return;
        } else { // create new FTPClient object and connect to ftp-server
            currentFTPClient = new FTPClient();
        }

        log.trace("Try to logon to ftp://" + ftpUserName + ":"
                  + ftpUserPassword.replaceAll(".", "*") + "@"
                  + ftpServerName + ":" + ftpServerPort);

        int tries = 0;
        boolean logOnSuccessful = false;
        while (!logOnSuccessful && tries < FTP_RETRIES) {
            tries++;
            try {
                currentFTPClient.connect(ftpServerName, ftpServerPort);
                currentFTPClient.setDataTimeout(FTP_DATATIMEOUT);
                if (!currentFTPClient.login(ftpUserName, ftpUserPassword)) {
                    final String message = "Could not log in [from host: "
                        + SystemUtils.getLocalHostName()
                        + "] to '"
                        + ftpServerName
                        + "' on port " + ftpServerPort
                        + " with user '"
                        + ftpUserName + "' password '"
                        + ftpUserPassword.replaceAll(".", "*")
                        + "': " + getFtpErrorMessage();
                    log.warn(message);
                    throw new IOFailure(message);
                }

                if (!currentFTPClient.setFileType(FTPClient.BINARY_FILE_TYPE)) {
                    final String message = "Could not set binary on '"
                        + ftpServerName
                        + "', losing high bits.  Error: "
                        + getFtpErrorMessage();
                    log.warn(message);
                    throw new IOFailure(message);
                }

                // This only means that PASV is sent before every transfer
                // command.
                currentFTPClient.enterLocalPassiveMode();

                log.debug("w/ DataTimeout (ms): "
                        + currentFTPClient.getDefaultTimeout());
                logOnSuccessful = true;
            } catch (IOException e) {
                final String msg = "Connect to " + ftpServerName
                + " from host: "
                + SystemUtils.getLocalHostName() + " failed";
                if (tries < FTP_RETRIES) {
                    log.debug(msg + ". Attempt #" + tries + " of max "
                            + FTP_RETRIES
                            + ". Will sleep a while before trying to "
                            + "connect again. Exception: ", e);
                    TimeUtils.exponentialBackoffSleep(tries, Calendar.MINUTE);
                } else {
                    log.warn(msg + ". This was the last (#" + tries
                            + ") connection attempt");
                    throw new IOFailure(msg, e);
                }
            }
        }

        log.debug("Logged onto ftp://" + ftpUserName + ":"
                  + ftpUserPassword.replaceAll(".", "*")
                  + "@" + ftpServerName + ":" + ftpServerPort);
    }

    /**
     * Get the reply code and string from the ftp client.
     *
     * @return A string with the FTP servers last reply code and message.
     */
    private String getFtpErrorMessage() {
        return ("Error " + currentFTPClient.getReplyCode() + ": '"
                + currentFTPClient.getReplyString() + "'");
    }

     /**
     * Log out from the FTP server.
     */
    private void logOut() {
        log.debug("Trying to log out.");
        try {
            if (currentFTPClient != null) {
                currentFTPClient.disconnect();
            }
        } catch (IOException e) {
            String msg = "Disconnect from '" + ftpServerName
                               + "' failed ";
            if (e instanceof CopyStreamException) {
                CopyStreamException realException = (CopyStreamException) e;
                msg += "(real cause = " + realException.getIOException() + ")";
            }
            log.warn(msg, e);
        }
    }

}
