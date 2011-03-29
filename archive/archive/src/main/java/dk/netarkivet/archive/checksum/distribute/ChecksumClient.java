/* File:    $Id$
 * Version: $Revision$
 * Date:    $Date$
 * Author:  $Author$
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

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

import dk.netarkivet.archive.bitarchive.distribute.BatchMessage;
import dk.netarkivet.archive.bitarchive.distribute.GetFileMessage;
import dk.netarkivet.archive.bitarchive.distribute.GetMessage;
import dk.netarkivet.archive.bitarchive.distribute.RemoveAndGetFileMessage;
import dk.netarkivet.archive.bitarchive.distribute.UploadMessage;
import dk.netarkivet.archive.distribute.ReplicaClient;
import dk.netarkivet.common.distribute.ChannelID;
import dk.netarkivet.common.distribute.Channels;
import dk.netarkivet.common.distribute.JMSConnection;
import dk.netarkivet.common.distribute.JMSConnectionFactory;
import dk.netarkivet.common.distribute.RemoteFile;
import dk.netarkivet.common.distribute.arcrepository.ReplicaType;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.IllegalState;
import dk.netarkivet.common.utils.batch.FileBatchJob;

/**
 * Proxy for remote checksum archive. Establishes the jms connection to the
 * remote checksum archive.
 * 
 * Can be used in combination with any type of ChecksumServerAPI.
 */
public class ChecksumClient implements ReplicaClient {
    /** The log.*/
    protected static Log log = LogFactory.getLog(ChecksumClient.class);

    /** Connection to JMS provider.*/
    private JMSConnection jmsCon;

    /** 
     * Connection information.
     * The connection to contact all checksum archives.
     */
    private ChannelID theChecksumChannel;
    /** The name of the replica whose client this is.*/
    private String replicaId;
    
    /**
     * The constructor. Cannot be used directly, use getInstance instead.
     * 
     * @param theCRin
     * The channel for contacting the checksum archive.
     * @throws IOFailure
     * If there is a problem with the connection.
     */
    private ChecksumClient(ChannelID theCRin) throws IOFailure {
        this.theChecksumChannel = theCRin;
        replicaId = Channels.retrieveReplicaFromIdentifierChannel(
                theChecksumChannel.getName()).getId();
        jmsCon = JMSConnectionFactory.getInstance();
    }

    /**
     * The method for invoking an instance of this class.
     * 
     * @param theCRin The channel for contacting the checksum archive.
     * @return A new instance.
     * @throws IOFailure If there is a problem with the connection.
     * @throws ArgumentNotValid If the checksum replica channel is null.
     */
    public static ChecksumClient getInstance(ChannelID theCRin)
            throws IOFailure, ArgumentNotValid {
        // validate arguments
        ArgumentNotValid.checkNotNull(theCRin, "ChannelID theCRin");

        // Create a new instance (no static instance, since it would prevent
        // multi checksum replica clients).
        return new ChecksumClient(theCRin);
    }

    /**
     * Method for sending correct messages to the replica. This CorrectMessage
     * is used to correct a bad entry in the archive.
     * 
     * @param msg The CorrectMessage to send to the replica.
     * @throws ArgumentNotValid If the CorrectMessage is null.
     */
    public void sendCorrectMessage(CorrectMessage msg) throws ArgumentNotValid {
        ArgumentNotValid.checkNotNull(msg, "CorrectMessage msg");
        
        // send the message to the archive.
        jmsCon.resend(msg, theChecksumChannel);
        
        // log the message.
        log.debug("Resending CorrectMessage: " + msg.toString() + "'.");
    }

    /**
     * Method for sending a GetAllFilenamesMessage to a checksum archive.
     * 
     * @param msg The GetAllFilenamesMessage, which will be send through the 
     * jms connection to the checksum archive.
     * @throws ArgumentNotValid If the GetAllFilenamesMessage is null.
     */
    public void sendGetAllFilenamesMessage(GetAllFilenamesMessage msg) 
            throws ArgumentNotValid {
        ArgumentNotValid.checkNotNull(msg, "GetAllFilenamesMessage msg");
        // send the message to the archive.
        jmsCon.resend(msg, theChecksumChannel);

        // log message.
        log.debug("Resending GetAllFilenamesMessage: '" + msg.toString() 
                + "'.");
    }

    /**
     * Method for sending the GetAllChecksumMessage to the ChecksumReplica.
     * 
     * @param msg The GetAllChecksumMessage, which will be sent through the jms
     * connection to the checksum archive.
     * @throws ArgumentNotValid If the GetAllChecksumsMessage is null.
     */
    public void sendGetAllChecksumsMessage(GetAllChecksumsMessage msg) 
            throws ArgumentNotValid {
        ArgumentNotValid.checkNotNull(msg, "GetAllChecksumsMessage msg");
        // send the message to the archive.
        jmsCon.resend(msg, theChecksumChannel);

        // log message.
        log.debug("Sending GetAllChecksumMessage: '" + msg.toString() + "'.");
    }

    /**
     * Method for retrieving the checksum of a specific arcfile within the
     * archive.
     * 
     * @param msg The GetChecksumMessage which will be sent to the checksum
     * archive though the jms connection.
     */
    public void sendGetChecksumMessage(GetChecksumMessage msg) {
        // Validate arguments
        ArgumentNotValid.checkNotNull(msg, "GetChecksumMessage msg");

        jmsCon.resend(msg, theChecksumChannel);

        // log what we are doing.
        log.debug("Sending GetChecksumMessage: '" + msg.toString() + "'.");
    }

    /**
     * Method for retrieving the checksum of a specific arcfile within the
     * archive.
     * 
     * @param replyChannel The channel where the reply should be sent.
     * @param filename The GetChecksumMessage which has been sent to the 
     * checksum archive though the jms connection.
     * @return The GetChecksumMessage which is sent.
     * @throws ArgumentNotValid If the reply channel is null or if the filename
     * is either null or the empty string.
     */
    public GetChecksumMessage sendGetChecksumMessage(ChannelID replyChannel, 
            String filename) throws ArgumentNotValid {
        // Validate arguments
        ArgumentNotValid.checkNotNull(replyChannel, "ChannelID replyChannel");
        ArgumentNotValid.checkNotNullOrEmpty(filename, "String filename");

        // make GetChecksumMessage for the specific file.
        GetChecksumMessage msg = new GetChecksumMessage(theChecksumChannel, 
                replyChannel, filename, replicaId);
        jmsCon.send(msg);

        // log what we are doing.
        log.debug("Sending GetChecksumMessage: '" + msg.toString() + "'.");
        
        return msg;
    }

    /**
     * Method for retrieving the type of replica. In this case the replica is
     * a checksum replica.
     * 
     * @return The type of this replica, in this case Checksum replica.
     */
    public ReplicaType getType() {
        return ReplicaType.CHECKSUM;
    }

    /**
     * Method for uploading a file to the archive. This is only uploaded to one
     * of the archives, not all of them. Thus using the 'any' channel.
     * 
     * @param rf The file to upload to the archive.
     * @throws ArgumentNotValid If the remote file is null.
     */
    public void sendUploadMessage(RemoteFile rf) throws ArgumentNotValid {
        // validate arguments.
        ArgumentNotValid.checkNotNull(rf, "RemoteFile rf");

        // create and send message.
        UploadMessage up = new UploadMessage(theChecksumChannel, 
                Channels.getTheRepos(), rf);
        jmsCon.send(up);

        // log message
        log.debug("Sending upload message '" + up.toString() + "'.");
    }

    /**
     * Method for sending batch job messages to the replica.
     * This is not allowed since this archive at the end of this client is a 
     * checksum archive, which cannot handle batch jobs.
     * 
     * @param replyChannel The channel where the reply should be sent.
     * @param job The batchjob to execute.
     * @return Nothing. It always throws an exception, since it is not allowed
     * to run batchjobs on a checksum archive.
     * @throws IllegalState Always. Since it is not legal to send a batchjob to
     * a checksum replica.
     * @throws ArgumentNotValid If the channel or the batchjob is null.
     */
    public BatchMessage sendBatchJob(ChannelID replyChannel, FileBatchJob job) 
            throws IllegalState, ArgumentNotValid {
        ArgumentNotValid.checkNotNull(replyChannel, "ChannelID replyChannel");
        ArgumentNotValid.checkNotNull(job, "FileBatchJob job");
        
        String errMsg = "Trying to execute the batchjob '" 
            + job.getClass().getName() + "' on a checksum replica with reply "
            + "channel '" + replyChannel + "'. This is not allowed!"; 
        log.error(errMsg);
        throw new IllegalState(errMsg);
    }

    /**
     * Method for sending batch job messages to the replica.
     * This is not allowed since this archive at the end of this client is a 
     * checksum archive, which cannot handle batch jobs.
     * 
     * @param msg The batch message.
     * @return Nothing. It always throws an exception, since it is not allowed
     * to run batchjobs on a checksum archive.
     * @throws IllegalState Always. Since it is not legal to send a batchjob to
     * a checksum replica.
     * @throws ArgumentNotValid If the message is null.
     */
    public BatchMessage sendBatchJob(BatchMessage msg) throws IllegalState, 
            ArgumentNotValid {
        ArgumentNotValid.checkNotNull(msg, "BatchMessage msg");
        
        String errMsg = "Trying to execute the batchjob '" + msg.toString()
                + "' on a checksum replica."; 
        log.error(errMsg);
        throw new IllegalState(errMsg);
    }

    /**
     * This method is intended to retrieve a record from an arc-file within the
     * archive. But since this handles checksum archive, it does not have the
     * actual arc-files, and this function should therefore fail.
     * 
     * @param msg The GetMessage for retrieving the arc-record from the archive.
     * @throws IllegalState Always. Since checksum replicas cannot handle this 
     * kind of messages.
     * @throws ArgumentNotValid If the message is null.
     */
    public void sendGetMessage(GetMessage msg) throws IllegalState, 
            ArgumentNotValid {
        ArgumentNotValid.checkNotNull(msg, "GetMessage msg");
        
        String errMsg = "A checksum replica cannot handle a GetMessage such "
            + "as '" + msg + "'";
        log.error(errMsg);
        throw new IllegalState(errMsg);
    }

    /**
     * This method is intended to retrieve an arc-file from the archive. 
     * But since this handles checksum archive, it does not have the
     * actual arc-files, and this function should therefore fail.
     * 
     * @param gfm The GetFileMessage for retrieving the arc-file from the 
     * archive.
     * @throws IllegalState Always. Since checksum replicas cannot handle this 
     * kind of messages.
     * @throws ArgumentNotValid If the message is null.
     */
    public void sendGetFileMessage(GetFileMessage gfm) throws IllegalState, 
            ArgumentNotValid {
        ArgumentNotValid.checkNotNull(gfm, "GetFileMessage gfm");
        
        String errMsg = "A checksum replica cannot handle a GetFileMessage "
            + "such as '" + gfm + "'.";
        log.error(errMsg);
        throw new IllegalState(errMsg);
    }

    /**
     * This method is intended to retrieve an arc-file from the archive. 
     * But since this handles checksum archive, it does not have the
     * actual arc-files, and this function should therefore fail.
     * 
     * @param msg The RemoveAndGetFileMessage for removing and retrieving an 
     * arc-file from the archive.
     * @throws IllegalState Always. Since checksum replicas cannot handle this 
     * kind of messages.
     * @throws ArgumentNotValid If the message is null.
     */
    public void sendRemoveAndGetFileMessage(RemoveAndGetFileMessage msg) 
            throws IllegalState, ArgumentNotValid {
        ArgumentNotValid.checkNotNull(msg, "RemoveAndGetFileMessage msg");
        
        String errMsg = "A checksum replica cannot handle a "
            + "RemoveAndGetFileMessage such as '" + msg + "'.";
        log.error(errMsg);
        throw new IllegalState(errMsg);
    }

    /**
     * Method for closing this instance.
     */
    public void close() {
        log.debug("The ChecksumClient for replica '" + replicaId 
                + "' has been shut down.");
    }
}
