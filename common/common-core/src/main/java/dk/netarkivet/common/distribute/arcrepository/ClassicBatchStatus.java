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
package dk.netarkivet.common.distribute.arcrepository;

import java.io.File;
import java.io.OutputStream;
import java.net.URI;
import java.util.Collection;
import java.util.List;

import dk.netarkivet.common.distribute.RemoteFile;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IllegalState;
import dk.netarkivet.common.utils.batch.BatchJob;
import dk.netarkivet.common.utils.batch.FileBatchJob;

/**
 * Class for transferring batch status information.
 */
public class ClassicBatchStatus implements BatchStatus {

    /** The total number of files processed so far. */
    private final int noOfFilesProcessed;
    /** A list of files that the batch job could not process. */
    private final Collection<URI> filesFailed;
    /** The application ID identifying the bitarchive, that run this batch job. */
    private final String bitArchiveAppId;
    /** The file containing the result of the batch job. */
    private RemoteFile resultFile;

    /** A list of exceptions caught during the execution of the batchJob. */
    private final List<BatchJob.ExceptionOccurrence> exceptions;

    /**
     * Create a new BatchStatus object for a specific bitarchive.
     *
     * @param bitArchiveAppId The application ID identifying the bitarchive, that run this batch job.
     * @param filesFailed A list of files that the batch job could not process.
     * @param noOfFilesProcessed The total number of files processed
     * @param resultFile A file containing the result of the batch job
     * @param exceptions A list of exceptions caught during the execution of the batchJob
     */
    public ClassicBatchStatus(String bitArchiveAppId, Collection<URI> filesFailed, int noOfFilesProcessed,
            RemoteFile resultFile, List<FileBatchJob.ExceptionOccurrence> exceptions) {
        this.bitArchiveAppId = bitArchiveAppId;
        this.filesFailed = filesFailed;
        this.noOfFilesProcessed = noOfFilesProcessed;
        this.resultFile = resultFile;
        this.exceptions = exceptions;
    }

    /**
     * Create a new BatchStatus object for a specific bitarchive.
     *
     * @param filesFailed A list of files that the batch job could not process
     * @param noOfFilesProcessed The total number of files processed
     * @param resultFile A file containing the result of the batch job
     * @param exceptions A list of exceptions caught during the execution of the batchJob
     */
    public ClassicBatchStatus(Collection<URI> filesFailed, int noOfFilesProcessed, RemoteFile resultFile,
            List<FileBatchJob.ExceptionOccurrence> exceptions) {
        this("ALL_BITARCHIVE_APPS", filesFailed, noOfFilesProcessed, resultFile, exceptions);
    }


    @Override public int getNoOfFilesProcessed() {
        return noOfFilesProcessed;
    }


    @Override public Collection<URI> getFilesFailed() {
        return filesFailed;
    }


    @Override public String getBitArchiveAppId() {
        return bitArchiveAppId;
    }


    @Override public RemoteFile getResultFile() {
        return resultFile;
    }


    @Override public List<BatchJob.ExceptionOccurrence> getExceptions() {
        return exceptions;
    }


    @Override public void copyResults(File targetFile) throws IllegalState {
        ArgumentNotValid.checkNotNull(targetFile, "targetFile");
        if (resultFile != null) {
            try {
                resultFile.copyTo(targetFile);
            } finally {
                RemoteFile tmpResultFile = resultFile;
                resultFile = null;
                tmpResultFile.cleanup();
            }
        } else {
            throw new IllegalState("No results to copy into '" + targetFile + "' from batch job on '" + bitArchiveAppId
                    + "' (" + filesFailed.size() + " failures in " + noOfFilesProcessed + " processed files)");
        }
    }


    @Override public void appendResults(OutputStream stream) throws IllegalState {
        ArgumentNotValid.checkNotNull(stream, "OutputStream stream");
        if (resultFile != null) {
            try {
                resultFile.appendTo(stream);
            } finally {
                RemoteFile tmpResultFile = resultFile;
                resultFile = null;
                tmpResultFile.cleanup();
            }
        } else {
            throw new IllegalState("No results to append to '" + stream + "' from batch job on '" + bitArchiveAppId
                    + "' (" + filesFailed.size() + " failures in " + noOfFilesProcessed + " processed files)");
        }
    }


    @Override public boolean hasResultFile() {
        return resultFile != null;
    }

    /**
     * Returns a human-readable description of this object. The value returned should not be machine-processed, as it is
     * subject to change without notice.
     *
     * @return Human-readable description of this object.
     */
    public String toString() {
        return getFilesFailed().size() + " failures in processing " + getNoOfFilesProcessed() + " files at "
                + getBitArchiveAppId();
    }

}
