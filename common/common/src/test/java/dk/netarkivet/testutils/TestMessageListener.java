/* File:    $Id$
 * Version: $Revision$
 * Date:    $Date$
 * Author:  $Author$
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

package dk.netarkivet.testutils;

import javax.jms.Message;
import javax.jms.MessageListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import dk.netarkivet.common.distribute.JMSConnection;
import dk.netarkivet.common.distribute.NetarkivetMessage;

/**
 * A simple message listener that collects the messages given to it and
 * lets you query them
 *
 */

public class TestMessageListener implements MessageListener {
    protected List<NetarkivetMessage> received = new ArrayList<NetarkivetMessage>();

    public TestMessageListener() {
    }

    public void onMessage(Message msg) {
        synchronized (this) {
            NetarkivetMessage content = JMSConnection.unpack(msg);
            received.add(content);
        }
    }

    /** Get the last message received.
     *
     * @return The last message received by the listener.
     * @throws IndexOutOfBoundsException if no messages have been received.
     */
    public NetarkivetMessage getReceived() {
        return received.get(received.size() - 1);
    }

    /** Return the number of messages received so far.
     * @return the number of messages received so far
     */
    public int getNumReceived() {
        return received.size();
    }

    /** get a list of all messages received.
     *
     * @return a list of all messages received
     */
    public List<NetarkivetMessage> getAllReceived() {
        return received;
    }

    /** Reset the list of messages returned.
     *
     */
    public void reset() {
        received.clear();
    }

    /** Returns the number of received messages that were ok.
     *  @return the number of received messages that were ok
     */
    public int getNumOk() {
        int count = 0;
        for (Iterator<NetarkivetMessage> i = received.iterator(); i.hasNext(); ) {
            NetarkivetMessage msg = i.next();
            if (msg.isOk()) {
                count++;
            }
        }
        return count;
    }

    /** Returns the number of received messages that were not ok.
     * @return  the number of received messages that were not ok
     */
    public int getNumNotOk() {
        int count = 0;
        for (Iterator<NetarkivetMessage> i = received.iterator(); i.hasNext(); ) {
            NetarkivetMessage msg = i.next();
            if (!msg.isOk()) {
                count++;
            }
        }
        return count;
    }

    /**
     * Looks up the messages that are instances of the given class.
     * @param msgClass A subclass of NetarkivetMessage, e.g. BatchMessage.class;
     * @return The instance that was most recently received, or null if no
     * messages of the specified type has been received.
     */
    public NetarkivetMessage getLastInstance(Class msgClass) {
        NetarkivetMessage result = null;
        for (NetarkivetMessage msg : received) {
            if (msgClass.isInstance(msg)) {
                result = msg;
            }
        }
        return result;
    }
}
