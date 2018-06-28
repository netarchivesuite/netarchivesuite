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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.archive.ArchiveSettings;
import dk.netarkivet.archive.Constants;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.PermissionDenied;
import dk.netarkivet.common.exceptions.UnknownID;
import dk.netarkivet.common.utils.ApplicationUtils;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.Settings;

/**
 * This class handles file lookup and encapsulates the actual placement of files.
 */
public final class BitarchiveAdmin {

    /** The class logger. */
    private static final Logger log = LoggerFactory.getLogger(BitarchiveAdmin.class);

    /**
     * Map containing the archive directories and their files. The file must be the CanonicalFile (use
     * getCanonicalFile() before access).
     */
    private Map<File, List<String>> archivedFiles = Collections
            .synchronizedMap(new LinkedHashMap<File, List<String>>());

    /**
     * Map containing the time for the latest update of the filelist for each archive directory. The file must be the
     * CanonicalFile (use getCanonicalFile() before access).
     */
    private Map<File, Long> archiveTime = Collections.synchronizedMap(new HashMap<File, Long>());

    /** Singleton instance. */
    private static BitarchiveAdmin instance;

    /** How much space we must have available *in a single dir* before we will listen for new uploads. */
    private final long minSpaceLeft;

    /** How much space we require available *in every dir* after we have accepted an upload. */
    private final long minSpaceRequired;

    /** Are readOnly Directories allowed. */
    private final boolean readOnlyAllowed;

    /**
     * Creates a new BitarchiveAdmin object for an existing bit archive. Reads the directories to use from settings.
     *
     * @throws ArgumentNotValid If the settings for minSpaceLeft is non-positive or the setting for minSpaceRequired is
     * negative.
     * @throws PermissionDenied If any of the directories cannot be created or are not writeable.
     * @throws IOFailure If it is not possible to retrieve the canonical file for the directories.
     */
    private BitarchiveAdmin() throws ArgumentNotValid, PermissionDenied, IOFailure {
        String[] filedirnames = Settings.getAll(ArchiveSettings.BITARCHIVE_SERVER_FILEDIR);
        minSpaceLeft = Settings.getLong(ArchiveSettings.BITARCHIVE_MIN_SPACE_LEFT);
        readOnlyAllowed = Settings.getBoolean(ArchiveSettings.BITARCHIVE_READ_ONLY_ALLOWED);

        log.info("readOnlyAllowed is: {}", readOnlyAllowed);

        // Check, if value of minSpaceLeft is greater than zero
        if (minSpaceLeft <= 0L) {
            log.warn("Wrong setting of minSpaceLeft read from Settings: {}", minSpaceLeft);
            throw new ArgumentNotValid("Wrong setting of minSpaceLeft read from Settings: " + minSpaceLeft);
        }

        minSpaceRequired = Settings.getLong(ArchiveSettings.BITARCHIVE_MIN_SPACE_REQUIRED);
        // Check, if value of minSpaceRequired is at least zero
        if (minSpaceLeft < 0L) {
            log.warn("Wrong setting of minSpaceRequired read from Settings: {}", minSpaceLeft);
            throw new ArgumentNotValid("Wrong setting of minSpaceRequired read from Settings: " + minSpaceLeft);
        }

        log.info("Requiring at least {} bytes free.", minSpaceRequired);
        log.info("Listening if at least {} bytes free.", minSpaceLeft);

        try {
            for (String filedirname : filedirnames) {
                File basedir = new File(filedirname).getCanonicalFile();
                File filedir = new File(basedir, Constants.FILE_DIRECTORY_NAME);
                // Ensure that 'filedir' exists. If it doesn't, it is created
                ApplicationUtils.dirMustExist(filedir);

                File tempdir = new File(basedir, Constants.TEMPORARY_DIRECTORY_NAME);
                // Ensure that 'tempdir' exists. If it doesn't, it is created
                ApplicationUtils.dirMustExist(tempdir);

                File atticdir = new File(basedir, Constants.ATTIC_DIRECTORY_NAME);
                // Ensure that 'atticdir' exists. If it doesn't, it is created
                ApplicationUtils.dirMustExist(atticdir);

                // initialise the variables archivedFiles and archiveTime
                archivedFiles.put(basedir, new ArrayList<String>());
                archiveTime.put(basedir, 0L);
                updateFileList(basedir);

                final Long bytesUsedInDir = calculateBytesUsed(basedir);
                log.info(
                        "Using bit archive directorys {'{}', '{}', '{}'} under base directory: '{}' with {} bytes of content and {} bytes free. Current number of files archived: {}",
                        Constants.FILE_DIRECTORY_NAME, Constants.TEMPORARY_DIRECTORY_NAME,
                        Constants.ATTIC_DIRECTORY_NAME, basedir, bytesUsedInDir, FileUtils.getBytesFree(basedir),
                        archivedFiles.get(basedir).size());
            }
        } catch (IOException e) {
            throw new IOFailure("Could not retrieve Canonical files.", e);
        }
    }

    /**
     * Checks whether the filelist is up to date. If the modified timestamp for the a directory is larger than the last
     * recorded timestamp, then the stored filelist is updated with the latest changes.
     */
    public synchronized void verifyFilelistUpToDate() {
        for (File basedir : archivedFiles.keySet()) {
            File filedir = new File(basedir, Constants.FILE_DIRECTORY_NAME);
            long lastModified = filedir.lastModified();
            if (archiveTime.get(basedir) < lastModified) {
                // Update the list and the time.
                updateFileList(basedir);
            }
        }
    }

    /**
     * Method for updating the filelist for a given basedir.
     *
     * @param basedir The basedir to update the filelist for.
     * @throws ArgumentNotValid If basedir is null or if it not a proper directory.
     * @throws UnknownID If the basedir cannot be found both the archivedFiles map or the archiveTime map.
     * @throws IOFailure If it is not possible to retrieve the canonical file for the basedir.
     */
    public void updateFileList(File basedir) throws ArgumentNotValid, UnknownID, IOFailure {
        ArgumentNotValid.checkNotNull(basedir, "File basedir");
        // ensure that it is the CanonicalFile for the directory.
        try {
            basedir = basedir.getCanonicalFile();
        } catch (IOException e) {
            throw new IOFailure("Could not retrieve canonical path for file '" + basedir, e);
        }
        if (!basedir.isDirectory()) {
            throw new ArgumentNotValid("The directory '" + basedir.getPath() + " is not a proper directory.");
        }
        if (!archivedFiles.containsKey(basedir) || !archiveTime.containsKey(basedir)) {
            throw new UnknownID("The directory '" + basedir + "' is not known "
                    + "by the settings. Known directories are: " + archivedFiles.keySet());
        }

        log.debug("Updating the filelist for '{}'.", basedir);
        File filedir = new File(basedir, Constants.FILE_DIRECTORY_NAME);
        if (!checkArchiveDir(filedir)) {
            throw new UnknownID("The directory '" + filedir + "' is not an " + " archive directory.");
        }

        String[] dirContent = filedir.list();
        List<String> filenames = new ArrayList<String>(dirContent.length);
        for (String file : dirContent) {
            // ensure that only files are handled
            if ((new File(filedir, file)).isFile()) {
                filenames.add(file);
            } else {
                log.warn("The file '{}' in directory {} is not a proper file.", file, filedir.getPath());
            }
        }
        archivedFiles.put(basedir, filenames);
        archiveTime.put(basedir, filedir.lastModified());
    }

    /**
     * Returns true if we have at least one dir with the required amount of space left.
     *
     * @return true if we have at least one dir with the required amount of space left, otherwise false.
     */
    public boolean hasEnoughSpace() {
        for (File dir : archivedFiles.keySet()) {
            if (checkArchiveDir(dir) && FileUtils.getBytesFree(dir) > minSpaceLeft) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns a temporary place for the the file to be stored.
     *
     * @param arcFileName The simple name (i.e. no dirs) of the ARC file.
     * @param requestedSize How large the file is in bytes.
     * @return The path where the arcFile should go.
     * @throws ArgumentNotValid If arcFileName is null or empty, or requestedSize is negative.
     * @throws IOFailure if there is no more room left to store this file of size=requestedSize
     */
    public File getTemporaryPath(String arcFileName, long requestedSize) throws ArgumentNotValid, IOFailure {
        ArgumentNotValid.checkNotNullOrEmpty(arcFileName, "arcFile");
        ArgumentNotValid.checkNotNegative(requestedSize, "requestedSize");

        for (File dir : archivedFiles.keySet()) {
            long bytesFreeInDir = FileUtils.getBytesFree(dir);
            // TODO If it turns out that it has not enough space for
            // this file, it should resend the Upload message
            // This should probably be handled in the
            // method BitarchiveServer.visit(UploadMessage msg)
            // This is bug 1586.

            if (checkArchiveDir(dir) && (bytesFreeInDir > minSpaceLeft)
                    && (bytesFreeInDir - requestedSize > minSpaceRequired)) {
                File filedir = new File(dir, Constants.TEMPORARY_DIRECTORY_NAME);
                return new File(filedir, arcFileName);
            } else {
                log.debug("Not enough space on dir '{}' for file '{}' of size {} bytes. Only {} left", dir.getPath(),
                        arcFileName, requestedSize, bytesFreeInDir);
            }
        }
        log.warn("No space left in dirs: {}, to store file '{}' of size {}", archivedFiles.keySet(), arcFileName,
                requestedSize);
        throw new IOFailure("No space left in dirs: " + archivedFiles.keySet() + ", to store file '" + arcFileName
                + "' of size " + requestedSize);
    }

    /**
     * Moves a file from temporary storage to file storage.
     * <p>
     * Note: It is checked, if tempLocation resides in directory TEMPORARY_DIRECTORY_NAME and whether the parent of
     * tempLocation is a Bitarchive directory.
     *
     * @param tempLocation The temporary location where the file was stored. This must be a path returned from
     * getTemporaryPath
     * @return The location where the file is now stored
     * @throws IOFailure if tempLocation is not created from getTemporaryPath or file cannot be moved to Storage
     * location.
     * @throws ArgumentNotValid If the tempLocation file is null.
     */
    public File moveToStorage(File tempLocation) throws IOFailure, ArgumentNotValid {
        ArgumentNotValid.checkNotNull(tempLocation, "tempLocation");
        try {
            tempLocation = tempLocation.getCanonicalFile();
        } catch (IOException e) {
            throw new IOFailure("Could not retrieve the canonical file for '" + tempLocation + "'.", e);
        }
        String arcFileName = tempLocation.getName();

        /**
         * Check, that File tempLocation resides in directory TEMPORARY_DIRECTORY_NAME.
         */
        File arcFilePath = tempLocation.getParentFile();
        if (arcFilePath == null || !arcFilePath.getName().equals(Constants.TEMPORARY_DIRECTORY_NAME)) {
            throw new IOFailure("Location '" + tempLocation + "' is not in " + "tempdir '"
                    + Constants.TEMPORARY_DIRECTORY_NAME + "'");
        }
        /**
         * Check, that arcFilePath (now known to be TEMPORARY_DIRECTORY_NAME) resides in a recognised Bitarchive
         * Directory.
         */
        File basedir = arcFilePath.getParentFile();
        if (basedir == null || !isBitarchiveDirectory(basedir)) {
            throw new IOFailure("Location '" + tempLocation + "' is not in " + "recognised archive directory.");
        }
        /**
         * Move File tempLocation to new location: storageFile
         */
        File storagePath = new File(basedir, Constants.FILE_DIRECTORY_NAME);
        File storageFile = new File(storagePath, arcFileName);
        if (!tempLocation.renameTo(storageFile)) {
            throw new IOFailure("Could not move '" + tempLocation.getPath() + "' to '" + storageFile.getPath() + "'");
        }
        // Update the filelist for the directory with this new file.
        final File canonicalFile;
        try {
            canonicalFile = basedir.getCanonicalFile();
        } catch (IOException e) {
            throw new IOFailure("Could not find canonical file for " + basedir.getAbsolutePath(), e);
        }
        final List<String> fileList = archivedFiles.get(canonicalFile);
        if (fileList == null) {
            throw new UnknownID("The directory " + basedir.getAbsolutePath() + " was not found in the map of known directories and files.");
        }
        fileList.add(arcFileName);
        archiveTime.put(canonicalFile, storagePath.lastModified());
        return storageFile;
    }

    /**
     * Checks whether a directory is one of the known bitarchive directories.
     *
     * @param theDir The dir to check
     * @return true If it is a valid archive directory; otherwise returns false.
     * @throws IOFailure if theDir or one of the valid archive directories does not exist
     * @throws ArgumentNotValid if theDir is null
     */
    protected boolean isBitarchiveDirectory(File theDir) throws ArgumentNotValid, IOFailure {
        ArgumentNotValid.checkNotNull(theDir, "File theDir");
        try {
            return archivedFiles.containsKey(theDir.getCanonicalFile());
        } catch (IOException e) {
            throw new IOFailure("Could not retrieve the canonical file for '" + theDir + "'.", e);
        }
    }

    /**
     * Check that the given file is a directory appropriate for use. A File is appropiate to use as archivedir, if the
     * file is an existing directory, and is writable by this java process.
     *
     * @param file A file
     * @return true, if 'file' is an existing directory and is writable.
     * @throws ArgumentNotValid if 'file' is null.
     */
    private boolean checkArchiveDir(File file) throws ArgumentNotValid {
        ArgumentNotValid.checkNotNull(file, "file");

        if (readOnlyAllowed) {
            log.info("checkArchiveDir skipped for Directory '{}'. Assuming directory is ok due to readOnlyAllowed-Setting set to true", file);
            return true;
        }

        if (!file.exists()) {
            log.warn("Directory '{}' does not exist", file);
            return false;
        }
        if (!file.isDirectory()) {
            log.warn("Directory '{}' is not a directory after all", file);
            return false;
        }
        if (!file.canWrite()) {
            log.warn("Directory '{}' is not writable", file);
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
        // Ensure that the filelist is up to date.
        verifyFilelistUpToDate();
        List<File> files = new ArrayList<File>();
        for (File archivePath : archivedFiles.keySet()) {
            File archiveDir = new File(archivePath, Constants.FILE_DIRECTORY_NAME);
            if (checkArchiveDir(archiveDir)) {
                List<String> filesHere = archivedFiles.get(archivePath);
                for (String filename : filesHere) {
                    files.add(new File(archiveDir, filename));
                }
            }
        }
        return files.toArray(new File[files.size()]);
    }

    /**
     * Return an array of all files in this archive that match a given regular expression on the filename.
     *
     * @param regexp A precompiled regular expression matching whole filenames. This will probably be given to a
     * FilenameFilter
     * @return An array of all the files in this bitarchive that exactly match the regular expression on the filename
     * (sans paths).
     */
    public File[] getFilesMatching(final Pattern regexp) {
        ArgumentNotValid.checkNotNull(regexp, "Pattern regexp");
        // Ensure that the filelist is up to date.
        verifyFilelistUpToDate();
        List<File> files = new ArrayList<File>();
        for (File archivePath : archivedFiles.keySet()) {
            File archiveDir = new File(archivePath, Constants.FILE_DIRECTORY_NAME);
            if (checkArchiveDir(archiveDir)) {
                for (String filename : archivedFiles.get(archivePath)) {
                    if (regexp.matcher(filename).matches()) {
                        files.add(new File(archiveDir, filename));
                    }
                }
            }
        }
        return files.toArray(new File[files.size()]);
    }

    /**
     * Return the path that a given arc file can be found in.
     *
     * @param arcFileName Name of an arc file (with no path)
     * @return A BitarchiveARCFile for the given file, or null if the file does not exist.
     */
    public BitarchiveARCFile lookup(String arcFileName) {
        ArgumentNotValid.checkNotNullOrEmpty(arcFileName, "arcFileName");
        verifyFilelistUpToDate();
        for (File archivePath : archivedFiles.keySet()) {
            File archiveDir = new File(archivePath, Constants.FILE_DIRECTORY_NAME);
            if (checkArchiveDir(archiveDir)) {
                File archiveFile = new File(archiveDir, arcFileName);
                if (archiveFile.exists()) {
                    return new BitarchiveARCFile(arcFileName, archiveFile);
                }
            }
        }
        // the arcfile named "arcFileName" does not exist in this bitarchive.
        log.trace("The arcfile named '{}' does not exist in this bitarchve", arcFileName);
        return null;
    }

    /**
     * Calculate how many bytes are used by all files in a directory.
     *
     * @param filedir An existing directory with a FILE_DIRECTORY_NAME subdir and a TEMPORARY_DIRECTORY_NAME subdir.
     * @return Number of bytes used by all files in the directory (not including overhead from partially used blocks).
     */
    private long calculateBytesUsed(File filedir) {
        long used = 0;
        File[] files = new File(filedir, Constants.FILE_DIRECTORY_NAME).listFiles();
        // Check, that listFiles method returns valid information
        if (files != null) {
            for (File datafiles : files) {
                if (datafiles.isFile()) {
                    // Add size of file f to amount of bytes used.
                    used += datafiles.length();
                } else {
                    log.warn("Non-file '{}' found in archive", datafiles.getAbsolutePath());
                }
            }
        } else {
            log.warn("filedir does not contain a directory named: {}", Constants.FILE_DIRECTORY_NAME);
        }
        File[] tempfiles = new File(filedir, Constants.TEMPORARY_DIRECTORY_NAME).listFiles();
        // Check, that listFiles() method returns valid information
        if (tempfiles != null) {
            for (File tempfile : tempfiles) {
                if (tempfile.isFile()) {
                    // Add size of file f to amount of bytes used.
                    used += tempfile.length();
                } else {
                    log.warn("Non-file '{}' found in archive", tempfile.getAbsolutePath());
                }
            }
        } else {
            log.warn("filedir does not contain a directory named: {}", Constants.TEMPORARY_DIRECTORY_NAME);
        }
        File[] atticfiles = new File(filedir, Constants.ATTIC_DIRECTORY_NAME).listFiles();
        // Check, that listFiles() method returns valid information
        if (atticfiles != null) {
            for (File atticfile : atticfiles) {
                if (atticfile.isFile()) {
                    // Add size of file tempfiles[i] to amount of bytes used.
                    used += atticfile.length();
                } else {
                    log.warn("Non-file '{}' found in archive", atticfile.getAbsolutePath());
                }
            }
        } else {
            log.warn("filedir does not contain a directory named: {}", Constants.ATTIC_DIRECTORY_NAME);
        }
        return used;
    }

    /**
     * Get the one and only instance of the bitarchive admin.
     *
     * @return A BitarchiveAdmin object
     */
    public static synchronized BitarchiveAdmin getInstance() {
        if (instance == null) {
            instance = new BitarchiveAdmin();
        }
        return instance;
    }

    /**
     * Close down the bitarchive admin. Currently has no data to store.
     */
    public void close() {
        archivedFiles.clear();
        archiveTime.clear();
        instance = null;
    }

    /**
     * Return the path used to store files that are removed by RemoveAndGetFileMessage.
     *
     * @param existingFile a File object for an existing file in the bitarchive
     * @return The full path of the file in the attic dir
     */
    public File getAtticPath(File existingFile) {
        ArgumentNotValid.checkNotNull(existingFile, "File existingFile");
        // Find where the file resides so we can use a dir in the same place.
        try {
            existingFile = existingFile.getCanonicalFile();
        } catch (IOException e) {
            throw new IOFailure("Could not retrieve canonical file for '" + existingFile + "'.", e);
        }
        String arcFileName = existingFile.getName();
        File parentDir = existingFile.getParentFile().getParentFile();
        if (!isBitarchiveDirectory(parentDir)) {
            log.warn("Attempt to get attic path for non-archived file '{}'", existingFile);
            throw new ArgumentNotValid("File should belong to a bitarchive dir," + " but " + existingFile + " doesn't");
        }
        // Ensure that 'atticdir' exists. If it doesn't, it is created
        File atticdir = new File(parentDir, Constants.ATTIC_DIRECTORY_NAME);
        ApplicationUtils.dirMustExist(atticdir);
        return new File(atticdir, arcFileName);
    }

}
