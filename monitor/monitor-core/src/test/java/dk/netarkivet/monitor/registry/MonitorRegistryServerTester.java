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
package dk.netarkivet.monitor.registry;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Map;
import java.util.Set;

import org.junit.Ignore;
import org.junit.Test;

import dk.netarkivet.common.distribute.JMSConnection;
import dk.netarkivet.common.distribute.JMSConnectionFactory;
import dk.netarkivet.common.distribute.monitorregistry.HostEntry;
import dk.netarkivet.common.utils.ExceptionUtils;
import dk.netarkivet.monitor.registry.distribute.MonitorRegistryServer;
import dk.netarkivet.monitor.registry.distribute.RegisterHostMessage;

public class MonitorRegistryServerTester {

    @Test
    @Ignore("FIXME - fails JMSConnection tries to connect to localhost:7676 - test still active in monitor-test module")
    public void testGetInstance() {
    	JMSConnection jc = JMSConnectionFactory.getInstance();
    	assertTrue(jc.getClass().getName().equals(
    			"dk.netarkivet.common.distribute.JMSConnectionMockupMQ"));
        MonitorRegistryServer server = null;
        try {
            server = MonitorRegistryServer.getInstance();
            assertFalse("server should not be null", server == null);
        } catch (Exception e) {
            fail("Getinstance should not throw exception but did");
        } finally {
            if (server != null) {
                server.cleanup();
            }
        }
    }

    @Test
    @Ignore("FIXME - fails JMSConnection tries to connect to localhost:7676 - test still active in monitor-test module")
    public void testVisit() {
    	JMSConnection jc = JMSConnectionFactory.getInstance();
    	assertTrue(jc.getClass().getName().equals(
    			"dk.netarkivet.common.distribute.JMSConnectionMockupMQ"));
    	JMSConnectionFactory.getInstance().cleanup();
        MonitorRegistryServer server = null;
        try {
            server = MonitorRegistryServer.getInstance();
            assertFalse("server should not be null", server == null);
        } catch (Exception e) {
            fail("Getinstance should not throw exception but did: " 
            		+ ExceptionUtils.getStackTrace(e));
        }
        RegisterHostMessage msg = new RegisterHostMessage("localhost", 8081, 8181);
        server.visit(msg);
        Map<String, Set<HostEntry>> map = MonitorRegistry.getInstance().getHostEntries();
        Set<HostEntry> set = map.get("localhost");
        assertTrue("Should contain hostEntry for localhost", set != null);
        assertTrue("Should only have one element", set.size() == 1);
        HostEntry localhostEntry = set.iterator().next();
        assertTrue(localhostEntry.getJmxPort() == 8081);
        assertTrue(localhostEntry.getRmiPort() == 8181);
        assertTrue(localhostEntry.getName() == "localhost");
        server.cleanup();
    }
}
