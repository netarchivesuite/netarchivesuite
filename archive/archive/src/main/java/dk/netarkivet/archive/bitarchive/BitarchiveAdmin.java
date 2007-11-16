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

package dk.netarkivet.archive.bitarchive;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.common.Settings;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.PermissionDenied;
import dk.netarkivet.common.utils.ApplicationUtils;
import dk.netarkivet.common.utils.FileUtils;

/**
 * This class handles file lookup and encapsulates the actual placement of
 * files.
 */
public class BitarchiveAdmin {
    private final Log log = LogFactory.getLog(getClass().getName());

    /**
     * The list of valid archive directories.
     *
     */
    private List<File> archivePaths = new ArrayList<File>();

    /**
     * Singleton instance.
     */
    private static BitarchiveAdmin instance;

    /**
     * How much space we must have available *in a single dir*
     * before we will listen  for new uploads.
     */
    private final long minSpaceLeft;

    /**
     * The name of the directory in which files are stored.
     */
    private static String FILE_DIRECTORY_NAME = "filedir";
    /**
     * Temporary directory used during upload, where partial files exist, until
     * moved into directory FILE_DIRECTORY_NAME.
     */
    private static String TEMPORARY_DIRECTORY_NAME = "tempdir";

    /**
     * Directory where "deleted" files are placed".
     */
    private static String ATTIC_DIRECTORY_NAME = "atticdir";

    /**
     * Creates a new BitarchiveAdmin object for an existing bit archive.
     * Reads the directories to use from settings.
     *
     * @throws PermissionDenied If any of the directories cannot be created or
     *                          are not writeable.
     */
    private BitarchiveAdmin() {
        String[] filedirnames =
                Settings.getAll(Settings.BITARCHIVE_SERVER_FILEDIR);
        minSpaceLeft = Settings.getLong(Settings.BITARCHIVE_MIN_SPACE_LEFT);
        // Check, if value of minSpaceLeft is greater than zero
        if (minSpaceLeft <= 0L) {
            log.warn(
                    "Wrong setting of minSpaceLeft read from Settings: "
                    + minSpaceLeft);
            throw new ArgumentNotValid(
                    "Wrong setting of minSpaceLeft read from Settings: "
                    + minSpaceLeft);
        }

        log.info("Requiring at least " + minSpaceLeft + " bytes free.");

        for (int i = 0; i < filedirnames.length; i++) {
            File basedir = new File(filedirnames[i]);
            File filedir = new File(basedir, FILE_DIRECTORY_NAME);

            // Ensure that 'filedir' exists. If it doesn't, it is created
            ApplicationUtils.dirMustExist(filedir);
            File tempdir = new File(basedir, TEMPORARY_DIRECTORY_NAME);

            // Ensure that 'tempdir' exists. If it doesn't, it is created
            ApplicationUtils.dirMustExist(tempdir);

            File atticdir = new File(basedir, ATTIC_DIRECTORY_NAME);

            // Ensure that 'atticdir' exists. If it doesn't, it is created
            ApplicationUtils.dirMustExist(atticdir);
            archivePaths.add(basedir);
            final Long bytesUsedInDir = new Long(calculateBytesUsed(basedir));
            log.info("Using bit archive directory '" + basedir + "' with "
                    + bytesUsedInDir + " bytes of content and "
                    + FileUtils.getBytesFree(basedir) + " bytes free");
        }
    }

    /**
     * Returns true if we have at least one dir with the required amount
     * of space left.
     *
     * @return true if we have at least one dir with the required amount
     *         of space left, otherwise false.
     */
    public boolean hasEnoughSpace() {
        for (File dir : archivePaths) {
            if (checkArchiveDir(dir)
                && FileUtils.getBytesFree(dir) > minSpaceLeft) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns a temporary place for the the file to be stored.
     *
     * @param arcFile       The simple name (i.e. no dirs) of the ARC file.
     * @param requestedSize How large the file is in bytes.
     * @return The path where the arcFile should go.
     *
     * @throws ArgumentNotValid
     *          If arcFile is null or empty, or requestedSize is negative.
     * @throws IOFailure if there is no more room left to store this file of
     *                   size=requestedSize
     */
    public File getTemporaryPath(String arcFile, long requestedSize)
    throws ArgumentNotValid, IOFailure {
        ArgumentNotValid.checkNotNullOrEmpty(arcFile, "arcFile");
        ArgumentNotValid.checkNotNegative(requestedSize, "requestedSize");

        for (Iterator<File> i = archivePaths.iterator(); i.hasNext(); ) {
            File dir = i.next();
            if (checkArchiveDir(dir)) {
                if (FileUtils.getBytesFree(dir) > requestedSize) {
                    File filedir = new File(dir, TEMPORARY_DIRECTORY_NAME);
                    return new File(filedir, arcFile);
                }
            }
        }
        String errMsg = "No space left to store '" + arcFile + "' of size "
                        + requestedSize;
        log.fatal(errMsg);
        throw new IOFailure(errMsg);
    }

    /**
     * Moves a file from temporary storage to file storage.
     *
     * Note: It is checked, if tempLocation resides in directory
     *  TEMPORARY_DIRECTORY_NAME and whether the parent of tempLocation
     *   is a Bitarchive directory.
     *
     * @param tempLocation The temporary location where the file was stored.
     *                     This must be a path returned from getTemporaryPath
     *
     * @return The location where the file is now stored
     * @throws IOFailure if tempLocation is not created from getTemporaryPath
     *                   or file cannot be moved to Storage location.
     */
    public File moveToStorage(File tempLocation) {
        ArgumentNotValid.checkNotNull(tempLocation, "tempLocation");
        tempLocation = tempLocation.getAbsoluteFile();
        String arcFileName = tempLocation.getName();

        /**
         * Check, that File tempLocation resides in directory
         * TEMPORARY_DIRECTORY_NAME.
         */
        File arcFilePath = tempLocation.getParentFile();
        if (arcFilePath == null
            || !arcFilePath.getName().equals(TEMPORARY_DIRECTORY_NAME)) {
            throw new IOFailure("Location '" + tempLocation + "' is not in "
                                + "tempdir '" + TEMPORARY_DIRECTORY_NAME + "'");
        }
        /**
         * Check, that arcFilePath (now known to be TEMPORARY_DIRECTORY_NAME)
         * resides in a recognised Bitarchive Directory.
         */
        File archivedir = arcFilePath.getParentFile();
        if (archivedir == null || !isBitarchiveDirectory(archivedir)) {
            throw new IOFailure("Location '" + tempLocation + "' is not in "
                                + "recognised archive directory.");
        }
        /**
         * Move File tempLocation to new location: storageFile
         */
        File storagePath = new File(archivedir, FILE_DIRECTORY_NAME);
        File storageFile = new File(storagePath, arcFileName);
        if (!tempLocation.renameTo(storageFile)) {
            throw new IOFailure("Could not move '" + tempLocation.getPath()
                                + "' to '" + storageFile.getPath() + "'");
        }
        return storageFile;
    }

    /** Checks whether a directory is one of the known bitarchive dirs.
     *
     * @param theDir The dir to check
     * @return whether it is a valid archive directory
     * @throws IOFailure if archivedir or one of the valid archive directories does not exist
     */
    private boolean isBitarchiveDirectory(File theDir) {
        try {
            theDir = theDir.getCanonicalFile();
            for (Iterator<File> i = archivePaths.iterator(); i.hasNext(); ) {
                File knowndir = i.next();
                if (knowndir.getCanonicalFile().equals(theDir)) {
                    return true;
                }
            }
            return false;
        } catch (IOException e) {
            log.warn("File not known", e);
            throw new IOFailure("File not known", e);
        }
    }

    /**
     * Check that the given file is a directory appropriate for use.
     *
     * @param file A file
     * @return true, if 'file' is an existing directory and is writable.
     * @throws ArgumentNotValid if 'file' is null.
     */
    private boolean checkArchiveDir(File file) {
        ArgumentNotValid.checkNotNull(file, "file");
        if (!file.exists()) {
            log.warn("Directory '" + file + "' does not exist");
            return false;
        }
        if (!file.isDirectory()) {
            log.warn("Directory '" + file
                    + "' is not a directory after all");
            return false;
        }
        if (!file.canWrite()) {
            log.warn("Directory '" + file + "' is not writable");
            return false;
        }
        return true;
    }

    /**
     * Return array with references to all files in the archive.
     *
     * @return array with references to all files in the archive
     */
    public File[] getFiles() {
        List<File> files = new ArrayList<File>();
        for (Iterator<File> i = archivePaths.iterator(); i.hasNext();) {
            File archiveDir = new File(i.next(), FILE_DIRECTORY_NAME);
            if (checkArchiveDir(archiveDir)) {
                File[] filesHere = archiveDir.listFiles();
                for (int j = 0; j < filesHere.length; j++) {
                    if (!filesHere[j].isFile()) {
                        log.warn("Non-file '" + filesHere[j]
                                      + "' found among archive files");
                    } else {
                        files.add(filesHere[j]);
                    }
                }
            }
        }
        return (files.toArray(new File[0]));
    }

    /** Return an array of all files in this archive that match a given
     * regular expression on the filename.
     *
     * @param regexp A precompiled regular expression matching whole filenames.
     * This will probably be given to a FilenameFilter
     * @return An array of all the files in this bitarchive that exactly match
     * the regular expression on the filename (sans paths).
     */
    public File[] getFilesMatching(final Pattern regexp) {
        List<File> files = new ArrayList<File>();
        for (File archivePath : archivePaths) {
            File archiveDir = new File(archivePath, FILE_DIRECTORY_NAME);
            if (checkArchiveDir(archiveDir)) {
                File[] filesHere = archiveDir.listFiles(new FilenameFilter() {
                    public boolean accept(File dir, String name) {
                        if (regexp.matcher(name).matches()) {
                            if (!new File(dir, name).isFile()) {
                                log.warn("Non-file '" + new File(dir, name)
                                        + "' found among archive files");
                                return false;
                            } else {
                                return true;
                            }
                        } else {
                            return false;
                        }
                    }
                });
                files.addAll(Arrays.asList(filesHere));
            }
        }
        return files.toArray(new File[0]);
    }

    /**
     * Return the path that a given arc file can be found in.
     *
     * @param arcFileName Name of an arc file (with no path)
     * @return A BitarchiveARCFile for the given file, or null if the
     *         file does not exist.
     */
    public BitarchiveARCFile lookup(String arcFileName) {
        ArgumentNotValid.checkNotNullOrEmpty(arcFileName, "arcFileName");
        for (Iterator i = archivePaths.iterator(); i.hasNext();) {
            File archiveDir = new File((File) i.next(), FILE_DIRECTORY_NAME);

            if (checkArchiveDir(archiveDir)) {
                File filename = new File(archiveDir, arcFileName);
                if (filename.exists()) {
                    if (filename.isFile()) {
                        return new BitarchiveARCFile(arcFileName, filename);
                    }
                    log.fatal("Corrupt bitarchive: Non-file '" + filename
                            + "' found in"
                            + " place of archive file");
                }
            }
        }
        return null; // the arcfile named "arcFileName" does not exist in this bitarchive.
    }

    /**
     * Calculate how many bytes are used by all files in a directory.
     *
     * @param filedir An existing directory with a FILE_DIRECTORY_NAME subdir
     *                and a TEMPORARY_DIRECTORY_NAME subdir.
     * @return Number of bytes used by all files in the directory (not including
     *         overhead from partially used blocks).
     */
    private long calculateBytesUsed(File filedir) {
        long used = 0;
        File[] files = new File(filedir, FILE_DIRECTORY_NAME).listFiles();
        if (files != null) { // Check, that listFiles method returns valid information
            for (int i = 0; i < files.length; i++) {
                if (files[i].isFile()) {
                    used += files[i].length(); // Add size of file files[i] to amount of bytes used.
                } else {
                    log.warn("Non-file '" + files[i] + "' found in archive");
                }
            }
        } else {
            log.warn("filedir does not contain a directory named: "
                        + FILE_DIRECTORY_NAME);
        }
        File[] tempfiles = new File(filedir,
                                    TEMPORARY_DIRECTORY_NAME).listFiles();
        if (tempfiles != null) { // Check, that listFiles() method returns valid information
            for (int i = 0; i < tempfiles.length; i++) {
                if (tempfiles[i].isFile()) {
                    // Add size of file tempfiles[i] to amount of bytes used.
                    used += tempfiles[i].length();
                } else {
                    log.warn(
                            "Non-file '" + tempfiles[i] + "' found in archive");
                }
            }
        } else {
            log.warn("filedir does not contain a directory named: "
                        + TEMPORARY_DIRECTORY_NAME);
        }
        File[] atticfiles = new File(filedir, ATTIC_DIRECTORY_NAME).listFiles();
        if (atticfiles != null) { // Check, that listFiles() method returns valid information
            for (int i = 0; i < atticfiles.length; i++) {
                if (atticfiles[i].isFile()) {
                    // Add size of file tempfiles[i] to amount of bytes used.
                    used += atticfiles[i].length();
                } else {
                    log.warn("Non-file '" + atticfiles[i]
                                + "' found in archive");
                }
            }
        } else {
            log.warn("filedir does not contain a directory named: "
                        + ATTIC_DIRECTORY_NAME);
        }
        return used;
    }

    /**
     * Get the one and only instance of the bitarchive admin.
     *
     * @return A BitarchiveAdmin object
     */
    public static BitarchiveAdmin getInstance() {
        if (instance == null) {
            instance = new BitarchiveAdmin();
        }
        return instance;
    }

    /**
     * Close down the bitarchive admin.
     * Currently has no data to store.
     */
    public void close() {
        instance = null;
    }

    /** Return the path used to store files that are removed by
     * RemoveAndGetFileMessage.
     *
     * @param existingFile
     * @return The full path of the file in the attic dir
     */
    public File getAtticPath(File existingFile) {
        // Find where the file resides so we can use a dir in the same place.
        existingFile = existingFile.getAbsoluteFile();
        String arcFileName = existingFile.getName();
        File parentDir = existingFile.getParentFile().getParentFile();
        if (!isBitarchiveDirectory(parentDir)) {
            log.warn("Attempt to get attic path for non-archived file '"
                    + existingFile + "'");
            throw new ArgumentNotValid("File should belong to a bitarchive dir,"
                    + " but " + existingFile + " doesn't");
        }
        // Ensure that 'atticdir' exists. If it doesn't, it is created
        File atticdir = new File(parentDir, ATTIC_DIRECTORY_NAME);
        ApplicationUtils.dirMustExist(atticdir);
        return new File(atticdir,
                        arcFileName);
    }
}
