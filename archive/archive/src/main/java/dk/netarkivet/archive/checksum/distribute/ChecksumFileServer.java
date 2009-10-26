/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
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
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301 
 *  USA
 */
package dk.netarkivet.archive.checksum.distribute;

import java.io.File;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.archive.util.FileUtils;

import dk.netarkivet.archive.bitarchive.distribute.UploadMessage;
import dk.netarkivet.archive.checksum.ChecksumArchive;
import dk.netarkivet.archive.checksum.FileChecksumArchive;
import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.distribute.Channels;
import dk.netarkivet.common.distribute.JMSConnectionFactory;
import dk.netarkivet.common.distribute.RemoteFile;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IllegalState;
import dk.netarkivet.common.exceptions.UnknownID;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.utils.SystemUtils;

/**
 * The server for the ChecksumFileApplication. 
 * Used for communication between the ArcRepository and the checksum archive.
 */
public class ChecksumFileServer extends ChecksumArchiveServer {
    
    /**
     * The logger used by this class.
     */
    private static final Log log 
            = LogFactory.getLog(ChecksumFileServer.class);
    
    /**
     * The instance of this server.
     */
    protected static ChecksumFileServer instance;
    
    /**
     * The archive which contain the actual data.
     */
    protected ChecksumArchive cs;
        
    /**
     * Returns the unique instance of this class.
     * 
     * The server creates an instance of the checksum it creates access to and
     * starts to listen to a JMS messages on the incoming JMS queue.
     * <p/>
     * 
     * Should this do the heart beats to a monitor?
     * This would be quite odd, since Checksum does not use a monitor.
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

        // initialise the JMSConnection.
        jmsCon = JMSConnectionFactory.getInstance();

        // initialise the channel.
        theCR = Channels.getTheCR();

        // Start listening to the channels.
        jmsCon.setListener(theCR, this);

        // create the application identifier
        checksumAppId = createAppId();

        // get the instance of the checksum.
        cs = FileChecksumArchive.getInstance();

        // log that this instance has successfully been invoked.
        log.info("ChecksumFileServer '" + checksumAppId + "' initialised.");
    }
    
    /**
     * Method for closing the instance.
     */
    public void close() {
        log.info("ChecksumFileServer '" + checksumAppId + "' closing down.");
        cleanup();
        if (jmsCon != null) {
            jmsCon.removeListener(theCR, this);
            jmsCon = null;
        }
        log.info("ChecksumFileServer '" + checksumAppId + "' closed down.");
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
            String applicationInstanceId = Settings
                    .get(CommonSettings.APPLICATION_INSTANCE_ID);
            if (!applicationInstanceId.isEmpty()) {
                id += "_" + applicationInstanceId;
            }
        } catch (UnknownID e) {
            // Ignore the fact, that there is no APPLICATION_INSTANCE_ID in
            // settings
            log.warn("No setting APPLICATION_INSTANCE_ID found in settings: "
                    + e);
        }
        return id;
    }    

    /**
     * The method for uploading arc files.
     * 
     * @param msg The upload message, containing the file to upload.
     */
    public void visit(UploadMessage msg) {
        log.debug("Receving upload message: " + msg.toString());
        try {
            try {
                // retrieve the data file.
                RemoteFile uploadFile = msg.getRemoteFile();
                
                // upload the file to the checksum instance.
                cs.upload(uploadFile, msg.getArcfileName());
                
                // cleanup after use of file.
                uploadFile.cleanup();
            } catch (Throwable e) {
                log.warn("Cannot process upload message '" + msg + "'", e);
                msg.setNotOk(e);
            }
            // check if enough space and thus whether to remove listener.
            finally {
                if (!cs.hasEnoughSpace()) {
                    log.warn("Not enough space any more.");
                    jmsCon.removeListener(theCR, this);
                }
            }
        } catch (Throwable e) {
            log.warn("Cannnot remove listener after upload message '" + msg
                    + "'", e);
        } finally {
            log.info(msg.toString());
            jmsCon.reply(msg);
        }
    }

    /**
     * Method for correcting an entry in the archive.
     * This method starts by removing the record, and then uploading it again 
     * to the checksum archive.
     * 
     * @param msg The message containing the correct instance of the file to 
     * correct. 
     */
    public void visit(CorrectMessage msg) {
        log.debug("Receiving correct message: " + msg.toString());
        try {
            String filename = msg.getArcfileName();
            String currentCs = cs.getChecksum(filename);
            String incorrectCs = msg.getIncorrectChecksum();

            // check that the current checksum is incorrect as supposed.
            if(!currentCs.equals(incorrectCs)) {
                String errMsg = "Wrong checksum for the entry for file '" + filename
                + "' has the checksum '" + currentCs + "', though it "
                + "was supposed to have the checksum '" + incorrectCs + "'.";
                log.error(errMsg);
                throw new IllegalState(errMsg);
            }

            // retrieve the data as a file.
            File correctFile = File.createTempFile("correct", filename, 
                    FileUtils.TMPDIR);
            msg.getData(correctFile);
            
            // put the file into the archive.
            cs.correct(filename, correctFile);
        } catch (Throwable e) {
            // Handle errors.
            log.warn("Cannot handle CorrectMessage: '" + msg + "'", e);
            msg.setNotOk(e);
        } finally {
            // log and reply at the end.
            log.info(msg.toString());
            jmsCon.reply(msg);
        }
    }

    /**
     * Method for retrieving the checksum of a record.
     * 
     * @param msg The GetChecksumMessage which contains the name of the record
     * to have its checksum retrieved.
     * @throws ArgumentNotValid If the message is null.
     */
    public void visit(GetChecksumMessage msg) {
        ArgumentNotValid.checkNotNull(msg, "GetChecksumMessage msg");
        
        log.debug("Recieving get checksum message: " + msg.toString());
        try {
            // get the name of the arc file
            String filename = msg.getArcfileName();
            // get the checksum of the arc file
            String checksum = cs.getChecksum(filename);

            // Check if the checksum was found. If not throw exception.
            if(checksum == null || checksum.isEmpty()) {
                // The error is logged, when the exception is catched.
                throw new IllegalState("Cannot fetch checksum of an entry, "
                        + "which is not within the archive.");
            } 
            
            // send the checksum of the arc file.
            msg.setChecksum(checksum);
        } catch (Throwable e) {
            // Handle errors (if the file cannot be found).
            log.warn("Cannot handle '" + msg.getClass().getName() 
                    + "' containing the message: " + msg, e);
            msg.setNotOk(e);
        } finally {
            // log the message and reply.
            log.info(msg.toString());
            jmsCon.reply(msg);
        }
    }
    
    /**
     * Method for retrieving all the filenames within the archive.
     * 
     * @param msg The GetAllFilenamesMessage.
     */
    public void visit(GetAllFilenamesMessage msg) {
        log.debug("Receving get all filenames message: " + msg.toString());

        try {
            // get all the file names
            msg.setFile(cs.getAllFilenames());
        } catch (Throwable e) {
            log.warn("Cannot retrieve the filenames to reply on the " 
                    + msg.getClass().getName() + " : " + msg, e);
            msg.setNotOk(e);
        } finally {
            // log the message and reply.
            log.info(msg.toString());
            jmsCon.reply(msg);
        }
    }
    
    /**
     * Method for retrieving a map containing all the checksums and their 
     * corresponding filenames within the archive.
     * 
     * @param msg The GetAllChecksumMessage.
     */
    public void visit(GetAllChecksumsMessage msg) {
        log.debug("Receiving get all checksum message: " + msg.toString());

        try {
            msg.setResultingFile(cs.getArchiveAsFile());
        } catch (Throwable e) {
            log.warn("Cannot retrieve all the checksums.", e);
            msg.setNotOk(e);
        } finally {
            // log the message and reply
            log.info(msg.toString());
            jmsCon.reply(msg);
        }
    }
}
