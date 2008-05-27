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

package dk.netarkivet.common.utils;

import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.TabularData;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import javax.naming.ServiceUnavailableException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.sun.jndi.rmi.registry.RegistryContextFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.common.Settings;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.UnknownID;

/**
 * Various JMX-related utility functions that have nowhere better to live.
 *
 */
public class JMXUtils {
    public static final Log log = LogFactory.getLog(JMXUtils.class.getName());

    /** The system property that Java uses to get an initial context
     * for JNDI. This must be set for RMI connections to work.
     */
    private static final String JNDI_INITIAL_CONTEXT_PROPERTY =
        "java.naming.factory.initial";

    /** The JMX timeout in milliseconds. */
    private static final double TIMEOUT
            = (double) Settings.getLong(Settings.JMX_TIMEOUT) * 1000.0;
    /** The maximum number of times we back off on getting an mbean or a job.
     * The cumulative time trying is 2^(MAX_TRIES) milliseconds,
     * thus the constant is defined as log2(TIMEOUT), as set in settings.
     */
    public static final int MAX_TRIES = (int) Math.ceil(
            Math.log(TIMEOUT) / Math.log(2.0));

    /**
     * If no initial JNDI context has been configured,
     * configures the system to use Sun's standard one.
     * This is necessary for RMI connections to work.
     */
    private static void ensureJndiInitialContext() {

       if (System.getProperty(JNDI_INITIAL_CONTEXT_PROPERTY) == null) {
           System.setProperty(
                   JNDI_INITIAL_CONTEXT_PROPERTY,
                   RegistryContextFactory.class.getCanonicalName());
           log.info("Set property '" + JNDI_INITIAL_CONTEXT_PROPERTY + "' to: "
                   + RegistryContextFactory.class.getCanonicalName());
       } else {
           log.debug("Property '" + JNDI_INITIAL_CONTEXT_PROPERTY
                   + "' is set to: "
                   + System.getProperty(JNDI_INITIAL_CONTEXT_PROPERTY));
       }
    }

    /**
     * Constructs the same service URL that JConsole does
     * on the basis of a server name, a JMX port number,
     * and a RMI port number.
     *
     * Example URL:
     * service:jmx:rmi://0.0.0.0:9999/jndi/rmi://0.0.0.0:1099/JMXConnector
     * where RMI port number = 9999, JMX port number = 1099
     * server = 0.0.0.0 aka localhost(?).
     *
     * @param server The server that should be connected to using
     * the constructed URL.
     * @param jmxPort The number of the JMX port that should be connected to
     * using the constructed URL (may not be a negative number)
     * @param rmiPort The number of the RMI port that should be connected to
     * using the constructed URL, or -1 if the default RMI port should be used.
     * @return the constructed URL.
     */
    public static JMXServiceURL getUrl(String server,
            int jmxPort, int rmiPort) {
        ArgumentNotValid.checkNotNullOrEmpty(server, "String server");
        ArgumentNotValid.checkNotNegative(jmxPort, "int jmxPort");
        
        String url;
        if (rmiPort != -1) {
            url = "service:jmx:rmi://" + server + ":" + rmiPort + "/jndi/rmi://"
                  + server + ":" + jmxPort + "/jmxrmi";
        } else {
            url = "service:jmx:rmi:///jndi/rmi://"
                  + server + ":" + jmxPort + "/jmxrmi";
        }
        log.trace("Created url for JMX-connections: " + url);
        try {
            return new JMXServiceURL(url);
        } catch (MalformedURLException e) {
            throw new UnknownID(
                    "Could not create new JMXServiceURL from "
                    + url, e);
        }
    }

    /**
     * Returns a connection to a remote MbeanServer defined by
     * the given arguments.
     * @param server the remote servername
     * @param jmxPort the remote jmx-port
     * @param rmiPort the remote rmi-port
     * @param userName the username
     * @param password the password
     * @return a MBeanServerConnection to a remote MbeanServer defined by
     * the given arguments.
     */
    public static MBeanServerConnection getMBeanServerConnection(
            String server, int jmxPort,
            int rmiPort, String userName, String password) {
        ArgumentNotValid.checkNotNullOrEmpty(server, "String server");
        ArgumentNotValid.checkNotNegative(jmxPort, "int jmxPort");
        ArgumentNotValid.checkNotNegative(rmiPort, "int rmiPort");
        ArgumentNotValid.checkNotNullOrEmpty(userName, "String userName");
        ArgumentNotValid.checkNotNullOrEmpty(password, "String password");

        JMXServiceURL jmxServiceUrl = getUrl(server, jmxPort, rmiPort);
        Map<String,String[]> credentials =
            packageCredentials(userName, password);
        return getMBeanServerConnection(jmxServiceUrl, credentials);
    }

    /**
     * Connects to the given (url-specified) service point,
     * sending the given credentials as login.
     * @param url The JMX service url of some JVM on some machine.
     * @param credentials a map with (at least) one entry, mapping
     * "jmx.remote.credentials" to a String array of length 2.
     * Its first item should be the user name.
     * Its second item should be the password.
     * @return An MBeanServerConnection representing the
     * connected session.
     */
    public static MBeanServerConnection getMBeanServerConnection(
            JMXServiceURL url,
            Map<String,String[]> credentials) {
        ArgumentNotValid.checkNotNull(url, "JMXServiceURL url");
        ArgumentNotValid.checkNotNull(credentials, 
                "Map<String,String[]> credentials");
        try {
            ensureJndiInitialContext();
            return JMXConnectorFactory.connect(url, credentials)
                .getMBeanServerConnection();
        } catch (IOException e) {
            throw new IOFailure("Could not connect to "
                    + url.toString() , e);
        }
    }

    /**
     * Packages credentials as an environment for JMX connections.
     * This packaging has the same form that JConsole uses:
     * a one-entry Map, the mapping of "jmx.remote.credentials" being
     * an array containing the user name and the password.
     * @param userName The user to login as
     * @param password The password to use for that user
     * @return the packaged credentials
     */
    public static Map<String,String[]> packageCredentials(
            String userName, String password) {
        ArgumentNotValid.checkNotNullOrEmpty(userName, "String userName");
        ArgumentNotValid.checkNotNullOrEmpty(password, "String password");        
        Map<String,String[]> credentials = new HashMap<String,String[]>(1);
        credentials.put("jmx.remote.credentials",
                new String[]{userName, password});
        return credentials;
    }

    /** Execute a command on a bean.
     *
     * @param connection Connection to the server holding the bean.
     * @param beanName Name of the bean.
     * @param command Command to execute.
     * @param arguments Arguments to the command.  Only string arguments are
     * possible at the moment.
     * @return The return value of the executed command.
     */
    public static Object executeCommand(MBeanServerConnection connection,
                                  String beanName, String command,
                                  String... arguments) {
        ArgumentNotValid.checkNotNull(connection, 
                "MBeanServerConnection connection");
        ArgumentNotValid.checkNotNullOrEmpty(beanName, "String beanName");
        ArgumentNotValid.checkNotNullOrEmpty(command, "String command");
        ArgumentNotValid.checkNotNull(arguments, "String... arguments");
        
        log.debug("Preparing to execute " + command + " with args " +
                  Arrays.toString(arguments) + " on " + beanName);
        try {
            final String[] signature = new String[arguments.length];
            Arrays.fill(signature, String.class.getName());
            // The first time we attempt to connect to an mbean, we might have
            // to wait a bit for it to appear
            Throwable lastException;
            int tries = 0;
            do {
                tries++;
                try {
                    Object ret = connection.invoke(getBeanName(beanName), command,
                                             arguments, signature);
                    log.debug("Executed command " + command + " returned " + ret);
                    return ret;
                } catch (InstanceNotFoundException e) {
                    lastException = e;
                    if (tries < MAX_TRIES) {
                        TimeUtils.exponentialBackoffSleep(tries);
                    }
                }
            } while (tries < MAX_TRIES);
            throw new IOFailure("Failed to find MBean " + beanName
                                + " for executing " + command
                                + " after " + tries + " attempts",
                                lastException);
        } catch (MBeanException e) {
            throw new IOFailure("MBean exception for " + beanName, e);
        } catch (ReflectionException e) {
            throw new IOFailure("Reflection exception for " + beanName, e);
        } catch (IOException e) {
            throw new IOFailure("Generic IO problem for " + beanName, e);
        }
    }

    /** Get the value of an attribute from a bean.
     *
     * @param beanName Name of the bean to get an attribute for.
     * @param attribute Name of the attribute to get.
     * @param connection A connection to the JMX server for the bean.
     * @return Value of the attribute.
     */
    public static Object getAttribute(String beanName, String attribute,
                                      MBeanServerConnection connection) {
        ArgumentNotValid.checkNotNullOrEmpty(beanName, "String beanName");
        ArgumentNotValid.checkNotNullOrEmpty(attribute, "String attribute");
        ArgumentNotValid.checkNotNull(connection, "MBeanServerConnection connection");
        
        log.debug("Preparing to get attribute " + attribute
                 + " on " + beanName);
        try {
            // The first time we attempt to connect to an mbean, we might have
            // to wait a bit for it to appear
            Throwable lastException;
            int tries = 0;
            do {
                tries++;
                try {
                    Object ret = connection
                            .getAttribute(getBeanName(beanName), attribute);
                    log.debug("Getting attribute " + attribute
                             + " returned " + ret);
                    return ret;
                } catch (InstanceNotFoundException e) {
                    lastException = e;
                    if (tries < MAX_TRIES) {
                        TimeUtils.exponentialBackoffSleep(tries);
                    }
                }
            } while (tries < MAX_TRIES);
            throw new IOFailure("Failed to find MBean " + beanName
                                + " for getting attribute " + attribute
                                + " after " + tries + " attempts",
                                lastException);
        } catch (AttributeNotFoundException e) {
            throw new IOFailure("MBean exception for " + beanName, e);
        } catch (MBeanException e) {
            throw new IOFailure("MBean exception for " + beanName, e);
        } catch (ReflectionException e) {
            throw new IOFailure("Reflection exception for " + beanName, e);
        } catch (IOException e) {
            throw new IOFailure("Generic IO problem for " + beanName, e);
        }
    }

    /** Get a bean name from a string version.
     *
     * @param beanName String representation of bean name
     * @return Object representing that bean name.
     */
    public static ObjectName getBeanName(String beanName) {
        ArgumentNotValid.checkNotNullOrEmpty(beanName, "String beanName");
        try {
            return new ObjectName(beanName);
        } catch (MalformedObjectNameException e) {
            throw new ArgumentNotValid("Name " + beanName + " is not a valid "
                                       + "object name", e);
        }
    }

    /** Get a JMXConnector to a given host and port, using login and password.
     *
     * @param hostName The host to attempt to connect to.
     * @param jmxPort The port on the host to connect to (a non-negative number)
     * @param login The login name to authenticate as (typically "controlRole"
     * or "monitorRole".
     * @param password The password for JMX access
     * @return A JMX connector to the given host and port, using default RMI
     */
    public static JMXConnector getJMXConnector(String hostName,
                                               int jmxPort, final String login,
                                               final String password) {
        ArgumentNotValid.checkNotNullOrEmpty(hostName, "String hostName");
        ArgumentNotValid.checkNotNegative(jmxPort, "int jmxPort");
        ArgumentNotValid.checkNotNullOrEmpty(login, "String login");
        ArgumentNotValid.checkNotNullOrEmpty(password, "String password");
        
        JMXServiceURL rmiurl = getUrl(hostName, jmxPort, -1);
        Map<String, ?> environment =
                (Map<String, ?>) packageCredentials(login, password);
        Throwable lastException;
        int retries = 0;
        do {
            try {
                return JMXConnectorFactory.connect(rmiurl, environment);
            } catch (IOException e) {
                lastException = e;
                if (retries < MAX_TRIES &&
                    e.getCause() != null
                    && e.getCause() instanceof ServiceUnavailableException) {
                    // Sleep a bit before trying again
                    TimeUtils.exponentialBackoffSleep(retries);
                    continue;
                }
                break;
            }
        } while (retries++ < MAX_TRIES);
        throw new IOFailure("Failed to connect to URL " + rmiurl + " after "
                            + retries + " attempts",
                            lastException);
    }

    /** Get a single CompositeData object out of a TabularData structure.
     *
     * @param items TabularData structure as returned from JMX calls
     * @return The one item in the items structure.
     * @throws ArgumentNotValid if there is not exactly one item in items,
     * or items is null
     */
    public static CompositeData getOneCompositeData(TabularData items) {
        ArgumentNotValid.checkNotNull(items, "TabularData items");
        ArgumentNotValid.checkTrue(items.size() == 1,
                                   "TabularData items should have 1 item");
        return (CompositeData) items.values().toArray()[0];
    }

    /** Execute a single command, closing the connector afterwards.  If you
     * wish to hold on to the connector, call
     * JMXUtils#executeCommand(MBeanServerConnection, String, String, String[])
     *
     * @param connector A one-shot connector object.
     * @param beanName The name of the bean to execute a command on.
     * @param command The command to execute.
     * @param arguments The arguments to the command (all strings)
     * @return Whatever the command returned.
     */
    public static Object executeCommand(JMXConnector connector, String beanName,
                                  String command, String... arguments) {
        ArgumentNotValid.checkNotNull(connector, "JMXConnector connector");
        ArgumentNotValid.checkNotNullOrEmpty(beanName, "String beanName");
        ArgumentNotValid.checkNotNullOrEmpty(command, "String command");
        ArgumentNotValid.checkNotNull(arguments, "String... arguments");

        MBeanServerConnection connection = null;
        try {
            connection = connector.getMBeanServerConnection();
        } catch (IOException e) {
            throw new IOFailure("Failure getting JMX connection", e);
        }
        try {
            return executeCommand(connection, beanName,
                                           command, arguments);
        } finally {
            try {
                connector.close();
            } catch (IOException e) {
                log.warn("Couldn't close connection to " + beanName, e);
            }
        }
    }

    /** Get the value of an attribute, closing the connector afterwards.  If you
     * wish to hold on to the connector, call
     * JMXUtils#executeCommand(MBeanServerConnection, String, String, String[])
     *
     * @param connector A one-shot connector object.
     * @param beanName The name of the bean to get an attribute from.
     * @param attribute The attribute to get.
     * @return Whatever the command returned.
     */
    public static Object getAttribute(JMXConnector connector, String beanName,
                                String attribute) {
        ArgumentNotValid.checkNotNull(connector, "JMXConnector connector");
        ArgumentNotValid.checkNotNullOrEmpty(beanName, "String beanName");
        ArgumentNotValid.checkNotNullOrEmpty(attribute, "String attribute");
        
        MBeanServerConnection connection = null;
        try {
            connection = connector.getMBeanServerConnection();
        } catch (IOException e) {
            throw new IOFailure("Failure getting JMX connection", e);
        }
        try {
            return getAttribute(beanName, attribute, connection);
        } finally {
            try {
                connector.close();
            } catch (IOException e) {
                log.warn("Couldn't close connection to " + beanName, e);
            }
        }
    }
}
