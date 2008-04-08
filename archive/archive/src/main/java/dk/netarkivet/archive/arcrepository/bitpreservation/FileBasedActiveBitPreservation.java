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
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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
import dk.netarkivet.common.utils.KeyValuePair;
import dk.netarkivet.common.utils.StringUtils;
import dk.netarkivet.common.utils.arc.FileBatchJob;

/**
 * Class handling integrity check of the arcrepository. <p/> This class must
 * run on the same machine as the arcrepository, as it uses the same admin data
 * file (read-only).  However, it still talks JMS with the arcrepository.
 */
public class FileBasedActiveBitPreservation
        implements ActiveBitPreservation, CleanupIF {
    /** The class log. */
    private static final Log log
            = LogFactory.getLog(FileBasedActiveBitPreservation.class);

    /**
     * When replacing a broken file, the broken file is downloaded and stored in
     * a temporary directory under Settings.COMMON_TEMP_DIR with this name.
     * It can then be inspected at your leisure.
     */
    private static final String REMOVED_FILES = "bitpreservation";

    /**
     * This should be updated at the entrance of each major use block, to ensure
     * it is reasonably in sync with the file.  We cannot, however, guarantee
     * total sync, as the file can change at any time.  We consider it good
     * enough that it is updated every time there is user interaction.
     */
    private ReadOnlyAdminData admin;

    /**
     * File preservation is done in a singleton, which means that any user using
     * the file preservation interface will update the same state.
     *
     * Nothing breaks by two users simultaneously do bit preservation actions,
     * but it may have undesirable consequences, such as two users
     * simultaneously starting checksum jobs of the full archive.
     */
    private static FileBasedActiveBitPreservation instance;

    /** Hook to close down application. */
    private CleanupHook closeHook;

    /** Initialises a FileBasedActiveBitPreservation instance. */
    protected FileBasedActiveBitPreservation() {
        this.admin = AdminData.getReadOnlyInstance();
        this.closeHook = new CleanupHook(this);
        Runtime.getRuntime().addShutdownHook(closeHook);
    }

    /**
     * Get singleton instance.
     *
     * @return the singleton.
     */
    public static synchronized FileBasedActiveBitPreservation getInstance() {
        if (instance == null) {
            instance = new FileBasedActiveBitPreservation();
        }
        return instance;
    }

    /**
     * Retrieve the preservation status for the files with the given filenames. 
     * This will ask for a fresh checksum from the bitarchives and admin data.
     *
     * @param filenames List of filenames
     *
     * @return a map of the preservation status for the given files.
     * The preservationstate is null, if the file named does
     * not exist in admin data.
     *
     * @throws ArgumentNotValid if argument is null
     */
    public Map<String, FilePreservationState> getFilePreservationStateMap(
            String... filenames) {
        ArgumentNotValid.checkNotNull(filenames, "String... filenames");
        // Start by retrieving the admin status
        admin.synchronize();
            
        // temporary datastructures to hold admindata info
        Map<String, ArcRepositoryEntry> adminInfo
        = new HashMap<String, ArcRepositoryEntry>();
        Set<String> missingInAdmindata = new HashSet<String>();
        
        for (String filename: filenames) {
            ArcRepositoryEntry ae = admin.getEntry(filename);
            if (ae != null){
                adminInfo.put(filename, ae);
            } else {
                missingInAdmindata.add(filename);
            }
        }
        
        // create and return the preservation status

        Map<String, FilePreservationState> filepreservationStates 
            = new HashMap<String, FilePreservationState>();
        
        // Add null-entries for the files absent from admindata. 
        for (String missing: missingInAdmindata) {
            filepreservationStates.put(missing, null);
        }
        if (missingInAdmindata.size() > 0) {
            log.warn("The following " + missingInAdmindata.size()
                    + " files are unknown to admindata: "
                    + StringUtils.conjoin(",", missingInAdmindata));
        }
        // For every filename present in admin data,
        // get a Map that maps from location -> checksums
        // This takes a long time.
        Map<String, Map<Location, List<String>>> checksumMaps 
            = getChecksumMaps(adminInfo.keySet());
        
        // construct FilePreservationState objects for all filenames in
        // admin data
        for (String filename: adminInfo.keySet()) {
            filepreservationStates.put(filename, 
                    new FilePreservationState(filename,
                            adminInfo.get(filename),
                            checksumMaps.get(filename)
                            )
            );
        }
        return filepreservationStates;
    }
    
    /**
     * Get FilePreservationState for the given file in the bitarchives
     * and admin data.
     * @param filename A given file
     * @return the FilePreservationState for the given file. This will be null,
     * if the filename is not found in admin data.
     */
    public FilePreservationState getFilePreservationState(String filename) {
        ArgumentNotValid.checkNotNullOrEmpty(filename, "String filename");
        Map<String, FilePreservationState> filepreservationStates
            = getFilePreservationStateMap(filename);
        
        return filepreservationStates.get(filename);
    }
    
    
    /**
     * Generate a map of checksums for these filenames in the bitarchives.
     *
     * @param filenames The filenames to get the checksums for.
     *
     * @return Map containing the output of checksum jobs from the bitarchives.
     */
    private Map<String, Map<Location, List<String>>>
    getChecksumMaps(Set<String> filenames) {
     
        Map<String, Map<Location, List<String>>> checksummaps =
                new HashMap<String, Map<Location, List<String>>>();
        
        //Only make one checksum job for each location
        for (Location ba : Location.getKnown()) {
            // get the checksum information
            Map<String, List<String>> checksums = getChecksums(ba, filenames);
            log.debug("Adding checksums for location '"
                      + ba + "' for filenames: "
                      + StringUtils.conjoin(",", filenames));
            for (String filename : filenames) {
                // update checksummaps
                Map<Location, List<String>> locationMap;
                if (checksummaps.containsKey(filename)) {
                    locationMap = checksummaps.get(filename);
                } else {
                    locationMap = new HashMap<Location, List<String>>();
                    checksummaps.put(filename, locationMap);
                }
                
                List<String> checksumsForFileOnBa = checksums.get(filename);
                if (checksumsForFileOnBa == null) {
                    // Checksum for file not available on location ba
                    checksumsForFileOnBa = new ArrayList<String>();
                }
                locationMap.put(ba, checksumsForFileOnBa);
            }
        }
        return checksummaps;
    }

    /**
     * Get the checksum of a list of files in a bitarchive.
     *
     * Note that this method runs a batch job on the bitarchives, and therefore
     * may take a long time, depending on network delays.
     *
     * @param ba       The bitarchive to ask for checksum
     * @param filenames The names of the files to ask for checksums for
     *
     * @return The MD5 checksums of the files, or the empty string if the file
     *         was not in the bitarchive.
     *
     * @see ChecksumJob#parseLine(String)
     */
    private Map<String, List<String>> getChecksums(
            Location ba, Set<String> filenames) {
                
        ChecksumJob checksumJob = new ChecksumJob();
        
        checksumJob.processOnlyFilesNamed(new ArrayList<String>(filenames));
        
        String batchResult;
        try {
            PreservationArcRepositoryClient arcrep =
                    ArcRepositoryClientFactory.getPreservationInstance();
            BatchStatus batchStatus = arcrep.batch(checksumJob, ba.getName());
            if (batchStatus.hasResultFile()) {
                ByteArrayOutputStream buf = new ByteArrayOutputStream();
                batchStatus.appendResults(buf);
                try {
                    batchResult = buf.toString("UTF-8");
                } catch (UnsupportedEncodingException e) {
                    throw new IOFailure(
                            "Should never happen: Unsupported encoding UTF-8",
                            e);
                }
            } else {
                batchResult = "";
            }
        } catch (NetarkivetException e) {
            // TODO Shouldn't we rather throw an exception here?
            log.warn("Error asking location '" + ba + "' for checksums", e);
            return Collections.emptyMap();
        }
        
        Map<String, List<String>> filesAndChecksums
            = new HashMap<String, List<String>>();
        
        
        // parse the batchResult
        if (batchResult.length() > 0) {
            String[] lines = batchResult.split("\n");
                for (String s : lines) {
                    try {
                        KeyValuePair<String, String> fileChecksum
                                = ChecksumJob.parseLine(s);
                        final String filename = fileChecksum.getKey();
                        final String checksum = fileChecksum.getValue();
                        if (!filenames.contains(filename)) {
                            log.debug(
                                    "Got checksum for unexpected file '"
                                    + filename + " while asking "
                                    + "location '" + ba
                                    + "' for checksum of the following files: '"
                                    + filenames + "'");
                        } else {
                            // Add checksum to list associated with filename
                            List<String> checksums;
                             if (filesAndChecksums.containsKey(filename)) {
                                 checksums = filesAndChecksums.get(filename); 
                             } else {
                                 checksums = new ArrayList<String>();
                                 filesAndChecksums.put(filename, checksums);
                             }
                             checksums.add(checksum);
                        }
                    } catch (ArgumentNotValid e) {
                        log.warn("Got malformed checksum '" + s
                                 + "' while asking location '" + ba
                                 + "' for checksum of the following files: '"
                                 + filenames + "'");
                    }
                }
        } else {
            log.debug("Empty result returned from ChecksumJob " + checksumJob);
        }
                
        return filesAndChecksums;
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
        ArgumentNotValid.checkNotNull(bitarchive, "Location bitarchive");
        File missingOutput = WorkFiles.getFile(bitarchive,
                                               WorkFiles.MISSING_FILES_BA);
        if (!missingOutput.exists()) {
            throw new IllegalState("Could not find the file: "
                                   + missingOutput.getAbsolutePath());
        }
        return FileUtils.readListFromFile(missingOutput);
    }

    /**
     * This method takes as input the name of a bitarchive location for which we
     * wish to run a FileListJob. It also reads in the known files in the
     * arcrepository from the AdminData directory specified in the Setting
     * DIRS_ARCREPOSITORY_ADMIN. The two file lists are compared and a
     * subdirectory missingFiles is created with two unsorted files:
     * 'missingba.txt' containing missing files, ie those registered in the
     * admin data, but not found in the bitarchive, and 'missingadmindata.txt'
     * containing extra files, ie. those found in the bitarchive but not in the
     * arcrepository admin data.
     *
     * TODO The second file is never used on the current implementation.
     *
     * FIXME: It is unclear if the decision if which files are missing isn't
     * better suited to be in getMissingFiles, so this method only runs the
     * batch job.
     *
     * @param location the location to search for missing files
     *
     * @throws ArgumentNotValid if the given directory does not contain a file
     *                          filelistOutput/sorted.txt, or the argument
     *                          location is null
     * @throws PermissionDenied if the output directory cannot be created
     */
    public void findMissingFiles(Location location) {
        ArgumentNotValid.checkNotNull(location, "Location location");
        runFileListJob(location);
        log.trace("findMissingFile in dir '"
                  + WorkFiles.getPreservationDir(location) + "'");
        admin.synchronize();

        // Create set of file names from bitarchive data
        Set<String> filesInBitarchive = new HashSet<String>(
                WorkFiles.getLines(location, WorkFiles.FILES_ON_BA));

        // Get set of files in arcrepository
        Set<String> arcrepNameSet = admin.getAllFileNames();

        // Find difference set 1
        Set<String> extraFilesInAdminData = new HashSet<String>(arcrepNameSet);
        extraFilesInAdminData.removeAll(filesInBitarchive);

        // Log result
        if (extraFilesInAdminData.size() > 0) {
            log.warn("The " + extraFilesInAdminData.size() + " files '"
                     + new ArrayList<String>(extraFilesInAdminData).subList(0,
                             Math.min(extraFilesInAdminData.size(), 10))
                     + "' have wrong checksum in the bitarchive listing in '"
                     + WorkFiles.getPreservationDir(location)
                    .getAbsolutePath() + "'");
        }

        // Write output data
        WorkFiles.write(location, WorkFiles.MISSING_FILES_BA,
                        extraFilesInAdminData);

        // Find difference set 2
        Set<String> extraFilesInBA = new HashSet<String>(filesInBitarchive);
        extraFilesInBA.removeAll(arcrepNameSet);

        // Log result
        if (extraFilesInBA.size() > 0) {
            log.warn("The " + extraFilesInBA.size() + " files '"
                     + new ArrayList<String>(extraFilesInBA).subList(0,
                             Math.min(extraFilesInBA.size(), 10))
                     + "' have wrong checksum in the bitarchive listing in '"
                     + WorkFiles.getPreservationDir(location)
                    .getAbsolutePath() + "'");
        }

        // Write output data
        WorkFiles.write(location, WorkFiles.MISSING_FILES_ADMINDATA,
                        extraFilesInBA);
        log.trace("Findmissing files - done");
    }

    /**
     * Method to get a list of all files in a given bitarchive. The result is
     * stored (unsorted) in the area specified by WorkFiles.FILES_ON_BA.
     *
     * @param location the location where the given bitarchive lies
     *
     * @throws PermissionDenied if the output directories cannot be created
     * @throws IOFailure        if there is a problem writing the output file,
     *                          or if the job fails for some reason
     */
    private void runFileListJob(Location location) throws IOFailure {
        // Pick the right directory to output to
        File batchOutputFile = WorkFiles.getFile(location,
                                                 WorkFiles.FILES_ON_BA);
        log.trace("runFileListJob for location '" + location
                  + "', output file '" + batchOutputFile + "'");

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
        ArgumentNotValid.checkNotNull(bitarchive, "Location bitarchive");
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
     * This method finds out which files in a given bitarchive are
     * misrepresented in the admin data: Either having the wrong checksum or not
     * being marked as uploaded when it actually is. <p/> It uses the admindata
     * file from the DIRS_ARCREPOSITORY_ADMIN directory, as well as the files
     * output by a runChecksumJob.  The erroneous files are stored in files.
     *
     * FIXME: It is unclear if the decision if which files are changed isn't
     * better suited to be in getChangedFiles, so this method only runs the
     * batch job.
     *
     * @param location the bitarchive location the checksumjob came from
     *
     * @throws IOFailure        On file or network trouble.
     * @throws PermissionDenied if the output directory cannot be created
     * @throws ArgumentNotValid if argument location is null
     */
    public void findChangedFiles(Location location) {
        ArgumentNotValid.checkNotNull(location, "Location location");
        runChecksumJob(location);
        admin.synchronize();

        // Create set of checksumsfrom bitarchive data
        Set<String> bitarchiveChecksumSet = new HashSet<String>(
                WorkFiles.getLines(location, WorkFiles.CHECKSUMS_ON_BA));

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
        for (String fileName : admin.getAllFileNames(
                location,
                BitArchiveStoreState.UPLOAD_COMPLETED)) {
            arcrepCompletedChecksumSet.add(ChecksumJob.makeLine(
                    fileName, admin.getCheckSum(fileName)));
        }

        // Find files where checksums differ
        Set<String> wrongChecksums = new HashSet<String>(bitarchiveChecksumSet);
        wrongChecksums.removeAll(arcrepChecksumSet);

        // Find files where state is wrong
        Set<String> wrongStates = new HashSet<String>(bitarchiveChecksumSet);
        wrongStates.removeAll(wrongChecksums);
        wrongStates.removeAll(arcrepCompletedChecksumSet);

        // Remove files unknown in admin data (note  - these are not ignored,
        // they will be handled by missing files operations)
        for (String checksum : new ArrayList<String>(wrongChecksums)) {
            Map.Entry<String, String> entry = ChecksumJob.parseLine(checksum);
            if (!admin.hasEntry(entry.getKey())) {
                wrongChecksums.remove(checksum);
                wrongStates.remove(checksum);
            }
        }

        // Log result
        if (wrongChecksums.size() > 0) {
            log.warn("The " + wrongChecksums.size() + " files '"
                     + new ArrayList<String>(wrongChecksums).subList(0,
                             Math.min(wrongChecksums.size(), 10))
                     + "' have wrong checksum in the bitarchive listing in '"
                     + WorkFiles.getPreservationDir(location)
                    .getAbsolutePath() + "'");
        }
        if (wrongStates.size() > 0) {
            log.warn("The " + wrongStates.size() + " files '"
                     + new ArrayList<String>(wrongStates).subList(0,
                             Math.min(wrongStates.size(), 10))
                     + "' have wrong states in the bitarchive listing in '"
                     + WorkFiles.getPreservationDir(location)
                    .getAbsolutePath() + "'");
        }

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
     * Runs a checksum job on the bit archive at the given location. Output is
     * written to file returned by WorkFiles.getChecksumOutputFile(location).
     *
     * @param location One of the bitarchive locations.
     *
     * @throws IOFailure If unable to create output dirs or if unable to
     *                   write/read output to files.
     */
    private void runChecksumJob(Location location) {
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
     * Return the number of files found in the bitarchive. If nothing is known
     * about the bitarchive location, -1 is returned.
     *
     * @param bitarchive the bitarchive to check
     *
     * @return the number of files found in the bitarchive.  If nothing is known
     * about the bitarchive location, -1 is returned.
     */
    public long getNumberOfFiles(Location bitarchive) {
        ArgumentNotValid.checkNotNull(bitarchive, "Location bitarchive");
        File unsortedOutput = WorkFiles.getFile(bitarchive,
                                                WorkFiles.FILES_ON_BA);

        if (!unsortedOutput.exists()) {
            return -1;
        }

        return FileUtils.countLines(unsortedOutput);
    }

    /**
     * Get the number of missing files in a given bitarchive. If nothing is
     * known about the bitarchive location, -1 is returned.
     *
     * @param bitarchive a given bitarchive
     *
     * @return the number of missing files in the given bitarchive. If nothing
     * is known about the bitarchive location, -1 is returned.
     */
    public long getNumberOfMissingFiles(Location bitarchive) {
        ArgumentNotValid.checkNotNull(bitarchive, "Location bitarchive");

        File missingOutput = WorkFiles.getFile(bitarchive,
                                               WorkFiles.MISSING_FILES_BA);
        if (!missingOutput.exists()) {
            return -1;
        }

        return FileUtils.countLines(missingOutput);
    }

    /**
     * Get the number of wrong files for a bitarchive. If nothing is known
     * about the bitarchive location, -1 is returned.
     *
     * @param bitarchive a bitarchive
     *
     * @return the number of wrong files for the bitarchive. If nothing is known
     * about the bitarchive location, -1 is returned.
     */
    public long getNumberOfChangedFiles(Location bitarchive) {
        ArgumentNotValid.checkNotNull(bitarchive, "Location bitarchive");
        File wrongFileOutput = WorkFiles.getFile(bitarchive,
                                                 WorkFiles.WRONG_FILES);

        if (!wrongFileOutput.exists()) {
            return -1;
        }

        return FileUtils.countLines(wrongFileOutput);
    }

    /**
     * Get the date for last time the checksum information was updated for
     * this location.
     * @param location The location to check last time for.
     * @return The date for last check. Will return 1970-01-01 for never.
     */
    public Date getDateForChangedFiles(Location location) {
        ArgumentNotValid.checkNotNull(location, "Location location");
        return WorkFiles.getLastUpdate(location, WorkFiles.WRONG_FILES);
    }

    /**
     * Get the date for last time the missing files information was updated for
     * this location.
     * @param location The location to check last time for.
     * @return The date for last check. Will return 1970-01-01 for never.
     */
    public Date getDateForMissingFiles(Location location) {
        ArgumentNotValid.checkNotNull(location, "Location location");
        return WorkFiles.getLastUpdate(location, WorkFiles.FILES_ON_BA);
    }


    /**
     * Check that files are indeed missing on the bitarchive location, and
     * present in admin data and reference location. If so, upload missing files
     * from reference location to this location.
     *
     * @param location The location to restore files to
     * @param filenames The names of the files.
     *
     * TODO Decide whether or not to throw IllegalState
     * @throws IllegalState  If one of the files is unknown
     *    (Known files will be attempted though)
     * @throws IOFailure    If some file cannot be reestablished. All files
     *                          will be attempted, though.
     */
    public void uploadMissingFiles(Location location, String... filenames) {
        ArgumentNotValid.checkNotNull(location, "Location location");
        ArgumentNotValid.checkNotNull(filenames, "String... filenames");
        List<String> troubleNames = new ArrayList<String>();
        
        Map<String,FilePreservationState> preservationStates
            =  getFilePreservationStateMap(filenames);
        for (Map.Entry<String, FilePreservationState> entry
                : preservationStates.entrySet()) {
            String fn = entry.getKey();
            FilePreservationState fps = entry.getValue();
            try {
                if (fps == null) {
                    throw new IllegalState("No state known about '" + fn + "'");
                }
                if (!fps.isAdminDataOk()) {
                    setAdminDataFailed(fn, location);
                    admin.synchronize();
                    fps = getFilePreservationState(fn);
                    if (fps == null) {
                        throw new IllegalState("No state known about '"
                                + fn + "'");
                    }
                }
                reestablishMissingFile(fn, location, fps);
            } catch (Exception e) {
                log.warn("Trouble updating file '" + fn + "'", e);
                troubleNames.add(fn);
            }
        }
        if (troubleNames.size() > 0) {
            throw new IOFailure("Could not update all files. The following"
                                + " files were not fixed: " + troubleNames);
        }
    }

    /**
     * Reestablish a file missing in a bitarchive. The following pre-conditions
     * for reestablishing the file are checked before changing anything:<p> 
     * 1) the file is registered correctly in AdminData <br>
     * 2) the file is missing in the given bitarchive <br>
     * 3) the file is present in another bitarchive (the
     * reference archive)<br> 
     * 4) admin data and the reference archive agree on the
     * checksum of the file.
     *
     * @param fileName          name of the file to reestablish
     * @param damagedBitarchive Name of the bitarchive missing the file
     * @param fps               The FilePreservationStatus of the file to fix.
     * @throws IOFailure        On trouble updating the file.
     */
    private void reestablishMissingFile(
            String fileName,
            Location damagedBitarchive, FilePreservationState fps) {
        log.debug("Reestablishing missing file '" + fileName
                  + "' in bitarchive '" + damagedBitarchive + "'.");
        if (!satisfiesMissingFileConditions(fps, damagedBitarchive,
                                            fileName)) {
            throw new IOFailure(
                    "Unable to reestablish missing file. '" + fileName + "'. "
                    + "It is not in the right state.");
        }
        // Retrieve the file from the reference archive
        Location referenceArchive = fps.getReferenceBitarchive();
        try {
            PreservationArcRepositoryClient arcrep =
                    ArcRepositoryClientFactory.getPreservationInstance();
            File tmpDir = FileUtils.createUniqueTempDir(FileUtils.getTempDir(),
                                                        REMOVED_FILES);
            File missingFile = new File(tmpDir, fileName);
            arcrep.getFile(fileName, referenceArchive, missingFile);
            arcrep.store(missingFile);
            tmpDir.delete();
        } catch (IOFailure e) {
            String errmsg = "Failed to reestablish '" + fileName
                            + "' in '" + damagedBitarchive.getName()
                            + "' with copy from '" + referenceArchive + "'";
            log.warn(errmsg, e);
            throw new IOFailure(errmsg,
                                e);
        }
        log.info("Reestablished " + fileName
                 + " in " + damagedBitarchive.getName()
                 + " with copy from " + referenceArchive.getName());
        FileUtils.removeLineFromFile(fileName,
                                     WorkFiles.getFile(
                                             damagedBitarchive,
                                             WorkFiles.MISSING_FILES_BA));
        FileUtils.appendToFile(WorkFiles.getFile(damagedBitarchive,
                                                 WorkFiles.FILES_ON_BA),
                               fileName);
    }

    /**
     * Checks the conditions that must be true before reestablishing a missing
     * file. Returns true if and only if all of the below are true; returns
     * false otherwise.<p>
     *
     * 1) the file is registered correctly in AdminData 
     * <br>2) the file is missing in the given bitarchive 
     * <br>3) the file is present in another bitarchive (the
     * reference archive) 
     * <br>4) admin data and the reference archive agree on the
     * checksum
     *
     * @param state            the status for one file in the bitarchives
     * @param damagedBitarchive the location where the file is corrupt or
     *                          missing
     * @param fileName          the name of the file being considered
     * @return true if all conditions are true, false otherwise.
     */
    private boolean satisfiesMissingFileConditions(
            FilePreservationState state,
            Location damagedBitarchive,
            String fileName) {
        // condition 1
        if (!state.isAdminDataOk()) {
            log.warn("Admin.data is not consistent regarding file '"
                     + fileName + "'");
            return false;
        }
        // condition 2
        if (!state.fileIsMissing(damagedBitarchive)) {
            log.warn("File '" + fileName
                     + "' is not missing in bitarchive on location '"
                     + damagedBitarchive.getName() + "'.");
            return false;
        }
        // conditions 3 and 4
        Location referenceArchive = state.getReferenceBitarchive();
        if (referenceArchive == null) {
            log.warn("No correct version of file '" + fileName
                     + "' exists in any archive");
            return false;
        }
        return true;
    }

    /**
     * Calls upon the arcrepository to change the known state for the given
     * file in one bitarchive.  This method uses JMS and blocks until a reply is
     * sent.
     *
     * @param filename The file to change state for
     * @param ba       The bitarchive to change state for the file for.
     *
     * @throws ArgumentNotValid if arguments are null or empty strings
     */
    private void setAdminDataFailed(String filename, Location ba) {
        ArcRepositoryClientFactory.getPreservationInstance()
                .updateAdminData(filename, ba.getName(),
                                 BitArchiveStoreState.UPLOAD_FAILED);
    }

    /**
     * Check that file checksum is indeed different to admin data and reference
     * location. If so, remove missing file and upload it from reference
     * location to this location.
     *
     * @param location The location to restore file to
     * @param filename The name of the file.
     * @param credentials The credentials used to perform this replace operation
     * @param checksum The expected checksum.
     *
     * @throws IOFailure        if the file cannot be reestablished
     * @throws PermissionDenied if the file is not in correct state
     */
    public void replaceChangedFile(Location location, String filename,
                                   String credentials, String checksum) {
        ArgumentNotValid.checkNotNull(location, "Location location");
        ArgumentNotValid.checkNotNullOrEmpty(filename, "String filename");
        ArgumentNotValid.checkNotNullOrEmpty(checksum, "String checksum");
        ArgumentNotValid.checkNotNullOrEmpty(credentials, "String credentials");
        removeAndGetFile(filename, location, checksum, credentials);
        uploadMissingFiles(location, filename);
    }

    /**
     * Call upon the arc repository to remove a file, returning it to this
     * machine.  The file is left around in case problems are later discovered,
     * and its location can be found in the log.
     *
     * @param filename    The file to remove.
     * @param bitarchive  The bitarchive to remove the file from.
     * @param checksum    The checksum of the file.
     * @param credentials Credentials required to run this operation.
     */
    private void removeAndGetFile(String filename, Location bitarchive,
                                  String checksum, String credentials) {
        ArcRepositoryClientFactory.getPreservationInstance()
                .removeAndGetFile(filename, bitarchive.getName(), checksum,
                                  credentials);
        FileUtils.appendToFile(WorkFiles.getFile(bitarchive,
                                                 WorkFiles.MISSING_FILES_BA),
                               filename);
        FileUtils.removeLineFromFile(filename, WorkFiles.getFile(
                bitarchive,
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
        //TODO implement method
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
        //TODO implement method
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
        //TODO implement method
        throw new NotImplementedException("Not implemented");
    }

    /**
     * Reestablish admin data to match bitarchive states for file.
     *
     * @param filename The file to reestablish state for.
     *
     * @throws PermissionDenied if the file is not in correct state
     */
    public void changeStateForAdminData(String filename) {
        ArgumentNotValid.checkNotNullOrEmpty(filename, "String filename");
        admin.synchronize();
        FilePreservationState fps 
            = getFilePreservationState(filename);
        String checksum = fps.getReferenceCheckSum();
        if (checksum == null || checksum.equals("")) {
            throw new PermissionDenied("No correct checksum for '"
                                       + filename + "'");
        }
        if (!admin.getCheckSum(filename).equals(checksum)) {
            ArcRepositoryClientFactory.getPreservationInstance()
                    .updateAdminChecksum(filename, checksum);
        }
        for (Location l : Location.getKnown()) {
            if (fps.getUniqueChecksum(l).equals(
                    admin.getCheckSum(filename))) {
                FileUtils.removeLineFromFile(
                        filename,
                        WorkFiles.getFile(l,
                                          WorkFiles.WRONG_FILES));
            }
        }
        //TODO Also update store states if wrong.
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
