package dk.netarkivet.archive.arcrepository.bitpreservation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.archive.arcrepositoryadmin.ChecksumStatus;
import dk.netarkivet.archive.arcrepositoryadmin.FileListStatus;
import dk.netarkivet.archive.arcrepositoryadmin.ReplicaFileInfo;
import dk.netarkivet.common.distribute.arcrepository.Replica;
import dk.netarkivet.common.distribute.arcrepository.ReplicaType;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IllegalState;

/**
 * This class contains the preservation data based on the database data
 * of a given file.
 * Contains the ReplicaFileInfos corresponding to the file. 
 */
public class DatabasePreservationState implements PreservationState {
    /** The log.*/
    private Log log = LogFactory.getLog(DatabasePreservationState.class);
    
    /**
     * The map containing all the entries for in the replicafileinfo table in 
     * the database and the replica they correspond to.
     */
    private Map<Replica, ReplicaFileInfo> entries = new HashMap<Replica, 
            ReplicaFileInfo>();
    /**
     * The name of the file.
     */
    private String filename;
    
    /**
     * Constructor.
     * 
     * @param file The name of the file.
     * @param rfis A list of the ReplicaFileInfo entries in the database for
     * the given file.
     * @throws ArgumentNotValid If the filename is null or the empty string, or
     * if the list of ReplicaFileInfos are null or empty. 
     */
    public DatabasePreservationState(String file, 
            List<ReplicaFileInfo> rfis) throws ArgumentNotValid {
        ArgumentNotValid.checkNotNullOrEmpty(file, "String file");
        ArgumentNotValid.checkNotNullOrEmpty(rfis, 
                "List<ReplicaFileInfo> rfis");
        
        this.filename = file;
        
        // retrieve the replica, and put it into the map along the fileinfo.
        for(ReplicaFileInfo rfi : rfis) {
            Replica rep = Replica.getReplicaFromId(rfi.getReplicaId());
            entries.put(rep, rfi);
        }
    }
    
    /** Get the checksum of this file in a specific bitarchive.
    *
    * @param replica The replica to get the checksum from.
    * @return The file's checksum, if it is present in the bitarchive, or
    * "" if it either is absent or an error occurred.
    * @throws ArgumentNotValid If the replica is null.
    */
    public List<String> getBitarchiveChecksum(Replica replica) 
            throws ArgumentNotValid {
        ArgumentNotValid.checkNotNull(replica, "Replica replica");
        
        // return empty list if the file is missing from replica.
        if(entries.get(replica).getFileListState() == FileListStatus.MISSING) {
            return new ArrayList<String>(0);
        }
        
        // initialize resulting array.
        List<String> res = new ArrayList<String>(1);
        // retrieve checksum for replica, and put into array.
        res.add(getUniqueChecksum(replica));

        return res;
    }
    
    /** Get the MD5 checksum stored in the admin data.
    *
    * @return Checksum value as found in the admin data given at creation.
    */
    public String getAdminChecksum() {
        // Dummy function.
        return "NO ADMIN CHECKSUM!";
    }
    
    /** Get the status of the file in a replica, according to the admin data.
     * This returns the status as a string for presentation purposes only.
     * TODO Needs localisation.
     *
     * @param replica The replica to get status for
     * @return Status that the admin data knows for this file in the replica.
     * @throws ArgumentNotValid If the replica is null.
     */
    public String getAdminBitarchiveState(Replica replica) 
            throws ArgumentNotValid {
        ArgumentNotValid.checkNotNull(replica, "Replica replica");
        
        return entries.get(replica).getUploadState().toString();
    }

    /**
     * INHERITED DUMMY FUNCTION!
     * 
     * Check if the admin data reflect the actual status of the archive.
     *
     * Admin State checking: For each replica the admin state is
     * compared to the checksum received from the bitarchive.
     *
     * If no checksum is received from the bitarchive the valid admin states
     * are UPLOAD_STARTED and UPLOAD_FAILED.
     * If a checksum is received from the bitarchive the valid admin state is
     * UPLOAD_COMPLETED
     * Admin checksum checking: The admin checksum must match the majority of
     * reported checksums.
     *
     * Notice that a valid Admin data record does NOT imply that everything is
     * ok. Specifically a file may be missing from a bitarchive, or the checksum
     * of a file in a bitarchive may be wrong.
     *
     * @return true, if admin data match the state of the bitarchives, false
     * otherwise
     */
    public boolean isAdminDataOk() {
        // TODO is this correct? What is this result used for?
        // No admin data = OK
        return true;
    }
    
    /**
     * Returns a reference to a bitarchive that contains a version of the file
     * with the correct checksum.
     *
     * The correct checksum is defined as the checksum that the majority of the
     * bitarchives and admin data agree upon.
     *
     * If no bitarchive exists with a correct version of the file null is
     * returned.
     *
     * @return the name of the reference bitarchive
     *  or null if no reference exists
     */
    public Replica getReferenceBitarchive() {
        for(Map.Entry<Replica, ReplicaFileInfo> entry : entries.entrySet()) {
            // Check whether it is a bitarchive with OK checksum.
            if(entry.getKey().getType() == ReplicaType.BITARCHIVE 
                    && entry.getValue().getChecksumState() 
                    == ChecksumStatus.OK) {
                log.debug("Found refe");
                return entry.getKey();
            }
        }

        // If no replica is found, then report and return null.
        log.warn("Cannot find a reference bitarchive for the file '"
                + filename + "'. Returning null.");
        return null;
    }
    
    /** Get a checksum that the whole bitarchive agrees upon, or else "".
    *
    * @param replica A replica to get checksum for this file from
    * @return The checksum for this file in the replica, if all machines
    * that have that file agree, otherwise "".  If no checksums are found,
    * also returns "".
    * @throws ArgumentNotValid If the replica is null.
    */
   public String getUniqueChecksum(Replica replica) throws ArgumentNotValid {
       ArgumentNotValid.checkNotNull(replica, "Replica replica");
       
       // return "" if the file is missing.
       if(entries.get(replica).getFileListState() == FileListStatus.MISSING) {
           return "";
       }
       
       return entries.get(replica).getChecksum();
   }

   /**
    * Check if the file is missing from a replica.
    *
    * @param replica the replica to check
    * @return true if the file is missing from the replica
    * @throws ArgumentNotValid If the replica is null.
    */
   public boolean fileIsMissing(Replica replica) throws ArgumentNotValid {
       ArgumentNotValid.checkNotNull(replica, "Replica replica");
       
       // TODO Is it missing if the status is unknown?
       return entries.get(replica).getFileListState() == FileListStatus.MISSING;
   }
   
   /**
    * THIS IS VOTING!
    * Retrieve checksum that the majority of checksum references
    * replicas agree upon.
    * 
    * TODO Voting is already done by the DatabasedActiveBitPreservation. Thus
    * replace with finding an entry with checksum-status = OK.  
    *
    * @return the reference checksum or "" if no majority exists
    */
   public String getReferenceCheckSum() {
       // Map containing the checksum and the count
       Map<String, Integer> checksumCount = new HashMap<String, Integer>();
       
       log.debug("Creating checksum count map for voting.");
       
       // Insert all the checksum of all the entries into the map.
       for(ReplicaFileInfo rfi : entries.values()) {
           String checksum = rfi.getChecksum();
           
           if(checksumCount.containsKey(checksum)) {
               // retrieve the count and add one
               Integer count = checksumCount.get(checksum) + 1;
               // put the count back into the map.
               checksumCount.put(checksum, count);
           } else {
               // Put the checksum into the map, with the count one.
               checksumCount.put(checksum, Integer.valueOf(1));
           }
       }
       
       log.debug("Perform the actual voting.");
       
       // go through the map to find the largest count.
       int largest = -1;
       String res = "NO CHECKSUMS!";
       boolean unique = false;
       for(Map.Entry<String, Integer> checksumEntry 
               : checksumCount.entrySet()) {
           // check whether this has the highest count.
           if(checksumEntry.getValue().intValue() > largest) {
               unique = true;
               largest = checksumEntry.getValue().intValue();
               res = checksumEntry.getKey();
           } else if(checksumEntry.getValue().intValue() == largest) {
               // If several checksums has the same largest count, then 
               // the checksum is not unique.
               unique = false;
           }
       }
       
       // Check whether unique, and report other wise.
       if(!unique) {
           // TODO handle differently? send notification?
           String errMsg = "No common checksum was found for the file '" 
               + filename + "'. The checksums found: " + checksumCount;
           log.error(errMsg);
           throw new IllegalState(errMsg);
       }
       
       // log the results.
       log.info("The replicas have voted about the checksum for the file '" 
               + filename + "' and have elected the checksum '" + res + "'.");
       
       return res;
   }

   /**
    * Returns true if the checksum reported by admin data is equal to the
    * majority checksum. If no majority checksum exists true is also returned.
    * When this method returns false it is possible to correct the admin
    * checksum using the majority checksum - when true is returned no better
    * checksum exists for admin data.
    *
    * @return true, if the checksum reported by admin data is equal to the
    * majority checksum
    */
   public boolean isAdminCheckSumOk() {
       // The database is always OK.
       return true;
   }
   
   /** Returns a human-readable representation of this object.  Do not depend
    * on this format for anything automated, as it may change at any time.
    *
    * @return Description of this object.
    */
   public String toString() {
       String res = "DatabasePreservationStatus for '" + filename + "'\n";
       for(ReplicaFileInfo rfi : entries.values()) {
           res += rfi.toString() + "\n";
       }
       return res;
   }

   /**
    * Get the filename, this FilePreservationState is about.
    * Needed to get at the filename given to constructor, and allow for
    * a better datastructure.
    * @return the filename
    */
   public String getFilename() {
       return filename;
   }
}
