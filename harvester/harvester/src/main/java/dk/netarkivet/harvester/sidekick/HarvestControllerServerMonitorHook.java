/* $Id$
 * $Date$
 * $Revision$
 * $Author$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2007 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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
package dk.netarkivet.harvester.sidekick;

import java.io.File;

import dk.netarkivet.common.Settings;
import dk.netarkivet.common.exceptions.NotImplementedException;

/**
 */
public class HarvestControllerServerMonitorHook extends DefaultMonitorHook {

    /**
     * Returns true if the application is running.
     *
     * @return true if the application is running
     */
    public boolean isRunning() {
        File file = new File(Settings.get(Settings.HARVEST_CONTROLLER_ISRUNNING_FILE) );
        return file.exists();
    }

    /**
     * Not implemented!
     */
    public String getReport() {
        throw new NotImplementedException("Not implemented!");
    }

    public String toString() {
        return "HarvestControllerServer";
    }
}
