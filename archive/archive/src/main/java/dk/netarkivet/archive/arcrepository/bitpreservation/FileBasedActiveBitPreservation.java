/* File:        $Id$
 * Revision:    $Revision$
 * Date:        $Date$
 * Author:      $Author$
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
import dk.netarkivet.common.distribute.arcrepository.ReplicaStoreState;
import dk.netarkivet.common.distribute.arcrepository.Replica;
import dk.netarkivet.common.distribute.arcrepository.PreservationArcRepositoryClient;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.IllegalState;
import dk.netarkivet.common.exceptions.NetarkivetException;
import dk.netarkivet.common.exceptions.NotImplementedException;
import dk.netarkivet.common.exceptions.PermissionDenied;
import dk.netarkivet.common.exceptions.UnknownID;
import dk.netarkivet.common.utils.CleanupHook;
import dk.netarkivet.common.utils.CleanupIF;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.StringUtils;
import dk.netarkivet.common.utils.batch.ChecksumJob;

/**
 * Class handling integrity check of the arcrepository. <p/> This class must
 * run on the same machine as the arcrepository, as it uses the same admin data
 * file (read-only).  However, it still talks JMS with the arcrepository.
 * 
 * @deprecated Use the DatabaseBasedActiveBitPreservation instead (define in
 * the setting: <b>settings.archive.admin.class</b>).
 */
@Deprecated
public class FileBasedActiveBitPreservation
        implements ActiveBitPreservation, CleanupIF {
    /** The class log. */
    private Log log = LogFactory.getLog(FileBasedActiveBitPreservation.class);

    /**
     * When replacing a broken file, the broken file is downloaded and stored in
     * a temporary directory under Settings.COMMON_TEMP_DIR with this name.
     * It can then be inspected at your leisure.
     */
    private static final String REMOVED_FILES = "bitpreservation";
    
    /**
     * The maximum size of logged collections. 
     * This is used either when a subcollection is extracted, or when objects
     * are concatenated.
     * Default value = 10. 
     */
    private static final int MAX_LIST_SIZE = 10;

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

    /** Initializes a FileBasedActiveBitPreservation instance. */
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
     * @return a map ([filename]-> [FilePreservationState]) of the preservation
     * status for the given files.
     * The preservationstate is null, if the file named does not exist
     * in admin data.
     *
     * @throws ArgumentNotValid If the list of filenames is null or contains 
     * a null.
     */
    public Map<String, PreservationState> getPreservationStateMap(
            String... filenames) throws ArgumentNotValid {
        ArgumentNotValid.checkNotNull(filenames, "String... filenames");
        // check, that the files are not empty strings
        for (String file: filenames) {
            ArgumentNotValid.checkNotNullOrEmpty(file, "String file");
        }
        // Start by retrieving the admin status
        admin.synchronize();
        
        // Temporary datastructures:
        // adminInfo: A map ([filename]->[ArcRepositoryEntry]) to hold admindata
        // info. Holds one entry for each of the files
        // known by admin data.
        // missingInAdminData: Contains the names of files that admindata just
        // don't know.
        Map<String, ArcRepositoryEntry> adminInfo = new HashMap<String, 
                ArcRepositoryEntry>();
        Set<String> missingInAdmindata = new HashSet<String>();
        
        for (String filename: filenames) {
            ArcRepositoryEntry ae = admin.getEntry(filename);
            if (ae != null){
                adminInfo.put(filename, ae);
            } else {
                missingInAdmindata.add(filename);
            }
        }

        if (missingInAdmindata.size() > 0) {
            log.warn("The following " + missingInAdmindata.size()
                    + " files are unknown to admindata: "
                    + StringUtils.conjoin(",", 
                            new ArrayList<String>(missingInAdmindata).subList(0,
                            Math.min(missingInAdmindata.size(), MAX_LIST_SIZE))
                            ));
        }
        
        // filepreservationStates: map ([filename] -> [filepreservationstate])
        // This is the datastructure returned from this method
        Map<String, PreservationState> filepreservationStates 
            = new HashMap<String, PreservationState>();
        
        // Phase 1: Add null FilePreservationState entries for the files
        // absent from admindata. 
        for (String missing: missingInAdmindata) {
            filepreservationStates.put(missing, (FilePreservationState) null);
        }
        // Phase 2: For every filename present in admin data,
        // construct a map ([replica] -> [list of checksums]).
        // The resulting map:
        //  map ([filename] -> map ([replica] -> [list of checksums])).
        // This takes a long time, as two batchjobs will be sent out to
        // to the bitarchives to compute checksums for the files with these
        // filenames.
        Map<String, Map<Replica, List<String>>> checksumMaps 
            = getChecksumMaps(adminInfo.keySet());
        
        // Phase 3: construct FilePreservationState objects for subset of
        // filenames known by admin data. The rest of the filenames are
        // represented with a null FilePreservationState object.        
        for (Map.Entry<String, ArcRepositoryEntry> entry 
                : adminInfo.entrySet()) {
            String filename = entry.getKey();
            ArcRepositoryEntry adminFileInfo = entry.getValue();
            filepreservationStates.put(filename, 
                    new FilePreservationState(filename,
                            adminFileInfo,
                            checksumMaps.get(filename)
                            )
            );
        }
        return filepreservationStates;
    }
    
    /**
     * Get the details of the state of the given file in the bitarchives
     * and admin data.
     * @param filename A given file
     * @return the FilePreservationState for the given file. This will be null,
     * if the filename is not found in admin data.
     */
    public PreservationState getPreservationState(String filename) {
        ArgumentNotValid.checkNotNullOrEmpty(filename, "String filename");
        Map<String, PreservationState> filepreservationStates
            = getPreservationStateMap(filename);
        
        return filepreservationStates.get(filename);
    }
    
    /**
     * Generate a map of checksums for these filenames in the bitarchives (
     * map ([filename] -> map ([replica] -> [list of checksums]))).
     * This takes a long time, as a batchjob will be sent out to
     * all the bitarchives to compute checksums for the files with these
     * filenames.
     *
     * @param filenames The filenames to get the checksums for.
     *
     * @return Map containing the output of checksum jobs from the bitarchives.
     */
    private Map<String, Map<Replica, List<String>>>
    getChecksumMaps(Set<String> filenames) {
     
        //checksummaps: map ([filename] -> map ([replica] 
        //  -> [list of checksums])).
        // This datastructure will contain for each filename the computed 
        // checksums for the file with this filename on all replicas
        // (bitarchives).
        Map<String, Map<Replica, List<String>>> checksummaps =
                new HashMap<String, Map<Replica, List<String>>>();
        
        //Only make one checksum job for each replica
        for (Replica rep : Replica.getKnown()) {
            // Get the checksum information from Replica 'rep' as
            // a map ([filename]->[list of checksums]).
            Map<String, List<String>> checksums = getChecksums(rep, filenames);
            log.debug("Adding checksums for replica '"
                      + rep + "' for filenames: "
                      + StringUtils.conjoin(",", filenames, MAX_LIST_SIZE));
            
            for (String filename : filenames) {
                // Update 'checksummaps' datastructure with the checksums
                // received from Replica 'rep'.
                
                // replicaMap: map ([replica] 
                //  -> [list of checksums for one filename]).
                Map<Replica, List<String>> replicaMap;
                // Get current map in 'checksummaps' datastructure for filename,
                // if it exists. Otherwise a new one is created, and
                // stored.
                if (checksummaps.containsKey(filename)) {
                    replicaMap = checksummaps.get(filename);
                } else {
                    replicaMap = new HashMap<Replica, List<String>>();
                    checksummaps.put(filename, replicaMap);
                }
                // Extract the list of checksums for the given filename from
                // the 'checksums' datastructure.
                List<String> checksumsForFileOnRep = checksums.get(filename);
                if (checksumsForFileOnRep == null) {
                    // If no checksums for file was available on replica 'ba'
                    // just add an empty list of checksums.
                    checksumsForFileOnRep = new ArrayList<String>();
                }
                // Add the list of checksums for the given file
                // on replica 'rep' to datastructure 'replicaMap'.
                replicaMap.put(rep, checksumsForFileOnRep);
            }
        }
        return checksummaps;
    }

    /**
     * Get the checksum of a list of files in a replica
     * (map ([filename] -> map ([replica] -> [list of checksums])).
     *
     * Note that this method runs a batch job on the bitarchives, and therefore
     * may take a long time, depending on network delays. 
     *
     * @param rep The replica to ask for checksums.
     * @param filenames The names of the files to ask for checksums for.
     * @return The MD5 checksums of the files, or the empty string if the file
     *         was not in the replica.
     * @see ChecksumJob#parseLine(String)
     */
    private Map<String, List<String>> getChecksums(Replica rep, 
            Set<String> filenames) {
        // initialise the resulting map.
        Map<String, List<String>> res =
            new HashMap<String, List<String>>();
        
        try {
            PreservationArcRepositoryClient arcClient =
                ArcRepositoryClientFactory.getPreservationInstance();
            // for each file extract the checksum through a checksum message
            // and then put it into the resulting map.
            for(String file : filenames) {
                // retrieve the checksum from the replica.
                String checksum = arcClient.getChecksum(rep.getId(), file);
                
                // put the checksum into a list, or make empty list if the 
                // checksum was not retrieved.
                List<String> csList;
                if(checksum == null || checksum.isEmpty()) {
                    log.warn("The checksum for file '" + file + "' from "
                            + "replica '" + rep + "' was invalid. "
                            + "Empty list returned");
                    csList = Collections.<String>emptyList();
                } else {
                    csList = new ArrayList<String>();
                    csList.add(checksum);
                }
                
                // put the filename and list into the map.
                res.put(file, csList);
            }

            log.debug("The map from a checksum archive: " + res.toString());
        } catch (NetarkivetException e) {
            // This is not critical. Log and continue.
            log.warn("The retrieval of checksums from a checksum archive was "
                    + "not successful.", e);
        }
        
        return res;
    }
    
    /**
     * Get a list of missing files in a given replica.
     *
     * @param replica A given replica.
     * @return A list of missing files in a given replica.
     * @throws IllegalState if the file with the list cannot be found.
     * @throws ArgumentNotValid If the replica is null.
     */
    public Iterable<String> getMissingFiles(Replica replica) 
            throws IllegalState, ArgumentNotValid {
        ArgumentNotValid.checkNotNull(replica, "Replica replica");
        File missingOutput = WorkFiles.getFile(replica,
                                               WorkFiles.MISSING_FILES_BA);
        if (!missingOutput.exists()) {
            throw new IllegalState("Could not find the file: "
                                   + missingOutput.getAbsolutePath());
        }
        return FileUtils.readListFromFile(missingOutput);
    }

    /**
     * This method takes as input the name of a replica for which we wish to
     * retrieve the list of files, either through a FileListJob or a 
     * GetAllFilenamesMessage. It also reads in the known files in the
     * arcrepository from the AdminData directory specified in the Setting
     * DIRS_ARCREPOSITORY_ADMIN. The two file lists are compared and a
     * subdirectory missingFiles is created with two unsorted files:
     * 'missingba.txt' containing missing files, ie those registered in the
     * admin data, but not found in the replica, and 'missingadmindata.txt'
     * containing extra files, ie. those found in the replica but not in the
     * arcrepository admin data.
     *
     * TODO The second file is never used on the current implementation.
     *
     * FIXME: It is unclear if the decision if which files are missing isn't
     * better suited to be in getMissingFiles, so this method only runs the
     * batch job.
     *
     * @param replica the replica to search for missing files
     *
     * @throws ArgumentNotValid If the given directory does not contain a file
     *                          filelistOutput/sorted.txt, or the argument
     *                          replica is null.
     * @throws PermissionDenied If the output directory cannot be created.
     */
    public void findMissingFiles(Replica replica) throws ArgumentNotValid, 
            PermissionDenied {
        ArgumentNotValid.checkNotNull(replica, "Replica replica");
        runFileListJob(replica);
        log.trace("Finding missing files in directory '" 
                + WorkFiles.getPreservationDir(replica) + "'");
        admin.synchronize();

        // Create set of file names from replica data
        Set<String> filesInReplica = new HashSet<String>(
                WorkFiles.getLines(replica, WorkFiles.FILES_ON_BA));

        // Get set of files in arcrepository
        Set<String> arcrepNameSet = admin.getAllFileNames();

        // Find difference set 1 (the files missing from the replica).
        Set<String> extraFilesInAdminData = new HashSet<String>(arcrepNameSet);
        extraFilesInAdminData.removeAll(filesInReplica);

        // Log result
        if (extraFilesInAdminData.size() > 0) {
            log.warn("The " + extraFilesInAdminData.size() + " files '"
                     + new ArrayList<String>(extraFilesInAdminData).subList(0,
                             Math.min(extraFilesInAdminData.size(), 
                                     MAX_LIST_SIZE))
                     + "' are not present in the replica listing in '"
                     + WorkFiles.getPreservationDir(replica)
                    .getAbsolutePath() + "'");
        }

        // Write output data
        WorkFiles.write(replica, WorkFiles.MISSING_FILES_BA,
                        extraFilesInAdminData);

        // Find difference set 2 (the files missing in admin.data).
        Set<String> extraFilesInRep = new HashSet<String>(filesInReplica);
        extraFilesInRep.removeAll(arcrepNameSet);

        // Log result
        if (extraFilesInRep.size() > 0) {
            log.warn("The " + extraFilesInRep.size() + " files '"
                     + new ArrayList<String>(extraFilesInRep).subList(0,
                             Math.min(extraFilesInRep.size(), MAX_LIST_SIZE))
                     + "' have been found in the replica listing in '"
                     + WorkFiles.getPreservationDir(replica)
                    .getAbsolutePath() + "' though they are not known by the "
                    + "system.");
        }

        // Write output data
        WorkFiles.write(replica, WorkFiles.MISSING_FILES_ADMINDATA,
                        extraFilesInRep);
        log.trace("Finished finding missing files.");
    }

    /**
     * Method to get a list of all files in a given bitarchive. The result is
     * stored (unsorted) in the area specified by WorkFiles.FILES_ON_BA.
     *
     * @param replica the replica where the given bitarchive lies
     *
     * @throws PermissionDenied if the output directories cannot be created
     * @throws IOFailure        if there is a problem writing the output file,
     *                          or if the job fails for some reason
     * @throws UnknownID If the replica has an unknown replicaType.
     */
    private void runFileListJob(Replica replica) throws IOFailure, UnknownID, 
            PermissionDenied {
        // Pick the right directory to output to
        File batchOutputFile = WorkFiles.getFile(replica,
                                                 WorkFiles.FILES_ON_BA);
        log.trace("runFileListJob for replica '" + replica
                  + "', output file '" + batchOutputFile + "'");

        // Retrieve a file containing all the filenames of the replica through
        // a GetAllFilenamesMessage
        File filenames = ArcRepositoryClientFactory.getPreservationInstance()
                .getAllFilenames(replica.getId());
        
        // copy the list of filenames to the output file.
        FileUtils.copyFile(filenames, batchOutputFile);
    }

    /**
     * Get a list of corrupt files in a given bitarchive.
     *
     * @param bitarchive a bitarchive
     *
     * @return a list of wrong files in a given bitarchive.
     *
     * @throws IllegalState if the file with the list cannot be found.
     */
    public Iterable<String> getChangedFiles(Replica bitarchive) 
            throws IllegalState {
        ArgumentNotValid.checkNotNull(bitarchive, "Replica bitarchive");
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
     * @param replica the bitarchive replica the checksumjob came from
     *
     * @throws IOFailure        On file or network trouble.
     * @throws PermissionDenied if the output directory cannot be created
     * @throws ArgumentNotValid if argument replica is null
     */
    public void findChangedFiles(Replica replica) throws IOFailure, 
            PermissionDenied, ArgumentNotValid {
        ArgumentNotValid.checkNotNull(replica, "Replica replica");
        runChecksumJob(replica);
        admin.synchronize();

        // Create set of checksums from bitarchive data
        Set<String> replicaChecksumSet = new HashSet<String>(
                WorkFiles.getLines(replica, WorkFiles.CHECKSUMS_ON_BA));

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
        for (String fileName : admin.getAllFileNames(
                replica,
                ReplicaStoreState.UPLOAD_COMPLETED)) {
            arcrepCompletedChecksumSet.add(ChecksumJob.makeLine(
                    fileName, admin.getCheckSum(fileName)));
        }

        // Find files where checksums differ
        Set<String> wrongChecksums = new HashSet<String>(replicaChecksumSet);
        wrongChecksums.removeAll(arcrepChecksumSet);

        // Find files where state is wrong
        Set<String> wrongStates = new HashSet<String>(replicaChecksumSet);
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
                             Math.min(wrongChecksums.size(), MAX_LIST_SIZE))
                     + "' have wrong checksum in the bitarchive listing in '"
                     + WorkFiles.getPreservationDir(replica)
                    .getAbsolutePath() + "'");
        }
        if (wrongStates.size() > 0) {
            log.warn("The " + wrongStates.size() + " files '"
                     + new ArrayList<String>(wrongStates).subList(0,
                             Math.min(wrongStates.size(), MAX_LIST_SIZE))
                     + "' have wrong states in the bitarchive listing in '"
                     + WorkFiles.getPreservationDir(replica)
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
        WorkFiles.write(replica, WorkFiles.WRONG_FILES,
                        wrongChecksumFilenames);
        WorkFiles.write(replica, WorkFiles.WRONG_STATES,
                        wrongStateFilenames);
    }

    /**
     * Runs a checksum job on if the replica is a bitarchive replica and sends 
     * a GetAllChecksumsMessage if the replica is a checksum replica. Output is
     * written to file returned by WorkFiles.getChecksumOutputFile(replica).
     * 
     * @param replica One of the bitarchive replicas.
     * @throws IOFailure If unable to create output dirs or if unable to
     *                   write/read output to files.
     */
    private void runChecksumJob(Replica replica) throws IOFailure {
        // Create directories for output
        File outputFile = WorkFiles.getFile(replica,
                                            WorkFiles.CHECKSUMS_ON_BA);

        // Retrieve a file containing the checksums of the replica through a 
        // GetAllChecksumsMessage.
        File checksumFile =  ArcRepositoryClientFactory
                .getPreservationInstance().getAllChecksums(replica.getId());
        
        // copy the resulting file to the output file.
        FileUtils.copyFile(checksumFile, outputFile);
    }

    /**
     * Return the number of files found in the replica. If nothing is known
     * about the replica, -1 is returned.
     *
     * @param replica the bitarchive to check
     *
     * @return the number of files found in the bitarchive.  If nothing is known
     * about the bitarchive replica, -1 is returned.
     * @throws ArgumentNotValid If the replica is null.
     */
    public long getNumberOfFiles(Replica replica) throws ArgumentNotValid {
        ArgumentNotValid.checkNotNull(replica, "Replica replica");
        File unsortedOutput = WorkFiles.getFile(replica,
                WorkFiles.FILES_ON_BA);

        if (!unsortedOutput.exists()) {
            return -1;
        }

        return FileUtils.countLines(unsortedOutput);
    }

    /**
     * Get the number of missing files in a given replica. If nothing is
     * known about the replica, -1 is returned.
     *
     * @param replica a given replica.
     * @return the number of missing files in the given replica. If nothing
     * is known about the replica, -1 is returned.
     * @throws ArgumentNotValid If the replica is null.
     */
    public long getNumberOfMissingFiles(Replica replica) 
            throws ArgumentNotValid {
        ArgumentNotValid.checkNotNull(replica, "Replica replica");

        File missingOutput = WorkFiles.getFile(replica, 
                WorkFiles.MISSING_FILES_BA);
        if (!missingOutput.exists()) {
            return -1;
        }

        return FileUtils.countLines(missingOutput);
    }

    /**
     * Get the number of wrong files for a replica. If nothing is known
     * about the replica, -1 is returned.
     *
     * @param replica a replica.
     * @return the number of wrong files for the replica. If nothing is known
     * about the replica, -1 is returned.
     * @throws ArgumentNotValid If the replica is null.
     */
    public long getNumberOfChangedFiles(Replica replica) 
            throws ArgumentNotValid {
        ArgumentNotValid.checkNotNull(replica, "Replica bitarchive");
        File wrongFileOutput = WorkFiles.getFile(replica, 
                WorkFiles.WRONG_FILES);

        if (!wrongFileOutput.exists()) {
            return -1;
        }

        return FileUtils.countLines(wrongFileOutput);
    }

    /**
     * Get the date for last time the checksum information was updated for
     * this replica.
     * @param replica The replica to check last time for.
     * @return The date for last check. Will return 1970-01-01 for never.
     * @throws ArgumentNotValid If the replica is null.
     */
    public Date getDateForChangedFiles(Replica replica) 
            throws ArgumentNotValid {
        ArgumentNotValid.checkNotNull(replica, "Replica replica");
        return WorkFiles.getLastUpdate(replica, WorkFiles.WRONG_FILES);
    }

    /**
     * Get the date for last time the missing files information was updated for
     * this replica.
     * @param replica The replica to check last time for.
     * @return The date for last check. Will return 1970-01-01 for never.
     * @throws ArgumentNotValid If the replica is null.
     */
    public Date getDateForMissingFiles(Replica replica) 
            throws ArgumentNotValid {
        ArgumentNotValid.checkNotNull(replica, "Replica replica");
        return WorkFiles.getLastUpdate(replica, WorkFiles.FILES_ON_BA);
    }


    /**
     * Check that the files we want to restore are indeed missing on the 
     * replica, and present in admin data and the reference bitarchive.
     * If so, upload missing files from reference replica to this replica.
     *
     * @param replica The replica to restore files to
     * @param filenames The names of the files.
     * @throws IllegalState  If one of the files is unknown 
     * (For all known files, there will be an attempt at udpload)
     * @throws IOFailure If some file cannot be reestablished. All files
     *  will be attempted, though.
     * @throws ArgumentNotValid If the replica or the list of filenames are 
     * null.
     */
    public void uploadMissingFiles(Replica replica, String... filenames) 
            throws IOFailure, IllegalState, ArgumentNotValid {
        ArgumentNotValid.checkNotNull(replica, "Replica replica");
        ArgumentNotValid.checkNotNull(filenames, "String... filenames");
        
        // Contains all files that we couldn't reestablish
        List<String> troubleNames = new ArrayList<String>();
        
        // preservationStates: map [filename]->[filepreservationstate]
        // Initialized here to contain an entry for each filename in vargargs
        // 'filenames'.
        Map<String, PreservationState> preservationStates
            =  getPreservationStateMap(filenames);
        
        // For each given filename, try to reestablish it on
        // Replica 'replica'
        for (String fn: filenames) {
            PreservationState fps = preservationStates.get(fn);
            try {
                if (fps == null) {
                    throw new IllegalState("No state known about '" + fn + "'");
                }
                if (!fps.isAdminDataOk()) {
                    setAdminDataFailed(fn, replica);
                    admin.synchronize();
                    fps = getPreservationState(fn);
                    if (fps == null) {
                        throw new IllegalState("No state known about '"
                                + fn + "'");
                    }
                }
                reestablishMissingFile(fn, replica, fps);
            } catch (Exception e) {
                log.warn("Trouble reestablishing file '" + fn
                        + "' on replica " + replica.getName() , e);
                troubleNames.add(fn);
            }
        }
        if (troubleNames.size() > 0) {
            throw new IOFailure("Could not reestablish all files. The following"
                                + " files were not reestablished: "
                                + troubleNames);
        }
    }

    /**
     * Reestablish a file missing in a replica. The following pre-conditions
     * for reestablishing the file are checked before changing anything:<p> 
     * 1) the file is registered correctly in AdminData. <br>
     * 2) the file is missing in the given replica. <br>
     * 3) the file is present in another replica, which must be a bitarchive 
     * replica (the reference archive).<br> 
     * 4) admin data and the reference archive agree on the
     * checksum of the file.
     *
     * @param fileName Name of the file to reestablish.
     * @param damagedReplica Name of the replica missing the file.
     * @param fps The FilePreservationStatus of the file to fix.
     * @throws IOFailure On trouble updating the file.
     */
    private void reestablishMissingFile(String fileName, Replica damagedReplica,
            PreservationState fps) throws IOFailure {
        log.debug("Reestablishing missing file '" + fileName
                  + "' in replica '" + damagedReplica + "'.");
        if (!satisfiesMissingFileConditions(fps, damagedReplica, fileName)) {
            throw new IOFailure(
                    "Unable to reestablish missing file. '" + fileName + "'. "
                    + "It is not in the right state.");
        }
        // Retrieve the file from the reference archive (must be a bitarchive)
        Replica referenceArchive = fps.getReferenceBitarchive();
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
                            + "' in '" + damagedReplica.getName()
                            + "' with copy from '" + referenceArchive + "'";
            log.warn(errmsg, e);
            throw new IOFailure(errmsg, e);
        }
        log.info("Reestablished " + fileName + " in " + damagedReplica.getName()
                 + " with copy from " + referenceArchive.getName());
        FileUtils.removeLineFromFile(fileName,
                                     WorkFiles.getFile(
                                             damagedReplica,
                                             WorkFiles.MISSING_FILES_BA));
        FileUtils.appendToFile(WorkFiles.getFile(damagedReplica,
                                                 WorkFiles.FILES_ON_BA),
                               fileName);
    }

    /**
     * Checks the conditions that must be true before reestablishing a missing
     * file. Returns true if and only if all of the below are true; returns
     * false otherwise.<p>
     *
     * 1) the file is registered correctly in AdminData.<br/>
     * 2) the file is missing in the given bitarchive.<br/>
     * 3) the file is present in another bitarchive (the reference archive).
     * <br/>
     * 4) admin data and the reference archive agree on the checksum.
     *
     * @param state the status for one file in the bitarchives.
     * @param damagedReplica the replica where the file is corrupt or missing.
     * @param fileName the name of the file being considered.
     * @return true if all conditions are true, false otherwise.
     */
    private boolean satisfiesMissingFileConditions(PreservationState state,
            Replica damagedReplica, String fileName) {
        // condition 1
        if (!state.isAdminDataOk()) {
            log.warn("Admin.data is not consistent regarding file '"
                     + fileName + "'");
            return false;
        }
        // condition 2
        if (!state.fileIsMissing(damagedReplica)) {
            log.warn("File '" + fileName
                     + "' is not missing in bitarchive on replica '"
                     + damagedReplica.getName() + "'.");
            return false;
        }
        // conditions 3 and 4
        Replica referenceArchive = state.getReferenceBitarchive();
        if (referenceArchive == null) {
            log.warn("No correct version of file '" + fileName
                     + "' exists in any archive");
            return false;
        }
        return true;
    }

    /**
     * Calls upon the arcrepository to change the known state for the given
     * file in one replica.  This method uses JMS and blocks until a reply is
     * sent.
     * We don't wait for an acknowledgement that admin data indeed has been 
     * updated.
     *
     * @param filename The file to change state for
     * @param rep       The replica to change state for the file for.
     * @throws ArgumentNotValid if arguments are null or empty strings
     */
    private void setAdminDataFailed(String filename, Replica rep) 
            throws ArgumentNotValid {
        ArgumentNotValid.checkNotNullOrEmpty(filename, "String filename");
        ArgumentNotValid.checkNotNull(rep, "Replica rep");
        
        ArcRepositoryClientFactory.getPreservationInstance()
                .updateAdminData(filename, rep.getId(),
                                 ReplicaStoreState.UPLOAD_FAILED);
    }

    /**
     * Check that file checksum is indeed different to admin data and reference
     * replica. If so, remove missing file and upload it from reference
     * replica to this replica.
     *
     * @param replica The replica to restore file to
     * @param filename The name of the file.
     * @param credentials The credentials used to perform this replace operation
     * @param checksum The expected checksum.
     * @throws IOFailure        if the file cannot be reestablished
     * @throws PermissionDenied if the file is not in correct state
     * @throws ArgumentNotValid If the filename, the credentials or the checksum
     * either are null or contain the empty string, or if the replica is null.
     */
    public void replaceChangedFile(Replica replica, String filename, 
            String credentials, String checksum) throws ArgumentNotValid, 
            IOFailure, PermissionDenied {
        ArgumentNotValid.checkNotNull(replica, "Replica replica");
        ArgumentNotValid.checkNotNullOrEmpty(filename, "String filename");
        ArgumentNotValid.checkNotNullOrEmpty(checksum, "String checksum");
        ArgumentNotValid.checkNotNullOrEmpty(credentials, "String credentials");
        
        // Send a correct message to the archive.
        correctArchiveEntry(replica, filename, checksum, credentials);
    }
    
    /**
     * Method for correcting a corrupt entry in an archive. This message is 
     * handled different for the different replicas
     * 
     * @param replica The replica which contains the bad entry.
     * @param filename The name of the file.
     * @param checksum The checksum of the bad entry.
     * @param credentials The credentials for correcting the bad entry.
     */
    private void correctArchiveEntry(Replica replica, String filename, 
            String checksum, String credentials) {
        // get the preservation state.
        Map<String, PreservationState> preservationStates
                =  getPreservationStateMap(filename);
        PreservationState fps = preservationStates.get(filename);

        // Use the preservation state to find a reference archive (bitarchive).
        Replica referenceArchive = fps.getReferenceBitarchive();
        
        // Get the arc repository client and a temporary file
        PreservationArcRepositoryClient arcrepClient =
                ArcRepositoryClientFactory.getPreservationInstance();
        File tmpDir = FileUtils.createUniqueTempDir(FileUtils.getTempDir(),
                REMOVED_FILES);
        File missingFile = new File(tmpDir, filename);
        
        // retrieve a good copy of the file
        arcrepClient.getFile(filename, referenceArchive, missingFile);

        // correct the bad entry in the archive with the retrieved good copy.
        arcrepClient.correct(replica.getId(), checksum, missingFile, 
                credentials);

        // cleanup afterwards.
        tmpDir.delete();
    }

    /**
     * Return a list of files present in bitarchive but missing in AdminData.
     *
     * @return A list of missing files.
     * @throws NotImplementedException Always, since this will not been 
     * implemented.
     */
    public Iterable<String> getMissingFilesForAdminData() 
            throws NotImplementedException {
        throw new NotImplementedException("Not to be implemented");
    }

    /**
     * Return a list of files with wrong checksum or status in admin data.
     *
     * @return A list of files with wrong checksum or status.
     * @throws NotImplementedException Always, since this will not been 
     * implemented.
     */
    public Iterable<String> getChangedFilesForAdminData() 
            throws NotImplementedException {
        throw new NotImplementedException("Not to be implemented");
    }

    /**
     * Reestablish admin data to match bitarchive states for files.
     *
     * @param filenames The files to reestablish state for.
     * @throws NotImplementedException Always, since this will not been 
     * implemented.
     * @throws ArgumentNotValid If the list of filenames are null.
     */
    public void addMissingFilesToAdminData(String... filenames) 
            throws NotImplementedException, ArgumentNotValid {
        ArgumentNotValid.checkNotNull(filenames, "String... filenames");
        //TODO implement method
        throw new NotImplementedException("Not to be implemented");
    }

    /**
     * Reestablish admin data to match replica states for file.
     *
     * @param filename The file to reestablish state for.
     * @throws PermissionDenied if the file is not in correct state
     * @throws ArgumentNotValid If the filename is null or empty.
     */
    public void changeStateForAdminData(String filename) 
            throws PermissionDenied, ArgumentNotValid {
        ArgumentNotValid.checkNotNullOrEmpty(filename, "String filename");
        admin.synchronize();
        PreservationState fps 
            = getPreservationState(filename);
        String checksum = fps.getReferenceCheckSum();
        if (checksum == null || checksum.isEmpty()) {
            throw new PermissionDenied("No correct checksum for '"
                                       + filename + "'");
        }
        if (!admin.getCheckSum(filename).equals(checksum)) {
            ArcRepositoryClientFactory.getPreservationInstance()
                    .updateAdminChecksum(filename, checksum);
        }
        for (Replica rep : Replica.getKnown()) {
            if (fps.getUniqueChecksum(rep).equals(
                    admin.getCheckSum(filename))) {
                FileUtils.removeLineFromFile(
                        filename,
                        WorkFiles.getFile(rep, WorkFiles.WRONG_FILES));
            }
        }
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
