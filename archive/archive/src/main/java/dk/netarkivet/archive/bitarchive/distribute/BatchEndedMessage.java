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
package dk.netarkivet.archive.bitarchive.distribute;

import java.io.File;
import java.util.Collection;
import java.util.List;

import dk.netarkivet.archive.distribute.ArchiveMessage;
import dk.netarkivet.archive.distribute.ArchiveMessageVisitor;
import dk.netarkivet.common.distribute.ChannelID;
import dk.netarkivet.common.distribute.Channels;
import dk.netarkivet.common.distribute.RemoteFile;
import dk.netarkivet.common.distribute.arcrepository.BatchStatus;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.utils.batch.FileBatchJob;

/**
 * An instance of this class is sent by a bitarchive machine (Bitarchive class)
 * to a BitarchiveMonitorServer to indicate that that single machine has
 * finished processing a batch job.
 */
public class BatchEndedMessage extends ArchiveMessage {
    /** The identifier for the bitarchive application, that performed the 
     * batch-job. */
    private String baApplicationId;
    /** The identifier for the message, that initiated the batch-job. */
    private String originatingBatchMsgId;
    /** Number of files processed by the batch-job. */
    private int noOfFilesProcessed;
    /** Collection of files that the batch-job could not process. */
    private Collection<File> filesFailed;
    /** The result of the batchJob. */
    private RemoteFile rf;
    /** List of exceptions that occurred during processing. */
    private List<FileBatchJob.ExceptionOccurrence> exceptions;
     


    /**
     * Message to signal from a BitarchiveServer to the BitarchiveMonitorServer
     *  that the Bit Archive Application identified by
     * BA_ApplicationId has completed its part of the batch job.
     *
     * Holds status information: list of files processed and a list of ARC
     * files (file names)
     * on which the batch job failed.
     *
     * @param to the channel to which this message is to be sent (must be a
     * BAMON channel)
     * @param baAppId Identifier for the machine sending this message,
     * usually containing the IP address and http port number
     * @param originatingBatchMsgId the Id field from the original batch message
     * @param rf he remote file reference containing the output of the batch
     * job (may be null if no output is generated).
     * @throws ArgumentNotValid If the BA_ApplicationId or the 
     * originatingBatchMsgId are null or empty, or if the channel 'to' is null.
     */
    public BatchEndedMessage(ChannelID to, String baAppId,
            String originatingBatchMsgId, RemoteFile rf) 
            throws ArgumentNotValid {
        super(to, Channels.getError());
        ArgumentNotValid.checkNotNull(to, "ChannelID to");
        ArgumentNotValid.checkNotNullOrEmpty(baAppId, "String baAppId");
        ArgumentNotValid.checkNotNullOrEmpty(originatingBatchMsgId,
                "String originatingBatchMsgId");

        this.baApplicationId = baAppId;
        this.originatingBatchMsgId = originatingBatchMsgId;
        this.rf = rf;
    }

    /**
     * Message to signal from a BitarchiveServer to the BitarchiveMonitorServer
     *  that the Bit Archive Application identified by
     * BA_ApplicationId has completed its part of the batch job.
     *
     * Holds status information: list of files processed and a list of ARC
     * files (file names)
     * on which the batch job failed.
     *
     * @param to the channel to which this message is to be sent (must be a
     * BAMON channel)
     * @param originatingBatchMsgId the Id field from the original batch message
     * @param status The object containing status info.
     */
    public BatchEndedMessage(ChannelID to, String originatingBatchMsgId,
                             BatchStatus status) {
        super(to, Channels.getError());
        ArgumentNotValid.checkNotNull(to, "to");
        ArgumentNotValid.checkNotNullOrEmpty(originatingBatchMsgId,
                "String originatingBatchMsgId");
        ArgumentNotValid.checkNotNull(status, "BatchStatus status");

        this.originatingBatchMsgId = originatingBatchMsgId;
        this.baApplicationId = status.getBitArchiveAppId();
        this.rf = status.getResultFile();
        this.noOfFilesProcessed = status.getNoOfFilesProcessed();
        this.filesFailed = status.getFilesFailed();
        this.exceptions = status.getExceptions();
    }

    /**
     * Returns id information for the bitarchive which generated this message.
     * @return the id information
     */
    public String getBitarchiveID() {
        return baApplicationId;
    }

    /**
     * Returns the Id of the BatchMessage which originated this message.
     * @return the Id
     */
    public String getOriginatingBatchMsgID() {
        return originatingBatchMsgId;
    }

    /**
     * Returns the number of files processed by this batch job on this machine.
     * @return the number of files processed
     */
    public int getNoOfFilesProcessed() {
        return noOfFilesProcessed;
    }

    /**
     * Returns a collection of the names of files on which this batch job.
     * failed
     * 
     * @return a Collection of strings with the file names
     */
    public Collection<File> getFilesFailed() {
        return filesFailed;
    }

    /** Set the number of files processed in batch job.
     * @param number The number of processed files
     */
    public void setNoOfFilesProcessed(int number) {
        noOfFilesProcessed = number;
    }

    /** Set the files that failed in batch job.
      * @param files The collection of files that failed
     */
    public void setFilesFailed(Collection<File> files) {
        filesFailed = files;
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
     * Returns the remote file object containing the output of this job.
     * @return  the remote file object. May be null if this job generates no
     * output
     */
    public RemoteFile getRemoteFile() {
        return rf;
    }

    /** Human readable version of this object.
     * @return A human readable version of this object
     */
    public String toString(){
        return "\nBatchEndedMessage for batch job " + originatingBatchMsgId
                + "\nFrom Bitarchive " + baApplicationId
                + "\nFilesProcessed = " + noOfFilesProcessed
                + "\n" + super.toString();
    }
    
    /** Returns the list of the exceptions that occurred during processing.
     *
     * @return List of exceptions and occurrence information.
     */
    public List<FileBatchJob.ExceptionOccurrence> getExceptions() {
        return exceptions;
    }
    
}
