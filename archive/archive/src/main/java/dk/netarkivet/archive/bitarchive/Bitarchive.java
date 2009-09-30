/* File:       $Id$
 * Revision:   $Revision$
 * Author:     $Author$
 * Date:       $Date$
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
package dk.netarkivet.archive.bitarchive;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.archive.io.arc.ARCReader;
import org.archive.io.arc.ARCReaderFactory;
import org.archive.io.arc.ARCRecord;

import dk.netarkivet.common.distribute.RemoteFile;
import dk.netarkivet.common.distribute.RemoteFileFactory;
import dk.netarkivet.common.distribute.arcrepository.BatchStatus;
import dk.netarkivet.common.distribute.arcrepository.BitarchiveRecord;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.PermissionDenied;
import dk.netarkivet.common.exceptions.UnknownID;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.batch.BatchLocalFiles;
import dk.netarkivet.common.utils.batch.FileBatchJob;

/**
 * The central class in the bit archive. Implements the API: upload(), get(),
 * correct(), batch(). A bit archive is expected to not know about any other bit
 * archives, and is not considered responsible for making MD5 checksums.
 */
public class Bitarchive {
    /**
     * Administrative data for the current bitarchive.
     */
    private BitarchiveAdmin admin;

    /**
     * Logging output place.
     */
    protected final Log log = LogFactory.getLog(getClass().getName());

    /** The instance of the bitarchive. */
    private static Bitarchive instance;

    /**
     * Create a new Bitarchive with files stored on local disk in one or more
     * directories. This can reopen an existing bit archive or create a
     * Bitarchive from scratch, with no files on disk.
     *
     * @throws PermissionDenied
     *             if creating directory fails.
     */
    private Bitarchive() throws PermissionDenied {
        log.debug("Starting bit archive");
        admin = BitarchiveAdmin.getInstance();
    }

    /**
     * Release all resources allocated by the bitarchive Ensures that all admin
     * data and log data are flushed.
     */
    public void close() {
        admin.close();
        instance = null;
    }

    /**
     * Get an ARC record out of the archive. Returns null if the arcfile is
     * not found in this bitarchive.
     *
     * @param arcfile
     *            The name of an ARC file.
     * @param index
     *            Index of the ARC record in the file
     * @return A BitarchiveRecord object for the record in question. This record
     *         contains the data from the file.
     * @throws ArgumentNotValid
     *             If arcfile is null/empty, or if index is out of bounds
     * @throws IOFailure
     *             If there were problems reading the arcfile.
     * @throws UnknownID Does it really, and when ?
     */
    public BitarchiveRecord get(String arcfile, long index)
            throws ArgumentNotValid, UnknownID, IOFailure {
        /*
         * TODO: Change return type into RemoteFile. This should only cause
         * changes in GetFileMessage.
         */
        log.info("GET: " + arcfile + ":" + index);
        ArgumentNotValid.checkNotNullOrEmpty(arcfile, "arcfile");
        BitarchiveARCFile barc = admin.lookup(arcfile);
        if (barc == null) {
            log.debug("Get request for file not on this machine: " + arcfile);
            return null;
        }
        ARCReader arcReader = null;
        ARCRecord arc = null;
        try {
            if ((barc.getSize() <= index) || (index < 0)) {
                String s = "GET: index out of bounds: " + arcfile + ":" + index
                        + " > " + barc.getSize();
                log.warn(s);
                throw new ArgumentNotValid(s);
            }
            File in = barc.getFilePath();
            arcReader = ARCReaderFactory.get(in);
            arc = (ARCRecord) arcReader.get(index);
            BitarchiveRecord result = new BitarchiveRecord(arc);

            // release resources locked
            log.info("GET: Got " + result.getLength()
                    + " bytes of data from " + arcfile + ":" + index);
            // try {
            // Thread.sleep(1000);
            // } catch (InterruptedException e) {
            //
            // }
            return result;
        } catch (IOException e) {
            final String msg = "Could not get data from " + arcfile + " at: "
                    + index + "; Stored at: " + barc.getFilePath();
            log.warn(msg);
            throw new IOFailure(msg, e);
        } catch (IndexOutOfBoundsException e) {
            final String msg = "Could not get data from " + arcfile + " at: "
                    + index + "; Stored at: " + barc.getFilePath();
            log.warn(msg);
            throw new IOFailure(msg, e);
        } finally {
            try {
                if (arc != null) {
                    arc.close();
                }
                if (arcReader != null) {
                    arcReader.close();
                }
            } catch (IOException e) {
                log.warn("Could not close ARCReader or ARCRecord: " + e);
            }
        }
    }

    /**
     * Upload an ARC file to this archive.
     *
     * @param arcfile
     *            A file to add to the archive.
     * @param fileName
     *            the arcfiles filename.  The file will be identified in
     *            the archive by this filename
     * @throws PermissionDenied
     *             if arcfile already exists in the archive
     * @throws IOFailure
     *             if an IO failure occurs (e.g. running out of disk space)
     * @throws ArgumentNotValid
     *             if arcfile is null or the filename is null or empty.
     */
    public void upload(RemoteFile arcfile, String fileName)
            throws PermissionDenied, ArgumentNotValid, IOFailure {
        log.info("Upload: " + arcfile);
        // Verify input parameters
        ArgumentNotValid.checkNotNull(arcfile, "arcfile");
        ArgumentNotValid.checkNotNullOrEmpty(fileName, "fileName");

        // Check if file already exists in the archive
        if (admin.lookup(fileName) != null) {
            String errMsg = "Upload: file already exists: '" + fileName
                            + "' while uploading '" + arcfile + "'.";
            log.warn(errMsg);
            throw new PermissionDenied(errMsg);
        }

        // Everything seems ok, initiate copy of file into archive
        copyRemoteFileToArchive(arcfile, fileName);
        log.info("Upload: completed uploading " + fileName);
    }

    /**
     * Run a batch job on all ARC entries in the archive. <p/> This currently
     * runs synchronously, and returns only after finish() has been called.
     *
     * @param bitarchiveAppId
     *            A String representing the bitarchive AppId.
     * @param job
     *            An object that implements the ARCBatchJob interface. The
     *            initialize() method will be called before processing and the
     *            finish() method will be called afterwards. The process()
     *            method will be called with each ARC entry.
     * @throws ArgumentNotValid
     *             if job or file is null.
     * @throws IOFailure
     *             if there was problems writing to the RemoteFile
     * @return A localBatchStatus
     */
    public BatchStatus batch(String bitarchiveAppId, final FileBatchJob job) {
        ArgumentNotValid.checkNotNullOrEmpty(
                bitarchiveAppId, "String bitarchiveAppId");
        ArgumentNotValid.checkNotNull(job, "FileBatchJob job");
        log.info("Starting batch job on bitarchive application with id '"
                + bitarchiveAppId + "': " + job.getClass().getName());
        BatchStatus returnStatus;

        File tmpFile = null;
        try {
            tmpFile = File.createTempFile("BatchOutput", "",
                    FileUtils.getTempDir());
            final OutputStream os = new FileOutputStream(tmpFile);

            try {
                // Run the batch job
                log.debug("Batch: Job " + job + " started at "
                        + new Date());
                File[] processFiles
                        = admin.getFilesMatching(job.getFilenamePattern());
                final BatchLocalFiles localBatchRunner =
                    new BatchLocalFiles(processFiles);
                localBatchRunner.run(job, os);
                log.debug("Batch: Job " + job + " finished at "
                        + new Date());
            } finally { // Make sure the OutputStream is closed no matter what.
                // This allows us to delete the file on Windows
                // in case of error.
                try {
                    os.close();
                } catch (IOException e) {
                    // We're cleaning up, failing to close won't stop us
                    log.warn(
                            "Failed to close outputstream in batch");
                }
            }
            // write output from batch job back to remote file
            returnStatus = new BatchStatus(bitarchiveAppId,
                    job.getFilesFailed(),
                    job.getNoOfFilesProcessed(),
                    RemoteFileFactory.getMovefileInstance(tmpFile),
                    job.getExceptions());
        } catch (IOException e) {
            log.fatal("Failed to create temporary file for batch "
                    + job, e);
            throw new IOFailure("Failed to create temporary file for batch "
                    + job, e);
        }
        log.info("Finished batch job " + job.getClass().getName()
                 + " with result: " + returnStatus);
        return returnStatus;
    }

    /**
     * Copies a remote file into the bitarchive storage and returns the storage
     * position of the file.
     *
     * @param arcfile
     *            The source file.
     * @param fileName
     *            the source files filename.
     * @return the storage position of the file.
     * @throws IOFailure
     *             if an error occurs while copying into the archive.
     */
    private File copyRemoteFileToArchive(RemoteFile arcfile, String fileName)
            throws IOFailure {
        File temp_destination
                = admin.getTemporaryPath(fileName, arcfile.getSize());
        File destination = null;
        try {
            //The file is first copied to a temporary destination on the same
            //mount. The reason for this is to eliminate that there are files
            //in the file-directory that are currupted because of upload 
            //errors. For example if the there is a break down after only half 
            //the file is uploaded. It also means that we do not need to clean 
            //up in the file directory, in case of failure - only the temporary 
            //destination needs clean up.
            arcfile.copyTo(temp_destination);
            //Note that the move operation is a constant time operation within
            //the same mount
            destination = admin.moveToStorage(temp_destination);
        } catch (Throwable e) {
            // destination is known to be null here, so don't worry about it.
            if (temp_destination.exists()) {
                temp_destination.delete();
            }
            throw new IOFailure("Can't copy file into archive: " + fileName, e);
        }
        return destination;
    }

    /**
     * Get a file for a given arcFileID.
     * @param arcFileID
     *            name of the file to be retrieved.
     * @return The file requested or null if not found
     * @throws ArgumentNotValid If arcFileID was null or empty.
     */
    public File getFile(String arcFileID) throws ArgumentNotValid {
        log.info("Get file '" + arcFileID + "'");
        ArgumentNotValid.checkNotNullOrEmpty(arcFileID, "arcFileID");
        BitarchiveARCFile barc = admin.lookup(arcFileID);
        if (barc == null) { // the file with ID: arcFileID was not found
            log.debug("File '" + arcFileID + "' not found on this machine");
            return null;
        }

        File path = barc.getFilePath();
        log.info("Getting file '" + path + "'");
        return path;
    }

    /**
     * Get the one instance of the bitarchive.
     *
     * @return An instance of the Bitarchive class.
     * @throws PermissionDenied
     *             If the storage area used for files is not accessible.
     */
    public static Bitarchive getInstance() throws PermissionDenied {
        if (instance == null) {
            instance = new Bitarchive();
        }
        return instance;
    }
}
