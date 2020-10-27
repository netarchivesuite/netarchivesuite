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
package dk.netarkivet.harvester.scheduler;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;

import dk.netarkivet.common.distribute.JMSConnection;
import dk.netarkivet.common.distribute.JMSConnectionMockupMQ;
import dk.netarkivet.harvester.datamodel.HarvestChannel;
import dk.netarkivet.harvester.datamodel.HarvestChannelDAO;
import dk.netarkivet.harvester.harvesting.distribute.CrawlProgressMessage;
import dk.netarkivet.harvester.harvesting.distribute.HarvesterReadyMessage;

public class HarvesterStatusReceiverTest {
    private JobDispatcher jobDispatcher;
    private HarvesterStatusReceiver receiver;
    private JMSConnection jmsConnection;
    private HarvestChannelDAO harvestChannelDao;

    @Before
    public void setUp() throws Exception {
        jobDispatcher = mock(JobDispatcher.class);
        jmsConnection = mock(JMSConnection.class);
        harvestChannelDao = mock(HarvestChannelDAO.class);
        receiver = new HarvesterStatusReceiver(jobDispatcher, jmsConnection, harvestChannelDao,
                new HarvestChannelRegistry());
    }

    @Test
    public void testStatusReception() {
        HarvestChannel highChan = new HarvestChannel("FOCUSED", false, true, "");
        HarvesterReadyMessage readyMessage = new HarvesterReadyMessage("Test", highChan.getName());
        when(harvestChannelDao.getByName(highChan.getName())).thenReturn(highChan);
        receiver.onMessage(JMSConnectionMockupMQ.getObjectMessage(readyMessage));
        verify(jobDispatcher).submitNextNewJob(highChan);
    }

    @Test
    public void testInvalidMessageType() {
        CrawlProgressMessage statusmessage = new CrawlProgressMessage(1, 1);
        receiver.onMessage(JMSConnectionMockupMQ.getObjectMessage(statusmessage));
    }
}
