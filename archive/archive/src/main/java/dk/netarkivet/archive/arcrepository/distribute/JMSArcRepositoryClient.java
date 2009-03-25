/*$Id$
* $Revision$
* $Date$
* $Author$
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
package dk.netarkivet.archive.arcrepository.distribute;

import java.io.File;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.archive.arcrepository.bitpreservation.AdminDataMessage;
import dk.netarkivet.archive.bitarchive.distribute.BatchMessage;
import dk.netarkivet.archive.bitarchive.distribute.BatchReplyMessage;
import dk.netarkivet.archive.bitarchive.distribute.GetFileMessage;
import dk.netarkivet.archive.bitarchive.distribute.GetMessage;
import dk.netarkivet.archive.bitarchive.distribute.RemoveAndGetFileMessage;
import dk.netarkivet.common.distribute.ChannelID;
import dk.netarkivet.common.distribute.Channels;
import dk.netarkivet.common.distribute.JMSConnection;
import dk.netarkivet.common.distribute.JMSConnectionFactory;
import dk.netarkivet.common.distribute.NetarkivetMessage;
import dk.netarkivet.common.distribute.RemoteFile;
import dk.netarkivet.common.distribute.Synchronizer;
import dk.netarkivet.common.distribute.arcrepository.ArcRepositoryClient;
import dk.netarkivet.common.distribute.arcrepository.BatchStatus;
import dk.netarkivet.common.distribute.arcrepository.BitArchiveStoreState;
import dk.netarkivet.common.distribute.arcrepository.BitarchiveRecord;
import dk.netarkivet.common.distribute.arcrepository.Replica;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.NetarkivetException;
import dk.netarkivet.common.utils.ExceptionUtils;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.NotificationsFactory;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.utils.batch.FileBatchJob;

/**
 * Client side usage of an arc repository.
 * All requests are forwarded to the ArcRepositoryServer over the network.
 * get and store messages are retried a number of time before giving up, and
 * will timeout after a specified time.
 */
public class JMSArcRepositoryClient extends Synchronizer implements
        ArcRepositoryClient {
    
    /** The default place in classpath where the settings file can be found. */
    private static String DEFAULT_SETTINGS_CLASSPATH
            = "dk/netarkivet/archive/arcrepository/distribute/JMSArcRepositoryClientSettings.xml";
    
    /*
     * The static initialiser is called when the class is loaded.
     * It will add default values for all settings defined in this class, by
     * loading them from a settings.xml file in classpath.
     */
    static {
        Settings.addDefaultClasspathSettings(DEFAULT_SETTINGS_CLASSPATH);
    }

    /** the one and only JMSArcRepositoryClient instance. */
    private static JMSArcRepositoryClient instance;

    /**
     * Logging output place.
     */
    protected final Log log = LogFactory.getLog(getClass());

    /** Listens on this queue for replies. */
    private ChannelID replyQ;
    /** Connection to JMS-broker. */
    private JMSConnection conn;

    /** The number of times to try sending a store message before giving up. */
    private long storeRetries;

    /** The length of time to wait for a store reply before giving up. */
    private long storeTimeout;

    /** The length of time to wait for a get reply before giving up. */
    private long getTimeout;

    // NOTE: The constants defining setting names below are left non-final on
    // purpose! Otherwise, the static initialiser that loads default values
    // will not run.

    /**
     * <b>settings.common.arcrepositoryClient.getTimeout</b>: <br>
     * The setting for how many milliseconds we will wait before giving up
     * on a lookup request to the Arcrepository.
     */
    public static final String ARCREPOSITORY_GET_TIMEOUT
            = "settings.common.arcrepositoryClient.getTimeout";

    /**
     * <b>settings.common.arcrepositoryClient.storeRetries</b>: <br>
     * The setting for the number of times to try sending a store message
     * before failing, including the first attempt.
     */
    public static String ARCREPOSITORY_STORE_RETRIES
            = "settings.common.arcrepositoryClient.storeRetries";

    /**
     * <b>settings.common.arcrepositoryClient.storeTimeout</b>: <br>
     * the setting for the timeout in milliseconds before retrying when calling
     * {@link ArcRepositoryClient#store(File)}.
     */
    public static final String ARCREPOSITORY_STORE_TIMEOUT
            = "settings.common.arcrepositoryClient.storeTimeout";

    /**
     * Adds this Synchronizer as listener on a jms connection.
     */
    protected JMSArcRepositoryClient() {
        storeRetries = Settings.getLong(
                ARCREPOSITORY_STORE_RETRIES);
        storeTimeout = Settings.getLong(
                ARCREPOSITORY_STORE_TIMEOUT);
        getTimeout = Settings.getLong(ARCREPOSITORY_GET_TIMEOUT);
        
        log.info("JMSArcRepositoryClient will retry a store " + storeRetries
                   + " times and timeout on each try after " + storeTimeout
                   + " milliseconds, and timeout on each getrequest after "
                   + getTimeout + " milliseconds.");
        replyQ = Channels.getThisReposClient();
        conn = JMSConnectionFactory.getInstance();
        conn.setListener(replyQ, this);
        log.info("JMSArcRepository listens for replies on channel '"
                 + replyQ + "'");
    }
    
    /**
     * Get an JMSArcRepositoryClient instance.
     * This is guaranteed to be a singleton. 
     * @return an JMSArcRepositoryClient instance.
     */
    public static synchronized JMSArcRepositoryClient getInstance() {
        if (instance == null) {
            instance = new JMSArcRepositoryClient();
        }
        return instance;
    }

    /**
     * Removes this object as a JMS listener.
     */
    public void close() {
        synchronized (this.getClass()) {
            if (conn != null) {
                conn.removeListener(replyQ, this);
                conn = null;
            }
            instance = null;
        }
    }

    /**
     * Sends a GetMessage on the "TheArcrepos" queue and waits for a reply.
     * This is a blocking call. Returns null if no message is returned
     * within Settings.ARCREPOSITORY_GET_TIMEOUT
     * @param arcfile The name of a file.
     * @param index   The offset of the wanted record in the file
     * @return a BitarchiveRecord-object or null if request times out or object
     * is not found.
     * @throws ArgumentNotValid If the given arcfile is null or empty, 
     * or the given index is negative.
     * @throws IOFailure If a wrong message is returned or the
     * get operation failed.
     */
    public BitarchiveRecord get(String arcfile, long index) throws ArgumentNotValid {
        ArgumentNotValid.checkNotNullOrEmpty(arcfile, "arcfile");
        ArgumentNotValid.checkNotNegative(index, "index");
        log.debug("Requesting get of record '" + arcfile + ":" + index + "'");
        long start = System.currentTimeMillis();
        GetMessage requestGetMsg = new GetMessage(Channels.getTheRepos(),
                replyQ, arcfile, index);
        NetarkivetMessage replyNetMsg = sendAndWaitForOneReply(requestGetMsg,
                getTimeout);
        long timePassed = System.currentTimeMillis() - start;
        log.debug("Reply received after " + (timePassed / 1000) + " seconds");
        if (replyNetMsg == null) {
            log.info("Request for record(" + arcfile + ":" + index
                    + ") timed out after "
                    + (getTimeout / 1000)
                    + " seconds. Returning null BitarchiveRecord");
            return null;
        }
        GetMessage replyGetMsg;
        try {
            replyGetMsg = (GetMessage) replyNetMsg;
        } catch (ClassCastException e) {
            String errorMsg = "Received invalid argument reply: '"
                              + replyNetMsg + "'";
            log.warn(errorMsg, e);
            throw new ArgumentNotValid(errorMsg, e);
        }
        if (!replyGetMsg.isOk()) {
            final String errMsg = "GetMessage failed: '"
                + replyGetMsg.getErrMsg() + "'"; 
            log.warn(errMsg);
            throw new ArgumentNotValid(errMsg);
        }
        return replyGetMsg.getRecord();
    }

    /**
     * Synchronously retrieves a file from a bitarchive and places it in a
     * local file.
     * This is the interface for sending GetFileMessage on the "TheArcrepos"
     * queue.
     * This is a blocking call.
     * @param arcfilename    Name of the arcfile to retrieve.
     * @param replica   The bitarchive to retrieve the data from.
     * @param toFile Filename of a place where the file fetched can be put.
     * @throws IOFailure if there are problems getting a reply or the file
     * could not be found.
     */
    public void getFile(String arcfilename, Replica replica, File toFile) {
        ArgumentNotValid.checkNotNullOrEmpty(arcfilename, "arcfilename");
        ArgumentNotValid.checkNotNull(replica, "replica");
        ArgumentNotValid.checkNotNull(toFile, "toFile");
        
        log.debug("Requesting get of file '" + arcfilename + "' from '"
                  + replica + "'");
        GetFileMessage gfMsg = new GetFileMessage(Channels.getTheRepos(),
                replyQ, arcfilename, replica.getId());
        GetFileMessage getFileMessage
                = (GetFileMessage) sendAndWaitForOneReply(gfMsg, 0);
        if (getFileMessage == null) {
            String msg = "GetFileMessage timed out before returning."
                         + "File not found?";
            log.debug(msg);
            throw new IOFailure(msg);
        } else if (!getFileMessage.isOk()) {
            String msg = "GetFileMessage failed: " + getFileMessage.getErrMsg();
            log.warn(msg);
            throw new IOFailure(msg);
        } else {
            getFileMessage.getData(toFile);
        }
    }

    /**
     * Sends a StoreMessage via the synchronized JMS connection method
     * sendAndWaitForOneReply(). After a successful storage operation,
     * both the local copy of the file and the copy on the ftp server are
     * deleted.
     * @param file A file to be stored. Must exist.
     * @throws IOFailure thrown if store is unsuccessful, or failed to clean
     * up files locally or on the ftp server after the store operation.
     * @throws ArgumentNotValid if file parameter is null or file is not an
     *                          existing file.
     */
    public void store(File file) throws IOFailure, ArgumentNotValid {
        ArgumentNotValid.checkNotNull(file, "file");
        ArgumentNotValid.checkTrue(file.isFile(),
                "The file '" + file.getPath() + "' is "
                + "not an existing file.");

        StringBuilder messages = new StringBuilder();
        for (long i = 0; i < storeRetries; i++) {
            StoreMessage outMsg = null;
            try {
                log.debug("Sending a StoreMessage with file '" + file.getPath()
                        + "'");
                outMsg = new StoreMessage(replyQ, file);
                NetarkivetMessage replyMsg
                        = sendAndWaitForOneReply(outMsg, storeTimeout);
                if (replyMsg != null && replyMsg.isOk()) {
                    try {
                        FileUtils.removeRecursively(file);
                    } catch (IOFailure e) {
                        log.warn("Failed to clean up '"
                                + file.getAbsolutePath() + "'", e);
                        // Not fatal
                    }
                    return;
                } else if (replyMsg == null) {
                    String msg = "Timed out"
                            + " while waiting for reply on store of file '"
                            + file.getPath() + "' on attempt number " + (i + 1)
                            + " of " + storeRetries;
                    log.warn(msg);
                    messages.append(msg + "\n");
                } else {
                    String msg = "The returned message '" + replyMsg
                            + "' was not ok"
                            + " while waiting for reply on store of file '"
                            + file.getPath() + "' on attempt number " + (i + 1)
                            + " of " + storeRetries + ". Error message was '"
                            + replyMsg.getErrMsg() + "'";
                    log.warn(msg);
                    messages.append(msg + "\n");
                }
            } catch (NetarkivetException e) {
                String msg = "Client-side exception occurred while storing '"
                        + file.getPath() + "' on attempt number " + (i + 1)
                        + " of " + storeRetries + ".";
                log.warn(msg, e);
                messages.append(msg + "\n");
                messages.append(ExceptionUtils.getStackTrace(e));
            } finally {
                if (outMsg != null) {
                    cleanUpAfterStore(outMsg);
                }
            }
        }
        String errMsg = "Could not store '" + file.getPath() + "' after "
                        + storeRetries + " attempts. Giving up.\n" + messages;
        log.warn(errMsg);
        NotificationsFactory.getInstance().errorEvent(errMsg);
        throw new IOFailure(errMsg);
    }

    /**
     * Tries to clean up a file on the FTP server after a store operation. Will
     * not throw exception on error, merely log exception.
     * @param m the StoreMessage sent back as reply
     */
    private void cleanUpAfterStore(StoreMessage m) {
        RemoteFile rf = null;
        try {
            rf = m.getRemoteFile();
        } catch (Exception e) {
            log.warn("Could not get remote file object from message " + m, e);
        }
        try {
            rf.cleanup();
        } catch (Exception e) {
            log.warn("Could not delete remote file on ftp server: " + rf, e);
        }
    }

    /**
     * Sends a BatchMessage to the Arcrepos queue and waits for the
     * BatchReplyMessage reply before returning.
     *
     * @param job        An object that implements the FileBatchJob interface.
     *                   The initialize() method will be called before
     *                   processing and the finish()
     *                   method will be called afterwards.  The process() method
     *                   will be called
     *                   with each File entry.
     * @param replicaId The id of the archive to execute the job on
     * @return           A local batch status
     * @throws IOFailure if no results can be read at all
     */
    public BatchStatus batch(FileBatchJob job, String replicaId) {
        ArgumentNotValid.checkNotNull(job, "job");
        ArgumentNotValid.checkNotNullOrEmpty(replicaId, "replicaId");

        log.debug("Starting batchjob '" + job + "' running on replica '"
                + replicaId +"'");
        BatchMessage bMsg = new BatchMessage(Channels.getTheRepos(), replyQ,
                job, replicaId);
        log.debug("Sending batchmessage to queue '" + Channels.getTheRepos()
                + "' with replyqueue set to '" + replyQ + "'");
        BatchReplyMessage brMsg =
            (BatchReplyMessage) sendAndWaitForOneReply(bMsg, 0);
        if (!brMsg.isOk()) {
            String msg = "The batch job '" + bMsg
                         + "' resulted in the following "
                         + "error: " + brMsg.getErrMsg();
            log.warn(msg);
            if (brMsg.getResultFile() == null) {
                // If no result is available at all, this is non-recoverable
                throw new IOFailure(msg);
            }
        }
        BatchStatus lbs = new BatchStatus(brMsg.getFilesFailed(),
                brMsg.getNoOfFilesProcessed(), brMsg.getResultFile(),
                job.getExceptions());
        return lbs;
    }

    /** Request update of admin data to specific state.
     *
     * @param fileName The file for which admin data should be updated.
     * @param bitarchiveName The bitarchive for which admin data should be
     * updated.
     * @param newval The new value in admin data.
     * @throws IOFailure If the reply to the request update timed out.
     * */
    public void updateAdminData(String fileName, String bitarchiveName,
            BitArchiveStoreState newval) {
        ArgumentNotValid.checkNotNullOrEmpty(fileName, "fileName");
        ArgumentNotValid.checkNotNullOrEmpty(bitarchiveName, "bitarchiveName");
        ArgumentNotValid.checkNotNull(newval, "newval");
        
        String msg = "Requesting update of admin data for file '" + fileName
                     + "' bitarchive '" + bitarchiveName + "' to state "
                     + newval;
        log.warn(msg);
        NotificationsFactory.getInstance().errorEvent(msg);
        AdminDataMessage aMsg =
            new AdminDataMessage(fileName, bitarchiveName, newval);
        // We only need to know that a reply to our message has arrived. 
        // The replyMessage is thrown away, because it does not contain 
        // any more useful knowledge.
        sendAndWaitForOneReply(aMsg, 0);
    }

    /** Request update of admin data to specific checksum.
     *
     * @param filename The file for which admin data should be updated.
     * @param checksum The new checksum for the file
     * @throws IOFailure If the reply to the request update timed out.
     */
    public void updateAdminChecksum(String filename, String checksum) {
        ArgumentNotValid.checkNotNullOrEmpty(filename, "filename");
        ArgumentNotValid.checkNotNullOrEmpty(checksum, "checksum");
        
        String msg = "Requesting update of admin data for file '" + filename
                     + "' to checksum '" + checksum;
        log.warn(msg);
        NotificationsFactory.getInstance().errorEvent(msg);
        AdminDataMessage aMsg = new AdminDataMessage(filename, checksum);
        // We only need to know that a reply to our message has arrived. 
        // The replyMessage is thrown away, because it does not contain 
        // any more useful knowledge.
        sendAndWaitForOneReply(aMsg, 0);
    }

    /**
     * Removes a file from the bitarchives, if given credentials and checksum
     * are correct.
     *
     * @param fileName The name of the file to delete
     * @param bitarchiveId The id of the bitarchive to delete the file in
     * @param checksum The checksum of the deleted file
     * @param credentials The credentials used to delete the file
     * @throws ArgumentNotValid if arguments are null or
     *  equal to the empty string
     * @throws IOFailure if we could not delete the remote file, ors
     * there was no response to our RemoveAndGetFileMessage within the allotted
     * time defined by the setting 
     * {@link JMSArcRepositoryClient#ARCREPOSITORY_STORE_TIMEOUT}.
     * @return The file that was removed
     */
    public File removeAndGetFile(String fileName, String bitarchiveId,
                                 String checksum, String credentials) {
        ArgumentNotValid.checkNotNullOrEmpty(fileName, "filename");
        ArgumentNotValid.checkNotNullOrEmpty(bitarchiveId, "bitarchiveName");
        ArgumentNotValid.checkNotNullOrEmpty(checksum, "checksum");
        ArgumentNotValid.checkNotNullOrEmpty(credentials, "credentials");
        
        String msg = "Requesting remove of file '" + fileName
                     + "' with checksum '"
                     + checksum + "' from bitarchive '" + bitarchiveId + "'";
        log.warn(msg);
        NotificationsFactory.getInstance().errorEvent(msg);
        RemoveAndGetFileMessage aMsg =
            new RemoveAndGetFileMessage(fileName, bitarchiveId,
                                        checksum, credentials);
        RemoveAndGetFileMessage replyMsg =
            (RemoveAndGetFileMessage)
            sendAndWaitForOneReply(aMsg, storeTimeout);

        // The removed file is returned, move to temp location
        if (replyMsg != null) {
            if (replyMsg.isOk()) {
                File removedFile = replyMsg.getData();
                log.debug("Stored copy of removed file: " + fileName + "   as: "
                         + removedFile.getAbsolutePath());
                return removedFile;
            } else {
                String message = "Could not delete remote file: "
                                 + replyMsg.getErrMsg();
                log.warn(message);
                throw new IOFailure(message);
            }
        } else {
            String message = "Request timed out while requesting remove of "
                             + "file '" + fileName + "' in bitarchive '"
                             + bitarchiveId + "'";
            log.warn(message);
            throw new IOFailure(message);
        }
    }
}
