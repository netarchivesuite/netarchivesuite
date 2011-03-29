/* File:     $Id$
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
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301 
 *  USA
 */

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
 * of a given filename.
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
     * @param fileName The name of the file.
     * @param rfis A list of the ReplicaFileInfo entries in the database for
     * the given file.
     * @throws ArgumentNotValid If the filename is null or the empty string, or
     * if the list of ReplicaFileInfos are null or empty. 
     */
    public DatabasePreservationState(String fileName, 
            List<ReplicaFileInfo> rfis) throws ArgumentNotValid {
        ArgumentNotValid.checkNotNullOrEmpty(fileName, "String fileName");
        ArgumentNotValid.checkNotNullOrEmpty(rfis, 
                "List<ReplicaFileInfo> rfis");
        
        this.filename = fileName;
        
        // retrieve the replica, and put it into the map along the fileinfo.
        for(ReplicaFileInfo rfi : rfis) {
            Replica rep = Replica.getReplicaFromId(rfi.getReplicaId());
            entries.put(rep, rfi);
        }
    }
    
    /** Get the checksum of this file in a specific replica.
    *
    * @param replica The replica to get the checksum from.
    * @return A list of the checksums for the file within the replica (only more
    * than one if there is duplicates in a bitarchive replica). An empty list
    * is returned if no file is present or if an error occurred.
    * @throws ArgumentNotValid If the replica is null.
    */
    public List<String> getReplicaChecksum(Replica replica) 
            throws ArgumentNotValid {
        ArgumentNotValid.checkNotNull(replica, "Replica replica");
        
        // return empty list if the file is missing from replica.
        if(entries.get(replica).getFileListState().equals(
                FileListStatus.MISSING)) {
            return new ArrayList<String>(0);
        }
        
        // initialize resulting array.
        List<String> res = new ArrayList<String>(1);
        // retrieve checksum for replica, and put into array.
        res.add(getUniqueChecksum(replica));

        return res;
    }
    
    /** 
     * Get the MD5 checksum stored in the admin data.
     * Inherited dummy function. No admin data for database instance, thus no
     * admin data checksum.
     *
     * @return Checksum value as found in the admin data given at creation.
     */
    public String getAdminChecksum() {
        // Dummy function.
        return "NO ADMIN CHECKSUM!";
    }
    
    /** Get the status of the file in a replica, according to the admin data.
     * This returns the status as a string for presentation purposes only.
     *
     * @param replica The replica to get status for
     * @return Status that the admin data knows for this file in the replica.
     * @throws ArgumentNotValid If the replica is null.
     */
    public String getAdminReplicaState(Replica replica) 
            throws ArgumentNotValid {
        ArgumentNotValid.checkNotNull(replica, "Replica replica");
        
        return entries.get(replica).getUploadState().toString();
    }

    /**
     * INHERITED DUMMY FUNCTION!
     * 
     * @return true, since a non-existing admin.data is OK for the database 
     * instance.
     */
    public boolean isAdminDataOk() {
        // No admin data = OK
        return true;
    }
    
    /**
     * Returns a reference to a replica that contains a version of the file
     * with the correct checksum.
     *
     * The correct checksum is defined as the checksum that the majority of the
     * replica and admin data agree upon.
     *
     * If no replica exists with a correct version of the file null is returned.
     *
     * @return the name of the reference replica or null if no reference exists.
     */
    public Replica getReferenceBitarchive() {
        for(Map.Entry<Replica, ReplicaFileInfo> entry : entries.entrySet()) {
            // Check whether it is a bitarchive with OK checksum.
            if(entry.getKey().getType().equals(ReplicaType.BITARCHIVE) 
                    && entry.getValue().getChecksumStatus().equals(
                            ChecksumStatus.OK)) {
                log.debug("Found reference bitarchive replica for file '" 
                        + filename + "'.");
                return entry.getKey();
            }
        }

        // If no replica is found, then report and return null.
        log.warn("Cannot find a reference bitarchive for the file '"
                + filename + "'. Returning null.");
        return null;
    }
    
   /** 
    * Get a checksum that the whole replica agrees upon, or else "".
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
       if(entries.get(replica).getFileListState().equals(
               FileListStatus.MISSING)) {
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
           
           // ignore if the checksum is invalid.
           if(checksum == null || checksum.isEmpty()) {
               log.warn("invalid checksum for replicafileinfo: " + rfi);
               continue;
           }
           
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
