/*$Id$
* $Revision$
* $Date$
* $Author$
*
* The Netarchive Suite - Software to harvest and preserve websites
* Copyright 2004-2010 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.archive.arcrepository.bitpreservation.AdminDataMessage;
import dk.netarkivet.archive.bitarchive.distribute.BatchMessage;
import dk.netarkivet.archive.bitarchive.distribute.BatchReplyMessage;
import dk.netarkivet.archive.bitarchive.distribute.GetFileMessage;
import dk.netarkivet.archive.bitarchive.distribute.GetMessage;
import dk.netarkivet.archive.bitarchive.distribute.RemoveAndGetFileMessage;
import dk.netarkivet.archive.checksum.distribute.CorrectMessage;
import dk.netarkivet.archive.checksum.distribute.GetAllChecksumsMessage;
import dk.netarkivet.archive.checksum.distribute.GetAllFilenamesMessage;
import dk.netarkivet.archive.checksum.distribute.GetChecksumMessage;
import dk.netarkivet.common.distribute.ChannelID;
import dk.netarkivet.common.distribute.Channels;
import dk.netarkivet.common.distribute.JMSConnectionFactory;
import dk.netarkivet.common.distribute.NetarkivetMessage;
import dk.netarkivet.common.distribute.RemoteFile;
import dk.netarkivet.common.distribute.RemoteFileFactory;
import dk.netarkivet.common.distribute.Synchronizer;
import dk.netarkivet.common.distribute.arcrepository.ArcRepositoryClient;
import dk.netarkivet.common.distribute.arcrepository.BatchStatus;
import dk.netarkivet.common.distribute.arcrepository.ReplicaStoreState;
import dk.netarkivet.common.distribute.arcrepository.BitarchiveRecord;
import dk.netarkivet.common.distribute.arcrepository.Replica;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.ExceptionUtils;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.NotificationsFactory;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.utils.batch.FileBatchJob;

/**
 * Client side usage of an arc repository. All requests are forwarded to the
 * ArcRepositoryServer over the network. get and store messages are retried a
 * number of time before giving up, and will timeout after a specified time.
 */
public class JMSArcRepositoryClient extends Synchronizer implements
                                                         ArcRepositoryClient {
    /** The default place in classpath where the settings file can be found. */
    private static String defaultSettingsClasspath
            = "dk/netarkivet/archive/arcrepository/distribute/"
                +"JMSArcRepositoryClientSettings.xml";

    /*
     * The static initialiser is called when the class is loaded.
     * It will add default values for all settings defined in this class, by
     * loading them from a settings.xml file in classpath.
     */
    static {
        Settings.addDefaultClasspathSettings(defaultSettingsClasspath);
    }
    
    /** The amount of milliseconds in a second. 1000. */
    private static final int MILLISECONDS_PER_SECOND = 1000;

    /** the one and only JMSArcRepositoryClient instance. */
    private static JMSArcRepositoryClient instance;

    /** Logging output place. */
    protected final Log log = LogFactory.getLog(getClass());

    /** Listens on this queue for replies. */
    private final ChannelID replyQ;

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
     * <b>settings.common.arcrepositoryClient.getTimeout</b>: <br> The setting
     * for how many milliseconds we will wait before giving up on a lookup
     * request to the Arcrepository.
     */
    public static final String ARCREPOSITORY_GET_TIMEOUT
            = "settings.common.arcrepositoryClient.getTimeout";

    /**
     * <b>settings.common.arcrepositoryClient.storeRetries</b>: <br> The setting
     * for the number of times to try sending a store message before failing,
     * including the first attempt.
     */
    public static final String ARCREPOSITORY_STORE_RETRIES
            = "settings.common.arcrepositoryClient.storeRetries";

    /**
     * <b>settings.common.arcrepositoryClient.storeTimeout</b>: <br> the setting
     * for the timeout in milliseconds before retrying when calling {@link
     * ArcRepositoryClient#store(File)}.
     */
    public static final String ARCREPOSITORY_STORE_TIMEOUT
            = "settings.common.arcrepositoryClient.storeTimeout";

    /** Adds this Synchronizer as listener on a jms connection. */
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
        JMSConnectionFactory.getInstance().setListener(replyQ, this);
        log.info("JMSArcRepository listens for replies on channel '"
                 + replyQ + "'");
    }

    /**
     * Get an JMSArcRepositoryClient instance. This is guaranteed to be a
     * singleton.
     *
     * @return an JMSArcRepositoryClient instance.
     */
    public static synchronized JMSArcRepositoryClient getInstance() {
        if (instance == null) {
            instance = new JMSArcRepositoryClient();
        }
        return instance;
    }

    /** Removes this object as a JMS listener. */
    public void close() {
        synchronized (JMSArcRepositoryClient.class) {
            JMSConnectionFactory.getInstance().removeListener(replyQ, this);
            instance = null;
        }
    }

    /**
     * Sends a GetMessage on the "TheArcrepos" queue and waits for a reply. This
     * is a blocking call. Returns null if no message is returned within
     * Settings.ARCREPOSITORY_GET_TIMEOUT
     *
     * @param arcfile The name of a file.
     * @param index   The offset of the wanted record in the file
     *
     * @return a BitarchiveRecord-object or null if request times out or object
     *         is not found.
     *
     * @throws ArgumentNotValid If the given arcfile is null or empty, or the
     *                          given index is negative.
     * @throws IOFailure        If a wrong message is returned or the get
     *                          operation failed.
     */
    public BitarchiveRecord get(String arcfile, long index)
            throws ArgumentNotValid, IOFailure {
        ArgumentNotValid.checkNotNullOrEmpty(arcfile, "arcfile");
        ArgumentNotValid.checkNotNegative(index, "index");
        log.debug("Requesting get of record '" + arcfile + ":" + index + "'");
        long start = System.currentTimeMillis();
        GetMessage requestGetMsg = new GetMessage(Channels.getTheRepos(),
                                                  replyQ, arcfile, index);
        NetarkivetMessage replyNetMsg = sendAndWaitForOneReply(requestGetMsg,
                                                               getTimeout);
        long timePassed = System.currentTimeMillis() - start;
        log.debug("Reply received after " + (timePassed 
                / MILLISECONDS_PER_SECOND) + " seconds");
        if (replyNetMsg == null) {
            log.info("Request for record(" + arcfile + ":" + index
                     + ") timed out after "
                     + (getTimeout / MILLISECONDS_PER_SECOND)
                     + " seconds. Returning null BitarchiveRecord");
            return null;
        }
        GetMessage replyGetMsg;
        try {
            replyGetMsg = (GetMessage) replyNetMsg;
        } catch (ClassCastException e) {
            throw new IOFailure("Received invalid argument reply: '"
                    + replyNetMsg + "'", e);
        }
        if (!replyGetMsg.isOk()) {
            throw new IOFailure("GetMessage failed: '"
                    + replyGetMsg.getErrMsg() + "'");
        }
        return replyGetMsg.getRecord();
    }

    /**
     * Synchronously retrieves a file from a bitarchive and places it in a local
     * file. This is the interface for sending GetFileMessage on the
     * "TheArcrepos" queue. This is a blocking call.
     *
     * @param arcfilename Name of the arcfile to retrieve.
     * @param replica     The bitarchive to retrieve the data from.
     * @param toFile      Filename of a place where the file fetched can be
     *                    put.
     * @throws ArgumentNotValid If the arcfilename are null or empty, or if 
     * either replica or toFile is null.
     * @throws IOFailure if there are problems getting a reply or the file could
     *                   not be found.
     */
    public void getFile(String arcfilename, Replica replica, File toFile) 
        throws ArgumentNotValid, IOFailure {
        ArgumentNotValid.checkNotNullOrEmpty(arcfilename, "arcfilename");
        ArgumentNotValid.checkNotNull(replica, "replica");
        ArgumentNotValid.checkNotNull(toFile, "toFile");

        log.debug("Requesting get of file '" + arcfilename + "' from '"
                  + replica + "'");
        //ArgumentNotValid.checkNotNull(replyQ, "replyQ must not be null");
        GetFileMessage gfMsg = new GetFileMessage(Channels.getTheRepos(),
                                                  replyQ, arcfilename,
                                                  replica.getId());
        GetFileMessage getFileMessage
                = (GetFileMessage) sendAndWaitForOneReply(gfMsg, 0);
        if (getFileMessage == null) {
            throw new IOFailure("GetFileMessage timed out before returning."
                    + "File not found?");
        } else if (!getFileMessage.isOk()) {
            throw new IOFailure("GetFileMessage failed: " 
                    + getFileMessage.getErrMsg());
        } else {
            getFileMessage.getData(toFile);
        }
    }

    /**
     * Sends a StoreMessage via the synchronized JMS connection method
     * sendAndWaitForOneReply(). After a successful storage operation, both the
     * local copy of the file and the copy on the ftp server are deleted.
     *
     * @param file A file to be stored. Must exist.
     *
     * @throws IOFailure        thrown if store is unsuccessful, or failed to
     *                          clean up files locally or on the ftp server
     *                          after the store operation.
     * @throws ArgumentNotValid if file parameter is null or file is not an
     *                          existing file.
     */
    public void store(File file) throws IOFailure, ArgumentNotValid {
        ArgumentNotValid.checkNotNull(file, "file");
        ArgumentNotValid.checkTrue(file.isFile(), "The file '" + file.getPath()
                + "' is not an existing file.");

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
                                 + file.getPath() + "' on attempt number "
                                 + (i + 1)
                                 + " of " + storeRetries;
                    log.warn(msg);
                    messages.append(msg).append("\n");
                } else {
                    String msg = "The returned message '" + replyMsg
                                 + "' was not ok"
                                 + " while waiting for reply on store of file '"
                                 + file.getPath() + "' on attempt number "
                                 + (i + 1)
                                 + " of " + storeRetries
                                 + ". Error message was '"
                                 + replyMsg.getErrMsg() + "'";
                    log.warn(msg);
                    messages.append(msg).append("\n");
                }
            } catch (Exception e) {
                String msg = "Client-side exception occurred while storing '"
                             + file.getPath() + "' on attempt number " + (i + 1)
                             + " of " + storeRetries + ".";
                log.warn(msg, e);
                messages.append(msg).append("\n");
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
     *
     * @param m the StoreMessage sent back as reply
     */
    private void cleanUpAfterStore(StoreMessage m) {
        RemoteFile rf;
        try {
            rf = m.getRemoteFile();
        } catch (Exception e) {
            log.warn("Could not get remote file object from message " + m, e);
            return;
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
     * @param job       An object that implements the FileBatchJob interface.
     *                  The initialize() method will be called before processing
     *                  and the finish() method will be called afterwards.  The
     *                  process() method will be called with each File entry.
     * @param replicaId The id of the archive to execute the job on
     * @return A local batch status
     * @throws IOFailure if no results can be read at all
     */
    public BatchStatus batch(FileBatchJob job, String replicaId) 
            throws IOFailure{
        ArgumentNotValid.checkNotNull(job, "job");
        ArgumentNotValid.checkNotNullOrEmpty(replicaId, "replicaId");

        log.debug("Starting batchjob '" + job + "' running on replica '"
                  + replicaId + "'");
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
        return new BatchStatus(brMsg.getFilesFailed(),
                               brMsg.getNoOfFilesProcessed(),
                               brMsg.getResultFile(),
                               job.getExceptions()
        );
    }

    /**
     * Request update of admin data to specific state.
     *
     * @param fileName       The file for which admin data should be updated.
     * @param replicaId The id if the replica that the administrative
     * data for fileName is wrong for.
     * @param newval         The new value in admin data.
     * @throws ArgumentNotValid If one of the arguments are invalid (null or 
     * empty string).
     * @throws IOFailure If the reply to the request update timed out.
     */
    public void updateAdminData(String fileName, String replicaId,
            ReplicaStoreState newval) throws ArgumentNotValid, IOFailure {
        ArgumentNotValid.checkNotNullOrEmpty(fileName, "String fileName");
        ArgumentNotValid.checkNotNullOrEmpty(replicaId, "String replicaId");
        ArgumentNotValid.checkNotNull(newval, "ReplicaStoreState newval");

        String msg = "Requesting update of admin data for file '" + fileName
                     + "' replica '" + replicaId + "' to state "
                     + newval;
        log.warn(msg);
        NotificationsFactory.getInstance().errorEvent(msg);
        AdminDataMessage aMsg =
                new AdminDataMessage(fileName, replicaId, newval);
        // We only need to know that a reply to our message has arrived. 
        // The replyMessage is thrown away, because it does not contain 
        // any more useful knowledge.
        sendAndWaitForOneReply(aMsg, 0);
    }

    /**
     * Request update of admin data to specific checksum.
     *
     * @param filename The file for which admin data should be updated.
     * @param checksum The new checksum for the file
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
     * @param fileName     The name of the file to delete
     * @param bitarchiveId The id of the bitarchive to delete the file in
     * @param checksum     The checksum of the deleted file
     * @param credentials  The credentials used to delete the file
     *
     * @return The file that was removed
     *
     * @throws ArgumentNotValid if arguments are null or equal to the empty
     * string
     * @throws IOFailure if we could not delete the remote file, or there was 
     * no response to our RemoveAndGetFileMessage within the allotted time 
     * defined by the setting 
     * {@link JMSArcRepositoryClient#ARCREPOSITORY_STORE_TIMEOUT}.
     */
    public File removeAndGetFile(String fileName, String bitarchiveId,
            String checksum, String credentials) throws IOFailure, 
            ArgumentNotValid {
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
                new RemoveAndGetFileMessage(Channels.getTheRepos(), 
                        Channels.getThisReposClient(), fileName, bitarchiveId,
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
                throw new IOFailure("Could not delete remote file: "
                        + replyMsg.getErrMsg());
            }
        } else {
            throw new IOFailure("Request timed out while requesting remove of "
                    + "file '" + fileName + "' in bitarchive '" + bitarchiveId 
                    + "'");
        }
    }

    /**
     * Retrieves all the checksum from the replica through a
     * GetAllChecksumMessage.
     *
     * This is the checksum archive alternative to running a ChecksumBatchJob.
     *
     * @param replicaId The id of the replica from which the checksums should be
     *                  retrieved.
     * @return A file containing filename and checksum of all the files in an 
     * archive in the same format as a ChecksumJob. 
     * @throws IOFailure If the reply is not of type GetAllChecksumsMessage
     * or if the file could not properly be retrieved from the reply message
     * or if the message timed out.
     * @throws ArgumentNotValid If the replicaId is null or empty. 
     * @see dk.netarkivet.archive.checksum.distribute.GetAllChecksumsMessage
     */
    public File getAllChecksums(String replicaId) throws IOFailure, 
            ArgumentNotValid {
        ArgumentNotValid.checkNotNullOrEmpty(replicaId, "String replicaId");
        log.debug("Sending GetAllChecksumMessage to replica '" + replicaId
                  + "'.");
        // time this.
        long start = System.currentTimeMillis();
        // make and send the message to the replica.
        GetAllChecksumsMessage gacMsg = new GetAllChecksumsMessage(Channels
                .getTheRepos(), replyQ, replicaId);
        NetarkivetMessage replyNetMsg = sendAndWaitForOneReply(gacMsg, 0);

        // calculate and log the time spent on handling the message.
        long timePassed = System.currentTimeMillis() - start;
        log.debug("Reply received after " + (timePassed 
                / MILLISECONDS_PER_SECOND) + " seconds.");
        // check whether the output was valid.
        if (replyNetMsg == null) {
            throw new IOFailure("Request for all checksum timed out after "
                    + (getTimeout / MILLISECONDS_PER_SECOND) + " seconds.");
        }
        // convert to the correct type of message.
        GetAllChecksumsMessage replyCSMsg;
        try {
            replyCSMsg = (GetAllChecksumsMessage) replyNetMsg;
        } catch (ClassCastException e) {
            throw new IOFailure("Received invalid reply message: '" 
                    + replyNetMsg, e);
        }

        try {
            // retrieve the data from this message and place it in tempDir.
            File result = File.createTempFile("tmp", "tmp", 
                    FileUtils.getTempDir());
            replyCSMsg.getData(result);

            return result;
        } catch (IOException e) {
            throw new IOFailure("Cannot create a temporary file for retrieving "
                    + "the data remote from checksum message: " 
                    + replyCSMsg, e);
        }
    }

    /**
     * Retrieves the names of all the files in the replica through a
     * GetAllFilenamesMessage.
     *
     * This is the checksum archive alternative to running a FilelistBatchJob.
     *
     * @param replicaId The id of the replica from which the list of filenames
     *                  should be retrieved.
     * @return A file with all the filenames within the archive of the given
     * replica. A null is returned if the message timeout.
     * @throws IOFailure If the reply is not of type GetAllFilenamesMessage
     * or if the file could not properly be retrieved from the reply message
     * @throws ArgumentNotValid If the replicaId is null or empty. 
     * @see dk.netarkivet.archive.checksum.distribute.GetAllFilenamesMessage
     */
    public File getAllFilenames(String replicaId) throws ArgumentNotValid, 
            IOFailure {
        ArgumentNotValid.checkNotNullOrEmpty(replicaId, "String replicaId");
        log.debug("Sending GetAllFilenamesMessage to replica '" + replicaId
                  + "'.");
        // time this.
        long start = System.currentTimeMillis();
        // make and send the message to the replica.
        GetAllFilenamesMessage gafMsg = new GetAllFilenamesMessage(Channels
                .getTheRepos(), replyQ, replicaId);
        NetarkivetMessage replyNetMsg = sendAndWaitForOneReply(gafMsg, 0);

        // calculate and log the time spent on handling the message.
        long timePassed = System.currentTimeMillis() - start;
        log.debug("Reply received after " + (timePassed 
                / MILLISECONDS_PER_SECOND) + " seconds.");
        // check whether the output was valid.
        if (replyNetMsg == null) {
            throw new IOFailure("Request for all filenames timed out after "
                    + (getTimeout / MILLISECONDS_PER_SECOND) + " seconds.");
        }
        // convert to the correct type of message.
        GetAllFilenamesMessage replyCSMsg;
        try {
            replyCSMsg = (GetAllFilenamesMessage) replyNetMsg;
        } catch (ClassCastException e) {
            throw new IOFailure("Received invalid reply message: '" 
                    + replyNetMsg, e);
        }

        try {
            // retrieve the data from this message.
            File result = File.createTempFile("tmp", "tmp", 
                    FileUtils.getTempDir());
            replyCSMsg.getData(result);

            return result;
        } catch (IOException e) {
            throw new IOFailure("Cannot create a temporary file for retrieving "
                    + " the data remote from checksum message: "
                    + replyCSMsg, e);
        }
    }
    
    /**
     * Retrieves the checksum of a specific file.
     * 
     * This is the checksum archive alternative to running a ChecksumJob 
     * limited to a specific file.
     * 
     * @param replicaId The ID of the replica to send the message.
     * @param filename The name of the file for whom the checksum should be
     * retrieved.
     * @return The checksum of the file in the replica. 
     * @throws IOFailure If the reply is not of type GetChecksumMessage. Or if
     * the message timed out.
     * @throws ArgumentNotValid If either the replicaId of the filename 
     * is null or empty. 
     */
    public String getChecksum(String replicaId, String filename) throws 
            ArgumentNotValid, IOFailure {
        ArgumentNotValid.checkNotNullOrEmpty(replicaId, "String replicaId");
        ArgumentNotValid.checkNotNullOrEmpty(filename, "String filename");
        log.debug("Sending GetChecksumMessage to replica '" + replicaId
                  + "' for file '" + filename + "'.");
        // time this.
        long start = System.currentTimeMillis();
        // make and send the message to the replica.
        GetChecksumMessage gcsMsg = new GetChecksumMessage(Channels
                .getTheRepos(), replyQ, filename, replicaId);
        NetarkivetMessage replyNetMsg = sendAndWaitForOneReply(gcsMsg, 0);
        // calculate and log the time spent on handling the message.
        long timePassed = System.currentTimeMillis() - start;
        log.debug("Reply received after " + (timePassed 
                / MILLISECONDS_PER_SECOND) + " seconds.");
        // check whether the output was valid.
        if (replyNetMsg == null) {
            throw new IOFailure("Request for checksum timed out after "
                    + (getTimeout / MILLISECONDS_PER_SECOND) + " seconds.");
        }
        
        // convert to the expected type of message.
        GetChecksumMessage replyCSMsg;
        try {
            replyCSMsg = (GetChecksumMessage) replyNetMsg;
        } catch (ClassCastException e) {
            throw new IOFailure("Received invalid reply message: '" 
                    + replyNetMsg, e);
        }

        if(!replyCSMsg.isOk()) {
            log.warn("The reply message for retrieval of checksum was not OK."
                    + " Tries to extract checksum anyway. " 
                    + replyCSMsg.getErrMsg());
        }
        return replyCSMsg.getChecksum();
    }

    /**
     * Method for correcting an entry in a replica.
     * This is done by sending a correct message to the replica.
     * 
     * The file which is removed from the replica is put into the tempDir.
     * 
     * @param replicaId The id of the replica to send the message.
     * @param checksum The checksum of the corrupt entry in the archive. It is 
     * important to validate that the checksum actually is wrong before 
     * correcting the entry.
     * @param file The file to correct the entry in the archive of the replica.
     * @param credentials A string with the password for allowing changes inside
     * an archive. If it does not correspond to the credentials of the archive, 
     * the correction will not be allowed.
     * @throws IOFailure If the message is not handled properly.
     * @throws ArgumentNotValid If the replicaId, the checksum or the 
     * credentials are either null or empty, or if file is null.
     */
    public File correct(String replicaId, String checksum, File file,
            String credentials) throws IOFailure, ArgumentNotValid {
        ArgumentNotValid.checkNotNullOrEmpty(replicaId, "String replicaId");
        ArgumentNotValid.checkNotNullOrEmpty(checksum, "String checksum");
        ArgumentNotValid.checkNotNull(file, "File file");
        ArgumentNotValid.checkNotNullOrEmpty(credentials, "String credentials");

        RemoteFile rm = RemoteFileFactory.getCopyfileInstance(file);
        CorrectMessage correctMsg = new CorrectMessage(Channels.getTheRepos(),
                replyQ, checksum, rm, replicaId, credentials);
        CorrectMessage responseMessage = (CorrectMessage) 
                sendAndWaitForOneReply(correctMsg, 0);

        if (responseMessage == null) {
            throw new IOFailure("Correct Message timed out before returning."
                    + " File not found?");
        } else if (!responseMessage.isOk()) {
            throw new IOFailure("CorrectMessage failed: "
                    + responseMessage.getErrMsg());
        }
        
        // retrieve the wrong file.
        RemoteFile removedFile = responseMessage.getRemovedFile();
        try {
            File destFile = new File(FileUtils.getTempDir(), 
                    removedFile.getName());
            removedFile.copyTo(destFile);
            return destFile;
        } catch(Throwable e) {
            String errMsg = "Problems occured during retrieval of file "
                + "removed from archive.";
            log.warn(errMsg, e);
            throw new IOFailure(errMsg, e);
        }
    }
}
