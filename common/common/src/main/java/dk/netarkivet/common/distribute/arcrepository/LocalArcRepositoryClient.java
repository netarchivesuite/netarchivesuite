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
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

package dk.netarkivet.common.distribute.arcrepository;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.archive.io.arc.ARCReader;
import org.archive.io.arc.ARCReaderFactory;
import org.archive.io.arc.ARCRecord;

import dk.netarkivet.common.distribute.RemoteFileFactory;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.IllegalState;
import dk.netarkivet.common.exceptions.PermissionDenied;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.MD5;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.utils.batch.BatchLocalFiles;
import dk.netarkivet.common.utils.batch.ChecksumJob;
import dk.netarkivet.common.utils.batch.FileBatchJob;

/**
 * A simple implementation of ArcRepositoryClient that just has a number of
 * local directories that it keeps its files in.  It doesn't implement
 * credentials checks or checksum storing.
 */
public class LocalArcRepositoryClient implements ArcRepositoryClient {
    /** The logger for this class. */
    private Log log = LogFactory.getLog(getClass());
    /** List of the directories that we store files in. Non-absolute dirs are
     * relative to the current directory. */
    private final List<File> storageDirs = new ArrayList<File>(1);
    /** If no directories are specified in the settings file, use a single
     * directory with this name.
     */
    private static final String DEFAULT_DIR_NAME = "ArcRepository";
    /** Store the file in the directories designated by this setting. */
    private static final String FILE_DIRS
            = "settings.common.arcrepositoryClient.fileDir";
    private static final String CREDENTIALS_SETTING
            = "settings.archive.bitarchive.thisCredentials";

    /** Create a new LocalArcRepositoryClient based on current settings. */
    public LocalArcRepositoryClient() {
        List<String> fileDirs =
                Arrays.asList(Settings.getAll(FILE_DIRS));
        if (fileDirs.size() == 0) {
            fileDirs.add(DEFAULT_DIR_NAME);
        }
        for (String fileName : fileDirs) {
            File f = new File(fileName);
            FileUtils.createDir(f);
            log.info("directory '" +  f.getAbsolutePath() 
                    + "' is part of this local archive repository");
            storageDirs.add(f);
        }
    }

    /** Call on shutdown to release external resources. */
    public void close() {
    }

    /**
     * Store the given file in the ArcRepository.  After storing, the file is
     * deleted.
     *
     * @param file A file to be stored. Must exist.
     * @throws IOFailure thrown if store is unsuccessful, or failed to clean
     * up files after the store operation.
     * @throws IllegalState if file already exists.
     * @throws ArgumentNotValid if file parameter is null or file is not an
     *                          existing file.
     */
    public void store(File file) throws IOFailure, ArgumentNotValid {
        ArgumentNotValid.checkNotNull(file, "File file");
        ArgumentNotValid.checkTrue(file.exists(), "File '" + file
                                                  + "' does not exist");
        if (findFile(file.getName()) != null) {
            throw new IllegalState("A file with the name '"
                                       + file.getName() + " is already stored");
        }
        for (File dir : storageDirs) {
            if (dir.canWrite() && FileUtils.getBytesFree(dir) > file.length()) {
                FileUtils.moveFile(file, new File(dir, file.getName()));
                return;
            }
        }
        throw new IOFailure("Not enough room for '" + file
                            + "' in any of the dirs " + storageDirs);
    }

    /**
     * Gets a single ARC record out of the ArcRepository.
     *
     * @param arcfile The name of a file containing the desired record.
     * @param index   The offset of the desired record in the file
     * @return a BitarchiveRecord-object, or null if request times out or object
     * is not found.
     * @throws ArgumentNotValid on null or empty filenames, or if index is
     * negative.
     * @throws IOFailure If the get operation failed.
     */
    public BitarchiveRecord get(String arcfile, long index)
            throws ArgumentNotValid {
        ArgumentNotValid.checkNotNullOrEmpty(arcfile, "String arcfile");
        ArgumentNotValid.checkNotNegative(index, "long index");
        File f = findFile(arcfile);
        if (f == null) {
            log.warn("File '" + arcfile 
                    + "' does not exist. Null BitarchiveRecord returned");
            return null;
        }
        ARCReader reader = null;
        ARCRecord record = null;
        try {
            reader = ARCReaderFactory.get(f, index);
            record = (ARCRecord) reader.get();
            return new BitarchiveRecord(record);
        } catch (IOException e) {
            throw new IOFailure("Error reading record from '"
                    + arcfile + "' offset " + index, e);
        } finally {
            if (record != null) {
                try {
                    record.close();
                } catch (IOException e) {
                    log.warn("Error closing ARC record '" + record + "'", e);
                }
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    log.warn("Error closing ARC reader '" + reader + "'", e);
                }
            }
        }
    }

    /**
     * Retrieves a file from an ArcRepository and places it in a local file.
     *
     * @param arcfilename Name of the arcfile to retrieve. 
     * @param replica The bitarchive to retrieve the data from.
     * @param toFile Filename of a place where the file fetched can be put.
     * @throws ArgumentNotValid if arcfilename is null or empty, or if toFile
     * is null
     * @throws IOFailure if there are problems reading or writing file, or 
     * the file with the given arcfilename could not be found.
     */
    public void getFile(String arcfilename, Replica replica, File toFile) {
        ArgumentNotValid.checkNotNullOrEmpty(arcfilename, "String arcfilename");
        ArgumentNotValid.checkNotNull(toFile, "File toFile");
        File f = findFile(arcfilename);
        if (f != null) {
            FileUtils.copyFile(f, toFile);
        } else {
            throw new IOFailure("File '" + arcfilename + "' does not exist");
        }
    }

    /**
     * Runs a batch job on each file in the ArcRepository.
     *
     * @param job An object that implements the FileBatchJob interface. The
     *  initialize() method will be called before processing and the finish()
     *  method will be called afterwards. The process() method will be called
     *  with each File entry. An optional function postProcess() allows handling
     *  the combined results of the batchjob, e.g. summing the results, sorting,
     *  etc.
     *
     * @param replicaId The archive to execute the job on.
     * @param args The arguments for the batchjob. This can be null.
     * @return The status of the batch job after it ended.
     * @throws ArgumentNotValid If the job is null or the replicaId is either
     * null or the empty string.
     * @throws IOFailure If a problem occurs during processing the batchjob.
     */
    public BatchStatus batch(final FileBatchJob job, String replicaId, 
            String... args) throws ArgumentNotValid, IOFailure {
        ArgumentNotValid.checkNotNull(job, "FileBatchJob job");
        ArgumentNotValid.checkNotNullOrEmpty(replicaId, 
                "String replicaId");
        OutputStream os = null;
        File resultFile;
        try {
            resultFile = File.createTempFile("batch", replicaId,
                    FileUtils.getTempDir());
            os = new FileOutputStream(resultFile);
            List<File> files = new ArrayList<File>();
            final FilenameFilter filenameFilter = new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    Pattern filenamePattern = job.getFilenamePattern();
                    return new File(dir, name).isFile() && (
                            filenamePattern == null
                            || filenamePattern.matcher(name).matches());
                }
            };
            for (File dir : storageDirs) {
                File[] filesInDir = dir.listFiles(filenameFilter);
                if (filesInDir != null) {
                    files.addAll(Arrays.asList(filesInDir));
                }
            }
            BatchLocalFiles batcher
                    = new BatchLocalFiles(files.toArray(
                            new File[files.size()]));
            batcher.run(job, os);
        } catch (IOException e) {
            throw new IOFailure("Cannot perform batch '" + job + "'", e);
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e) {
                    log.warn("Error closing batch output stream '" + os + "'",
                            e);
                }
            }
        }
        return new BatchStatus(replicaId, job.getFilesFailed(),
                job.getNoOfFilesProcessed(),
                RemoteFileFactory.getMovefileInstance(resultFile),
                //new ArrayList<FileBatchJob.ExceptionOccurrence>(0))
                job.getExceptions());
    }

    /** Updates the administrative data in the ArcRepository for a given
     * file and replica.
     *
     * @param fileName The name of a file stored in the ArcRepository.
     * @param bitarchiveId The id of the replica that the administrative
     * data for fileName is wrong for.
     * @param newval What the administrative data will be updated to.
     */
    public void updateAdminData(String fileName, String bitarchiveId,
                                ReplicaStoreState newval) {
    }

    /** Updates the checksum kept in the ArcRepository for a given
     * file.  It is the responsibility of the ArcRepository implementation to
     * ensure that this checksum matches that of the underlying files.
     *
     * @param filename The name of a file stored in the ArcRepository.
     * @param checksum The new checksum.
     */
    public void updateAdminChecksum(String filename, String checksum) {
    }

    /** Remove a file from one part of the ArcRepository, retrieving a copy
     * for security purposes.  This is typically used when repairing a file
     * that has been corrupted.
     *
     * @param fileName The name of the file to remove.
     * @param bitarchiveId The id of the replica from which to remove the file.
     * Not used in this implementation, may be null.
     * @param checksum The checksum of the file to be removed.
     * @param credentials A string that shows that the user is allowed to
     * perform this operation.
     * @return A local copy of the file removed.
     * @throws ArgumentNotValid On null or empty parameters for fileName,
     * checksum or credentials.
     * @throws IOFailure On IO trouble.
     * @throws PermissionDenied On wrong MD5 sum or wrong credentials.
     */
    public File removeAndGetFile(String fileName, String bitarchiveId,
                                 String checksum, String credentials) {
        // Ignores bitarchiveName, checksum, and credentials for now
        ArgumentNotValid.checkNotNullOrEmpty(fileName, "String fileName");
        ArgumentNotValid.checkNotNullOrEmpty(checksum, "String checksum");
        ArgumentNotValid.checkNotNullOrEmpty(credentials, "String credentials");
        File file = findFile(fileName);
        if (file == null) {
            throw new IOFailure("Cannot find file '" + fileName + "'");
        }
        try {
            if (!MD5.generateMD5onFile(file).equals(checksum)) {
                throw new PermissionDenied("Wrong checksum for removing file '"
                                       + fileName + "'");
            }
        } catch (IOException e) {
            throw new IOFailure("Unable to check md5 on file '" + file + "'",
                                e);
        }
        if (!credentials.equals(Settings.get(CREDENTIALS_SETTING))) {
            throw new PermissionDenied("Wrong credentials for removing file '" 
                                   + fileName + "'");
        }
        File copiedTo = null;
        try {
            copiedTo = File.createTempFile("removeAndGetFile", fileName);
        } catch (IOException e) {
            throw new IOFailure("Cannot make temp file to copy '"
                    + fileName + "' into", e);
        }
        FileUtils.moveFile(file, copiedTo);
        return copiedTo;
    }

    /** Returns a File object for a filename if it exists in the archive.
     *
     * @param filename Name of file to find.
     * @return A File object for the filename if the file exists,
     * otherwise null.
     */
    private File findFile(String filename) {
        for (File dir : storageDirs) {
            final File file = new File(dir, filename);
            if (file.isFile()) {
                return file;
            }
        }
        return null;
    }

    /**
     * Method for retrieving the checksums of all the files of the replica.
     * 
     * @param replicaId Inherited dummy argument.
     * @return A file containing the names and checksum of all the files in
     * the system.
     * @throws ArgumentNotValid If the replicaId is either null or the empty
     * string.
     * @throws IOFailure If an unexpected IOException is caught.
     */
    @Override
    public File getAllChecksums(String replicaId) throws IOFailure, 
            ArgumentNotValid {
        ArgumentNotValid.checkNotNullOrEmpty(replicaId, "String replicaId");
        
        try {
            List<String> checksums = new ArrayList<String>();
            // go through the different storageDirs and find files and checksums.
            for(File dir : storageDirs) {
                // go through all file and calculate the checksum
                for(File entry : dir.listFiles()) {
                    String checksum = MD5.generateMD5onFile(entry);
                    String filename = entry.getName();

                    checksums.add(ChecksumJob.makeLine(filename, checksum));
                }
            }

            // create a file with the results.
            File res = File.createTempFile("all", "checksums", 
                    FileUtils.getTempDir());
            FileUtils.writeCollectionToFile(res, checksums);
            return res;
        } catch (IOException e) {
            throw new IOFailure("Received unexpected IOFailure: ", e);
        }
    }

    /**
     * Method for retrieving all the filenames of the replica.
     * 
     * @param replicaId Inherited dummy argument.
     * @return A file containing the names of all the files.
     * @throws ArgumentNotValid If the replicaId is either null or empty.
     * @throws IOFailure If an IOException is caught.
     */
    @Override
    public File getAllFilenames(String replicaId) throws IOFailure, 
            ArgumentNotValid {
        ArgumentNotValid.checkNotNullOrEmpty(replicaId, "String replicaId");
        
        List<String> filenames = new ArrayList<String>();
        // go through the different storageDirs and put the name of the files
        // into the resulting list of filenames.
        for(File dir : storageDirs) {
            for(String name : dir.list()) {
                filenames.add(name);
            }
        }
        
        try {
            File res = File.createTempFile("all", "filenames", 
                    FileUtils.getTempDir());
            FileUtils.writeCollectionToFile(res, filenames);
            return res;
        } catch (IOException e) {
            throw new IOFailure("Received unexpected IOFailure: ", e);
        }
    }

    /**
     * Method for correcting a bad entry.
     * Calls 'removeAndGetFile' followed by 'store'.
     *  
     * @param replicaId Inherited dummy argument.
     * @param checksum The checksum of the bad entry.
     * @param file The new file to replace the bad entry.
     * @param credentials The 'password' to allow changing the archive.
     * @return The bad entry file.
     * @throws ArgumentNotValid If one of the arguments are null, or if a string
     * is empty.
     * @throws PermissionDenied If the credentials or checksum are invalid. 
     */
    @Override
    public File correct(String replicaId, String checksum, File file, 
	    String credentials) throws ArgumentNotValid, PermissionDenied {
        ArgumentNotValid.checkNotNullOrEmpty(replicaId, "String replicaId");
        ArgumentNotValid.checkNotNullOrEmpty(checksum, "String checksum");
        ArgumentNotValid.checkNotNull(file, "File file");
        ArgumentNotValid.checkNotNullOrEmpty(credentials, "String credentials");
        
        // remove bad file.
        File res = removeAndGetFile(file.getName(), replicaId, 
                checksum, credentials);
        // store good new file.
        store(file);
        // return bad file.
        return res;
    }

    /**
     * Method for finding the checksum of a file.
     * 
     * @param replicaId Inherited dummy variable.
     * @param filename The name of the file to calculate the checksum.
     * @return The checksum of the file, or the empty string if the file was 
     * not found or an error occured.
     * @throws ArgumentNotValid If the replicaId or the filename is either
     * null or the empty string.
     */
    @Override
    public String getChecksum(String replicaId, String filename) 
            throws ArgumentNotValid {
        ArgumentNotValid.checkNotNullOrEmpty(replicaId, "String replicaId");
        ArgumentNotValid.checkNotNullOrEmpty(filename, "String filename");
        
        try {
            return MD5.generateMD5onFile(findFile(filename));
        } catch (IOException e) {
            log.warn("Unexpected error when generating MD5 on file '" 
                    + filename + "'. The empty string returned", e);
            return "";
        }
    }
}
