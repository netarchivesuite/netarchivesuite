/* File:             $Id$
 * Revision:         $Revision$
 * Author:           $Author$
 * Date:             $Date$
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

import dk.netarkivet.archive.distribute.ArchiveMessage;
import dk.netarkivet.archive.distribute.ArchiveMessageVisitor;
import dk.netarkivet.common.distribute.ChannelID;
import dk.netarkivet.common.distribute.Channels;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.utils.batch.FileBatchJob;


/**
 * Container for batch jobs. Messages of this class should be sent to a
 * BAMON queue from where they are collected by a BitarchiveMonitorServer.
 * The BitarchiveMonitorServer also creates instances of this class and sends
 * them to the individual bitarchive machines.
 *
 * The response to this message comes in the form of a BatchReplyMessage
 * placed on the senders queue.
 */
public class BatchMessage extends ArchiveMessage {

    /** The batch job, this message is sent to initiate. */
    private FileBatchJob job;
    /** The id of this replica. */
    private String replicaId;

    /**
     * Creates a BatchMessage object which can be used to initiate a batch
     * job.  This is used by BitarchiveMonitorServer to create the message
     * sent to the bitarchive machines.
     *
     * @param to The channel to which the batch message is to be sent
     * @param job  The batch job to be executed
     * @param replicaId id of this replica.
     */
    public BatchMessage(ChannelID to,
                        FileBatchJob job, String replicaId) {
        this(to, Channels.getError(), job, replicaId);
    }

    /**
     * Creates a BatchMessage object which can be used to initiate a batch
     * job.
     *
     * @param to The channel to which the batch message is to be sent
     * @param replyTo The channel whereto the reply to this message is sent.
     * @param job  The batch job to be executed
     * @param replicaId id of this replica.
     */
    public BatchMessage(ChannelID to, ChannelID replyTo,
                        FileBatchJob job, String replicaId) {
        super(to, replyTo);
        ArgumentNotValid.checkNotNull(job, "job");
        this.job = job;
        this.replicaId = replicaId;
    }

  /**
   * Retrieves batch job.
   * @return Batch job
   */
    public FileBatchJob getJob() {
        return job;
    }

    /**
     * Returns the replica id.
     * @return the replica id
     */
    public String getReplicaId() {
        return replicaId;
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
     * Retrieval of a string representation of this object.
     * 
     * @return A string representation of the instance of class.
     * @see dk.netarkivet.common.distribute.NetarkivetMessage#toString()
     */
    public String toString() {
        return super.toString() + " Job: " + job.getClass().getName() 
                + ", on files: " + job.getFilenamePattern() + ", for replica: "
                + replicaId;
    }

}
