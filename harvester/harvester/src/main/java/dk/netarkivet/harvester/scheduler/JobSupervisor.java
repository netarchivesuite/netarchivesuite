/* File:    $Id$
 * Revision: $Revision$
 * Author:   $Author$
 * Date:     $Date$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2012 The Royal Danish Library, the Danish State and
 * University Library, the National Library of France and the Austrian
 * National Library.
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
 * Foundation,dk.netarkivet.harvester.schedulerFloor, Boston, MA  02110-1301  USA
 */
package dk.netarkivet.harvester.scheduler;

import dk.netarkivet.common.lifecycle.ComponentLifeCycle;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.utils.TimeUtils;
import dk.netarkivet.harvester.HarvesterSettings;
import dk.netarkivet.harvester.datamodel.Job;
import dk.netarkivet.harvester.datamodel.JobDAO;
import dk.netarkivet.harvester.datamodel.JobStatus;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Date;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Responsible for cleaning obsolete jobs, see {@link #start()} for details.
 */
public class JobSupervisor implements ComponentLifeCycle {
    /** The logger to use.    */
    private final Log log = LogFactory.getLog(getClass()); 
    /** For scheduling tasks */
    private final Timer timer = new Timer();

    /**
     * <ol>
     * <li> Starts the rescheduling of left over jobs (in a separate thread).
     * <li> Starts the timer for cleaning old jobs. eg. jobs that have been run 
     * longer than {@link HarvesterSettings#JOB_TIMEOUT_TIME}.
     * </ol>
     */
    @Override
    public void start() {
        Thread thread = new Thread(new Runnable() {
            public void run()  {
                rescheduleLeftOverJobs();
            }
        });
        thread.start();

        timer.schedule( new TimerTask() {
            public void run() {
                cleanOldJobs();
            }
        }, 
        Settings.getInt(HarvesterSettings.JOB_TIMEOUT_TIME));  
    }

    @Override
    public void shutdown() {
        timer.cancel();
    }

    /**
     * Reschedule all jobs with JobStatus SUBMITTED. 
     * Runs in a separate thread to avoid blocking.
     * 
     * Package protected to allow unit testing.
     */
    void rescheduleLeftOverJobs() {
        final JobDAO dao = JobDAO.getInstance();
        final Iterator<Long> jobs = 
                dao.getAllJobIds(JobStatus.SUBMITTED);
        int resubmitcount = 0;
        while (jobs.hasNext()) {
            long oldID = jobs.next();
            long newID = dao.rescheduleJob(oldID);
            if (log.isInfoEnabled()) {
                log.info("Resubmitting old job " + oldID + 
                        " as " + newID);
            }
            resubmitcount++;
        }
        log.info(resubmitcount + " jobs has been resubmitted.");
    }

    /**
     * Stops any job that has been in status STARTED a very long time defined
     * by the {@link HarvesterSettings#JOB_TIMEOUT_TIME} setting.
     * 
     * Package protected to allow unit testing.
     */
    void cleanOldJobs() {
        try {
            final JobDAO dao = JobDAO.getInstance();
            final Iterator<Long> startedJobs = dao.getAllJobIds(JobStatus.STARTED);
            int stoppedJobs = 0;
            while (startedJobs.hasNext()) {
                long id = startedJobs.next();
                Job job = dao.read(id);

                long timeDiff =
                        Settings.getLong(HarvesterSettings.JOB_TIMEOUT_TIME) 
                        * TimeUtils.SECOND_IN_MILLIS;
                Date endTime = new Date();
                endTime.setTime(job.getActualStart().getTime() + timeDiff);
                if (new Date().after(endTime)) {
                    final String msg = " Job " + id
                            + " has exceeded its timeout of " + 
                            (Settings.getLong(HarvesterSettings.JOB_TIMEOUT_TIME) 
                                    / TimeUtils.HOUR_IN_MINUTES) + 
                                    " minutes." + " Changing status to " + "FAILED.";
                    log.warn(msg);
                    job.setStatus(JobStatus.FAILED);
                    job.appendHarvestErrors(msg);
                    dao.update(job);
                    stoppedJobs++;
                }
            }
            if (stoppedJobs > 0) {
                log.warn("Changed " + stoppedJobs + " jobs from STARTED to FAILED");
            }
        } catch (Throwable thr) {
            log.error("Unable to stop obsolete jobs", thr);
        }
    }
}
