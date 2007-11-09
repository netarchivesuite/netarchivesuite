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
package dk.netarkivet.archive.arcrepository.bitpreservation;

import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.Set;

import dk.netarkivet.common.Settings;
import dk.netarkivet.common.distribute.arcrepository.Location;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.IllegalState;
import dk.netarkivet.common.utils.FileUtils;

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
     * @param loc The location that we're doing bitpreservation for.
     * @param fileType The type of file we will be using.
     * @return A directory (created if necessary) under the bitpreservation
     * dir (see settings) for the file to go into.
     */
    private static File getDir(Location loc, WorkFiles fileType) {
        switch (fileType) {
            case MISSING_FILES_BA:
            case MISSING_FILES_ADMINDATA:
                return makeRelativeDir(loc, MISSING_FILES_DIR);
            case WRONG_STATES:
            case WRONG_FILES:
                return makeRelativeDir(loc, WRONGFILESDIR);
            case FILES_ON_BA:
                return getFilelistOutputDir(makeRelativeDir(loc, MISSING_FILES_DIR));
            case FILES_ON_REFERENCE_BA:
                return getFilelistOutputDir(makeRelativeDir(loc, BA_LIST_DIR));
            case INSERT_IN_ADMIN:
            case DELETE_FROM_ADMIN:
            case UPLOAD_TO_BA:
            case DELETE_FROM_BA:
                return makeRelativeDir(loc, ACTION_LIST_DIR);
            case CHECKSUMS_ON_BA:
                return getFilelistOutputDir(makeRelativeDir(loc, CHECKSUM_DIR));
        }
        throw new IllegalState("Impossible workfile type " + fileType);
    }

    /** Get the directory that file listings are to live in, creating it if
     * necessary.
     * @param dir The directory that the file listings should live under.
     * Note that this is not directly derived from the name of the location,
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
     * @param location The name of a bitarchive.
     * @return The directory to place bitpreservation for the archive under.
     */
    static File getPreservationDir(Location location) {
        File base = new File(Settings.get(Settings.DIR_ARCREPOSITORY_BITPRESERVATION));
        return new File(base, location.getName());
    }

    /** Make a directory object relative to an location, creating the
     * directory if necessary.
     *
     * @param location The location that we're doing bitpreservation for
     * @param dirname The name of the directory to create
     * @return An object representing the directory
     */
    private static File makeRelativeDir(Location location, String dirname) {
        File dir = getPreservationDir(location);
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
    public static void write(Location location, WorkFiles missingFilesBa,
                             Set<String> files) {
    	ArgumentNotValid.checkNotNull(location, "location");
    	ArgumentNotValid.checkNotNull(missingFilesBa, "missingFilesBa");
    	ArgumentNotValid.checkNotNull(files, "files");
        FileUtils.writeCollectionToFile(getFile(location, missingFilesBa),
                files);
    }

    public static File getFile(Location loc, WorkFiles fileType) {
    	ArgumentNotValid.checkNotNull(loc, "loc");
    	ArgumentNotValid.checkNotNull(fileType, "fileType");
        return new File(getDir(loc, fileType), getFileName(fileType));
    }

    public static Date getLastUpdate(Location loc, WorkFiles fileType) {
    	ArgumentNotValid.checkNotNull(loc, "loc");
    	ArgumentNotValid.checkNotNull(fileType, "fileType");
        return new Date(getFile(loc, fileType).lastModified());
    }

    public static long getLineCount(Location loc, WorkFiles fileType) {
    	ArgumentNotValid.checkNotNull(loc, "loc");
    	ArgumentNotValid.checkNotNull(fileType, "fileType");
        return FileUtils.countLines(getFile(loc, fileType));
    }

    public static void removeLine(Location loc, WorkFiles fileType, String line) {
    	ArgumentNotValid.checkNotNull(loc, "loc");
    	ArgumentNotValid.checkNotNull(fileType, "fileType");
    	ArgumentNotValid.checkNotNullOrEmpty(line, "line");
        FileUtils.removeLineFromFile(line, getFile(loc, fileType));
    }

    public static List<String> getLines(Location location,
                                        WorkFiles fileType) {
    	ArgumentNotValid.checkNotNull(location, "location");
    	ArgumentNotValid.checkNotNull(fileType, "fileType");
        return FileUtils.readListFromFile(getFile(location, fileType));
    }

}
