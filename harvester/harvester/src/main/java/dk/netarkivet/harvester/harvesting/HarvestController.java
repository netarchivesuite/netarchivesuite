/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
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

package dk.netarkivet.harvester.harvesting;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.archive.io.arc.ARCWriter;

import dk.netarkivet.common.Settings;
import dk.netarkivet.common.distribute.arcrepository.ArcRepositoryClientFactory;
import dk.netarkivet.common.distribute.arcrepository.HarvesterArcRepositoryClient;
import dk.netarkivet.common.distribute.indexserver.IndexClientFactory;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.SystemUtils;
import dk.netarkivet.common.utils.arc.ARCUtils;
import dk.netarkivet.harvester.datamodel.Job;
import dk.netarkivet.harvester.datamodel.StopReason;
import dk.netarkivet.harvester.harvesting.distribute.DomainHarvestReport;
import dk.netarkivet.harvester.harvesting.distribute.MetadataEntry;
import dk.netarkivet.harvester.harvesting.distribute.PersistentJobData;

/**
 * This class handles all the things in a single harvest that are not related
 * directly related either to launching Heritrix or to handling JMS messages.
 *
 */

public class HarvestController {
    /**
     * The singleton instance of this class.  Calling close() on the instance
     * will null this field.
     */
    private static HarvestController instance;
    private Log log
            = LogFactory.getLog(HarvestController.class);

    /**
     * String in crawl.log, that Heritrix writes
     *  as the last entry in the progress-statistics.log.
     */
    private static final String HERITRIX_ORDERLY_FINISH_STRING =
        "CRAWL ENDED";


    /**
     * The max time to wait for heritrix to close last ARC files (in secs).
     */
    private static final int WAIT_FOR_HERITRIX_TIMEOUT_SECS = 5;

    /**
     * The ArcRepositoryClient used to communicate with the ArcRepository to
     * store the generated arc-files.
     */
    private HarvesterArcRepositoryClient arcRepController;

    /**
     * Private constructor controlled by getInstance().
     */
    private HarvestController() {
        arcRepController = ArcRepositoryClientFactory.getHarvesterInstance();
    }

    /**
     * Get the instance of the singleton HarvestController.
     *
     * @return The singleton instance.
     */
    public static synchronized HarvestController getInstance() {
        if (instance == null) {
            instance = new HarvestController();
        }
        return instance;
    }

    /**
     * Clean up this singleton, releasing the ArcRepositoryClient and removing
     * the instance.  This instance should not be used after this method has
     * been called.  After this has been called, new calls to getInstance will
     * return a new instance.
     */
    public void cleanup() {
        if (arcRepController != null) {
            arcRepController.close();
        }
        instance = null;
    }

    /**
     * Writes the files involved with a harvests.
     *
     * @param crawldir        The directory that the crawl should take place
     *                        in.
     * @param job             The Job object containing various harvest setup
     *                        data.
     * @param metadataEntries Any metadata entries sent along with the job that
     *                        should be stored for later use.
     * @return An object encapsulating where these files have been written.
     */
    public HeritrixFiles writeHarvestFiles(File crawldir, Job job,
                                         List<MetadataEntry> metadataEntries) {
        final HeritrixFiles files =
            new HeritrixFiles(crawldir,
                              job.getJobID(),
                              job.getOrigHarvestDefinitionID());

        // Create harvestInfo file in crawldir
        // & create preharvest-metadata-1.arc
        log.debug("Writing persistent job data for job " + job.getJobID());
        // Check that harvestInfo does not yet exist

        // Write job data to persistent storage (harvestinfo file)
        new PersistentJobData(files.getCrawlDir()).write(job);
        // Create jobId-preharvest-metadata-1.arc for this job
        writePreharvestMetadata(job, metadataEntries, crawldir);

        files.writeSeedsTxt(job.getSeedList());
        files.writeOrderXml(job.getOrderXMLdoc());

        files.writeIndex(fetchDeduplicateIndex(metadataEntries));
        return files;
    }

    /**
     * Writes metadata to an ARC with the following name:
     * <jobid>-preharvest-metadata-1.arc.
     *
     * @param harvestJob a given Job.
     * @param metadata   the list of metadata entries to write to ARC.
     * @param crawlDir   the directory, where the ARC-file will be written.
     * @throws IOFailure If there are errors in writing the ARC file.
     */
    private void writePreharvestMetadata(Job harvestJob,
                                         List<MetadataEntry> metadata,
                                         File crawlDir)
            throws IOFailure {
        if (metadata.size() == 0) {
            // Do not generate preharvest metadata file for empty list
            return;
        }
        File arcFile =
            new File(crawlDir,
                        HarvestDocumentation.getPreharvestMetadataARCFileName(
                                        harvestJob.getJobID()));
        try {
            ARCWriter aw = null;
            try {
                aw = ARCUtils.createARCWriter(arcFile);
                for (MetadataEntry m : metadata) {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    baos.write(m.getData());
                    aw.write(m.getURL(), m.getMimeType(),
                             SystemUtils.getLocalIP(),
                             System.currentTimeMillis(), baos.size(), baos);
                }
            } finally {
                try {
                    if (aw != null) {
                        aw.close();
                    }
                } catch (IOException e) {
                    //TODO: Is this fatal? What if data isn't flushed?
                    log.warn("Unable to close ArcWriter '"
                             + aw.getFile().getAbsolutePath() + "'", e);
                }
            }
        } catch (IOException e) {
            throw new IOFailure("Error writing to arcfile for job "
                                + harvestJob.getJobID() + ".\n", e);
        }
    }

    /**
     * Creates the actual HeritrixLauncher instance and runs it, after the
     * various setup files have been written.
     *
     * @param files Description of files involved in running Heritrix.
     */
    public void runHarvest(HeritrixFiles files) {
        HeritrixLauncher hl = HeritrixLauncher.getInstance(files);
        hl.doCrawl();
    }

    /**
     * Controls storing all files involved in a job.  The files are
     *  1) The actual ARC files,
     *  2) The metadata files
     *  The crawl.log is parsed and information for each domain is generated
     *  and stored in a DomainHarvestReport object which
     *  is sent along in the crawlstatusmessage.
     *
     * Additionally, any leftover open ARC files are closed and harvest
     * documentation is extracted before upload starts.
     *
     * @param files The HeritrixFiles object for this crawl.
     * @param errorMessage A place where error messages accumulate.
     * @param failedFiles  List of files that failed to upload.
     * @return An object containing info about the domains harvested.
     */
    public DomainHarvestReport storeFiles(
            HeritrixFiles files, StringBuilder errorMessage,
            List<File> failedFiles) {
        ArgumentNotValid.checkNotNull(files, "HeritrixFiles files");
        ArgumentNotValid.checkNotNull(errorMessage, "StringBuilder errorMessage");
        ArgumentNotValid.checkNotNull(failedFiles, "List<File> failedFiles");
        long jobID = files.getJobID();
        long harvestID = files.getHarvestID();
        File crawlDir = files.getCrawlDir();
        try {
            IngestableFiles inf = new IngestableFiles(crawlDir, jobID);
            inf.closeOpenFiles(WAIT_FOR_HERITRIX_TIMEOUT_SECS);
            // Create a metadata ARC file
            HarvestDocumentation.documentHarvest(crawlDir, jobID, harvestID);
            // Upload all files
            uploadFiles(inf.getArcFiles(), errorMessage, failedFiles);
            uploadFiles(inf.getMetadataArcFiles(), errorMessage, failedFiles);
            // Make the domainHarvestReport ready for uploading
            return generateHeritrixDomainHarvestReport(files, errorMessage);
        } catch (IOFailure e) {
            String errMsg = "IOFailure occurred, while trying to upload files";
            log.warn(errMsg, e);
            throw new IOFailure(errMsg, e);
        }
    }

    /**
     * Generate DomainHarvestReport object that contains information about
     * the domains harvested, or log a warning if the crawl.log was not found.
     *
     * @param files The heritrix files object for this crawl to get logs from.
     * @param errorMessage An accumulator for error messages.
     * @return A report object with the domainHarvest data, or null for none
     * present.
     */
    private DomainHarvestReport generateHeritrixDomainHarvestReport(
            HeritrixFiles files,
            StringBuilder errorMessage) {
        File heritrixCrawlLog = files.getCrawlLog();
        File heritrixStatisticsLog = files.getProgressStatisticsLog();
        StopReason defaultStopReason =
            findDefaultStopReason(heritrixStatisticsLog);

        if (heritrixCrawlLog.isFile()) {
            return new HeritrixDomainHarvestReport(heritrixCrawlLog,
                    defaultStopReason);
        } else {
            String errorMsg = "No crawl.log found in '"
                              + heritrixCrawlLog.getAbsolutePath() + "'";
            errorMessage.append(errorMsg).append("\n");
            log.warn(errorMsg);
            return null;
        }
    }

    /**
     * Upload given files to the arc repository.
     *
     * @param files        List of (ARC) files to upload.
     * @param errorMessage Accumulator for error messages.
     * @param failedFiles  Accumulator for failed files.
     */
    private void uploadFiles(List<File> files, StringBuilder errorMessage,
                             List<File> failedFiles) {
        // Upload all arcfiles
        if (files != null) {
            for (File f : files) {
                try {
                    log.info("Uploading to arcrepository the file '" 
                            + f.getName() + "'.");
                    arcRepController.store(f);
                } catch (Exception e) {
                    File oldJobsDir
                            = new File(Settings.get(
                            Settings.HARVEST_CONTROLLER_OLDJOBSDIR));
                    String errorMsg = "Error uploading arcfile '"
                                      + f.getAbsolutePath()
                                      + "' Will be moved to '"
                                      + oldJobsDir.getAbsolutePath() + "'";
                    errorMessage.append(errorMsg).append("\n")
                            .append(e.toString()).append("\n");
                    log.warn(errorMsg, e);
                    failedFiles.add(f);
                }
            }
        }
    }

    /**
     * Retrieve the list of jobs for deduplicate reduction.
     *
     * Runs through all metadata entries, finding duplicate reduction entries,
     * and parsing all jobIDs in them, warning only on errors.
     *
     * @param metadataEntries list of metadataEntries
     * @return the list of jobs for deduplicate reduction
     */
    private List<Long> parseJobIDsForDuplicateReduction(
            List<MetadataEntry> metadataEntries) {
        // find metadataEntry for duplicatereduction if any.
        List<Long> result = new ArrayList<Long>();
        for (MetadataEntry me : metadataEntries) {
            if (me.isDuplicateReductionMetadataEntry()) {
                String s = new String(me.getData());
                String[] longs = s.split(",");
                for (String stringLong : longs) {
                    try {
                        result.add(new Long(Long.parseLong(stringLong)));
                    } catch (NumberFormatException e) {
                        log.warn("Unable convert String '" + stringLong
                                 + "' in duplicate reduction jobid list"
                                 + " metadataEntry '" + s
                                 + "' to a jobID. Ignoring.",
                                 e);
                    }
                }
            }
        }
        return result;
    }

    /**
     * Get an index for deduplication.  This will make a call to the index
     * server, requesting an index for the given IDs.  The files will then be
     * cached locally.
     *
     * If we request index for IDs that don't exist/have problems, we get a
     * smaller set of IDs in our cache files, and next time we ask for the same
     * index, we will call the index server again. This will be handled well,
     * though, because if the ids are still missing, we will get a reply telling
     * us to use the cached smaller index anyway.
     *
     * @param metadataEntries list of metadataEntries top get jobIDs from.
     * @return a directory  containing the index itself
     * @throws IOFailure on errors retrieving the index from the client.
     * TODO: Better forgiving handling of no index available
     */
    private File fetchDeduplicateIndex(List<MetadataEntry> metadataEntries) {
        // Get list of jobs, which should be used for duplicate reduction
        // and retrieve a luceneIndex from the IndexServer
        // based on the crawl.logs from these jobs and their CDX'es.
        List<Long> jobIDsForDuplicateReduction
                = parseJobIDsForDuplicateReduction(metadataEntries);
        return IndexClientFactory.getDedupCrawllogInstance().getIndex(
                new HashSet<Long>(jobIDsForDuplicateReduction));
    }

    /**
     * Get whether we stopped normally in progress statistics log
     * @param logFile A progress-statistics.log file
     * @return StopReason.DOWNLOAD_COMPLETE for progress statistics ending with
     * CRAWL ENDED, StopReason.DOWNLOAD_UNFINISHED otherwise or if file does
     * not exist.
     * @throws ArgumentNotValid on null argument.
     */
    public static StopReason findDefaultStopReason(File logFile) {
        ArgumentNotValid.checkNotNull(logFile, "File logFile");
        if (!logFile.exists()) {
            return StopReason.DOWNLOAD_UNFINISHED;
        }
        String lastLine = FileUtils.readLastLine(logFile);
        if (lastLine.contains(HERITRIX_ORDERLY_FINISH_STRING)) {
            return StopReason.DOWNLOAD_COMPLETE;
        } else {
            return StopReason.DOWNLOAD_UNFINISHED;
        }
    }

}
