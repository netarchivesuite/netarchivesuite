/*
 * #%L
 * Netarchivesuite - common - test
 * %%
 * Copyright (C) 2005 - 2014 The Royal Danish Library, the Danish State and University Library,
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

package dk.netarkivet.common.distribute;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import javax.jms.BytesMessage;
import javax.jms.Connection;
import javax.jms.ConnectionConsumer;
import javax.jms.ConnectionFactory;
import javax.jms.ConnectionMetaData;
import javax.jms.Destination;
import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.QueueBrowser;
import javax.jms.QueueReceiver;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.ServerSessionPool;
import javax.jms.Session;
import javax.jms.StreamMessage;
import javax.jms.TemporaryQueue;
import javax.jms.TemporaryTopic;
import javax.jms.TextMessage;
import javax.jms.Topic;
import javax.jms.TopicSubscriber;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.exceptions.NotImplementedException;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.utils.batch.TestJob;

/**
 * A MockUp message queue, that generates a connection and destinations suitable for testing.
 */
@SuppressWarnings({"rawtypes", "unused", "serial"})
public class JMSConnectionMockupMQ extends JMSConnection {
    private static final Log log = LogFactory.getLog(JMSConnectionMockupMQ.class);

    /**
     * A set of threads where onMessage has been called. This object is notified when all threads have finished
     * executing.
     */
    protected final Set<Thread> concurrentTasksToComplete = Collections.synchronizedSet(new HashSet<Thread>());
    /**
     * A map from channelnames to destinations.
     */
    protected Map<String, TestDestination> destinations;

    /** Constructor. Does nothing, initConnection is used for setup. */
    protected JMSConnectionMockupMQ() {
        super();
        log.info("Creating instance of " + getClass().getName());
    }

    /**
     * Get the singleton, and initialise it if it is new.
     *
     * @return A JMSConnection
     */
    public static synchronized JMSConnection getInstance() {
        if (instance == null) {
            instance = new JMSConnectionMockupMQ();
            instance.initConnection();
        }
        return instance;
    }

    protected void initConnection() {
        super.initConnection();
        destinations = new HashMap<String, TestDestination>();
    }

    protected ConnectionFactory getConnectionFactory() throws JMSException {
        return new JMSConnectionMockupMQ.TestConnectionFactory();
    }

    protected Destination getDestination(String channelName) throws JMSException {
        if (destinations == null) {
            destinations = new HashMap<String, TestDestination>();
        }
        TestDestination destination = destinations.get(channelName);
        if (destination == null) {
            if (Channels.isTopic(channelName)) {
                destination = new TestTopic(channelName);
            } else {
                destination = new TestQueue(channelName);
            }
            destinations.put(channelName, destination);
        }
        return destination;
    }

    /**
     * Does nothing.
     *
     * @param e The exception to ignore :-)
     */
    public void onException(JMSException e) {
        // Ignore
    }

    /** Clean up singleton instance and internal state */
    public void cleanup() {
        super.cleanup();
        instance = null;
        concurrentTasksToComplete.clear();
        destinations = null;
    }

    /**
     * Waits until all threads where onMessage has been called have finished executing.
     */
    public void waitForConcurrentTasksToFinish() {
        synchronized (concurrentTasksToComplete) {
            while (!concurrentTasksToComplete.isEmpty()) {
                try {
                    concurrentTasksToComplete.wait();
                } catch (InterruptedException e) {
                    // Go on
                }
            }
        }
    }

    /**
     * Wrap a NetarkivetMessage into an ObjectMessage
     *
     * @param nMsg a NetarkivetMessage
     *
     * @return an ObjectMessage
     */
    public static ObjectMessage getObjectMessage(NetarkivetMessage nMsg) {
        return new TestObjectMessage(nMsg);
    }

    /**
     *
     */
    public static void clearTestQueues() {
        JMSConnectionMockupMQ.getInstance().initConnection();
    }

    /**
     * For testing purposes: Set the ID of a message
     *
     * @param msg The message to set the id on
     * @param id the new id
     */
    public static void updateMsgID(NetarkivetMessage msg, String id) {
        msg.updateId(id);
    }

    /**
     *
     */
    public static void useJMSConnectionMockupMQ() {
        // JMSConnectionFactory.getInstance().cleanup();
        Settings.set(CommonSettings.JMS_BROKER_CLASS, "dk.netarkivet.common.distribute.JMSConnectionMockupMQ");
        JMSConnectionMockupMQ.getInstance().cleanup();
    }

    /**
     * Returns a list of all MessageListeners listening to a particular channel
     *
     * @param channel The channel
     * @return list of listeners
     */
    public List<MessageListener> getListeners(ChannelID channel) {
        TestDestination destination = destinations.get(channel.getName());
        if (destination == null) {
            return Collections.emptyList();
        } else {
            return new ArrayList<MessageListener>(destination.listeners);
        }
    }

    /**
     *
     * @param job
     * @param channelID
     * @return
     */
    public boolean isSentToChannel(TestJob job, ChannelID channelID) {
        TestDestination destination = destinations.get(channelID.getName());
        for (TestObjectMessage sentMessage : destination.sent) {
            NetarkivetMessage message = unpack(sentMessage);
        }
        return false;
    }

    /**
     *
     */
    protected static class TestConnectionFactory implements ConnectionFactory {

        /**
         *
         * @return
         * @throws JMSException
         */
        public Connection createConnection() throws JMSException {
            return new TestConnection();
        }

        /**
         *
         * @param string
         * @param string1
         * @return
         * @throws JMSException
         */
        public Connection createConnection(String string, String string1) throws JMSException {
            return new TestConnection();
        }
    }

    /**
     *
     */
    protected static class TestConnection implements Connection {

        /**
         *
         */
        public boolean isStarted = false;
        private ExceptionListener exceptionListener;

        /**
         *
         * @param b
         * @param i
         * @return
         * @throws JMSException
         */
        public Session createSession(boolean b, int i) throws JMSException {
            return new TestSession();
        }

        /**
         *
         * @throws JMSException
         */
        public void start() throws JMSException {
            if (isStarted) {
                throw new IllegalStateException(this + " already started");
            }
            isStarted = true;
        }

        /**
         *
         * @throws JMSException
         */
        public void stop() throws JMSException {
            isStarted = false;
        }

        /**
         *
         * @throws JMSException
         */
        public void close() throws JMSException {
            isStarted = false;
            // TODO: Methods should start throwing exceptions
        }

        /**
         *
         * @param exceptionListener
         * @throws JMSException
         */
        public void setExceptionListener(ExceptionListener exceptionListener) throws JMSException {
            this.exceptionListener = exceptionListener;
        }

        /**
         *
         * @return
         * @throws JMSException
         */
        public ExceptionListener getExceptionListener() throws JMSException {
            return this.exceptionListener;
        }

        /**
         *
         * @return
         * @throws JMSException
         */
        public String getClientID() throws JMSException {
            throw new NotImplementedException("Not implemented");
        }

        /**
         *
         * @param string
         * @throws JMSException
         */
        public void setClientID(String string) throws JMSException {
            throw new NotImplementedException("Not implemented");
        }

        /**
         *
         * @return
         * @throws JMSException
         */
        public ConnectionMetaData getMetaData() throws JMSException {
            throw new NotImplementedException("Not implemented");
        }

        /**
         *
         * @param destination
         * @param string
         * @param serverSessionPool
         * @param i
         * @return
         * @throws JMSException
         */
        public ConnectionConsumer createConnectionConsumer(Destination destination, String string,
                ServerSessionPool serverSessionPool, int i) throws JMSException {
            throw new NotImplementedException("Not implemented");
        }

        /**
         *
         * @param topic
         * @param string
         * @param string1
         * @param serverSessionPool
         * @param i
         * @return
         * @throws JMSException
         */
        public ConnectionConsumer createDurableConnectionConsumer(Topic topic, String string, String string1,
                ServerSessionPool serverSessionPool, int i) throws JMSException {
            throw new NotImplementedException("Not implemented");
        }
    }

    /**
     *
     */
    protected static class TestSession implements Session {

        /**
         *
         * @param serializable
         * @return
         * @throws JMSException
         */
        public ObjectMessage createObjectMessage(Serializable serializable) throws JMSException {
            return new TestObjectMessage(serializable);
        }

        /**
         *
         * @return
         * @throws JMSException
         */
        public BytesMessage createBytesMessage() throws JMSException {
            throw new NotImplementedException("Not implemented");
        }

        /**
         *
         * @param destination
         * @return
         * @throws JMSException
         */
        public MessageProducer createProducer(Destination destination) throws JMSException {
            return new TestMessageProducer(destination);
        }

        /**
         *
         * @param destination
         * @return
         * @throws JMSException
         */
        public MessageConsumer createConsumer(Destination destination) throws JMSException {
            return new TestMessageConsumer(destination);
        }

        /**
         *
         * @return
         * @throws JMSException
         */
        public MapMessage createMapMessage() throws JMSException {
            throw new NotImplementedException("Not implemented");
        }

        /**
         *
         * @return
         * @throws JMSException
         */
        public Message createMessage() throws JMSException {
            throw new NotImplementedException("Not implemented");
        }

        /**
         *
         * @return
         * @throws JMSException
         */
        public ObjectMessage createObjectMessage() throws JMSException {
            throw new NotImplementedException("Not implemented");
        }

        /**
         *
         * @return
         * @throws JMSException
         */
        public StreamMessage createStreamMessage() throws JMSException {
            throw new NotImplementedException("Not implemented");
        }

        /**
         *
         * @return
         * @throws JMSException
         */
        public TextMessage createTextMessage() throws JMSException {
            throw new NotImplementedException("Not implemented");
        }

        /**
         *
         * @param string
         * @return
         * @throws JMSException
         */
        public TextMessage createTextMessage(String string) throws JMSException {
            throw new NotImplementedException("Not implemented");
        }

        /**
         *
         * @return
         * @throws JMSException
         */
        public boolean getTransacted() throws JMSException {
            throw new NotImplementedException("Not implemented");
        }

        /**
         *
         * @return
         * @throws JMSException
         */
        public int getAcknowledgeMode() throws JMSException {
            throw new NotImplementedException("Not implemented");
        }

        /**
         *
         * @throws JMSException
         */
        public void commit() throws JMSException {
            throw new NotImplementedException("Not implemented");
        }

        /**
         *
         * @throws JMSException
         */
        public void rollback() throws JMSException {
            throw new NotImplementedException("Not implemented");
        }

        /**
         *
         * @throws JMSException
         */
        public void close() throws JMSException {
            throw new NotImplementedException("Not implemented");
        }

        /**
         *
         * @throws JMSException
         */
        public void recover() throws JMSException {
            throw new NotImplementedException("Not implemented");
        }

        /**
         *
         * @return
         * @throws JMSException
         */
        public MessageListener getMessageListener() throws JMSException {
            throw new NotImplementedException("Not implemented");
        }

        /**
         *
         * @param messageListener
         * @throws JMSException
         */
        public void setMessageListener(MessageListener messageListener) throws JMSException {
            throw new NotImplementedException("Not implemented");
        }

        public void run() {
            throw new NotImplementedException("Not implemented");
        }

        /**
         *
         * @param destination
         * @param string
         * @return
         * @throws JMSException
         */
        public MessageConsumer createConsumer(Destination destination, String string) throws JMSException {
            throw new NotImplementedException("Not implemented");
        }

        /**
         *
         * @param destination
         * @param string
         * @param b
         * @return
         * @throws JMSException
         */
        public MessageConsumer createConsumer(Destination destination, String string, boolean b) throws JMSException {
            throw new NotImplementedException("Not implemented");
        }

        /**
         *
         * @param string
         * @return
         * @throws JMSException
         */
        @Override
        public Queue createQueue(String string) throws JMSException {
            return new TestQueue(string);
        }

        /**
         *
         * @param string
         * @return
         * @throws JMSException
         */
        public Topic createTopic(String string) throws JMSException {
            throw new NotImplementedException("Not implemented");
        }

        /**
         *
         * @param topic
         * @param string
         * @return
         * @throws JMSException
         */
        public TopicSubscriber createDurableSubscriber(Topic topic, String string) throws JMSException {
            throw new NotImplementedException("Not implemented");
        }

        /**
         *
         * @param topic
         * @param string
         * @param string1
         * @param b
         * @return
         * @throws JMSException
         */
        public TopicSubscriber createDurableSubscriber(Topic topic, String string, String string1, boolean b)
                throws JMSException {
            throw new NotImplementedException("Not implemented");
        }

        /**
         *
         * @param queue
         * @return
         * @throws JMSException
         */
        public QueueBrowser createBrowser(Queue queue) throws JMSException {
            throw new NotImplementedException("Not implemented");
        }

        /**
         *
         * @param queue
         * @param string
         * @return
         * @throws JMSException
         */
        public QueueBrowser createBrowser(Queue queue, String string) throws JMSException {
            throw new NotImplementedException("Not implemented");
        }

        /**
         *
         * @return
         * @throws JMSException
         */
        public TemporaryQueue createTemporaryQueue() throws JMSException {
            throw new NotImplementedException("Not implemented");
        }

        /**
         *
         * @return
         * @throws JMSException
         */
        public TemporaryTopic createTemporaryTopic() throws JMSException {
            throw new NotImplementedException("Not implemented");
        }

        /**
         *
         * @param string
         * @throws JMSException
         */
        public void unsubscribe(String string) throws JMSException {
            throw new NotImplementedException("Not implemented");
        }
    }

    /**
     *
     */
    protected static class TestMessageConsumer implements MessageConsumer {
        private MessageListener listener;

        /**
         *
         */
        protected TestDestination destination;

        /**
         *
         * @param destination
         */
        public TestMessageConsumer(Destination destination) {
            this.destination = (TestDestination) destination;
        }

        /**
         *
         * @return
         * @throws JMSException
         */
        public MessageListener getMessageListener() throws JMSException {
            return listener;
        }

        /**
         *
         * @param messageListener
         * @throws JMSException
         */
        public void setMessageListener(MessageListener messageListener) throws JMSException {
            listener = messageListener;
            destination.listeners.add(listener);
        }

        /**
         *
         * @throws JMSException
         */
        public void close() throws JMSException {
            // TODO: Methods should start throwing exceptions
            destination.listeners.remove(listener);
            listener = null;
        }

        /**
         *
         * @return
         * @throws JMSException
         */
        @Override
        public String getMessageSelector() throws JMSException {
            throw new NotImplementedException("Not implemented");
        }

        /**
         *
         * @return
         * @throws JMSException
         */
        @Override
        public Message receive() throws JMSException {
            throw new NotImplementedException("Not implemented");
        }

        /**
         *
         * @param l
         * @return
         * @throws JMSException
         */
        @Override
        public Message receive(long l) throws JMSException {
            throw new NotImplementedException("Not implemented");
        }

        /**
         *
         * @return
         * @throws JMSException
         */
        public Message receiveNoWait() throws JMSException {
            throw new NotImplementedException("Not implemented");
        }
    }

    /**
     *
     */
    public class TestQueueReceiver extends TestMessageConsumer implements QueueReceiver {

        /**
         *
         * @param destination
         */
        public TestQueueReceiver(Destination destination) {
            super(destination);
        }

        /**
         *
         * @return
         * @throws JMSException
         */
        public Queue getQueue() throws JMSException {
            return (Queue) destination;
        }

        /**
         *
         * @return
         * @throws JMSException
         */
        @Override
        public Message receiveNoWait() throws JMSException {
            List<TestObjectMessage> messageQueue = ((TestQueue) getQueue()).messageQueue;
            if (messageQueue.isEmpty())
                return null;
            else
                return ((TestQueue) getQueue()).messageQueue.remove(0);
        }
    }

    /**
     *
     */
    protected static class TestMessageProducer implements MessageProducer {
        TestDestination destination;
        List<TestObjectMessage> messages = new ArrayList<TestObjectMessage>();
        private static AtomicInteger id = new AtomicInteger(0);

        /**
         *
         * @param destination
         */
        public TestMessageProducer(Destination destination) {
            this.destination = (TestDestination) destination;
        }

        /**
         *
         * @return
         * @throws JMSException
         */
        public Destination getDestination() throws JMSException {
            return destination;
        }

        /**
         *
         * @throws JMSException
         */
        public void close() throws JMSException {
            // TODO: Methods should start throwing exceptions
            destination = null;
        }

        /**
         *
         * @param message
         * @throws JMSException
         */
        public void send(Message message) throws JMSException {
            TestObjectMessage testObjectMessage = (TestObjectMessage) message;
            messages.add(testObjectMessage);
            testObjectMessage.id = "msg" + id.incrementAndGet();
            if (destination instanceof Topic) {
                for (MessageListener ml : destination.listeners) {
                    TestObjectMessage clone;
                    try {
                        // TODO: We really should copy the object, but the
                        // tests do not expect it. :-(
                        // clone = Serial.serial(testObjectMessage);
                        clone = testObjectMessage;
                    } catch (Exception e) {
                        throw new JMSException("Serialization failed: " + e);
                    }
                    new CallOnMessageThread(ml, clone).start();
                }
            } else if (destination.listeners.size() > 0) {
                MessageListener[] mls = destination.listeners
                        .toArray(new MessageListener[destination.listeners.size()]);
                MessageListener ml = mls[new Random().nextInt(mls.length)];
                TestObjectMessage clone;
                try {
                    // TODO: We really should copy the object, but the
                    // tests do not expect it. :-(
                    // clone = Serial.serial(testObjectMessage);
                    clone = testObjectMessage;
                } catch (Exception e) {
                    throw new JMSException("Serialization failed: " + e);
                }
                new CallOnMessageThread(ml, clone).start();
            } else if (destination instanceof Queue) {
                ((TestQueue) destination).messageQueue.add(testObjectMessage);
            }
            destination.sent.add(testObjectMessage);
        }

        private void checkForExceptionsToThrow() {
            // TODO Auto-generated method stub

        }

        /**
         *
         * @param b
         * @throws JMSException
         */
        public void setDisableMessageID(boolean b) throws JMSException {
            throw new NotImplementedException("Not implemented");
        }

        /**
         *
         * @return
         * @throws JMSException
         */
        public boolean getDisableMessageID() throws JMSException {
            throw new NotImplementedException("Not implemented");
        }

        /**
         *
         * @param b
         * @throws JMSException
         */
        public void setDisableMessageTimestamp(boolean b) throws JMSException {
            throw new NotImplementedException("Not implemented");
        }

        /**
         *
         * @return
         * @throws JMSException
         */
        public boolean getDisableMessageTimestamp() throws JMSException {
            throw new NotImplementedException("Not implemented");
        }

        /**
         *
         * @param i
         * @throws JMSException
         */
        public void setDeliveryMode(int i) throws JMSException {
            throw new NotImplementedException("Not implemented");
        }

        /**
         *
         * @return
         * @throws JMSException
         */
        public int getDeliveryMode() throws JMSException {
            throw new NotImplementedException("Not implemented");
        }

        /**
         *
         * @param i
         * @throws JMSException
         */
        public void setPriority(int i) throws JMSException {
            throw new NotImplementedException("Not implemented");
        }

        /**
         *
         * @return
         * @throws JMSException
         */
        public int getPriority() throws JMSException {
            throw new NotImplementedException("Not implemented");
        }

        /**
         *
         * @param l
         * @throws JMSException
         */
        public void setTimeToLive(long l) throws JMSException {
            throw new NotImplementedException("Not implemented");
        }

        /**
         *
         * @return
         * @throws JMSException
         */
        public long getTimeToLive() throws JMSException {
            throw new NotImplementedException("Not implemented");
        }

        /**
         *
         * @param message
         * @param i
         * @param i1
         * @param l
         * @throws JMSException
         */
        public void send(Message message, int i, int i1, long l) throws JMSException {
            throw new NotImplementedException("Not implemented");
        }

        /**
         *
         * @param destination
         * @param message
         * @throws JMSException
         */
        public void send(Destination destination, Message message) throws JMSException {
            throw new NotImplementedException("Not implemented");
        }

        /**
         *
         * @param destination
         * @param message
         * @param i
         * @param i1
         * @param l
         * @throws JMSException
         */
        public void send(Destination destination, Message message, int i, int i1, long l) throws JMSException {
            throw new NotImplementedException("Not implemented");
        }
    }

    /**
     *
     */
    protected static class TestDestination implements Destination {

        /**
         *
         */
        protected String name;

        /**
         *
         */
        protected Set<MessageListener> listeners = new HashSet<MessageListener>();

        /**
         *
         */
        protected List<TestObjectMessage> sent = new ArrayList<TestObjectMessage>();
    }

    /**
     *
     */
    protected static class TestQueue extends TestDestination implements Queue {

        /**
         *
         */
        protected List<TestObjectMessage> messageQueue = new ArrayList<TestObjectMessage>();

        /**
         *
         * @param name
         */
        public TestQueue(String name) {
            this.name = name;
        }

        /**
         *
         * @return
         * @throws JMSException
         */
        public String getQueueName() throws JMSException {
            return name;
        }
    }

    /**
     *
     */
    protected static class TestTopic extends TestDestination implements Topic {

        /**
         *
         * @param name
         */
        public TestTopic(String name) {
            this.name = name;
        }

        /**
         *
         * @return
         * @throws JMSException
         */
        public String getTopicName() throws JMSException {
            return name;
        }
    }

    /**
     *
     */
    public static class TestObjectMessage implements ObjectMessage, Serializable {

        /**
         *
         */
        protected Serializable serializable;

        /**
         *
         */
        public String id;

        /**
         *
         * @param serializable
         */
        public TestObjectMessage(Serializable serializable) {
            this.serializable = serializable;
        }

        /**
         *
         * @return
         * @throws JMSException
         */
        public Serializable getObject() throws JMSException {
            return serializable;
        }

        /**
         *
         * @return
         * @throws JMSException
         */
        public String getJMSMessageID() throws JMSException {
            return id;
        }

        // Empty implementation in methods - require to be implemented by the
        // javax.jms.ObjectMessage interface

        /**
         *
         * @param object
         * @throws JMSException
         */
        
        public void setObject(Serializable object) throws JMSException {
            throw new NotImplementedException("Empty implementation - dummy method");
        }

        /**
         *
         * @param s
         * @throws JMSException
         */
        public void setJMSType(String s) throws JMSException {
            throw new NotImplementedException("Empty implementation - dummy method");
        }

        /**
         *
         * @param s
         * @throws JMSException
         */
        public void setJMSMessageID(String s) throws JMSException {
            throw new NotImplementedException("Empty implementation - dummy method");
        }

        /**
         *
         * @return
         * @throws JMSException
         */
        public long getJMSTimestamp() throws JMSException {
            throw new NotImplementedException("Empty implementation - dummy method");
        }

        /**
         *
         * @param l
         * @throws JMSException
         */
        public void setJMSTimestamp(long l) throws JMSException {
            throw new NotImplementedException("Empty implementation - dummy method");
        }

        /**
         *
         * @return
         * @throws JMSException
         */
        public byte[] getJMSCorrelationIDAsBytes() throws JMSException {
            throw new NotImplementedException("Empty implementation - dummy method");
        }

        /**
         *
         * @param bytes
         * @throws JMSException
         */
        public void setJMSCorrelationIDAsBytes(byte[] bytes) throws JMSException {
            throw new NotImplementedException("Empty implementation - dummy method");
        }

        /**
         *
         * @param s
         * @throws JMSException
         */
        public void setJMSCorrelationID(String s) throws JMSException {
            throw new NotImplementedException("Empty implementation - dummy method");
        }

        /**
         *
         * @return
         * @throws JMSException
         */
        public String getJMSCorrelationID() throws JMSException {
            throw new NotImplementedException("Empty implementation - dummy method");
        }

        /**
         *
         * @return
         * @throws JMSException
         */
        public Destination getJMSReplyTo() throws JMSException {
            throw new NotImplementedException("Empty implementation - dummy method");
        }

        /**
         *
         * @param destination
         * @throws JMSException
         */
        public void setJMSReplyTo(Destination destination) throws JMSException {
            throw new NotImplementedException("Empty implementation - dummy method");
        }

        /**
         *
         * @return
         * @throws JMSException
         */
        public Destination getJMSDestination() throws JMSException {
            throw new NotImplementedException("Empty implementation - dummy method");
        }

        /**
         *
         * @param destination
         * @throws JMSException
         */
        public void setJMSDestination(Destination destination) throws JMSException {
            throw new NotImplementedException("Empty implementation - dummy method");
        }

        /**
         *
         * @return
         * @throws JMSException
         */
        public int getJMSDeliveryMode() throws JMSException {
            throw new NotImplementedException("Empty implementation - dummy method");
        }

        /**
         *
         * @param i
         * @throws JMSException
         */
        public void setJMSDeliveryMode(int i) throws JMSException {
            throw new NotImplementedException("Empty implementation - dummy method");
        }

        /**
         *
         * @return
         * @throws JMSException
         */
        public boolean getJMSRedelivered() throws JMSException {
            throw new NotImplementedException("Empty implementation - dummy method");
        }

        /**
         *
         * @param b
         * @throws JMSException
         */
        public void setJMSRedelivered(boolean b) throws JMSException {
            throw new NotImplementedException("Empty implementation - dummy method");
        }

        /**
         *
         * @return
         * @throws JMSException
         */
        public String getJMSType() throws JMSException {
            throw new NotImplementedException("Empty implementation - dummy method");
        }

        /**
         *
         * @return
         * @throws JMSException
         */
        public long getJMSExpiration() throws JMSException {
            throw new NotImplementedException("Empty implementation - dummy method");
        }

        /**
         *
         * @param l
         * @throws JMSException
         */
        public void setJMSExpiration(long l) throws JMSException {
            throw new NotImplementedException("Empty implementation - dummy method");
        }

        /**
         *
         * @return
         * @throws JMSException
         */
        public int getJMSPriority() throws JMSException {
            throw new NotImplementedException("Empty implementation - dummy method");
        }

        /**
         *
         * @param i
         * @throws JMSException
         */
        public void setJMSPriority(int i) throws JMSException {
            throw new NotImplementedException("Empty implementation - dummy method");
        }

        /**
         *
         * @throws JMSException
         */
        public void clearProperties() throws JMSException {
            throw new NotImplementedException("Empty implementation - dummy method");
        }

        /**
         *
         * @param s
         * @return
         * @throws JMSException
         */
        public boolean propertyExists(String s) throws JMSException {
            throw new NotImplementedException("Empty implementation - dummy method");
        }

        /**
         *
         * @param s
         * @return
         * @throws JMSException
         */
        public boolean getBooleanProperty(String s) throws JMSException {
            throw new NotImplementedException("Empty implementation - dummy method");
        }

        /**
         *
         * @param s
         * @return
         * @throws JMSException
         */
        public byte getByteProperty(String s) throws JMSException {
            throw new NotImplementedException("Empty implementation - dummy method");
        }

        /**
         *
         * @param s
         * @return
         * @throws JMSException
         */
        public short getShortProperty(String s) throws JMSException {
            throw new NotImplementedException("Empty implementation - dummy method");
        }

        /**
         *
         * @param s
         * @return
         * @throws JMSException
         */
        public int getIntProperty(String s) throws JMSException {
            throw new NotImplementedException("Empty implementation - dummy method");
        }

        /**
         *
         * @param s
         * @return
         * @throws JMSException
         */
        public long getLongProperty(String s) throws JMSException {
            throw new NotImplementedException("Empty implementation - dummy method");
        }

        /**
         *
         * @param s
         * @return
         * @throws JMSException
         */
        public float getFloatProperty(String s) throws JMSException {
            throw new NotImplementedException("Empty implementation - dummy method");
        }

        /**
         *
         * @param s
         * @return
         * @throws JMSException
         */
        public double getDoubleProperty(String s) throws JMSException {
            throw new NotImplementedException("Empty implementation - dummy method");
        }

        /**
         *
         * @param s
         * @return
         * @throws JMSException
         */
        public String getStringProperty(String s) throws JMSException {
            throw new NotImplementedException("Empty implementation - dummy method");
        }

        /**
         *
         * @param s
         * @return
         * @throws JMSException
         */
        public Object getObjectProperty(String s) throws JMSException {
            throw new NotImplementedException("Empty implementation - dummy method");
        }

        /**
         *
         * @return
         * @throws JMSException
         */
        public Enumeration getPropertyNames() throws JMSException {
            throw new NotImplementedException("Empty implementation - dummy method");
        }

        /**
         *
         * @param s
         * @param b
         * @throws JMSException
         */
        public void setBooleanProperty(String s, boolean b) throws JMSException {
            throw new NotImplementedException("Empty implementation - dummy method");
        }

        /**
         *
         * @param s
         * @param b
         * @throws JMSException
         */
        public void setByteProperty(String s, byte b) throws JMSException {
            throw new NotImplementedException("Empty implementation - dummy method");
        }

        /**
         *
         * @param s
         * @param i
         * @throws JMSException
         */
        public void setShortProperty(String s, short i) throws JMSException {
            throw new NotImplementedException("Empty implementation - dummy method");
        }

        /**
         *
         * @param s
         * @param i
         * @throws JMSException
         */
        public void setIntProperty(String s, int i) throws JMSException {
            throw new NotImplementedException("Empty implementation - dummy method");
        }

        /**
         *
         * @param s
         * @param l
         * @throws JMSException
         */
        public void setLongProperty(String s, long l) throws JMSException {
            throw new NotImplementedException("Empty implementation - dummy method");
        }

        /**
         *
         * @param s
         * @param v
         * @throws JMSException
         */
        public void setFloatProperty(String s, float v) throws JMSException {
            throw new NotImplementedException("Empty implementation - dummy method");
        }

        /**
         *
         * @param s
         * @param v
         * @throws JMSException
         */
        public void setDoubleProperty(String s, double v) throws JMSException {
            throw new NotImplementedException("Empty implementation - dummy method");
        }

        /**
         *
         * @param s
         * @param s1
         * @throws JMSException
         */
        public void setStringProperty(String s, String s1) throws JMSException {
            throw new NotImplementedException("Empty implementation - dummy method");
        }

        /**
         *
         * @param s
         * @param o
         * @throws JMSException
         */
        public void setObjectProperty(String s, Object o) throws JMSException {
            throw new NotImplementedException("Empty implementation - dummy method");
        }

        /**
         *
         * @throws JMSException
         */
        public void acknowledge() throws JMSException {
            throw new NotImplementedException("Empty implementation - dummy method");
        }

        /**
         *
         * @throws JMSException
         */
        public void clearBody() throws JMSException {
            throw new NotImplementedException("Empty implementation - dummy method");
        }

        public String toString() {
            return "TestObjectMessage: " + (serializable == null ? "null" : serializable.toString());
        }

    } // end WrappedMessage

    /**
     *
     */
    protected static class CallOnMessageThread extends Thread {
        private final MessageListener listener;
        private final TestObjectMessage msg;

        /**
         *
         * @param listener
         * @param wMsg
         */
        public CallOnMessageThread(MessageListener listener, TestObjectMessage wMsg) {
            this.listener = listener;
            this.msg = wMsg;
            ((JMSConnectionMockupMQ) JMSConnectionMockupMQ.getInstance()).concurrentTasksToComplete.add(this);
        }

        public void run() {
            synchronized (listener) {
                listener.onMessage(msg);
            }

            Set<Thread> concurrentTasksToComplete = ((JMSConnectionMockupMQ) JMSConnectionMockupMQ.getInstance()).concurrentTasksToComplete;
            synchronized (concurrentTasksToComplete) {
                concurrentTasksToComplete.remove(Thread.currentThread());
                if (concurrentTasksToComplete.isEmpty()) {
                    concurrentTasksToComplete.notifyAll();
                }
            }
        }
    }

    /**
     *
     */
    public class TestQueueBrowser implements QueueBrowser {
        private final TestQueue queue;

        /**
         *
         * @param queue
         */
        public TestQueueBrowser(TestQueue queue) {
            this.queue = queue;
        }

        /**
         *
         * @throws JMSException
         */
        @Override
        public void close() throws JMSException {
        }

        /**
         *
         * @return
         * @throws JMSException
         */
        @Override
        public Enumeration getEnumeration() throws JMSException {
            return Collections.enumeration(queue.messageQueue);
        }

        /**
         *
         * @return
         * @throws JMSException
         */
        @Override
        public String getMessageSelector() throws JMSException {
            return null;
        }

        /**
         *
         * @return
         * @throws JMSException
         */
        @Override
        public Queue getQueue() throws JMSException {
            return queue;
        }
    }

    /**
     *
     */
    public class TestQueueSession extends TestSession implements QueueSession {

        /**
         *
         * @param queue
         * @return
         * @throws JMSException
         */
        @Override
        public QueueBrowser createBrowser(Queue queue) throws JMSException {
            return new TestQueueBrowser((TestQueue) getDestination(queue.getQueueName()));
        }

        /**
         *
         * @param queue
         * @return
         * @throws JMSException
         */
        @Override
        public QueueReceiver createReceiver(Queue queue) throws JMSException {
            return new TestQueueReceiver((TestQueue) getDestination(queue.getQueueName()));
        }

        /**
         *
         * @param arg0
         * @param arg1
         * @return
         * @throws JMSException
         */
        @Override
        public QueueReceiver createReceiver(Queue arg0, String arg1) throws JMSException {
            return null;
        }

        /**
         *
         * @param arg0
         * @return
         * @throws JMSException
         */
        @Override
        public QueueSender createSender(Queue arg0) throws JMSException {
            return null;
        }
    }

    @Override
    public QueueSession getQueueSession() throws JMSException {
        return new TestQueueSession();
    }
}
