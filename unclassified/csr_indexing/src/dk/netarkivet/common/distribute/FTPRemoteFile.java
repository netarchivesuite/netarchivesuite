/* $Id$
 * $Revision$
 * $Date$
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.net.ftp.FTPClient;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.MD5;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.utils.SystemUtils;

/**
 * Class encapsulating upload to & download from an ftp-server.
 *
 * Transfers are done using binary type and passive mode, if available.
 */
final public class FTPRemoteFile extends AbstractRemoteFile {
    /** The default place in classpath where the settings file can be found. */
    private static final String DEFAULT_SETTINGS_CLASSPATH
            = "dk/netarkivet/common/distribute/FTPRemoteFileSettings.xml";

    /*
     * The static initialiser is called when the class is loaded.
     * It will add default values for all settings defined in this class, by
     * loading them from a settings.xml file in classpath.
     */
    static {
        Settings.addDefaultClasspathSettings(
                DEFAULT_SETTINGS_CLASSPATH
        );
    }

    /**
     * A named logger for this class.
     */
    private static final transient Log log =
            LogFactory.getLog(FTPRemoteFile.class.getName());

    /**
     * Ftp-connection information. Read ftp-related settings from settings.xml.
     * Notice that these settings get transferred to the receiver, which is
     * necessary to allow the receiver to get data from different servers.
     */
    private String ftpServerName = Settings.get(FTP_SERVER_NAME);

    /** The ftp-server port. */
    private final int ftpServerPort = Settings.getInt(
            FTP_SERVER_PORT);
    /** The username used to connect to the ftp-server. */
    private final String ftpUserName = Settings.get(
            FTP_USER_NAME);
    /** The password used to connect to the ftp-server. */
    private final String ftpUserPassword = Settings.get(
            FTP_USER_PASSWORD);

    /**
     * The FTP client object for the current connection.
     */
    private transient FTPClient currentFTPClient;

    /**
     * The name that we use for the file on the FTP server.  This is only for
     * internal use.
     */
    private final String ftpFileName;

    /** If useChecksums is true, contains the file checksum. */
    protected final String checksum;

    // NOTE: The constants defining setting names below are left non-final on
    // purpose! Otherwise, the static initialiser that loads default values
    // will not run.

    /** 
     * <b>settings.common.remoteFile.serverName</b>: <br>
     * The setting for the FTP-server used. */
    public static String FTP_SERVER_NAME
            = "settings.common.remoteFile.serverName";
    
    /** 
     * <b>settings.common.remoteFile.serverPort</b>: <br>
     * The setting for the FTP-server port used. */
    public static String FTP_SERVER_PORT
            = "settings.common.remoteFile.serverPort";

    /** 
     * <b>settings.common.remoteFile.userName</b>: <br>
     * The setting for the FTP username. */
    public static String FTP_USER_NAME
            = "settings.common.remoteFile.userName";

    /** 
     * <b>settings.common.remoteFile.userPassword</b>: <br>
     * The setting for the FTP password. * */
    public static String FTP_USER_PASSWORD
            = "settings.common.remoteFile.userPassword";
    
    /**
     * <b>settings.common.remoteFile.retries</b>: <br>
     * The setting for the number of times FTPRemoteFile should try before
     * giving up a copyTo operation. */
    public static final String FTP_COPYTO_RETRIES_SETTINGS
            = "settings.common.remoteFile.retries";

    /**
     * How many times we will retry upload and download.
     */
    private static final transient int FTP_COPYTO_RETRIES
            = Settings.getInt(FTP_COPYTO_RETRIES_SETTINGS);

    /**
     * Private constructor used by getInstance() static-method Tries to generate
     * unique name on ftp-server.
     *
     * @param localFile         File used to create new file on ftp-server.
     * @param useChecksums      If true, checksums will be used to check
     *                          transfers.
     * @param fileDeletable     If true, this file will be deleted after upload
     *                          to FTP.
     * @param multipleDownloads If true, the file will not be removed from FTP
     *                          server automatically after first download.
     * @throws IOFailure        if MD5 checksum fails, or ftp fails
     * @throws ArgumentNotValid if the local file cannot be read.
     */
    private FTPRemoteFile(File localFile, boolean useChecksums,
                          boolean fileDeletable, boolean multipleDownloads)
            throws IOFailure {
        super(localFile, useChecksums, fileDeletable, multipleDownloads);

        if (filesize == 0) {
            try {
                if (useChecksums) {
                    checksum = MD5.generateMD5onFile(file);
                } else {
                    checksum = null;
                }
                ftpFileName = "-";
            } catch (IOException e) {
                throw new IOFailure("I/O trouble generating checksum on file '"
                                    + file.getAbsolutePath() + "'", e);
            }
        } else {
            // If the ftpServerName is localhost, it is not going to work across
            // a network.  Warn about this.
            if (ftpServerName.equalsIgnoreCase("localhost")) {
                ftpServerName = SystemUtils.getLocalHostName();
                log.debug("ftpServerName set to localhost on machine: "
                          + SystemUtils.getLocalHostName() + ", resetting to "
                          + ftpServerName);
            }
            // A large enough number to make it unlikely that two files are
            // created with the same FTP server name.  Already the millisecond
            // datestamp reduces likelyhood, with this even if two
            // processes/threads try to upload the same file in the same
            // millisecond (very unlikely) they have only .01% chance of
            // clashing.
            final int aMagicNumber = 100000;
            ftpFileName = file.getName() + "-"
                          + (int) (Math.random() * aMagicNumber) + "-"
                          + new Date().getTime();
            InputStream in;
            try {
                in = new FileInputStream(localFile);
            } catch (FileNotFoundException e) {
                final String message = "Couldn't prepare file '"
                                       + localFile
                                       + "' for remote access. File not found.";
                log.debug(message, e);
                throw new IOFailure(message, e);
            }
            log.debug("Writing '" + file.getName() + "' as '" + ftpFileName
                      + "' on ftp-server " + ftpServerName);

            // Writing inlined in constructor to allow the checksum field to
            // be final (and thus must be set in constructor).
            try {
                logOn();
                if (useChecksums) {
                    in = new DigestInputStream(in,
                                               MD5.getMessageDigestInstance());
                }
                boolean success = false;
                int tried = 0;
                while (!success && tried < FTP_COPYTO_RETRIES) {
                    tried++;
                    try {
                        success = currentFTPClient.storeFile(ftpFileName, in);
                        if (!success) {
                            log.debug("FTP store failed attempt '" + tried
                                      + "' of " + FTP_COPYTO_RETRIES
                                      + ": " + getFtpErrorMessage());
                        }
                    } catch (IOException e) {
                        final String message = "Write operation to '"
                                               + ftpFileName
                                               + "' failed on attempt " + tried
                                               + " of " + FTP_COPYTO_RETRIES;
                        log.debug(message, e);
                    }
                }
                if (!success) {
                    final String msg = "Failed to upload '" + localFile
                                       + "' after "
                                       + tried + " attempts";
                    log.warn(msg);
                    throw new IOFailure(msg);
                }
                log.debug("Completed writing the file '" + ftpFileName + "'");

                if (useChecksums) {
                    checksum = MD5.toHex(
                            ((DigestInputStream) in).getMessageDigest()
                                    .digest());
                    log.debug(
                            "Checksum of '" + ftpFileName + "' is:" + checksum);
                } else {
                    checksum = null;
                }
            } finally {
                try {
                    if (in != null) {
                        in.close();
                    }
                } catch (IOException e) {
                    log.warn("Problem closing inputstream");
                    // not a serious bug
                }
                logOut();
                log.debug("Ftp logout");
            }
        }
        if (fileDeletable) {
            try {
                FileUtils.removeRecursively(localFile);
            } catch (IOFailure e) {
                log.warn("Couldn't remove tmp file "
                         + localFile, e);
                // Not fatal
            }
        }
    }

    /**
     * Create a remote file that handles the transport of the remote file data.
     * This method is used by the sender to prepare the transport.
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
    public static final RemoteFile getInstance(File localFile,
                                               Boolean useChecksums,
                                               Boolean fileDeletable,
                                               Boolean multipleDownloads)
            throws IOFailure {
        ArgumentNotValid.checkNotNull(localFile, "File remoteFile");
        return new FTPRemoteFile(localFile, useChecksums,
                                 fileDeletable, multipleDownloads);
    }

    /**
     * An implementation of the getInputStream operation that works with FTP.
     * Notice that most of the special work (logging out and checking MD5)
     * happens in the close() method of the returned InputStream, since that is
     * the only place where we can know we're done.
     *
     * @return An InputStream that will deliver the data transferred by FTP.
     *         Holding on to this for long periods without reading any data
     *         might cause a timeout.
     */
    public InputStream getInputStream() {
        if (filesize == 0) {
            return new ByteArrayInputStream(new byte[]{});
        }
        try {
            logOn();

            InputStream in = currentFTPClient.retrieveFileStream(ftpFileName);
            if (in == null) {
                throw new IOFailure("Unable to retrieve input stream:"
                                    + getFtpErrorMessage());
            }
            if (useChecksums) {
                in = new DigestInputStream(in, MD5.getMessageDigestInstance());
            }
            return new FilterInputStream(in) {
                public void close() throws IOException {
                    try {
                        super.close();
                        if (useChecksums) {
                            String newChecksum =
                                    MD5.toHex(((DigestInputStream) in)
                                            .getMessageDigest().digest());
                            if (!newChecksum.equals(checksum)) {
                                final String msg = "Checksums of '"
                                                   + ftpFileName
                                                   + "' do not match! Should be "
                                                   + checksum
                                                   + " but was " + newChecksum;
                                log.warn(msg);
                                throw new IOFailure(msg);
                            }
                        }
                    } finally {
                        logOut();
                        if (!multipleDownloads) {
                            cleanup();
                        }
                    }
                }
            };
        } catch (IOException e) {
            final String msg = "Creating inputstream from '" + ftpFileName
                               + "' failed";
            log.warn(msg, e);
            throw new IOFailure(msg, e);
        }
    }

    /**
     * Write the contents of this ftp remote file to an output stream.
     * Notice that while the checksum of the transferred data is checked, no
     * retries are performed, and in case of failure, there is no guarantee
     * that any data have been transferred.
     *
     * @param out OutputStream that the data will be written to.  This stream
     * will not be closed by this operation.
     * @throws IOFailure If append operation fails
     */
    public void appendTo(OutputStream out) {
        ArgumentNotValid.checkNotNull(out, "OutputStream out");

        if (filesize == 0) {
            return;
        }

        try {
            logOn();

            if (useChecksums) {
                out = new DigestOutputStream(out,
                        MD5.getMessageDigestInstance());
            }
            if (!currentFTPClient.retrieveFile(ftpFileName, out)) {
                final String msg = "Append operation from '"
                        + ftpFileName + "' failed: " + getFtpErrorMessage();
                log.warn(msg);
                throw new IOFailure(msg);
            }
            out.flush();
            if (useChecksums) {
                String newChecksum = MD5.toHex(((DigestOutputStream) out)
                        .getMessageDigest().digest());
                if (checksum != null && !checksum.equals(newChecksum)) {
                    final String msg = "Checksums of '" + ftpFileName
                            + "' do not match! Should be " + checksum
                            + " but was " + newChecksum;
                    log.warn(msg);
                    throw new IOFailure(msg);
                }
            }
        } catch (IOException e) {
            final String msg = "Append operation from '" + ftpFileName
                    + "' failed";
            log.warn(msg, e);
            throw new IOFailure(msg, e);
        } finally {
            logOut();
            if (!multipleDownloads) {
                cleanup();
            }
        }
    }

    /**
     * Cleanup will delete the file on the FTP server. This method should never
     * throw exceptions. It is idempotent, meaning it can be called twice
     * without trouble.
     */
    public void cleanup() {
        if (filesize == 0) {
            return;
        }
        log.debug("Deleting file '" + ftpFileName + "' from ftp server");
        try {
            logOn();
            currentFTPClient.deleteFile(ftpFileName);
        } catch (Exception e) {
            log.warn("Error while deleting ftp file '" + ftpFileName
                     + "' for file '" + file.getName() + "'", e);
        } finally {
            // try to disconnect before returning from method
            try {
                logOut();
            } catch (Exception e) {
                log.warn("Error while deleting ftp file '" + ftpFileName
                         + "' for file '" + file.getName() + "'", e);
            }
        }
        log.debug("File '" + ftpFileName + "' deleted from ftp server. "
                + "Cleanup finished.");
    }

    /**
     * Create FTPClient and log on to ftp-server, if not already connected to
     * ftp-server.  Attempts to set binary mode and passive mode.
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

        try {
            currentFTPClient.connect(ftpServerName, ftpServerPort);

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

            // This only means that PASV is sent before every transfer command.
            currentFTPClient.enterLocalPassiveMode();
        } catch (IOException e) {
            final String msg = "Connect to " + ftpServerName + " from host: "
                               + SystemUtils.getLocalHostName() + " failed";
            log.warn(msg, e);
            throw new IOFailure(msg, e);
        }

        log.debug("Logged onto ftp://" + ftpUserName + ":"
                  + ftpUserPassword.replaceAll(".", "*")
                  + "@" + ftpServerName + ":" + ftpServerPort);
    }

    /**
     * Log out from the FTP server.
     *
     * @throws IOFailure if disconnecting fails.
     */
    private void logOut() {
        try {
            if (currentFTPClient != null) {
                currentFTPClient.disconnect();
            }
        } catch (IOException e) {
            final String msg = "Disconnect from '" + ftpServerName
                               + "' failed";
            log.warn(msg, e);
            throw new IOFailure(msg, e);
        } catch (NullPointerException e) {
            /*
            * The currentFTPClient.disconnect() call occasionally
            * generates NullPointer-exception. This is a known bug:
            * http://issues.apache.org/bugzilla/show_bug.cgi?id=26296
            * We can ignore this exception.
            */
        }
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
     * Return a human-readable description of the object.
     *
     * @return description of object -- do not machineparse.
     */
    public String toString() {
        return "RemoteFile '" + file.getName() + "' (#" + checksum + ")";
    }

    /** Get checksum for file, or null if checksums were not requested.
     * @return checksum for file, or null if checksums were not requested.
     */
    public String getChecksum() {
        return checksum;
    }
}

