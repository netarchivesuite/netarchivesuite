/*
 * #%L
 * Netarchivesuite - common
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
package dk.netarkivet.common.distribute.arcrepository.bitrepository;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.jms.JMSException;

import org.bitrepository.access.AccessComponentFactory;
import org.bitrepository.access.getchecksums.BlockingGetChecksumsClient;
import org.bitrepository.access.getchecksums.GetChecksumsClient;
import org.bitrepository.access.getchecksums.conversation.ChecksumsCompletePillarEvent;
import org.bitrepository.access.getfile.GetFileClient;
import org.bitrepository.access.getfileids.GetFileIDsClient;
import org.bitrepository.bitrepositoryelements.ChecksumDataForFileTYPE;
import org.bitrepository.bitrepositoryelements.ChecksumSpecTYPE;
import org.bitrepository.bitrepositoryelements.FilePart;
import org.bitrepository.client.eventhandler.BlockingEventHandler;
import org.bitrepository.client.eventhandler.ContributorEvent;
import org.bitrepository.client.eventhandler.ContributorFailedEvent;
import org.bitrepository.client.eventhandler.OperationEvent;
import org.bitrepository.client.eventhandler.OperationEvent.OperationEventType;
import org.bitrepository.client.exceptions.NegativeResponseException;
import org.bitrepository.commandline.clients.PagingGetFileIDsClient;
import org.bitrepository.commandline.eventhandler.CompleteEventAwaiter;
import org.bitrepository.commandline.eventhandler.GetFileEventHandler;
import org.bitrepository.commandline.output.DefaultOutputHandler;
import org.bitrepository.commandline.output.OutputHandler;
import org.bitrepository.commandline.outputformatter.GetFileIDsOutputFormatter;
import org.bitrepository.common.exceptions.OperationFailedException;
import org.bitrepository.common.settings.Settings;
import org.bitrepository.common.settings.SettingsProvider;
import org.bitrepository.common.settings.XMLFileSettingsLoader;
import org.bitrepository.common.utils.ChecksumUtils;
import org.bitrepository.common.utils.SettingsUtils;
import org.bitrepository.modify.ModifyComponentFactory;
import org.bitrepository.modify.putfile.BlockingPutFileClient;
import org.bitrepository.modify.putfile.PutFileClient;
import org.bitrepository.protocol.FileExchange;
import org.bitrepository.protocol.ProtocolComponentFactory;
import org.bitrepository.protocol.messagebus.MessageBus;
import org.bitrepository.protocol.security.BasicMessageAuthenticator;
import org.bitrepository.protocol.security.BasicMessageSigner;
import org.bitrepository.protocol.security.BasicOperationAuthorizor;
import org.bitrepository.protocol.security.BasicSecurityManager;
import org.bitrepository.protocol.security.MessageAuthenticator;
import org.bitrepository.protocol.security.MessageSigner;
import org.bitrepository.protocol.security.OperationAuthorizor;
import org.bitrepository.protocol.security.PermissionStore;
import org.bitrepository.protocol.security.SecurityManager;
import org.bitrepository.settings.repositorysettings.Collection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;

/**
 * The class for interacting with the BitRepository, e.g. put files, get files, etc.
 */
public class Bitrepository implements AutoCloseable {

    /** Logging mechanism. */
    private static final Logger logger = LoggerFactory.getLogger(Bitrepository.class);

    /** National bitrepository settings. */
    private final Settings bitmagSettings;

    /** The client for performing the PutFile operation.*/
    private final PutFileClient bitMagPutClient;

    /** The client for performing the GetFile operation.*/
    private final GetFileClient bitMagGetClient;

    /** The client for performing the GetFileID operation.*/
    private final GetFileIDsClient bitMagGetFileIDsClient;

    /** The client for performing the GetChecksums operation.*/
    private final GetChecksumsClient bitMagGetChecksumsClient;

    /** The message bus used by the putfileClient. */
    private final MessageBus bitMagMessageBus;

    private static volatile Bitrepository instance;

    /**
     * Constructor for the BitRepository class.
     * @param configDir A Bitrepository settingsdirectory
     * @param bitmagKeyFilename Optional certificate filename relative to configDir
     * @throws ArgumentNotValid if configDir is null
     */
    private Bitrepository(File configDir, String bitmagKeyFilename) {
        logger.debug("Initialising bitrepository");
        ArgumentNotValid.checkExistsDirectory(configDir, "File configDir");

        /* The archive settings directory needed to upload to a bitmag style repository */
        logger.info("Reading bitrepository settings from {}", configDir.getAbsolutePath());

        /* The authentication key used by the putfileClient. */
        File privateKeyFile;
        if (bitmagKeyFilename == null){
        	privateKeyFile = new File(configDir, UUID.randomUUID().toString()); // This file should never exist
        } else {
        	privateKeyFile = new File(configDir, bitmagKeyFilename);
        }
        logger.info("keyfile: {}", privateKeyFile.getAbsolutePath());

        logger.info("Initialising bitrepository settings.");


        /* The bitrepository component id. */
        String componentId = BitrepositoryUtils.generateComponentID();
        logger.info("componentId: {}", componentId);

        SettingsProvider settingsLoader =
                new SettingsProvider(
                        new XMLFileSettingsLoader(
                                configDir.getAbsolutePath()),
                        componentId);
        bitmagSettings = settingsLoader.getSettings();
        SettingsUtils.initialize(bitmagSettings);

        logger.info("Initialising bitrepository security manager.");
        // Mandatory,even if we point to a nonexisting keyfile
        PermissionStore permissionStore = new PermissionStore();
        MessageAuthenticator authenticator = new BasicMessageAuthenticator(permissionStore);
        MessageSigner signer = new BasicMessageSigner();
        OperationAuthorizor authorizer = new BasicOperationAuthorizor(permissionStore);
        /* The bitmag security manager.*/
        SecurityManager bitMagSecurityManager = new BasicSecurityManager(bitmagSettings.getRepositorySettings(),
                privateKeyFile.getAbsolutePath(),
                authenticator, signer, authorizer, permissionStore,
                bitmagSettings.getComponentID());

        logger.info("Getting bitrepository message bus");
        bitMagMessageBus = ProtocolComponentFactory.getInstance().getMessageBus(
                bitmagSettings, bitMagSecurityManager);

        logger.info("Initialising bitrepository clients");
        bitMagPutClient = ModifyComponentFactory.getInstance().retrievePutClient(
                bitmagSettings, bitMagSecurityManager, componentId);
        AccessComponentFactory acf = AccessComponentFactory.getInstance();
        bitMagGetClient = acf.createGetFileClient(bitmagSettings, bitMagSecurityManager, componentId);
        bitMagGetFileIDsClient = acf.createGetFileIDsClient(bitmagSettings, bitMagSecurityManager, componentId);
        bitMagGetChecksumsClient = acf.createGetChecksumsClient(bitmagSettings, bitMagSecurityManager, componentId);
    }

    public static Bitrepository getInstance(File configDir, String bitmagKeyFilename) {
        if (instance == null) {
            //Double-checked locking. See https://www.baeldung.com/java-singleton-double-checked-locking
            synchronized (Bitrepository.class) {
                if (instance == null) {
                    instance = new Bitrepository(configDir, bitmagKeyFilename);
                }
            }
        }
        return instance;
    }

    /**
     * Attempts to upload a given file.
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
        if (BitrepositoryUtils.getCollectionPillars(collectionId).isEmpty()) {
            logger.warn("The given collection Id {} does not exist", collectionId);
            return false;
        }
        boolean success = false;
        try {
            OperationEventType finalEvent = putTheFile(bitMagPutClient, file, fileId, collectionId,
                    maxNumberOfFailingPillars); // TODO where to put it?
            if(finalEvent == OperationEventType.COMPLETE) {
                success = true;
                logger.info("File '{}' uploaded successfully. ",file.getAbsolutePath());
            } else {
                logger.warn("Upload of file '{}' failed with event-type '{}'.", file.getAbsolutePath(), finalEvent);
            }
        } catch (Exception e) {
            logger.warn("Unexpected error while storing file '{}'", file.getAbsolutePath(), e);
            success = false;
        }
        return success;
    }

    /**
     * Upload the file to the uploadserver, initiate the PutFile request, and wait for the
     * request to finish.
     * @param client the PutFileClient responsible for the put operation.
     * @param packageFile The package to upload
     * @param collectionID The ID of the collection to upload to.
     * @param maxNumberOfFailingPillars Max number of acceptable store failures
     * @return OperationEventType.FAILED if operation failed; otherwise returns OperationEventType.COMPLETE
     * @throws IOException If unable to upload the packageFile to the uploadserver
     */
    private OperationEventType putTheFile(PutFileClient client, File packageFile, String fileID, String collectionID,
            int maxNumberOfFailingPillars) throws IOException, URISyntaxException {
        FileExchange fileexchange = ProtocolComponentFactory.getInstance().getFileExchange(this.bitmagSettings);
        BlockingPutFileClient bpfc = new BlockingPutFileClient(client);
        URL url = fileexchange.uploadToServer(packageFile);
        ChecksumSpecTYPE csSpec = ChecksumUtils.getDefault(this.bitmagSettings);
        ChecksumDataForFileTYPE validationChecksum = BitrepositoryUtils.getValidationChecksum(
                packageFile, csSpec);

        ChecksumSpecTYPE requestChecksum = null;
        String putFileMessage = "Putting the file '" + packageFile + "' with the file id '"
                + fileID + "' from Netarchivesuite";

        NetarchivesuiteBlockingEventHandler putFileEventHandler = new NetarchivesuiteBlockingEventHandler(collectionID,
                maxNumberOfFailingPillars);
        try {
            bpfc.putFile(collectionID, url, fileID, packageFile.length(), validationChecksum, requestChecksum,
                    putFileEventHandler, putFileMessage);
        } catch (OperationFailedException e) {
            logger.warn("The putFile Operation was not a complete success ({})."
                    + " Checksum whether we accept anyway.", putFileMessage, e);
            if(putFileEventHandler.hasFailed()) {
                return OperationEventType.FAILED;
            } else {
                return OperationEventType.COMPLETE;
            }
        } finally {
            // delete the uploaded file from server
            fileexchange.deleteFromServer(url);
        }
        logger.info("The putFile Operation succeeded ({})", putFileMessage);
        return OperationEventType.COMPLETE;
    }

    /**
     * Get a file with a given fileId from a given collection.
     * @param fileId A fileId of a package known to exist in the repository
     * @param collectionId A given collection in the repository
     * @param filePart The part of the file to 'get'. Set to null, if retrieving the whole file.
     * @param usepillar Which pillar to get from
     * @return the file if found. Otherwise an exception is thrown
     * @throws IOFailure If not found or an error occurred during the fetch process.
     */
    public File getFile(final String fileId, final String collectionId, final FilePart filePart, String usepillar)
            throws IOFailure {
        ArgumentNotValid.checkNotNullOrEmpty(fileId, "String fileId");
        ArgumentNotValid.checkNotNullOrEmpty(collectionId, "String collectionId");
        // Does collection exists? If not throw exception
        if (BitrepositoryUtils.getCollectionPillars(collectionId).isEmpty()) {
            throw new IOFailure("The given collection Id does not exist");
        }
        OutputHandler output = new DefaultOutputHandler(Bitrepository.class);
        URL fileUrl = getDeliveryUrl(fileId);
        // Note that this eventHandler is blocking
        CompleteEventAwaiter eventHandler = new GetFileEventHandler(this.bitmagSettings, output);
        output.debug("Initiating the GetFile conversation.");
        String auditTrailInformation = "Retrieving package '" + fileId + "' from collection '" + collectionId
                + "' using pillar '" + usepillar + "'";
        logger.info(auditTrailInformation);

        bitMagGetClient.getFileFromSpecificPillar(collectionId, fileId, filePart, fileUrl, usepillar, eventHandler,
                auditTrailInformation);
        
        OperationEvent finalEvent = eventHandler.getFinish();
        if(finalEvent.getEventType() == OperationEventType.COMPLETE) {
            File result = null;
            try {
                result = downloadFile(fileUrl);
            } catch (IOException e) {
                throw new IOFailure(
                        "Download was successful, but we failed to create result File: ", e);
            }
            return result;
        } else {
            throw new IOFailure("Download of package w/ id '" + fileId + "' failed. Reason: "
                    + finalEvent.getInfo());
        }
    }

    /**
     * Downloads the file from the URL defined in the conversation and deletes it from the fileExchange server.
     * @param fileUrl The URL for the file to download.
     * @throws IOException If an connection issue occur.
     */
    private File downloadFile(URL fileUrl) throws IOException {
        File outputFile = File.createTempFile("Extracted", null);
        FileExchange fileexchange = BitrepositoryUtils.getFileExchange(bitmagSettings);
        String fileAddress = fileUrl.toExternalForm();
        try {
            fileexchange.downloadFromServer(outputFile, fileAddress);
        } finally {
            try {
                fileexchange.deleteFromServer(fileUrl);
            } catch (URISyntaxException e) {
                throw new IOException("Failed to delete file '"+fileUrl.toExternalForm()+"'after download",e);
            }
        }
        return outputFile;
    }

    /**
     * Generates the URL for where the file should be delivered from the GetFile operation.
     * @param fileId The id of the file.
     * @return The URL where the file should be located.
     */
    private URL getDeliveryUrl(String fileId) {
        try {
            return BitrepositoryUtils.getFileExchange(bitmagSettings).getURL(fileId);
        } catch (MalformedURLException e) {
            throw new IllegalStateException("Could not make an URL for the file '"
                    + fileId + "'.", e);
        }
    }

    /**
     * Check if a package with the following id exists within a specific collection.
     * @param packageId A given packageId
     * @param collectionID A given collection ID
     * @return true, if a package with the given ID exists within the given collection. Otherwise returns false
     */
    public boolean existsInCollection(String packageId, String collectionID) {
    	ArgumentNotValid.checkNotNullOrEmpty(packageId, "String packageId");
    	ArgumentNotValid.checkNotNullOrEmpty(collectionID, "String collectionId");
        // Does collection exists? If not return false
        List<String> collectionPillars = BitrepositoryUtils.getCollectionPillars(collectionID);
        if (collectionPillars.isEmpty()) {
            logger.warn("The given collection Id does not exist");
            return false;
        }

        OutputHandler output = new DefaultOutputHandler(Bitrepository.class);

        GetFileIDsOutputFormatter outputFormatter = new GetFileIDsNoFormatter(output);
        long timeout = BitrepositoryUtils.getClientTimeout(bitmagSettings);

        PagingGetFileIDsClient pagingClient = new PagingGetFileIDsClient(
                bitMagGetFileIDsClient, timeout, outputFormatter, output);
        
        boolean success = pagingClient.getFileIDs(collectionID, packageId, collectionPillars);
        return success;
    }

    /**
     * Check the checksums for a whole collection, or only a single packageId in a collection.
     * @param packageID A given package ID (if null, checksums for the whole collection is requested)
     * @param collectionID A given collection ID
     * @param maxNumberOfFailingPillars Max number of acceptable store failures
     * @return a map with the results from the pillars
     * @throws IOFailure If it fails to retrieve the checksums.
     */
    public Map<String, ChecksumsCompletePillarEvent> getChecksums(String packageID, String collectionID,
            int maxNumberOfFailingPillars) throws IOFailure {
    	ArgumentNotValid.checkNotNullOrEmpty(collectionID, "String collectionId");
       
        //If packageID = null, checksum is requested for all files in the collection.
        if (packageID != null) {
            logger.info("Collecting checksums for package '" + packageID + "' in collection '" + collectionID + "'");
        } else {
            logger.info("Collecting checksums for all packages in collection '" + collectionID + "'");
        }
        BlockingGetChecksumsClient client = new BlockingGetChecksumsClient(bitMagGetChecksumsClient);
        ChecksumSpecTYPE checksumSpec = ChecksumUtils.getDefault(bitmagSettings);
        BlockingEventHandler eventhandler = new NetarchivesuiteBlockingEventHandler(collectionID,
                maxNumberOfFailingPillars);

        try {
            client.getChecksums(collectionID, null, packageID, checksumSpec, null, eventhandler, null);
        } catch (NegativeResponseException e) {
            throw new IOFailure("Got bad feedback from the bitrepository ", e);
        }

        int results = eventhandler.getResults().size();
        if (results > 0) {
            logger.info("Got back {} successful responses", eventhandler.getResults().size());
        }

        int failures = eventhandler.getFailures().size();
        if (failures > 0) {
            logger.warn("Got back {} failures",  eventhandler.getFailures().size());
            for (ContributorFailedEvent failure : eventhandler.getFailures()) {
                logger.error("Failure on GetChecksums: {}, ",failure.toString());
            }
        }
        if (eventhandler.hasFailed()) {
            throw new IOFailure("Failed to retrieve checksums");
        }

        Map<String, ChecksumsCompletePillarEvent> resultsMap = new HashMap<String,
		 ChecksumsCompletePillarEvent>();

        for (ContributorEvent e : eventhandler.getResults()) {
            ChecksumsCompletePillarEvent event = (ChecksumsCompletePillarEvent) e;
            resultsMap.put(event.getContributorID(), event);
        }
        return resultsMap;
    }

    /**
     * Shutdown the messagebus.
     */
    public void shutdown() {
        if (bitMagMessageBus != null) {
            try {
                bitMagMessageBus.close();
            } catch (JMSException e) {
                logger.warn("JMSException caught during shutdown of messagebus ", e);
            }
        }
    }

    /**
     * @return The list of known CollectionIDs (and duplicates has been removed).
     */
    public List<String> getKnownCollections() {
        List<Collection> knownCollections = bitmagSettings.getRepositorySettings()
                .getCollections().getCollection();
        Set<String> collectionIDs = new HashSet<>();
        for (Collection c : knownCollections) {
            collectionIDs.add(c.getID());
        }
        return new ArrayList<>(collectionIDs);
    }

    /**
     * Retrieves the list of file ids in the given collection from the default pillar.
     * @param collectionID The collection to retrieve the list of file ids from.
     * @param usepillar Which pillar to get from
     * @return The list of file ids.
     */
    public List<String> getFileIds(String collectionID, String usepillar) {

    	OutputHandler output = new DefaultOutputHandler(Bitrepository.class);
        GetFileIDsListFormatter outputFormatter = new GetFileIDsListFormatter(output);

        long timeout = BitrepositoryUtils.getClientTimeout(bitmagSettings);
        List<String> usepillarListOnly = new ArrayList<String>();
        usepillarListOnly.add(usepillar);

        PagingGetFileIDsClient pagingClient = new PagingGetFileIDsClient(
                bitMagGetFileIDsClient, timeout, outputFormatter, output);

        boolean success = pagingClient.getFileIDs(collectionID, null,
                usepillarListOnly);
        if (success) {
        	return outputFormatter.getFoundIds();
        } else {
        	return null;
        }
    }

    @Override
    public void close() throws Exception {
        this.shutdown();
    }
}
