/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
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
package dk.netarkivet.harvester.distribute;

import dk.netarkivet.common.distribute.JMSConnection;
import dk.netarkivet.common.distribute.JMSConnectionFactory;
import dk.netarkivet.harvester.datamodel.JobPriority;
import dk.netarkivet.harvester.harvesting.distribute.HarvestControllerServer;
import dk.netarkivet.harvester.harvesting.distribute.HarvesterStatusMessage;
import dk.netarkivet.harvester.scheduler.HarvestDispatcher;

/**
 * This utility class allows users to use the command line to send
 * {@link HarvesterStatusMessage}s from the command line to fix the
 * {@link HarvestDispatcher} internal state.
 *
 * This is especially useful to remove a crashed {@link HarvestControllerServer}
 * from the list of available harvesters.
 *
 */
public class HarvesterStatusNotifier {

    /**
     * @param args
     */
    public static void main(String[] args) {

        if (args.length != 3) {
            printUsage();
        }

        String appInstId = args[0];
        JobPriority p = JobPriority.fromString(args[1]);
        boolean available = Boolean.parseBoolean(args[2]);

        JMSConnection jmsConn = JMSConnectionFactory.getInstance();
        jmsConn.send(new HarvesterStatusMessage(appInstId, p, available));

        System.out.println("Successfully sent message.");
        System.exit(0);
    }

    public static final void printUsage() {
        System.out.println("Usage: java "
                + HarvesterStatusNotifier.class.getCanonicalName()
                + "\n<application instance id : string>"
                + "\n<job_priority [HIGHPRIORITY | LOWPRIORITY]>"
                + "\n<available [true | false]>");
    }
}
