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

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import dk.netarkivet.common.distribute.arcrepository.bitrepository.Bitrepository;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.NotImplementedException;
import dk.netarkivet.common.utils.NotificationType;
import dk.netarkivet.common.utils.NotificationsFactory;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.utils.batch.FileBatchJob;

/**
 * Client side usage of an arc repository. All requests are forwarded to the ArcRepositoryServer over the network. get
 * and store messages are retried a number of time before giving up, and will timeout after a specified time.
 * POC client, where the store is done to a Bitmagrepository, and the rest is done using the distributed netarchivesuite archive.
 */
public class JMSBitmagArcRepositoryClient extends Synchronizer implements ArcRepositoryClient {

    /** The default place in classpath where the settings file can be found. */

	/* private static String defaultSettingsClasspath = "dk/netarkivet/archive/arcrepository/distribute/JMSArcRepositoryClientSettings.xml";
	 */

   

    /** The amount of milliseconds in a second. 1000. */
    private static final int MILLISECONDS_PER_SECOND = 1000;

    /** the one and only JMSArcRepositoryClient instance. */
    private static JMSBitmagArcRepositoryClient instance;

    /** Logging output place. */
    protected static final Logger log = LoggerFactory.getLogger(JMSBitmagArcRepositoryClient.class);

    /** Listens on this queue for replies. */
    private final ChannelID replyQ;

    /** The length of time to wait for a get reply before giving up. */
    private long getTimeout;

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
    }

    private static final String BITREPOSITORY_TEMPDIR = "settings.common.arcrepositoryClient.bitrepository.tempdir";    

    private static final String BITREPOSITORY_SETTINGS_DIR = "settings.common.arcrepositoryClient.bitrepository.settingsDir";
     
    // optional so we don't force the user to use credentials.
    private static final String BITREPOSITORY_KEYFILENAME = "settings.common.arcrepositoryClient.bitrepository.keyfilename"; 

    private static final String BITREPOSITORY_STORE_MAX_PILLAR_FAILURES = "settings.common.arcrepositoryClient.bitrepository.storeMaxPillarFailures";
    
    private static final String BITREPOSITORY_COLLECTIONID =  "settings.common.arcrepositoryClient.bitrepository.collectionID";
    
    private static final String BITREPOSITORY_USEPILLAR =  "settings.common.arcrepositoryClient.bitrepository.usepillar";

    private String collectionId;

	private File tempdir;

	private int maxStoreFailures;

	private Bitrepository bitrep;

	private String usepillar; 
    
    /**
     * <b>settings.common.arcrepositoryClient.getTimeout</b>: <br>
     * The setting for how many milliseconds we will wait before giving up on a lookup request to the Arcrepository.
     */
    public static final String ARCREPOSITORY_GET_TIMEOUT = "settings.common.arcrepositoryClient.bitrepository.getTimeout"; // Optional

    
    /**
     * <b>settings.common.arcrepositoryClient.storeRetries</b>: <br>
     * The setting for the number of times to try sending a store message before failing, including the first attempt.
     */
    //public static final String ARCREPOSITORY_STORE_RETRIES = "settings.common.arcrepositoryClient.storeRetries";

    /**
     * <b>settings.common.arcrepositoryClient.storeTimeout</b>: <br>
     * the setting for the timeout in milliseconds before retrying when calling {@link ArcRepositoryClient#store(File)}.
     */
    //public static final String ARCREPOSITORY_STORE_TIMEOUT = "settings.common.arcrepositoryClient.storeTimeout";

    /** Adds this Synchronizer as listener on a jms connection. */
    protected JMSBitmagArcRepositoryClient() {
        //storeRetries = Settings.getLong(ARCREPOSITORY_STORE_RETRIES);
        //storeTimeout = Settings.getLong(ARCREPOSITORY_STORE_TIMEOUT);
        getTimeout = Settings.getLong(ARCREPOSITORY_GET_TIMEOUT);

        log.info(
                "JMSBitmagArcRepositoryClient will timeout on each getrequest after {} milliseconds.", getTimeout);
        replyQ = Channels.getThisReposClient();
        JMSConnectionFactory.getInstance().setListener(replyQ, this);
        log.info("JMSBitmagArcRepositoryClient listens for replies on channel '{}'", replyQ);

    }

    private void initialiseBitrepositoryClient() {
        File configDir = Settings.getFile(BITREPOSITORY_SETTINGS_DIR);
        log.info("Getting bitmag config from " + BITREPOSITORY_SETTINGS_DIR + "=" + configDir.getAbsolutePath());
        String keyfilename = Settings.get(BITREPOSITORY_KEYFILENAME);
        this.collectionId = Settings.get(BITREPOSITORY_COLLECTIONID);
        if (this.collectionId == null || "".equals(this.collectionId)) {
            this.collectionId = Settings.get(CommonSettings.ENVIRONMENT_NAME);
        }
        this.tempdir = Settings.getFile(BITREPOSITORY_TEMPDIR);
        this.maxStoreFailures = Settings.getInt(BITREPOSITORY_STORE_MAX_PILLAR_FAILURES);
        this.usepillar = Settings.get(BITREPOSITORY_USEPILLAR);
        tempdir.mkdirs();
        log.info("Storing tempfiles in folder {}", tempdir);

        // Initialize connection to the bitrepository
        this.bitrep = new Bitrepository(configDir, keyfilename, maxStoreFailures, usepillar);
        if (!bitrep.getKnownCollections().contains(this.collectionId)) {
            throw new ArgumentNotValid("The bitrepository doesn't know about the collection " + this.collectionId);
        }
    }

    /**
     * Get an JMSArcRepositoryClient instance. This is guaranteed to be a singleton.
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
    public void close() {
        synchronized (JMSBitmagArcRepositoryClient.class) {
            JMSConnectionFactory.getInstance().removeListener(replyQ, this);
            instance = null;
        }
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
        NetarkivetMessage replyNetMsg = sendAndWaitForOneReply(requestGetMsg, getTimeout);
        long timePassed = System.currentTimeMillis() - start;
        log.debug("Reply received after {} seconds", (timePassed / MILLISECONDS_PER_SECOND));
        if (replyNetMsg == null) {
            log.info("Request for record({}:{}) timed out after {} seconds. Returning null BitarchiveRecord", arcfile,
                    index, (getTimeout / MILLISECONDS_PER_SECOND));
            return null;
        }
        GetMessage replyGetMsg;
        try {
            replyGetMsg = (GetMessage) replyNetMsg;
        } catch (ClassCastException e) {
            throw new IOFailure("Received invalid argument reply: '" + replyNetMsg + "'", e);
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
     * Sends a StoreMessage via the synchronized JMS connection method sendAndWaitForOneReply(). After a successful
     * storage operation, both the local copy of the file and the copy on the ftp server are deleted.
     *
     * @param file A file to be stored. Must exist.
     * @throws IOFailure thrown if store is unsuccessful, or failed to clean up files locally or on the ftp server after
     * the store operation.
     * @throws ArgumentNotValid if file parameter is null or file is not an existing file.
     */
    public void store(File file) throws IOFailure, ArgumentNotValid {
        ArgumentNotValid.checkExistsNormalFile(file, "File '" + file + "' does not exist");
        if (bitrep == null) {
            initialiseBitrepositoryClient();
        }
        // Check if file already exists
        if (bitrep.existsInCollection(file.getName(), collectionId)) {
            log.warn("The file '{}' is already in collection '{}'", file.getName(), collectionId);
            return;
        } else {
            // upload file
            boolean uploadSuccessful = bitrep.uploadFile(file, collectionId);
            if (!uploadSuccessful) {
                String errMsg  = "Upload to collection '" + collectionId + "' of file '" + file.getName()  + "' failed.";
                NotificationsFactory.getInstance().notify(errMsg, NotificationType.ERROR);
                throw new IOFailure(errMsg);
            }  else {
                log.info("Upload to collection '{}' of file '{}' was successful", collectionId, file.getName());
            }
        }
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
    	throw new NotImplementedException("updateAdminData is relegated to the bitrepository software");
    }

    /**
     * Not implemented. This functionality is delegated to bitrepository software.
     */
    @Override
    public void updateAdminChecksum(String filename, String checksum) {
    	throw new NotImplementedException("updateAdminChecksum is relegated to the bitrepository software");
    }

    /**
     * Not implemented. This functionality is delegated to bitrepository software.
     */
    @Override
    public File removeAndGetFile(String fileName, String bitarchiveId, String checksum, String credentials)
            throws IOFailure, ArgumentNotValid {
    	throw new NotImplementedException("removeAndGetFile is relegated to the bitrepository software"); 
    }

    /**
     * Not implemented. This functionality is delegated to bitrepository software.
     */
    @Override
    public File getAllChecksums(String replicaId) throws IOFailure, ArgumentNotValid {
    	throw new NotImplementedException("getAllChecksums is relegated to the bitrepository software"); 
    }
    
    /**
     * Not implemented. This functionality is delegated to bitrepository software.
     */
    @Override
    public File getAllFilenames(String replicaId) throws ArgumentNotValid, IOFailure {
    	throw new NotImplementedException("getAllFilenames is relegated to the bitrepository software");        
   }

    /**
     * Not implemented. This functionality is delegated to bitrepository software.
     */
    @Override
    public String getChecksum(String replicaId, String filename) throws ArgumentNotValid, IOFailure {
    	throw new NotImplementedException("GetChecksum is not implemented here");       
    }

    /**
     * Not implemented. This functionality is delegated to bitrepository software.
     */
    @Override
    public File correct(String replicaId, String checksum, File file, String credentials) throws IOFailure,
            ArgumentNotValid {
    	throw new NotImplementedException("Correct is relegated to the bitrepository software");
         
    }

}
