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

import javax.management.ObjectName;
import java.util.Set;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;

/**
 * JMX interface for connection objects that can be used for accessing
 * MBeans on remote servers. Connection method and policies are
 * implementation-dependent.
 */
public interface JMXProxyConnection {
    /**
     * Method to create a proxy to a given MBean on some remote server.
     * Example use:
     *
     * SingleLogRecord logMsg = (SingleLogRecord)
     *     myJMXProxyFactory.createProxy(myObjectName,SingleLogRecord.class);
     *
     * @param name The name of an MBean on some remote server.
     * @param intf The interface that the returned proxy should implement.
     * @param <T>
     * @return an object implementing T. This object forwards all method calls
     * to the named MBean.
     */
    public <T> T createProxy(ObjectName name, Class<T> intf);
    /**
     * Get the set of ObjectNames from the remote MBeanserver, that matches
     * the given query.
     * @param query the given query
     * @return the set of ObjectNames, that matches the given query.
     * @throws IOFailure on communication trouble.
     * @throws ArgumentNotValid on null or empty query.
     */
    public Set<ObjectName> query(String query);

    /** Returns true if this object still can return usable proxies.
     *
     * @return True if we can return usable proxies.  Otherwise, somebody
     * may have to make a new instance of JMXProxyFactory to get new proxies.
     */
    public boolean isLive();
}
