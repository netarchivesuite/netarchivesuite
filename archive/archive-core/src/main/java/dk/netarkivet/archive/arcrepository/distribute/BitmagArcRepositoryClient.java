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
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import javax.jms.JMSException;

import org.apache.commons.io.FileUtils;
import org.bitrepository.access.getfile.GetFileClient;
import org.bitrepository.modify.putfile.PutFileClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.distribute.ChannelID;
import dk.netarkivet.common.distribute.Channels;
import dk.netarkivet.common.distribute.JMSConnectionFactory;
import dk.netarkivet.common.distribute.Synchronizer;
import dk.netarkivet.common.distribute.arcrepository.ArcRepositoryClient;
import dk.netarkivet.common.distribute.arcrepository.BatchStatus;
import dk.netarkivet.common.distribute.arcrepository.BitarchiveRecord;
import dk.netarkivet.common.distribute.arcrepository.Replica;
import dk.netarkivet.common.distribute.arcrepository.ReplicaStoreState;
import dk.netarkivet.common.distribute.bitrepository.BitmagUtils;
import dk.netarkivet.common.distribute.bitrepository.action.getfile.GetFileAction;
import dk.netarkivet.common.distribute.bitrepository.action.putfile.PutFileAction;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.NotImplementedException;
import dk.netarkivet.common.utils.NotificationType;
import dk.netarkivet.common.utils.NotificationsFactory;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.utils.batch.FileBatchJob;
import dk.netarkivet.common.utils.service.WarcRecordClient;

/**
 * Client side usage of an arc repository. All non-writing requests are forwarded to the ArcRepositoryServer over the network.
 * Store requests are sent directly to the bitrepository messagebus.
 *
 * Get and store messages are retried a number of time before giving up, and will timeout after a specified time.
 *
 * So called mixed-mode client, where the store is done to a Bitmagrepository, and the rest (access and batch-processing)
 * is done using the distributed netarchivesuite archive.
 */
public class BitmagArcRepositoryClient extends Synchronizer implements ArcRepositoryClient, AutoCloseable {

    /** the one and only BitmagArcRepositoryClient instance. */
    private static BitmagArcRepositoryClient instance;

    /** Logging output place. */
    protected static final Logger log = LoggerFactory.getLogger(BitmagArcRepositoryClient.class);

    /** Listens on this queue for replies. */
    private final ChannelID replyQ;

    private long timeoutGetOpsMillis;

    // NOTE: The constants defining setting names below are left non-final on
    // purpose! Otherwise, the static initialiser that loads default values
    // will not run.

    /** The default place in classpath where the settings file can be found. */
    private static String defaultSettingsClasspath = "dk/netarkivet/common/distribute/arcrepository/bitrepository/"
            + "BitmagArcRepositoryClientSettings.xml";

    /*
     * The static initialiser is called when the class is loaded. It will add default values for all settings defined in
     * this class, by loading them from a settings.xml file in classpath.
     */
    static {
        Settings.addDefaultClasspathSettings(defaultSettingsClasspath);
        BitmagUtils.initialize();
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

    /** The pillar to use.*/
    private String usepillar;

    private WarcRecordClient warcRecordClient;

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
    private BitmagArcRepositoryClient() {
        timeoutGetOpsMillis = Settings.getLong(ARCREPOSITORY_GET_TIMEOUT);
        log.info("BitmagArcRepositoryClient will timeout on each get request after {} milliseconds.",
                timeoutGetOpsMillis);

        replyQ = Channels.getThisReposClient();
        JMSConnectionFactory.getInstance().setListener(replyQ, this);
        log.info("BitmagArcRepositoryClient listens for replies on channel '{}'", replyQ);

        File configDir = Settings.getFile(BITREPOSITORY_SETTINGS_DIR);
        log.info("Getting bitmag config from " + BITREPOSITORY_SETTINGS_DIR + "=" + configDir.getAbsolutePath());

        this.collectionId = BitmagUtils.getDefaultCollectionID();
        log.info("Using '{}' as default collectionID", collectionId);

        if (BitmagUtils.getKnownPillars(collectionId).isEmpty()) {
            log.warn("The given collection Id '{}' does not exist", collectionId);
            throw new RuntimeException("collection Id does not exist");
        }
        this.usepillar = Settings.get(BITREPOSITORY_USEPILLAR);

        File tempdir = Settings.getFile(BITREPOSITORY_TEMPDIR);
        try {
            FileUtils.forceMkdir(tempdir);
            log.info("Storing tempfiles in folder '{}'", tempdir);
        } catch (IOException e) {
            throw new IOFailure("Failed to create tempdir '" + tempdir + "'", e);
        }

        URI baseUrl;
        try {
            baseUrl = new URI(Settings.get(CommonSettings.WRS_BASE_URL));
        } catch (URISyntaxException e) {
            throw new IOFailure("Invalid url '" + Settings.get(CommonSettings.WRS_BASE_URL)
                    + "' provided for warc record service as base url");
        }
        warcRecordClient = new WarcRecordClient(baseUrl);
    }

    /**
     * Get an instance of this class. This is guaranteed to be a singleton.
     *
     * @return an JMSArcRepositoryClient instance.
     */
    public static synchronized BitmagArcRepositoryClient getInstance() {
        if (instance == null) {
            instance = new BitmagArcRepositoryClient();
        }
        return instance;
    }

    /** Removes this object as a JMS listener. */
    @Override
    public synchronized void close() {
        try {
            JMSConnectionFactory.getInstance().removeListener(replyQ, this);
            instance = null;
            BitmagUtils.shutdown();
        }
        catch (JMSException e){
            log.error("JMS could not be closed properly");
        }
    }

    /**
     * Initializes a WarcRecordClient to interact with the warc records service and uses it to request a record
     * with the given name and offset/index.
     *
     * @param arcfile The name of a file.
     * @param index The offset of the wanted record in the file
     * @return a BitarchiveRecord-object.
     * @throws ArgumentNotValid If the given arcfile is null or empty, or the given index is negative.
     * @throws IOFailure If an invalid URL is provided for the warc record service or the operation failed.
     */
    public BitarchiveRecord get(String arcfile, long index) throws ArgumentNotValid, IOFailure {
        ArgumentNotValid.checkNotNullOrEmpty(arcfile, "arcfile");
        ArgumentNotValid.checkNotNegative(index, "index");
        log.debug("Requesting get of record '{}:{}'", arcfile, index);
        BitarchiveRecord bitarchiveRecord = warcRecordClient.getBitarchiveRecord(arcfile, index);
        if (bitarchiveRecord == null) {
            throw new IOFailure("Got null when trying to get record '" + arcfile + ":" + index + "'.");
        }
        return bitarchiveRecord;
    }

    /**
     * Synchronously retrieves a file from a bitarchive and places it in a local file. This implementation retrieves the
     * file using bitrepository.org software.
     *
     * @param arcfilename Name of the arcfile to retrieve.
     * @param replica This parameter is ignored in this implementation. The file is retrieved from the fastest pillar.
     * @param toFile Filename of a place where the file fetched can be put. If this file already exists it must be empty
     *               otherwise this method-call will fail.
     * @throws ArgumentNotValid If the arcfilename are null or empty, or if either replica or toFile is null.
     * @throws IOFailure if there are problems getting a reply or the file could not be found.
     */
    public void getFile(String arcfilename, Replica replica, File toFile) throws ArgumentNotValid, IOFailure {
        ArgumentNotValid.checkNotNullOrEmpty(arcfilename, "arcfilename");
        ArgumentNotValid.checkNotNull(toFile, "toFile");

        if (toFile.exists() && toFile.length() == 0) {
            toFile.delete();
        }
        if (toFile.exists() && toFile.length() != 0) {
            throw new IOFailure("Cannot retrieve file from bitrepository as target file " + toFile.getAbsolutePath()
                    + " not empty.");
        }
        GetFileClient getFileClient = BitmagUtils.getFileClient();
        GetFileAction getFileAction = new GetFileAction(getFileClient, collectionId, arcfilename, toFile);
        getFileAction.performAction();

        if (!getFileAction.actionIsSuccess()) {
            String message = "Could not retrieve file " + arcfilename + ". Last status from bitrepository is " + getFileAction
                    .getInfo();
            log.warn(message);
            throw new IOFailure(message);
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

        //Attempt to upload the file.
        // If not there, this will work
        // If already there, with same checksum, this will work.
        // If already there, with different checksum, this will fail
        boolean uploadSuccessful = this.uploadFile(file, fileId);
        if (!uploadSuccessful) {
            String errMsg =
                    "Upload to collection '" + collectionId + "' of file '" + fileId + "' failed.";
            error(errMsg);
        } else {
            log.info("Upload to collection '{}' of file '{}' was successful", collectionId, fileId);
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
        String msg = "Batch is no longer used";
        log.warn(msg);
        return null;
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
        String msg = "Batch is no longer used";
        log.warn(msg);
        return null;
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
     * Attempts to upload a given file.
     * @param file The file to upload. Should exist. The packageId is the name of the file
     * @param fileId The Id of the file to upload
     * @return true if the upload succeeded, false otherwise.
     */
    public boolean uploadFile(final File file, final String fileId) {
        ArgumentNotValid.checkExistsNormalFile(file, "File file");
        boolean success = false;

        log.info("Calling putFileClient.");
        PutFileClient putFileClientLocal = BitmagUtils.getPutFileClient();
        PutFileAction putfileInstance = new PutFileAction(putFileClientLocal, collectionId, file, fileId);
        putfileInstance.performAction();

        if (putfileInstance.actionIsSuccess()) {
            success = true;
            log.info("BitmagArcRepositoryClient uploadFile.");
            log.info("File '{}' uploaded successfully. ",file.getAbsolutePath());
        } else {
            log.warn("Upload of file '{}' failed ", file.getAbsolutePath());
        }
        return success;
    }
}
