/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
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
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.archive.io.arc.ARCReader;
import org.archive.io.arc.ARCReaderFactory;
import org.archive.io.arc.ARCRecord;

import dk.netarkivet.common.Settings;
import dk.netarkivet.common.distribute.RemoteFileFactory;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.arc.BatchLocalFiles;
import dk.netarkivet.common.utils.arc.FileBatchJob;

/**
 * A simple implementation of ArcRepositoryClient that just has a number of
 * local directories that it keeps its files in.  It doesn't implement
 * credentials checks or checksum storing.
 */
public class LocalArcRepositoryClient implements ArcRepositoryClient {
    private Log log = LogFactory.getLog(getClass());
    /** List of the directories that we store files in */
    private final List<File> storageDirs = new ArrayList<File>(1);
    /** If no directories are specified in the settings file, use a single
     * directory with this name
     */
    private static final String DEFAULT_DIR_NAME = "ArcRepository";

    /** Create a new LocalArcRepositoryClient based on current settings. */
    public LocalArcRepositoryClient() {
        List<String> fileDirs =
                Arrays.asList(Settings.getAll("settings.common.arcrepositoryClient.fileDir"));
        if (fileDirs.size() == 0) {
            fileDirs.add(DEFAULT_DIR_NAME);
        }
        for (String fileName : fileDirs) {
            File f = new File(fileName);
            FileUtils.createDir(f);
            storageDirs.add(f);
        }
    }

    /** Call on shutdown to release external resources. */
    public void close() {
    }

    /**
     * Store the given file in the ArcRepository.  After storing, the file is
     * deleted.
     *
     * @param file A file to be stored. Must exist.
     * @throws IOFailure thrown if store is unsuccesful, or failed to clean
     * up files after the store operation.
     * @throws ArgumentNotValid if file parameter is null or file is not an
     *                          existing file.
     */
    public void store(File file) throws IOFailure, ArgumentNotValid {
        ArgumentNotValid.checkNotNull(file, "File file");
        ArgumentNotValid.checkTrue(file.exists(), "File '" + file
                                                  + "' does not exist");
        if (findFile(file.getName()) != null) {
            throw new ArgumentNotValid("A file with the name '"
                                       + file.getName() + " is already stored");
        }
        for (File dir : storageDirs) {
            if (dir.canWrite() && FileUtils.getBytesFree(dir) > file.length()) {
                FileUtils.copyFile(file, new File(dir, file.getName()));
                FileUtils.remove(file);
                return;
            }
        }
        throw new IOFailure("Not enough room for '" + file
                            + "' in any of the dirs " + storageDirs);
    }

    /**
     * Gets a single ARC record out of the ArcRepository.
     *
     * @param arcfile The name of a file containing the desired record.
     * @param index   The offset of the desired record in the file
     * @return a BitarchiveRecord-object, or null if request times out or object
     * is not found.
     * @exception ArgumentNotValid If the get operation failed.
     */
    public BitarchiveRecord get(String arcfile, long index)
            throws ArgumentNotValid {
        ArgumentNotValid.checkNotNullOrEmpty(arcfile, "String arcfile");
        ArgumentNotValid.checkNotNegative(index, "long index");
        File f = findFile(arcfile);
        if (f == null) {
            throw new ArgumentNotValid("File '" + arcfile + "' does not exist");
        }
        ARCReader reader = null;
        ARCRecord record = null;
        try {
            reader = ARCReaderFactory.get(new File(f, arcfile), index);
            record = (ARCRecord) reader.get();
            return new BitarchiveRecord(record);
        } catch (IOException e) {
            throw new ArgumentNotValid("Error reading record from '"
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
     * @param location The bitarchive to retrieve the data from.
     * @param toFile Filename of a place where the file fetched can be put.
     * @throws IOFailure if there are problems getting a reply or the file
     * could not be found.
     */
    public void getFile(String arcfilename, Location location, File toFile) {
        ArgumentNotValid.checkNotNullOrEmpty(arcfilename, "String arcfilename");
        ArgumentNotValid.checkNotNull(location, "location");
        ArgumentNotValid.checkNotNull(toFile, "File toFile");
        File f = findFile(arcfilename);
        if (f != null) {
            FileUtils.copyFile(f, toFile);
        } else {
            throw new IOFailure("File '" + arcfilename + "' does not exist");
        }
    }

    /**
     * Runs a batch job on each file in the ArcRepository.
     *
     * @param job An object that implements the FileBatchJob interface. The
     *  initialize() method will be called before processing and the finish()
     *  method will be called afterwards. The process() method will be called
     *  with each File entry.
     *
     * @param locationName The archive to execute the job on.
     * @return The status of the batch job after it ended.
     */
    public BatchStatus batch(final FileBatchJob job, String locationName) {
        OutputStream os = null;
        try {
            File resultFile =  File.createTempFile("batch", locationName,
                    FileUtils.getTempDir());
            os = new FileOutputStream(resultFile);
            List<File> files = new ArrayList<File>();
            final FilenameFilter filenameFilter = new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    Pattern filenamePattern = job.getFilenamePattern();
                    if (filenamePattern != null &&
                        new File(dir, name).isFile()) {
                        return filenamePattern.matcher(name).matches();
                    } else {
                        return true;
                    }
                }
            };
            for (File dir : storageDirs) {
                File[] filesInDir = dir.listFiles(filenameFilter);
                if (filesInDir != null) {
                    files.addAll(Arrays.asList(filesInDir));
                }
            }
            BatchLocalFiles batcher
                    = new BatchLocalFiles(files.toArray(new File[0]));
            batcher.run(job, os);
            return new BatchStatus(locationName, job.getFilesFailed(),
                    job.getNoOfFilesProcessed(),
                    RemoteFileFactory.getMovefileInstance(resultFile));
        } catch (IOException e) {
            throw new IOFailure("Cannot perform batch '" + job + "'", e);
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
    }

    /** Updates the administrative data in the ArcRepository for a given
     * file and location.
     *
     * @param fileName The name of a file stored in the ArcRepository.
     * @param bitarchiveName The name of the location that the administrative
     * data for fileName is wrong for.
     * @param newval What the administrative data will be updated to.
     */
    public void updateAdminData(String fileName, String bitarchiveName,
                                BitArchiveStoreState newval) {
    }

    /** Updates the checksum kept in the ArcRepository for a given
     * file.  It is the responsibility of the ArcRepository implementation to
     * ensure that this checksum matches that of the underlying files.
     *
     * @param filename The name of a file stored in the ArcRepository.
     * @param checksum The new checksum.
     */
    public void updateAdminChecksum(String filename, String checksum) {
    }

    /** Remove a file from one part of the ArcRepository, retrieveing a copy
     * for security purposes.  This is typically used when repairing a file
     * that has been corrupted.
     *
     * @param fileName The name of the file to remove.
     * @param bitarchiveName The location from which to remove the file.
     * @param checksum The checksum of the file to be removed.
     * @param credentials A string that shows that the user is allowed to
     * perform this operation.
     * @return A local copy of the file removed.
     */
    public File removeAndGetFile(String fileName, String bitarchiveName,
                                 String checksum, String credentials) {
        // Ignores bitarchiveName, checksum, and credentials for now
        File file = findFile(fileName);
        if (file == null) {
            throw new IOFailure("Cannot find file '" + fileName + "'");
        }
        File copiedTo = null;
        try {
            copiedTo = File.createTempFile("removeAndGetFile", fileName);
        } catch (IOException e) {
            throw new IOFailure("Cannot make temp file to copy '"
                    + fileName + "' into", e);
        }
        FileUtils.copyFile(file, copiedTo);
        FileUtils.remove(file);
        return copiedTo;
    }

    /** Returns a File object for a filename if it exists in the archive.
     *
     * @param filename Name of file to find.
     * @return A File object for the filename if the file exists,
     * otherwise null.
     */
    private File findFile(String filename) {
        for (File dir : storageDirs) {
            final File file = new File(dir, filename);
            if (file.exists()) {
                return file;
            }
        }
        return null;
    }
}