/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2009 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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
package dk.netarkivet.monitor.registry.distribute;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.common.distribute.Channels;
import dk.netarkivet.common.distribute.JMSConnectionFactory;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.utils.CleanupIF;
import dk.netarkivet.monitor.distribute.MonitorMessageHandler;
import dk.netarkivet.monitor.registry.MonitorRegistry;

/**
 * The monitor registry server listens on JMS for hosts that wish to register
 * themselves to the service. The registry lists hosts that can be monitored
 * with JMX.
 */
public class MonitorRegistryServer extends MonitorMessageHandler
        implements CleanupIF {
    private static MonitorRegistryServer instance;
    private final Log log = LogFactory.getLog(getClass());

    /**
     * Start listening for registry messages.
     */
    private MonitorRegistryServer() {
        JMSConnectionFactory.getInstance().setListener(
                Channels.getTheMonitorServer(), this);
        log.info("MonitorRegistryServer listening for messages on channel '"
                 + Channels.getTheMonitorServer() + "'");
    }

    /** Get the registry server singleton.
     * @return The registry server.
     */
    public static MonitorRegistryServer getInstance() {
        if (instance == null) {
            instance = new MonitorRegistryServer();
        }
        return instance;
    }

    /**
     * This method registers the sender as a host to be monitored with JMX.
     *
     * @throws ArgumentNotValid on null parameter. 
     */
    public void visit(RegisterHostMessage msg) {
        ArgumentNotValid.checkNotNull(msg, "RegisterHostMessage msg");
        MonitorRegistry.getInstance().register(msg.getHostEntry());
    }

    /** Remove listener on shutdown. */
    public void cleanup() {
        JMSConnectionFactory.getInstance().removeListener(
                Channels.getTheMonitorServer(), this);
    }
}
