/* File:        $Id$
 * Date:        $Date$
 * Revision:    $Revision$
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
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.KeyValuePair;
import dk.netarkivet.common.utils.MD5;
import dk.netarkivet.common.utils.batch.FileBatchJob;


/**
 * Class responsible for checksumming files locally in a
 * bit archive application.
 *
 */
public class ChecksumJob extends FileBatchJob {

    protected transient Log log = LogFactory.getLog(getClass().getName());

    /**
     * Initialization of a ChecksumJob: a new structure for storing files
     * failed is created.
     *
     * @see FileBatchJob#initialize(OutputStream)
     */
    public void initialize(OutputStream os) {
    }

    /**
     * Generates MD5 checksum for file identified by 'file' and writes
     * the checksum to the given OutputStream.
     * Errors during checksumming are logged and files on which checksumming
     * fails are stored in filesFailed.
     *
     * @param file The file to process.
     * @param os The outputStream to write the result to
     * @return false, if errors occurred while processing the file
     * @see FileBatchJob#processFile(File, OutputStream)
     */
    public boolean processFile(File file, OutputStream os) {
        ArgumentNotValid.checkNotNull(file, "file");
        try {
            os.write((file.getName()
                    + dk.netarkivet.archive.arcrepository.bitpreservation
                        .Constants.STRING_FILENAME_SEPARATOR
                    + MD5.generateMD5onFile(file) + "\n").getBytes());
        } catch (IOException e) {
            log.warn("Checksumming of file " + file.getName()
                    + " failed: ", e);
            return false;
        }
        return true;
    }

    /**
     * Finishing the job requires nothing particular.
     *
     * @see FileBatchJob#finish(OutputStream)
     */
    public void finish(OutputStream os) {
    }

    /** Create a line in checksum job format from a filename and a checksum.
     *
     * @param filename A filename (no path)
     * @param checksum An MD5 checksum
     * @return A string of the correct format for a checksum job output.
     */
    public static String makeLine(String filename, String checksum) {
        ArgumentNotValid.checkNotNullOrEmpty(filename, "filename");
        ArgumentNotValid.checkNotNullOrEmpty(checksum, "checksum");
        return filename + Constants.STRING_FILENAME_SEPARATOR + checksum;
    }

    /** Parse a line of output into a key-value pair.
     *
     * @param line The line to parse, of the form
     *  <filename>##<checksum>
     * @return The filename->checksum mapping.
     * @throws ArgumentNotValid if the line is not on the correct form.
     */
    public static KeyValuePair<String, String> parseLine(String line) {
        ArgumentNotValid.checkNotNull(line, "checksum line");
        String[] parts = line.split(Constants.STRING_FILENAME_SEPARATOR);
        if (parts.length != 2) {
            throw new ArgumentNotValid("String '" + line + "' is not on"
                    + " checksum output form");
        }
        return new KeyValuePair<String, String>(parts[0], parts[1]);
    }

    /**
     * Write a human-readily description of this ChecksumJob object.
     * Writes out the name of the ChecksumJob, the number of files processed,
     * and the number of files that failed during processing.
     * @return a human-readily description of this ChecksumJob object
     */
    public String toString() {
        int noOfFailedFiles;
        if (filesFailed == null) {
            noOfFailedFiles = 0;
        } else {
            noOfFailedFiles = filesFailed.size();
        }
        return ("Checksum job " + getClass().getName()
                + ": [Files Processed = " + noOfFilesProcessed
                + "; Files  failed = " + noOfFailedFiles + "]");
    }

    /**
     * Invoke default method for deserializing object, and reinitialise the
     * logger.
     * @param s the InputStream
     */
    private void readObject(ObjectInputStream s) {
        try {
            s.defaultReadObject();
        } catch (Exception e) {
            throw new IOFailure("Unexpected error during deserialization", e);
        }
        log = LogFactory.getLog(getClass().getName());
    }

    /**
     * Invoke default method for serializing object.
     * @param s the OutputStream
     */
    private void writeObject(ObjectOutputStream s) {
        try {
            s.defaultWriteObject();
        } catch (Exception e) {
            throw new IOFailure("Unexpected error during serialization", e);
        }
    }

}
