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
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.utils.StringUtils;

/**
 * Interface defining a batch job to run on a set of files. The job is initialized by calling initialize(), executed on
 * a file by calling processFile() and any cleanup is handled by finish().
 */
@SuppressWarnings({"serial"})
public abstract class FileBatchJob implements BatchJob, Serializable {

    /** The class log. */
    private static final Logger log = LoggerFactory.getLogger(FileBatchJob.class);

    /**
     * Regular expression for the files to process with this job. By default, all files are processed. This pattern must
     * match the entire filename, but not the path (e.g. .*foo.* for any file with foo in it).
     */
    private Pattern filesToProcess = Pattern.compile(EVERYTHING_REGEXP);

    /** The total number of files processed (including any that generated errors). */
    protected int noOfFilesProcessed = 0;

    /**
     * If positiv it is the timeout of specific Batch Job in miliseconds. If numbers is negative we use standard timeout
     * from settings.
     */
    protected long batchJobTimeout = -1;

    /** A Set of files which generated errors. */
    protected Collection<URI> filesFailed = new HashSet<>();

    /** A list with information about the exceptions thrown during the execution of the batchjob. */
    protected List<ExceptionOccurrence> exceptions = new ArrayList<ExceptionOccurrence>();

    /**
     * Get the pattern for files that should be processed.
     *
     * @return A pattern for files to process.
     */
    public Pattern getFilenamePattern() {
        return filesToProcess;
    }

    /**
     * Return the number of files processed in this job.
     *
     * @return the number of files processed in this job
     */
    public int getNoOfFilesProcessed() {
        return noOfFilesProcessed;
    }

    public void setNoOfFilesProcessed(int noOfFilesProcessed) {
        this.noOfFilesProcessed = noOfFilesProcessed;
    }

    /**
     * Return the list of names of files where processing failed. An empty list is returned, if none failed.
     *
     * @return the possibly empty list of names of files where processing failed
     */
    public Collection<URI> getFilesFailed() {
        return filesFailed;
    }

    public void setFilesFailed(Collection<URI> filesFailed) {
        this.filesFailed = filesFailed;
    }

    /**
     * Get the list of exceptions that have occurred during processing.
     *
     * @return List of exceptions together with information on where they happened.
     */
    public List<ExceptionOccurrence> getExceptions() {
        return exceptions;
    }


    @Override
    public void setFilesToProcess(Pattern compile) {
        filesToProcess = compile;
    }

    /**
     * Record an exception that occurred during the processFile of this job and that should be returned with the result.
     * If maxExceptionsReached() returns true, this method silently does nothing.
     *
     * @param currentFile The file that is currently being processed.
     * @param currentOffset The relevant offset into the file when the exception happened (e.g. the start of an ARC
     * record).
     * @param outputOffset The offset we were at in the outputstream when the exception happened. If UNKNOWN_OFFSET, the
     * offset could not be found.
     * @param e The exception thrown. This exception must be serializable.
     */
    protected void addException(File currentFile, long currentOffset, long outputOffset, Exception e) {
        if (!maxExceptionsReached()) {
            exceptions.add(new ExceptionOccurrence(currentFile, currentOffset, outputOffset, e));
        } else {
            if (log.isTraceEnabled()) {
                log.trace("Exception not added, because max exceptions reached. currentFile = {},currentOffset = {},"
                        + "outputOffset = {}, exception: ", currentFile.getAbsolutePath(), currentOffset, outputOffset,
                        e);
            }
        }
    }

    /**
     * Record an exception that occurred during the initialize() method of this job.
     *
     * @param outputOffset The offset we were at in the outputstream when the exception happened. If UNKNOWN_OFFSET, the
     * offset could not be found.
     * @param e The exception thrown. This exception must be serializable.
     */
    protected void addInitializeException(long outputOffset, Exception e) {
        if (!maxExceptionsReached()) {
            exceptions.add(new ExceptionOccurrence(true, outputOffset, e));
        } else {
            log.trace("Exception not added, because max exceptions reached. outputOffset = {}, exception: ",
                    outputOffset, e);
        }
    }

    /**
     * Record an exception that occurred during the finish() method of this job.
     *
     * @param outputOffset The offset we were at in the outputstream when the exception happened. If UNKNOWN_OFFSET, the
     * offset could not be found.
     * @param e The exception thrown. This exception must be serializable.
     */
    protected void addFinishException(long outputOffset, Exception e) {
        if (!maxExceptionsReached()) {
            exceptions.add(new ExceptionOccurrence(false, outputOffset, e));
        } else {
            log.trace("Exception not added, because max exceptions reached. outputOffset = {}, exception: ",
                    outputOffset, e);
        }
    }

    /**
     * Getter for batchJobTimeout. If the batchjob has not defined a maximum time (thus set the value to -1) then the
     * default value from settings are used.
     *
     * @return timeout in miliseconds.
     */
    public long getBatchJobTimeout() {
        if (batchJobTimeout != -1) {
            return batchJobTimeout;
        } else {
            return Long.parseLong(Settings.get(CommonSettings.BATCH_DEFAULT_TIMEOUT));
        }
    }

    /**
     * Returns true if we have already recorded the maximum number of exceptions. At this point, no more exceptions will
     * be recorded, and processing should be aborted.
     *
     * @return True if the maximum number of exceptions (MAX_EXCEPTIONS) has been recorded already.
     */
    protected boolean maxExceptionsReached() {
        return exceptions.size() >= ExceptionOccurrence.MAX_EXCEPTIONS;
    }

    /**
     * Override predefined timeout period for batchjob.
     *
     * @param batchJobTimeout timout period
     */
    public void setBatchJobTimeout(long batchJobTimeout) {
        this.batchJobTimeout = batchJobTimeout;
    }


    /**
     * Process
     *
     * @param os the OutputStream to which output should be written
     * @return true if the file was successfully processed, false otherwise
     */
    public abstract boolean processFile(File file, OutputStream os);



}
