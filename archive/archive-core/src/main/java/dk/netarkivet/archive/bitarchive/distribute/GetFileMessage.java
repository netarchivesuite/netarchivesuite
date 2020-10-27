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
package dk.netarkivet.archive.bitarchive.distribute;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.archive.distribute.ArchiveMessage;
import dk.netarkivet.archive.distribute.ArchiveMessageVisitor;
import dk.netarkivet.common.distribute.ChannelID;
import dk.netarkivet.common.distribute.RemoteFile;
import dk.netarkivet.common.distribute.RemoteFileFactory;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;

/**
 * Message requesting a file from a bitarchive. Messages is forwarded through arcrepository, but reponds directly to
 * sender.
 */
@SuppressWarnings({"serial"})
public class GetFileMessage extends ArchiveMessage {

    private static final Logger log = LoggerFactory.getLogger(GetFileMessage.class);

    /** the file to retrieve. */
    private String arcfileName;

    /** The actual data. */
    private RemoteFile remoteFile;
    /** This replica id. */
    private String replicaId;

    /**
     * Constructor for get file message.
     *
     * @param to Recipient
     * @param replyTo Original sender
     * @param arcfileName The file to retrieve
     * @param replicaId The bitarchive replica id to retrieve it from.
     */
    public GetFileMessage(ChannelID to, ChannelID replyTo, String arcfileName, String replicaId) {
        super(to, replyTo);
        this.arcfileName = arcfileName;
        this.replicaId = replicaId;
    }

    /**
     * Set the file this message should return. Note: This will make a remote file handle fopr the file.
     *
     * @param data Content of the file to retrieve
     */
    public void setFile(File data) {
        remoteFile = RemoteFileFactory.getCopyfileInstance(data);
    }

    /**
     * Writes the the content of the retrieved file into a local file. Note: This is transferred through a remote file
     * handle, and then the handle is invalidated. This method may only be called once.
     *
     * @param toFile where to write the content
     * @throws IOFailure on error reading the remote file or writing the local file
     * @throws ArgumentNotValid If the file is null.
     */
    public void getData(File toFile) throws ArgumentNotValid, IOFailure {
        ArgumentNotValid.checkNotNull(toFile, "toFile");
        if (remoteFile == null) {
            throw new IOFailure("No file present in message to get file '" + arcfileName + "'");
        }
        remoteFile.copyTo(toFile);
        try {
            remoteFile.cleanup();
        } catch (IOFailure e) {
            // Just log errors on deleting. They are fairly harmless.
            // Can't make Logger a field, as this class is Serializable
            log.warn("Could not delete remote file {}", remoteFile.getName());
        }
        remoteFile = null;
    }

    /**
     * Retrieve the replica id.
     *
     * @return replica id
     */
    public String getReplicaId() {
        return replicaId;
    }

    /**
     * Get name of the file to retrieve.
     *
     * @return file name
     */
    public String getArcfileName() {
        return arcfileName;
    }

    /**
     * Clear content buffer.
     */
    public void clearBuffer() {
        remoteFile = null;
    }

    /**
     * Should be implemented as a part of the visitor pattern. fx.: public void accept(ArchiveMessageVisitor v) {
     * v.visit(this); }
     *
     * @param v A message visitor
     */
    public void accept(ArchiveMessageVisitor v) {
        v.visit(this);
    }

    /**
     * Retrieval of a string representation of this instance.
     *
     * @return The string representing this instance.
     * @see dk.netarkivet.common.distribute.NetarkivetMessage#toString()
     */
    public String toString() {
        return super.toString() + " Arcfile: " + arcfileName;
    }

}
