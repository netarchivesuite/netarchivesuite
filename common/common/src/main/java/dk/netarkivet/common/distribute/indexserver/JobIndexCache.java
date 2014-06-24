package dk.netarkivet.common.distribute.indexserver;

import java.util.Set;

/** An interface to a cache of data for jobs. */
public interface JobIndexCache {
    /** Get an index for the given list of job IDs.
     * The resulting file contains a suitably sorted list.
     * This method should always be safe for asynchronous calling.
     * This method may use a cached version of the file.
     *
     * @param jobIDs Set of job IDs to generate index for.
     * @return An index, consisting of a file and the set this is an index for.
     * This file must not be modified or deleted, since it is part of the cache
     * of data.
     */
    Index<Set<Long>> getIndex(Set<Long> jobIDs);

    /**
     * Request an index from the indexserver. Prepare the index but don't 
     * give it to me.
     * @param jobSet Set of job IDs to generate index for.
     * @param harvestId Harvestdefinition associated with this set of jobs
     */
    void requestIndex(Set<Long> jobSet, Long harvestId);
}
