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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.lifecycle.LifeCycleComponent;
import dk.netarkivet.common.utils.ExceptionUtils;
import dk.netarkivet.common.utils.NotificationsFactory;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.harvester.HarvesterSettings;
import dk.netarkivet.harvester.datamodel.DBSpecifics;
import dk.netarkivet.harvester.datamodel.HarvestDefinitionDAO;
import dk.netarkivet.harvester.datamodel.Job;
import dk.netarkivet.harvester.datamodel.JobDAO;
import dk.netarkivet.harvester.datamodel.JobStatus;
import dk.netarkivet.harvester.harvesting.HeritrixLauncher;
import dk.netarkivet.harvester.harvesting.distribute.HarvestControllerClient;
import dk.netarkivet.harvester.harvesting.distribute.MetadataEntry;


/**
 * This class handles dispatching of scheduled Harvest jobs to the Harvest servers.
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

    /** The Client to use for communicating with the known harvest servers.   */
    private HarvestControllerClient hcc;

    /** The thread used to control when new dispatches should be run. */
    private Thread dispatcherThread;

    /**
     * Listener after responses from submitted Harvest jobs.
     */
    private static HarvestSchedulerMonitorServer hsmon;

    /**
     * Backup-related fields.
     */
     private Date lastBackupDate = null;
     private int backupInitHour; // legal values: 0..23

    /**
     * Create new instance of the HarvestScheduler.
     */
    public HarvestScheduler() {
        log.info("Creating HarvestScheduler");
        hcc = HarvestControllerClient.getInstance();
        hsmon = HarvestSchedulerMonitorServer.getInstance();
        backupInitHour = Settings.getInt(CommonSettings.DB_BACKUP_INIT_HOUR);
        if (backupInitHour < 0 || backupInitHour > 23) {
            log.warn("Illegal value for backupHour "
                    + "(Settting = DB_BACKUP_INIT_HOUR) found: "
                    + backupInitHour);
            log.info("BackupHour set to 0");
            backupInitHour = 0;
        } else {
            log.info("Backup hour is " + backupInitHour);
        }
    }

    /**
     * Start the thread responsible for reading Harvest definitions from the database,
     * and dispatching the harvest job to the servers.
     */
    public void start() {
        dispatcherThread = new Thread("HarvestSceduler") { 
            public void run() {
                log.debug("Rescheduling any leftover jobs");
                rescheduleSubmittedJobs();
                int dispatchPeriode = 
                    Settings.getInt(HarvesterSettings.DISPATCH_JOBS_PERIOD);
                log.info("Scheduling dispatch every " + (dispatchPeriode/1000) +
                        " seconds");
                try {
                    while (!dispatcherThread.isInterrupted()) {
                        dispatchJobs();
                        Thread.sleep(dispatchPeriode);                
                    }
                } catch (InterruptedException e) {
                    log.info("HarvestJobDispatcher interrupted" + e.getMessage() );
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
                final String msg = " Job "+ id + " has exceeded its timeout of "
                + (Settings.getLong( HarvesterSettings.JOB_TIMEOUT_TIME) / 60) + " minutes." +
                " Changing status to " + "FAILED.";
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
     * Schedule all jobs ready for execution and perform backup if required. 
     */
    synchronized void dispatchJobs() {

        // stop STARTED jobs which have been on for more than
        // settings.harvester.scheduler.jobtimeouttime time.
        stopTimeoutJobs();

        // ToDo moved to new DB backup class
        if (backupNow()) { // Check if we want to backup the database now?
            File backupDir = new File("DB-Backup-" + System.currentTimeMillis());
            try {
                DBSpecifics.getInstance().backupDatabase(backupDir);
                lastBackupDate = new Date();
            } catch (SQLException e) {
                String errMsg = "Unable to backup database to dir: "
                    + backupDir + "\n"
                    + ExceptionUtils.getSQLExceptionCause(e);
                log.warn(errMsg, e);
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
        if (HarvestDefinitionDAO.getInstance().isGeneratingJobs()) {
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
	 * Submit those jobs that are ready for submission and the harvester queue
	 * is empty.
	 * */
    synchronized void submitNewJobs() {
        final JobDAO dao = JobDAO.getInstance();
        Iterator<Long> jobsToSubmit = dao.getAllJobIds(JobStatus.NEW);
        if (!jobsToSubmit.hasNext()) {
            log.trace("No jobs to be run at this time");
        } else {
            log.trace("Submitting new jobs.");
        }

        int i = 0;
        while (jobsToSubmit.hasNext()) {
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

                hcc.doOneCrawl(jobToSubmit, metadata);
                log.trace("Job " + jobToSubmit + " sent to harvest queue.");
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
        if (i > 0) {
            log.info("Submitted " + i + " jobs for harvesting");
        }
    }

    /**
     * Release allocated resources (JMS connections), stop scheduling harvests,
     * and nullify the singleton.
     */
    public void close() {
        log.debug("HarvestScheduler closing down.");
        shutdown();
        log.trace("HarvestScheduler now closed");
    }


    /**
     * Release allocated resources (JMS connections) and stops dispatching harvest jobs, all without logging.
     * @override
     */
    public void shutdown() {
        if (dispatcherThread != null) {
            synchronized (dispatcherThread) {
                dispatcherThread.interrupt();
            }
        }
        dispatcherThread = null;
        if (hcc != null) {
            hcc.close();
            hcc = null;
        }
        if (hsmon != null) {
            hsmon.cleanup();
        }
        hsmon = null;
    }
}
