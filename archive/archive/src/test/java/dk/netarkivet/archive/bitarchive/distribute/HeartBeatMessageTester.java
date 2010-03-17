/* File:    $Id$
 * Version: $Revision$
 * Date:    $Date$
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
package dk.netarkivet.archive.bitarchive.distribute;

import junit.framework.TestCase;

import dk.netarkivet.common.distribute.ChannelID;
import dk.netarkivet.common.distribute.Channels;
import dk.netarkivet.common.exceptions.ArgumentNotValid;

/**
 */
public class HeartBeatMessageTester extends TestCase {

    private String baID = "BA_ID";
    private ChannelID baMon = Channels.getTheBamon();

    /**
     * Verify that the constructor only fails if it is given
     * a null ChannelID or a null or empty applicationId:
     */
    public void testConstructor() {

        HeartBeatMessage hbm = null;

        try {
            hbm = new HeartBeatMessage(null, baID);
            fail("HeartBeatMessage constructor shouldn't accept null as Channel parameter.");
        } catch (ArgumentNotValid e) {
            //Expected.
        }

        try {
            hbm = new HeartBeatMessage(baMon, null);
            fail("HeartBeatMessage constructor shouldn't accept null as application ID.");
        } catch (ArgumentNotValid e) {//Expected.
        }


        try {
            hbm = new HeartBeatMessage(baMon, "");
            fail("HeartBeatMessage constructor shouldn't accept empty string as application ID.");
        } catch (ArgumentNotValid e) {//Expected.
        }

        // The OK case:
        hbm = new HeartBeatMessage(baMon, baID);

    }

    /**
     * Verify that getTimestamp(), getApplicationId() and getLogLevel()
     * behave as expected.
     */
    public void testGetters() {

        long time = System.currentTimeMillis();
        HeartBeatMessage hbm = new HeartBeatMessage(baMon, baID);

        assertTrue("Timestamp of HeartBeatMessage does not make sense.",
                hbm.getTimestamp() >= time);

        assertEquals("ApplicationID of HeartBeatMessage is not as excepted.",
                baID, hbm.getBitarchiveID());
    }

}
