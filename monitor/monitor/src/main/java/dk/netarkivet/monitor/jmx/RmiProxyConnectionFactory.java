/* $Id$
* $Revision$
* $Date$
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

package dk.netarkivet.monitor.jmx;

import javax.management.MBeanServerConnection;
import javax.management.MBeanServerInvocationHandler;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.JMXUtils;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.monitor.MonitorSettings;


/** Creates RMI-based JMX connections to remote servers. */
public class RmiProxyConnectionFactory implements
                                       JMXProxyConnectionFactory {
    /**
     * Returns a JMXProxyFactory for a specific server, jmxport, rmiport,
     * username, and password. Makes sure that an initial context for JNDI has
     * been specified. Then constructs a RMI-based JMXServiceUrl using the
     * server and port. Finally connects to the URL using the name and
     * password.
     *
     * @param server   the given remote server
     * @param jmxPort  the JMX port on that server
     * @param rmiPort  the RMI port on that server (dedicated to the above
     *                 jmxPort)
     * @param userName the userName for access to the MBeanserver on that
     *                 server
     * @param password the password for access to the MBeanserver on that
     *                 server
     *
     * @return a JMXProxyFactory with the above properties.
     */
    public JMXProxyConnection getConnection(String server, int jmxPort,
                                            int rmiPort, String userName,
                                            String password) {
        ArgumentNotValid.checkNotNullOrEmpty(server, "String server");
        ArgumentNotValid.checkNotNegative(jmxPort, "int jmxPort");
        ArgumentNotValid.checkNotNegative(rmiPort, "int rmiPort");
        ArgumentNotValid.checkNotNullOrEmpty(userName, "String userName");
        ArgumentNotValid.checkNotNullOrEmpty(password, "String password");
        return
                new MBeanServerProxyConnection(server, jmxPort, rmiPort,
                                               userName,
                                               password);
    }

    /**
     * A JMXProxyFactory that constructs proxies by forwarding method calls
     * through an MBeanServerConnection.
     */
    private static class MBeanServerProxyConnection
            implements JMXProxyConnection {
        /** The connection to use for method call forwarding. */
        private MBeanServerConnection connection;
        /** Whether we are currently in the process of connecting. */
        private final AtomicBoolean connecting = new AtomicBoolean(false);
        /** The given remote server. */
        private String server;
        /** The JMX port on the server. */
        private int jmxPort;
        /** The RMI call back port on the server. */
        private int rmiPort;
        /** The JMX username on the server. */
        private String userName;
        /** The JMX password on the server. */
        private String password;
        /** The class logger. */
        private static Log log
                = LogFactory.getLog(MBeanServerProxyConnection.class);
        /** How long to wait for the proxied JMX connection in milliseconds. */
        private static final long JMX_TIMEOUT
                = Settings.getLong(MonitorSettings.JMX_PROXY_TIMEOUT);

        /**
         * Proxies an MBean connection with the given parameters.
         *
         * @param server   the given remote server
         * @param jmxPort  the JMX port on that server
         * @param rmiPort  the RMI port on that server (dedicated to the above
         *                 jmxPort)
         * @param userName the userName for access to the MBeanserver on that
         *                 server
         * @param password the password for access to the MBeanserver on that
         *                 server
         */
        public MBeanServerProxyConnection(final String server,
                                          final int jmxPort,
                                          final int rmiPort,
                                          final String userName,
                                          final String password) {
            this.server = server;
            this.jmxPort = jmxPort;
            this.rmiPort = rmiPort;
            this.userName = userName;
            this.password = password;
            connect();
        }

        /**
         * Initialise a thread to connect to the remote server. This method does
         * not wait for the connection to finish, so there is no guarantee that
         * the connection is initialised at the end of this method. Ensures that
         * we only do one connect() operation at a time.
         */
        private void connect() {
            new Thread() {
                public void run() {
                    if (connection == null
                        && connecting.compareAndSet(false, true)) {
                        try {
                            connection = JMXUtils.getMBeanServerConnection(
                                    server, jmxPort, rmiPort, userName,
                                    password);
                            log.info("Connected to remote JMX"
                                     + " server '" + server + "', port '"
                                     + jmxPort + "', rmiPort '" + rmiPort
                                     + "', user '" + userName + "'");
                        } catch (Exception e) {
                            log.warn("Unable to connect to remote JMX"
                                     + " server '" + server + "', port '"
                                     + jmxPort + "', rmiPort '" + rmiPort
                                     + "', user '" + userName + "'", e);
                        } finally {
                            connecting.set(false);
                            synchronized (connecting) {
                                connecting.notifyAll();
                            }
                        }
                    }
                }
            }.start();
        }

        /** Sleep until the timeout has occured, or connection is succesful. */
        private void waitForConnection() {
            long timeouttime = System.currentTimeMillis() + JMX_TIMEOUT;
            while (connecting.get()
                   && (timeouttime - System.currentTimeMillis() > 0)) {
                try {
                    synchronized (connecting) {
                        connecting.wait(Math.max(
                                timeouttime - System.currentTimeMillis(), 1));
                    }
                } catch (InterruptedException e) {
                    //Just ignore it
                }
            }
        }

        /**
         * Return object names from remote location.
         *
         * @param query The query remote mbeans should match
         *
         * @return set of names of matching mbeans.
         *
         * @throws IOFailure        on communication trouble.
         * @throws ArgumentNotValid on null or empty query.
         */
        public Set<ObjectName> query(String query) {
            ArgumentNotValid.checkNotNullOrEmpty(query, "String query");
            if (connection == null) {
                connect();
                waitForConnection();
            }
            if (connection == null) {
                throw new IOFailure(
                        "Could not get connection for query '" + query + "'");
            }
            try {
                return connection.queryNames(new ObjectName(query), null);
            } catch (IOException e) {
                throw new IOFailure("Unable to query for remote mbeans "
                                    + "matching '" + query + "'", e);
            } catch (MalformedObjectNameException e) {
                throw new IOFailure(
                        "Couldn't construct the objectName with string"
                        + " argument:' " + query + "'.", e);
            }
        }

        /**
         * Returns true if this object still can return usable proxies.
         *
         * @return True if we can return usable proxies.  Otherwise, somebody
         *         may have to make a new instance of JMXProxyFactory to get new
         *         proxies.
         */
        public boolean isLive() {
            if (connection == null) {
                connect();
                waitForConnection();
                if (connection == null) {
                    return false;
                }
            }
            try {
                this.connection.getMBeanCount();
                return true;
            } catch (Exception e) {
                /* Catching the exception appears to be the only way to check
                 * that the connection is dead.
                 */
                return false;
            }
        }

        /**
         * Uses Java's built-in facilities for creating proxies to remote
         * MBeans. Does not support notifications.
         *
         * @param name The name of an MBean on the registered MBeanServerConnection
         * @param intf The interface that the returned proxy should implement.
         *
         * @return an object implementing T. This object forwards all method
         *         calls to the MBean registered under the given name on the
         *         MBeanServerConnection that we use.
         */
        public <T> T createProxy(ObjectName name, Class<T> intf) {
            ArgumentNotValid.checkNotNull(name, "ObjectName name");
            ArgumentNotValid.checkNotNull(intf, "Class<T> intf");
            return MBeanServerInvocationHandler.newProxyInstance(
                    connection,
                    name,
                    intf,
                    false); //Set true to enable notifications
        }
    }
}
