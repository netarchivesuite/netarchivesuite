/* File:    $Id$
 * Revision: $Revision$
 * Author:   $Author$
 * Date:     $Date$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2010 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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
package dk.netarkivet.harvester.scheduler;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.jms.JMSException;
import javax.jms.QueueBrowser;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.common.distribute.JMSConnection;
import dk.netarkivet.common.distribute.JMSConnectionFactory;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.lifecycle.LifeCycleComponent;
import dk.netarkivet.common.utils.ExceptionUtils;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.harvester.HarvesterSettings;
import dk.netarkivet.harvester.datamodel.Job;
import dk.netarkivet.harvester.datamodel.JobDAO;
import dk.netarkivet.harvester.datamodel.JobPriority;
import dk.netarkivet.harvester.datamodel.JobStatus;
import dk.netarkivet.harvester.harvesting.HeritrixLauncher;
import dk.netarkivet.harvester.harvesting.distribute.DoOneCrawlMessage;
import dk.netarkivet.harvester.harvesting.distribute.JobChannelUtil;
import dk.netarkivet.harvester.harvesting.distribute.MetadataEntry;

/**
 * This class handles dispatching of scheduled Harvest jobs to the Harvest 
 * servers.<p>
 * The scheduler loads all active harvest definitions on a regular basis and
 * extracts the scheduling information for each definition.
 * When a harvest definition is scheduled to start the scheduler
 * creates the corresponding harvest jobs and submits these
 * to the active HarvestServers.<p>  
 * 
 * It also handles backup and makes sure backup is not performed while 
 * jobs are being scheduled.<p>
 *
 * Note: Only one <code>HarvestScheduler</code> should be running at a time. 
 */
public class HarvestScheduler extends LifeCycleComponent {

    /** The logger to use.    */
    protected static final Log log = LogFactory.getLog(
            HarvestScheduler.class.getName());

    /** The thread used to control when new dispatches should be run. */
    private Thread dispatcherThread;
     
     /** Connection to JMS provider. */
     private JMSConnection jmsConnection;          

     /** Used for storing a map of the <code>QueueBrowsers</code> used be the 
      * <code>HarvestScheduler</code>, so they don't need to be created over
      * and over (see Bug 2059).
      */
     private Map<JobPriority, QueueBrowser> queueBrowsers;

    /**
     * Create new instance of the HarvestScheduler.
     */
    public HarvestScheduler() {
        log.info("Creating HarvestScheduler");
        jmsConnection = JMSConnectionFactory.getInstance();
    }
    
    /**
     * Start the thread responsible for reading Harvest definitions from the 
     * database, and dispatching the harvest job to the servers.
     */
    public void start() {          
        //ToDo implement real scheduling with timeout functionality.
        dispatcherThread = new Thread("HarvestScheduler") { 
            public void run() {
                log.debug("Rescheduling any leftover jobs");
                rescheduleSubmittedJobs();
                int dispatchPeriode = 
                    Settings.getInt(HarvesterSettings.DISPATCH_JOBS_PERIOD);
                log.info("Scheduling dispatch every " + (dispatchPeriode/1000) 
                        + " seconds");
                try {
                    while (!dispatcherThread.isInterrupted()) {
                        try {
                            dispatchJobs();
                        } catch (Exception e) {
                            log.error("Unable to dispatch new harvest jobs", e);
                        }
                        Thread.sleep(dispatchPeriode);                
                    }
                } catch (InterruptedException e) {
                    log.info("HarvestJobDispatcher interrupted, " 
                            + e.getMessage());
                }        
            }
        };
        dispatcherThread.start();
    } 

    /**
     * Reschedule all jobs with JobStatus SUBMITTED.
     */
    private void rescheduleSubmittedJobs() {
        final JobDAO dao = JobDAO.getInstance();
        final Iterator<Long> jobs = dao.getAllJobIds(JobStatus.SUBMITTED);
        int resubmitcount = 0;
        while (jobs.hasNext()) {
            long oldID = jobs.next();
            long newID = dao.rescheduleJob(oldID);
            log.info("Resubmitting old job " + oldID + " as " + newID);
            resubmitcount++;
        }
        log.info(resubmitcount + " has been resubmitted.");
    }

    /**
     * Stop any job that has been in status STARTED a very long time defined
     * by the HarvesterSettings.JOB_TIMEOUT_TIME setting.
     *
     */
    private void stopTimeoutJobs() {
        final JobDAO dao = JobDAO.getInstance();
        final Iterator<Long> jobs = dao.getAllJobIds(JobStatus.STARTED);
        int stoppedJobs = 0;
        while (jobs.hasNext()) {
            long id = jobs.next();
            Job job = dao.read(id);

            long timeDiff =
                Settings.getLong(HarvesterSettings.JOB_TIMEOUT_TIME) * 1000;
            Date endTime = new Date();
            endTime.setTime(job.getActualStart().getTime() + timeDiff);
            if (new Date().after(endTime)) {
                final String msg = " Job " + id 
                    + " has exceeded its timeout of "
                    + (Settings.getLong(
                            HarvesterSettings.JOB_TIMEOUT_TIME) / 60) 
                            + " minutes." + " Changing status to " + "FAILED.";
                log.warn(msg);
                job.setStatus(JobStatus.FAILED);
                job.appendHarvestErrors(msg);
                dao.update(job);
                stoppedJobs++;
            }
        }
        if(stoppedJobs > 0) {
            log.warn("Changed " + stoppedJobs + " jobs from STARTED to FAILED");
        }
    }

    /**
     * Dispatched new jobs
     * Stop jobs with status STARTED, which have been on running for more 
     * than settings.harvester.scheduler.jobtimeouttime time.
     */
    void dispatchJobs() {
        stopTimeoutJobs();
        submitNewJobs();
    }

    /**
     * Submit the next new job if the relevant message queue is empty. 
     */
    synchronized void submitNewJobs() {
        try {
            for (JobPriority priority: JobPriority.values()) {
            	// This check was cause because of memory leak in Bug 2059 
            	if (Settings.getBoolean( 
            			HarvesterSettings.SINGLE_JOB_DISPATCHING)) {
            		if (isQueueEmpty(priority)) {
            			submitNextNewJob(priority);
            		} else {
            			if (log.isTraceEnabled()) log.trace(
            					"Skipping dispatching of " 
            					+ priority + " jobs, the message queue is full");
            		}
            	} else {
            		submitNextNewJob(priority);
            	}
            }
        } catch (JMSException e) {
            log.error("Unable to determine whether message queue is empty", e);
        }
    }
    
    /**
     * Submit the next new job (the one with the lowest ID) with the given 
     * priority.
     */
    private void submitNextNewJob(JobPriority priority) {
        final JobDAO dao = JobDAO.getInstance();
        Iterator<Long> jobsToSubmit = dao.getAllJobIds(JobStatus.NEW, priority);
        if (!jobsToSubmit.hasNext()) {
            if (log.isTraceEnabled() ) {
                log.trace("No " + priority + " jobs to be run at this time");
            }           
        } else {
            if (log.isDebugEnabled() ) {
                log.debug("Submitting new " + priority + " job");
            }
            final long jobID = jobsToSubmit.next();
            Job jobToSubmit = null;
            try {
                jobToSubmit = dao.read(jobID);

                jobToSubmit.setStatus(JobStatus.SUBMITTED);
                jobToSubmit.setSubmittedDate(new Date());
                dao.update(jobToSubmit);
                //Add alias metadata
                List<MetadataEntry> metadata
                = new ArrayList<MetadataEntry>();
                MetadataEntry aliasMetadataEntry
                = MetadataEntry.makeAliasMetadataEntry(
                        jobToSubmit.getJobAliasInfo(),
                        jobToSubmit.getOrigHarvestDefinitionID(),
                        jobToSubmit.getHarvestNum(),
                        jobToSubmit.getJobID());
                if (aliasMetadataEntry != null) {
                    metadata.add(aliasMetadataEntry);
                }

                //Add duplicationReduction MetadataEntry, if Deduplication 
                //is enabled.
                if (HeritrixLauncher.isDeduplicationEnabledInTemplate(
                        jobToSubmit.getOrderXMLdoc())) {
                    MetadataEntry duplicateReductionMetadataEntry
                    = MetadataEntry.makeDuplicateReductionMetadataEntry(
                            dao.getJobIDsForDuplicateReduction(jobID),
                            jobToSubmit.getOrigHarvestDefinitionID(),
                            jobToSubmit.getHarvestNum(),
                            jobToSubmit.getJobID()
                    );

                    if (duplicateReductionMetadataEntry != null) {
                        metadata.add(duplicateReductionMetadataEntry);
                    }
                }

                doOneCrawl(jobToSubmit, metadata);
                if (log.isTraceEnabled() ) {
                    log.trace("Job " + jobToSubmit + " sent to harvest queue.");
                }
            } catch (Throwable e) {
                String message = "Error while scheduling job " + jobID;
                log.warn(message, e);
                if (jobToSubmit != null) {
                    jobToSubmit.setStatus(JobStatus.FAILED);
                    jobToSubmit.appendHarvestErrors(message);
                    jobToSubmit.appendHarvestErrorDetails(
                            ExceptionUtils.getStackTrace(e));
                    dao.update(jobToSubmit);
                }
            }
        }
    }
    
    /**
     * Checks that the message queue for the given harvest job is empty and 
     * therefore ready for the next message.
     * @param priority The job priority used for the channel of the queue
     * @return Is the queue empty
     * @throws JMSException Unable to retrieve queue information
     */
    private boolean isQueueEmpty(JobPriority priority) throws JMSException {
    	if (queueBrowsers == null) {
    		createQueueBrowsers();
    	}
    	QueueBrowser qBrowser = queueBrowsers.get(priority);
    	try {
    		return !qBrowser.getEnumeration().hasMoreElements();
    	} catch (JMSException e) {
    		log.warn("Failed to check if queues where empty, trying to " +
    				"reestablish session and queue browsers ", e);
    		createQueueBrowsers();
    		qBrowser = queueBrowsers.get(priority);
    		return !qBrowser.getEnumeration().hasMoreElements();
    	}
    }
    
    private void createQueueBrowsers() throws JMSException {
        queueBrowsers = new HashMap<JobPriority, QueueBrowser>();
        
        for (JobPriority priority: JobPriority.values()) {
            log.debug("Creating QueueBrowser for " + priority + " jobs");
            queueBrowsers.put(priority, 
                    jmsConnection.createQueueBrowser(
                            JobChannelUtil.getChannel(priority)));
        }
    }
    
    /**
     * Submit an doOneCrawl request to a HarvestControllerServer with correct
     * priority.
     * @param job the specific job to send
     * @param metadata pre-harvest metadata to store in arcfile.
     * @throws ArgumentNotValid one of the parameters are null
     * @throws IOFailure if unable to send the doOneCrawl request to a
     * harvestControllerServer
     */
    public void doOneCrawl(Job job, List<MetadataEntry> metadata)
            throws ArgumentNotValid, IOFailure {
        ArgumentNotValid.checkNotNull(job, "job");
        ArgumentNotValid.checkNotNull(metadata, "metadata");

        DoOneCrawlMessage nMsg = new DoOneCrawlMessage(job, 
                JobChannelUtil.getChannel(job.getPriority()), metadata);
        log.debug("Send crawl request: " + nMsg);
        jmsConnection.send(nMsg);
    }

    /**
     * Release allocated resources (JMS connections) and stops dispatching 
     * harvest jobs, all without logging.
     */
    @Override
    public void shutdown() {
        log.debug("HarvestScheduler closing down.");
        if (dispatcherThread != null) {
            dispatcherThread.interrupt();
            dispatcherThread = null;
        }
        jmsConnection = null;
    }
}
