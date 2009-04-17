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
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.distribute.indexserver.Index;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.Settings;

/**
 * A generic cache that stores items in files.  This abstract superclass
 * handles placement of the cache directory and adding/getting files using
 * the subclasses' methods for generating filenames.
 *
 */
public abstract class FileBasedCache<I> {
    /** Cache directory. */
    File cacheDir;
    /** Logger. */
    private Log log = LogFactory.getLog(getClass().getName());

    /** Creates a new FileBasedCache object.  This creates a directory under
     * the main cache directory holding cached files.
     *
     * @param cacheName Name of this cache (enabling sharing among processes).
     * The directoriy creating in the cachedir will have this name.
     */
    public FileBasedCache(String cacheName) {
        ArgumentNotValid.checkNotNullOrEmpty(cacheName, "cacheName");
        this.cacheDir = new File(new File(
                Settings.get(CommonSettings.CACHE_DIR)),
                cacheName).getAbsoluteFile();
        log.info("Metadata cache for '" + cacheName + "' uses directory '"
                 + getCacheDir().getAbsolutePath() + "'");
        FileUtils.createDir(getCacheDir());
    }

    /** Get the directory that the files are cached in.  Subclasses should
     * override this to create their own directory with this directory.  The
     * full directory structure will be created if required in the constructor.
     *
     * @return A directory that cache files can reside in.
     */
    public File getCacheDir() {
        return cacheDir;
    }

    /** Get the file that caches content for the given ID.
     *
     * @param id Some sort of id that uniquely identifies the item within the
     * cache.
     * @return A file (possibly nonexistant or empty) that can cache the data
     * for the id.
     */
    public abstract File getCacheFile(I id);

    /** Fill in actual data in the file in the cache.  This is the workhorse
     * method that is allowed to modify the cache.  When this method is called,
     * the cache can assume that getCacheFile(id) does not exist.
     *
     * @param id Some identifier for the item to be cached.
     * @return An id of content actually available.  In most cases, this will
     * be the same as id, but for complex I it could be a subset (or null if
     * the type argument I is a simple type).  If the return value is not the
     * same as id, the file will not contain cached data, and may not even
     * exist.
     */
    protected abstract I cacheData(I id);

    /** Ensure that a file containing the appropriate content exists for the ID.
     * If the content cannot be found, this method may return null (if I is
     * a simple type) or an appropriate subset (if I is, say, a Set) indicating
     * the data that is actually available.  In the latter case, calling cache
     * on the returned set should always fill the file for that subset (barring
     * catastrophic failure).
     *
     * Locking:  If the file is not immediately found, we enter a file-creation
     * state.  To avoid corrupted data, we must ensure that only one cache
     * instance, and only one thread within any instance, creates the file.
     * Thus as long as somebody else seems to be creating the file, we wait
     * and see if they finish.  This is checked by having an exclusive lock
     * on a ".working" file (we cannot use the result file, as it has to be
     * created to be locked, and we may end up with a different cached file
     * than we thought, see above).  The .working file itself is irrelevant,
     * only the lock on it matters.
     *
     * @param id Some sort of id that uniquely identifies the item within
     * the cache.
     * @return The id given if it was successfully fetched, otherwise null
     * if the type parameter I does not allow subsets, or a subset of id
     * if it does.  This subset should be immediately cacheable.
     * 
     * FIXME added method synchronization. Try to fix bug 1547
     */
    public I cache(I id) {
        ArgumentNotValid.checkNotNull(id, "id");
        File cachedFile = getCacheFile(id);
        if (cachedFile.exists()) {
            return id;
        } else {
            try {
        	File fileBehindLockFile 
        		= new File(cachedFile.getAbsolutePath() + ".working");
                FileOutputStream lockFile = new FileOutputStream(
                        fileBehindLockFile);
                FileLock lock = null;
                try {
                    // Make sure no other thread tries to create this
                    synchronized (fileBehindLockFile.getAbsolutePath().intern()) {
                	
                        // Make sure no other process tries to create this
                        log.debug("locking filechannel for file '"
                        	+ fileBehindLockFile.getAbsolutePath()
                        	+ "' (thread = "
                        	+ Thread.currentThread().getName() + ")");
                        try {
                            lock = lockFile.getChannel().lock();
                        } catch (OverlappingFileLockException e) {
                            log.warn(e);
                            throw new IOException(e);
                        }
                        // Now we know nobody else touches the file
                        // Just in case, check that the file wasn't created
                        // in the interim.  If it was, we can return it.
                        if (cachedFile.exists()) {
                            return id;
                        }
                        
                        return cacheData(id);
                    }
                } finally {
                    if (lock != null) {
                        log.debug("release lock on filechannel "
                                +  lockFile.getChannel());
                        lock.release();
                    }
                    lockFile.close();
                }
            } catch (IOException e) {
                String errMsg = "Error obtaining lock for file '"
                    + cachedFile.getAbsolutePath() + "'.";
                log.warn(errMsg, e);
                throw new IOFailure(errMsg, e);
            }
        }
    }

    /** Utility method to get a number of cache entries at a time.
     * Implementations of FileBasedCache may override this to perform the
     * caching more efficiently, if caching overhead per file is large.
     *
     * @param IDs List of IDs that uniquely identify a set of items within
     * the cache.
     * @return A map from ID to the files containing cached data for those
     * IDs.  If caching failed, even partially, for an ID, the entry for the ID
     * doesn't exist.
     */
    public Map<I, File> get(Set<I> IDs) {
        ArgumentNotValid.checkNotNull(IDs, "IDs");
        Map<I, File> result = new HashMap<I, File>(IDs.size());
        for (I ID : IDs) {
            if (ID.equals(cache(ID))) {
                result.put(ID, getCacheFile(ID));
            } else {
                result.put(ID, null);
            }
        }
        return result;
    }

    /** Forgiving index generating method, that returns a file with an
     * index, of the greatest possible subset of a given id, and the subset.
     *
     * If the type I for instance is a Set, you may get an index of only a
     * subset. If I is a File, null may be seen as a subset.
     *
     * @see #cache for more information.
     *
     * @param id The requested index.
     * @return An index over the greatest possible subset, and the subset.
     */
    public Index<I> getIndex(I id) {
        I response = id;
        I lastResponse = null;
        while (response != null && !response.equals(lastResponse)) {
            if (lastResponse != null) {
                log.info("Requested index of type '" 
                         + this.getCacheDir().getName() + "' data '"
                         + lastResponse
                         + "' not available. Retrying with available subset '"
                         + response + "'");
            }
            lastResponse = response;
            response = cache(lastResponse);
        }
        File cacheFile = getCacheFile(response);
        log.info("Generated index '" + cacheFile + "' of id '" + response
                 + "', request was for '" + id + "'");
        return new Index<I>(cacheFile, response);
    }
}

