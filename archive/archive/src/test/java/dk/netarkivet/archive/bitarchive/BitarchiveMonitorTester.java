package dk.netarkivet.archive.bitarchive;/* $ ID: ${NAME}.java Aug 27, 2009 3:10:52 PM hbk $
* $ Revision: $
* $ Date: Aug 27, 2009 3:10:52 PM $ 
* $ @auther hbk $
* The Netarchive Suite - Software to harvest and preserve websites
* Copyright 2004-2009 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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

import junit.framework.TestCase;

import dk.netarkivet.archive.arcrepository.distribute.JMSArcRepositoryClient;
import dk.netarkivet.archive.bitarchive.distribute.BatchMessage;
import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.distribute.Channels;
import dk.netarkivet.common.distribute.arcrepository.Replica;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.testutils.preconfigured.MockupJMS;
import dk.netarkivet.testutils.preconfigured.ReloadSettings;

/** Unit test for BitarchiveMonitorTester */
public class BitarchiveMonitorTester extends TestCase {
    private BitarchiveMonitor bitarchiveMonitor;
    private JMSArcRepositoryClient jmsARClient;
    private ReloadSettings rs = new ReloadSettings();

    private MockupJMS mj = new MockupJMS();

    private static final Replica ONE = Replica.getReplicaFromId("ONE");

    @Override
    public void setUp() {
        rs.setUp();
        mj.setUp();
        bitarchiveMonitor = new BitarchiveMonitor();
        //jmsARClient = (JMSArcRepositoryClient) ArcRepositoryClientFactory.getPreservationInstance();
    }

    @Override
    public void tearDown() {
        //if (jmsARClient != null) {
       //     jmsARClient.close();
        //}
        mj.tearDown();
        rs.tearDown();
    }

    public void testBatchJobTimout() {
        TimeoutBatch timeoutBatch = new TimeoutBatch();

        //jmsARClient.batch(timeoutBatch, "ONE");
        BatchMessage msg = new BatchMessage(Channels.getTheBamon(), Channels
                .getError(), timeoutBatch, Settings.get(
                CommonSettings.USE_REPLICA_ID));
        
        long batchTimeout = msg.getJob().getBatchJobTimeout();
        /*bitarchiveMonitor.registerBatch("asdfsdf", msg.getReplyTo(),
                        "asdfsdf",
                        batchTimeout > 0 ? batchTimeout : Settings.getLong(
                        ArchiveSettings.BITARCHIVE_BATCH_JOB_TIMEOUT)
                        );*/

        assertEquals(batchTimeout, batchTimeout > 0 ? batchTimeout :
                                   TestInfo.BITARCHIVE_BATCH_JOB_TIMEOUT);

        EvilBatch eb = new EvilBatch();

        //jmsARClient.batch(timeoutBatch, "ONE");
        msg = new BatchMessage(Channels.getTheBamon(), Channels
                .getError(), eb, Settings.get(
                CommonSettings.USE_REPLICA_ID));

        batchTimeout = msg.getJob().getBatchJobTimeout();
        /*bitarchiveMonitor.registerBatch("asdfsdf", msg.getReplyTo(),
                        "asdfsdf",
                        batchTimeout > 0 ? batchTimeout :
                            TestInfo.BITARCHIVE_BATCH_JOB_TIMEOUT
                        );*/
        assertEquals(TestInfo.BITARCHIVE_BATCH_JOB_TIMEOUT, 
                        batchTimeout > 0 ? batchTimeout :
                            TestInfo.BITARCHIVE_BATCH_JOB_TIMEOUT);
        
    }
}