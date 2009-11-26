/* File:       $Id$
 * Revision:   $Revision$
 * Author:     $Author$
 * Date:       $Date$
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

import org.apache.commons.logging.LogFactory;

import dk.netarkivet.archive.distribute.ArchiveMessage;
import dk.netarkivet.archive.distribute.ArchiveMessageVisitor;
import dk.netarkivet.common.distribute.ChannelID;
import dk.netarkivet.common.distribute.RemoteFile;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;

/**
 * The message to correct a bad entry in an archive.
 * <li>In a bitarchive it should replace a corrupted file. </li>
 * <li>In a checksum archive it should replace checksum entry of the file.</li>
 * <br>
 * The message contains the checksum of the 'bad' entry in the archive, which is
 * only corrected if it actually has this 'bad checksum'.
 */
public class CorrectMessage extends ArchiveMessage {
    /** The file to replace the current bad entry. */
    private RemoteFile theRemoteFile;
    /** The name of the arc-file. */
    private String arcFilename;
    /** The 'bad' checksum. */
    private String theIncorrectChecksum;
    /** The replica, where this message should be sent.*/
    private String replicaId;
    /** The credentials to allow the correction of the archive entry.*/
    private String credentials;

    /**
     * Constructor.
     * Initializes the variables.
     * 
     * @param to Where the message should be sent.
     * @param replyTo Who is sending this message.
     * @param badChecksum The checksum of the 'bad' entry.
     * @param file The file to replace the 'bad' entry.
     * @param repId The identification of the replica, where this message 
     * should be sent.
     * @param cred The credentials to allow the correction of an entry.
     */
    public CorrectMessage(ChannelID to, ChannelID replyTo, String badChecksum, 
            RemoteFile file, String repId, String cred) {
        super(to, replyTo);
        ArgumentNotValid.checkNotNull(file, "RemoteFile file");
        ArgumentNotValid.checkNotNullOrEmpty(badChecksum, "String checksum");
        this.theIncorrectChecksum = badChecksum;
        this.theRemoteFile = file;
        this.arcFilename = file.getName();
        this.replicaId = repId;
        this.credentials = cred;
    }

    /**
     * Retrieve name of the uploaded file.
     * @return current value of arcfileName
     */
    public String getArcfileName() {
        return arcFilename;
    }
    
    /**
     * Retrieves the content of the remoteFile and writes it into the local 
     * file.
     * Note: This is transferred through a remote file handle, and then the
     * handle is invalidated. This method may only be called once.
     * 
     * @param toFile where to write the content
     * @throws IOFailure on error reading the remote file
     * or writing the local file
     * @throws ArgumentNotValid If <b>toFile</b> is null.
     */
    public void getData(File toFile) throws IOFailure, ArgumentNotValid {
        ArgumentNotValid.checkNotNull(toFile, "toFile");
        
        // ensure that the local file exists.
        if (theRemoteFile == null) {
            throw new IOFailure("No remoteFile in this message.");
        }
        // retrieve the data.
        theRemoteFile.copyTo(toFile);
        try {
            // cleanup afterwards.
            theRemoteFile.cleanup();
        } catch (IOFailure e) {
            //Just log errors on deleting. They are fairly harmless.
            // Can't make Logger a field, as this class is Serializable
            LogFactory.getLog(getClass().getName()).warn(
                    "Could not cleanup remote file " + theRemoteFile.getName());
        }
        theRemoteFile = null;
    }
    
    /**
     * Method for retrieving the 'bad' checksum which should correspond to
     * the checksum of the current entry on this file in the archive.
     * 
     * @return The checksum for the archive entry.
     */
    public String getIncorrectChecksum() {
        return theIncorrectChecksum;
    }
    
    /**
     * Method for retrieving the replica, where this message should be sent.
     * 
     * @return The id of the replica where this message should be sent.
     */
    public String getReplicaId() {
        return replicaId;
    }
    
    /**
     * The credentials to allow correction of an entry in the archive.
     * 
     * @return The credentials.
     */
    public String getCredentials() {
        return credentials;
    }

    /**
     * Accept this message.
     *
     * @param v The message visitor accepting this message.
     */
    public void accept(ArchiveMessageVisitor v) {
        v.visit(this);
    }

    /**
     * Generate String representation of this object.
     * @return String representation of this object
     */
    public String toString() {
        return super.toString() + " Arcfile: " + arcFilename;
    }
}
