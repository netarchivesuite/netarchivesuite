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
package dk.netarkivet.monitor.distribute;

import javax.jms.Message;
import javax.jms.MessageListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.distribute.JMSConnection;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.PermissionDenied;
import dk.netarkivet.monitor.registry.distribute.RegisterHostMessage;

/**
 * This default message handler shields of all unimplemented methods from the MonitorMessageVisitor interface.
 * <p>
 * Classes should not implement MonitorMessageVisitor but extend this class.
 *
 * @see MonitorMessageVisitor
 */
public abstract class MonitorMessageHandler implements MonitorMessageVisitor, MessageListener {

    private static final Logger log = LoggerFactory.getLogger(MonitorMessageHandler.class);

    /**
     * Creates a MonitorMessageHandler object.
     */
    public MonitorMessageHandler() {
    }

    /**
     * Unpacks and calls accept() on the message object.
     * <p>
     * This method catches <b>all</b> exceptions and logs them.
     *
     * @param msg a ObjectMessage
     */
    public void onMessage(Message msg) {
        ArgumentNotValid.checkNotNull(msg, "Message msg");
        log.trace("Message received:\n{}", msg.toString());
        try {
            ((MonitorMessage) JMSConnection.unpack(msg)).accept(this);
        } catch (ClassCastException e) {
            log.warn("Invalid message type", e);
        } catch (Throwable e) {
            log.warn("Error processing message '{}'", msg, e);
        }
    }

    /**
     * Handles when a handler receives a message it is not prepare to handle.
     *
     * @param msg The received message.
     * @throws PermissionDenied Always
     */
    private void deny(MonitorMessage msg) {
        throw new PermissionDenied("'" + this + "' provides no handling for " + msg + " of type "
                + msg.getClass().getName() + " and should not be invoked!");
    }

    /**
     * This method should be overridden and implemented by a sub class if message handling is wanted.
     *
     * @param msg a RegisterHostMessage
     * @throws PermissionDenied when invoked
     */
    public void visit(RegisterHostMessage msg) throws PermissionDenied {
        ArgumentNotValid.checkNotNull(msg, "RegsiterHostMessage msg");
        deny(msg);
    }

}
