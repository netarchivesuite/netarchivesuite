package dk.netarkivet.archive.arcrepository.bitpreservation;

import dk.netarkivet.common.exceptions.ArgumentNotValid;

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
    public static ChecksumStatus fromOrdinal(int status) {
        switch (status) {
            case 0: return UNKNOWN;
            case 1: return CORRUPT;
            case 2: return OK;
            default: throw new ArgumentNotValid(
                    "Invalid checksum status with number " + status);
        }
    }
}
