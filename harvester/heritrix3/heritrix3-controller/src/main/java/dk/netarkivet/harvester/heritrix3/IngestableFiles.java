/*
 * #%L
 * Netarchivesuite - harvester
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
package dk.netarkivet.harvester.heritrix3;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.Constants;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.PermissionDenied;
import dk.netarkivet.common.exceptions.IllegalState;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.harvester.HarvesterSettings;
import dk.netarkivet.harvester.harvesting.metadata.MetadataFileWriter;

/**
 * Encapsulation of files to be ingested into the archive. These files are presently placed subdirectories under the
 * crawldir.
 */
public class IngestableFiles {

    private static final Logger log = LoggerFactory.getLogger(IngestableFiles.class);

    /** Subdir with final metadata file in it. */
    protected static final String METADATA_SUB_DIR = "metadata";

    /** Subdir with temporary metadata file in it. */
    private static final String TMP_SUB_DIR = "tmp-meta";

    /** jobId for present harvestjob. */
    private long jobId;

    /** crawlDir for present harvestjob. */
    private File crawlDir;

    /**
     * Writer to this jobs metadatafile. This is closed when the metadata is marked as ready.
     */
    private MetadataFileWriter writer = null;

    /** Whether we've had an error in metadata generation. */
    private boolean error = false;

    private String harvestnamePrefix;

    public static final String METADATA_FILENAME_FORMAT = Settings.get(HarvesterSettings.METADATA_FILENAME_FORMAT);

    private Long harvestId;

    private File heritrixJobDir;	
    /**
     * Constructor for this class. HeritrixFiles contains information about crawlDir, jobId, and harvestnameprefix for a
     * specific finished harvestjob.
     *
     * @param files An instance of Heritrix3Files
     * @throws ArgumentNotValid if null-arguments are given; if jobID < 1; if crawlDir does not exist
     */
    public IngestableFiles(Heritrix3Files files) {
        ArgumentNotValid.checkNotNull(files, "files");
        ArgumentNotValid.checkNotNull(files.getCrawlDir(), "crawlDir");
        ArgumentNotValid.checkPositive(files.getJobID(), "jobID");
        ArgumentNotValid.checkNotNullOrEmpty(files.getArchiveFilePrefix(), "harvestnamePrefix");
        this.heritrixJobDir = files.getHeritrixJobDir();
        this.crawlDir = files.getCrawlDir();
        if (!crawlDir.exists()) {
            throw new ArgumentNotValid("The given crawlDir (" + crawlDir.getAbsolutePath() + ") does not exist");
        }
        this.jobId = files.getJobID();
        this.harvestnamePrefix = files.getArchiveFilePrefix();
        this.harvestId = files.getHarvestID();
        // Create subdir 'metadata' if not already exists.
        FileUtils.createDir(getMetadataDir());
        // Create/scratch subdir 'tmp-meta'
        if (getTmpMetadataDir().isDirectory()) {
        	FileUtils.removeRecursively(getTmpMetadataDir());
        	log.warn("Removed directory {} with contents", getTmpMetadataDir());
        }
        FileUtils.createDir(getTmpMetadataDir());
    }

    /**
     * Check, if the metadatafile already exists. If this is true, metadata has been successfully generated. If false,
     * either metadata has not finished being generated, or there was an error generating them.
     *
     * @return true, if it does exist; false otherwise.
     */
    public boolean isMetadataReady() {
        return getMetadataFile().isFile();
    }

    /**
     * Return true if the metadata generation process is known to have failed.
     *
     * @return True if metadata generation is finished without success, false if generation is still ongoing or has been
     * successfully done.
     */
    public boolean isMetadataFailed() {
        return error;
    }

    /**
     * Marks generated metadata as final, closes the writer, and moves the temporary metadata file to its final
     * position.
     *
     * @throws PermissionDenied If the metadata has already been marked as ready, or if no metadata file exists upon
     * success.
     * @throws IOFailure if there is an error marking the metadata as ready.
     */
    public void closeMetadataFile() {
        if (isMetadataReady()) {
            throw new PermissionDenied("Metadata file " + getMetadataFile().getAbsolutePath() + " already exists");
        }
        writer.close(); // close writer down
        if (!getTmpMetadataFile().exists()) {
            String message = "No metadata was generated despite claims that metadata generation was successful.";
            throw new PermissionDenied(message);
        }
        getTmpMetadataFile().renameTo(getMetadataFile());
    }
    
    /**
     * Set error state. 
     * @param isError True, if error, otherwise false;
     */
    public void setErrorState(boolean isError) {
        error = isError;
    }
    
    /**
     * Get a MetaDatafileWriter for the temporary metadata file. Successive calls to this method on the same object will
     * return the same writer. Once the metadata have been finalized, calling this method will fail.
     *
     * @return a MetaDatafileWriter for the temporary metadata file.
     * @throws PermissionDenied if metadata generation is already finished.
     */
    public MetadataFileWriter getMetadataWriter() {
        if (isMetadataReady()) {
            throw new PermissionDenied("Metadata file " + getMetadataFile().getAbsolutePath() + " already exists");
        }
        if (isMetadataFailed()) {
            throw new PermissionDenied("Metadata generation of file " + getMetadataFile().getAbsolutePath()
                    + " has already failed.");
        }
        if (writer == null) {
            writer = MetadataFileWriter.createWriter(getTmpMetadataFile());
        }
        return writer;
    }

    /**
     * Gets the files containing the metadata.
     *
     * @return the files in the metadata dir
     * @throws IllegalState if the metadata file is not ready, either because generation is still going on or there
     * was an error generating the metadata.
     */
    public List<File> getMetadataArcFiles() {
        // Our one known metadata file must exist.
        if (!isMetadataReady()) {
            throw new IllegalState("Metadata file " + getMetadataFile().getAbsolutePath() + " does not exist");
        }
        return Arrays.asList(new File[] {getMetadataFile()});
    }

    /**
     * Constructs the metadata subdir from the crawlDir.
     *
     * @return The metadata subdir as a File
     */
    private File getMetadataDir() {
        return new File(crawlDir, METADATA_SUB_DIR);
    }

    /**
     * Constructs the single metadata arc file from the crawlDir and the jobID.
     * 
     * @return metadata arc file as a File
     */
    protected File getMetadataFile() {
        return new File(getMetadataDir(), MetadataFileWriter.getMetadataArchiveFileName(Long.toString(jobId), harvestId));
    }

    /**
     * Constructs the TEMPORARY metadata subdir from the crawlDir.
     *
     * @return The tmp-metadata subdir as a File
     */
    public File getTmpMetadataDir() {
        return new File(crawlDir, TMP_SUB_DIR);
    }

    /**
     * Constructs the TEMPORARY metadata arc file from the crawlDir and the jobID.
     *
     * @return tmp-metadata arc file as a File
     */
    private File getTmpMetadataFile() {
        return new File(getTmpMetadataDir(), MetadataFileWriter.getMetadataArchiveFileName(Long.toString(jobId), harvestId));
    }

    /**
     * Get a list of all ARC files that should get ingested. Any open files should be closed with closeOpenFiles first.
     *
     * @return The ARC files that are ready to get ingested.
     */
    public List<File> getArcFiles() {
        File arcsdir = getArcsDir();
        if (arcsdir.exists()) {
            if (!arcsdir.isDirectory()) {
                throw new IOFailure(arcsdir.getPath() + " is not a directory");
            }
            return Arrays.asList(arcsdir.listFiles(FileUtils.ARCS_FILTER));
        } else {
            return new LinkedList<File>();
        }
    }

    /**
     * @return the arcs dir in the our crawl directory.
     */
    public File getArcsDir() {
        return new File(heritrixJobDir, "latest/" + Constants.ARCDIRECTORY_NAME);
    }

    /**
     * @return the warcs dir in the our crawl directory.
     */
    public File getWarcsDir() {
        return new File(heritrixJobDir, "latest/" + Constants.WARCDIRECTORY_NAME);
    }

    /**
     * @return the warcs dir in the our crawl directory.
     */
    public File getReportsDir() {
        return new File(heritrixJobDir, "latest/reports");
    }
        
    /**
     * Get a list of all WARC files that should get ingested. Any open files should be closed with closeOpenFiles first.
     *
     * @return The WARC files that are ready to get ingested.
     */
    public List<File> getWarcFiles() {
        File warcsdir = getWarcsDir();
        if (warcsdir.exists()) {
            if (!warcsdir.isDirectory()) {
                throw new IOFailure(warcsdir.getPath() + " is not a directory");
            }
	    //log

            return Arrays.asList(warcsdir.listFiles(FileUtils.WARCS_FILTER));
        } else {
            return new LinkedList<File>();
        }
    }
    
    public File getHeritrix3JobDir() {
    	return this.heritrixJobDir;
    }

    /**
     * Close any ".open" files left by a crashed Heritrix. ARC and/or WARC files ending in .open indicate that Heritrix
     * is still writing to them. If Heritrix has died, we can just rename them before we upload. This must not be done
     * while harvesting is still in progress.
     *
     * @param waitSeconds How many seconds to wait before closing files. This may be done in order to allow Heritrix to
     * finish writing before we close the files.
     */
    public void closeOpenFiles(int waitSeconds) {
        // wait for Heritrix threads to create and close last arc or warc files
        try {
            Thread.sleep(waitSeconds * 1000L);
        } catch (InterruptedException e) {
            log.debug("Thread woken prematurely from sleep.", e);
        }

        closeOpenFiles(Constants.ARCDIRECTORY_NAME, FileUtils.OPEN_ARCS_FILTER);
        closeOpenFiles(Constants.WARCDIRECTORY_NAME, FileUtils.OPEN_WARCS_FILTER);
    }

    /**
     * Given an archive sub-directory name and a filter to match against this method tries to rename the matched files.
     * Files that can not be renamed generate a log message. The filter should always match files that end with ".open"
     * as a minimum.
     *
     * @param archiveDirName archive directory name, currently "arc" or "warc"
     * @param filter filename filter used to select ".open" files to rename
     */
    protected void closeOpenFiles(String archiveDirName, FilenameFilter filter) {
        File arcsdir = new File(crawlDir, archiveDirName);
        log.debug("Trying to close open archive files in directory {}", arcsdir);
        File[] files = arcsdir.listFiles(filter);
        if (files != null) {
            for (File file : files) {
                final String fname = file.getAbsolutePath();
                // Note: Due to regexp we know filename is at least 5 characters
                File tofile = new File(fname.substring(0, fname.length() - 5));
                if (!file.renameTo(tofile)) {
                    log.warn("Failed to rename '{}' to '{}'", file.getAbsolutePath(), tofile.getAbsolutePath());
                }
            }
        }
    }

    /**
     * Remove any temporary files.
     */
    public void cleanup() {
    	log.debug("Removing the directory '{}'", getTmpMetadataDir());
        FileUtils.removeRecursively(getTmpMetadataDir());
        writer = null;
    }

    /**
     * @return the jobID of the harvest job being processed.
     */
    public long getJobId() {
        return this.jobId;
    }

    /**
     * @return the harvestID of the harvest job being processed.
     */
    public long getHarvestID() {
        return this.harvestId;
    }

    /**
     * @return the harvestnamePrefix of the harvest job being processed.
     */
    public String getHarvestnamePrefix() {
        return this.harvestnamePrefix;
    }

    /**
     * @return the crawlDir of the harvest job being processed.
     */
    public File getCrawlDir() {
        return this.crawlDir;
    }

}
