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
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.distribute.JMSConnection;
import dk.netarkivet.common.distribute.arcrepository.ArcRepositoryClientFactory;
import dk.netarkivet.common.distribute.arcrepository.HarvesterArcRepositoryClient;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.ExceptionUtils;
import dk.netarkivet.common.utils.NotificationType;
import dk.netarkivet.common.utils.NotificationsFactory;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.harvester.HarvesterSettings;
import dk.netarkivet.harvester.datamodel.JobStatus;
import dk.netarkivet.harvester.harvesting.PersistentJobData;
import dk.netarkivet.harvester.harvesting.distribute.CrawlStatusMessage;
import dk.netarkivet.harvester.harvesting.report.DomainStatsReport;
import dk.netarkivet.harvester.harvesting.report.HarvestReport;
import dk.netarkivet.harvester.heritrix3.report.HarvestReportFactory;
import dk.netarkivet.harvester.heritrix3.report.HarvestReportGenerator;

public class PostProcessing {

    /** The logger to use. */
    private static final Logger log = LoggerFactory.getLogger(PostProcessing.class);

    /** The max time to wait for heritrix to close last ARC or WARC files (in secs). */
    private static final int WAIT_FOR_HERITRIX_TIMEOUT_SECS = 5;

    /** The JMSConnection to use. */
    private JMSConnection jmsConnection;

    /** The ArcRepositoryClient used to communicate with the ArcRepository to store the generated arc-files. */
    private HarvesterArcRepositoryClient arcRepController;

    /** The singleton instance of this class. Calling cleanup() on the instance will null this field. */
    private static PostProcessing instance;

    /**
     * Private constructor controlled by getInstance().
     */
    private PostProcessing(JMSConnection jmsConnection) {
        arcRepController = ArcRepositoryClientFactory.getHarvesterInstance();
        this.jmsConnection = jmsConnection;
    }

    /**
     * Get the instance of the singleton HarvestController.
     *
     * @return The singleton instance.
     */
    public static synchronized PostProcessing getInstance(JMSConnection jmsConnection) {
        if (instance == null) {
            instance = new PostProcessing(jmsConnection);
        }
        return instance;
    }

    /**
     * Clean up this singleton, releasing the ArcRepositoryClient and removing the instance. This instance should not be
     * used after this method has been called. After this has been called, new calls to getInstance will return a new
     * instance.
     */
    public void cleanup() {
        if (arcRepController != null) {
            arcRepController.close();
        }
        resetInstance();
    }

    /**
     * Reset the singleton instance.
     */
    private static void resetInstance() {
        instance = null;
    }

    /**
     * Looks for old job directories that await uploading of data.
     * The existence of the harvestInfo.xml in the 
     */
    public void processOldJobs() {
        // Search through all crawldirs and process PersistentJobData
        // files in them
        File crawlDir = new File(Settings.get(HarvesterSettings.HARVEST_CONTROLLER_SERVERDIR));
        log.info("Looking for unprocessed crawldata in '{}'",crawlDir );
        File[] subdirs = crawlDir.listFiles();
        for (File oldCrawlDir : subdirs) {
            if (PersistentJobData.existsIn(oldCrawlDir)) {
                // Assume that crawl had not ended at this point so
                // job must be marked as failed
                final String msg = "Found old unprocessed job data in dir '" + oldCrawlDir.getAbsolutePath()
                        + "'. Crawl probably interrupted by " + "shutdown of HarvestController. " + "Processing data.";
                log.warn(msg);
                NotificationsFactory.getInstance().notify(msg, NotificationType.WARNING);
                doPostProcessing(oldCrawlDir, new IOFailure("Crawl probably interrupted by "
                        + "shutdown of HarvestController"));
            }
        }
    }

    /**
     * Do postprocessing of data in a crawldir.</br>
     * 1. Retrieve jobID, and crawlDir from the harvestInfoFile using class PersistentJobData</br>
     * 2. finds JobId and arcsdir</br> 
     * 3. calls storeArcFiles</br> 
     * 4. moves harvestdir to oldjobs and deletes crawl.log and other superfluous files.
     *
     * @param crawlDir The location of harvest-info to be processed
     * @param crawlException any exceptions thrown by the crawl which need to be reported back to the scheduler (may be
     * null for success)
     * @throws IOFailure if the harvestInfo.xml file cannot be read
     */
    public void doPostProcessing(File crawlDir, Throwable crawlException) throws IOFailure {
    	File harvestInfoFile = PersistentJobData.getHarvestInfoFile(crawlDir);
        log.debug("Post-processing files in directory '{}' based on the harvestInfofile '{}'", crawlDir.getAbsolutePath(), harvestInfoFile);
        
        if (!harvestInfoFile.exists()) {
            throw new IOFailure("Critical error: No '" + harvestInfoFile.getName() + "' found in directory: '" + crawlDir.getAbsolutePath() + "'");
        }

        PersistentJobData harvestInfo = new PersistentJobData(crawlDir);
        Long jobID = harvestInfo.getJobID();

        StringBuilder errorMessage = new StringBuilder();
        HarvestReport dhr = null;
        List<File> failedFiles = new ArrayList<File>();

        Heritrix3Files files = Heritrix3Files.getH3HeritrixFiles(crawlDir, harvestInfo);
        
        try {
            log.info("Store files in directory '{}' " + "from jobID: {}.", crawlDir, jobID);
            dhr = storeFiles(files, errorMessage, failedFiles);
        } catch (Exception e) {
            String msg = "Trouble occurred during postprocessing (including upload of files) in '" + crawlDir.getAbsolutePath() + "'";
            log.warn(msg, e);
            errorMessage.append(e.getMessage()).append("\n");
            // send a mail about this problem
            NotificationsFactory.getInstance().notify(
                    msg + ". Errors accumulated during the postprocessing: " + errorMessage.toString(),
                    NotificationType.ERROR, e);
        } finally {
            // Send a done or failed message back to harvest scheduler
            // FindBugs claims a load of known null value here, but that
            // will not be the case if storeFiles() succeeds.
            CrawlStatusMessage csm;

            if (crawlException == null && errorMessage.length() == 0) {
                log.info("Job with ID {} finished with status DONE", jobID);
                csm = new CrawlStatusMessage(jobID, JobStatus.DONE, dhr);
            } else {
                log.warn("Job with ID {} finished with status FAILED", jobID);
                csm = new CrawlStatusMessage(jobID, JobStatus.FAILED, dhr);
                setErrorMessages(csm, crawlException, errorMessage.toString(), dhr == null, failedFiles.size());
            }
            
            try { // TODO What kind of errors are we actually catching here if any
            	if (jmsConnection != null) {
            		jmsConnection.send(csm); 
            	} else {
            		log.error("CrawlStatusMessage was not sent, as jmsConnection variable was null!");
            	}
            	if (crawlException == null && errorMessage.length() == 0) { // and the message is sent without throwing an exception
            		log.info("Deleting crawl.log and progressstatistics.log for job {} ", jobID);
            		files.deleteFinalLogs();
            	}
            } finally {
                // Delete superfluous files and move the rest to oldjobs.
                // Cleanup is in an extra finally, because it consists of large amounts
                // of data we need to remove, even on send trouble.
            	File oldJobsdir = new File(Settings.get(HarvesterSettings.HARVEST_CONTROLLER_OLDJOBSDIR));
                log.info("Now doing cleanup after harvesting job with id '{}' and moving the rest of the job to oldjobsdir '{}' ", jobID, oldJobsdir);
                files.cleanUpAfterHarvest(oldJobsdir);
            }
        }
        log.info("Done post-processing files for job {} in dir: '{}'", jobID, crawlDir.getAbsolutePath());
    }

    /**
     * Adds error messages from an exception to the status message errors.
     *
     * @param csm The message we're setting messages on.
     * @param crawlException The exception that got thrown from further in, possibly as far in as Heritrix.
     * @param errorMessage Description of errors that happened during upload.
     * @param missingHostsReport If true, no hosts report was found.
     * @param failedFiles List of files that failed to upload.
     */
    private void setErrorMessages(CrawlStatusMessage csm, Throwable crawlException, String errorMessage,
            boolean missingHostsReport, int failedFiles) {
        if (crawlException != null) {
            csm.setHarvestErrors(crawlException.toString());
            csm.setHarvestErrorDetails(ExceptionUtils.getStackTrace(crawlException));
        }
        if (errorMessage.length() > 0) {
            String shortDesc = "";
            if (missingHostsReport) {
                shortDesc = "No hosts report found";
            }
            if (failedFiles > 0) {
                if (shortDesc.length() > 0) {
                    shortDesc += ", ";
                }
                shortDesc += failedFiles + " files failed to upload";
            }
            csm.setUploadErrors(shortDesc);
            csm.setUploadErrorDetails(errorMessage);
        }
    }

    /**
     * Controls storing all files involved in a job. The files are 1) The actual ARC/WARC files, 2) The metadata files
     * The crawl.log is parsed and information for each domain is generated and stored in a AbstractHarvestReport object
     * which is sent along in the crawlstatusmessage.
     * <p>
     * Additionally, any leftover open ARC files are closed and harvest documentation is extracted before upload starts.
     *
     * @param files The HeritrixFiles object for this crawl. Not Null.
     * @param errorMessage A place where error messages accumulate. Not Null.
     * @param failedFiles List of files that failed to upload. Not Null.
     * @return An object containing info about the domains harvested.
     * @throws ArgumentNotValid if an argument isn't valid.
     */
    private HarvestReport storeFiles(Heritrix3Files files, StringBuilder errorMessage, List<File> failedFiles)
            throws ArgumentNotValid {
        ArgumentNotValid.checkNotNull(files, "Heritrix3Files files");
        ArgumentNotValid.checkNotNull(errorMessage, "StringBuilder errorMessage");
        ArgumentNotValid.checkNotNull(failedFiles, "List<File> failedFiles");
        long jobID = files.getJobID();
        log.info("Store the files from harvest in '{}'", files.getCrawlDir());
        try {
            IngestableFiles inf = new IngestableFiles(files);

            inf.closeOpenFiles(WAIT_FOR_HERITRIX_TIMEOUT_SECS);
            // Create a metadata archive file
            HarvestDocumentation.documentHarvest(inf);
            // Upload all files 

            // Check, if arcsdir or warcsdir is empty
            // Send a notification, if this is the case
            if (inf.getArcFiles().isEmpty() && inf.getWarcFiles().isEmpty()) {
                String errMsg = "Probable error in Heritrix job setup. "
                        + "No arcfiles or warcfiles generated by Heritrix for job " + jobID;
                log.warn(errMsg);
                NotificationsFactory.getInstance().notify(errMsg, NotificationType.WARNING);
            } else {
                if (!inf.getArcFiles().isEmpty()) {
                	log.info("Beginning upload of {} ARC files", inf.getArcFiles().size());
                    uploadFiles(inf.getArcFiles(), errorMessage, failedFiles);
                }
                if (!inf.getWarcFiles().isEmpty()) {
                	log.info("Beginning upload of {} WARC files", inf.getWarcFiles().size());
                    uploadFiles(inf.getWarcFiles(), errorMessage, failedFiles);
                }
            }

            // Now the ARC/WARC files have been uploaded,
            // we finally upload the metadata archive file.
            log.info("Beginning upload of the {} metadafile(s) ", inf.getMetadataArcFiles().size());
            uploadFiles(inf.getMetadataArcFiles(), errorMessage, failedFiles);
            
            // Make the harvestReport ready for transfer back to the scheduler 
            DomainStatsReport dsr =  HarvestReportGenerator.getDomainStatsReport(files);
            		 
            return HarvestReportFactory.generateHarvestReport(dsr);
        } catch (IOFailure e) {
            String errMsg = "IOFailure occurred, while trying to upload files";
            log.warn(errMsg, e);
            throw new IOFailure(errMsg, e);
        }
    }

    /**
     * Upload given files to the archive repository.
     *
     * @param files List of (ARC/WARC) files to upload.
     * @param errorMessage Accumulator for error messages.
     * @param failedFiles Accumulator for failed files.
     */
    private void uploadFiles(List<File> files, StringBuilder errorMessage, List<File> failedFiles) {
        // Upload all archive files
        if (files != null) {
        	int count=0;
            for (File f : files) {
            	count++;
                try {
                    log.info("Uploading file #{} - '{}' to arcrepository.", count, f.getName());
                    arcRepController.store(f);
                    log.info("File '{}' uploaded successfully to the arcrepository.", f.getName());
                } catch (Exception e) {
                    File oldJobsDir = new File(Settings.get(HarvesterSettings.HARVEST_CONTROLLER_OLDJOBSDIR));
                    String errorMsg = "Error uploading file '" + f.getAbsolutePath() + "' Will be moved to the oldjobs directory '"
                            + oldJobsDir.getAbsolutePath() + "'";
                    errorMessage.append(errorMsg).append("\n").append(e.toString()).append("\n");
                    log.warn(errorMsg, e);
                    failedFiles.add(f);
                }
            }
        }
    }

}
