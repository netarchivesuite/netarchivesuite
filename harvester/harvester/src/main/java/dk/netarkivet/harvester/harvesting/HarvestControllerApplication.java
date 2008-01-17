/* File:                $Id$
 * Revision:            $Revision$
 * Author:              $Author$
 * Date:                $Date$
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
package dk.netarkivet.harvester.harvesting;

import java.io.File;
import java.io.IOException;

import dk.netarkivet.common.Settings;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.ApplicationUtils;
import dk.netarkivet.harvester.harvesting.distribute.HarvestControllerServer;

/**
 * This application controls the Heritrix harvester which does the actual
 * harvesting, and is also responsible for uploading the harvested data to the
 * ArcRepository.
 *
 * @see HarvestControllerServer
 */
public class HarvestControllerApplication {
    /**
     * Runs the HarvestController Application. Settings are read from config
     * files.
     *
     * Creates a file './hcsRunning.tmp' which is used by a SideKick to check if
     * the application is running. The file has been marked 'deleteOnExit()'.
     *
     * @param args an empty array
     * @throws IOFailure if unable to create ISRUNNING_FILE
     */
    public static void main(String[] args) {
        File file = new File(Settings.get(
                Settings.HARVEST_CONTROLLER_ISRUNNING_FILE));
        try {
            file.createNewFile();
            file.deleteOnExit();
            //TODO: Don't log here, because it initialises the logger
            //before the application ID is set. Should be handled better.
            //log.info("Created file '" + file.getAbsolutePath()
            //         + "' to indicate this process is running. "
            //         + "Will delete on exit.");
        } catch (IOException e) {
            throw new IOFailure("Couldn't create file '" + file
                                + "' to indicate this process is running.", e);
        }
        ApplicationUtils.startApp(HarvestControllerServer.class, args);
    }
}
