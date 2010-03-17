/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2010 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301 
 *  USA
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
    /** 
     * The MISSING_FILES_BA is the workfile for the list of missing files 
     * for a bitarchive. 
     */
    MISSING_FILES_BA, 
    /** 
     * The MISSING_FILES_ADMINDATA is the workfile for the list of 
     * missing files for the admin data. 
     */
    MISSING_FILES_ADMINDATA, 
    /** WRONG_FILES. Files with wrong checksum??? .*/
    WRONG_FILES,
    /** FILES_ON_REFERENCE_BA. File list from reference ba?.*/
    FILES_ON_REFERENCE_BA, 
    /** INSERT_IN_ADMIN. Files to insert into admin data .*/
    INSERT_IN_ADMIN, 
    /** DELETE_FROM_ADMIN. Files to delete from admin data .*/
    DELETE_FROM_ADMIN,
    /** UPLOAD_TO_BA. Files to upload to the bitarchive .*/
    UPLOAD_TO_BA,
    /** DELETE_FROM_BA. Files to delete from the bitarchive .*/
    DELETE_FROM_BA, 
    /** WRONG_STATES. Files that are in wrong state .*/
    WRONG_STATES, 
    /** FILES_ON_BA. Unsorted list of files in a bitarchive .*/
    FILES_ON_BA, 
    /** CHECKSUMS_ON_BA. Unsorted list of files in a bitarchive .*/
    CHECKSUMS_ON_BA;

    /** 
     * Directory and filenames to be used by ActiveBitPreservation when
     * checking for missing files in a bitarchive.
     */
    private static final String FILELIST_OUTPUT_DIR = "filelistOutput";
    /** missingFiles .*/
    private static final String MISSING_FILES_DIR = "missingFiles";
    /** checksums .*/
    private static final String CHECKSUM_DIR = "checksums";
    /** wrongFiles .*/
    private static final String WRONGFILESDIR = "wrongFiles";
    /** referenceFiles .*/
    private static final String BA_LIST_DIR = "referenceFiles";
    /** actionsList .*/
    private static final String ACTION_LIST_DIR = "actionList";

    /** unsorted.txt .*/
    private static final String FILE_LISTING_FILENAME = "unsorted.txt";
    /** sorted.txt .*/
    private static final String SORTED_OUTPUT_FILE = "sorted.txt";
    /** missingba.txt .*/
    private static final String MISSING_FILES_BA_FILENAME = "missingba.txt";
    /** missingadmindata.txt .*/
    private static final String MISSING_FILES_ADMINDATA_FILENAME = 
        "missingadmindata.txt";
    /** wrongfiles.txt .*/
    private static final String WRONG_FILES_FILENAME = "wrongfiles.txt";
    /** insertinadmin.txt .*/
    private static final String INSERT_IN_ADMIN_FILENAME = "insertinadmin.txt";
    /** deletefromadmin.txt .*/
    private static final String DELETE_FROM_ADMIN_FILENAME = 
        "deletefromadmin.txt";
    /** uploadtoba.txt .*/
    private static final String UPLOAD_TO_BA_FILENAME = "uploadtoba.txt";
    /** deletefromba.txt .*/
    private static final String DELETE_FROM_BA_FILENAME = "deletefromba.txt";
    /** wrongstates.txt .*/
    private static final String WRONG_STATES_FILENAME = "wrongstates.txt";

    /** Get the name of the file (sans dir) that corresponds to a given
     * type of workfile.
     *
     * @param fileType the type of workfile.
     * @return A string with the filename (sans dir) of the corresponding file.
     * @throws IllegalState If it is an unsupported file type.
     */
    private static String getFileName(WorkFiles fileType) 
            throws IllegalState {
        // check the filetype
        switch (fileType) {
            case MISSING_FILES_BA: return MISSING_FILES_BA_FILENAME;
            case MISSING_FILES_ADMINDATA: 
                return MISSING_FILES_ADMINDATA_FILENAME;
            case WRONG_FILES: return WRONG_FILES_FILENAME;
            case INSERT_IN_ADMIN: return INSERT_IN_ADMIN_FILENAME;
            case DELETE_FROM_ADMIN: return DELETE_FROM_ADMIN_FILENAME;
            case UPLOAD_TO_BA: return UPLOAD_TO_BA_FILENAME;
            case DELETE_FROM_BA: return DELETE_FROM_BA_FILENAME;
            case WRONG_STATES: return  WRONG_STATES_FILENAME;
            case FILES_ON_REFERENCE_BA:
            case FILES_ON_BA:
            case CHECKSUMS_ON_BA: return FILE_LISTING_FILENAME;
            default: 
                throw new IllegalState("Impossible workfile type " + fileType);
        }
    }

    /** 
     * Get the directory that files of a given type should go into.
     *
     * @param rep The replica that we're doing bitpreservation for.
     * @param fileType The type of file we will be using.
     * @return A directory (created if necessary) under the bitpreservation
     * dir (see settings) for the file to go into.
     * @throws IllegalState If it is an unsupported file type.
     */
    private static File getDir(Replica rep, WorkFiles fileType) 
            throws IllegalState {
        // check the type
        switch (fileType) {
            case MISSING_FILES_BA:
            case MISSING_FILES_ADMINDATA:
                return makeRelativeDir(rep, MISSING_FILES_DIR);
            case WRONG_STATES:
            case WRONG_FILES:
                return makeRelativeDir(rep, WRONGFILESDIR);
            case FILES_ON_BA:
                return getFilelistOutputDir(makeRelativeDir(rep, 
                        MISSING_FILES_DIR));
            case FILES_ON_REFERENCE_BA:
                return getFilelistOutputDir(makeRelativeDir(rep, BA_LIST_DIR));
            case INSERT_IN_ADMIN:
            case DELETE_FROM_ADMIN:
            case UPLOAD_TO_BA:
            case DELETE_FROM_BA:
                return makeRelativeDir(rep, ACTION_LIST_DIR);
            case CHECKSUMS_ON_BA:
                return getFilelistOutputDir(makeRelativeDir(rep, CHECKSUM_DIR));
            default: 
                throw new IllegalState("Impossible workfile type " + fileType);
        }
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
    protected static File getPreservationDir(Replica replica) {
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
     * @throws IOFailure If the file does not exist.
     */
    protected static File getSortedFile(File unsortedFile) throws IOFailure {
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

    /* public interfaces below here. */
    /**
     * Method for writing the list of files to a work file.
     * 
     * @param replica The replica for the working file.
     * @param fileType The type of working file.
     * @param files The list of filenames (?) to write to the working file.
     */
    public static void write(Replica replica, WorkFiles fileType,
            Set<String> files) {
        ArgumentNotValid.checkNotNull(replica, "Replica replica");
        ArgumentNotValid.checkNotNull(fileType, "WorkFiles fileType");
        ArgumentNotValid.checkNotNull(files, "Set<String> files");
        FileUtils.writeCollectionToFile(getFile(replica, fileType),
                files);
    }

    /**
     * Method for retrieving a working file.
     * Note: it is not tested, whether the file exists.
     * 
     * @param rep The replica to whom the file corresponds.
     * @param fileType The type of working file.
     * @return The requested working file.
     */
    public static File getFile(Replica rep, WorkFiles fileType) {
        ArgumentNotValid.checkNotNull(rep, "Replica rep");
        ArgumentNotValid.checkNotNull(fileType, "WorkFiles fileType");
        return new File(getDir(rep, fileType), getFileName(fileType));
    }

    /**
     * Method for retrieving the last modified date of a working file for a
     * specific replica.
     * 
     * FIXME this might throw odd exceptions if the file does not exist.
     * 
     * @param rep The replica for the working file.
     * @param fileType The type of working file.
     * @return The last modified date for the working file.
     */
    public static Date getLastUpdate(Replica rep, WorkFiles fileType) {
        ArgumentNotValid.checkNotNull(rep, "Replica rep");
        ArgumentNotValid.checkNotNull(fileType, "WorkFiles fileType");
        return new Date(getFile(rep, fileType).lastModified());
    }

    /**
     * Method for retrieving the lines of a working file for a specific 
     * replica.
     * 
     * @param replica The replica of the working file.
     * @param fileType The type of workfile.
     * @return A list containing the lines of the file.
     */
    public static List<String> getLines(Replica replica,
                                        WorkFiles fileType) {
        ArgumentNotValid.checkNotNull(replica, "Replica replica");
        ArgumentNotValid.checkNotNull(fileType, "WorkFiles fileType");
        return FileUtils.readListFromFile(getFile(replica, fileType));
    }
}
