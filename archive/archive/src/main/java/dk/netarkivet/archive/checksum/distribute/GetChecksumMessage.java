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
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301 
 *  USA
 */
package dk.netarkivet.archive.checksum.distribute;

import dk.netarkivet.archive.distribute.ArchiveMessage;
import dk.netarkivet.archive.distribute.ArchiveMessageVisitor;
import dk.netarkivet.common.distribute.ChannelID;
import dk.netarkivet.common.exceptions.ArgumentNotValid;

/**
 * The GetChecksumMessage has the purpose to retrieve the checksum of a 
 * specific file.
 * 
 * This is checksum replica alternative to sending a ChecksumBatchJob, with
 * a filename limitation.
 * 
 *  TODO: decide whether to sent this to a specific replica
 */
public class GetChecksumMessage extends ArchiveMessage {
    /** A random generated serial version UID.*/
    private static final long serialVersionUID = 3562485628628056203L;

    /** The prefix for this message.*/
    static final String GET_CHECKSUM_MESSAGE_PREFIX = "GetChecksum";
    /** The name of the arc file to retrieve the checksum from.*/
    private String arcFilename;
    /** The resulting checksum for the arcFile.*/
    private String checksum;

    /**
     * Constructor.
     *  
     * @param to Where this message should be sent.
     * @param replyTo Where the reply for this message should be sent.
     * @param filename The name of the file.
     */
    protected GetChecksumMessage(ChannelID to, ChannelID replyTo, 
	    String filename) {
        super(to, replyTo, GET_CHECKSUM_MESSAGE_PREFIX);
        // validate arguments
        ArgumentNotValid.checkNotNull(to, "ChannelID to");
        ArgumentNotValid.checkNotNull(replyTo, "ChannelID replyTo");
        ArgumentNotValid.checkNotNullOrEmpty(filename, "String filename");
        
        arcFilename = filename;
    }
    
    /**
     * Retrieve name of the uploaded file.
     * @return current value of arcfileName
     */
      public String getArcfileName() {
        return arcFilename;
      }
      
      /**
       * 
       * @return
       */
      public String getChecksum() {
	  if(checksum == null) {
	      // TODO: handle the case when the checksum has not been set.
//	      throw new IllegalState("The checksum has not been calculated.");
	  }
	  return checksum;
      }
      
      /**
       * Method for returning the result of the checksum.
       * 
       * @param cs The checksum.
       */
      public void setChecksum(String cs) {
	  checksum = cs;
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
          return super.toString() + " Arcfiles: " + arcFilename;
      }
}
