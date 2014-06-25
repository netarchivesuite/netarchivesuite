package dk.netarkivet.monitor.registry;

import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;

import dk.netarkivet.common.distribute.monitorregistry.HostEntry;
import dk.netarkivet.monitor.registry.distribute.MonitorRegistryServer;
import dk.netarkivet.monitor.registry.distribute.RegisterHostMessage;

public class MonitorRegistryServerTester extends TestCase {

    public void setUp() {
      // Out commented to avoid reference to harvester module from monitor module.
      // JMSConnectionMockupMQ.useJMSConnectionMockupMQ();
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
