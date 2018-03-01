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
package dk.netarkivet.monitor.registry.distribute;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.distribute.Channels;
import dk.netarkivet.common.distribute.JMSConnectionFactory;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.utils.CleanupIF;
import dk.netarkivet.monitor.distribute.MonitorMessageHandler;
import dk.netarkivet.monitor.registry.MonitorRegistry;

/**
 * The monitor registry server listens on JMS for hosts that wish to register themselves to the service. The registry
 * lists hosts that can be monitored with JMX.
 */
public class MonitorRegistryServer extends MonitorMessageHandler implements CleanupIF {

    private static MonitorRegistryServer instance;
    private static final Logger log = LoggerFactory.getLogger(MonitorRegistryServer.class);

    /**
     * Start listening for registry messages.
     */
    private MonitorRegistryServer() {
        JMSConnectionFactory.getInstance().setListener(Channels.getTheMonitorServer(), this);
        log.info("MonitorRegistryServer listening for messages on channel '{}'", Channels.getTheMonitorServer());
    }

    /**
     * Get the registry server singleton.
     *
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
        // FIXME These commands fail when shutting down properly. (kill $PID)
        // instead of kill -9 $PID. See NAS-1976
        // JMSConnectionFactory.getInstance().removeListener(
        // Channels.getTheMonitorServer(), this);
    }

}
