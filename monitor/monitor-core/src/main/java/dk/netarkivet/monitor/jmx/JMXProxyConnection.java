/*
 * #%L
 * Netarchivesuite - monitor
 * %%
 * Copyright (C) 2005 - 2018 The Royal Danish Library, 
 *             the National Library of France and the Austrian National Library.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 2.1 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */
package dk.netarkivet.monitor.jmx;

import java.util.Set;

import javax.management.ObjectName;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;

/**
 * JMX interface for connection objects that can be used for accessing MBeans on remote servers. Connection method and
 * policies are implementation-dependent.
 */
public interface JMXProxyConnection {
    /**
     * Method to create a proxy to a given MBean on some remote server. Example use:
     * <p>
     * SingleLogRecord logMsg = (SingleLogRecord) myJMXProxyFactory.createProxy(myObjectName,SingleLogRecord.class);
     *
     * @param name The name of an MBean on some remote server.
     * @param intf The interface that the returned proxy should implement.
     * @param <T>
     * @return an object implementing T. This object forwards all method calls to the named MBean.
     */
    <T> T createProxy(ObjectName name, Class<T> intf);

    /**
     * Get the set of ObjectNames from the remote MBeanserver, that matches the given query.
     *
     * @param query the given query
     * @return the set of ObjectNames, that matches the given query.
     * @throws IOFailure on communication trouble.
     * @throws ArgumentNotValid on null or empty query.
     */
    Set<ObjectName> query(String query);

    /**
     * Returns true if this object still can return usable proxies.
     *
     * @return True if we can return usable proxies. Otherwise, somebody may have to make a new instance of
     * JMXProxyFactory to get new proxies.
     */
    boolean isLive();
}
