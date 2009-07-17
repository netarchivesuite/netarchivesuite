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
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Session;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.PermissionDenied;
import dk.netarkivet.common.utils.CleanupHook;
import dk.netarkivet.common.utils.CleanupIF;
import dk.netarkivet.common.utils.TimeUtils;

/**
 * Handles the communication with a JMS broker. Note on Thread-safety: the
 * methods and fields of JMSConnection are not accessed by multiple threads
 * (though JMSConnection itself creates threads). Thus no synchronization is
 * needed on methods and fields of JMSConnection. A shutdown hook is also added,
 * which closes the connection. Class JMSConnection is now also a
 * exceptionhandler for the JMS Connections
 */
public abstract class JMSConnection implements ExceptionListener, CleanupIF {
    /** The log. */
    protected static final Log log = LogFactory.getLog(JMSConnection.class
            .getName());

    /**
     * Separator used in the consumerkey. Separates the ChannelName from the
     * MessageListener.toString().
     */
    protected static final String CONSUMER_KEY_SEPARATOR = "##";

    /** The number to times to (re)try whenever a JMSException is thrown. */
    protected static final int JMS_MAXTRIES = 3;

    /** The JMS Connection. */
    protected Connection connection;

    /**
     * The Session handling messages sent to / received from the NetarchiveSuite
     * queues.
     */
    protected Session myQSess;

    /**
     * The Session handling messages sent to / received from the NetarchiveSuite
     * topics.
     */
    protected Session myTSess;

    /** Map for caching message producers. */
    protected Map<String, MessageProducer> producers
            = new HashMap<String, MessageProducer>();

    /**
     * Map for caching message consumers (topic-subscribers and
     * queue-receivers).
     */
    protected Map<String, MessageConsumer> consumers
            = new HashMap<String, MessageConsumer>();

    /** Semaphore for whether or not a reconnect is in progress. */
    protected static final AtomicBoolean reconnectInProgress
            = new AtomicBoolean(false);

    /** Shutdown hook that closes the JMS connection. */
    protected Thread closeHook;
    /**
     * Singleton pattern is be used for this class. This is the one and only
     * instance.
     */
    protected static JMSConnection instance;

    /**
     * Should be implemented according to a specific JMS broker.
     *
     * @return QueueConnectionFactory
     *
     * @throws JMSException If unable to get QueueConnectionFactory
     */
    protected abstract ConnectionFactory getConnectionFactory()
            throws JMSException;

    /**
     * Should be implemented according to a specific JMS broker.
     *
     * @param destinationName the name of the wanted Queue
     *
     * @return The destination. Note that the implementation should make sure
     *         that this is a Queue or a Topic, as required by the
     *         NetarchiveSuite design. {@link Channels#isTopic(String)}
     *
     * @throws JMSException If unable to get a destination.
     */
    protected abstract Destination getDestination(String destinationName)
            throws JMSException;

    /**
     * Exceptionhandler for the JMSConnection. Implemented according to a
     * specific JMS broker. Should try to reconnect if at all possible.
     *
     * @param e a JMSException
     */
    public abstract void onException(JMSException e);

    /** Class constructor. */
    protected JMSConnection() {
    }

    /**
     * Initializes the JMS connection. Creates and starts connection and
     * sessions for queues and topics. Adds a shutdown hook that closes down
     * JMSConnection. Adds this object as ExceptionListener for the connection.
     *
     * @throws IOFailure if initialization fails.
     */
    protected void initConnection() throws IOFailure {
        log.debug("Initializing a JMS connection " + getClass().getName());

        int tries = 0;
        JMSException lastException = null;
        boolean operationSuccessful = false;
        while (!operationSuccessful && tries < JMS_MAXTRIES) {
            tries++;
            try {
                establishConnectionAndSessions();
                operationSuccessful = true;
            } catch (JMSException e) {
                cleanup();
                log.debug("Connect failed (try " + tries + ")", e);
                lastException = e;
                if (tries < JMS_MAXTRIES) {
                    log.debug("Will sleep a while before trying to"
                              + " connect again");
                    TimeUtils.exponentialBackoffSleep(tries,
                                                      Calendar.MINUTE);
                }
            }
        }
        if (!operationSuccessful) {
            log.warn("Could not initialize JMS connection "
                     + getClass(), lastException);
            throw new IOFailure("Could not initialize JMS connection "
                                + getClass(), lastException);
        }
        closeHook = new CleanupHook(this);
        Runtime.getRuntime().addShutdownHook(closeHook);
    }

    /**
     * Submit an object to the destination queue. This method cannot be
     * overridden. Override the method sendMessage to change functionality.
     *
     * @param msg The NetarkivetMessage to send to the destination queue (null
     *            not allowed)
     *
     * @throws ArgumentNotValid if nMsg is null.
     * @throws IOFailure        if the operation failed.
     */
    public final void send(NetarkivetMessage msg) {
        ArgumentNotValid.checkNotNull(msg, "nMsg");
        log.trace("Sending message:\n" + msg.toString());
        sendMessage(msg, msg.getTo());
    }

    /**
     * Sends a message msg to the channel defined by the parameter to - NOT the
     * channel defined in the message.
     *
     * @param msg Message to be sent
     * @param to  The destination channel
     */
    public final void resend(NetarkivetMessage msg, ChannelID to) {
        ArgumentNotValid.checkNotNull(msg, "msg");
        ArgumentNotValid.checkNotNull(to, "to");
        log.trace("Resending message:\n" + msg.toString());
        sendMessage(msg, to);
    }

    /**
     * Submit an object to the reply queue.
     *
     * @param msg The NetarkivetMessage to send to the reply queue (null not
     *            allowed)
     *
     * @throws ArgumentNotValid if nMsg is null.
     * @throws PermissionDenied if message nMsg has not been sent yet.
     * @throws IOFailure        if unable to reply.
     */
    public final void reply(NetarkivetMessage msg) {
        ArgumentNotValid.checkNotNull(msg, "nMsg");
        // TODO: The below is probably a faulty check: It assumes that you
        // only use reply on sent messages, but sometimes we reply by using a
        // new message, and still use the reply function (at least in unit
        // tests) Commented for now.
        // if (!msg.hasBeenSent()) {
        //     throw new PermissionDenied("Message has not been sent yet");
        // }
        sendMessage(msg, msg.getReplyTo());
    }

    /**
     * Method adds a listener to the given queue or topic.
     *
     * @param mq the messagequeue to listen to
     * @param ml the messagelistener
     *
     * @throws IOFailure if the operation failed.
     */
    public void setListener(ChannelID mq, MessageListener ml) throws IOFailure {
        ArgumentNotValid.checkNotNull(mq, "ChannelID mq");
        ArgumentNotValid.checkNotNull(ml, "MessageListener ml");
        setListener(mq.getName(), ml);
    }

    /**
     * Removes the specified MessageListener from the given queue or topic.
     *
     * @param mq the given queue or topic
     * @param ml a messagelistener
     *
     * @throws IOFailure On network trouble
     */
    public void removeListener(ChannelID mq, MessageListener ml)
            throws IOFailure {
        ArgumentNotValid.checkNotNull(mq, "ChannelID mq");
        ArgumentNotValid.checkNotNull(ml, "MessageListener ml");
        removeListener(ml, mq.getName());
    }

    /** Clean up. */
    public void cleanup() {
        log.info("Starting cleanup");
        try {
            if (closeHook != null) {
                Runtime.getRuntime().removeShutdownHook(closeHook);
            }
        } catch (IllegalStateException e) {
            //Okay, it just means we are already shutting down.
        }
        closeHook = null;
        waitUntilReconnectIsDone();
        try {
            // Close terminates all pending message received on the connection's
            // sessions' consumers.
            if (connection != null) { // close connection
                connection.close();
            }
            connection = null;
            // reset this to make sure anything that keeps an instance of this
            // now invalid object doesn't have any invalid references to work
            // with, and for garbage collection.
            if (consumers != null) {
                consumers.clear();
            }
            consumers = null;
            if (producers != null) {
                producers.clear();
            }
            producers = null;
        } catch (JMSException e) {
            throw new IOFailure("Error closing JMS Connection.", e);
        }
        log.info("Cleanup finished");
    }

    /**
     * Unwraps a NetarkivetMessage from an ObjectMessage.
     *
     * @param msg a javax.jms.ObjectMessage
     *
     * @return a NetarkivetMessage
     *
     * @throws ArgumentNotValid when msg in valid or format of JMS Object
     *                          message is invalid
     */
    public static NetarkivetMessage unpack(Message msg)
            throws ArgumentNotValid {
        ArgumentNotValid.checkNotNull(msg, "msg");

        ObjectMessage objMsg;
        try {
            objMsg = (ObjectMessage) msg;
        } catch (ClassCastException e) {
            log.warn("Invalid message type: " + msg.getClass());
            throw new ArgumentNotValid("Invalid message type: "
                                       + msg.getClass());
        }

        NetarkivetMessage netMsg;
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
     * Submit an ObjectMessage to the destination channel.
     *
     * @param nMsg the NetarkivetMessage to be wrapped and send as an
     *             ObjectMessage
     * @param to   the destination channel
     *
     * @throws IOFailure if message failed to be sent.
     */
    protected void sendMessage(NetarkivetMessage nMsg, ChannelID to)
            throws IOFailure {
        JMSException lastException = null;
        boolean operationSuccessful = false;
        int tries = 0;

        while (!operationSuccessful && tries < JMS_MAXTRIES) {
            tries++;
            waitUntilReconnectIsDone();
            try {
                doSend(nMsg, to);
                operationSuccessful = true;
            } catch (JMSException e) {
                log.debug("Send failed (try " + tries + ")", e);
                lastException = e;
                if (tries < JMS_MAXTRIES) {
                    log.debug("Will sleep a while before trying to"
                              + " send again");
                    //TODO: Is it reasonable to reconnect here? 
                    TimeUtils.exponentialBackoffSleep(tries,
                                                      Calendar.MINUTE);
                }
            }
        }
        if (!operationSuccessful) {
            log.warn("Send failed", lastException);
            throw new IOFailure("Send failed.", lastException);
        }
    }

    /**
     * Do a reconnect to the JMSbroker. Does absolutely nothing, if already in
     * the process of reconnecting.
     */
    protected void reconnect() {
        try {
            if (!reconnectInProgress.compareAndSet(false, true)) {
                log.debug("Reconnection already in progress. Do nothing");
                return;
            }
            log.info("Trying to reconnect to jmsbroker");

            Map<String, MessageConsumer> savedConsumers
                    = new HashMap<String, MessageConsumer>(consumers);

            boolean operationSuccessful = false;
            JMSException lastException = null;
            int tries = 0;
            while (!operationSuccessful && tries < JMS_MAXTRIES) {
                tries++;
                try {
                    doReconnect(savedConsumers);
                    operationSuccessful = true;
                } catch (JMSException e) {
                    lastException = e;
                    log.debug("Reconnect failed (try " + tries + ")", e);
                    if (tries < JMS_MAXTRIES) {
                        log.debug("Will sleep a while before trying to"
                                  + " reconnect again");
                        TimeUtils.exponentialBackoffSleep(tries,
                                                          Calendar.MINUTE);
                    }
                }
            }
            if (!operationSuccessful) {
                log.warn("Reconnect to JMS broker failed",
                         lastException);
                cleanup();
            }
        } finally {
            // Tell everybody, that we are not trying to reconnect any longer
            reconnectInProgress.compareAndSet(true, false);
        }
    }

    /**
     * Helper method for getting the right producer for a queue or topic.
     *
     * @param queueName The name of the channel
     *
     * @return The producer for that channel. A new one is created, if none
     *         exists.
     *
     * @throws JMSException If a new producer cannot be created.
     */
    private MessageProducer getProducer(String queueName) throws JMSException {
        // Check if producer is in cache
        // If it is not, it is created and stored in cache:
        MessageProducer producer = producers.get(queueName);
        if (producer == null) {
            producer = getSessionForDestination(queueName).createProducer(
                    getDestination(queueName));
            producers.put(queueName, producer);
        }
        return producer;
    }

    /**
     * Helper method for getting the right consumer for a queue or topic, and
     * message listener.
     *
     * @param channelName The name of the channel
     * @param ml The message listener to add as listener to the channel
     *
     * @return The producer for that channel. A new one is created, if none
     *         exists.
     *
     * @throws JMSException If a new producer cannot be created.
     */
    private MessageConsumer getConsumer(String channelName, MessageListener ml)
            throws JMSException {
        String key = getConsumerKey(channelName, ml);
        MessageConsumer consumer = consumers.get(key);
        if (consumer == null) {
            consumer = getSessionForDestination(channelName).createConsumer(
                    getDestination(channelName));
            consumers.put(key, consumer);
        }
        return consumer;
    }

    private ObjectMessage getObjectMessage(ChannelID channel,
                                           NetarkivetMessage nMsg)
            throws JMSException {
        ObjectMessage objectMessage;
        objectMessage = getSessionForDestination(
                channel.getName()).createObjectMessage(nMsg);
        return objectMessage;
    }

    private Session getSessionForDestination(String queueName) {
        if (Channels.isTopic(queueName)) {
            return myTSess;
        } else {
            return myQSess;
        }
    }

    /**
     * Generate a consumerkey based on the given channel name and
     * messageListener.
     *
     * @param channel         Channel name
     * @param messageListener a messageListener
     *
     * @return the generated consumerkey.
     */
    protected static String getConsumerKey(
            String channel, MessageListener messageListener) {
        return channel + CONSUMER_KEY_SEPARATOR + messageListener;
    }

    /**
     * Get the channelName embedded in a consumerKey.
     *
     * @param consumerKey a consumerKey
     *
     * @return name of channel embedded in a consumerKey
     */
    private static String getChannelName(String consumerKey) {
        //assumes argument consumerKey was created using metod getConsumerKey()
        return consumerKey.split(CONSUMER_KEY_SEPARATOR)[0];
    }

    /**
     * Helper method to establish one QueueConnection and associated Session,
     * and one TopicConnection and associated Session.
     *
     * @throws JMSException If some JMS error occurred during the creation of
     *                      the required JMS connections and sessions
     */
    private void establishConnectionAndSessions() throws JMSException {
        // Establish a queue connection and a session
        connection = getConnectionFactory().createConnection();
        myQSess = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        myTSess = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        connection.setExceptionListener(this);
        connection.start();
    }

    /**
     * Sends an ObjectMessage on a queue destination.
     *
     * @param msg the NetarkivetMessage to be wrapped and send as an
     *            ObjectMessage.
     * @param to  the destination topic.
     *
     * @throws JMSException if message failed to be sent.
     */
    private void doSend(NetarkivetMessage msg, ChannelID to)
            throws JMSException {
        ObjectMessage message = getObjectMessage(to, msg);
        synchronized (msg) {
            getProducer(to.getName()).send(message);
            // Note: Id is only updated if the message does not already have an
            // id. This ensures that resent messages keep the same ID
            msg.updateId(message.getJMSMessageID());
        }
        log.trace("Sent message '" + msg.toString() + "'");
    }

    /**
     * Method adds a listener to the given queue or topic.
     *
     * @param channelName the messagequeue to listen to
     * @param ml          the messagelistener
     *
     * @throws IOFailure if the operation failed.
     */
    private void setListener(String channelName, MessageListener ml) {
        log.debug("Adding " + ml.toString() + " as listener to "
                  + channelName);
        String errMsg = "JMS-error - could not add Listener to queue/topic: "
                        + channelName;

        int tries = 0;
        boolean operationSuccessful = false;
        JMSException lastException = null;
        while (!operationSuccessful && tries < JMS_MAXTRIES) {
            tries++;
            waitUntilReconnectIsDone();
            try {
                getConsumer(channelName, ml).setMessageListener(ml);
                operationSuccessful = true;
            } catch (JMSException e) {
                lastException = e;
                log.debug("Set listener failed (try " + tries + ")", e);
                if (tries < JMS_MAXTRIES) {
                    log.debug("Will sleep a while before trying to"
                              + " set listener again");
                    TimeUtils.exponentialBackoffSleep(tries, Calendar.MINUTE);
                }
            }
        }

        if (!operationSuccessful) {
            log.warn(errMsg, lastException);
            throw new IOFailure(errMsg, lastException);
        }
    }

    private void removeListener(MessageListener ml, String channelName) {
        String errMsg = "JMS-error - could not remove Listener from "
                        + "queue/topic: " + channelName;
        int tries = 0;
        JMSException lastException = null;
        boolean operationSuccessful = false;

        log.info("Removing listener from channel '" + channelName + "'");
        while (!operationSuccessful && tries < JMS_MAXTRIES) {
            waitUntilReconnectIsDone();
            try {
                tries++;
                MessageConsumer messageConsumer = getConsumer(channelName, ml);
                messageConsumer.close();
                consumers.remove(getConsumerKey(channelName, ml));
                operationSuccessful = true;
            } catch (JMSException e) {
                lastException = e;
                log.warn(errMsg, e);
                // Wait sometime before trying again
                TimeUtils.exponentialBackoffSleep(tries, Calendar.MINUTE);
            }
        }
        if (!operationSuccessful) {
            throw new IOFailure(errMsg, lastException);
        }
    }

    /**
     * Wait until the reconnection process is finished. Intervals of one minute
     * are waited before checking the status of the reconnection.
     */
    private void waitUntilReconnectIsDone() {
        while (reconnectInProgress.get()) {
            // Wait one minute before checking if reconnection is finished
            log.debug("Waiting one minute to let the reconnect process finish");
            TimeUtils.exponentialBackoffSleep(1, Calendar.MINUTE);
        }
    }

    /**
     * Reconnect to JMSBroker and reestablish sessions. Resets senders and
     * publishers.
     *
     * @param savedConsumers Listeners to readd after reestablishing connection.
     *
     * @throws JMSException If unable to reconnect to JMSBroker and/or
     *                      reestablish sesssions
     */
    private void doReconnect(Map<String, MessageConsumer> savedConsumers)
            throws JMSException {
        try {
            cleanup();
        } catch (IOFailure e) {
            log.debug("Trouble cleaning up JMSConnection", e);
            //Harmless, just try to reconnect.
        }
        establishConnectionAndSessions();
        // Add listeners already stored in the consumers map
        log.debug("Re-add listeners");
        for (Map.Entry<String, MessageConsumer> consumer
                : savedConsumers.entrySet()) {
            try {
                setListener(getChannelName(consumer.getKey()),
                            consumer.getValue().getMessageListener());
            } catch (IOFailure e) {
                // We cannot do anything more at this point
                log.warn("Exception thrown while adding listeners", e);
            }
        }
        log.info("Reconnect successful");
    }
}
