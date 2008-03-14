/* File:     $Id$
* Revision:  $Revision$
* Author:    $Author$
* Date:      $Date$
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
package dk.netarkivet.common.utils.arc;

import java.io.File;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.utils.StringUtils;

/**
 *
 * Interface defining a batch job to run on a set of files.
 * The job is initialized by calling initialize(), executed on
 * a file by calling processFile() and any cleanup is
 * handled by finish().
 */
public abstract class FileBatchJob implements Serializable {
    
    /** Regular expression for the files to process with this job.
     * By default, all files are processed.  This pattern must match the
     * entire filename, but not the path (e.g. .*foo.* for any file with
     * foo in it).
     */
    private Pattern filesToProcess = Pattern.compile(".*");

    /** The total number of files processed (including any that 
     * generated errors).
     */
    protected int noOfFilesProcessed = 0;
    
    /** A Set of files which generated errors. */
    protected Set<File> filesFailed = new HashSet<File>();

    /**
     * Initialize the job before runnning.
     * This is called before the processFile() calls
     * @param os the OutputStream to which output should be written
     */
    public abstract void initialize(OutputStream os);

    /**
     * Process one file stored in the bit archive.
     *
     * @param file the file to be processed.
     * @param os the OutputStream to which output should be written
     * @return true if the file was successfully processed, false otherwise
     */
    public abstract boolean processFile(File file, OutputStream os);

    /**
     * Finish up the job.
     * This is called after the last process() call.
     * @param os the OutputStream to which output should be written
     */
    public abstract void finish(OutputStream os);


    /** Mark the job to process only the specified files.  This will
     * override any previous setting of which files to process.
     *
     * @param specifiedFilenames A list of filenamess to process (without
     * paths). If null, all files will be processed.
     */
    public void processOnlyFilesNamed(List<String> specifiedFilenames) {
        if (specifiedFilenames != null) {
            List<String> quoted = new ArrayList<String>();
            for (String name : specifiedFilenames) {
                quoted.add(Pattern.quote(name));
            }
            processOnlyFilesMatching(quoted);
        } else {
            processOnlyFilesMatching(".*");
        }
    }


    /** Helper method for only processing one file.  This will
     * override any previous setting of which files to process.
     *
     * @param specifiedFilename The name of the single file that should
     * be processed.  Should not include any path information.
     */
    public void processOnlyFileNamed(String specifiedFilename) {
        ArgumentNotValid.checkNotNullOrEmpty(specifiedFilename, 
            "specificedFilename");
        processOnlyFilesMatching(Pattern.quote(specifiedFilename));
    }

    /** Set this job to match only a certain set of patterns.  This will
     * override any previous setting of which files to process.
     *
     * @param specifiedPatterns The patterns of file names that this job
     * will operate on. These should not include any path information, but
     * should match the entire filename (e.g. .*foo.* for any file with foo in
     * the name).
     */
    public void processOnlyFilesMatching(List<String> specifiedPatterns) {
        ArgumentNotValid.checkNotNull(specifiedPatterns,
         "specifiedPatterns");
        processOnlyFilesMatching("("
                        + StringUtils.conjoin("|", specifiedPatterns) + ")");
    }

    /** Set this job to match only a certain pattern.  This will
     * override any previous setting of which files to process.
     *
     * @param specifiedPattern Regular expression of file names that this job
     * will operate on. This should not include any path information, but should
     * match the entire filename (e.g. .*foo.* for any file with foo in the
     * name).
     */
    public void processOnlyFilesMatching(String specifiedPattern) {
        ArgumentNotValid.checkNotNullOrEmpty(specifiedPattern, 
                "specificedPattern");
        filesToProcess = Pattern.compile(specifiedPattern);
    }

    /** Get the pattern for files that should be processed.
     *
     * @return A pattern for files to process.
     */
    public Pattern getFilenamePattern() {
        return filesToProcess;
    }

    /**
     * Return the number of ARC-files processed in this job
     * (at this bit archive application).
     *
     * @return the number of ARC-files processed in this job
     */
    public int getNoOfFilesProcessed() {
        return noOfFilesProcessed;
    }

    /**
     * Return the list of names of ARC-files where processing
     * (of one or more ARC records) failed or an empty list if none failed.
     *
     * @return the possibly empty list of names of ARC-files where processing
     * (of one or more ARC records) failed
     */
    public Collection<File> getFilesFailed() {
        return filesFailed;
    }
}
