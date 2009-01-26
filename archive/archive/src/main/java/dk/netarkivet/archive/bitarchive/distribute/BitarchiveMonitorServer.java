/* File:        $Id$
 * Date:        $Date$
 * Revision:    $Revision$
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

import java.util.Observable;
import java.util.Observer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.archive.bitarchive.BitarchiveMonitor;
import dk.netarkivet.archive.distribute.ArchiveMessageHandler;
import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.distribute.Channels;
import dk.netarkivet.common.distribute.JMSConnection;
import dk.netarkivet.common.distribute.JMSConnectionFactory;
import dk.netarkivet.common.distribute.RemoteFile;
import dk.netarkivet.common.distribute.RemoteFileFactory;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.CleanupIF;
import dk.netarkivet.common.utils.Settings;

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
     * Creates an instance of a BitarchiveMonitorServer.
     *
     * @throws IOFailure - if an error with the JMSConnection occurs
     */
    protected BitarchiveMonitorServer() throws IOFailure {
        bamon = new BitarchiveMonitor();
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
            bamon.registerBatch(inbMsg.getID(), inbMsg.getReplyTo(),
                        outbMsg.getID());
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
     * @param beMsg
     */
    public void visit(final BatchEndedMessage beMsg) {
        log.info("Received batch ended from bitarchive '"
                 + beMsg.getBitarchiveID() + "': " + beMsg);
        bamon.signOfLife(beMsg.getBitarchiveID());
        try {
            new Thread() {
                public void run() {
                    bamon.bitarchiveReply(beMsg.getOriginatingBatchMsgID(),
                                          beMsg.getBitarchiveID(),
                                          beMsg.getNoOfFilesProcessed(),
                                          beMsg.getFilesFailed(),
                                          beMsg.getRemoteFile(),
                                          beMsg.isOk() ? null :
                                          beMsg.getErrMsg(),
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
                doBatchReply((BitarchiveMonitor.BatchJobStatus) arg);
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
        BatchReplyMessage brMsg =
                new BatchReplyMessage(bjs.originalRequestReplyTo,
                                      Channels.getTheBamon(),
                                      bjs.originalRequestID,
                                      bjs.noOfFilesProcessed,
                                      bjs.filesFailed, resultsFile);
        if (bjs.errMsg != null) {
            brMsg.setNotOk(bjs.errMsg);
        }
        con.send(brMsg);

        log.info("BatchReplyMessage: '" + brMsg
                 + "' sent from BA monitor to queue: '"
                 + brMsg.getTo()
                 + "'");
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
            instance = null;
            if (bamon != null) {
                bamon.cleanup();
                bamon = null;
            }
        }
    }
}
