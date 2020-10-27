/*
 * #%L
 * Netarchivesuite - archive
 * %%
 * Copyright (C) 2005 - 2018 The Royal Danish Library, 
 *             the National Library of France and the Austrian National Library.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 2.1 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */
package dk.netarkivet.archive.bitarchive.distribute;

import java.util.TimerTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.distribute.ChannelID;
import dk.netarkivet.common.distribute.JMSConnection;
import dk.netarkivet.common.distribute.JMSConnectionFactory;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;

/**
 * Thread reponsible for sending out periodic HeartBeatMessages. The BitarchiveServer is closed down if any error occurs
 * whilst sending heartbeats.
 */
public class HeartBeatSender extends TimerTask {
    private static final Logger log = LoggerFactory.getLogger(HeartBeatSender.class);
    /** the receiver to receive heartbeats. */
    private ChannelID receiver;
    /** the BitarchiveServer of this HeartBeatSender. */
    private BitarchiveServer baServer;
    /** the id of the application sending the heartbeat message. */
    private String applicationId;
    /** the connection to use when sending heartbeats. */
    private JMSConnection con;

    /**
     * Constructs a HearBeatSender that sends heartbeats.
     *
     * @param inReceiver - the receiver to receive the heartbeats
     * @param inBaServer - the BitarchiveServer of this HeartBeatSender
     * @throws ArgumentNotValid - if in_baServer is null
     * @throws IOFailure - if getting an JMSConnection instance fails
     */
    public HeartBeatSender(ChannelID inReceiver, BitarchiveServer inBaServer) throws ArgumentNotValid, IOFailure {
        ArgumentNotValid.checkNotNull(inBaServer, "inBaServer");
        receiver = inReceiver;
        applicationId = inBaServer.getBitarchiveAppId();
        baServer = inBaServer;
        con = JMSConnectionFactory.getInstance();
    }

    /**
     * This is the run method of the thread sending heartbeats. The BitarchiveServer is closed down if any error occurs.
     */
    public void run() {
        try {
            con.send(new HeartBeatMessage(receiver, applicationId));
        } catch (Throwable t) {
            log.error("An unexpected error occurred. BitarchiveServer couldn't ping BitarchiveMonitorServer.", t);
        }
    }

    /**
     * Retrieval of a string representation of this instance.
     *
     * @return The string representation of this instance.
     */
    public String toString() {
        return super.toString() + ", Receiver: " + receiver + ", BitArchiveServer: " + baServer.toString();
    }

}
