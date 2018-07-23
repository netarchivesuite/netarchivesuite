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
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import is.hi.bok.deduplicator.DigestIndexer;

/**
 * This worker class handles the indexing of one single crawl-log and associated cdxfile.
 */
public class DigestIndexerWorker implements Callable<Boolean> {

    /** The log. */
    private static final Logger log = LoggerFactory.getLogger(DigestIndexerWorker.class);

    /** The full path to the index. */
    private String indexlocation;
    /** The ID of the job which logfiles are being indexed. */
    private Long jobId;
    /** The crawllog from the job. */
    private File crawlLog;
    /** The cdxfile from the job. */
    private File cdxfile;
    /** The options for the indexing process. */
    private DigestOptions indexingOptions;
    /** Was this process successful. */
    private boolean successful = true;
    /** String defining this task among other tasks. */
    private String taskID;

    /**
     * Constructor for the DigestIndexerWorker.
     *
     * @param indexpath The full path to the index
     * @param jobId The ID of the job which logfiles are being indexed
     * @param crawllogfile The crawllog from the job
     * @param cdxFile The cdxfile from the job
     * @param indexingOptions The options for the indexing process.
     * @param taskID string defining this task
     */
    public DigestIndexerWorker(String indexpath, Long jobId, File crawllogfile, File cdxFile,
            DigestOptions indexingOptions, String taskID) {
        ArgumentNotValid.checkNotNullOrEmpty(indexpath, "String indexpath");
        ArgumentNotValid.checkNotNull(crawllogfile, "File crawllogfile");
        ArgumentNotValid.checkNotNull(cdxFile, "File cdxFile");
        ArgumentNotValid.checkNotNull(indexingOptions, "DigestOptions indexingOptions");
        ArgumentNotValid.checkNotNullOrEmpty(taskID, "String taskID");
        this.indexlocation = indexpath;
        this.jobId = jobId;
        this.crawlLog = crawllogfile;
        this.cdxfile = cdxFile;
        this.indexingOptions = indexingOptions;
        this.taskID = taskID;
    }

    /**
     * This method does the actual indexing.
     *
     * @return true, if the indexing completes successfully; otherwise it returns false
     */
    @Override
    public Boolean call() {
        try {
            log.info("Starting subindexing task ({}) of data from job {}", taskID, this.jobId);
            DigestIndexer localindexer = CrawlLogIndexCache.createStandardIndexer(indexlocation);
            CrawlLogIndexCache.indexFile(jobId, crawlLog, cdxfile, localindexer, indexingOptions);

            log.info("Completed subindexing task ({}) of data from job {} w/ {} index-entries)", taskID, this.jobId,
                    localindexer.getIndex().numDocs());

            localindexer.close();
        } catch (Throwable t) {
            successful = false;
            log.warn("Indexing for job w/ id {} failed.", jobId, t);
        }
        return successful;

    }

}
