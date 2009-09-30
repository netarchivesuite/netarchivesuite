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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.archive.ArchiveSettings;
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
    
    /**
     * The logger used by this class.
     */
    private static final Log log = LogFactory.getLog(FileChecksumArchive.class);
    
    /**
     * The current instance of this class.
     */
    private static FileChecksumArchive instance;
    
    /**
     * The name of the file containing the checksum.
     */
    private String checksumFileName;
    
    /**
     * The file to store the checksum.
     * Each line should contain the following:
     * arc-filename + ## + checksum.
     */
    private File checksumFile;
    
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
        
        // TODO create new setting: CHECKSUM_MIN_SPACE_LEFT ?
        minSpaceLeft = Settings.getLong(
                ArchiveSettings.BITARCHIVE_MIN_SPACE_LEFT);
        // make sure, that minSpaceLeft is non-negative.
        if(minSpaceLeft < 0) {
            String msg = "Wrong setting of minSpaceRequired read from "
                + "Settings: " + minSpaceLeft;
            log.warn(msg);
            throw new ArgumentNotValid(msg);
        }
        
        // extract the filename
        checksumFileName = Settings.get(ArchiveSettings.CHECKSUM_FILENAME); 
        // initialise file.
        checksumFile = new File(checksumFileName);
        
        // make sure, that the file exists.
        if(!checksumFile.exists()) {
            try {
                checksumFile.createNewFile();
            } catch (IOException e) {
                String msg = "Cannot create checksum file!";
                log.error(msg);
                throw new IOFailure(msg, e);
            }
        }
    }
    
    /**
     * Method for retrieving the name of the checksum file.
     * 
     * @return The checksum file name.
     */
    public String getFilename() {
        return checksumFileName;
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
     * Method for retrieving the record for a specific file.
     * 
     * @param filename The name of the file for which the record should 
     *     be found.
     * @return The record of the file, or null if it was not within the archive.
     * The record is in the format: filename##checksum.
     * @throws IOFailure If problems with reading the checksum file.
     */
    private String getRecord(String filename) throws IOFailure {
        try {
            // initialise the reader for reading the archive.
            BufferedReader br = new BufferedReader(
                    new FileReader(checksumFile));
            
            // initialise the record.
            String record = br.readLine();
            
            // Go through all records and check whether they have the correct 
            // filename. Until the end of file is reached (record == null). 
            while(record != null) {
                // Retrieve the name of the record
                String name = extractFilename(record);
                
                // If the record name is identical to the filename, 
                // then return the record.
                if(name.equals(filename)) {
                    return record;
                }
                
                // Else retrieve the next record.
                record = br.readLine();
            }
            
            // if file was not found, then return null.
            return null;
        } catch (IOException e) {
            // Handle exceptions involving reading the records.
            String msg = "Cannot retrieve a record for file: '" + filename 
                + "'.";
            log.warn(msg, e);
            throw new IOFailure(msg, e);
        }
    }
    
    /**
     * Checks whether a file is within the archive.
     * 
     * @param filename The name of the file.
     * @return Whether the file is within this archive.
     */
    private boolean hasFile(String filename) {
        // Retrieve the record for the file.
        String line = getRecord(filename);
        
        // return whether a record is found.
        return (line != null);
    }

    /**
     * The method for uploading an arcFile to the archive.
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
        
        // check if file already exist in archive.
        if(hasFile(filename)) {
            log.warn("Cannot upload arcfile '" + filename + "', "
                    + "it is already archived");
            // It is a success that it already is within the archive, thus do
            // not throw an exception. 
            return;
        }
        
        //append the file.
        try {
            // get file writer, and append the writing. 
            boolean appendToFile = true;
            FileWriter fwrite = new FileWriter(checksumFile, appendToFile);
            
            // initialise the record.
            StringBuilder record = new StringBuilder();
            
            // Create the record in the format: filename##checksum
            record.append(filename);
            record.append(CHECKSUM_SEPARATOR);
            record.append(calculateChecksum(arcfile.getInputStream()));
            record.append("\n");            
            
            // Write the record to the archive file.
            fwrite.append(record.toString());
            
            // close fileWriter.
            fwrite.flush();
            fwrite.close();
        } catch (IOException e) {
            // Handle exceptions involving writing the record.
            String msg = "Cannot write the record for file: '" + filename 
                + "'.";
            log.warn(msg, e);
            throw new IOFailure(msg, e);
        }
    }
    
    /**
     * Method for retrieving the checksum of a record, based on the filename.
     * 
     * @param filename The name of the file to have recorded in the archive.
     * @return The checksum of a record.
     * @throws IOFailure If the file is not within the archive.
     * @throws ArgumentNotValid If the filename is not valid (null or empty).
     */
    public String getChecksum(String filename) throws IOFailure, 
            ArgumentNotValid {
        // validate the argument
        ArgumentNotValid.checkNotNullOrEmpty(filename, "String filename");
        
        // extract the record for the file
        String record = getRecord(filename);
        
        // Check whether the record is found.
        if(record == null) {
            String msg = "The file '" + filename + "' cannot be found in the "
                    + "archive. It has not yet been uploaded.";
            log.warn(msg);
            throw new IOFailure(msg);
        }
        
        // Return the checksum of the record.
        return extractChecksum(record);
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
     * Method for removing a record. This is used to correct the record.
     * This finds the record and removes it if the checksum is not correct.
     * 
     * @param filename The name of the file whose entry in the archive should 
     * be removed.
     * @param checksum The valid checksum for the entry in the archive.
     * @return Whether the record was removed. It returns false when either 
     * the entry cannot be found in the archive, or that it already has the 
     * correct checksum.
     * @throws IOFailure If the archive file cannot be recreated without 
     * the given record.
     * @throws ArgumentNotValid If the filename is either null or empty, or if
     * the checksum is null.
     */
    @Override
    public boolean removeRecord(String filename, String checksum) 
            throws IOFailure, ArgumentNotValid {
        // validate argument
        ArgumentNotValid.checkNotNullOrEmpty(filename, "String filename");
        ArgumentNotValid.checkNotNull(checksum, "String checksum");
        
        try {
            // get the content of the file.
            String content = FileUtils.readFile(checksumFile); 
        
            // find the start of the filename (added the checksum to make sure, 
            // that it is the entire filename)
            int start = content.indexOf(filename + CHECKSUM_SEPARATOR);
            if(start < 0) {
                log.warn("Cannot find the file '" + filename 
                        + "' in the archive.");
                return false;
            }

            // Get the index of the end-line after the arc-filename.
            // If no end-line is found, then it must be the last record.
            int end = content.indexOf("\n", start);
            if(end < 0) {
                end = content.length();
            } 
            
            // Makes sure, that this record does not have the correct checksum.
            if(content.substring(start, end).contains(checksum)) {
                log.debug("The current file '" + filename 
                        + "' has the valid checksum '" + checksum + "'.");
                return false;
            }
        
            // Initialise writing of the content without the record. 
            FileWriter fw = new FileWriter(checksumFile);
            
            // Write everthing before and after the record to the checksumfile.
            // write entries the prior to the entry, if they exists (start > 0).
            if(start > 0) {
                fw.write(content.substring(0, start-1));
            } 
            
            // write the entries afterward the current entry, if they exists.
            if(end < content.length()) {
                fw.write(content.substring(end, content.length()));
            }
            
            // finish writing and close the file.
            fw.flush();
            fw.close();
        
            // The record has now successfully been removed.
            return true;
        } catch (IOException e) {
            // handle exceptions when reading/writing the checksum file.
            String msg = "Cannot remove file '" + filename + "'";
            log.warn(msg, e);
            throw new IOFailure(msg, e);
        }
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
            File tempFile = File.createTempFile("tmp", "tmp", 
                    FileUtils.getTempDir());
        
            FileUtils.copyFile(checksumFile, tempFile);
            
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
            
            // Retrieve the list of content
            List<String> lines = FileUtils.readListFromFile(checksumFile);
        
            // put the content into the file.
            for(String line : lines) {
                fw.append(extractFilename(line));
                fw.append("\n");
            }

            // flush and close the file, before returning it.
            fw.flush();
            fw.close();
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
