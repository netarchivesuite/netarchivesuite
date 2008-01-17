/*$Id$
* $Revision$
* $Date$
* $Author$
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
package dk.netarkivet.archive.arcrepository.distribute;

import java.io.File;

import dk.netarkivet.archive.distribute.ArchiveMessage;
import dk.netarkivet.archive.distribute.ArchiveMessageVisitor;
import dk.netarkivet.common.distribute.ChannelID;
import dk.netarkivet.common.distribute.Channels;
import dk.netarkivet.common.distribute.RemoteFile;
import dk.netarkivet.common.distribute.RemoteFileFactory;
import dk.netarkivet.common.exceptions.ArgumentNotValid;

/**
 * Messages requesting store of file. This message is sent to the arc repository
 * which distributes the file to the known bitarchives, and checks the result,
 * and then responds to the sender.
 */
public class StoreMessage extends ArchiveMessage {
    /** prefix to identify this message type. */
    private static final String STORE_MESSAGE_PREFIX = "Store";

    /** The actual data. */
    private RemoteFile theRemoteFile;

    /**
     * Construct StoreMessage.
     * @param replyTo Channel to reply back to
     * @param arcfile The file to store
     */
    public StoreMessage(ChannelID replyTo, File arcfile) {
        super(Channels.getTheArcrepos(), replyTo, STORE_MESSAGE_PREFIX);
        ArgumentNotValid.checkNotNull(arcfile, "arcfile");
        theRemoteFile = RemoteFileFactory.getDistributefileInstance(arcfile);
    }

  /**
   * Retrieve name of the stored file.
   * @return current value of arcfileName
   */
    public String getArcfileName() {
      return theRemoteFile.getName();
    }

  /**
   * Get method for field theRemoteFile.
   * @return Current value of theRemoteFile
   */
    public RemoteFile getRemoteFile() {
        return theRemoteFile;
    }

    /**
     * Should be implemented as a part of the visitor pattern. fx.: public void
     * accept(ArchiveMessageVisitor v) { v.visit(this); }
     *
     * @param v A message visitor
     */
    public void accept(ArchiveMessageVisitor v) {
        v.visit(this);
    }

    /**
     * Generate String representation of this object.
     * @return String representation of this object
     */
    public String toString() {
        return super.toString() + " Arcfile: " + getArcfileName();
    }

}
