/* File:    $Id$
* Revision: $Revision$
* Author:   $Author$
* Date:     $Date$
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
package dk.netarkivet.common.distribute.arcrepository;

/**
 * This class encapsulates the different upload states, while storing a file
 * in the bitarchive.
 * Used by the classes ArcRepository, AdminData, and ArcRepositoryEntry.
 * @see dk.netarkivet.archive.arcrepository.ArcRepository
 * @see dk.netarkivet.archive.arcrepositoryadmin.AdminData
 * @see dk.netarkivet.archive.arcrepositoryadmin.ArcRepositoryEntry
 */
public enum BitArchiveStoreState {
    /** Upload to a bitarchive has started. */
    UPLOAD_STARTED, 
    /** Data has been successfully uploaded to a bitarchive. */
    DATA_UPLOADED, 
    /** Upload to bitarchive completed, which means that it has been verified
     * by a checksumJob. */
    UPLOAD_COMPLETED, 
    /** Upload to bitarchive has failed. */
    UPLOAD_FAILED;
}
