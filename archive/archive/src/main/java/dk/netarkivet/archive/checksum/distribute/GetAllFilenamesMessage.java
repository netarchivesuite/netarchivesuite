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

import org.apache.commons.logging.LogFactory;

import dk.netarkivet.archive.distribute.ArchiveMessage;
import dk.netarkivet.archive.distribute.ArchiveMessageVisitor;
import dk.netarkivet.common.distribute.ChannelID;
import dk.netarkivet.common.distribute.RemoteFile;
import dk.netarkivet.common.distribute.RemoteFileFactory;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;

/**
 * The GetAllFilenamesMessage is sent to retrieve all the filenames in a 
 * specific replica. The result is a file in the same format as a FilelistJob.
 */
public class GetAllFilenamesMessage extends ArchiveMessage {
    /** 
     * The file with the current content, which will be retrieved from the 
     * sender of this message.
     */
    private RemoteFile remoteFile;
    /** The id for the replica where this message should be sent.*/
    private String replicaId;

    /**
     * Constructor.
     * 
     * @param to The channel the message is sent to.
     * @param replyTo The channel the reply is sent to.
     */
    public GetAllFilenamesMessage(ChannelID to, ChannelID replyTo, 
            String repId) {
        super(to, replyTo);

        this.replicaId = repId;
    }
    
    /**
     * Method for setting the resulting file. This file will be retrieved from 
     * the caller of this message.
     * 
     * @param file The file with the checksum message.
     */
    public void setFile(File file) {
        ArgumentNotValid.checkNotNull(file, "File file");

        remoteFile = RemoteFileFactory.getMovefileInstance(file);
    }
    
    /**
     * Method for retrieving the resulting file.
     * 
     * @param toFile The file for the remotely retrieved content.
     */
    public void getData(File toFile) {
        ArgumentNotValid.checkNotNull(toFile, "File toFile");
        if (remoteFile == null) {
            throw new IOFailure("No remote file has been retrieved. This "
                    + "message is either NotOK or has never been sent.");
        }
        remoteFile.copyTo(toFile);
        try {
            remoteFile.cleanup();
        } catch (IOFailure e) {
            //Just log errors on deleting. They are fairly harmless.
            // Can't make Logger a field, as this class is Serializable
            LogFactory.getLog(getClass().getName()).warn(
                    "Could not delete remote file " + remoteFile.getName());
        }
        remoteFile = null;
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
     * Accept this message.
     *
     * @param v The message visitor accepting this message.
     */
    public void accept(ArchiveMessageVisitor v) {
        v.visit(this);
    }
}
