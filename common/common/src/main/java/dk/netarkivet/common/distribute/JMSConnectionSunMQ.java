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

import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;

import com.sun.messaging.ConnectionConfiguration;
import com.sun.messaging.Queue;
import com.sun.messaging.Topic;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.Settings;


/**
 * Handles the communication with a Sun JMS broker.
 *
 * Methods are implemented to get a connection, as well as queues and topics.
 * The error handling will try to reconnect on given error scenarios.
 *
 * The warnings and errorcodes reported by Sun Message Queue 4.1 can be found in
 * Appendix A Sun Java System Message Queue 4.1 Developer's Guide for Java
 * Clients: http://docs.sun.com/app/docs/doc/819-7757/aeqgo?a=view
 */
public class JMSConnectionSunMQ extends JMSConnection {
    /** The log. */
    private static final Log log = LogFactory.getLog(JMSConnectionSunMQ.class);

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

    /**
     * Singleton pattern is be used for this class. This is the one and only
     * instance.
     */
    protected static JMSConnectionSunMQ instance;

    /** The errorcode for failure of the JMSbroker to acknowledge a message. */
    static final String PACKET_ACK_FAILED = "C4000";

    /** The errorcode for failure to write to a JMS connection. */
    static final String WRITE_PACKET_FAILED = "C4001";

    /** The errorcode for failure to fread from a JMS connection. */
    static final String READ_PACKET_FAILED = "C4002";

    /**
     * The errorcode signifying that the current session to the JMSbroker has
     * been closed by the jmsbroker. One of the reasons: that the JMSbroker has
     * been shutdown previously.
     */
    static final String SESSION_IS_CLOSED = "C4059";

    /**
     * The errorcode signifying that the JMSbroker has been shutdown. This
     * errorcode is issued by the JMS-client.
     */
    static final String RECEIVED_GOODBYE_FROM_BROKER = "C4056";

    // NOTE: The constants defining setting names below are left non-final on
    // purpose! Otherwise, the static initialiser that loads default values
    // will not run.

    /**
     * <b>settings.common.jms.broker</b>: <br> The JMS broker host contacted by
     * the JMS connection.
     */
    public static String JMS_BROKER_HOST = "settings.common.jms.broker";

    /**
     * <b>settings.common.jms.port</b>: <br> The port the JMS connection should
     * use.
     */
    public static String JMS_BROKER_PORT = "settings.common.jms.port";

    /** Constructor. */
    private JMSConnectionSunMQ() {
        super();
        log.info("Creating instance of " + getClass().getName());
        initConnection();
    }

    /**
     * Intialises an Open Message Queue JMS connection.
     *
     * @return A JMSConnection
     *
     * @throws IOFailure when connection to JMS broker failed
     */
    public static synchronized JMSConnectionSunMQ getInstance()
            throws IOFailure {
        if (instance == null) {
            instance = new JMSConnectionSunMQ();
        }
        return instance;
    }

    /**
     * Returns a new QueueConnectionFactory. This is an SunMQ implementation of
     * QueueConnectionFactory.
     *
     * Notice: The return type is explicitly defined with package prefix to
     * avoid name collision with javax.jms.QueueConnectionFactory
     *
     * @return QueueConnectionFactory
     *
     * @throws JMSException If unable to create a QueueConnectionfactory with
     *                      the necessary properties: imqConsumerflowLimit set
     *                      to 1, imqBrokerHostname and imqBrokerHostPort set to
     *                      the values defined in our settings.
     */
    protected ConnectionFactory getConnectionFactory()
            throws JMSException {
        log.info("Establishing SunMQ JMS Connection to '"
                 + Settings.get(JMS_BROKER_HOST) + ":" + Settings.getInt(
                JMS_BROKER_PORT) + "'");
        com.sun.messaging.ConnectionFactory cFactory
                = new com.sun.messaging.ConnectionFactory();
        cFactory.setProperty(
                ConnectionConfiguration.imqBrokerHostName,
                Settings.get(JMS_BROKER_HOST));
        cFactory.setProperty(
                ConnectionConfiguration.imqBrokerHostPort,
                Settings.get(JMS_BROKER_PORT));
        cFactory.setProperty(
                ConnectionConfiguration.imqConsumerFlowLimit,
                "1");
        return cFactory;
    }

    /**
     * Returns an Queue or a Topic. This is an SunMQ implementation of Queue
     * and Topic. The method depends on the JMS provider being configured to
     * autocreate queues and topics.
     *
     * @param channelName the name of the queue or topic.
     *
     * @return A queue or topic depending on the channel name.
     *
     * @throws JMSException If unable to create the destination.
     */
    protected Destination getDestination(String channelName)
            throws JMSException {
        boolean isTopic = Channels.isTopic(channelName);
        if (isTopic) {
            return new Topic(channelName);
        } else {
            return new Queue(channelName);
        }
    }

    /**
     * Reset the singleton and close the connection by calling super().
     */
    public void cleanup() {
        synchronized (JMSConnectionSunMQ.class) {
            instance = null;
            super.cleanup();
        }
    }

    /**
     * Exceptionhandler for the JMSConnection. Only handles exceptions, if
     * reconnectInProgress is false. Only handles exceptions with errorcodes
     * PACKET_ACK_FAILED, READ_PACKET_FAILED, WRITE_PACKET_FAILED,
     * SESSION_IS_CLOSED, and RECEIVED_GOODBYE_FROM_BROKER.
     * In all these cases, the connection is attempted reestablished.
     *
     * @param e an JMSException
     */
    public void onException(JMSException e) {
        ArgumentNotValid.checkNotNull(e, "JMSException e");
        final String errorcode = e.getErrorCode();
        log.warn("JMSException with errorcode '"
                 + errorcode + "' encountered: " + e);

        // Try to re-establish connections to the jmsbroker only when errorcode
        // matches one of:
        // - PACKET_ACK_FAILED
        // - READ_PACKET_FAILED
        // - WRITE_PACKET_FAILED
        // - SESSION_IS_CLOSED
        // - RECEIVED_GOODBYE_FROM_BROKER
        if (errorcode.equals(PACKET_ACK_FAILED)
            || errorcode.equals(SESSION_IS_CLOSED)
            || errorcode.equals(READ_PACKET_FAILED)
            || errorcode.equals(WRITE_PACKET_FAILED)
            || errorcode.equals(RECEIVED_GOODBYE_FROM_BROKER)) {
            synchronized (JMSConnectionSunMQ.class) {
                reconnect();
            }
        } else {
            log.warn("Exception not handled. "
                     + "Don't know how to handle exceptions with errorcode "
                     + errorcode, e);
        }
    }
}
