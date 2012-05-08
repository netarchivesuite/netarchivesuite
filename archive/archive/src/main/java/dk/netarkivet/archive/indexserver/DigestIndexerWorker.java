/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2012 The Royal Danish Library, the Danish State and
 * University Library, the National Library of France and the Austrian
 * National Library.
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

import is.hi.bok.deduplicator.DigestIndexer;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Callable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;

/** 
 * This worker class handles the indexing of one single crawl-log 
 * and associated cdxfile. 
 */ 
public class DigestIndexerWorker implements Callable<Boolean> {

    /** The log. */
    private static Log log
            = LogFactory.getLog(DigestIndexer.class.getName());
    /** The full path to the index. */
    private String indexlocation;
    /** The ID of the job which logfiles are being indexed. */
    private Long jobId;
    /** The crawllog from the job. */
    private File crawlLog;
    /** The cdxfile from the job.*/
    private File cdxfile;
    /** The options for the indexing process. */
    private DigestOptions indexingOptions;
    /** Was this process successful. */
    private boolean successful = true;
    /** String defining this task among other tasks. */
    private String taskID;
        
    /**
     * Constructor for the DigestIndexerWorker.
     * @param indexpath The full path to the index
     * @param jobId The ID of the job which logfiles are being indexed
     * @param crawllogfile The crawllog from the job 
     * @param cdxFile The cdxfile from the job
     * @param indexingOptions The options for the indexing process.
     * @param taskID string defining this task
     */
    public DigestIndexerWorker(String indexpath, Long jobId, File crawllogfile, 
            File cdxFile, DigestOptions indexingOptions, String taskID) {
        ArgumentNotValid.checkNotNullOrEmpty(indexpath, "String indexpath");
        ArgumentNotValid.checkNotNull(crawllogfile, "File crawllogfile");
        ArgumentNotValid.checkNotNull(cdxFile, "File cdxFile");
        ArgumentNotValid.checkNotNull(indexingOptions, 
                "DigestOptions indexingOptions");
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
     * @return true, if the indexing completes successfully;
     * otherwise it returns false
     */
    @Override
    public Boolean call() {
        try {
            log.info("Starting subindexing task (" + taskID + ") of data from job "
                    + this.jobId);
            DigestIndexer localindexer
                = CrawlLogIndexCache.createStandardIndexer(indexlocation);
            CrawlLogIndexCache.indexFile(jobId, crawlLog, cdxfile, localindexer,
                    indexingOptions);
            localindexer.close(indexingOptions.getOptimizeIndex());
            log.info("Completed subindexing task (" + taskID + ") of data from job "
                    + this.jobId + " w/ " + localindexer.getIndex().numDocs()        
                    + " index-entries)");
        } catch (IOException e) {
            successful = false;
            log.warn("Indexing for job w/ id " + jobId + " failed.", e);
        } catch (IOFailure e) {
            successful = false;
            log.warn("Indexing for job w/ id " + jobId + " failed.", e); 
        } 
        return successful;
 
    }
}
