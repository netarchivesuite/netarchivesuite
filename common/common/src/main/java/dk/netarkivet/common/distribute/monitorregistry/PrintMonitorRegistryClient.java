/* $Id$
 * $Date$
 * $Revision$
 * $Author$
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
package dk.netarkivet.common.distribute.monitorregistry;

import dk.netarkivet.common.utils.JMXUtils;

/**
 * A trivial monitor registry client, that doesn't register anywhere, but simply
 * reports where it might be monitored on stdout.
 */
public class PrintMonitorRegistryClient implements MonitorRegistryClient {
    /** Simply print info given in constructor to stdout.
     *
     * @param hostName Name of host you can monitor this application on.
     * @param jmxPort JMX port you can monitor this application on.
     * @param rmiPort RMI port communication will happen on. 
     */
    public void register(String hostName, int jmxPort, int rmiPort) {
        System.out.println("This client may be monitored on '"
                           + JMXUtils.getUrl(hostName, jmxPort, rmiPort)
                           + "'");
    }
}
