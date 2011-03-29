/* $Id$
 * $Revision$
 * $Author$
 * $Date$
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
package dk.netarkivet.common.distribute;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.PermissionDenied;
import dk.netarkivet.common.utils.ExceptionUtils;


/**
 * Common base class for all messages exchanged in the NetarchiveSuite.
 */
public abstract class NetarkivetMessage implements Serializable {

    // contains the error messages when isOk=false
    private String errMsg;

    // when false an error occurred processing the message
    private boolean isOk = true;

    // id of this message. Is set when sent and null until then
    private String id;

    // Channel to which this message is to be sent
    private ChannelID to;

    // Channel on which replies are expected
    private ChannelID replyTo;

    // a delimiter to separate error messages
    static final String ERROR_DELIMITTER = "\n-----------------\n";

    protected String replyOfId;

    /**
     * Creates a new NetarkivetMessage.
     * @param to the initial receiver of the message
     * @param replyTo the initial sender of the message
     * @throws ArgumentNotValid if to==replyTo, the replyTo parameter is a
     * topic instead of a queue, or there is a null parameter.
     */
    protected NetarkivetMessage(ChannelID to, ChannelID replyTo) {
        ArgumentNotValid.checkNotNull(to, "to");
        ArgumentNotValid.checkNotNull(replyTo, "replyTo");

        if (to.getName().equals(replyTo.getName())) {
            throw new ArgumentNotValid("to and replyTo should not be equal.");
        }

        // Have not implemented replying to a topic because there is no use
        // for it in our current architecture
        if (replyTo.isTopic()) {
            throw new ArgumentNotValid("Reply channel must be queue but "
                    + replyTo.toString() + " is a Topic");
        }

        this.to = to;
        this.replyTo = replyTo;
        this.id = null;
        this.replyOfId = null;
    }

    /**
     * Did an error occur when processing the message.
     * @return true if no error occurred, otherwise false
     */
    public boolean isOk() {
        return isOk;
    }

    /**
     * Set or append error message. Sets isOk field to false.
     * @param err error message
     */
    public void setNotOk(String err) {
        if (isOk) {
            errMsg = err;
            this.isOk = false;
        } else {
            errMsg += ERROR_DELIMITTER;
            errMsg += err;
        }
    }

    /** Set error message based on an exception.
     *
     * @param e An exception thrown during processing.
     */
    public void setNotOk(Throwable e) {
        setNotOk(e.toString()+"\n"+ExceptionUtils.getStackTrace(e));
    }

    /**
     * Retrieve error message.
     * @throws PermissionDenied if the message is not an error message
     * @return error message
     */
    public String getErrMsg() throws PermissionDenied{
        if (isOk) {
            throw new PermissionDenied("Can't get error message for message '"
                    + this + " that has had no error");
        }
        return errMsg;
    }

    /**
     * Retrieve message id. Note that message ID is not set until message is
     * sent, and this method must not be called before then.
     * @return message id
     * @throws PermissionDenied If the message has not yet been sent.
     */
    public synchronized String getID() {
        if (id == null) {
            throw new PermissionDenied("This message has not been sent, and "
                                       + "does not yet have an ID");
        }
        return id;
    }

    /** Sets the ID of this message if it has not already been set.
     *
     * @param newId The new ID
     */
    synchronized void updateId(String newId) {
        if (this.id == null) {
            this.id = newId;
        }
        if (this.replyOfId == null) {
            this.replyOfId = newId;
        }
    }

    /**
     * Retrieve replyOfId. This is set by subclasses of NetarkivetMessage, to
     * indicate that this is a reply of some other message. If
     * the subclass doesn't set replyOfId, this method behaves like getId.
     * @return replyOfId
     */
    public String getReplyOfId() {
        if (replyOfId != null) {
            return replyOfId;
        } else {
            return getID();
        }
    }

    /**
     * Retrieve initial destination.
     * @return initial destination
     */
    public ChannelID getTo() {
        return to;
    }

    /**
     * Retrieve specified reply channel.
     * @return initial origin
     */
    public ChannelID getReplyTo() {
        return replyTo;
    }

    /**
     * Returns a string containing:
     * <id>: To <toName> ReplyTo <replyToName> <isOK> [:error message].
     * @return String representation of Message.
     */
    public String toString() {
        String s = (id == null ? "NO ID" : id);
        s += ": To " + to.getName() + " ReplyTo " + replyTo.getName();
        s += isOk()?" OK":" Error: " + errMsg;
        return s;
    }

    /**
     * Invoke default method for deserializing object.
     * @param s The stream the object is read from.
     */
    private void readObject(ObjectInputStream s) {
        try {
            s.defaultReadObject();
        } catch (Exception e) {
            throw new IOFailure("Unexpected error during deserialization", e);
        }
    }

    /**
     * Invoke default method for serializing object.
     * @param s The stream the object is written to.
     */
    private void writeObject(ObjectOutputStream s){
        try {
            s.defaultWriteObject();
        } catch (Exception e) {
            throw new IOFailure("Unexpected error during serialization", e);
        }
    }

    /**
     * Check, if a given message has been sent yet.
     * If the message has a null id, it hasn't been sent yet.
     * @return true, if message has been sent yet, false otherwise.
     */
    public synchronized boolean hasBeenSent() {
        return (this.id != null);
    }
}

