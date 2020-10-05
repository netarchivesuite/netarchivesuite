package dk.netarkivet.harvester.tools.dummy;

import java.io.File;

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
import dk.netarkivet.harvester.datamodel.Job;
import dk.netarkivet.harvester.datamodel.JobStatus;
import dk.netarkivet.harvester.distribute.HarvesterChannels;
import dk.netarkivet.harvester.distribute.HarvesterMessageHandler;
import dk.netarkivet.harvester.harvesting.distribute.CrawlStatusMessage;
import dk.netarkivet.harvester.harvesting.distribute.DoOneCrawlMessage;
import dk.netarkivet.harvester.harvesting.distribute.HarvesterReadyMessage;
import dk.netarkivet.harvester.harvesting.distribute.HarvesterRegistrationRequest;
import dk.netarkivet.harvester.harvesting.distribute.HarvesterRegistrationResponse;

/**
 * This class responds to JMS doOneCrawl messages from the HarvestScheduler and waits 10 minutes before failing the job. 
 * <p>
 * Initially, it registers its channel with the Scheduler by sending a HarvesterRegistrationRequest and waits for a positive HarvesterRegistrationResponse
 * that its channel is recognized. If not recognized by the Scheduler, the HarvestControllerServer will send a notification about this, 
 * and then close down the application.
 * 
 * During its operation CrawlStatus messages are sent to the HarvestSchedulerMonitorServer. When responding to the message,
 * it sends a message with status 'STARTED'. When 10 minutes have passed, a message is sent with status 'FAILED'.
 * <p>
 * Before the 10 minutes starts, the JMS listener is removed to avoid handling more than one doOneCrawlMessage at a time
 * During the 10 minutes, it will send two (instead of one) HarvesterReadyMessages to the scheduler to test issue NAS-2614.
 * The interval between sending HarvesterReadyMessages is defined by the setting 'settings.harvester.harvesting.sendReadyDelay'.    
 * <p>
 */
public class FaultyHarvestControllerServer extends HarvesterMessageHandler implements CleanupIF {
    
    
	    /** The logger to use. */
	    private static final Logger log = LoggerFactory.getLogger(FaultyHarvestControllerServer.class);

	    /** The unique instance of this class. */
	    private static FaultyHarvestControllerServer instance;

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

	    /** The CHANNEL of jobs processed by this instance. */
	    private static final String CHANNEL = Settings.get(HarvesterSettings.HARVEST_CONTROLLER_CHANNEL);

	    /** Jobs are fetched from this queue. */
	    private ChannelID jobChannel;

	    /** the serverdir, where the harvesting takes place. */
	    private final File serverDir;

	    /** This is true while a doOneCrawl is running. No jobs are accepted while this is running. */
	    private CrawlStatus status;

	    /**
	     * Returns or creates the unique instance of this singleton.
	     * @return The instance
	     * @throws PermissionDenied If the serverdir or oldjobsdir can't be created
	     * @throws IOFailure if data from old harvests exist, but contain illegal data
	     */
	    public static synchronized FaultyHarvestControllerServer getInstance() throws IOFailure {
	        if (instance == null) {
	            instance = new FaultyHarvestControllerServer();
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
	    private FaultyHarvestControllerServer() throws IOFailure {
	        log.info("Starting {}.", this.getClass());
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

	        // Get JMS-connection
	        // Channel THIS_CLIENT is only used for replies to store messages so
	        // do not set as listener here. It is registered in the arcrepository
	        // client.
	        // Channel ANY_xxxPRIORIRY_HACO is used for listening for jobs, and
	        // registered below.

	        jmsConnection = JMSConnectionFactory.getInstance();
	        
	        status = new CrawlStatus();
	        log.info("SEND_READY_DELAY used by {} is {}", this.getClass().getName(), status.getSendReadyDelay());

	       
	        // Register for listening to harvest channel validity responses
	        JMSConnectionFactory.getInstance().setListener(HARVEST_CHAN_VALID_RESP_ID, this);

	        // Ask if the channel this harvester is assigned to is valid
	        jmsConnection.send(new HarvesterRegistrationRequest(FaultyHarvestControllerServer.CHANNEL, applicationInstanceId));
	        log.info("Requested to check the validity of harvest channel '{}'", FaultyHarvestControllerServer.CHANNEL);
	    }

	    /**
	     * Release all jms connections. Close the Controller
	     */
	    public synchronized void close() {
	        log.info("Closing {}.", this.getClass().getName());
	        cleanup();
	        log.info("Closed down {}", this.getClass().getName());
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
	     * Here we receives a DoOneCrawlMessage, waits 10 minutes, and then fails the job.
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

	        final Job job = msg.getJob();

            // Every job must have an ID or we can do nothing with it, not even
            // send a proper failure message back.
            Long jobID = job.getJobID();

            log.info("Received crawlrequest for job {}: '{}'", jobID, msg);
            
            // Send message to scheduler that job is started
            CrawlStatusMessage csmStarted = new CrawlStatusMessage(jobID, JobStatus.STARTED);
            jmsConnection.send(csmStarted);

            removeListener();
            
            log.info("Waiting 10 minutes to illustrate a real harvest");
            // wait 10 minutes (should this be in a thread of its own???
            try {
				Thread.sleep(10 *// minutes to sleep
				        60 *   // seconds to a minute
				        1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
            
            csmStarted = new CrawlStatusMessage(jobID, JobStatus.FAILED);
            jmsConnection.send(csmStarted);
            
            
            startAcceptingJobs();
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
	            log.info("Sending ready message #1 from {}", this.getClass());
	            jmsConnection.send(new HarvesterReadyMessage(applicationInstanceId + " on " + physicalServerName,
	                    FaultyHarvestControllerServer.CHANNEL));
	            log.info("Sending ready message #2 from {}", this.getClass());
	            jmsConnection.send(new HarvesterReadyMessage(applicationInstanceId + " on " + physicalServerName,
	                    FaultyHarvestControllerServer.CHANNEL));
	        }

	        public int getSendReadyDelay() {
	        	return SEND_READY_DELAY;
	        }
	        
	    }
}
