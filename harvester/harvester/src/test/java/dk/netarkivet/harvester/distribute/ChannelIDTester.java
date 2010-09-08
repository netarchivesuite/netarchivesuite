/* File:    $Id$
 * Revision:$Revision$
 * Author:  $Author$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2010 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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

import junit.framework.TestCase;
import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.distribute.ChannelID;
import dk.netarkivet.common.distribute.Channels;
import dk.netarkivet.common.exceptions.UnknownID;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.harvester.HarvesterSettings;
import dk.netarkivet.harvester.datamodel.JobPriority;
import dk.netarkivet.harvester.harvesting.distribute.JobChannelUtil;
import dk.netarkivet.testutils.preconfigured.ReloadSettings;

/**
 * Tests the part of ChannelID class that relates to the harvesting module.
 * The rest of ChannelID is tested in dk.netarkivet.common.distribute.ChannelIDTester
 */
public class ChannelIDTester extends TestCase {
    ReloadSettings rs = new ReloadSettings();
   
    public void setUp() {
        rs.setUp();
        Settings.set(CommonSettings.APPLICATION_NAME,
                "dk.netarkivet.archive.indexserver.IndexServerApplication");
        Settings.set(CommonSettings.APPLICATION_INSTANCE_ID, "XXX");
    }

    /**
     * Test that each channel is equal only to itself.
     */
    public void testChannelIdentity(){
        JobPriority priority = JobPriority.valueOf(Settings.get(HarvesterSettings.HARVEST_CONTROLLER_PRIORITY));
        ChannelID harvestJobChannel = JobChannelUtil.getChannel(priority);
        ChannelID[] channelArray =
         {Channels.getAllBa(), harvestJobChannel, Channels.getAnyBa(),
          Channels.getError(), Channels.getTheRepos(), Channels.getTheBamon(),
          Channels.getTheSched(), Channels.getThisReposClient()};
        for (int i = 0; i<channelArray.length; i++){
            for (int j = 0; j<channelArray.length; j++){
                if (i == j) {
                    assertEquals("Two different instances of same queue "
                            +channelArray[i].getName(), channelArray[i],
                            channelArray[j]);
                    assertEquals("Two instances of same channel have different " +
                            "names: "
                            + channelArray[i].getName() + " and " +
                            channelArray[j].getName(), channelArray[i].getName(),
                            channelArray[j].getName() ) ;
                }
                else {
                    assertNotSame("Two different queues are the same object "
                            +channelArray[i].getName() + " "
                            + channelArray[j].getName(), channelArray[i],
                            channelArray[j]);
                    assertNotSame("Two different channels have same name",
                            channelArray[i].getName(), channelArray[j].getName());
                }
            }
        }
    }
}
