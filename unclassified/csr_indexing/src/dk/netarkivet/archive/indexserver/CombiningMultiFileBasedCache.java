/* File: $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2009 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301 
 *  USA
 */

package dk.netarkivet.archive.indexserver;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import dk.netarkivet.common.utils.FileUtils;

/**
 * This class provides the framework for classes that cache the effort of
 * combining multiple files into one.  For instance, creating a Lucene index
 * out of crawl.log files takes O(nlogn) where n is the number of lines in
 * the files combined.
 *
 * It is based on an underlying cache of single files.
 * It handles the possibility of some of the files in the underlying cache
 * not being available by telling which files are available rather than by
 * sending an incomplete file.
 * 
 * @param <T> A comparable instance. Inheriting the 
 * java.lang.Comparable interface.
 */
public abstract class CombiningMultiFileBasedCache<T extends Comparable<T>>
        extends MultiFileBasedCache<T> {

    /** The raw data cache that this cache gets data from. */
    protected FileBasedCache<T> rawcache;

    /** Constructor for a CombiningMultiFileBasedCache.
     *
     * @param name The name of the cache
     * @param rawcache The underlying cache of single files.
     */
    protected CombiningMultiFileBasedCache(String name,
            FileBasedCache<T> rawcache) {
        super(name);
        this.rawcache = rawcache;
    }

    /** This is called when an appropriate file for the ids in question
     * has not been found.  It is expected to do the actual operations
     * necessary to get the data.  At the outset, the file for the given
     * IDs is expected to be not present.
     *
     * @param ids The set of identifiers for which we want the corresponding
     *            data
     * @return The set of IDs, or subset if data fetching failed for some IDs.
     * If some IDs failed, the file is not filled, though some data may be
     * cached at a lower level.
     */
    protected Set<T> cacheData(Set<T> ids) {
        Map<T, File> filesFound = prepareCombine(ids);
        File resultFile = getCacheFile(ids);
        if (filesFound.size() == ids.size()) {
            combine(filesFound);
        } else {
            FileUtils.remove(resultFile);
        }
        return filesFound.keySet();
    }

    /** Prepare needed data for performing combine().  This should ensure that
     * all data is ready to use, or else the ids where the data cannot be
     * obtained should be missing in the returned set.
     * @param ids Set of job IDs to get ready to combine
     * @return The map of ID->file of the data we will combine for each ID.
     * If subclasses override this method to ensure other data is present,
     * jobs with missing IDs should be removed from this map.
     */
    protected Map<T, File> prepareCombine(Set<T> ids) {
        Map<T, File> rawdata = rawcache.get(ids);
        // First figure out which files were found
        Map<T, File> filesFound = new HashMap<T, File>();
        for (Map.Entry<T, File> entry : rawdata.entrySet()) {
            if (entry.getValue() != null) {
                filesFound.put(entry.getKey(), entry.getValue());
            }
        }
        return filesFound;
    }

    /** Combine a set of files found in the raw data cache to form our
     * kind of file.
     *
     * @param filesFound The files that were found for the IDs in the raw
     * data cache.  The map must not contain any null values.
     */
    protected abstract void combine(Map<T, File> filesFound);
}
