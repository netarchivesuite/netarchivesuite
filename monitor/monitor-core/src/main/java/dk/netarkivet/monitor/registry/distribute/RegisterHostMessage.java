/*
 * #%L
 * Netarchivesuite - monitor
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
package dk.netarkivet.monitor.registry.distribute;

import dk.netarkivet.common.distribute.Channels;
import dk.netarkivet.common.distribute.monitorregistry.HostEntry;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.monitor.distribute.MonitorMessage;
import dk.netarkivet.monitor.distribute.MonitorMessageVisitor;

/**
 * This type of message is sent to the monitor registry server to register the host for remote JMX monitoring.
 */
@SuppressWarnings({"serial"})
public class RegisterHostMessage extends MonitorMessage {
    /** The HostEntry to register. */
    HostEntry hostEntry;

    /**
     * Creates a message with the JMX host entry for a host registered to the monitor server.
     *
     * @param name The name of the remote host.
     * @param jmxPort The JMX port allocated on the remote host.
     * @param rmiPort The RMI port allocated on the remote host.
     * @throws ArgumentNotValid on null or empty hostname, or negative ports.
     */
    public RegisterHostMessage(String name, int jmxPort, int rmiPort) {
        super(Channels.getTheMonitorServer(), Channels.getError());
        ArgumentNotValid.checkNotNullOrEmpty(name, "String name");
        ArgumentNotValid.checkNotNegative(jmxPort, "int jmxPort");
        ArgumentNotValid.checkNotNegative(rmiPort, "int rmiPort");
        this.hostEntry = new HostEntry(name, jmxPort, rmiPort);
    }

    /**
     * Should be implemented as a part of the visitor pattern. e.g.: <code>
     * public void accept(MonitorMessageVisitor v) {
     * v.visit(this);
     * }
     * </code>
     *
     * @param v A message visitor.
     * @see MonitorMessageVisitor
     */
    public void accept(MonitorMessageVisitor v) {
        v.visit(this);
    }

    /**
     * Get the host entry for the host registering.
     *
     * @return The host entry.
     */
    public HostEntry getHostEntry() {
        return hostEntry;
    }
}
