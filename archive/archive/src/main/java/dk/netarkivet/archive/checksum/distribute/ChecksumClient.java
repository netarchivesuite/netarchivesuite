/* File:    $Id$
 * Version: $Revision$
 * Date:    $Date$
 * Author:  $Author$
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
    protected static final Log log = LogFactory.getLog(ChecksumClient.class
            .getName());

    // The instance.
    private static ChecksumClient instance;

    // Connection to JMS provider
    private JMSConnection jmsCon;

    // connection information
    // The connection to contact all checksum archives.
    private ChannelID the_CR;
    // The client.
    private ChannelID clientId = Channels.getTheRepos();

    /**
     * The constructor. Cannot be used directly, use getInstance instead.
     * 
     * @param all_cs_in
     * The channel for contacting all checksum archives.
     * @param any_cs_in
     * The channel for contacting any checksum archive.
     * @throws IOFailure
     * If there is a problem with the connection.
     */
    private ChecksumClient(ChannelID the_CR_in) throws IOFailure {
        this.the_CR = the_CR_in;
        jmsCon = JMSConnectionFactory.getInstance();
    }

    /**
     * The method for retrieving the invoked the instance. If not invoked yet,
     * then invoke.
     * 
     * @param all_cs_in
     * The channel for contacting all checksum archives.
     * @param any_cs_in
     * The channel for contacting any checksum archive.
     * @return The instance.
     * @throws IOFailure
     * If there is a problem with the connection.
     */
    public static ChecksumClient getInstance(ChannelID the_CR_in)
            throws IOFailure {
        // validate arguments
        ArgumentNotValid.checkNotNull(the_CR_in, "ChannelID all_cs_in");

        // Create instance if not created already.
        if (instance == null) {
            instance = new ChecksumClient(the_CR_in);
        }
        return instance;
    }

    /**
     * Method for correcting a entity in the archive. This will remove the old
     * entry and upload the arcfile as the new.
     * 
     * @param arcfile
     * The RemoteFile which should correct the current one in the
     * archive, which is wrong.
     */
    @Override
    public void correct(RemoteFile arcfile, String checksum) {
        // validate argument
        ArgumentNotValid.checkNotNull(arcfile, "RemoteFile arcfile");

        // create and send message.
        CorrectMessage cmsg = new CorrectMessage(the_CR, clientId, checksum,
                arcfile);
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
    @Override
    public void getAllFilenames(GetAllFilenamesMessage msg) {
        ArgumentNotValid.checkNotNull(msg, "msg");
        // send the message to the archive.
        jmsCon.resend(msg, the_CR);

        // log message.
        log.debug("\nResending GetAllFilenamesMessage:\n " + msg.toString());
    }

    /**
     * Method for sending the GetAllChecksumMessage to the ChecksumReplica.
     * 
     * @param msg The GetAllChecksumMessage, which will be sent though the jms
     * connection to the checksum archive.
     */
    public void getAllChecksums(GetAllChecksumMessage msg) {
        ArgumentNotValid.checkNotNull(msg, "msg");
        // send the message to the archive.
        jmsCon.resend(msg, the_CR);

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
    @Override
    public void getChecksum(GetChecksumMessage msg) {
        // Validate arguments
        ArgumentNotValid.checkNotNull(msg, "msg");

        jmsCon.resend(msg, the_CR);

        // log what we are doing.
        log.debug("\nSending GetChecksumMessage: \n" + msg.toString());
    }

    /**
     * Method for retrieving the checksum of a specific arcfile within the
     * archive.
     * 
     * @param msg The GetChecksumMessage which will be sent to the checksum
     * archive though the jms connection.
     */
    @Override
    public GetChecksumMessage getChecksum(ChannelID replyChannel, 
            String filename) {
        // Validate arguments
        ArgumentNotValid.checkNotNull(replyChannel, "ChannelID replyChannel");
        ArgumentNotValid.checkNotNullOrEmpty(filename, "String filename");

        GetChecksumMessage msg = new GetChecksumMessage(the_CR, replyChannel, 
                filename);
        jmsCon.send(msg);

        // log what we are doing.
        log.debug("\nSending GetChecksumMessage: \n" + msg.toString());
        
        return msg;
    }

    /**
     * Method for retrieving the type of replica.
     * 
     * @return The type of this replica.
     */
    @Override
    public ReplicaType getType() {
        // Returns the current replica type in the settings.
        return ReplicaType.CHECKSUM;
    }

    /**
     * Method for uploading a file to the archive. This is only uploaded to one
     * of the archives, not all of them. Thus using the 'any' channel.
     * 
     * @param rf
     * The file to upload to the archive.
     */
    @Override
    public void upload(RemoteFile rf) {
        // validate arguments.
        ArgumentNotValid.checkNotNull(rf, "rf");

        // create and send message.
        UploadMessage up = new UploadMessage(the_CR, clientId, rf);
        jmsCon.send(up);

        // log message
        log.debug("Sending upload message\n" + up.toString());
    }

    @Override
    public BatchMessage batch(ChannelID replyChannel, FileBatchJob job) {
        log.error("Trying to execute the batchjob '" + job.getClass().getName()
                + "' on a checksum replica with reply channel '" + replyChannel
                + "'.");
        // TODO Auto-generated method stub
        throw new NotImplementedException("Checksum replicas cannot handle "
                + "BatchJobs.");
    }

    @Override
    public BatchMessage batch(BatchMessage msg) {
        log.error("Trying to execute the batchjob '" + msg.toString()
                + "' on a checksum replica.");
        // TODO Auto-generated method stub
        throw new NotImplementedException("Checksum replicas cannot handle "
                + "BatchJobs.");
    }

    @Override
    public void get(GetMessage msg) {
        log.error("The GetMessage '" + msg + "' cannot be sent to checksum"
                + " replica.");
        // TODO Auto-generated method stub
        throw new NotImplementedException("Checksum replicas cannot handle "
                + "GetMessage.");
    }

    @Override
    public void getFile(GetFileMessage gfm) {
        log.error("The GetFileMessage '" + gfm + "' cannot be sent to checksum"
                + " replica.");
        // TODO Auto-generated method stub
        throw new NotImplementedException("Cannot retrieve a file from a"
                + "checksum replica.");
    }

    @Override
    public void removeAndGetFile(RemoveAndGetFileMessage msg) {
        log.error("The RemoveAndGetFileMessage '" + msg + "' cannot be sent "
                + "to checksum replica.");
        // TODO Auto-generated method stub
        throw new NotImplementedException("Cannot retrieve a file from a"
                + "checksum replica.");
    }

    @Override
    public void close() {
        // TODO Auto-generated method stub
        if (instance != null) {
            instance = null;
        }
    }
}
