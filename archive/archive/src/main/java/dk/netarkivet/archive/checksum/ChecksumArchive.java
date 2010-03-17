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
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301 
 *  USA
 */
package dk.netarkivet.archive.checksum;

import java.io.File;
import java.io.InputStream;

import dk.netarkivet.common.distribute.RemoteFile;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.IllegalState;

/**
 * This abstract class is the interface for the checksum archives, which can be
 * one of the following: <br>
 * - <b>FileChecksumArchive</b> where the archive is placed in a single file. 
 * <br>
 * <b><i>TODO</i></b><br>
 * - <b>DatabaseChecksumArchive</b> where the archive is placed in a 
 * database. <br>
 * 
 * @see dk.netarkivet.archive.checksum.FileChecksumArchive
 */
public abstract class ChecksumArchive {
    
    /**
     * Constructor.
     */
    protected ChecksumArchive() { }
    
    /**
     * Method for checking whether there is enough space left on the hard drive.
     * 
     * @return Whether there is enough space left on the hard drive.
     */
    public abstract boolean hasEnoughSpace();
    
    /**
     * Method for removing a bad entry from the archive.
     * This finds the record and removes it if it has the incorrect checksum.
     * The incorrect record is not deleted, but instead put into a backup file
     * for all the incorrect records.
     * 
     * @param filename The name of the file whose record should be removed.
     * @param correctFile The correct remote file to replace the bad one in 
     * the archive.
     * @return A file containing the removed data.
     * @throws ArgumentNotValid If one of the arguments are not valid.
     * @throws IOFailure If the entry cannot be corrected.
     * @throws IllegalState If no such entry exists to be corrected, or if the 
     * entry has a different checksum than expected.
     */
    public abstract File correct(String filename, File correctFile) 
            throws IOFailure, ArgumentNotValid, IllegalState;
    
    /**
     * Method for retrieving the checksum of a specific entry in the archive.
     * 
     * @param filename The name of the file entry in the archive for whom the 
     * checksum should be retrieved.
     * @return The checksum of a record, or null if it was not found.
     */
    public abstract String getChecksum(String filename);
    
    /**
     * Method for checking whether an entry exists within the archive.
     * 
     * @param filename The name of the file whose entry in the archive should 
     * be determined.
     * @return Whether an entry with the filename was found.
     */
    public abstract boolean hasEntry(String filename);
    
    /**
     * Method for uploading a new file to the archive.
     * The checksum of the file needs to be calculated before it is placed
     * in the archive with the given filename.
     * 
     * @param arcfile The remote file to be uploaded.
     * @param filename The name of the file.
     */
    public abstract void upload(RemoteFile arcfile, String filename);
    
    /**
     * Method for calculating the checksum of a specific file.
     * 
     * @param f The file to calculate the checksum from.
     * @return The checksum of the file.
     */
    protected abstract String calculateChecksum(File f);
    
    /**
     * Method for calculating the checksum when the file is received in the 
     * form of an inputstream.
     * @param is The input stream to calculate the checksum from.
     * @return The checksum of the inputstream.
     */
    protected abstract String calculateChecksum(InputStream is);
    
    /**
     * Method for retrieving the archive as a temporary file containing the 
     * checksum entries.
     * 
     * @return A temporary checksum file.
     */
    public abstract File getArchiveAsFile();
    
    /**
     * Method for retrieving the names of all the files within the archive as
     * a temporary file.
     * 
     * @return A temporary file containing the list of all the filenames.
     * This file has one filename per line.
     */
    public abstract File getAllFilenames();
    
    /**
     * Method for cleaning up when closing down.
     */
    public abstract void cleanup();
    
}
