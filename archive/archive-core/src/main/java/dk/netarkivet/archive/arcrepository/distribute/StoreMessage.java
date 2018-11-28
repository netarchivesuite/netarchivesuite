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
package dk.netarkivet.archive.arcrepository.distribute;

import java.io.File;

import dk.netarkivet.archive.distribute.ArchiveMessage;
import dk.netarkivet.archive.distribute.ArchiveMessageVisitor;
import dk.netarkivet.common.distribute.ChannelID;
import dk.netarkivet.common.distribute.Channels;
import dk.netarkivet.common.distribute.RemoteFile;
import dk.netarkivet.common.distribute.RemoteFileFactory;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.utils.ChecksumCalculator;

/**
 * Messages requesting store of file. This message is sent to the arc repository which distributes the file to the known
 * bitarchives, and checks the result, and then responds to the sender.
 */
@SuppressWarnings({"serial"})
public class StoreMessage extends ArchiveMessage {
    /** The actual data. */
    private RemoteFile theRemoteFile;
    private String precomputedChecksum;
    /**
     * Construct StoreMessage.
     *
     * @param replyTo Channel to reply back to
     * @param arcfile The file to store
     */
    public StoreMessage(ChannelID replyTo, File arcfile) {
        super(Channels.getTheRepos(), replyTo);
        ArgumentNotValid.checkNotNull(arcfile, "arcfile");
        theRemoteFile = RemoteFileFactory.getDistributefileInstance(arcfile);
        precomputedChecksum = ChecksumCalculator.calculateMd5(arcfile);
    }

    /**
     * Retrieve name of the stored file.
     *
     * @return current value of arcfileName
     */
    public String getArcfileName() {
        return theRemoteFile.getName();
    }

    public String getPrecomputedChecksum() {
        return precomputedChecksum;
    }
    
    /**
     * Get method for field theRemoteFile.
     *
     * @return Current value of theRemoteFile
     */
    public RemoteFile getRemoteFile() {
        return theRemoteFile;
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
     * Generate String representation of this object.
     *
     * @return String representation of this object
     */
    public String toString() {
        return super.toString() + " Arcfile: " + getArcfileName() + ", precomputed checksum: " 
        		+ precomputedChecksum;
    }

}
