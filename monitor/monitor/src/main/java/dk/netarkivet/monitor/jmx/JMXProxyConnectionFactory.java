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



/**
 * Common interface for objects that supply JMX connections
 * to remote servers.
 *
 * This interface does not specify which protocol to use for
 * connection, nor whether previously created connections will
 * be cached for reuse.
 */
public interface JMXProxyConnectionFactory {
    /**
     * Establish a JMX connection to a remote server.
     * @param server The name of remote server to connect to.
     * @param port The port to connect to on the remote server.
     * @param rmiPort The RMI-port to use in this connection.
     * @param userName The user name to log in as.
     * @param password The password for the specified user.
     * @return a connection object that can be used for accessing
     * MBeans on the remote server.
     */
    public JMXProxyConnection getConnection(
            String server,
            int port,
            int rmiPort,
            String userName,
            String password);
}
