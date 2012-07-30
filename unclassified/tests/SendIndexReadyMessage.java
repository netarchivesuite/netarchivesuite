/* File:    $Id$
 * Version: $Revision$
 * Date:    $Date$
 * Author:  $Author$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2012 The Royal Danish Library, the Danish State and
 * University Library, the National Library of France and the Austrian
 * National Library.
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
import dk.netarkivet.common.distribute.ChannelID;
import dk.netarkivet.common.distribute.Channels;
import dk.netarkivet.common.distribute.JMSConnection;
import dk.netarkivet.common.distribute.JMSConnectionFactory;
import dk.netarkivet.harvester.distribute.IndexReadyMessage;

/**
 * Send a {@link IndexReadyMessage} to the HarvestJobManager to
 * inform, that an deduplication index is ready for at certain harvest ID.
 * 
 */
public class SendIndexReadyMessage {

    /**
     * @param args The harvestID
     */
    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println(
                    "arguments missing. Expected <harvestid>");
            System.exit(1);
        }
        Long harvestId = Long.parseLong(args[0]);
        JMSConnection con = JMSConnectionFactory.getInstance();
        ChannelID to = Channels.getTheSched();
        ChannelID replyTo = Channels.getError();
        IndexReadyMessage msg = new IndexReadyMessage(harvestId, to, replyTo); 
        con.send(msg);
        con.cleanup();
    }
}
