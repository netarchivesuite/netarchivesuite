/* File:        $Id$
 * Revision:    $Revision$
 * Date:        $Date$
 * Author:      $Author$
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.common.distribute.ChannelID;
import dk.netarkivet.common.distribute.Channels;
import dk.netarkivet.common.distribute.JMSConnection;
import dk.netarkivet.common.distribute.JMSConnectionFactory;
import dk.netarkivet.common.distribute.RemoteFile;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.arc.FileBatchJob;


/**
 * Proxy for remote bitarchive.
 * Establishes a JMS connection to the remote bitarchive
 */
public class BitarchiveClient {
    // Each message is assigned a message id
    protected static final Log log
            = LogFactory.getLog(BitarchiveClient.class.getName());

    // Connection to JMS provider
    private JMSConnection con;

    // connection information
    private ChannelID all_ba;
    private ChannelID any_ba;
    private ChannelID the_bamon;
    private ChannelID clientId = Channels.getTheArcrepos();

    /**
     * Establish the connection to the server.
     *
     * @param all_ba_in topic to all bitarchives
     * @param any_ba_in queue to one of the bitarchives
     * @param the_bamon_in queue to the bitarchive monitor
     * @throws IOFailure If there is a problem making the connection.
     */
    private BitarchiveClient(ChannelID all_ba_in, ChannelID any_ba_in,
                             ChannelID the_bamon_in) throws IOFailure {
        this.all_ba = all_ba_in;
        this.any_ba = any_ba_in;
        this.the_bamon = the_bamon_in;
        con = JMSConnectionFactory.getInstance();
    }

    /**
     * Factory that establish the connection to the server.
     *
     * @param all_ba_in topic to all bitarchives
     * @param any_ba_in queue to one of the bitarchives
     * @param the_bamon_in queue to the bitarchive monitor
     * @return A BitarchiveClient
     * @throws IOFailure If there is a problem making the connection.
     */
    public static BitarchiveClient getInstance(ChannelID all_ba_in,
                                               ChannelID any_ba_in,
                                               ChannelID the_bamon_in) throws IOFailure {
        return new BitarchiveClient(all_ba_in, any_ba_in, the_bamon_in);
    }

    /**
     * Submit a get request to the bitarchive.
     *
     * @param arcfile The file containing the requested record
     * @param index   Offset of the ARC record in the file
     * @return The submitted message or null if an error occured
     */
    public GetMessage get(String arcfile, long index) {
        ArgumentNotValid.checkNotNullOrEmpty(arcfile, "arcfile");
        ArgumentNotValid.checkNotNegative(index, "index");

        // Create and send get message
        GetMessage msg = new GetMessage(all_ba, clientId, arcfile,
                index);
        con.send(msg);

        return msg;
    }

    /** Submit an already constructed batch message to the archive.
     * The reply goes directly back to whoever sent the message.
     *
     * @param msg the message to be processed by the get command.
     */
    public void get(GetMessage msg) {
        ArgumentNotValid.checkNotNull(msg, "msg");

        log.debug("Resending get message '" + msg + "' to bitarchives");

        JMSConnection con = JMSConnectionFactory.getInstance();
        try {
            con.resend(msg, Channels.getAllBa());
        } catch (Throwable e) {
            log.warn("Failure while resending " + msg, e);
            try {
                msg.setNotOk(e);
                con.reply(msg);
            } catch (Throwable e1) {
                log.warn("Failed to send error message back", e1);
            }
        }
    }

    /**
     * Submit an already constructed getfile message to the archive.
     *
     * @param msg get file message to retrieve
     */
    public void getFile(GetFileMessage msg) {
        ArgumentNotValid.checkNotNull(msg, "msg");
        log.debug("Resending get file message '" + msg + "' to bitarchives");
        con.resend(msg, this.all_ba);
    }

    /**
     * Forward the message to ALL_BA.
     * @param msg the message to forward
     */
    public void removeAndGetFile(RemoveAndGetFileMessage msg) {
        ArgumentNotValid.checkNotNull(msg, "msg");
        con.resend(msg, this.all_ba);
    }

    /**
     * Submit an upload request to the bitarchive.
     *
     * @param rf The file to upload
     * @throws IOFailure If access to file denied
     * @throws ArgumentNotValid    if arcfile is null
     */
    public void upload(RemoteFile rf) {
        ArgumentNotValid.checkNotNull(rf, "rf");
        UploadMessage up = new UploadMessage(any_ba, clientId, rf);
        log.debug("\nSending upload message\n" + up.toString());
        con.send(up);
    }

    /**
     * Submit an already constructed get message to the archive.
     * This is used by the ArcRepository when forwarding
     * batch jobs from its clients.
     *
     * @param bMsg a BatchMessage
     * @return The submitted message
     * @throws ArgumentNotValid if message is null.
     */
    public BatchMessage batch(BatchMessage bMsg) throws ArgumentNotValid {
        ArgumentNotValid.checkNotNull(bMsg, "bMsg");
        log.debug("Resending batch message '" + bMsg + "' to bitarchive" 
                  + " monitor");
        con.resend(bMsg, this.the_bamon);
        return bMsg;
    }

    /**
     * Submit a batch job to the archive.
     * This is used by the ArcRepository when it needs to run batch jobs
     * for its own reasons (i.e. when checksumming a file as part of
     * the Store operation.
     *
     * @param replyChannel The channel that the reply of this job should
     * be sent to.
     * @param job The job that should be run on the bit archive
     * handled by this client.
     * @return The submitted message
     * @throws ArgumentNotValid if any parameter was null.
     * @throws IOFailure if sending the batch message did not succeed.
     */
    public BatchMessage batch(ChannelID replyChannel,
                              FileBatchJob job)
            throws ArgumentNotValid, IOFailure {
        ArgumentNotValid.checkNotNull(replyChannel, "replyChannel");
        ArgumentNotValid.checkNotNull(job, "job");
        BatchMessage bMsg = new BatchMessage(this.the_bamon, replyChannel, job,
                "No value should be needed; this message was sent "
                + "directly to the bit archive.");
        con.send(bMsg);
        return bMsg;
    }

    /**
     * Release jms connections.
     */
    public void close() {
        log.debug("Client has been shutdown");
    }
}
