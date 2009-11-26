/* File:        $Id$
 * Date:        $Date$
 * Revision:    $Revision$
 * Author:      $Author$
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
package dk.netarkivet.archive.arcrepository.bitpreservation;

import java.sql.Date;

/**
 * This is a container for the ReplicaFileInfo table in the bitpreservation 
 * database.
 */
public class ReplicaFileInfo {
    /** The guid. Unique identification key.*/
    public long guid;
    /** The replicaId. The identification of the replica.*/
    public String replicaId;
    /** The id of the file in the file table.*/
    public long fileId;
    /** The id of the segment in the segment.*/
    public long segmentId;
    /** The checksum of the file in the segment within the replica.*/
    public String checksum;
    /** The uploadstatus.*/
    public int uploadStatus;
    /** The filelist status.*/
    public FileListStatus filelistStatus;
    /** The date for the last filelist update of the entry.*/
    public Date filelistCheckdatetime;
    /** The date for the last checksum update of the entry.*/ 
    public Date checksumCheckdatetime;
    
    /**
     * Constructor.
     * 
     * @param gId The guid.
     * @param rId The replicaId.
     * @param fId The fileId.
     * @param sId the segmentId.
     * @param cs The checksum.
     * @param us The updatestatus.
     * @param fs The filelist status.
     * @param fDate The date for the last filelist update.
     * @param cDate The date for the last checksum update.
     */
    public ReplicaFileInfo(long gId, String rId, long fId, long sId, String cs,
            int us, int fs, Date fDate, Date cDate) {
        // validate ?
        this.guid = gId;
        this.replicaId = rId;
        this.fileId = fId;
        this.segmentId = sId;
        this.checksum = cs;
        this.uploadStatus = us;
        this.filelistStatus = FileListStatus.fromOrdinal(fs);
        this.filelistCheckdatetime = fDate;
        this.checksumCheckdatetime = cDate;
    }
    
    /**
     * Retrieves this object as as a string. Contains all the variables.
     * 
     * @return A string representing this object.
     */
    public String toString() {
        return guid + ":" + replicaId + ":" + fileId + ":" + segmentId + ":"
                + checksum + ":" + uploadStatus + ":" + filelistStatus + ":"
                + filelistCheckdatetime + ":" + checksumCheckdatetime;
    }
}
