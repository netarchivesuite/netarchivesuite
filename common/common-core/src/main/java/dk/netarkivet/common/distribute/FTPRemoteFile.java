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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.DigestInputStream;
import java.security.DigestOutputStream;
import java.util.Date;
import java.util.Random;

import org.apache.commons.io.IOUtils;
import org.apache.commons.net.io.CopyStreamException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.ChecksumCalculator;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.NotificationType;
import dk.netarkivet.common.utils.NotificationsFactory;
import dk.netarkivet.common.utils.Settings;

/**
 * Class encapsulating upload to & download from an ftp-server.
 * <p>
 * Transfers are done using binary type and passive mode, if available.
 */
@SuppressWarnings({"serial"})
public final class FTPRemoteFile extends AbstractRemoteFile {

    /** A named logger for this class. */
    private static final transient Logger log = LoggerFactory.getLogger(FTPRemoteFile.class);

    /**
     * How many times we will retry upload, download, and logon.
     */
    public static int FTP_RETRIES = Settings.getInt(CommonSettings.FTP_RETRIES_SETTINGS);
    /**
     * How large a data timeout on our FTP connections.
     */
    public static int FTP_DATATIMEOUT = Settings.getInt(CommonSettings.FTP_DATATIMEOUT_SETTINGS);

    /** The default place in classpath where the settings file can be found. */
    private static final String DEFAULT_SETTINGS_CLASSPATH = "dk/netarkivet/common/distribute/FTPRemoteFileSettings.xml";

    /*
     * The static initialiser is called when the class is loaded. It will add default values for all settings defined in
     * this class, by loading them from a settings.xml file in classpath.
     */
    static {
        Settings.addDefaultClasspathSettings(DEFAULT_SETTINGS_CLASSPATH);
    }

    /**
     * Ftp-connection information. FTP-related settings are by default read from settings, unless connectionParameters
     * are given in the constructor.
     */
    private String ftpServerName;

    /** The ftp-server port. */
    private final int ftpServerPort;
    /** The username used to connect to the ftp-server. */
    private final String ftpUserName;
    /** The password used to connect to the ftp-server. */
    private final String ftpUserPassword;

    /** The name that we use for the file on the FTP server. This is only for internal use. */
    private final String ftpFileName;

    /** If useChecksums is true, contains the file checksum. */
    protected final String checksum;
    
    private FTPConnectionManager cm;

    // NOTE: The constants defining setting names below are left non-final on
    // purpose! Otherwise, the static initialiser that loads default values
    // will not run.

    /**
     * Private constructor used by getInstance() static-method Tries to generate unique name on ftp-server.
     *
     * @param localFile File used to create new file on ftp-server.
     * @param useChecksums If true, checksums will be used to check transfers.
     * @param fileDeletable If true, this file will be deleted after upload to FTP.
     * @param multipleDownloads If true, the file will not be removed from FTP server automatically after first
     * download.
     * @param connectionParams If not null, contains connection parameters to the FTP-server desired by the user
     * @throws IOFailure if MD5 checksum fails, or ftp fails
     * @throws ArgumentNotValid if the local file cannot be read.
     */
    private FTPRemoteFile(File localFile, boolean useChecksums, boolean fileDeletable, boolean multipleDownloads,
            RemoteFileSettings connectionParams) throws IOFailure {
        super(localFile, useChecksums, fileDeletable, multipleDownloads);
        if (connectionParams != null) {
            // use the connection parameters desired by the user.
            this.ftpServerName = connectionParams.getServerName();
            this.ftpServerPort = connectionParams.getServerPort();
            this.ftpUserName = connectionParams.getUserName();
            this.ftpUserPassword = connectionParams.getUserPassword();
        } else {
            // use the connection parameters specified by the settings.
            this.ftpServerName = Settings.get(CommonSettings.FTP_SERVER_NAME);
            this.ftpServerPort = Settings.getInt(CommonSettings.FTP_SERVER_PORT);
            this.ftpUserName = Settings.get(CommonSettings.FTP_USER_NAME);
            this.ftpUserPassword = Settings.get(CommonSettings.FTP_USER_PASSWORD);
        }
        this.cm = new FTPConnectionManager(ftpUserName, ftpUserPassword, ftpServerName, ftpServerPort, 
        		Settings.getInt(CommonSettings.FTP_RETRIES_SETTINGS), Settings.getInt(CommonSettings.FTP_DATATIMEOUT_SETTINGS));

        if (filesize == 0) {
            if (useChecksums) {
                checksum = ChecksumCalculator.calculateMd5(file);
            } else {
                checksum = null;
            }
            ftpFileName = "-";
        } else { 
        	// A large enough number to make it unlikely that two files are
            // created with the same FTP server name. Already the millisecond
            // datestamp reduces the likelihood, with this even if two
            // processes/threads try to upload the same file in the same
            // millisecond (very unlikely) they have only .01% chance of
            // clashing.
            final int aMagicNumber = 100000;
            ftpFileName = file.getName() + "-" + new Random().nextInt(aMagicNumber) + "-" + new Date().getTime();
            InputStream in;

            try {
                in = new FileInputStream(localFile);
            } catch (FileNotFoundException e) {
                final String message = "Couldn't prepare file '" + localFile + "' for remote access. File not found.";
                log.debug(message, e);
                throw new IOFailure(message, e);
            }
            log.debug("Writing '{}' as '{}' on ftp-server {}", file.getName(), ftpFileName, cm.getFtpServer());

            // Writing inlined in constructor to allow the checksum field to
            // be final (and thus must be set in constructor).
            try {
                cm.logOn();
                if (useChecksums) {
                    in = new DigestInputStream(in, ChecksumCalculator.getMessageDigest(ChecksumCalculator.MD5));
                }
                boolean success = false;
                int tried = 0;
                String message = null;
                while (!success && tried < FTP_RETRIES) {
                    tried++;
                    try {
                        success = cm.getFTPClient().storeFile(ftpFileName, in);
                        if (!success) {
                            log.debug("FTP store failed attempt '{}' of {}: {}", tried, FTP_RETRIES,
                                    cm.getFtpErrorMessage());
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
                    final String msg = "Failed to upload '" + localFile + "' after " + tried 
                            + " attempts. Reason for last failure: " +  message;
                    log.warn(msg);
                    // Send an Notification because of this
                    NotificationsFactory.getInstance().notify(msg, NotificationType.ERROR);
                    throw new IOFailure(msg);
                }
                log.debug("Completed writing the file '{}'", ftpFileName);

                if (useChecksums) {
                    checksum = ChecksumCalculator.toHex(((DigestInputStream) in).getMessageDigest().digest());
                    log.debug("Checksum of '{}' is:{}", ftpFileName, checksum);
                } else {
                    checksum = null;
                }
            } finally {
                IOUtils.closeQuietly(in);
                cm.logOut();
                log.debug("Ftp logout");
            }
        }
        if (fileDeletable) {
            try {
                FileUtils.removeRecursively(localFile);
            } catch (IOFailure e) {
                // Not fatal
                log.warn("Couldn't remove tmp file {}", localFile, e);
            }
        }
    }

    /**
     * Create a remote file that handles the transport of the remote file data. This method is used by the sender to
     * prepare the transport.
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
        return new FTPRemoteFile(localFile, useChecksums, fileDeletable, multipleDownloads, null);
    }

    public static RemoteFile getInstance(File localFile, Boolean useChecksums, Boolean fileDeletable,
            Boolean multipleDownloads, RemoteFileSettings connectionParams) throws IOFailure {
        ArgumentNotValid.checkNotNull(localFile, "File remoteFile");
        return new FTPRemoteFile(localFile, useChecksums, fileDeletable, multipleDownloads, connectionParams);
    }

    /**
     * An implementation of the getInputStream operation that works with FTP. Notice that most of the special work
     * (logging out and checking MD5) happens in the close() method of the returned InputStream, since that is the only
     * place where we can know we're done.
     *
     * @return An InputStream that will deliver the data transferred by FTP. Holding on to this for long periods without
     * reading any data might cause a timeout.
     */
    @Override
    public InputStream getInputStream() {
        if (filesize == 0) {
            return new ByteArrayInputStream(new byte[] {});
        }
        try {
            cm.logOn();

            InputStream in = cm.getFTPClient().retrieveFileStream(ftpFileName);
            if (in == null) {
                throw new IOFailure("Unable to retrieve input stream:" + cm.getFtpErrorMessage());
            }
            if (useChecksums) {
                in = new DigestInputStream(in, ChecksumCalculator.getMessageDigest(ChecksumCalculator.MD5));
            }
            return new FilterInputStream(in) {
                public void close() throws IOException {
                    try {
                        super.close();
                        if (useChecksums) {
                            String newChecksum = ChecksumCalculator.toHex(((DigestInputStream) in).getMessageDigest()
                                    .digest());
                            if (!newChecksum.equals(checksum)) {
                                final String msg = "Checksums of '" + ftpFileName + "' do not match! " + "Should be "
                                        + checksum + " but was " + newChecksum;
                                log.warn(msg);
                                throw new IOFailure(msg);
                            }
                        }
                    } finally {
                        cm.logOut();
                        if (!multipleDownloads) {
                            cleanup();
                        }
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

    /**
     * Write the contents of this ftp remote file to an output stream. Notice that while the checksum of the transferred
     * data is checked, no retries are performed, and in case of failure, there is no guarantee that any data have been
     * transferred.
     *
     * @param out OutputStream that the data will be written to. This stream will not be closed by this operation.
     * @throws IOFailure If append operation fails
     */
    @Override
    public void appendTo(OutputStream out) {
        ArgumentNotValid.checkNotNull(out, "OutputStream out");

        if (filesize == 0) {
            return;
        }

        try {
            cm.logOn();

            if (useChecksums) {
                out = new DigestOutputStream(out, ChecksumCalculator.getMessageDigest(ChecksumCalculator.MD5));
            }
            if (!cm.getFTPClient().retrieveFile(ftpFileName, out)) {
                final String msg = "Append operation from '" + ftpFileName + "' failed: " + cm.getFtpErrorMessage();
                log.warn(msg);
                throw new IOFailure(msg);
            }
            out.flush();
            if (useChecksums) {
                String newChecksum = ChecksumCalculator.toHex(((DigestOutputStream) out).getMessageDigest().digest());
                if (checksum != null && !checksum.equals(newChecksum)) {
                    final String msg = "Checksums of '" + ftpFileName + "' do not match! Should be " + checksum
                            + " but was " + newChecksum;
                    log.warn(msg);
                    throw new IOFailure(msg);
                }
            }
        } catch (IOException e) {
            String msg = "Append operation from '" + ftpFileName + "' failed ";
            if (e instanceof CopyStreamException) {
                CopyStreamException realException = (CopyStreamException) e;
                msg += "(real cause = " + realException.getIOException() + ")";
            }
            log.warn(msg, e);
            throw new IOFailure(msg, e);
        } finally {
            cm.logOut();
            if (!multipleDownloads) {
                cleanup();
            }
        }
    }

    /**
     * Cleanup will delete the file on the FTP server. This method should never throw exceptions. It is idempotent,
     * meaning it can be called twice without trouble.
     */
    @Override
    public void cleanup() {
        if (filesize == 0) {
            return;
        }
        boolean deleted = false;
        log.debug("Deleting file '{}' from ftp server", ftpFileName);
        try {
            cm.logOn();
            deleted = cm.getFTPClient().deleteFile(ftpFileName);
        } catch (Exception e) {
            log.warn("Error while deleting ftp file '{}' for file '{}'", ftpFileName, file.getName(), e);
        } finally {
            // try to disconnect before returning from method
            try {
                cm.logOut();
            } catch (Exception e) {
                log.warn("Unexpected error while logging out ", e);
            }
        }
        log.debug("File '{}' {} deleted from ftp server. Cleanup finished.", ftpFileName, deleted?"was":"was not");
    }

    /**
     * Return a human-readable description of the object.
     *
     * @return description of object -- not machine readable
     */
    public String toString() {
        return "RemoteFile '" + file.getName() + "' (#" + checksum + ")";
    }

    /**
     * Get checksum for file, or null if checksums were not requested.
     *
     * @return checksum for file, or null if checksums were not requested.
     */
    public String getChecksum() {
        return checksum;
    }

    /**
     * Retrieval of the number of retries for retrieving a file from a FTP server. Returns the setting for number of
     * retries.
     *
     * @return The number of retries for the FTP connection, defined in settings.
     */
    @Override
    public int getNumberOfRetries() {
        return FTP_RETRIES;
    }

    public static RemoteFileSettings getRemoteFileSettings() {
        return new RemoteFileSettings(Settings.get(CommonSettings.FTP_SERVER_NAME),
                Settings.getInt(CommonSettings.FTP_SERVER_PORT), Settings.get(CommonSettings.FTP_USER_NAME),
                Settings.get(CommonSettings.FTP_USER_PASSWORD));
    }

}
