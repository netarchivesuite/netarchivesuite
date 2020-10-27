/*
 * #%L
 * Netarchivesuite - common
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

package dk.netarkivet.common.utils.batch;

import java.io.File;
import java.io.OutputStream;
import java.util.Date;
import java.util.HashSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.BatchTermination;
import dk.netarkivet.common.utils.Settings;

/**
 * Class for running FileBatchJobs on a set of local files. The constructor takes an array of files to be processed and
 * the run() method takes a FileBatchJob and applies it to each file in turn.
 */
public class BatchLocalFiles {

    /** The class logger. */
    private static final Logger log = LoggerFactory.getLogger(BatchLocalFiles.class);

    /** The list of files to run batch jobs on. */
    private File[] files;

    /** The last time logging was performed. Initial 0 to ensure logging the first time. */
    private long lastLoggingDate = 0;
    /** The time when the batchjob was started. */
    private long startTime = 0;

    /**
     * Given an array of files, constructs a BatchLocalFiles instance to be used in running a batch job over those
     * files.
     *
     * @param incomingFiles The files that should be used processed by the batchjob
     * @throws ArgumentNotValid if incomingFiles is null or contains a null entry
     */
    public BatchLocalFiles(File[] incomingFiles) throws ArgumentNotValid {
        ArgumentNotValid.checkNotNull(incomingFiles, "incomingFiles");
        for (int i = 0; i < incomingFiles.length; i++) {
            ArgumentNotValid.checkNotNull(incomingFiles[i], "Null element at index " + i + " in file list for batch.");
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
            long logInterval = Settings.getLong(CommonSettings.BATCH_LOGGING_INTERVAL);
            // get the time for starting the batchjob (used for logging).
            startTime = new Date().getTime();
            // Process each file:
            for (File file : files) {
                fileCount++;
                if (job.getFilenamePattern().matcher(file.getName()).matches()) {
                    long currentTime = new Date().getTime();
                    // perform logging if necessary.
                    if (lastLoggingDate + logInterval < currentTime) {
                        log.info(
                                "The batchjob '{}' has run for {} seconds and has reached file '{}', which is number {} out of {}",
                                job.getClass(), (currentTime - startTime) / 1000, file.getName(), fileCount,
                                files.length);
                        // set that we have just logged.
                        lastLoggingDate = currentTime;
                    }
                    processFile(job, file, os);
                }

                // check whether the batchjob should stop.
                if (Thread.currentThread().isInterrupted()) {
                    // log and throw an error (not exception, they are caught!)
                    String errMsg = "The batchjob '" + job.toString() + "' has been interrupted and will terminate!";
                    log.warn(errMsg);
                    // TODO make new exception to thrown instead.
                    throw new BatchTermination(errMsg);
                }
            }
        } catch (Exception e) {
            // TODO Consider adding this initialization exception to the list
            // of exception accumulated:
            // job.addInitializeException(outputOffset, e)
            log.warn("Exception while initializing job {}", job, e);

            // rethrow exception
            if (e instanceof BatchTermination) {
                throw (BatchTermination) e;
            }
        } finally {
            // Finally, allow the job to finish: */
            try {
                job.finish(os);
            } catch (Exception e) {
                // TODO consider adding this finalization exception to the list
                // of exception accumulated:
                // job.addFinishException(outputOffset, e)
                log.warn("Exception while finishing job {}", job, e);

                // rethrow exception
                if (e instanceof BatchTermination) {
                    throw (BatchTermination) e;
                }
            }
        }
    }

    /**
     * Process a single file.
     *
     * @param job The job that does the processing
     * @param file The file to process
     * @param os Where to put the output.
     */
    private void processFile(FileBatchJob job, final File file, OutputStream os) {
        log.trace("Started processing of file '{}'.", file.getAbsolutePath());
        boolean success = false;
        try {
            success = job.processFile(file, os);
        } catch (Exception e) {
            // TODO consider adding this exception to the list
            // of exception accumulated:
            // job.addException(currentFile, currentOffset, outputOffset, e)
            log.warn("Exception while processing file {} with job {}", file, job, e);
        }
        job.noOfFilesProcessed++;
        if (!success) {
            job.filesFailed.add(file);
        }
    }

}
