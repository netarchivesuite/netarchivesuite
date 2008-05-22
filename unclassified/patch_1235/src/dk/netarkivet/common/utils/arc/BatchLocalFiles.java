/* File:    $Id$
 * Version: $Revision$
 * Date:    $Date$
 * Author:  $Author$
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
import java.util.HashSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.common.exceptions.ArgumentNotValid;

/**
 * Class for running FileBatchJobs on a set of local files. The constructor
 * takes an array of files to be processed and the run() method takes a
 * FileBatchJob and applies it to each file in turn.
 */
public class BatchLocalFiles {
    /** The list of files to run batch jobs on: */
    private File[] files;
    private Log log = LogFactory.getLog(BatchLocalFiles.class);

    /**
     * Given an array of files, constructs a BatchLocalFiles instance
     * to be used in running a batch job over those files
     *
     * @param in_files - the files that should be used for batching.
     * @throws ArgumentNotValid if in_files is null or contains a null entry
     */
    public BatchLocalFiles(File[] in_files) throws ArgumentNotValid {
        ArgumentNotValid.checkNotNull(in_files, "in_files");
        for (int i = 0; i < in_files.length; i++) {
            if (in_files[i] == null) {
                throw new ArgumentNotValid("Null element at index " + i +
                        " in file list for batch.");
            }
        }
        this.files = in_files;
    }

    /**
     * Run the given job on the files associated with this object.
     *
     * @param job - the job to be executed
     * @param os - the OutputStream to which output data is written
     */
    public void run(FileBatchJob job, OutputStream os) {
        ArgumentNotValid.checkNotNull(job, "FileBatchJob job");
        ArgumentNotValid.checkNotNull(os, "OutputStream os");
        //Initialize the job:
        job.noOfFilesProcessed = 0;
        job.filesFailed = new HashSet<File>();
        try {
            job.initialize(os);
            //Process each file:
            for (File file : files) {
                if (job.getFilenamePattern().matcher(file.getName()).matches()) {
                    processFile(job, file, os);
                }
            }
        } catch (Exception e) {
            log.warn("Exception while initializing job " + job, e);
        } finally {
            // Finally, allow the job to finish: */
            try {
                job.finish(os);
            } catch (Exception e) {
                log.warn("Exception while finishing job " + job, e);
            }
        }
    }

    /** Process a single file.
     *
     * @param job The job that does the processing
     * @param file The file to process
     * @param os Where to put the output.
     */
    private void processFile(FileBatchJob job, final File file, OutputStream os) {
        boolean success = false;
        try {
            success = job.processFile(file, os);
        } catch (Exception e) {
            log.warn("Exception while processing file " + file
                     + " with job " + job, e);
        }
        job.noOfFilesProcessed++;
        if (!success) {
            job.filesFailed.add(file);
        }
    }

}
