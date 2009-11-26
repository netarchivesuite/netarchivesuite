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
package dk.netarkivet.archive.arcrepository.bitpreservation;

import dk.netarkivet.common.exceptions.ArgumentNotValid;

/**
 * The status for the upload.
 * This is to be used by the DatabaseBasedActiveBitPreservation.
 */
public enum FileListStatus {
    /** If the status has not been defined. This is the initial value.*/
    NO_FILELIST_STATUS,
    /** If the file is missing from a file list or a checksum list.*/
    MISSING,
    /** If the file has the correct checksum.*/
    OK;
    
    /**
     * Method to retrieve the FileListStatus based on an integer.
     *
     * @param status A certain integer for the upload status
     * @return The UploadStatus related to the certain integer
     * @throws ArgumentNotValid If argument rt does not correspond
     * to a UploadStatus.
     */
    public static FileListStatus fromOrdinal(int status) 
            throws ArgumentNotValid {
        switch (status) {
            case 0: return NO_FILELIST_STATUS;
            case 1: return MISSING;
            case 2: return OK;
            default: throw new ArgumentNotValid(
                    "Invalid filelist status with number " + status);
        }
    }
}
