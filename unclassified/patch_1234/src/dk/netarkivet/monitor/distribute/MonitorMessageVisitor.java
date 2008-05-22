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

package dk.netarkivet.monitor.distribute;

import dk.netarkivet.monitor.registry.distribute.RegisterHostMessage;

/**
 * Interface for all classes which handles monitor-related messages received
 * from a JMS server. This is implemented with a visitor pattern:  Upon
 * receipt, the MonitorMessageHandler.onMessage() method invokes the
 * MonitorMessage.accept() method on the message with itself as argument.
 * The accept() method in turn invokes the MonitorMessageVisitor.visit() method,
 * using method overloading to invoke the visit method for the message received.
 *
 * Thus to handle a message, you should subclass MonitorMessageHandler and
 * override the visit() method for that kind of message.  You should not
 * implement this interface in any other way.
 *
 */
public interface MonitorMessageVisitor {
    /** This method should be overridden to handle the receipt of a message.
     *
     * @param msg A received message.
     */
    public void visit(RegisterHostMessage msg);
}