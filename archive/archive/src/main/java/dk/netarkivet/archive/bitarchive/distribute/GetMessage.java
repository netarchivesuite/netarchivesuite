/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2009 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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

package dk.netarkivet.archive.bitarchive.distribute;

import dk.netarkivet.archive.distribute.ArchiveMessage;
import dk.netarkivet.archive.distribute.ArchiveMessageVisitor;
import dk.netarkivet.common.distribute.ChannelID;
import dk.netarkivet.common.distribute.arcrepository.BitarchiveRecord;

/**
 * Container for get requests.
 */
public class GetMessage extends ArchiveMessage {
    /** the arcfile to retrieve an record from.  */
    private String arcfile;
    /** offset of the record to retrieve. */
    private long index;
    /** the retrieved record. */
    private BitarchiveRecord record;

    /** 
     * Constructor.
     * 
     * @param to Where the message should be sent.
     * @param replyTo where the reply of this message should be sent. 
     * @param arcfile The name of the file to retrieve a arc-record from.
     * @param index The offset of the arc-file.
     */
    public GetMessage(ChannelID to, ChannelID replyTo, String arcfile,
            long index) {
        super(to, replyTo);
        this.arcfile = arcfile;
        this.index = index;
    }

    /**
     * Get name of the arc file.
     * @return file name
     */
    public String getArcFile() {
        return arcfile;
    }

    /**
     * Index of the record to retrieve.
     * @return offset
     */
    public long getIndex()  {
        return index;
    }

    /**
     * Register retrieved record.
     * @param rec Record retrieved from arcfile at offset index
     */
    public void setRecord(BitarchiveRecord rec) {
        record = rec;
    }

    /**
     * Get the data retrieved from the arcfile.
     * @return Record from arcfile
     */
    public BitarchiveRecord getRecord() {
        return record;
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
     * Retrieval of a string representation of this instance.
     * 
     * @return The string representation of this instance.
     */
    public String toString() {
        return super.toString() + " Arcfile: " + arcfile + " Offset: " + index;
    }

}
