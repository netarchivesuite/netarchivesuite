
package dk.netarkivet.harvester.indexserver;

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
