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
 *   USA
 */

package dk.netarkivet.archive.arcrepositoryadmin;

import dk.netarkivet.common.exceptions.ArgumentNotValid;

/**
 * The status of the checksum for the bitpreservation database. 
 */
public enum ChecksumStatus {
    /** The status is 'UNKNOWN' before a update has taken place.*/
    UNKNOWN,
    /** The status is 'CORRUPT' if the checksum of the replicafileinfo entry 
     * does not match the checksum of the majority of the other replicafileinfo
     * entries for the same file but for the other replicas.*/
    CORRUPT,
    /** The status is 'OK' if the checksum of the replicafileinfo entry
     * is identical to the checksum of the other replicafileinfo entries for 
     * same file but for the other replicas.*/
    OK;
    
    /**
     * Method to retrieve the FileListStatus based on an integer.
     *
     * @param status A certain integer for the upload status
     * @return The UploadStatus related to the certain integer
     * @throws ArgumentNotValid If argument rt does not correspond
     * to a UploadStatus.
     */
    public static ChecksumStatus fromOrdinal(int status) 
            throws ArgumentNotValid {
        switch (status) {
            case 0: return UNKNOWN;
            case 1: return CORRUPT;
            case 2: return OK;
            default: throw new ArgumentNotValid(
                    "Invalid checksum status with number " + status);
        }
    }
}
