/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
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

import dk.netarkivet.archive.distribute.ArchiveMessage;
import dk.netarkivet.archive.distribute.ArchiveMessageVisitor;
import dk.netarkivet.common.distribute.ChannelID;
import dk.netarkivet.common.exceptions.ArgumentNotValid;

/**
 * The GetChecksumMessage has the purpose to retrieve the checksum of a 
 * specific file.
 * 
 * This is checksum replica alternative to sending a ChecksumBatchJob, with
 * a filename limitation.
 */
public class GetChecksumMessage extends ArchiveMessage {
    /** The name of the arc file to retrieve the checksum from.*/
    private String arcFilename;
    /** The resulting checksum for the arcFile.*/
    private String checksum;
    /** The id of the replica where the checksum should be retrieved.*/
    private String replicaId;
    /** Variable to tell whether this is a reply.*/
    private boolean isReply = false;

    /**
     * Constructor.
     *  
     * @param to Where this message should be sent.
     * @param replyTo Where the reply for this message should be sent.
     * @param filename The name of the file.
     * @param repId The id of the replica where the message is to be sent.
     */
    public GetChecksumMessage(ChannelID to, ChannelID replyTo, 
            String filename, String repId) {
        super(to, replyTo);
        // validate arguments (channels are validated in 'super').
        ArgumentNotValid.checkNotNullOrEmpty(filename, "String filename");
        ArgumentNotValid.checkNotNullOrEmpty(repId, "String repId");

        this.arcFilename = filename;
        this.replicaId = repId;
    }

    /**
     * Retrieve name of the uploaded file.
     * 
     * @return current value of arcfileName
     */
    public String getArcfileName() {
        return arcFilename;
    }

    /**
     * Retrieves the replica id.
     * 
     * @return The replica id.
     */
    public String getReplicaId() {
        return replicaId;
    }

    /**
     * Retrieves the checksum. This method is intended for the reply.
     * If this checksum has not been sent, then the value is null.
     * 
     * @return The retrieved checksum, or null if the entry was not found in 
     * the archive. 
     */
    public String getChecksum() {
        return checksum;
    }
    
    /**
     * Retrieves the variable for telling whether this it currently is a reply
     * to this message or not.
     * 
     * @return Whether this is a reply or not.
     */
    public boolean getIsReply() {
        return isReply;
    }
    
    /**
     * Set that this is a reply. This should be set when there is replied to 
     * this message. 
     * <b>isReply = true</b>.
     */
    public void setIsReply() {
        isReply = true;
    }

    /**
     * Method for returning the result of the checksum.
     * 
     * @param cs The checksum.
     * @throws ArgumentNotValid If the checksum which is attempted to be set
     * is either null or an empty string.
     */
    public void setChecksum(String cs) throws ArgumentNotValid {
        ArgumentNotValid.checkNotNullOrEmpty(cs, "String cs");

        checksum = cs;
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
     * 
     * @return String representation of this object
     */
    public String toString() {
        return super.toString() + " Arcfiles: " + arcFilename 
        + ", ReplicaId: " + replicaId + ", Checksum: " + checksum;
    }
}
