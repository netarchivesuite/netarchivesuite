/*
 * #%L
 * Netarchivesuite - harvester - test
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
package dk.netarkivet.harvester.distribute;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import dk.netarkivet.common.distribute.ChannelID;
import dk.netarkivet.common.distribute.Channels;
import dk.netarkivet.harvester.datamodel.HarvestChannel;

public class HarvesterChannelsTester {
    /**
     * Test if static Channels.isTopic(String name) works. Only names containing substring "ALL_BA" is considered a name
     * for a topic.
     */
    @Test
    public void testIsTopic() {
        ChannelID[] queues = new ChannelID[] {
                HarvesterChannels.getHarvestJobChannelId(new HarvestChannel("FOCUSED", false, true, "")),
                HarvesterChannels.getHarvestJobChannelId(new HarvestChannel("SNAPSHOT", true, true, "")),
                HarvesterChannels.getTheSched(), //
                HarvesterChannels.getHarvestMonitorChannel()
        };
        for (ChannelID queue : queues) {
            String queueName = queue.getName();
            assertFalse(queueName + " is not a topic", Channels.isTopic(queueName));
        }

        ChannelID[] topics = new ChannelID[] {HarvesterChannels.getHarvesterStatusChannel()};

        for (ChannelID topic : topics) {
            String topicName = topic.getName();
            assertTrue(topicName + " is a topic", Channels.isTopic(topicName));
        }
    }
}
