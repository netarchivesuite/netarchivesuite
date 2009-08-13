/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
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
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301 
 *  USA
 */
package dk.netarkivet.archive.checksum.distribute;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.archive.bitarchive.distribute.UploadMessage;
import dk.netarkivet.archive.checksum.FileChecksumArchive;
import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.distribute.Channels;
import dk.netarkivet.common.distribute.JMSConnectionFactory;
import dk.netarkivet.common.exceptions.IllegalState;
import dk.netarkivet.common.exceptions.UnknownID;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.utils.SystemUtils;

public class ChecksumFileServer extends ChecksumServerAPI {
    
    /**
     * The logger used by this class.
     */
    private static final Log log 
            = LogFactory.getLog(ChecksumFileServer.class.getName());
    
    /**
     * The instance of this server.
     */
    protected static ChecksumFileServer instance;
    
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
	if(instance == null) {
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
	
	// initialise the channels.
	// TODO: decide whether to use specific channels for the Checksum.
	theCR = Channels.getTheCR(); 
	
	// Start listening to the channels.
	jmsCon.setListener(theCR, this);
	
	// TODO: Heartbeat? If so, then here.
	
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
	if(jmsCon != null) {
	    jmsCon.removeListener(theCR, this);
	    jmsCon = null;
	}
	log.info("ChecksumFileServer '" + checksumAppId + "' closed down.");
    }
    
    /**
     * Method for cleaning up, when closing this instance down.
     */
    public void cleanup() {
	// ??
	instance = null;
	cs.cleanup();
    }

    /**
     * Method for retrieving the identification of this application.
     * 
     * @return The id of this application.
     */
    @Override
    public String getAppId() {
	return checksumAppId;
    }

    /**
     * Method for creating the identification for this application.
     * 
     * @return The id of this application.
     */
    @Override
    protected String createAppId() {
	String id;
	// Create an id with the IP address of this current host
	id = SystemUtils.getLocalIP();

	// Append an underscore and APPLICATION_INSTANCE_ID from settings
	// to the id, if specified in settings.
	// If no APPLICATION_INSTANCE_ID is found do nothing.
	try {
	    String applicationInstanceId = Settings.get(
		    CommonSettings.APPLICATION_INSTANCE_ID);
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
    @Override
    public void visit(UploadMessage msg) {
	try {
	    try {
		// upload the file to the checksum instance.
		cs.upload(msg.getRemoteFile(), msg.getArcfileName());
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
     * Method for correcting a record.
     * This method starts by removing the record, and then uploading it again 
     * to the checksum archive.
     * 
     * @param msg The message containing the correct instance of the file to 
     * correct. 
     */
    @Override
    public void visit(CorrectMessage msg) {
	try {
	    // get the name of the file
	    String filename = msg.getArcfileName();
	    
	    // try to remove the record.
	    boolean success = cs.removeRecord(filename, msg.getChecksum());
	    
	    // if the record is successfully removed, then upload the corrected
	    // file. Else throw an error.
	    if(success) {
		cs.upload(msg.getRemoteFile(), filename);		
	    } else {
		throw new IllegalState("Cannot remove the record: '"
			+ filename + "'");
	    }
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
     */
    @Override
    public void visit(GetChecksumMessage msg) {
	try {
	    // get the name of the arc file
	    String filename = msg.getArcfileName();
	    // get the checksum of the arc file
	    String checksum;
	    
	    // retrieve the checksum of the files individually 
	    checksum = cs.getChecksum(filename);	

	    // send the checksum of the arc file.
	    msg.setChecksum(checksum);

	} catch (Throwable e) {
	    // Handle errors (if the file cannot be found).
	    log.warn("Cannot find arc file.", e);
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
    @Override
    public void visit(GetAllFilenamesMessage msg) {
	try {
	    // get all the file names
	    msg.setFile(cs.getAllFilenames());
	} catch (Throwable e) {
	    log.warn("Cannot retrieve the filenames.", e);
	    msg.setNotOk(e);
	} finally {
	    // log the message and reply.
	    log.info(msg.toString());
	    jmsCon.reply(msg);
	}
    }
    
    /**
     * Method for retrieving a map containing all the checksums and their 
     * corresponding filenames within the archive
     * 
     * @param msg The GetAllChecksumMessage.
     */
    @Override
    public void visit(GetAllChecksumMessage msg) {
	try {
	    msg.setFile(cs.getArchiveAsFile());
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
