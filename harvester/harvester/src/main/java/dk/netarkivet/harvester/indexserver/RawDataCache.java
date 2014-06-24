
package dk.netarkivet.harvester.indexserver;

import java.io.File;
import java.util.Map;
import java.util.Set;

/**
 * An interface for getting raw data out of the bitarchives based on
 * job IDs.
 */
public interface RawDataCache {
    /** Cache the raw data associated with the given ID as a file, or null if
     * we could not find any such data.  The data can be found in the file
     * specified by getCacheFile(ID), if this call is successfull.
     *
     * @param id The job ID to look for data for.
     * @return The data found, or null on failure.
     */
    Long cache(Long id);

    /** Get the raw data files for a set of job IDs.  This is commonly a
     *  convenience wrapper around cache(Long) and getCacheFile(Long), but may
     *  reduce overhead.
     *
     * @param ids Set of job IDs to get data for.
     * @return Map of ID to file containing data for that ID, or to null
     * if data could not be found.
     */
    Map<Long, File> get(Set<Long> ids);
}
