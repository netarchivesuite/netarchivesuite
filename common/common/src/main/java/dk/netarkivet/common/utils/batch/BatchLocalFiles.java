/* File:    $Id$
 * Version: $Revision$
 * Date:    $Date$
 * Author:  $Author$
*
* The Netarchive Suite - Software to harvest and preserve websites
* Copyright 2004-2009 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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

package dk.netarkivet.common.utils.batch;

import java.io.File;
import java.io.OutputStream;
import java.util.Date;
import java.util.HashSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.utils.Settings;

/**
 * Class for running FileBatchJobs on a set of local files. The constructor
 * takes an array of files to be processed and the run() method takes a
 * FileBatchJob and applies it to each file in turn.
 */
public class BatchLocalFiles {
    /** The list of files to run batch jobs on. */
    private File[] files;
    /** The class logger. */
    private Log log = LogFactory.getLog(BatchLocalFiles.class);
    /** 
     * The last time logging was performed. 
     * Initial 0 to ensure logging the first time.
     */
    private long lastLoggingDate = 0;
    /** The time when the batchjob was started.*/
    private long startTime = 0;

    /**
     * Given an array of files, constructs a BatchLocalFiles instance
     * to be used in running a batch job over those files.
     *
     * @param incomingFiles The files that should be used processed
     * by the batchjob
     * @throws ArgumentNotValid if incomingFiles is null or contains
     * a null entry
     */
    public BatchLocalFiles(File[] incomingFiles) throws ArgumentNotValid {
        ArgumentNotValid.checkNotNull(incomingFiles, "incomingFiles");
        for (int i = 0; i < incomingFiles.length; i++) {
            ArgumentNotValid.checkNotNull(incomingFiles[i],
                    "Null element at index " + i + " in file list for batch.");
        }
        this.files = incomingFiles;
    }

    /**
     * Run the given job on the files associated with this object.
     *
     * @param job - the job to be executed
     * @param os - the OutputStream to which output data is written
     */
    public void run(FileBatchJob job, OutputStream os) {
        ArgumentNotValid.checkNotNull(job, "FileBatchJob job");
        ArgumentNotValid.checkNotNull(os, "OutputStream os");
        // Initialise the job:
        job.noOfFilesProcessed = 0;
        job.filesFailed = new HashSet<File>();
        try {
            job.initialize(os);
            // count the files (used for logging).
            int fileCount = 0;
            // the time in milliseconds between the status logging
            long logInterval = Settings.getLong(
                    CommonSettings.BATCH_LOGGING_INTERVAL);
            // get the time for starting the batchjob (used for logging).
            startTime = new Date().getTime();
            //Process each file:
            for (File file : files) {
                fileCount++;
                if (job.getFilenamePattern().matcher(file.getName())
                        .matches()) {
                    long currentTime = new Date().getTime();
                    // perform logging if necessary.
                    if(lastLoggingDate + logInterval < currentTime) {
                        log.info("The batchjob '" + job.getClass() 
                                + "' has run for " 
                                + (currentTime-startTime)/1000 + " seconds and "
                                + "has reached file '" + file.getName() 
                                + "' which is number " + fileCount + " out of " 
                                + files.length);
                        // set that we have just logged.
                        lastLoggingDate = currentTime;
                    }
                    processFile(job, file, os);
                }
            }
        } catch (Exception e) {
            // TODO Consider adding this initialization exception to the list
            // of exception accumulated:
            // job.addInitializeException(outputOffset, e)
            log.warn("Exception while initializing job " + job, e);
        } finally {
            // Finally, allow the job to finish: */
            try {
                job.finish(os);
            } catch (Exception e) {
                // TODO consider adding this finalization exception to the list
                // of exception accumulated:
                // job.addFinishException(outputOffset, e)
                log.warn("Exception while finishing job " + job, e);
            }
        }
    }

    /** Process a single file.
     *
     * @param job The job that does the processing
     * @param file The file to process
     * @param os Where to put the output.
     */
    private void processFile(FileBatchJob job, final File file,
            OutputStream os) {
        log.debug("Started processing of file '" +  file.getAbsolutePath()
                + "'.");
        boolean success = false;
        try {
            success = job.processFile(file, os);
        } catch (Exception e) {
            // TODO consider adding this exception to the list
            // of exception accumulated:
            // job.addException(currentFile, currentOffset, outputOffset, e)
            log.warn("Exception while processing file " + file
                     + " with job " + job, e);
        }
        job.noOfFilesProcessed++;
        if (!success) {
            job.filesFailed.add(file);
        }
    }
}