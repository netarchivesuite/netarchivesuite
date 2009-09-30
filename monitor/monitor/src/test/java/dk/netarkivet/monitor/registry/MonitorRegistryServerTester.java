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
package dk.netarkivet.monitor.registry;

import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;

import dk.netarkivet.common.distribute.JMSConnectionMockupMQ;
import dk.netarkivet.common.distribute.monitorregistry.HostEntry;
import dk.netarkivet.monitor.registry.distribute.MonitorRegistryServer;
import dk.netarkivet.monitor.registry.distribute.RegisterHostMessage;

public class MonitorRegistryServerTester extends TestCase {

    public void setUp() {
        JMSConnectionMockupMQ.useJMSConnectionMockupMQ();
    }
    
    public void testGetInstance() {
        MonitorRegistryServer server = null;
        try {
            server = MonitorRegistryServer.getInstance();
            assertFalse("server should not be null", server == null);
        } catch (Exception e) {
            fail("Getinstance should not try exception but did");
        } finally {
            if (server != null) {
                server.cleanup();
            }
        }
    }
    
    public void testVisit() {
        MonitorRegistryServer server = null;
        try {
            server = MonitorRegistryServer.getInstance();
            assertFalse("server should not be null", server == null);
        } catch (Exception e) {
            fail("Getinstance should not try exception but did");
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
