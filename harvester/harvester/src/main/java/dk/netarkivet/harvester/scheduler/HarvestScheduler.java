/* File:    $Id$
* Revision: $Revision$
* Author:   $Author$
* Date:     $Date$
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
package dk.netarkivet.harvester.scheduler;

import javax.jms.Message;
import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.common.Settings;
import dk.netarkivet.common.distribute.Channels;
import dk.netarkivet.common.distribute.JMSConnectionFactory;
import dk.netarkivet.common.utils.CleanupIF;
import dk.netarkivet.common.utils.ExceptionUtils;
import dk.netarkivet.harvester.datamodel.DBSpecifics;
import dk.netarkivet.harvester.datamodel.HarvestDefinitionDAO;
import dk.netarkivet.harvester.datamodel.Job;
import dk.netarkivet.harvester.datamodel.JobDAO;
import dk.netarkivet.harvester.datamodel.JobStatus;
import dk.netarkivet.harvester.harvesting.distribute.HarvestControllerClient;
import dk.netarkivet.harvester.harvesting.distribute.MetadataEntry;


/**
 * This class handles scheduling of heritrix jobs.
 * The scheduler loads all active harvest definitions and
 * extracts the scheduling information for each definition.
 * When a harvest definition is scheduled to start the scheduler
 * creates the corresponding heritrix jobs and submits these
 * to the active HarvestServers.  It also handles backup and makes
 * sure backup is not performed while jobs are being scheduled.
 * This class is not Threadsafe.
 */
public class HarvestScheduler implements CleanupIF {
    /** The logger to use.    */
    protected static final Log log = LogFactory.getLog(
            HarvestScheduler.class.getName());

    /** The Client to use for communicating with the known servers.   */
    private HarvestControllerClient hcc;
    private Timer timer;

    /** The unique instance of this class. */
    private static HarvestScheduler instance;

    /** The period between checking if new jobs should be generated.
     * This is one minute because that's the finest we can define in a harvest definition.
     */
    private static final int GENERATE_JOBS_PERIOD = 60*1000;

    /** Lock guaranteeing that only one timertask is running at a time, and
     * that allows the next timer tasks to drop out if one is still running.
     */
    private boolean running;
    private static HarvestSchedulerMonitorServer hsmon;

    /**
     * Backup-related fields.
     */
     private Date lastBackupDate = null;
     private int backupInitHour; // legal values: 0..23

    /**
     * Create new instance of the HarvestScheduler.
     */
    private HarvestScheduler() {
        log.info("Creating HarvestScheduler");
        hcc = HarvestControllerClient.getInstance();
        hsmon = HarvestSchedulerMonitorServer.getInstance();
        backupInitHour = Settings.getInt(Settings.DB_BACKUP_INIT_HOUR);
        if (backupInitHour < 0 || backupInitHour > 23) {
            log.warn("Illegal value for backupHour (Settting = DB_BACKUP_INIT_HOUR) found: " + backupInitHour);
            log.info("BackupHour set to 0");
            backupInitHour = 0;
        } else {
            log.info("Backup hour is " + backupInitHour);
        }
    }

    /**
     * Get the unique instance of the harvest scheduler. If the instance is
     * new it is started up to begin scheduling harvests.
     * @return The instance
     */
    public static synchronized HarvestScheduler getInstance() {
        if (instance == null) {
            instance = new HarvestScheduler();
            //automatically calls the run-method..
            instance.run();
        }
        return instance;
    }


    /**
     * Start scheduling of harvest definitions.
     * The location of the harvestdefinition data is retrieved from:
     *    settings.xml
     */
    public void run() {
        try {
            log.debug("Rescheduling any leftover jobs");
            rescheduleJobs();
            log.debug("Starting scheduling of harvestdefinitions");
            log.info("Scheduler running every "
                          + (GENERATE_JOBS_PERIOD/1000) + " seconds");
            TimerTask task = new TimerTask() {
                public void run() {
                    try {
                        synchronized (timer) {
                            scheduleJobs();
                        }
                    } catch (Throwable e) {
                        log.warn("Exception while scheduling new jobs", e);
                    }
                }
            };
            final GregorianCalendar cal = new GregorianCalendar();
            // Schedule running every GENERATE_JOBS_PERIOD milliseconds
            // presently one minut.
            scheduleJobs();
            timer = new Timer(true);
            timer.scheduleAtFixedRate(task, cal.getTime(), GENERATE_JOBS_PERIOD);
        } catch (Throwable t) {
            log.warn("Scheduling terminated due to exception",
                       t);
            t.printStackTrace();
        }
    }

    private void rescheduleJobs() {
        // In case we were shut down without JMS queues being cleaned, remove
        // those messages left
        List<Message> loprimsgs = JMSConnectionFactory.getInstance().removeAllMessages
                (Channels.getAnyLowpriorityHaco());
        List<Message> hiprimsgs = JMSConnectionFactory.getInstance().removeAllMessages
                (Channels.getAnyHighpriorityHaco());
        // TODO: Resubmit those just taken out with same ID, as we know no
        // harvester is using them.
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


    /** Schedule all jobs ready for execution and perform backup if required. */
    private void scheduleJobs() {
        synchronized (this) {
            if (running) {
                log.debug("Previous scheduleJobs not finished, "
                               + "skipping new one.");
                return;
            }
            running = true;
        }
        if (backupNow()) { // Check if we want to backup the database now?
            File backupDir = new File("DB-Backup-"
                                      + System.currentTimeMillis());
            try {
                DBSpecifics.getInstance().backupDatabase(backupDir);
                lastBackupDate = new Date();
            } catch (SQLException e) {
                log.warn("Unable to backup database to dir: " + backupDir, e);
            }
        }

        try {
            final HarvestDefinitionDAO hddao =
                HarvestDefinitionDAO.getInstance();
            hddao.generateJobs(new Date());
            submitNewJobs();
        } finally {
            synchronized (this) {
                running = false;
            }
        }
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
        boolean isBackupHour = (cal.get(Calendar.HOUR_OF_DAY) == backupInitHour);
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
    private int getHoursPassedSince (Date theDate){
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

    /** Submit those jobs that are ready for submission. */
    private synchronized void submitNewJobs() {
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
                //Add jobid metadata

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
        cleanup();
        log.trace("HarvestScheduler now closed");
    }


    /**
     * Release allocated resources (JMS connections), stop scheduling harvests,
     * and nullify the singleton, all without logging.
     * @see CleanupIF#cleanup()
     */
    public void cleanup() {
        if (timer != null) {
            synchronized (timer) {
                timer.cancel();
            }
        }
        timer = null;
        if (hcc != null) {
            hcc.close();
            hcc = null;
        }
        if (hsmon != null) {
            hsmon.cleanup();
        }
        hsmon = null;
        instance = null;
    }

}
