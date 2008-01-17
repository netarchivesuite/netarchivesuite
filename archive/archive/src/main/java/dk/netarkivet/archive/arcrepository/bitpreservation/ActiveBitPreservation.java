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
package dk.netarkivet.archive.arcrepository.bitpreservation;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.archive.Constants;
import dk.netarkivet.archive.arcrepositoryadmin.AdminData;
import dk.netarkivet.archive.arcrepositoryadmin.ArcRepositoryEntry;
import dk.netarkivet.archive.arcrepositoryadmin.ReadOnlyAdminData;
import dk.netarkivet.common.distribute.arcrepository.ArcRepositoryClientFactory;
import dk.netarkivet.common.distribute.arcrepository.BatchStatus;
import dk.netarkivet.common.distribute.arcrepository.BitArchiveStoreState;
import dk.netarkivet.common.distribute.arcrepository.Location;
import dk.netarkivet.common.distribute.arcrepository.PreservationArcRepositoryClient;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.PermissionDenied;
import dk.netarkivet.common.utils.CleanupHook;
import dk.netarkivet.common.utils.CleanupIF;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.I18n;
import dk.netarkivet.common.utils.arc.FileBatchJob;

/**
 * Class handling integrity check of a given bit archive.
 * <p/>
 * This class must run on the same machine as the arcrepository, as it uses the
 * same admin data file (read-only).  However, it still talks JMS with the
 * arcrepository.
 */
public class ActiveBitPreservation implements CleanupIF {
    /** the class log. */
    private Log log = LogFactory.getLog(getClass().getName());

    /** Internationalisation object. */
    private static final I18n I18N
            = new I18n(Constants.TRANSLATIONS_BUNDLE);

    /**
     * This should be updated at the entrance of each major use block, to ensure
     * it is reasonably in sync with the file.  We cannot, however, guarantee
     * total sync, as the file can change at any time.  We consider it good
     * enough that it is updated every time there is user interaction.
     */
    private ReadOnlyAdminData admin;

    /**
     * Note that having this as a singleton prevents more than one user from
     * working on this at the same time.  We have no safeguards against abuse!
     */
    private static ActiveBitPreservation instance = null;

    /**
     * Every time we need to store intermediate data during preservation,
     * files will be put in a temp-dir with this prefix.
     */
    private static final String TMP_DIR_PREFIX = "bitpreservation";
    /** Hook to close down application. */
    private CleanupHook closeHook;

    /**
     * Initalises an ActiveBitPreservation instance.
     */
    protected ActiveBitPreservation() {
        this.admin = AdminData.getReadOnlyInstance();
        closeHook = new CleanupHook(this);
        Runtime.getRuntime().addShutdownHook(closeHook);
    }

    /**
     * Get singleton ActiveBitPreservation instance.
     *
     * @return a singleton ActiveBitPreservation instance
     */
    public static ActiveBitPreservation getInstance() {
        synchronized (ActiveBitPreservation.class) {
            if (instance == null) {
                instance = new ActiveBitPreservation();
            }
        }
        return instance;
    }

    /**
     * This method finds out which files in a given bitarchive are
     * misrepresented in the admin data: Either having the wrong checksum or not
     * being marked as uploaded when it actually is.
     * <p/>
     * It uses the admindata file from the DIRS_ARCREPOSITORY_ADMIN directory,
     * as well as the files output by a runChecksumJob.  The erroneous files are
     * stored in files.
     *
     * @param location the bitarchive location the checksumjob came from
     * @throws IOFailure if the given directory does not contain a file
     * checksumjobOutput/unsorted.txt, or it cannot be read
     * @throws PermissionDenied if the output directory cannot be created
     * @throws ArgumentNotValid if arguments location is null
     */
    public void findWrongFiles(Location location) {
    	ArgumentNotValid.checkNotNull(location, "location");
        admin.synchronize();

        // Create set of checksumsfrom bitarchive data
        Set<String> bitarchiveChecksumSet = new HashSet<String>
                (WorkFiles.getLines(location, WorkFiles.CHECKSUMS_ON_BA));

        // Get set of files in arcrepository
        Set<String> arcrepChecksumSet = new HashSet<String>();
        for (String fileName : admin.getAllFileNames()) {
            arcrepChecksumSet.add(ChecksumJob.makeLine(fileName,
                    admin.getCheckSum(fileName)));
        }

        // Get set of completed files in arcrepository
        // Note that these files use the format <filename>##<checksum> to
        // conform to the checksum output.
        Set<String> arcrepCompletedChecksumSet = new HashSet<String>();
        for (String fileName : admin.getAllFileNames(location,
                BitArchiveStoreState.UPLOAD_COMPLETED)) {
            arcrepCompletedChecksumSet.add(ChecksumJob.makeLine(fileName,
                    admin.getCheckSum(fileName)));
        }

        // Find files where checksums differ
        Set<String> wrongChecksums = new HashSet<String>(bitarchiveChecksumSet);
        wrongChecksums.removeAll(arcrepChecksumSet);

        // Find files where state is wrong
        Set<String> wrongStates = new HashSet<String>(bitarchiveChecksumSet);
        wrongStates.removeAll(wrongChecksums);
        wrongStates.removeAll(arcrepCompletedChecksumSet);

        // Remove files unknown in admin data
        for (String checksum : new ArrayList<String>(wrongChecksums)) {
            Map.Entry<String, String> entry = ChecksumJob.parseLine(checksum);
            if (!admin.hasEntry(entry.getKey())) {
                wrongChecksums.remove(checksum);
                wrongStates.remove(checksum);
            }
        }

        // Log result
        log.info("The files " + wrongChecksums
                    + " have wrong checksum in the bitarchive listing in "
                    + WorkFiles.getPreservationDir(location)
                               .getAbsolutePath());

        // Collect all names of files with the wrong checksum
        Set<String> wrongChecksumFilenames = new HashSet<String>();
        for (String checksum : wrongChecksums) {
            Map.Entry<String, String> entry = ChecksumJob.parseLine(checksum);
            wrongChecksumFilenames.add(entry.getKey());
        }

        // Collect all names of files with the wrong state
        Set<String> wrongStateFilenames = new HashSet<String>();
        for (String checksum : wrongStates) {
            Map.Entry<String, String> entry = ChecksumJob.parseLine(checksum);
            wrongStateFilenames.add(entry.getKey());
        }

        // Write output data to the files.
        WorkFiles.write(location, WorkFiles.WRONG_FILES,
                wrongChecksumFilenames);
        WorkFiles.write(location, WorkFiles.WRONG_STATES,
                wrongStateFilenames);
    }

    /**
     * This method takes as input the name of a bitarchive location for which
     * we have previously run a runFileListJob. It reads in the known files in
     * the arcrepository from the AdminData directory specified in the Setting
     * DIRS_ARCREPOSITORY_ADMIN. The two file lists are compared and a
     * subdirectory missingFiles is created with two files: missingba.txt
     * containing missing files, ie those in the arcrepository but not in the
     * bitarchive. This file is unsorted. missingadmindata.txt containing extra
     * files, ie. those found in the bitarchive but not in the arcrepository
     * admin data. This file is unsorted.
     *
     * @param location the location to search for missing files
     * @throws ArgumentNotValid if the given directory does not contain a file
     * filelistOutput/sorted.txt, or the argument location is null
     * @throws PermissionDenied if the output directory cannot be created
     */
    public void findMissingFiles(Location location) {
        ArgumentNotValid.checkNotNull(location, "location");
        log.trace("findMissingFile in dir:"
                     + WorkFiles.getPreservationDir(location));
        admin.synchronize();

        // Create set of file names from bitarchive data
        Set<String> filesInBitarchive = new HashSet<String>
                (WorkFiles.getLines(location, WorkFiles.FILES_ON_BA));

        // Get set of files in arcrepository
        Set<String> arcrepNameSet = admin.getAllFileNames();

        // Find difference set 1
        Set<String> extraFilesInAdminData = new HashSet<String>(arcrepNameSet);
        extraFilesInAdminData.removeAll(filesInBitarchive);

        // Log result
        log.info("The files " + extraFilesInAdminData
                    + " are missing in the bitarchive listing in "
                    + WorkFiles.getPreservationDir(location)
                .getAbsolutePath());

        // Write output data
        WorkFiles.write(location, WorkFiles.MISSING_FILES_BA, extraFilesInAdminData);

        // Find difference set 2
        Set<String> extraFilesInBA = new HashSet<String>(filesInBitarchive);
        extraFilesInBA.removeAll(arcrepNameSet);

        // Log result
        log.info("The files " + extraFilesInBA
                    + " are missing in the admin data listing");

        // Write output data
        WorkFiles.write(location, WorkFiles.MISSING_FILES_ADMINDATA, extraFilesInBA);
        log.trace("Findmissing files -ok");

    }

    /**
     * This methods figures out what actions to take for missing files by
     * comparing checksums for missing files with the reference bitarchive.
     * <p/>
     * It assumes that findMissingFiles has been run first, so its output
     * is available.  It outputs to the following areas defined by WorkFiles:
     * <ul>
     * <li>INSERT_IN_ADMIN: Files present in both bitarchives but
     * not in admin data.</li>
     * <li> DELETE_FROM_ADMIN: Files only present in admin
     * data but in no bitarchives.</li>
     * <li> UPLOAD_TO_BA: Files present in admindata and
     * reference location but not in this bitarchive.</li>
     * <li>DELETE_FROM_BA: Files
     * only present in this location but unknown in both admindata and
     * referencelocation.</li>
     * </ul>
     *
     * @param location The bitarchive we're doing bitpreservation for.
     * @param referenceLocation A different bitarchive used for reference.
     * @throws IOFailure if the necessary output from findMissingFiles cannot be
     * found.
     * @throws PermissionDenied if the output directory cannot be created
     * @throws ArgumentNotValid if arguments are null
     */
    public void generateActionListForMissingFiles(Location location,
                                                  Location referenceLocation) {
        ArgumentNotValid.checkNotNull(location, "location");
        ArgumentNotValid.checkNotNull(referenceLocation, "referenceLocation");
        // Determine which files we need to check - files that appeear
        // problematic in either the bitarchive or admin data are checked
        // against the reference archive to determine the correct action.
        // Read lists of problem files
        List<String> adminFiles =
                WorkFiles.getLines(location, WorkFiles.MISSING_FILES_BA);
        List<String> baFiles =
                WorkFiles.getLines(location, WorkFiles.MISSING_FILES_ADMINDATA);

        List<String> referenceFiles = getReferenceFileList(location,
                referenceLocation, baFiles, adminFiles);

        // Create the four action lists
        Set<String> insertAdmin = new HashSet<String>(baFiles);
        insertAdmin.retainAll(referenceFiles);
        Set<String> deleteAdmin = new HashSet<String>(adminFiles);
        deleteAdmin.removeAll(referenceFiles);
        Set<String> insertBA = new HashSet<String>(adminFiles);
        insertBA.retainAll(referenceFiles);
        Set<String> deleteBA = new HashSet<String>(baFiles);
        deleteBA.removeAll(referenceFiles);

        log.info("Action lists for " + location + " shows "
                + insertAdmin.size() + " files to insert in admin data, "
                + deleteAdmin.size() + " files to delete from admin data, "
                + insertBA.size() + " files to insert in bitarchive, "
                + deleteBA.size() + " files to delete from bitarchive.");

        // Write the four action lists
        WorkFiles.write(location, WorkFiles.INSERT_IN_ADMIN, insertAdmin);
        WorkFiles.write(location, WorkFiles.DELETE_FROM_ADMIN, deleteAdmin);
        WorkFiles.write(location, WorkFiles.UPLOAD_TO_BA, insertBA);
        WorkFiles.write(location, WorkFiles.DELETE_FROM_BA, deleteBA);
    }

    /**
     * Generate and return the list of files from the reference bitarchive, but
     * only for the files that we have problems with.
     *
     * @param location The bitarchive location that we're doing bit
     * preservation for.
     * @param referenceLocation The reference bitarchive location.
     * @param baFiles The list of files that are missing in the bitarchive
     * @param adminFiles The list of files that are missing in admin data
     * @return The list of files on the reference bitarchive, out of those
     * given in baFiles and adminFiles.
     */
    private List<String> getReferenceFileList(Location location,
                                              Location referenceLocation,
                                              List<String> baFiles,
                                              List<String> adminFiles) {
        Set<String> problemFilesSet = new HashSet<String>(adminFiles);
        problemFilesSet.addAll(baFiles);

        // Check status of problem files on reference location
        List<String> problemFiles = new ArrayList<String>(problemFilesSet);
        // Note swapped order of args
        runFileListJob(referenceLocation, location, problemFiles);

        // Read list of problem files on reference location
        return WorkFiles.getLines(location, WorkFiles.FILES_ON_REFERENCE_BA);
    }

    /**
     * Retrieve the preservation status for the file with a given filename.
     *
     * @param filename a given filename
     * @return the preservation status for the file with a given filename,
     * or null if the file named does not exist.
     * @throws ArgumentNotValid if argument is null or the empty string
     */
    public FilePreservationStatus getFilePreservationStatus(String filename) {
    	ArgumentNotValid.checkNotNullOrEmpty(filename, "filename");
        // Start by retrieving the admin status
        admin.synchronize();
        ArcRepositoryEntry ae = admin.getEntry(filename);

        // create and return the preservation status

        if (ae != null) {
            return new FilePreservationStatus(filename, ae);
        } else {
            return null;
        }
    }

    /**
     * Calls upon the arc repository to change the known state for the given
     * file in one bitarchive.  This method uses JMS and blocks until a reply is
     * sent.
     *
     * @param filename The file to change state for
     * @param ba The bitarchive to change state for the file for.
     * @param state The state to change to.
     * @throws ArgumentNotValid if arguments are null or empty strings
     */
    public void setAdminData(String filename, Location ba,
                             BitArchiveStoreState state) {
    	ArgumentNotValid.checkNotNullOrEmpty(filename, "filename");
    	ArgumentNotValid.checkNotNull(ba, "ba");
    	ArgumentNotValid.checkNotNull(state, "state");
        ArcRepositoryClientFactory.getPreservationInstance()
                .updateAdminData(filename, ba.getName(), state);
    }

    /**
     * Calls upon the arc repository to change the known checksum for the given
     * file in one bitarchive.  This method uses JMS and blocks until a reply is
     * sent.
     *
     * @param filename The file to change state for
     * @param checksum The checksum to change to.
     * @throws ArgumentNotValid if arguments are null or empty strings
     */
    public void setAdminChecksum(String filename, String checksum) {
    	ArgumentNotValid.checkNotNullOrEmpty(filename, "filename");
    	ArgumentNotValid.checkNotNullOrEmpty(checksum, "checksum");
        ArcRepositoryClientFactory.getPreservationInstance()
                .updateAdminChecksum(filename, checksum);
    }

    /**
     * Runs a checksum job on the bit archive at the given location. Output
     * is written to file returned by WorkFiles.getChecksumOutputFile(location).
     *
     * @param location One of the bitarchive locations.
     * @throws IOFailure If unable to create output dirs or if unable to
     * write/read output to files.
     */
    public void runChecksumJob(Location location) {
        runChecksumJob(location, null);
    }

    /**
     * Runs a checksum job on the bit archive at the given location. Output
     * is written to file returned by WorkFiles.getChecksumOutputFile(location).
     *
     * @param location One of the bitarchive locations.
     * @param specifiedFiles Only process specfied files. May be null, meaning
     * all files.
     * @throws IOFailure If unable to create output dirs or if unable to
     * write/read output to files.
     */
    public void runChecksumJob(Location location,
                               List<String> specifiedFiles) {
        ArgumentNotValid.checkNotNull(location, "location");
        // Create directories for output
        File outputFile = WorkFiles.getFile(location, WorkFiles.CHECKSUMS_ON_BA);

        // Send checksum batch job
        log.info("Bit integrity check started on bit archive "
                    + location);
        runBatchJob(new ChecksumJob(), location, specifiedFiles, outputFile);
    }

    /**
     * Method to get a list of all files in a given bitarchive.  The result
     * is stored in the file list output area (WorkFiles.FILES_ON_BA).
     *
     * @param location the location where the given bitarchive lies
     * @throws PermissionDenied if the output directories cannot be created
     * @throws IOFailure if there is a problem writing the output file, or if
     * the job fails for some reason
     * @throws ArgumentNotValid
     */
    public void runFileListJob(Location location)
            throws ArgumentNotValid, IOFailure {
        runFileListJob(location, null, null);
    }

    /**
     * Method to get a list of all files in a given bitarchive and store list in
     * the given directory. The result is stored (unsorted) in the area
     * specified by WorkFiles.FILES_ON_BA or
     * WorkFiles.FILES_ON_REFERENCE_BA.
     *
     * @param location the location where the given bitarchive lies
     * @param referencedBy The location that we are in the process of
     * doing bit preservation for. This determines where output is placed.
     * @param specifiedFiles only run the job on the specified files. May be
     * null, meaning all files
     * @throws PermissionDenied if the output directories cannot be created
     * @throws IOFailure if there is a problem writing the output file, or if
     * the job fails for some reason
     * @throws ArgumentNotValid
     */
    public void runFileListJob(Location location, Location referencedBy,
                               List<String> specifiedFiles)
            throws ArgumentNotValid, IOFailure {
        // Pick the right directory to output to
        File batchOutputFile;
        if (referencedBy != null) {
            batchOutputFile =
            WorkFiles.getFile(referencedBy, WorkFiles.FILES_ON_REFERENCE_BA);
        } else {
            batchOutputFile = WorkFiles.getFile(location, WorkFiles.FILES_ON_BA);
        }
        log.trace("runFileListJob for archive:" + location
                     + " output file:" + batchOutputFile);

        // Send filelist batch job
        runBatchJob(new FileListJob(), location, specifiedFiles,
                    batchOutputFile);
    }

    /**
     * Run any batch job on a location, possibly restricted to a certain set
     * of files, and place the output in the given file.  The results will also
     * be checked to verify that there for each file processed is a line in the
     * output file.
     * @param job The job to run.
     * @param location The location (bitarchive) that the job should run
     * on.
     * @param specifiedFiles The files to run the job on, or null if it should
     * run on all files.
     * @param batchOutputFile Where to put the result of the job.
     */
    private void runBatchJob(FileBatchJob job, Location location,
                             List<String> specifiedFiles,
                             File batchOutputFile) {
        job.processOnlyFilesNamed(specifiedFiles);
        BatchStatus status = ArcRepositoryClientFactory.getPreservationInstance()
                .batch(job, location.getName());

        // Write output to file, if we got any
        if (status.hasResultFile()) {
            status.copyResults(batchOutputFile);
            checkNumberOfLines(batchOutputFile, status);
        }

        // Report errors
        if (!status.getFilesFailed().isEmpty()) {
            reportBatchErrors(status);
        }
        log.info("FileBatchJob succeeded and processed "
                + status.getNoOfFilesProcessed() + " files on bitarchive at "
                + location);
    }

    /**
     * Check that the file returned by a batch job contains one line per file
     * that has been *successfully* processed by the batch job.
     *
     * @param unsortedFile The file containing the output
     * @param status The status message that contains the information on the
     * number of file process
     */
    private void checkNumberOfLines(File unsortedFile, BatchStatus status) {
        int expectedNumberOfLines = status.getNoOfFilesProcessed()
                                    - status.getFilesFailed().size();
        long lines = FileUtils.countLines(unsortedFile);
        if (lines != expectedNumberOfLines) {
            log.warn("Number of files found (" + lines
                           + ") does not match with number reported by job ("
                           + expectedNumberOfLines + "). Files found are:\n "
                           + FileUtils.readListFromFile(unsortedFile));
        }
    }

    /**
     * Extract and concatenate error information from a batch job, reporting it
     * in the log at Warning level.
     *
     * @param batchStatus The status report from a batch job.
     */
    private void reportBatchErrors(BatchStatus batchStatus) {
        StringBuilder s = new StringBuilder();
        for (File file : batchStatus.getFilesFailed()) {
            s.append(file.getName());
            s.append("\n");
        }
        log.warn("Bit integrity check failed on "
                       + batchStatus.getFilesFailed().size()
                       + " files in bitarchive "
                       + batchStatus.getBitArchiveAppId()
                       + ":\n" + s);
    }

    /**
     * Reestablish a file missing in a bitarchive. The following pre-conditions
     * for reestablishing the file are checked before changing anything:
     * 1) the file is registered correctly in AdminData
     * 2) the file is missing in the given bitarchive
     * 3) the file is present in another bitarchive (the reference archive)
     * 4) admin data and the reference archive agree on the
     * checksum of the file.
     *
     * If these conditions are not satisfied, an error is
     * appended to result and we return false without changing the bitarchives.
     *
     * @param fileName name of the file to reestablish
     * @param damagedBitarchive Name of the bitarchive missing the file
     * @param l the locale
     * @param result Output buffer for writing a textual description of the result of the operation
     * @return True if reestablishing succeeded
     */
    public boolean reestablishMissingFile(
            String fileName,
            Location damagedBitarchive,
            StringBuilder result, Locale l) {
        log.debug("Reestablishing missing file '" + fileName
                + "' in bitarchive '" + damagedBitarchive + "'.");
        FilePreservationStatus status = getFilePreservationStatus(fileName);
        if (null == status) {
            result.append(I18N.getString(l,
                    "errmsg;attempt.at.getting.preservation.status.for.file.0.failed",
                    fileName));
            log.warn("Attempt at getting preservation status for file '"
                    + fileName + "'.");
            return false;
        }
        if (!satisfiesMissingFileConditions(status, damagedBitarchive,
                fileName, result, l)) {
            return false;
        }
        // Retrieve the file from the reference archive
        Location referenceArchive = status.getReferenceBitarchive();
        try {
            PreservationArcRepositoryClient arcrep =
                ArcRepositoryClientFactory.getPreservationInstance();
            File tmpDir = FileUtils.createUniqueTempDir(FileUtils.getTempDir(),
                    TMP_DIR_PREFIX);
            File missingFile = new File(tmpDir, fileName);
            arcrep.getFile(fileName, referenceArchive, missingFile);
            arcrep.store(missingFile);
            tmpDir.delete();
        } catch (IOFailure e) {
            result.append(I18N.getString(l,
                    "errmsg;attempt.at.restoring.0.in.bitarchive.at.1.failed",
                    fileName, damagedBitarchive.getName()));
            result.append("<br/>" + e.toString() + "<br/>");
            log.warn("Failed to reestablish " + fileName +
                    " in " + damagedBitarchive.getName()
                    + " with copy from " + referenceArchive,e);
            return false;
        }
        result.append(I18N.getString(l,
                "attempt.at.restoring.0.in.bitarchive.at.1.succeeded",
                fileName, damagedBitarchive.getName()) + "<br/>");

        log.info("Reestablished " + fileName
                + " in "+  damagedBitarchive.getName()
                + " with copy from " + referenceArchive.getName());
        return true;
    }

    /**
     * Checks the conditions that must be true before reestablishing a missing file.
     * Returns true if and only if all of the below are true;
     * returns false and outputs a message on the given StringBuilder otherwise.
     *
     * 1) the file is registered correctly in AdminData
     * 2) the file is missing in the given bitarchive
     * 3) the file is present in another bitarchive (the reference archive)
     * 4) admin data and the reference archive agree on the checksum
     *
     * @param status the status for one file in the bitarchives
     * @param damagedBitarchive the location where the file is corrupt
     *  or missing
     * @param fileName the name of the file being considered
     * @param result the StringBuilder object containing the result.
     * @param l the current Locale
     *
     * @return true if all conditions are true, false otherwise.
     */
    private boolean satisfiesMissingFileConditions(
            FilePreservationStatus status,
            Location damagedBitarchive,
            String fileName,
            StringBuilder result, Locale l) {
        // condition 1
        if (!status.isAdminDataOk()) {
            result.append(I18N.getString(l,
                    "errmsg;admin.data.not.consistent.for.file.0", fileName));
            log.warn("Admin.data is not consistent regarding file: " + fileName);
            return false;
        }
        // condition 2
        if (!status.fileIsMissing(damagedBitarchive)) {
            result.append(I18N.getString(l,
                    "errmsg;file.0.not.missing.in.bitarchive.on.1",
                    fileName, damagedBitarchive.getName()));
            log.warn("File '" + fileName + "' is not missing in bitarchive on location '"
                    + damagedBitarchive.getName() + "'.");
            return false;
        }
        // conditions 3 and 4
        Location referenceArchive = status.getReferenceBitarchive();
        if (referenceArchive == null) {
            result.append(I18N.getString(l,
                    "errmsg;no.correct.version.of.0.in.any.archive",
                    fileName));
            log.warn("No correct version of file '" + fileName
                    + "' in any archive");
           return false;
        }
        return true;
    }

    /**
     * Call upon the arc repository to remove a file, returning it to this
     * machine.  The file is left around in case problems are later discovered,
     * and its location can be found in the log.
     *
     * @param filename The file to remove.
     * @param bitarchive The bitarchive to remove the file from.
     * @param checksum The checksum of the file.
     * @param credentials Credentials required to run this operation.
     */
    public void removeAndGetFile(String filename, Location bitarchive,
                                 String checksum, String credentials) {
    	ArgumentNotValid.checkNotNullOrEmpty(filename, "filename");
    	ArgumentNotValid.checkNotNullOrEmpty(checksum, "checksum");
    	ArgumentNotValid.checkNotNullOrEmpty(credentials, "credentials");
    	ArgumentNotValid.checkNotNull(bitarchive, "bitarchive");
        ArcRepositoryClientFactory.getPreservationInstance()
                .removeAndGetFile(filename, bitarchive.getName(), checksum,
                        credentials);
    }

    /**
     * Shut down cleanly.
     */
    public void close() {
        if (closeHook != null) {
            Runtime.getRuntime().removeShutdownHook(closeHook);
        }
        closeHook = null;
        cleanup();
    }
    /**
     * @see CleanupIF#cleanup()
     */
    public void cleanup() {
        // In case a listener was set up, remove it.
        ArcRepositoryClientFactory.getPreservationInstance().close();
        instance = null;
    }
}
