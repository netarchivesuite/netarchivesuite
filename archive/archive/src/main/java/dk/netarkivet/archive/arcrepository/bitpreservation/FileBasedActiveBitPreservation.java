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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
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
import dk.netarkivet.common.exceptions.IllegalState;
import dk.netarkivet.common.exceptions.NetarkivetException;
import dk.netarkivet.common.exceptions.NotImplementedException;
import dk.netarkivet.common.exceptions.PermissionDenied;
import dk.netarkivet.common.utils.CleanupHook;
import dk.netarkivet.common.utils.CleanupIF;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.I18n;
import dk.netarkivet.common.utils.KeyValuePair;
import dk.netarkivet.common.utils.arc.FileBatchJob;

/**
 * Class handling integrity check of a given bit archive. <p/> This class must
 * run on the same machine as the arcrepository, as it uses the same admin data
 * file (read-only).  However, it still talks JMS with the arcrepository.
 */
public class FileBasedActiveBitPreservation
        implements ActiveBitPreservation, CleanupIF {
    /** the class log. */
    private static Log log = LogFactory.getLog(FileBasedActiveBitPreservation.class);

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
    private static FileBasedActiveBitPreservation instance = null;

    /**
     * Every time we need to store intermediate data during preservation, files
     * will be put in a temp-dir with this prefix.
     */
    private static final String TMP_DIR_PREFIX = "bitpreservation";
    /** Hook to close down application. */
    private CleanupHook closeHook;

    /** Initalises an ActiveBitPreservation instance. */
    protected FileBasedActiveBitPreservation() {
        this.admin = AdminData.getReadOnlyInstance();
        closeHook = new CleanupHook(this);
        Runtime.getRuntime().addShutdownHook(closeHook);
    }

    /**
     * Get singleton ActiveBitPreservation instance.
     *
     * @return a singleton ActiveBitPreservation instance
     */
    public static FileBasedActiveBitPreservation getInstance() {
        synchronized (FileBasedActiveBitPreservation.class) {
            if (instance == null) {
                instance = new FileBasedActiveBitPreservation();
            }
        }
        return instance;
    }

    /**
     * Retrieve the preservation status for the file with a given filename.
     *
     * @param filename a given filename
     *
     * @return the preservation status for the file with a given filename, or
     *         null if the file named does not exist.
     *
     * @throws ArgumentNotValid if argument is null or the empty string
     */
    public FilePreservationStatus getFilePreservationStatus(String filename) {
        ArgumentNotValid.checkNotNullOrEmpty(filename, "filename");
        // Start by retrieving the admin status
        admin.synchronize();
        ArcRepositoryEntry ae = admin.getEntry(filename);

        // create and return the preservation status
        if (ae != null) {
            return new FilePreservationStatus(filename, ae, getChecksumMap(
                    filename));
        } else {
            return null;
        }
    }

    /** Generate a map of checksums for this file in the bitarchive.
     *
     * @return Map containing the output of checksum jobs from the bitarchives.
     * @param filename
     */
    static Map<Location, List<String>> getChecksumMap(String filename) {
        // get the checksum information
        Map<Location, List<String>> baname2checksum =
                new HashMap<Location, List<String>>();
        for (Location ba : Location.getKnown()) {
            List<String> checksum = getChecksums(ba, filename);
            log.debug("Putting checksum line '" + checksum + "' in for " + ba);
            baname2checksum.put(ba, checksum);
        }
        return baname2checksum;
    }

    /**
     * Get the checksum of a single file in a bitarchive.
     *
     * Note that this method runs a batch job on the bitarchives, and therefore
     * takes a long time.
     *
     * @param ba The bitarchive to ask for checksum
     * @param filename
     * @return The MD5 checksums of the file, or the empty string if the file was
     *         not in the bitarchive.
     * @see ChecksumJob#parseLine(String)
     */
    static List<String> getChecksums(Location ba, String filename) {
        ChecksumJob checksumJob = new ChecksumJob();
        checksumJob.processOnlyFileNamed(filename);
        String res;
        try {
            PreservationArcRepositoryClient arcrep =
                    ArcRepositoryClientFactory.getPreservationInstance();
            BatchStatus batchStatus = arcrep.batch(checksumJob, ba.getName());
            if (batchStatus.hasResultFile()) {
                ByteArrayOutputStream buf = new ByteArrayOutputStream();
                batchStatus.appendResults(buf);
                res = buf.toString();
            } else {
                res = "";
            }
        } catch (NetarkivetException e) {
            log.warn("Error asking '" + ba + "' for checksums", e);
            return Collections.emptyList();
        }
        List<String> checksums = new ArrayList<String>();
        if (res.length() > 0) {
            String[] lines = res.split("\n");
            for (String s : lines) {
                try {
                    KeyValuePair<String, String> fileChecksum = ChecksumJob.parseLine(s);
                    if (!fileChecksum.getKey().equals(filename)) {
                        log.debug("Got checksum for unexpected file '"
                                + fileChecksum.getKey() + " while asking " + ba
                                + " for checksum of '" + filename + "'");
                    } else {
                        checksums.add(fileChecksum.getValue());
                    }
                } catch (ArgumentNotValid e) {
                    log.warn("Got malformed checksum '" + res
                            + "' while asking '" + ba + "' for checksum of '"
                            + filename + "'");
                }
            }
        }
        return checksums;
    }

    /**
     * Get a list of missing files in a given bitarchive.
     *
     * @param bitarchive a given bitarchive
     *
     * @return a list of missing files in a given bitarchive.
     *
     * @throws IllegalState if the file with the list cannot be found.
     */
    public Iterable<String> getMissingFiles(Location bitarchive) {
        File missingOutput = WorkFiles.getFile(bitarchive,
                                               WorkFiles.MISSING_FILES_BA);
        if (!missingOutput.exists()) {
            throw new IllegalState("Could not find the file: "
                                   + missingOutput.getAbsolutePath());
        }
        return FileUtils.readListFromFile(missingOutput);
    }

    /**
     * TODO: Integrate in getMissingFiles
     *
     * This method takes as input the name of a bitarchive location for which we
     * have previously run a runFileListJob. It reads in the known files in the
     * arcrepository from the AdminData directory specified in the Setting
     * DIRS_ARCREPOSITORY_ADMIN. The two file lists are compared and a
     * subdirectory missingFiles is created with two files: missingba.txt
     * containing missing files, ie those in the arcrepository but not in the
     * bitarchive. This file is unsorted. missingadmindata.txt containing extra
     * files, ie. those found in the bitarchive but not in the arcrepository
     * admin data. This file is unsorted.
     *
     * @param location the location to search for missing files
     *
     * @throws ArgumentNotValid if the given directory does not contain a file
     *                          filelistOutput/sorted.txt, or the argument
     *                          location is null
     * @throws PermissionDenied if the output directory cannot be created
     */
    public void findMissingFiles(Location location) {
        ArgumentNotValid.checkNotNull(location, "location");
        runFileListJob(location);
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
        WorkFiles.write(location, WorkFiles.MISSING_FILES_BA,
                        extraFilesInAdminData);

        // Find difference set 2
        Set<String> extraFilesInBA = new HashSet<String>(filesInBitarchive);
        extraFilesInBA.removeAll(arcrepNameSet);

        // Log result
        log.info("The files " + extraFilesInBA
                 + " are missing in the admin data listing");

        // Write output data
        WorkFiles.write(location, WorkFiles.MISSING_FILES_ADMINDATA,
                        extraFilesInBA);
        log.trace("Findmissing files -ok");
    }

    /**
     * TODO: Integrate in getMissingFiles
     *
     * Method to get a list of all files in a given bitarchive. The result is
     * stored (unsorted) in the area specified by WorkFiles.FILES_ON_BA.
     *
     * @param location       the location where the given bitarchive lies
     * @throws PermissionDenied if the output directories cannot be created
     * @throws IOFailure        if there is a problem writing the output file,
     *                          or if the job fails for some reason
     */
    public void runFileListJob(Location location)
            throws IOFailure {
        // Pick the right directory to output to
        File batchOutputFile = WorkFiles.getFile(location,
                                                WorkFiles.FILES_ON_BA);
        log.trace("runFileListJob for archive:" + location
                  + " output file:" + batchOutputFile);

        // Send filelist batch job
        runBatchJob(new FileListJob(), location, null, batchOutputFile);
    }

    /**
     * Get a list of wrong files in a given bitarchive.
     *
     * @param bitarchive a bitarchive
     *
     * @return a list of wrong files in a given bitarchive.
     *
     * @throws IllegalState if the file with the list cannot be found.
     */
    public Iterable<String> getChangedFiles(Location bitarchive) {
        File wrongFilesOutput = WorkFiles.getFile(bitarchive,
                                                  WorkFiles.WRONG_FILES);

        if (!wrongFilesOutput.exists()) {
            throw new IllegalState("Could not find the file: "
                                   + wrongFilesOutput.getAbsolutePath());
        }

        // Create set of file names from bitarchive data
        return FileUtils.readListFromFile(wrongFilesOutput);
    }

    /**
     * TODO: Integrate in getChangedFiles
     *
     * This method finds out which files in a given bitarchive are
     * misrepresented in the admin data: Either having the wrong checksum or not
     * being marked as uploaded when it actually is. <p/> It uses the admindata
     * file from the DIRS_ARCREPOSITORY_ADMIN directory, as well as the files
     * output by a runChecksumJob.  The erroneous files are stored in files.
     *
     * @param location the bitarchive location the checksumjob came from
     *
     * @throws IOFailure        if the given directory does not contain a file
     *                          checksumjobOutput/unsorted.txt, or it cannot be
     *                          read
     * @throws PermissionDenied if the output directory cannot be created
     * @throws ArgumentNotValid if arguments location is null
     */
    public void findWrongFiles(Location location) {
        ArgumentNotValid.checkNotNull(location, "location");
        runChecksumJob(location);
        admin.synchronize();

        // Create set of checksumsfrom bitarchive data
        Set<String> bitarchiveChecksumSet = new HashSet<String>
                (WorkFiles.getLines(location, WorkFiles.CHECKSUMS_ON_BA));

        // Get set of files in arcrepository
        Set<String> arcrepChecksumSet = new HashSet<String>();
        for (String fileName : admin.getAllFileNames()) {
            arcrepChecksumSet.add(ChecksumJob.makeLine(fileName,
                                                       admin.getCheckSum(
                                                               fileName)));
        }

        // Get set of completed files in arcrepository
        // Note that these files use the format <filename>##<checksum> to
        // conform to the checksum output.
        Set<String> arcrepCompletedChecksumSet = new HashSet<String>();
        for (String fileName : admin.getAllFileNames(location,
                                                     BitArchiveStoreState.UPLOAD_COMPLETED)) {
            arcrepCompletedChecksumSet.add(ChecksumJob.makeLine(fileName,
                                                                admin.getCheckSum(
                                                                        fileName)));
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
     * TODO: Integrate in getChangedFiles
     *
     * Runs a checksum job on the bit archive at the given location. Output is
     * written to file returned by WorkFiles.getChecksumOutputFile(location).
     *
     * @param location       One of the bitarchive locations.
     * @throws IOFailure If unable to create output dirs or if unable to
     *                   write/read output to files.
     */
    public void runChecksumJob(Location location) {
        ArgumentNotValid.checkNotNull(location, "location");
        // Create directories for output
        File outputFile = WorkFiles.getFile(location,
                                            WorkFiles.CHECKSUMS_ON_BA);

        // Send checksum batch job
        log.info("Bit integrity check started on bit archive "
                 + location);
        runBatchJob(new ChecksumJob(), location, null, outputFile);
    }

    /**
     * Return the number of files found in the bitarchive. If no information
     * found about the bitarchive -1 is returned
     *
     * @param bitarchive the bitarchive to check
     *
     * @return the number of files found in the bitarchive
     */
    public long getNumberOfFiles(Location bitarchive) {
        ArgumentNotValid.checkNotNull(bitarchive, "bitarchive");
        File unsortedOutput = WorkFiles.getFile(bitarchive,
                                                WorkFiles.FILES_ON_BA);

        if (!unsortedOutput.exists()) {
            return -1;
        }

        return FileUtils.countLines(unsortedOutput);
    }

    /**
     * Get the number of missing files in a given bitarchive.
     *
     * @param bitarchive a given bitarchive
     *
     * @return the number of missing files in the given bitarchive.
     */
    public long getNumberOfMissingFiles(Location bitarchive) {
        ArgumentNotValid.checkNotNull(bitarchive, "bitarchive");

        File missingOutput = WorkFiles.getFile(bitarchive,
                                               WorkFiles.MISSING_FILES_BA);
        if (!missingOutput.exists()) {
            return -1;
        }

        return FileUtils.countLines(missingOutput);
    }

    /**
     * Get the number of wrong files for a bitarchive.
     *
     * @param bitarchive a bitarchive
     *
     * @return the number of wrong files for the bitarchive.
     */
    public long getNumberOfChangedFiles(Location bitarchive) {
        ArgumentNotValid.checkNotNull(bitarchive, "bitarchive");
        File wrongFileOutput = WorkFiles.getFile(bitarchive,
                                                 WorkFiles.WRONG_FILES);

        if (!wrongFileOutput.exists()) {
            return -1;
        }

        return FileUtils.countLines(wrongFileOutput);
    }

    public Date getDateForChangedFiles(Location location) {
        return WorkFiles.getLastUpdate(location, WorkFiles.WRONG_FILES);
    }

    public Date getDateForMissingFiles(Location location) {
        return WorkFiles.getLastUpdate(location, WorkFiles.FILES_ON_BA);
    }



    /**
     * Check that files are indeed missing, and present in admin data and
     * reference location. If so, upload missing files from reference location
     * to this location.
     *
     * @param location The location to restore files to
     * @param filename The names of the files.
     *
     * @throws IOFailure if the file cannot be reestablished
     * @throws PermissionDenied if the file is not in correct state
     */
    public void reuploadMissingFiles(Location location, String... filename) {
        for (String fn : filename) {
            FilePreservationStatus fps = getFilePreservationStatus(fn);
            if (!fps.isAdminDataOk()) {
                setAdminData(fn, location);
                admin.synchronize();
            }
            StringBuilder res = new StringBuilder();
            if (!reestablishMissingFile(fn, location, res, Locale.ENGLISH)) {
                throw new IOFailure(res.toString());
            }

        }
    }

    /**
     * TODO: Integrate in reuploadMissingFiles
     *
     * Reestablish a file missing in a bitarchive. The following pre-conditions
     * for reestablishing the file are checked before changing anything: 1) the
     * file is registered correctly in AdminData 2) the file is missing in the
     * given bitarchive 3) the file is present in another bitarchive (the
     * reference archive) 4) admin data and the reference archive agree on the
     * checksum of the file.
     *
     * If these conditions are not satisfied, an error is appended to result and
     * we return false without changing the bitarchives.
     *
     * @param fileName          name of the file to reestablish
     * @param damagedBitarchive Name of the bitarchive missing the file
     * @param l                 the locale
     * @param result            Output buffer for writing a textual description
     *                          of the result of the operation
     *
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
                                         fileName,
                                         damagedBitarchive.getName()));
            result.append("<br/>" + e.toString() + "<br/>");
            log.warn("Failed to reestablish " + fileName +
                     " in " + damagedBitarchive.getName()
                     + " with copy from " + referenceArchive, e);
            return false;
        }
        result.append(I18N.getString(l,
                                     "attempt.at.restoring.0.in.bitarchive.at.1.succeeded",
                                     fileName, damagedBitarchive.getName())
                      + "<br/>");

        log.info("Reestablished " + fileName
                 + " in " + damagedBitarchive.getName()
                 + " with copy from " + referenceArchive.getName());
        FileUtils.removeLineFromFile(fileName, WorkFiles.getFile(damagedBitarchive,
                                               WorkFiles.MISSING_FILES_BA));
        FileUtils.appendToFile(WorkFiles.getFile(damagedBitarchive,
                                                 WorkFiles.FILES_ON_BA), fileName);
        return true;
    }

    /**
     * TODO: Integrate in reuploadMissingFiles
     *
     * Checks the conditions that must be true before reestablishing a missing
     * file. Returns true if and only if all of the below are true; returns
     * false and outputs a message on the given StringBuilder otherwise.
     *
     * 1) the file is registered correctly in AdminData 2) the file is missing
     * in the given bitarchive 3) the file is present in another bitarchive (the
     * reference archive) 4) admin data and the reference archive agree on the
     * checksum
     *
     * @param status            the status for one file in the bitarchives
     * @param damagedBitarchive the location where the file is corrupt or
     *                          missing
     * @param fileName          the name of the file being considered
     * @param result            the StringBuilder object containing the result.
     * @param l                 the current Locale
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
                                         "errmsg;admin.data.not.consistent.for.file.0",
                                         fileName));
            result.append("<br/>");
            log.warn(
                    "Admin.data is not consistent regarding file: " + fileName);
            return false;
        }
        // condition 2
        if (!status.fileIsMissing(damagedBitarchive)) {
            result.append(I18N.getString(l,
                                         "errmsg;file.0.not.missing.in.bitarchive.on.1",
                                         fileName,
                                         damagedBitarchive.getName()));
            result.append("<br/>");
            log.warn("File '" + fileName
                     + "' is not missing in bitarchive on location '"
                     + damagedBitarchive.getName() + "'.");
            return false;
        }
        // conditions 3 and 4
        Location referenceArchive = status.getReferenceBitarchive();
        if (referenceArchive == null) {
            result.append(I18N.getString(l,
                                         "errmsg;no.correct.version.of.0.in.any.archive",
                                         fileName));
            result.append("<br/>");
            log.warn("No correct version of file '" + fileName
                     + "' in any archive");
            return false;
        }
        return true;
    }

    /**
     * Check that file checksum is indeed different to admin data and reference
     * location. If so, remove missing file and upload it from reference
     * location to this location.
     *
     * @param location The location to restore file to
     * @param filename The name of the file.
     * @param checksum The expected checksum.
     * @throws IOFailure if the file cannot be reestablished
     * @throws PermissionDenied if the file is not in correct state
     */
    public void replaceChangedFile(Location location, String filename,
                                   String credentails, String checksum) {
        removeAndGetFile(filename, location, checksum, credentails);
        reuploadMissingFiles(location, filename);
    }

    /**
     * TODO: Integrate in replaceChangedFile
     * Call upon the arc repository to remove a file, returning it to this
     * machine.  The file is left around in case problems are later discovered,
     * and its location can be found in the log.
     *
     * @param filename    The file to remove.
     * @param bitarchive  The bitarchive to remove the file from.
     * @param checksum    The checksum of the file.
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
        FileUtils.appendToFile(WorkFiles.getFile(bitarchive,
                                                 WorkFiles.MISSING_FILES_BA),
                               filename);
        FileUtils.removeLineFromFile(filename, WorkFiles.getFile(bitarchive,
                                                 WorkFiles.FILES_ON_BA));
    }

    /**
     * Return a list of files present in bitarchive but missing in AdminData.
     *
     * @return A list of missing files.
     *
     * @throws IOFailure if the list cannot be generated.
     */
    public Iterable getMissingFilesForAdminData() {
        //TODO: implement method
        throw new NotImplementedException("Not implemented");
    }

    /**
     * Return a list of files with wrong checksum or status in admin data.
     *
     * @return A list of files with wrong checksum or status.
     *
     * @throws IOFailure if the list cannot be generated.
     */
    public Iterable getChangedFilesForAdminData() {
        //TODO: implement method
        throw new NotImplementedException("Not implemented");
    }

    /**
     * Reestablish admin data to match bitarchive states for files.
     *
     * @param filename The files to reestablish state for.
     *
     * @throws PermissionDenied if the file is not in correct state
     */
    public void addMissingFilesToAdminData(String... filename) {
        //TODO: implement method
        throw new NotImplementedException("Not implemented");
    }

    /**
     * Reestablish admin data to match bitarchive states for file.
     *
     * @param filename The file to reestablish state for.
     *
     * @throws PermissionDenied if the file is not in correct state
     */
    public void changeStatusForAdminData(String filename) {
        //TODO: implement method
        throw new NotImplementedException("Not implemented");
    }

    /**
     * TODO: Integrate in changeStatusForAdminData
     *
     * Calls upon the arc repository to change the known state for the given
     * file in one bitarchive.  This method uses JMS and blocks until a reply is
     * sent.
     *
     * @param filename The file to change state for
     * @param ba       The bitarchive to change state for the file for.
     * @throws ArgumentNotValid if arguments are null or empty strings
     */
    public void setAdminData(String filename, Location ba
    ) {
        ArgumentNotValid.checkNotNullOrEmpty(filename, "filename");
        ArgumentNotValid.checkNotNull(ba, "ba");
        ArcRepositoryClientFactory.getPreservationInstance()
                .updateAdminData(filename, ba.getName(),
                                 BitArchiveStoreState.UPLOAD_FAILED);
    }

    /**
     * TODO: Integrate in changeStatusForAdminData
     *
     * Calls upon the arc repository to change the known checksum for the given
     * file in one bitarchive.  This method uses JMS and blocks until a reply is
     * sent.
     *
     * @param filename The file to change state for
     * @param checksum The checksum to change to.
     *
     * @throws ArgumentNotValid if arguments are null or empty strings
     */
    public void setAdminChecksum(String filename, String checksum) {
        ArgumentNotValid.checkNotNullOrEmpty(filename, "filename");
        ArgumentNotValid.checkNotNullOrEmpty(checksum, "checksum");
        ArcRepositoryClientFactory.getPreservationInstance()
                .updateAdminChecksum(filename, checksum);
    }

















    /**
     * Run any batch job on a location, possibly restricted to a certain set of
     * files, and place the output in the given file.  The results will also be
     * checked to verify that there for each file processed is a line in the
     * output file.
     *
     * @param job             The job to run.
     * @param location        The location (bitarchive) that the job should run
     *                        on.
     * @param specifiedFiles  The files to run the job on, or null if it should
     *                        run on all files.
     * @param batchOutputFile Where to put the result of the job.
     */
    private void runBatchJob(FileBatchJob job, Location location,
                             List<String> specifiedFiles,
                             File batchOutputFile) {
        job.processOnlyFilesNamed(specifiedFiles);
        BatchStatus status
                = ArcRepositoryClientFactory.getPreservationInstance()
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
     * @param status       The status message that contains the information on
     *                     the number of file process
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

    /** Shut down cleanly. */
    public void close() {
        if (closeHook != null) {
            Runtime.getRuntime().removeShutdownHook(closeHook);
        }
        closeHook = null;
        cleanup();
    }

    /** @see CleanupIF#cleanup() */
    public void cleanup() {
        // In case a listener was set up, remove it.
        ArcRepositoryClientFactory.getPreservationInstance().close();
        instance = null;
    }

}
