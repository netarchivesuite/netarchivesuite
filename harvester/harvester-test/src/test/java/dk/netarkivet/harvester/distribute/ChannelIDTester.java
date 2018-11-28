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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;

import org.junit.Test;

import dk.netarkivet.common.distribute.ChannelID;
import dk.netarkivet.common.distribute.Channels;
import dk.netarkivet.harvester.datamodel.HarvestChannel;

/**
 * Tests the part of ChannelID class that relates to the harvesting module. The rest of ChannelID is tested in
 * dk.netarkivet.common.distribute.ChannelIDTester
 */
public class ChannelIDTester {
    /**
     * Test that each channel is equal only to itself.
     */
    @Test
    public void testChannelIdentity() {
        ChannelID harvestJobChannel = HarvesterChannels.getHarvestJobChannelId(new HarvestChannel("FOCUSED", false,
                true, ""));
        ChannelID[] channelArray = {Channels.getAllBa(), harvestJobChannel, Channels.getAnyBa(), Channels.getError(),
                Channels.getTheRepos(), Channels.getTheBamon(), HarvesterChannels.getTheSched(), Channels.getThisReposClient()};
        for (int i = 0; i < channelArray.length; i++) {
            for (int j = 0; j < channelArray.length; j++) {
                if (i == j) {
                    assertEquals("Two different instances of same queue " + channelArray[i].getName(), channelArray[i],
                            channelArray[j]);
                    assertEquals(
                            "Two instances of same channel have different " + "names: " + channelArray[i].getName()
                                    + " and " + channelArray[j].getName(), channelArray[i].getName(),
                            channelArray[j].getName());
                } else {
                    assertNotSame("Two different queues are the same object " + channelArray[i].getName() + " "
                            + channelArray[j].getName(), channelArray[i], channelArray[j]);
                    assertNotSame("Two different channels have same name", channelArray[i].getName(),
                            channelArray[j].getName());
                }
            }
        }
    }
}
