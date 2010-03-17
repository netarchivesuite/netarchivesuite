/* $Id$
 * $Revision$
 * $Date$
 * $Author$
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

}
