/*
 * #%L
 * Netarchivesuite - harvester
 * %%
 * Copyright (C) 2005 - 2014 The Royal Danish Library, the Danish State and University Library,
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
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.Constants;
import dk.netarkivet.common.distribute.arcrepository.ArcRepositoryClientFactory;
import dk.netarkivet.common.distribute.arcrepository.BatchStatus;
import dk.netarkivet.common.distribute.arcrepository.Replica;
import dk.netarkivet.common.distribute.arcrepository.ReplicaType;
import dk.netarkivet.common.distribute.arcrepository.ViewerArcRepositoryClient;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.utils.archive.ArchiveBatchJob;
import dk.netarkivet.common.utils.archive.GetMetadataArchiveBatchJob;
import dk.netarkivet.harvester.HarvesterSettings;

/**
 * This is an implementation of the RawDataCache specialized for data out of metadata files. It uses regular expressions
 * for matching URL and mime-type of ARC entries for the kind of metadata we want.
 */
public class RawMetadataCache extends FileBasedCache<Long> implements RawDataCache {

    /** The logger for this class. */
    private static final Logger log = LoggerFactory.getLogger(RawMetadataCache.class);

    /** A regular expression object that matches everything. */
    public static final Pattern MATCH_ALL_PATTERN = Pattern.compile(".*");
    /** The prefix (cache name) that this cache uses. */
    private final String prefix;
    /**
     * The arc repository interface. This does not need to be closed, it is a singleton.
     */
    private ViewerArcRepositoryClient arcrep = ArcRepositoryClientFactory.getViewerInstance();

    /** The job that we use to dig through metadata files. */
    private final ArchiveBatchJob job;

    /**
     * Create a new RawMetadataCache. For a given job ID, this will fetch and cache selected content from metadata files
     * (&lt;ID&gt;-metadata-[0-9]+.arc). Any entry in a metadata file that matches both patterns will be returned. The
     * returned data does not directly indicate which file they were from, though parts intrinsic to the particular
     * format might.
     *
     * @param prefix A prefix that will be used to distinguish this cache's files from other caches'. It will be used
     * for creating a directory, so it must not contain characters not legal in directory names.
     * @param urlMatcher A pattern for matching URLs of the desired entries. If null, a .* pattern will be used.
     * @param mimeMatcher A pattern for matching mime-types of the desired entries. If null, a .* pattern will be used.
     */
    public RawMetadataCache(String prefix, Pattern urlMatcher, Pattern mimeMatcher) {
        super(prefix);
        this.prefix = prefix;
        Pattern urlMatcher1;
        if (urlMatcher != null) {
            urlMatcher1 = urlMatcher;
        } else {
            urlMatcher1 = MATCH_ALL_PATTERN;
        }
        Pattern mimeMatcher1;
        if (mimeMatcher != null) {
            mimeMatcher1 = mimeMatcher;
        } else {
            mimeMatcher1 = MATCH_ALL_PATTERN;
        }
        log.info("Metadata cache for '{}' is fetching metadata with urls matching '{}' and mimetype matching '{}'",
                prefix, urlMatcher1.toString(), mimeMatcher1);
        job = new GetMetadataArchiveBatchJob(urlMatcher1, mimeMatcher1);
    }

    /**
     * Get the file potentially containing (cached) data for a single job.
     *
     * @param id The job to find data for.
     * @return The file where cache data for the job can be stored.
     * @see FileBasedCache#getCacheFile(Object)
     */
    @Override
    public File getCacheFile(Long id) {
        ArgumentNotValid.checkNotNull(id, "job ID");
        ArgumentNotValid.checkNotNegative(id, "job ID");
        return new File(getCacheDir(), prefix + "-" + id + "-cache");
    }

    /**
     * Actually cache data for the given ID.
     *
     * @param id A job ID to cache data for.
     * @return A File containing the data. This file will be the same as getCacheFile(ID);
     * @see FileBasedCache#cacheData(Object)
     */
    protected Long cacheData(Long id) {
        final String replicaUsed = Settings.get(CommonSettings.USE_REPLICA_ID);
        log.debug("Extract using a batchjob of type '{}' cachedata from files matching '{}{}' on replica '{}'", job
                .getClass().getName(), id, Constants.METADATA_FILE_PATTERN_SUFFIX, replicaUsed);
        job.processOnlyFilesMatching(".*" + id + ".*" + Constants.METADATA_FILE_PATTERN_SUFFIX);
        BatchStatus b = arcrep.batch(job, replicaUsed);

        // This check ensures that we got data from at least one file.
        // Mind you, the data may be empty, but at least one file was
        // successfully processed.
        if (b.hasResultFile() && b.getNoOfFilesProcessed() > b.getFilesFailed().size()) {
            File cacheFileName = getCacheFile(id);
            b.copyResults(cacheFileName);
            log.debug("Cached data for job '{}' for '{}'", id, prefix);
            return id;
        } else {
            // Look for data in other bitarchive replicas, if this option is enabled
            if (!Settings.getBoolean(HarvesterSettings.INDEXSERVER_INDEXING_LOOKFORDATAINOTHERBITARCHIVEREPLICAS)) {
                log.info("No data found for job '{}' for '{}' in local bitarchive '{}'. ", id, prefix, replicaUsed);
                return null;
            } else {
                log.info("No data found for job '{}' for '{}' in local bitarchive '{}'. Trying other replicas.", id,
                        prefix, replicaUsed);
                for (Replica rep : Replica.getKnown()) {
                    // Only use different bitarchive replicas than replicaUsed
                    if (rep.getType().equals(ReplicaType.BITARCHIVE) && !rep.getId().equals(replicaUsed)) {
                        log.debug("Trying to retrieve index data for job '{}' from '{}'.", id, rep.getId());
                        b = arcrep.batch(job, rep.getId());

                        // Perform same check as for the batchresults from
                        // the default replica.
                        if (b.hasResultFile() && (b.getNoOfFilesProcessed() > b.getFilesFailed().size())) {
                            File cacheFileName = getCacheFile(id);
                            b.copyResults(cacheFileName);
                            log.info("Cached data for job '{}' for '{}' from '{}' instead of '{}'", id, prefix, rep,
                                    replicaUsed);
                            return id;
                        } else {
                            log.trace("No data found for job '{}' for '{}' in bitarchive '{}'. ", id, prefix, rep);
                        }
                    }
                }
                log.info("No data found for job '{}' for '{}' in all bitarchive replicas", id, prefix);
                return null;
            }
        }
    }

}
