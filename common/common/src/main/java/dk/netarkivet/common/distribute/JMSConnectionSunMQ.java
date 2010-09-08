/* $Id$
 * $Date$
 * $Revision$
 * $Author$
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

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.QueueConnection;

import java.util.Arrays;
import javax.jms.QueueSession;
import javax.jms.Session;

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

    public static final String[] RECONNECT_ERRORCODES = {
            "C4000", //Packet acknowledgment failed
            "C4001", //Write packet failed
            "C4002", //Read packet failed
            "C4003", //Connection timed out
            "C4036", //Server error
            "C4056", //Received goodbye from broker
            "C4059", //Session is closed
            "C4062", //Connection is closed
            "C4063"  //Consumer is closed
    };

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

    private QueueConnection qConnection;

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
    public static synchronized JMSConnection getInstance()
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
    protected com.sun.messaging.ConnectionFactory getConnectionFactory()
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
     * Returns an Queue or a Topic. This is an SunMQ implementation of Queue and
     * Topic. The method depends on the JMS provider being configured to
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

    /** Reset the singleton and close the connection by calling super(). */
    public void cleanup() {
        synchronized (JMSConnectionSunMQ.class) {
            instance = null;
            super.cleanup();
        }
    }

    /**
     * Exceptionhandler for the JMSConnection. Will try to reconnect on errors
     * with error codes defined in the constant RECONNECT_ERRORCODES.
     *
     * @param e an JMSException
     */
    public void onException(JMSException e) {
        ArgumentNotValid.checkNotNull(e, "JMSException e");
        final String errorcode = e.getErrorCode();
        log.warn("JMSException with errorcode '"
                 + errorcode + "' encountered: " + e);

        if (Arrays.asList(RECONNECT_ERRORCODES).contains(errorcode)) {
            reconnect();
        } else {
            log.warn("Exception not handled. "
                     + "Don't know how to handle exceptions with errorcode "
                     + errorcode, e);
        }
    }

    @Override
    public synchronized QueueSession getQueueSession() throws JMSException {
        if (qConnection == null ) {
            qConnection = getConnectionFactory().createQueueConnection();
        }
        boolean transacted = false;
        return qConnection.createQueueSession(transacted, Session.AUTO_ACKNOWLEDGE);
    }
}
