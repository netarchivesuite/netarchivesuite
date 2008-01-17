/* $Id$
* $Revision$
* $Date$
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

package dk.netarkivet.monitor.jmx;

import javax.management.MBeanServerConnection;
import javax.management.MBeanServerInvocationHandler;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.io.IOException;
import java.util.Set;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.jmx.JMXUtils;


/**
 * Creates RMI-based JMX connections to remote servers.
 */
public class RmiProxyConnectionFactory implements
                                       JMXProxyConnectionFactory {
    /**
     * Returns a JMXProxyFactory for a specific server, jmxport,
     * rmiport, username, and password.
     * Makes sure that an initial context for JNDI has been specified.
     * Then constructs a RMI-based JMXServiceUrl using the server and port.
     * Finally connects to the URL using the name and password.
     * @param server the given remote server
     * @param jmxPort the JMX port on that server
     * @param rmiPort the RMI port on that server
     *  (dedicated to the above jmxPort)
     * @param userName the userName for access
     *  to the MBeanserver on that server
     * @param password the password for access
     *  to the MBeanserver on that server
     * @return a JMXProxyFactory with the above properties.
     */
    public JMXProxyConnection getConnection(String server, int jmxPort,
            int rmiPort,
            String userName, String password) {
        ArgumentNotValid.checkNotNullOrEmpty(server, "String server");
        ArgumentNotValid.checkNotNegative(jmxPort, "int jmxPort");
        ArgumentNotValid.checkNotNegative(rmiPort, "int rmiPort");
        ArgumentNotValid.checkNotNullOrEmpty(userName, "String userName");
        ArgumentNotValid.checkNotNullOrEmpty(password, "String password");
        return
            new MBeanServerProxyConnection(
                JMXUtils.getMBeanServerConnection(server, jmxPort, rmiPort,
                        userName, password));
        }

    /**
     * A JMXProxyFactory that constructs proxies by forwarding
     * method calls through an MBeanServerConnection.
     */
    private static class MBeanServerProxyConnection
            implements JMXProxyConnection {
        /** The connection to use for method call forwarding. */
        private MBeanServerConnection connection;

        /**
         * Registers the given MBeanServerConnection.
         * @param connection The connection to use for method call forwarding
         */
        public MBeanServerProxyConnection(
                MBeanServerConnection connection) {
            this.connection = connection;
        }

        /** Return object names from remote location
         *
         * @param query The query remote mbeans should match
         * @return set of names of matching mbeans.
         * @throws IOFailure on communication trouble.
         * @throws ArgumentNotValid on null or empty query.
         */
        public Set<ObjectName> query(String query) {
            ArgumentNotValid.checkNotNullOrEmpty(query, "String query");
            try {
                return (Set<ObjectName>) connection.queryNames(new ObjectName(query), null);
            } catch (IOException e) {
                throw new IOFailure("Unable to query for remote mbeans "
                                    + "matching '" + query + "'", e);
            } catch (MalformedObjectNameException e) {
                throw new IOFailure(
                        "Couldn't construct the objectName with string"
                        + " argument:' " + query + "'.", e);
            }
        }

        /** Returns true if this object still can return usable proxies.
         *
         * @return True if we can return usable proxies.  Otherwise, somebody
         * may have to make a new instance of JMXProxyFactory to get new proxies.
         */
        public boolean isLive() {
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
         * Uses Java's built-in facilities for creating proxies
         * to remote MBeans. Does not support notifications.
         * @param name The name of an MBean on the registered
         *  MBeanServerConnection
         * @param intf The interface that the returned proxy should implement.
         * @return an object implementing T. This object forwards all method
         * calls to the MBean registered under the given name on
         * the MBeanServerConnection that we use.
         */
        public <T> T createProxy(ObjectName name, Class<T> intf) {
            ArgumentNotValid.checkNotNull(name, "ObjectName name");
            ArgumentNotValid.checkNotNull(intf, "Class<T> intf");
            return (T) MBeanServerInvocationHandler.newProxyInstance(
                    connection,
                    name,
                    intf,
                    false); //Set true to enable notifications
        }
    }
}
