/*
 * #%L
 * Netarchivesuite - harvester
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

package dk.netarkivet.harvester.indexserver;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.distribute.indexserver.Index;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.Settings;

/**
 * A generic cache that stores items in files. This abstract superclass handles placement of the cache directory and
 * adding/getting files using the subclasses' methods for generating filenames.
 *
 * @param <T> The type of cache.
 */
public abstract class FileBasedCache<T> {

    /** Logger. */
    private static final Logger log = LoggerFactory.getLogger(FileBasedCache.class);

    /** Cache directory. */
    protected File cacheDir;

    /**
     * Creates a new FileBasedCache object. This creates a directory under the main cache directory holding cached
     * files.
     *
     * @param cacheName Name of this cache (enabling sharing among processes). The directory created in the cachedir
     * will have this name.
     */
    public FileBasedCache(String cacheName) {
        ArgumentNotValid.checkNotNullOrEmpty(cacheName, "cacheName");
        this.cacheDir = new File(new File(Settings.get(CommonSettings.CACHE_DIR)), cacheName).getAbsoluteFile();
        log.info("Metadata cache for '{}' uses directory '{}'", cacheName, getCacheDir().getAbsolutePath());
        FileUtils.createDir(getCacheDir());
    }

    /**
     * Get the directory that the files are cached in. Subclasses should override this to create their own directory
     * with this directory. The full directory structure will be created if required in the constructor.
     *
     * @return A directory that cache files can reside in.
     */
    public File getCacheDir() {
        return cacheDir;
    }

    /**
     * Get the file that caches content for the given ID.
     *
     * @param id Some sort of id that uniquely identifies the item within the cache.
     * @return A file (possibly nonexistant or empty) that can cache the data for the id.
     */
    public abstract File getCacheFile(T id);

    /**
     * Fill in actual data in the file in the cache. This is the workhorse method that is allowed to modify the cache.
     * When this method is called, the cache can assume that getCacheFile(id) does not exist.
     *
     * @param id Some identifier for the item to be cached.
     * @return An id of content actually available. In most cases, this will be the same as id, but for complex I it
     * could be a subset (or null if the type argument I is a simple type). If the return value is not the same as id,
     * the file will not contain cached data, and may not even exist.
     */
    protected abstract T cacheData(T id);

    /**
     * Ensure that a file containing the appropriate content exists for the ID. If the content cannot be found, this
     * method may return null (if I is a simple type) or an appropriate subset (if I is, say, a Set) indicating the data
     * that is actually available. In the latter case, calling cache on the returned set should always fill the file for
     * that subset (barring catastrophic failure).
     * <p>
     * Locking: If the file is not immediately found, we enter a file-creation state. To avoid corrupted data, we must
     * ensure that only one cache instance, and only one thread within any instance, creates the file. Thus as long as
     * somebody else seems to be creating the file, we wait and see if they finish. This is checked by having an
     * exclusive lock on a ".working" file (we cannot use the result file, as it has to be created to be locked, and we
     * may end up with a different cached file than we thought, see above). The .working file itself is irrelevant, only
     * the lock on it matters.
     *
     * @param id Some sort of id that uniquely identifies the item within the cache.
     * @return The id given if it was successfully fetched, otherwise null if the type parameter I does not allow
     * subsets, or a subset of id if it does. This subset should be immediately cacheable.
     */
    public T cache(T id) {
        ArgumentNotValid.checkNotNull(id, "id");
        File cachedFile = getCacheFile(id);
        try {
            File fileBehindLockFile = new File(cachedFile.getAbsolutePath() + ".working");
            FileOutputStream lockFile = new FileOutputStream(fileBehindLockFile);
            FileLock lock = null;
            // Make sure no other thread tries to create this
            // FIXME welcome to a memory leak, intern strings are never freed from memory again!
            log.debug("Waiting to enter synchronization on {}", fileBehindLockFile.getAbsolutePath().intern());
            // FIXME Potential memory leak. intern() remembers all strings until JVM exits.
            synchronized (fileBehindLockFile.getAbsolutePath().intern()) {
                try {
                    // Make sure no other process tries to create this.
                    log.debug("locking filechannel for file '{}' (thread = {})", fileBehindLockFile.getAbsolutePath(),
                            Thread.currentThread().getName());
                    try {
                        lock = lockFile.getChannel().lock();
                    } catch (OverlappingFileLockException e) {
                        // Exception is logged below
                        throw new IOException(e.getMessage(), e);
                    }
                    // Now we know nobody else touches the file.
                    // If the file already exists, just return it.
                    if (cachedFile.exists()) {
                        return id;
                    }
                    return cacheData(id);
                } finally {
                    if (lock != null) {
                        log.debug("release lock on filechannel {}", lockFile.getChannel());
                        lock.release();
                    }
                    lockFile.close();
                }
            }
        } catch (IOException e) {
            String errMsg = "Error obtaining lock for file '" + cachedFile.getAbsolutePath() + "'.";
            log.warn(errMsg, e);
            throw new IOFailure(errMsg, e);
        }
    }

    /**
     * Utility method to get a number of cache entries at a time. Implementations of FileBasedCache may override this to
     * perform the caching more efficiently, if caching overhead per file is large.
     *
     * @param ids List of IDs that uniquely identify a set of items within the cache.
     * @return A map from ID to the files containing cached data for those IDs. If caching failed, even partially, for
     * an ID, the entry for the ID doesn't exist.
     */
    public Map<T, File> get(Set<T> ids) {
        ArgumentNotValid.checkNotNull(ids, "Set<I> ids");
        Map<T, File> result = new HashMap<T, File>(ids.size());
        for (T id : ids) {
            if (id.equals(cache(id))) {
                result.put(id, getCacheFile(id));
            } else {
                result.put(id, null);
            }
        }
        return result;
    }

    /**
     * Forgiving index generating method, that returns a file with an index, of the greatest possible subset of a given
     * id, and the subset.
     * <p>
     * If the type I for instance is a Set, you may get an index of only a subset. If I is a File, null may be seen as a
     * subset.
     *
     * @param id The requested index.
     * @return An index over the greatest possible subset, and the subset.
     * @see #cache for more information.
     */
    public Index<T> getIndex(T id) {
        T response = id;
        T lastResponse = null;
        while (response != null && !response.equals(lastResponse)) {
            if (lastResponse != null) {
                log.info("Requested index of type '{}' data '{}' not available. Retrying with available subset '{}'",
                        this.getCacheDir().getName(), lastResponse, response);
            }
            lastResponse = response;
            response = cache(lastResponse);
        }
        File cacheFile = getCacheFile(response);
        log.info("Generated index '{}' of id '{}', request was for '{}'", cacheFile, response, id);
        return new Index<T>(cacheFile, response);
    }

}
