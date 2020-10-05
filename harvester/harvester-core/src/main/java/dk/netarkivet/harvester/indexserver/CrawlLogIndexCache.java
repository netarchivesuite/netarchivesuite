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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.SimpleFSDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.distribute.indexserver.JobIndexCache;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.utils.TimeUtils;
import dk.netarkivet.common.utils.ZipUtils;
import dk.netarkivet.harvester.HarvesterSettings;
import is.hi.bok.deduplicator.CrawlDataIterator;
import is.hi.bok.deduplicator.DigestIndexer;

/**
 * A cache that serves Lucene indices of crawl logs for given job IDs. Uses the DigestIndexer in the deduplicator
 * software: http://deduplicator.sourceforge.net/apidocs/is/hi/bok/deduplicator/DigestIndexer.html Upon combination of
 * underlying files, each file in the Lucene index is gzipped and the compressed versions are stored in the directory
 * given by getCacheFile(). The subclass has to determine in its constructor call which mime types are included.
 */
public abstract class CrawlLogIndexCache extends CombiningMultiFileBasedCache<Long> implements JobIndexCache {

    /** The log. */
    private static final Logger log = LoggerFactory.getLogger(CrawlLogIndexCache.class);

    /** Needed to find origin information, which is file+offset from CDX index. */
    private final CDXDataCache cdxcache = new CDXDataCache();

    /** the useBlacklist set to true results in docs matching the mimefilter being ignored. */
    private boolean useBlacklist;

    /** An regular expression for the mimetypes to include or exclude from the index. See useBlackList. */
    private String mimeFilter;

    /** The time to sleep between each check of completeness. */
    private final long sleepintervalBetweenCompletenessChecks = Settings
            .getLong(HarvesterSettings.INDEXSERVER_INDEXING_CHECKINTERVAL);

    /** Number to separate logs the different combine tasks. */
    private int indexingJobCount = 0;

    /**
     * Constructor for the CrawlLogIndexCache class.
     *
     * @param name The name of the CrawlLogIndexCache
     * @param blacklist Shall the mimefilter be considered a blacklist or a whitelist?
     * @param mimeFilter A regular expression for the mimetypes to exclude/include
     */
    public CrawlLogIndexCache(String name, boolean blacklist, String mimeFilter) {
        super(name, new CrawlLogDataCache());
        useBlacklist = blacklist;
        this.mimeFilter = mimeFilter;
    }

    /**
     * Prepare data for combining. This class overrides prepareCombine to make sure that CDX data is available.
     *
     * @param ids Set of IDs that will be combined.
     * @return Map of ID->File of data to combine for the IDs where we could find data.
     */
    protected Map<Long, File> prepareCombine(Set<Long> ids) {
        log.info("Starting to generate {} for the {} jobs: {}", getCacheDir().getName(), ids.size(), ids);
        Map<Long, File> returnMap = super.prepareCombine(ids);
        Set<Long> missing = new HashSet<Long>();
        for (Long id : returnMap.keySet()) {
            Long cached = cdxcache.cache(id);
            if (cached == null) {
                missing.add(id);
            }
        }
        if (!missing.isEmpty()) {
            log.warn("Data not found for {} jobs: {}", missing.size(), missing);
        }
        for (Long id : missing) {
            returnMap.remove(id);
        }
        return returnMap;
    }

    /**
     * Combine a number of crawl.log files into one Lucene index. This index is placed as gzip files under the directory
     * returned by getCacheFile().
     *
     * @param rawfiles The map from job ID into crawl.log contents. No null values are allowed in this map.
     */
    protected void combine(Map<Long, File> rawfiles) {
        ++indexingJobCount;
        long datasetSize = rawfiles.values().size();
        log.info("Starting combine task #{}. This combines a dataset with {} crawl logs (thread = {})",
                indexingJobCount, datasetSize, Thread.currentThread().getName());

        File resultDir = getCacheFile(rawfiles.keySet());
        Set<File> tmpfiles = new HashSet<File>();
        String indexLocation = resultDir.getAbsolutePath() + ".luceneDir";
        ThreadPoolExecutor executor = null;
        try {
            DigestIndexer indexer = createStandardIndexer(indexLocation);
            final boolean verboseIndexing = false;
            DigestOptions indexingOptions = new DigestOptions(this.useBlacklist, verboseIndexing, this.mimeFilter);
            long count = 0;
            Set<IndexingState> outstandingJobs = new HashSet<IndexingState>();
            final int maxThreads = Settings.getInt(HarvesterSettings.INDEXSERVER_INDEXING_MAXTHREADS);
            executor = new ThreadPoolExecutor(maxThreads, maxThreads, 0L, TimeUnit.MILLISECONDS,
                    new LinkedBlockingQueue<Runnable>());

            executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

            for (Map.Entry<Long, File> entry : rawfiles.entrySet()) {
                Long jobId = entry.getKey();
                File crawlLog = entry.getValue();
                // Generate UUID to ensure a unique filedir for the index.
                File tmpFile = new File(FileUtils.getTempDir(), UUID.randomUUID().toString());
                tmpfiles.add(tmpFile);
                String localindexLocation = tmpFile.getAbsolutePath();
                Long cached = cdxcache.cache(jobId);
                if (cached == null) {
                    log.warn("Skipping the ingest of logs for job {}. Unable to retrieve cdx-file for job.",
                            entry.getKey());
                    continue;
                }
                File cachedCDXFile = cdxcache.getCacheFile(cached);

                // Dispatch this indexing task to a separate thread that
                // handles the sorting of the logfiles and the generation
                // of a lucene index for this crawllog and cdxfile.
                ++count;
                String taskID = count + " out of " + datasetSize;
                log.debug("Making subthread for indexing job " + jobId + " - task " + taskID);
                Callable<Boolean> task = new DigestIndexerWorker(localindexLocation, jobId, crawlLog, cachedCDXFile,
                        indexingOptions, taskID);
                Future<Boolean> result = executor.submit(task);
                outstandingJobs.add(new IndexingState(jobId, localindexLocation, result));
            }

            // wait for all the outstanding subtasks to complete.
            Set<Directory> subindices = new HashSet<Directory>();

            // Deadline for the combine-task
            long combineTimeout = Settings.getLong(HarvesterSettings.INDEXSERVER_INDEXING_TIMEOUT);
            long timeOutTime = System.currentTimeMillis() + combineTimeout;

            // The indexwriter for the totalindex.
            IndexWriter totalIndex = indexer.getIndex();
            int subindicesInTotalIndex = 0;
            // Max number of segments in totalindex.
            int maxSegments = Settings.getInt(HarvesterSettings.INDEXSERVER_INDEXING_MAX_SEGMENTS);

            final int ACCUMULATED_SUBINDICES_BEFORE_MERGING = 200;

            while (outstandingJobs.size() > 0) {
                log.info("Outstanding jobs in combine task #{} is now {}", indexingJobCount, outstandingJobs.size());
                Iterator<IndexingState> iterator = outstandingJobs.iterator();
                if (timeOutTime < System.currentTimeMillis()) {
                    log.warn("Max indexing time exceeded for one index ({}). Indexing stops here, "
                            + "although missing subindices for {} jobs",
                            TimeUtils.readableTimeInterval(combineTimeout), outstandingJobs.size());
                    break;
                }
                while (iterator.hasNext() && subindices.size() < ACCUMULATED_SUBINDICES_BEFORE_MERGING) {
                    Future<Boolean> nextResult;
                    IndexingState next = iterator.next();
                    if (next.getResultObject().isDone()) {
                        nextResult = next.getResultObject();
                        try {
                            // check, if the indexing failed
                            if (nextResult.get()) {
                                subindices.add(new SimpleFSDirectory(new File(next.getIndex())));
                            } else {
                                log.warn("Indexing of job {} failed.", next.getJobIdentifier());
                            }

                        } catch (InterruptedException e) {
                            log.warn("Unable to get Result back from indexing thread", e);
                        } catch (ExecutionException e) {
                            log.warn("Unable to get Result back from indexing thread", e);
                        }
                        // remove the done object from the set
                        iterator.remove();
                    }
                }

                if (subindices.size() >= ACCUMULATED_SUBINDICES_BEFORE_MERGING) {

                    log.info(
                            "Adding {} subindices to main index. Forcing index to contain max {} files (related to combine task #{})",
                            subindices.size(), maxSegments, indexingJobCount);
                    totalIndex.addIndexes(subindices.toArray(new Directory[0]));
                    totalIndex.forceMerge(maxSegments);
                    totalIndex.commit();
                    for (Directory luceneDir : subindices) {
                        luceneDir.close();
                    }
                    subindicesInTotalIndex += subindices.size();
                    log.info(
                            "Completed adding {} subindices to main index, now containing {} subindices(related to combine task #{})",
                            subindices.size(), subindicesInTotalIndex, indexingJobCount);
                    subindices.clear();
                } else {
                    sleepAwhile();
                }
            }

            log.info("Adding the final {} subindices to main index. "
                    + "Forcing index to contain max {} files (related to combine task #{})", subindices.size(),
                    maxSegments, indexingJobCount);

            totalIndex.addIndexes(subindices.toArray(new Directory[0]));
            totalIndex.forceMerge(maxSegments);
            totalIndex.commit();
            for (Directory luceneDir : subindices) {
                luceneDir.close();
            }
            subindices.clear();

            log.info("Adding operation completed (combine task #{})!", indexingJobCount);
            long docsInIndex = totalIndex.numDocs();

            indexer.close();
            log.info("Closed index (related to combine task #{}", indexingJobCount);

            // Now the index is made, gzip it up.
            File totalIndexDir = new File(indexLocation);
            log.info("Gzip-compressing the individual {} index files of combine task # {}",
                    totalIndexDir.list().length, indexingJobCount);
            ZipUtils.gzipFiles(totalIndexDir, resultDir);
            log.info(
                    "Completed combine task #{} that combined a dataset with {} crawl logs (entries in combined index: {}) - compressed index has size {}",
                    indexingJobCount, datasetSize, docsInIndex, FileUtils.getHumanReadableFileSize(resultDir));
        } catch (IOException e) {
            throw new IOFailure("Error setting up craw.log index framework for " + resultDir.getAbsolutePath(), e);
        } finally {
            // close down Threadpool-executor
            closeDownThreadpoolQuietly(executor);
            FileUtils.removeRecursively(new File(indexLocation));
            for (File temporaryFile : tmpfiles) {
                FileUtils.removeRecursively(temporaryFile);
            }
        }
    }

    /**
     * Try to release all resources connected to the given ThreadPoolExecutor.
     *
     * @param executor a ThreadPoolExecutor
     */
    private void closeDownThreadpoolQuietly(ThreadPoolExecutor executor) {
        if (executor == null) {
            return;
        }
        if (!executor.isShutdown()) {
            executor.shutdownNow();
        }
    }

    /**
     * Helper class to sleep a little between completeness checks.
     */
    private void sleepAwhile() {
        try {
            Thread.sleep(sleepintervalBetweenCompletenessChecks);
        } catch (InterruptedException e) {
            log.trace("Was awoken early from sleep: ", e);
        }
    }

    /**
     * Ingest a single crawl.log file using the corresponding CDX file to find offsets.
     *
     * @param id ID of a job to ingest.
     * @param crawllogfile The file containing the crawl.log data for the job
     * @param cdxfile The file containing the cdx data for the job
     * @param options The digesting options used.
     * @param indexer The indexer to add to.
     */
    protected static void indexFile(Long id, File crawllogfile, File cdxfile, DigestIndexer indexer,
            DigestOptions options) {
        log.debug("Ingesting the crawl.log file '{}' related to job {}", crawllogfile.getAbsolutePath(), id);
        boolean blacklist = options.getUseBlacklist();
        final String mimefilter = options.getMimeFilter();
        final boolean verbose = options.getVerboseMode();

        CrawlDataIterator crawlLogIterator = null;
        File sortedCdxFile = null;
        File tmpCrawlLog = null;
        BufferedReader cdxBuffer = null;
        try {
            sortedCdxFile = getSortedCDX(cdxfile);
            cdxBuffer = new BufferedReader(new FileReader(sortedCdxFile));
            tmpCrawlLog = getSortedCrawlLog(crawllogfile);
            crawlLogIterator = new CDXOriginCrawlLogIterator(tmpCrawlLog, cdxBuffer);
            indexer.writeToIndex(crawlLogIterator, mimefilter, blacklist, "ERROR", verbose);
        } catch (IOException e) {
            throw new IOFailure("Fatal error indexing " + id, e);
        } finally {
            try {
                if (crawlLogIterator != null) {
                    crawlLogIterator.close();
                }
                if (tmpCrawlLog != null) {
                    FileUtils.remove(tmpCrawlLog);
                }
                if (cdxBuffer != null) {
                    cdxBuffer.close();
                }
                if (sortedCdxFile != null) {
                    FileUtils.remove(sortedCdxFile);
                }
            } catch (IOException e) {
                log.warn("Error cleaning up after crawl log index cache generation", e);
            }
        }
    }

    /**
     * Get a sorted, temporary CDX file corresponding to the given CDXfile.
     *
     * @param cdxFile A cdxfile
     * @return A temporary file with CDX info for that just sorted according to the standard CDX sorting rules. This
     * file will be removed at the exit of the JVM, but should be attempted removed when it is no longer used.
     */
    protected static File getSortedCDX(File cdxFile) {
        try {
            final File tmpFile = File.createTempFile("sorted", "cdx", FileUtils.getTempDir());
            // This throws IOFailure, if the sorting operation fails
            FileUtils.sortCDX(cdxFile, tmpFile);
            tmpFile.deleteOnExit();
            return tmpFile;
        } catch (IOException e) {
            throw new IOFailure("Error while making tmp file for " + cdxFile, e);
        }
    }

    /**
     * Get a sorted, temporary crawl.log file from an unsorted one.
     *
     * @param file The file containing an unsorted crawl.log file.
     * @return A temporary file containing the entries sorted according to URL. The file will be removed upon exit of
     * the JVM, but should be attempted removed when it is no longer used.
     */
    protected static File getSortedCrawlLog(File file) {
        try {
            File tmpCrawlLog = File.createTempFile("sorted", "crawllog", FileUtils.getTempDir());
            // This throws IOFailure, if the sorting operation fails
            FileUtils.sortCrawlLog(file, tmpCrawlLog);
            tmpCrawlLog.deleteOnExit();
            return tmpCrawlLog;
        } catch (IOException e) {
            throw new IOFailure("Error creating sorted crawl log file for '" + file + "'", e);
        }
    }

    /**
     * Create standard deduplication indexer.
     *
     * @param indexLocation The full path to the indexing directory
     * @return the created deduplication indexer.
     * @throws IOException If unable to open the index.
     */
    protected static DigestIndexer createStandardIndexer(String indexLocation) throws IOException {
        // Setup Lucene for indexing our crawllogs
        // MODE_BOTH: Both URL's and Hash are indexed: Alternatives:
        // DigestIndexer.MODE_HASH or DigestIndexer.MODE_URL
        String indexingMode = DigestIndexer.MODE_BOTH;
        // used to be 'equivalent' setting
        boolean includeNormalizedURL = false;
        // used to be 'timestamp' setting
        boolean includeTimestamp = true;
        // used to be 'etag' setting
        boolean includeEtag = true;
        boolean addToExistingIndex = false;
        DigestIndexer indexer = new DigestIndexer(indexLocation, indexingMode, includeNormalizedURL, includeTimestamp,
                includeEtag, addToExistingIndex);
        return indexer;
    }

}
