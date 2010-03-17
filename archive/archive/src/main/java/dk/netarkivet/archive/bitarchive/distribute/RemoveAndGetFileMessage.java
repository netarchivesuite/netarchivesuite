/* File:     $Id$
 * Revision: $Revision$
 * Author:   $Author$
 * Date:     $Date$
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
package dk.netarkivet.archive.bitarchive.distribute;

import java.io.File;

import org.apache.commons.logging.LogFactory;

import dk.netarkivet.archive.distribute.ArchiveMessage;
import dk.netarkivet.archive.distribute.ArchiveMessageVisitor;
import dk.netarkivet.common.distribute.ChannelID;
import dk.netarkivet.common.distribute.RemoteFile;
import dk.netarkivet.common.distribute.RemoteFileFactory;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.FileUtils;

/**
 * Message requesting a file to be removed and returned from a bitarchive.
 * Messages is forwarded through arcrepository, but reponds directly to sender.
 */
public class RemoveAndGetFileMessage extends ArchiveMessage {
    /**  The file to retrieve. */
    private String fileName;
    /** The actual data. */
    private RemoteFile remoteFile;
    /** This replica id. */
    private String replicaId;

    /** The checksum of the file to remove. */
    private String checksum;

    /** The bitarchive credentials. */
    private String credentials;

    /**
     * Constructor.
     * @param to Where to send the message.
     * @param replyTo Where the reply of the message should be sent.
     * @param fileName The name of the file to remove and retrieve.
     * @param replicaId The id of the replica to sent it to.
     * @param checksum The checksum of the bad file to remove and retrieve.
     * @param credentials The right credentials for the operation.
     */
    public RemoveAndGetFileMessage(ChannelID to, ChannelID replyTo, 
            String fileName, String replicaId, String checksum, 
            String credentials) {
        super(to, replyTo);
        this.fileName = fileName;
        this.replicaId = replicaId;
        this.checksum = checksum;
        this.credentials = credentials;
    }

    /**
     * Set the file this message should remove and return.
     * Note: This will make a remote file handle for the file.
     * @param data Content of the file to retrieve
     * @throws ArgumentNotValid If the data file is null.
     */
    public void setFile(File data) throws ArgumentNotValid {
        ArgumentNotValid.checkNotNull(data, "File data");
        
        remoteFile = RemoteFileFactory.getCopyfileInstance(data);
    }

    /**
     * Writes the the content of the retrieved file into a local file.
     * Note: This is transferred through a remote file handle, and then the
     * handle is invalidated. This method may only be called once.
     * @return file content
     * @throws IOFailure on error reading the file
     */
    public File getData() throws IOFailure {
        // ensure that the remote file exists.
        if (remoteFile == null) {
            throw new IOFailure("No file present in message");
        }
        // Retrieve the remote file and put it into a temporary file.
        File file = new File(FileUtils.getTempDir(), remoteFile.getName());
            remoteFile.copyTo(file);
        try {
            remoteFile.cleanup();
        } catch (IOFailure e) {
            //Just log errors on deleting. They are fairly harmless.
            //Note: Do not make a field of this logger, or if you do, remember
            //to make it transient and reinitialise it in readObject
            LogFactory.getLog(getClass().getName())
                .warn("Could not delete"
                        + " remote file "
                        + remoteFile.getName());
        }
        return file;
    }
    
    /**
     * Returns the remote file.
     * 
     * @return The remote file.
     */
    public RemoteFile getRemoteFile() {
        return remoteFile;
    }

    /**
     * Retrieve the replica id.
     * @return replica id
     */
    public String getReplicaId() {
        return replicaId;
    }

    /**
     * Get name of the file to retrieve.
     * @return file name
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * Get the checksum of the file to remove.
     * @return the checksum of the file to remove
     */
    public String getCheckSum() {
        return checksum;
    }

    /**
     * Get the credentials for the remove operation.
     * @return the credentials for the remove operation
     */
    public String getCredentials() {
        return credentials;
    }

    /**
     * Clear content buffer.
     */
    public void clearBuffer() {
        remoteFile = null;
    }

    /**
     * Should be implemented as a part of the visitor pattern. fx.: public void
     * accept(ArchiveMessageVisitor v) { v.visit(this); }
     *
     * @param v A message visitor
     */
    public void accept(ArchiveMessageVisitor v) {
        v.visit(this);
    }

    /**
     * Retrieval of a string representation of this instance.
     * 
     * @return The string representation of this instance.
     * @see dk.netarkivet.common.distribute.NetarkivetMessage#toString()
     */
    public String toString() {
        return super.toString() + " file: " + fileName;
    }

}
