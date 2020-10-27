/*
 * #%L
 * Netarchivesuite - common
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

package dk.netarkivet.common.management;

import java.io.IOException;
import java.io.Serializable;
import java.lang.management.ManagementFactory;
import java.rmi.registry.LocateRegistry;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

import javax.management.MBeanServer;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.distribute.monitorregistry.MonitorRegistryClientFactory;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.utils.SystemUtils;

/**
 * Utility class that handles exposing the platform mbean server using rmi, and using specified ports and password
 * files. <br/>
 * <br/>
 * <p>
 * See http://java.sun.com/j2se/1.5.0/docs/guide/jmx/tutorial/security.html <br/>
 * <br/>
 * <p>
 * TODO This implementation is not robust and could be improved. <br/>
 * TODO For instance: - Singleton behaviour <br/>
 * TODO -Reuse of already created registry <br/>
 * TODO Usage of access rights (for read-only mbeans) (see reference above)
 */
public class MBeanConnectorCreator {

    private static final Logger log = LoggerFactory.getLogger(MBeanConnectorCreator.class);

    public static boolean isExposed = false;

    private static final String SERVICE_JMX_RMI_URL = "service:jmx:rmi://{0}:{1}/jndi/rmi://{0}:{2}/jmxrmi";

    private static final String ENVIRONMENT_PASSWORD_FILE_PROPERTY = "jmx.remote.x.password.file";

    /**
     * Registers an RMI connector to the local mbean server in a private RMI registry, under the name "jmxrmi". The port
     * for the registry is read from settings, and the RMI port used for exposing the connector is also read from
     * settings. Access to the mbean server is restricted by the rules set in the password file, likewise read from
     * settings.
     *
     * @throws IOFailure on trouble exposing the server.
     */
    public static synchronized void exposeJMXMBeanServer() {
        try {
            if (!isExposed) {
                int jmxPort = Settings.getInt(CommonSettings.JMX_PORT);
                int rmiPort = Settings.getInt(CommonSettings.JMX_RMI_PORT);
                String passwordFile = Settings.get(CommonSettings.JMX_PASSWORD_FILE);

                // Create a private registry for the exposing the JMX connector.
                LocateRegistry.createRegistry(jmxPort);
                // Create a URL that signifies that we wish to use the local
                // registry created above, and listen for rmi callbacks on the
                // RMI port of this machine, exposing the mbeanserver with the
                // name "jmxrmi".
                String canonicalHostName = SystemUtils.getLocalHostName();
                JMXServiceURL url = new JMXServiceURL(MessageFormat.format(SERVICE_JMX_RMI_URL, canonicalHostName,
                        Integer.toString(rmiPort), Integer.toString(jmxPort)));
                // Insert the password file into environment used when creating
                // the connector server.
                Map<String, Serializable> env = new HashMap<String, Serializable>();
                env.put(ENVIRONMENT_PASSWORD_FILE_PROPERTY, passwordFile);
                // Register the connector to the local mbean server in this
                // registry under that URL, using the created environment
                // settings.
                MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
                JMXConnectorServer cs = JMXConnectorServerFactory.newJMXConnectorServer(url, env, mbs);
                // Start the connector server.
                cs.start();
                isExposed = true;
                // Register the JMX server at the registry.
                MonitorRegistryClientFactory.getInstance().register(canonicalHostName,
                        Settings.getInt(CommonSettings.JMX_PORT), Settings.getInt(CommonSettings.JMX_RMI_PORT));

                if (log.isInfoEnabled()) {
                    log.info("Registered mbean server in registry on port {} communicating on port {} "
                            + "using password file '{}'." + "\nService URL is {}", jmxPort, rmiPort, passwordFile,
                            url.toString());
                }
            }
        } catch (IOException e) {
            throw new IOFailure("Error creating and registering an RMIConnector to the platform mbean server.", e);
        }
    }

}
