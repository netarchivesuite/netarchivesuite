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
 * This is to be used by the 
 */
public enum UploadStatus {
    /** If the status has not been defined.*/
    NO_UPLOAD_STATUS,
    /** If the upload has started.*/
    STARTED,
    /** If the upload has failed.*/
    FAILED,
    /** If the upload has completed.*/
    COMPLETED;
    
    /**
     *
     * @param us A certain integer for the upload status
     * @return The UploadStatus related to the certain integer
     * @throws ArgumentNotValid If argument rt does not correspond
     * to a UploadStatus.
     */
    public static UploadStatus fromOrdinal(int us) throws ArgumentNotValid {
        switch (us) {
            case 0: return NO_UPLOAD_STATUS;
            case 1: return STARTED;
            case 2: return FAILED;
            case 3: return COMPLETED;
            default: throw new ArgumentNotValid(
                    "Invalid upload status with number " + us);
        }
    }
}
