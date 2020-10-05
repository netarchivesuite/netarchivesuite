/*
 * #%L
 * Netarchivesuite - common
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
package dk.netarkivet.common.distribute.indexserver;

import java.util.Set;

/**
 * An interface to a cache of data for jobs.
 */
public interface JobIndexCache {

    /**
     * Get an index for the given list of job IDs. The resulting file contains a suitably sorted list. This method
     * should always be safe for asynchronous calling. This method may use a cached version of the file.
     *
     * @param jobIDs Set of job IDs to generate index for.
     * @return An index, consisting of a file and the set this is an index for. This file must not be modified or
     * deleted, since it is part of the cache of data.
     */
    Index<Set<Long>> getIndex(Set<Long> jobIDs);

    /**
     * Request an index from the indexserver. Prepare the index but don't give it to me.
     *
     * @param jobSet Set of job IDs to generate index for.
     * @param harvestId Harvestdefinition associated with this set of jobs
     */
    void requestIndex(Set<Long> jobSet, Long harvestId);

}
