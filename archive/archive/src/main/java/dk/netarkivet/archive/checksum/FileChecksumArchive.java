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
package dk.netarkivet.archive.checksum;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.archive.ArchiveSettings;
import dk.netarkivet.archive.arcrepository.bitpreservation.ChecksumJob;
import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.distribute.RemoteFile;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.IllegalState;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.MD5;
import dk.netarkivet.common.utils.Settings;

/**
 * A checksum archive in the form of a file (as alternative to a database).<br>
 * 
 * Each entry in the file is on its own line, thus the number of lines is the 
 * number of entries.<br>
 * The entries on a line is in the format of a ChecksumJob: <br>
 * <b>'filename' + ## + 'checksum'</b> <br>
 * The lines are not sorted.
 */
public final class FileChecksumArchive extends ChecksumArchive {
    /**
     * The character sequence for separating the filename from the checksum.
     */
    private static final String CHECKSUM_SEPARATOR = dk.netarkivet.archive
            .arcrepository.bitpreservation.Constants.STRING_FILENAME_SEPARATOR;
    
    /** The prefix to the filename. */
    private static final String FILENAME_PREFIX = "checksum_";
    /** The suffix to the filename. */
    private static final String FILENAME_SUFFIX = ".md5";
    /** The suffix of the filename of the recreation file.*/
    private static final String RECREATE_PREFIX = "recreate_";
    /** The suffix of the filename of the recreation file.*/
    private static final String RECREATE_SUFFIX = ".checksum";
    /** The prefix to the removedEntryFile. */
    private static final String WRONG_FILENAME_PREFIX = "removed_";
    /** The suffix to the removedEntryFile. */
    private static final String WRONG_FILENAME_SUFFIX = ".checksum";
    
    /**
     * The logger used by this class.
     */
    private static final Log log = LogFactory.getLog(FileChecksumArchive.class);
    
    /**
     * The current instance of this class.
     */
    private static FileChecksumArchive instance;
    
    /**
     * The file to store the checksum.
     * Each line should contain the following:
     * arc-filename + ## + checksum.
     */
    private File checksumFile;
    
    /**
     * The file for storing all the deleted entries.
     * Each entry should be: 'date :' + 'wrongEntry'.
     */
    private File wrongEntryFile;
    
    /**
     * This map consists of the archive loaded into the memory. It is faster to 
     * use a memory archive than the the checksum file, though all entries must
     * exist both in the file and the memory.   
     */
    private Map<String, String> checksumArchive = Collections.synchronizedMap(
            new HashMap<String, String>());
    
    /**
     * The minimum space left.
     */
    private long minSpaceLeft;
    
    /**
     * Method for obtaining the current singleton instance of this class.
     * If the instance of this class has not yet been constructed, then
     * it will be initialised.
     *  
     * @return The current instance of this class.
     */
    public static FileChecksumArchive getInstance() {
        if(instance == null) {
            instance = new FileChecksumArchive();
        }
        return instance;
    }
    
    /**
     * Constructor.
     * Retrieves the minimum space left variable, and ensures the existence of
     * the archive file. If the file does not exist, then it is created.
     * 
     * @throws ArgumentNotValid If the variable minimum space left is smaller
     * than zero. 
     * @throws IOFailure If the checksum file cannot be created.
     */
    private FileChecksumArchive() throws IOFailure, ArgumentNotValid {
        super();
        
        // Get the minimum space left setting.
        minSpaceLeft = Settings.getLong(
                ArchiveSettings.CHECKSUM_MIN_SPACE_LEFT);
        // make sure, that minSpaceLeft is non-negative.
        if(minSpaceLeft < 0) {
            String msg = "Wrong setting of minSpaceRequired read from "
                + "Settings: " + minSpaceLeft;
            log.warn(msg);
            throw new ArgumentNotValid(msg);
        }

        // Initialize the archive and bad-entry files.
        initializeFiles();
    }
    
    /**
     * Method for retrieving the name of the checksum file.
     * 
     * @return The checksum file name.
     */
    public String getFileName() {
        return checksumFile.getPath();
    }
    
    /**
     * Method for retrieving the name of the wrongEntryFile.
     * 
     * @return The wrong entry file name.
     */
    public String getWrongEntryFilename() {
        return wrongEntryFile.getPath();
    }

    /**
     * Method for testing where there is enough space left on local drive.
     * 
     * @return Whether there is enough space left.
     */
    public boolean hasEnoughSpace() {
        // The file must be valid and have enough space.
        if (checkArchiveFile(checksumFile)
                && (FileUtils.getBytesFree(checksumFile) > minSpaceLeft)) {
            return true;
        }
        return false;
    }
    
    /**
     * Method for testing whether there is enough left on the local drive
     * for recreating the checksum file.
     *  
     * @return False only if there is not enough space left.
     */
    private boolean hasEnoughSpaceForRecreate() {
        // check if the checksum file is larger than space left and the minimum
        // space left.
        if(checksumFile.length() + minSpaceLeft 
                > FileUtils.getBytesFree(checksumFile)) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Method for initializing the files.
     * Starts by initializing the removedEntryFile before initializing the 
     * checksumFile.
     * If the checksum file already exists, then it is loaded into memory.
     */
    private void initializeFiles() {
        // Extract the dir-name and create the dir (if it does not yet exist).
        File checksumDir = new File(Settings.get(
                ArchiveSettings.CHECKSUM_BASEDIR));
        if(!checksumDir.exists()) {
            checksumDir.mkdir();
        }
        
        // Get the name and initialise the wrong entry file.
        wrongEntryFile = new File(checksumDir, makeWrongEntryFileName());
        
        // ensure that the file exists.
        if(!wrongEntryFile.exists()) {
            try {
                wrongEntryFile.createNewFile();
            } catch (IOException e) {
                String msg = "Cannot create 'wrongEntryFile'!";
                log.error(msg);
                throw new IOFailure(msg, e);
            }
        }

        // get the name of the file and initialise it.
        checksumFile = new File(checksumDir, makeChecksumFileName());
        
        // make sure, that the file exists.
        if(!checksumFile.exists()) {
            try {
                checksumFile.createNewFile();
            } catch (IOException e) {
                String msg = "Cannot create checksum archive file!";
                log.error(msg);
                throw new IOFailure(msg, e);
            }
        } else {
            // If the archive file already exists, then it must consist of the
            // archive for this replica. It must therefore be loaded into the 
            // memory.
            loadFile();
        }
    }
    
    /**
     * Loads an existing checksum archive file into the memory.
     * This will go through every line, and if the line is valid, then it is 
     * loaded into the checksumArchive map in the memory. If the line is 
     * invalid then a warning is issued and the line is put into the 
     * wrongEntryFile.
     * 
     * If a bad entry is found, then the archive file has to be recreated 
     * afterwards, since the bad entry otherwise still would be in the archive 
     * file.
     */
    private void loadFile() {
        // Checks whether a bad entry was found, to decide whether the archive
        // file should be recreated.
        boolean recreate = false;
        
        // extract all the data from the file.
        List<String> entries;
        synchronized(checksumFile) {
            entries = FileUtils.readListFromFile(checksumFile);
        }

        String filename;
        String checksum;

        // go through all entries and extract their filename and checksum.
        for(String record : entries) {
            try {
                // extract the filename and checksum
                filename = extractFilename(record);
                checksum = extractChecksum(record);
                // If their are extracted correct, then they will be put 
                // into the archive.
                checksumArchive.put(filename, checksum);
            } catch (IllegalState e) {
                log.warn("An invalid entry in the loaded file: '" + record 
                        + "' This will be put in the wrong entry file.", e);
                // put into wrongEntryFile!
                appendWrongRecordToWrongEntryFile(record);
                recreate = true;
            }
        }
        
        // If a bad entry is found, then the archive file should be recreated.
        // Otherwise the bad entries might still be in the archive file next 
        // time the FileChecksumArchive is initialized/restarted.
        if(recreate) {
            recreateArchiveFile();
        }
    }
    
    /**
     * Recreates the archive file from the memory.
     * Makes a new file which contains the entire archive, and then
     * move the new archive file on top of the old one.
     * This is used when to recreate the archive file, when an record has been
     * removed.
     * 
     * @throws IOFailure If a problem occur when writing the new file.
     */
    private void recreateArchiveFile() throws IOFailure {
        try {
            // Handle the case, when there is not enough space left for 
            // recreating the 
            if(!hasEnoughSpaceForRecreate()) {
                String errMsg = "Not enough space left to recreate the "
                    + "checksum file.";
                log.error(errMsg);
                throw new IOFailure(errMsg);
            }

            // This should be synchronized, so no new entries can be made
            // while recreating the archive file.
            synchronized(checksumFile) {
                // initialize and create the file.
                File recreateFile = new File(checksumFile.getParentFile(), 
                        makeRecreateFileName());
                if(!recreateFile.createNewFile()) {
                    log.warn("Cannot create new file. The recreate checksum "
                            + "file did already exist.");
                }

                // put the archive into the file.
                FileWriter fw = new FileWriter(recreateFile);
                try {
                    for(Map.Entry<String, String> entry
                            : checksumArchive.entrySet()) {
                        String record = entry.getKey() + CHECKSUM_SEPARATOR 
                        + entry.getValue();
                        fw.append(record + "\n");
                    }
                } finally {
                    fw.flush();
                    fw.close();
                }
                
                // Move the file.
                FileUtils.moveFile(recreateFile, checksumFile);
            }
        } catch (IOException e) {
            String errMsg = "The checksum file has not been recreated as "
                + "attempted. The archive in memory and the one on file are "
                + "no longer identical.";
            log.error(errMsg, e);
            throw new IOFailure(errMsg, e);
        }
    }
    
    /**
     * Creates the string for the name of the checksum file.
     * E.g. checksum_REPLICA.md5.
     * 
     * @return The name of the file.
     */
    private String makeChecksumFileName() {
        return FILENAME_PREFIX + Settings.get(CommonSettings.USE_REPLICA_ID) 
                + FILENAME_SUFFIX;
    }
    
    /**
     * Creates the string for the name of the recreate file.
     * E.g. recreate_REPLICA.checksum.
     * 
     * @return The name of the file for recreating the checksum file.
     */
    private String makeRecreateFileName() {
        return RECREATE_PREFIX + Settings.get(CommonSettings.USE_REPLICA_ID)
                + RECREATE_SUFFIX;
    }
    
    /**
     * Creates the string for the name of the wrongEntryFile.
     * E.g. removed_REPLICA.checksum
     * 
     * @return The name of the wrongEntryFile.
     */
    private String makeWrongEntryFileName() {
        return WRONG_FILENAME_PREFIX + Settings.get(
                CommonSettings.USE_REPLICA_ID) + WRONG_FILENAME_SUFFIX;
    }
    
    /**
     * Method for validating a file for use as checksum file.
     * This basically checks whether the file exists, 
     * whether it is a directory instead of a file, 
     * and whether it is writable. 
     * 
     * It has to exist and be writable, but it may not be a directory.
     * 
     * @param file The file to validate.
     * @return Whether the file is valid.
     */
    private boolean checkArchiveFile(File file) {
        // The file must exist.
        if (!file.isFile()) {
            log.warn("The file '" + file.getAbsolutePath()
                    + "' is not a valid file.");
            return false;
        }
        // It must be writable.
        if (!file.canWrite()) {
            log.warn("The file '" + file.getAbsolutePath() 
                    + "' is not writable");
            return false;
        }
        return true;
    }

    /**
     * Method for extracting the filename of a record. 
     * 
     * @param record The record from which the filename should be extracted.
     * @return The filename of a record.
     */
    private String extractFilename(String record) {
        // A record is : filename##checksum,
        // thus the split gives the array: [filename, checksum]
        // and the first element in the array is thus the filename.
        String[] split = record.split(CHECKSUM_SEPARATOR);
        // handle the case when a entry has the wrong format. 
        if(split.length < 2) {
            String errMsg = "The record [" + record + "] cannot be parsed into"
                    + " a filename part and a checksum part.";
            log.warn(errMsg);
            throw new IllegalState(errMsg);
        }
        
        return split[0];
    }
    
    /**
     * Method for extracting the checksum of a record. 
     * 
     * @param record The record from which the checksum should be extracted.
     * @return The checksum of a record.
     * @throws IllegalState If the record cannot be parsed.
     */
    private String extractChecksum(String record) throws IllegalState {
        // A record is : filename##checksum,
        // thus the split gives the array: [filename, checksum]
        // and the second element in the array is thus the checksum.
        String[] split = record.split(CHECKSUM_SEPARATOR);
        // handle the case when a entry has the wrong format. 
        if(split.length < 2) {
            String errMsg = "The record [" + record + "] cannot be parsed into"
                    + " a filename part and a checksum part.";
            log.warn(errMsg);
            throw new IllegalState(errMsg);
        }
        
        return split[1];
    }
    
    /**
     * Appending an checksum archive entry to the checksum file.
     * The record string is created and appended to the file.
     *  
     * @param filename The name of the file to add.
     * @param checksum The checksum of the file to add.
     * @throws IOException If something is wrong when writing to the file.
     */
    private synchronized void appendEntryToFile(String filename, String 
            checksum) throws IOException {
        // initialise the record.
        String record = filename + CHECKSUM_SEPARATOR + checksum + "\n";
        
        // get a filewriter for the checksum file, and append the record. 
        boolean appendToFile = true;
        
        // Synchronize to ensure that the file is not overridden during the
        // appending of the new entry.
        synchronized(checksumFile) {
            FileWriter fwrite = new FileWriter(checksumFile, appendToFile);
            try {
                fwrite.append(record);
            } finally {
                // close fileWriter.
                fwrite.flush();
                fwrite.close();
            }
        }
    }
    
    /**
     * Method for appending a 'wrong' entry in the wrongEntryFile.
     * It will be written when the wrong entry was appended:
     * date + ## + wrongRecord.
     * 
     * @param wrongRecord The record to append.
     * @throws IOFailure If the wrong record cannot be appended correctly.
     */
    private synchronized void appendWrongRecordToWrongEntryFile(String 
            wrongRecord) throws IOFailure {
        try {
            // Create the string to append: date + 'wrong record'.
            String entry = new Date().toString() + " : " + wrongRecord + "\n";

            // get a filewriter for the checksum file, and append the record. 
            boolean appendToFile = true;
            FileWriter fwrite = new FileWriter(wrongEntryFile, appendToFile);
            fwrite.append(entry);

            // close fileWriter.
            fwrite.flush();
            fwrite.close();
        } catch (IOException e) {
            String errMsg = "Cannot put a bad record to the 'wrongEntryFile'.";
            log.warn(errMsg, e);
            throw new IOFailure(errMsg, e);
        }
    }

    /**
     * The method for uploading an arcFile to the archive.
     * 
     * TODO use file instead of remoteFile. Now the remoteFile is not 
     * automatically cleaned up afterwards.
     * 
     * @param arcfile The remote file containing the arcFile to upload.
     * @param filename The name of the arcFile.
     * @throws IOFailure If the entry cannot be added to the archive.
     * @throws ArgumentNotValid If the RemoteFile is null or if the filename
     * is not valid.
     */
    public void upload(RemoteFile arcfile, String filename) throws IOFailure, 
            ArgumentNotValid {
        // Validate arguments.
        ArgumentNotValid.checkNotNull(arcfile, "RemoteFile arcfile");
        ArgumentNotValid.checkNotNullOrEmpty(filename, "String filename");

        // calculate the checksum
        String checksum = calculateChecksum(arcfile.getInputStream());
        
        // check if file already exist in archive.
        if(checksumArchive.containsKey(filename)) {
            // handle whether the checksum are the same.
            if(checksumArchive.get(filename).equals(checksum)) {
                log.warn("Cannot upload arcfile '" + filename + "', "
                        + "it is already archived with the same checksum: '"
                        + checksum);
            } else {
                log.error("Cannot upload arcfile '" + filename + "', "
                        + "it is already archived with different checksum."
                        + " Archive checksum: '" + checksumArchive.get(filename)
                        + "' and the uploaded file has: '" + checksum + "'.");
                // TODO Perhaps throw exception?
            }
            
            // It is a success that it already is within the archive, thus do
            // not throw an exception. 
            return;
        }
        
        // otherwise put the file into memory and file. 
        try {
            appendEntryToFile(filename, checksum);
            checksumArchive.put(filename, checksum);
        } catch (IOException e) {
            String msg = "Could not append the file '" + filename 
                    + "' with the checksum '" + checksum + "' to the archive.";
            log.warn(msg);
            throw new IOFailure(msg, e);
        }
    }
    
    /**
     * Method for retrieving the checksum of a record, based on the filename.
     * 
     * @param filename The name of the file to have recorded in the archive.
     * @return The checksum of a record, or null if it was not found.
     * @throws ArgumentNotValid If the filename is not valid (null or empty).
     */
    public String getChecksum(String filename) throws ArgumentNotValid {
        // validate the argument
        ArgumentNotValid.checkNotNullOrEmpty(filename, "String filename");
        
        // Return the checksum of the record.
        return checksumArchive.get(filename);
    }
    
    /**
     * Method for checking whether an entry exists within the archive.
     * 
     * @param filename The name of the file whose entry in the archive should 
     * be determined.
     * @return Whether an entry with the filename was found.
     */
    public boolean hasEntry(String filename) {
        ArgumentNotValid.checkNotNullOrEmpty(filename, "String filename");

        // Return whether the archive contains an entry with the filename.
        return checksumArchive.containsKey(filename);
    }

    /**
     * Method for calculating the checksum of a file.
     * 
     * @param f The file to calculate the checksum of.
     * @return The checksum of the file.
     * @throws IOFailure If a IOException is caught during the calculation of
     * the MD5-checksum.
     */
    @Override
    protected String calculateChecksum(File f) throws IOFailure {
        try {
            // calculate the MD5 checksum
            return MD5.generateMD5onFile(f);
        } catch (IOException e) {
            // Handle exception during processing of the file.
            String msg = "Cannot calculate the MD5 checksum on file.";
            log.warn(msg, e);
            throw new IOFailure(msg, e);
        }
    }

    /**
     * Method for calculating the checksum of a inputstream.
     * 
     * @param is The inputstream to calculate the checksum of.
     * @return The checksum of the inputstream.
     * @throws IOFailure If a error occurs during the generation of the 
     * MD5 checksum.
     */
    @Override
    protected String calculateChecksum(InputStream is) throws IOFailure {
        return MD5.generateMD5(is);
    }
    
    /**
     * Method for correcting a bad entry from the archive.
     * The current incorrect entry is put into the wrongEntryFile. 
     * Then it calculates the checksum and corrects the entry for the file, and 
     * then the checksum file is recreated from the archive in the memory.
     * 
     * @param filename The name of the file whose record should be removed.
     * @param correctFile The file that should replace the current entry
     * @throws ArgumentNotValid If one of the arguments are not valid.
     * @throws IOFailure If the entry cannot be corrected. Either the bad entry
     * cannot be stored, or the new checksum file cannot be created.
     * @throws IllegalState If no such entry exists to be corrected, or if the 
     * entry has a different checksum than the incorrectChecksum.
     */
    @Override
    public void correct(String filename, File correctFile) 
            throws IOFailure, ArgumentNotValid, IllegalState {
        ArgumentNotValid.checkNotNullOrEmpty(filename, "String filename");
        ArgumentNotValid.checkNotNull(correctFile, "File correctFile");
        
        // If no file entry exists, then IllegalState
        if(!checksumArchive.containsKey(filename)) {
            String errMsg = "No file entry for file '" + filename + "'.";
            log.error(errMsg);
            throw new IllegalState(errMsg);
        }
        
        // retrieve the checksum
        String currentChecksum = checksumArchive.get(filename);
        
        // Make entry in the wrongEntryFile.
        appendWrongRecordToWrongEntryFile(ChecksumJob.makeLine(filename, 
                currentChecksum));
        
        // Calculate the new checksum and correct the entry.
        String newChecksum = calculateChecksum(correctFile);
        if(newChecksum.equals(currentChecksum)) {
            // TODO finish?
            log.warn("The correct and the incorrect checksums are the same!");
            return;
        }
        
        // Correct the bad entry, by changing the value to the newChecksum.'
        // Since the checksumArchive is a hashmap, then putting an existing 
        // entry with a new value will override the existing one.
        checksumArchive.put(filename, newChecksum);
        
        // Recreate the archive file.
        recreateArchiveFile();
    }
    
    /**
     * Method for retrieving the archive as a temporary file containing the 
     * checksum entries. Each line should contain one checksum entry in the 
     * format produced by the ChecksumJob.
     * 
     * @return A temporary checksum file, which is a copy of the archive file.
     * @throws IOFailure If problems occurs during the creation of the file.
     */
    public File getArchiveAsFile() throws IOFailure {
        try {
            // create new temporary file of the archive.
            File tempFile = File.createTempFile("tmp", "tmp", 
                    FileUtils.getTempDir());
            synchronized(checksumFile) {
                FileUtils.copyFile(checksumFile, tempFile);
            }
            
            return tempFile;
        } catch (IOException e) {
            String msg = "Cannot create the output file containing all the "
                + "entries of this archive.";
            log.warn(msg);
            throw new IOFailure(msg);
        }
    }

    /**
     * Method for retrieving the names of all the files within the archive as
     * a temporary file.
     * 
     * @return A temporary file containing the list of all the filenames.
     * This file has one filename per line.
     * @throws IOFailure If problems occurs during the creation of the file.
     */
    public File getAllFilenames() throws IOFailure {
        try {
            File tempFile = File.createTempFile("tmp", "tmp", 
                    FileUtils.getTempDir());
            FileWriter fw = new FileWriter(tempFile);

            try {
                // put the content into the file.
                for(String filename : checksumArchive.keySet()) {
                    fw.append(filename);
                    fw.append("\n");
                }

            } finally {
                // flush and close the file, before returning it.
                fw.flush();
                fw.close();
            }
            return tempFile;
        } catch (IOException e) {
            String msg = "Cannot create the output file containing the "
                + "filenames of all the entries of this archive.";
            log.warn(msg);
            throw new IOFailure(msg);
        }
    }
    
    /**
     * The method for cleaning up when done.
     * It sets the checksum file and the instance to null. 
     */
    public void cleanup() {
        checksumFile = null;
        instance = null;
    }
}
