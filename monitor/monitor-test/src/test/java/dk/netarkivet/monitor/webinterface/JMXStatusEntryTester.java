
package dk.netarkivet.monitor.webinterface;

import java.util.Hashtable;
import java.util.Locale;

import javax.management.ObjectName;

import junit.framework.TestCase;

public class JMXStatusEntryTester extends TestCase {
    
    private Hashtable<String,String> properties;
    
    public void setUp() {
        properties = new Hashtable<String, String>();         
        properties.put(JMXSummaryUtils.JMXPhysLocationProperty, "EAST");
        properties.put(JMXSummaryUtils.JMXMachineNameProperty, "machine");
        properties.put(JMXSummaryUtils.JMXApplicationNameProperty, "SH");
        properties.put(JMXSummaryUtils.JMXApplicationInstIdProperty, "XX");
        properties.put(JMXSummaryUtils.JMXHttpportProperty, "8081");
        properties.put(JMXSummaryUtils.JMXHarvestChannelProperty, "FOCUSED");
        properties.put(JMXSummaryUtils.JMXArchiveReplicaNameProperty, "ReplicaOne");
        properties.put(JMXSummaryUtils.JMXIndexProperty, "1");
    }
    
    public void testConstructor() throws Exception {                
        JMXStatusEntry entry = new JMXStatusEntry(
                ObjectName.getInstance("east", properties));
        assertTrue(entry.getApplicationName().equals("SH"));
        assertTrue(entry.getApplicationInstanceID().equals("XX"));
        assertTrue(entry.getMachineName().equals("machine"));
        assertTrue(entry.getHTTPPort().equals("8081"));
        assertTrue(entry.getIndex().equals("1"));
        assertTrue(entry.getPhysicalLocation().equals("EAST"));
        assertTrue(entry.getArchiveReplicaName().equals("ReplicaOne"));
        assertTrue(entry.getHarvestPriority().equals("FOCUSED"));
    }
    
    public void testGetLogmessage() throws Exception {
        JMXStatusEntry entry = new JMXStatusEntry(
                ObjectName.getInstance("east", properties));
        Locale l = new Locale("da");
        System.out.println(entry.getLogMessage(l));
    }
    
//    public void testQueryJMX() throws Exception {
//        JMXStatusEntry.mBeanServer.
//        JMXStatusEntry.queryJMX("nonsens");
//    }
}
