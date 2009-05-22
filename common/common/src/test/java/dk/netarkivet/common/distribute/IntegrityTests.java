/* File:    $Id$
 * Date:    $Date$
 * Revision:$Revision$
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

import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TopicConnection;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import junit.framework.TestCase;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.exceptions.UnknownID;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.RememberNotifications;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.harvester.HarvesterSettings;
import dk.netarkivet.harvester.datamodel.JobPriority;
import dk.netarkivet.testutils.FileAsserts;
import dk.netarkivet.testutils.LogUtils;
import dk.netarkivet.testutils.TestUtils;
import dk.netarkivet.testutils.preconfigured.PreventSystemExit;
import dk.netarkivet.testutils.preconfigured.ReloadSettings;

/**
 * Tests JMSConnection, the class that handles all JMS operations for Netarkivet.
 */
public class IntegrityTests extends TestCase {
    /**
     * We need two arbitrary (but different) queues for testing send and reply.
     */
    private static final ChannelID sendQ = Channels.getAnyLowpriorityHaco();

    private static final ChannelID replyQ = Channels.getTheSched();

    private static final ChannelID sendTopic = Channels.getAllBa();

    private static final int WAIT_MS = 9000;

    /**
     * Used in methods testNListenersToTopic and
     * testMoreThanThreeListenersToQueue, should be set to > 3:
     */
    private static final int NO_OF_LISTENERS = 4;

    private static final PreventSystemExit pes = new PreventSystemExit();

    private JMSConnection conn;

    ReloadSettings rs = new ReloadSettings();

    public void setUp() {
        rs.setUp();
        Settings.set(CommonSettings.JMS_BROKER_CLASS,
                     JMSConnectionSunMQ.class.getName());
        /* Do not send notification by email. Print them to STDOUT. */
        Settings.set(CommonSettings.NOTIFICATIONS_CLASS,
                     RememberNotifications.class.getName());
        JMSConnectionFactory.getInstance().close();
        conn = JMSConnectionFactory.getInstance();
        pes.setUp();
    }

    public void tearDown() {
        ChannelsTester.resetChannels();
        JMSConnectionFactory.getInstance().close();
        pes.tearDown();
        rs.tearDown();
    }

    /**
     * Verify that we can remove a given MessageListener from a given Queue.
     * Note that this method does not test that a MessageListener can removed from a Topic
     * - at the moment we have no need for that.
     * If the need arises, a test case should be written for Topics as well.
     */
    public void testRemoveListener() {
        TestMessage testMsg = new TestMessage(sendQ, replyQ);
        TestMessageListener listener1 = new TestMessageListener(testMsg);
        TestMessageListener listener2 = new TestMessageListener(testMsg);
        TestMessageListener listener3 = new TestMessageListener(testMsg);
        conn.setListener(sendQ, listener1);
        conn.setListener(sendQ, listener2);

        synchronized (this) {
            conn.send(testMsg);
            try {
                wait(WAIT_MS);
            } catch (InterruptedException e) {
                //This is expected
            }
        }

        //The test is ok if exactly one of the listeners is ok.
        boolean ok = listener1.getOk() ? !listener2.getOk() : listener2.getOk();
        assertTrue("Expected test message '" + testMsg.toString() +
                "'\nto be received by exactly one listener within " + WAIT_MS + " milliseconds.", ok);

        //Removing listener1 - test that a message will only be received by listener2:
        // This also tests fix of bug 235:
        conn.removeListener(sendQ, listener1);
        listener1.resetOkState();
        listener2.resetOkState();

        synchronized (this) {
            conn.send(testMsg);
            try {
                wait(WAIT_MS);
            } catch (InterruptedException e) {
                //This is expected
            }
        }

        assertTrue("Expected test message " + testMsg.toString()
                + "\nshould be received by listener2 only",
                !listener1.getOk() && listener2.getOk());

        //Now removing listener2 and setting a third listener
        //Test that a message is neither received by listener1 nor listener2:
        conn.removeListener(sendQ, listener2);
        conn.setListener(sendQ, listener3);
        listener1.resetOkState();
        listener2.resetOkState();
        listener3.resetOkState();

        synchronized (this) {
            conn.send(testMsg);
            try {
                wait(WAIT_MS);
            } catch (InterruptedException e) {
                //This is expected
            }
        }

        assertTrue("Expected test message " + testMsg.toString()
                + "\nshould not be received by neither listener1 "
                + "nor listener2", !listener1.getOk() && !listener2.getOk());

        conn.setListener(sendQ, listener1);

        //This should be quite fast, the message is already waiting.
        synchronized (this) {
            try {
                if (!listener1.getOk()) {
                    wait(WAIT_MS);
                }
            } catch (InterruptedException e) {
                //This is expected
            }
        }

        //The test is ok if exactly one of the listeners is ok.
        boolean okAfterRemovalAndReset = listener1.getOk() ? !listener3.getOk() : listener3.getOk();
        assertTrue("Expected test message " + testMsg.toString()
                + "\nto be received by either listener1 or listener3 "
                + "after reading it", okAfterRemovalAndReset);

    }

    /**
     * Verify that a sent message is only delivered to one listener
     * (this is point-to-point semantics).
     * <p/>
     * This is an integrity test because:  It tests that JMS behaves as
     * expected.
     */
    public void testTwoListenersSend() {
        TestMessage testMsg = new TestMessage(sendQ, replyQ);
        TestMessageListener listener1 = new TestMessageListener(testMsg);
        TestMessageListener listener2 = new TestMessageListener(testMsg);
        conn.setListener(sendQ, listener1);
        conn.setListener(sendQ, listener2);
        synchronized (this) {
            conn.send(testMsg);
            try {
                wait(WAIT_MS);
            } catch (InterruptedException e) {
                //This is expected
            }
        }

        //The test is ok if exactly one of the listeners is ok.
        int listeners = 0;
        if (listener1.getOk()) {
            listeners++;
        }
        if (listener2.getOk()) {
            listeners++;
        }
        assertEquals("Expected test message: (" + testMsg.toString() +
                ") to be received by exactly one listener within " + WAIT_MS + " milliseconds.",
                1, listeners);
    }

    /**
     * Test that we can subscribe more than three (3) listeners to a queue and
     * that exactly one receives the message
     * <p/>
     * This is an integrity test because:  We are testing that JMS itself
     * behaves correctly.
     * <p/>
     * This is used for testing the Platform Ed. Enterprise License feature of
     * queue
     * Requires a running broker with Platform Ed. Enterprise License
     * (e.g. trial license: /opt/imq/bin/imqbrokerd -license try)
     */
    public void testMoreThanThreeListenersToQueue() {
        TestMessage testMsg = new TestMessage(sendQ, replyQ);
        List<MessageListener> listeners = new ArrayList<MessageListener>(NO_OF_LISTENERS);

        for (int i = 0; i < NO_OF_LISTENERS; i++) {
            TestMessageListener aListener = new TestMessageListener(testMsg);
            listeners.add(aListener);
            conn.setListener(sendQ, aListener);
        }

        synchronized (this) {
            conn.send(testMsg);
            // Listen for two notifies in case two messages are received
            for (int i = 0; i < 2; i++) {
                try {
                    wait(WAIT_MS);
                } catch (InterruptedException e) {
                    //This is expected
                }
            }
        }

        int oks = 0;
        for (int i = 0; i < NO_OF_LISTENERS; i++) {
            if (((TestMessageListener) listeners.get(i)).getOk()) {
                ++oks;
            }
        }

        assertTrue("Expected test message " + testMsg.toString() +
                "to be received by exactly 1 of the " + NO_OF_LISTENERS + " listeners within " + WAIT_MS + " milliseconds. Received " + oks, (oks == 1));
    }

    /**
     * Verify that a sent message arrives unchanged to a listener.
     */
    public void testListenAndSend() {
        TestMessage testMsg = new TestMessage(sendQ, replyQ);
        TestMessageListener listener = new TestMessageListener(testMsg);
        conn.setListener(sendQ, listener);
        synchronized (this) {
            conn.send(testMsg);
            try {
                wait(WAIT_MS);
            } catch (InterruptedException e) {
                //This is expected
            }
        }
        assertTrue("Expected test message >" + testMsg.toString() + "< to have arrived on queue " + replyQ +
                " within " + WAIT_MS + " milliseconds.", listener.getOk());
    }

    /**
     * Verify that a replied message on a queue arrives unchanged to a listener.
     */
    public void testListenAndReply() {
        TestMessage testMsg = new TestMessage(sendQ, replyQ);
        conn.send(testMsg);
        TestMessageListener listener = new TestMessageListener(testMsg);
        conn.setListener(replyQ, listener);
        synchronized (this) {
            conn.reply(testMsg);
            try {
                wait(WAIT_MS);
            } catch (InterruptedException e) {
                //This is expected
            }
        }
        assertTrue("Expected test message " + testMsg.toString() + "to have arrived on queue " + replyQ +
                " within " + WAIT_MS + " milliseconds", listener.getOk());
    }

    /**
     * Test that we can subscribe more than one listener to a topic and that they
     * all receive the message.
     */
    public void testNListenersToTopic() {
        TestMessage testMsg = new TestMessage(sendTopic, replyQ);
        List<MessageListener> listeners = new ArrayList<MessageListener>(NO_OF_LISTENERS);

        for (int i = 0; i < NO_OF_LISTENERS; i++) {
            TestMessageListener aListener = new TestMessageListener(testMsg);
            listeners.add(aListener);
            conn.setListener(sendTopic, aListener);
        }

        synchronized (this) {
            conn.send(testMsg);
            for (int i = 0; i < NO_OF_LISTENERS; i++) {
                try {
                    wait(WAIT_MS);
                } catch (InterruptedException e) {
                    //This is expected
                }
                boolean all_ok = true;
                for (int j = 0; j < NO_OF_LISTENERS; j++) {
                    if (((TestMessageListener) listeners.get(j)).getOk()) {
                        all_ok = false;
                    }
                }
                if (all_ok) {
                    break;
                }
            }
        }

        List<MessageListener> oks = new ArrayList<MessageListener>(NO_OF_LISTENERS);
        for (int i = 0; i < NO_OF_LISTENERS; i++) {
            if (((TestMessageListener) listeners.get(i)).getOk()) {
                oks.add(listeners.get(i));
            }
        }

        assertEquals("Expected test message " + testMsg.toString()
                + "to be received by exactly " + NO_OF_LISTENERS
                + " listeners within " + WAIT_MS + " milliseconds, but got "
                + oks,
                NO_OF_LISTENERS, oks.size());
    }

    /**
     * Tests that no messages are generated twice.
     * @throws Exception
     */
    public void testMsgIds() throws Exception {
        //Just for emptying the queue
        String priority1 = Settings.get(
                HarvesterSettings.HARVEST_CONTROLLER_PRIORITY);
        ChannelID result1;
        if (priority1.equals(JobPriority.LOWPRIORITY.toString())) {
            result1 = Channels.getAnyLowpriorityHaco();
        } else
        {
            if (priority1.equals(JobPriority.HIGHPRIORITY.toString())) {
                result1 = Channels.getAnyHighpriorityHaco();
            } else
            throw new UnknownID(priority1 + " is not a valid priority");
        }
        conn.setListener(Channels.getAnyBa(),
                new TestMessageListener(new TestMessage(Channels.getAnyBa(),
                        result1)));
        Set<String> set = new HashSet<String>();

        for (int i = 0; i < 100; i++) {
            String priority = Settings.get(
                    HarvesterSettings.HARVEST_CONTROLLER_PRIORITY);
            ChannelID result;
            if (priority.equals(JobPriority.LOWPRIORITY.toString())) {
                result = Channels.getAnyLowpriorityHaco();
            } else
            {
                if (priority.equals(JobPriority.HIGHPRIORITY.toString())) {
                    result = Channels.getAnyHighpriorityHaco();
                } else
                throw new UnknownID(priority + " is not a valid priority");
            }
            NetarkivetMessage msg
                    = new TestMessage(Channels.getAnyBa(), result);
            conn.send(msg);
            assertTrue("No msg ID must be there twice", set.add(msg.getID()));
            Logger log = Logger.getLogger(getClass().getName());
            log.finest("Generated message ID " + msg.getID());
        }
        conn = JMSConnectionFactory.getInstance();
        for (int i = 0; i < 100; i++) {
            String priority = Settings.get(
                    HarvesterSettings.HARVEST_CONTROLLER_PRIORITY);
            ChannelID result;
            if (priority.equals(JobPriority.LOWPRIORITY.toString())) {
                result = Channels.getAnyLowpriorityHaco();
            } else
            {
                if (priority.equals(JobPriority.HIGHPRIORITY.toString())) {
                    result = Channels.getAnyHighpriorityHaco();
                } else
                throw new UnknownID(priority + " is not a valid priority");
            }
            NetarkivetMessage msg
                    = new TestMessage(Channels.getAnyBa(), result);
            conn.send(msg);
            assertTrue("No msg ID must be there twice", set.add(msg.getID()));
            Logger log = Logger.getLogger(getClass().getName());
            log.finest("Generated message ID " + msg.getID());
        }
        //To test messages are unique between processes, run the unittest by two
        // JVMs simultanously (increase the number of messages generated or
        // insert delay to have time for starting two processes).
        // Then compare the logs:
        //
        //$ grep Generated netarkivtesta.log | cut -f 3- > a
        //$ grep Generated netarkivtestb.log | cut -f 3- > b
        //$ cat a b | sort | uniq -d
        //
        //This should produce no output unless two message IDs are equal.
    }

    /**
     * Tries to generate the mysterious NullPointerException of bug 220.
     * @throws java.io.IOException
     */
    public void testProvokeNullPointer() throws IOException {
        Settings.set(CommonSettings.REMOTE_FILE_CLASS, FTPRemoteFile.class.getName());
        File testFile1 = new File("tests/dk/netarkivet/common/distribute/data/originals/arc_record0.txt");
        File LOGFILE = new File("tests/testlogs/netarkivtest.log");
        int tries = 100;
        for (int i = 0; i < tries; i++) {
            RemoteFile rf = RemoteFileFactory.getInstance(testFile1, true, false,
                                                          true);
            rf.cleanup();
        }
        LogUtils.flushLogs(FTPRemoteFile.class.getName());
        LogUtils.flushLogs(this.getClass().getName());
        String log = FileUtils.readFile(LOGFILE);
        FileAsserts.assertFileNotContains(log, LOGFILE, "NullPointerException");
    }

    /**
     * Tries to send a message to a Queue.
     *  - Makes a TestMessageConsumer, that listens to the ArcRepository Queue.
     *  - Sends a message to that queue.
     *  - Verifies, that this message is sent and received un-modified.
     * @throws InterruptedException
     */
    public void testQueueSendMessage() throws InterruptedException {
        TestMessageConsumer mc = new TestMessageConsumer();
        conn.setListener(Channels.getTheRepos(), mc);

        NetarkivetMessage nMsg = new TestMessage(Channels.getTheRepos(),
                Channels.getError(), "testQueueSendMessage");
        synchronized (mc) {
            conn.send(nMsg);
            mc.wait();
        }
        assertEquals(
                "Arcrepos queue MessageConsumer should have received message.",
                nMsg.toString(), mc.nMsg.toString());
    }

    /**
     * Sets up 3 message consumers, all listening on the same channel.
     * Then sends a message on that channel.
     * Verify, that the message is received by all three consumers.
     * @throws InterruptedException
     */
    public void testTopicSendMessage() throws InterruptedException {
        TestMessageConsumer mc1 = new TestMessageConsumer();
        TestMessageConsumer mc2 = new TestMessageConsumer();
        TestMessageConsumer mc3 = new TestMessageConsumer();
        conn.setListener(Channels.getAllBa(), mc1);
        conn.setListener(Channels.getAllBa(), mc2);
        conn.setListener(Channels.getAllBa(), mc3);

        NetarkivetMessage nMsg = new TestMessage(Channels.getAllBa(), Channels
                .getError(), "testTopicSendMessage");
        conn.send(nMsg);
        synchronized (mc1) {
            if (mc1.nMsg == null) mc1.wait();
        }
        synchronized (mc2) {
            if (mc2.nMsg == null) mc2.wait();
        }
        synchronized (mc3) {
            if (mc3.nMsg == null) mc3.wait();
        }

        assertEquals(
                "Arcrepos queue MessageConsumer should have received message.",
                nMsg.toString(), mc1.nMsg.toString());
        assertEquals(
                "Arcrepos queue MessageConsumer should have received message.",
                nMsg.toString(), mc2.nMsg.toString());
        assertEquals(
                "Arcrepos queue MessageConsumer should have received message.",
                nMsg.toString(), mc3.nMsg.toString());
    }

     /**
     * Tests that a JMSConnection will trigger System.exit() (by default) when
     * a JMSException is thrown.
     * FIXME: This is no longer true. The JMSConnection con is now its own exceptionhandler!
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     * @throws JMSException
     */
     public void testExitOnExceptionInTopic() 
     throws NoSuchFieldException, IllegalAccessException, JMSException {
         if (!TestUtils.runningAs("CSR")) {
             return;
         }
         JMSConnection con = JMSConnectionFactory.getInstance();
         Field topicConnectionField = con.getClass().getSuperclass().getDeclaredField("myTConn");
         topicConnectionField.setAccessible(true);
         TopicConnection qc = (TopicConnection) topicConnectionField.get(con);
         ExceptionListener qel = qc.getExceptionListener();
         assertNotNull("There should be an exception listener on the queue", qel);
         try {
             qel.onException(new JMSException("A JMS Exception", 
                     JMSConnectionSunMQ.SESSION_IS_CLOSED));
             //fail("Should throw a security exception trying to exit on a unit test");
         } catch (SecurityException e) {
             //expected
         }
     }

    private class TestMessageListener implements MessageListener {
        private NetarkivetMessage expected;
        private boolean ok;

        public TestMessageListener(TestMessage tm) {
            expected = tm;
            ok = false;
        }

        public synchronized boolean getOk() {
            return ok;
        }

        public void resetOkState() {
            ok = false;
        }

        public void onMessage(Message msg) {
            synchronized (IntegrityTests.this) {
                NetarkivetMessage nMsg = JMSConnection.unpack(msg);
                ok = nMsg.equals(expected);
                IntegrityTests.this.notifyAll();
            }
        }
    }

    /**
     * A simple subclass of NetarkivetMessage to be used for test purposes only.
     * The only added functionality is that toString() outputs a representation
     * of the "entire visible state" of the message.
     */
    private static class TestMessage extends NetarkivetMessage {
        String testID;
        public TestMessage(ChannelID sendQ, ChannelID recQ) {
            super(sendQ, recQ, "IntegrityTests.TestMessage");
        }

        public TestMessage(ChannelID to, ChannelID replyTo, String testID) {
            super(to, replyTo, "NetarkivetMessageTester.TestMessage");
            this.testID = testID;
        }

        public boolean equals(Object o2) {
            if (o2 == this || !(o2 instanceof NetarkivetMessage)) {
                return true;
            }
            try {
                return getID().equals(((NetarkivetMessage) o2).getID());
            } catch (Exception e) {
                return false;
            }
        }

        public String toString() {
            return super.toString() + "(" + getTo().toString() + "," + getReplyTo().toString() + ")" + ":" + isOk()
                    + (isOk() ? "" : getErrMsg());
        }

        public String getTestID() {
            return testID;
        }
    }

    public static final class TestMessageConsumer implements MessageListener {

        public NetarkivetMessage nMsg;

        JMSConnection con;

        public TestMessageConsumer() {
            con = JMSConnectionFactory.getInstance();
        }

        public void onMessage(Message msg) {
            synchronized (this) {
                nMsg = JMSConnection.unpack(msg);
                this.notifyAll();
            }
        }
    }
}