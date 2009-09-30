/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
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

package dk.netarkivet.common.distribute.arcrepository;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.archive.io.arc.ARCReader;
import org.archive.io.arc.ARCReaderFactory;
import org.archive.io.arc.ARCRecord;

import dk.netarkivet.common.distribute.RemoteFileFactory;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.NotImplementedException;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.batch.BatchLocalFiles;
import dk.netarkivet.common.utils.batch.FileBatchJob;

/**
 * A minimal implementation of ArcRepositoryClient that just has one local
 * directory that it keeps its files in, no checking no nothing.
 */
public class TrivialArcRepositoryClient implements ArcRepositoryClient {
    /** The directory name of the local arcrepository. */
    private static final String ARC_REPOSITORY_DIR_NAME = "ArcRepository";
    /** Store files in this dir -- might later use a separate setting. */
    private final File dir
            = new File(FileUtils.getTempDir(), ARC_REPOSITORY_DIR_NAME);
    /** The class logger. */
    private Log log = LogFactory.getLog(getClass());

    /**
     * Constructor for this class.
     * Creates a local directory for the arcrepository.
     */
    public TrivialArcRepositoryClient() {
        FileUtils.createDir(dir);
    }

    /** Call on shutdown to release external resources. */
    public void close() {
    }

    /**
     * Store the given file in the ArcRepository.  After storing, the file is
     * deleted.
     *
     * @param file A file to be stored. Must exist.
     * @throws IOFailure thrown if store is unsuccessful, or failed to clean
     * up files after the store operation.
     * @throws ArgumentNotValid if file parameter is null or file is not an
     *                          existing file.
     */
    public void store(File file) throws IOFailure, ArgumentNotValid {
        ArgumentNotValid.checkNotNull(file, "file");
        FileUtils.copyFile(file, new File(dir, file.getName()));
        FileUtils.remove(file);
    }

    /**
     * Gets a single ARC record out of the ArcRepository.
     *
     * @param arcfile The name of a file containing the desired record.
     * @param index   The offset of the desired record in the file
     * @return a BitarchiveRecord-object, or null if request times out or object
     * is not found.
     * @throws ArgumentNotValid if arcfile is null or empty, or index
     * is negative
     * @throws IOFailure If the get operation failed.
     */
    public BitarchiveRecord get(String arcfile, long index)
            throws ArgumentNotValid {
        ArgumentNotValid.checkNotNullOrEmpty(arcfile, "arcfile");
        ArgumentNotValid.checkNotNegative(index, "index");  
        ARCReader reader = null;
        ARCRecord record = null;
        try {
            reader = ARCReaderFactory.get(new File(dir, arcfile), index);
            record = (ARCRecord) reader.get();
            return new BitarchiveRecord(record);
        } catch (IOException e) {
            throw new IOFailure("Error reading record from '"
                    + arcfile + "' offset " + index, e);
        } finally {
            if (record != null) {
                try {
                    record.close();
                } catch (IOException e) {
                    log.info("Error closing ARC record '" + record + "'", e);
                }
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    log.info("Error closing ARC reader '" + reader + "'", e);
                }
            }
        }
    }

    /**
     * Retrieves a file from an ArcRepository and places it in a local file.

     * @param arcfilename Name of the arcfile to retrieve.
     * @param replica The bitarchive to retrieve the data from
     * (not used in this implementation)
     * @param toFile Filename of a place where the file fetched can be put.
     * @throws IOFailure if there are problems getting a reply or the file
     * could not be found.
     */
    public void getFile(String arcfilename, Replica replica, File toFile) {
        ArgumentNotValid.checkNotNullOrEmpty(arcfilename, "arcfilename");
        ArgumentNotValid.checkNotNull(toFile, "toFile");
        FileUtils.copyFile(new File(dir, arcfilename), toFile);
    }

    /**
     * Runs a batch job on each file in the ArcRepository.
     *
     * @param job An object that implements the FileBatchJob interface. The
     *  initialize() method will be called before processing and the finish()
     *  method will be called afterwards. The process() method will be called
     *  with each File entry.
     *
     * @param replicaId The archive to execute the job on 
     * (not used in this implementation)
     * @return The status of the batch job after it ended.
     */
    public BatchStatus batch(final FileBatchJob job, String replicaId) {
        ArgumentNotValid.checkNotNull(job, "job");
        OutputStream os = null;
        File resultFile;
        try {
            resultFile = File.createTempFile("batch", replicaId,
                    FileUtils.getTempDir());
            os = new FileOutputStream(resultFile);
            File[] files = dir.listFiles(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    Pattern filenamePattern = job.getFilenamePattern();
                    return new File(dir, name).isFile() && (
                            filenamePattern == null
                            || filenamePattern.matcher(name).matches());
                }
            });
            BatchLocalFiles batcher = new BatchLocalFiles(files);
            batcher.run(job, os);
        } catch (IOException e) {
            throw new IOFailure("Cannot perform batch job '" + job + "'", e);
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e) {
                    log.info("Error closing batch output stream '" + os + "'",
                            e);
                }
            }
        }
        return new BatchStatus(replicaId, job.getFilesFailed(),
                job.getNoOfFilesProcessed(),
                RemoteFileFactory.getMovefileInstance(resultFile), 
                job.getExceptions());
    }

    /** Updates the administrative data in the ArcRepository for a given
     * file and replica. (not implemented)
     *
     * @param fileName The name of a file stored in the ArcRepository.
     * @param bitarchiveId The id of the replica that the administrative
     * data for fileName is wrong for.
     * @param newval What the administrative data will be updated to.
     */
    public void updateAdminData(String fileName, String bitarchiveId,
                                ReplicaStoreState newval) {
        throw new NotImplementedException("Function has not been implemented");
    }

    /** Updates the checksum kept in the ArcRepository for a given
     * file.  It is the responsibility of the ArcRepository implementation to
     * ensure that this checksum matches that of the underlying files.
     *
     * @param filename The name of a file stored in the ArcRepository.
     * @param checksum The new checksum.
     */
    public void updateAdminChecksum(String filename, String checksum) {
        throw new NotImplementedException("Function has not been implemented");
    }

    /** Remove a file from one part of the ArcRepository, retrieving a copy
     * for security purposes.  This is typically used when repairing a file
     * that has been corrupted.
     *
     * @param fileName The name of the file to remove.
     * @param bitarchiveId The id of the replica from which to remove the file (not used)
     * @param checksum The checksum of the file to be removed (not used)
     * @param credentials A string that shows that the user is allowed to
     * perform this operation (not used)
     * @return A local copy of the file removed.
     */
    public File removeAndGetFile(String fileName, String bitarchiveId,
                                 String checksum, String credentials) {
        ArgumentNotValid.checkNotNullOrEmpty(fileName, "fileName");
        // Ignores bitarchiveId, checksum, and credentials for now
        File copiedTo = null;
        try {
            copiedTo = File.createTempFile("removeAndGetFile", fileName);
        } catch (IOException e) {
            throw new IOFailure("Cannot make temp file to copy '"
                    + fileName + "' into", e);
        }
        File file = new File(dir, fileName);
        FileUtils.copyFile(file, copiedTo);
        FileUtils.remove(file);
        return copiedTo;
    }

    public File getAllChecksums(String replicaId) {
        // TODO Auto-generated method stub
        throw new NotImplementedException("TODO: Implement me!");
    }

    public File getAllFilenames(String replicaId) {
        // TODO Auto-generated method stub
        throw new NotImplementedException("TODO: Implement me!");
    }

    public void correct(String replicaId, String checksum, File file,
                        String credentials) {
        // TODO Auto-generated method stub
        throw new NotImplementedException("TODO: Implement me!");
    }
}
