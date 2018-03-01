/*
 * #%L
 * Netarchivesuite - monitor
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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.distribute.monitorregistry.HostEntry;
import dk.netarkivet.common.exceptions.ArgumentNotValid;

/**
 * A registry of known JMX URLs. This class is coded to be thread safe.
 */
public class MonitorRegistry {

    /** A map from host names to known host entries. */
    private Map<String, Set<HostEntry>> hostEntries = Collections
            .synchronizedMap(new HashMap<String, Set<HostEntry>>());
    /** The singleton instance. */
    private static MonitorRegistry instance;
    /** The logger for this class. */
    private static final Logger log = LoggerFactory.getLogger(MonitorRegistry.class);

    /**
     * Get the singleton instance.
     *
     * @return The singleton instance.
     */
    public static synchronized MonitorRegistry getInstance() {
        if (instance == null) {
            instance = new MonitorRegistry();
        }
        return instance;
    }

    /**
     * Register or re-register a JMX host entry.
     *
     * @param hostEntry The entry to add
     * @throws ArgumentNotValid if hostEntry is null.
     */
    public synchronized void register(HostEntry hostEntry) {
        ArgumentNotValid.checkNotNull(hostEntry, "HostEntry hostEntry");
        Set<HostEntry> set = hostEntries.get(hostEntry.getName());
        if (set == null) {
            set = Collections.synchronizedSet(new HashSet<HostEntry>());
            hostEntries.put(hostEntry.getName(), set);
        }
        
        if (set.add(hostEntry)) { 
            // An hostEntry did not previously exist with tuple<name,jmxport, jmxport>=<hostEntry.getName(), hostEntry.getJmxPort(), hostEntry.getRmiPort()>
            log.info("Added host '{}' port {}/{}", hostEntry.getName(), hostEntry.getJmxPort(), hostEntry.getRmiPort());
        } else { 
            // remove and add the entry with updated timestamp.
            set.remove(hostEntry);
            set.add(hostEntry);
            log.trace("Updated time for '{}' port {}/{} to {}", hostEntry.getName(), hostEntry.getJmxPort(),
                    hostEntry.getRmiPort(), hostEntry.getTime());
        }
    }

    /**
     * Get all JMX host entries.
     *
     * @return All JMX host entries.
     */
    public synchronized Map<String, Set<HostEntry>> getHostEntries() {
        return new HashMap<String, Set<HostEntry>>((hostEntries));
    }

}
