/*
 * #%L
 * Netarchivesuite - archive
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
package dk.netarkivet.archive.distribute;

import javax.jms.Message;
import javax.jms.MessageListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.archive.arcrepository.bitpreservation.AdminDataMessage;
import dk.netarkivet.archive.arcrepository.distribute.StoreMessage;
import dk.netarkivet.archive.bitarchive.distribute.BatchEndedMessage;
import dk.netarkivet.archive.bitarchive.distribute.BatchMessage;
import dk.netarkivet.archive.bitarchive.distribute.BatchReplyMessage;
import dk.netarkivet.archive.bitarchive.distribute.GetFileMessage;
import dk.netarkivet.archive.bitarchive.distribute.GetMessage;
import dk.netarkivet.archive.bitarchive.distribute.HeartBeatMessage;
import dk.netarkivet.archive.bitarchive.distribute.RemoveAndGetFileMessage;
import dk.netarkivet.archive.bitarchive.distribute.UploadMessage;
import dk.netarkivet.archive.checksum.distribute.CorrectMessage;
import dk.netarkivet.archive.checksum.distribute.GetAllChecksumsMessage;
import dk.netarkivet.archive.checksum.distribute.GetAllFilenamesMessage;
import dk.netarkivet.archive.checksum.distribute.GetChecksumMessage;
import dk.netarkivet.common.distribute.JMSConnection;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.PermissionDenied;

/**
 * This default message handler shields of all unimplemented methods from the ArchiveMessageVisitor interface.
 * <p>
 * Classes should not implement ArchiveMessageVisitor but extend this class.
 *
 * @see ArchiveMessageVisitor
 */
public abstract class ArchiveMessageHandler implements ArchiveMessageVisitor, MessageListener {

    /** The log. */
    private static final Logger log = LoggerFactory.getLogger(ArchiveMessageHandler.class);

    /**
     * Creates a ArchiveMessageHandler object.
     */
    public ArchiveMessageHandler() {
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
            ((ArchiveMessage) JMSConnection.unpack(msg)).accept(this);
        } catch (ClassCastException e) {
            log.warn("Invalid message type", e);
        } catch (Throwable t) {
            log.warn("Error processing message '{}'", msg, t);
        }
    }

    /**
     * Handles when a handler receives a message it is not prepare to handle.
     *
     * @param msg The received message.
     * @throws PermissionDenied Always
     */
    private void deny(ArchiveMessage msg) throws PermissionDenied {
        throw new PermissionDenied("'" + this + "' provides no handling for " + msg + " of type "
                + msg.getClass().getName() + " and should not be invoked!");
    }

    /**
     * This method should be overridden and implemented by a sub class if message handling is wanted.
     *
     * @param msg a BatchEndedMessage
     * @throws PermissionDenied when invoked
     */
    public void visit(BatchEndedMessage msg) throws PermissionDenied {
        ArgumentNotValid.checkNotNull(msg, "msg");
        deny(msg);
    }

    /**
     * This method should be overridden and implemented by a sub class if message handling is wanted.
     *
     * @param msg a BatchMessage
     * @throws PermissionDenied when invoked
     */
    public void visit(BatchMessage msg) throws PermissionDenied {
        ArgumentNotValid.checkNotNull(msg, "msg");
        deny(msg);
    }

    /**
     * This method should be overridden and implemented by a sub class if message handling is wanted.
     *
     * @param msg a BatchReplyMessage
     * @throws PermissionDenied when invoked
     */
    public void visit(BatchReplyMessage msg) throws PermissionDenied {
        ArgumentNotValid.checkNotNull(msg, "msg");
        deny(msg);
    }

    /**
     * This method should be overridden and implemented by a sub class if message handling is wanted.
     *
     * @param msg a GetFileMessage
     * @throws PermissionDenied when invoked
     */
    public void visit(GetFileMessage msg) throws PermissionDenied {
        ArgumentNotValid.checkNotNull(msg, "msg");
        deny(msg);
    }

    /**
     * This method should be overridden and implemented by a sub class if message handling is wanted.
     *
     * @param msg a GetMessage
     * @throws PermissionDenied when invoked
     */
    public void visit(GetMessage msg) throws PermissionDenied {
        ArgumentNotValid.checkNotNull(msg, "msg");
        deny(msg);
    }

    /**
     * This method should be overridden and implemented by a sub class if message handling is wanted.
     *
     * @param msg a HeartBeatMessage
     * @throws PermissionDenied when invoked
     */
    public void visit(HeartBeatMessage msg) throws PermissionDenied {
        ArgumentNotValid.checkNotNull(msg, "msg");
        deny(msg);
    }

    /**
     * This method should be overridden and implemented by a sub class if message handling is wanted.
     *
     * @param msg a StoreMessage
     * @throws PermissionDenied when invoked
     */
    public void visit(StoreMessage msg) throws PermissionDenied {
        ArgumentNotValid.checkNotNull(msg, "msg");
        deny(msg);
    }

    /**
     * This method should be overridden and implemented by a sub class if message handling is wanted.
     *
     * @param msg an UploadMessage
     * @throws PermissionDenied when invoked
     */
    public void visit(UploadMessage msg) throws PermissionDenied {
        ArgumentNotValid.checkNotNull(msg, "msg");
        deny(msg);
    }

    /**
     * This method should be overridden and implemented by a sub class if message handling is wanted.
     *
     * @param msg a AdminDataMessage
     * @throws PermissionDenied when invoked
     */
    public void visit(AdminDataMessage msg) throws PermissionDenied {
        ArgumentNotValid.checkNotNull(msg, "msg");
        deny(msg);
    }

    /**
     * This method should be overridden and implemented by a sub class if message handling is wanted.
     *
     * @param msg a RemoveAndGetFile
     * @throws PermissionDenied when invoked
     */
    public void visit(RemoveAndGetFileMessage msg) throws PermissionDenied {
        ArgumentNotValid.checkNotNull(msg, "msg");
        deny(msg);
    }

    /**
     * This method should be overridden and implemented by a sub class if message handling is wanted.
     *
     * @param msg an CorrectMessage for correcting a record.
     * @throws PermissionDenied when invoked
     */
    public void visit(CorrectMessage msg) throws PermissionDenied {
        ArgumentNotValid.checkNotNull(msg, "msg");
        deny(msg);
    }

    /**
     * This method should be overridden and implemented by a sub class if message handling is wanted.
     *
     * @param msg the GetChecksumMessage
     * @throws PermissionDenied when invoked
     */
    public void visit(GetChecksumMessage msg) throws PermissionDenied {
        ArgumentNotValid.checkNotNull(msg, "msg");
        deny(msg);
    }

    /**
     * This method should be overridden and implemented by a sub class if message handling is wanted.
     *
     * @param msg the GetAllChecksumMessage
     * @throws PermissionDenied when invoked
     */
    public void visit(GetAllChecksumsMessage msg) throws PermissionDenied {
        ArgumentNotValid.checkNotNull(msg, "msg");
        deny(msg);
    }

    /**
     * This method should be overridden and implemented by a sub class if message handling is wanted.
     *
     * @param msg an GetAllFilenamesMessage
     * @throws PermissionDenied when invoked
     */
    public void visit(GetAllFilenamesMessage msg) throws PermissionDenied {
        ArgumentNotValid.checkNotNull(msg, "msg");
        deny(msg);
    }

}
