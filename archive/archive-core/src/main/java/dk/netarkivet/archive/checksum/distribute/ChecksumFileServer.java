/*
 * #%L
 * Netarchivesuite - archive
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
package dk.netarkivet.archive.checksum.distribute;

import java.io.File;
import java.nio.file.Files;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.archive.ArchiveSettings;
import dk.netarkivet.archive.bitarchive.distribute.UploadMessage;
import dk.netarkivet.archive.checksum.ChecksumArchive;
import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.distribute.Channels;
import dk.netarkivet.common.distribute.JMSConnectionFactory;
import dk.netarkivet.common.distribute.RemoteFile;
import dk.netarkivet.common.distribute.RemoteFileFactory;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IllegalState;
import dk.netarkivet.common.exceptions.UnknownID;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.NotificationType;
import dk.netarkivet.common.utils.NotificationsFactory;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.utils.SystemUtils;

/**
 * The server for the ChecksumFileApplication. Used for communication between the ArcRepository and the checksum
 * archive.
 */
public class ChecksumFileServer extends ChecksumArchiveServer {

    /** The logger used by this class. */
    private static final Logger log = LoggerFactory.getLogger(ChecksumFileServer.class);

    /** The instance of this server. */
    protected static ChecksumFileServer instance;

    /** The archive which contain the actual data. */
    protected ChecksumArchive cs;

    /** The character to separate the applicationInstanceId and the IP address. */
    public static final String APPLICATION_ID_SEPARATOR = "_";

	private boolean usePrecomputedChecksumDuringUpload;

    /**
     * Returns the unique instance of this class.
     * <p>
     * The server creates an instance of the checksum it creates access to and starts to listen to a JMS messages on the
     * incoming JMS queue.
     * <p>
     * <p>
     * Should this do the heart beats to a monitor? This would be quite odd, since Checksum does not use a monitor.
     *
     * @return This instance.
     */
    public static ChecksumFileServer getInstance() {
        if (instance == null) {
            instance = new ChecksumFileServer();
        }
        return instance;
    }

    /**
     * Constructor.
     */
    private ChecksumFileServer() {
        // log that this instance is been invoked.
        log.info("Initialising the ChecksumFileServer.");

        // get the instance of the checksum archive
        cs = ChecksumArchiveFactory.getInstance();

        // initialise the JMSConnection.
        jmsCon = JMSConnectionFactory.getInstance();

        // initialise the channel.
        theCR = Channels.getTheCR();

        // Start listening to the channel.
        jmsCon.setListener(theCR, this);

        // create the application identifier
        checksumAppId = createAppId();

        usePrecomputedChecksumDuringUpload = Settings.getBoolean(ArchiveSettings.CHECKSUM_USE_PRECOMPUTED_CHECKSUM_DURING_UPLOAD);
        
        // log that this instance has successfully been invoked.
        log.info("ChecksumFileServer '{}' initialised. Using precomputedChecksums during upload: {}", checksumAppId, usePrecomputedChecksumDuringUpload);
    }

    /**
     * Method for closing the instance.
     */
    public void close() {
        log.info("ChecksumFileServer '{}' closing down.", checksumAppId);
        cleanup();
        if (jmsCon != null) {
            jmsCon.removeListener(theCR, this);
            jmsCon = null;
        }
        log.info("ChecksumFileServer '{}' closed down.", checksumAppId);
    }

    /**
     * Method for cleaning up, when closing this instance down.
     */
    public void cleanup() {
        instance = null;
        cs.cleanup();
    }

    /**
     * Method for retrieving the identification of this application.
     *
     * @return The id of this application.
     */
    public String getAppId() {
        return checksumAppId;
    }

    /**
     * Method for creating the identification for this application.
     *
     * @return The id of this application.
     */
    protected String createAppId() {
        String id;
        // Create an id with the IP address of this current host
        id = SystemUtils.getLocalIP();

        // Append an underscore and APPLICATION_INSTANCE_ID from settings
        // to the id, if specified in settings.
        // If no APPLICATION_INSTANCE_ID is found do nothing.
        try {
            String applicationInstanceId = Settings.get(CommonSettings.APPLICATION_INSTANCE_ID);
            if (!applicationInstanceId.isEmpty()) {
                id += APPLICATION_ID_SEPARATOR + applicationInstanceId;
            }
        } catch (UnknownID e) {
            // Ignore the fact, that there is no APPLICATION_INSTANCE_ID in
            // settings
            log.warn("No setting APPLICATION_INSTANCE_ID found in settings: ", e);
        }
        return id;
    }

    /**
     * The method for uploading arc files. Note that cleanup of the upload file embedded in the message is delegated the
     * method {@link ChecksumArchive#upload(RemoteFile, String)}
     *
     * @param msg The upload message, containing the file to upload.
     * @throws ArgumentNotValid If the UploadMessage is null.
     */
    public void visit(UploadMessage msg) throws ArgumentNotValid {
        ArgumentNotValid.checkNotNull(msg, "UploadMessage msg");
        log.debug("Receiving UploadMessage: " + msg.toString());
        try {
            try {
            	if (usePrecomputedChecksumDuringUpload) {
            		cs.upload(msg.getPrecomputedChecksum(), msg.getArcfileName());
            	} else {
            		cs.upload(msg.getRemoteFile(), msg.getArcfileName());
            	}
            } catch (Throwable e) {
                log.warn("Cannot process upload message '{}'", msg, e);
                msg.setNotOk(e);
            } finally { // check if enough space
                if (!cs.hasEnoughSpace()) {
                    jmsCon.removeListener(theCR, this);
                	String errMsg = "Not enough space any more. Stopped listening to messages from " 
                			+ theCR + ". Restart application after fixing problem";
                	
                    log.warn(errMsg);
                    NotificationsFactory.getInstance().notify(errMsg, NotificationType.ERROR);
                }
            }
        } catch (Throwable e) {
            log.warn("Cannnot remove listener after upload message '{}'", msg, e);
        } finally {
            log.debug("Replying to UploadMessage: {}", msg.toString());
            jmsCon.reply(msg);
        }
    }

    /**
     * Method for correcting an entry in the archive. It start by ensuring that the file exists, then it checks the
     * credentials. Then it is checked whether the "bad entry" does have the "bad checksum". If no problems occurred,
     * then the bad entry will be corrected by the archive (the bad entry is removed from the archive file and put into
     * the "wrong entry" file. Then the new entry is placed in the archive file.
     * <p>
     * If it fails in any of the above, then the method fails (throws an exception which is caught and use for replying
     * NotOk to the message).
     *
     * @param msg The message containing the correct instance of the file to correct.
     * @throws ArgumentNotValid If the correct message is null.
     */
    public void visit(CorrectMessage msg) throws ArgumentNotValid {
        ArgumentNotValid.checkNotNull(msg, "CorrectMessage msg");
        log.debug("Receiving correct message: {}", msg.toString());
        // the file for containing the received file from the message.
        File correctFile = null;
        try {
            String filename = msg.getArcfileName();
            String currentCs = cs.getChecksum(filename);
            String incorrectCs = msg.getIncorrectChecksum();

            // ensure that the entry actually exists.
            if (currentCs == null) {
                // This exception is logged later.
                throw new IllegalState("Cannot correct an entry for the file '" + filename
                        + "', since it is not within the archive.");
            }

            // Check credentials
            String credentialsReceived = msg.getCredentials();
            if (credentialsReceived == null || credentialsReceived.isEmpty()
                    || !credentialsReceived.equals(Settings.get(ArchiveSettings.ENVIRONMENT_THIS_CREDENTIALS))) {
                throw new IllegalState("The received credentials '" + credentialsReceived
                        + "' were invalid. The entry of " + "file '" + filename + "' will not be corrected.");
            }

            // check that the current checksum is incorrect as supposed.
            if (!currentCs.equals(incorrectCs)) {
                throw new IllegalState("Wrong checksum for the entry for file '" + filename + "' has the checksum '"
                        + currentCs + "', " + "though it was supposed to have the checksum '" + incorrectCs + "'.");
            }

            // retrieve the data as a file.
            correctFile = Files.createTempFile(FileUtils.getTempDir().toPath(), "correct", filename).toFile();
            msg.getData(correctFile);

            // Log and notify
            String warning = "The record for file '" + filename + "' is being corrected at '"
                    + Settings.get(CommonSettings.USE_REPLICA_ID) + "'";
            log.warn(warning);
            NotificationsFactory.getInstance().notify(warning, NotificationType.WARNING);

            // put the file into the archive.
            File badFile = cs.correct(filename, correctFile);

            // Send the file containing the removed entry back.
            msg.setRemovedFile(RemoteFileFactory.getMovefileInstance(badFile));
        } catch (Throwable t) {
            // Handle errors.
            log.warn("Cannot handle CorrectMessage: '{}'", msg, t);
            msg.setNotOk(t);
        } finally {
            // log and reply at the end.
            log.info("Replying CorrectMessage: {}", msg.toString());
            jmsCon.reply(msg);

            // cleanup the data file
            if (correctFile != null) {
                FileUtils.remove(correctFile);
            }
        }
    }

    /**
     * Method for retrieving the checksum of a record.
     *
     * @param msg The GetChecksumMessage which contains the name of the record to have its checksum retrieved.
     * @throws ArgumentNotValid If the message is null.
     */
    public void visit(GetChecksumMessage msg) throws ArgumentNotValid {
        ArgumentNotValid.checkNotNull(msg, "GetChecksumMessage msg");

        log.debug("Receiving GetChecksumMessage: {}", msg.toString());
        try {
            // get the name of the arc file
            String filename = msg.getArcfileName();
            // get the checksum of the arc file
            String checksum = cs.getChecksum(filename);

            // Check if the checksum was found. If not throw exception.
            if (checksum == null || checksum.isEmpty()) {
                // The error is logged, when the exception is caught.
                throw new IllegalState("Cannot fetch checksum of an entry, " + filename
                        + ", which is not within the archive.");
            }

            // send the checksum of the arc file.
            msg.setChecksum(checksum);
        } catch (Throwable e) {
            // Handle errors (if the file cannot be found).
            log.warn("Cannot handle '{}' containing the message: {}", msg.getClass().getName(), msg, e);
            msg.setNotOk(e);
        } finally {
            // TODO this should be set elsewhere.
            msg.setIsReply();
            // log the message and reply.
            log.info("Replying GetChecksumMessage: {}", msg.toString());
            jmsCon.reply(msg);
        }
    }

    /**
     * Method for retrieving all the filenames within the archive.
     *
     * @param msg The GetAllFilenamesMessage.
     * @throws ArgumentNotValid If the GetAllFilenamesMessages is null.
     */
    public void visit(GetAllFilenamesMessage msg) throws ArgumentNotValid {
        ArgumentNotValid.checkNotNull(msg, "GetAllFilenamesMessage msg");
        log.debug("Receiving GetAllFilenamesMessage: {}", msg.toString());

        try {
            // get all the file names
            msg.setFile(cs.getAllFilenames());
        } catch (Throwable e) {
            log.warn("Cannot retrieve the filenames to reply on the {} : {}", msg.getClass().getName(), msg, e);
            msg.setNotOk(e);
        } finally {
            // log the message and reply.
            log.info("Replying GetAllFilenamesMessage: {}", msg.toString());
            jmsCon.reply(msg);
        }
    }

    /**
     * Method for retrieving a map containing all the checksums and their corresponding filenames within the archive.
     *
     * @param msg The GetAllChecksumMessage.
     * @throws ArgumentNotValid If the GetAllChecksumMessage is null.
     */
    public void visit(GetAllChecksumsMessage msg) throws ArgumentNotValid {
        ArgumentNotValid.checkNotNull(msg, "GetAllChecksumsMessage msg");
        log.debug("Receiving GetAllChecksumsMessage: {}", msg.toString());

        try {
            msg.setFile(cs.getArchiveAsFile());
        } catch (Throwable e) {
            log.warn("Cannot retrieve all the checksums.", e);
            msg.setNotOk(e);
        } finally {
            // log the message and reply
            log.info("Replying GetAllChecksumsMessage: {}", msg.toString());
            jmsCon.reply(msg);
        }
    }

}
