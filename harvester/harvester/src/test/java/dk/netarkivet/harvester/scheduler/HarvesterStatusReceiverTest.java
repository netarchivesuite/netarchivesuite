/* File:    $Id$
 * Revision: $Revision$
 * Author:   $Author$
 * Date:     $Date$
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
 * Foundation,dk.netarkivet.harvester.schedulerFloor, Boston, MA  02110-1301  USA
 */
package dk.netarkivet.harvester.scheduler;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import junit.framework.TestCase;
import dk.netarkivet.common.distribute.JMSConnection;
import dk.netarkivet.common.distribute.JMSConnectionMockupMQ;
import dk.netarkivet.harvester.dao.HarvestChannelDAO;
import dk.netarkivet.harvester.datamodel.HarvestChannel;
import dk.netarkivet.harvester.harvesting.distribute.CrawlProgressMessage;
import dk.netarkivet.harvester.harvesting.distribute.HarvesterReadyMessage;

public class HarvesterStatusReceiverTest extends TestCase  {
    private JobDispatcher jobDispatcher;
    private HarvesterStatusReceiver receiver;
    private JMSConnection jmsConnection;
    private HarvestChannelDAO harvestChannelDao;
    
    public void setUp() throws Exception {
        jobDispatcher = mock(JobDispatcher.class);
        jmsConnection = mock(JMSConnection.class);
        harvestChannelDao = mock(HarvestChannelDAO.class);
        receiver = new HarvesterStatusReceiver(jobDispatcher, jmsConnection, harvestChannelDao, new HarvestChannelRegistry());
    }
    
    public void testStatusReception() {
        HarvestChannel highChan = new HarvestChannel("FOCUSED", "", true);
        HarvesterReadyMessage readyMessage =
                new HarvesterReadyMessage("Test", highChan.getName());
        when(harvestChannelDao.getByName(highChan.getName())).thenReturn(highChan);
        receiver.onMessage(JMSConnectionMockupMQ.getObjectMessage(readyMessage));
        verify(jobDispatcher).submitNextNewJob(highChan);
    }
    
    public void testInvalidMessageType() {
        CrawlProgressMessage statusmessage =  new CrawlProgressMessage(1,1);
        receiver.onMessage( JMSConnectionMockupMQ.getObjectMessage(statusmessage));
    }
}
