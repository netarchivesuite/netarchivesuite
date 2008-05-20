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

package dk.netarkivet.common.distribute.indexserver;

import java.io.File;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.common.Settings;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.StringUtils;

/**
 * A trivial JobIndexCache implementation that just assumes somebody places
 * the indexes in the right place (in TrivialJobIndexCache under the cache dir).
 *
 */
public class TrivialJobIndexCache implements JobIndexCache {
    private Log log = LogFactory.getLog(getClass());
    private static final String CACHE_SUBDIR = "TrivialJobIndexCache";
    private final File dir = new File(Settings.get(Settings.CACHE_DIR),
            CACHE_SUBDIR);
    private final RequestType requestType;

    /** Construct a trivial cache that requires manual setup of files.
     *
     * The directory that the files are to be put into will be created by
     * this method.
     */
    public TrivialJobIndexCache(RequestType t) {
        ArgumentNotValid.checkNotNull(t, "RequestType t");
        requestType = t;
        FileUtils.createDir(dir);
    }

    /**
     * Get an index for the given list of job IDs.
     * The resulting file contains a suitably sorted list.
     * This method should always be safe for asynchronous calling.
     * This method may use a cached version of the file.
     *
     * @param jobIDs Set of job IDs to generate index for.
     * @return A file containing the index. This file must not be modified or
     *         deleted, since it is part of the cache of data.
     */
    public JobIndex<Set<Long>> getIndex(Set<Long> jobIDs) {
        ArgumentNotValid.checkNotNull(jobIDs, "jobIds");
        File cacheFile = new File(dir,
                StringUtils.conjoin("-",jobIDs ) + "-" +
                requestType + "-cache");
        // If the list of jobids is empty, prepend 0 to the cache-dir
        if (jobIDs.isEmpty()) {
            cacheFile = new File(dir, "0-" + requestType + "-cache");
        }

        if (!cacheFile.exists()) {
            log.warn("The cache does not contain '" + cacheFile + "' for "
                    + jobIDs);
        }
        return new JobIndex(cacheFile, jobIDs);
    }
}
