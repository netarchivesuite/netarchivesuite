/* File:    $Id$
 * Version: $Revision$
 * Date:    $Date$
 * Author:  $Author$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2007 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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
package dk.netarkivet.common.distribute;

import javax.jms.Message;
import javax.jms.MessageListener;
import java.io.IOException;
import java.util.Date;

import junit.framework.TestCase;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.testutils.preconfigured.ReloadSettings;

public class SynchronizerTester extends TestCase {
    private static final ChannelID toQ = Channels.getAnyBa();
    private static final ChannelID replyToQ = Channels.getError();
    private JMSConnection con;

    private static final long DELAY_TIME = 20;
    private static final int WAIT_TIME = 5;

    ReloadSettings rs = new ReloadSettings();

    public void setUp() throws IOException {
        rs.setUp();
        JMSConnectionTestMQ.useJMSConnectionTestMQ();
        JMSConnectionTestMQ.clearTestQueues();
        con = JMSConnectionFactory.getInstance();
    }

    public void tearDown() {
        rs.tearDown();
    }


    /**
     * Verify that sendAndWaitForOneReply() fails when given null first input.
     */
    public void testSendAndWaitForOneReplyNull() {
        Synchronizer sync = new Synchronizer();
        try {
            sync.sendAndWaitForOneReply(null, 0);
            fail("testSendAndWaitForOneReply() should fail on null input");
        } catch (ArgumentNotValid e) {
            //Expected
        }
    }

    /**
     * Tests that everything works if the correct parameters are submitted to
     * the Synchronizer.
     */
    public void testNormalBehaviour() {
        NetarkivetMessage msg = new TestMessage(toQ, replyToQ);
        Synchronizer sync = new Synchronizer();
        /**
         * The sender is also the listener. Avoids the need for creating a
         * separate server thread for replying.
         */
        con.setListener(toQ, sync);
        SynchronizerRunner sr = new SynchronizerRunner(sync, msg, 0);

        /**
         * Pretest: Test everything is initialized.
         */
        // Test no messages have been received.
        assertNull("A reply shouldn't have been received before " +
                   "request/reply has been dispatched", sr.getReceived());

        /**
         * Send one message and check all queues
         */
        // Send message
        sr.sendAndWaitForOneReply();

        /**
         * Emulate a message has been received by onMessage()
         */
        // Wait for reply message
        waitUntilEnded(sr);

        // Check if a message have been received and its
        // the correct message.
        assertNotNull("A reply should have been received", sr.getReceived());
        assertEquals("The reply message should correspond to the request " +
                     "message", msg, sr.getReceived());
    }

    /**
     * Tests that the synchronizer doesn't trigger if it gets a wrong message
     * as a reply.
     */
    public void testWrongReplyToRequest() {
        NetarkivetMessage msg = new TestMessage(toQ, replyToQ);
        NetarkivetMessage msgOther = new TestMessage(replyToQ, toQ);
        Synchronizer sync = new Synchronizer();
        /**
         * The sender is also the listener. Avoids the need for creating a
         * separate server thread for replying.
         */
        con.setListener(replyToQ, sync);
        SynchronizerRunner sr = new SynchronizerRunner(sync, msg, 0);

        /**
         * Pretest: Test everything is initialized.
         */
        // Test no messages have been received.
        assertNull("A reply shouldn't have been received before " +
                   "request/reply has been dispatched", sr.getReceived());

        /**
         * Send one message and check all queues.
         */
        // Send message
        sr.sendAndWaitForOneReply();
        con.send(msgOther);

        // Check that no messages have been received.
        assertNull("A reply shouldn't have been received before reply has " +
                   "been dispatched", sr.getReceived());

        /**
         * Emulate a wrong message has been received by onMessage()
         */
        // Wait for reply message
        waitUntilEnded(sr);

        // Check that no messages have been received.
        assertNull("A reply shouldn't have been received when incorrect reply" +
                   " has been dispatched", sr.getReceived());
    }

    /**
     * Tests that sendAndWaitForOneReply isn't triggered if message with wrong
     * replyOfId is received by onMessage.
     */
    public void testOnMessageBehaviourOnWrongReplyID() {
        NetarkivetMessage msg = new TestMessage(toQ, replyToQ, "UNKNOWN_ID");
        Synchronizer sync = new Synchronizer();
        /**
         * The sender is also the listener. Avoids the need for creating a
         * separate server thread for replying.
         */
        con.setListener(toQ, sync);
        SynchronizerRunner sr = new SynchronizerRunner(sync, msg, 0);

        /**
         * Pretest: Test everything is initialized.
         */
        // Test no messages have been received.
        assertNull("A reply shouldn't have been received before " +
                   "request/reply has been dispatched", sr.getReceived());

        /**
         * Send one message and check all queues
         */
        // Send message
        sr.sendAndWaitForOneReply();

        // Wait for reply message
        waitUntilEnded(sr);

        // Check that no messages have been received.
        assertNull("A reply shouldn't have been received when incorrect reply" +
                   " has been dispatched", sr.getReceived());
    }

    /**
     * Tests that sendAndWaitForOneReply is triggered if message with correct
     * replyOfId is received by onMessage.
     */
    public void testOnMessageBehaviourOnCorrectReplyID() {
        NetarkivetMessage msg = new TestMessage(toQ, replyToQ);
        Synchronizer sync = new Synchronizer();
        /**
         * The sender is also the listener. Avoids the need for creating a
         * separate server thread for replying.
         */
        con.setListener(toQ, sync);
        SynchronizerRunner sr = new SynchronizerRunner(sync, msg, 0);

        /**
         * Pretest: Test everything is initialized.
         */
        // Test no messages have been received.
        assertNull("A reply shouldn't have been received before " +
                   "request/reply has been dispatched", sr.getReceived());

        /**
         * Send one message and check all queues
         */
        
        // 1. Send message
        sr.sendAndWaitForOneReply();

        // 2. Wait for reply message
        waitUntilEnded(sr);

        // 3. Check if a message have been received and its
        // the correct message.
        assertNotNull("A reply should have been received", sr.getReceived());
        assertEquals("The reply message should correspond to the request " +
                     "message", msg, sr.getReceived());
    }

    /**
     * This test checks that we handle being woken by other than expected
     * means.
     *
     * It first sets up a Synchronizer to listen for replies. Then a listener is
     * added to the toQ that just notifies the message (thus waking the
     * Synchronizer). While the Synchronizer doesn't leave the method since it
     * wasn't notified for having received a message, it does call getId() on
     * the message. Our message counts how many times getId() is called on it.
     * The first time is when sendAndWaitForOneReply starts, the second time is
     * when it is mistakenly woken up. When the second getId() is called, we
     * know we got the false notify and can continue the test.  No reply will be
     * available.
     */
    public void testWakingOnWrongNotify() {
        final NetarkivetMessage msg = new TestMessage(toQ, replyToQ) {
            int queries;

            public String getID() {
                queries++;
                // Should wait for the exact number of calls to getId before
                // waking this thread.
                if (queries == 2) {
                    synchronized (SynchronizerTester.this) {
                        SynchronizerTester.this.notifyAll();
                    }
                }
                return super.getID();
            }
        };
        /* This special synchronizer wakes up the listener early */
        Synchronizer sync = new Synchronizer();
        /*
         * The sender is also the listener. Avoids the need for creating a
         * separate server thread for replying.
         */
        con.setListener(replyToQ, sync);
        con.setListener(toQ, new MessageListener() {
            public void onMessage(Message message) {
                synchronized (msg) {
                    msg.notifyAll();
                }

            }
        });
        SynchronizerRunner sr = new SynchronizerRunner(sync, msg,
                                                       WAIT_TIME);

        /*
         * Pretest: Test everything is initialized.
         */
        // Test no messages have been received.
        assertNull("A reply shouldn't have been received before " +
                   "request/reply has been dispatched", sr.getReceived());

        /*
         * Send one message and check all queues
         */
        // Send message
        sr.sendAndWaitForOneReply();
        ((JMSConnectionTestMQ) con).waitForConcurrentTasksToFinish();
        try {
            synchronized (this) {
                this.wait(2000);
            }
            synchronized (msg) {
            }
        } catch (InterruptedException e) {

        }
        assertNull("A reply shouldn't have been received before " +
                   "request/reply has been dispatched", sr.getReceived());

        con.reply(msg);
        // Wait for reply message
        waitUntilEnded(sr);

        // Check if a message have been received and its
        // the correct message.
        assertNotNull("A reply should have been received", sr.getReceived());
        assertEquals("The reply message should correspond to the request " +
                     "message", msg, sr.getReceived());
    }

    /**
     * Tests that a timed-out synchronizer returns null.
     */
    public void testTimeout() throws InterruptedException {
        NetarkivetMessage msg = new TestMessage(toQ, replyToQ);
        Synchronizer sync = new Synchronizer();
        MessageListener listener = new DelayedReplier();
        con.setListener(toQ, listener);
        con.setListener(replyToQ, sync);
        SynchronizerRunner sr = new SynchronizerRunner(sync, msg,
                                                       WAIT_TIME);
        sr.sendAndWaitForOneReply();
        waitUntilEnded(sr);
        assertTrue("Should have returned from sendAndWaitForOneReply",
                   sr.isEnded());
        assertNull("Should have returned null, not " + sr.received,
                   sr.received);
    }

    /**
     * @param sr a Thread wrapper for the Synchronizer.
     */
    private void waitUntilEnded(SynchronizerRunner sr) {
        int loops = 0;
        while (!sr.isEnded() && loops < 20) {
            try {
                Thread.sleep(TestInfo.SHORT_TIME);
            } catch (InterruptedException e) {
            }
            loops++;
        }
    }


    public static class DelayedReplier implements MessageListener {
        public void onMessage(Message message) {
            TestMessage tm = (TestMessage) JMSConnection.unpack(message);
            TestMessage reply = new TestMessage(tm.getReplyTo(), tm.getTo(),
                                                tm.getReplyOfId());
            Date d = new Date();
            long finishTime = d.getTime() + DELAY_TIME;
            while (true) {
                d = new Date();
                if (d.getTime() > finishTime) {
                    JMSConnectionFactory.getInstance().send(reply);
                    return;
                }
            }

        }
    }


    /**
     * Used for testing. Runs a synchronizer.sendAndWaitForOneReply(msg,timeout)
     * in a new thread.
     */
    private static class SynchronizerRunner extends Thread {
        private Synchronizer sync;
        private NetarkivetMessage msg;
        private NetarkivetMessage received;
        private boolean ended;
        private int timeout;

        public SynchronizerRunner(Synchronizer sync,
                                  NetarkivetMessage msg, int timeout) {
            this.sync = sync;
            this.msg = msg;
            this.received = null;
            this.timeout = timeout;
            this.ended = false;
        }

        public void run() {
            received = sync.sendAndWaitForOneReply(msg, timeout);
            ended = true;
        }

        public void sendAndWaitForOneReply() {
            start();
        }

        public NetarkivetMessage getReceived() {
            return received;
        }

        public boolean isEnded() {
            return ended;
        }

    }

    /**
     * An extension of NetarkivetMessage that does not add functionality (except
     * public constructor).
     */
    private static class TestMessage extends NetarkivetMessage {
        public TestMessage(ChannelID to, ChannelID replyTo) {
            super(to, replyTo, "NetarkivetMessageTester.TestMessage");
        }

        public TestMessage(ChannelID to, ChannelID replyTo, String replyOfId) {
            super(to, replyTo, "NetarkivetMessageTester.TestMessage");
            this.replyOfId = replyOfId;
        }

    }
}
