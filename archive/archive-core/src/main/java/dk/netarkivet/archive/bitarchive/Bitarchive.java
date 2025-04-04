/*
 * #%L
 * Netarchivesuite - archive
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
package dk.netarkivet.archive.bitarchive;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Date;

import org.archive.io.ArchiveReader;
import org.archive.io.ArchiveReaderFactory;
import org.archive.io.ArchiveRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
 * The central class in the bit archive. Implements the API: upload(), get(), correct(), batch(). A bit archive is
 * expected to not know about any other bit archives, and is not considered responsible for making MD5 checksums.
 */
public class Bitarchive {

    /** Administrative data for the current bitarchive. */
    private BitarchiveAdmin admin;

    /** Logging output place. */
    protected static final Logger log = LoggerFactory.getLogger(Bitarchive.class);

    /** The instance of the bitarchive. */
    private static Bitarchive instance;

    /**
     * Create a new Bitarchive with files stored on local disk in one or more directories. This can reopen an existing
     * bit archive or create a Bitarchive from scratch, with no files on disk.
     *
     * @throws PermissionDenied if creating directory fails.
     */
    private Bitarchive() throws PermissionDenied {
        log.debug("Starting bit archive");
        admin = BitarchiveAdmin.getInstance();
    }

    /**
     * Release all resources allocated by the bitarchive Ensures that all admin data and log data are flushed.
     */
    public void close() {
        admin.close();
        instance = null;
    }

    /**
     * Get an ARC or WARC record out of the archive. Returns null if the archive file is not found in this bitarchive.
     *
     * @param arcfile The name of an Archive file.
     * @param index Index of the Archive record in the file
     * @return A BitarchiveRecord object for the record in question. This record contains the data from the file.
     * @throws ArgumentNotValid If arcfile is null/empty, or if index is out of bounds
     * @throws IOFailure If there were problems reading the arcfile.
     * @throws UnknownID Does it really, and when ?
     */
    public BitarchiveRecord get(String arcfile, long index) throws ArgumentNotValid, UnknownID, IOFailure {
        /*
         * TODO Change return type into RemoteFile. This should only cause changes in GetFileMessage.
         */
        log.info("GET: {}:{}", arcfile, index);
        ArgumentNotValid.checkNotNullOrEmpty(arcfile, "arcfile");
        BitarchiveARCFile barc = admin.lookup(arcfile);
        if (barc == null) {
            log.debug("Get request for file not on this machine: {}", arcfile);
            return null;
        }
        ArchiveReader arcReader = null;
        ArchiveRecord arc = null;
        try {
            if ((barc.getSize() <= index) || (index < 0)) {
                log.warn("GET: index out of bounds: {}:{} > {}", arcfile, index, barc.getSize());
                throw new ArgumentNotValid("GET: index out of bounds: " + arcfile + ":" + index + " > "
                        + barc.getSize());
            }
            File in = barc.getFilePath();
            arcReader = ArchiveReaderFactory.get(in);
            arc = arcReader.get(index);
            BitarchiveRecord result = new BitarchiveRecord(arc, arcfile);

            // release resources locked
            log.info("GET: Got {} bytes of data from {}:{}", result.getLength(), arcfile, index);
            // try {
            // Thread.sleep(1000);
            // } catch (InterruptedException e) {
            //
            // }
            return result;
        } catch (IOException e) {
            log.warn("Could not get data from {} at: {}; Stored at: {}", arcfile, index, barc.getFilePath());
            throw new IOFailure("Could not get data from " + arcfile + " at: " + index + "; Stored at: "
                    + barc.getFilePath(), e);
        } catch (IndexOutOfBoundsException e) {
            log.warn("Could not get data from {} at: {}; Stored at: {}", arcfile, index, barc.getFilePath());
            throw new IOFailure("Could not get data from " + arcfile + " at: " + index + "; Stored at: "
                    + barc.getFilePath(), e);
        } finally {
            try {
                if (arc != null) {
                    arc.close();
                }
                if (arcReader != null) {
                    arcReader.close();
                }
            } catch (IOException e) {
                log.warn("Could not close ARCReader or ARCRecord!", e);
            }
        }
    }

    /**
     * Upload an ARC file to this archive.
     *
     * @param arcfile A file to add to the archive.
     * @param fileName the arcfiles filename. The file will be identified in the archive by this filename
     * @throws PermissionDenied if arcfile already exists in the archive
     * @throws IOFailure if an IO failure occurs (e.g. running out of disk space)
     * @throws ArgumentNotValid if arcfile is null or the filename is null or empty.
     */
    public void upload(RemoteFile arcfile, String fileName) throws PermissionDenied, ArgumentNotValid, IOFailure {
        log.info("Upload: {}", arcfile);
        // Verify input parameters
        ArgumentNotValid.checkNotNull(arcfile, "arcfile");
        ArgumentNotValid.checkNotNullOrEmpty(fileName, "fileName");

        // Check if file already exists in the archive
        if (admin.lookup(fileName) != null) {
            log.warn("Upload: file already exists: '{}' while uploading '{}'.", fileName, arcfile);
            throw new PermissionDenied("Upload: file already exists: '" + fileName + "' while uploading '" + arcfile
                    + "'.");
        }

        // Everything seems ok, initiate copy of file into archive
        copyRemoteFileToArchive(arcfile, fileName);
        log.info("Upload: completed uploading {}", fileName);
    }

    /**
     * Run a batch job on all ARC entries in the archive.
     * <p>
     * This currently runs synchronously, and returns only after finish() has been called.
     *
     * @param bitarchiveAppId A String representing the bitarchive AppId.
     * @param job An object that implements the ARCBatchJob interface. The initialize() method will be called before
     * processing and the finish() method will be called afterwards. The process() method will be called with each ARC
     * entry.
     * @return A localBatchStatus
     * @throws ArgumentNotValid if job or file is null.
     * @throws IOFailure if there was problems writing to the RemoteFile
     */
    public BatchStatus batch(String bitarchiveAppId, final FileBatchJob job) throws ArgumentNotValid, IOFailure {
        ArgumentNotValid.checkNotNullOrEmpty(bitarchiveAppId, "String bitarchiveAppId");
        ArgumentNotValid.checkNotNull(job, "FileBatchJob job");
        log.info("Starting batch job on bitarchive application with id '{}': '{}', on filename-pattern: '{}'",
                bitarchiveAppId, job.getClass().getName(), job.getFilenamePattern());
        BatchStatus returnStatus;

        File tmpFile = null;
        try {
            tmpFile = Files.createTempFile(FileUtils.getTempDir().toPath(), "BatchOutput", "").toFile();
            final OutputStream os = new FileOutputStream(tmpFile);

            try {
                // Run the batch job
                log.debug("Batch: Job {} started at {}", job, new Date());
                File[] processFiles = admin.getFilesMatching(job.getFilenamePattern());

                final BatchLocalFiles localBatchRunner = new BatchLocalFiles(processFiles);
                localBatchRunner.run(job, os);
                log.debug("Batch: Job {} finished at {}", job, new Date());
            } finally { // Make sure the OutputStream is closed no matter what.
                // This allows us to delete the file on Windows
                // in case of error.
                try {
                    os.close();
                } catch (IOException e) {
                    // We're cleaning up, failing to close won't stop us
                    log.warn("Failed to close outputstream in batch");
                }
            }
            // write output from batch job back to remote file
            returnStatus = new BatchStatus(bitarchiveAppId, job.getFilesFailed(), job.getNoOfFilesProcessed(),
                    RemoteFileFactory.getMovefileInstance(tmpFile), job.getExceptions());
        } catch (IOException e) {
            log.error("Failed to create temporary file for batch {}", job, e);
            throw new IOFailure("Failed to create temporary file for batch " + job, e);
        }
        log.info(
                "Finished batch job on bitarchive application with id '{}': '{}', on filename-pattern: '{}' + with result: {}",
                bitarchiveAppId, job.getClass().getName(), job.getFilenamePattern(), returnStatus);
        return returnStatus;
    }

    /**
     * Copies a remote file into the bitarchive storage and returns the storage position of the file.
     *
     * @param arcfile The source file.
     * @param fileName the source files filename.
     * @return the storage position of the file.
     * @throws IOFailure if an error occurs while copying into the archive.
     */
    private File copyRemoteFileToArchive(RemoteFile arcfile, String fileName) throws IOFailure {
        File tempDestination = admin.getTemporaryPath(fileName, arcfile.getSize());
        File destination = null;
        try {
            // The file is first copied to a temporary destination on the same
            // mount. The reason for this is to eliminate that there are files
            // in the file-directory that are currupted because of upload
            // errors. For example if the there is a break down after only half
            // the file is uploaded. It also means that we do not need to clean
            // up in the file directory, in case of failure - only the temporary
            // destination needs clean up.
            arcfile.copyTo(tempDestination);
            // Note that the move operation is a constant time operation within
            // the same mount
            destination = admin.moveToStorage(tempDestination);
        } catch (Throwable e) {
            // destination is known to be null here, so don't worry about it.
            if (tempDestination.exists()) {
                tempDestination.delete();
            }
            throw new IOFailure("Can't copy file into archive: " + fileName, e);
        }
        return destination;
    }

    /**
     * Get a file for a given arcFileID.
     *
     * @param arcFileID name of the file to be retrieved.
     * @return The file requested or null if not found
     * @throws ArgumentNotValid If arcFileID was null or empty.
     */
    public File getFile(String arcFileID) throws ArgumentNotValid {
        log.info("Get file '{}'", arcFileID);
        ArgumentNotValid.checkNotNullOrEmpty(arcFileID, "arcFileID");
        BitarchiveARCFile barc = admin.lookup(arcFileID);
        if (barc == null) { // the file with ID: arcFileID was not found
            log.debug("File '{}' not found on this machine", arcFileID);
            return null;
        }

        File path = barc.getFilePath();
        log.info("Getting file '{}'", path);
        return path;
    }

    /**
     * Get the one instance of the bitarchive.
     *
     * @return An instance of the Bitarchive class.
     * @throws PermissionDenied If the storage area used for files is not accessible.
     */
    public static Bitarchive getInstance() throws PermissionDenied {
        if (instance == null) {
            instance = new Bitarchive();
        }
        return instance;
    }

}
