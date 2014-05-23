/* File:        $Id: IndexReadyMessage.java 2617 2013-02-13 15:51:13Z svc $
 * Revision:    $Revision: 2617 $
 * Author:      $Author: svc $
 * Date:        $Date: 2013-02-13 16:51:13 +0100 (Wed, 13 Feb 2013) $
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2012 The Royal Danish Library, the Danish State and
 * University Library, the National Library of France and the Austrian
 * National Library.
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

import dk.netarkivet.common.distribute.ChannelID;
import dk.netarkivet.common.distribute.Channels;
import junit.framework.TestCase;

public class IndexReadyMessageTester extends TestCase {

    public void testConstructor() {
        ChannelID replyTo = Channels.getTheIndexServer();
        ChannelID to = Channels.getTheSched();
        IndexReadyMessage irm = new IndexReadyMessage(42L, true, to, replyTo);
        assertTrue(42L == irm.getHarvestId());
        assertTrue(true == irm.getIndexOK());
        
        irm = new IndexReadyMessage(43L, false, to, replyTo);
        assertTrue(43L == irm.getHarvestId());
        assertTrue(false == irm.getIndexOK());
        
        
    }
}
