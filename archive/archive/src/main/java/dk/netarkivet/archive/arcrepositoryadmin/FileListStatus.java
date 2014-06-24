package dk.netarkivet.archive.arcrepositoryadmin;

import dk.netarkivet.common.exceptions.ArgumentNotValid;

/**
 * The status for the file list updates.
 * This is used by the DatabaseBasedActiveBitPreservation.
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
     * @param status A specific integer for the upload status
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
