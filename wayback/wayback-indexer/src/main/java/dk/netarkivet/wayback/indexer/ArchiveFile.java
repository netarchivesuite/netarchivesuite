/*
 * #%L
 * Netarchivesuite - wayback
 * %%
 * Copyright (C) 2005 - 2014 The Royal Danish Library, the Danish State and University Library,
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
package dk.netarkivet.wayback.indexer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.Hashtable;
import java.util.UUID;
import java.util.regex.Pattern;

import javax.persistence.Entity;
import javax.persistence.Id;

import org.apache.commons.math3.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.distribute.arcrepository.ArcRepositoryClientFactory;
import dk.netarkivet.common.distribute.arcrepository.BatchStatus;
import dk.netarkivet.common.distribute.arcrepository.PreservationArcRepositoryClient;
import dk.netarkivet.common.exceptions.IllegalState;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.utils.arc.ARCUtils;
import dk.netarkivet.common.utils.archive.GetMetadataArchiveBatchJob;
import dk.netarkivet.common.utils.batch.FileBatchJob;
import dk.netarkivet.common.utils.warc.WARCUtils;
import dk.netarkivet.wayback.Constants;
import dk.netarkivet.wayback.WaybackSettings;
import dk.netarkivet.wayback.batch.DeduplicateToCDXAdapter;
import dk.netarkivet.wayback.batch.WaybackCDXExtractionARCBatchJob;
import dk.netarkivet.wayback.batch.WaybackCDXExtractionWARCBatchJob;

/**
 * This class represents a file in the arcrepository which may be indexed by the indexer.
 */
@Entity
public class ArchiveFile {
    
    /** Logger for this class. */
    private static final Logger log = LoggerFactory.getLogger(ArchiveFile.class);

    /** The name of the file in the arcrepository. */
    private String filename;

    /** Boolean flag indicating whether the file has been indexed. */
    private boolean isIndexed;

    /** The name of the unsorted cdx index file created from the archive file. */
    private String originalIndexFileName;

    /** The number of times an attempt to index this file has failed. */
    private int indexingFailedAttempts;

    /** The date on which this file was indexed. */
    private Date indexedDate;

    /**
     * Constructor, creates a new instance in the unindexed state.
     */
    public ArchiveFile() {
        isIndexed = false;
        indexedDate = null;
    }

    /**
     * Gets originalIndexFileName.
     *
     * @return the originalIndexFileName
     */
    public String getOriginalIndexFileName() {
        return originalIndexFileName;
    }

    /**
     * Sets originalIndexFileName.
     *
     * @param originalIndexFileName The new original index filename
     */
    public void setOriginalIndexFileName(String originalIndexFileName) {
        this.originalIndexFileName = originalIndexFileName;
    }

    /**
     * Returns indexedDate.
     *
     * @return the date indexed.
     */
    public Date getIndexedDate() {
        return indexedDate;
    }

    /**
     * Sets indexedDate.
     *
     * @param indexedDate The new indexed date.
     */
    public void setIndexedDate(Date indexedDate) {
        this.indexedDate = indexedDate;
    }

    /**
     * The filename is used as a natural key because it is a fundamental property of the arcrepository that filenames
     * are unique.
     *
     * @return the filename.
     */
    @Id
    public String getFilename() {
        return filename;
    }

    /**
     * Sets the filename.
     *
     * @param filename The new filename
     */
    public void setFilename(String filename) {
        this.filename = filename;
    }

    /**
     * Returns true if the file has been indexed.
     *
     * @return whether the file is indexed
     */
    public boolean isIndexed() {
        return isIndexed;
    }

    /**
     * Sets whether the file has been indexed.
     *
     * @param indexed The new value of the isIndexed variable.
     */
    public void setIndexed(boolean indexed) {
        isIndexed = indexed;
    }

    /**
     * Gets the number of failed indexing attempts.
     *
     * @return the number of failed attempts
     */
    public int getIndexingFailedAttempts() {
        return indexingFailedAttempts;
    }

    /**
     * Sets the number of failed indexing attempts.
     *
     * @param indexingFailedAttempts The number of failed indexing attempts
     */
    public void setIndexingFailedAttempts(int indexingFailedAttempts) {
        this.indexingFailedAttempts = indexingFailedAttempts;
    }

    /**
     * Run a batch job to index this file, storing the result locally. If this method runs successfully, the isIndexed
     * flag will be set to true and the originalIndexFileName field will be set to the (arbitrary) name of the file
     * containing the results. The values are persisted to the datastore.
     *
     * @throws IllegalState If the indexing has already been done.
     */
    public void index() throws IllegalState {
        log.info("Indexing {}", this.getFilename());
        if (isIndexed) {
            throw new IllegalState("Attempted to index file '" + filename + "' which is already indexed");
        }
        // TODO the following if-block could be replaced by some fancier more
        // general class with methods for associating particular types of
        // archived files with particular types of batch processor. e.g.
        // something with a signature like
        // List<FileBatchJob> getIndexers(ArchiveFile file)
        // This more-flexible approach
        // may be of value when we begin to add warc support.
        FileBatchJob theJob = null;
        if (filename.matches("(.*)" + Settings.get(CommonSettings.METADATAFILE_REGEX_SUFFIX))) {
            // All the work is redelegated to the generateDeduplicationCdx method
            generateDeduplicationCdx(filename);
            return;
        } else if (ARCUtils.isARC(filename)) {
            theJob = new WaybackCDXExtractionARCBatchJob();
        } else if (WARCUtils.isWarc(filename)) {
            theJob = new WaybackCDXExtractionWARCBatchJob();
        } else {
            log.warn("Skipping indexing of file with filename '{}'", filename);
            return;
        }
        theJob.processOnlyFileNamed(filename);
        PreservationArcRepositoryClient client = ArcRepositoryClientFactory.getPreservationInstance();
        String replicaId = Settings.get(WaybackSettings.WAYBACK_REPLICA);
        log.info("Submitting {} for {} to {}", theJob.getClass().getName(), getFilename(), replicaId.toString());
        BatchStatus batchStatus = client.batch(theJob, replicaId);
        log.info("Batch job for {} returned", this.getFilename());
        // Normally expect exactly one file per job.
        if (!batchStatus.getFilesFailed().isEmpty() || batchStatus.getNoOfFilesProcessed() == 0
                || !batchStatus.getExceptions().isEmpty()) {
            logBatchError(batchStatus);
        } else {
            if (batchStatus.getNoOfFilesProcessed() > 1) {
                log.warn(
                        "Processed '{}' files for {}.\n This may indicate a doublet in the arcrepository. Proceeding with caution.",
                        batchStatus.getNoOfFilesProcessed(), this.getFilename());
            }
            try {
                collectResults(batchStatus);
            } catch (Exception e) {
                logBatchError(batchStatus);
                log.error("Failed to retrieve results", e);
            }
        }
    }

    /**
     * Collects the batch results from the BatchStatus, first to a file in temporary directory, after which they are
     * renamed to the directory WAYBACK_BATCH_OUTPUTDIR. The status of this object is then updated to reflect that the
     * object has been indexed.
     *
     * @param status the status of a batch job.
     */
    private void collectResults(BatchStatus status) {
        // Use an arbitrary filename for the output
        String outputFilename = UUID.randomUUID().toString();

        // Read the name of the temporary output directory and create it if
        // necessary
        String tempBatchOutputDirName = Settings.get(WaybackSettings.WAYBACK_INDEX_TEMPDIR);
        final File outDir = new File(tempBatchOutputDirName);
        FileUtils.createDir(outDir);

        // Copy the batch output to the temporary directory.
        File batchOutputFile = new File(outDir, outputFilename);
        log.info("Collecting index for '{}' to '{}'", this.getFilename(), batchOutputFile.getAbsolutePath());
        status.copyResults(batchOutputFile);
        log.info("Finished collecting index for '{}' to '{}'", this.getFilename(), batchOutputFile.getAbsolutePath());
        // Read the name of the final batch output directory and create it if
        // necessary
        String finalBatchOutputDir = Settings.get(WaybackSettings.WAYBACK_BATCH_OUTPUTDIR);
        final File finalDirectory = new File(finalBatchOutputDir);
        FileUtils.createDir(finalDirectory);

        // Move the output file from the temporary directory to the final
        // directory
        File finalFile = new File(finalDirectory, outputFilename);
        batchOutputFile.renameTo(finalFile);

        // Update the file status in the object store
        originalIndexFileName = outputFilename;
        isIndexed = true;
        log.info("Indexed '{}' to '{}'", this.filename, finalFile.getAbsolutePath());
        (new ArchiveFileDAO()).update(this);
    }
    
    private void generateDeduplicationCdx(String filename) {
        File crawllogfile = new File(FileUtils.getTempDir(), UUID.randomUUID().toString());
        File deduplicationMigrationFile = new File(FileUtils.getTempDir(), UUID.randomUUID().toString());
        PreservationArcRepositoryClient client = ArcRepositoryClientFactory.getPreservationInstance();
        String replicaId = Settings.get(WaybackSettings.WAYBACK_REPLICA);
        FileBatchJob crawlLogJob = new GetMetadataArchiveBatchJob(
                Pattern.compile(Constants.CRAWL_LOG_URL_PATTERN_STRING), Pattern.compile("text/plain"));
        crawlLogJob.processOnlyFileNamed(filename);
        FileBatchJob deduplicatioMigrationJob = new GetMetadataArchiveBatchJob(
                Pattern.compile(Constants.DEDUPLICATION_MIGRATION_URL_PATTERN_STRING), Pattern.compile("text/plain"));
        deduplicatioMigrationJob.processOnlyFileNamed(filename);
        log.info("Submitting {} for {} to {}", crawlLogJob.getClass().getName(), getFilename(), replicaId.toString());
        BatchStatus batchStatus = client.batch(crawlLogJob, replicaId);
        try {
            // Normally expect exactly one file per job.
            if (!batchStatus.getFilesFailed().isEmpty() || batchStatus.getNoOfFilesProcessed() == 0
                    || !batchStatus.getExceptions().isEmpty()) {
                logBatchError(batchStatus);
            } else {
                if (batchStatus.getNoOfFilesProcessed() > 1) {
                    log.warn(
                            "Processed '{}' files for {}.\n This may indicate a doublet in the arcrepository. Proceeding with caution.",
                            batchStatus.getNoOfFilesProcessed(), this.getFilename());
                }
                // Retrieve logfile from batchresult
                batchStatus.copyResults(crawllogfile);
                // Try to find a duplicationmigration record in the same file
                batchStatus = client.batch(deduplicatioMigrationJob, replicaId);
                if (batchStatus.hasResultFile()) {
                    batchStatus.copyResults(deduplicationMigrationFile);
                }
                boolean doMigration = deduplicationMigrationFile.exists() && deduplicationMigrationFile.length() > 0;
                File resultfile = new File(FileUtils.getTempDir(), UUID.randomUUID().toString());
                FileOutputStream fos = new FileOutputStream(resultfile);
                DeduplicateToCDXAdapter adapter = new DeduplicateToCDXAdapter();
                Hashtable<Pair<String, Long>, Long> lookup = null;
                if (doMigration) {
                    log.info("Migration record found and retrieved from file {}", filename);
                    lookup = DeduplicationMigration.parseMigrationRecords(deduplicationMigrationFile, filename);
                }
                FileInputStream inputStream = new FileInputStream(crawllogfile);
                adapter.setLookup(lookup);
                adapter.adaptStream(inputStream, fos);
                fos.close();
                
                // Finish process
                log.info("Finished collecting dedupindex for '{}' to '{}'", this.getFilename(), resultfile.getAbsolutePath());
                String finalBatchOutputDir = Settings.get(WaybackSettings.WAYBACK_BATCH_OUTPUTDIR);
                final File finalDirectory = new File(finalBatchOutputDir);
                FileUtils.createDir(finalDirectory);

                // Move the output file from the temporary directory to the final
                // directory
                File finalFile = new File(finalDirectory, resultfile.getName());
                resultfile.renameTo(finalFile);

                // Update the file status in the object store
                originalIndexFileName = resultfile.getName();
                isIndexed = true;
                log.info("Indexed '{}' to '{}'", this.filename, finalFile.getAbsolutePath());
                (new ArchiveFileDAO()).update(this);
            }
        } catch (IOException e) {
            log.error("Unexpected exception while making cdx-generation for file {}: {}", filename, e);
        } finally {
            crawllogfile.delete();
            deduplicationMigrationFile.delete();
        }
    }

    /**
     * Logs the error and increments the number of failed attempts for this ArchiveFile.
     *
     * @param status the status of the batch job.
     */
    private void logBatchError(BatchStatus status) {
        String message = "Error indexing file '" + getFilename() + "'\n" + "Number of files processed: '"
                + status.getNoOfFilesProcessed() + "'\n" + "Number of files failed '" + status.getFilesFailed().size()
                + "'";
        if (!status.getExceptions().isEmpty()) {
            message += "\n Exceptions thrown: " + "\n";
            for (FileBatchJob.ExceptionOccurrence e : status.getExceptions()) {
                message += e.toString() + "\n";
            }
        }
        log.error(message);
        indexingFailedAttempts += 1;
        (new ArchiveFileDAO()).update(this);
    }

    // Autogenerated code
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ArchiveFile that = (ArchiveFile) o;

        if (indexingFailedAttempts != that.indexingFailedAttempts) {
            return false;
        }
        if (isIndexed != that.isIndexed) {
            return false;
        }
        if (!filename.equals(that.filename)) {
            return false;
        }

        if (indexedDate != null ? !indexedDate.equals(that.indexedDate) : that.indexedDate != null) {
            return false;
        }
        if (originalIndexFileName != null ? !originalIndexFileName.equals(that.originalIndexFileName)
                : that.originalIndexFileName != null) {
            return false;
        }

        return true;
    }

    // Autogenerated code
    @Override
    public int hashCode() {
        int result = filename.hashCode();
        result = 31 * result + (isIndexed ? 1 : 0);
        result = 31 * result + (originalIndexFileName != null ? originalIndexFileName.hashCode() : 0);
        result = 31 * result + indexingFailedAttempts;
        result = 31 * result + (indexedDate != null ? indexedDate.hashCode() : 0);
        return result;
    }

}
