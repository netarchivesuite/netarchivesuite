/* File:      $Id$
 * Revision:  $Revision$
 * Author:    $Author$
 * Date:      $Date$
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
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301 
 *  USA
 */

package dk.netarkivet.archive.distribute;

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
import dk.netarkivet.archive.indexserver.distribute.IndexRequestMessage;

/**
 * Interface for all classes which handles archive-related messages received
 * from a JMS server. This is implemented with a visitor pattern:  Upon
 * receipt, the ArchiveMessageHandler.onMessage() method invokes the
 * ArchiveMessage.accept() method on the message with itself as argument.
 * The accept() method in turn invokes the ArchiveMessageVisitor.visit() method,
 * using method overloading to invoke the visit method for the message received.
 *
 * Thus to handle a message, you should subclass ArchiveMessageHandler and
 * override the visit() method for that kind of message.  You should not
 * implement this interface in any other way.
 *
 */
public interface ArchiveMessageVisitor {
    /** This method should be overridden to handle the receipt of a message.
     *
     * @param msg A received message.
     */
    void visit(BatchEndedMessage msg);

    /** This method should be overridden to handle the receipt of a message.
     *
     * @param msg A received message.
     */
    void visit(BatchMessage msg);

    /** This method should be overridden to handle the receipt of a message.
     *
     * @param msg A received message.
     */
    void visit(BatchReplyMessage msg);

    /** This method should be overridden to handle the receipt of a message.
     *
     * @param msg A received message.
     */
    void visit(GetFileMessage msg);

    /** This method should be overridden to handle the receipt of a message.
     *
     * @param msg A received message.
     */
    void visit(GetMessage msg);

    /** This method should be overridden to handle the receipt of a message.
     *
     * @param msg A received message.
     */
    void visit(HeartBeatMessage msg);

    /** This method should be overridden to handle the receipt of a message.
     *
     * @param msg A received message.
     */
    void visit(StoreMessage msg);

    /** This method should be overridden to handle the receipt of a message.
     *
     * @param msg A received message.
     */
    void visit(UploadMessage msg);

    /** This method should be overridden to handle the receipt of a message.
     *
     * @param msg A received message.
     */
    void visit(AdminDataMessage msg);

    /** This method should be overridden to handle the receipt of a message.
     *
     * @param msg A received message.
     */
    void visit(RemoveAndGetFileMessage msg);

    /** This method should be overridden to handle the receipt of a message.
     *
     * @param msg A received message.
     */
    void visit(IndexRequestMessage msg);
    
    /** This method should be overridden to handle the receipt of a message.
     * 
     * @param msg A received message.
     */
    void visit(GetChecksumMessage msg);
    
    /** This method should be overridden to handle the receipt of a message.
     * 
     * @param msg A received message.
     */
    void visit(GetAllChecksumsMessage msg);

    /** This method should be overridden to handle the receipt of a message.
     * 
     * @param msg A received message.
     */
    void visit(CorrectMessage msg);

    /** This method should be overridden to handle the receipt of a message.
     * 
     * @param msg A received message.
     */
    void visit(GetAllFilenamesMessage msg);
}
