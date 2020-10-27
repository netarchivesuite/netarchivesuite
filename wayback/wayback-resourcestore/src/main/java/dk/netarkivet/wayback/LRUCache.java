/*
 * #%L
 * Netarchivesuite - wayback
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
package dk.netarkivet.wayback;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.utils.Settings;

/**
 * An LRU cache, based on <code>LinkedHashMap</code>.
 * <p>
 * <p>
 * This cache has a fixed maximum number of elements (<code>cacheSize</code>). If the cache is full and another entry is
 * added, the LRU (least recently used) entry is dropped.
 * <p>
 * <p>
 * This class is thread-safe. All methods of this class are synchronized.
 * <p>
 * <p>
 * Author: Christian d'Heureuse, Inventec Informatik AG, Zurich, Switzerland<br>
 * Multi-licensed: EPL / LGPL / GPL / AL / BSD.
 * <p>
 * Modified slightly to fit the use of a wayback file cache.
 */
public class LRUCache {
    /** The hashtable loadfactor used here. */
    private static final float hashTableLoadFactor = 0.75f;
    /** The instance of this class. */
    private static LRUCache instance = null;
    /** The map containing pointers to the cache itself. */
    private LinkedHashMap<String, File> map;
    /** The size of the cache. */
    private int cacheSize;
    /** The cache containing the files. */
    private File cacheDir;

    /** Logger. */
    private Log logger = LogFactory.getLog(getClass().getName());

    /**
     * Creates a new LRU cache. Using filename as the key, and the cached file as the value.
     *
     * @param dir The directory where the file is stored.
     * @param cacheSize the maximum number of entries that will be kept in this cache.
     */
    public LRUCache(File dir, int cacheSize) {
        // Validate args
        ArgumentNotValid.checkPositive(cacheSize, "int cacheSize");
        ArgumentNotValid.checkNotNull(dir, "File dir");
        dir.mkdirs();
        ArgumentNotValid.checkTrue(dir.exists(), "Cachedir '" + dir.getAbsolutePath() + "' does not exist");

        this.cacheSize = cacheSize;
        this.cacheDir = dir;

        int hashTableCapacity = (int) Math.ceil(cacheSize / hashTableLoadFactor) + 1;
        map = new LinkedHashMap<String, File>(hashTableCapacity, hashTableLoadFactor, true) {
            // (an anonymous inner class)
            private static final long serialVersionUID = 1;

            @Override
            protected boolean removeEldestEntry(Map.Entry<String, File> eldest) {
                boolean removeEldest = size() > LRUCache.this.cacheSize;
                if (removeEldest) {
                    logger.info("Deleting file '" + eldest.getValue().getAbsolutePath() + "' from cache.");
                    boolean deleted = eldest.getValue().delete();
                    if (!deleted) {
                        logger.warn("Unable to deleted LRU file from cache: " + eldest.getValue());
                    }
                }
                return removeEldest;
            }
        };

        // fill up the map with the contents in cachedir
        // if the contents in cachedir exceeds the given cachesize,
        // change the size of the cache
        String[] cachedirFiles = cacheDir.list();
        logger.info("Initializing the cache with the contents of the cachedir '" + cacheDir.getAbsolutePath() + "'");
        if (cachedirFiles.length > this.cacheSize) {
            logger.warn("Changed the cachesize from " + cacheSize + " to " + cachedirFiles.length);
            this.cacheSize = cachedirFiles.length;
        }
        for (String cachefile : cachedirFiles) {
            map.put(cachefile, new File(cacheDir, cachefile));
        }
        logger.info("The contents of the cache is now " + map.size() + " files");
    }

    /**
     * Constructor, where the arguments for the primary constructor is read from settings.
     */
    public LRUCache() {
        this(new File(Settings.get(WaybackSettings.WAYBACK_RESOURCESTORE_CACHE_DIR)), Settings
                .getInt(WaybackSettings.WAYBACK_RESOURCESTORE_CACHE_MAXFILES));
    }

    /**
     * @return instance of our Cache
     */
    public static synchronized LRUCache getInstance() {
        if (instance == null) {
            instance = new LRUCache();
        }
        return instance;
    }

    /**
     * Retrieves an entry from the cache.<br>
     * The retrieved entry becomes the MRU (most recently used) entry.
     *
     * @param key the key whose associated value is to be returned.
     * @return the value associated to this key, or null if no value with this key exists in the cache.
     */
    public synchronized File get(String key) {
        return map.get(key);
    }

    /**
     * Adds an entry to this cache. The new entry becomes the MRU (most recently used) entry. If an entry with the
     * specified key already exists in the cache, it is replaced by the new entry. If the cache is full, the LRU (least
     * recently used) entry is removed from the cache.
     *
     * @param key the key with which the specified value is to be associated.
     * @param value a value to be associated with the specified key.
     */
    public synchronized void put(String key, File value) {
        map.put(key, value);
    }

    /**
     * Clears the cache.
     */
    public synchronized void clear() {
        map.clear();
    }

    /**
     * Returns the number of used entries in the cache.
     *
     * @return the number of entries currently in the cache.
     */
    public synchronized int usedEntries() {
        return map.size();
    }

    /**
     * @return the cacheDir
     */
    public File getCacheDir() {
        return cacheDir;
    }
}
