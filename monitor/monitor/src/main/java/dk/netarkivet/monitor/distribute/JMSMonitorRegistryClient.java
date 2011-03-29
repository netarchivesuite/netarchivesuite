/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
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
package dk.netarkivet.monitor.distribute;

import java.util.Timer;
import java.util.TimerTask;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.distribute.JMSConnectionFactory;
import dk.netarkivet.common.distribute.monitorregistry.MonitorRegistryClient;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.NetarkivetException;
import dk.netarkivet.common.utils.CleanupHook;
import dk.netarkivet.common.utils.CleanupIF;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.monitor.MonitorSettings;
import dk.netarkivet.monitor.registry.distribute.RegisterHostMessage;

/**
 * The monitor registry client sends messages with JMS to register the host
 * for JMX monitoring.
 */
public class JMSMonitorRegistryClient implements MonitorRegistryClient,
                                                 CleanupIF {
    /** The singleton instance of this class. */
    private static JMSMonitorRegistryClient instance;
    /** The logger for this class. */
    private final Log log = LogFactory.getLog(getClass());
    /** The cleanup hook that will clean up this client on VM shutdown. */
    private CleanupHook hook;
    /** The timer that sends messages. */
    private Timer registryTimer;
    /** One minute in milliseconds.
     * Used for control of timer task that sends messages. */
    private static final long MINUTE_IN_MILLISECONDS = 60000L;
    /** Zero milliseconds from now.
     * Used for control of timer task that sends messages. */
    private static final long NOW = 0L;

    /**
     * Intialises the client.
     */
    private JMSMonitorRegistryClient() {
        hook = new CleanupHook(this);
        Runtime.getRuntime().addShutdownHook(hook);
    }

    /** Get the registry client singleton.
     * @return The registry client.
     */
    public static JMSMonitorRegistryClient getInstance() {
        if (instance == null) {
            instance = new JMSMonitorRegistryClient();
        }
        return instance;
    }

    /** Register this host for monitoring.
     * Once this method is called it will reregister for monitoring every
     * minute, to ensure the scheduling is done.
     * If called again, it will restart the timer that registers the host.
     * @param localHostName The name of the host.
     * @param jmxPort The port for JMX connections to the host.
     * @param rmiPort The port for RMI connections for JMX communication.
     * @throws ArgumentNotValid on null or empty hostname, or negative port
     * numbers.
     */
    public synchronized void register(final String localHostName,
                                      final int jmxPort,
                                      final int rmiPort) {
        ArgumentNotValid.checkNotNullOrEmpty(localHostName,
                                             "String localHostName");
        ArgumentNotValid.checkNotNegative(jmxPort, "int jmxPort");
        ArgumentNotValid.checkNotNegative(rmiPort, "int rmiPort");
        if (registryTimer != null) {
            registryTimer.cancel();
        }
        registryTimer = new Timer("Monitor-registry-client", true);
        TimerTask timerTask = new TimerTask() {
            /** The action to be performed by this timer task. */
            public void run() {
                JMSConnectionFactory.getInstance().send(
                        new RegisterHostMessage(localHostName,
                                                jmxPort,
                                                rmiPort)
                );
                log.trace("Registering this client for monitoring,"
                          + " using hostname '" + localHostName
                          + "' and JMX/RMI ports "
                          + jmxPort + "/"
                          + rmiPort);
            }
        };
        
        long reregister_delay = Settings.getLong(MonitorSettings.DEFAULT_REREGISTER_DELAY);
        try {
            reregister_delay = Long.parseLong(Settings.get(
                    CommonSettings.MONITOR_REGISTRY_CLIENT_REREGISTERDELAY));
        }
        catch (NumberFormatException e1) {
            log.warn("Couldn't parse setting " 
                     + CommonSettings.MONITOR_REGISTRY_CLIENT_REREGISTERDELAY
                     + ". Only numbers are allowed. Using defaultvalue "
                     + MonitorSettings.DEFAULT_REREGISTER_DELAY);
        }
        catch(NetarkivetException e2) {
            log.warn("Couldn't find setting " 
                    + CommonSettings.MONITOR_REGISTRY_CLIENT_REREGISTERDELAY
                    + ". Using defaultvalue "
                    + MonitorSettings.DEFAULT_REREGISTER_DELAY);
        }

        log.info("Registering this client for monitoring every "
                 + reregister_delay
                 + " minutes, using hostname '"
                 + localHostName + "' and JMX/RMI ports "
                 + jmxPort + "/"
                 + rmiPort);
        registryTimer.scheduleAtFixedRate(timerTask, NOW,
                                            reregister_delay
                                          * MINUTE_IN_MILLISECONDS);
    }


    /**
     * Used to clean up a class from within a shutdown hook. Must not do any
     * logging. Program defensively, please.
     */
    public synchronized void cleanup() {
        if (registryTimer != null) {
            registryTimer.cancel();
            registryTimer = null;
        }
        try {
            Runtime.getRuntime().removeShutdownHook(hook);
        } catch (IllegalStateException e) {
            //Okay, it just means we are already shutting down.
        }
        hook = null;
    }
}