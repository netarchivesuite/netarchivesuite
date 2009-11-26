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
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301 
 *  USA
 */
package dk.netarkivet.archive.arcrepository.bitpreservation;

import java.io.File;
import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.common.distribute.arcrepository.ArcRepositoryClientFactory;
import dk.netarkivet.common.distribute.arcrepository.BatchStatus;
import dk.netarkivet.common.distribute.arcrepository.PreservationArcRepositoryClient;
import dk.netarkivet.common.distribute.arcrepository.Replica;
import dk.netarkivet.common.distribute.arcrepository.ReplicaType;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.IllegalState;
import dk.netarkivet.common.exceptions.NotImplementedException;
import dk.netarkivet.common.exceptions.UnknownID;
import dk.netarkivet.common.utils.CleanupHook;
import dk.netarkivet.common.utils.CleanupIF;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.batch.FileBatchJob;

/**
 * The database based active bit preservation.
 * This is the alternative to the FileBasedActiveBitPreservation.
 * 
 * A database is used to handle the bitpreservation. 
 */
public final class DatabaseBasedActiveBitPreservation implements 
        ActiveBitPreservation, CleanupIF {
    /** The log.*/
    private Log log
            = LogFactory.getLog(
                    DatabaseBasedActiveBitPreservation.class);
    
    /**
     * When replacing a broken file, the broken file is downloaded and stored in
     * a temporary directory under Settings.COMMON_TEMP_DIR with this name.
     * It can then be inspected at your leisure.
     * TODO this is the same constant as in FileBasedActiveBitPreservation, 
     * thus change to global constant instead. Perhaps constant in parent class.
     */
    private static final String REMOVED_FILES = "bitpreservation";
    
    /** The current instance.*/
    private static DatabaseBasedActiveBitPreservation instance;
    
    /** Hook to close down this application.*/
    private CleanupHook closeHook;
    
    /** The instance to contain the access to the database.*/
    private BitPreservationDAO cache;
    
    /**
     * Constructor.
     * Initialises the database and closeHook.
     */
    private DatabaseBasedActiveBitPreservation() {
        // initialise the database.
        cache = ReplicaCacheDatabase.getInstance();

        // TODO if the cache is empty, then rebuild the database.

        // create and initialise the closing hook
        closeHook = new CleanupHook(this);
        Runtime.getRuntime().addShutdownHook(closeHook);
    }
    
    /**
     * Method for retrieving the current instance of this class.
     * 
     * @return The instance.
     */
    public static DatabaseBasedActiveBitPreservation getInstance() {
        if (instance == null) {
            instance = new DatabaseBasedActiveBitPreservation();
        }
        return instance;
    }
    
    /**
     * This is a method to recreate the database, if it somehow has been lost.
     * It just calls all the replicas and retrieves the checksum for each 
     * replica in the ChecksumEntry form, which contain both the checksum and 
     * the filenames. Thus the names of all the files within any replica should
     * be retrieved, along with at least one checksum.
     * These checksum results should then be put into the database.
     */
    public void rebuildDatabase() {
        // go through all replicas to retrieve the checksum content of
        // their content.
        for (Replica replica : Replica.getKnown()) {
            List<ChecksumEntry> checksumOutput = getChecksumList(replica);
            cache.addChecksumInformation(checksumOutput, replica);
        }
    }

    /**
     * Method for retrieving the filelist from a specific replica.
     * The filelist are retrieved differently for the different types of 
     * replica:
     * <br/> The filelist of a bitarchive is retrieved through running 
     * the BatchJob FilelistJob on the replica.
     * <br/> The filelist of a checksumsarchive is retrieved through a 
     * GetAllFilenamesMessage.
     * 
     * @param replica The replica to retrieve the filelist from.
     * @return The names of the files in a list.
     * @throws ArgumentNotValid If the replica is 'null'.
     * @throws UnknownID If the replica has a unhandled replica type.
     */
    private List<String> getFilenamesList(Replica replica) throws 
            ArgumentNotValid, UnknownID {
        // validate
        ArgumentNotValid.checkNotNull(replica, "Replica replica");

        // handle the different types of replica.
        if (replica.getType() == ReplicaType.BITARCHIVE) {
            // create temporary file.
            File outputFile = WorkFiles.getFile(replica,
                    WorkFiles.CHECKSUMS_ON_BA);

            // send the batch job.
            log.info("Running ChecksumJob on replica '" + replica + "'.");
            runBatchJob(new FileListJob(), replica, null, outputFile);

            // parse the output file into resulting list and return it.
            return FileUtils.readListFromFile(outputFile);
        } else if (replica.getType() == ReplicaType.CHECKSUM) {
            // send request
            log.info("Retrieving checksum from replica '" + replica + "'.");

            // Retrieve the checksum job.
            return FileUtils
                    .readListFromFile(ArcRepositoryClientFactory
                            .getPreservationInstance().getAllFilenames(
                                    replica.getId()));
        } else {
            // throw a error
            String msg = "Cannot handle the replica type of replica '"
                    + replica + "'. Thus cannot find changed files.";
            log.warn(msg);
            throw new UnknownID(msg);
        }
    }
    
    /**
     * Method for retrieving the checksums from a specific replica.
     * The checksum are retrieved differently for the different types of 
     * replica:
     * <br/> The checksums of a bitarchive is retrieved through running 
     * the BatchJob ChecksumJob on the replica.
     * <br/> The checksums of a checksumsarchive is retrieved through a 
     * GetAllChecksumMessage.
     * 
     * @param replica The replica to retrieve the checksums from.
     * @return The checksums in the format of ChecksumEntry.
     * @throws IOFailure If a bitarchive does not return a output file.
     * @throws IllegalState If the replica has an unknown replica type.
     */
    private List<ChecksumEntry> getChecksumList(Replica replica)
            throws IOFailure, IllegalState {
        // validate
        ArgumentNotValid.checkNotNull(replica, "Replica replica");

        // handle the different types of replica.
        if (replica.getType() == ReplicaType.BITARCHIVE) {
            // create temporary file.
            File outputFile = WorkFiles.getFile(replica,
                    WorkFiles.CHECKSUMS_ON_BA);

            // send the batch job.
            log.info("Running ChecksumJob on replica '" + replica + "'.");
            runBatchJob(new ChecksumJob(), replica, null, outputFile);

            // handle the case where no output file was retrieved.
            if (outputFile == null || !outputFile.exists()) {
                String errMsg = "The ChecksumJob sent to replica '" + replica
                        + "' did not give any output file.";
                log.warn(errMsg);
                throw new IOFailure(errMsg);
            }

            // parse the output file into resulting list and return it.
            return ChecksumEntry.parseChecksumJob(outputFile);
        } else if (replica.getType() == ReplicaType.CHECKSUM) {
            // send request
            log.info("Retrieving checksum from replica '" + replica + "'.");

            // Retrieve the checksum job.
            return ChecksumEntry
                    .parseChecksumJob(ArcRepositoryClientFactory
                            .getPreservationInstance().getAllChecksums(
                                    replica.getId()));
        } else {
            // Unknown replica type -> this is not allowed!
            String msg = "Cannot handle the replica type of replica '"
                    + replica + "'. Thus cannot find changed files.";
            log.warn(msg);
            throw new IllegalState(msg);
        }
    }
    
    /**
     * Run any batch job on a replica, possibly restricted to a certain set of
     * files, and place the output in the given file. The results will also be
     * checked to verify that there for each file processed is a line in the
     * output file.
     *
     * @param job The BatchJob to run upon the archive.
     * @param replica The replica (which has to be a bitarchive) that the job 
     * should run on.
     * @param specifiedFiles  The files to run the job on, or null if it should
     * run on all files.
     * @param batchOutputFile Where to put the result of the job.
     * @throws IllegalState If the replica is not of the type 'BITARCHIVE', 
     * which is required for a batchjob.
     * @throws IOFailure If the batchjob status is invalid.
     */
    private void runBatchJob(FileBatchJob job, Replica replica,
            List<String> specifiedFiles, File batchOutputFile) 
            throws IOFailure, IllegalState {
        // Makes sure, that the replica is of the type 'bitarchive'.
        if (!replica.getType().equals(ReplicaType.BITARCHIVE)) {
            String msg = "The replica '" + replica + "' has to be of the "
                    + "type '" + ReplicaType.BITARCHIVE.name();
            log.warn(msg);
            throw new IllegalState(msg);
        }

        job.processOnlyFilesNamed(specifiedFiles);
        BatchStatus status = ArcRepositoryClientFactory
                .getPreservationInstance().batch(job, replica.getId());
        
        // Write output to file, if we got any
        if (!status.hasResultFile()) {
            String msg = "No result file for FileBatchJob on replica '"
                    + replica + "'.";
            log.warn(msg);
            throw new IOFailure(msg);
        }

        // Report errors
        if (!status.getFilesFailed().isEmpty()) {
            String msg = "Failed with the following files: "
                    + status.getFilesFailed().toString();
            log.warn(msg);
            throw new IOFailure(msg);
        }
        
        // copy the results to the output file.
        status.copyResults(batchOutputFile);
        log.info("FileBatchJob succeeded and processed "
                 + status.getNoOfFilesProcessed() + " files on replica "
                 + replica);
    }
    
    /**
     * Method to reestablish a file missing in a replica. The file is retrieved
     * from a given bitarchive replica, which is known to contain a proper
     * version of this file.
     * The reestablishment is done by first retrieving the file and then 
     * sending out a store message with this file. Then any replica who is 
     * missing the file will obtain it. 
     * 
     * @param filename The name of the file to reestablish.
     * @param repWithFile The replica where the file should be retrieved from.
     */
    private void reestablishMissingFile(String filename, Replica repWithFile) {
        // send a GetFileMessage to this bitarchive replica for the file.
        try {
            // Contact the ArcRepository.
            PreservationArcRepositoryClient arcrep = ArcRepositoryClientFactory
                    .getPreservationInstance();
            // Create temporary file.
            File tmpDir = FileUtils.createUniqueTempDir(FileUtils.getTempDir(),
                    REMOVED_FILES);
            File missingFile = new File(tmpDir, filename);
            // retrieve the file and send store message with it. Then the
            // file will be stored in all replicas, where it is missing.
            arcrep.getFile(filename, repWithFile, missingFile);
            arcrep.store(missingFile);
            // remove the temporary file afterwards.
            tmpDir.delete();
        } catch (Exception e) {
            String errMsg = "Failed to reestablish '" + filename
                    + "' with copy from '" + repWithFile + "'";
            log.warn(errMsg);
            // log error and report file.
            throw new IOFailure(errMsg, e);
        }
    }
    
    /**
     * Method for retrieving a file from a bitarchive.
     * 
     * @param filename The file to retrieve.
     * @param repWithFile The replica where the file should be retrieved from.
     * @return The file from the bitarchive.
     */
    private File retrieveRemoteFile(String filename, Replica repWithFile) {
        // Contact the ArcRepository.
        PreservationArcRepositoryClient arcrep = ArcRepositoryClientFactory
                .getPreservationInstance();
        // Create temporary file.
        File tmpDir = FileUtils.createUniqueTempDir(FileUtils.getTempDir(),
                REMOVED_FILES);
        File missingFile = new File(tmpDir, filename);
        // retrieve the file and send store message with it. Then the
        // file will be stored in all replicas, where it is missing.
        arcrep.getFile(filename, repWithFile, missingFile);

        return missingFile;
    }
    
    /**
     * This method is used for making sure, that all replicas are up-to-date 
     * before trying to validate the checksums of the files within it.
     * 
     * TODO set a time limit for last date to update. This has to be a variable
     * in settings, which should have the default '0', meaning no time limit.
     * If more time has passed than acceptable, then a new checksum job should
     * be run.
     */
    private void initChecksumStatusUpdate() {
        // go through all replicas.
        for (Replica replica : Replica.getKnown()) {
            // retrieve the date for the last checksum update.
            Date csDate = cache.getDateOfLastWrongFilesUpdate(replica);

            if (csDate == null) {
                // run a checksum job on the replica.
                runChecksum(replica);
            }
        }
    }
    
    /**
     * The method for retrieving the checksums for all the files witin a
     * replica.
     * This method sends the checksum job to the replica archive.
     * 
     * @param replica The replica to retrieve the checksums from.
     */
    private void runChecksum(Replica replica) {
        // Run checksum job upon replica
        List<ChecksumEntry> checksumEntries = getChecksumList(replica);

        // update database with new checksums
        cache.addChecksumInformation(checksumEntries, replica);
    }

    /**
     * The method calculates the number of files which has a wrong checksum
     * for the replica.
     * This simple counts all the entries in the replicafileinfo table for the
     * replica where the filelist_status is set to CORRUPT.
     * 
     * @param replica The replica for which to count the number of changed 
     * files.
     * @return The number of files for the replica where the checksum does not
     * correspond to the checksum of the same file in the other replicas.
     */
    public long getNumberOfChangedFiles(Replica replica) {
        // validate
        ArgumentNotValid.checkNotNull(replica, "Replica replica");

        // Retrieves the number of entries in replicafileinfo which has
        // filelist_status set to 'CORRUPT' and belong to the replica.
        return cache.getNumberOfWrongFilesInLastUpdate(replica);
    }

    /**
     * This method retrieves the name of all the files which has a wrong 
     * checksum for the replica.
     * It simple returns the filename of all the entries in the 
     * replicafileinfo table for the replica where the filelist_status is 
     * set to CORRUPT.
     * 
     * @param replica The replica for which the changed files should be found.
     * @return The names of files in the replica where the checksum does not
     * correspond to the checksum of the same file in the other replicas.
     */
    public Iterable<String> getChangedFiles(Replica replica) {
        // validate
        ArgumentNotValid.checkNotNull(replica, "Replica replica");

        // Retrieves the list of filenames for the entries in replicafileinfo
        // which has filelist_status set to 'CORRUPT' and belong to the replica.
        return cache.getWrongFilesInLastUpdate(replica);
    }

    /**
     * This method calculates the number of files which are not found in the 
     * given replica.
     * This simple counts all the entries in the replicafileinfo table for the
     * replica where the filelist_status is set to MISSING.
     * 
     * @param replica The replica for which to count the number of missing 
     * files.
     * @return The number of files which is missing in the replica.
     */
    public long getNumberOfMissingFiles(Replica replica) {
        // validate
        ArgumentNotValid.checkNotNull(replica, "Replica replica");

        // Retrieves the number of entries in replicafileinfo which has
        // filelist_status set to 'MISSING' and belong to the replica.
        return cache.getNumberOfMissingFilesInLastUpdate(replica);
    }

    /**
     * This method retrieves the name of all the files which are missing for 
     * the given replica.
     * It simple returns the filename of all the entries in the 
     * replicafileinfo table for the replica where the filelist_status is 
     * set to MISSING.
     * 
     * @param replica The replica for which the missing files should be found.
     * @return The names of files in the replica which are missing.
     */
    public Iterable<String> getMissingFiles(Replica replica) {
        // validate
        ArgumentNotValid.checkNotNull(replica, "Replica replica");

        // Retrieves the list of filenames for the entries in replicafileinfo
        // which has filelist_status set to 'MISSING' and belong to the replica.
        return cache.getMissingFilesInLastUpdate(replica);
    }

    /**
     * This method retrieves the date for the latest checksum update was 
     * performed for the replica. This means the date for the latest the
     * replica has calculated the checksum of all the files within its archive.
     * 
     * This method does not call out to the replicas. It only contacts the 
     * local database.
     * 
     * @param replica The replica for which the date for last checksum update 
     * should be retrieved.
     * @return The date for the last time the checksums has been update. If the 
     * checksum update has never occurred, then a null is returned. 
     */
    public Date getDateForChangedFiles(Replica replica) {
        // validate
        ArgumentNotValid.checkNotNull(replica, "Replica replica");

        // Retrieves the checksum_updated date for the entry in the replica
        // table in the database corresponding to the replica argument.
        return cache.getDateOfLastWrongFilesUpdate(replica);
    }

    /**
     * This method retrieves the date for the latest filelist update was 
     * performed for the replica. This means the date for the latest the
     * replica has retrieved the list of all the files within the archive.
     * 
     * This method does not call out to the replicas. It only contacts the 
     * local database.
     * 
     * @param replica The replica for which the date for last filelist update 
     * should be retrieved.
     * @return The date for the last time the filelist has been update. If the 
     * filelist update has never occurred, then a null is returned. 
     */
    public Date getDateForMissingFiles(Replica replica) {
        // validate
        ArgumentNotValid.checkNotNull(replica, "Replica replica");

        // Retrieves the filelist_updated date for the entry in the replica
        // table in the database corresponding to the replica argument.
        return cache.getDateOfLastMissingFilesUpdate(replica);
    }
    
    /**
     * The method is used to update the checksum for all the files in a replica.
     * The checksum for the replica is retrieved either through a ChecksumJob
     * (for a bitarchive) or through a GetAllChecksumMessage (for a 
     * checksumarchive).
     * This will take a very large amount of time for the bitarchive, but a 
     * more limited amount of time for the checksumarchive.
     * 
     * The corresponding replicafileinfo entries in the database for the 
     * retrieved checksum results will be updated. Then a checksum update will 
     * be performed to check for corrupted replicafileinfo.
     * 
     * @param replica The replica to find the changed files for.
     * @throws ArgumentNotValid If the replica is null.
     */
    public void findChangedFiles(Replica replica) throws ArgumentNotValid {
        // validate
        ArgumentNotValid.checkNotNull(replica, "Replica replica");

        // retrieve updated checksums from the replica.
        runChecksum(replica);

        // make sure, that all replicas are up-to-date.
        initChecksumStatusUpdate();

        // update to find changes.
        cache.updateChecksumStatus();
    }

    /**
     * This method retrieves the filelist for the replica, and then it updates
     * the database with this list of filenames.
     * 
     * @param replica The replica to find the missing files for.
     * @throws ArgumentNotValid If the replica is null.
     */
    public void findMissingFiles(Replica replica) throws ArgumentNotValid {
        // validate
        ArgumentNotValid.checkNotNull(replica, "Replica replica");

        // retrieve the filelist from the replica.
        List<String> filenames = getFilenamesList(replica);

        // put them into the database.
        cache.addFileListInformation(filenames, replica);
    }

    /**
     * Method for retrieving the FilePreservationState for a specific file.
     * 
     * @param filename The name of the file for whom the FilePreservationState
     * should be retrieved.
     * @return The FilePreservationState for the file.
     * @throws NotImplementedException This method has not yet been implemented.
     * @throws ArgumentNotValid If the filename does not have a valid name.
     */
    @Override
    public FilePreservationState getFilePreservationState(String filename) 
            throws NotImplementedException, ArgumentNotValid {
        ArgumentNotValid.checkNotNullOrEmpty(filename, "String filename");
        
        // TODO requires changing the FilePreservationState ->
        // should not use admin.data

        // How to handle FilePreservationState ??
        // Make new!

        throw new NotImplementedException("TODO: Implement me!");
    }

    /**
     * Method for retrieving the FilePreservationState for a list of files.
     * 
     * @param filenames The list of filenames whos FilePreservationState should
     * be retrieved.
     * @return A mapping between the filenames and their FilePreservationState.
     * @throws NotImplementedException Since it has not yet been implemented.
     * @throws ArgumentNotValid If the filenames are invalid.
     */
    @Override
    public Map<String, FilePreservationState> getFilePreservationStateMap(
            String... filenames) throws ArgumentNotValid, 
            NotImplementedException {
        ArgumentNotValid.checkNotNull(filenames, "String... filenames");
        
        // TODO Auto-generated method stub
        throw new NotImplementedException("TODO: Implement me!");
    }

    /**
     * This method finds the number of files which are known to be in the 
     * archive of a specific replica.
     * This method will not go out to the replica, but only contact the local
     * database.
     * The number of files in the replica is retrieved from the database
     * by counting the amount of files in the replicafileinfo table which 
     * belong to the replica and which has the filelist_status set to OK.
     * 
     * @param replica The replica for which the number of files should be 
     * counted.
     * @return The number of files for a specific replica.
     */
    public long getNumberOfFiles(Replica replica) {
        // returns the amount of files, which is not missing.
        return cache.getNumberOfFiles(replica);
    }

    /**
     * Check that the checksum of the file is indeed different to the value in
     * admin data and reference replica. If so, remove missing file and upload
     * it from reference replica to this replica.
     *
     * @param replica The replica to restore file to
     * @param filename The name of the file
     * @param credentials The credentials used to perform this replace operation
     * @param checksum  The known bad checksum. Only a file with this bad
     * checksum is attempted repaired.
     * @throws IOFailure if the file cannot be reestablished.
     * @throws UnknownID if the file is not in correct state.
     * @throws ArgumentNotValid if any of the arguments are not valid.
     */
    public void replaceChangedFile(Replica replica, String filename,
            String credentials, String checksum) throws UnknownID, IOFailure, 
            ArgumentNotValid {
        ArgumentNotValid.checkNotNull(replica, "Replica replica");
        ArgumentNotValid.checkNotNullOrEmpty(filename, "String filename");
        ArgumentNotValid.checkNotNullOrEmpty(checksum, "String checksum");
        ArgumentNotValid.checkNotNullOrEmpty(credentials, "String credentials");

        // replace the file.
        if(replica.getType().equals(ReplicaType.BITARCHIVE)) {
            System.out.println("Trying to remove file from bitarchive.");
            // remove file.
            // Send remove file message!
            ArcRepositoryClientFactory.getPreservationInstance()
                    .removeAndGetFile(filename, replica.getId(), checksum, 
                            credentials);
            // upload file.
            uploadMissingFiles(replica, filename);
        } else if(replica.getType().equals(ReplicaType.CHECKSUM)) {
            System.out.println("Trying to remove file from checksumarchive");
            
            // find replica
            Replica repWithFile = cache.getBitarchiveWithGoodFile(filename, 
                    replica);
            // retrieve the file from the replica.
            File missingFile = retrieveRemoteFile(filename, repWithFile);
            // upload the file to the replica where it is missing
            ArcRepositoryClientFactory.getPreservationInstance().correct(
                    replica.getId(), checksum, missingFile, credentials);
        } else {
            String errMsg = "The replica is of a type which is currently not "
                + "handled: " + replica;
            log.warn(errMsg);
            throw new UnknownID(errMsg);
        }
    }

    /**
     * This method is used to upload missing files to a replica.
     * For each file a good version of this file is found, and it is 
     * reestablished on the replicas where it is missing.
     * 
     * @param replica The replica where the files are missing.
     * @param filenames The names of the files which are missing in the given
     * replica.
     */
    public void uploadMissingFiles(Replica replica, String... filenames) {
        // make record of files, which is not uploaded correct.
        List<String> filesFailedReestablishment = new ArrayList<String>();

        // For each file in filenames
        for (String file : filenames) {
            // retrieve a replica which has the file and the checksum_status
            // is 'OK'. Though do not allow the replica where the file is
            // missing to be returned.
            Replica fileRep = cache.getBitarchiveWithGoodFile(file, replica);

            // make sure, that a replica was found.
            if (fileRep == null) {
                // issue warning, report file, and continue to next file.
                String errMsg = "No bitarchive replica contains a version of "
                        + "the file with an acceptable checksum.";
                log.warn(errMsg);
                filesFailedReestablishment.add(file);
                continue;
            }

            try {
                // reestablish the missing file.
                reestablishMissingFile(file, fileRep);
            } catch (IOFailure e) {
                // if error, then not successfully reestablishment for the file.
                log.warn("The file '" + file + "' has not been reestablished "
                        + "on replica '" + replica + "' with a correct version"
                        + " from replica '" + fileRep + "'.", e);
                filesFailedReestablishment.add(file);
            }
        }

        // make warning if not all the files could be reestablished.
        if (filesFailedReestablishment.size() > 0) {
            throw new IOFailure("The following "
                    + filesFailedReestablishment.size() + " out of "
                    + filenames.length + " files could not be reestablished: "
                    + filesFailedReestablishment);
        }
    }

    /**
     * This should reestablish the state for the file.
     * 
     * @param filename The name of the file to change the state for.
     * @throws ArgumentNotValid If the filename is invalid.
     * @throws NotImplementedException Since it has not yet been implemented.
     */
    @Override
    public void changeStateForAdminData(String filename) 
            throws ArgumentNotValid, NotImplementedException {
        ArgumentNotValid.checkNotNullOrEmpty(filename, "String filename");

        // This function should not deal with admin.data.
        throw new NotImplementedException("TODO: Implement me!");
    }

    /**
     * Old method, which refers to the checksum replica part of admin data.
     * 
     * @return Nothing, since it always throws an exception.
     * @throws NotImplementedException This method will not be implemented.
     */
    @Override
    public Iterable<String> getMissingFilesForAdminData() 
            throws NotImplementedException {
        // This function should not deal with admin.data.
        throw new NotImplementedException("Old method, which refers to the "
                + "checksum replica part of admin data.");
    }

    /**
     * Old method, which refers to the checksum replica part of admin data.
     * 
     * @return Nothing, since it always throws an exception.
     * @throws NotImplementedException This method will not be implemented.
     */
    @Override
    public Iterable<String> getChangedFilesForAdminData() 
            throws NotImplementedException {
        // This function should not deal with admin.data.
        throw new NotImplementedException("Old method, which refers to the "
                + "checksum replica part of admin data.");
    }

    /**
     * Old method, which refers to teh checksum replica part of admin data.
     * 
     * @param filenames The list of filenames which should be added to admin 
     * data.
     * @throws NotImplementedException This method will not be implemented.
     * @throws ArgumentNotValid If filenames invalid.
     */
    @Override
    public void addMissingFilesToAdminData(String... filenames) throws 
            ArgumentNotValid, NotImplementedException {
        ArgumentNotValid.checkNotNull(filenames, "String... filenames");
        
        // This function should not deal with admin.data.        
        throw new NotImplementedException("Old method, which refers to the "
                + "checksum replica part of admin data.");
    }
    
    /**
     * Method for closing the running instance of this class.
     */
    public void close() {
        if (closeHook != null) {
            Runtime.getRuntime().removeShutdownHook(closeHook);
        }

        cleanup();
    }
    
    /**
     * Method for cleaning up this instance.
     */
    @Override
    public void cleanup() {
        instance = null;
        cache.cleanup();
    }
}
