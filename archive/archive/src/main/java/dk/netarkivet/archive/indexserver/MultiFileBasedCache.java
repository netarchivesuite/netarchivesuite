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

package dk.netarkivet.archive.indexserver;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.utils.MD5;
import dk.netarkivet.common.utils.StringUtils;

/**
 * Implementation of file based cache, that works with the assumption we are
 * working on a set if ids, of which we might only get a subset correct.
 *
 * Implements generating a filename from this.
 *
 */

public abstract class MultiFileBasedCache<T extends Comparable>
        extends FileBasedCache<Set<T>> {
    /** Maximum number of IDs we will put in a filename.  Above this
     * number, a checksum of the ids is generated instead.  This is done
     * to protect us from getting filenames too long for the filesystem.
     */
    private static final int MAX_IDS_IN_FILENAME = 4;

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
     * @param IDs A set of IDs to generate a filename for
     * @return A filename that uniquely identifies this set of IDs within
     * the cache.  It is considered acceptable to have collisions at a
     * likelihood the order of 1/2^128 (i.e. use MD5 to abbreviate long lists).
     */
    public File getCacheFile(Set<T> IDs) {
        ArgumentNotValid.checkNotNull(IDs, "Set<T> IDs");

        List<T> sorted = new ArrayList<T>(IDs);
        Collections.sort(sorted);

        String allIDsString = StringUtils.conjoin("-",sorted );
        if (sorted.size() > MAX_IDS_IN_FILENAME) {
            String firstNIDs = StringUtils.conjoin("-",sorted.subList(0,
                                                                  MAX_IDS_IN_FILENAME) );
            return new File(getCacheDir(), firstNIDs + "-"
                                           + MD5.generateMD5(allIDsString.getBytes()) + "-cache");
        } else {
            return new File(getCacheDir(), allIDsString + "-cache");
        }
    }
}
