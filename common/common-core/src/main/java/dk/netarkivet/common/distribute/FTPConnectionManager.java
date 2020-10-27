package dk.netarkivet.common.distribute;

import java.io.IOException;
import java.io.Serializable;
import java.util.Calendar;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.io.CopyStreamException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.SystemUtils;
import dk.netarkivet.common.utils.TimeUtils;

public class FTPConnectionManager implements Serializable {

	
	/** A named logger for this class. */
    private static final transient Logger log = LoggerFactory.getLogger(FTPConnectionManager.class);

    /** The FTP client object for the current connection. */
    private transient FTPClient currentFTPClient;

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

	private int ftpRetries;

	private int ftpDataTimeout;
	
	public FTPConnectionManager(String ftpUserName, String ftpUserPassword,
            String ftpServerName, int ftpServerPort, int ftpRetries, int ftpDataTimeout) {
		
		this.ftpUserName = ftpUserName;
		this.ftpUserPassword = ftpUserPassword;
		this.ftpServerName = ftpServerName;
		if (ftpServerName.equalsIgnoreCase("localhost")) {
            this.ftpServerName = SystemUtils.getLocalHostName();
            log.debug("ftpServerName set to localhost on machine: {}, resetting to {}",
                    SystemUtils.getLocalHostName(), ftpServerName);
        }
		
		this.ftpServerPort = ftpServerPort;
		this.ftpRetries = ftpRetries;
		this.ftpDataTimeout = ftpDataTimeout;
	}
	
	/**
     * Create FTPClient and log on to ftp-server, if not already connected to ftp-server. Attempts to set binary mode
     * and passive mode. Will try to login up to FTP_RETRIES times, if login fails.
     */
    void logOn() {
        if (currentFTPClient != null && currentFTPClient.isConnected()) {
            return;
        } else { // create new FTPClient object and connect to ftp-server
            currentFTPClient = new FTPClient();
        }

        if (log.isDebugEnabled()) {
            log.trace("Try to logon to ftp://{}:{}@{}:{}", ftpUserName, ftpUserPassword.replaceAll(".", "*"),
                    ftpServerName, ftpServerPort);
        }

        int tries = 0;
        boolean logOnSuccessful = false;
        while (!logOnSuccessful && tries < ftpRetries) {
            tries++;
            try {
                currentFTPClient.connect(ftpServerName, ftpServerPort);
                currentFTPClient.setDataTimeout(ftpDataTimeout);
                if (!currentFTPClient.login(ftpUserName, ftpUserPassword)) {
                    final String message = "Could not log in [from host: " + SystemUtils.getLocalHostName() + "] to '"
                            + ftpServerName + "' on port " + ftpServerPort + " with user '" + ftpUserName
                            + "' password '" + ftpUserPassword.replaceAll(".", "*") + "': " + getFtpErrorMessage();
                    log.warn(message);
                    throw new IOFailure(message);
                }

                if (!currentFTPClient.setFileType(FTPClient.BINARY_FILE_TYPE)) {
                    final String message = "Could not set binary on '" + ftpServerName
                            + "', losing high bits.  Error: " + getFtpErrorMessage();
                    log.warn(message);
                    throw new IOFailure(message);
                }

                // This only means that PASV is sent before every transfer
                // command.
                currentFTPClient.enterLocalPassiveMode();

                log.debug("w/ DataTimeout (ms): {}", currentFTPClient.getDefaultTimeout());
                logOnSuccessful = true;
            } catch (IOException e) {
                final String msg = "Connect to " + ftpServerName + " from host: " + SystemUtils.getLocalHostName()
                        + " failed";
                if (tries < ftpRetries) {
                    log.debug(
                            "{}. Attempt #{} of max {}. Will sleep a while before trying to connect again. Exception: ",
                            msg, tries, ftpRetries);
                    TimeUtils.exponentialBackoffSleep(tries, Calendar.MINUTE);
                } else {
                    log.warn("{}. This was the last (#{}) connection attempt", msg, tries);
                    throw new IOFailure(msg, e);
                }
            }
        }

        if (log.isDebugEnabled()) {
            log.debug("Logged onto ftp://{}:{}@{}:{}", ftpUserName, ftpUserPassword.replaceAll(".", "*"),
                    ftpServerName, ftpServerPort);
        }
    }

    /**
     * Get the reply code and string from the ftp client.
     *
     * @return A string with the FTP servers last reply code and message.
     */
    public String getFtpErrorMessage() {
        return ("Error " + currentFTPClient.getReplyCode() + ": '" + currentFTPClient.getReplyString() + "'");
    }

    /**
     * Log out from the FTP server.
     */
    void logOut() {
        log.debug("Trying to log out.");
        try {
            if (currentFTPClient != null) {
                currentFTPClient.disconnect();
            }
        } catch (IOException e) {
            String msg = "Disconnect from '" + ftpServerName + "' failed ";
            if (e instanceof CopyStreamException) {
                CopyStreamException realException = (CopyStreamException) e;
                msg += "(real cause = " + realException.getIOException() + ")";
            }
            log.warn(msg, e);
        }
    }

	public FTPClient getFTPClient() {
		return currentFTPClient;
	}

	public String getFtpServer() {
		return ftpServerName;
	}

}
