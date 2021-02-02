/*
 * #%L
 * Netarchivesuite - wayback
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
package dk.netarkivet.wayback.indexer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.persistence.Entity;
import javax.persistence.Id;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.util.ToolRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.distribute.arcrepository.ArcRepositoryClientFactory;
import dk.netarkivet.common.distribute.arcrepository.BatchStatus;
import dk.netarkivet.common.distribute.arcrepository.PreservationArcRepositoryClient;
import dk.netarkivet.common.distribute.bitrepository.Bitrepository;
import dk.netarkivet.common.exceptions.IllegalState;
import dk.netarkivet.common.utils.BitmagUtils;
import dk.netarkivet.common.utils.service.FileResolver;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.SettingsFactory;
import dk.netarkivet.common.utils.hadoop.HadoopFileUtils;
import dk.netarkivet.common.utils.hadoop.HadoopJobUtils;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.utils.service.SimpleFileResolver;
import dk.netarkivet.common.utils.arc.ARCUtils;
import dk.netarkivet.common.utils.batch.FileBatchJob;
import dk.netarkivet.common.utils.warc.WARCUtils;
import dk.netarkivet.wayback.WaybackSettings;
import dk.netarkivet.wayback.batch.DeduplicationCDXExtractionBatchJob;
import dk.netarkivet.wayback.batch.WaybackCDXExtractionARCBatchJob;
import dk.netarkivet.wayback.batch.WaybackCDXExtractionWARCBatchJob;
import dk.netarkivet.wayback.hadoop.CDXMapper;
import dk.netarkivet.common.utils.hadoop.HadoopJobTool;

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
     * Indexes this file by either running a hadoop job or a batch job, depending on settings.
     *
     * @throws IllegalState If the indexing has already been done.
     */
    public void index() throws IllegalState {
        log.info("Indexing {}", this.getFilename());
        if (isIndexed) {
            throw new IllegalState("Attempted to index file '" + filename + "' which is already indexed");
        }

        if (Settings.getBoolean(CommonSettings.USE_BITMAG_HADOOP_BACKEND)) {
            hadoopIndex();
        } else {
            batchIndex();
        }
    }

    /**
     * Runs a map-only (no reduce) job to index this file.
     */
    private void hadoopIndex() {
        boolean isArchiveFile = ARCUtils.isARC(filename) || WARCUtils.isWarc(filename);
        if (!isArchiveFile) {
            log.warn("Skipping indexing of file with filename '{}'", filename);
            return;
        }

        System.setProperty("HADOOP_USER_NAME", Settings.get(CommonSettings.HADOOP_USER_NAME));
        Configuration conf = HadoopJobUtils.getConfFromSettings();
        UUID uuid = UUID.randomUUID();
        log.info("File {} indexed with job uuid for i/o {}.", this.filename, uuid);
        try (FileSystem fileSystem = FileSystem.newInstance(conf)) {
            String hadoopInputDir = Settings.get(CommonSettings.HADOOP_MAPRED_CDXJOB_INPUT_DIR);
            if (hadoopInputDir == null) {
                log.error("Parent input dir specified by {} must not be null.", CommonSettings.HADOOP_MAPRED_CDXJOB_INPUT_DIR);
                return;
            }
            try {
                HadoopFileUtils.initDir(fileSystem, hadoopInputDir);
            } catch (IOException e) {
                log.error("Failed to init input dir {}", hadoopInputDir, e);
                return;
            }
            Path hadoopInputNameFile = new Path(hadoopInputDir, uuid.toString());
            log.info("Hadoop input file will be {}", hadoopInputNameFile);

            String parentOutputDir = Settings.get(CommonSettings.HADOOP_MAPRED_CDXJOB_OUTPUT_DIR);
            if (parentOutputDir == null) {
                log.error("Parent output dir specified by {} must not be null.", CommonSettings.HADOOP_MAPRED_CDXJOB_OUTPUT_DIR);
                return;
            }
            try {
                HadoopFileUtils.initDir(fileSystem, parentOutputDir);
            } catch (IOException e) {
                log.error("Failed to init output dir {}", parentOutputDir, e);
                return;
            }
            Path jobOutputDir = new Path(parentOutputDir, uuid.toString());
            log.info("Output directory for job is {}", jobOutputDir);
            java.nio.file.Path localInputTempFile = null;
            localInputTempFile = Files.createTempFile(null, null);

            FileResolver fileResolver = SettingsFactory.getInstance(CommonSettings.FILE_RESOLVER_CLASS);
            if (fileResolver instanceof SimpleFileResolver) {
                String pillarParentDir = Settings.get(CommonSettings.HADOOP_MAPRED_INPUT_FILES_PARENT_DIR);
                ((SimpleFileResolver) fileResolver).setDirectory(Paths.get(pillarParentDir));
            }
            java.nio.file.Path filePath = fileResolver.getPath(filename);
            String inputLine = "file://" + filePath.toString();
            log.info("Inserting {} in {}.", inputLine, localInputTempFile);
            Files.write(localInputTempFile, inputLine.getBytes());
            // Write the input file to hdfs
            log.info("Copying file with input paths {} to hdfs {}.", localInputTempFile, hadoopInputNameFile);
            fileSystem.copyFromLocalFile(false, new Path(localInputTempFile.toAbsolutePath().toString()),
                    hadoopInputNameFile);
            log.info("Starting CDX job on file '{}'", filename);
            int exitCode = 0;
            try {
                log.info("Starting hadoop job with input {} and output {}.", hadoopInputNameFile, jobOutputDir);
                exitCode = ToolRunner.run(new HadoopJobTool(conf, new CDXMapper()),
                        new String[] {hadoopInputNameFile.toString(), jobOutputDir.toString()});
                if (exitCode == 0) {
                    log.info("CDX job for file {} was a success!", filename);
                    collectHadoopResults(fileSystem, jobOutputDir);
                } else {
                    log.warn("Hadoop job failed with exit code '{}'", exitCode);
                }
            } catch (Exception exception) {
               log.error("Hadoop indexing job failed to run normally.", exception);
            }
        } catch (IOException e) {
           log.error("Error on hadoop filesystem.", e);
        }

    }

    /**
     * Runs a map-only (no reduce) job to index this file.
     * Uses the costly approach of copying the file to hdfs first
     */
    private void hadoopHDFSIndex() {
        // For now only handles WARC files
        String hadoopInputDir = Settings.get(CommonSettings.HADOOP_MAPRED_CDXJOB_INPUT_DIR);
        // As each file for now has its own job, the inputfile for each job
        // is just made unique from the archivefile's name
        Path hadoopInputNameFile = new Path(
                filename.substring(0, filename.lastIndexOf('.')) + "_map_input.txt");
        Configuration conf = HadoopJobUtils.getConfFromSettings();
        Bitrepository bitrep = BitmagUtils.initBitrep();

        // Get file and put it in hdfs
        log.info("Getting file '{}' from bitmag for indexing", filename);
        File inputFile = bitrep.getFile(filename, "netarkivet", null, Settings.get(
                dk.netarkivet.common.distribute.bitrepository.BitmagUtils.BITREPOSITORY_USEPILLAR));
        Path inputFilePath = new Path(inputFile.getAbsolutePath());
        FileSystem fs = null;
        try {
            fs = FileSystem.get(conf);
            try {
                log.info("Copying '{}' to hdfs", inputFilePath.toString());
                fs.copyFromLocalFile(false, inputFilePath, new Path(hadoopInputDir)); // TODO Need hadoopInputDir to exist prior to this!
            } catch (IOException e) {
                log.warn("Failed to upload '{}' to hdfs", inputFilePath.toString(), e);
                return;
            }
            fs.deleteOnExit(hadoopInputNameFile);

            // Write the filename/path of the WARC-file to the input file for Hadoop to process.
            // NB files of same name are overwritten by default
            try {
                log.info("Creating input file '{}' on hdfs", hadoopInputNameFile);
                FSDataOutputStream fsdos = fs.create(hadoopInputNameFile);
                log.info("Writing input line '{}' to input file", hadoopInputDir + "/" + filename);
                fsdos.writeBytes(hadoopInputDir + "/" + filename);
            } catch (IOException e) {
                log.warn("Could not write input line to {}", hadoopInputNameFile, e);
                return;
            }

            // Start job on file
            log.info("Starting CDXJob on file '{}'", filename);
            try {
                // TODO Guess conditioning on which file it is should be handled here by designating different mapper classes
                int exitCode = ToolRunner.run(new HadoopJobTool(conf, new CDXMapper()),
                        new String[] {
                                hadoopInputNameFile.getName(), Settings.get(CommonSettings.HADOOP_MAPRED_CDXJOB_OUTPUT_DIR)});

                if (exitCode != 0) {
                    log.warn("Hadoop job failed with exit code '{}'", exitCode);
                    try {
                        fs.close();
                    } catch (IOException e) {
                        log.warn("Problem closing FileSystem: ", e);
                    }
                } else {
                    collectHadoopResults(fs, new Path(Settings.get(CommonSettings.HADOOP_MAPRED_CDXJOB_OUTPUT_DIR)));
                }
            } catch (Exception e) {
                log.warn("Running hadoop job threw exception", e);
            }
        } catch (Exception e) {
            log.warn("Couldn't get FileSystem from configuration", e);
        } finally {
            try {
                if (fs != null) {
                    fs.close();
                }
            } catch (IOException e) {
                log.warn("Problem closing FileSystem: ", e);
            }
        }
    }

    /**
     * Copies the results from the Hadoop job to a file in a local tempdir and afterwards moves
     * the results to WAYBACK_BATCH_OUTPUTDIR. The status of this object is then updated to reflect that the
     * object has been indexed.
     * @param fs The Hadoop FileSystem that is used
     * @param jobOutputDir The job output dir to find the 'part'-files in, which contain the resulting cdx lines.
     */
    private void collectHadoopResults(FileSystem fs, Path jobOutputDir) {
        File outputFile = makeNewFileInWaybackTempDir();
        log.info("Collecting index for '{}' from parts in '{}' to '{}'", this.getFilename(), jobOutputDir, outputFile.getAbsolutePath());
        try {
            List<String> cdxLines = HadoopJobUtils.collectOutputLines(fs, jobOutputDir);
            FileUtils.writeCollectionToFile(outputFile, cdxLines);
            log.info("Finished collecting index for '{}' to '{}'", this.getFilename(), outputFile.getAbsolutePath());
        } catch (IOException e) {
            log.warn("Could not collect index results from '{}'", jobOutputDir.toString(), e);
        }
        File finalFile = moveFileToWaybackOutputDir(outputFile);

        // Update the file status in the object store
        originalIndexFileName = outputFile.getName();
        isIndexed = true;
        log.info("Indexed '{}' to '{}'. Marking as indexed in DB.", this.filename, finalFile.getAbsolutePath());
        (new ArchiveFileDAO()).update(this);
    }

    /**
     * Run a batch job to index this file, storing the result locally. If this method runs successfully, the isIndexed
     * flag will be set to true and the originalIndexFileName field will be set to the (arbitrary) name of the file
     * containing the results. The values are persisted to the datastore.
     */
    private void batchIndex() {
        // TODO the following if-block could be replaced by some fancier more
        // general class with methods for associating particular types of
        // archived files with particular types of batch processor. e.g.
        // something with a signature like
        // List<FileBatchJob> getIndexers(ArchiveFile file)
        // This more-flexible approach
        // may be of value when we begin to add warc support.
        FileBatchJob theJob = null;
        if (filename.matches("(.*)" + Settings.get(CommonSettings.METADATAFILE_REGEX_SUFFIX))) {
            theJob = new DeduplicationCDXExtractionBatchJob();
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
        log.info("Submitting {} for {} to {}", theJob.getClass().getName(), getFilename(), replicaId);
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
        File batchOutputFile = makeNewFileInWaybackTempDir();
        log.info("Collecting index for '{}' to '{}'", this.getFilename(), batchOutputFile.getAbsolutePath());
        status.copyResults(batchOutputFile);
        log.info("Finished collecting index for '{}' to '{}'", this.getFilename(), batchOutputFile.getAbsolutePath());
        File finalFile = moveFileToWaybackOutputDir(batchOutputFile);

        // Update the file status in the object store
        originalIndexFileName = batchOutputFile.getName();
        isIndexed = true;
        log.info("Indexed '{}' to '{}'", this.filename, finalFile.getAbsolutePath());
        (new ArchiveFileDAO()).update(this);
    }

    /**
     * Helper method.
     * Makes a new file in the wayback temp dir and returns it.
     * If the directory does not exist, it is also created.
     * @return A new file in the wayback temp dir.
     */
    private File makeNewFileInWaybackTempDir() {
        // Use an arbitrary filename for the output
        String outputFilename = UUID.randomUUID().toString();

        // Read the name of the temporary output directory and create it if necessary
        String tempOutputDir = Settings.get(WaybackSettings.WAYBACK_INDEX_TEMPDIR);
        final File outDir = new File(tempOutputDir);
        FileUtils.createDir(outDir);

        // Copy the output to the temporary directory.
        return new File(outDir, outputFilename);
    }

    /**
     * Helper method.
     * Moves (renames) the output file from the batch process to the wayback output dir.
     * If the directory does not exist, it is also created.
     * @param outputFile The file to move
     * @return The file now in the output dir
     */
    private File moveFileToWaybackOutputDir(File outputFile) {
        // Read the name of the final batch output directory and create it if necessary
        String finalBatchOutputDir = Settings.get(WaybackSettings.WAYBACK_BATCH_OUTPUTDIR);
        final File finalDirectory = new File(finalBatchOutputDir);
        FileUtils.createDir(finalDirectory);

        // Move the output file from the temporary directory to the final directory
        File finalFile = new File(finalDirectory, outputFile.getName());
        outputFile.renameTo(finalFile);
        return finalFile;
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
