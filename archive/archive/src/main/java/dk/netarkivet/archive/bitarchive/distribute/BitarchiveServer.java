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
import java.util.Timer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.archive.ArchiveSettings;
import dk.netarkivet.archive.bitarchive.Bitarchive;
import dk.netarkivet.archive.bitarchive.BitarchiveAdmin;
import dk.netarkivet.archive.distribute.ArchiveMessageHandler;
import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.distribute.ChannelID;
import dk.netarkivet.common.distribute.Channels;
import dk.netarkivet.common.distribute.JMSConnection;
import dk.netarkivet.common.distribute.JMSConnectionFactory;
import dk.netarkivet.common.distribute.NullRemoteFile;
import dk.netarkivet.common.distribute.arcrepository.BatchStatus;
import dk.netarkivet.common.distribute.arcrepository.BitarchiveRecord;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.PermissionDenied;
import dk.netarkivet.common.exceptions.UnknownID;
import dk.netarkivet.common.utils.CleanupIF;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.MD5;
import dk.netarkivet.common.utils.NotificationsFactory;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.utils.SystemUtils;


/**
 * Bitarchive container responsible for processing the different classes of
 * message which can be received by a bitarchive and returning appropriate data.
 *
 */
public class BitarchiveServer extends ArchiveMessageHandler implements
        CleanupIF {

    /**
     * The bitarchive serviced by this server.
     */
    private Bitarchive ba;

    /**
     * The admin data for the bit archive.
     */
    private BitarchiveAdmin baa;

    /**
     * The unique instance of this class.
     */
    private static BitarchiveServer instance;

    /**
     * the jms connection.
     */
    private JMSConnection con;

    /**
     * The logger used by this class.
     */
    private static final Log log
            = LogFactory.getLog(BitarchiveServer.class.getName());

    /**
     * the thread which sends heartbeat messages from this bitarchive to its
     * BitarchiveMonitorServer.
     */
    private HeartBeatSender heartBeatSender;

    /**
     * the unique id of this application.
     */
    private String bitarchiveAppId;

    /**
     * Channel to listen on for get/batch/correct.
     */
    private ChannelID allBa;
    /**
     * Topic to listen on for store.
     */
    private ChannelID anyBa;
    /**
     * Channel to send BatchEnded messages to when replying.
     */
    private ChannelID baMon;

    /**
     * Returns the unique instance of this class
     * The server creates an instance of the bitarchive it provides access to
     * and starts to listen to JMS messages on the incomming jms queue
     * <p/>
     * Also, heartbeats are sent out at regular intervals to the Bitarchive
     * Monitor, to tell that this bitarchive is alive.
     *
     * @return the instance
     * @throws UnknownID        - if there was no heartbeat frequency defined in
     *                          settings
     * @throws ArgumentNotValid - if the heartbeat frequency in settings is
     *                          invalid or either argument is null
     */
    public static synchronized BitarchiveServer getInstance() {
        if (instance == null) {
            instance = new BitarchiveServer();
        }
        return instance;
    }

    /**
     * The server creates an instance of the bitarchive it provides access to
     * and starts to listen to JMS messages on the incomming jms queue
     * <p/>
     * Also, heartbeats are sent out at regular intervals to the Bitarchive
     * Monitor, to tell that this bitarchive is alive.
     *
     * @throws UnknownID        - if there was no heartbeat frequency or temp
     *                            dir defined in settings or if the
     *                            bitarchiveid cannot be created.
     * @throws PermissionDenied - if the temporary directory or the file
     *                            directory cannot be written
     */
    private BitarchiveServer()
            throws UnknownID, PermissionDenied {
        boolean listening = false; // are we listening to queue ANY_BA
        File serverdir = FileUtils.getTempDir();
        if (!serverdir.exists()) {
            serverdir.mkdirs();
        }
        if (!serverdir.canWrite()) {
            throw new PermissionDenied(
                    "Not allowed to write to temp directory '"
                    + serverdir + "'");
        }
        log.info("Storing temporary files at '" + serverdir.getPath() + "'");

        bitarchiveAppId = createBitarchiveAppId();

        allBa = Channels.getAllBa();
        anyBa = Channels.getAnyBa();
        baMon = Channels.getTheBamon();
        ba = Bitarchive.getInstance();
        con = JMSConnectionFactory.getInstance();
        con.setListener(allBa, this);
        baa = BitarchiveAdmin.getInstance();
        if (baa.hasEnoughSpace()) {
            con.setListener(anyBa, this);
            listening = true;
        } else {
            log.warn("Not enough space to guarantee store -- not listening "
                    + "to " + anyBa.getName());
        }

        // Create and start the heartbeat sender
        Timer timer = new Timer(true);
        heartBeatSender = new HeartBeatSender(baMon, this);
        long frequency
                = Settings.getLong(
                ArchiveSettings.BITARCHIVE_HEARTBEAT_FREQUENCY);
        timer.scheduleAtFixedRate(heartBeatSender, 0, frequency);
        log.info("Heartbeat frequency: '" + frequency + "'");
        // Next logentry depends on whether we are listening to ANY_BA or not
        String logmsg = "Created bitarchive server listening on: "
            + allBa.getName();
        if (listening) {
            logmsg += " and " + anyBa.getName();
        }

        log.info(logmsg);

        log.info("Broadcasting heartbeats on: " + baMon.getName());
    }

    /**
     * Ends the heartbeat sender before next loop and removes the
     * server as listener on allBa and anyBa. Clsoes bitarchive.
     */
    public synchronized void close() {
        log.info("BitarchiveServer " + getBitarchiveAppId() + " closing down");
        cleanup();
        if (con != null) {
            con.removeListener(allBa, this);
            con.removeListener(anyBa, this);
            con = null;
        }
        log.info("BitarchiveServer " + getBitarchiveAppId() + " closed down");
    }

    /**
     * Ends the heartbeat sender before next loop.
     */
    public void cleanup() {
        if (ba != null) {
            ba.close();
            ba = null;
        }
        if (baa != null) {
            baa.close();
            baa = null;
        }
        if (heartBeatSender != null) {
            heartBeatSender.cancel();
            heartBeatSender = null;
        }
        instance = null;
    }

    /**
     * Process a get request and send the result back to the client. If the
     * arcfile is not found on this bitarchive machine, nothing happens.
     *
     * @param msg a container for upload request
     */
    public void visit(GetMessage msg) {
        BitarchiveRecord bar;
        log.trace("Processing getMessage(" + msg.getArcFile() + ":"
                + msg.getIndex() + ").");
        try {
            bar = ba.get(msg.getArcFile(), msg.getIndex());            
        } catch (Throwable e) {
            log.warn("Error while processing get message '" + msg + "'", e);
            msg.setNotOk(e);
            con.reply(msg);
            return;
        }
        if (bar != null) {
            msg.setRecord(bar);
            log.debug("Sending reply: " + msg.toString());
            con.reply(msg);
        } else {
            log.trace("Record(" + msg.getArcFile() + ":" + msg.getIndex() 
                    + "). not found on this BitarchiveServer");
        }
    }


    /**
     * Process a upload request and send the result back to the client.
     * This may be a very time consuming process and is a blocking call.
     *
     * @param msg a container for upload request
     */
    public void visit(UploadMessage msg) {
        // TODO Implement a thread-safe solution on resource level rather than
        // message processor level.
        try {
            try {
                synchronized (this) {
                    // Important when two identical files are uploaded
                    // simultanously.
                    ba.upload(msg.getRemoteFile(), msg.getArcfileName());
                }
            } catch (Throwable e) {
                log.warn("Error while processing upload message '" + msg + "'",
                         e);
                msg.setNotOk(e);
            }
            // Stop listening if disk is now full
            finally {
                if (!baa.hasEnoughSpace()) {
                    log.warn("Cannot guarantee enough space, no longer "
                            + "listening to " + anyBa.getName() + "for uploads");
                    con.removeListener(anyBa, this);
                }
            }
        } catch (Throwable e) {
            //This block will be executed if the above finally block throws an
            //exception. Therefore the message is not set to notOk here
            log.warn("Error while removing listener after upload message '"
                     + msg + "'", e);
        } finally {
            log.info("Sending reply: " + msg.toString());
            con.reply(msg);
        }
    }

    /**
     * Removes an arcfile from the bitarchive and returns
     * the removed file as an remotefile.
     *
     * Answers OK if the file is actually removed.
     * Answers notOk if the file exists with wrong checksum or wrong credentials
     * Doesn't answer if the file doesn't exist.
     *
     * This method always generates a warning when deleting a file.
     *
     * Before the file is removed it is verified that
     * - the file exists in the bitarchive
     * - the file has the correct checksum
     * - the supplied credentials are correct
     * @param msg a container for remove request
     */
    public void visit(RemoveAndGetFileMessage msg) {
        String mesg = "Request to move file '" + msg.getArcfileName()
                      + "' with checksum '" + msg.getCheckSum() + "' to attic";
        log.warn(mesg);
        NotificationsFactory.getInstance().errorEvent(mesg);

        File foundFile = ba.getFile(msg.getArcfileName());
        // Only send an reply if the file was found
        if (foundFile == null) {
            log.warn("Remove: '" + msg.getArcfileName() + "' not found");
            return;
        }

        try {

            log.debug("File located - now checking the credentials");
            // Check credentials
            String credentialsReceived = msg.getCredentials();
            ArgumentNotValid.checkNotNullOrEmpty(credentialsReceived,
                    "credentialsReceived");
            if (!credentialsReceived.equals(Settings.get(
                    ArchiveSettings.ENVIRONMENT_THIS_CREDENTIALS))) {
                String message = "Attempt to remove '" + foundFile
                        + "' with wrong credentials!";
                log.warn(message);
                msg.setNotOk(message);
                return;
            }

            log.debug("Credentials accepted, now checking the checksum");

            String checksum = MD5.generateMD5onFile(foundFile);

            if (!checksum.equals(msg.getCheckSum())) {
                final String message =
                        "Attempt to remove '" + foundFile
                        + " failed due to checksum mismatch: "
                        + msg.getCheckSum() + " != " + checksum;
                log.warn(message);
                msg.setNotOk(message);
                return;
            }

            log.debug("Checksums matched - preparing to move and return file");
            File moveTo = baa.getAtticPath(foundFile);
            if (!foundFile.renameTo(moveTo)) {
                final String message = "Failed to move the file:" + foundFile
                        + "to attic";
                log.warn(message);
                msg.setNotOk(message);
                return;
            }
            msg.setFile(moveTo);

            log.warn("Removed file '" + msg.getArcfileName()
                        + "' with checksum '"
                    + msg.getCheckSum() + "'");
        } catch (Exception e) {
            final String message = "Error while processing message '"
                                   + msg + "'";
            log.warn(message, e);
            msg.setNotOk(e);
        } finally {
            con.reply(msg);
        }
    }


    /**
     * Process a batch job and send the result back to the client.
     *
     * @param msg a container for batch jobs
     */
    public void visit(final BatchMessage msg) {
        Thread batchThread = new Thread("Batch-" + msg.getID()) {
            public void run() {
                try {
                    // TODO Possibly tell batch something that will let
                    //  it create more comprehensible file names.
                    //Run the batch job on all files on this machine
                    BatchStatus batchStatus = ba.batch(bitarchiveAppId,
                                                       msg.getJob());

                    // Create the message which will contain the reply
                    BatchEndedMessage resultMessage
                            = new BatchEndedMessage(baMon, msg.getID(),
                                                    batchStatus);

                    //Update informational fields in reply message
                    if (batchStatus.getFilesFailed().size() > 0) {
                        resultMessage.setNotOk(
                                "Batch job failed on "
                                + batchStatus.getFilesFailed().size()
                                + " files.");
                    }


                    //Send the reply
                    con.send(resultMessage);
                    log.debug("Submitted result message for batch job:"
                             + msg.getID());
                } catch (Throwable e) {
                    log.warn("Batch processing failed for message '"
                            + msg + "'", e);
                    BatchEndedMessage failMessage
                            = new BatchEndedMessage(
                                    baMon, bitarchiveAppId,
                                    msg.getID(), new NullRemoteFile());
                    failMessage.setNotOk(e);
                    con.send(failMessage);
                    log.debug("Submitted failure message for batch job:"
                             + msg.getID());
                }
            }
        };
        batchThread.start();
    }

    /**
     * Process a getFile request and send the result back to the client.
     *
     * @param msg a container for a getfile request
     */
    public void visit(GetFileMessage msg) {
        try {
            File foundFile = ba.getFile(msg.getArcfileName());
            // Only send an reply if the file was found
            if (foundFile != null) {
                msg.setFile(foundFile);
                log.info("Sending reply: " + msg.toString());
                con.reply(msg);
            }
        } catch (Throwable e) {
            log.warn("Error while processing get file message '" + msg + "'",
                     e);
        }
    }


    /**
     * Returns a String that identifies this bit archive application
     * (within the bit archive, i.e. either with id ONE or TWO)
     *
     * @return String with IP address of this host and, if specified, the
     *         APPLICATION_INSTANCE_ID from settings
     */
    public String getBitarchiveAppId() {
        return bitarchiveAppId;
    }


    /**
     * Returns a String that identifies this bit archive application
     * (within the bit archive, i.e. either with id ONE or TWO).
     * The string has the following form: hostaddress[_applicationinstanceid]
     * fx. "10.0.0.1_appOne" or just "10.0.0.1", if no applicationinstanceid
     * has been chosen. 
     *
     * @return String with IP address of this host and, if specified, the
     *         APPLICATION_INSTANCE_ID from settings
     * @throws UnknownID - if InetAddress.getLocalHost() failed
     */
    private String createBitarchiveAppId() throws UnknownID {
        String id;

        // Create an id with the IP address of this current host
        id = SystemUtils.getLocalIP();

        // Append an underscore and APPLICATION_INSTANCE_ID from settings
        // to the id, if specified in settings.
        // If no APPLICATION_INSTANCE_ID is found do nothing.
        try {
            String applicationInstanceId = Settings.get(
                    CommonSettings.APPLICATION_INSTANCE_ID);
            if (!applicationInstanceId.isEmpty()) {
                id += "_" + applicationInstanceId;
            }
        } catch (UnknownID e) {
            // Ignore the fact, that there is no APPLICATION_INSTANCE_ID in
            // settings
            log.warn("No setting APPLICATION_INSTANCE_ID found in settings");
        }

        return id;
    }
}
