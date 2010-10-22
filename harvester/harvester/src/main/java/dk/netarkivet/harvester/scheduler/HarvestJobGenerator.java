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

import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.common.lifecycle.ComponentLifeCycle;
import dk.netarkivet.common.utils.NotificationsFactory;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.harvester.HarvesterSettings;
import dk.netarkivet.harvester.datamodel.HarvestDefinition;
import dk.netarkivet.harvester.datamodel.HarvestDefinitionDAO;

/**
 * Handles the generation of new jobs based on the harvest definitions in
 * persistent storage. The <code>HarvestJobGenerator</code> continuously scans
 * the harvest definition database for harvest which should be run now. If a HD
 * defines a harvest which should be run, a Harvest Job is created in the
 * harvest job database.
 */
public class HarvestJobGenerator implements ComponentLifeCycle {

    /**
     * This class is an executor for scheduled job generation tasks.
     * @see ScheduledThreadPoolExecutor
     */
    private static class JobGenerationExec
    extends ScheduledThreadPoolExecutor {

        public JobGenerationExec() {
            // We need only 1 thread
            super(1);
        }

        @Override
        protected void afterExecute(Runnable task, Throwable t) {
            if (t != null) {
                log.error("Error during job generation", t);
            }
        }

    }

    /** The set of HDs (or rather their OIDs) that are currently being
     * scheduled in a separate thread.
     * This set is a SynchronizedSet
     */
    private static Set<Long> harvestDefinitionsBeingScheduled =
        Collections.synchronizedSet(new HashSet<Long>());
    
    private static final Log log = 
        LogFactory.getLog(HarvestJobGenerator.class.getName());

    /** The executor used to schedule the generator jobs. */
    private JobGenerationExec genExec;

    private static final HarvestDefinitionDAO haDefinitionDAO =
        HarvestDefinitionDAO.getInstance();

    /**
     * Starts the job generation scheduler.
     */
    @Override
    public void start() {
        genExec = new JobGenerationExec();
        genExec.scheduleAtFixedRate(
                new JobGeneratorTask(), 0,
                Settings.getInt(HarvesterSettings.GENERATE_JOBS_PERIOD),
                TimeUnit.MILLISECONDS);
    }

    @Override
    public void shutdown() {
        if (genExec != null) {
            genExec.shutdownNow();
        }
    }    

    /**
     * Contains the functionality for the individual JobGenerations 
     */
    static class JobGeneratorTask implements Runnable {

        @Override
        public synchronized void run() {
            generateJobs(new Date());
        }          

        /**
         * Check if jobs should be generated for any ready harvest definitions 
         * for the specified time.
         * @param timeToGenerateJobsFor Jobs will be generated which should be 
         * run at this time. 
         * Note: In a production system the provided time will normally be 
         * current time, but during testing we need to simulated other 
         * points-in-time  
         */
        static void generateJobs(Date timeToGenerateJobsFor) {
            final Iterable<Long> readyHarvestDefinitions = 
                haDefinitionDAO.getReadyHarvestDefinitions(timeToGenerateJobsFor);
            for (final Long id : readyHarvestDefinitions) {      
             // Make every HD run in its own thread, but at most once.
                if (harvestDefinitionsBeingScheduled.contains(id)) {
                    // With the small importance of this logmessage,
                    // we won't spend time looking up the corresponding name for
                    // the harvestdefinition with this id number.
                    log.debug("Not creating jobs for harvestdefinition with id #" + id
                            + " as the previous scheduling is still running");
                    continue;
                }

                final HarvestDefinition harvestDefinition = 
                    haDefinitionDAO.read(id);
                
                harvestDefinitionsBeingScheduled.add(id);

                if (!harvestDefinition.runNow(timeToGenerateJobsFor)) {
                    log.trace("The harvestdefinition '" +  
                            harvestDefinition.getName() +
                    "' should not run now.");
                    log.trace("numEvents: " + harvestDefinition.getNumEvents());
                    continue;
                }

                new Thread("JobGeneratorTask-" + id) {
                    public void run() {
                        try {
                            int jobsMade = harvestDefinition.createJobs();
                            log.info("Created " + jobsMade
                                    + " jobs for harvest definition '"
                                    + harvestDefinition.getName() + "'");
                            haDefinitionDAO.update(harvestDefinition);
                        } catch (Throwable e) {
                            try {
                                HarvestDefinition hd
                                = haDefinitionDAO.read(harvestDefinition.getOid());
                                hd.setActive(false);
                                haDefinitionDAO.update(hd);
                                String errMsg = "Exception while scheduling"
                                    + "harvestdefinition '" + 
                                    harvestDefinition.getName() + "'. The " +
                                    "harvestdefinition has been deactivated!";
                                log.warn(errMsg, e);
                                NotificationsFactory.getInstance().
                                errorEvent(errMsg, e);
                            } catch (Exception e1) {
                                String errMsg = "Exception while scheduling" + 
                                "harvestdefinition '" + harvestDefinition.getName() 
                                + "'. The harvestdefinition couldn't be " +
                                "deactivated!";
                                log.warn(errMsg, e);
                                log.warn("Unable to deactivate", e1);
                                NotificationsFactory.getInstance().
                                errorEvent(errMsg, e);
                            }
                        } finally {
                            harvestDefinitionsBeingScheduled.
                            remove(id);
                            log.debug("Removed '" + harvestDefinition.getName()
                                    + "' from list of harvestdefinitions to be "
                                    + "scheduled. Harvestdefinitions still to "
                                    + "be scheduled: "
                                    + harvestDefinitionsBeingScheduled);
                        }
                    }
                }.start();
            }
        }
    }
    
    //Hack, used by test
    static void clearGeneratingJobs() {
        harvestDefinitionsBeingScheduled.clear();
    }
}
