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
import java.io.IOException;
import java.util.Hashtable;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.math3.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.distribute.arcrepository.ArcRepositoryClientFactory;
import dk.netarkivet.common.distribute.arcrepository.BatchStatus;
import dk.netarkivet.common.distribute.arcrepository.Replica;
import dk.netarkivet.common.distribute.arcrepository.ReplicaType;
import dk.netarkivet.common.distribute.arcrepository.ViewerArcRepositoryClient;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.utils.archive.ArchiveBatchJob;
import dk.netarkivet.common.utils.archive.GetMetadataArchiveBatchJob;
import dk.netarkivet.harvester.HarvesterSettings;
import dk.netarkivet.harvester.harvesting.metadata.MetadataFile;

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

    /** The actual pattern to be used for matching the url in the metadata record */
    private Pattern urlPattern;
 
   /** The actual pattern to be used for matching the mimetype in the metadata record */ 
    private Pattern mimePattern;
 
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
        urlPattern = urlMatcher1;
        Pattern mimeMatcher1;
        if (mimeMatcher != null) {
            mimeMatcher1 = mimeMatcher;
        } else {
            mimeMatcher1 = MATCH_ALL_PATTERN;
        }
        mimePattern = mimeMatcher1;
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
        final String metadataFilePatternSuffix = Settings.get(CommonSettings.METADATAFILE_REGEX_SUFFIX);
        //FIXME The current specifiedPattern also accepts files that that includes the Id in the metadatafile name, either
        // as a prefix, infix, or suffix (NAS-1712)
        final String specifiedPattern = ".*" + id + ".*" + metadataFilePatternSuffix;
        // This suggested solution below is incompatible with the prefix pattern (see NAS-2714) 
        // introduced in harvester/harvester-core/src/main/java/dk/netarkivet/harvester/harvesting/metadata/MetadataFileWriter.java
        // part of release 5.3.1
        //final String specifiedPattern = id + metadataFilePatternSuffix; 
        log.debug("Extract using a batchjob of type '{}' cachedata from files matching '{}' on replica '{}'. Url pattern is '{}' and mimepattern is '{}'", job
                .getClass().getName(), specifiedPattern, replicaUsed, urlPattern, mimePattern);
        job.processOnlyFilesMatching(specifiedPattern);
        BatchStatus b = arcrep.batch(job, replicaUsed);
        // This check ensures that we got data from at least one file.
        // Mind you, the data may be empty, but at least one file was
        // successfully processed.
        if (b.hasResultFile() && b.getNoOfFilesProcessed() > b.getFilesFailed().size()) {
            migrateDuplicates(id, replicaUsed, specifiedPattern, b);
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
                            migrateDuplicates(id, rep.getId(), specifiedPattern, b);
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

    /**
     * If this cache represents a crawllog cache then this method will attempt to migrate any duplicate annotations in
     * the crawl log using data in the duplicationmigration metadata record. This migrates filename/offset
     * pairs from uncompressed to compressed (w)arc files. This method has the side effect of copying the index
     * cache (whether migrated or not) into the cache file whose name is generated from the id.
     * @param id the id of the cache
     * @param replicaUsed which replica to look the file up in
     * @param specifiedPattern the pattern specifying the files to be found
     * @param originalBatchJob the original batch job which returned the unmigrated data.
     */
    private void migrateDuplicates(Long id, String replicaUsed, String specifiedPattern, BatchStatus originalBatchJob) {
        File cacheFileName = getCacheFile(id);
        Pattern duplicatePattern = Pattern.compile(".*duplicate:\"([^,]+),([0-9]+).*");
        if (urlPattern.pattern().equals(MetadataFile.CRAWL_LOG_PATTERN)) {
            GetMetadataArchiveBatchJob job2 = new GetMetadataArchiveBatchJob(Pattern.compile(".*duplicationmigration.*"), Pattern.compile("text/plain"));
            job2.processOnlyFilesMatching(specifiedPattern);
            BatchStatus b2 = arcrep.batch(job2, replicaUsed);
            File migration = null;
            try {
                migration = File.createTempFile("migration", "txt");
            } catch (IOException e) {
                throw new IOFailure("Could not create temporary output file.");
            }
            if (b2.hasResultFile()) {
                b2.copyResults(migration);
            }
            boolean doMigration =  migration.exists() && migration.length() > 0;
            Hashtable<Pair<String, Long>, Long> lookup = new Hashtable<>();
            if (doMigration) {
                log.info("Doing migration for {}", id);
                try {
                    final List<String> migrationLines = org.apache.commons.io.FileUtils.readLines(migration);
                    log.info("{} migration records found for job {}", migrationLines.size(), id);
                    // duplicationmigration lines should look like this: "FILENAME 496812 393343 1282069269000"
                    // But only the first 3 entries are used.
                    for (String line : migrationLines) {
                    	// duplicationmigration lines look like this: "FILENAME 496812 393343 1282069269000"
                        String[] splitLine = StringUtils.split(line);
                        if (splitLine.length >= 3) { 
                            lookup.put(new Pair<String, Long>(splitLine[0], Long.parseLong(splitLine[1])),
                                 Long.parseLong(splitLine[2])); 
                          } else {
                               log.warn("Line '" + line + "' has a wrong format. Ignoring line");
                          }
                    }
                } catch (IOException e) {
                    throw new IOFailure("Could not read " + migration.getAbsolutePath());
                } finally {
                    migration.delete();
                }
            }
            if (doMigration) {
                File crawllog = null;
                try {
                    crawllog = File.createTempFile("dedup", "txt");
                } catch (IOException e) {
                    throw new IOFailure("Could not create temporary output file.");
                }
                originalBatchJob.copyResults(crawllog);
                try {
                    int matches = 0;
                    int errors = 0;
                    for (String line :  org.apache.commons.io.FileUtils.readLines(crawllog)) {
                        Matcher m = duplicatePattern.matcher(line);
                        if (m.matches()) {
                            matches++;
                            Long newOffset = lookup.get(new Pair<String, Long>(m.group(1), Long.parseLong(m.group(2))));
                            if (newOffset == null) {
                                log.warn("Could not migrate duplicate in " + line);
                                FileUtils.appendToFile(cacheFileName, line);
                                errors++;
                            } else {
                                String newLine = line.substring(0, m.start(2)) + newOffset + line.substring(m.end(2));
                                newLine = newLine.replace(m.group(1), m.group(1) + ".gz");
                                FileUtils.appendToFile(cacheFileName, newLine);
                            }
                        } else {
                            FileUtils.appendToFile(cacheFileName, line);
                        }
                    }
                    log.info("Found and migrated {} duplicate lines for job {} with {} errors", matches, id, errors); 
                } catch (IOException e) {
                    throw new IOFailure("Could not read " + crawllog.getAbsolutePath());
                } finally {
                    crawllog.delete();
                }
            } else {
                originalBatchJob.copyResults(cacheFileName);
            }
        } else {
            originalBatchJob.copyResults(cacheFileName);
        }
        log.debug("Cached data for job '{}' for '{}'", id, prefix);
    }

}
