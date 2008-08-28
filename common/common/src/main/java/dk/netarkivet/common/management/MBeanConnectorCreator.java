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

package dk.netarkivet.common.management;

import javax.management.MBeanServer;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;
import java.io.IOException;
import java.io.Serializable;
import java.lang.management.ManagementFactory;
import java.rmi.registry.LocateRegistry;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.distribute.monitorregistry.MonitorRegistryClientFactory;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.utils.SystemUtils;

/**
 * Utility class that handles exposing the platform mbean server using rmi, and
 * using specified ports and password files.
 *
 * See http://java.sun.com/j2se/1.5.0/docs/guide/jmx/tutorial/security.html
 *
 * TODO This implementation is not robust and could be improved.
 * TODO For instance: - Singleton behaviour
 * TODO                -Reuse of already created registry
 * TODO Usage of access rights (for read-only mbeans) (see reference above)
 *
 */

public class MBeanConnectorCreator {
    public static boolean isExposed = false;
    private static final String SERVICE_JMX_RMI_URL
            = "service:jmx:rmi://localhost:%d/jndi/rmi://localhost:%d/jmxrmi";
    private static final String ENVIRONMENT_PASSWORD_FILE_PROPERTY
            = "jmx.remote.x.password.file";
    private static final Log log = LogFactory.getLog(
            MBeanConnectorCreator.class);

    /**
     * Registers an RMI connector to the local mbean server in a private RMI
     * registry, under the name "jmxrmi". The port for the registry is read from
     * settings, and the RMI port used for exposing the connector is also read
     * from settings. Access to the mbean server is restricted by the rules set
     * in the password file, likewise read from settings.
     *
     * @throws IOFailure on trouble exposing the server.
     */
    public static synchronized void exposeJMXMBeanServer() {
        try {
            if (!isExposed) {
                int jmxPort = Settings.getInt(CommonSettings.JMX_PORT);
                int rmiPort = Settings.getInt(CommonSettings.JMX_RMI_PORT);
                String passwordFile = Settings.get(
                        CommonSettings.JMX_PASSWORD_FILE);
                log.info("Registering mbean server in registry on port "
                         + jmxPort + " communicating on port " + rmiPort
                         + " using password file '" + passwordFile + "'");

                //Create a private registry for the exposing the JMX connector
                LocateRegistry.createRegistry(jmxPort);
                // Create a URL that signifies that we wish to use the local
                // registry created above, and listen for rmi callbacks on the
                // RMI port of this machine, exposing the mbeanserver with the
                // name "jmxrmi".
                JMXServiceURL url = new JMXServiceURL(
                        String.format(SERVICE_JMX_RMI_URL,
                                      rmiPort,
                                      jmxPort));
                // Insert the password file into environment used when creating
                // the connector server.
                Map<String, Serializable> env
                        = new HashMap<String, Serializable>();
                env.put(ENVIRONMENT_PASSWORD_FILE_PROPERTY,
                        passwordFile);
                // Register the connector to the local mbean server in this
                // registry under that URL, using the created environment
                // settings.
                MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
                JMXConnectorServer cs
                        = JMXConnectorServerFactory.newJMXConnectorServer(
                        url, env, mbs);
                // Start the connector server.
                cs.start();
                isExposed = true;
                //Register the JMX server at the registry.
                MonitorRegistryClientFactory.getInstance().register(
                        SystemUtils.getLocalHostName(),
                        Settings.getInt(CommonSettings.JMX_PORT),
                        Settings.getInt(CommonSettings.JMX_RMI_PORT));
            }
        } catch (IOException e) {
            throw new IOFailure("Error creating and registering an"
                                + " RMIConnector to the platform mbean server.",
                                e);
        }
    }
}
