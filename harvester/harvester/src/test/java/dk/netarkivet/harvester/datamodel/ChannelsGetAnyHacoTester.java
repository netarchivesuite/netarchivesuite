/*$Id$
* $Revision$
* $Date$
* $Author$
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
package dk.netarkivet.harvester.datamodel;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.distribute.ChannelID;
import dk.netarkivet.common.distribute.Channels;
import dk.netarkivet.common.distribute.ChannelsTester;
import dk.netarkivet.common.exceptions.UnknownID;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.harvester.HarvesterSettings;
import dk.netarkivet.testutils.preconfigured.ReloadSettings;

/** 
 * Supplementing tests of Channels class that requires the harvester package. 
 */
public class ChannelsGetAnyHacoTester extends ChannelsTester {

    ReloadSettings rs = new ReloadSettings();
    public ChannelsGetAnyHacoTester(String s) {
        super(s);
    }
    /**
     * This tests the getAnyHaco methods for generating the right priorities
     * in both the version taking the priority as argument and reading the
     * priority from settings.
     */
    public void testGetAnyHaco() {
        String env = Settings.get(CommonSettings.ENVIRONMENT_NAME);

        // Test setting low priority in settings
        Settings.set(HarvesterSettings.HARVEST_CONTROLLER_PRIORITY,
                     "LOWPRIORITY");
        resetChannels();
        String priority2 = Settings.get(
                HarvesterSettings.HARVEST_CONTROLLER_PRIORITY);
        ChannelID result5;
        if (priority2.equals(JobPriority.LOWPRIORITY.toString())) {
            result5 = Channels.getAnyLowpriorityHaco();
        } else {
            if (priority2.equals(JobPriority.HIGHPRIORITY.toString())) {
                result5 = Channels.getAnyHighpriorityHaco();
            } else {
                throw new UnknownID(priority2 + " is not a valid priority");
            }
        }
        ChannelID anyHaco0 = result5;
        assertEquals("Channel should be low priority",
                     env + "_COMMON_ANY_LOWPRIORITY_HACO",
                     anyHaco0.getName());

        // Test setting high priority in argument
        ChannelID result4;
        if ("HIGHPRIORITY".equals(JobPriority.LOWPRIORITY.toString())) {
            result4 = Channels.getAnyLowpriorityHaco();
        } else {
            if ("HIGHPRIORITY".equals(JobPriority.HIGHPRIORITY.toString())) {
                result4 = Channels.getAnyHighpriorityHaco();
            } else {
                throw new UnknownID("HIGHPRIORITY"
                        + " is not a valid priority");
            }
        }
        ChannelID anyHaco1 = result4;
        assertEquals("Channel should be high priority",
                     env + "_COMMON_ANY_HIGHPRIORITY_HACO",
                     anyHaco1.getName());

        // Test setting high priority in settings
        Settings.set(HarvesterSettings.HARVEST_CONTROLLER_PRIORITY,
                     "HIGHPRIORITY");
        resetChannels();
        String priority1 = Settings.get(
                HarvesterSettings.HARVEST_CONTROLLER_PRIORITY);
        ChannelID result3;
        if (priority1.equals(JobPriority.LOWPRIORITY.toString())) {
            result3 = Channels.getAnyLowpriorityHaco();
        } else {
            if (priority1.equals(JobPriority.HIGHPRIORITY.toString())) {
                result3 = Channels.getAnyHighpriorityHaco();
            } else {
            throw new UnknownID(priority1 + " is not a valid priority");
            }
        }
        ChannelID anyHaco2 = result3;
        assertEquals("Channel should be high priority",
                     env + "_COMMON_ANY_HIGHPRIORITY_HACO",
                     anyHaco2.getName());

        // Test setting high priority in argument
        ChannelID result2;
        if ("LOWPRIORITY".equals(JobPriority.LOWPRIORITY.toString())) {
            result2 = Channels.getAnyLowpriorityHaco();
        } else {
            if ("LOWPRIORITY".equals(JobPriority.HIGHPRIORITY.toString())) {
                result2 = Channels.getAnyHighpriorityHaco();
            } else {
                throw new UnknownID("LOWPRIORITY" + " is not a valid priority");
            }
        }
        ChannelID anyHaco3 = result2;
        assertEquals("Channel should be low priority",
                     env + "_COMMON_ANY_LOWPRIORITY_HACO",
                     anyHaco3.getName());

        // Test setting high priority in argument
        ChannelID result1;
        if ("HIGHPRIORITY".equals(JobPriority.LOWPRIORITY.toString())) {
            result1 = Channels.getAnyLowpriorityHaco();
        } else {
            if ("HIGHPRIORITY".equals(JobPriority.HIGHPRIORITY.toString())) {
                result1 = Channels.getAnyHighpriorityHaco();
            } else {
                throw new UnknownID("HIGHPRIORITY"
                        + " is not a valid priority");
            }
        }
        ChannelID anyHaco4 = result1;
        assertEquals("Channel should be high priority",
                     env + "_COMMON_ANY_HIGHPRIORITY_HACO",
                     anyHaco4.getName());

        // Test illegal argument in argument
        try {
            ChannelID result;
            if ("ILLEGALPRIORITY".equals(JobPriority.LOWPRIORITY.toString())) {
                result = Channels.getAnyLowpriorityHaco();
            } else {
                if ("ILLEGALPRIORITY".equals(
                        JobPriority.HIGHPRIORITY.toString())) {
                    result = Channels.getAnyHighpriorityHaco();
                } else {
                    throw new UnknownID("ILLEGALPRIORITY"
                            + " is not a valid priority");
                }
            }
            fail("Should throw exception on illegal priority");
        } catch (UnknownID e) {
            //expected
        }

        // Test illegal argument in settings
        Settings.set(HarvesterSettings.HARVEST_CONTROLLER_PRIORITY,
                     "ILLEGALPRIORITY");
        resetChannels();
        try {
            String priority
                = Settings.get(HarvesterSettings.HARVEST_CONTROLLER_PRIORITY);
            ChannelID result;
            if (priority.equals(JobPriority.LOWPRIORITY.toString())) {
                result = Channels.getAnyLowpriorityHaco();
            } else {
                if (priority.equals(JobPriority.HIGHPRIORITY.toString())) {
                    result = Channels.getAnyHighpriorityHaco();
                } else {
                    throw new UnknownID(priority + " is not a valid priority");
                }
            }
            fail("Should throw exception on illegal priority");
        } catch (UnknownID e) {
            //expected
        }
    }


}
