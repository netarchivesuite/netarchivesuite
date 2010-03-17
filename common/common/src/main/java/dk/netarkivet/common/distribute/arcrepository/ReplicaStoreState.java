/* File:    $Id$
* Revision: $Revision$
* Author:   $Author$
* Date:     $Date$
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
package dk.netarkivet.common.distribute.arcrepository;

/**
 * This class encapsulates the different upload states, while storing a file
 * in the archive of a replica .
 * Used by the classes ArcRepository, AdminData, and ArcRepositoryEntry.
 * 
 * TODO Needs localisation.
 * 
 * @see dk.netarkivet.archive.arcrepository.ArcRepository
 * @see dk.netarkivet.archive.arcrepositoryadmin.AdminData
 * @see dk.netarkivet.archive.arcrepositoryadmin.ArcRepositoryEntry
 */
public enum ReplicaStoreState {
    /** Upload to a replica archive has started. */
    UPLOAD_STARTED, 
    /** Data has been successfully uploaded to a replica archive. */
    DATA_UPLOADED, 
    /** Upload to replica archive completed, which means that it has been 
     * verified by a checksumJob. */
    UPLOAD_COMPLETED, 
    /** Upload to the replica archive has failed. */
    UPLOAD_FAILED,
    /** If it is unknown whether a file has been successfully uploaded to a 
     * replica or not. Used in the database. */
    UNKNOWN_UPLOAD_STATE;
    
    public static ReplicaStoreState fromOrdinal(int i) {
        if(i == 0) {
            return UPLOAD_STARTED;
        } 
        if(i == 1) {
            return DATA_UPLOADED;
        } 
        if(i == 2) {
            return UPLOAD_COMPLETED;
        }
        if(i == 3) {
            return UPLOAD_FAILED;
        }
        // anything else is unknown.
        return UNKNOWN_UPLOAD_STATE;
    }
}
