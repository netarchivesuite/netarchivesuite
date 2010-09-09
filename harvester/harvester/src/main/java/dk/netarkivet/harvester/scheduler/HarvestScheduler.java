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

import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.jms.JMSException;
import javax.jms.QueueBrowser;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.distribute.ChannelID;
import dk.netarkivet.common.distribute.JMSConnection;
import dk.netarkivet.common.distribute.JMSConnectionFactory;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.lifecycle.LifeCycleComponent;
import dk.netarkivet.common.utils.ExceptionUtils;
import dk.netarkivet.common.utils.NotificationsFactory;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.utils.StringUtils;
import dk.netarkivet.harvester.HarvesterSettings;
import dk.netarkivet.harvester.datamodel.DBSpecifics;
import dk.netarkivet.harvester.datamodel.Job;
import dk.netarkivet.harvester.datamodel.JobDAO;
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
    protected static final Log LOG = LogFactory.getLog(
            HarvestScheduler.class.getName());

    private static class HarvestSchedulerExecutor
    extends ScheduledThreadPoolExecutor {

        public HarvestSchedulerExecutor() {
            // We need only 1 thread for harvest scheduling
            super(1);
        }

        @Override
        protected void afterExecute(Runnable task, Throwable t) {
            if (t != null) {
                LOG.error("Error during harvest scheduling", t);
            }
        }

    }

    /**
     * Backup-related fields.
     */
     private Date lastBackupDate = null;
     private int backupInitHour; // legal values: 0..23

     /** Connection to JMS provider. */
     private JMSConnection jmsConnection;

     /**
      * The executor handle used to control how frequently, jobs are scheduled.
      * @see ScheduledThreadPoolExecutor
      */
     ScheduledFuture<?> schedulerHandle;

    /**
     * Create new instance of the HarvestScheduler.
     */
    public HarvestScheduler() {
        LOG.info("Creating HarvestScheduler");
        jmsConnection = JMSConnectionFactory.getInstance();
        backupInitHour = Settings.getInt(CommonSettings.DB_BACKUP_INIT_HOUR);
        if (backupInitHour < 0 || backupInitHour > 23) {
            LOG.warn("Illegal value for backupHour "
                    + "(Settting = DB_BACKUP_INIT_HOUR) found: "
                    + backupInitHour);
            LOG.info("BackupHour set to 0");
            backupInitHour = 0;
        } else {
            LOG.info("Backup hour is " + backupInitHour);
        }
    }

    /**
     * Start the thread responsible for reading Harvest definitions from the
     * database, and dispatching the harvest job to the servers.
     */
    public void start() {

        // First reschedule any leftover jobs
        LOG.debug("Rescheduling any leftover jobs");
        rescheduleSubmittedJobs();

        Runnable scheduleJobsTask = new Runnable() {
            long time = System.currentTimeMillis();
            @Override
            public void run() {
                long startTime = System.currentTimeMillis();
                long elapsed = startTime - time;
                LOG.info("Will schedule jobs. "
                        + StringUtils.formatDuration(elapsed / 1000)
                        + " elapsed since last scheduling pass started.");

                dispatchJobs();

                long endTime = System.currentTimeMillis();
                elapsed = endTime - startTime;
                LOG.info("Finished scheduling pass in "
                        + (elapsed < 1000 ? elapsed + " ms" :
                            StringUtils.formatDuration(elapsed / 1000))
                        + ".");

                time = endTime;
            }
        };

        // Schedule running every minute. This is one minute because
        // that's the finest we can define in a harvest definition.
        HarvestSchedulerExecutor scheduler = new HarvestSchedulerExecutor();
        schedulerHandle = scheduler.scheduleAtFixedRate(
                scheduleJobsTask, 0, 1, TimeUnit.MINUTES);
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
            LOG.info("Resubmitting old job " + oldID + " as " + newID);
            resubmitcount++;
        }
        LOG.info(resubmitcount + " has been resubmitted.");
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
                final String msg = " Job " + id + " has exceeded its timeout of "
                + (Settings.getLong( HarvesterSettings.JOB_TIMEOUT_TIME) / 60) +
                " minutes." + " Changing status to " + "FAILED.";
                LOG.warn(msg);
                job.setStatus(JobStatus.FAILED);
                job.appendHarvestErrors(msg);
                dao.update(job);
                stoppedJobs++;
            }
        }
        if(stoppedJobs > 0) {
            LOG.warn("Changed " + stoppedJobs + " jobs from STARTED to FAILED");
        }
    }

    /**
     * Schedule all jobs ready for execution and perform backup if required.
     */
    synchronized void dispatchJobs() {

        // stop STARTED jobs which have been on for more than
        // settings.harvester.scheduler.jobtimeouttime time.
        stopTimeoutJobs();

        // To be removed when the Harvestjob functionality is moved to its own
        // application connected to a database server
        if (backupNow()) { // Check if we want to backup the database now?
            File backupDir = new File("DB-Backup-" +
                    System.currentTimeMillis());
            try {
                DBSpecifics.getInstance().backupDatabase(backupDir);
                lastBackupDate = new Date();
            } catch (SQLException e) {
                String errMsg = "Unable to backup database to dir: "
                    + backupDir + "\n"
                    + ExceptionUtils.getSQLExceptionCause(e);
                LOG.warn(errMsg, e);
                NotificationsFactory.getInstance().errorEvent(
                        errMsg, e);
            }
        }
        submitNewJobs();
    }

    /**
     * Is it now time to backup the database?
     * Check that no jobs are being generated, that we backup as soon as
     * possible if backup is behind schedule, that we don't backup several
     * times in an hour, and that the first backup is done at the right time.
     * @return true, if it is now time to backup the database
     */
    private boolean backupNow() {
        // Never backup while making jobs
        if (HarvestJobGenerator.isGeneratingJobs()) {
            return false;
        }
        Calendar cal = Calendar.getInstance();
        boolean isBackupHour = (cal.get(Calendar.HOUR_OF_DAY)
                == backupInitHour);
        if (lastBackupDate != null) {
            int hoursPassed = getHoursPassedSince(lastBackupDate);
            if (hoursPassed < 1) {
                return false; // Never backup if it's been done within an hour
            } else if (hoursPassed > 25) {
                return true; // Backup at any time if we're behind schedule
            }
        }
        return isBackupHour;
    }

    /**
     * Find the number of hours passed since argument theDate.
     * returns -1, if number of hours passed since theDate is negative.
     * @param theDate the date compared to current time.
     * @return the number of hours passed since argument theDate
     */
    private int getHoursPassedSince(Date theDate){
        Calendar currentCal = Calendar.getInstance();
        Calendar theDateCal = Calendar.getInstance();
        theDateCal.setTime(theDate);
        long millisecondsPassed = currentCal.getTimeInMillis()
                                  - theDateCal.getTimeInMillis();
        if (millisecondsPassed < 0) {
            return -1;
        }
        int secondsPassed = (int) (millisecondsPassed / 1000L);
        return (secondsPassed / 3600);
    }

	/**
	 * Submit those jobs that are ready for submission if the relevant
	 * message queue is empty.
	 * */
    synchronized void submitNewJobs() {
        final JobDAO dao = JobDAO.getInstance();
        Iterator<Long> jobsToSubmit = dao.getAllJobIds(JobStatus.NEW);
        if (!jobsToSubmit.hasNext()) {
            LOG.trace("No jobs to be run at this time");
        } else {
            LOG.trace("Submitting new jobs.");
        }

        int numberOfSubmittedJobs = 0;
        while (jobsToSubmit.hasNext()) {
            final long jobID = jobsToSubmit.next();
            Job jobToSubmit = null;
            try {
                jobToSubmit = dao.read(jobID);

                if (isQueueEmpty(JobChannelUtil.getChannel(
                        jobToSubmit.getPriority()))) {
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
                    numberOfSubmittedJobs++;
                    LOG.trace("Job " + jobToSubmit + " sent to harvest queue.");
                }
            } catch (Throwable e) {
                String message = "Error while scheduling job " + jobID;
                LOG.warn(message, e);
                if (jobToSubmit != null) {
                    jobToSubmit.setStatus(JobStatus.FAILED);
                    jobToSubmit.appendHarvestErrors(message);
                    jobToSubmit.appendHarvestErrorDetails(
                            ExceptionUtils.getStackTrace(e));
                    dao.update(jobToSubmit);
                }
            }
        }
        if (numberOfSubmittedJobs > 0) {
            LOG.info("Submitted " + numberOfSubmittedJobs + " jobs for harvesting");
        }
    }

    /**
     * Checks that the message queue for the given harvest job is empty and
     * therefore ready for the next message.
     * @param job The job to check the queue for
     * @return Is the queue empty
     * @throws JMSException Unable to retrieve queue information
     */
    private boolean isQueueEmpty(ChannelID channelId) throws JMSException {
        QueueBrowser qBrowser = jmsConnection.createQueueBrowser(channelId);
        return !qBrowser.getEnumeration().hasMoreElements();
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
        LOG.debug("Send crawl request: " + nMsg);
        jmsConnection.send(nMsg);
    }

    /**
     * Release allocated resources (JMS connections) and stops dispatching
     * harvest jobs, all without logging.
     * @override
     */
    public void shutdown() {
        LOG.debug("HarvestScheduler closing down.");
        if (schedulerHandle != null) {
            synchronized (schedulerHandle) {
                schedulerHandle.cancel(false);
            }
        }
        schedulerHandle = null;
        jmsConnection = null;
    }
}
