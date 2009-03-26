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

import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.Queue;
import javax.jms.QueueReceiver;
import javax.jms.Topic;
import javax.jms.TopicSubscriber;
import java.util.Calendar;

import com.sun.messaging.ConnectionConfiguration;
import com.sun.messaging.ConnectionFactory;
import com.sun.messaging.QueueConnectionFactory;
import com.sun.messaging.TopicConnectionFactory;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.UnknownID;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.utils.TimeUtils;


/**
 * Handles the communication with a Sun JMS broker.
 *
 * Note on Thread-safety: the methods and fields of JMSConnection are
 * not accessed by multiple threads (though JMSConnection itself creates
 * threads).
 * Thus no synchronization is needed on methods and fields of JMSConnection.
 * A shutdown hook is also added, which closes the connection.
 *
 * The warnings and errorcodes reported by Sun Message Queue 4.1 can be found
 * in Appendix A Sun Java System Message Queue 4.1 Developer's Guide
 * for Java Clients: http://docs.sun.com/app/docs/doc/819-7757/aeqgo?a=view
 */
public class JMSConnectionSunMQ extends JMSConnection {
    /** The default place in classpath where the settings file can be found. */
    private static String DEFAULT_SETTINGS_CLASSPATH
            = "dk/netarkivet/common/distribute/JMSConnectionSunMQSettings.xml";

    /*
     * The static initialiser is called when the class is loaded.
     * It will add default values for all settings defined in this class, by
     * loading them from a settings.xml file in classpath.
     */
    static {
        Settings.addDefaultClasspathSettings(
                DEFAULT_SETTINGS_CLASSPATH
        );
    }

    /** Singleton pattern is be used for this class.
     * This is the one and only instance. */
    protected static JMSConnectionSunMQ instance = null;

    /** The errorcode for failure of the JMSbroker to acknowledge a message. */
    final static String PACKET_ACK_FAILED = "C4000";

    /** The errorcode signifying that the current session to the JMSbroker
     * has been closed by the jmsbroker.
     * One of the reasons: that the JMSbroker has been shutdown previously.
     */
    final static String SESSION_IS_CLOSED = "C4059";

    /**
     * The errorcode signifying that the JMSbroker
     * has been shutdown. This errorcode is issued by the JMS-client.
     */
    final static String RECEIVED_GOODBYE_FROM_BROKER = "C4056";

    // NOTE: The constants defining setting names below are left non-final on
    // purpose! Otherwise, the static initialiser that loads default values
    // will not run.

    /** 
     * <b>settings.common.jms.broker</b>: <br>
     * The JMS broker host contacted by the JMS connection. */
    public static final String JMS_BROKER_HOST = "settings.common.jms.broker";

    /** 
     * <b>settings.common.jms.port</b>: <br>
     * The port the JMS connection should use. */
    public static final String JMS_BROKER_PORT = "settings.common.jms.port";

    /**
     * Constructor.
     */
    private JMSConnectionSunMQ() {
        super();
        log.info("Creating instance of " + getClass().getName());
        initConnection();
    }

    /**
     * Intialises an Open Message Queue JMS connection.
     *
     * @return A JMSConnection
     * @throws IOFailure when connection to JMS broker failed
     */
    public static synchronized JMSConnectionSunMQ getInstance()
    throws UnknownID, IOFailure {
        if (instance == null) {
            instance = new JMSConnectionSunMQ();
        }
        return instance;
    }

    /**
     * Returns a new QueueConnectionFactory. This is an SunMQ
     * implementation of QueueConnectionFactory.
     *
     * Notice: The return type is explicitly defined with package prefix to
     * avoid name collision with javax.jms.QueueConnectionFactory
     *
     * @throws JMSException
     * @return QueueConnectionFactory
     */
    protected QueueConnectionFactory getQueueConnectionFactory()
    throws JMSException {
        QueueConnectionFactory cFactory = new QueueConnectionFactory();
        ((ConnectionFactory) cFactory).setProperty(
                ConnectionConfiguration.imqBrokerHostName,
                getHost());
        ((ConnectionFactory) cFactory).setProperty(
                ConnectionConfiguration.imqBrokerHostPort,
                String.valueOf(getPort()));
        ((ConnectionFactory) cFactory).setProperty(
                ConnectionConfiguration.imqConsumerFlowLimit,
                "1");
        return cFactory;
    }

    /**
     * Returns a new TopicConnectionFactory. This is an SunMQ
     * implementation of TopicConnectionFactory.
     *
     * Notice: The return type is explicitly defined with package prefix to
     * avoid name collision with javax.jms.TopicConnectionFactory
     *
     * @throws JMSException
     * @return TopicConnectionFactory
     */
    protected TopicConnectionFactory getTopicConnectionFactory() throws JMSException {
        TopicConnectionFactory cFactory = new TopicConnectionFactory();
        ((ConnectionFactory) cFactory).setProperty(
                ConnectionConfiguration.imqBrokerHostName,
                getHost());
        ((ConnectionFactory) cFactory).setProperty(
                ConnectionConfiguration.imqBrokerHostPort,
                String.valueOf(getPort()));
        ((ConnectionFactory) cFactory).setProperty(
                ConnectionConfiguration.imqConsumerFlowLimit,
                "1");
        return cFactory;
    }

    /**
     * Returns an Queue. This is an SunMQ implementation of Queue.
     * If no queue exists a new one will be created.
     *
     * @param queueName the name of the queue.
     * @throws JMSException
     * @return Queue
     */
    protected Queue getQueue(String queueName) throws JMSException {
        return new com.sun.messaging.Queue(queueName);
    }

    /**
     * Returns an Topic. This is an SunMQ implementation of Topic.
     * If no topic exists a new one will be created.
     *
     * @param topicName the name of the topic.
     * @throws JMSException
     * @return Topic
     */
    protected Topic getTopic(String topicName) throws JMSException {
        return new com.sun.messaging.Topic(topicName);
    }

    /**
     * Close the connection and reset the singleton.
     */
    public void cleanup() {
        synchronized (JMSConnectionSunMQ.class) {
            instance = null;
            super.cleanup();
        }
    }
    
    /**
     * Get the host where the currently used JMSbroker resides.
     * Only used by logging
     * @return the host where the currently used JMSbroker resides
     */
    public String getHost()  {
        return Settings.get(JMS_BROKER_HOST);
    }
    
    /**
     * Get the number of the port which the currently used JMSbroker uses.
     * Only used by logging
     * @return the number of the port which the currently used JMSbroker uses.
     */
    public int getPort()  {
        return Settings.getInt(JMS_BROKER_PORT);
    }

    /**
     * Exceptionhandler for the JMSConnection.
     * Only handles exceptions, if reconnectInProgress is false.
     * Only handles exceptions with errorcodes PACKET_ACK_FAILED,
     * SESSION_IS_CLOSED, and RECEIVED_GOODBYE_FROM_BROKER.
     *
     * @param e an JMSException
     */
    public void onException(JMSException e) {
        ArgumentNotValid.checkNotNull(e, "JMSException e");
        final String errorcode = e.getErrorCode();
        log.warn("JMSException with errorcode '"
                +  errorcode + "' encountered: " + e);

        // Try to re-establish connections to the jmsbroker only when errorcode
        // matches one of:
        // - PACKET_ACK_FAILED
        // - SESSION_IS_CLOSED
        // - RECEIVED_GOODBYE_FROM_BROKER
        if (errorcode.equals(PACKET_ACK_FAILED)
                || errorcode.equals(SESSION_IS_CLOSED)
                || errorcode.equals(RECEIVED_GOODBYE_FROM_BROKER)) {
            performReconnect();
        } else {
            log.warn("Exception not handled. "
                    + "Don't know how to handle exceptions with errorcode "
                    + errorcode);
        }
    }

    /**
     * Do a reconnect to the JMSbroker.
     * Does absolutely nothing, if already in the process of reconnecting.
     */
    private void performReconnect(){
        if (!reconnectInProgress.compareAndSet(false, true)) {
            log.debug("Reconnection already in progress. Do nothing");
            return;
        }

        log.info("Trying to reconnect to jmsbroker at "
                + getHost() + ":" + getPort());

        boolean operationSuccessful = false;
        JMSException lastException = null;
        int tries = 0;
        while (!operationSuccessful && tries < JMS_MAXTRIES) {
            tries++;
            try {
                reconnect();
                operationSuccessful = true;
            } catch (JMSException e) {
                lastException = e;
                log.warn("Nr #" + tries
                        + " attempt at reconnect failed with exception. ", e);
                if (tries < JMS_MAXTRIES) {
                    log.debug("Will sleep a while before trying to"
                        + "reconnect again");
                    TimeUtils.exponentialBackoffSleep(tries, Calendar.MINUTE);
                }
            }

        }
        if (!operationSuccessful) {
            // Tell everybody, that we are not trying to reconnect any longer
            reconnectInProgress.compareAndSet(true, false);
            throw new IOFailure("Reconnect failed with exception ",
                    lastException);
        }

        // Add listeners already stored in the consumers map
        log.debug("Add listeners");
        try {
            for (String consumerkey: consumers.keySet()) {
                String channelName = getChannelName(consumerkey);
                boolean isTopic = Channels.isTopic(channelName);
                MessageConsumer mc = consumers.get(consumerkey);
                if (isTopic) {
                    TopicSubscriber myTopicSubscriber 
                        = myTSess.createSubscriber(getTopic(channelName));
                    myTopicSubscriber.setMessageListener(
                            mc.getMessageListener());
                } else {
                    Queue queue = getQueue(channelName);
                    QueueReceiver myQueueReceiver
                        = myQSess.createReceiver(queue);
                    myQueueReceiver.setMessageListener(mc.getMessageListener());
                }
            }
            log.debug("Using this() as exceptionhandler for the two JMS"
                + "Connections");
            myQConn.setExceptionListener(this);
            myTConn.setExceptionListener(this);
        } catch (JMSException e) {
            // We cannot do anything more at this point
            log.warn("Exception thrown while adding listeners", e);
        }
        reconnectInProgress.compareAndSet(true, false);
        log.info("Reconnect successful");
    }

    /**
     * Reconnect to JMSBroker and reestablish sessions.
     * Resets senders and publishers.
     * @throws JMSException
     */
    private void reconnect() throws JMSException {
        try {
            myQSess.close();
        } catch (Exception e) {
            log.debug("Unable to close current Queue Session", e);
        }

        try {
            myQConn.close();
        } catch (Exception e) {
            log.debug("Unable to close current Queue Connection", e);
        }

        try {
            myTSess.close();
        } catch (Exception e) {
            log.debug("Unable to close current Topic Session", e);
        }

        try {
            myTConn.close();
        } catch (Exception e) {
            log.debug("Unable to close current Topic Connection", e);
        }

        establishConnectionAndSessions();

        // Reset senders & publishers
        senders.clear();
        publishers.clear();
    }

}
