
package dk.netarkivet.monitor.webinterface;

import java.util.Locale;

/** An interface that specifies the information available in our JMX
 * log mbeans.
 */
public interface StatusEntry extends Comparable<StatusEntry> {
    /** 
     * Get the (physical) location this status entry is from, e.g. EAST.
     * @return the (physical) location this status entry is from, e.g. EAST */
    String getPhysicalLocation();

    /** 
     * Get the name of the host (machine) this status entry is from.
     * @return the name of the host (machine) this status entry is from. */
    String getMachineName();

    /** 
     * Get the HTTP port used by the application this status entry is from.
     * Used for HTTP and self-identification.
     * @return the HTTP port that the application that this status entry is from
     */
    String getHTTPPort();

    /** 
     * Get the name of the application that this status entry is from.
     * @return the name of the application that this status entry is from. */
    String getApplicationName();

    /** 
     * Get the instance id of the application that this status entry is from.
     * @return the instance id of the application that this status entry is from. */
    String getApplicationInstanceID();

    /** 
     * Get the priority of the harvest queue that this status entry is from.
     * @return the priority of the harvest queue that this status entry is from. */
    String getHarvestPriority();

    /** 
     * Get the replica id of the application that this status entry is represents.
     * @return the replica id of the application that this status entry is represents. */
    String getArchiveReplicaName();

    /** 
     * Get the index in the list of most recent log messages that this status
     * entry is from.
     * @return the index in the list of most recent log messages that this status
     * entry is from.
     */
    String getIndex();

    /** 
     * Get the actual message.
     * @param l the current locale (only used to translate errormessages)
     * @return the actual status message */
    String getLogMessage(Locale l);
}
