/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2011 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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
    /** Was this process successfull. */
    private boolean successfull = true;
    /** Optimization when closing index. */
    private boolean optimizeIndex = true;
        
    /**
     * Constructor for the DigestIndexerWorker.
     * @param indexpath The full path to the index
     * @param jobId The ID of the job which logfiles are being indexed
     * @param crawllogfile The crawllog from the job 
     * @param cdxFile The cdxfile from the job
     * @param indexingOptions The options for the indexing process.
     */
    public DigestIndexerWorker(String indexpath, Long jobId, File crawllogfile, 
            File cdxFile, DigestOptions indexingOptions) {
        ArgumentNotValid.checkNotNullOrEmpty(indexpath, "String indexpath");
        ArgumentNotValid.checkNotNull(crawllogfile, "File crawllogfile");
        ArgumentNotValid.checkNotNull(cdxFile, "File cdxFile");
        ArgumentNotValid.checkNotNull(indexingOptions, 
                "DigestOptions indexingOptions");
        this.indexlocation = indexpath;
        this.jobId = jobId;
        this.crawlLog = crawllogfile;
        this.cdxfile = cdxFile;
        this.indexingOptions = indexingOptions;  
    }
    
    /**
     * This method does the actual indexing.
     * @return true, if the indexing completes successfully;
     * otherwise it returns false
     */
    @Override
    public Boolean call() {
        try {
            log.info("Starting subindexing task of data from job "
                    + this.jobId);
            DigestIndexer localindexer
                = CrawlLogIndexCache.createStandardIndexer(indexlocation);
            CrawlLogIndexCache.indexFile(jobId, crawlLog, cdxfile, localindexer,
                    indexingOptions);
            localindexer.close(optimizeIndex);

            log.info("Completed subindexing task of data from job "
                    + this.jobId + " w/ " + localindexer.getIndex().docCount()
                    + " index-entries)");
        } catch (IOException e) {
            successfull = false;
            log.warn("Indexing for job w/ id " + jobId + " failed.", e);
        } catch (IOFailure e) {
            successfull = false;
            log.warn("Indexing for job w/ id " + jobId + " failed.", e); 
        } 
        return successfull;
 
    }
}
