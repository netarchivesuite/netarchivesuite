/*$Id$
* $Revision$
* $Date$
* $Author$
*
* The Netarchive Suite - Software to harvest and preserve websites
* Copyright 2004-2007 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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
package dk.netarkivet.common.distribute;

import junit.framework.TestCase;

import dk.netarkivet.common.Settings;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.UnknownID;
import dk.netarkivet.harvester.datamodel.JobPriority;
import dk.netarkivet.testutils.StringAsserts;

/**
 * Unittests of the class dk.netarkivet.common.distribute.Channels.
 */
public class ChannelsTester extends TestCase {

    public ChannelsTester(String s) {
        super(s);
    }

    public static void resetChannels() {
        Channels.reset();
    }

    public void setUp() {
    }

    public void tearDown() {
        Settings.reload();
        resetChannels();
    }

    /** This test checks that changing settings and resetting
     * actually changes things.
     * @throws Exception
     */
    public void testReset() throws Exception {
        String env = Settings.get(Settings.ENVIRONMENT_NAME);
        assertEquals("Channel must have default name before changing settings",
                env + "_SB_THE_BAMON", Channels.getTheBamon().getName());
        Settings.set(Settings.ENVIRONMENT_LOCATION_NAMES, "SB", "KB");
        Settings.set(Settings.ENVIRONMENT_THIS_LOCATION, "KB");
        assertEquals("Channel name must not change just because setting does",
                env + "_SB_THE_BAMON", Channels.getTheBamon().getName());
        resetChannels();
        assertEquals("Channel name should change after resetting channels",
                env + "_KB_THE_BAMON", Channels.getTheBamon().getName());
    }

    /** This test checks that changing settings and resetting
     * actually changes things.
     * @throws Exception
     */
    public void testBadLocation() throws Exception {
        String env = Settings.get(Settings.ENVIRONMENT_NAME);
        assertEquals("Channel must have default name before changing settings",
                env + "_" + Settings.get(Settings.ENVIRONMENT_THIS_LOCATION)
                + "_THE_BAMON", Channels.getTheBamon().getName());
        Settings.set(Settings.ENVIRONMENT_THIS_LOCATION, "NOWHERE");
        try {
            resetChannels();
            Channels.getTheBamon();
            fail("Should fail when getting channel after setting bad location");
        } catch (ArgumentNotValid e) {
            // Expected exception
        }
    }

    /**
     * This tests the getAnyHaco methods for generating the right priorities
     * in both the version taking the priority as argument and reading the
     * priority from settings.
     */
    public void testGetAnyHaco() {
        String env = Settings.get(Settings.ENVIRONMENT_NAME);

        // Test setting low priority in settings
        Settings.set(Settings.HARVEST_CONTROLLER_PRIORITY, "LOWPRIORITY");
        Channels.reset();
        String priority2 = Settings.get(Settings.HARVEST_CONTROLLER_PRIORITY);
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
        Settings.set(Settings.HARVEST_CONTROLLER_PRIORITY, "HIGHPRIORITY");
        Channels.reset();
        String priority1 = Settings.get(Settings.HARVEST_CONTROLLER_PRIORITY);
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
        Settings.set(Settings.HARVEST_CONTROLLER_PRIORITY, "ILLEGALPRIORITY");
        Channels.reset();
        try {
            String priority
                = Settings.get(Settings.HARVEST_CONTROLLER_PRIORITY);
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

    /**
     * Test method to get BAMOn channel for a particular location.
     */
    public void testGetBAMONForLocation() {
        ChannelID ch1 = Channels.getBaMonForLocation("KB");
        assertFalse("Should get channel for KB, not " + ch1.getName(),
                ch1.getName().lastIndexOf("KB") == -1);
        ChannelID ch2 = Channels.getBaMonForLocation("SB");
        assertFalse("Should get channel for SB, not " + ch2.getName(),
                ch2.getName().lastIndexOf("SB") == -1);
        try {
            ChannelID ch3 = Channels.getBaMonForLocation("AB");
            fail("Should throw exception, not return " + ch3.getName());
        } catch (ArgumentNotValid e) {
            //expected
        }

        Settings.set(Settings.ENVIRONMENT_NAME, "A_B");
        Channels.reset();
        ChannelID ch = Channels.getBaMonForLocation("SB");
        StringAsserts.assertStringContains(
                "Should find channel even when environment "
                + "contains underscores",
                "SB", ch.getName());
    }


    /**
     * Verify that getting the JMS channel for the index server
     *  - does not throw an exception
     *  - returns a non-null value.
     */
    public void testGetThisIndexClient() {
        assertNotNull("Should return a non-null ChannelID",
                Channels.getThisIndexClient());
    }
    /**
     * Verify that getting the JMS channel for the local index client
     *  - does not throw an exception
     *  - returns a non-null value.
     */
    public void testGetTheIndexServer() {
        assertNotNull("Should return a non-null ChannelID",
                Channels.getTheIndexServer());
    }

    /**
     * Test if static method Channels.isTopic(String name) works.
     * Only names containing substring "ALL_BA" is considered a name
     * for a topic.
     */
    public void testIsTopic() {
        ChannelID[]queues = new ChannelID[]{
                Channels.getAnyHighpriorityHaco(),
                Channels.getAnyBa(),
                Channels.getAnyLowpriorityHaco(),
                Channels.getTheArcrepos(),
                Channels.getTheIndexServer(),
                Channels.getTheMonitorServer(),
                Channels.getError(),
                Channels.getTheSched(),
                Channels.getThisIndexClient()
        };
        for (ChannelID queue : queues) {
           String queueName = queue.getName();
           assertFalse(queueName + " is not a topic",
                   Channels.isTopic(queueName));
        }

        String topicName = Channels.getAllBa().getName();
        assertTrue(topicName + " is a topic", Channels.isTopic(topicName));
    }
}
