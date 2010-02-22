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
package dk.netarkivet.archive.arcrepositoryadmin;

import java.sql.Date;

import dk.netarkivet.common.distribute.arcrepository.ReplicaStoreState;

/**
 * This is a container for the ReplicaFileInfo table in the bitpreservation 
 * database.
 */
public class ReplicaFileInfo {
    /** The guid. Unique identification key.*/
    private long guid;
    /** The replicaId. The identification of the replica.*/
    private String replicaId;
    /** The id of the file in the file table.*/
    private long fileId;
    /** The id of the segment in the segment.*/
    private long segmentId;
    /** The checksum of the file in the segment within the replica.*/
    private String checksum;
    /** The uploadstatus.*/
    private ReplicaStoreState uploadStatus;
    /** The filelist status.*/
    private FileListStatus filelistStatus;
    /** The checksum status.*/
    private ChecksumStatus checksumStatus;
    /** The date for the last filelist update of the entry.*/
    private Date filelistCheckdatetime;
    /** The date for the last checksum update of the entry.*/ 
    private Date checksumCheckdatetime;
    
    /**
     * Constructor.
     * 
     * @param gId The guid.
     * @param rId The replicaId.
     * @param fId The fileId.
     * @param sId the segmentId.
     * @param cs The checksum.
     * @param us The uploadstatus.
     * @param fs The fileliststatus.
     * @param css The checksumstatus.
     * @param fDate The date for the last filelist update.
     * @param cDate The date for the last checksum update.
     */
    public ReplicaFileInfo(long gId, String rId, long fId, long sId, String cs,
            int us, int fs, int css, Date fDate, Date cDate) {
        // validate ?
        this.guid = gId;
        this.replicaId = rId;
        this.fileId = fId;
        this.segmentId = sId;
        this.checksum = cs;
        this.uploadStatus = ReplicaStoreState.fromOrdinal(us);
        this.filelistStatus = FileListStatus.fromOrdinal(fs);
        this.checksumStatus = ChecksumStatus.fromOrdinal(css);
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
    
    /**
     * Retrieves the guid.
     * @return The guid.
     */
    public long getGuid() {
        return guid;
    }
    
    /**
     * Retrieves the replicaId.
     * @return The replicaId.
     */
    public String getReplicaId() {
        return replicaId;
    }

    /**
     * Retrieves the fileId.
     * @return The fileId.
     */
    public long getFileId() {
        return fileId;
    }
    
    /**
     * Retrieves the segmentId.
     * @return The segmentId.
     */
    public long getSegmentId() {
        return segmentId;
    }

    /**
     * Retrieves the checksum.
     * @return The checksum.
     */
    public String getChecksum() {
        return checksum;
    }

    /**
     * Retrieves the uploadState.
     * @return The uploadState.
     */
    public ReplicaStoreState getUploadState() {
        return uploadStatus;
    }

    /**
     * Retrieves the filelistStatus.
     * @return The filelistStatus.
     */
    public FileListStatus getFileListState() {
        return filelistStatus;
    }
    
    /**
     * Retrieves the checksumStatus.
     * @return The checksumStatus.
     */
    public ChecksumStatus getChecksumState() {
        return checksumStatus;
    }

    /**
     * Retrieves the filelistCheckdatetime.
     * @return The filelistCheckdatetime.
     */
    public Date getFileListDate() {
        return filelistCheckdatetime;
    }
    
    /**
     * Retrieves the checksumCheckDatetime.
     * @return The checksumCheckDateTime.
     */
    public Date getChecksumCheckdatetime() {
        return checksumCheckdatetime;
    }
}
