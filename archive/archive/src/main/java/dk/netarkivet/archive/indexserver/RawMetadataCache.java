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
import java.io.IOException;
import java.io.OutputStream;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.archive.io.arc.ARCRecord;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.Constants;
import dk.netarkivet.common.distribute.arcrepository.ArcRepositoryClientFactory;
import dk.netarkivet.common.distribute.arcrepository.BatchStatus;
import dk.netarkivet.common.distribute.arcrepository.ViewerArcRepositoryClient;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.utils.arc.ARCBatchJob;

/**
 * This is an implementation of the RawDataCache specialized for data out
 * of metadata files.  It uses regular expressions for matching URL and
 * mime-type of ARC entries for the kind of metadata we want.
 *
 */
public class RawMetadataCache extends FileBasedCache<Long>
        implements RawDataCache {
    /** A regular expression object that matches everything. */
    private static final Pattern MATCH_ALL_PATTERN = Pattern.compile(".*");
    /** The prefix (cache name) that this cache uses. */
    private final String prefix;
    /** The arc repository interface.
     * This does not need to be closed, it is a singleton.
     */
    private ViewerArcRepositoryClient arcrep
        = ArcRepositoryClientFactory.getViewerInstance();

    /** The job that we use to dig through metadata files. */
    private final ARCBatchJob job;

    /** The logger for this class. */
    private final Log log = LogFactory.getLog(getClass());

    /** Create a new RawMetadataCache.  For a given job ID, this will fetch
     * and cache selected content from metadata files
     * (&lt;ID&gt;-metadata-[0-9]+.arc).  Any entry in a metadata file that
     * matches both patterns will be returned.  The returned data does not
     * directly indicate which file they were from, though parts intrinsic to
     * the particular format might.
     *
     * @param prefix A prefix that will be used to distinguish this cache's
     * files from other caches'.  It will be used for creating a directory,
     * so it must not contain characters not legal in directory names.
     * @param URLMatcher A pattern for matching URLs of the desired entries.
     * If null, a .* pattern will be used.
     * @param mimeMatcher A pattern for matching mime-types of the desired
     * entries.  If null, a .* pattern will be used.
     */
    public RawMetadataCache(String prefix, Pattern URLMatcher,
                            Pattern mimeMatcher) {
        super(prefix);
        this.prefix = prefix;
        Pattern URLMatcher1;
        if (URLMatcher != null) {
            URLMatcher1 = URLMatcher;
        } else {
            URLMatcher1 = MATCH_ALL_PATTERN;
        }
        Pattern mimeMatcher1;
        if (mimeMatcher != null) {
            mimeMatcher1 = mimeMatcher;
        } else {
            mimeMatcher1 = MATCH_ALL_PATTERN;
        }
        log.info("Metadata cache for '" + prefix + "' is fetching"
                 + " metadata with urls matching '" + URLMatcher1.toString()
                 + "' and mimetype matching '" + mimeMatcher1 + "'");
        job = new GetMetadataARCBatchJob(URLMatcher1, mimeMatcher1);
    }

    /** Get the file potentially containing (cached) data for a single job.
     *
     * @see FileBasedCache#getCacheFile(Object)
     * @param id The job to find data for.
     * @return The file where cache data for the job can be stored.
     */
    public File getCacheFile(Long id) {
        ArgumentNotValid.checkNotNull(id, "job ID");
        ArgumentNotValid.checkNotNegative(id, "job ID");
        return new File(getCacheDir(), prefix + "-" + id + "-cache");
    }

    /** Actually cache data for the given ID.
     *
     * @see FileBasedCache#cacheData(Object)
     * @param ID A job ID to cache data for.
     * @return A File containing the data.  This file will be the same as
     * getCacheFile(ID);
     */
    protected Long cacheData(Long ID) {
        final String replicaUsed = Settings.get(CommonSettings.USE_REPLICA_ID);
        log.debug("Extract using a batchjob of type '"
                + job.getClass().getName()
                + "' cachedata from files matching '"
                + ID + Constants.METADATA_FILE_PATTERN_SUFFIX
                + "' on replica '" 
                + replicaUsed + "'");
        job.processOnlyFilesMatching(ID
                + Constants.METADATA_FILE_PATTERN_SUFFIX);
        BatchStatus b = arcrep.batch(job, replicaUsed);
        
        // This check ensures that we got data from at least one file.
        // Mind you, the data may be empty, but at least one file was
        // successfully processed.
        if (b.hasResultFile()
                && b.getNoOfFilesProcessed() > b.getFilesFailed().size()) {
            File cacheFileName = getCacheFile(ID);
            b.copyResults(cacheFileName);
            log.debug("Cached data for job '" + ID
                    + "' for '" + prefix + "'");
            return ID;
        } else {
            log.debug("No data found for job '" + ID
                    + "' for '" + prefix + "'");
            return null;
        }
    }

    /** A batch job that extracts metadata. */
    private static class GetMetadataARCBatchJob extends ARCBatchJob {
        private final Pattern URLMatcher;
        private final Pattern mimeMatcher;

        /**
         * TODO: Javadoc.
         */
        public GetMetadataARCBatchJob(Pattern URLMatcher, Pattern mimeMatcher) {
            this.URLMatcher = URLMatcher;
            this.mimeMatcher = mimeMatcher;
        }

        /**
         * TODO: Javadoc.
         */
        public void initialize(OutputStream os) {
        }

        /**
         * TODO: Javadoc.
         */
        public void processRecord(ARCRecord sar, OutputStream os) {
            if (URLMatcher.matcher(sar.getMetaData().getUrl()).matches()
                    && mimeMatcher.matcher(
                            sar.getMetaData().getMimetype()).matches()) {
                try {
                    try {                        
                        byte[] buf = new byte[Constants.IO_BUFFER_SIZE];
                        int bytesRead;
                        while ((bytesRead = sar.read(buf)) != -1) {
                            os.write(buf, 0, bytesRead);
                        }
                    } finally {
                        //TODO Should we close ARCRecord here???
                        //if (is != null) {
                        //    is.close();
                        //}
                    }
                } catch (IOException e) {
                    String message = "Error writing body of ARC entry '"
                            + sar.getMetaData().getArcFile() + "' offset '"
                            + sar.getMetaData().getOffset() + "'";
                    throw new IOFailure(message, e);
                }
            }
        }

        /**
         * TODO: Javadoc.
         */
        public void finish(OutputStream os) {
        }
        
        /**
         * Humanly readable description of this instance.
         */
        public String toString() {
            return getClass().getName() + ", with arguments: URLMatcher= " 
            + URLMatcher + ", mimeMatcher = " + mimeMatcher;
        }
    }
}
