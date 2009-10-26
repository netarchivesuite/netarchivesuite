/* File:        $Id$
 * Revision:    $Revision$
 * Date:        $Date$
 * Author:      $Author$
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
import dk.netarkivet.common.distribute.arcrepository.ReplicaStoreState;
import dk.netarkivet.common.distribute.arcrepository.Replica;
import dk.netarkivet.common.distribute.arcrepository.PreservationArcRepositoryClient;
import dk.netarkivet.common.distribute.arcrepository.ReplicaType;
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
import dk.netarkivet.common.utils.KeyValuePair;
import dk.netarkivet.common.utils.NotificationsFactory;
import dk.netarkivet.common.utils.StringUtils;
import dk.netarkivet.common.utils.batch.FileBatchJob;

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
     * @throws ArgumentNotValid if argument is null
     */
    public Map<String, FilePreservationState> getFilePreservationStateMap(
            String... filenames) {
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

        if (missingInAdmindata.size() > 0) {
            log.warn("The following " + missingInAdmindata.size()
                    + " files are unknown to admindata: "
                    + StringUtils.conjoin(",", 
                            new ArrayList<String>(missingInAdmindata).subList(0,
                            Math.min(missingInAdmindata.size(), 10))
                            ));
        }
        
        // filepreservationStates: map ([filename] -> [filepreservationstate])
        // This is the datastructure returned from this method
        Map<String, FilePreservationState> filepreservationStates 
            = new HashMap<String, FilePreservationState>();
        
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
        for (Map.Entry<String,ArcRepositoryEntry> entry: adminInfo.entrySet()) {
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
    public FilePreservationState getFilePreservationState(String filename) {
        ArgumentNotValid.checkNotNullOrEmpty(filename, "String filename");
        Map<String, FilePreservationState> filepreservationStates
            = getFilePreservationStateMap(filename);
        
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
                      + StringUtils.conjoin(",", filenames));
            
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
                List<String> checksumsForFileOnBa = checksums.get(filename);
                if (checksumsForFileOnBa == null) {
                    // If no checksums for file was available on replica 'ba'
                    // just add an empty list of checksums.
                    checksumsForFileOnBa = new ArrayList<String>();
                }
                // Add the list of checksums for the given file
                // on replica 'rep' to datastructure 'replicaMap'.
                replicaMap.put(rep, checksumsForFileOnBa);
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
     * TODO remodel the retrieval of checksums by using GetAllChecksumsMessage.
     *
     * @param rep The replica to ask for checksums.
     * @param filenames The names of the files to ask for checksums for.
     * @return The MD5 checksums of the files, or the empty string if the file
     *         was not in the replica.
     * @throws UnknownID If the replica has a unhandled replica type.
     * @see ChecksumJob#parseLine(String)
     */
    private Map<String, List<String>> getChecksums(
            Replica rep, Set<String> filenames) {
        // Retrieve the checksums dependent on the type of replica
        if(rep.getType() == ReplicaType.BITARCHIVE) {
            return getChecksumsFromBitarchive(rep, filenames);
        } else if(rep.getType() == ReplicaType.CHECKSUM) {
            return getChecksumFromChecksumArchive(rep, filenames);
        } else {
            throw new UnknownID("Unhandled replica type for replica: " + rep);
        }
    }
    
    /**
     * Retrieves the checksums from the checksum archive.
     * This is currently done by retrieving all the checksums and extracting 
     * only the wanted. 
     * TODO change this to do the intended method.
     * 
     * @param rep The checksum replica.
     * @param filenames The list of filenames.
     * @return An empty map.
     */
    private Map<String, List<String>> getChecksumFromChecksumArchive(
            Replica rep, Set<String> filenames) {
        Map<String, List<String>> res = 
            Collections.<String, List<String>>emptyMap();
        
        try {
            // get all the checsums
            PreservationArcRepositoryClient arcClient = 
                ArcRepositoryClientFactory.getPreservationInstance();
            File checksumFile = arcClient.getAllChecksums(rep.getId());
            
            // go through all entries and extract the relevant ones.
            for(String line : FileUtils.readListFromFile(checksumFile)) {
                KeyValuePair<String, String> entry = ChecksumJob.parseLine(line);
                // If the file is wanted, then put it into the map.
                if(filenames.contains(entry.getKey())) {
                    // remove from filename list, so it is not found twice.
                    filenames.remove(entry.getKey());
                    
                    // put this entry into the map.
                    List<String> csList = new ArrayList<String>(1);
                    csList.add(entry.getValue());
                    res.put(entry.getKey(), csList);
                }
                // TODO if an entry for the file already has been put into the 
                // map, then also add this entry to corresponding list.
            }
        } catch (NetarkivetException e) {
            // This is not critical. Log and continue.
            log.warn("The retrieval of checksums from a checksum archive was "
                    + "not successfull.", e);
        }
        
        return res;
    }
    
    /**
     * Creates an instance of the batchjob ChecksumBatchJob limited to only run 
     * upon the listed files and sends it to the replica, which has to be a
     * bitarchive replica. 
     * 
     * @param rep The replica to retrieve the checksums from. This has to be 
     * a bitarchive replica.
     * @param filenames The list of filenames to retrieve the checksums from.
     * @return The MD5 checksum of the filenames or an empty string if the file
     * was not found in the replica.
     */
    private Map<String, List<String>> getChecksumsFromBitarchive(Replica rep,
            Set<String> filenames) {
        
        // Configure the Checksum batchjob.
        ChecksumJob checksumJob = new ChecksumJob();
        checksumJob.processOnlyFilesNamed(new ArrayList<String>(filenames));
        
        // The result of the Checksum batchjob.
        String batchResult;
        
        // Execute the batchjob, and wait for the result.
        try {
            PreservationArcRepositoryClient arcrep =
                    ArcRepositoryClientFactory.getPreservationInstance();
            BatchStatus batchStatus = arcrep.batch(checksumJob, rep.getId());
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
            // Send notification, and return an empty map
            String errMsg = "Error asking replica '" + rep + "' for checksums";
            NotificationsFactory.getInstance().errorEvent(errMsg, e);
            log.warn(errMsg, e);
            return Collections.emptyMap();
        }
        
        // map ([filename] -> [List of checksums for a given filename]).
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
                                + "replica '" + rep
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
                            + "' while asking replica '" + rep
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
     * Get a list of missing files in a given replica.
     *
     * @param replica A given replica.
     *
     * @return A list of missing files in a given replica.
     *
     * @throws IllegalState if the file with the list cannot be found.
     */
    public Iterable<String> getMissingFiles(Replica replica) {
        ArgumentNotValid.checkNotNull(replica, "Replica bitarchive");
        File missingOutput = WorkFiles.getFile(replica,
                                               WorkFiles.MISSING_FILES_BA);
        if (!missingOutput.exists()) {
            throw new IllegalState("Could not find the file: "
                                   + missingOutput.getAbsolutePath());
        }
        return FileUtils.readListFromFile(missingOutput);
    }

    /**
     * This method takes as input the name of a bitarchive replica for which we
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
     * @param replica the replica to search for missing files
     *
     * @throws ArgumentNotValid if the given directory does not contain a file
     *                          filelistOutput/sorted.txt, or the argument
     *                          replica is null
     * @throws PermissionDenied if the output directory cannot be created
     */
    public void findMissingFiles(Replica replica) {
        ArgumentNotValid.checkNotNull(replica, "Replica replica");
        runFileListJob(replica);
        log.trace("findMissingFile in dir '"
                  + WorkFiles.getPreservationDir(replica) + "'");
        admin.synchronize();

        // Create set of file names from replica data
        Set<String> filesInReplica = new HashSet<String>(
                WorkFiles.getLines(replica, WorkFiles.FILES_ON_BA));

        // Get set of files in arcrepository
        Set<String> arcrepNameSet = admin.getAllFileNames();

        // Find difference set 1
        Set<String> extraFilesInAdminData = new HashSet<String>(arcrepNameSet);
        extraFilesInAdminData.removeAll(filesInReplica);

        // Log result
        if (extraFilesInAdminData.size() > 0) {
            log.warn("The " + extraFilesInAdminData.size() + " files '"
                     + new ArrayList<String>(extraFilesInAdminData).subList(0,
                             Math.min(extraFilesInAdminData.size(), 10))
                     + "' have wrong checksum in the replica listing in '"
                     + WorkFiles.getPreservationDir(replica)
                    .getAbsolutePath() + "'");
        }

        // Write output data
        WorkFiles.write(replica, WorkFiles.MISSING_FILES_BA,
                        extraFilesInAdminData);

        // Find difference set 2
        Set<String> extraFilesInBA = new HashSet<String>(filesInReplica);
        extraFilesInBA.removeAll(arcrepNameSet);

        // Log result
        if (extraFilesInBA.size() > 0) {
            log.warn("The " + extraFilesInBA.size() + " files '"
                     + new ArrayList<String>(extraFilesInBA).subList(0,
                             Math.min(extraFilesInBA.size(), 10))
                     + "' have wrong checksum in the bitarchive listing in '"
                     + WorkFiles.getPreservationDir(replica)
                    .getAbsolutePath() + "'");
        }

        // Write output data
        WorkFiles.write(replica, WorkFiles.MISSING_FILES_ADMINDATA,
                        extraFilesInBA);
        log.trace("Findmissing files - done");
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
    private void runFileListJob(Replica replica) throws IOFailure, UnknownID {
        // Pick the right directory to output to
        File batchOutputFile = WorkFiles.getFile(replica,
                                                 WorkFiles.FILES_ON_BA);
        log.trace("runFileListJob for replica '" + replica
                  + "', output file '" + batchOutputFile + "'");

        if(replica.getType() == ReplicaType.CHECKSUM) {
            FileUtils.copyFile(ArcRepositoryClientFactory
                    .getPreservationInstance().getAllFilenames(replica.getId()),
                    batchOutputFile);
        } else if (replica.getType() == ReplicaType.BITARCHIVE) {
            // Send filelist batch job
            runBatchJob(new FileListJob(), replica, null, batchOutputFile);
        } else {
            String errMsg = "Cannot handle the replica type of replica '"
                + replica + "'.";
            log.warn(errMsg);
            throw new UnknownID(errMsg);
        }
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
    public Iterable<String> getChangedFiles(Replica bitarchive) {
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
    public void findChangedFiles(Replica replica) {
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
                                                       admin.getCheckSum(
                                                               fileName)));
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
                             Math.min(wrongChecksums.size(), 10))
                     + "' have wrong checksum in the bitarchive listing in '"
                     + WorkFiles.getPreservationDir(replica)
                    .getAbsolutePath() + "'");
        }
        if (wrongStates.size() > 0) {
            log.warn("The " + wrongStates.size() + " files '"
                     + new ArrayList<String>(wrongStates).subList(0,
                             Math.min(wrongStates.size(), 10))
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
     * Runs a checksum job on the bit archive at the given replica. Output is
     * written to file returned by WorkFiles.getChecksumOutputFile(replica).
     *
     * @param replica One of the bitarchive replicas.
     * @throws ArgumentNotValid If <b>replica</b> is null.
     * @throws UnknownID If the replica has an unhandled replica type.
     * @throws IOFailure If unable to create output dirs or if unable to
     *                   write/read output to files.
     */
    private void runChecksumJob(Replica replica) throws ArgumentNotValid, 
            UnknownID, IOFailure {
        ArgumentNotValid.checkNotNull(replica, "replica");
        // Create directories for output
        File outputFile = WorkFiles.getFile(replica,
                                            WorkFiles.CHECKSUMS_ON_BA);

        if(replica.getType() == ReplicaType.CHECKSUM) {
            FileUtils.copyFile(ArcRepositoryClientFactory
                    .getPreservationInstance().getAllChecksums(replica.getId()),
                    outputFile);
        } else if (replica.getType() == ReplicaType.BITARCHIVE) {
            // TODO It should be written when  the integrity check is complete.
            // Send checksum batch job
            log.info("Bit integrity check started on the bitarchive replica '"
        	    + replica + "'.");
            runBatchJob(new ChecksumJob(), replica, null, outputFile);
        } else {
            String errMsg = "Cannot handle the replica type of replica '"
                + replica + "'.";
            log.warn(errMsg);
            throw new UnknownID(errMsg);
        }
    }

    /**
     * Return the number of files found in the bitarchive. If nothing is known
     * about the bitarchive replica, -1 is returned.
     *
     * @param bitarchive the bitarchive to check
     *
     * @return the number of files found in the bitarchive.  If nothing is known
     * about the bitarchive replica, -1 is returned.
     */
    public long getNumberOfFiles(Replica bitarchive) {
        ArgumentNotValid.checkNotNull(bitarchive, "Replica bitarchive");
        File unsortedOutput = WorkFiles.getFile(bitarchive,
                                                WorkFiles.FILES_ON_BA);

        if (!unsortedOutput.exists()) {
            return -1;
        }

        return FileUtils.countLines(unsortedOutput);
    }

    /**
     * Get the number of missing files in a given bitarchive. If nothing is
     * known about the bitarchive replica, -1 is returned.
     *
     * @param bitarchive a given bitarchive
     *
     * @return the number of missing files in the given bitarchive. If nothing
     * is known about the bitarchive replica, -1 is returned.
     */
    public long getNumberOfMissingFiles(Replica bitarchive) {
        ArgumentNotValid.checkNotNull(bitarchive, "Replica bitarchive");

        File missingOutput = WorkFiles.getFile(bitarchive,
                                               WorkFiles.MISSING_FILES_BA);
        if (!missingOutput.exists()) {
            return -1;
        }

        return FileUtils.countLines(missingOutput);
    }

    /**
     * Get the number of wrong files for a bitarchive. If nothing is known
     * about the bitarchive replica, -1 is returned.
     *
     * @param bitarchive a bitarchive
     *
     * @return the number of wrong files for the bitarchive. If nothing is known
     * about the bitarchive replica, -1 is returned.
     */
    public long getNumberOfChangedFiles(Replica bitarchive) {
        ArgumentNotValid.checkNotNull(bitarchive, "Replica bitarchive");
        File wrongFileOutput = WorkFiles.getFile(bitarchive,
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
     */
    public Date getDateForChangedFiles(Replica replica) {
        ArgumentNotValid.checkNotNull(replica, "Replica replica");
        return WorkFiles.getLastUpdate(replica, WorkFiles.WRONG_FILES);
    }

    /**
     * Get the date for last time the missing files information was updated for
     * this replica.
     * @param replica The replica to check last time for.
     * @return The date for last check. Will return 1970-01-01 for never.
     */
    public Date getDateForMissingFiles(Replica replica) {
        ArgumentNotValid.checkNotNull(replica, "Replica replica");
        return WorkFiles.getLastUpdate(replica, WorkFiles.FILES_ON_BA);
    }


    /**
     * Check that files are indeed missing on the bitarchive replica, and
     * present in admin data and reference replica. If so, upload missing files
     * from reference replica to this replica.
     *
     * @param replica The replica to restore files to
     * @param filenames The names of the files.
     *
     * @throws IllegalState  If one of the files is unknown 
     * (For all known files, there will be an attempt at udpload)
     * @throws IOFailure If some file cannot be reestablished. All files
     *  will be attempted, though.
     */
    public void uploadMissingFiles(Replica replica, String... filenames) {
        ArgumentNotValid.checkNotNull(replica, "Replica replica");
        ArgumentNotValid.checkNotNull(filenames, "String... filenames");
        
        // Contains all files that we couldn't reestablish
        List<String> troubleNames = new ArrayList<String>();
        
        // preservationStates: map [filename]->[filepreservationstate]
        // Initialized here to contain an entry for each filename in vargargs
        // 'filenames'.
        Map<String, FilePreservationState> preservationStates
            =  getFilePreservationStateMap(filenames);
        
        // For each given filename, try to reestablish it on
        // Replica 'replica'
        for (String fn: filenames) {
            FilePreservationState fps = preservationStates.get(fn);
            try {
                if (fps == null) {
                    throw new IllegalState("No state known about '" + fn + "'");
                }
                if (!fps.isAdminDataOk()) {
                    setAdminDataFailed(fn, replica);
                    admin.synchronize();
                    fps = getFilePreservationState(fn);
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
     * Reestablish a file missing in a bitarchive. The following pre-conditions
     * for reestablishing the file are checked before changing anything:<p> 
     * 1) the file is registered correctly in AdminData <br>
     * 2) the file is missing in the given bitarchive <br>
     * 3) the file is present in another bitarchive (the
     * reference archive)<br> 
     * 4) admin data and the reference archive agree on the
     * checksum of the file.
     *
     * @param fileName Name of the file to reestablish
     * @param damagedReplica Name of the replica missing the file
     * @param fps The FilePreservationStatus of the file to fix.
     * @throws IOFailure On trouble updating the file.
     */
    private void reestablishMissingFile(
            String fileName,
            Replica damagedReplica, FilePreservationState fps) {
        log.debug("Reestablishing missing file '" + fileName
                  + "' in bitarchive '" + damagedReplica + "'.");
        if (!satisfiesMissingFileConditions(fps, damagedReplica,
                                            fileName)) {
            throw new IOFailure(
                    "Unable to reestablish missing file. '" + fileName + "'. "
                    + "It is not in the right state.");
        }
        // Retrieve the file from the reference archive
        // TODO: ensure that referenceArchive is a bitarchive.
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
            throw new IOFailure(errmsg,
                                e);
        }
        log.info("Reestablished " + fileName
                 + " in " + damagedReplica.getName()
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
     * 1) the file is registered correctly in AdminData 
     * <br>2) the file is missing in the given bitarchive 
     * <br>3) the file is present in another bitarchive (the
     * reference archive) 
     * <br>4) admin data and the reference archive agree on the
     * checksum
     *
     * @param state            the status for one file in the bitarchives
     * @param damagedReplica the replica where the file is corrupt or
     *                          missing
     * @param fileName          the name of the file being considered
     * @return true if all conditions are true, false otherwise.
     */
    private boolean satisfiesMissingFileConditions(
            FilePreservationState state,
            Replica damagedReplica,
            String fileName) {
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
     * file in one bitarchive.  This method uses JMS and blocks until a reply is
     * sent.
     *
     * @param filename The file to change state for
     * @param rep       The replica to change state for the file for.
     *
     * @throws ArgumentNotValid if arguments are null or empty strings
     */
    private void setAdminDataFailed(String filename, Replica rep) {
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
     *
     * @throws IOFailure        if the file cannot be reestablished
     * @throws PermissionDenied if the file is not in correct state
     */
    public void replaceChangedFile(Replica replica, String filename,
                                   String credentials, String checksum) {
        ArgumentNotValid.checkNotNull(replica, "Replica replica");
        ArgumentNotValid.checkNotNullOrEmpty(filename, "String filename");
        ArgumentNotValid.checkNotNullOrEmpty(checksum, "String checksum");
        ArgumentNotValid.checkNotNullOrEmpty(credentials, "String credentials");
        removeAndGetFile(filename, replica, checksum, credentials);
        // The file named 'filename' is fetched from the reference replica
        // and uploaded to this replica
        uploadMissingFiles(replica, filename);
        // Remove filename from the WRONG_FILES list
        FileUtils.removeLineFromFile(filename, WorkFiles.getFile(
                replica,
                WorkFiles.WRONG_FILES));
    }

    /**
     * Call upon the arc repository to remove a file, returning it to this
     * machine.  The file is left around in case problems are later discovered,
     * and its location can be found in the log.
     *
     * @param filename    The file to remove.
     * @param rep  The replica to remove the file from. rep must be a 
     * bitarchive replica.
     * @param checksum    The checksum of the file.
     * @param credentials Credentials required to run this operation.
     */
    private void removeAndGetFile(String filename, Replica rep,
                                  String checksum, String credentials) {
        ArcRepositoryClientFactory.getPreservationInstance()
                .removeAndGetFile(filename, rep.getId(), checksum,
                                  credentials);
        FileUtils.appendToFile(WorkFiles.getFile(rep,
                                                 WorkFiles.MISSING_FILES_BA),
                               filename);
        FileUtils.removeLineFromFile(filename, WorkFiles.getFile(
                rep,
                WorkFiles.FILES_ON_BA));
    }

    /**
     * Return a list of files present in bitarchive but missing in AdminData.
     *
     * @return A list of missing files.
     *
     * @throws IOFailure if the list cannot be generated.
     */
    public Iterable<String> getMissingFilesForAdminData() {
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
    public Iterable<String> getChangedFilesForAdminData() {
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
        //TODO Also update store states if wrong.
    }

    /**
     * Run any batch job on a replica, possibly restricted to a certain set of
     * files, and place the output in the given file.  The results will also be
     * checked to verify that there for each file processed is a line in the
     * output file.
     *
     * @param job             The job to run.
     * @param replica        The replica (bitarchive) that the job should run
     *                        on.
     * @param specifiedFiles  The files to run the job on, or null if it should
     *                        run on all files.
     * @param batchOutputFile Where to put the result of the job.
     */
    private void runBatchJob(FileBatchJob job, Replica replica,
                             List<String> specifiedFiles,
                             File batchOutputFile) {
        job.processOnlyFilesNamed(specifiedFiles);
        BatchStatus status
                = ArcRepositoryClientFactory.getPreservationInstance()
                .batch(job, replica.getId());

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
                 + replica);
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
