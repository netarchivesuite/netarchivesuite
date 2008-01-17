/* $Id$
* $Revision$
* $Date$
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

package dk.netarkivet.deploy;

/**
 * Common interface for objects that work on
 * a deployment description (e.g. it-config.xml).
 *
 * In its first versions, only handles elements relevant
 * for the JMX agent.
 */
public interface DeploymentBuilder {
    /**
     * Configure the password that should be used
     * to monitor any of the applications using JMX.
     * @param password The password, in clear text, e.g. "kodeord".
     */
    public void setJmxPassword(String password);

    /**
     * Configure the location for which hosts
     * are about to be described.
     * @param location the given location
     */
    public void setLocation(String location);

    /**
     * Get builder for configuring a host in the deployment.
     * @return A new Host object.
     */
    public HostBuilder newHostBuilder();

    /**
     * Notify this object that the all of the
     * deployment description has been seen.
     * Perform post-parsing work.
     */
    public void done();

    /**
     * Interface for objects that work on the description
     * of a single host (server) in the deployment.
     *
     * Note that setters and adders may be called in any order.
     * The done() method must be called exactly once,
     * after all setting and adding has been done.
     */
    public interface HostBuilder {
        /**
         * Configure the name of the host.
         * @param serverName The full name of the server,
         * e.g. "sb-test-bar-001.sb.dk".
         */
        public void setName(String serverName);

        /**
         * Add an application running on this host.
         * @param jmxPortNo the port number assigned to monitor
         * the given process.
         */
        public void addJmxPort(int jmxPortNo);

        /**
         * Process the Host configuration - no
         * more changes or additions will be made.
         */
        public void done();
    }
}
