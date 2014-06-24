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
            log.trace("Updated time for '" + hostEntry.getName() + "' port "
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
