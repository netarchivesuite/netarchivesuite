/*
 * #%L
 * Netarchivesuite - harvester - test
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

package dk.netarkivet.viewerproxy;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.archive.io.arc.ARCRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.Constants;
import dk.netarkivet.common.distribute.arcrepository.BatchStatus;
import dk.netarkivet.common.distribute.arcrepository.ViewerArcRepositoryClient;
import dk.netarkivet.common.distribute.indexserver.Index;
import dk.netarkivet.common.distribute.indexserver.JobIndexCache;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.NotImplementedException;
import dk.netarkivet.common.utils.ChecksumCalculator;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.ProcessUtils;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.utils.StringUtils;
import dk.netarkivet.common.utils.arc.ARCBatchJob;
import dk.netarkivet.harvester.HarvesterSettings;

/**
 * This class handles retrieval and merging of index.cdx files for sets of jobs.
 * <p>
 * It has been designed to allow multiple instances to use the same cache dir without interfering with each other, even
 * if they run in separate VMs.
 *
 * @deprecated Use {@link JobIndexCache}-mechanism instead
 */
@SuppressWarnings({"unchecked", "rawtypes", "serial"})
public class LocalCDXCache implements JobIndexCache {
    /**
     * Don't put more than this number of job ids in the filename. Above this number, a checksum of the job ids is
     * generated instead. This is done to protect us from getting filenames too long for the filesystem.
     */
    private static final int MAX_JOB_IDS_IN_FILENAME = 4;
    private static final String PREFIX = "job-";
    private static final String SUFFIX = "-index.cdx";

    private final ViewerArcRepositoryClient arcRepos;
    
    private final Logger log = LoggerFactory.getLogger(LocalCDXCache.class);
    private static final String WORK_SUFFIX = ".unsorted";

    /**
     * The directory that we store CDX cache files in. Would like to use a common dir for ViewerProxy, but it is not
     * defined yet.
     */
    private static final File CACHE_DIR = new File(new File(Settings.get(HarvesterSettings.VIEWERPROXY_DIR)),
            "viewerproxy/cdxcache");
    /**
     * How long we sleep between each check for another process having finished creating an index file.
     */
    private static final long SLEEP_INTERVAL = 100;

    /**
     * Construct a new CDXCache object.
     *
     * @param arcRepos Viewer ArcRepositoryClient
     */
    public LocalCDXCache(ViewerArcRepositoryClient arcRepos) {
        this.arcRepos = arcRepos;
        FileUtils.createDir(CACHE_DIR);
    }

    /**
     * Returns the name of the index file for a set of jobIds. This filename must be unique for these IDs and always
     * give the same for the same set of IDs. In this implementation, long lists of IDs will be shortened to the first
     * few IDs followed by an MD5 sum of all the IDs.
     *
     * @param jobIDs Set of job IDs, in no particular order.
     * @return A File that specifies where the index.cdx data for the job IDs should reside. This does not check whether
     * the file exists or even if the directory it belongs to exists.
     */
    private File getIndexFile(Set<Long> jobIDs) {
        List<Long> jobIDList = new ArrayList<Long>(jobIDs);
        Collections.sort(jobIDList);

        String allIDsString = StringUtils.conjoin("-", jobIDList);
        if (jobIDList.size() > MAX_JOB_IDS_IN_FILENAME) {
            String firstNIDs = StringUtils.conjoin("-", jobIDList.subList(0, MAX_JOB_IDS_IN_FILENAME));
            return new File(CACHE_DIR, PREFIX + firstNIDs + "-"
                    + ChecksumCalculator.calculateMd5(allIDsString.getBytes()) + SUFFIX);
        } else {
            return new File(CACHE_DIR, PREFIX + allIDsString + SUFFIX);
        }
    }

    /**
     * Get a job index for the given list of IDs. The resulting file contains a sorted list of the CDX lines for the
     * jobs in question. This method is safe for asynchronous calling. This method may use a cached version of the file.
     *
     * @param jobIDs List of job IDs to generate index for.
     * @return A file containing an index, and always the full set.
     */
    public Index<Set<Long>> getIndex(Set<Long> jobIDs) {
        FileUtils.createDir(CACHE_DIR);
        ArgumentNotValid.checkNotNullOrEmpty(jobIDs, "jobIDs");
        File indexFile = getIndexFile(jobIDs);
        File workFile = new File(indexFile.getAbsolutePath() + WORK_SUFFIX);
        workFile.deleteOnExit();
        try {
            if (workFile.createNewFile()) {
                // Nobody else could create the indexFile now, so check
                // if it exists -- if so, we can just use that.
                // Safer but slower than checking existence twice
                if (indexFile.exists()) {
                    return new Index(indexFile, jobIDs);
                    // workFile deleted in finally.
                }
                OutputStream tmpOutput = new FileOutputStream(workFile);
                retrieveIndex(jobIDs, tmpOutput);
                tmpOutput.close();
                ProcessUtils.runProcess(new String[] {"LANG=C"}, "sort", workFile.getAbsolutePath(), "-o",
                        indexFile.getAbsolutePath());
            } else {
                while (workFile.exists()) {
                    try { // Wait till other process ends
                        Thread.sleep(SLEEP_INTERVAL);
                    } catch (InterruptedException e) {
                        log.debug("Sleep interrupted: ", e);
                    }
                }
            }
        } catch (IOException e) {
            throw new IOFailure("Error while creating index", e);
        } finally {
            FileUtils.remove(workFile);
        }
        if (!indexFile.exists()) {
            throw new IOFailure("Failed to create index file for " + jobIDs);
        }
        // TODO actually, this may not be the right set, it may only be a subset
        // It is not possible to figure out what cdx files were found using only
        // one batch job
        return new Index(indexFile, jobIDs);
    }

    /**
     * Gets and extract index data from metadata for a given file, squirting them into the given outputStream.
     *
     * @param jobIDs A jobId to get index data for
     * @param out An OutputStream to place the data in.
     */
    private void retrieveIndex(Set<Long> jobIDs, OutputStream out) {
        List<String> metadataFiles = new ArrayList<String>();
        for (Long jobID : jobIDs) {
            metadataFiles.add(".*" + jobID + ".*" + Settings.get(CommonSettings.METADATAFILE_REGEX_SUFFIX));
        }
        ARCBatchJob job = new CDXCacheBatchJob();
        job.processOnlyFilesMatching(metadataFiles);
        BatchStatus status = arcRepos.batch(job, Settings.get(CommonSettings.USE_REPLICA_ID));
        if (status.hasResultFile()) {
            status.appendResults(out);
        }
        if (status.getNoOfFilesProcessed() != jobIDs.size()) {
            log.info("Only found " + status.getNoOfFilesProcessed() + " files when asking for jobs " + jobIDs);
        }
    }

    /**
     * A batch job that extracts exactly the index parts of metadata files.
     */
    private static class CDXCacheBatchJob extends ARCBatchJob {
        /**
         * Constructor for CDXCacheBatchJob.
         */
        public CDXCacheBatchJob() {
            batchJobTimeout = 7 * Constants.ONE_DAY_IN_MILLIES;
        }

        /**
         * Initialize the batch job.
         *
         * @param os output stream where output from batch job is returned.
         */
        public void initialize(OutputStream os) {
        }

        /**
         * Routine for a single ARC Record.
         *
         * @param os output stream for output of batch job.
         * @param record the ARC record to work on.
         */
        public void processRecord(ARCRecord record, OutputStream os) {
            if (record.getMetaData().getMimetype().equals(Constants.CDX_MIME_TYPE)) {
                try {
                    int bytesRead;
                    byte[] buffer = new byte[Constants.IO_BUFFER_SIZE];
                    while ((bytesRead = record.read(buffer)) != -1) {
                        os.write(buffer, 0, bytesRead);
                    }
                } catch (IOException e) {
                    throw new IOFailure("Error transferring data from CDX record", e);
                }
            }
        }

        /**
         * Is called when batch job is finished. Nothing to do.
         *
         * @param os output stream for returning output from batchjob.
         */
        public void finish(OutputStream os) {
        }
    }

    @Override
    public void requestIndex(Set<Long> jobSet, Long harvestId) {
        throw new NotImplementedException("Not implemented for this type of cache");

    }
}
