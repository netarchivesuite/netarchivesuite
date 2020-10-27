/*
 * #%L
 * Netarchivesuite - archive
 * %%
 * Copyright (C) 2005 - 2018 The Royal Danish Library, 
 *             the National Library of France and the Austrian National Library.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 2.1 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */
package dk.netarkivet.archive.arcrepositoryadmin;

import dk.netarkivet.common.exceptions.ArgumentNotValid;

/**
 * The status for the file list updates. This is used by the DatabaseBasedActiveBitPreservation.
 */
public enum FileListStatus {
    /** If the status has not been defined. This is the initial value. */
    NO_FILELIST_STATUS,
    /** If the file is missing from a file list or a checksum list. */
    MISSING,
    /** If the file has the correct checksum. */
    OK;

    /**
     * Method to retrieve the FileListStatus based on an integer.
     *
     * @param status A specific integer for the upload status
     * @return The UploadStatus related to the certain integer
     * @throws ArgumentNotValid If argument rt does not correspond to a UploadStatus.
     */
    public static FileListStatus fromOrdinal(int status) throws ArgumentNotValid {
        switch (status) {
        case 0:
            return NO_FILELIST_STATUS;
        case 1:
            return MISSING;
        case 2:
            return OK;
        default:
            throw new ArgumentNotValid("Invalid filelist status with number " + status);
        }
    }
}
