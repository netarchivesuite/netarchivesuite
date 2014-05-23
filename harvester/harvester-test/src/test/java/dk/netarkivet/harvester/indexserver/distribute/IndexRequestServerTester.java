/* $Id$
 * $Revision$
 * $Date$
 * $Author$
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
package dk.netarkivet.harvester.indexserver.distribute;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import junit.framework.TestCase;

import dk.netarkivet.common.distribute.ChannelID;
import dk.netarkivet.common.distribute.JMSConnectionFactory;
import dk.netarkivet.common.distribute.JMSConnectionMockupMQ;
import dk.netarkivet.common.distribute.RemoteFile;
import dk.netarkivet.common.distribute.indexserver.RequestType;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.harvester.indexserver.FileBasedCache;
import dk.netarkivet.harvester.indexserver.MockupMultiFileBasedCache;
import dk.netarkivet.harvester.indexserver.distribute.IndexRequestMessage;
import dk.netarkivet.harvester.indexserver.distribute.IndexRequestServer;
import dk.netarkivet.testutils.ClassAsserts;
import dk.netarkivet.testutils.GenericMessageListener;
import dk.netarkivet.testutils.preconfigured.*;

public class IndexRequestServerTester extends TestCase {
    private static final Set<Long> JOB_SET = new HashSet<Long>(Arrays.asList(
            new Long[]{2L, 4L, 8L, 16L, 32L}));
    private static final Set<Long> JOB_SET2 = new HashSet<Long>(Arrays.asList(
            new Long[]{1L, 3L, 7L, 15L, 31L}));

    IndexRequestServer server;

    private UseTestRemoteFile ulrf = new UseTestRemoteFile();
    private PreventSystemExit pse = new PreventSystemExit();
    private PreserveStdStreams pss = new PreserveStdStreams();
    private MoveTestFiles mtf = new MoveTestFiles(TestInfo.ORIGINALS_DIR,
                                                  TestInfo.WORKING_DIR);
    private MockupJMS mjms = new MockupJMS();
    private MockupMultiFileBasedCache mmfbc = new MockupMultiFileBasedCache();
    ReloadSettings rs = new ReloadSettings();

    public void setUp() {
        rs.setUp();
        ulrf.setUp();
        mjms.setUp();
        mtf.setUp();
        pss.setUp();
        pse.setUp();
        mmfbc.setUp();
    }

    public void tearDown() {
        if (server != null) {
            server.close();
        }
        
        mmfbc.tearDown();
        pse.tearDown();
        pss.tearDown();
        mtf.tearDown();
        mjms.tearDown();
        ulrf.tearDown();
        rs.tearDown();
    }

    /**
     * Verify that factory method - does not throw exception - returns non-null
     * value.
     */
    public void testGetInstance() {
        assertNotNull("Factory method should return non-null object",
                      IndexRequestServer.getInstance());
        server = ClassAsserts.assertSingleton(IndexRequestServer.class);
    }

    /**
     * Verify that visit() - throws exception on null message or message that is
     * not ok - returns a non-ok message if handler fails with exception or no
     * handler registered
     */
    public void testVisitFailures() throws InterruptedException {
        server = IndexRequestServer.getInstance();
        mmfbc.setMode(MockupMultiFileBasedCache.Mode.FAILING);
        server.setHandler(RequestType.CDX, mmfbc);
        server.start();
        try {
            server.visit((IndexRequestMessage) null);
            fail("Should throw ArgumentNotValid on null");
        } catch (ArgumentNotValid e) {
            //expected
        }

        IndexRequestMessage irMsg = new IndexRequestMessage(
                RequestType.CDX, JOB_SET, null);
        JMSConnectionMockupMQ.updateMsgID(irMsg, "irMsg1");
        GenericMessageListener listener = new GenericMessageListener();
        JMSConnectionMockupMQ conn
                = (JMSConnectionMockupMQ) JMSConnectionFactory.getInstance();
        conn.setListener(irMsg.getReplyTo(), listener);

        server.visit(irMsg);
        conn.waitForConcurrentTasksToFinish();
        //Give a little time to reply
        Thread.sleep(200);
        conn.waitForConcurrentTasksToFinish();

        assertEquals("Should have received reply",
                     1, listener.messagesReceived.size());
        assertTrue("Should be the right type",
                   listener.messagesReceived.get(0)
                           instanceof IndexRequestMessage);
        IndexRequestMessage msg
                = (IndexRequestMessage) listener.messagesReceived.get(0);
        assertEquals("Should be the right message",
                     irMsg.getID(), msg.getID());
        assertFalse("Should not be OK", msg.isOk());

        irMsg = new IndexRequestMessage(RequestType.DEDUP_CRAWL_LOG, JOB_SET, null);
        JMSConnectionMockupMQ.updateMsgID(irMsg, "irMsg2");

        server.visit(irMsg);
        conn.waitForConcurrentTasksToFinish();
        //Give a little time to reply
        Thread.sleep(200);
        conn.waitForConcurrentTasksToFinish();

        assertEquals("Should have received reply",
                     2, listener.messagesReceived.size());
        assertTrue("Should be the right type",
                   listener.messagesReceived.get(1)
                           instanceof IndexRequestMessage);
        msg = (IndexRequestMessage) listener.messagesReceived.get(1);
        assertEquals("Should be the right message",
                     irMsg.getID(), msg.getID());
        assertFalse("Should not be OK", msg.isOk());

        irMsg = new IndexRequestMessage(RequestType.DEDUP_CRAWL_LOG, JOB_SET, null);
        JMSConnectionMockupMQ.updateMsgID(irMsg, "irMsg3");

    }

    /**
     * Verify that visit() - extracts correct info from message - calls the
     * appropriate handler - encodes the return value appropriately - sends
     * message back as reply
     */
    public void testVisitNormal() throws IOException, InterruptedException {
        for (RequestType t : RequestType.values()) {
            subtestVisitNormal(t);
        }
    }

    private void subtestVisitNormal(RequestType t) throws IOException,
                                                          InterruptedException {
        //Start server and set a handler
        mmfbc.tearDown();
        mmfbc.setUp();
        mmfbc.setMode(MockupMultiFileBasedCache.Mode.REPLYING);
        server = IndexRequestServer.getInstance();
        server.setHandler(t, mmfbc);
        server.start();

        //A message to visit with
        IndexRequestMessage irm = new IndexRequestMessage(t, JOB_SET, null);
        JMSConnectionMockupMQ.updateMsgID(irm, "irm-1");

        //Listen for replies
        GenericMessageListener listener = new GenericMessageListener();
        JMSConnectionMockupMQ conn
                = (JMSConnectionMockupMQ) JMSConnectionFactory.getInstance();
        ChannelID channelID = irm.getReplyTo();
        conn.setListener(channelID, listener);

        //Execute visit
        server.visit(irm);
        conn.waitForConcurrentTasksToFinish();
        //Give a little time to reply
        Thread.sleep(200);
        conn.waitForConcurrentTasksToFinish();

        assertHandlerCalledWithParameter(mmfbc);

        //Check reply is sent
        assertEquals("Should have received reply",
                     1, listener.messagesReceived.size());
        assertTrue("Should be the right type",
                   listener.messagesReceived.get(0)
                           instanceof IndexRequestMessage);
        IndexRequestMessage msg
                = (IndexRequestMessage) listener.messagesReceived.get(0);
        assertEquals("Should be the right message",
                     irm.getID(), msg.getID());
        assertTrue("Should be OK", msg.isOk());

        //Check contents of file replied
        File extractFile = File.createTempFile("extr", "act",
                                               TestInfo.WORKING_DIR);
        assertFalse("Message should not indicate directory",
                    msg.isIndexIsStoredInDirectory());
        RemoteFile resultFile = msg.getResultFile();
        resultFile.copyTo(extractFile);

        // Order in the JOB_SET and the extract file can't be guaranteed
        // So we are comparing between the contents of the two sets, not 
        // the order, which is dubious in relation to sets anyway.

        Set<Long> longFromExtractFile = new HashSet<Long>();
        FileInputStream fis = new FileInputStream(extractFile);
        try {
        for (int i = 0; i < JOB_SET.size(); i++) {
            longFromExtractFile.add(Long.valueOf(fis.read()));
        }
        assertEquals("End of file expected after this",
                     -1, fis.read());
        } catch (IOException e) {
            fail("Exception thrown: " + e);
        } finally {
            if (fis != null) {
                fis.close();
            }
        }
        assertTrue(
                "JOBSET, and the contents of extractfile should be identical",
                longFromExtractFile.containsAll(JOB_SET));

        FileUtils.remove(mmfbc.getCacheFile(JOB_SET));

        conn.removeListener(channelID, listener);
    }

    /**
     * Verify that a message sent to the index server queue is dispatched to the
     * appropriate handler if non-null and ok. Verify that no call is made if
     * message is null or not ok.
     */
    public void testIndexServerListener() throws InterruptedException {
        //Start server and set a handler
    	
        server = IndexRequestServer.getInstance();
        server.setHandler(RequestType.CDX, mmfbc);
        server.start();
        Thread.sleep(200); // necessary for the unittest to pass 

        //Send OK message
        IndexRequestMessage irm = new IndexRequestMessage(RequestType.CDX,
                                                          JOB_SET, null);
        JMSConnectionMockupMQ.updateMsgID(irm, "ID-0");
        JMSConnectionMockupMQ conn
                = (JMSConnectionMockupMQ) JMSConnectionFactory.getInstance();
        conn.send(irm);
        conn.waitForConcurrentTasksToFinish();
        //Give a little time to reply
        Thread.sleep(200);
        conn.waitForConcurrentTasksToFinish();

        assertHandlerCalledWithParameter(mmfbc);

        //Send not-OK message
        irm = new IndexRequestMessage(RequestType.CDX, JOB_SET, null);
        JMSConnectionMockupMQ.updateMsgID(irm, "ID-1");
        irm.setNotOk("Not OK");
        conn.send(irm);
        conn.waitForConcurrentTasksToFinish();
        //Give a little time to reply
        Thread.sleep(200);
        conn.waitForConcurrentTasksToFinish();

        //Check handler is NOT called
        assertEquals("Should NOT have called handler again", 1,
                     mmfbc.cacheCalled);
    }

    /**
     * Verify that - setHandler() throws exception on null values - calling
     * setHandler twice on same type replaces first handler
     */
    public void testSetHandler() throws InterruptedException {
        server = IndexRequestServer.getInstance();
        try {
            server.setHandler(RequestType.CDX, (FileBasedCache<Set<Long>>)null);
            fail("should have thrown exception on null value.");
        } catch (ArgumentNotValid e) {
            //expected
        }

        server = IndexRequestServer.getInstance();
        try {
            server.setHandler(null, mmfbc);
            fail("should have thrown exception on null value.");
        } catch (ArgumentNotValid e) {
            //expected
        }

        //Start server and set a handler
        server = IndexRequestServer.getInstance();
        server.setHandler(RequestType.CDX, mmfbc);

        //A message to visit with
        IndexRequestMessage irm = new IndexRequestMessage(RequestType.CDX,
                                                          JOB_SET, null);
        JMSConnectionMockupMQ.updateMsgID(irm, "dummyID");
        //Execute visit
        server.visit(irm);
        JMSConnectionMockupMQ conn
                = (JMSConnectionMockupMQ) JMSConnectionFactory.getInstance();
        conn.waitForConcurrentTasksToFinish();
        //Give a little time to reply
        Thread.sleep(200);
        conn.waitForConcurrentTasksToFinish();

        assertHandlerCalledWithParameter(mmfbc);

        //Set new handler
        MockupMultiFileBasedCache mjic2 = new MockupMultiFileBasedCache();
        mjic2.setUp();
        server.setHandler(RequestType.CDX, mjic2);

        //Execute new visit
        irm = new IndexRequestMessage(RequestType.CDX, JOB_SET, null);
        JMSConnectionMockupMQ.updateMsgID(irm, "dummyID");
        server.visit(irm);
        conn.waitForConcurrentTasksToFinish();
        //Give a little time to reply
        Thread.sleep(200);
        conn.waitForConcurrentTasksToFinish();

        //Check the first handler is not called again
        assertEquals("Handler should NOT be called", 1,
                     mmfbc.cacheCalled);

        assertHandlerCalledWithParameter(mjic2);
        mjic2.tearDown();
    }

    public void testUnblocking() throws InterruptedException {
        mmfbc.setMode(MockupMultiFileBasedCache.Mode.WAITING);
        server = IndexRequestServer.getInstance();
        server.setHandler(RequestType.CDX, mmfbc);
        server.start();
        Thread.sleep(200); // necessary for the unittest to pass 

        //A message to visit with
        IndexRequestMessage irm = new IndexRequestMessage(RequestType.CDX,
                                                          JOB_SET, null);
        //Another message to visit with
        IndexRequestMessage irm2 = new IndexRequestMessage(RequestType.CDX,
                                                           JOB_SET2, null);

        //Listen for replies
        GenericMessageListener listener = new GenericMessageListener();
        JMSConnectionMockupMQ conn
                = (JMSConnectionMockupMQ) JMSConnectionFactory.getInstance();
        conn.setListener(irm.getReplyTo(), listener);

        //Send both messages
        conn.send(irm);
        conn.send(irm2);
        conn.waitForConcurrentTasksToFinish();
        //Give a little time to reply
        Thread.sleep(200);
        conn.waitForConcurrentTasksToFinish();

        assertEquals("Should have replies from both messages",
                     2, listener.messagesReceived.size());

        //Now, we test that the threads have actually run simultaneously, and
        //have awaken each other; not just timed out.
        assertTrue("Threads should have been woken up", mmfbc.woken);
    }


    private void assertHandlerCalledWithParameter(
            MockupMultiFileBasedCache mjic) {
        //Check the handler is called
        assertEquals("Handler should be called", 1, mjic.cacheCalled);
        assertEquals("Handler should be called with right parameter",
                     JOB_SET, mjic.cacheParameter);
    }
}
