/*
 * #%L
 * Netarchivesuite - common - test
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
package dk.netarkivet.common.distribute;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.UnknownID;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.testutils.StringAsserts;
import dk.netarkivet.testutils.preconfigured.ReloadSettings;

/**
 * Unittests of the class dk.netarkivet.common.distribute.Channels.
 */
public class ChannelsTester {
    ReloadSettings rs = new ReloadSettings();

    @Before
    public void setUp() {
        rs.setUp();
    }

    @After
    public void tearDown() {
        Channels.reset();
        rs.tearDown();
    }

    /**
     * This test checks that changing settings and resetting actually changes things.
     *
     * @throws Exception
     */
    @Test
    public void testReset() throws Exception {
        String env = Settings.get(CommonSettings.ENVIRONMENT_NAME);
        assertEquals("Channel must have default name before changing settings", env + "_ONE_THE_BAMON", Channels
                .getTheBamon().getName());
        Settings.set(CommonSettings.USE_REPLICA_ID, "TWO");
        assertEquals("Channel name must not change just because setting does", env + "_ONE_THE_BAMON", Channels
                .getTheBamon().getName());
        Channels.reset();
        assertEquals("Channel name should change after resetting channels", env + "_TWO_THE_BAMON", Channels
                .getTheBamon().getName());
    }

    /**
     * This test checks that changing settings and resetting actually changes things.
     *
     * @throws Exception
     */
    @Test
    public void testBadLocation() throws Exception {
        Channels.reset();
        String env = Settings.get(CommonSettings.ENVIRONMENT_NAME);
        assertEquals("Channel must have default name before changing settings",
                env + "_" + Settings.get(CommonSettings.USE_REPLICA_ID) + "_THE_BAMON", Channels.getTheBamon()
                        .getName());
        Settings.set(CommonSettings.USE_REPLICA_ID, "NOWHERE");
        try {
            Channels.reset();
            Channels.getTheBamon();
            fail("Should fail when getting channel after setting bad location");
        } catch (UnknownID e) {
            // Expected exception
        }
    }

    /**
     * Test method to get BAMOn channel for a particular location.
     */
    @Test
    public void testGetBAMONForReplica() {
        ChannelID ch1 = Channels.getBaMonForReplica("TWO");
        assertFalse("Should get channel for TWO, not " + ch1.getName(), ch1.getName().lastIndexOf("TWO") == -1);
        ChannelID ch2 = Channels.getBaMonForReplica("ONE");
        assertFalse("Should get channel for ONE, not " + ch2.getName(), ch2.getName().lastIndexOf("ONE") == -1);
        try {
            ChannelID ch3 = Channels.getBaMonForReplica("AB");
            fail("Should throw exception, not return " + ch3.getName());
        } catch (ArgumentNotValid e) {
            // expected
        }

        Settings.set(CommonSettings.ENVIRONMENT_NAME, "A_B");
        Channels.reset();
        ChannelID ch = Channels.getBaMonForReplica("ONE");
        StringAsserts.assertStringContains("Should find channel even when environment " + "contains underscores",
                "ONE", ch.getName());
    }

    /**
     * Verify that getting the JMS channel for the index server - does not throw an exception - returns a non-null
     * value.
     */
    @Test
    public void testGetThisIndexClient() {
        assertNotNull("Should return a non-null ChannelID", Channels.getThisIndexClient());
    }

    /**
     * Verify that getting the JMS channel for the local index client - does not throw an exception - returns a non-null
     * value.
     */
    @Test
    public void testGetTheIndexServer() {
        assertNotNull("Should return a non-null ChannelID", Channels.getTheIndexServer());
    }

    /**
     * Test if static method Channels.isTopic(String name) works. Only names containing substring "ALL_BA" is considered
     * a name for a topic.
     */
    @Test
    public void testIsTopic() {
        ChannelID[] queues = new ChannelID[] {Channels.getAnyBa(), //
                Channels.getTheRepos(), //
                Channels.getTheIndexServer(), //
                Channels.getError(), //
                Channels.getThisIndexClient(), //
        };

        for (ChannelID queue : queues) {
            String queueName = queue.getName();
            assertFalse(queueName + " is not a topic", Channels.isTopic(queueName));
        }

        ChannelID[] topics = new ChannelID[] {Channels.getAllBa(), Channels.getTheMonitorServer(),};

        for (ChannelID topic : topics) {
            String topicName = topic.getName();
            assertTrue(topicName + " is a topic", Channels.isTopic(topicName));
        }
    }
}
