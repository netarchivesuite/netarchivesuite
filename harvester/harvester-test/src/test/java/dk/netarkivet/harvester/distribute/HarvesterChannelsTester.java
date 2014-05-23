/*$Id$
* $Revision$
* $Date$
* $Author$
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
package dk.netarkivet.harvester.distribute;

import dk.netarkivet.common.distribute.ChannelID;
import dk.netarkivet.common.distribute.Channels;
import dk.netarkivet.harvester.datamodel.HarvestChannel;
import junit.framework.TestCase;

/**
 * Unittests of the class dk.netarkivet.common.distribute.Channels.
 */
public class HarvesterChannelsTester extends TestCase {
    /**
     * Test if static Channels.isTopic(String name) works.
     * Only names containing substring "ALL_BA" is considered a name
     * for a topic.
     */
    public void testIsTopic() {
        ChannelID[]queues = new ChannelID[]{
                HarvesterChannels.getHarvestJobChannelId(
                        new HarvestChannel("FOCUSED", false, true, "")),
                HarvesterChannels.getHarvestJobChannelId(
                		new HarvestChannel("SNAPSHOT", true, true, "")),
                HarvesterChannels.getHarvestMonitorChannel()
        };
        for (ChannelID queue : queues) {
           String queueName = queue.getName();
           assertFalse(queueName + " is not a topic",
                   Channels.isTopic(queueName));
        }


        ChannelID[]topics = new ChannelID[]{
                HarvesterChannels.getHarvesterStatusChannel()
        };

        for (ChannelID topic : topics) {
            String topicName = topic.getName();
            assertTrue(topicName + " is a topic",
                    Channels.isTopic(topicName));
         }
    }
}
