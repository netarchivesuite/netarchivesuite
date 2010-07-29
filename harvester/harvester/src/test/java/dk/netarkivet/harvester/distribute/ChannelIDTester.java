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
        String priority1 = Settings.get(HarvesterSettings.HARVEST_CONTROLLER_PRIORITY);
        JobPriority p = JobPriority.valueOf(priority1);
        ChannelID result1;
        switch(p) {
            case LOWPRIORITY:
                result1 = Channels.getAnyLowpriorityHaco();
                break;
            case HIGHPRIORITY:
                result1 = Channels.getAnyHighpriorityHaco();
                break;
            default:
                throw new UnknownID(priority1 + " is not a valid priority");
        }
        ChannelID[] l1 =
         {Channels.getAllBa(), result1, Channels.getAnyBa(),
          Channels.getError(), Channels.getTheRepos(), Channels.getTheBamon(),
          Channels.getTheSched(), Channels.getThisReposClient()};
        String priority = Settings.get(HarvesterSettings.HARVEST_CONTROLLER_PRIORITY);
        ChannelID result;
        JobPriority p1 = JobPriority.valueOf(priority1);
        switch(p1) {
            case LOWPRIORITY:
                result = Channels.getAnyLowpriorityHaco();
                break;
            case HIGHPRIORITY:
                result = Channels.getAnyHighpriorityHaco();
                break;
            default:
                throw new UnknownID(priority + " is not a valid priority");
        }
        ChannelID[] l2 =
                {Channels.getAllBa(), result, Channels.getAnyBa(),
                 Channels.getError(), Channels.getTheRepos(), Channels.getTheBamon(),
                 Channels.getTheSched(), Channels.getThisReposClient()};

        for (int i = 0; i<l1.length; i++){
            for (int j = 0; j<l2.length; j++){
                if (i == j) {
                    assertEquals("Two different instances of same queue "
                            +l1[i].getName(), l1[i],
                            l2[j]);
                    assertEquals("Two instances of same channel have different " +
                            "names: "
                            + l1[i].getName() + " and " +
                            l2[j].getName(), l1[i].getName(),
                            l2[j].getName() ) ;
                }
                else{
                    assertNotSame("Two different queues are the same object "
                            +l1[i].getName() + " "
                            + l2[j].getName(), l1[i],
                            l2[j]);
                    assertNotSame("Two different channels have same name",
                            l1[i].getName(), l2[j].getName());
                }
            }
        }
    }
}
