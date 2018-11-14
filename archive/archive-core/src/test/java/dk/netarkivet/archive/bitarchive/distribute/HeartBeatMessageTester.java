/*
 * #%L
 * Netarchivesuite - archive - test
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
package dk.netarkivet.archive.bitarchive.distribute;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import dk.netarkivet.common.distribute.ChannelID;
import dk.netarkivet.common.distribute.Channels;
import dk.netarkivet.common.exceptions.ArgumentNotValid;

/**
 * Unittests for the HearBeatMessage class.
 */
public class HeartBeatMessageTester {

    private String baID = "BA_ID";
    private ChannelID baMon = Channels.getTheBamon();

    /**
     * Verify that the constructor only fails if it is given a null ChannelID or a null or empty applicationId:
     */
    @Test
    public void testConstructor() {

        HeartBeatMessage hbm = null;

        try {
            hbm = new HeartBeatMessage(null, baID);
            fail("HeartBeatMessage constructor shouldn't accept null as Channel parameter.");
        } catch (ArgumentNotValid e) {
            // Expected.
        }

        try {
            hbm = new HeartBeatMessage(baMon, null);
            fail("HeartBeatMessage constructor shouldn't accept null as application ID.");
        } catch (ArgumentNotValid e) {
            // Expected.
        }

        try {
            hbm = new HeartBeatMessage(baMon, "");
            fail("HeartBeatMessage constructor shouldn't accept empty string as application ID.");
        } catch (ArgumentNotValid e) {
            // Expected.
        }

        // The OK case:
        hbm = new HeartBeatMessage(baMon, baID);

        assertNotNull(hbm);
    }

    /**
     * Verify that getTimestamp(), getApplicationId() and getLogLevel() behave as expected.
     */
    @Test
    public void testGetters() {

        long time = System.currentTimeMillis();
        HeartBeatMessage hbm = new HeartBeatMessage(baMon, baID);

        assertTrue("Timestamp of HeartBeatMessage does not make sense.", hbm.getTimestamp() >= time);

        assertEquals("ApplicationID of HeartBeatMessage is not as excepted.", baID, hbm.getBitarchiveID());
    }
}
