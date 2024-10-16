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

import dk.netarkivet.common.distribute.ChannelID;
import dk.netarkivet.common.distribute.Channels;
import dk.netarkivet.common.distribute.NetarkivetMessage;
import dk.netarkivet.common.exceptions.ArgumentNotValid;

/**
 * Message for telling the bitarchives to terminate a specific batchjob.
 */
@SuppressWarnings({"serial"})
public class BatchTerminationMessage extends NetarkivetMessage {

    /** The ID of the batchjob to terminate. */
    private String terminateID;

    /**
     * Constructor.
     *
     * @param to Where the message should be sent.
     * @param batchID The ID of the batchjob to terminate.
     * @throws ArgumentNotValid If the batchID is either null or the empty string.
     */
    public BatchTerminationMessage(ChannelID to, String batchID) throws ArgumentNotValid {
        this(to, Channels.getError(), batchID);
    }

    /**
     * Constructor.
     *
     * @param to Where the message should be sent.
     * @param replyTo Where the message is sent from.
     * @param batchID The ID of the batchjob to terminate.
     * @throws ArgumentNotValid If the batchID is either null or the empty string.
     */
    public BatchTerminationMessage(ChannelID to, ChannelID replyTo, String batchID) throws ArgumentNotValid {
        super(to, replyTo);
        ArgumentNotValid.checkNotNullOrEmpty(batchID, "String batchID");
        terminateID = batchID;
    }

    /**
     * Method for retrieving the ID of the batchjob to terminate.
     *
     * @return The ID of the batchjob to terminate.
     */
    public String getTerminateID() {
        return terminateID;
    }

    /**
     * Extends the default toString of NetarkiveMessage with the terminateID.
     *
     * @return The string representation of this message.
     */
    public String toString() {
        return super.toString() + ", terminateID = " + terminateID;
    }
}
