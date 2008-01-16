/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2007 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package dk.netarkivet.archive.bitarchive.distribute;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;

import dk.netarkivet.archive.distribute.ArchiveMessage;
import dk.netarkivet.archive.distribute.ArchiveMessageVisitor;
import dk.netarkivet.common.distribute.ChannelID;
import dk.netarkivet.common.distribute.RemoteFile;
import dk.netarkivet.common.exceptions.ArgumentNotValid;

/**
 * Message class used by the bit archive monitor to notify the ArcRepository
 * of a completed batch job.
 *
 */
public class BatchReplyMessage extends ArchiveMessage {
    /** Number of files processed by the BatchJob. */
    private int noOfFilesProcessed;
    /** Set of files that the BatchJob could not process. */
    private HashSet<File> filesFailed;
    /** The result of the BatchJob. */
    private RemoteFile resultFile;
    /** prefix to identify this message type.*/
    private static final String IDPREFIX = "BatchReply";

    /**
     * Message to signal from BitarchiveMonitorServer that the batch job
     * identified by originatingBatchMsgId
     * is completed.
     * Holds status information: list of files processed and a list of files
     * on which the batch job failed
     *
     * @param to The queue to which this message is to be sent. This will
     * normally be the ARCREPOS queue
     * @param replyTo
     * @param originatingBatchMsgId The Id of the BathMessage which gave
     * rise to this reply
     * @param filesProcessed  the total number of file processed in this
     * batch job
     * @param filesFailed a Collection<String> of the names of files on
     * which this batch job failed
     * @param resultFile The RemoteFile containing the output from
     * the batch job, or null if an error occurred that prevented
     * the creation of the file.
     * @throws ArgumentNotValid if the input parameters are not meaningful
     */
    public BatchReplyMessage(ChannelID to,
                             ChannelID replyTo,
                             String originatingBatchMsgId,
                             int filesProcessed,
                             Collection<File> filesFailed,
                             RemoteFile resultFile)
            throws ArgumentNotValid {
        // replyTo must be set here because it is used by AdminData to work
        // out which bitarchive the batch job operated on
        super(to, replyTo, IDPREFIX);
        ArgumentNotValid.checkNotNullOrEmpty(originatingBatchMsgId,
                "originatingBatchMsgId");
        ArgumentNotValid.checkTrue(filesProcessed >= 0,
                "filesProcessed should not be less than zero");

        this.replyOfId = originatingBatchMsgId;
        this.noOfFilesProcessed = filesProcessed;
        if (filesFailed != null) {
            this.filesFailed = new HashSet<File>(filesFailed);
        } else {
            this.filesFailed = new HashSet<File>();
        }
        this.resultFile = resultFile;
    }

    /**
     * Returns the total number of files processed by this batch job.
     * @return the number of files
     */
    public int getNoOfFilesProcessed() {
        return noOfFilesProcessed;
    }

    /**
     * returns a Collection<File> of the names of files on which this.
     * batch job failed (may be null)
     * @return the Collection of file names
     */
    public Collection<File> getFilesFailed() {
        return filesFailed;
    }

    /**
     * Returns the RemoteFile that contains the output of this batchjob. May
     * be null if the message is not ok.
     * @return the RemoteFile mentioned above. May be null, if the message is
     * not ok.
     */
    public RemoteFile getResultFile() {
        return resultFile;
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
     * Returns a string representing the instance.
     *
     * @return a string
     */
    public String toString() {
        return "\nBatchReplyMessage for batch job " + replyOfId
                + "\nFilesProcessed = " + noOfFilesProcessed
                + "\nFilesFailed = " + filesFailed.size()
                + "\n" + super.toString();
    }

}
