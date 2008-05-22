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
package dk.netarkivet.monitor.registry;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.common.distribute.monitorregistry.HostEntry;
import dk.netarkivet.common.exceptions.ArgumentNotValid;

/**
 * A registry of known JMX URLs.
 * This class is coded to be thread safe.
 */
public class MonitorRegistry {
    /** A map from host names to known host entries. */
    private Map<String, Set<HostEntry>> hostEntries
            = Collections.synchronizedMap(new HashMap<String,
            Set<HostEntry>>());
    /** The singleton instance. */
    private static MonitorRegistry instance;
    /** The logger for this class. */
    private Log log = LogFactory.getLog(getClass());

    /** Get the singleton instance.
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
     * Register a new JMX host entry.
     * @param hostEntry The entry to add
     *
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
            log.info("Added host '" + hostEntry.getName() + "' port "
                     + hostEntry.getJmxPort() + "/" + hostEntry.getRmiPort());
        } else {
            set.remove(hostEntry);
            set.add(hostEntry);
            log.debug("Updated time for '" + hostEntry.getName() + "' port "
                     + hostEntry.getJmxPort() + "/" + hostEntry.getRmiPort()
                     + " to " + hostEntry.getTime());
        }
    }

    /**
     * Get all JMX host entries.
     * @return All JMX host entries.
     */
    public synchronized Map<String, Set<HostEntry>> getHostEntries() {
        return new HashMap<String, Set<HostEntry>>((hostEntries));
    }
}
