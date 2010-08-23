package dk.netarkivet.harvester.scheduler;

import dk.netarkivet.common.lifecycle.ComponentLifeCycle;
import dk.netarkivet.common.utils.CleanupIF;

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
 * Handles the dispatching of scheduled harvest to the harvest servers based on the
 * harvests defined in the database. <p>
 * 
 * Handles backup of the harvest job DB (Note, this is note implemented, the backup code should be moved from the 
 * HarvestSchduler to this class).
 */
public class HarvestJobManager implements ComponentLifeCycle {
    /** The dispatcher being started to send scheduled jobs to the harvest servers. */
    private HarvestScheduler harvestJobDispatcher;
    
    /**
     * Creates the <code>HarvestScheduler</code> and starts it.
     */
    public HarvestJobManager() {
        harvestJobDispatcher = new HarvestScheduler();
    }

    @Override
    public void start() {
        harvestJobDispatcher.start();
    }

    /**
     * Shuts down the <code>HarvestScheduler</code>.
     * 
     * @override
     */
    public void shutdown() {
        if (harvestJobDispatcher != null) {
            harvestJobDispatcher.close();
        }
    }
}
