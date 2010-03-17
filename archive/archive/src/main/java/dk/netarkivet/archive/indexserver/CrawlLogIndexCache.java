/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2010 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301 
 *  USA
 */

package dk.netarkivet.archive.indexserver;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import is.hi.bok.deduplicator.CrawlDataIterator;
import is.hi.bok.deduplicator.DigestIndexer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.common.distribute.indexserver.JobIndexCache;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.UnknownID;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.ZipUtils;

/**
 * A cache that serves Lucene indices of crawl logs for given job IDs.
 * Uses the DigestIndexer in the deduplicator software:
 * http://deduplicator.sourceforge.net/apidocs/is/hi/bok/deduplicator/DigestIndexer.html
 * Upon combination of underlying files, each file in the Lucene index is
 * gzipped and the compressed versions are stored in the directory given by
 * getCacheFile().
 * The subclass has to determine in its constructor call which mime types are
 * included.
 *
 */
public abstract class CrawlLogIndexCache extends
                CombiningMultiFileBasedCache<Long> implements JobIndexCache {
    /** Needed to find origin information, which is file+offset from CDX index.
     */
    private final CDXDataCache cdxcache = new CDXDataCache();
    /** Optimizes Lucene index, if set to true. */
    private static final boolean OPTIMIZE_INDEX = true;
    /** the useBlacklist set to true results in docs matching the
       mimefilter being ignored. */
    private boolean useBlacklist;
    /** An regular expression for the mimetypes to include or exclude from
     * the index. See useBlackList.
     */
    private String mimeFilter;
    /** The log. */
    private static Log log
            = LogFactory.getLog(CrawlLogIndexCache.class.getName());
    /**
     * Constructor for the CrawlLogIndexCache class.
     * @param name The name of the CrawlLogIndexCache
     * @param blacklist Shall the mimefilter be considered a blacklist 
     *  or a whitelist?
     * @param mimeFilter A regular expression for the mimetypes to
     * exclude/include
     */
    public CrawlLogIndexCache(String name, boolean blacklist,
                              String mimeFilter) {
        super(name, new CrawlLogDataCache());
        useBlacklist = blacklist;
        this.mimeFilter = mimeFilter;
    }

    /** Prepare data for combining.  This class overrides prepareCombine to
     * make sure that CDX data is available.
     *
     * @param ids Set of IDs that will be combined.
     * @return Map of ID->File of data to combine for the IDs where we could
     * find data.
     */
    protected Map<Long, File> prepareCombine(Set<Long> ids) {
        log.info("Starting to generate " + getCacheDir().getName()
                 + " for jobs: " + ids);
        Map<Long, File> returnMap = super.prepareCombine(ids);
        Set<Long> missing = new HashSet<Long>();
        for (Long id : returnMap.keySet()) {
            Long cached = cdxcache.cache(id);
            if (cached == null) {
                missing.add(id);
            }
        }
        if (!missing.isEmpty()) {
            log.info("Data not found for jobs: " +  missing);
        }
        for (Long id : missing) {
            returnMap.remove(id);
        }
        return returnMap;
    }

    /** Combine a number of crawl.log files into one Lucene index.  This index
     * is placed as gzip files under the directory returned by getCacheFile().
     *
     * @param rawfiles The map from job ID into crawl.log contents. No
     * null values are allowed in this map.
     */
    protected void combine(Map<Long, File> rawfiles) {
        File resultFile = getCacheFile(rawfiles.keySet());
        String indexLocation = resultFile.getAbsolutePath() + ".luceneDir";
        try {
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
            DigestIndexer indexer =
                new DigestIndexer(indexLocation,
                                    indexingMode,
                                    includeNormalizedURL,
                                    includeTimestamp,
                                    includeEtag,
                                    addToExistingIndex);

            for (Map.Entry<Long, File> entry : rawfiles.entrySet()) {
                indexFile(entry.getKey(), entry.getValue(), indexer);
            }
            indexer.close(OPTIMIZE_INDEX);
            // Now the index is made, gzip it up.
            ZipUtils.gzipFiles(new File(indexLocation), resultFile);
        } catch (IOException e) {
            throw new IOFailure("Error setting up craw.log index framework for "
                    + resultFile.getAbsolutePath(), e);
        } finally {
            FileUtils.removeRecursively(new File(indexLocation));
        }
    }

    /** Ingest a single crawl.log file using the corresponding CDX file to find
     * offsets.
     *
     * @param id ID of a job to ingest.
     * @param file The file containing the jobs crawl.log data.
     * @param indexer The indexer to add to.
     */
    private void indexFile(Long id, File file, DigestIndexer indexer) {
        // variable 'blacklist' set to true results in docs matching the
        // mimefilter being ignored.
        boolean blacklist = useBlacklist;
        final String mimefilter = mimeFilter;
        final boolean verbose = false; //Avoids System.out.println's
        CrawlDataIterator crawlLogIterator = null;
        File cdxFile = null;
        File tmpCrawlLog = null;
        BufferedReader cdxBuffer = null;
        try {
            cdxFile = getSortedCDX(id);
            cdxBuffer = new BufferedReader(new FileReader(cdxFile));
            tmpCrawlLog = getSortedCrawlLog(file);
            crawlLogIterator = new CDXOriginCrawlLogIterator(
                    tmpCrawlLog, cdxBuffer);
            indexer.writeToIndex(
                    crawlLogIterator, mimefilter, blacklist, "ERROR", verbose);
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
                if (cdxFile != null) {
                    FileUtils.remove(cdxFile);
                }
            } catch (IOException e) {
                log.info("Error cleaning up after"
                        + " crawl log index cache generation", e);
            }
        }
    }

    /** Get a sorted, temporary CDX file for a given job.
     *
     * @param jobid The ID of the job to get a sorted CDX file for.
     * @return A temporary file with CDX info for that just sorted according
     * to the standard CDX sorting rules.  This file will be removed at the
     * exit of the JVM, but should be attempted removed when it is no longer
     * used.
     */
    private File getSortedCDX(long jobid) {
        Long cached = cdxcache.cache(jobid);
        if (cached == null) {
            throw new UnknownID("Couldn't find cache for job " + jobid);
        }
        try {
            final File tmpFile = File.createTempFile("sorted", "cdx",
                    FileUtils.getTempDir());
            // This throws IOFailure, if the sorting operation fails 
            FileUtils.sortCDX(cdxcache.getCacheFile(cached), tmpFile);
            tmpFile.deleteOnExit();
            return tmpFile;
        } catch (IOException e) {
            throw new IOFailure("Error while making tmp file for " + cached, e);
        }
    }

    /** Get a sorted, temporary crawl.log file from an unsorted one.
     *
     * @param file The file containing an unsorted crawl.log file.
     * @return A temporary file containing the entries sorted according to
     * URL.  The file will be removed upon exit of the JVM, but should be
     * attempted removed when it is no longer used.
     */
    private static File getSortedCrawlLog(File file) {
        try {
            File tmpCrawlLog = File.createTempFile("sorted", "crawllog",
                    FileUtils.getTempDir());
            // This throws IOFailure, if the sorting operation fails
            FileUtils.sortCrawlLog(file, tmpCrawlLog);
            tmpCrawlLog.deleteOnExit();
            return tmpCrawlLog;
        } catch (IOException e) {
            throw new IOFailure("Error creating sorted crawl log file for '"
                    + file + "'", e);
        }
    }
}
