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
package dk.netarkivet.harvester.scheduler;

import java.util.Date;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;

import javax.inject.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.lifecycle.ComponentLifeCycle;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.utils.TimeUtils;
import dk.netarkivet.harvester.HarvesterSettings;
import dk.netarkivet.harvester.datamodel.Job;
import dk.netarkivet.harvester.datamodel.JobDAO;
import dk.netarkivet.harvester.datamodel.JobStatus;

/**
 * Responsible for cleaning obsolete jobs, see {@link #start()} for details.
 */
public class JobSupervisor implements ComponentLifeCycle {

    /** The logger to use. */
    private static final Logger log = LoggerFactory.getLogger(JobSupervisor.class);

    /** For scheduling tasks */
    private final Timer timer = new Timer();

    private final Provider<JobDAO> jobDaoProvider;
    private final Long jobTimeoutTime;

    /**
     * @param jobDaoProvider Used for accessing the jobdao.
     * @param jobTimeoutTime timeout in seconds.
     */
    public JobSupervisor(Provider<JobDAO> jobDaoProvider, Long jobTimeoutTime) {
        this.jobDaoProvider = jobDaoProvider;
        this.jobTimeoutTime = jobTimeoutTime;
    }

    /**
     * <ol>
     * <li>Starts the rescheduling of left over jobs (in a separate thread).
     * <li>Starts the timer for cleaning old jobs. eg. jobs that have been run longer than
     * {@link HarvesterSettings#JOB_TIMEOUT_TIME}.
     * </ol>
     */
    @Override
    public void start() {
        Thread thread = new Thread(new Runnable() {
            public void run() {
                rescheduleLeftOverJobs();
            }
        });
        thread.start();

        timer.schedule(new TimerTask() {
            public void run() {
                cleanOldJobs();
            }
        }, Settings.getInt(HarvesterSettings.JOB_TIMEOUT_TIME));
    }

    @Override
    public void shutdown() {
        timer.cancel();
    }

    /**
     * Reschedule all jobs with JobStatus SUBMITTED. Runs in a separate thread to avoid blocking.
     * <p>
     * Package protected to allow unit testing.
     */
    void rescheduleLeftOverJobs() {
        final Iterator<Long> jobs = jobDaoProvider.get().getAllJobIds(JobStatus.SUBMITTED);
        int resubmitcount = 0;
        while (jobs.hasNext()) {
            long oldID = jobs.next();
            long newID = jobDaoProvider.get().rescheduleJob(oldID);
            log.info("Resubmitting old job {} as {}", oldID, newID);
            ++resubmitcount;
        }
        log.info("{} jobs has been resubmitted.", resubmitcount);
    }

    /**
     * Stops any job that has been in status STARTED a very long time defined by the
     * {@link HarvesterSettings#JOB_TIMEOUT_TIME} setting.
     * <p>
     * Package protected to allow unit testing.
     */
    void cleanOldJobs() {
        try {
            final Iterator<Long> startedJobs = jobDaoProvider.get().getAllJobIds(JobStatus.STARTED);
            int stoppedJobs = 0;
            while (startedJobs.hasNext()) {
                long id = startedJobs.next();
                Job job = jobDaoProvider.get().read(id);

                long timeDiff = jobTimeoutTime * TimeUtils.SECOND_IN_MILLIS;
                Date endTime = new Date();
                endTime.setTime(job.getActualStart().getTime() + timeDiff);
                if (new Date().after(endTime)) {
                    final String msg = " Job " + id + " has exceeded its timeout of "
                            + (jobTimeoutTime / TimeUtils.HOUR_IN_MINUTES) + " minutes." + " Changing status to "
                            + "FAILED.";
                    log.warn(msg);
                    job.setStatus(JobStatus.FAILED);
                    job.appendHarvestErrors(msg);
                    jobDaoProvider.get().update(job);
                    ++stoppedJobs;
                }
            }
            if (stoppedJobs > 0) {
                log.warn("Changed {} jobs from STARTED to FAILED", stoppedJobs);
            }
        } catch (Throwable t) {
            log.error("Unable to stop obsolete jobs", t);
        }
    }

}
