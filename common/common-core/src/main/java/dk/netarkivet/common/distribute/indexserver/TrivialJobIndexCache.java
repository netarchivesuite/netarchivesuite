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

import java.io.File;
import java.util.Set;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.NotImplementedException;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.Settings;

/**
 * A trivial JobIndexCache implementation that just assumes somebody places the indexes in the right place (in
 * TrivialJobIndexCache under the cache dir).
 */
public class TrivialJobIndexCache implements JobIndexCache {

    private static final String CACHE_SUBDIR = "TrivialJobIndexCache";

    private final File dir = new File(Settings.get(CommonSettings.CACHE_DIR), CACHE_SUBDIR);

    private final RequestType requestType;

    /**
     * Construct a trivial cache that requires manual setup of files.
     * <p>
     * The directory that the files are to be put into will be created by this method.
     *
     * @param t The type of requests handled
     */
    public TrivialJobIndexCache(RequestType t) {
        ArgumentNotValid.checkNotNull(t, "RequestType t");
        requestType = t;
        FileUtils.createDir(dir);
    }

    /**
     * Get an index for the given list of job IDs. The resulting file contains a suitably sorted list. This method
     * should always be safe for asynchronous calling. This method may use a cached version of the file.
     *
     * @param jobIDs Set of job IDs to generate index for.
     * @return A file containing the index, and always the full set. The file must not be modified or deleted, since it
     * is part of the cache of data.
     * @throws IOFailure if there is no cache file for the set.
     */
    @Override
    public Index<Set<Long>> getIndex(Set<Long> jobIDs) {
        ArgumentNotValid.checkNotNull(jobIDs, "Set<Long> jobIDs");

        File cacheFile = new File(dir, FileUtils.generateFileNameFromSet(jobIDs, "-" + requestType + "-cache"));

        if (!cacheFile.exists()) {
            throw new IOFailure("The cache does not contain '" + cacheFile + "' for " + jobIDs);
        }
        return new Index<Set<Long>>(cacheFile, jobIDs);
    }

    @Override
    public void requestIndex(Set<Long> jobSet, Long harvestId) {
        throw new NotImplementedException("This feature is not implemented for this type of cache");
    }

}
