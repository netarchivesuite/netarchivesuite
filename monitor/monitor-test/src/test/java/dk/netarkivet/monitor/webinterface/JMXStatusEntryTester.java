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

package dk.netarkivet.monitor.webinterface;

import static org.junit.Assert.assertTrue;

import java.util.Hashtable;
import java.util.Locale;

import javax.management.ObjectName;

import org.junit.Before;
import org.junit.Test;

public class JMXStatusEntryTester {

    private Hashtable<String, String> properties;

    @Before
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

    @Test
    public void testConstructor() throws Exception {
        JMXStatusEntry entry = new JMXStatusEntry(ObjectName.getInstance("east", properties));
        assertTrue(entry.getApplicationName().equals("SH"));
        assertTrue(entry.getApplicationInstanceID().equals("XX"));
        assertTrue(entry.getMachineName().equals("machine"));
        assertTrue(entry.getHTTPPort().equals("8081"));
        assertTrue(entry.getIndex().equals("1"));
        assertTrue(entry.getPhysicalLocation().equals("EAST"));
        assertTrue(entry.getArchiveReplicaName().equals("ReplicaOne"));
        assertTrue(entry.getHarvestPriority().equals("FOCUSED"));
    }

    @Test
    public void testGetLogmessage() throws Exception {
        JMXStatusEntry entry = new JMXStatusEntry(ObjectName.getInstance("east", properties));
        Locale l = new Locale("da");
        System.out.println(entry.getLogMessage(l));
    }
}
