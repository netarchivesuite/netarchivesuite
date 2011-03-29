/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
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
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301 
 *  USA
 */

package dk.netarkivet.archive.indexserver;

import java.io.File;
import java.util.Set;

import dk.netarkivet.common.utils.FileUtils;

/**
 * Implementation of file based cache, that works with the assumption we are
 * working on a set if ids, of which we might only get a subset correct.
 *
 * Implements generating a filename from this.
 *
 * @param <T> The cache type, must extend java.lang.Comparable.
 */
public abstract class MultiFileBasedCache<T extends Comparable<T>>
        extends FileBasedCache<Set<T>> {

    /**
     * Creates a new FileBasedCache object.  This creates a directory under the
     * main cache directory holding cached files.
     *
     * @param cacheName Name of this cache (enabling sharing among processes).
     *                  The directoriy creating in the cachedir will have this
     *                  name.
     */
    public MultiFileBasedCache(String cacheName) {
        super(cacheName);
    }

    /** Get the filename for the file containing the combined data for a
     * set of IDs.
     *
     * @param ids A set of IDs to generate a filename for
     * @return A filename that uniquely identifies this set of IDs within
     * the cache.  It is considered acceptable to have collisions at a
     * likelihood the order of 1/2^128 (i.e. use MD5 to abbreviate long lists).
     */
    public File getCacheFile(Set<T> ids) {
        String fileName = FileUtils.generateFileNameFromSet(ids, "-cache");
        return new File(getCacheDir(), fileName);
    }
}
