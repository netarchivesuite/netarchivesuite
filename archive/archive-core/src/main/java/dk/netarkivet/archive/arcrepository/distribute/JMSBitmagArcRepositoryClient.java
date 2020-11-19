/*
 * #%L
 * Netarchivesuite - archive
 * %%
 * Copyright (C) 2005 - 2014 The Royal Danish Library, the Danish State and University Library,
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
package dk.netarkivet.archive.arcrepository.distribute;

import static dk.netarkivet.common.distribute.bitrepository.BitmagUtils.getChecksumsClient;

import dk.netarkivet.archive.bitarchive.distribute.BatchMessage;
import dk.netarkivet.archive.bitarchive.distribute.BatchReplyMessage;
import dk.netarkivet.archive.bitarchive.distribute.GetFileMessage;
import dk.netarkivet.archive.bitarchive.distribute.GetMessage;
import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.distribute.ChannelID;
import dk.netarkivet.common.distribute.Channels;
import dk.netarkivet.common.distribute.JMSConnectionFactory;
import dk.netarkivet.common.distribute.NetarkivetMessage;
import dk.netarkivet.common.distribute.Synchronizer;
import dk.netarkivet.common.distribute.arcrepository.ArcRepositoryClient;
import dk.netarkivet.common.distribute.arcrepository.BatchStatus;
import dk.netarkivet.common.distribute.arcrepository.BitarchiveRecord;
import dk.netarkivet.common.distribute.arcrepository.Replica;
import dk.netarkivet.common.distribute.arcrepository.ReplicaStoreState;
import dk.netarkivet.common.distribute.bitrepository.Bitrepository;
import dk.netarkivet.common.distribute.bitrepository.BitmagUtils;
import dk.netarkivet.common.distribute.bitrepository.NetarchivesuiteBlockingEventHandler;
import dk.netarkivet.common.distribute.bitrepository.action.putfile.PutFileAction;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.NotImplementedException;
import dk.netarkivet.common.utils.NotificationType;
import dk.netarkivet.common.utils.NotificationsFactory;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.utils.batch.FileBatchJob;
import org.apache.commons.io.FileUtils;
import org.bitrepository.access.getchecksums.GetChecksumsClient;
import org.bitrepository.access.getchecksums.conversation.ChecksumsCompletePillarEvent;
import org.bitrepository.bitrepositoryelements.ChecksumDataForChecksumSpecTYPE;
import org.bitrepository.bitrepositoryelements.ChecksumDataForFileTYPE;
import org.bitrepository.bitrepositoryelements.ChecksumSpecTYPE;
import org.bitrepository.client.eventhandler.OperationEvent;
import org.bitrepository.common.exceptions.OperationFailedException;
import org.bitrepository.common.utils.ChecksumUtils;
import org.bitrepository.modify.putfile.BlockingPutFileClient;
import org.bitrepository.modify.putfile.PutFileClient;
import org.bitrepository.protocol.FileExchange;
import org.bitrepository.protocol.ProtocolComponentFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Map;

/**
 * Client side usage of an arc repository. All non-writing requests are forwarded to the ArcRepositoryServer over the network.
 * Store requests are sent directly to the bitrepository messagebus.
 *
 * Get and store messages are retried a number of time before giving up, and will timeout after a specified time.
 *
 * So called mixed-mode client, where the store is done to a Bitmagrepository, and the rest (access and batch-processing)
 * is done using the distributed netarchivesuite archive.
 */
public class JMSBitmagArcRepositoryClient extends Synchronizer implements ArcRepositoryClient, AutoCloseable {

        /** the one and only JMSBitmagArcRepositoryClient instance. */
    private static JMSBitmagArcRepositoryClient instance;
    private static PutFileClient putfileClientInstance;  // NEW

    /** Logging output place. */
    protected static final Logger log = LoggerFactory.getLogger(JMSBitmagArcRepositoryClient.class);

    /** Listens on this queue for replies. */
    private final ChannelID replyQ;

    /** The length of time to wait for a get reply before giving up. */
    private long timeoutGetOpsMillis;

    // NOTE: The constants defining setting names below are left non-final on
    // purpose! Otherwise, the static initialiser that loads default values
    // will not run.

    /** The default place in classpath where the settings file can be found. */
    private static String defaultSettingsClasspath = "dk/netarkivet/common/distribute/arcrepository/bitrepository/"
            + "JmsBitmagArcRepositoryClientSettings.xml";

    /*
     * The static initialiser is called when the class is loaded. It will add default values for all settings defined in
     * this class, by loading them from a settings.xml file in classpath.
     */
    static {
        Settings.addDefaultClasspathSettings(defaultSettingsClasspath);
        if (putfileClientInstance == null) {            // NEW
            putfileClientInstance = BitmagUtils.getPutFileClient();
        }
       // From Bitrepository.getInstance() (File configDir, String bitmagKeyFilename)
        /*
        final String  CLIENT_CERTIFICATE_FILE = "client-certificate.pem";
           String applicationConfig = System.getProperty("dk.kb.applicationConfig");
           Path configDir = Paths.get(applicationConfig);
           Path clientCertificate = configDir.resolve(CLIENT_CERTIFICATE_FILE);
           BitmagUtils.initialize(configDir, clientCertificate);
        Bitrepository.getInstance(configDir, CLIENT_CERTIFICATE_FILE);
        */
    }

    /**
     * <b>settings.common.arcrepositoryClient.bitrepository.tempdir</b>: <br/>
     * The setting for where the bitrepository has its temporary directory.
     */
    private static final String BITREPOSITORY_TEMPDIR = "settings.common.arcrepositoryClient.bitrepository.tempdir";
    /**
     * <b>settings.common.arcrepositoryClient.bitrepository.settingsDir</b>: <br/>
     * The setting for where the bitrepository settings directory can be found.
     */
    private static final String BITREPOSITORY_SETTINGS_DIR =
            "settings.common.arcrepositoryClient.bitrepository.settingsDir";
    /**
     * <b>settings.common.arcrepositoryClient.bitrepository.keyfilename</b>: <br/>
     * The setting for the name of the keyfile for the bitrepository certificate.
     * This setting is optional. If specified as a relative path, it is evaluated relative to the
     * bitrepository settinbgs directory.
     */
    private static final String BITREPOSITORY_KEYFILENAME =
            "settings.common.arcrepositoryClient.bitrepository.keyfilename";
    /**
     * <b>settings.common.arcrepositoryClient.bitrepository.storeMaxPillarFailures</b>: <br/>
     * The setting for how many pillars are allowed to fail during a store/putfile operation.
     */
    private static final String BITREPOSITORY_STORE_MAX_PILLAR_FAILURES =
            "settings.common.arcrepositoryClient.bitrepository.storeMaxPillarFailures";
    /**
     * <b>settings.common.arcrepositoryClient.bitrepository.collectionID</b>: <br/>
     * The setting for which collection ID to use. If unspecified, this falls back to the NetarchiveSuite environment name.
     */
    private static final String BITREPOSITORY_COLLECTIONID =
            "settings.common.arcrepositoryClient.bitrepository.collectionID";
    /**
     * <b>settings.common.arcrepositoryClient.bitrepository.usepillar</b>: <br/>
     * The setting for which pillar to use... This is currently not used. Probably intended for the retrieval of files.
     */
    private static final String BITREPOSITORY_USEPILLAR =
            "settings.common.arcrepositoryClient.bitrepository.usepillar";
    /** The bitrepository collection id for the */
    private String collectionId;
    /** The temporary directory for the bitrepository client.*/
    private File tempdir;
    /** The maximum number of failures for a storage to be accepted.*/
    private int maxStoreFailures;
    /** The bitrepository interface.*/
    private Bitrepository bitrep;
    /** The pillar to use.*/
    private String usepillar;

    /**
     * <b>settings.common.arcrepositoryClient.getTimeout</b>: <br>
     * The setting for how many milliseconds we will wait before giving up on a lookup request to the Arcrepository.
     */
    public static final String ARCREPOSITORY_GET_TIMEOUT = "settings.common.arcrepositoryClient.bitrepository.getTimeout"; // Optional

    /**
     * Initialise the bitrepository client.
     *
     * @throws ArgumentNotValid if the bitrepository does not recognize the specified collectionID
     * @throws IOFailure if the tempdir does not exist and cannot be created
     * @see #BITREPOSITORY_COLLECTIONID
     * @see #BITREPOSITORY_TEMPDIR
     *
     */
    private JMSBitmagArcRepositoryClient() {
        synchronized (JMSBitmagArcRepositoryClient.class){
            if (instance != null){
                throw new RuntimeException("Attempting to start an additional "+JMSBitmagArcRepositoryClient.class+" instance");
            } else {
                instance = this;
            }
        }

        timeoutGetOpsMillis = Settings.getLong(ARCREPOSITORY_GET_TIMEOUT);

        log.info(
                "JMSBitmagArcRepositoryClient will timeout on each getrequest after {} milliseconds.",
                timeoutGetOpsMillis);
        replyQ = Channels.getThisReposClient();
        JMSConnectionFactory.getInstance().setListener(replyQ, this);
        log.info("JMSBitmagArcRepositoryClient listens for replies on channel '{}'", replyQ);

        File configDir = Settings.getFile(BITREPOSITORY_SETTINGS_DIR);
        log.info("Getting bitmag config from " + BITREPOSITORY_SETTINGS_DIR + "=" + configDir.getAbsolutePath());

        String keyfilename = Settings.get(BITREPOSITORY_KEYFILENAME);

        String collectionId = Settings.get(BITREPOSITORY_COLLECTIONID);
        if (collectionId == null || collectionId.trim().isEmpty()) {
            collectionId = Settings.get(CommonSettings.ENVIRONMENT_NAME);
            log.info("No collectionId set so using default value {}", collectionId);
        }
        this.collectionId = collectionId;
        this.maxStoreFailures = Settings.getInt(BITREPOSITORY_STORE_MAX_PILLAR_FAILURES);
        this.usepillar = Settings.get(BITREPOSITORY_USEPILLAR);

        File tempdir = Settings.getFile(BITREPOSITORY_TEMPDIR);
        try {
            FileUtils.forceMkdir(tempdir);
            log.info("Storing tempfiles in folder {}", tempdir);
        } catch (IOException e) {
            throw new IOFailure("Failed to create tempdir '" + tempdir + "'", e);
        }

        // Initialize connection to the bitrepository
        this.bitrep = Bitrepository.getInstance(configDir, keyfilename);
        if (!bitrep.getKnownCollections().contains(this.collectionId)) {
            close();
            throw new ArgumentNotValid("The bitrepository doesn't know about the collection " + this.collectionId);
        }
    }

    /**
     * Get an instance of this class. This is guaranteed to be a singleton.
     *
     * @return an JMSArcRepositoryClient instance.
     */
    public static synchronized JMSBitmagArcRepositoryClient getInstance() {
        if (instance == null) {
            instance = new JMSBitmagArcRepositoryClient();
        }
        return instance;
    }

    /** Removes this object as a JMS listener. */
    @Override
    public synchronized void close() {
        JMSConnectionFactory.getInstance().removeListener(replyQ, this);
        if (bitrep != null) {
            bitrep.shutdown();
        }
        instance = null;
    }

    /**
     * Sends a GetMessage on the "TheArcrepos" queue and waits for a reply. This is a blocking call. Returns null if no
     * message is returned within Settings.ARCREPOSITORY_GET_TIMEOUT
     *
     * @param arcfile The name of a file.
     * @param index The offset of the wanted record in the file
     * @return a BitarchiveRecord-object or null if request times out or object is not found.
     * @throws ArgumentNotValid If the given arcfile is null or empty, or the given index is negative.
     * @throws IOFailure If a wrong message is returned or the get operation failed.
     */
    public BitarchiveRecord get(String arcfile, long index) throws ArgumentNotValid, IOFailure {
        ArgumentNotValid.checkNotNullOrEmpty(arcfile, "arcfile");
        ArgumentNotValid.checkNotNegative(index, "index");
        log.debug("Requesting get of record '{}:{}'", arcfile, index);

        long start = System.currentTimeMillis();
        GetMessage requestGetMsg = new GetMessage(Channels.getTheRepos(), replyQ, arcfile, index);
        NetarkivetMessage replyNetMsg = sendAndWaitForOneReply(requestGetMsg, timeoutGetOpsMillis);
        long timePassed = System.currentTimeMillis() - start;

        if (replyNetMsg == null) {
            
            log.warn("Request for record({}:{}) timed out after {} seconds. Returning null BitarchiveRecord", arcfile,
                    index, (timeoutGetOpsMillis / 1000));
            return null;
        } else {
            log.debug("Reply for '{}:{}' received after {} seconds", arcfile, index, (timePassed / 1000));
        }

        GetMessage replyGetMsg;
        try {
            replyGetMsg = (GetMessage) replyNetMsg;
        } catch (ClassCastException e) {
            throw new IOFailure("Received invalid argument reply: '" + replyNetMsg + "'. Not a GetMessage", e);
        }

        if (!replyGetMsg.isOk()) {
            throw new IOFailure("GetMessage failed: '" + replyGetMsg.getErrMsg() + "'");
        }
        return replyGetMsg.getRecord();
    }

    /**
     * Synchronously retrieves a file from a bitarchive and places it in a local file. This is the interface for sending
     * GetFileMessage on the "TheArcrepos" queue. This is a blocking call.
     *
     * @param arcfilename Name of the arcfile to retrieve.
     * @param replica The bitarchive to retrieve the data from.
     * @param toFile Filename of a place where the file fetched can be put.
     * @throws ArgumentNotValid If the arcfilename are null or empty, or if either replica or toFile is null.
     * @throws IOFailure if there are problems getting a reply or the file could not be found.
     */
    public void getFile(String arcfilename, Replica replica, File toFile) throws ArgumentNotValid, IOFailure {
        ArgumentNotValid.checkNotNullOrEmpty(arcfilename, "arcfilename");
        ArgumentNotValid.checkNotNull(replica, "replica");
        ArgumentNotValid.checkNotNull(toFile, "toFile");

        log.debug("Requesting get of file '{}' from '{}'", arcfilename, replica);
        // ArgumentNotValid.checkNotNull(replyQ, "replyQ must not be null");
        GetFileMessage gfMsg = new GetFileMessage(Channels.getTheRepos(), replyQ, arcfilename, replica.getId());
        GetFileMessage getFileMessage = (GetFileMessage) sendAndWaitForOneReply(gfMsg, 0);
        if (getFileMessage == null) {
            throw new IOFailure("GetFileMessage timed out before returning." + "File not found?");
        } else if (!getFileMessage.isOk()) {
            throw new IOFailure("GetFileMessage failed: " + getFileMessage.getErrMsg());
        } else {
            getFileMessage.getData(toFile);
        }
    }


    /**
     * Store the file in the bitrepository.
     * After a successful storage operation, both the local copy of the file and the copy on the ftp server are deleted.
     *
     * @param file A file to be stored. Must exist.
     * @throws IOFailure thrown if store is unsuccessful, or failed to clean up files locally or on the ftp server after
     * the store operation.
     * @throws ArgumentNotValid if file parameter is null or file is not an existing file.
     */
    public void store(File file) throws IOFailure, ArgumentNotValid {
        //TODO revisit this method if we want to be able to accept uploads to only a subset of pillars, and have the bitrepo heal itself later
        ArgumentNotValid.checkExistsNormalFile(file, "File '" + file + "' does not exist");

        final String fileId = file.getName();

        // upload file

        //Attempt to upload the file.
        // If not there, this will work
        // If already there, with same checksum, this will work.
        // If already there, with different checksum, this will fail
        boolean uploadSuccessful = bitrep.uploadFile(file, fileId, collectionId, maxStoreFailures);
        if (!uploadSuccessful) {
            String errMsg =
                    "Upload to collection '" + collectionId + "' of file '" + fileId + "' failed.";
            error(errMsg);
        } else {
            //TODO check if this check is actually ever nessesary
            log.info("Upload to collection '{}' of file '{}' reported success, so let's check", collectionId, fileId);
            checkFileConsistency(file, fileId);
            log.info("Upload to collection '{}' of file '{}' was successful", collectionId, fileId);
        }
    }

    /**
     * Checks the consistency of a file across all pillars after its upload.
     * @param file The file to have been uploaded.
     * @param fileId The id of the file.
     */
    protected void checkFileConsistency(File file, String fileId) {
        //get the known checksums for the file in bitrep
        Map<String, ChecksumsCompletePillarEvent> checksumResults =
                bitrep.getChecksums(fileId, collectionId, maxStoreFailures);

        //for each pillar in this collection
        for (String collectionPillar: BitmagUtils.getKnownPillars(collectionId) ){
            boolean foundInThisPillar = false;

            //Get the checksum result for this pillar for this file
            ChecksumsCompletePillarEvent checksumResult = checksumResults.get(collectionPillar);

            for (ChecksumDataForChecksumSpecTYPE checksum : checksumResult.getChecksums().getChecksumDataItems()) {

                //for each checksum result for this file (there should be none others but...)
                if (fileId.equals(checksum.getFileID())) {

                    //mark the file as found in this pillar
                    foundInThisPillar = true;

                    //Checksum the local file so we can compare
                    ChecksumDataForFileTYPE validationChecksum =
                            BitmagUtils.getValidationChecksum(file, checksumResult.getChecksumType());

                    //If the checksums do not match, we have a failure
                    if ( ! Arrays.equals(validationChecksum.getChecksumValue(), checksum.getChecksumValue())) {
                        String errMsg =
                                fileId + " in " + collectionId + " in " + collectionPillar+" has a different checksum than local file " + file;
                        error(errMsg);
                        return;
                    }
                }
            }

            if (! foundInThisPillar ) {
                String errMsg =
                        fileId + " in " + collectionId + " was missing on pillar "+collectionPillar;
                error(errMsg);
                return;
            }
        }
    }

    /**
     * Handle an error situation. Sends a notification, and throws an error.
     * @param errMsg The message for the error.
     */
    protected void error(String errMsg) {
        NotificationsFactory.getInstance().notify(errMsg, NotificationType.ERROR);
        throw new IOFailure(errMsg);
    }

    /**
     * Runs a batch batch job on each file in the ArcRepository.
     * <p>
     * Note: The id for the batchjob is the empty string, which removes the possibility of terminating the batchjob
     * remotely while it is running.
     *
     * @param job An object that implements the FileBatchJob interface. The initialize() method will be called before
     * processing and the finish() method will be called afterwards. The process() method will be called with each File
     * entry. An optional function postProcess() allows handling the combined results of the batchjob, e.g. summing the
     * results, sorting, etc.
     * @param replicaId The archive to execute the job on.
     * @param args The arguments for the batchjob.
     * @return The status of the batch job after it ended.
     */
    public BatchStatus batch(FileBatchJob job, String replicaId, String... args) {
        return batch(job, replicaId, "", args);
    }

    /**
     * Runs a batch job on each file in the ArcRepository.
     *
     * @param job An object that implements the FileBatchJob interface. The initialize() method will be called before
     * processing and the finish() method will be called afterwards. The process() method will be called with each File
     * entry. An optional function postProcess() allows handling the combined results of the batchjob, e.g. summing the
     * results, sorting, etc.
     * @param replicaId The archive to execute the job on.
     * @param args The arguments for the batchjob. This is allowed to be null.
     * @param batchId The id for the batch process.
     * @return The status of the batch job after it ended.
     * @throws ArgumentNotValid If the job is null or the replicaId is either null or the empty string.
     * @throws IOFailure If no result file is returned.
     */
    public BatchStatus batch(FileBatchJob job, String replicaId, String batchId, String... args) throws IOFailure,
            ArgumentNotValid {
        ArgumentNotValid.checkNotNull(job, "FileBatchJob job");
        ArgumentNotValid.checkNotNullOrEmpty(replicaId, "String replicaId");

        log.debug("Starting batchjob '{}' running on replica '{}'", job, replicaId);
        BatchMessage bMsg = new BatchMessage(Channels.getTheRepos(), replyQ, job, replicaId, batchId, args);
        log.debug("Sending batchmessage to queue '{}' with replyqueue set to '{}'", Channels.getTheRepos(), replyQ);
        BatchReplyMessage brMsg = (BatchReplyMessage) sendAndWaitForOneReply(bMsg, 0);
        if (!brMsg.isOk()) {
            String msg = "The batch job '" + bMsg + "' resulted in the following " + "error: " + brMsg.getErrMsg();
            log.warn(msg);
            if (brMsg.getResultFile() == null) {
                // If no result is available at all, this is non-recoverable
                throw new IOFailure(msg);
            }
        }
        return new BatchStatus(brMsg.getFilesFailed(), brMsg.getNoOfFilesProcessed(), brMsg.getResultFile(),
                job.getExceptions());
    }

    /**
     * Not implemented. This functionality is delegated to bitrepository software.
     */
    @Override
    public void updateAdminData(String fileName, String replicaId, ReplicaStoreState newval) throws ArgumentNotValid,
            IOFailure {
        throw new NotImplementedException("updateAdminData is delegated to the bitrepository software");
    }

    /**
     * Not implemented. This functionality is delegated to bitrepository software.
     */
    @Override
    public void updateAdminChecksum(String filename, String checksum) {
        throw new NotImplementedException("updateAdminChecksum is delegated to the bitrepository software");
    }

    /**
     * Not implemented. This functionality is delegated to bitrepository software.
     */
    @Override
    @Deprecated
    public File removeAndGetFile(String fileName, String bitarchiveId, String checksum, String credentials)
            throws IOFailure, ArgumentNotValid {
        throw new NotImplementedException("removeAndGetFile is delegated to the bitrepository software");
    }

    /**
     * Not implemented. This functionality is delegated to bitrepository software.
     */
    @Override
    @Deprecated
    public File getAllChecksums(String replicaId) throws IOFailure, ArgumentNotValid {
        throw new NotImplementedException("getAllChecksums is delegated to the bitrepository software");
    }

    /**
     * Not implemented. This functionality is delegated to bitrepository software.
     */
    @Override
    @Deprecated
    public File getAllFilenames(String replicaId) throws ArgumentNotValid, IOFailure {
        throw new NotImplementedException("getAllFilenames is delegated to the bitrepository software");
    }

    /**
     * Not implemented. This functionality is delegated to bitrepository software.
     */
    @Override
    @Deprecated
    public String getChecksum(String replicaId, String filename) throws ArgumentNotValid, IOFailure {
        throw new NotImplementedException("GetChecksum is not implemented here");
    }

    /**
     * Not implemented. This functionality is delegated to bitrepository software.
     */
    @Override
    @Deprecated
    public File correct(String replicaId, String checksum, File file, String credentials) throws IOFailure,
            ArgumentNotValid {
        throw new NotImplementedException("Correct is delegated to the bitrepository software");

    }


    /**
     * Attempts to upload a given file.    NEW
     *
     * @param file The file to upload. Should exist. The packageId is the name of the file
     * @param collectionId The Id of the collection to upload to
     * @param maxNumberOfFailingPillars Max number of acceptable store failures
     * @return true if the upload succeeded, false otherwise.
     */
    public boolean uploadFile(final File file, final String fileId, final String collectionId,
            int maxNumberOfFailingPillars) {
        ArgumentNotValid.checkExistsNormalFile(file, "File file");
        // Does collection exists? If not return false
        if (BitmagUtils.getKnownPillars(collectionId).isEmpty()) {
            log.warn("The given collection Id {} does not exist", collectionId);
            return false;
        }
        boolean success = false;
        try {
            OperationEvent.OperationEventType finalEvent = putTheFile(putfileClientInstance, file, fileId, collectionId,
                    maxNumberOfFailingPillars); // TODO where to put it?
            if(finalEvent == OperationEvent.OperationEventType.COMPLETE) {
                success = true;
                log.info("File '{}' uploaded successfully. ",file.getAbsolutePath());
            } else {
                log.warn("Upload of file '{}' failed with event-type '{}'.", file.getAbsolutePath(), finalEvent);
            }
        } catch (Exception e) {
            log.warn("Unexpected error while storing file '{}'", file.getAbsolutePath(), e);
            success = false;
        }
        return success;
    }


    /**
     * Upload the file to the uploadserver, initiate the PutFile request, and wait for the  NEW NOT finished
     * request to finish.
     * @param client the PutFileClient responsible for the put operation.
     * @param packageFile The package to upload
     * @param collectionID The ID of the collection to upload to.
     * @param maxNumberOfFailingPillars Max number of acceptable store failures
     * @return OperationEventType.FAILED if operation failed; otherwise returns OperationEventType.COMPLETE
     * @throws IOException If unable to upload the packageFile to the uploadserver
     */
    private OperationEvent.OperationEventType putTheFile(PutFileClient client, File packageFile, String fileID, String collectionID,
            int maxNumberOfFailingPillars) throws IOException, URISyntaxException {

        // use public PutFileAction(PutFileClient client, String collectionID, File targetFile, String fileID)
        FileExchange fileExchange = null;
        URL url = null;
        try {                                                                                   // NEW
            PutFileAction ca = new PutFileAction(client, collectionId, packageFile, fileID);
            ca.performAction();

            // --> To be changed
            fileExchange = BitmagUtils.getFileExchange();
            // FileExchange fileexchange = ProtocolComponentFactory.getInstance().getFileExchange(this.bitmagSettings);
            // BlockingPutFileClient bpfc = new BlockingPutFileClient(client);

            // URL url = fileexchange.putFile(packageFile);
            url = BitmagUtils.getFileExchangeBaseURL();
            // ChecksumSpecTYPE csSpec = ChecksumUtils.getDefault(this.bitmagSettings);

            GetChecksumsClient chkClient = BitmagUtils.getChecksumsClient();
            // chkClient.getChecksums();


           // ChecksumDataForFileTYPE validationChecksum = BitmagUtils.getValidationChecksum(
           //         packageFile, csSpec);

            ChecksumSpecTYPE requestChecksum = null;
            String putFileMessage = "Putting the file '" + packageFile + "' with the file id '"
                    + fileID + "' from Netarchivesuite";

            // NetarchivesuiteBlockingEventHandler putFileEventHandler = new NetarchivesuiteBlockingEventHandler(collectionID,
            //      maxNumberOfFailingPillars);
            // <-- To be changed
        /*
        try {
            bpfc.putFile(collectionID, url, fileID, packageFile.length(), validationChecksum, requestChecksum,
                    putFileEventHandler, putFileMessage);
        } catch (OperationFailedException e) {
            log.warn("The putFile Operation was not a complete success ({})."
                    + " Checksum whether we accept anyway.", putFileMessage, e);
            if(putFileEventHandler.hasFailed()) {
                return OperationEvent.OperationEventType.FAILED;
            } else {
                return OperationEvent.OperationEventType.COMPLETE;
            }
         */


        } finally {
            // delete the uploaded file from server
            fileExchange.deleteFile(url);
        }
        log.info("The putFile Operation succeeded ({})", "putFileMessage");
        return OperationEvent.OperationEventType.COMPLETE;
    }

}
