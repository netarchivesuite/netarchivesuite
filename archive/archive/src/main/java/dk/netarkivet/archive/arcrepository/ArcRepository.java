/* $Id$
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
package dk.netarkivet.archive.arcrepository;

import java.io.File;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.archive.ArchiveSettings;
import dk.netarkivet.archive.arcrepository.bitpreservation.AdminDataMessage;
import dk.netarkivet.archive.arcrepository.bitpreservation.ChecksumJob;
import dk.netarkivet.archive.arcrepository.distribute.ArcRepositoryServer;
import dk.netarkivet.archive.arcrepository.distribute.StoreMessage;
import dk.netarkivet.archive.arcrepositoryadmin.UpdateableAdminData;
import dk.netarkivet.archive.bitarchive.distribute.BatchMessage;
import dk.netarkivet.archive.bitarchive.distribute.BatchReplyMessage;
import dk.netarkivet.archive.bitarchive.distribute.BitarchiveClient;
import dk.netarkivet.archive.bitarchive.distribute.RemoveAndGetFileMessage;
import dk.netarkivet.archive.bitarchive.distribute.UploadMessage;
import dk.netarkivet.common.distribute.ChannelID;
import dk.netarkivet.common.distribute.Channels;
import dk.netarkivet.common.distribute.JMSConnection;
import dk.netarkivet.common.distribute.JMSConnectionFactory;
import dk.netarkivet.common.distribute.NullRemoteFile;
import dk.netarkivet.common.distribute.RemoteFile;
import dk.netarkivet.common.distribute.arcrepository.BitArchiveStoreState;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.IllegalState;
import dk.netarkivet.common.exceptions.PermissionDenied;
import dk.netarkivet.common.exceptions.UnknownID;
import dk.netarkivet.common.utils.CleanupIF;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.NotificationsFactory;
import dk.netarkivet.common.utils.Settings;

/**
 * The Arcrepository handles the communication with the different bitarchives.
 * This class ensures that arc files are stored in all available
 * bitarchives and verifies that the storage process succeeded. Retrieval of data
 * from a bitarchive goes through the JMSArcRepositoryClient that contacts the
 * appropriate (typically nearest) bitarchive and retrieves data from this
 * archive. Batch execution is sent to the bitarchive(s). Correction operations
 * are typically only allowed on one bitarchive.
 */
public class ArcRepository implements CleanupIF {

    private final Log log = LogFactory.getLog(getClass().getName());

    /**
     * The unique instance (singleton) of this class.
     */
    private static ArcRepository instance;

    /**
     * The administration data associated with the arcrepository.
     */
    private UpdateableAdminData ad;

    /**
     * The class which listens to messages sent to this instance of
     * Arcrepository or its subclasses.
     */
    private ArcRepositoryServer arcReposhandler;

    /**
     * The bit archives a connection is established to. Elements in this map
     * must be connected BitarchiveClients. 
     */
    private final Map<String, BitarchiveClient> connectedBitarchives =
        new HashMap<String, BitarchiveClient>();

    /**
     * Map from MessageId to arcfiles for which there are outstanding checksum
     * jobs.
     */
    private final Map<String, String> outstandingChecksumFiles =
        new HashMap<String, String>();

    /**
     * Map from filenames to remote files. Used for retrieving a remote file
     * reference while a store operation is in process.
     */
    private final Map<String, RemoteFile> outstandingRemoteFiles =
        new HashMap<String, RemoteFile>();

    /**
     * Map from bitarchive names to Map from filenames to the number of times a
     * file has been attempted uploaded to the the bitarchive.
     */
    private final Map<String, Map<String, Integer>> uploadRetries =
        new HashMap<String, Map<String, Integer>>();

    /**
     * A singular instance of JMSConnection, used for replying to Store
     * messages.
     */
    private JMSConnection jmsCon;

    /**
     * Constructor for the ArcRepository. Connects the ArcRepository to all
     * BitArchives, and initialises admin data
     *
     * @throws IOFailure
     *             if admin data cannot be read/initialised or we cannot'
     *             connect to some bitarchive.
     * @throws IllegalState
     *             if inconsistent channel info is given in settings.
     */
    private ArcRepository() throws IOFailure, IllegalState {
        //UpdateableAdminData Throws IOFailure
        this.ad = UpdateableAdminData.getUpdateableInstance(); 
        this.jmsCon = JMSConnectionFactory.getInstance();
        this.arcReposhandler = new ArcRepositoryServer(this);

        // Get channels
        ChannelID[] allBas = Channels.getAllArchives_ALL_BAs();
        ChannelID[] anyBas = Channels.getAllArchives_ANY_BAs();
        ChannelID[] theBamons = Channels.getAllArchives_BAMONs();
        // Checks equal number of channels
        checkChannels(allBas, anyBas, theBamons);

        for (int i = 0; i < allBas.length; i++) {
            connectToBitarchive(allBas[i], anyBas[i], theBamons[i]);
        }

        log.info("Starting the ArcRepository");
    }

    /**
     * Returns the unique ArcRepository instance.
     *
     * @return the instance.
     * @throws IOFailure
     *             if admin data cannot be read/initialised or we cannot'
     *             connect to some bitarchive.
     * @throws IllegalState
     *             if inconsistent channel info is given in settings.
     */
    public static synchronized ArcRepository getInstance()
            throws IllegalState, IOFailure {
        if (instance == null) {
            instance = new ArcRepository();
        }
        return instance;
    }

    /**
     * Sanity check for data consistency in the construction of the
     * ArcRepository, specifically that the number of ALL_BA, ANY_BA, and
     * THE_BAMON queues are all equal to the number of credentials.
     *
     * @param allBas
     *            The topics for bitarchives
     * @param anyBas
     *            The queues for bitarchives
     * @param theBamons
     *            The queues for bitarchive monitors
     * @throws IllegalState
     *             if inconsistent data is found
     */
    private void checkChannels(ChannelID[] allBas, ChannelID[] anyBas,
            ChannelID[] theBamons) throws IllegalState {

        if (theBamons.length != allBas.length
                || theBamons.length != anyBas.length) {

            StringBuilder values = new StringBuilder(
                    "Inconsistent data found in "
                            + "construction of ArcRepository: \n");
            values.append("\nALL_BAs: ");
            values.append(Arrays.toString(allBas));
            values.append("\nANY_BAs: ");
            values.append(Arrays.toString(anyBas));
            values.append("\nTHE_BAMONs: ");
            values.append(Arrays.toString(theBamons));

            throw new IllegalState(values.toString());
        }
    }

    /**
     * Establish a connection to a new bitarchive.
     *
     * @param allBa
     *            The ALL_BA channel of the given Bitarhive
     * @param anyBa
     *            The ANY_BA channel of the given Bitarhive
     * @param theBamon
     *            The THE_BAMON channel of the given Bitarhive
     * @throws IOFailure
     *             If we cannot connect to the bitarchive.
     */
    private void connectToBitarchive(ChannelID allBa, ChannelID anyBa,
            ChannelID theBamon) throws IOFailure {
        BitarchiveClient bac = BitarchiveClient.getInstance(allBa, anyBa,
                theBamon);

        connectedBitarchives.put(theBamon.getName(), bac);
    }

    /**
     * Stores a file in all known Bitarchives. This runs asynchronously, and
     * returns immediately. Side effects: 
     * 1) The RemoteFile added to List outstandingRemoteFiles, where overwrite
     *    is allowed.
     * 2) TODO: Check, if other sideeffects exist, and document them.
     *
     * @param rf
     *            A remotefile to be stored.
     * @param replyInfo
     *            A StoreMessage used to reply with success or failure.
     * @throws IOFailure
     *             If file couldn't be stored.
     * @throws ArgumentNotValid
     *             if a input parameter is null     
     */
    public synchronized void store(RemoteFile rf, StoreMessage replyInfo)
            throws IOFailure {
        ArgumentNotValid.checkNotNull(rf, "rf");
        ArgumentNotValid.checkNotNull(replyInfo, "replyInfo");

        final String filename = rf.getName();
        log.info("Store started: '" + filename + "'");

        // Record, that store of this filename is in progress
        // needed for retrying uploads.
        if (outstandingRemoteFiles.containsKey(filename)) {
            log.info("File: '" + filename + "' was outstanding from the start.");
        }
        outstandingRemoteFiles.put(filename, rf);

        if (ad.hasEntry(filename)) {

            // Any valid entry (and all existing entries are now
            // known to be valid) by definition has a checksum.
            if (!rf.getChecksum().equals(ad.getCheckSum(filename))) {
                String msg = "Attempting to store file '" + filename
                             + "' with a different checksum than before: "
                             + "Old checksum: " + ad.getCheckSum(filename)
                             + ", new checksum: " + rf.getChecksum();
                log.warn(msg);
                replyNotOK(filename, replyInfo);
                return;
            }
            log.debug("Retrying store of already known file '" + filename + "',"
                     + " Already completed: " + isStoreCompleted(filename));
            ad.setReplyInfo(filename, replyInfo);
        } else {
            ad.addEntry(filename, replyInfo, rf.getChecksum());
        }
        for (Map.Entry<String, BitarchiveClient> entry : connectedBitarchives
                .entrySet()) {
            startUpload(rf, entry.getValue(), entry.getKey());
        }

        // Check state and reply if needed
        considerReplyingOnStore(filename);
    }

    /**
     * Initiate uploading of file to a specific bitarchive. The corresponding
     * upload record in admin data is created.
     *
     * @param rf
     *            remotefile to upload to bitarchive.
     * @param bitarchiveClient
     *            The bitarchive client to upload to.
     * @param bitarchiveId
     *            The name of the bitarchive, where RemoteFile is to be stored.
     */
    private synchronized void startUpload(RemoteFile rf,
            BitarchiveClient bitarchiveClient, String bitarchiveId) {
        final String filename = rf.getName();
        log.debug("Upload started of file '" + filename + "' at '"
                   + bitarchiveId + "'");

        if (!ad.hasState(filename, bitarchiveId)) {
            // New upload
            ad.setState(filename, bitarchiveId,
                    BitArchiveStoreState.UPLOAD_STARTED);
            bitarchiveClient.upload(rf);
        } else {
            // Recovery from old upload
            BitArchiveStoreState storeState = ad.getState(filename,
                    bitarchiveId);
            log.trace("Recovery from old upload. StoreState: " + storeState);
            switch (storeState) {
            case UPLOAD_FAILED:
            case UPLOAD_STARTED:
            case DATA_UPLOADED:
                // Unknown condition in bitarchive. Test with checksum job.
                if (storeState == BitArchiveStoreState.UPLOAD_FAILED) {
                    ad.setState(filename, bitarchiveId,
                            BitArchiveStoreState.UPLOAD_STARTED);
                }
                sendChecksumJob(filename, bitarchiveClient);
                break;
            case UPLOAD_COMPLETED:
                break;
            default:
                throw new UnknownID("Unknown state: '" + storeState + "'");
            }
        }
    }

    /**
     * Send a checksumjob for a file to a bitarchive, and register the file in
     * the map of outstanding checksum files.
     *
     * @param filename
     *            The file to checksum
     * @param bitarchiveClient
     *            The client to send checksum job to
     */
    private void sendChecksumJob(String filename,
            BitarchiveClient bitarchiveClient) {
        ChecksumJob checksumJob = new ChecksumJob();
        checksumJob.processOnlyFileNamed(filename);
        BatchMessage msg = bitarchiveClient.batch(Channels.getTheRepos(),
                checksumJob);
        outstandingChecksumFiles.put(msg.getID(), filename);
        log.debug("Checksum job submitted for: '" + filename + "'");
    }

    /**
     * Test whether the current state is such that we may send a reply for the
     * file we are currently processing, and send the reply if it is. We reply
     * only when there is an outstanding message to reply to, and a) The file is
     * reported complete in all bitarchives or b) No bitarchive has outstanding
     * reply messages AND some bitarchive has reported failure.
     *
     * @param arcFileName
     *            The arcfile we consider replying to.
     */
    private synchronized void considerReplyingOnStore(String arcFileName) {
        if (ad.hasReplyInfo(arcFileName)) {
            if (isStoreCompleted(arcFileName)) {
                replyOK(arcFileName, ad.removeReplyInfo(arcFileName));
            } else if (oneBitArchiveHasFailed(arcFileName)
                    && noBitArchiveInStateUploadStarted(arcFileName)) {
                replyNotOK(arcFileName, ad.removeReplyInfo(arcFileName));
            }
        }
    }

    /**
     * Reply to a store message with status Ok.
     *
     * @param arcFileName
     *            The file for which we are replying
     * @param msg
     *            The message to reply to
     */
    private synchronized void replyOK(String arcFileName, StoreMessage msg) {
        outstandingRemoteFiles.remove(arcFileName);
        clearRetries(arcFileName);
        log.info("Store OK: '" + arcFileName + "'");
        log.debug("Sending store OK reply to message '" + msg + "'");
        jmsCon.reply(msg);
    }

    /**
     * Reply to a store message with status Not Ok.
     *
     * @param arcFileName
     *            The file for which we are replying
     * @param msg
     *            The message to reply to
     */
    private synchronized void replyNotOK(String arcFileName, StoreMessage msg) {
        outstandingRemoteFiles.remove(arcFileName);
        clearRetries(arcFileName);
        msg.setNotOk("Failure while trying to store ARC file: " + arcFileName);
        log.warn("Store NOT OK: '" + arcFileName + "'");
        log.debug("Sending store NOT OK reply to message '" + msg + "'");
        jmsCon.reply(msg);
    }

    /**
     * Check if all bitarchives have reported that storage has been successfully
     * completed. If this is the case return true else false
     *
     * @param arcfileName
     *            the file being stored
     * @return true only if all bitarchives report UPLOAD_COMPLETED
     */
    private boolean isStoreCompleted(String arcfileName) {
        // TODO: remove quadratic scaling hidden here!!
        for (String baname : connectedBitarchives.keySet()) {
            if (ad.getState(arcfileName, baname)
                    != BitArchiveStoreState.UPLOAD_COMPLETED) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks if there are at least one bitarchive that has reported that 
     * storage has failed. If this is the case return true else false
     *
     * @param arcFileName  the file being stored
     * @return true     only if at least one bitarchive report UPLOAD_FAILED
     */
    private boolean oneBitArchiveHasFailed(String arcFileName) {
        for (String baname : connectedBitarchives.keySet()) {
            if (ad.getState(arcFileName, baname)
                    == BitArchiveStoreState.UPLOAD_FAILED) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if no bitarchive that has reported that upload is in started state.
     * If this is the case return true else false
     *
     * @param arcFileName  the file being stored
     * @return true     only if no bitarchive report UPLOAD_STARTED
     */
    private boolean noBitArchiveInStateUploadStarted(String arcFileName) {
        for (String baname : connectedBitarchives.keySet()) {
            if (ad.getState(arcFileName, baname)
                    == BitArchiveStoreState.UPLOAD_STARTED) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns a bitarchive client based on a replica id.
     *
     * @param replicaId  the replica id
     * @return a bitarchive client a bitarchive client
     * @throws ArgumentNotValid
     *             if replicaId parameter is null
     */
    public BitarchiveClient getBitarchiveClientFromReplicaId(
            String replicaId) throws ArgumentNotValid {
        ArgumentNotValid.checkNotNullOrEmpty(replicaId, "replicaId");

        String channelName = Channels.getBaMonForReplica(replicaId)
                .getName();
        BitarchiveClient bac = connectedBitarchives.get(channelName);

        if (bac == null) {
            throw new UnknownID("Unknown channe id: " + channelName);
        }

        return bac;
    }

    private String resolveBitarchiveID(String s) {
        return s.replaceAll("ALL_BA", "THE_BAMON").replaceAll("ANY_BA",
                "THE_BAMON");
    }

    /**
     * Event handler for upload messages reporting the upload result.
     * Checks the success status of the upload and
     * updates admin data accordingly
     *
     * @param msg
     *            an UploadMessage
     */
    public synchronized void onUpload(UploadMessage msg) {
        ArgumentNotValid.checkNotNull(msg, "msg");
        log.debug("Received upload reply: " + msg.toString());

        String bitarchiveName = resolveBitarchiveID(msg.getTo().getName());

        if (msg.isOk()) {
            processDataUploaded(msg.getArcfileName(), bitarchiveName);
        } else {
            processUploadFailed(msg.getArcfileName(), bitarchiveName);
        }
    }

    /**
     * Process the report by a bitarchive that a file was correctly uploaded.
     * 1. Update the upload, and store states appropriately
     * 2. Verify that data are correctly stored in the archive by running
     * a batch job on the archived
     * file to perform a MD5 checksum comparison
     * 3. Check if store operation is completed and update admin data if so
     *
     * @param arcfileName
     *            The arcfile that was uploaded
     * @param bitarchiveName
     *            The bitarchive that uploaded it
     */
    private synchronized void processDataUploaded(String arcfileName,
            String bitarchiveName) {
        log.debug("Data uploaded '" + arcfileName + "' ," + bitarchiveName);
        ad.setState(arcfileName, bitarchiveName,
                BitArchiveStoreState.DATA_UPLOADED);

        sendChecksumJob(arcfileName, connectedBitarchives.get(bitarchiveName));
    }

    /**
     * Update admin data with the information that upload
     * to a bitarchive failed.
     * The bitarchive record is set to UPLOAD_FAILED
     *
     * @param arcfileName
     *            the file that resulted in an upload failure
     * @param bitarchiveName
     *            the bitarchive that could not upload the file
     */
    private void processUploadFailed(String arcfileName, 
                                     String bitarchiveName) {
        log.warn("Upload failed for ARC file '" + arcfileName
                + "' to bit archive '" + bitarchiveName + "'");

        // Update state to reflect upload failure
        ad.setState(arcfileName, bitarchiveName,
                BitArchiveStoreState.UPLOAD_FAILED);
        considerReplyingOnStore(arcfileName);
    }

    /**
     * Called when we receive replies on our checksum batch jobs.
     *
     * @param msg
     *            a BatchReplyMessage
     */
    public synchronized void onBatchReply(BatchReplyMessage msg) {
        ArgumentNotValid.checkNotNull(msg, "msg");
        log.debug("BatchReplyMessage received: '" + msg + "'");

        if (!outstandingChecksumFiles.containsKey(msg.getReplyOfId())) {
            // Message was NOT expected
            log.warn("Received batchreply message with unknown originating "
                    + "ID " + msg.getReplyOfId() + "\n" + msg.toString()
                    + "\n. Known IDs are: "
                    + outstandingChecksumFiles.keySet().toString());
            return;
        }

        String arcfileName = outstandingChecksumFiles
                .remove(msg.getReplyOfId());

        // Check incoming message
        if (!msg.isOk()) {
            //Checksum job has ended with errors, but can contain checksum 
            //anyway, therefore it is logged - but we try to go on 
            log.warn("Message '" + msg.getID()
                            + "' is reported not okay"
                            + "\nReported error: '" + msg.getErrMsg() + "'"
                            + "\nTrying to process anyway.");
        }

        // Parse results
        // if legal result is found it is placed in reportedChecksum
        // if illegal or errors occurs reportedChecksum is set to ""
        RemoteFile checksumResFile = msg.getResultFile();
        String reportedChecksum = "";
        boolean checksumReadOk = false;
        if (checksumResFile == null || 
            checksumResFile instanceof NullRemoteFile) {
            log.debug("Message '" + msg.getID()
                            + "' returned no results"
                            + (checksumResFile == null ? " (was null)" : "")
                            + "\nNo checksum to use for file '"
                            + arcfileName + "'");
        } else {
            //Read checksum
            // Copy result to a local file
            File outputFile = new File(FileUtils.getTempDir(),
                    msg.getReplyTo().getName()
                            + "_" + arcfileName + "_checksumOutput.txt");
            try {
                checksumResFile.copyTo(outputFile);

                // Read checksum from local file
                reportedChecksum = readChecksum(outputFile, arcfileName);
                checksumReadOk = true;
            } catch (IOFailure e) {
                log.warn("Couldn't read checksumjob "
                        + "output for '" + arcfileName + "'", e);
            } catch (IllegalState e) {
                log.warn("Couldn't read result of checksumjob "
                        + "in '" + arcfileName + "'", e);
            }

            // Clean up output file and remote file
            // clean up does NOT result in general error, i.e. 
            // reportedChecksum is NOT set to "" in case of errors
            try {
                FileUtils.removeRecursively(outputFile);
            } catch (IOFailure e) {
                log.warn("Couldn't clean up checksumjob "
                        + "output file '" + outputFile + "'", e);
            }
            try {
                checksumResFile.cleanup();
            } catch (IOFailure e) {
                log.warn("Couldn't clean up checksumjob "
                        + "remote file '" + checksumResFile.getName() + "'", e);
            }
        }

        // Process result
        String orgCheckSum = ad.getCheckSum(arcfileName);
        String bitarchive = resolveBitarchiveID(msg.getReplyTo().getName());
        processCheckSum(arcfileName, bitarchive, orgCheckSum,
                reportedChecksum, msg.isOk() && checksumReadOk);
    }

    /**
     * Reads output from a checksum file.  Only the first instance of the
     * desired file will be used.  If other filenames are encountered than
     * the wanted one, they will be logged at level warning, as that is
     * indicative of serious errors.  Having more than one instance of the
     * desired file merely means it was found in several bitarchives, which
     * is not our problem.
     *
     * @param outputFile
     *            The file to read checksum from.
     * @param arcfileName
     *            The arcfile to find checksum for.
     * @return The checksum, or the empty string
     *          if no checksum found for arcfilename.
     * @throws IOFailure If any error occurs reading the file.
     * @throws IllegalState if readen format is wrong
     */
    private String readChecksum(File outputFile, String arcfileName) {
        //List of lines in batch (checksum job) output file
        List<String> lines = FileUtils.readListFromFile(outputFile);
        //List of checksums found in batch (checksum job) output file
        List<String> checksumList = new ArrayList<String>();
        
        //Extract checksums for arcfile from lines
        //If errors occurs then throw exception
        for (String line : lines) {
            String readFileName = "";
            String checksum = "";
            String[] tokens = line.split(
                    dk.netarkivet.archive.arcrepository
                      .bitpreservation.Constants.STRING_FILENAME_SEPARATOR);
            boolean ignoreLine = false;
            
            //Check line format
            ignoreLine = (tokens.length == 0);
            if (tokens.length != 2 && !ignoreLine) { //wrong format
                throw new IllegalState("Read checksum line " +
                   (tokens.length == 0 ? "was empty": "had unexpected format")
                   + " '" + line + "'");
            }
            
            //Check checksum and arc-file name in line
            if (!ignoreLine) {
                readFileName = tokens[0];
                checksum = tokens[1];
                if (checksum.length() == 0) { //wrong format of checksum
                    //do not exit - there may be more checksums
                    ignoreLine = true;
                    log.warn("There were an empty checksum in result for " 
                             + "checksums to arc-file '" + arcfileName 
                             + "(line: '" + line + "')");
                } else {
                    if (!readFileName.equals(arcfileName)) { //wrong arcfile
                        // do not exit - there may be more checksums
                        ignoreLine = true;
                        log.warn("There were an unexpected arc-file name in " 
                                + "checksum result for arc-file '" 
                                + arcfileName
                                + "'" + "(line: '" + line + "')");
                    }
                }
            }
            
            //Check against earlier readen checksums, if more than one
            if (checksumList.size() > 0 && !ignoreLine) {
                //Ignore if the checksums are the same
                if (!checksum.equals(
                        checksumList.get(checksumList.size() - 1))) {
                    throw new IllegalState("There were minimum "  
                        + (checksumList.size() + 1) + " checksums "
                        + "in result where some of them were "
                        + "different (last checksum line: '" + line 
                        + "')");
                }
            }
            
            //Add error free non-empty found checksum in list
            if (!ignoreLine) {
                checksumList.add(checksum);
            }
        }

        // Check that checksum list contain a result, 
        // log if it has more than one result
        if (checksumList.size() > 1) {
            //Log and proceed - the checksums are equal
            log.warn("Arcfile '" + arcfileName + "' was found with " 
                      + checksumList.size() + "occurences of the checksum: "
                      + checksumList.get(0));
        }
        
        if (checksumList.size() == 0) {
            log.debug("Arcfile '" + arcfileName
                    + "' not found in lines of checksum output file '"
                    + outputFile
                    + "':  " + FileUtils.readListFromFile(outputFile));
            return "";
        } else {
            return checksumList.get(0);
        }
    }

    /**
     * Process reporting of a checksum from a bitarchive for a specific file as
     * part of a store operation for the file. Verify that the checksum is
     * correct, update the BitArchiveStoreState state.
     * Invariant: upload-state is changed or retry count is increased.
     *
     * @param arcFileName
     *            The file being stored.
     * @param bitarchiveName
     *            The bitarchive reporting a checksum.
     * @param orgChecksum
     *            The original checksum.
     * @param reportedChecksum
     *            The checksum calculated by the bitarchive. This value is "",
     *            if an error has occured (except reply NOT ok from bitarchive).
     * @param checksumReadOk
     *            Tells whether the checksum was read ok by batch job.
     */
    private synchronized void processCheckSum(String arcFileName,
            String bitarchiveName, String orgChecksum,
            String reportedChecksum,
            boolean checksumReadOk) {
        log.debug("Checksum received ... processing");
        ArgumentNotValid.checkNotNullOrEmpty(arcFileName, "arcfileName");
        ArgumentNotValid.checkNotNullOrEmpty(bitarchiveName, "bitarchiveName");
        ArgumentNotValid.checkNotNullOrEmpty(orgChecksum, "orgChecksum");
        ArgumentNotValid.checkNotNull(reportedChecksum, "reportedChecksum");

        //Log if we do not find file outstanding
        //we proceed anyway in order to be sure to update stae of file
        if (!outstandingRemoteFiles.containsKey(arcFileName)) {
            log.warn("Could not find arc-file as outstanding " 
                      + "remote file: '" + arcFileName + "'");
        }

        //If everything works fine complete process of this checksum
        if (orgChecksum.equals(reportedChecksum) 
             && !reportedChecksum.isEmpty() ) {
            
            // Checksum is valid and job matches expected results
            ad.setState(arcFileName, bitarchiveName,
                    BitArchiveStoreState.UPLOAD_COMPLETED);

            // Find out if and how to make general reply on store()
            // remove file from outstandingRemoteFiles if a reply is given
            considerReplyingOnStore(arcFileName);
            
            return;
        } 

        //Log error or retry upload
        if (reportedChecksum.isEmpty()) { //no checksum found
            if (checksumReadOk) { //no errors in finding no checksum
                if (retryOk(bitarchiveName, arcFileName)) { // we can retry
                    if (outstandingRemoteFiles.containsKey(arcFileName)) {
                        RemoteFile rf = outstandingRemoteFiles.get(arcFileName);
                        //Retry upload only if allowed and in case we are sure 
                        //that the empty checksum means that the arcfile is not 
                        //in the archive
                        log.debug("Retrying upload of '" + arcFileName + "'");
                        ad.setState(rf.getName(), bitarchiveName,
                                BitArchiveStoreState.UPLOAD_STARTED);
                        connectedBitarchives.get(bitarchiveName).upload(rf);
                        incRetry(bitarchiveName, arcFileName);
                        return;
                    } //else logning was done allready above
                } else { //cannot retry
                    log.warn("Cannot do more retry upload of "
                        + "remote file: '" + arcFileName + "' to '"
                        + bitarchiveName + "', reported checksum='"
                        + reportedChecksum + "'" );
                }
            } else { //error in getting checksum
                log.warn("Cannot retry upload of " 
                    + "remote file: '" + arcFileName + "' to '"
                    + bitarchiveName + "', reported checksum='"
                    + reportedChecksum + "' due to earlier batchjob" 
                    + " error." );
            }
        } else { //non empty checksum
            if (!orgChecksum.equals(reportedChecksum)) {
                log.warn("Cannot upload (wrong checksum) '" + arcFileName
                        + "' to '"+ bitarchiveName + "', reported checksum='"
                        + reportedChecksum + "'");
            } else {
                log.warn("Cannot upload (unknown reason) '" + arcFileName
                        + "' to '"+ bitarchiveName + "', reported checksum='"
                        + reportedChecksum + "'");
            }
        }
        
        // This point is reached if there is some kind of (logged) error, i.e.
        // - the file has not been accepted as completed
        // - the file has not been sent to retry of upload
        ad.setState(arcFileName, bitarchiveName,
                    BitArchiveStoreState.UPLOAD_FAILED);
        considerReplyingOnStore(arcFileName);
    }

    /**
     * Keep track of upload retries of an arcfile to an archive.
     *
     * @param bitarchiveName The name of a given bitarchive
     * @param arcfileName The name of a given ARC file
     * @return true if it is ok to retry an upload of arcfileName to
     *         bitarchiveName
     */
    private boolean retryOk(String bitarchiveName, String arcfileName) {
        Map<String, Integer> bitarchiveRetries = uploadRetries
                .get(bitarchiveName);
        if (bitarchiveRetries == null) {
            return true;
        }
        Integer retryCount = bitarchiveRetries.get(arcfileName);
        if (retryCount == null) {
            return true;
        }

        if (retryCount  
            >= Settings.getInt(ArchiveSettings.ARCREPOSITORY_UPLOAD_RETRIES)) {
            return false;
        }

        return true;
    }

    /**
     * Increment the number of upload retries.
     *
     * @param bitarchiveName The name of a given bitarchive
     * @param arcfileName The name of a given ARC file
     */
    private void incRetry(String bitarchiveName, String arcfileName) {
        Map<String, Integer> bitarchiveRetries = uploadRetries
                .get(bitarchiveName);
        if (bitarchiveRetries == null) {
            bitarchiveRetries = new HashMap<String, Integer>();
            uploadRetries.put(bitarchiveName, bitarchiveRetries);
        }

        Integer retryCount = bitarchiveRetries.get(arcfileName);
        if (retryCount == null) {
            bitarchiveRetries.put(arcfileName, new Integer(1));
            return;
        }

        bitarchiveRetries.put(arcfileName, new Integer(retryCount + 1));
    }

    /**
     * Remove all retry tracking information for the arcfile.
     *
     * @param arcfileName The name of a given ARC file
     */
    private void clearRetries(String arcfileName) {
        for (String baname : uploadRetries.keySet()) {
            Map<String, Integer> baretries = uploadRetries.get(baname);
            baretries.remove(arcfileName);
        }
    }

    /**
     * Change admin data entry for a given file.
     *
     * The following information is contained in the given AdminDataMessage:
     * 1) The name of the given file to change the entry for,
     * 2) the name of the bitarchive to modify the entry for,
     * 3) a boolean that says whether or not to replace the checksum for the
     *   entry for the given file in AdminData,
     * 4) a replacement for the case where the former value is true.
     *
     * @param msg an AdminDataMessage object
     */
    public void updateAdminData(AdminDataMessage msg) {

        if (!ad.hasEntry(msg.getFileName())) {
            throw new ArgumentNotValid("No admin entry exists for the file '"
                    + msg.getFileName() + "'");
        }

        String message = "Handling request to change admin data for '" +
                msg.getFileName() + "'. "
                + (msg.isChangeStoreState() ?
                   "Change store state to " + msg.getNewvalue() : "")
                + (msg.isChangeChecksum() ?
                "Change checksum to " + msg.getChecksum() : "");
        log.warn(message);
        NotificationsFactory.getInstance().errorEvent(message);

        if (msg.isChangeStoreState()) {
            String bamonname = Channels.getBaMonForReplica(
                    msg.getBitarchiveId()).getName();
            ad.setState(msg.getFileName(), bamonname, msg.getNewvalue());
        }

        if (msg.isChangeChecksum()) {
            ad.setCheckSum(msg.getFileName(), msg.getChecksum());
        }
    }

    /**
     * Forwards a RemoveAndGetFileMessage to the designated bitarchive. Before
     * forwarding the message it is verified that the checksum of the file to
     * remove differs from the registered checksum of the file to remove. If no
     * registration exists for the file to remove the message is always
     * forwarded.
     *
     * @param msg
     *            the message to forward to a bitarchive
     */
    public void removeAndGetFile(RemoveAndGetFileMessage msg) {
        // Prevent removal of files with correct checksum
        if (ad.hasEntry(msg.getArcfileName())) {
            String refchecksum = ad.getCheckSum(msg.getArcfileName());
            if (msg.getCheckSum().equals(refchecksum)) {
                throw new ArgumentNotValid(
                       "Attempting to remove file with correct checksum. File="
                                + msg.getArcfileName() + "; with checksum:"
                                + msg.getCheckSum() + ";");
            }
        }

        // checksum ok - try to remove the file
        log.warn("Requesting remove of file '" + msg.getArcfileName()
                 + "' with checksum '" + msg.getCheckSum()
                 + "' from: '" + msg.getReplicaId() + "'");
        NotificationsFactory.getInstance().errorEvent(
                "Requesting remove of file '"
                 + msg.getArcfileName()
                 + "' with checksum '" + msg.getCheckSum()
                 + "' from: '" + msg.getReplicaId() + "'");
        BitarchiveClient bac = getBitarchiveClientFromReplicaId(msg
                .getReplicaId());
        bac.removeAndGetFile(msg);

    }

    /**
     * Close all bitarchive connections, open loggers, and the ArcRepository
     * handler.
     */
    public void close() {
        log.info("Closing down ArcRepository");
        cleanup();
        log.info("Closed ArcRepository");
    }

    /**
     * closes all connections and nulls the instance.
     */
    public void cleanup() {
        if (arcReposhandler != null) {
            arcReposhandler.close();
            arcReposhandler = null;
        }
        if (connectedBitarchives != null) {
            for (BitarchiveClient cba : connectedBitarchives.values()) {
                cba.close();
            }
        }
        if (ad != null) {
            ad.close();
            ad = null;
        }
        instance = null;
    }
}
