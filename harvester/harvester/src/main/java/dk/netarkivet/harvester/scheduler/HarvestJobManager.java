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
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301 USA
 */

package dk.netarkivet.harvester.scheduler;

import dk.netarkivet.common.distribute.JMSConnection;
import dk.netarkivet.common.distribute.JMSConnectionFactory;
import dk.netarkivet.common.lifecycle.LifeCycleComponent;
import dk.netarkivet.harvester.datamodel.HarvestChannelDAO;
import dk.netarkivet.harvester.datamodel.HarvestDBConnection;
import dk.netarkivet.harvester.datamodel.HarvestDefinitionDAO;
import dk.netarkivet.harvester.datamodel.JobDAO;

/**
 * Handles the dispatching of scheduled harvest to the harvest servers based on
 * the harvests defined in the database. <p>
 */
public class HarvestJobManager extends LifeCycleComponent {
    private final JMSConnection jmsConnection;

    /**
     * Creates the components handling the harvest job management and hooks them
     * up to the <code>HarvestJobManager</code>s lifecycle.
     */
    public HarvestJobManager() {
        jmsConnection = JMSConnectionFactory.getInstance();       
        JobDispatcher jobDispather = new JobDispatcher(
                jmsConnection,
                HarvestDefinitionDAO.getInstance(),
                JobDAO.getInstance()
        );
        HarvestChannelRegistry harvestChannelRegistry = new HarvestChannelRegistry();

        addChild(new HarvesterStatusReceiver(
                jobDispather,
                jmsConnection,
                HarvestChannelDAO.getInstance(),
                harvestChannelRegistry));
        
        addChild(new HarvestJobGenerator(jobDispather, harvestChannelRegistry));
        
        addChild(new HarvestSchedulerMonitorServer());
        
        addChild(new JobSupervisor());
    }

    @Override
    public void shutdown() {
        super.shutdown();

        // Release DB resources
        HarvestDBConnection.cleanup();
    }
}
