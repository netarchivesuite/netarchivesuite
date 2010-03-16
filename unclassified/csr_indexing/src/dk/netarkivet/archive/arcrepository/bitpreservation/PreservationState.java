/* File:        $Id: FilePreservationState.java 1192 2009-12-16 11:26:10Z jolf $
 * Revision:    $Revision: 1192 $
 * Author:      $Author: jolf $
 * Date:        $Date: 2009-12-16 12:26:10 +0100 (Wed, 16 Dec 2009) $
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

import java.util.List;

import dk.netarkivet.common.distribute.arcrepository.Replica;

/**
 * The interface for the preservations states used by the web applications.
 */
public interface PreservationState {
    /** 
     * Get the checksum of this file in a specific replica.
     *
     * @param replica The replica to get the checksum from.
     * @return The file's checksum, if it is present in the replica, or
     * "" if it either is absent or an error occurred.
     */
    List<String> getBitarchiveChecksum(Replica replica);
    
    /** Get the MD5 checksum stored in the admin data.
    *
    * @return Checksum value as found in the admin data given at creation.
    */
    String getAdminChecksum();
    
    /** Get the status of the file in a bitarchive, according to the admin data.
     * This returns the status as a string for presentation purposes only.
     * TODO Needs localisation.
     *
     * @param replica The replica to get status for
     * @return Status that the admin data knows for this file in the bitarchive.
     */
    String getAdminBitarchiveState(Replica replica);

    /**
     * Check if the admin data reflect the actual status of the archive.
     *
     * Admin State checking: For each bitarchive the admin state is
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
    boolean isAdminDataOk();
    
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
    Replica getReferenceBitarchive();
    
    /** Get a checksum that the whole bitarchive agrees upon, or else "".
    *
    * @param replica A replica to get checksum for this file from
    * @return The checksum for this file in the replica, if all machines
    * that have that file agree, otherwise "".  If no checksums are found,
    * also returns "".
    *
    */
   String getUniqueChecksum(Replica replica);

   /**
    * Check if the file is missing from a bitarchive.
    *
    * @param bitarchive the bitarchive to check
    * @return true if the file is missing from the bitarchive
    */
   boolean fileIsMissing(Replica bitarchive);
   
   /**
    * Retrieve checksum that the majority of checksum references
    * (bitarchives+admin) agree upon.
    *
    * @return the reference checksum or "" if no majority exists
    */
   String getReferenceCheckSum();

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
   boolean isAdminCheckSumOk();
   
   /** Returns a human-readable representation of this object.  Do not depend
    * on this format for anything automated, as it may change at any time.
    *
    * @return Description of this object.
    */
   String toString();

   /**
    * Get the filename, this FilePreservationState is about.
    * Needed to get at the filename given to constructor, and allow for
    * a better datastructure.
    * @return the filename
    */
   String getFilename();
}
