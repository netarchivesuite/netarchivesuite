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
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.distribute.ChannelID;
import dk.netarkivet.common.distribute.JMSConnection;
import dk.netarkivet.common.distribute.JMSConnectionFactory;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.PermissionDenied;
import dk.netarkivet.common.exceptions.UnknownID;
import dk.netarkivet.common.lifecycle.PeriodicTaskExecutor;
import dk.netarkivet.common.utils.ApplicationUtils;
import dk.netarkivet.common.utils.CleanupIF;
import dk.netarkivet.common.utils.DomainUtils;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.NotificationType;
import dk.netarkivet.common.utils.NotificationsFactory;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.utils.SystemUtils;
import dk.netarkivet.common.utils.TimeUtils;
import dk.netarkivet.harvester.HarvesterSettings;
import dk.netarkivet.harvester.datamodel.HarvestDefinitionInfo;
import dk.netarkivet.harvester.datamodel.Job;
import dk.netarkivet.harvester.datamodel.JobStatus;
import dk.netarkivet.harvester.distribute.HarvesterChannels;
import dk.netarkivet.harvester.distribute.HarvesterMessageHandler;
import dk.netarkivet.harvester.harvesting.distribute.CrawlStatusMessage;
import dk.netarkivet.harvester.harvesting.distribute.DoOneCrawlMessage;
import dk.netarkivet.harvester.harvesting.distribute.HarvesterReadyMessage;
import dk.netarkivet.harvester.harvesting.distribute.HarvesterRegistrationRequest;
import dk.netarkivet.harvester.harvesting.distribute.HarvesterRegistrationResponse;
import dk.netarkivet.harvester.harvesting.metadata.MetadataEntry;

/**
 * This class responds to JMS doOneCrawl messages from the HarvestScheduler and launches a Heritrix crawl with the
 * received job description. The generated ARC files are uploaded to the bitarchives once a harvest job has been
 * completed.
 * 
 * Initially, the HarvestControllerServer registers its channel with the Scheduler by sending a HarvesterRegistrationRequest and waits for a 
 * positive HarvesterRegistrationResponse that its channel is recognized. 
 * If not recognized by the Scheduler, the HarvestControllerServer will send a notification about this, 
 * and then close down the application.
 * <p>
 * During its operation CrawlStatus messages are sent to the HarvestSchedulerMonitorServer. When starting the actual
 * harvesting a message is sent with status 'STARTED'. When the harvesting has finished a message is sent with either
 * status 'DONE' or 'FAILED'. Either a 'DONE' or 'FAILED' message with result should ALWAYS be sent if at all possible,
 * but only ever one such message per job.
 * While the harvestControllerServer is waiting for the harvesting to finish, it sends HarvesterReadyMessages to the scheduler.
 * The interval between each HarvesterReadyMessage being sent is defined by the setting 'settings.harvester.harvesting.sendReadyDelay'.    
 * 
 * <p>
 * It is necessary to be able to run the Heritrix harvester on several machines and several processes on each machine.
 * Each instance of Heritrix is started and monitored by a HarvestControllerServer.
 * <p>
 * Initially, all directories under serverdir are scanned for harvestinfo files. If any are found, they are parsed for
 * information, and all remaining files are attempted uploaded to the bitarchive. It will then send back a
 * CrawlStatusMessage with status failed.
 * <p>
 * A new thread is started for each actual crawl, in which the JMS listener is removed. Threading is required since JMS
 * will not let the called thread remove the listener that's being handled.
 * <p>
 * After a harvestjob has been terminated, either successfully or unsuccessfully, the serverdir is again scanned for
 * harvestInfo files to attempt upload of files not yet uploaded. Then it begins to listen again after new jobs, if
 * there is enough room available on the machine. If not, it logs a warning about this, which is also sent as a
 * notification.
 */
public class HarvestControllerServer extends HarvesterMessageHandler implements CleanupIF {

    /** The logger to use. */
    private static final Logger log = LoggerFactory.getLogger(HarvestControllerServer.class);

    /** The unique instance of this class. */
    private static HarvestControllerServer instance;

    /** The configured application instance id. @see CommonSettings#APPLICATION_INSTANCE_ID */
    private final String applicationInstanceId = Settings.get(CommonSettings.APPLICATION_INSTANCE_ID);

    /** The name of the server this <code>HarvestControllerServer</code> is running on. */
    private final String physicalServerName = DomainUtils.reduceHostname(SystemUtils.getLocalHostName());

    /** Min. space required to start a job. */
    private final long minSpaceRequired;

    /** The JMSConnection to use. */
    private JMSConnection jmsConnection;

    /** The JMS channel on which to listen for {@link HarvesterRegistrationResponse}s. */
    public static final ChannelID HARVEST_CHAN_VALID_RESP_ID = HarvesterChannels
            .getHarvesterRegistrationResponseChannel();

    private final PostProcessing postProcessing;

    /** The CHANNEL of jobs processed by this instance. */
    private static final String CHANNEL = Settings.get(HarvesterSettings.HARVEST_CONTROLLER_CHANNEL);

    /** Jobs are fetched from this queue. */
    private ChannelID jobChannel;

    /** the serverdir, where the harvesting takes place. */
    private final File serverDir;

    /** This is true while a doOneCrawl is running. No jobs are accepted while this is running. */
    private CrawlStatus status;

    /**
     * Returns or creates the unique instance of this singleton The server creates an instance of the HarvestController,
     * uploads arc-files from unfinished harvests, and starts to listen to JMS messages on the incoming jms queues.
     * @return The instance
     * @throws PermissionDenied If the serverdir or oldjobsdir can't be created
     * @throws IOFailure if data from old harvests exist, but contain illegal data
     */
    public static synchronized HarvestControllerServer getInstance() throws IOFailure {
        if (instance == null) {
            instance = new HarvestControllerServer();
        }
        return instance;
    }

    /**
     * In this constructor, the server creates an instance of the HarvestController, uploads any arc-files from
     * incomplete harvests. Then it starts listening for new doOneCrawl messages, unless there is no available space. In
     * that case, it sends a notification to the administrator and pauses.
     * @throws PermissionDenied If the serverdir or oldjobsdir can't be created.
     * @throws IOFailure If harvestInfoFile contains invalid data.
     * @throws UnknownID if the settings file does not specify a valid queue priority.
     */
    private HarvestControllerServer() throws IOFailure {
        log.info("Starting HarvestControllerServer.");
        log.info("Bound to harvest channel '{}'", CHANNEL);

        // Make sure serverdir (where active crawl-dirs live) and oldJobsDir
        // (where old crawl dirs are stored) exist.
        serverDir = new File(Settings.get(HarvesterSettings.HARVEST_CONTROLLER_SERVERDIR));
        ApplicationUtils.dirMustExist(serverDir);
        log.info("Serverdir: '{}'", serverDir);
        minSpaceRequired = Settings.getLong(HarvesterSettings.HARVEST_SERVERDIR_MINSPACE);
        if (minSpaceRequired <= 0L) {
            log.warn("Wrong setting of minSpaceLeft read from Settings: {}", minSpaceRequired);
            throw new ArgumentNotValid("Wrong setting of minSpaceLeft read from Settings: " + minSpaceRequired);
        }
        log.info("Harvesting requires at least {} bytes free.", minSpaceRequired);
        
        // If shutdown.txt found in serverdir, just close down the HarvestControllerApplication at once.
        shutdownNowOrContinue();

        // Get JMS-connection
        // Channel THIS_CLIENT is only used for replies to store messages so
        // do not set as listener here. It is registered in the arcrepository
        // client.
        // Channel ANY_xxxPRIORIRY_HACO is used for listening for jobs, and
        // registered below.

        jmsConnection = JMSConnectionFactory.getInstance();
        postProcessing = PostProcessing.getInstance(jmsConnection);
        log.debug("Obtained JMS connection.");

        status = new CrawlStatus();
        log.info("SEND_READY_DELAY used by HarvestControllerServer is {}", status.getSendReadyDelay());

        // If any unprocessed jobs are left on the server, process them now
        postProcessing.processOldJobs();

        // Register for listening to harvest channel validity responses
        JMSConnectionFactory.getInstance().setListener(HARVEST_CHAN_VALID_RESP_ID, this);

        // Ask if the channel this harvester is assigned to is valid
        jmsConnection.send(new HarvesterRegistrationRequest(HarvestControllerServer.CHANNEL, applicationInstanceId));
        log.info("Requested to check the validity of harvest channel '{}'", HarvestControllerServer.CHANNEL);
    }

    /**
     * Release all jms connections. Close the Controller
     */
    public synchronized void close() {
        log.info("Closing HarvestControllerServer.");
        cleanup();
        log.info("Closed down HarvestControllerServer");
    }

    /**
     * Will be called on shutdown.
     *
     * @see CleanupIF#cleanup()
     */
    public void cleanup() {
        if (jmsConnection != null) {
            jmsConnection.removeListener(HARVEST_CHAN_VALID_RESP_ID, this);
            if (jobChannel != null) {
                jmsConnection.removeListener(jobChannel, this);
            }
        }
        // Stop the sending of status messages
        status.stopSending();
        instance = null;
    }

    @Override
    public void visit(HarvesterRegistrationResponse msg) {
        // If we have already started or the message notifies for another channel, resend it.
        String channelName = msg.getHarvestChannelName();
        if (status.isChannelValid() || !CHANNEL.equals(channelName)) {
            // Controller has already started
            jmsConnection.resend(msg, msg.getTo());
            if (log.isTraceEnabled()) {
                log.trace("Resending harvest channel validity message for channel '{}'", channelName);
            }
            return;
        }

        if (!msg.isValid()) {
        	String errMsg = "Received message stating that channel '" +  channelName + "' is invalid. Will stop. "
            		+ "Probable cause: the channel is not one of the known channels stored in the channels table"; 
            log.error(errMsg);
            // Send a notification about this, ASAP
            NotificationsFactory.getInstance().notify(errMsg, NotificationType.ERROR);
            close();
            return;
        }

        log.info("Received message stating that channel '{}' is valid.", channelName);
        // Environment and connections are now ready for processing of messages
        jobChannel = HarvesterChannels.getHarvestJobChannelId(channelName, msg.isSnapshot());

        // Only listen for harvester jobs if enough available space
        beginListeningIfSpaceAvailable();

        // Notify the harvest dispatcher that we are ready
        startAcceptingJobs();
        status.startSending();
    }

    /** Start listening for crawls, if space available. */
    private void beginListeningIfSpaceAvailable() {
        long availableSpace = FileUtils.getBytesFree(serverDir);
        if (availableSpace > minSpaceRequired) {
            log.info("Starts to listen to new jobs on queue '{}'", jobChannel);
            jmsConnection.setListener(jobChannel, this);
        } else {
            String pausedMessage = "Not enough available diskspace. Only " + availableSpace + " bytes available."
                    + " Harvester is paused.";
            log.error(pausedMessage);
            NotificationsFactory.getInstance().notify(pausedMessage, NotificationType.ERROR);
        }
    }

    /**
     * Start listening for new crawl requests again. This actually doesn't re-add a listener, but the listener only gets
     * removed when we're so far committed that we're going to exit at the end. So to start accepting jobs again, we
     * stop resending messages we get.
     */
    private synchronized void startAcceptingJobs() {
        // allow this harvestControllerServer to receive messages again
        status.setRunning(false);
    }

    /**
     * Stop accepting more jobs. After this is called, all crawl messages received will be resent to the queue. A bit
     * further down, we will stop listening altogether, but that requires another thread.
     */
    private synchronized void stopAcceptingJobs() {
        status.setRunning(true);
        log.debug("No longer accepting jobs.");
    }

    /**
     * Stop listening for new crawl requests.
     */
    private void removeListener() {
        log.debug("Removing listener on CHANNEL '{}'", jobChannel);
        jmsConnection.removeListener(jobChannel, this);
    }
    
    /**
     * Does the operator want us to shutdown now.
     * TODO In a later implementation, the harvestControllerServer could
     * be notified over JMX. Now we just look for a "shutdown.txt" file in the HARVEST_CONTROLLER_SERVERDIR 
     * log that we're shutting down, send a notification about this, and then shutdown.
     */
    private void shutdownNowOrContinue() {
        File shutdownFile = new File(serverDir, "shutdown.txt");
        
        if (shutdownFile.exists()) {
        	String msg = "Found shutdown-file in serverdir '" +  serverDir.getAbsolutePath() + "'. Shutting down the application"; 
            log.info(msg);
            NotificationsFactory.getInstance().notify(msg, NotificationType.INFO);
            instance.cleanup();
            System.exit(0);
        }
    }

    /**
     * Checks that we're available to do a crawl, and if so, marks us as unavailable, checks that the job message is
     * well-formed, and starts the thread that the crawl happens in. If an error occurs starting the crawl, we will
     * start listening for messages again.
     * <p>
     * The sequence of actions involved in a crawl are:</br> 1. If we are already running, resend the job to the queue
     * and return</br> 2. Check the job for validity</br> 3. Send a CrawlStatus message that crawl has STARTED</br> In a
     * separate thread:</br> 4. Unregister this HACO as listener</br> 5. Create a new crawldir (based on the JobID and a
     * timestamp)</br> 6. Write a harvestInfoFile (using JobID and crawldir) and metadata</br> 7. Instantiate a new
     * HeritrixLauncher</br> 8. Start a crawl</br> 9. Store the generated arc-files and metadata in the known
     * bit-archives </br>10. _Always_ send CrawlStatus DONE or FAILED</br> 11. Move crawldir into oldJobs dir</br>
     *
     * @param msg The crawl job
     * @throws IOFailure On trouble harvesting, uploading or processing harvestInfo
     * @throws UnknownID if jobID is null in the message
     * @throws ArgumentNotValid if the status of the job is not valid - must be SUBMITTED
     * @throws PermissionDenied if the crawldir can't be created
     * @see #visit(DoOneCrawlMessage) for more details
     */
    public void visit(DoOneCrawlMessage msg) throws IOFailure, UnknownID, ArgumentNotValid, PermissionDenied {
        // Only one doOneCrawl at a time. Returning should almost never happen,
        // since we deregister the listener, but we may receive another message
        // before the listener is removed. Also, if the job message is
        // malformed or starting the crawl fails, we re-add the listener.
        synchronized (this) {
            if (status.isRunning()) {
                log.warn(
                        "Received crawl request, but sent it back to queue, as another crawl is already running: '{}'",
                        msg);
                jmsConnection.resend(msg, jobChannel);
                try {
                    // Wait a second before listening again, so the message has
                    // a chance of getting snatched by another harvester.
                    Thread.sleep(TimeUtils.SECOND_IN_MILLIS);
                } catch (InterruptedException e) {
                    // Since we're not waiting for anything critical, we can
                    // ignore this exception.
                }
                return;
            }
            stopAcceptingJobs();
        }

        Thread t = null;

        // This 'try' matches a finally that restores running=false if we don't
        // start a crawl after all
        try {
            final Job job = msg.getJob();

            // Every job must have an ID or we can do nothing with it, not even
            // send a proper failure message back.
            Long jobID = job.getJobID();
            if (jobID == null) {
                log.warn("DoOneCrawlMessage arrived without JobID: '{}'", msg.toString());
                throw new UnknownID("DoOneCrawlMessage arrived without JobID");
            }

            log.info("Received crawlrequest for job {}: '{}'", jobID, msg);

            // Send message to scheduler that job is started
            CrawlStatusMessage csmStarted = new CrawlStatusMessage(jobID, JobStatus.STARTED);
            jmsConnection.send(csmStarted);

            // Jobs should arrive with status "submitted". If this is not the
            // case, log the error and send a job-failed message back.
            // HarvestScheduler likes getting a STARTED message before
            // FAILED, so we oblige it here.
            if (job.getStatus() != JobStatus.SUBMITTED) {
                String message = "Message '" + msg.toString() + "' arrived with" + " status " + job.getStatus()
                        + " for job " + jobID + ", should have been STATUS_SUBMITTED";
                log.warn(message);
                sendErrorMessage(jobID, message, message);
                throw new ArgumentNotValid(message);
            }

            final List<MetadataEntry> metadataEntries = msg.getMetadata();

            Thread t1;
            // Create thread in which harvesting will occur
            t1 = new HarvesterThread(job, msg.getOrigHarvestInfo(), metadataEntries);
            // start thread which will remove this listener, harvest, store, and
            // exit the VM
            t1.start();
            log.info("Started harvester thread for job {}", jobID);
            // We delay assigning the thread variable until start() has
            // succeeded. Thus, if start() fails, we will resume accepting
            // jobs.
            t = t1;
        } finally {
            // If we didn't start a thread for crawling after all, accept more
            // messages
            if (t == null) {
                startAcceptingJobs();
            }
        }
        // Now return from this method letting the thread do the work.
        // This is important as it allows us to receive upload-replies from
        // THIS_CLIENT in the crawl thread.
    }

    /**
     * Sends a CrawlStatusMessage for a failed job with the given short message and detailed message.
     *
     * @param jobID ID of the job that failed
     * @param message A short message indicating what went wrong
     * @param detailedMessage A more detailed message detailing why it went wrong.
     */
    public void sendErrorMessage(long jobID, String message, String detailedMessage) {
        CrawlStatusMessage csm = new CrawlStatusMessage(jobID, JobStatus.FAILED, null);
        csm.setHarvestErrors(message);
        csm.setHarvestErrorDetails(detailedMessage);
        jmsConnection.send(csm);
    }

    /**
     * A Thread class for the actual harvesting. This is required in order to stop listening while we're busy
     * harvesting, since JMS doesn't allow the called thread to remove the listener that was called.
     */
    private class HarvesterThread extends Thread {

        /** The harvester Job in this thread. */
        private final Job job;

        /** Stores documentary information about the harvest. */
        private final HarvestDefinitionInfo origHarvestInfo;

        /** The list of metadata associated with this Job. */
        private final List<MetadataEntry> metadataEntries;

        /**
         * Constructor for the HarvesterThread class.
         *
         * @param job a harvesting job
         * @param origHarvestInfo Info about the harvestdefinition that scheduled this job
         * @param metadataEntries metadata associated with the given job
         */
        public HarvesterThread(Job job, HarvestDefinitionInfo origHarvestInfo, List<MetadataEntry> metadataEntries) {
            this.job = job;
            this.origHarvestInfo = origHarvestInfo;
            this.metadataEntries = metadataEntries;
        }

        /**
         * The thread body for the harvester thread. Removes the JMS listener, sets up the files for Heritrix, then
         * passes control to the HarvestController to perform the actual harvest.
         * <p>
         *
         * @throws PermissionDenied if we cannot create the crawl directory.
         * @throws IOFailure if there are problems preparing or running the crawl.
         */
        public void run() {
            try {
                // We must remove the listener inside a thread,
                // as JMS doesn't allow us to remove it within the
                // call it made.
                removeListener();

                HarvestJob harvestJob = new HarvestJob(instance);
                harvestJob.init(job, origHarvestInfo, metadataEntries);
                Heritrix3Files files = harvestJob.getHeritrix3Files();

                Throwable crawlException = null;
                try {
                    harvestJob.runHarvest();
                } catch (Throwable e) {
                    String msg = "Error during crawling. The crawl may have been only partially completed.";
                    log.warn(msg, e);
                    crawlException = e;
                    throw new IOFailure(msg, e);
                } finally {
                	postProcessing.doPostProcessing(files.getCrawlDir(), crawlException);
                }
            } catch (Throwable t) {
                String msg = "Fatal error while operating job '" + job + "'";
                log.error(msg, t);
                NotificationsFactory.getInstance().notify(msg, NotificationType.ERROR, t);
            } finally {
                log.info("Ending crawl of job : {}", job.getJobID());
                // process serverdir for files not yet uploaded.
                postProcessing.processOldJobs();
                instance.shutdownNowOrContinue();
                startAcceptingJobs();
                beginListeningIfSpaceAvailable();
            }
        }
    }

    /**
     * Used for maintaining the running status of the crawling, is it running or not. Will also take care of notifying
     * the HarvestJobManager of the status.
     */
    private class CrawlStatus implements Runnable {

        /** The status. */
        private Boolean running = false;

        private boolean channelIsValid = false;

        /** Handles the periodic sending of status messages. */
        private PeriodicTaskExecutor statusTransmitter;

        private final int SEND_READY_DELAY = Settings.getInt(HarvesterSettings.SEND_READY_DELAY);

        /**
         * Starts the sending of status messages. Interval defined by HarvesterSettings.SEND_READY_DELAY .
         */
        public void startSending() {
            this.channelIsValid = true;
            statusTransmitter = new PeriodicTaskExecutor("HarvesterStatus", this, 0,
            		getSendReadyDelay());
        }

        /**
         * Stops the sending of status messages.
         */
        public void stopSending() {
            if (statusTransmitter != null) {
                statusTransmitter.shutdown();
                statusTransmitter = null;
            }
        }

        /**
         * Returns <code>true</code> if the a doOneCrawl is running, else <code>false</code>.
         * @return Whether a crawl running.
         */
        public boolean isRunning() {
            return running;
        }

        /**
         * Used for changing the running state in methods startAcceptingJobs and stopAcceptingJobs 
         * @param running The new status
         */
        public void setRunning(boolean running) {
            this.running = running;
        }

        /**
         * @return the channelIsValid
         */
        protected final boolean isChannelValid() {
            return channelIsValid;
        }

        @Override
        public void run() {
            try {
                Thread.sleep(getSendReadyDelay());
            } catch (Exception e) {
                log.error("Unable to sleep", e);
            }
            if (!running) {
                jmsConnection.send(new HarvesterReadyMessage(applicationInstanceId + " on " + physicalServerName,
                        HarvestControllerServer.CHANNEL));
            }
        }

        public int getSendReadyDelay() {
        	return SEND_READY_DELAY;
        }
        
    }

}
