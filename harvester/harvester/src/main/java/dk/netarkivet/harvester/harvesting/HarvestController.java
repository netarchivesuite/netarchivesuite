/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2011 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.archive.io.arc.ARCWriter;
import org.dom4j.Document;
import org.dom4j.Node;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.distribute.arcrepository.ArcRepositoryClientFactory;
import dk.netarkivet.common.distribute.arcrepository.BatchStatus;
import dk.netarkivet.common.distribute.arcrepository.BitarchiveRecord;
import dk.netarkivet.common.distribute.arcrepository.HarvesterArcRepositoryClient;
import dk.netarkivet.common.distribute.indexserver.Index;
import dk.netarkivet.common.distribute.indexserver.IndexClientFactory;
import dk.netarkivet.common.distribute.indexserver.JobIndexCache;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.NotificationsFactory;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.utils.SystemUtils;
import dk.netarkivet.common.utils.arc.ARCUtils;
import dk.netarkivet.common.utils.batch.FileBatchJob;
import dk.netarkivet.common.utils.cdx.CDXRecord;
import dk.netarkivet.common.utils.cdx.ExtractCDXJob;
import dk.netarkivet.harvester.HarvesterSettings;
import dk.netarkivet.harvester.datamodel.HeritrixTemplate;
import dk.netarkivet.harvester.datamodel.Job;
import dk.netarkivet.harvester.harvesting.distribute.MetadataEntry;
import dk.netarkivet.harvester.harvesting.distribute.PersistentJobData;
import dk.netarkivet.harvester.harvesting.distribute.PersistentJobData.HarvestDefinitionInfo;
import dk.netarkivet.harvester.harvesting.report.HarvestReport;
import dk.netarkivet.harvester.harvesting.report.HarvestReportFactory;
//import dk.netarkivet.viewerproxy.webinterface.Reporting;

/**
 * This class handles all the things in a single harvest that are not related
 * directly related either to launching Heritrix or to handling JMS messages.
 *
 */
public class HarvestController {
    /**
     * The singleton instance of this class.  Calling cleanup() on the instance
     * will null this field.
     */
    private static HarvestController instance;
    /** The instance logger. */
    private Log log
            = LogFactory.getLog(HarvestController.class);

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
        //instance = null;
        resetInstance();
    }
    
    /**
     * Reset the singleton instance. 
     */
    private static void resetInstance() {
        instance = null;
    }
    

    /**
     * Writes the files involved with a harvests.
     * Creates the Heritrix arcs directory to ensure that this
     * directory exists in advance.
     *
     * @param crawldir        The directory that the crawl should take place
     *                        in.
     * @param job             The Job object containing various harvest setup
     *                        data.
     * @param hdi             The object encapsulating documentary information
     *                        about the harvest.
     * @param metadataEntries Any metadata entries sent along with the job that
     *                        should be stored for later use.
     * @return An object encapsulating where these files have been written.
     */
    public HeritrixFiles writeHarvestFiles(
            File crawldir,
            Job job,
            HarvestDefinitionInfo hdi,
            List<MetadataEntry> metadataEntries) {
        final HeritrixFiles files =
            new HeritrixFiles(crawldir,
                              job.getJobID(),
                              job.getOrigHarvestDefinitionID());

        
        // Check, if this job is a job that tries to continue a previous job
        // using the Heritrix recover.gz log.
        if (job.getContinuationOf() != null) {
            Long previousJob = job.getContinuationOf();
            List<CDXRecord> metaCDXes = null;
            try {
                metaCDXes 
                = getMetadataCDXRecordsForJob(previousJob);
            } catch (IOFailure e) {
                log.debug("Failed to retrive CDX of metatadata records. Maybe the metadata arcfile for job " 
                        + previousJob + " does not exist in repository", e);
            }
            
            CDXRecord recoverlogCDX = null;
            if (metaCDXes != null) {
                for (CDXRecord cdx : metaCDXes) {
                    if (cdx.getURL().matches(MetadataFile.RECOVER_LOG_PATTERN)) {
                        recoverlogCDX = cdx;
                    }
                }
                if (recoverlogCDX == null) {
                    log.debug("No recover.gz log found in metadata-arcfile");
                } else {
                    log.debug("recover.gz log found in metadata-arcfile");
                }
            }
            
            BitarchiveRecord br = null;
            if (recoverlogCDX != null) { // Retrieve recover.gz from metadata.arc file
                br = ArcRepositoryClientFactory.getViewerInstance().get(
                        recoverlogCDX.getArcfile(), recoverlogCDX.getOffset());
                if (br != null) {
                    log.debug("recover.gz log retrieved from metadata-arcfile");
                    files.writeRecoverBackupfile(br.getData());
                    // modify order.xml, so Heritrix recover-path points
                    // to the retrieved recoverlog
                    insertHeritrixRecoverPathInOrderXML(job, files);
                } else {
                    log.debug("recover.gz log not retrieved from metadata-arcfile");
                }
            } 
        }
        
        // Create harvestInfo file in crawldir
        // & create preharvest-metadata-1.arc
        log.debug("Writing persistent job data for job " + job.getJobID());
        // Check that harvestInfo does not yet exist
        
        // Write job data to persistent storage (harvestinfo file)
        new PersistentJobData(files.getCrawlDir()).write(job, hdi);
        // Create jobId-preharvest-metadata-1.arc for this job
        writePreharvestMetadata(job, metadataEntries, crawldir);

        files.writeSeedsTxt(job.getSeedListAsString());
       
        files.writeOrderXml(job.getOrderXMLdoc());
        // Only retrieve index if deduplication is not disabled in the template.
        if (HeritrixLauncher.isDeduplicationEnabledInTemplate(
                job.getOrderXMLdoc())) {
            log.debug("Deduplication enabled. Fetching deduplication index..");
            files.setIndexDir(fetchDeduplicateIndex(metadataEntries));
        } else {
            log.debug("Deduplication disabled.");
        }

        // Create Heritrix arcs directory before starting Heritrix to ensure
        // the arcs directory exists in advance.
        boolean created = files.getArcsDir().mkdir();
        if (!created) {
            log.warn("Unable to create arcsdir: " + files.getArcsDir());
        }
        return files;
    }

    /**
     * Insert the correct recoverpath in the order.xml for the given harvestjob.
     * @param job A harvestjob
     * @param files Heritrix files related to this harvestjob.
     */
    private void insertHeritrixRecoverPathInOrderXML(Job job, HeritrixFiles files) {
        Document order = job.getOrderXMLdoc();
        final String RECOVERLOG_PATH_XPATH =
                "/crawl-order/controller/string[@name='recover-path']";
        Node orderXmlNode = order.selectSingleNode(RECOVERLOG_PATH_XPATH);
        if (orderXmlNode != null) {
            orderXmlNode.setText(files.getRecoverBackupGzFile().getAbsolutePath());
            log.debug("The Heritrix recover path now refers to '" 
                    + files.getRecoverBackupGzFile().getAbsolutePath()
                    + "'.");
            job.setOrderXMLDoc(order);
        } else {
            throw new IOFailure(
                    "Unable to locate the '" + RECOVERLOG_PATH_XPATH 
                    + "' element in order.xml: "
                    + order.asXML());
        }
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
                    ByteArrayInputStream bais = new ByteArrayInputStream(
                            m.getData());
                    aw.write(m.getURL(), m.getMimeType(),
                             SystemUtils.getLocalIP(),
                             System.currentTimeMillis(), m.getData().length,
                             bais);
                }
            } finally {
                try {
                    if (aw != null) {
                        aw.close();
                    }
                } catch (IOException e) {
                    //TODO Is this fatal? What if data isn't flushed?
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
     * @param files Description of files involved in running Heritrix. Not Null.
     * @throws ArgumentNotValid if an argument isn't valid.
     */
    public void runHarvest(HeritrixFiles files) throws ArgumentNotValid {
        ArgumentNotValid.checkNotNull(files, "HeritrixFiles files");
        HeritrixLauncher hl = HeritrixLauncherFactory.getInstance(files);
        hl.doCrawl();
    }

    /**
     * Controls storing all files involved in a job.  The files are
     *  1) The actual ARC files,
     *  2) The metadata files
     *  The crawl.log is parsed and information for each domain is generated
     *  and stored in a AbstractHarvestReport object which
     *  is sent along in the crawlstatusmessage.
     *
     * Additionally, any leftover open ARC files are closed and harvest
     * documentation is extracted before upload starts.
     *
     * @param files The HeritrixFiles object for this crawl. Not Null.
     * @param errorMessage A place where error messages accumulate. Not Null.
     * @param failedFiles  List of files that failed to upload. Not Null.
     * @return An object containing info about the domains harvested.
     * @throws ArgumentNotValid if an argument isn't valid.
     */
    public HarvestReport storeFiles(
            HeritrixFiles files, StringBuilder errorMessage,
            List<File> failedFiles) throws ArgumentNotValid {
        ArgumentNotValid.checkNotNull(files, "HeritrixFiles files");
        ArgumentNotValid.checkNotNull(errorMessage,
                "StringBuilder errorMessage");
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

            // Check, if arcsdir is empty
            // Send a notification, if this is the case
            if (inf.getArcFiles().isEmpty()) {
                String errMsg = "Probable error in Heritrix job setup. "
                        + "No arcfiles generated by Heritrix for job " +  jobID;
                log.warn(errMsg);
                NotificationsFactory.getInstance().errorEvent(errMsg);
            } else {
                uploadFiles(inf.getArcFiles(), errorMessage, failedFiles);
            }

            uploadFiles(inf.getMetadataArcFiles(), errorMessage, failedFiles);

            // Make the harvestReport ready for uploading
            return HarvestReportFactory.generateHarvestReport(files);

        } catch (IOFailure e) {
            String errMsg = "IOFailure occurred, while trying to upload files";
            log.warn(errMsg, e);
            throw new IOFailure(errMsg, e);
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
        // Upload all archive files
        if (files != null) {
            for (File f : files) {
                try {
                    log.info("Uploading file '" + f.getName()
                             + "' to arcrepository.");
                    arcRepController.store(f);
                    log.info("File '" + f.getName()
                            + "' uploaded successfully to arcrepository.");
                } catch (Exception e) {
                    File oldJobsDir
                            = new File(Settings.get(
                            HarvesterSettings.HARVEST_CONTROLLER_OLDJOBSDIR));
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
     * @param metadataEntries list of metadataEntries.
     * @return the list of jobs for deduplicate reduction.
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
                        result.add(Long.parseLong(stringLong));
                    } catch (NumberFormatException e) {
                        log.warn("Unable to convert String '" + stringLong
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
     * @return a directory  containing the index itself.
     * @throws IOFailure on errors retrieving the index from the client.
     * TODO Better forgiving handling of no index available
     */
    private File fetchDeduplicateIndex(List<MetadataEntry> metadataEntries) {
        // Get list of jobs, which should be used for duplicate reduction
        // and retrieve a luceneIndex from the IndexServer
        // based on the crawl.logs from these jobs and their CDX'es.
        HashSet<Long> jobIDsForDuplicateReduction = new HashSet<Long>(
                parseJobIDsForDuplicateReduction(metadataEntries));

        // The client for requesting job index.
        JobIndexCache jobIndexCache
                = IndexClientFactory.getDedupCrawllogInstance();

        // Request the index and return the index file.
        Index<Set<Long>> jobIndex = jobIndexCache.getIndex(
                jobIDsForDuplicateReduction);
        return jobIndex.getIndexFile();
    }
    
    /**
     * Submit a batch job to generate cdx for all metadata files for a job, and
     * report result in a list.
     * @param jobid The job to get cdx for.
     * @return A list of cdx records.
     * @throws ArgumentNotValid If jobid is 0 or negative.
     * @throws IOFailure On trouble generating the cdx
     */
    public static List<CDXRecord> getMetadataCDXRecordsForJob(long jobid) {
        ArgumentNotValid.checkPositive(jobid, "jobid");
        FileBatchJob cdxJob = new ExtractCDXJob(false);
        cdxJob.processOnlyFilesMatching(jobid + "-metadata-[0-9]+\\.arc(\\.gz)?");
        File f;
        try {
            f = File.createTempFile(jobid + "-reports", ".cdx",
                                    FileUtils.getTempDir());
        } catch (IOException e) {
            throw new IOFailure("Could not create temporary file", e);
        }
        BatchStatus status
                = ArcRepositoryClientFactory.getViewerInstance().batch(
                cdxJob, Settings.get(CommonSettings.USE_REPLICA_ID));
        status.getResultFile().copyTo(f);
        List<CDXRecord> records;
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(f));
            records = new ArrayList<CDXRecord>();
            for (String line = reader.readLine();
                 line != null; line = reader.readLine()) {
                String[] parts = line.split("\\s+");
                CDXRecord record = new CDXRecord(parts);
                records.add(record);
            }
        } catch (IOException e) {
            throw new IOFailure("Unable to read results from file '" + f
                                + "'", e);
        } finally {
            IOUtils.closeQuietly(reader);
            FileUtils.remove(f);
        }
        return records;
    }

}
