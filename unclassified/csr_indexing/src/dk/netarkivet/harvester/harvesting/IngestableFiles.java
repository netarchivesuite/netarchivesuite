/* $Id$
* $Revision$
* $Date$
* $Author$
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
package dk.netarkivet.harvester.harvesting;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.archive.io.arc.ARCWriter;

import dk.netarkivet.common.Constants;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.PermissionDenied;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.arc.ARCUtils;


/**
 * Encapsulation of files to be ingested into the archive.
 * These files are presently placed subdirectories under the crawldir.
 *
 */
public class IngestableFiles {
    private final Log log = LogFactory.getLog(getClass());

    /** Subdir with final metadata file in it. */
    private static final String METADATA_SUB_DIR = "metadata";

    /** Subdir with temporary metadata file in it. */
    private static final String TMP_SUB_DIR = "tmp-meta";

    /** jobId for present harvestjob. */
    private long jobId;

    /** crawlDir for present harvestjob. */
    private File crawlDir;

    /** Writer to this jobs metadatafile.
     * This is closed when the metadata is marked as ready.
     */
    private ARCWriter writer = null;

    /** Whether we've had an error in metadata generation. */
    private boolean error = false;

    /**
     * Constructor for this class.
     * @param crawlDir directory, where all files for the harvestjob
     *                 (including metadataFile) are
     * @param jobID ID for the given harvestjob
     * @throws ArgumentNotValid if null-arguments are given;
     *                          if jobID < 1;
     *                          if crawlDir does not exist
     */
    public IngestableFiles(File crawlDir, long jobID) {
        ArgumentNotValid.checkNotNull(crawlDir, "crawlDir");
        ArgumentNotValid.checkPositive(jobID, "jobID");
        if (!crawlDir.exists()) {
            throw new ArgumentNotValid("The given crawlDir ("
                    + crawlDir.getAbsolutePath()
                    + ") does not exist");
        }
        this.jobId = jobID;
        this.crawlDir = crawlDir;
        // Create subdir 'metadata' if not already exists.
        FileUtils.createDir(getMetadataDir());
        // Create/scratch subdir 'tmp-meta'
        FileUtils.removeRecursively(getTmpMetadataDir());
        FileUtils.createDir(getTmpMetadataDir());
    }

    /**
     * Check, if the metadatafile already exists.
     * If this is true, metadata has been successfully generated.
     * If false, either metadata has not finished being generated, or there
     * was an error generating them.
     * @return true, if it does exist; false otherwise.
     */
    public boolean isMetadataReady() {
        return getMetadataFile().isFile();
    }

    /** Return true if the metadata generation process is known to have failed.
     *
     * @return True if metadata generation is finished without success,
     * false if generation is still ongoing or has been successfully done.
     */
    public boolean isMetadataFailed() {
        return error;
    }

    /**
     * Marks generated metadata as final.
     * Closes the arcwriter and moves the temporary metadata file to its
     * final position, if successfull.
     * @param success True if metadata was successfully generated, false
     * otherwise.
     * @throws PermissionDenied If the metadata has already been marked as
     * ready, or if no metadata file exists upon success.
     * @throws IOFailure if there is an error marking the metadata as ready.
     */
    public void setMetadataGenerationSucceeded(boolean success) {
        if (isMetadataReady()) {
            throw new PermissionDenied(
                    "Metadata file " + getMetadataFile().getAbsolutePath()
                    + " already exists");
        }
        try {
            writer.close();
            writer = null;
        } catch (IOException e) {
            String message = "Error closing metadata arc writer";
            throw new IOFailure(message, e);
        }
        if (success) {
            if (!getTmpMetadataFile().exists()) {
                String message = "No metadata was generated despite claims"
                        + " that metadata generation was successfull.";
                throw new PermissionDenied(message);
            }
            getTmpMetadataFile().renameTo(getMetadataFile());
            FileUtils.removeRecursively(getTmpMetadataDir());
        } else {
            error = true;
        }
    }

    /**
     * Get a ARCWriter for the temporary metadata arc-file.
     * Successive calls to this method on the same object will return the
     * same writer.  Once the metadata have been finalized, calling
     * this method will fail.
     * @return a ARCWriter for the temporary metadata arc-file.
     * @throws PermissionDenied if metadata generation is already
     * finished.
     */
    public ARCWriter getMetadataArcWriter() {
        if (isMetadataReady()) {
            throw new PermissionDenied(
                    "Metadata file " + getMetadataFile().getAbsolutePath()
                    + " already exists");
        }
        if (isMetadataFailed()) {
            throw new PermissionDenied("Metadata generation of file "
                    + getMetadataFile().getAbsolutePath()
                    + " has already failed.");
        }
        if (writer == null) {
            writer = ARCUtils.createARCWriter(getTmpMetadataFile());
        }
        return writer;
    }

    /**
     * Gets the files containing the metadata.
     * @return the files in the metadata dir
     * @throws PermissionDenied if the metadata file is not ready, either
     * because generation is still going on or there was an error generating
     * the metadata.
     */
    public List<File> getMetadataArcFiles() {
        // Our one known metadata file must exist.
        if (!isMetadataReady()) {
            throw new PermissionDenied(
                    "Metadata file " + getMetadataFile().getAbsolutePath()
                    + " does not exist");
        }
        return Arrays.asList(new File[]{getMetadataFile()});
    }

    /**
     * Constructs the metadata subdir from the crawlDir.
     * @return The metadata subdir as a File
     */
    private File getMetadataDir() {
        return new File(crawlDir, METADATA_SUB_DIR);
    }

    /**
     * Constructs the single metadata arc file from the crawlDir and the jobID.
     * @return metadata arc file as a File
     */
    private File getMetadataFile(){
        return
            new File(getMetadataDir(),
                    HarvestDocumentation.
                    getMetadataARCFileName(Long.toString(jobId)));
    }

    /**
     * Constructs the TEMPORARY metadata subdir from the crawlDir.
     * @return The tmp-metadata subdir as a File
     */
    private File getTmpMetadataDir() {
        return new File(crawlDir, TMP_SUB_DIR);
    }

    /**
     * Constructs the TEMPORARY metadata arc file from the crawlDir and
     * the jobID.
     * @return tmp-metadata arc file as a File
     */
    private File getTmpMetadataFile(){
        return
            new File(getTmpMetadataDir(),
                    HarvestDocumentation.
                    getMetadataARCFileName(Long.toString(jobId)));
    }



    /** Get a list of all ARC files that should get ingested.  Any open files
     * should be closed with closeOpenFiles first.
     *
     * @return The ARC files that are ready to get ingested.
     */
    public List<File> getArcFiles() {
        File arcsdir = new File(crawlDir, Constants.ARCDIRECTORY_NAME);
        if (!arcsdir.isDirectory()) {
            throw new IOFailure(arcsdir.getPath() + " is not a directory");
        }
        return Arrays.asList(arcsdir.listFiles(FileUtils.ARCS_FILTER));
    }

    /** Close any ".open" files left by a crashed Heritrix.  ARC files ending
     * in .open indicate that Heritrix is still writing to them. If Heritrix
     * has died, we can just rename them before we upload.
     * This must not be done while harvesting is still in progress.
     *
     * @param waitSeconds How many seconds to wait before closing files.  This
     * may be done in order to allow Heritrix to finish writing before we close
     * the files.
     */
    public void closeOpenFiles(int waitSeconds) {
        // wait for Heritrix threads to create and close last arc files
        try {
            Thread.sleep(waitSeconds * 1000L);
        } catch (InterruptedException e) {
            log.debug("Thread woken prematurely from sleep.", e);
        }

        File arcsdir = new File(crawlDir, Constants.ARCDIRECTORY_NAME);
        File[] files = arcsdir.listFiles(FileUtils.OPEN_ARCS_FILTER);
        if (files != null) {
            for (File file : files) {
                final String fname = file.getAbsolutePath();
                //Note: Due to regexp we know filename is at least 5 characters
                File tofile = new File(fname.substring(0, fname.length() - 5));
                if (!file.renameTo(tofile)) {
                    log.warn("Failed to rename '" + file.getAbsolutePath()
                             + "' to '"
                             + tofile.getAbsolutePath() + "'");
                }
            }
        }
    }
}
