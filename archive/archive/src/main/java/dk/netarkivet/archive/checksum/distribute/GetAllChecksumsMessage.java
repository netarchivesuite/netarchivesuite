/* File:    $Id$
 * Version: $Revision$
 * Date:    $Date$
 * Author:  $Author$
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
import dk.netarkivet.common.distribute.RemoteFileFactory;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;

/**
 * The GetChecksumMessage has the purpose to retrieve the checksum of all 
 * the files. The output is in the form of a file corresponding to the reply 
 * file of a ChecksumJob.
 * 
 * This is checksum replica alternative to sending a ChecksumBatchJob.
 */
public class GetAllChecksumsMessage extends ArchiveMessage {
    /** The file containing the output.*/
    private RemoteFile rf;
    /** The id for the replica where this message should be sent.*/
    private String replicaId;

    /**
     * Constructor.
     * 
     * @param to Where this message is headed.
     * @param replyTo Where the reply on this message is sent.
     * @param repId The replica where the job involved in this message is
     * to be performed.
     */
    public GetAllChecksumsMessage(ChannelID to, ChannelID replyTo, 
            String repId) {
        super(to, replyTo);
        this.replicaId = repId;
    }
    
    /**
     * Method for setting the resulting file. This file will be retrieved from 
     * the caller of this message. This should be a movable instance since the
     * temporary file should be removed after is has been retrieved.
     * 
     * TODO cleanup if remoteFile already has been set.
     * 
     * @param file The file with the checksum message.
     * @throws ArgumentNotValid If <b>file</b> is null. 
     */
    public void setFile(File file) throws ArgumentNotValid {
        ArgumentNotValid.checkNotNull(file, "File file");

        rf = RemoteFileFactory.getMovefileInstance(file);
    }
    
    /**
     * Method for retrieving the resulting file. This method can only be called 
     * once, since the remoteFile is cleaned up and set to null.
     * 
     * @param toFile The file for the remotely retrieved content.
     * @throws IOFailure If the data in the remoteFile already has be retrieved.
     * @throws ArgumentNotValid If <b>toFile</b> is null.
     */
    public void getData(File toFile) throws IOFailure, ArgumentNotValid {
        ArgumentNotValid.checkNotNull(toFile, "File toFile");
        if (rf == null) {
            throw new IOFailure("The remote file is not valid. "
                    + "Data cannot be retrieved.");
        }
        rf.copyTo(toFile);
        try {
            rf.cleanup();
        } catch (IOFailure e) {
            // Just log errors on deleting. They are fairly harmless.
            // Can't make Logger a field, as this class is Serializable
            LogFactory.getLog(getClass().getName()).warn(
                    "Could not delete remote file " + rf.getName());
        }
        rf = null;
    }
    
    /**
     * Method for retrieving the id for the replica where this message should 
     * be sent.
     * 
     * @return The id for the replica.
     */
    public String getReplicaId() {
        return replicaId;
    }

    /**
     * Generate String representation of this object.
     * 
     * @return String representation of this object
     */
    public String toString() {
        return super.toString() + " replicaid: " + replicaId;
    }

    /**
     * Accept visitation. 
     * 
     * @param v The ArchiveMessageVisitor which accepts this message.
     */
    public void accept(ArchiveMessageVisitor v) {
        v.visit(this);
    }
}
