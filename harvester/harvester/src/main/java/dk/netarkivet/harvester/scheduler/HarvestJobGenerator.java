package dk.netarkivet.harvester.scheduler;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.common.lifecycle.ComponentLifeCycle;
import dk.netarkivet.common.utils.NotificationsFactory;
import dk.netarkivet.harvester.datamodel.HarvestDefinition;
import dk.netarkivet.harvester.datamodel.HarvestDefinitionDAO;

/* File:    $Id: $
 * Revision: $Revision: $
 * Author:   $Author: $
 * Date:     $Date: $
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

/**
 * Handles the generation of new jobs based on the harvest definitions in persistent storage. 
 * The <code>HarvestJobGenerator</code> continuously scans the harvest definition database for harvest which 
 * should be run now. If a HD defines a harvest which should be run, a Harvest Job is created in the harvest
 * job database.
 */
public class HarvestJobGenerator implements ComponentLifeCycle {
    private static final Log log = LogFactory.getLog(HarvestJobGenerator.class.getName());

    /** The Timer used to schedule the generator jobs. */
    private Timer generationTimer;

    private static final HarvestDefinitionDAO haDefinitionDAO = HarvestDefinitionDAO.getInstance();

    /** 
     * The period between checking if new jobs should be generated.
     * This is one minute because that's the finest we can define in a harvest
     * definition.
     */
    private static final int GENERATE_JOBS_PERIOD = 60*1000;

    /**
     * Starts the job generation scheduler.
     */
    @Override
    public void start() {
        generationTimer = new Timer(true);
        generationTimer.scheduleAtFixedRate(new JobGeneratorTask(), 0, GENERATE_JOBS_PERIOD);
    }

    @Override
    public void shutdown() {
        generationTimer.cancel();
    }    

    /**
     * Contains the functionality for the individual JobGenerations 
     */
    public static class JobGeneratorTask extends TimerTask {
        @Override
        public synchronized void run() {
            final Iterable<Long> readyHarvestDefinitions = haDefinitionDAO.getReadyHarvestDefinitions(new Date());
            for (final Long id : readyHarvestDefinitions) {                
                final HarvestDefinition harvestDefinition = haDefinitionDAO.read(id);

                if (!harvestDefinition.runNow(new Date())) {
                    log.trace("The harvestdefinition '" +  harvestDefinition.getName()
                            + "' should not run now.");
                    log.trace("numEvents: " + harvestDefinition.getNumEvents());
                    continue;
                }
                
                new Thread("JobGeneratorTask-"+id) {
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
                                    + "harvestdefinition '"+ harvestDefinition.getName()
                                    + "'. The harvestdefinition has been" + " deactivated!";
                                log.warn(errMsg, e);
                                NotificationsFactory.getInstance().errorEvent(errMsg, e);
                            } catch (Exception e1) {
                                String errMsg = "Exception while scheduling" + "harvestdefinition '"
                                    + harvestDefinition.getName() + "'. The harvestdefinition couldn't be"
                                    + " deactivated!";
                                log.warn(errMsg, e);
                                log.warn("Unable to deactivate", e1);
                                NotificationsFactory.getInstance().errorEvent(errMsg, e);
                            }
                        } 
                    }
                }.start();
            }
        }
    }
}
