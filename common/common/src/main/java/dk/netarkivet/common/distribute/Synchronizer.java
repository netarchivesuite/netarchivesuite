/* File:      $Id$
 * Revision:  $Revision$
 * Author:    $Author$
 * Date:      $Date$
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
package dk.netarkivet.common.distribute;

import javax.jms.Message;
import javax.jms.MessageListener;
import java.util.Hashtable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;

/**
 * Converts an asynchronous call to a synchronous call. The method
 * sendAndWaitForOneReply() is a blocking call which responds when a
 * reply is received or returns null on timeout.
 *
 */
public class Synchronizer implements MessageListener {

    private final Log log = LogFactory.getLog(getClass().getName());

    /** Collection containing messages on which a reply is awaited. */
    private Hashtable<String, NetarkivetMessage> requests;

    /**
     * Collection containing reply messages which have not yet been returned to
     * the caller.
     */
    private Hashtable<String, NetarkivetMessage> replies;

    /**
     * Initialise maps containing requests and replies.
     */
    public Synchronizer() {
        requests = new Hashtable<String, NetarkivetMessage>();
        replies = new Hashtable<String, NetarkivetMessage>();
    }

    /**
     * Receives replies from a message queue and triggers the blocked call in
     * sendAndWaitForOneReply().
     *
     * @param msg an ObjectMessage containing a NetarkivetMessage.
     */
    public void onMessage(Message msg) {
        ArgumentNotValid.checkNotNull(msg, "msg");
        NetarkivetMessage naMsg = JMSConnection.unpack(msg);
        NetarkivetMessage requestMsg;
        synchronized (requests) {
            requestMsg = requests.get(naMsg.getReplyOfId());
        }
        if (requestMsg != null) {
            synchronized (requestMsg) {
                replies.put(naMsg.getReplyOfId(), naMsg);
                requestMsg.notifyAll();
            }
        } else {
            log.warn("Received unexpected reply for unknown message '"
                    + naMsg.getReplyOfId() + "'. Ignored!!");
        }
    }

    /**
     * Sends a message to a message queue and blocks the method
     * invocation until a reply arrives. If it times out a null is returned.
     * If a spurious wakeup is received and a timeout is set, the method will
     * carry on waiting for the reply until the total timeout time has been used
     * up. If a spurious wakeup is received and
     * no timeout is set the method will just go back to waiting
     *
     * @param msg the request message
     * @param timeout the timeout in milliseconds (or zero for no
     * timeout)
     * @return a reply message from the receiver of the request or null if
     * timed out.
     */
    public NetarkivetMessage sendAndWaitForOneReply
            (NetarkivetMessage msg, long timeout) {
        ArgumentNotValid.checkNotNull(msg, "msg");
        boolean noTimeout = (timeout == 0);
        JMSConnection con = JMSConnectionFactory.getInstance();
        synchronized (msg) {
            synchronized (requests) {
                con.send(msg);
                requests.put(msg.getID(), msg);
            }
            try {
                while (!replies.containsKey(msg.getID())) {
                    long timeBeforeWait = System.currentTimeMillis();
                    msg.wait(timeout);
                    synchronized (requests) {
                        if (!replies.containsKey(msg.getID())) {
                            // At this point we either got an unexpected wakeup
                            // or timed out
                            long timeAfterWait = System.currentTimeMillis();
                            // the new timeout value
                            timeout -= timeAfterWait - timeBeforeWait;
                            if (noTimeout || timeout > 0) {   //Unexpected wakeup
                                log.debug("Unexpected wakeup for "
                                        + msg.toString());
                            } else {
                                //timed out
                                // NB! if timeout is exactly zero here then this
                                // counts as a timeout. Otherwise we would call
                                // wait(0) on the next loop with disastrous
                                // results
                                requests.remove(msg.getID());
                                log.debug("Timed out waiting "
                                        + "for reply to "
                                        + msg.toString());
                                return null;
                            }
                        }
                    }
                }
            } catch (InterruptedException e) {
                throw new IOFailure("Interrupted while waiting for reply to "
                        + msg, e);
            }
        }
        // If we get here, we must have received the expected reply
        synchronized (requests) {
            requests.remove(msg.getID());
            log.debug("Received reply for message: " + msg.toString());
            return replies.remove(msg.getID());
        }
    }
}
