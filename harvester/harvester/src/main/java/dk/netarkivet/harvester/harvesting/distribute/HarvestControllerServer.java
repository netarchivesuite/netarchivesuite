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
package dk.netarkivet.harvester.harvesting.distribute;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.common.Constants;
import dk.netarkivet.common.distribute.ChannelID;
import dk.netarkivet.common.distribute.Channels;
import dk.netarkivet.common.distribute.JMSConnection;
import dk.netarkivet.common.distribute.JMSConnectionFactory;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.PermissionDenied;
import dk.netarkivet.common.exceptions.UnknownID;
import dk.netarkivet.common.utils.ApplicationUtils;
import dk.netarkivet.common.utils.CleanupIF;
import dk.netarkivet.common.utils.ExceptionUtils;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.NotificationsFactory;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.harvester.HarvesterSettings;
import dk.netarkivet.harvester.datamodel.Job;
import dk.netarkivet.harvester.datamodel.JobPriority;
import dk.netarkivet.harvester.datamodel.JobStatus;
import dk.netarkivet.harvester.distribute.HarvesterMessageHandler;
import dk.netarkivet.harvester.harvesting.HarvestController;
import dk.netarkivet.harvester.harvesting.HeritrixFiles;

/**
 * This class responds to JMS doOneCrawl messages from the HarvestScheduler and
 * launches a Heritrix crawl with the received job description. The generated
 * ARC files are uploaded to the bitarchives once a harvest job has been
 * completed.
 * 
 * During its operation CrawlStatus messages are sent to the
 * HarvestSchedulerMonitorServer. When starting the actual harvesting a message
 * is sent with status 'STARTED'. When the harvesting has finished a message is
 * sent with either status 'DONE' or 'FAILED'. Either a 'DONE' or 'FAILED'
 * message with result should ALWAYS be sent if at all possible, but only ever
 * one such message per job.
 * 
 * It is necessary to be able to run the Heritrix harvester on several machines
 * and several processes on each machine. Each instance of Heritrix is started
 * and monitored by a HarvestControllerServer.
 * 
 * Initially, all directories under serverdir are scanned for harvestinfo files.
 * If any are found, they are parsed for information, and all remaining files
 * are attempted uploaded to the bitarchive. It will then send back a
 * crawlstatusmessage with status failed.
 * 
 * A new thread is started for each actual crawl, in which the JMS listener is
 * removed. Threading is required since JMS will not let the called thread
 * remove the listener that's being handled.
 * 
 * After a harvestjob has been terminated, either successfully or
 * unsuccessfully, the serverdir is again scanned for harvestInfo files to
 * attempt upload of files not yet uploaded. Then it begins to listen again
 * after new jobs, if there is enough room available on the machine. If not, it
 * logs a warning about this, which is also sent as a notification.
 */
public class HarvestControllerServer extends HarvesterMessageHandler
        implements CleanupIF {
    /** The unique instance of this class. */
    private static HarvestControllerServer instance;

    /** The logger to use. */
    private static final Log log
            = LogFactory.getLog(HarvestControllerServer.class);

    /** The message to write to log when starting the server. */
    private static final String STARTING_MESSAGE =
                "Starting HarvestControllerServer.";
    /** The message to write to log when server is started. */
    private static final String STARTED_MESSAGE =
                "HarvestControllerServer started.";    
    /** The message to write to log when stopping the server. */
    private static final String CLOSING_MESSAGE =
                "Closing HarvestControllerServer.";
    /** The message to write to log when server is stopped. */
    private static final String CLOSED_MESSAGE
            = "Closed down HarvestControllerServer";
    /** The message to write to log whan starting a crawl. */
    private static final String STARTCRAWL_MESSAGE = "Starting crawl of job :";
    /** The message to write to log after ending a crawl. */
    private static final String ENDCRAWL_MESSAGE = "Ending crawl of job :";
    /** The max time to wait for the hosts-report.txt to
     * be available (in secs). */
    static final int WAIT_FOR_HOSTS_REPORT_TIMEOUT_SECS = 30;
    /** Heritrix version property. */
    private static final String HERITRIX_VERSION_PROPERTY = "heritrix.version";
    /** queue-assignment-policy property. */
    private static final String HERITRIX_QUEUE_ASSIGNMENT_POLICY_PROPERTY =
        "org.archive.crawler.frontier.AbstractFrontier.queue-assignment-policy";
    /** The JMSConnection to use. */
    private JMSConnection jmsConnection;

    /** The (singleton) HarvestController that handles the non-JMS parts of
     * a harvest.
     */
    private final HarvestController controller;

    /** This is true while a doOneCrawl is running. No jobs are accepted while
     * this boolean is true. */
    private boolean running = false;
    /** Jobs are fetched from this queue. */ 
    private final ChannelID jobChannel;
    
    /** Min. space required to start a job. */
    private final long minSpaceRequired;
    
    /** the serverdir, where the harvesting takes place. */
    private final File serverDir;
    
    /**
     * In this constructor, the server creates an instance of the
     * HarvestController, uploads any arc-files from incomplete harvests.
     * Then it starts listening for new doOneCrawl messages, unless there
     * is no available space. In that case, it sends a notification to
     * the administrator and pauses.
     *
     * @throws PermissionDenied
     *             If the serverdir or oldjobsdir can't be created.
     * @throws IOFailure
     *             If harvestInfoFile contains invalid data.
     * @throws UnknownID if the settings file does not specify a valid queue
     * priority.
     */
    private HarvestControllerServer() throws IOFailure {
        log.info(STARTING_MESSAGE);

        // Make sure serverdir (where active crawl-dirs live) and oldJobsDir
        // (where old crawl dirs are stored) exist.
        serverDir = new File(Settings
                .get(HarvesterSettings.HARVEST_CONTROLLER_SERVERDIR));
        ApplicationUtils.dirMustExist(serverDir);
        log.info("Serverdir: '" + serverDir + "'");
        minSpaceRequired = Settings
                .getLong(HarvesterSettings.HARVEST_SERVERDIR_MINSPACE);
        if (minSpaceRequired <= 0L) {
            log.warn("Wrong setting of minSpaceLeft read from Settings: "
                    + minSpaceRequired);
            throw new ArgumentNotValid(
                    "Wrong setting of minSpaceLeft read from Settings: "
                            + minSpaceRequired);
        }
        log.info("Harvesting requires at least " + minSpaceRequired
                + " bytes free.");

        controller = HarvestController.getInstance();

        // Set properties "heritrix.version" and
        // "org.archive.crawler.frontier.AbstractFrontier
        //  .queue-assignment-policy"
        System.setProperty(HERITRIX_VERSION_PROPERTY,
                Constants.getHeritrixVersionString());
        System.setProperty(
                HERITRIX_QUEUE_ASSIGNMENT_POLICY_PROPERTY,
                "org.archive.crawler.frontier.HostnameQueueAssignmentPolicy,"
                + "org.archive.crawler.frontier.IPQueueAssignmentPolicy,"
                + "org.archive.crawler.frontier.BucketQueueAssignmentPolicy,"
                + "org.archive.crawler.frontier"
                    + ".SurtAuthorityQueueAssignmentPolicy,"
                + "dk.netarkivet.harvester.harvesting"
                    + ".DomainnameQueueAssignmentPolicy");
        // Get JMS-connection
        // Channel THIS_CLIENT is only used for replies to store messages so
        // do not set as listener here. It is registered in the arcrepository
        // client.
        // Channel ANY_xxxPRIORIRY_HACO is used for listening for jobs, and
        // registered below.
        jmsConnection = JMSConnectionFactory.getInstance();
        log.debug("Obtained JMS connection.");

        // If any unprocessed jobs are left on the server, process them now
        processOldJobs();

        // Environment and connections are now ready for processing of messages
        JobPriority p = JobPriority.valueOf(Settings
                .get(HarvesterSettings.HARVEST_CONTROLLER_PRIORITY));
        switch (p) {
        case HIGHPRIORITY:
            jobChannel = Channels.getAnyHighpriorityHaco();
            break;
        case LOWPRIORITY:
            jobChannel = Channels.getAnyLowpriorityHaco();
            break;
        default:
            throw new UnknownID(p + " is not a valid priority");
        }
        // Only listen for harvesterjobs if enough available space
        beginListeningIfSpaceAvailable();
    }

    /**
     * Returns or creates the unique instance of this singleton
     * The server creates an instance of the HarvestController,
     * uploads arc-files from unfinished harvests, and
     * starts to listen to JMS messages on the incoming jms queues.
     *
     * @return The instance
     * @throws PermissionDenied
     *             If the serverdir or oldjobsdir can't be created
     * @throws IOFailure
     *             if data from old harvests exist, but contain illegal data
     */
    public static synchronized HarvestControllerServer getInstance()
            throws IOFailure {
        if (instance == null) {
            instance = new HarvestControllerServer();
        }
        return instance;
    }

    /**
     * Release all jms connections. Close the Controller
     */
    public synchronized void close() {
        log.info(CLOSING_MESSAGE);
        cleanup();
        log.info(CLOSED_MESSAGE);
    }

    /**
     * Will be called on shutdown.
     * @see CleanupIF#cleanup()
     */
    public void cleanup() {
        if (controller != null) {
            controller.cleanup();
        }
        if (jmsConnection != null) {
            jmsConnection.removeListener(jobChannel, this);
        }
        instance = null;
    }

    /** 
     * Looks for old job directories that await uploading.
     */
    private void processOldJobs() {
        //Search through all crawldirs and process PersistentJobData
        // files in them
        File crawlDir = new File(Settings.get(
                HarvesterSettings.HARVEST_CONTROLLER_SERVERDIR));
        File[] subdirs = crawlDir.listFiles();
        for (File oldCrawlDir : subdirs) {
            if (PersistentJobData.existsIn(oldCrawlDir)) {
                // Assume that crawl had not ended at this point so
                // job must be marked as failed
                log.warn("Found old unprocessed job data in dir '"
                             + oldCrawlDir.getAbsolutePath()
                             + "'. Crawl probably interrupted by "
                             + "shutdown of HarvestController. "
                             + "Processing data.");
                //TODO Does such an unexpected job warrant an email?
                processHarvestInfoFile(oldCrawlDir,
                        new IOFailure("Crawl probably interrupted by "
                                      + "shutdown of HarvestController"));
            }
        }
    }

    /**
     * Checks that we're available to do a crawl, and if so, marks us as
     * unavailable, checks that the job message is well-formed, and starts
     * the thread that the crawl happens in.  If an error occurs starting the
     * crawl, we will start listening for messages again.
     *
     * The sequence of actions involved in a crawl are:</br>
     * 1. If we are already running, resend the job to the queue and return</br>
     * 2. Check the job for validity</br>
     * 3. Send a CrawlStatus message that crawl has STARTED</br>
     * In a separate thread:</br>
     * 4. Unregister this HACO as listener</br>
     * 5. Create a new crawldir (based on the JobID and a timestamp)</br>
     * 6. Write a harvestInfoFile (using JobID and crawldir) and metadata</br>
     * 7. Instantiate a new HeritrixLauncher</br>
     * 8. Start a crawl</br>
     * 9. Store the generated arc-files and metadata in the known bit-archives
     * </br>10. _Always_ send CrawlStatus DONE or FAILED</br>
     * 11. Move crawldir into oldJobs dir</br>

     * @see #visit(DoOneCrawlMessage) for more details
     * @param msg The crawl job
     * @throws IOFailure On trouble harvesting, uploading or
     * processing harvestInfo
     * @throws UnknownID if jobID is null in the message
     * @throws ArgumentNotValid
     *             if the status of the job is not valid - must be SUBMITTED
     * @throws PermissionDenied
     */
    private void onDoOneCrawl(final DoOneCrawlMessage msg)
            throws IOFailure, UnknownID, ArgumentNotValid, PermissionDenied {
        // Only one doOneCrawl at a time. Returning should almost never happen,
        // since we deregister the listener, but we may receive another message
        // before the listener is removed.  Also, if the job message is
        // malformed or starting the crawl fails, we re-add the listener.
        synchronized (this) {
            if (running) {
                log.warn("Received crawl request, but sent it back to queue, "
                         + "as another crawl is already running: '" + msg
                         + "'");
                jmsConnection.resend(msg, jobChannel);
                try {
                    // Wait a second before listening again, so the message has
                    // a chance of getting snatched by another harvester.
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    // Since we're not waiting for anything critical, we can
                    // ignore this exception.
                }
                return;
            }
            stopAcceptingJobs();
        }

        Thread t = null;

        //This 'try' matches a finally that restores running=false if we don't
        //start a crawl after all
        try {
            final Job job = msg.getJob();

            // Every job must have an ID or we can do nothing with it, not even
            // send a proper failure message back.
            Long jobID = job.getJobID();
            if (jobID == null) {
                log.warn("DoOneCrawlMessage arrived without JobID: '"
                        + msg.toString() + "'");
                throw new UnknownID("DoOneCrawlMessage arrived without JobID");
            }

            log.info("Received crawlrequest for job "
                        + jobID + ": '" + msg + "'");

            // Send message to scheduler that job is started
            CrawlStatusMessage csmStarted = new CrawlStatusMessage(
                    jobID, JobStatus.STARTED);
            jmsConnection.send(csmStarted);

            // Jobs should arrive with status "submitted". If this is not the
            // case, log the error and send a job-failed message back.
            // HarvestScheduler likes getting a STARTED message before
            // SUBMITTED, so we oblige it here.
            if (job.getStatus() != JobStatus.SUBMITTED) {
                String message = "Message '" + msg.toString() + "' arrived with"
                        + " status " + job.getStatus()
                                     + " for job " + jobID
                        + ", should have been STATUS_SUBMITTED";
                log.warn(message);
                sendErrorMessage(jobID, message, message);
                throw new ArgumentNotValid(message);
            }

            final List<MetadataEntry> metadataEntries = msg.getMetadata();

            Thread t1;
            // Create thread in which harvesting will occur
            t1 = new HarvesterThread(job, metadataEntries);
            // start thread which will remove this listener, harvest, store, and
            // exit the VM
            t1.start();
            log.info("Started harvester thread for job " + jobID);
            // We delay assigning the thread variable until start() has
            // succeeded.  Thus, if start() fails, we will resume accepting
            // jobs.
            t = t1;
        } finally {
            // If we didn't start a thread for crawling after all, accept more
            // messages
            if (t == null) {
                resumeAcceptingJobs();
            }
        }
        // Now return from this method letting the thread do the work.
        // This is important as it allows us to receive upload-replies from
        // THIS_CLIENT in the crawl thread.
    }

    /** Sends a CrawlStatusMessage for a failed job with the given short message
     * and detailed message.
     *
     * @param jobID ID of the job that failed
     * @param message A short message indicating what went wrong
     * @param detailedMessage A more detailed message detailing why it went
     * wrong.
     */
    private void sendErrorMessage(long jobID, String message,
                                  String detailedMessage) {
        CrawlStatusMessage csm = new CrawlStatusMessage(jobID, JobStatus.FAILED,
                null);
        csm.setHarvestErrors(message);
        csm.setHarvestErrorDetails(detailedMessage);
        jmsConnection.send(csm);
    }

    /** Stop accepting more jobs.  After this is called, all crawl messages
     * received will be resent to the queue.  A bit further down, we will stop
     * listening altogether, but that requires another thread.
     */
    private synchronized void stopAcceptingJobs() {
        running = true;
        log.debug("No longer accepting jobs.");
    }

    /** Start listening for new crawl requests again.  This actually doesn't
     * re-add a listener, but the listener only gets removed when we're so
     * far committed that we're going to exit at the end.  So to start accepting
     * jobs again, we stop resending messages we get.
     *
     */
    private synchronized void resumeAcceptingJobs() {
        //allow this haco to receive messages
        running = false;
        log.debug("Starting to accept jobs again.");
    }

    /** Stop listening for new crawl requests.
     *
     */
    private void removeListener() {
        log.debug("Removing listener on channel '" + jobChannel + "'");
        jmsConnection.removeListener(jobChannel, this);
    }

    /** Start listening for crawls, if space available. */
    private void beginListeningIfSpaceAvailable() {
        long availableSpace = FileUtils.getBytesFree(serverDir);
        if (availableSpace > minSpaceRequired) {
            log.info("Starts to listen to new jobs on queue '"
                    + jobChannel + "'");
            jmsConnection.setListener(jobChannel, this);
            log.info(STARTED_MESSAGE);
        } else {
            String PAUSED_MESSAGE = "Not enough available diskspace. Only "
                    + availableSpace + " bytes available. Harvester is paused.";
            log.warn(PAUSED_MESSAGE);
            NotificationsFactory.getInstance().errorEvent(PAUSED_MESSAGE);
        }
    }
    
    /** Adds error messages from an exception to the status message errors.
     *
     * @param csm The message we're setting messages on.
     * @param crawlException The exception that got thrown from further in,
     * possibly as far in as Heritrix.
     * @param errorMessage Description of errors that happened during upload.
     * @param missingHostsReport If true, no hosts report was found.
     * @param failedFiles List of files that failed to upload.
     */
    private void setErrorMessages(CrawlStatusMessage csm,
                                  Throwable crawlException,
                                  String errorMessage,
                                  boolean missingHostsReport,
                                  int failedFiles) {
        if (crawlException != null) {
            csm.setHarvestErrors(crawlException.toString());
            csm.setHarvestErrorDetails(
                    ExceptionUtils.getStackTrace(crawlException));
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
     * Receives a DoOneCrawlMessage and call onDoOneCrawl.
     *
     * @param msg the message received
     * @throws IOFailure
     *             if the crawl fails
     *             if unable to write to harvestInfoFile
     * @throws UnknownID
     *             if jobID is null in the message
     * @throws ArgumentNotValid
     *             if the status of the job is not valid - must be
     *             SUBMITTED
     * @throws PermissionDenied
     *             if the crawldir can't be created
     */
    public void visit(DoOneCrawlMessage msg)
            throws IOFailure, UnknownID, ArgumentNotValid, PermissionDenied {
        onDoOneCrawl(msg);
    }


    /**
     * Processes an existing harvestInfoFile:</br>
     * 1. Retrieve jobID, and crawlDir from the harvestInfoFile
     *      using class PersistentJobData</br>
     * 2. finds JobId and arcsdir</br>
     * 3. calls storeArcFiles</br>
     * 4. moves harvestdir to oldjobs and deletes crawl.log and
     *  other superfluous files.
     *
     * @param crawlDir The location of harvest-info to be processed
     * @param crawlException any exceptions thrown by the crawl which need to
     * be reported back to the scheduler (may be null for success)
     * @throws IOFailure if the file cannot be read
     */
    private void processHarvestInfoFile(File crawlDir,
            Throwable crawlException)
    throws IOFailure {
        log.debug("Post-processing files in '"
                     + crawlDir.getAbsolutePath() + "'");
        if (!PersistentJobData.existsIn(crawlDir)) {
            throw new IOFailure("No harvestInfo found in directory: "
                    + crawlDir.getAbsolutePath());
        }

        PersistentJobData harvestInfo = new PersistentJobData(crawlDir);
        Long jobID = harvestInfo.getJobID();

        StringBuilder errorMessage = new StringBuilder();
        DomainHarvestReport dhr = null;
        List<File> failedFiles = new ArrayList<File>();

        HeritrixFiles files =
            new HeritrixFiles(
                    crawlDir,
                    jobID,
                    harvestInfo.getOrigHarvestDefinitionID());
        try {
            log.info("Store files in directory '" + crawlDir + "' "
                    + "from jobID: " + jobID + ".");
            dhr = controller.storeFiles(
                    files, errorMessage, failedFiles);
        } catch (Exception e) {
            String msg = "Trouble during postprocessing of files in '"
                + crawlDir.getAbsolutePath() + "'"; 
            log.warn(msg, e);
            errorMessage.append(e.getMessage()).append("\n");
            // send a mail about this problem
            NotificationsFactory.getInstance().errorEvent(msg
                    + ". Errors accumulated during the postprocessing: "
                    + errorMessage.toString(), e);
        } finally {
            // Send a done or failed message back to harvest scheduler
            // FindBugs claims a load of known null value here, but that
            // will not be the case if storeFiles() succeeds.
            CrawlStatusMessage csm;

            if (crawlException == null && errorMessage.length() == 0) {
                log.warn("JobID: " + jobID + " done.");
                csm = new CrawlStatusMessage(jobID, JobStatus.DONE, dhr);
            } else {
                log.warn("JobID: " + jobID + " failed.");
                csm = new CrawlStatusMessage(jobID, JobStatus.FAILED, dhr);
                setErrorMessages(csm, crawlException, errorMessage.toString(),
                        dhr == null, failedFiles.size());
            }
            try {
                jmsConnection.send(csm);
                if (crawlException == null && errorMessage.length() == 0) {
                    files.deleteFinalLogs();
                }
            } finally {
                // Delete superfluous files and move the rest to oldjobs
                // Cleanup is in an extra finally, because it is large amounts
                // of data we need to remove, even on send trouble.
                log.info("Cleanup after harvesting job with id: "
                        + jobID + ".");
                files.cleanUpAfterHarvest(new File(
                        Settings.get(
                                HarvesterSettings.HARVEST_CONTROLLER_OLDJOBSDIR
                                )));
            }
        }
        log.info("Done post-processing files for job " + jobID
                + " in dir: '" + crawlDir.getAbsolutePath() + "'");
    }

    /** A Thread class for the actual harvesting.  This is required
     * in order to stop listening while we're busy harvesting, since JMS
     * doesn't allow the called thread to remove the listener that was
     * called.
     */
    private class HarvesterThread extends Thread {
        /** The harvester Job in this thread. */   
        private final Job job;
        /** The list of metadata associated with this Job. */
        private final List<MetadataEntry> metadataEntries;
        /** Constructor for the HarvesterThread class. 
         * @param job a harvesting job
         * @param metadataEntries metadata associated with the given job
         */
        public HarvesterThread(Job job, List<MetadataEntry> metadataEntries) {
            this.job = job;
            this.metadataEntries = metadataEntries;
        }

        /** The thread body for the harvester thread.  Removes the JMS
         * listener, sets up the files for Heritrix, then passes control
         * to the HarvestController to perform the actual harvest.
         *
         * TODO Get file writing into HarvestController as well
         *       (requires some rearrangement of the message sending)
         * @throws PermissionDenied if we cannot create the crawl directory.
         * @throws IOFailure if there are problems preparing or running the
         * crawl.
         */
        public void run() {
            try {
                // We must remove the listener inside a thread,
                // as JMS doesn't allow us to remove it within the
                // call it made.
                removeListener();

                File crawlDir = createCrawlDir();

                final HeritrixFiles files =
                        controller.writeHarvestFiles(crawlDir, job,
                                                     metadataEntries);

                log.info(STARTCRAWL_MESSAGE + " " + job.getJobID());

                Throwable crawlException = null;
                try {
                    controller.runHarvest(files);
                } catch (Throwable e) {
                    String msg = "Error during crawling. "
                                 + "The crawl may have been only "
                                 + "partially completed.";
                    log.warn(msg, e);
                    crawlException = e;
                    throw new IOFailure(msg, e);
                } finally {
                    // This handles some message sending, so it must live
                    // in HCS for now, but the meat of it should be in
                    // HarvestController
                    // TODO Refactor to be able to move this out.
                    // TODO This may overwrite another exception, since this
                    // may throw exceptions.
                    processHarvestInfoFile(files.getCrawlDir(), crawlException);
                }
            } catch (Throwable e) {
                String msg = "Fatal error while operating job '" + job + "'";
                log.fatal(msg, e);
                NotificationsFactory.getInstance().errorEvent(msg, e);
            } finally {
                log.info(ENDCRAWL_MESSAGE + " " + job.getJobID());
                // process serverdir for files not yet uploaded.
                processOldJobs();
                resumeAcceptingJobs();
                beginListeningIfSpaceAvailable();
            }
        }

        /** Create the crawl dir, but make sure a message is sent if there
         * is a problem.
         *
         * @return The directory that the crawl will take place in.
         * @throws PermissionDenied if the directory cannot be created.
         */
        private File createCrawlDir() {
            // The directory where arcfiles are stored (crawldir in the above
            // description)
            File crawlDir = null;
            // Create the crawldir.  This is done here in order to be able
            // to send a proper message if something goes wrong.
            try {
                File baseCrawlDir = new File(Settings.get(
                        HarvesterSettings.HARVEST_CONTROLLER_SERVERDIR));
                crawlDir = new File(baseCrawlDir,
                                    job.getJobID() + "_"
                                    + System.currentTimeMillis());
                FileUtils.createDir(crawlDir);
                log.info("Created crawl directory: '" + crawlDir + "'");
                return crawlDir;
            } catch (PermissionDenied e) {
                String message = "Couldn't create the directory for job "
                                 + job.getJobID();
                log.warn(message, e);
                sendErrorMessage(job.getJobID(), message,
                                 ExceptionUtils.getStackTrace(e));
                throw e;
            }
        }
    }

}
