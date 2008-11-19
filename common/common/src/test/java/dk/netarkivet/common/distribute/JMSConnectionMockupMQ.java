/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
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

import javax.jms.BytesMessage;
import javax.jms.Connection;
import javax.jms.ConnectionConsumer;
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
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
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
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;
import javax.jms.TopicPublisher;
import javax.jms.TopicSession;
import javax.jms.TopicSubscriber;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.IllegalState;
import dk.netarkivet.common.exceptions.NotImplementedException;
import dk.netarkivet.common.exceptions.UnknownID;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.utils.batch.TestJob;

/**
 *
 */
public class JMSConnectionMockupMQ extends JMSConnection {
    /** Singleton pattern is be used for this class. This is the one instance. */
    private static JMSConnectionMockupMQ instance = null;

    Map<String, TestJob> sentTestJobs;
    List<ChannelIDListenerPair> channelIDListenerPairs;

    // notifyAll is called on this object whenever all send or reply calls have been completed
    Set<Thread> concurrentTasksToComplete;
    static int idcount = 0;

    private JMSConnectionMockupMQ() {
        super();
        log.info("Creating instance of TestMQ");
    }

    /**
     * Establishes a connection to a specifik JMS-Broker defined in the settings
     * file.
     *
     * @return A JMSConnection
     * @throws UnknownID
     * @throws IOFailure
     *             when connection to JMS broker failed
     */
    public static JMSConnectionMockupMQ getInstance() throws UnknownID, IOFailure {
        if (instance == null) {
            instance = new JMSConnectionMockupMQ();
            instance.initConnection();
        }
        return instance;
    }

    protected void initConnection() {
        super.initConnection();
        channelIDListenerPairs = new ArrayList<ChannelIDListenerPair>();
        sentTestJobs = new HashMap<String, TestJob>();
        concurrentTasksToComplete = new HashSet<Thread>();
    }

    public static void clearTestQueues() {
        getInstance().initConnection();
    }

    public boolean waitForConcurrentTasksToFinish() {
        try {
            synchronized(concurrentTasksToComplete){
                while (!concurrentTasksToComplete.isEmpty()){
                    concurrentTasksToComplete.wait();
                }
            }
        }
        catch (InterruptedException e) {
            log.fatal("Interrupted whilst waiting for all send(..) and reply(..) to finish.");
            return false;
        }
        return true;
    }

    /** Check that the name of the channel is legal for JMS queues. */
    void checkChannelID(ChannelID mq) {
        // TODO: Find a more accurate definition of what are legal queue names
        if (mq.getName().indexOf(" ") > -1 || mq.getName().indexOf(".") > -1) {
            throw new IOFailure("Illegal queue name '" + mq.getName() + "'");
        }
    }

    public boolean isSentToChannel(TestJob job, ChannelID channelID) {

        checkChannelID(channelID);
        TestJob foundJob = (TestJob)sentTestJobs.get(job.getTestId());
        return (foundJob != null  &&  foundJob.getSentToChannel().equals(channelID));
    }

    protected void close() {
        channelIDListenerPairs.clear();
        sentTestJobs = null;
        instance = null;
    }

    public void cleanup() {
        instance = null;
        channelIDListenerPairs.clear();
        sentTestJobs = null;
    }

    public String getHost() {
        return "localhost";
    }

    public int getPort() {
        return 0;
    }

    class ChannelIDListenerPair {

        ChannelID channelID;
        MessageListener messageListener;

        ChannelIDListenerPair(ChannelID mq, MessageListener ml) {
            this.channelID = mq;
            this.messageListener = ml;
        }

        public String toString() {
            return "[" + channelID + " -> " + messageListener + "]";
        }

        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof JMSConnectionMockupMQ.ChannelIDListenerPair)) return false;

            final JMSConnectionMockupMQ.ChannelIDListenerPair channelIDListenerPair = (JMSConnectionMockupMQ.ChannelIDListenerPair) o;

            if (!channelID.equals(channelIDListenerPair.channelID)) return false;
            if (!messageListener.equals(channelIDListenerPair.messageListener)) return false;

            return true;
        }

        public int hashCode() {
            int result;
            result = channelID.hashCode();
            result = 29 * result + messageListener.hashCode();
            return result;
        }
    }

    class CallOnMessageThread extends Thread {
        protected final Logger log = Logger.getLogger(getClass().getName());
        MessageListener listener;
        JMSConnectionMockupMQ.WrappedMessage wMsg;

        public CallOnMessageThread(MessageListener in_listener, JMSConnectionMockupMQ.WrappedMessage in_wMsg){
            listener = in_listener;
            wMsg = in_wMsg;
        }
        public void run() {
            if (instance != null){
                synchronized(listener) {
                    listener.onMessage(wMsg);
                }
                log.finer("Called onMessage for "+ listener.toString());
            } else {
                log.warning("Found connection already closed when trying to send message to " + listener.toString());
            }

            synchronized(concurrentTasksToComplete) {
                concurrentTasksToComplete.remove(Thread.currentThread());
                if (concurrentTasksToComplete.isEmpty())
                    concurrentTasksToComplete.notifyAll();
            }
        }
    }

    static class WrappedMessage implements ObjectMessage {

        NetarkivetMessage netarkivetMessage;

        public WrappedMessage(NetarkivetMessage netarkivetMessage) {
            this.netarkivetMessage = netarkivetMessage;
        }

        public Serializable getObject() throws JMSException {
            return netarkivetMessage;
        }

        /**
         * Empty implementation in methods - require to be implemented by the javax.jms.ObjectMessage intyerface
         */

        public void setObject(Serializable object) throws JMSException {
            throw new NotImplementedException("Empty implementation - dummy method");
        }


        public void setJMSType(String s) throws JMSException {
            throw new NotImplementedException("Empty implementation - dummy method");
        }

        public String getJMSMessageID() throws JMSException {
            netarkivetMessage.updateId("ID" + JMSConnectionMockupMQ.idcount);
            JMSConnectionMockupMQ.idcount++;
            return netarkivetMessage.getID();
        }

        public void setJMSMessageID(String s) throws JMSException {
            throw new NotImplementedException("Empty implementation - dummy method");
        }

        public long getJMSTimestamp() throws JMSException {
            throw new NotImplementedException("Empty implementation - dummy method");
        }

        public void setJMSTimestamp(long l) throws JMSException {
            throw new NotImplementedException("Empty implementation - dummy method");
        }

        public byte[] getJMSCorrelationIDAsBytes() throws JMSException {
            throw new NotImplementedException("Empty implementation - dummy method");
        }

        public void setJMSCorrelationIDAsBytes(byte[] bytes) throws JMSException {
            throw new NotImplementedException("Empty implementation - dummy method");
        }

        public void setJMSCorrelationID(String s) throws JMSException {
            throw new NotImplementedException("Empty implementation - dummy method");
        }

        public String getJMSCorrelationID() throws JMSException {
            throw new NotImplementedException("Empty implementation - dummy method");
        }

        public Destination getJMSReplyTo() throws JMSException {
           throw new NotImplementedException("Empty implementation - dummy method");
        }

        public void setJMSReplyTo(Destination destination) throws JMSException {
            throw new NotImplementedException("Empty implementation - dummy method");
        }

        public Destination getJMSDestination() throws JMSException {
            throw new NotImplementedException("Empty implementation - dummy method");
        }

        public void setJMSDestination(Destination destination) throws JMSException {
            throw new NotImplementedException("Empty implementation - dummy method");
        }

        public int getJMSDeliveryMode() throws JMSException {
           throw new NotImplementedException("Empty implementation - dummy method");
        }

        public void setJMSDeliveryMode(int i) throws JMSException {
            throw new NotImplementedException("Empty implementation - dummy method");
        }

        public boolean getJMSRedelivered() throws JMSException {
            throw new NotImplementedException("Empty implementation - dummy method");
        }

        public void setJMSRedelivered(boolean b) throws JMSException {
            throw new NotImplementedException("Empty implementation - dummy method");
        }

        public String getJMSType() throws JMSException {
            throw new NotImplementedException("Empty implementation - dummy method");
        }

        public long getJMSExpiration() throws JMSException {
            throw new NotImplementedException("Empty implementation - dummy method");
        }

        public void setJMSExpiration(long l) throws JMSException {
            throw new NotImplementedException("Empty implementation - dummy method");
        }

        public int getJMSPriority() throws JMSException {
            throw new NotImplementedException("Empty implementation - dummy method");
        }

        public void setJMSPriority(int i) throws JMSException {
            throw new NotImplementedException("Empty implementation - dummy method");
        }

        public void clearProperties() throws JMSException {
            throw new NotImplementedException("Empty implementation - dummy method");
        }

        public boolean propertyExists(String s) throws JMSException {
            throw new NotImplementedException("Empty implementation - dummy method");
        }

        public boolean getBooleanProperty(String s) throws JMSException {
            throw new NotImplementedException("Empty implementation - dummy method");
        }

        public byte getByteProperty(String s) throws JMSException {
            throw new NotImplementedException("Empty implementation - dummy method");
        }

        public short getShortProperty(String s) throws JMSException {
            throw new NotImplementedException("Empty implementation - dummy method");
        }

        public int getIntProperty(String s) throws JMSException {
            throw new NotImplementedException("Empty implementation - dummy method");
        }

        public long getLongProperty(String s) throws JMSException {
           throw new NotImplementedException("Empty implementation - dummy method");
        }

        public float getFloatProperty(String s) throws JMSException {
            throw new NotImplementedException("Empty implementation - dummy method");
        }

        public double getDoubleProperty(String s) throws JMSException {
            throw new NotImplementedException("Empty implementation - dummy method");
        }

        public String getStringProperty(String s) throws JMSException {
            throw new NotImplementedException("Empty implementation - dummy method");
        }

        public Object getObjectProperty(String s) throws JMSException {
            throw new NotImplementedException("Empty implementation - dummy method");
        }

        public Enumeration getPropertyNames() throws JMSException {
            throw new NotImplementedException("Empty implementation - dummy method");
        }

        public void setBooleanProperty(String s, boolean b) throws JMSException {
            throw new NotImplementedException("Empty implementation - dummy method");
        }

        public void setByteProperty(String s, byte b) throws JMSException {
            throw new NotImplementedException("Empty implementation - dummy method");
        }

        public void setShortProperty(String s, short i) throws JMSException {
            throw new NotImplementedException("Empty implementation - dummy method");
        }

        public void setIntProperty(String s, int i) throws JMSException {
            throw new NotImplementedException("Empty implementation - dummy method");
        }

        public void setLongProperty(String s, long l) throws JMSException {
            throw new NotImplementedException("Empty implementation - dummy method");
        }

        public void setFloatProperty(String s, float v) throws JMSException {
            throw new NotImplementedException("Empty implementation - dummy method");
        }

        public void setDoubleProperty(String s, double v) throws JMSException {
            throw new NotImplementedException("Empty implementation - dummy method");
        }

        public void setStringProperty(String s, String s1) throws JMSException {
            throw new NotImplementedException("Empty implementation - dummy method");
        }

        public void setObjectProperty(String s, Object o) throws JMSException {
            throw new NotImplementedException("Empty implementation - dummy method");
        }

        public void acknowledge() throws JMSException {
            throw new NotImplementedException("Empty implementation - dummy method");
        }

        public void clearBody() throws JMSException {
            throw new NotImplementedException("Empty implementation - dummy method");
        }

        public String toString() {
            return "WrappedMessage: " + netarkivetMessage.toString();
        }

    } // end WrappedMessage

    /**
     * Wrap a NetarkivetMessage into an ObjectMessage
     *
     * @param nMsg a NetarkivetMessage
     * @return an ObjectMessage
     * @throws IOFailure if the conversion failed
     */
    public ObjectMessage getObjectMessage(NetarkivetMessage nMsg)
            throws IOFailure {
        return new JMSConnectionMockupMQ.WrappedMessage(nMsg);
    }


    /** For testing purposes: Set the ID of a message
     * @param msg The message to set the id on
     * @param id the new id
     */
    public static void updateMsgID(NetarkivetMessage msg, String id) {
        msg.updateId(id);
    }

    public static void useJMSConnectionTestMQ() {
        Settings.set(CommonSettings.JMS_BROKER_CLASS,
                     "dk.netarkivet.common.distribute.JMSConnectionTestMQ");
        getInstance().close();
    }

    protected class TestQueue implements Queue {
        String name;
        public List<Message> sent = new ArrayList<Message>();

        public TestQueue(String name) {
            this.name = name;
        }
        public String getQueueName() throws JMSException {
            return name;
        }
    }

    protected class TestTopic implements Topic {
        String name;
        public TestTopic(String name) {
            this.name = name;
        }
        public String getTopicName() throws JMSException {
            return name;
        }
    }

    protected QueueConnectionFactory getQueueConnectionFactory()
            throws JMSException {
        return new JMSConnectionMockupMQ.TestQueueConnectionFactory();
    }
    protected TopicConnectionFactory getTopicConnectionFactory()
            throws JMSException {
        return new JMSConnectionMockupMQ.TestTopicConnectionFactory();
    }

    protected Queue getQueue(String queueName) throws JMSException {
        return new TestQueue(queueName);
    }
    protected Topic getTopic(String topicName) throws JMSException {
        return new TestTopic(topicName);
    }

    protected class TestSession implements Session {
        int ids;

        public BytesMessage createBytesMessage() throws JMSException {
            //TODO: implement method
            throw new NotImplementedException("Not implemented");
        }

        public MapMessage createMapMessage() throws JMSException {
            //TODO: implement method
            throw new NotImplementedException("Not implemented");
        }

        public Message createMessage() throws JMSException {
            //TODO: implement method
            throw new NotImplementedException("Not implemented");
        }

        public ObjectMessage createObjectMessage() throws JMSException {
            //TODO: implement method
            throw new NotImplementedException("Not implemented");
        }

        public ObjectMessage createObjectMessage(Serializable serializable)
                throws JMSException {
            ((NetarkivetMessage)serializable).updateId("Q" + (ids++));
            return new TestObjectMessage(serializable);
        }

        public StreamMessage createStreamMessage() throws JMSException {
            //TODO: implement method
            throw new NotImplementedException("Not implemented");
        }

        public TextMessage createTextMessage() throws JMSException {
            //TODO: implement method
            throw new NotImplementedException("Not implemented");
        }

        public TextMessage createTextMessage(String string) throws JMSException {
            //TODO: implement method
            throw new NotImplementedException("Not implemented");
        }

        public boolean getTransacted() throws JMSException {
            //TODO: implement method
            throw new NotImplementedException("Not implemented");
        }

        public int getAcknowledgeMode() throws JMSException {
            //TODO: implement method
            throw new NotImplementedException("Not implemented");
        }

        public void commit() throws JMSException {
            //TODO: implement method
            throw new NotImplementedException("Not implemented");
        }

        public void rollback() throws JMSException {
            //TODO: implement method
            throw new NotImplementedException("Not implemented");
        }

        public void close() throws JMSException {
            //TODO: implement method
            throw new NotImplementedException("Not implemented");
        }

        public void recover() throws JMSException {
            //TODO: implement method
            throw new NotImplementedException("Not implemented");
        }

        public MessageListener getMessageListener() throws JMSException {
            //TODO: implement method
            throw new NotImplementedException("Not implemented");
        }

        public void setMessageListener(MessageListener messageListener)
                throws JMSException {
            //TODO: implement method
            throw new NotImplementedException("Not implemented");
        }

        public void run() {
            //TODO: implement method
            throw new NotImplementedException("Not implemented");
        }

        public MessageProducer createProducer(Destination destination)
                throws JMSException {
            //TODO: implement method
            throw new NotImplementedException("Not implemented");
        }

        public MessageConsumer createConsumer(Destination destination)
                throws JMSException {
            //TODO: implement method
            throw new NotImplementedException("Not implemented");
        }

        public MessageConsumer createConsumer(Destination destination,
                                              String string) throws JMSException {
            //TODO: implement method
            throw new NotImplementedException("Not implemented");
        }

        public MessageConsumer createConsumer(Destination destination,
                                              String string, boolean b)
                throws JMSException {
            //TODO: implement method
            throw new NotImplementedException("Not implemented");
        }

        public Queue createQueue(String string) throws JMSException {
            //TODO: implement method
            throw new NotImplementedException("Not implemented");
        }

        public Topic createTopic(String string) throws JMSException {
            //TODO: implement method
            throw new NotImplementedException("Not implemented");
        }

        public TopicSubscriber createDurableSubscriber(Topic topic, String string)
                throws JMSException {
            //TODO: implement method
            throw new NotImplementedException("Not implemented");
        }

        public TopicSubscriber createDurableSubscriber(Topic topic, String string,
                                                       String string1, boolean b)
                throws JMSException {
            //TODO: implement method
            throw new NotImplementedException("Not implemented");
        }

        public QueueBrowser createBrowser(Queue queue) throws JMSException {
            //TODO: implement method
            throw new NotImplementedException("Not implemented");
        }

        public QueueBrowser createBrowser(Queue queue, String string)
                throws JMSException {
            //TODO: implement method
            throw new NotImplementedException("Not implemented");
        }

        public TemporaryQueue createTemporaryQueue() throws JMSException {
            //TODO: implement method
            throw new NotImplementedException("Not implemented");
        }

        public TemporaryTopic createTemporaryTopic() throws JMSException {
            //TODO: implement method
            throw new NotImplementedException("Not implemented");
        }

        public void unsubscribe(String string) throws JMSException {
            //TODO: implement method
            throw new NotImplementedException("Not implemented");
        }
    }

    protected class TestQueueSession extends JMSConnectionMockupMQ.TestSession implements QueueSession {

        public QueueReceiver createReceiver(Queue queue) throws JMSException {
            return new TestQueueReceiver(queue);
        }

        public QueueReceiver createReceiver(Queue queue, String string)
                throws JMSException {
            //TODO: implement method
            throw new NotImplementedException("Not implemented");
        }

        public QueueSender createSender(Queue queue) throws JMSException {
            return new TestQueueSender(queue);
        }
    }

    protected class TestTopicSession extends JMSConnectionMockupMQ.TestSession implements TopicSession {

        public TopicSubscriber createSubscriber(Topic topic) throws JMSException {
            return new TestTopicSubscriber(topic);
        }

        public TopicSubscriber createSubscriber(Topic topic, String string,
                                                boolean b) throws JMSException {
            //TODO: implement method
            throw new NotImplementedException("Not implemented");
        }

        public TopicPublisher createPublisher(Topic topic) throws JMSException {
            return new TestTopicPublisher(topic);
        }
    }

    public class TestMessageConsumer implements MessageConsumer {
        MessageListener listener;
        public String getMessageSelector() throws JMSException {
            //TODO: implement method
            throw new NotImplementedException("Not implemented");
        }

        public MessageListener getMessageListener() throws JMSException {
            return listener;
        }

        public void setMessageListener(MessageListener messageListener)
                throws JMSException {
            listener = messageListener;
        }

        public Message receive() throws JMSException {
            //TODO: implement method
            throw new NotImplementedException("Not implemented");
        }

        public Message receive(long l) throws JMSException {
            //TODO: implement method
            throw new NotImplementedException("Not implemented");
        }

        public Message receiveNoWait() throws JMSException {
            //TODO: implement method
            throw new NotImplementedException("Not implemented");
        }

        public void close() throws JMSException {
            listener = null;
        }
    }

    protected class TestQueueReceiver extends TestMessageConsumer
            implements QueueReceiver {
        Queue queue;
        public TestQueueReceiver(Queue queue) {
            ArgumentNotValid.checkNotNull(queue, "queue");
            this.queue = queue;
        }

        public Queue getQueue() throws JMSException {
            return queue;
        }
        public Message receiveNoWait() throws JMSException {
            if (((TestQueue)queue).sent.size() == 0) {
                throw new IllegalState("Bad state: Nothing has been sent to this queue");
            }
            Message m = ((TestQueue)queue).sent.remove(0);
            return m;
        }
    }

    protected class TestTopicSubscriber extends TestMessageConsumer
            implements TopicSubscriber {
        Topic topic;
        public TestTopicSubscriber(Topic topic) {
            ArgumentNotValid.checkNotNull(topic, "topic");
            this.topic = topic;
        }

        public Topic getTopic() throws JMSException {
            return topic;
        }

        public boolean getNoLocal() throws JMSException {
            //TODO: implement method
            throw new NotImplementedException("Not implemented");
        }
    }

    public class TestMessageProducer implements MessageProducer {
        public List<Message> messagesSent = new ArrayList<Message>();
        public void setDisableMessageID(boolean b) throws JMSException {
            //TODO: implement method
            throw new NotImplementedException("Not implemented");
        }

        public boolean getDisableMessageID() throws JMSException {
            //TODO: implement method
            throw new NotImplementedException("Not implemented");
        }

        public void setDisableMessageTimestamp(boolean b) throws JMSException {
            //TODO: implement method
            throw new NotImplementedException("Not implemented");
        }

        public boolean getDisableMessageTimestamp() throws JMSException {
            //TODO: implement method
            throw new NotImplementedException("Not implemented");
        }

        public void setDeliveryMode(int i) throws JMSException {
            //TODO: implement method
            throw new NotImplementedException("Not implemented");
        }

        public int getDeliveryMode() throws JMSException {
            //TODO: implement method
            throw new NotImplementedException("Not implemented");
        }

        public void setPriority(int i) throws JMSException {
            //TODO: implement method
            throw new NotImplementedException("Not implemented");
        }

        public int getPriority() throws JMSException {
            //TODO: implement method
            throw new NotImplementedException("Not implemented");
        }

        public void setTimeToLive(long l) throws JMSException {
            //TODO: implement method
            throw new NotImplementedException("Not implemented");
        }

        public long getTimeToLive() throws JMSException {
            //TODO: implement method
            throw new NotImplementedException("Not implemented");
        }

        public Destination getDestination() throws JMSException {
            //TODO: implement method
            throw new NotImplementedException("Not implemented");
        }

        public void close() throws JMSException {
            //TODO: implement method
            throw new NotImplementedException("Not implemented");
        }

        public void send(Message message) throws JMSException {
            messagesSent.add(message);
        }

        public void send(Message message, int i, int i1, long l)
                throws JMSException {
            //TODO: implement method
            throw new NotImplementedException("Not implemented");
        }

        public void send(Destination destination, Message message)
                throws JMSException {
            //TODO: implement method
            throw new NotImplementedException("Not implemented");
        }

        public void send(Destination destination, Message message, int i, int i1,
                         long l) throws JMSException {
            //TODO: implement method
            throw new NotImplementedException("Not implemented");
        }
    }

    protected class TestQueueSender
            extends JMSConnectionMockupMQ.TestMessageProducer
            implements QueueSender {
        Queue queue;
        public TestQueueSender(Queue queue) {
            ArgumentNotValid.checkNotNull(queue, "queue");
            this.queue = queue;
        }

        public Queue getQueue() throws JMSException {
            return queue;
        }

        public void send(Message message) throws JMSException {
            super.send(message);
            ((TestQueue)queue).sent.add(message);
        }

        public void send(Queue queue, Message message) throws JMSException {
            assert queue == this.queue;
            super.send(message);
        }

        public void send(Queue queue, Message message, int i, int i1, long l)
                throws JMSException {
            //TODO: implement method
            throw new NotImplementedException("Not implemented");
        }
    }

    protected class TestTopicPublisher
            extends JMSConnectionMockupMQ.TestMessageProducer
            implements TopicPublisher {
        Topic topic;
        public TestTopicPublisher(Topic topic) {
            ArgumentNotValid.checkNotNull(topic, "topic");
            this.topic = topic;
        }

        public Topic getTopic() throws JMSException {
            return topic;
        }

        public void publish(Message message) throws JMSException {
            super.send(message);
        }

        public void publish(Message message, int i, int i1, long l)
                throws JMSException {
            //TODO: implement method
            throw new NotImplementedException("Not implemented");
        }

        public void publish(Topic topic, Message message) throws JMSException {
            //TODO: implement method
            throw new NotImplementedException("Not implemented");
        }

        public void publish(Topic topic, Message message, int i, int i1, long l)
                throws JMSException {
            //TODO: implement method
            throw new NotImplementedException("Not implemented");
        }

    }


    protected class TestQueueConnection extends JMSConnectionMockupMQ.TestConnection
            implements QueueConnection {
        public QueueSession createQueueSession(boolean b, int i)
                throws JMSException {
            return new JMSConnectionMockupMQ.TestQueueSession();
        }

        public ConnectionConsumer createConnectionConsumer(Queue queue,
                                                           String string,
                                                           ServerSessionPool serverSessionPool,
                                                           int i)
                throws JMSException {
            //TODO: implement method
            throw new NotImplementedException("Not implemented");
        }
    }

    protected class TestTopicConnection extends JMSConnectionMockupMQ.TestConnection
            implements TopicConnection {
        public TopicSession createTopicSession(boolean b, int i)
                throws JMSException {
            return new JMSConnectionMockupMQ.TestTopicSession();
        }

        public ConnectionConsumer createConnectionConsumer(Topic topic,
                                                           String string,
                                                           ServerSessionPool serverSessionPool,
                                                           int i)
                throws JMSException {
            //TODO: implement method
            throw new NotImplementedException("Not implemented");
        }
    }

    public class TestConnection implements Connection {
        public boolean isStarted;
        private ExceptionListener exceptionListener;
        
        public Session createSession(boolean b, int i) throws JMSException {
            //TODO: implement method
            throw new NotImplementedException("Not implemented");
        }

        public String getClientID() throws JMSException {
            //TODO: implement method
            throw new NotImplementedException("Not implemented");
        }

        public void setClientID(String string) throws JMSException {
            //TODO: implement method
            throw new NotImplementedException("Not implemented");
        }

        public ConnectionMetaData getMetaData() throws JMSException {
            //TODO: implement method
            throw new NotImplementedException("Not implemented");
        }

        public ExceptionListener getExceptionListener() throws JMSException {
            return this.exceptionListener;
        }

        public void setExceptionListener(ExceptionListener exceptionListener)
                throws JMSException {
            // TODO  implement so it works
            this.exceptionListener = exceptionListener;
        }

        public void start() throws JMSException {
            if (isStarted) {
                throw new IllegalStateException(this + " already started");
            }
            isStarted = true;
        }

        public void stop() throws JMSException {
            //TODO: implement method
            throw new NotImplementedException("Not implemented");
        }

        public void close() throws JMSException {
            //TODO: implement method
            throw new NotImplementedException("Not implemented");
        }

        public ConnectionConsumer createConnectionConsumer(Destination destination,
                                                           String string,
                                                           ServerSessionPool serverSessionPool,
                                                           int i)
                throws JMSException {
            //TODO: implement method
            throw new NotImplementedException("Not implemented");
        }

        public ConnectionConsumer createDurableConnectionConsumer(Topic topic,
                                                                  String string,
                                                                  String string1,
                                                                  ServerSessionPool serverSessionPool,
                                                                  int i)
                throws JMSException {
            //TODO: implement method
            throw new NotImplementedException("Not implemented");
        }
    }

    protected class TestQueueConnectionFactory
            implements QueueConnectionFactory {
        public QueueConnection createQueueConnection() throws JMSException {
            return new JMSConnectionMockupMQ.TestQueueConnection();
        }

        public QueueConnection createQueueConnection(String string, String string1)
                throws JMSException {
            return new JMSConnectionMockupMQ.TestQueueConnection();
        }

        public Connection createConnection() throws JMSException {
            return createQueueConnection();
        }

        public Connection createConnection(String string, String string1)
                throws JMSException {
            return createQueueConnection(string, string);
        }
    }

    protected class TestTopicConnectionFactory
            implements TopicConnectionFactory {

        public TopicConnection createTopicConnection() throws JMSException {
            return new JMSConnectionMockupMQ.TestTopicConnection();
        }

        public TopicConnection createTopicConnection(String string, String string1)
                throws JMSException {
            return new JMSConnectionMockupMQ.TestTopicConnection();
        }

        public Connection createConnection() throws JMSException {
            return createTopicConnection();
        }

        public Connection createConnection(String string, String string1)
                throws JMSException {
            return createTopicConnection(string, string);
        }
    }

    public void onException(JMSException arg0) {
        // TODO Auto-generated method stub
        
    }
}
