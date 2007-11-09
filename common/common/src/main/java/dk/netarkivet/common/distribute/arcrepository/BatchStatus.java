/* File:        $Id$
 * Revision:    $Revision$
 * Date:        $Date$
 * Author:      $Author$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2007 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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
package dk.netarkivet.common.distribute.arcrepository;

import java.io.File;
import java.io.OutputStream;
import java.util.Collection;

import dk.netarkivet.common.distribute.RemoteFile;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IllegalState;


/**
 * Class for transferring batch status information.
 *
 */
public class BatchStatus {
    private final int noOfFilesProcessed;
    private final Collection<File> filesFailed;
    private final String bitArchiveAppId;
    private RemoteFile resultFile;

    /** Create a new BatchStatus object for a specific bitarchive.
     *
     * @param bitArchiveAppId
     * @param filesFailed
     * @param noOfFilesProcessed
     * @param resultFile
     */
    public BatchStatus(String bitArchiveAppId,
                       Collection<File> filesFailed,
                       int noOfFilesProcessed,
                       RemoteFile resultFile) {
        this.bitArchiveAppId = bitArchiveAppId;
        this.filesFailed = filesFailed;
        this.noOfFilesProcessed = noOfFilesProcessed;
        this.resultFile = resultFile;
    }

    /** Create a new BatchStatus object for a specific bitarchive.
     *
     * @param filesFailed
     * @param noOfFilesProcessed
     * @param resultFile
     */
    public BatchStatus(Collection<File> filesFailed,
                       int noOfFilesProcessed,
                       RemoteFile resultFile) {
        this("ALL_BITARCHIVE_APPS", filesFailed, noOfFilesProcessed,
                resultFile);
    }

    /** Get the number of files processed by the batch job.  This counts all
     * files whether failed or not.
     *
     * @return number of files passed to processFile
     */
    public int getNoOfFilesProcessed() {
        return noOfFilesProcessed;
    }

    /** Get the File objects for the files that failed.
     *
     * @return A collection containing the files that processFile
     *          returned false on.
     */
    public Collection<File> getFilesFailed() {
        return filesFailed;
    }

    /** Get the appId (internal string) for the bitarchive that these are
     * the results from.
     * Set to ALL_BITARCHIVE_APPS if this it the combined status.
     *
     * @return A uniquely identifying string that should *not* be parsed
     */
    public String getBitArchiveAppId() {
        return bitArchiveAppId;
    }

    /** Get the file containing results from a batch job. This may be null,
     * if the batch job resulted in errors.
     *
     * @return A remote file containing results in some job-specific format.
     */
    public RemoteFile getResultFile() {
        return resultFile;
    }

    /** Copy the results of a batch job into a local file.  This deletes the
     * file from the remote server as appropriate. Note that this method or
     * appendResults can only be called once on a given object. If
     * hasResultFile() returns true, this method is safe to call.
     *
     * @param targetFile File to copy the results into. This file will be
     *  overridden if hasResultFile() returns true;
     * @throws IllegalState If the results have already been copied, or there
     * are no results to copy due to errors.
     */
    public void copyResults(File targetFile) throws IllegalState {
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
            throw new IllegalState("No results to copy into '" + targetFile
                    + "' from batch job on '" + bitArchiveAppId + "' ("
                    + filesFailed.size() + " failures in "
                    + noOfFilesProcessed + " processed files)");
        }
    }

    /** Append the results of a batch job into a stream.  This deletes the
     * results file from the remote server, so this or copyResults can only
     * be called once.  If hasResultFile() returns true, this method is
     * safe to call.
     *
     * @param stream A stream to append results to.
     * @throws IllegalState If the results have already been copied, or there
     * are no results to copy due to errors.
     */
    public void appendResults(OutputStream stream) throws IllegalState {
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
            throw new IllegalState("No results to append to '" + stream
                    + "' from batch job on '" + bitArchiveAppId + "' ("
                    + filesFailed.size() + " failures in "
                    + noOfFilesProcessed + " processed files)");
        }
    }

    /** Returns true if this object has a result file.  There is no result file
     * if no bitarchives succeeded in processing any files, or if the result
     * file sent has already been deleted (e.g., by calling copyResults or
     * appendResults).
     *
     * @return True if this object has a result file.
     */
    public boolean hasResultFile() {
        return resultFile != null;
    }
}
