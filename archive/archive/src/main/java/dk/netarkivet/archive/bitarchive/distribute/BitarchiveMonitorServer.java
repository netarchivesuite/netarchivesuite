/* File:        $Id$
 * Date:        $Date$
 * Revision:    $Revision$
 * Author:      $Author$
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
package dk.netarkivet.archive.bitarchive.distribute;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.archive.ArchiveSettings;
import dk.netarkivet.archive.arcrepository.bitpreservation.ChecksumJob;
import dk.netarkivet.archive.arcrepository.bitpreservation.FileListJob;
import dk.netarkivet.archive.bitarchive.BitarchiveMonitor;
import dk.netarkivet.archive.checksum.distribute.CorrectMessage;
import dk.netarkivet.archive.checksum.distribute.GetAllChecksumsMessage;
import dk.netarkivet.archive.checksum.distribute.GetAllFilenamesMessage;
import dk.netarkivet.archive.checksum.distribute.GetChecksumMessage;
import dk.netarkivet.archive.distribute.ArchiveMessageHandler;
import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.distribute.Channels;
import dk.netarkivet.common.distribute.JMSConnection;
import dk.netarkivet.common.distribute.JMSConnectionFactory;
import dk.netarkivet.common.distribute.NetarkivetMessage;
import dk.netarkivet.common.distribute.RemoteFile;
import dk.netarkivet.common.distribute.RemoteFileFactory;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.CleanupIF;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.KeyValuePair;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.utils.batch.FileBatchJob;

/**
 * Class representing message handling for the monitor for bitarchives. The
 * monitor is used for sending out and combining the results of executing batch
 * jobs.
 *
 * Batch jobs are received on the BAMON-channel, and resent to all bitarchives,
 * that are considered live by the bitarchive monitor.
 *
 * Lets the bitarchive monitor handle batch replies from the bitarchives, and
 * observes it for when the batch job is done. Then constructs a reply from the
 * data given, and sends it back to the originator.
 *
 * Also registers signs of life from the bitarchives in the bitarchive monitor.
 */
public class BitarchiveMonitorServer extends ArchiveMessageHandler
        implements Observer, CleanupIF {

    /**
     * The unique instance of this class.
     */
    private static BitarchiveMonitorServer instance;

    /**
     * Logger.
     */
    private static final Log log
            = LogFactory.getLog(BitarchiveMonitorServer.class);

    /**
     * The jms connection used.
     */
    private final JMSConnection con = JMSConnectionFactory.getInstance();

    /**
     * Object that handles logical operations.
     */
    private BitarchiveMonitor bamon;
    
    /**
     * Map for managing the messages, which are made into batchjobs.
     * The String is the ID of the message.
     */
    private Map<String, NetarkivetMessage> batchConversions = 
        new HashMap<String, NetarkivetMessage>();
    
    /**
     * The map for managing the CorrectMessages. This involves three stages.
     * 
     * First, a RemoveAndGetFileMessage is sent, and then the CorrectMessage is
     * put in the map along the ID of the RemoveAndGetFileMessage.
     * 
     * Second, the reply of the RemoveAndGetFileMessage is used to extract the
     * CorrectMessage from the Map. The CorrectMessage is then updated with the
     * results from the RemoveAndGetFileMessage. Then a UploadMessage is send 
     * with the 'correct' file, where the ID of the UploadMessage is put into
     * the map along the CorrectMessage.
     * 
     * Third, the reply of the UploadMessage is used to extract the 
     * CorrectMessage from the map again, and the results of the UploadMessage 
     * is used to update the UploadMessage, which is then returned.
     */
    private Map<String, CorrectMessage> correctMessages =
        new HashMap<String, CorrectMessage>();

    /**
     * Creates an instance of a BitarchiveMonitorServer.
     *
     * @throws IOFailure - if an error with the JMSConnection occurs
     */
    protected BitarchiveMonitorServer() throws IOFailure {
        bamon = BitarchiveMonitor.getInstance();
        bamon.addObserver(this);
        con.setListener(Channels.getTheBamon(), this);
        log.info("BitarchiveMonitorServer instantiated. "
                 + "Listening to queue: '" + Channels.getTheBamon() + "'.");
    }

    /**
     * Returns the unique instance of a BitarchiveMonitorServer.
     *
     * @return the instance
     * @throws IOFailure - if an error with the JMSConnection occurs
     */
    public static synchronized BitarchiveMonitorServer getInstance()
            throws IOFailure {
        if (instance == null) {
            instance = new BitarchiveMonitorServer();
        }
        return instance;
    }


    /**
     * This is the message handling method for BatchMessages.
     *
     * A new BatchMessage is created with the same Job as the incoming
     * BatchMessage and sent off to all live bitarchives.
     *
     * The incoming and outgoing batch messages are then registered at the
     * bitarchive monitor.
     *
     * @param inbMsg The message received
     */
    public void visit(BatchMessage inbMsg) {
        log.info("Received BatchMessage\n" + inbMsg.toString());
        try {
            BatchMessage outbMsg =
                    new BatchMessage(Channels.getAllBa(), inbMsg.getJob(),
                                     Settings.get(
                                             CommonSettings.USE_REPLICA_ID));
            con.send(outbMsg);
            long batchTimeout = inbMsg.getJob().getBatchJobTimeout();
            // if batch time out is not a positive number, then use settings.
            if(batchTimeout <= 0) {
                batchTimeout = Settings.getLong(
                        ArchiveSettings.BITARCHIVE_BATCH_JOB_TIMEOUT);
            }
            bamon.registerBatch(inbMsg.getID(), inbMsg.getReplyTo(),
                        outbMsg.getID(), batchTimeout);
        } catch (Exception e) {
            log.warn("Trouble while handling batch request '" + inbMsg + "'",
                     e);
        }
    }

    /**
     * This is the message handling method for BatchEndedMessages.
     *
     * This delegates the handling of the reply to the bitarchive monitor, which
     * will notify us if the batch job is now done.
     *
     * @param beMsg The BatchEndedMessage to be handled.
     */
    public void visit(final BatchEndedMessage beMsg) {
        log.info("Received batch ended from bitarchive '"
                 + beMsg.getBitarchiveID() + "': " + beMsg);
        bamon.signOfLife(beMsg.getBitarchiveID());
        try {
            new Thread() {
                public void run() {
                    // retrieve the error messages.
                    String errorMessages = null;
                    if(!beMsg.isOk()) {
                        errorMessages = beMsg.getErrMsg();
                    }
                    // send reply to the bitarchive.
                    bamon.bitarchiveReply(beMsg.getOriginatingBatchMsgID(),
                                          beMsg.getBitarchiveID(),
                                          beMsg.getNoOfFilesProcessed(),
                                          beMsg.getFilesFailed(),
                                          beMsg.getRemoteFile(),
                                          errorMessages,
                                          beMsg.getExceptions());
                }
            }.start();
        } catch (Exception e) {
            log.warn("Trouble while handling bitarchive reply '" + beMsg + "'",
                     e);
        }
    }


    /**
     * This is the message handling method for HeartBeatMessages.
     *
     * Registers a sign of life from a bitarchive.
     *
     * @param hbMsg the message that represents the sign of life
     */
    public void visit(HeartBeatMessage hbMsg) {
        try {
            bamon.signOfLife(hbMsg.getBitarchiveID());
        } catch (Exception e) {
            log.warn("Trouble while handling bitarchive sign of life '" + hbMsg
                     + "'", e);
        }
    }
    
    /**
     * This is the first step in correcting a bad entry.
     * 
     * First, a RemoveAndGetFileMessage is sent, and then the CorrectMessage is
     * put in the map along the ID of the RemoveAndGetFileMessage.
     * 
     * Second, the reply of the RemoveAndGetFileMessage is used to extract the
     * CorrectMessage from the Map. The CorrectMessage is then updated with the
     * results from the RemoveAndGetFileMessage. Then a UploadMessage is send 
     * with the 'correct' file, where the ID of the UploadMessage is put into
     * the map along the CorrectMessage.
     * 
     * Third, the reply of the UploadMessage is used to extract the 
     * CorrectMessage from the map again, and the results of the UploadMessage 
     * is used to update the UploadMessage, which is then returned.
     * 
     * @param cm The CorrectMessage to handle.
     * @throws ArgumentNotValid If the CorrectMessage is null.
     */
    public void visit(CorrectMessage cm) throws ArgumentNotValid {
        ArgumentNotValid.checkNotNull(cm, "CorrectMessage cm");
        log.info("Recieving CorrectMessage: " + cm);
        
        // Create the RemoveAndGetFileMessage for removing the file.
        RemoveAndGetFileMessage ragfm = new RemoveAndGetFileMessage(
                Channels.getAllBa(), Channels.getTheBamon(), 
                cm.getArcfileName(), cm.getReplicaId(), 
                cm.getIncorrectChecksum(), cm.getCredentials());
        
        // Send the message.
        con.send(ragfm);
        
        // Put the CorrectMessage into the map along the id of the 
        // RemoveAndGetFileMessage
        correctMessages.put(ragfm.getID(), cm);
    }
    
    /**
     * This is the second step in correcting a bad entry.
     * 
     * First, a RemoveAndGetFileMessage is sent, and then the CorrectMessage is
     * put in the map along the ID of the RemoveAndGetFileMessage.
     * 
     * Second, the reply of the RemoveAndGetFileMessage is used to extract the
     * CorrectMessage from the Map. The CorrectMessage is then updated with the
     * results from the RemoveAndGetFileMessage. Then a UploadMessage is send 
     * with the 'correct' file, where the ID of the UploadMessage is put into
     * the map along the CorrectMessage.
     * 
     * Third, the reply of the UploadMessage is used to extract the 
     * CorrectMessage from the map again, and the results of the UploadMessage 
     * is used to update the UploadMessage, which is then returned.
     * 
     * @param The RemoteAndGetFileMessage.
     * @throws ArgumentNotValid If the RemoveAndGetFileMessage is null.
     */
    public void visit(RemoveAndGetFileMessage msg) throws ArgumentNotValid {
        ArgumentNotValid.checkNotNull(msg, "RemoveAndGetFileMessage msg");
        log.info("Recieving RemoveAndGetFileMessage (presumably reply): " 
                + msg);

        // Retrieve the correct message
        CorrectMessage cm = correctMessages.remove(msg.getID());

        // update the correct message.
        cm.setRemovedFile(msg.getRemoteFile());

        // If the RemoveAndGetFileMessage has failed, then the CorrectMessage
        // has also filed, and should be returned as a fail.
        if(!msg.isOk()) {
            String errMsg = "The RemoveAndGetFileMessage has returned the error: '"
                + msg.getErrMsg() + "'. Reply to CorrectMessage with "
                + "the same error.";
            log.warn(errMsg);
            cm.setNotOk(errMsg);
            con.reply(cm);
            return;
        }
        
        // Create the upload message, send it, and store the CorrectMessage 
        // along with the ID of the upload message in the map.
        UploadMessage um = new UploadMessage(Channels.getAllBa(), 
                Channels.getTheBamon(), cm.getCorrectFile());
        con.send(um);
        correctMessages.put(um.getID(), cm);
    }
    
    /**
     * This is the third step in correcting a bad entry. 
     * 
     * First, a RemoveAndGetFileMessage is sent, and then the CorrectMessage is
     * put in the map along the ID of the RemoveAndGetFileMessage.
     * 
     * Second, the reply of the RemoveAndGetFileMessage is used to extract the
     * CorrectMessage from the Map. The CorrectMessage is then updated with the
     * results from the RemoveAndGetFileMessage. Then a UploadMessage is send 
     * with the 'correct' file, where the ID of the UploadMessage is put into
     * the map along the CorrectMessage.
     * 
     * Third, the reply of the UploadMessage is used to extract the 
     * CorrectMessage from the map again, and the results of the UploadMessage 
     * is used to update the UploadMessage, which is then returned.
     * 
     * @param The reply of the UploadMessage.
     * @throws ArgumentNotValid If the UploadMessage is null.  
     */
    public void visit(UploadMessage msg) throws ArgumentNotValid {
        ArgumentNotValid.checkNotNull(msg, "UploadMessage msg");
        log.info("Receiving a reply to an UploadMessage: " + msg);
        
        // retrieve the CorrectMessage.
        CorrectMessage cm = correctMessages.remove(msg.getID());
        
        // handle potential errors.
        if(!msg.isOk()) {
            cm.setNotOk(msg.getErrMsg());
        }
        
        // reply to the correct message.
        con.reply(cm);
    }
    
    /**
     * Method for handling the 
     * 
     * @param msg The GetAllChecksumsMessage, which will be made into a batchjob
     * and sent to the 
     */
    public void visit(GetAllChecksumsMessage msg) {
        log.info("Receiving GetAllChecksumsMessage '" + msg + "'");
        
        // Create batchjob for the GetAllChecksumsMessage.
        ChecksumJob cj = new ChecksumJob();
        
        // Execute the batchjob.
        executeConvertedBatch(cj, msg);
    }

    /**
     * Method for handling the 
     * 
     * @param msg The GetAllChecksumsMessage, which will be made into a batchjob
     * and sent to the 
     */
    public void visit(GetAllFilenamesMessage msg) {
        log.info("Receiving GetAllChecksumsMessage '" + msg + "'");

        // Create batchjob for the GetAllChecksumsMessage.
        FileListJob flj = new FileListJob();

        // Execute the batchjob.
        executeConvertedBatch(flj, msg);
    }

    /**
     * Method for handling the 
     * 
     * @param msg The GetAllChecksumsMessage, which will be made into a batchjob
     * and sent to the 
     */
    public void visit(GetChecksumMessage msg) {
        log.info("Receiving GetAllChecksumsMessage '" + msg + "'");
        
        // Create batchjob for the GetAllChecksumsMessage.
        ChecksumJob cj = new ChecksumJob();
        cj.processOnlyFileNamed(msg.getArcfileName());
        
        // Execute the batchjob.
        executeConvertedBatch(cj, msg);
    }
    
    /**
     * Method for executing messages converted into batchjobs.
     * 
     * @param job The job to execute.
     * @param msg The message which is converted into the batchjob.
     */
    private void executeConvertedBatch(FileBatchJob job, 
            NetarkivetMessage msg) {
        try {
            BatchMessage outbMsg =
                    new BatchMessage(Channels.getAllBa(), job,
                                     Settings.get(
                                             CommonSettings.USE_REPLICA_ID));
            con.send(outbMsg);
            
            long batchTimeout = job.getBatchJobTimeout();
            // if batch time out is not a positive number, then use settings.
            if(batchTimeout <= 0) {
                batchTimeout = Settings.getLong(
                        ArchiveSettings.BITARCHIVE_BATCH_JOB_TIMEOUT);
            }
            bamon.registerBatch(msg.getID(), msg.getReplyTo(),
                        outbMsg.getID(), batchTimeout);
            // Remember that the message is a batch conversion.
            log.info(outbMsg);

            batchConversions.put(msg.getID(), msg);
        } catch (Throwable e) {
            log.warn("Trouble while handling batch request '" + msg + "'",
                     e);
            msg.setNotOk(e);
            con.reply(msg);
        }
    }
    
    /**
     * Handles notifications from the bitarchive monitor, that a batch job is
     * complete.
     *
     * Spawns a new thread in which all the results are wrapped and sent
     * back in a reply to the originator of this batch request.
     *
     * @param o   the observable object. Should always be the bitarchive
     *            monitor. If it isn't, this notification will be logged and
     *            ignored.
     * @param arg an argument passed from the bitarchive monitor. This should
     *            always be a batch status object indicating the end of that
     *            batchjob. If it isn't, this notification will be logged and
     *            ignored.
     */
    public void update(Observable o, final Object arg) {
        if (o != bamon) {
            log.warn("Received unexpected notification from '" + o + "'");
            return;
        }
        if (arg == null) {
            log.warn("Received unexpected notification with no argument");
            return;
        }
        if (!(arg
                instanceof dk.netarkivet.archive.bitarchive
                            .BitarchiveMonitor.BatchJobStatus)) {
            log.warn("Received notification with incorrect argument type "
                     + arg.getClass() + ":'" + arg + "''");
            return;
        }
        new Thread() {
            public void run() {
                // convert the input argument.
                BitarchiveMonitor.BatchJobStatus bjs = 
                    (BitarchiveMonitor.BatchJobStatus) arg;
                
                // Check whether converted message or actual batchjob.
                if(batchConversions.containsKey(bjs.originalRequestID)) {
                    replyConvertedBatch(bjs);
                } else {
                    doBatchReply(bjs);
                }
            }
        }.start();
    }

    /**
     * This method sends a reply based on the information from bitarchives
     * received and stored in the given batch job status.
     *
     * It will concatenate the results from all the bitarchives in one file, and
     * construct a reply to the originating requester with all information.
     *
     * @param bjs Status of received messages from bitarchives.
     */
    private void doBatchReply(BitarchiveMonitor.BatchJobStatus bjs) {
        RemoteFile resultsFile = null;
        try {
            //Get remote file for batch  result
            resultsFile = RemoteFileFactory.getMovefileInstance(
                    bjs.batchResultFile);
        } catch (Exception e) {
            log.warn("Make remote file from "
                    + bjs.batchResultFile, e);
            bjs.appendError("Could not append batch results: " + e);
        }

        // Make batch reply message
        BatchReplyMessage brMsg =
            new BatchReplyMessage(bjs.originalRequestReplyTo,
                    Channels.getTheBamon(),
                    bjs.originalRequestID,
                    bjs.noOfFilesProcessed,
                    bjs.filesFailed, resultsFile);
        if (bjs.errorMessages != null) {
            brMsg.setNotOk(bjs.errorMessages);
        }

        // Send the batch reply message.
        con.send(brMsg);

        log.info("BatchReplyMessage: '" + brMsg + "' sent from BA monitor "
                + "to queue: '" + brMsg.getTo() + "'");
    }
    
    /**
     * Uses the batchjobstatus on the message converted batchjob to reply on 
     * the original message.
     * 
     * @param bjs The status of the batchjob.
     */
    private void replyConvertedBatch(BitarchiveMonitor.BatchJobStatus bjs) {
        // Retrieve the message corresponding to the converted batchjob.
        NetarkivetMessage msg = batchConversions.remove(bjs.originalRequestID);
        log.info("replying to converted batchjob message : " + msg);
        try {
            if(msg instanceof GetAllChecksumsMessage) {
                // Handle GetAllChecksumsMessage
                GetAllChecksumsMessage replyMsg = (GetAllChecksumsMessage) msg;
                
                // Set the resulting file.
                replyMsg.setFile(bjs.batchResultFile);

                // record any errors.
                if(bjs.errorMessages != null) {
                    replyMsg.setNotOk(bjs.errorMessages);
                }
                
                // reply
                log.info("Replying to GetAllChecksumsMessage '" + replyMsg 
                        + "'");
                con.reply(replyMsg);
            } else if(msg instanceof GetAllFilenamesMessage) {
                // Handle GetAllChecksumsMessage
                GetAllFilenamesMessage replyMsg = (GetAllFilenamesMessage) msg;
                
                // Set the resulting file.
                replyMsg.setFile(bjs.batchResultFile);

                // record any errors.
                if(bjs.errorMessages != null) {
                    replyMsg.setNotOk(bjs.errorMessages);
                }
                
                // reply
                log.info("Replying to GetAllFilenamesMessage '" + replyMsg 
                        + "'");
                con.reply(replyMsg);
            } else if(msg instanceof GetChecksumMessage) {
                // Handle GetChecksumMessage 
                GetChecksumMessage replyMsg = (GetChecksumMessage) msg;

                // read the temporary file
                List<String> output = FileUtils.readListFromFile(bjs.batchResultFile);
                
                if(output.size() < 1) {
                    String errMsg = "The batchjob did not find the file '"
                        + replyMsg.getArcfileName() + "' within the archive.";
                    log.warn(errMsg);
                    throw new IOFailure(errMsg);
                }
                if(output.size() > 1) {
                    log.warn("The file '" + replyMsg.getArcfileName() 
                            + "' was found " + output.size() + " times in the "
                            + "archive. Using the first found: " 
                            + output.get(0));
                    // TODO handle if different or at least log the others
                }
  
                // Extract the filename and checksum of the first result.
                KeyValuePair<String, String> firstResult = 
                    ChecksumJob.parseLine(output.get(0));
                
                // Check that the filename is valid
                if(!replyMsg.getArcfileName().equals(firstResult.getKey())) {
                    String errMsg = "The first result found the file '"
                        + firstResult.getKey() + "' but should have found '"
                        + replyMsg.getArcfileName() + "'.";
                    log.error(errMsg);
                    throw new IOFailure(errMsg);
                }
                
                // Put the checksum into the reply message, and reply.
                replyMsg.setChecksum(firstResult.getValue());
                replyMsg.setIsReply();
                con.reply(replyMsg);
                
                // cleanup batchjob file
                FileUtils.remove(bjs.batchResultFile);
                
            } else /* unhandled message type. */{
                String errMsg = "The message cannot be handled '" + msg + "'";
                log.error(errMsg);
                throw new IOFailure(errMsg);
            }
        } catch (Throwable e) {
            msg.setNotOk(e);
            con.reply(msg);
        }
    }

    /**
     * Close down this BitarchiveMonitor.
     */
    public void close() {
        log.info("BitarchiveMonitorServer closing down.");
        cleanup();
        log.info("BitarchiveMonitorServer closed down");
    }

    /**
     * Closes this BitarchiveMonitorServer cleanly.
     */
    public void cleanup() {
        if (instance != null) {
            con.removeListener(Channels.getTheBamon(), this);
            batchConversions.clear();
            instance = null;
            if (bamon != null) {
                bamon.cleanup();
                bamon = null;
            }
        }
    }
}
