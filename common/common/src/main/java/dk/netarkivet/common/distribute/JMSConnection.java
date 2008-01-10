/* $Id$
 * $Date$
 * $Revision$
 * $Author$
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

import javax.jms.Connection;
import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueReceiver;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.Topic;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;
import javax.jms.TopicPublisher;
import javax.jms.TopicSession;
import javax.jms.TopicSubscriber;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.common.Settings;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.CleanupHook;
import dk.netarkivet.common.utils.CleanupIF;

/**
 * Handles the communication with a JMS broker. Note on Thread-safety: the
 * methods and fields of JMSConnection are not accessed by multiple threads
 * (though JMSConnection itself creates threads). Thus no synchronization is
 * needed on methods and fields of JMSConnection. A shutdown hook is also added,
 * which closes the connection.
 *
 */
public abstract class JMSConnection implements ExceptionListener, CleanupIF {
    protected static final Log log = LogFactory.getLog(JMSConnection.class
            .getName());

    /** Various members needed to establish the JMS connection to a Queue. */
    private QueueConnectionFactory myQConnFactory;

    protected QueueConnection myQConn;

    protected QueueSession myQSess;

    /** Various members needed to establish the JMS connection to a Topic. */
    private TopicConnectionFactory myTConnFactory;

    protected TopicConnection myTConn;

    protected TopicSession myTSess;
    

    /**
     * Maps for caching senders and publishers (for queues and topics,
     * respectively).
     */
    private Map<String, QueueSender> senders = new HashMap<String, QueueSender>();

    private Map<String, TopicPublisher> publishers = new HashMap<String, TopicPublisher>();

    /**
     * Set for caching consumers (topic-subscribers and queue-receivers). Serves
     * the purpose of closing all consumers on call to JMSConnection.close()
     */
    protected Map<String, MessageConsumer> consumers = new HashMap<String, MessageConsumer>();

    static final String CONSUMER_KEY_SEPARATOR = "##";
    static final int JMS_MAXTRIES = 3;
    static final long WAIT_BEFORE_JMS_RETRY = 60000;

    /** Shutdown hook that closes the JMS connection. */
    private Thread closeHook;

    private String host;

    private String port;

    /**
     * Class constructor.
     * Sets the broker address and port number using
     * values taken from settings.
     */
    protected JMSConnection() {
        host = Settings.get(Settings.JMS_BROKER_HOST);
        port = Settings.get(Settings.JMS_BROKER_PORT);
    }
    
    /**
     * Initializes the JMS connection. Creating and starting connections to
     * queues and topics. Adds the JMSConnection to a shutdown hook.
     * @param useExceptionListener  if true, use this object as ExceptionListener 
     * for the queue and topic connections.
     * @throws IOFailure if initialization fails.
     */
    protected void initConnection(boolean useExceptionListener ) {
        log.info("JMS connection. Broker:" + host + ";");
        log.info("JMS connection. Port:" + port + ";");

        try {
            myQConnFactory = getQueueConnectionFactory();
            myTConnFactory = getTopicConnectionFactory();

            // Establish a queue connection and a session
            myQConn = myQConnFactory.createQueueConnection();
            myQSess = myQConn.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
            myQConn.setExceptionListener(this);

            // Establish a topic connection and a session
            myTConn = myTConnFactory.createTopicConnection();
            myTSess = myTConn.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);
            myTConn.setExceptionListener(this);

            // start consuming messages
            myQConn.start();
            myTConn.start();

            closeHook = new CleanupHook(this);
            Runtime.getRuntime().addShutdownHook(closeHook);
        } catch (JMSException e) {
            cleanup();
            throw new IOFailure("Could not initialize JMS connection to "
                                + host + ":"
                                + port, e);
        }
    }

    /**
     * Initializes the JMS connection. Creating and starting connections to
     * queues and topics. Adds the JMSConnection to a shutdown hook.
     * Sets this object as ExceptionListener for the queue and topic connections.
     * @throws IOFailure if initialization fails.
     */
    protected void initConnection() {
           this.initConnection(true);
    }

    /**
     * Should be implemented according to a specific JMS broker.
     *
     * @return QueueConnectionFactory
     * @throws JMSException
     */
    protected abstract QueueConnectionFactory getQueueConnectionFactory()
            throws JMSException;

    /**
     * Should be implemented according to a specific JMS broker.
     *
     * @return TopicConnectionFactory
     * @throws JMSException
     */
    protected abstract TopicConnectionFactory getTopicConnectionFactory()
            throws JMSException;

    /**
     * Should be implemented according to a specific JMS broker.
     *
     * @param queueName
     *            the name of the wanted Queue
     * @return Queue
     * @throws JMSException
     */
    protected abstract Queue getQueue(String queueName) throws JMSException;

    /**
     * Should be implemented according to a specific JMS broker.
     *
     * @param topicName
     *            The name of the wanted Topic
     * @return Topic
     * @throws JMSException
     */
    protected abstract Topic getTopic(String topicName) throws JMSException;

    /**
     * Submit an object to the destination queue. This method cannot be
     * overridden. Override the method sendMessage to change functionality.
     *
     * @param nMsg
     *            The NetarkivetMessage to send to the destination queue (null
     *            not allowed)
     * @throws ArgumentNotValid
     *             if nMsg is null.
     * @throws IOFailure
     *             if the operation failed.
     */
    public final void send(NetarkivetMessage nMsg) {
        ArgumentNotValid.checkNotNull(nMsg, "nMsg");
        log.trace("Sent message:\n" + nMsg.toString());
        this.sendMessage(nMsg, nMsg.getTo());
    }

    /**
     * Submit an object to the reply queue.
     * We try to do it JMX_MAXTRIES times before giving up.
     *
     * @param nMsg
     *            The NetarkivetMessage to send to the reply queue (null not
     *            allowed)
     * @throws ArgumentNotValid
     *             if nMsg is null.
     * @throws IOFailure
     *             if the operation failed.
     */
    public void reply(NetarkivetMessage nMsg) {
        ArgumentNotValid.checkNotNull(nMsg, "nMsg");
        JMSException lastException = null;
        boolean operationSuccessful = false;
        int tries = 0;
        
        while (!operationSuccessful || tries < JMS_MAXTRIES) {
            tries++;
            try {
                tries++;
                ObjectMessage msg = myQSess.createObjectMessage(nMsg);
                String queueName = nMsg.getReplyTo().getName();
                // Check if queueSender is in cache
                // If it is not, it is created and stored in cache:
                QueueSender queueSender = senders.get(queueName);
                if (queueSender == null) {
                    queueSender = myQSess.createSender(getQueue(queueName));
                    senders.put(queueName, queueSender);
                }
                synchronized (nMsg) {
                    log.debug("Sending message to " + queueName + "ID = "
                            + nMsg.replyOfId);
                    queueSender.send(msg);
                    log.debug("Sent message to "
                            + queueSender.getQueue().getQueueName());
                    // Note: Id is only updated if the message does not already
                    // have an id. This should never happen, 
                    // since this is a reply.
                    nMsg.updateId(msg.getJMSMessageID());
                }
                operationSuccessful = true;                
            } catch (JMSException e) {
                log.warn("Send failed (try " + tries + "):" + e);
                lastException = e;
            }
        }
        if (!operationSuccessful) {
            throw new IOFailure("Send failed.", lastException);
        }
    }

    /**
     * Submit an ObjectMessage to the destination channel.
     *
     * @param nMsg
     *            the NetarkivetMessage to be wrapped and send as an
     *            ObjectMessage
     * @param to
     *            the destination channel
     * @throws IOFailure
     *             if message failed to be sent.
     */
    protected void sendMessage(NetarkivetMessage nMsg, ChannelID to)
            throws IOFailure {
        JMSException lastException = null;
        boolean operationSuccessful = false;
        int tries = 0;
        
        while (!operationSuccessful || tries < JMS_MAXTRIES) {
            tries++;
            try {
                if (to.isTopic()) {
                    sendToTopic(nMsg, to);
                }
                // If a Channel is not a Topic, it is a Queue:
                else {
                    sendToQueue(nMsg, to);
                }
                operationSuccessful = true;
            } catch (JMSException e) {
                log.warn("Send failed (try " + tries + "):" + e);
                lastException = e;
            }
        }
        if (!operationSuccessful) {
            throw new IOFailure("Send failed.", lastException);
        }
    }

    /**
     * Sends an ObjectMessage on a topic destination.
     *
     * @param nMsg
     *            the NetarkivetMessage to be wrapped and send as an
     *            ObjectMessage.
     * @param to
     *            the destination topic.
     * @throws JMSException
     *             if message failed to be sent.
     */
    private void sendToTopic(NetarkivetMessage nMsg, ChannelID to)
            throws JMSException {
        String topicName = to.getName();
        ObjectMessage msg = myTSess.createObjectMessage(nMsg);

        // Check if topicPublisher is in cache
        // If it is not, it is created and stored in cache:
        TopicPublisher topicPublisher = publishers.get(topicName);
        if (topicPublisher == null) {
            topicPublisher = myTSess.createPublisher(getTopic(topicName));
            publishers.put(topicName, topicPublisher);
        }

        synchronized (nMsg) {
            topicPublisher.publish(msg);
            // Note: Id is only updated if the message does not already have an
            // id. This ensures that resent messages keep the same ID
            nMsg.updateId(msg.getJMSMessageID());
        }
        log.trace("Published message '" + nMsg.toString()
                  + "'");
    }

    /**
     * Sends an ObjectMessage on a queue destination.
     *
     * @param nMsg
     *            the NetarkivetMessage to be wrapped and send as an
     *            ObjectMessage.
     * @param to
     *            the destination topic.
     * @throws JMSException
     *             if message failed to be sent.
     */
    private void sendToQueue(NetarkivetMessage nMsg, ChannelID to)
            throws JMSException {
        String queueName = to.getName();
        ObjectMessage msg = myQSess.createObjectMessage(nMsg);

        // Check if queueSender is in cache
        // If it is not, it is created and stored in cache:
        QueueSender queueSender = senders.get(queueName);
        if (queueSender == null) {
            queueSender = myQSess.createSender(getQueue(queueName));
            senders.put(queueName, queueSender);
        }

        synchronized (nMsg) {
            queueSender.send(msg);
            // Note: Id is only updated if the message does not already have an
            // id. This ensures that resent messages keep the same ID
            nMsg.updateId(msg.getJMSMessageID());
        }
        log.trace("Sent message '" + nMsg.toString() + "'");
    }

    /**
     * Sends a message msg to the channel defined by the parameter to - NOT the
     * channel defined in the message.
     *
     * @param msg
     *            Message to be sent
     * @param to
     *            The destination channel
     */
    public final void resend(NetarkivetMessage msg, ChannelID to) {
        ArgumentNotValid.checkNotNull(msg, "msg");
        ArgumentNotValid.checkNotNull(to, "to");
        sendMessage(msg, to);
    }

    /**
     * Close all connections to the JMS broker.
     *
     * @throws IOFailure
     *             if closing one of the internal connection objects failed.
     */
    protected void close() {
        log.info("\nClosing JMS Connection\n");
        if (closeHook != null) {
            Runtime.getRuntime().removeShutdownHook(closeHook);
        }
        cleanup();
    }

    /**
     * Clean up.
     */
    public void cleanup() {
        try {
            // Close terminates all pending message receives on the connection's
            // sessions' consumers.
            // Close causes any of its sessions' transactions in progress to be
            // rolled back.
            if (myQConn != null) {
                myQConn.close();
            }
            myQConn = null;
            if (myTConn != null) {
                myTConn.close();
            }
            myTConn = null;

            // reset this to make sure anything that keeps an instance of this
            // now invalid object doesn't have any invalid references to work
            // with, and for garbage collection
            if (consumers != null) {
                consumers.clear();
            }
            consumers = null;
            if (senders != null) {
                senders.clear();
            }
            senders = null;
            if (publishers != null) {
                publishers.clear();
            }
        } catch (JMSException e) {
            throw new IOFailure("Error closing JMS Connection.", e);
        }

    }

    /**
     * Unwraps a NetarkivetMessage from an ObjectMessage.
     *
     * @param msg
     *            a javax.jms.ObjectMessage
     * @return a NetarkivetMessage
     * @throws ArgumentNotValid
     *             when msg in valid or reply from JMS server is invalid
     */
    public static NetarkivetMessage unpack(Message msg) throws ArgumentNotValid {
        ArgumentNotValid.checkNotNull(msg, "msg");

        ObjectMessage objMsg = null;
        try {
            objMsg = (ObjectMessage) msg;
        } catch (ClassCastException e) {
            log.warn("Invalid message type: " + msg.getClass());
            throw new ArgumentNotValid("Invalid message type: "
                    + msg.getClass());
        }

        NetarkivetMessage netMsg = null;
        String classname = ""; // for error reporting purposes
        try {
            classname = objMsg.getObject().getClass().getName();
            netMsg = (NetarkivetMessage) objMsg.getObject();
            // Note: Id is only updated if the message does not already have an
            // id. On unpack, this means the first time the message is received.
            netMsg.updateId(msg.getJMSMessageID());
        } catch (ClassCastException e) {
            log.warn("Invalid message type: " + classname, e);
            throw new ArgumentNotValid("Invalid message type: " + classname, e);
        } catch (Exception e) {
            String message = "Message invalid. Unable to unpack "
                             + "message: " + classname;
            log.warn(message, e);
            throw new ArgumentNotValid(message, e);
        }
        log.trace("Unpacked message '" + netMsg + "'");
        return netMsg;
    }

    /**
     * Method adds a listener to the given queue or topic.
     *
     * @param mq the messagequeue to listen to
     * @param ml the messagelistener
     * @throws IOFailure if the operation failed.
     */
    public void setListener(ChannelID mq, MessageListener ml) throws IOFailure {
        ArgumentNotValid.checkNotNull(mq, "ChannelID mq");
        ArgumentNotValid.checkNotNull(ml, "MessageListener ml");
        log.debug("Adding " + ml.toString() + " as listener to "
                + mq.toString());
        String errMsg = "JMS-error - could not add Listener to queue/topic: "
            + mq.getName();
        String key = getConsumerKey(mq, ml);

        int tries = 0;
        boolean operationSuccessful = false;
        JMSException lastException = null;
        while (!operationSuccessful || tries < JMS_MAXTRIES) {
            tries++;
            try {
                if (mq.isTopic()) {
                    TopicSubscriber myTopicSubscriber = myTSess
                    .createSubscriber(getTopic(mq.getName()));
                    myTopicSubscriber.setMessageListener(ml);

                    if (consumers.get(key) == null) {
                        consumers.put(key, myTopicSubscriber);
                    }

                }
                // If a Channel is not a Topic, it is a Queue:
                else {
                    Queue queue = getQueue(mq.getName());
                    QueueReceiver myQueueReceiver = myQSess.createReceiver(queue);
                    myQueueReceiver.setMessageListener(ml);

                    if (consumers.get(key) == null) {
                        consumers.put(key, myQueueReceiver);
                    }
                }
                operationSuccessful = true;
            } catch (JMSException e) {
                lastException = e;
                log.warn(errMsg, e);   
            }
        }
        
        if (!operationSuccessful) {
            throw new IOFailure(errMsg, lastException);
        }
    }

    /**
     * Removes the specified MessageListener from the given queue or topic.
     *
     * @param mq the given queue or topic
     * @param ml a messagelistener
     * @throws IOFailure
     */
    public void removeListener(ChannelID mq, MessageListener ml)
            throws IOFailure {
        ArgumentNotValid.checkNotNull(mq, "ChannelID mq");
        ArgumentNotValid.checkNotNull(ml, "MessageListener ml");
        String errMsg = "JMS-error - could not remove Listener from queue/topic: "
            + mq.getName();
        int tries = 0;
        JMSException lastException = null;
        boolean operationSuccessful = false;
        while (!operationSuccessful || tries < JMS_MAXTRIES) {
            try {
                tries++;
                if (mq.isTopic()) {
                    removeListener(mq, ml, myTConn);
                }
                // If a Channel is not a Topic, it is a Queue:
                else {
                    removeListener(mq, ml, myQConn);
                }
                operationSuccessful = true;
            } catch (JMSException e) {           
                lastException = e;
                log.warn(errMsg, e);
            }
        }       
        if (!operationSuccessful) {
            throw new IOFailure(errMsg, lastException);
        }
    }

    /**
     * Auxiliary method for removing a MessageListener.
     * @param channelID 
     * @param messageListener
     * @param connection 
     * @throws JMSException
     */
    private void removeListener(ChannelID channelID,
            MessageListener messageListener, Connection connection)
            throws JMSException {

        String key = getConsumerKey(channelID, messageListener);
        MessageConsumer messageConsumer = consumers.get(key);

        if (messageConsumer != null) {
            // connection.stop();
            messageConsumer.setMessageListener(null);
            messageConsumer.close();
            consumers.remove(key);
            // connection.start();
        } else {
            log.debug("No MessageConsumer for consumer with key '" + key
                      + "' was found among registered consumers. "
                      + "Unable to remove MessageListener " + messageListener);
        }

    }

    /**
     * Remove all messages waiting in a queue/topic. When this method ends,
     * there are no messages on the queue/topic (though some can still arrive
     * afterwards). For a queue, this does not ensure that all messages
     * disappear, other processes could still make it in and take some. For a
     * topic, this does not ensure that nobody will receive these messages.
     *
     * @param mq
     *            The queue/topic to remove messages from
     * @return A list of all messages removed from the queue/topic.
     */
    public List<Message> removeAllMessages(ChannelID mq) {
        ArgumentNotValid.checkNotNull(mq, "ChannelID mq");
        MessageConsumer consumer = null;
        List<Message> messages = null;
        String message = "JMS error while emptying channel '" + mq + "'";
        int tries = 0;
        JMSException lastException = null;
        boolean operationSuccessful = false;
        while (!operationSuccessful || tries < JMS_MAXTRIES) {
            try {
                messages = new ArrayList<Message>();
                if (mq.isTopic()) {
                    consumer = myTSess.createSubscriber(getTopic(mq.getName()));
                } else {
                    consumer = myQSess.createReceiver(getQueue(mq.getName()));
                }
                Message msg;
                while ((msg = consumer.receiveNoWait()) != null) {
                    messages.add(msg);
                }
                consumer.close();
                operationSuccessful = true;
            } catch (JMSException e) {
                lastException = e;
                if (consumer != null) {
                    try {
                        consumer.close();
                    } catch (JMSException e1) {
                        // Already dealing with one exception (perhaps the same)
                        // We're merely making a best-effort attempt right now.
                    }
                }
                log.warn(message, e);            
            }

        }
        if (!operationSuccessful) {
            throw new IOFailure(message, lastException);
        }
        return messages;
    }

    /**
     * Generate a consumerkey based on the given channelID and messageListener.
     * @param channelID a given channelID
     * @param messageListener a messageListener
     * @return the generated consumerkey.
     */
    private static String getConsumerKey(ChannelID channelID,
            MessageListener messageListener) {
        return channelID.getName() + CONSUMER_KEY_SEPARATOR + messageListener;
    }
    
    /**
     * Get the channelName embedded in a consumerKey.
     * @param consumerKey a consumerKey
     * @return name of channel embedded in a consumerKey
     */
    protected static String getChannelName(String consumerKey) {
        //assumes argument consumerKey was created using metod getConsumerKey()
        return consumerKey.split(CONSUMER_KEY_SEPARATOR)[0];
    }
    
    
    /**
     * Get the hostname for the JMSBroker.
     * @return the hostname for the JMSBroker
     */
    public String getHost() {
        return host;
    }

    /**
     * Get the port for the JMSBroker.
     * @return the port for the JMSBroker
     */
    public String getPort() {
        return port;
    }
    
    /**
     * Exceptionhandler for the JMSConnection.
     * Implemented according to a specific JMS broker.
     * @param e an JMSException
     */
    public abstract void onException(JMSException e);
}
