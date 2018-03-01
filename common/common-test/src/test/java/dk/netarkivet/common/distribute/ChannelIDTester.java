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
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.exceptions.PermissionDenied;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.testutils.StringAsserts;
import dk.netarkivet.testutils.preconfigured.ReloadSettings;

/**
 * Tests the ChannelID class that defines instances of message channels (ie. queues and topics).
 */
public class ChannelIDTester {
    ReloadSettings rs = new ReloadSettings();

    @Before
    public void setUp() {
        rs.setUp();
        Settings.set(CommonSettings.APPLICATION_NAME, "dk.netarkivet.archive.indexserver.IndexServerApplication");
        Settings.set(CommonSettings.APPLICATION_INSTANCE_ID, "XXX");
    }

    /**
     * Verify that a queue instance is serializable.
     */
    @Test
    public void testSerializability() throws IOException, ClassNotFoundException {
        // Take two queues: one for study and one for reference.
        ChannelID q1 = Channels.getAnyBa();
        ChannelID q2 = Channels.getAnyBa();
        // Now serialize and deserialize the studied queue (but NOT the
        // reference):
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream ous = new ObjectOutputStream(baos);
        ous.writeObject(q1);
        ous.close();
        baos.close();
        ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray()));
        q1 = (ChannelID) ois.readObject();
        // Finally, compare their visible states:
        assertEquals("The two channels should have the same name", q1.getName(), q2.getName());
    }

    /**
     * Verify that a topic instance is Serializable.
     */
    @Test
    public void testTopicSerializability() throws IOException, ClassNotFoundException {
        // Take two queues: one for study and one for reference.
        ChannelID q1 = Channels.getAllBa();
        ChannelID q2 = Channels.getAllBa();
        // Now serialize and deserialize the studied queue (but NOT the
        // reference):
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream ous = new ObjectOutputStream(baos);
        ous.writeObject(q1);
        ous.close();
        baos.close();
        ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray()));
        q1 = (ChannelID) ois.readObject();
        // Finally, compare their visible states:
        assertEquals("The two channels should have the same name", q1.getName(), q2.getName());
    }

    /**
     * Test that AllArchives_ALL_BAs returns as array of ALL_BA channels which are all distinct and all contain ALL_BA
     * in name.
     */
    @Test
    public void testALL_ALL_BAs() {
        ChannelID[] ALL_BAs = Channels.getAllArchives_ALL_BAs();
        for (int i = 0; i < ALL_BAs.length; i++) {
            // Ignore the channels for the checksum replicas.
            if (ALL_BAs[i] != null) {
                StringAsserts.assertStringContains("ChannelID.getAllArchives_ALL_BAs() returned a channel"
                        + " without ALL_BA in its name", "ALL_BA", ALL_BAs[i].getName());
                for (int j = 0; j < ALL_BAs.length; j++) {
                    // Ignore the channels for the checksum replicas.
                    if (ALL_BAs[j] != null) {
                        if (i != j) {
                            assertNotSame("Two ALL_BAs have the same name " + ALL_BAs[i].getName(),
                                    ALL_BAs[i].getName(), ALL_BAs[j].getName());
                        }
                    }
                }
            }
        }
    }

    /**
     * Test that AllArchives_ANY_BAs returns as array of ANY_BA channels which are all distinct and all contain ANY_BA
     * in name
     */
    @Test
    public void testALL_ANY_BAs() {
        ChannelID[] ANY_BAs = Channels.getAllArchives_ANY_BAs();
        for (int i = 0; i < ANY_BAs.length; i++) {
            // Ignore the channels for the checksum replicas.
            if (ANY_BAs[i] == null) {
                continue;
            }
            StringAsserts.assertStringContains("ChannelID.getAllArchives_ANY_BAs() returned a channel"
                    + " without ANY_BA in its name", "ANY_BA", ANY_BAs[i].getName());
            for (int j = 0; j < ANY_BAs.length; j++) {
                // Ignore the channels for the checksum replicas.
                if (ANY_BAs[j] == null) {
                    continue;
                }
                if (i != j) {
                    assertNotSame("Two ANY_BAs have the same name " + ANY_BAs[i].getName(), ANY_BAs[i].getName(),
                            ANY_BAs[j].getName());
                }
            }
        }
    }

    /**
     * Test that AllArchives_BAMONs returns as array of BAMON channels which are all distinct and all contain BAMON in
     * name.
     */
    @Test
    public void testAllArchives_BAMONs() {
        ChannelID[] BAMONs = Channels.getAllArchives_BAMONs();
        for (int i = 0; i < BAMONs.length; i++) {
            // Ignore the channels for the checksum replicas.
            if (BAMONs[i] == null) {
                continue;
            }
            assertTrue("ChannelID.getAllArchives_BAMONs() returned a channel without BAMON " + "in its name: "
                    + BAMONs[i].getName(), BAMONs[i].getName().indexOf("BAMON") != -1);
            for (int j = 0; j < BAMONs.length; j++) {
                // Ignore the channels for the checksum replicas.
                if (BAMONs[j] == null) {
                    continue;
                }
                if (i != j) {
                    assertNotSame("Two BAMONs have the same name " + BAMONs[i].getName(), BAMONs[i].getName(),
                            BAMONs[j].getName());
                }
            }
        }
    }

    /**
     * Test that AllArchives_CRs returns as array of checksum replica channels which are all distinct and all contain
     * THE_CR in name.
     */
    @Test
    public void testAllArchives_CRs() {
        ChannelID[] CRs = Channels.getAllArchives_CRs();
        for (int i = 0; i < CRs.length; i++) {
            // Ignore the channels for the checksum replicas.
            if (CRs[i] == null) {
                continue;
            }
            assertTrue(
                    "ChannelID.getAllArchives_BAMONs() returned a channel without BAMON " + "in its name: "
                            + CRs[i].getName(), CRs[i].getName().indexOf("THE_CR") != -1);
            for (int j = 0; j < CRs.length; j++) {
                // Ignore the channels for the checksum replicas.
                if (CRs[j] == null) {
                    continue;
                }
                if (i != j) {
                    assertNotSame("Two BAMONs have the same name " + CRs[i].getName(), CRs[i].getName(),
                            CRs[j].getName());
                }
            }
        }
    }

    /**
     * Test that AllArchives_BAMONs returns as array of BAMON channels which are all distinct and all contain BAMON in
     * name.
     */
    @Test
    public void testClients() {
        ChannelID client = Channels.getThisReposClient();
        assertTrue(
                "ChannelID.getThisClient() returned a channel without " + "THIS_REPOS_CLIENT in its name: "
                        + client.getName(), client.getName().indexOf("THIS_REPOS_CLIENT") != -1);
        assertTrue("ChannelID.getThisClient() returned a channel without " + "application settings in its name: "
                + client.getName(), client.getName().indexOf("_IS") != -1);
        assertTrue("ChannelID.getThisClient() returned a channel without "
                        + "application instance settings in its name: " + client.getName(),
                client.getName().indexOf("_XXX") != -1);
    }

    /**
     * Tests that only the ArcRepository may request channels for all bit archives. At the time of writing, there is no
     * ArcRepository application, so this functionality cannot be implemented (or tested).
     */
    @Test
    @Ignore("no ArcRepository application yet")
    public void excludedtesttestALL_ALL_BAsAccess() {
        try {
            Channels.getAllArchives_ALL_BAs();
            fail("ChannelID.getAllArchives_ALL_BAs() failed to throw exception when "
                    + "application was not arcrepository");
        } catch (PermissionDenied e) {
            // expected
        }

    }
}
