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
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Hashtable;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.math3.util.Pair;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
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
import dk.netarkivet.common.utils.hadoop.GetMetadataMapper;
import dk.netarkivet.common.utils.hadoop.HadoopJob;
import dk.netarkivet.common.utils.hadoop.HadoopJobStrategy;
import dk.netarkivet.common.utils.hadoop.HadoopJobUtils;
import dk.netarkivet.common.utils.hadoop.MetadataExtractionStrategy;
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

    /** The actual pattern to be used for matching the url in the metadata record */
    private Pattern urlPattern;
 
    /** The actual pattern to be used for matching the mimetype in the metadata record */ 
    private Pattern mimePattern;
 
    /** Try to migrate jobs with a duplicationmigration record. */
    private boolean tryToMigrateDuplicationRecords;
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
        // Should we try to migrate duplicaterecords, yes or no.
        tryToMigrateDuplicationRecords = Settings.getBoolean(HarvesterSettings.INDEXSERVER_INDEXING_TRY_TO_MIGRATE_DUPLICATION_RECORDS);
        log.info("Metadata cache for '{}' is fetching metadata with urls matching '{}' and mimetype matching '{}'. Migration of duplicate records is " 
                + (tryToMigrateDuplicationRecords? "enabled":"disabled"), 
                prefix, urlMatcher1.toString(), mimeMatcher1);
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
        if (Settings.getBoolean(CommonSettings.USE_BITMAG_HADOOP_BACKEND)) {
            return cacheDataHadoop(id);
        } else {
            return cacheDataBatch(id);
        }
    }

    /**
     * Cache data for the given ID using Hadoop.
     *
     * @param id A job ID to cache data for.
     * @return A File containing the data. This file will be the same as getCacheFile(ID);
     * @see FileBasedCache#cacheData(Object)
     */
    private Long cacheDataHadoop(Long id) {
        final String metadataFilePatternSuffix = Settings.get(CommonSettings.METADATAFILE_REGEX_SUFFIX);
        final String specifiedPattern = "(.*-)?" + id + "(-.*)?" + metadataFilePatternSuffix;
        Configuration conf = HadoopJobUtils.getConf();
        conf.setPattern(GetMetadataMapper.URL_PATTERN, urlPattern);
        conf.setPattern(GetMetadataMapper.MIME_PATTERN, mimePattern);

        try (FileSystem fileSystem = FileSystem.newInstance(conf)) {
            HadoopJobStrategy jobStrategy = new MetadataExtractionStrategy(id, fileSystem);
            HadoopJob job = new HadoopJob(id, jobStrategy);
            job.processOnlyFilesMatching(specifiedPattern);
            job.prepareJobInputOutput(fileSystem);
            job.run();
            // If no error is thrown, job was success
            File cacheFileName = getCacheFile(id);
            if (tryToMigrateDuplicationRecords) {
                log.warn("Attempting to migrate duplication records via hadoop. This operation is not well tested.");
                List<String> metadataLines = HadoopJobUtils.collectOutputLines(fileSystem, job.getJobOutputDir());
                migrateDuplicatesHadoop(id, fileSystem, specifiedPattern, metadataLines, cacheFileName);
            } else {
                log.info("Collecting hadoop output to {}", cacheFileName.getAbsolutePath());
                try (OutputStream os = new FileOutputStream(cacheFileName)) {
                    HadoopJobUtils.collectOutputLines(fileSystem, job.getJobOutputDir(), os);
                }
                log.info("Collected {} bytes hadoop output to {}", cacheFileName.length(), cacheFileName.getAbsolutePath());
            }
            log.debug("Cached data for job '{}' for '{}'", id, prefix);
            return id;
        } catch (IOException e) {
            log.error("Error instantiating Hadoop filesystem for job {}.", id, e);
            return null;
        }
    }

    /**
     * If this cache represents a crawllog cache then this method will attempt to migrate any duplicate annotations in
     * the crawl log using data in the duplicationmigration metadata record. This migrates filename/offset
     * pairs from uncompressed to compressed (w)arc files. This method has the side effect of copying the index
     * cache (whether migrated or not) into the cache file whose name is generated from the id.
     * @param id the id of the cache
     * @param fileSystem the filesystem on which the operations are carried out
     * @param specifiedPattern the pattern specifying the files to be found
     * @param originalJobResults the original hadoop job results which is a list containing the unmigrated data.
     * @param cacheFileName the cache file for the job which the index cache is copied to.
     */
    private void migrateDuplicatesHadoop(Long id, FileSystem fileSystem, String specifiedPattern, List<String> originalJobResults, File cacheFileName) {
        log.debug("Looking for a duplicationmigration record for id {}", id);
        if (urlPattern.pattern().equals(MetadataFile.CRAWL_LOG_PATTERN)) {
            Configuration conf = fileSystem.getConf();
            conf.setPattern(GetMetadataMapper.URL_PATTERN, Pattern.compile(".*duplicationmigration.*"));
            conf.setPattern(GetMetadataMapper.MIME_PATTERN, Pattern.compile("text/plain"));
            HadoopJobStrategy jobStrategy = new MetadataExtractionStrategy(id, fileSystem);
            HadoopJob job = new HadoopJob(id, jobStrategy);
            job.processOnlyFilesMatching(specifiedPattern);
            job.prepareJobInputOutput(fileSystem);
            job.run();

            try {
                List<String> metadataLines = HadoopJobUtils.collectOutputLines(fileSystem, job.getJobOutputDir());
                handleMigrationHadoop(id, metadataLines, originalJobResults, cacheFileName);
            } catch (IOException e) {
                log.error("Failed getting duplicationmigration lines output from Hadoop job with ID: {}", id);
            }
        } else {
            copyResults(id, originalJobResults, cacheFileName);
        }
    }

    /**
     * Helper method for {@link #migrateDuplicatesHadoop}.
     * Does the actual handling of migration after the job has finished successfully.
     * @param id The id of the cache.
     * @param metadataLines The resulting lines from the duplication-migration job.
     * @param originalJobResults The original hadoop job results which is a list containing the unmigrated data.
     * @param cacheFileName The cache file for the job which the index cache is copied to.
     */
    private void handleMigrationHadoop(Long id, List<String> metadataLines, List<String> originalJobResults,
            File cacheFileName) {
        File migration = null;
        try {
            migration = Files.createTempFile("migration", "txt").toFile();
        } catch (IOException e) {
            throw new IOFailure("Could not create temporary output file.");
        }
        if (metadataLines.size() > 0) {
            copyResults(id, metadataLines, migration);
        }
        boolean doMigration = migration.exists() && migration.length() > 0;
        if (doMigration) {
            log.info("Found a nonempty duplicationmigration record. Now we do the migration for job {}", id);
            Hashtable<Pair<String, Long>, Long> lookup = createLookupTableFromMigrationLines(id, migration);

            File crawllog = createTempOutputFile();
            copyResults(id, originalJobResults, crawllog);
            migrateFilenameOffsetPairs(id, cacheFileName, crawllog, lookup);
        } else {
            copyResults(id, originalJobResults, cacheFileName);
        }
    }

    /**
     * Helper method for creating a temp file to copy job results to.
     * @return A new temp file to copy output to.
     */
    private File createTempOutputFile() {
        File crawllog = null;
        try {
            crawllog = Files.createTempFile("dedup", "txt").toFile();
        } catch (IOException e) {
            throw new IOFailure("Could not create temporary output file.");
        }
        return crawllog;
    }

    /**
     * Helper method for Hadoop methods.
     * Copies the results of a job to a file.
     * @param id The ID of the current job.
     * @param jobResults The resulting lines output from a job.
     * @param file The file to copy the results to.
     */
    private void copyResults(Long id, List<String> jobResults, File file) {
        try {
            Files.write(Paths.get(file.getAbsolutePath()), jobResults);
        } catch (IOException e) {
            throw new IOFailure("Failed writing results of job with ID '" + id + "' to file " + file.getAbsolutePath());
        }
    }

    /**
     * Actually cache data for the given ID.
     *
     * @param id A job ID to cache data for.
     * @return A File containing the data. This file will be the same as getCacheFile(ID);
     * @see FileBasedCache#cacheData(Object)
     */
    private Long cacheDataBatch(Long id) {
        final String replicaUsed = Settings.get(CommonSettings.USE_REPLICA_ID);
        final String metadataFilePatternSuffix = Settings.get(CommonSettings.METADATAFILE_REGEX_SUFFIX);
        // Same pattern here as defined in class dk.netarkivet.viewerproxy.webinterface.Reporting
        final String specifiedPattern = "(.*-)?" + id + "(-.*)?" + metadataFilePatternSuffix;

        final ArchiveBatchJob job = new GetMetadataArchiveBatchJob(urlPattern, mimePattern);
        log.debug("Extract using a batchjob of type '{}' cachedata from files matching '{}' on replica '{}'. Url pattern is '{}' and mimepattern is '{}'", job
                .getClass().getName(), specifiedPattern, replicaUsed, urlPattern, mimePattern);

        job.processOnlyFilesMatching(specifiedPattern);
        BatchStatus b = arcrep.batch(job, replicaUsed);
        // This check ensures that we got data from at least one file.
        // Mind you, the data may be empty, but at least one file was
        // successfully processed.
        if (b.hasResultFile() && b.getNoOfFilesProcessed() > b.getFilesFailed().size()) {
            File cacheFileName = getCacheFile(id);
            if (tryToMigrateDuplicationRecords) {
                migrateDuplicatesBatch(id, replicaUsed, specifiedPattern, b, cacheFileName);
            } else {
                b.copyResults(cacheFileName);
            }
            log.debug("Cached data for job '{}' for '{}'", id, prefix);
            return id;
        } else {
            // Look for data in other bitarchive replicas, if this option is enabled
            if (!Settings.getBoolean(HarvesterSettings.INDEXSERVER_INDEXING_LOOKFORDATAINOTHERBITARCHIVEREPLICAS)) {
                log.info("No data found for job '{}' for '{}' in local bitarchive '{}'. ", id, prefix, replicaUsed);
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
                            if (tryToMigrateDuplicationRecords) {
                                migrateDuplicatesBatch(id, rep.getId(), specifiedPattern, b, cacheFileName);
                            } else {
                                b.copyResults(cacheFileName);
                            }
                            log.debug("Cached data for job '{}' for '{}'", id, prefix);
                            return id;
                        } else {
                            log.trace("No data found for job '{}' for '{}' in bitarchive '{}'. ", id, prefix, rep);
                        }
                    }
                }
                log.info("No data found for job '{}' for '{}' in all bitarchive replicas", id, prefix);
            }
            return null;
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
    private void migrateDuplicatesBatch(Long id, String replicaUsed, String specifiedPattern, BatchStatus originalBatchJob, File cacheFileName) {
        log.debug("Looking for a duplicationmigration record for id {}", id);
        if (urlPattern.pattern().equals(MetadataFile.CRAWL_LOG_PATTERN)) {
            GetMetadataArchiveBatchJob job2 = new GetMetadataArchiveBatchJob(Pattern.compile(".*duplicationmigration.*"), Pattern.compile("text/plain"));
            job2.processOnlyFilesMatching(specifiedPattern);
            BatchStatus b2 = arcrep.batch(job2, replicaUsed);
            File migration = null;
            try {
                migration = Files.createTempFile("migration", "txt").toFile();
            } catch (IOException e) {
                throw new IOFailure("Could not create temporary output file.");
            }
            if (b2.hasResultFile()) {
                b2.copyResults(migration);
            }
            boolean doMigration = migration.exists() && migration.length() > 0;
            if (doMigration) {
                log.info("Found a nonempty duplicationmigration record. Now we do the migration for job {}", id);
                Hashtable<Pair<String, Long>, Long> lookup = createLookupTableFromMigrationLines(id, migration);

                File crawllog = createTempOutputFile();
                originalBatchJob.copyResults(crawllog);
                migrateFilenameOffsetPairs(id, cacheFileName, crawllog, lookup);
            } else {
                originalBatchJob.copyResults(cacheFileName);
            }
        } else {
            originalBatchJob.copyResults(cacheFileName);
        }
    }

    /**
     * Helper method.
     * Does the actual migration of filename/offset pairs from uncompressed to compressed (w)arc files.
     * This method has the side effect of copying the index cache (whether migrated or not) into the cache file
     * whose name is generated from the ID
     * @param id The ID of the current job.
     * @param cacheFileName The cache file for the job which the index cache is copied to.
     * @param crawllog A temp file containing the resulting lines of the Hadoop job.
     * @param lookup A lookup table to get the filename/offset pairs from.
     */
    private void migrateFilenameOffsetPairs(Long id, File cacheFileName, File crawllog, Hashtable<Pair<String, Long>, Long> lookup) {
        Pattern duplicatePattern = Pattern.compile(".*duplicate:\"([^,]+),([0-9]+).*");
        try {
            int matches = 0;
            int errors = 0;
            for (String line : org.apache.commons.io.FileUtils.readLines(crawllog)) {
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
    }

    /**
     * Helper method.
     * Generates a lookup table of filename/offset pairs from a file containing extracted metadata lines.
     * @param id The ID for the current job.
     * @param migrationFile The file containing the extracted metadata lines.
     * @return A lookup table of filename/offset pairs.
     */
    private Hashtable<Pair<String, Long>, Long> createLookupTableFromMigrationLines(Long id, File migrationFile) {
        Hashtable<Pair<String, Long>, Long> lookup = new Hashtable<>();
        try {
            final List<String> migrationLines = org.apache.commons.io.FileUtils.readLines(migrationFile);
            log.info("{} migrationFile records found for job {}", migrationLines.size(), id);
            for (String line : migrationLines) {
                // duplicationmigration lines look like this: "FILENAME 496812 393343 1282069269000"
                // But only the first 3 entries are used.
                String[] splitLine = StringUtils.split(line);
                if (splitLine.length >= 3) {
                    lookup.put(new Pair<String, Long>(splitLine[0], Long.parseLong(splitLine[1])),
                         Long.parseLong(splitLine[2]));
                } else {
                   log.warn("Line '" + line + "' has a wrong format. Ignoring line");
                }
            }
        } catch (IOException e) {
            throw new IOFailure("Could not read " + migrationFile.getAbsolutePath());
        } finally {
            migrationFile.delete();
        }
        return lookup;
    }
}
