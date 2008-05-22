/* File:        $Id$
* Revision:    $Revision$
* Author:      $Author$
* Date:        $Date$
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
package dk.netarkivet.common.distribute.monitorregistry;

import java.io.Serializable;
import java.util.Date;

import dk.netarkivet.common.exceptions.ArgumentNotValid;

/**
 * Helper class to encapsulate information about one remote JmxConnection.
 */
public class HostEntry implements Serializable {
    /**
     * The name of the remote host.
     */
    private final String name;
    /**
     * The JMX port allocated on the remote host.
     */
    private final int jmxPort;
    /**
     * The RMI port allocated on the remote host.
     */
    private final int rmiPort;
    /**
     * The time this host-entry was last seen alive.
     */
    private Date time;

    /**
     * Constructor for the HostEntry helper class.
     *
     * @param name    The name of the remote host
     * @param jmxPort The JMX port allocated on the remote host
     * @param rmiPort The RMI port allocated on the remote host
     */
    public HostEntry(String name, int jmxPort, int rmiPort) {
        ArgumentNotValid.checkNotNullOrEmpty(name, "String name");
        ArgumentNotValid.checkPositive(jmxPort, "int jmxPort");
        ArgumentNotValid.checkPositive(rmiPort, "int rmiPort");
        this.name = name;
        this.jmxPort = jmxPort;
        this.rmiPort = rmiPort;
        this.time = new Date();
    }

    /**
     * Get the JMX port for connections.
     * @return The jmx port.
     */
    public int getJmxPort() {
        return jmxPort;
    }

    /**
     * Get the host name.
     * @return The name.
     */
    public String getName() {
        return name;
    }

    /**
     * Get the RMI port for connections.
     * @return The rmi port.
     */
    public int getRmiPort() {
        return rmiPort;
    }

    /**
     * Get the time this host was last seen alive.
     * @return The time this host was last seen alive.
     */
    public Date getTime() {
        return time;
    }

    /**
     * Return whether two hosts are equal.
     *
     * Two hosts are considered equal, if they have the same name and JMX/RMI
     * ports. However, the time last seen alive is not, and should not be,
     * considered.
     *
     * @see Object#equals(Object)
     *
     * @param obj The host to compare with.
     * @return Whether the two objects represent the same host.
     */
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof HostEntry)) return false;

        final HostEntry hostEntry1 = (HostEntry) obj;

        if (name != null ? !name.equals(hostEntry1.name)
                : hostEntry1.name != null) return false;

        if (jmxPort != hostEntry1.jmxPort){
            return false;
        }
        if (rmiPort != hostEntry1.rmiPort) {
            return false;
        }
        return true;
    }

    /**
     * Return hash code. Coded to be consistent with equals.
     *
     * @return Hash code for this object.
     *
     * @see Object#hashCode()
     */
    public int hashCode() {
        int result;
        result = (name != null ? name.hashCode() : 0);
        result = 29 * result + jmxPort*1;
        result = 29 * result + rmiPort*2;
        return result;
    }

    /**
     * Get a human readable representation of this host and ports.
     * @return A human readable string.
     */
    public String toString() {
        return "Host=" + name + ", JMXport=" + jmxPort + ", RMIport=" + rmiPort
                + ", last seen live at " + time;
    }

    /**
     * Update the time for when the host was last seen alive.
     * @param time The time last seen alive.
     * @throws ArgumentNotValid on null parameter.
     */
    public void setTime(Date time) {
        ArgumentNotValid.checkNotNull(time, "Date time");
        this.time=time;
    }
}
