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
package dk.netarkivet.archive.arcrepository.bitpreservation;

import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.Set;

import dk.netarkivet.archive.ArchiveSettings;
import dk.netarkivet.common.distribute.arcrepository.Replica;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.IllegalState;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.Settings;

/**
 * This class encapsulates access to the files used in bitpreservation.
 *
 * The following files are encapsulated:
 *
 * "unsorted.txt": Unsorted list of files in a bitarchive
 * "sorted.txt": Sorted list of files in a bitarchive
 *
 * "missingba.txt": Files that are missing in a bitarchive
 * "missingadmindata.txt"; Files that are missing from admin data
 * "wrongfiles.txt": Files with wrong checksum???
 * "referenceba.txt"; File list from reference ba?
 *
 * "wrongstates.txt"; Files that are in wrong state
 * "insertinadmin.txt"; Files to insert into admin data
 * "deletefromadmin.txt"; Files to delete from admin data
 * "uploadtoba.txt"; Files to upload to the bitarchive
 * "deletefromba.txt"; Files to delete from the bitarchive
 *
 */
public enum WorkFiles {
    MISSING_FILES_BA, MISSING_FILES_ADMINDATA, WRONG_FILES,
    FILES_ON_REFERENCE_BA, INSERT_IN_ADMIN, DELETE_FROM_ADMIN,
    UPLOAD_TO_BA, DELETE_FROM_BA, WRONG_STATES, FILES_ON_BA, CHECKSUMS_ON_BA ;

    /** Directory and filenames to be used by ActiveBitPreservation when
     * checking for missing files in a bitarchive.
     */
    private static final String FILELIST_OUTPUT_DIR = "filelistOutput";
    private static final String MISSING_FILES_DIR = "missingFiles";
    private static final String CHECKSUM_DIR = "checksums";
    private static final String WRONGFILESDIR = "wrongFiles";
    private static final String BA_LIST_DIR = "referenceFiles";
    private static final String ACTION_LIST_DIR = "actionList";

    private static final String FILE_LISTING_FILENAME = "unsorted.txt";
    private static final String SORTED_OUTPUT_FILE = "sorted.txt";
    private static final String MISSING_FILES_BA_FILENAME = "missingba.txt";
    private static final String MISSING_FILES_ADMINDATA_FILENAME = "missingadmindata.txt";
    private static final String WRONG_FILES_FILENAME = "wrongfiles.txt";
    private static final String INSERT_IN_ADMIN_FILENAME = "insertinadmin.txt";
    private static final String DELETE_FROM_ADMIN_FILENAME = "deletefromadmin.txt";
    private static final String UPLOAD_TO_BA_FILENAME = "uploadtoba.txt";
    private static final String DELETE_FROM_BA_FILENAME = "deletefromba.txt";
    private static final String WRONG_STATES_FILENAME = "wrongstates.txt";

    /** Get the name of the file (sans dir) that corresponds to a given
     * type of workfile.
     *
     * @param fileType the type of workfile
     * @return A string with the filename (sans dir) of the corresponding file
     */
    private static String getFileName(WorkFiles fileType) {
        switch (fileType) {
            case MISSING_FILES_BA: return MISSING_FILES_BA_FILENAME;
            case MISSING_FILES_ADMINDATA: return MISSING_FILES_ADMINDATA_FILENAME;
            case WRONG_FILES: return WRONG_FILES_FILENAME;
            case INSERT_IN_ADMIN: return INSERT_IN_ADMIN_FILENAME;
            case DELETE_FROM_ADMIN: return DELETE_FROM_ADMIN_FILENAME;
            case UPLOAD_TO_BA: return UPLOAD_TO_BA_FILENAME;
            case DELETE_FROM_BA: return DELETE_FROM_BA_FILENAME;
            case WRONG_STATES: return  WRONG_STATES_FILENAME;
            case FILES_ON_REFERENCE_BA:
            case FILES_ON_BA:
            case CHECKSUMS_ON_BA: return FILE_LISTING_FILENAME;
        }
        throw new IllegalState("Impossible workfile type " + fileType);
    }

    /** Get the directory that files of a given type should go into.
     *
     * @param rep The replica that we're doing bitpreservation for.
     * @param fileType The type of file we will be using.
     * @return A directory (created if necessary) under the bitpreservation
     * dir (see settings) for the file to go into.
     */
    private static File getDir(Replica rep, WorkFiles fileType) {
        switch (fileType) {
            case MISSING_FILES_BA:
            case MISSING_FILES_ADMINDATA:
                return makeRelativeDir(rep, MISSING_FILES_DIR);
            case WRONG_STATES:
            case WRONG_FILES:
                return makeRelativeDir(rep, WRONGFILESDIR);
            case FILES_ON_BA:
                return getFilelistOutputDir(makeRelativeDir(rep, MISSING_FILES_DIR));
            case FILES_ON_REFERENCE_BA:
                return getFilelistOutputDir(makeRelativeDir(rep, BA_LIST_DIR));
            case INSERT_IN_ADMIN:
            case DELETE_FROM_ADMIN:
            case UPLOAD_TO_BA:
            case DELETE_FROM_BA:
                return makeRelativeDir(rep, ACTION_LIST_DIR);
            case CHECKSUMS_ON_BA:
                return getFilelistOutputDir(makeRelativeDir(rep, CHECKSUM_DIR));
        }
        throw new IllegalState("Impossible workfile type " + fileType);
    }

    /** Get the directory that file listings are to live in, creating it if
     * necessary.
     * @param dir The directory that the file listings should live under.
     * Note that this is not directly derived from the name of the replica,
     * as it can also be used for reference file listings.
     * @return The directory to put file listings in under the given dir.
     */
    private static File getFilelistOutputDir(File dir) {
        String filelistOutputDir = FILELIST_OUTPUT_DIR;
        File outputDir = new File(dir, filelistOutputDir);
        FileUtils.createDir(outputDir);
        return outputDir;
    }

    /** Get the base dir for all files related to bitpreservation for a
     * given bitarchive.
     *
     * @param replica The name of a bitarchive.
     * @return The directory to place bitpreservation for the archive under.
     */
    static File getPreservationDir(Replica replica) {
        File base = new File(Settings.get(
                ArchiveSettings.DIR_ARCREPOSITORY_BITPRESERVATION));
        return new File(base, replica.getId());
    }

    /** Make a directory object relative to an replica, creating the
     * directory if necessary.
     *
     * @param replica The replica that we're doing bitpreservation for
     * @param dirname The name of the directory to create
     * @return An object representing the directory
     */
    private static File makeRelativeDir(Replica replica, String dirname) {
        File dir = getPreservationDir(replica);
        File outputDir = new File(dir, dirname);
        FileUtils.createDir(outputDir);
        return outputDir;
    }

    /** Get a sorted file from an unsorted one, updating if necessary.
     *
     * @param unsortedFile An unsorted file
     * @return A file that contains the same lines as unsortedFile, but
     * sorted.  The file will be placed in the same directory as the input
     * file, but have the name Constants.SORTED_OUTPUT_FILE defines.
     */
    static File getSortedFile(File unsortedFile) {
        unsortedFile = unsortedFile.getAbsoluteFile();
        File sortedOutput = new File(unsortedFile.getParentFile(),
                SORTED_OUTPUT_FILE);
        if (!unsortedFile.exists()) {
            throw new IOFailure("Unsorted input file '" + unsortedFile
                    + "' does not exist");
        }
        if (unsortedFile.lastModified() >= sortedOutput.lastModified()) {
            FileUtils.makeSortedFile(unsortedFile, sortedOutput);
        }
        return sortedOutput;
    }

    /* public interfaces below here */
    public static void write(Replica replica, WorkFiles missingFilesBa,
                             Set<String> files) {
    	ArgumentNotValid.checkNotNull(replica, "replica");
    	ArgumentNotValid.checkNotNull(missingFilesBa, "missingFilesBa");
    	ArgumentNotValid.checkNotNull(files, "files");
        FileUtils.writeCollectionToFile(getFile(replica, missingFilesBa),
                files);
    }

    public static File getFile(Replica rep, WorkFiles fileType) {
    	ArgumentNotValid.checkNotNull(rep, "rep");
    	ArgumentNotValid.checkNotNull(fileType, "fileType");
        return new File(getDir(rep, fileType), getFileName(fileType));
    }

    public static Date getLastUpdate(Replica rep, WorkFiles fileType) {
    	ArgumentNotValid.checkNotNull(rep, "rep");
    	ArgumentNotValid.checkNotNull(fileType, "fileType");
        return new Date(getFile(rep, fileType).lastModified());
    }

    public static long getLineCount(Replica rep, WorkFiles fileType) {
    	ArgumentNotValid.checkNotNull(rep, "rep");
    	ArgumentNotValid.checkNotNull(fileType, "fileType");
        return FileUtils.countLines(getFile(rep, fileType));
    }

    public static void removeLine(Replica rep, WorkFiles fileType, String line) {
    	ArgumentNotValid.checkNotNull(rep, "rep");
    	ArgumentNotValid.checkNotNull(fileType, "fileType");
    	ArgumentNotValid.checkNotNullOrEmpty(line, "line");
        FileUtils.removeLineFromFile(line, getFile(rep, fileType));
    }

    public static List<String> getLines(Replica replica,
                                        WorkFiles fileType) {
    	ArgumentNotValid.checkNotNull(replica, "replica");
    	ArgumentNotValid.checkNotNull(fileType, "fileType");
        return FileUtils.readListFromFile(getFile(replica, fileType));
    }

}
