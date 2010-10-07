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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
    /** The list of arguments for the batchjob.*/
    private List<String> args;
    /** The ID for the batch process.*/
    private String batchID;

    /**
     * Creates a BatchMessage object which can be used to initiate a batch
     * job.  This is used by BitarchiveMonitorServer to create the message
     * sent to the bitarchive machines.
     * 
     * Note: The id for the batchjob is the empty string, which removes the 
     * possibility of terminating the batchjob remotely while it is running.
     *
     * @param to The channel to which the batch message is to be sent
     * @param job  The batch job to be executed
     * @param replicaId id of this replica.
     */
    public BatchMessage(ChannelID to, FileBatchJob job, String replicaId) {
        this(to, Channels.getError(), job, replicaId, "", new String[]{});
    }
    
    /**
     * Creates a BatchMessage object which can be used to initiate a batch
     * job. 
     * 
     * Note: The id for the batchjob is the empty string, which removes the 
     * possibility of terminating the batchjob remotely while it is running.
     *
     * @param to The channel to which the batch message is to be sent
     * @param replyTo The channel whereto the reply to this message is sent.
     * @param job  The batch job to be executed
     * @param replicaId id of this replica.
     * @param arguments The arguments for initialising the batchjob.
     * @throws ArgumentNotValid If the job is null, or the replica is either 
     * null or the empty string.
     */
    public BatchMessage(ChannelID to, ChannelID replyTo, FileBatchJob job, 
            String replicaId, String ... arguments) {
        this(to, replyTo, job, replicaId, "", arguments);
    }

    /**
     * Creates a BatchMessage object which can be used to initiate a batch
     * job.
     *
     * @param to The channel to which the batch message is to be sent
     * @param replyTo The channel whereto the reply to this message is sent.
     * @param job  The batch job to be executed
     * @param replicaId id of this replica.
     * @param batchId The id for the process which runs the batchjob.
     * @param arguments The arguments for initialising the batchjob. This is 
     * allowed to be null.
     * @throws ArgumentNotValid If the job is null, or the replica is either 
     * null or the empty string.
     */
    public BatchMessage(ChannelID to, ChannelID replyTo, FileBatchJob job, 
            String replicaId, String batchId, String ... arguments) 
            throws ArgumentNotValid {
        super(to, replyTo);
        ArgumentNotValid.checkNotNull(job, "job");
        ArgumentNotValid.checkNotNullOrEmpty(replicaId, "String replicaId");
        ArgumentNotValid.checkNotNull(batchId, "String batchId");
        this.job = job;
        this.replicaId = replicaId;
        this.batchID = batchId;
        this.args = new ArrayList<String>();
        if(arguments != null && !(arguments.length == 0)) {
            Collections.addAll(this.args, arguments);
        }
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
     * Returns the arguments for the batchjob.
     * @return The arguments for the batchjob.
     */
    public List<String> getArgs() {
        return args;
    }

    /**
     * Returns the predefined ID for the batch process. If no Id is available, 
     * then the message id is returned.
     * @return The ID for the batch process, or the message id, if no specific
     * batch id has been declared.
     */
    public String getBatchID() {
        // if the batchId is empty, then use the message id as process id.
        if(batchID.isEmpty()) {
            return super.getID();
        }
        return batchID;
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
        + ", on filename-pattern: " + job.getFilenamePattern() 
        + ", for replica: " + replicaId;
    }
}
