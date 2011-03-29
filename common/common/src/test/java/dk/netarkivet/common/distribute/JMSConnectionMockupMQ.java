/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2010 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.archive.bitarchive.distribute.BatchMessage;
import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.exceptions.NotImplementedException;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.utils.batch.FileBatchJob;
import dk.netarkivet.common.utils.batch.TestJob;

/**
 * A MockUp message queue, that generates a connection and destinations suitable
 * for testing.
 */
public class JMSConnectionMockupMQ extends JMSConnection {
    private static final Log log
            = LogFactory.getLog(JMSConnectionMockupMQ.class); 

    /**
     * A set of threads where onMessage has been called. This object is notified
     * when all threads have finished executing.
     */
    protected final Set<Thread> concurrentTasksToComplete
            = Collections.synchronizedSet(new HashSet<Thread>());
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
    public static JMSConnection getInstance() {
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

    protected ConnectionFactory getConnectionFactory()
            throws JMSException {
        return new JMSConnectionMockupMQ.TestConnectionFactory();
    }

    protected Destination getDestination(String channelName)
            throws JMSException {
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
     * Waits until all threads where onMessage has been called have finished
     * executing.
     */
    public void waitForConcurrentTasksToFinish() {
        synchronized (concurrentTasksToComplete) {
            while (!concurrentTasksToComplete.isEmpty()) {
                try {
                    concurrentTasksToComplete.wait();
                } catch (InterruptedException e) {
                    //Go on
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

    public static void clearTestQueues() {
        JMSConnectionMockupMQ.getInstance().initConnection();
    }

    /**
     * For testing purposes: Set the ID of a message
     *
     * @param msg The message to set the id on
     * @param id  the new id
     */
    public static void updateMsgID(NetarkivetMessage msg, String id) {
        msg.updateId(id);
    }

    public static void useJMSConnectionMockupMQ() {
//	JMSConnectionFactory.getInstance().cleanup();
        Settings.set(CommonSettings.JMS_BROKER_CLASS,
                     "dk.netarkivet.common.distribute.JMSConnectionMockupMQ");
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

    public boolean isSentToChannel(TestJob job, ChannelID channelID) {
        TestDestination destination = destinations.get(channelID.getName());
        for (TestObjectMessage sentMessage : destination.sent) {
            NetarkivetMessage message = unpack(sentMessage);
            if (message instanceof BatchMessage) {
                FileBatchJob batchJob = ((BatchMessage) message).getJob();
                if (batchJob instanceof TestJob) {
                    if (((TestJob) batchJob).getTestId().equals(job.getTestId())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    protected static class TestConnectionFactory implements ConnectionFactory {
        public Connection createConnection() throws JMSException {
            return new TestConnection();
        }

        public Connection createConnection(String string,
                                           String string1)
                throws JMSException {
            return new TestConnection();
        }
    }

    protected static class TestConnection implements Connection {
        public boolean isStarted = false;
        private ExceptionListener exceptionListener;

        public Session createSession(boolean b, int i) throws JMSException {
            return new TestSession();
        }

        public void start() throws JMSException {
            if (isStarted) {
                throw new IllegalStateException(this + " already started");
            }
            isStarted = true;
        }

        public void stop() throws JMSException {
            isStarted = false;
        }

        public void close() throws JMSException {
            isStarted = false;
            //TODO: Methods should start throwing exceptions
        }

        public void setExceptionListener(ExceptionListener exceptionListener)
                throws JMSException {
            this.exceptionListener = exceptionListener;
        }

        public ExceptionListener getExceptionListener() throws JMSException {
            return this.exceptionListener;
        }

        public String getClientID() throws JMSException {
            throw new NotImplementedException("Not implemented");
        }

        public void setClientID(String string) throws JMSException {
            throw new NotImplementedException("Not implemented");
        }

        public ConnectionMetaData getMetaData() throws JMSException {
            throw new NotImplementedException("Not implemented");
        }

        public ConnectionConsumer createConnectionConsumer(
                Destination destination,
                String string,
                ServerSessionPool serverSessionPool,
                int i)
                throws JMSException {
            throw new NotImplementedException("Not implemented");
        }

        public ConnectionConsumer createDurableConnectionConsumer(
                Topic topic,
                String string,
                String string1,
                ServerSessionPool serverSessionPool,
                int i)
                throws JMSException {
            throw new NotImplementedException("Not implemented");
        }
    }

    protected static class TestSession implements Session {
        public ObjectMessage createObjectMessage(Serializable serializable)
                throws JMSException {
            return new TestObjectMessage(serializable);
        }

        public BytesMessage createBytesMessage() throws JMSException {
            throw new NotImplementedException("Not implemented");
        }

        public MessageProducer createProducer(Destination destination)
                throws JMSException {
            return new TestMessageProducer(destination);
        }

        public MessageConsumer createConsumer(Destination destination)
                throws JMSException {
            return new TestMessageConsumer(destination);
        }

        public MapMessage createMapMessage() throws JMSException {
            throw new NotImplementedException("Not implemented");
        }

        public Message createMessage() throws JMSException {
            throw new NotImplementedException("Not implemented");
        }

        public ObjectMessage createObjectMessage() throws JMSException {
            throw new NotImplementedException("Not implemented");
        }

        public StreamMessage createStreamMessage() throws JMSException {
            throw new NotImplementedException("Not implemented");
        }

        public TextMessage createTextMessage() throws JMSException {
            throw new NotImplementedException("Not implemented");
        }

        public TextMessage createTextMessage(String string)
                throws JMSException {
            throw new NotImplementedException("Not implemented");
        }

        public boolean getTransacted() throws JMSException {
            throw new NotImplementedException("Not implemented");
        }

        public int getAcknowledgeMode() throws JMSException {
            throw new NotImplementedException("Not implemented");
        }

        public void commit() throws JMSException {
            throw new NotImplementedException("Not implemented");
        }

        public void rollback() throws JMSException {
            throw new NotImplementedException("Not implemented");
        }

        public void close() throws JMSException {
            throw new NotImplementedException("Not implemented");
        }

        public void recover() throws JMSException {
            throw new NotImplementedException("Not implemented");
        }

        public MessageListener getMessageListener() throws JMSException {
            throw new NotImplementedException("Not implemented");
        }

        public void setMessageListener(MessageListener messageListener)
                throws JMSException {
            throw new NotImplementedException("Not implemented");
        }

        public void run() {
            throw new NotImplementedException("Not implemented");
        }

        public MessageConsumer createConsumer(Destination destination,
                                              String string)
                throws JMSException {
            throw new NotImplementedException("Not implemented");
        }

        public MessageConsumer createConsumer(Destination destination,
                                              String string, boolean b)
                throws JMSException {
            throw new NotImplementedException("Not implemented");
        }

        @Override
        public Queue createQueue(String string) throws JMSException {
            return new TestQueue(string);
        }

        public Topic createTopic(String string) throws JMSException {
            throw new NotImplementedException("Not implemented");
        }

        public TopicSubscriber createDurableSubscriber(Topic topic,
                                                       String string)
                throws JMSException {
            throw new NotImplementedException("Not implemented");
        }

        public TopicSubscriber createDurableSubscriber(Topic topic,
                                                       String string,
                                                       String string1,
                                                       boolean b)
                throws JMSException {
            throw new NotImplementedException("Not implemented");
        }

        public QueueBrowser createBrowser(Queue queue) throws JMSException {
            throw new NotImplementedException("Not implemented");
        }

        public QueueBrowser createBrowser(Queue queue, String string)
                throws JMSException {
            throw new NotImplementedException("Not implemented");
        }

        public TemporaryQueue createTemporaryQueue() throws JMSException {
            throw new NotImplementedException("Not implemented");
        }

        public TemporaryTopic createTemporaryTopic() throws JMSException {
            throw new NotImplementedException("Not implemented");
        }

        public void unsubscribe(String string) throws JMSException {
            throw new NotImplementedException("Not implemented");
        }
    }

    protected static class TestMessageConsumer implements MessageConsumer {
        private MessageListener listener;
        protected TestDestination destination;

        public TestMessageConsumer(Destination destination) {
            this.destination = (TestDestination) destination;
        }

        public MessageListener getMessageListener() throws JMSException {
            return listener;
        }

        public void setMessageListener(MessageListener messageListener)
                throws JMSException {
            listener = messageListener;
            destination.listeners.add(listener);
        }

        public void close() throws JMSException {
            //TODO: Methods should start throwing exceptions
            destination.listeners.remove(listener);
            listener = null;
        }

        @Override
        public String getMessageSelector() throws JMSException {
            throw new NotImplementedException("Not implemented");
        }

        @Override
        public Message receive() throws JMSException {
            throw new NotImplementedException("Not implemented");
        }

        @Override
        public Message receive(long l) throws JMSException {
            throw new NotImplementedException("Not implemented");
        }

        public Message receiveNoWait() throws JMSException {
            throw new NotImplementedException("Not implemented");
        }
    }
    
    public class TestQueueReceiver 
    extends TestMessageConsumer 
    implements QueueReceiver {
        public TestQueueReceiver(Destination destination) {
            super(destination);
        }
        public Queue getQueue() throws JMSException {
            return (Queue)destination;
        }        
        @Override
        public Message receiveNoWait() throws JMSException {
            List<TestObjectMessage> messageQueue = ((TestQueue)getQueue()).messageQueue;
            if (messageQueue.isEmpty()) return null;
            else return ((TestQueue)getQueue()).messageQueue.remove(0);
        }
    }

    protected static class TestMessageProducer implements MessageProducer {
        TestDestination destination;
        List<TestObjectMessage> messages = new ArrayList<TestObjectMessage>();
        private static AtomicInteger id = new AtomicInteger(0);

        public TestMessageProducer(Destination destination) {
            this.destination = (TestDestination) destination;
        }

        public Destination getDestination() throws JMSException {
            return destination;
        }

        public void close() throws JMSException {
            //TODO: Methods should start throwing exceptions
            destination = null;
        }

        public void send(Message message) throws JMSException {
            TestObjectMessage testObjectMessage = (TestObjectMessage) message;
            messages.add(testObjectMessage);
            testObjectMessage.id = "msg" + id.incrementAndGet();
            if (destination instanceof Topic) {
                for (MessageListener ml : destination.listeners) {
                    TestObjectMessage clone;
                    try {
                        //TODO: We really should copy the object, but the
                        //tests do not expect it. :-(
                        //clone = Serial.serial(testObjectMessage);
                        clone = testObjectMessage;
                    } catch (Exception e) {
                        throw new JMSException("Serialization failed: " + e);
                    }
                    new CallOnMessageThread(ml, clone).start();
                }
            } else if (destination.listeners.size() > 0) {
                MessageListener[] mls
                        = destination.listeners.toArray(
                        new MessageListener[destination.listeners.size()]);
                MessageListener ml = mls[new Random().nextInt(mls.length)];
                TestObjectMessage clone;
                try {
                    //TODO: We really should copy the object, but the
                    //tests do not expect it. :-(
                    //clone = Serial.serial(testObjectMessage);
                    clone = testObjectMessage;
                } catch (Exception e) {
                    throw new JMSException("Serialization failed: " + e);
                }
                new CallOnMessageThread(ml, clone).start();
            } else if (destination instanceof Queue) {                
                ((TestQueue)destination).messageQueue.add(testObjectMessage);
            }
            destination.sent.add(testObjectMessage);
        }

        private void checkForExceptionsToThrow() {
            // TODO Auto-generated method stub
            
        }

        public void setDisableMessageID(boolean b) throws JMSException {
            throw new NotImplementedException("Not implemented");
        }

        public boolean getDisableMessageID() throws JMSException {
            throw new NotImplementedException("Not implemented");
        }

        public void setDisableMessageTimestamp(boolean b) throws JMSException {
            throw new NotImplementedException("Not implemented");
        }

        public boolean getDisableMessageTimestamp() throws JMSException {
            throw new NotImplementedException("Not implemented");
        }

        public void setDeliveryMode(int i) throws JMSException {
            throw new NotImplementedException("Not implemented");
        }

        public int getDeliveryMode() throws JMSException {
            throw new NotImplementedException("Not implemented");
        }

        public void setPriority(int i) throws JMSException {
            throw new NotImplementedException("Not implemented");
        }

        public int getPriority() throws JMSException {
            throw new NotImplementedException("Not implemented");
        }

        public void setTimeToLive(long l) throws JMSException {
            throw new NotImplementedException("Not implemented");
        }

        public long getTimeToLive() throws JMSException {
            throw new NotImplementedException("Not implemented");
        }

        public void send(Message message, int i, int i1, long l)
                throws JMSException {
            throw new NotImplementedException("Not implemented");
        }

        public void send(Destination destination, Message message)
                throws JMSException {
            throw new NotImplementedException("Not implemented");
        }

        public void send(Destination destination, Message message, int i,
                         int i1,
                         long l) throws JMSException {
            throw new NotImplementedException("Not implemented");
        }
    }

    protected static class TestDestination implements Destination {
        protected String name;
        protected Set<MessageListener> listeners
                = new HashSet<MessageListener>();
        protected List<TestObjectMessage> sent = new ArrayList<TestObjectMessage>();
    }

    protected static class TestQueue extends TestDestination implements Queue {
        protected List<TestObjectMessage> messageQueue = 
            new ArrayList<TestObjectMessage>();
        public TestQueue(String name) {
            this.name = name;
        }

        public String getQueueName() throws JMSException {
            return name;
        }
    }

    protected static class TestTopic extends TestDestination implements Topic {
        public TestTopic(String name) {
            this.name = name;
        }

        public String getTopicName() throws JMSException {
            return name;
        }
    }

    public static class TestObjectMessage implements ObjectMessage, Serializable {
        protected Serializable serializable;
        public String id;

        public TestObjectMessage(Serializable serializable) {
            this.serializable = serializable;
        }

        public Serializable getObject() throws JMSException {
            return serializable;
        }

        public String getJMSMessageID() throws JMSException {
            return id;
        }

        //Empty implementation in methods - require to be implemented by the
        //javax.jms.ObjectMessage interface

        public void setObject(Serializable object) throws JMSException {
            throw new NotImplementedException(
                    "Empty implementation - dummy method");
        }

        public void setJMSType(String s) throws JMSException {
            throw new NotImplementedException(
                    "Empty implementation - dummy method");
        }

        public void setJMSMessageID(String s) throws JMSException {
            throw new NotImplementedException(
                    "Empty implementation - dummy method");
        }

        public long getJMSTimestamp() throws JMSException {
            throw new NotImplementedException(
                    "Empty implementation - dummy method");
        }

        public void setJMSTimestamp(long l) throws JMSException {
            throw new NotImplementedException(
                    "Empty implementation - dummy method");
        }

        public byte[] getJMSCorrelationIDAsBytes() throws JMSException {
            throw new NotImplementedException(
                    "Empty implementation - dummy method");
        }

        public void setJMSCorrelationIDAsBytes(byte[] bytes)
                throws JMSException {
            throw new NotImplementedException(
                    "Empty implementation - dummy method");
        }

        public void setJMSCorrelationID(String s) throws JMSException {
            throw new NotImplementedException(
                    "Empty implementation - dummy method");
        }

        public String getJMSCorrelationID() throws JMSException {
            throw new NotImplementedException(
                    "Empty implementation - dummy method");
        }

        public Destination getJMSReplyTo() throws JMSException {
            throw new NotImplementedException(
                    "Empty implementation - dummy method");
        }

        public void setJMSReplyTo(Destination destination) throws JMSException {
            throw new NotImplementedException(
                    "Empty implementation - dummy method");
        }

        public Destination getJMSDestination() throws JMSException {
            throw new NotImplementedException(
                    "Empty implementation - dummy method");
        }

        public void setJMSDestination(Destination destination)
                throws JMSException {
            throw new NotImplementedException(
                    "Empty implementation - dummy method");
        }

        public int getJMSDeliveryMode() throws JMSException {
            throw new NotImplementedException(
                    "Empty implementation - dummy method");
        }

        public void setJMSDeliveryMode(int i) throws JMSException {
            throw new NotImplementedException(
                    "Empty implementation - dummy method");
        }

        public boolean getJMSRedelivered() throws JMSException {
            throw new NotImplementedException(
                    "Empty implementation - dummy method");
        }

        public void setJMSRedelivered(boolean b) throws JMSException {
            throw new NotImplementedException(
                    "Empty implementation - dummy method");
        }

        public String getJMSType() throws JMSException {
            throw new NotImplementedException(
                    "Empty implementation - dummy method");
        }

        public long getJMSExpiration() throws JMSException {
            throw new NotImplementedException(
                    "Empty implementation - dummy method");
        }

        public void setJMSExpiration(long l) throws JMSException {
            throw new NotImplementedException(
                    "Empty implementation - dummy method");
        }

        public int getJMSPriority() throws JMSException {
            throw new NotImplementedException(
                    "Empty implementation - dummy method");
        }

        public void setJMSPriority(int i) throws JMSException {
            throw new NotImplementedException(
                    "Empty implementation - dummy method");
        }

        public void clearProperties() throws JMSException {
            throw new NotImplementedException(
                    "Empty implementation - dummy method");
        }

        public boolean propertyExists(String s) throws JMSException {
            throw new NotImplementedException(
                    "Empty implementation - dummy method");
        }

        public boolean getBooleanProperty(String s) throws JMSException {
            throw new NotImplementedException(
                    "Empty implementation - dummy method");
        }

        public byte getByteProperty(String s) throws JMSException {
            throw new NotImplementedException(
                    "Empty implementation - dummy method");
        }

        public short getShortProperty(String s) throws JMSException {
            throw new NotImplementedException(
                    "Empty implementation - dummy method");
        }

        public int getIntProperty(String s) throws JMSException {
            throw new NotImplementedException(
                    "Empty implementation - dummy method");
        }

        public long getLongProperty(String s) throws JMSException {
            throw new NotImplementedException(
                    "Empty implementation - dummy method");
        }

        public float getFloatProperty(String s) throws JMSException {
            throw new NotImplementedException(
                    "Empty implementation - dummy method");
        }

        public double getDoubleProperty(String s) throws JMSException {
            throw new NotImplementedException(
                    "Empty implementation - dummy method");
        }

        public String getStringProperty(String s) throws JMSException {
            throw new NotImplementedException(
                    "Empty implementation - dummy method");
        }

        public Object getObjectProperty(String s) throws JMSException {
            throw new NotImplementedException(
                    "Empty implementation - dummy method");
        }

        public Enumeration getPropertyNames() throws JMSException {
            throw new NotImplementedException(
                    "Empty implementation - dummy method");
        }

        public void setBooleanProperty(String s, boolean b)
                throws JMSException {
            throw new NotImplementedException(
                    "Empty implementation - dummy method");
        }

        public void setByteProperty(String s, byte b) throws JMSException {
            throw new NotImplementedException(
                    "Empty implementation - dummy method");
        }

        public void setShortProperty(String s, short i) throws JMSException {
            throw new NotImplementedException(
                    "Empty implementation - dummy method");
        }

        public void setIntProperty(String s, int i) throws JMSException {
            throw new NotImplementedException(
                    "Empty implementation - dummy method");
        }

        public void setLongProperty(String s, long l) throws JMSException {
            throw new NotImplementedException(
                    "Empty implementation - dummy method");
        }

        public void setFloatProperty(String s, float v) throws JMSException {
            throw new NotImplementedException(
                    "Empty implementation - dummy method");
        }

        public void setDoubleProperty(String s, double v) throws JMSException {
            throw new NotImplementedException(
                    "Empty implementation - dummy method");
        }

        public void setStringProperty(String s, String s1) throws JMSException {
            throw new NotImplementedException(
                    "Empty implementation - dummy method");
        }

        public void setObjectProperty(String s, Object o) throws JMSException {
            throw new NotImplementedException(
                    "Empty implementation - dummy method");
        }

        public void acknowledge() throws JMSException {
            throw new NotImplementedException(
                    "Empty implementation - dummy method");
        }

        public void clearBody() throws JMSException {
            throw new NotImplementedException(
                    "Empty implementation - dummy method");
        }

        public String toString() {
            return "TestObjectMessage: "
                   + (serializable == null ? "null" : serializable.toString());
        }

    } // end WrappedMessage

    protected static class CallOnMessageThread extends Thread {
        private final MessageListener listener;
        private final TestObjectMessage msg;

        public CallOnMessageThread(MessageListener listener,
                                   TestObjectMessage wMsg) {
            this.listener = listener;
            this.msg = wMsg;
            ((JMSConnectionMockupMQ) JMSConnectionMockupMQ.getInstance())
                    .concurrentTasksToComplete.add(
                    this);
        }

        public void run() {
            synchronized (listener) {
                listener.onMessage(msg);
            }

            Set<Thread> concurrentTasksToComplete
                    = ((JMSConnectionMockupMQ)
                    JMSConnectionMockupMQ.getInstance())
                    .concurrentTasksToComplete;
            synchronized (concurrentTasksToComplete) {
                concurrentTasksToComplete.remove(Thread.currentThread());
                if (concurrentTasksToComplete.isEmpty()) {
                    concurrentTasksToComplete.notifyAll();
                }
            }
        }
    }
    
    public class TestQueueBrowser implements QueueBrowser {
        private final TestQueue queue;
        
        public TestQueueBrowser(TestQueue queue) {
            this.queue = queue;
        }

        @Override
        public void close() throws JMSException {
        }

        @Override
        public Enumeration getEnumeration() throws JMSException {
            return Collections.enumeration(queue.messageQueue);
        }

        @Override
        public String getMessageSelector() throws JMSException {
            return null;
        }
        @Override
        public Queue getQueue() throws JMSException {
            return queue;
        }
    }
    
    public class TestQueueSession extends TestSession implements QueueSession {
        @Override
        public QueueBrowser createBrowser(Queue queue) throws JMSException {
            return new TestQueueBrowser((TestQueue)getDestination(queue.getQueueName()));
        }
        @Override
        public QueueReceiver createReceiver(Queue queue) throws JMSException {
            return new TestQueueReceiver((TestQueue)getDestination(queue.getQueueName()));
        }
        @Override
        public QueueReceiver createReceiver(Queue arg0, String arg1)
                throws JMSException {
            return null;
        }
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
