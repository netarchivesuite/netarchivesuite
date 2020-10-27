/*
 * #%L
 * Netarchivesuite - monitor - test
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

import javax.management.remote.JMXServiceURL;

import org.junit.Assert;
import org.junit.Test;

import com.sun.jndi.rmi.registry.RegistryContextFactory;

import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.JMXUtils;

public class JMXUtilsTester {

    private static final String JNDI_INITIAL_CONTEXT_PROPERTY = "java.naming.factory.initial";
    private String defaultServer = "localhost";

    /*
     * Test method for 'dk.netarkivet.monitor.webinterface.JMXUtils.getUrl(String, int, int)'
     */
    @Test
    public final void testGetUrl() {
        JMXServiceURL JmxServiceUrl = JMXUtils.getUrl(defaultServer, 8000, 8100);
        Assert.assertNotNull(JmxServiceUrl);
    }

    /*
     * Test method for 'dk.netarkivet.monitor.webinterface.JMXUtils.getConnection(JMXServiceURL, Map<String, String[]>)'
     */
    @Test
    public final void testGetConnection() {
        ensureJndiInitialContext();
        JMXServiceURL JmxServiceUrl = JMXUtils.getUrl(defaultServer, 8000, 8100);
        // JmxServiceUrl = JMXUtils.getUrl("kb-test-adm-001.kb.dk", 8000, 8100);
        try {
            JMXUtils.getMBeanServerConnection(JmxServiceUrl,
                    JMXUtils.packageCredentials("monitorRole", "monitorRolePassword"));
        } catch (IOFailure e) {
            // Expected
        }
    }

    /**
     * If no initial JNDI context has been configured, configures the system to use Sun's standard one. This is
     * necessary for RMI connections to work.
     */
    private static void ensureJndiInitialContext() {

        if (System.getProperty(JNDI_INITIAL_CONTEXT_PROPERTY) == null) {
            System.setProperty(JNDI_INITIAL_CONTEXT_PROPERTY, RegistryContextFactory.class.getCanonicalName());
            System.out.println("Set property '" + JNDI_INITIAL_CONTEXT_PROPERTY + "' to: "
                    + RegistryContextFactory.class.getCanonicalName());
        } else {
            System.out.println("Property '" + JNDI_INITIAL_CONTEXT_PROPERTY + "' is set to: "
                    + System.getProperty(JNDI_INITIAL_CONTEXT_PROPERTY));
        }
    }

}
