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

package dk.netarkivet.harvester.harvesting.distribute;

import java.util.concurrent.atomic.AtomicInteger;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueReceiver;
import javax.jms.QueueSession;
import javax.jms.Session;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.sun.messaging.QueueConnectionFactory;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.distribute.ChannelID;
import dk.netarkivet.common.distribute.Channels;
import dk.netarkivet.common.distribute.JMSConnectionSunMQ;
import dk.netarkivet.common.utils.RememberNotifications;
import dk.netarkivet.common.utils.Settings;

/**
 * A test of the behaviour if onMessage() hangs when there is more than one listener to a queue.
 */
@Ignore("Needs to be running in deploy-test module according to junit3 test suite")
public class HangingListenerTest {

    public static AtomicInteger messages_received = new AtomicInteger(0);

    @Before
    public void setUp() {
        Settings.set(CommonSettings.JMS_BROKER_CLASS, JMSConnectionSunMQ.class.getName());
        // JMSConnection.getInstance();
        Channels.reset();
        /* Do not send notification by email. Print them to STDOUT. */
        Settings.set(CommonSettings.NOTIFICATIONS_CLASS, RememberNotifications.class.getName());
    }

    @After
    public void tearDown() {
        // JMSConnection.getInstance().cleanup();
    }

    /**
     * Tests what happens if we have a blocking listener. It appears that by default, a listener may pre-queue messages
     * even while it is blocked processing the previous message. This is not desirable behaviour.
     *
     * @throws InterruptedException
     * @throws JMSException
     */
    // Out commented to avoid reference to archive module from harvester module.
    @Test
    @Ignore("commented out to avoid reference to archive module")
    public void testNotListeningWhileProcessingSunMQ() throws InterruptedException, JMSException {
        // if (!Settings.get(CommonSettings.JMS_BROKER_CLASS).equals(JMSConnectionSunMQ.class.getName())) {
        // fail("Wrong message queue for test");
        // }
        // JMSConnectionFactory.getInstance().cleanup();
        // JMSConnectionFactory.getInstance();
        // long blockingTime = 1000l;
        // int messagesSent = 10;
        // BlockingListener nonBlocker = new BlockingListener();
        // BlockingListener blocker = new BlockingListener(true, blockingTime);
        // ChannelID theQueue = Channels.getTheBamon();
        // JMSConnection con = JMSConnectionFactory.getInstance();
        // MiniConnectionSunMQ con2 = new MiniConnectionSunMQ();
        // // Set the production JMS connection to listen with the blocking
        // // listener
        // con.setListener(theQueue, blocker);
        // con2.setListener(theQueue, nonBlocker);
        // for (int i = 0; i < messagesSent; i++) {
        // NetarkivetMessage msg = new BatchMessage(theQueue, new ChecksumJob(), "ONE");
        // con.send(msg);
        // }
        // while(HangingListenerTest.messages_received.get() < messagesSent) {}
        // Thread.sleep(2*blockingTime);
        // assertEquals("Blocking listener should only have been called once", 1, blocker.called);
        // System.out.println("Repeat:");
        // for (int i = 0; i < messagesSent; i++) {
        // NetarkivetMessage msg = new BatchMessage(theQueue, new ChecksumJob(), "ONE");
        // con.send(msg);
        // }
        // while(HangingListenerTest.messages_received.get() < messagesSent) {}
        // Thread.sleep(2*blockingTime);
        // assertEquals("Blocking listener should now have been called twice", 2, blocker.called);
        // con.cleanup();
        // con2.cleanup();
    }

    public static class MiniConnectionSunMQ {

        QueueSession myQSess;
        QueueConnection myQConn;

        public MiniConnectionSunMQ() throws JMSException {
            String host = Settings.get(JMSConnectionSunMQ.JMS_BROKER_HOST);
            String port = Settings.get(JMSConnectionSunMQ.JMS_BROKER_PORT);
            QueueConnectionFactory cFactory = new QueueConnectionFactory();
            ((com.sun.messaging.ConnectionFactory) cFactory).setProperty(
                    com.sun.messaging.ConnectionConfiguration.imqBrokerHostName, host);
            ((com.sun.messaging.ConnectionFactory) cFactory).setProperty(
                    com.sun.messaging.ConnectionConfiguration.imqBrokerHostPort, String.valueOf(port));
            ((com.sun.messaging.ConnectionFactory) cFactory).setProperty(
                    com.sun.messaging.ConnectionConfiguration.imqConsumerFlowLimit, "1");
            myQConn = cFactory.createQueueConnection();
            myQSess = myQConn.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
            myQConn.start();
        }

        public void setListener(ChannelID mq, MessageListener ml) throws JMSException {
            Queue queue = new com.sun.messaging.Queue(mq.getName());
            QueueReceiver myQueueReceiver = myQSess.createReceiver(queue);
            myQueueReceiver.setMessageListener(ml);
        }

        public void cleanup() throws JMSException {
            myQConn.close();
        }
    }

    public static class BlockingListener implements MessageListener {

        public int called = 0;
        long timeToBlockMS;
        boolean isBlocking;

        public BlockingListener() {
            this(false, 0);
        }

        public BlockingListener(boolean block, long timeToBlockMS) {
            this.timeToBlockMS = timeToBlockMS;
            isBlocking = block;
        }

        public void onMessage(Message message) {
            called++;
            HangingListenerTest.messages_received.addAndGet(1);
            if (!isBlocking) {
                System.out.println("Message received by non-blocking listener at " + System.currentTimeMillis());
                return;
            }
            System.out.println("Message received by blocking listener at " + System.currentTimeMillis());
            try {
                Thread.sleep(timeToBlockMS);
            } catch (InterruptedException e) {
                // Expected ??
            }
            System.out.println("Blocking listener returned at " + System.currentTimeMillis());
        }
    }

}
