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

package dk.netarkivet.harvester.distribute;

import javax.jms.Message;
import javax.jms.MessageListener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.common.distribute.JMSConnection;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.PermissionDenied;
import dk.netarkivet.harvester.harvesting.distribute.CrawlStatusMessage;
import dk.netarkivet.harvester.harvesting.distribute.DoOneCrawlMessage;

/**
 * This default message handler shields of all unimplemented methods from the
 * HarvesterMessageVisitor interface.
 *
 * Classes should not implement HarvesterMessageVisitor but extend this class.
 *
 * @see HarvesterMessageVisitor
 *
 */
public abstract class HarvesterMessageHandler
        implements HarvesterMessageVisitor, MessageListener {

    private final Log log = LogFactory.getLog(getClass().getName());

    /**
     * Creates a HarvesterMessageHandler object.
     */
    public HarvesterMessageHandler() {
    }

    /**
     * Unpacks and calls accept() on the message object.
     *
     * This method catches <b>all</b> exceptions and logs them.
     *
     * @param msg an ObjectMessage
     */
    public void onMessage(Message msg) {
        ArgumentNotValid.checkNotNull(msg, "msg");
        log.trace("Message received:\n" + msg.toString());
        try {
            ((HarvesterMessage) JMSConnection.unpack(msg)).accept(this);
        } catch (ClassCastException e) {
            log.warn("Invalid message type", e);
        } catch (Throwable e) {
            log.warn("Error processing message '" + msg + "'", e);
        }
    }

    /** Handles when a handler receives a message it is not prepare to handle.
     *
     * @param msg The received message.
     * @throws PermissionDenied Always
     */
    private void deny(HarvesterMessage msg) {
        throw new PermissionDenied("'" + this + "' provides no handling for "
                + msg + " of type " + msg.getClass().getName()
                + " and should not be invoked!");
    }

    /**
     * This method should be overridden and implemented by a sub class if
     * message handling is wanted.
     * @param msg a CrawlStatusMessage
     * @throws PermissionDenied when invoked
     */
    public void visit(CrawlStatusMessage msg) throws PermissionDenied {
        ArgumentNotValid.checkNotNull(msg, "msg");
        deny(msg);
    }

    /**
     * This method should be overridden and implemented by a sub class if
     * message handling is wanted.
     * @param msg a DoOneCrawlMessage
     * @throws PermissionDenied when invoked
     */
    public void visit(DoOneCrawlMessage msg) throws PermissionDenied {
        ArgumentNotValid.checkNotNull(msg, "msg");
        deny(msg);
    }
}
