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

import dk.netarkivet.archive.distribute.ArchiveMessage;
import dk.netarkivet.archive.distribute.ArchiveMessageVisitor;
import dk.netarkivet.common.distribute.ChannelID;
import dk.netarkivet.common.distribute.Channels;
import dk.netarkivet.common.exceptions.ArgumentNotValid;

/**
 * Simple class representing a HeartBeat message from a bit archive application. A heartbeat has an applicationId, that
 * identifies the application that generated the heartbeat.
 * <p>
 * TODO This class should probably contain more status data from bit archive application later.
 */
@SuppressWarnings({"serial"})
public class HeartBeatMessage extends ArchiveMessage {

    /**
     * time when heartbeat occurred. Note that timestamps cannot be compared between processes.
     */
    private long timestamp;
    /** id of the application sending the heartbeat. */
    private String applicationId;

    /**
     * Creates a heartbeat message. The time of the heartbeat is set to the creation of this object.
     *
     * @param inReceiver ChannelID for the recipient of this message.
     * @param applicationId - id of the application that sent the heartbeat
     */
    public HeartBeatMessage(ChannelID inReceiver, String applicationId) {
        super(inReceiver, Channels.getError());
        ArgumentNotValid.checkNotNullOrEmpty(applicationId, "applicationId");
        timestamp = System.currentTimeMillis();
        this.applicationId = applicationId;
    }

    /**
     * @return time of heartbeat occurrence.
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * @return id of the application that generated the heartbeat.
     */
    public String getBitarchiveID() {
        return applicationId;
    }

    /**
     * Retrieval of a string representation of this instance.
     *
     * @return The string representation of this instance.
     */
    public String toString() {
        return ("Heartbeat for " + applicationId + " at " + timestamp);
    }

    /**
     * Should be implemented as a part of the visitor pattern. fx.: public void accept(ArchiveMessageVisitor v) {
     * v.visit(this); }
     *
     * @param v A message visitor
     */
    public void accept(ArchiveMessageVisitor v) {
        v.visit(this);
    }
}
