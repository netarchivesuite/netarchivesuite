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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

import javax.persistence.Entity;
import javax.persistence.Id;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.util.Progressable;
import org.apache.hadoop.util.ToolRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.distribute.arcrepository.ArcRepositoryClientFactory;
import dk.netarkivet.common.distribute.arcrepository.BatchStatus;
import dk.netarkivet.common.distribute.arcrepository.PreservationArcRepositoryClient;
import dk.netarkivet.common.exceptions.IllegalState;
import dk.netarkivet.common.utils.hadoop.GetMetadataMapper;
import dk.netarkivet.common.utils.hadoop.HadoopJob;
import dk.netarkivet.common.utils.hadoop.HadoopJobStrategy;
import dk.netarkivet.common.utils.service.FileResolver;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.SettingsFactory;
import dk.netarkivet.common.utils.hadoop.HadoopFileUtils;
import dk.netarkivet.common.utils.hadoop.HadoopJobUtils;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.utils.service.SimpleFileResolver;
import dk.netarkivet.common.utils.arc.ARCUtils;
import dk.netarkivet.common.utils.batch.FileBatchJob;
import dk.netarkivet.common.utils.hadoop.HadoopJobTool;
import dk.netarkivet.common.utils.warc.WARCUtils;
import dk.netarkivet.wayback.WaybackSettings;
import dk.netarkivet.wayback.batch.DeduplicationCDXExtractionBatchJob;
import dk.netarkivet.wayback.batch.WaybackCDXExtractionARCBatchJob;
import dk.netarkivet.wayback.batch.WaybackCDXExtractionWARCBatchJob;
import dk.netarkivet.wayback.hadoop.CDXMapper;
import dk.netarkivet.wayback.hadoop.CDXStrategy;
import sun.security.krb5.KrbException;

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

        Configuration conf = HadoopJobUtils.getConf();
        conf.set("cdx_filename", filename);
        try (FileSystem fileSystem = FileSystem.newInstance(conf)) {
            HadoopJobStrategy jobStrategy = new CDXStrategy(0L, fileSystem);
            HadoopJob job = new HadoopJob(0L, jobStrategy);
            UUID uuid = UUID.randomUUID();
            Path jobInputFile = jobStrategy.createJobInputFile(uuid);
            job.setJobInputFile(jobInputFile);
            createJobInputFile(filename, jobInputFile, fileSystem);
            Path jobOutputDir = jobStrategy.createJobOutputDir(uuid);
            job.setJobOutputDir(jobOutputDir);
            int exitCode = jobStrategy.runJob(jobInputFile, jobOutputDir);
            if (exitCode == 0) {
                log.info("CDX job for file {} was a success!", filename);
                collectHadoopResults(fileSystem, jobOutputDir);
            } else {
                log.warn("Hadoop job failed with exit code '{}'", exitCode);
                this.setIndexingFailedAttempts(indexingFailedAttempts++);
                (new ArchiveFileDAO()).update(this);
            }
        } catch (IOException e) {
            log.warn("Failure in indexing {}", filename, e);
        }
    }

    public static void main(String[] args) throws KrbException, IOException {
        HadoopJobUtils.doKerberosLogin();
        ArchiveFile archiveFile = new ArchiveFile();
        archiveFile.setFilename(args[0]);
        archiveFile.hadoopIndex();
    }

    private void createJobInputFile(String filename, Path jobInputFile, FileSystem fileSystem) throws IOException {
        //Create the input file locally
        File localInputTempFile = File.createTempFile("cdxextract", ".txt",
                Settings.getFile(CommonSettings.DIR_COMMONTEMPDIR));
        FileResolver fileResolver = SettingsFactory.getInstance(CommonSettings.FILE_RESOLVER_CLASS);
        if (fileResolver instanceof SimpleFileResolver) {
            String pillarParentDir = Settings.get(CommonSettings.HADOOP_MAPRED_INPUT_FILES_PARENT_DIR);
            ((SimpleFileResolver) fileResolver).setDirectory(Paths.get(pillarParentDir));
        }
        java.nio.file.Path filePath = fileResolver.getPath(filename);
        if (filePath == null) {
            log.warn("No path identified for file '{}'", filename);
            throw new FileNotFoundException("File resolver failed to identity file " + filename);
        }
        String inputLine = "file://" + filePath.toString();
        log.info("Inserting {} in {}.", inputLine, localInputTempFile);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(localInputTempFile))) {
            writer.write(inputLine);
            writer.newLine();
        }

        // Write the input file to hdfs
        log.info("Copying file with input paths {} to hdfs filesystem {}, {}.", localInputTempFile, fileSystem, jobInputFile);
        Path src = new Path(localInputTempFile.getAbsolutePath());
        log.info("Copying from {}", src);
/*        try (FSDataOutputStream fsDataOutputStream = fileSystem.create(jobInputFile)) {
            log.info("Writing data to input file.");
            fsDataOutputStream.writeUTF("file://" + filePath.toString());
        }*/
        fileSystem.copyFromLocalFile(
                src,
                jobInputFile
        );


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
        log.info("Collecting results for {} from {} to {}", this.getFilename(), jobOutputDir, outputFile.getAbsolutePath());
        try (OutputStream os = new FileOutputStream(outputFile)) {
            HadoopJobUtils.collectOutputLines(fs, jobOutputDir, os);
        } catch (IOException e) {
            log.warn("Could not collect index results from '{}'", jobOutputDir.toString(), e);
        }
        log.info("Collected {} bytes of index for {} from {} to {}", outputFile.length(), this.getFilename(), jobOutputDir, outputFile.getAbsolutePath());
        File finalFile = moveFileToWaybackOutputDir(outputFile);
        log.info("Moved index for {} to {}", this.getFilename(), finalFile.getAbsolutePath());
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
