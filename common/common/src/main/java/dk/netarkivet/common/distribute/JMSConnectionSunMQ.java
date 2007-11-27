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
import javax.jms.Queue;
import javax.jms.Topic;

import com.sun.messaging.ConnectionConfiguration;
import com.sun.messaging.ConnectionFactory;
import com.sun.messaging.QueueConnectionFactory;
import com.sun.messaging.TopicConnectionFactory;

import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.UnknownID;

/**
 * Handles the communication with a Sun JMS broker.
 *
 * Note on Thread-safety: the methods and fields of JMSConnection are
 * not accessed by multiple threads (though JMSConnection itself creates
 * threads).
 * Thus no synchronization is needed on methods and fields of JMSConnection.
 * A shutdown hook is also added, which closes the connection.
 *
 */
public class JMSConnectionSunMQ extends JMSConnection {
    /** Singleton pattern is be used for this class. This is the one instance. */
    protected static JMSConnectionSunMQ instance = null;

    /**
     * Constructor.
     */
    private JMSConnectionSunMQ() {
        super();
        log.info("Creating instance of " + getClass().getName());
        initConnection();
    }

    /**
     * Intialises a Open Message Queue JMS connection.
     *
     * @return A JMSConnection
     * @throws IOFailure when connection to JMS broker failed
     */
    public static synchronized JMSConnectionSunMQ getInstance() throws UnknownID, IOFailure {
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
        /*((com.sun.messaging.ConnectionFactory) cFactory).setProperty(com.sun.messaging.ConnectionConfiguration.imqReconnectEnabled, "true");
        ((com.sun.messaging.ConnectionFactory) cFactory).setProperty(com.sun.messaging.ConnectionConfiguration.imqReconnectAttempts, "2");
        ((com.sun.messaging.ConnectionFactory) cFactory).setProperty(com.sun.messaging.ConnectionConfiguration.imqReconnectInterval, "10");*/
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
        /*((com.sun.messaging.ConnectionFactory) cFactory).setProperty(com.sun.messaging.ConnectionConfiguration.imqReconnectEnabled, "true");
        ((com.sun.messaging.ConnectionFactory) cFactory).setProperty(com.sun.messaging.ConnectionConfiguration.imqReconnectAttempts, "2");
        ((com.sun.messaging.ConnectionFactory) cFactory).setProperty(com.sun.messaging.ConnectionConfiguration.imqReconnectInterval, "10");*/

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
     * Close the connection and reset the singleton
     */
    public void cleanup() {
        synchronized (JMSConnectionSunMQ.class) {
            instance = null;
            super.cleanup();
        }
    }
}
