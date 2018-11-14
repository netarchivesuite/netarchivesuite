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
package dk.netarkivet.archive.distribute;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import dk.netarkivet.archive.bitarchive.distribute.BitarchiveClient;
import dk.netarkivet.archive.checksum.distribute.ChecksumClient;
import dk.netarkivet.common.distribute.Channels;
import dk.netarkivet.common.distribute.JMSConnectionMockupMQ;
import dk.netarkivet.common.distribute.arcrepository.ReplicaType;
import dk.netarkivet.testutils.ReflectUtils;

public class ReplicaClientFactoryTester {

    @Before
    public void setUp() {
        JMSConnectionMockupMQ.useJMSConnectionMockupMQ();
        Channels.reset();
    }

    @After
    public void tearDown() {
        JMSConnectionMockupMQ.clearTestQueues();
    }

    @Test
    public void testUtilityConstructor() {
        ReflectUtils.testUtilityConstructor(ReplicaClientFactory.class);
    }

    @Test
    public void testList() {
        List<ReplicaClient> clients = ReplicaClientFactory.getReplicaClients();

        for (ReplicaClient client : clients) {
            if (client instanceof ChecksumClient) {
                assertEquals("ChecksumClients must be of type " + ReplicaType.CHECKSUM, ReplicaType.CHECKSUM,
                        client.getType());
            } else if (client instanceof BitarchiveClient) {
                assertEquals("BitarchiveClients must be of type " + ReplicaType.BITARCHIVE, ReplicaType.BITARCHIVE,
                        client.getType());
            } else {
                fail("Unknown replica type: " + client.getType());
            }
        }
    }
}
