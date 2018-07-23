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

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.lifecycle.ComponentLifeCycle;
import dk.netarkivet.common.lifecycle.PeriodicTaskExecutor;
import dk.netarkivet.common.utils.NotificationType;
import dk.netarkivet.common.utils.NotificationsFactory;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.harvester.HarvesterSettings;
import dk.netarkivet.harvester.datamodel.HarvestChannel;
import dk.netarkivet.harvester.datamodel.HarvestChannelDAO;
import dk.netarkivet.harvester.datamodel.HarvestDefinition;
import dk.netarkivet.harvester.datamodel.HarvestDefinitionDAO;
import dk.netarkivet.harvester.scheduler.jobgen.JobGenerator;
import dk.netarkivet.harvester.scheduler.jobgen.JobGeneratorFactory;

/**
 * Handles the generation of new jobs based on the harvest definitions in persistent storage. The
 * <code>HarvestJobGenerator</code> continuously scans the harvest definition database for harvest which should be run
 * now. If a HD defines a harvest which should be run, a Harvest Job is created in the harvest job database.
 */
public class HarvestJobGenerator implements ComponentLifeCycle {

    /** The class logger. */
    private static final Logger log = LoggerFactory.getLogger(HarvestJobGenerator.class);

    /**
     * The set of HDs (or rather their OIDs) that are currently being scheduled in a separate thread. This set is a
     * SynchronizedSet
     */
    protected static Set<Long> harvestDefinitionsBeingScheduled = Collections.synchronizedSet(new HashSet<Long>());

    /**
     * A map giving access to the thread generating jobs for a given harvest definition.
     */
    protected static Map<Long, JobGeneratorTask.JobGeneratorThread> threadMap = Collections.synchronizedMap(new HashMap<>());

    /**
     * Used the store the currenttimeMillis when the scheduling of a particular harvestdefinition # started or when last
     * a warning was issued.
     */
    protected static Map<Long, Long> schedulingStartedMap = Collections.synchronizedMap(new HashMap<Long, Long>());

    /** The executor used to schedule the generator jobs. */
    private PeriodicTaskExecutor genExec;

    /** @see HarvesterSettings#JOBGEN_POSTPONE_UNREGISTERED_HARVEST_CHANNEL */
    private static final boolean postponeUnregisteredChannel = Settings
            .getBoolean(HarvesterSettings.JOBGEN_POSTPONE_UNREGISTERED_HARVEST_CHANNEL);

    /** The HarvestDefinitionDAO used by the HarvestJobGenerator. */
    private static final HarvestDefinitionDAO haDefinitionDAO = HarvestDefinitionDAO.getInstance();

    private final HarvestChannelRegistry harvestChannelRegistry;

    public HarvestJobGenerator(final HarvestChannelRegistry harvestChannelRegistry) {
        this.harvestChannelRegistry = harvestChannelRegistry;
    }

    /**
     * Starts the job generation scheduler.
     */
    @Override
    public void start() {
        int generateJobsPeriod = Settings.getInt(HarvesterSettings.GENERATE_JOBS_PERIOD);
        genExec = new PeriodicTaskExecutor("JobGeneratorTask", new JobGeneratorTask(harvestChannelRegistry), 0, generateJobsPeriod);
        log.info("JobGeneratorTask set to run every {} seconds", generateJobsPeriod);
    }

    @Override
    public void shutdown() {
        if (genExec != null) {
            genExec.shutdown();
        }
    }

    /**
     * Contains the functionality for the individual JobGenerations.
     */
    static class JobGeneratorTask implements Runnable {

        private final HarvestChannelRegistry harvestChannelRegistry;
        
        public JobGeneratorTask(HarvestChannelRegistry harvestChannelRegistry) {
            this.harvestChannelRegistry = harvestChannelRegistry;
        }

        @Override
        public void run() {
            try {
                generateJobs(new Date());
            } catch (Exception e) {
                log.info("Exception caught at fault barrier while generating jobs.", e);
            }
        }

        /**
         * Check if jobs should be generated for any ready harvest definitions for the specified time.
         *
         * @param timeToGenerateJobsFor Jobs will be generated which should be run at this time. Note: In a production
         * system the provided time will normally be current time, but during testing we need to simulated other
         * points-in-time
         */
        void generateJobs(Date timeToGenerateJobsFor) {
            final Iterable<Long> readyHarvestDefinitions = haDefinitionDAO
                    .getReadyHarvestDefinitions(timeToGenerateJobsFor);
            log.trace("Generating jobs for harvests that should run at time '{}'", timeToGenerateJobsFor);
            HarvestChannelDAO hChanDao = HarvestChannelDAO.getInstance();
            
            for (final Long id : readyHarvestDefinitions) {
                // Make every HD run in its own thread, but at most once.
                synchronized(harvestDefinitionsBeingScheduled) {
                    if (harvestDefinitionsBeingScheduled.contains(id)) {
                        if (takesSuspiciouslyLongToSchedule(id)) {
                            String harvestName = haDefinitionDAO.getHarvestName(id);
                            String errMsg = "Possible problem creating jobs for harvestdefinition #" + id + " (" + harvestName + ")"
                                    + " as the previous scheduling is still running. Trying to recover.";
                            if (haDefinitionDAO.isSnapshot(id)) {
                                // Log only at level debug if the ID represents
                                // is a snapshot harvestdefinition, which are only run
                                // once anyway
                                log.debug(errMsg);
                                continue;
                            } else { // Log at level WARN, and send a notification, if it is time
                                log.warn(errMsg);
                                threadMap.get(id).killScheduling();
                                NotificationsFactory.getInstance().notify(errMsg, NotificationType.WARNING);
                            }
                        } else {
                            log.debug("We'll skip HD #{}. Jobgeneration of it has been running since {}", id, 
                                    new Date(schedulingStartedMap.get(id)) );
                            continue;
                        }
                    } else {
                        harvestDefinitionsBeingScheduled.add(id); // mark the harvest as being scheduled right now
                    }
                }

                final HarvestDefinition harvestDefinition = haDefinitionDAO.read(id);

                if (!harvestDefinition.isSnapShot()) {
                    Long chanId = harvestDefinition.getChannelId();

                    HarvestChannel chan = (chanId == null ? hChanDao.getDefaultChannel(false) : hChanDao
                            .getById(chanId));

                    String channelName = chan.getName();
                    if (postponeUnregisteredChannel && !harvestChannelRegistry.isRegistered(channelName)) {
                        log.info("Harvest channel '{}' has not yet been registered by any harvester, hence harvest "
                                + "definition '{}' ({}) cannot be processed by the job generator for now.",
                                channelName, harvestDefinition.getName(), id);
                        harvestDefinitionsBeingScheduled.remove(id);
                        continue;
                    }
                }
                
                schedulingStartedMap.put(id, System.currentTimeMillis());

                if (!harvestDefinition.runNow(timeToGenerateJobsFor)) {
                    log.trace("The harvestdefinition #{}'{}' should not run now.", id, harvestDefinition.getName());
                    log.trace("numEvents: {}", harvestDefinition.getNumEvents());
                    continue;
                }

                log.info("Starting to create jobs for harvest definition #{}({})", id, harvestDefinition.getName());

                final JobGeneratorThread jobGeneratorThread = new JobGeneratorThread(id, harvestDefinition);
                threadMap.put(id, jobGeneratorThread);
                jobGeneratorThread.start();
            }
        }

        /**
         * Find out if a scheduling takes more than is acceptable currently 5 minutes.
         *
         * @param harvestId A given harvestId
         * @return true, if a scheduling of the given harvestId has taken more than 5 minutes, or false, if not or no
         * scheduling for this harvestId is underway
         */
        private static boolean takesSuspiciouslyLongToSchedule(Long harvestId) {
            // acceptable delay before issuing warning is currently hard-wired to
            // 5 minutes (5 * 60 * 1000 milliseconds)
            final long acceptableDelay = 5 * 60 * 1000;
            Long timewhenscheduled = schedulingStartedMap.get(harvestId);
            if (timewhenscheduled == null) {
                return false;
            } else {
                long now = System.currentTimeMillis();
                if (timewhenscheduled + acceptableDelay < now) {
                    // updates the schedulingStartedMap with currenttime for
                    // the given harvestID when returning true
                    schedulingStartedMap.put(harvestId, now);
                    return true;
                } else {
                    return false;
                }
            }
        }

        private static class JobGeneratorThread extends Thread {
            private final Long id;
            private final HarvestDefinition harvestDefinition;

            public JobGeneratorThread(Long id, HarvestDefinition harvestDefinition) {
                super("JobGeneratorTask-" + id);
                this.id = id;
                this.harvestDefinition = harvestDefinition;
            }

            public void run() {
                try {
                    JobGenerator jobGen = JobGeneratorFactory.getInstance();
                    int jobsMade = jobGen.generateJobs(harvestDefinition);
                    if (jobsMade > 0) {
                        log.info("Created {} jobs for harvest definition ({})", jobsMade,
                                harvestDefinition.getName());
                    } else {
                        String msg = "No jobs created for harvest definition '"
                                + harvestDefinition.getName()
                                + "'. Probable cause: harvest tries to continue harvest that is already finished ";
                        log.warn(msg);
                        NotificationsFactory.getInstance().notify(msg, NotificationType.WARNING);
                    }
                    haDefinitionDAO.update(harvestDefinition);
                } catch (Throwable e) {
                    try {
                        HarvestDefinition hd = haDefinitionDAO.read(harvestDefinition.getOid());
                        hd.setActive(false);
                        haDefinitionDAO.update(hd);
                        String errMsg = "Exception while scheduling harvestdefinition #" + id + "("
                                + harvestDefinition.getName() + "). The harvestdefinition has been "
                                + "deactivated!";
                        log.warn(errMsg, e);
                        NotificationsFactory.getInstance().notify(errMsg, NotificationType.ERROR, e);
                    } catch (Exception e1) {
                        String errMsg = "Exception while scheduling harvestdefinition #" + id + "("
                                + harvestDefinition.getName() + "). The harvestdefinition couldn't be "
                                + "deactivated!";
                        log.warn(errMsg, e);
                        log.warn("Unable to deactivate", e1);
                        NotificationsFactory.getInstance().notify(errMsg, NotificationType.ERROR, e);
                    }
                } finally {
                    killScheduling();
                }
            }

            public void killScheduling() {
                harvestDefinitionsBeingScheduled.remove(id);
                schedulingStartedMap.remove(id);
                threadMap.remove(id);
                log.debug("Removed HD #{}({}) from list of harvestdefinitions to be scheduled. "
                                + "Harvestdefinitions still to be scheduled: {}", id, harvestDefinition.getName(),
                        harvestDefinitionsBeingScheduled);
            }
        }
    }

}
