package dk.netarkivet.common.distribute.arcrepository;

import dk.netarkivet.common.exceptions.ArgumentNotValid;

/**
 * Enumeration of the possible replica types used for replicas.
 */
public enum ReplicaType {
    /** 
     * If no replica type has been set
     **/    
    NO_REPLICA_TYPE,
     /** 
     * bitarchive replica which contain files stored in repository
     **/    
    BITARCHIVE,
    /** 
     * Checksum replica which contains checksums of files in repository 
     **/
    CHECKSUM;

    /**
     * Helper method that gives a proper object from e.g. settings.
     *
     * @param rt a certain integer for a replica type
     * @return the ReplicaType related to a certain integer
     * @throws ArgumentNotValid If argument tu is invalid
     * (i.e. does not correspond to a TimeUnit)
     */
    public static ReplicaType fromOrdinal(int rt) {
        ArgumentNotValid.checkNotNull(rt, "rt");
        switch (rt) {
            case 0: return NO_REPLICA_TYPE;
            case 1: return BITARCHIVE;
            case 2: return CHECKSUM;
            default: throw new ArgumentNotValid("Invalid replica " + rt);
        }
    }
   
    /**
    * Helper method that gives a proper object from e.g. settings.
    *
    * @param rt a certain integer for a replica type
    * @return the ReplicaType related to a certain integer
    * @throws ArgumentNotValid If argument tu is invalid
    * (i.e. does not correspond to a TimeUnit)
    */
    public static ReplicaType fromSetting(String s) {
        ArgumentNotValid.checkNotNull(s, "s");
        String t = s.toLowerCase();       
        if (s.equals("bitarchive")) {
            return BITARCHIVE;
        } else {
            if (s.equals("checksum")) {
                return CHECKSUM;
            } else {
                return NO_REPLICA_TYPE;
            }
        } 
    } 
}
