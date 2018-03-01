/*
 * #%L
 * Netarchivesuite - common
 * %%
 * Copyright (C) 2005 - 2018 The Royal Danish Library, 
 *             the National Library of France and the Austrian National Library.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 2.1 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */

package dk.netarkivet.common.utils.batch;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.OutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;

/**
 * A batch job which returns a list of all files in the bitarchive in which it runs.
 */
@SuppressWarnings({"serial"})
public class FileListJob extends FileBatchJob {

    private static final transient Logger log = LoggerFactory.getLogger(FileListJob.class);

    /** The constructor. */
    public FileListJob() {
        // Keep the batchJobTimeout at default (-1) so it will be overridden
        // by the settings for default batch timeout.
    }

    /**
     * Initializes fields in this class.
     *
     * @param os the OutputStream to which data is to be written
     */
    public void initialize(OutputStream os) {
    }

    /**
     * Invoke default method for deserializing object, and reinitialise the logger.
     *
     * @param s the ObjectInputStream from which the object is read
     */
    private void readObject(ObjectInputStream s) {
        try {
            s.defaultReadObject();
        } catch (Exception e) {
            throw new IOFailure("Unexpected error during deserialization", e);
        }
    }

    /**
     * Writes the name of the arcfile to the OutputStream.
     *
     * @param file an arcfile
     * @param os the OutputStream to which data is to be written
     * @return false If listing of this arcfile fails; otherwise true
     */
    public boolean processFile(File file, OutputStream os) {
        ArgumentNotValid.checkNotNull(file, "file");
        String result = file.getName() + "\n";
        try {
            os.write(result.getBytes());
        } catch (IOException e) {
            log.warn("Listing of file {} failed: ", file.getName(), e);
            return false;
        }
        return true;
    }

    /**
     * Does nothing.
     *
     * @param os the OutputStream to which data is to be written
     */
    public void finish(OutputStream os) {
    }

    /**
     * Return a human-readable representation of a FileListJob.
     *
     * @return a human-readable representation of a FileListJob
     */
    public String toString() {
        int filesFailedCount;
        if (filesFailed == null) {
            filesFailedCount = 0;
        } else {
            filesFailedCount = filesFailed.size();
        }
        return ("\nFileList job:\nFiles Processed = " + noOfFilesProcessed + "\nFiles  failed = " + filesFailedCount);
    }

}
