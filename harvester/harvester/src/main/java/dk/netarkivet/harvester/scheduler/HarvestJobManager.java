/*
 * #%L
 * Netarchivesuite - harvester
 * %%
 * Copyright (C) 2005 - 2014 The Royal Danish Library, the Danish State and University Library,
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

import javax.inject.Provider;

import dk.netarkivet.common.distribute.JMSConnection;
import dk.netarkivet.common.distribute.JMSConnectionFactory;
import dk.netarkivet.common.lifecycle.LifeCycleComponent;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.harvester.HarvesterSettings;
import dk.netarkivet.harvester.datamodel.HarvestChannelDAO;
import dk.netarkivet.harvester.datamodel.HarvestDBConnection;
import dk.netarkivet.harvester.datamodel.HarvestDefinitionDAO;
import dk.netarkivet.harvester.datamodel.JobDAO;

/**
 * Handles the dispatching of scheduled harvest to the harvest servers based on the harvests defined in the database.
 * <p>
 */
public class HarvestJobManager extends LifeCycleComponent {

    private final JMSConnection jmsConnection;

    /**
     * Creates the components handling the harvest job management and hooks them up to the
     * <code>HarvestJobManager</code>s lifecycle.
     */
    public HarvestJobManager() {
        jmsConnection = JMSConnectionFactory.getInstance();
        JobDispatcher jobDispather = new JobDispatcher(jmsConnection, HarvestDefinitionDAO.getInstance(), JobDAO
                .getInstance());
        HarvestChannelRegistry harvestChannelRegistry = new HarvestChannelRegistry();

        addChild(new HarvesterStatusReceiver(jobDispather, jmsConnection, HarvestChannelDAO.getInstance(),
                harvestChannelRegistry));

        addChild(new HarvestJobGenerator(harvestChannelRegistry));

        addChild(new HarvestSchedulerMonitorServer());

        addChild(new JobSupervisor(createJobDaoProvider(), Settings.getLong(HarvesterSettings.JOB_TIMEOUT_TIME)));
    }

    @Override
    public void shutdown() {
        super.shutdown();

        // Release DB resources
        HarvestDBConnection.cleanup();
    }

    private Provider<JobDAO> createJobDaoProvider() {
        return new Provider<JobDAO>() {
            @Override
            public JobDAO get() {
                return JobDAO.getInstance();
            }
        };
    }

}
