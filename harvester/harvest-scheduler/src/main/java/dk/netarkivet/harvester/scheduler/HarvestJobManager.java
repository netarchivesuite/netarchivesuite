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

import javax.inject.Provider;

import dk.netarkivet.common.distribute.JMSConnection;
import dk.netarkivet.common.distribute.JMSConnectionFactory;
import dk.netarkivet.common.lifecycle.LifeCycleComponent;
import dk.netarkivet.common.utils.Notifications;
import dk.netarkivet.common.utils.NotificationsFactory;
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
    /**
     * Creates the components handling the harvest job management and hooks them up to the
     * <code>HarvestJobManager</code>s lifecycle.
     */
    public HarvestJobManager() {
        JobDispatcher jobDispather = new JobDispatcher(getJMSConnectionProvider().get(), HarvestDefinitionDAO.getInstance(),
                JobDAO.getInstance());
        HarvestChannelRegistry harvestChannelRegistry = new HarvestChannelRegistry();

        addChild(new HarvesterStatusReceiver(jobDispather, getJMSConnectionProvider().get(), HarvestChannelDAO.getInstance(),
                harvestChannelRegistry));

        addChild(new HarvestJobGenerator(harvestChannelRegistry));

        addChild(new HarvestSchedulerMonitorServer(
                getJMSConnectionProvider(),
                getJobDAOProvider(),
                getHarvestDefinitionDAOProvider(),
                getNotificationsProvider()
        ));

        addChild(new JobSupervisor(getJobDAOProvider(), Settings.getLong(HarvesterSettings.JOB_TIMEOUT_TIME)));
    }

    @Override
    public void shutdown() {
        super.shutdown();
        HarvestDBConnection.cleanup();
    }
/*
    public static Provider<JMSConnection> getJMSConnectionProvider() {
        return () -> JMSConnectionFactory.getInstance();
    }
    public static Provider<JobDAO> getJobDAOProvider() {
        return () -> JobDAO.getInstance();
    }
    public static Provider<HarvestDefinitionDAO> getHarvestDefinitionDAOProvider() {
        return () -> HarvestDefinitionDAO.getInstance();
    }
    public static Provider<Notifications> getNotificationsProvider() {
        return () -> NotificationsFactory.getInstance();
    }
*/

    public static Provider<JMSConnection> getJMSConnectionProvider() {
        return new Provider<JMSConnection>() {

            @Override
            public JMSConnection get() {
                return JMSConnectionFactory.getInstance();
            }};
    }
    public static Provider<JobDAO> getJobDAOProvider() {
        return new Provider<JobDAO>() {

            @Override
            public JobDAO get() {
                return JobDAO.getInstance();
            }};
    }
    public static Provider<HarvestDefinitionDAO> getHarvestDefinitionDAOProvider() {
        return new Provider<HarvestDefinitionDAO>() {

            @Override
            public HarvestDefinitionDAO get() {
                return HarvestDefinitionDAO.getInstance();
            }};
    }
    public static Provider<Notifications> getNotificationsProvider() {
        return new Provider<Notifications>() {

            @Override
            public Notifications get() {
                return NotificationsFactory.getInstance();
            }};
    }
 
}
