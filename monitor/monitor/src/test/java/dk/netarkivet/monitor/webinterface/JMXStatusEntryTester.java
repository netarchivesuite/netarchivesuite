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
        properties.put(JMXSummaryUtils.JMXHarvestPriorityProperty, "HIGHPRIORITY");
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
        assertTrue(entry.getHarvestPriority().equals("HIGHPRIORITY"));
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
