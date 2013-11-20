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

import junit.framework.TestCase;
import dk.netarkivet.common.distribute.JMSConnectionMockupMQ;
import dk.netarkivet.harvester.datamodel.HarvestChannel;
import dk.netarkivet.harvester.harvesting.distribute.CrawlProgressMessage;
import dk.netarkivet.harvester.harvesting.distribute.HarvesterReadyMessage;
import dk.netarkivet.testutils.preconfigured.MockupJMS;
import dk.netarkivet.testutils.preconfigured.ReloadSettings;

public class HarvesterStatusReceiverTest extends TestCase  {
    private ReloadSettings reloadSettings = new ReloadSettings();
    private MockJobDispatcher JobDispatcher;
    private HarvesterStatusReceiver receiver;
    MockupJMS jms = new MockupJMS();
    
    public void setUp() throws Exception {
        reloadSettings.setUp();
        jms.setUp();
        JobDispatcher = new MockJobDispatcher();
        receiver = new HarvesterStatusReceiver(
                JobDispatcher, jms.getJMSConnection());
    }
    
    protected class MockJobDispatcher extends JobDispatcher {
        private String receivedChannelName;
        
        public MockJobDispatcher() {
            super(jms.getJMSConnection());
        }

        @Override
        protected void submitNextNewJob(HarvestChannel hChan) {
            receivedChannelName = hChan.getName();
        }
    }
    
    public void testStatusReception() {
    	HarvestChannel highChan = new HarvestChannel("FOCUSED", "", true);
        HarvesterReadyMessage statusmessage = 
                new HarvesterReadyMessage("Test", highChan.getName());
        receiver.onMessage(
                JMSConnectionMockupMQ.getObjectMessage(statusmessage));
        assertEquals(highChan.getName(), JobDispatcher.receivedChannelName);
    }
    
    public void testInvalidMessageType() {
        CrawlProgressMessage statusmessage = 
                new CrawlProgressMessage(1,1);
        receiver.onMessage(
                JMSConnectionMockupMQ.getObjectMessage(statusmessage));
    }
}
