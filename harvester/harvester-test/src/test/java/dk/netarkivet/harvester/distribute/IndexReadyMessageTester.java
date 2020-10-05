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

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import dk.netarkivet.common.distribute.ChannelID;
import dk.netarkivet.common.distribute.Channels;

public class IndexReadyMessageTester {

    @Test
    public void testConstructor() {
        ChannelID replyTo = Channels.getTheIndexServer();
        ChannelID to = HarvesterChannels.getTheSched();
        IndexReadyMessage irm = new IndexReadyMessage(42L, true, to, replyTo);
        assertTrue(42L == irm.getHarvestId());
        assertTrue(true == irm.getIndexOK());

        irm = new IndexReadyMessage(43L, false, to, replyTo);
        assertTrue(43L == irm.getHarvestId());
        assertTrue(false == irm.getIndexOK());

    }
}
