/*
 * #%L
 * Netarchivesuite - common
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
package dk.netarkivet.common.distribute.monitorregistry;

import dk.netarkivet.common.utils.JMXUtils;

/**
 * A trivial monitor registry client, that doesn't register anywhere, but simply reports where it might be monitored on
 * stdout.
 */
public class PrintMonitorRegistryClient implements MonitorRegistryClient {
    /**
     * Simply print info given in constructor to stdout.
     *
     * @param hostName Name of host you can monitor this application on.
     * @param jmxPort JMX port you can monitor this application on.
     * @param rmiPort RMI port communication will happen on.
     */
    public void register(String hostName, int jmxPort, int rmiPort) {
        System.out.println("This client may be monitored on '" + JMXUtils.getUrl(hostName, jmxPort, rmiPort) + "'");
    }
}
