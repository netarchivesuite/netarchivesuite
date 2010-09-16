/* File:        $Id$
 * Date:        $Date$
 * Revision:    $Revision$
 * Author:      $Author$
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
package dk.netarkivet.archive.bitarchive;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.archive.ArchiveSettings;
import dk.netarkivet.common.distribute.ChannelID;
import dk.netarkivet.common.distribute.RemoteFile;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.CleanupIF;
import dk.netarkivet.common.utils.ExceptionUtils;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.utils.StringUtils;
import dk.netarkivet.common.utils.batch.FileBatchJob;

/**
 * Class representing the monitor for bitarchives. The monitor is used for
 * sending out and combining the results of executing batch jobs.
 *
 * Registers outgoing batchjobs to bitarchives, and handles replies from
 * bitarchives, finally notifying observers when all bitarchives have replied,
 * or when the batch times out, after a time specified in settings.
 *
 * We wait for replies from bitarchives that are considered live when the batch
 * begins. A bitarchive is considered live if we have heard any activity from it
 * within a time specified in settings.
 */
public class BitarchiveMonitor extends Observable implements CleanupIF {
    /**
     * The current instance.
     */
    private static BitarchiveMonitor instance;
    
    /**
     * The time of the latest sign of life received from each bitarchive.
     */
    private Map<String, Long> bitarchiveSignsOfLife =
            Collections.synchronizedMap(new HashMap<String, Long>());

    /**
     * The acceptable delay in milliseconds between signs of life.
     */
    private final long acceptableSignOfLifeDelay;


    /**
     * Map from the ID of batch jobs sent to bitarchives, to tuple class of
     * status for this batch job. The Map contains all batch jobs currently
     * running.
     */
    private Map<String, BatchJobStatus> runningBatchJobs =
            Collections.synchronizedMap(new HashMap<String, BatchJobStatus>());

    /**
     * Logger for this class.
     */
    private static Log log = LogFactory.getLog(BitarchiveMonitor.class);

    /**
     * Initialises the bitarchive monitor. During this, the acceptable delay
     * between signs of life and the timeout setting for batchjobs are read and
     * logged.
     */
    private BitarchiveMonitor() {
        acceptableSignOfLifeDelay = Settings.getLong(
                ArchiveSettings.BITARCHIVE_ACCEPTABLE_HEARTBEAT_DELAY);
        log.info("Bitarchive liveness times out after "
                 + acceptableSignOfLifeDelay + " milliseconds.");
    }
    
    /**
     * Method for retrieving the current instance.
     * If no instance has been instantiated, then a new one will be created.
     * 
     * @return The current instance of the BitarchiveMonitor.
     */
    public static synchronized BitarchiveMonitor getInstance() {
        if(instance == null) {
            instance = new BitarchiveMonitor();
        }
        return instance;
    }

    /**
     * Registers a sign of life from a bitarchive. This method logs when new bit
     * archives present themselves.
     *
     * @param appID the ID of the bitarchive that generated the life sign
     */
    public void signOfLife(String appID) {
        ArgumentNotValid.checkNotNullOrEmpty(appID, "String appID");
        long now = System.currentTimeMillis();
        if ((!bitarchiveSignsOfLife.containsKey(appID))) {
            log.info("Bitarchive '" + appID + "' is now known by the bitarchive"
                     + " monitor");
        }
        log.trace("Received sign of life from bitarchive '" + appID + "'");
        bitarchiveSignsOfLife.put(appID, now);
    }

    /**
     * Register a new batch sent to the bitarchives.
     *
     * This registers a new batchstatus object, with a list of live bitarchives
     * awaiting reply, and a timer task letting the job time out after the
     * specified time.
     *
     * @param requestID         The ID of the batch request.
     * @param requestReplyTo    The replyTo channel of the batch request.
     * @param bitarchiveBatchID The ID of the batch job sent on to the bit
     *                          archives.
     * @param timeout           Timeout of specific batch job.
     * @throws ArgumentNotValid If any argument is null, or either string is 
     *                          empty.
     */
    public void registerBatch(String requestID, ChannelID requestReplyTo,
            String bitarchiveBatchID, long timeout) throws ArgumentNotValid {
        ArgumentNotValid.checkNotNullOrEmpty(requestID, "String requestID");
        ArgumentNotValid.checkNotNull(requestReplyTo,
                                      "ChannelID requestReplyTo");
        ArgumentNotValid.checkNotNullOrEmpty(bitarchiveBatchID,
                                             "String bitarchiveBatchID");
        BatchJobStatus bjs = new BatchJobStatus(
                requestID, requestReplyTo, bitarchiveBatchID,
                getRunningBitarchiveIDs(), timeout
        );
        runningBatchJobs.put(bitarchiveBatchID, bjs);
        log.info("Registered Batch job from " + requestID + " with timeout "
                + timeout + ". Number of outstanding batchjobs are now: " 
               + runningBatchJobs.size());
    }

    /**
     * Generate a set of bitarchiveIDs that are considered live.
     *
     * @return Set of IDs of active bitarchives
     */
    private Set<String> getRunningBitarchiveIDs() {
        Map<String, Long> signsOfLifeCopy;
        long now;
        synchronized (bitarchiveSignsOfLife) {
            now = System.currentTimeMillis();
            signsOfLifeCopy = new HashMap<String, Long>(bitarchiveSignsOfLife);
        }
        Set<String> runningApps = new HashSet<String>();
        for (Map.Entry<String, Long> baID : signsOfLifeCopy.entrySet()) {
            if (baID.getValue() + acceptableSignOfLifeDelay > now) {
                runningApps.add(baID.getKey());
            } else {
                log.warn("Not listening for replies from the bitarchive '"
                         + baID.getKey()
                         + "' which hasn't shown signs of life in "
                         + (now - baID.getValue())
                         + " milliseconds");
                // Remove the bitarchive to ensure this warning is not logged
                // more than once, and a new message is logged when it returns.
                bitarchiveSignsOfLife.remove(baID.getKey());
            }
        }
        return runningApps;
    }

    /**
     * Handle a reply received from a bitarchive.
     *
     * This method registers the information from the bitarchive in the batch
     * status for this job, if any (otherwise logs and quits).
     *
     * If this is the last bitarchive we were missing replies from, notify
     * observers with the batch status for this job.
     * 
     * TODO why are the 'exceptions' argument not used?
     *
     * @param bitarchiveBatchID The ID of the batch job sent on to the bit 
     * archives.
     * @param bitarchiveID The ID of the replying bitarchive.
     * @param noOfFilesProcessed The number of files the bitarchive has 
     * processed.
     * @param filesFailed A collection of filenames of failed files in 
     * that bitarchive. Might be null if no files failed.
     * @param remoteFile A remote pointer to a file with results from that 
     * bitarchive. Might be null if job was not OK.
     * @param errMsg An error message, if the job was not successful on the 
     * bitarchive, or null for none.
     * @param exceptions A list of exceptions caught during batch processing.
     * @throws ArgumentNotValid If either ID is null.
     */
    public void bitarchiveReply(String bitarchiveBatchID, String bitarchiveID, 
            int noOfFilesProcessed, Collection<File> filesFailed, 
            RemoteFile remoteFile, String errMsg,
            List<FileBatchJob.ExceptionOccurrence> exceptions) 
            throws ArgumentNotValid {
        ArgumentNotValid.checkNotNullOrEmpty(bitarchiveBatchID,
                "String bitarchiveBatchID");
        ArgumentNotValid.checkNotNullOrEmpty(bitarchiveID,
                "String bitarchiveID");
        ArgumentNotValid.checkNotNegative(noOfFilesProcessed, 
                "int noOfFilesProcessed");
       
        BatchJobStatus bjs = runningBatchJobs.get(bitarchiveBatchID);
        if (bjs == null) {
            // If the batch ID does not correspond to any of the pending batch
            // jobs, just log and ignore the message.
            log.debug(
                    "The batch ID '" + bitarchiveBatchID
                    + "' of the received reply from bitarchives does not "
                    + "correspond to any pending batch job. Ignoring and "
                    + "deleting RemoteFile '" + remoteFile + "'."
                    + "Only knows batchjob with IDs: "
                    + runningBatchJobs.keySet());
           
            if (remoteFile != null) {
                remoteFile.cleanup();
            }
        } else {
            bjs.updateWithBitarchiveReply(bitarchiveID, noOfFilesProcessed, 
                    filesFailed, remoteFile, errMsg);
        }
    }

    /** Notifies observers that the given batch job has ended.
     *
     * @param batchJobStatus The batch job that has ended.
     */
    private void notifyBatchEnded(BatchJobStatus batchJobStatus) {
        runningBatchJobs.remove(
                batchJobStatus.bitarchiveBatchID);
        // Notify observers that this batch is done
        setChanged();
        notifyObservers(batchJobStatus);
        log.info("Batchjob '" + batchJobStatus.bitarchiveBatchID + "' finished."
                + "The number of outstanding batchjobs are now: " 
                + runningBatchJobs.size());
    }

    /**
     * Closes this BitarchiveMonitor cleanly. Currently does nothing.
     */
    public void cleanup() {
        instance = null;
    }

    /**
     * Class handling state and updates in batch job status.
     *
     * This class remembers information about the batchjob sent, and information
     * from all bitarchive replies received. It also contains information about
     * the original requester of the batchjob.
     */
    public final class BatchJobStatus {

        /**
         * The timer task that handles timeout of this batch job.
         */
        private final BatchTimeoutTask batchTimeoutTask;
        /**
         * The ID of the job sent to the bitarchives.
         */
        private final String bitarchiveBatchID;
        /**
         * Have we begun replying for this batch job?
         */
        private boolean notifyInitiated;

        /**
         * the ID of the original batch request.
         */
        public final String originalRequestID;
        
        /**
         * The reply channel for the original request.
         */
        public final ChannelID originalRequestReplyTo;
        
        /**
         * set containing the bitarchives that were alive when we sent the
         * job, but haven't answered yet.
         */
        public final Set<String> missingRespondents;
        
        /**
         * The accumulated number of files processed in replies received so
         * far.
         */
        public int noOfFilesProcessed;
        
        /**
         * The accumulated list of files failed in replies received so far.
         */
        public final Collection<File> filesFailed;
        
        /**
         * A string with a concatenation of errors. This error message is null,
         * if the job is successful.
         */
        public String errorMessages;
        
        /**
         * A File with a concatenation of results from replies received so far.
         */
        public final File batchResultFile;
        
        /**
         * A list of the exceptions that occurred during processing.
         */
        public final List<FileBatchJob.ExceptionOccurrence> exceptions;

        /**
         * The timeout for batch jobs in milliseconds.
        */
        private long batchTimeout;

        /**
         * Initialise the status on a fresh batch request. Apart from the given
         * values, a file is created to store batch results in.
         * <b>Sideeffect</b>: BatchTimeout is started here
         *
         * @param originalRequestID      The ID of the originating request.
         * @param originalRequestReplyTo The reply channel for the originating
         *                               request.
         * @param bitarchiveBatchID      The ID of the job sent to bitarchives.
         * @param missingRespondents     List of all live bitarchives, used to
         *                               know which bitarchives to await reply
         *                               from.
         * @param timeout                Timeout for Batch job
         * @throws IOFailure if a file for batch results cannot be made.
         */
        private BatchJobStatus(String originalRequestID, 
                ChannelID originalRequestReplyTo, String bitarchiveBatchID,
                Set<String> missingRespondents, long timeout) 
                throws IOFailure {
            this.originalRequestID = originalRequestID;
            this.originalRequestReplyTo = originalRequestReplyTo;
            this.bitarchiveBatchID = bitarchiveBatchID;
            this.missingRespondents = missingRespondents;
            batchTimeoutTask
                    = new BatchTimeoutTask(bitarchiveBatchID);
            batchTimeout = timeout;
            Timer timer = new Timer(true);
            timer.schedule(batchTimeoutTask, batchTimeout);
            this.noOfFilesProcessed = 0;
            try {
                this.batchResultFile = File.createTempFile(
                        bitarchiveBatchID, "batch_aggregation",
                        FileUtils.getTempDir());
            } catch (IOException e) {
                final String errMsg = "Unable to create file for batch output";
                log.warn(errMsg);
                throw new IOFailure(errMsg, e);
            }
            this.filesFailed = new ArrayList<File>();
            //Null indicates no error
            this.errorMessages = null;
            this.notifyInitiated = false;

            exceptions = new ArrayList<FileBatchJob.ExceptionOccurrence>();
        }

        /**
         * Appends the given message to the current error message.
         *
         * @param errMsg A message describing what went wrong.
         */
        public void appendError(String errMsg) {
            if (this.errorMessages == null) {
                this.errorMessages = errMsg;
            } else {
                this.errorMessages += "\n" + errMsg;
            }
        }

        /**
         * Updates the status with info from a bitarchive reply.
         *
         * This will add the results given to the status, and if this was the
         * last remaining bitarchive, also sends a notification to all observers
         * of the bitarchive monitor.
         *
         * @param bitarchiveID The ID of the bitarchive that has replied
         * @param numberOfFilesProcessed The number of files processed by that 
         * bit archive.
         * @param failedFiles List of files failed in that bit archive.
         * @param remoteFile A pointer to a remote file with results from the 
         * bitarchive.
         * @param errMsg An error message with errors from that bit archive.
         */
        private synchronized void updateWithBitarchiveReply(String bitarchiveID,
                int numberOfFilesProcessed, Collection<File> failedFiles, 
                RemoteFile remoteFile, String errMsg) {
            if (notifyInitiated) {
                log.debug("The reply for batch job: '"
                          + bitarchiveBatchID
                          + "' from bitarchive '" + bitarchiveID
                          + "' arrived after we had started replying."
                          + "Ignoring this reply.");
                remoteFile.cleanup();
                return;
            }
            // found is set to true, if bitarchiveID was among
            // the missingRespondents, before it was deleted.
            boolean found = missingRespondents.remove(bitarchiveID);

            // Handle the reply, even though the bitarchive was not known to be
            // live, but log a warning.
            if (!found) {
                log.warn("Received a batch reply for: "
                         + bitarchiveBatchID
                         + " from an unexpected bit archive: '"
                         + bitarchiveID + "'");
            }
            this.noOfFilesProcessed += numberOfFilesProcessed;
            if (failedFiles != null) {
                this.filesFailed.addAll(failedFiles);
            }

            appendRemoteFileToAggregateFile(remoteFile);
            this.exceptions.addAll(this.exceptions);

            // In case the batch reply contains an error, the final
            // we append this error.
            if (errMsg != null) {
                appendError(errMsg);
                log.warn("Received batch reply with error: " + errMsg
                         + " at BA monitor from bitarchive " + bitarchiveID);
            }

            // if all archives have answered then notify observers that we are
            // done.
            if (missingRespondents.isEmpty()) {
                notifyBatchEnded();
            }
        }

        /**
         * Append a remotefile to the batch result aggregate file. Adds info on
         * errors while concatenating to the batch status.
         *
         * @param rf A remotefile to read from
         */
        private void appendRemoteFileToAggregateFile(RemoteFile rf) {
            if (rf != null) {
                OutputStream aggregateStream = null;
                try {
                    aggregateStream = new FileOutputStream(
                            batchResultFile, true);
                    rf.appendTo(aggregateStream);

                    try {
                        rf.cleanup();
                    } catch (IOFailure e) {
                        log.warn("Could not remove remotefile '" + rf + "'", e);
                        //Harmless, though. Continue
                    }
                } catch (IOFailure e) {
                    String errMsg = "Exception while aggregating batch "
                                    + " output for " + rf.getName() + ": "
                                    + ExceptionUtils.getStackTrace(e);
                    appendError(errMsg);
                } catch (IOException e) {
                    String errMsg = "Exception while aggregating batch "
                                    + " output for " + rf.getName() + ": "
                                    + ExceptionUtils.getStackTrace(e);
                    appendError(errMsg);
                } finally {
                    if (aggregateStream != null) {
                        try {
                            aggregateStream.close();
                        } catch (IOException e) {
                            String errMsg = "Exception while aggregating batch "
                                            + " output for " + rf.getName()
                                            + ": "
                                            + ExceptionUtils.getStackTrace(e);
                            appendError(errMsg);
                        }
                    }
                }
            }
        }

        /**
         * Checks whether this batch job is already being notified about. If
         * not, it notifies observers with this batch status.
         *
         */
        private synchronized void notifyBatchEnded() {
            if (!notifyInitiated) {
                notifyInitiated = true;
                batchTimeoutTask.cancel();
                BitarchiveMonitor.this.notifyBatchEnded(this);
            }
        }

    }

    /**
     * A timertask that makes batch ended notifications happen after a time
     * specified in settings has elapsed, even though not all replies have been
     * received.
     */
    private class BatchTimeoutTask extends TimerTask {
        /**
         * The ID of the batch job this object handles timeout for.
         */
        private final String bitarchiveBatchID;

        /**
         * Initiate a timer task for the given batch job status.
         *
         * @param bitarchiveBatchID The ID of the batch job to monitor timeout
         *                          for.
         */
        public BatchTimeoutTask(String bitarchiveBatchID) {
            ArgumentNotValid.checkNotNullOrEmpty(bitarchiveBatchID,
                    "String bitarchiveBatchID");
            this.bitarchiveBatchID = bitarchiveBatchID;
        }

        /**
         * Send a notifications on timeout if a notification is not already
         * initiated.
         */
        public void run() {
            // synchronize to ensure timeouts and batchreplies do not interfere
            // with one another
            BatchJobStatus bjs
                    = runningBatchJobs.get(bitarchiveBatchID);
            if (bjs != null) {
                synchronized (bjs) {
                    if (bjs.notifyInitiated) {
                        //timeout occurred, but we are already in the process of
                        // notifying. Just ignore.
                        return;
                    }
                    try {
                        String errMsg =
                                "A timeout has occurred for batch job: "
                                + bjs.bitarchiveBatchID
                                + ". Missing replies from ["
                                + StringUtils.conjoin(
                                        ", ", bjs.missingRespondents)
                                + "]";
                        log.warn(errMsg);
                        bjs.appendError(errMsg);
                        bjs.notifyBatchEnded();
                    } catch (Throwable t) {
                        log.warn("An error occurred during execution of "
                                 + "timeout task.", t);
                    }
                }
            }
        }
    }
}
