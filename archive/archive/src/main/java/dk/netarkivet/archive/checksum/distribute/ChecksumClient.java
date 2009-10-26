/* File:    $Id$
 * Version: $Revision$
 * Date:    $Date$
 * Author:  $Author$
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
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
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
import dk.netarkivet.common.exceptions.NotImplementedException;
import dk.netarkivet.common.utils.batch.FileBatchJob;

/**
 * Proxy for remote checksum archive. Establishes the jms connection to the
 * remote checksum archive.
 * 
 * Can be used in combination with any type of ChecksumServerAPI.
 */
public class ChecksumClient implements ReplicaClient {
    // Each message is assigned a message id
    protected static final Log log = LogFactory.getLog(ChecksumClient.class);

    // The instance.
    private static ChecksumClient instance;

    // Connection to JMS provider
    private JMSConnection jmsCon;

    /** 
     * Connection information.
     * The connection to contact all checksum archives.
     */
    private ChannelID theCR;

    /**
     * The constructor. Cannot be used directly, use getInstance instead.
     * 
     * @param theCRin
     * The channel for contacting the checksum archive.
     * @throws IOFailure
     * If there is a problem with the connection.
     */
    private ChecksumClient(ChannelID theCRin) throws IOFailure {
        this.theCR = theCRin;
        jmsCon = JMSConnectionFactory.getInstance();
    }

    /**
     * The method for retrieving the invoked the instance. If not invoked yet,
     * then invoke.
     * 
     * @param theCRin
     * The channel for contacting the checksum archive.
     * @return The instance.
     * @throws IOFailure
     * If there is a problem with the connection.
     */
    public static ChecksumClient getInstance(ChannelID theCRin)
            throws IOFailure {
        // validate arguments
        ArgumentNotValid.checkNotNull(theCRin, "ChannelID theCRin");

        // Create instance if not created already.
        if (instance == null) {
            instance = new ChecksumClient(theCRin);
        }
        return instance;
    }

    /**
     * Method for correcting a entity in the archive. If the entry in the 
     * archive has the incorrect checksum, then it will be removed and the
     * remote arcfile will be used to replace it.
     * The old 'wrong' entry should not be thrown away, it should be placed in
     * a container for the incorrect entries.
     * 
     * @param arcfile
     * The RemoteFile which should correct the current one in the
     * archive, which is wrong.
     * @param checksum The checksum of the 'wrong' entry to validate that it is
     * wrong.
     */
    public void correct(RemoteFile arcfile, String checksum) {
        // validate argument
        ArgumentNotValid.checkNotNull(arcfile, "RemoteFile arcfile");

        // create and send message.
        CorrectMessage cmsg = new CorrectMessage(theCR, Channels.getTheRepos(),
                checksum, arcfile);
        jmsCon.send(cmsg);

        // log that the message has been sent.
        log.debug("\nSending correct message: \n" + cmsg.toString());
    }

    /**
     * Method for sending a GetAllFilenamesMessage to a checksum archive.
     * 
     * @param msg
     * The GetAllFilenamesMessage, which will be send through the jms
     * connection to the checksum archive.
     */
    public void getAllFilenames(GetAllFilenamesMessage msg) {
        ArgumentNotValid.checkNotNull(msg, "msg");
        // send the message to the archive.
        jmsCon.resend(msg, theCR);

        // log message.
        log.debug("\nResending GetAllFilenamesMessage:\n " + msg.toString());
    }

    /**
     * Method for sending the GetAllChecksumMessage to the ChecksumReplica.
     * 
     * @param msg The GetAllChecksumMessage, which will be sent through the jms
     * connection to the checksum archive.
     */
    public void getAllChecksums(GetAllChecksumsMessage msg) {
        ArgumentNotValid.checkNotNull(msg, "msg");
        // send the message to the archive.
        jmsCon.resend(msg, theCR);

        // log message.
        log.debug("\nSending GetAllChecksumMessage:\n " + msg.toString());
    }

    /**
     * Method for retrieving the checksum of a specific arcfile within the
     * archive.
     * 
     * @param msg The GetChecksumMessage which will be sent to the checksum
     * archive though the jms connection.
     */
    public void getChecksum(GetChecksumMessage msg) {
        // Validate arguments
        ArgumentNotValid.checkNotNull(msg, "msg");

        jmsCon.resend(msg, theCR);

        // log what we are doing.
        log.debug("\nSending GetChecksumMessage: \n" + msg.toString());
    }

    /**
     * Method for retrieving the checksum of a specific arcfile within the
     * archive.
     * 
     * @param replyChannel The channel where the reply should be sent.
     * @param filename The GetChecksumMessage which has been sent to the 
     * checksum archive though the jms connection.
     * @return The GetChecksumMessage which is sent.
     */
    public GetChecksumMessage getChecksum(ChannelID replyChannel, 
            String filename) {
        // Validate arguments
        ArgumentNotValid.checkNotNull(replyChannel, "ChannelID replyChannel");
        ArgumentNotValid.checkNotNullOrEmpty(filename, "String filename");

        // TODO make method for not having the replica id.
        GetChecksumMessage msg = new GetChecksumMessage(theCR, replyChannel, 
                filename, "No replicaId is needed. This message is sent "
                + "directly to the checksum archive.");
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
    public void upload(RemoteFile rf) throws ArgumentNotValid {
        // validate arguments.
        ArgumentNotValid.checkNotNull(rf, "rf");

        // create and send message.
        UploadMessage up = new UploadMessage(theCR, Channels.getTheRepos(), rf);
        jmsCon.send(up);

        // log message
        log.debug("Sending upload message\n" + up.toString());
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
    public BatchMessage batch(ChannelID replyChannel, FileBatchJob job) 
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
    public BatchMessage batch(BatchMessage msg) throws IllegalState, 
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
    public void get(GetMessage msg) throws IllegalState, ArgumentNotValid {
        ArgumentNotValid.checkNotNull(msg, "GetMessage msg");
        
        String errMsg = "The GetMessage '" + msg + "' cannot be sent to checksum"
                + " replica.";
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
    public void getFile(GetFileMessage gfm) throws IllegalState, 
            ArgumentNotValid {
        ArgumentNotValid.checkNotNull(gfm, "GetFileMessage gfm");
        
        String errMsg = "The GetFileMessage '" + gfm + "' cannot be sent to "
                + "checksum replica.";
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
    public void removeAndGetFile(RemoveAndGetFileMessage msg) 
            throws IllegalState, ArgumentNotValid {
        ArgumentNotValid.checkNotNull(msg, "RemoveAndGetFileMessage msg");
        
        String errMsg = "The RemoveAndGetFileMessage '" + msg + "' cannot be "
                + "handled by a checksum replica.";
        log.error(errMsg);
        throw new IllegalState(errMsg);
    }

    /**
     * Method for closing this instance.
     */
    public void close() {
        if (instance != null) {
            instance = null;
        }
    }
}
