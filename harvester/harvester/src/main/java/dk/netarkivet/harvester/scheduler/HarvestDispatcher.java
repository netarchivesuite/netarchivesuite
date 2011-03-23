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

import javax.jms.MessageListener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.common.distribute.Channels;
import dk.netarkivet.common.distribute.JMSConnection;
import dk.netarkivet.common.distribute.JMSConnectionFactory;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.lifecycle.ComponentLifeCycle;
import dk.netarkivet.common.utils.ExceptionUtils;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.harvester.HarvesterSettings;
import dk.netarkivet.harvester.datamodel.HarvestDefinitionDAO;
import dk.netarkivet.harvester.datamodel.Job;
import dk.netarkivet.harvester.datamodel.JobDAO;
import dk.netarkivet.harvester.datamodel.JobPriority;
import dk.netarkivet.harvester.datamodel.JobStatus;
import dk.netarkivet.harvester.datamodel.SparseFullHarvest;
import dk.netarkivet.harvester.datamodel.SparsePartialHarvest;
import dk.netarkivet.harvester.distribute.HarvesterMessageHandler;
import dk.netarkivet.harvester.harvesting.HeritrixLauncher;
import dk.netarkivet.harvester.harvesting.distribute.DoOneCrawlMessage;
import dk.netarkivet.harvester.harvesting.distribute.JobChannelUtil;
import dk.netarkivet.harvester.harvesting.distribute.MetadataEntry;
import dk.netarkivet.harvester.harvesting.distribute.PersistentJobData.HarvestDefinitionInfo;
import dk.netarkivet.harvester.harvesting.distribute.ReadyForJobMessage;

/**
 * This class handles dispatching of scheduled Harvest jobs to the Harvest
 * servers<p>
 * The scheduler loads all active harvest definitions on a regular basis and
 * extracts the scheduling information for each definition.
 * When a harvest definition is scheduled to start the scheduler
 * creates the corresponding harvest jobs and submits these
 * to the active HarvestServers.<p>
 *
 * The dispatching of jobs occurs when at least one {@link ReadyForJobMessage}
 * has been received by the dispatcher. A job of the same priority is then
 * issued.
 *
 * <p>
 *
 * Note: Only one <code>HarvestDispatcher</code> should be running at a time.
 */
public class HarvestDispatcher extends HarvesterMessageHandler
implements MessageListener, ComponentLifeCycle {

    /**
     * Encapsulates counters that keep track of how many jobs are ready to
     * be submitted by priority (i.e. the count of crawlers that have notified
     * being ready to process a job).
     */
    private static class DispatchState {
        /**
         * Counts the number of {@link ReadyForJobMessage}s received
         * for each {@link JobPriority}. This represents the available "slots" for
         * dispatching new jobs.
         */
        private Map<JobPriority, Integer> slots =
            new HashMap<JobPriority, Integer>();

        public String toString() {
            String statusLine = "Ready to submit";
            for (JobPriority p : JobPriority.values()) {
                statusLine += "\n" + getReadyCount(p) + " " + p + " jobs";
            }
            return statusLine;
        }

        /**
         * Returns the number of {@link ReadyForJobMessage} for the
         * given {@link JobPriority}.
         * @param p the job priority
         * @return the number of {@link ReadyForJobMessage} for the
         * given {@link JobPriority}.
         */
        private synchronized int getReadyCount(JobPriority p) {
            Integer count = slots.get(p);
            return (count == null ? 0 : count.intValue());
        }

        private synchronized void incrementReadyCount(JobPriority p) {
            Integer count = slots.get(p);
            int newCount = (count == null ? 0 : count.intValue()) + 1;
            slots.put(p, newCount);

            if (log.isInfoEnabled()) {
                log.info(toString());
            }
        }

        private synchronized void decrementReadyCount(JobPriority p) {
            Integer count = slots.get(p);
            int newCount = (count == null ? 0 : count.intValue()) - 1;
            slots.put(p, newCount);

            if (log.isInfoEnabled()) {
                log.info(toString());
            }
        }

    }

    /** The logger to use.    */
    protected static final Log log = LogFactory.getLog(
            HarvestDispatcher.class.getName());

    /**
     * The period in milliseconds between dispatches.
     */
    private static int dispatchPeriodInMillis =
        Settings.getInt(HarvesterSettings.DISPATCH_JOBS_PERIOD);

    /** The thread used to control when new dispatches should be run. */
    private Thread dispatcherThread;

    /** Connection to JMS provider. */
    private JMSConnection jmsConnection;

    /**
     * The state of the job dispatch, encapsulates the counters of jobs that 
     * are ready to be submitted by priority, following reception of
     * a {@link ReadyForJobMessage}.
     */
    private final DispatchState state = new DispatchState();

    /**
     * Create new instance of the HarvestDispatcher.
     */
    public HarvestDispatcher() {
        if (log.isInfoEnabled()) {
            log.info("Creating HarvestDispatcher");
        }
        jmsConnection = JMSConnectionFactory.getInstance();
    }

    /**
     * Start the thread responsible for reading Harvest definitions from the
     * database, and dispatching the harvest job to the servers.
     */
    public void start() {

        // Listen to ReadyForJob messages
        JMSConnectionFactory.getInstance().setListener(
                Channels.getHarvestDispatcherChannel(), this);

        if (log.isDebugEnabled()) {
            log.debug("Rescheduling any leftover jobs");
        }
        rescheduleLeftOverJobs();

        //ToDo implement real scheduling with timeout functionality.
        // See FR https://sbforge.org/jira/browse/NAS-1811
        dispatcherThread = new Thread("HarvestDispatcher") {
            public void run() {
                if (log.isInfoEnabled()) {
                    log.info("Scheduling dispatch every "
                            + (dispatchPeriodInMillis/1000) + " seconds");
                }
                try {
                    while (!dispatcherThread.isInterrupted()) {
                        try {
                            dispatchJobs();
                        } catch (Exception e) {
                            log.error("Unable to dispatch new harvest jobs", e);
                        }
                        Thread.sleep(dispatchPeriodInMillis);
                    }
                } catch (InterruptedException e) {
                    if (log.isInfoEnabled()) {
                        log.info("HarvestJobDispatcher interrupted, "
                                + e.getMessage());
                    }
                }
            }
        };
        dispatcherThread.start();
    }

    /**
     * Reschedule all jobs with JobStatus SUBMITTED.
     */
    private void rescheduleLeftOverJobs() {
        final JobDAO dao = JobDAO.getInstance();
        final Iterator<Long> jobs = dao.getAllJobIds(JobStatus.SUBMITTED);
        int resubmitcount = 0;
        while (jobs.hasNext()) {
            long oldID = jobs.next();
            long newID = dao.rescheduleJob(oldID);
            if (log.isInfoEnabled()) {
                log.info("Resubmitting old job " + oldID + " as " + newID);
            }
            resubmitcount++;
        }
        if (log.isInfoEnabled()) {
            log.info(resubmitcount + " has been resubmitted.");
        }
    }

    /**
     * Stop any job that has been in status STARTED a very long time defined
     * by the HarvesterSettings.JOB_TIMEOUT_TIME setting.
     *
     */
    private void stopTimeoutJobs() {
        final JobDAO dao = JobDAO.getInstance();
        final Iterator<Long> startedJobs = dao.getAllJobIds(JobStatus.STARTED);
        int stoppedJobs = 0;
        while (startedJobs.hasNext()) {
            long id = startedJobs.next();
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
        if (stoppedJobs > 0) {
            log.warn("Changed " + stoppedJobs + " jobs from STARTED to FAILED");
        }
    }

    /**
     * Dispatches new jobs.
     * First stops jobs with status STARTED, which have been on running for more
     * than settings.harvester.scheduler.jobtimeouttime time.
     * Then submits new jobs if crawlers are free.
     */
    void dispatchJobs() {
        stopTimeoutJobs();
        submitNewJobs();
    }

    /**
     * For each {@link JobPriority}, submits as many jobs
     * as {@link ReadyForJobMessage}s have been received, and decrements the
     * submit "slot" accordingly.
     */
    void submitNewJobs() {
        for (JobPriority p : JobPriority.values()) {
            int readyMessagesCount = state.getReadyCount(p);
            for (int i = 0; i < readyMessagesCount; i++) {
                submitNextNewJob(p);
            }
        }
    }

    /**
     * Submit the next new job (the one with the lowest ID) with the given
     * priority, and updates the internal counter as needed.
     * @param priority the job priority
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
                List<MetadataEntry> metadata = new ArrayList<MetadataEntry>();
                MetadataEntry aliasMetadataEntry =
                    MetadataEntry.makeAliasMetadataEntry(
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

                // Extract documentary information about the harvest
                HarvestDefinitionDAO hDao = HarvestDefinitionDAO.getInstance();
                String hName = hDao.getHarvestName(
                        jobToSubmit.getOrigHarvestDefinitionID());

                String schedule = "";
                String hdComments = "";
                SparseFullHarvest fh = hDao.getSparseFullHarvest(hName);
                if (fh != null) {
                    hdComments = fh.getComments();
                } else {
                    SparsePartialHarvest ph =
                        hDao.getSparsePartialHarvest(hName);

                    if (ph == null) {
                        throw new ArgumentNotValid("No harvest definition "
                                + "found for id '"
                                + jobToSubmit.getOrigHarvestDefinitionID()
                                + "', named '" + hName + "'");
                    }

                    // The schedule name can only be documented for
                    // focused crawls.
                    schedule = ph.getScheduleName();

                    hdComments = ph.getComments();
                }

                doOneCrawl(jobToSubmit, hName, hdComments, schedule, metadata);
                if (log.isTraceEnabled() ) {
                    log.trace("Job " + jobToSubmit + " sent to harvest queue.");
                }

                state.decrementReadyCount(priority);
            } catch (Throwable e) {
                String message = "Error while scheduling job " + jobID
                    + ". Job status changed to FAILED";
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
     * Submit an doOneCrawl request to a HarvestControllerServer with correct
     * priority.
     * @param job the specific job to send
     * @param origHarvestName the harvest definition's name
     * @param origHarvestDesc the harvest definition's description
     * @param origHarvestSchedule the harvest definition schedule name
     * @param metadata pre-harvest metadata to store in arcfile.
     * @throws ArgumentNotValid one of the parameters are null
     * @throws IOFailure if unable to send the doOneCrawl request to a
     * harvestControllerServer
     */
    public void doOneCrawl(
            Job job,
            String origHarvestName,
            String origHarvestDesc,
            String origHarvestSchedule,
            List<MetadataEntry> metadata)
    throws ArgumentNotValid, IOFailure {
        ArgumentNotValid.checkNotNull(job, "job");
        ArgumentNotValid.checkNotNull(metadata, "metadata");

        DoOneCrawlMessage nMsg = new DoOneCrawlMessage(
                job,
                JobChannelUtil.getChannel(job.getPriority()),
                new HarvestDefinitionInfo(
                        origHarvestName, origHarvestDesc, origHarvestSchedule),
                        metadata);
        if (log.isDebugEnabled()) {
            log.debug("Send crawl request: " + nMsg);
        }
        jmsConnection.send(nMsg);
    }

    /**
     * Release allocated resources (JMS connections) and stops dispatching
     * harvest jobs, all without logging.
     */
    @Override
    public void shutdown() {
        log.debug("HarvestDispatcher closing down.");
        if (dispatcherThread != null) {
            dispatcherThread.interrupt();
            dispatcherThread = null;
        }

        JMSConnectionFactory.getInstance().removeListener(
                Channels.getHarvestDispatcherChannel(), this);

        jmsConnection = null;
    }

    @Override
    public void visit(ReadyForJobMessage msg) {
        ArgumentNotValid.checkNotNull(msg, "msg");
        state.incrementReadyCount(msg.getJobProprity());
    }

}
