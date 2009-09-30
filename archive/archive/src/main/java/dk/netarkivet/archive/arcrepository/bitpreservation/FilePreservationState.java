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
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package dk.netarkivet.archive.arcrepository.bitpreservation;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.archive.arcrepositoryadmin.ArcRepositoryEntry;
import dk.netarkivet.common.distribute.arcrepository.ReplicaStoreState;
import dk.netarkivet.common.distribute.arcrepository.Replica;
import dk.netarkivet.common.exceptions.ArgumentNotValid;

/**
 * This class collects the available bit preservation information for a file.
 * <br>This information is the following:
 * <br>1) admin information for the file for each bitarchive and<br>
 * <br>2) the actual upload status
 */
public class FilePreservationState {
    private static final Log log = LogFactory.getLog(
            FilePreservationState.class);

    /** the name of the preserved file. */
    private String filename;

    /** the information as seen by the ArcRepository. */
    private ArcRepositoryEntry adminStatus;

    /** the checksums of the file in the individual bitarchives.
     * Normally, there will only be one entry in the list, but it must also
     * handle the case where multiple copies exist in a bitarchive.
     */
    private Map<Replica, List<String>> bitarchive2checksum;

    /**
     * Create new instance of the preservation status for a file.
     *
     * @param filename The filename to get status for
     * @param admindata The admin data for the file
     * @param checksumMap The map with the checksums for this file in 
     *  all bitarchives
     * @throws ArgumentNotValid if filename is null or empty string, 
     *  or if admindata is null.
     */
    FilePreservationState(String filename, ArcRepositoryEntry admindata,
                           Map<Replica, List<String>> checksumMap) {
        ArgumentNotValid.checkNotNullOrEmpty(filename, "String filename");
        ArgumentNotValid.checkNotNull(admindata,
                "ArcRepositoryEntry admindata");
        this.filename = filename;
        adminStatus = admindata;
        bitarchive2checksum = checksumMap;
    }

    /** Get the checksum of this file in a specific bitarchive.
     *
     * @param bitarchive The bitarchive to get the checksum from.
     * @return The file's checksum, if it is present in the bitarchive, or
     * "" if it either is absent or an error occurred.
     */
    public List<String> getBitarchiveChecksum(Replica bitarchive) {
        ArgumentNotValid.checkNotNull(bitarchive, "Replica bitarchive");
        if (bitarchive2checksum.containsKey(bitarchive)) {
            return bitarchive2checksum.get(bitarchive);
        } else {
            return Collections.emptyList();
        }
    }

    /** Get the MD5 checksum stored in the admin data.
     *
     * @return Checksum value as found in the admin data given at creation.
     */
    public String getAdminChecksum() {
        return adminStatus.getChecksum();
    }

    /** Get the status of the file in a bitarchive, according to the admin data.
     * This returns the status as a string for presentation purposes only.
     * TODO: Needs localisation.
     *
     * @param bitarchive The bitarchive to get status for
     * @return Status that the admin data knows for this file in the bitarchive.
     */
    public String getAdminBitarchiveState(Replica bitarchive) {
        ArgumentNotValid.checkNotNull(bitarchive, "Replica bitarchive");
        ReplicaStoreState state = getAdminBitarchiveStoreState(bitarchive);
        if (state != null) {
            return state.toString();
        } else {
            return "No state";
        }
    }

    /** Get the status of the file in a bitarchive, according to the admin data.
     *
     * @param bitarchive The bitarchive to get status for
     * @return Status that the admin data knows for this file in the bitarchive.
     */
    public ReplicaStoreState getAdminBitarchiveStoreState(
            Replica bitarchive) {
        ArgumentNotValid.checkNotNull(bitarchive, "Replica bitarchive");
        String bamonname = bitarchive.getIdentificationChannel().getName();
        return adminStatus.getStoreState(bamonname);
    }

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
    public boolean isAdminDataOk() {
        // Check the bitarchive states against the admin information
        for (Replica r : Replica.getKnown()) {
            ReplicaStoreState adminstate = getAdminBitarchiveStoreState(r);
            List<String> checksum = getBitarchiveChecksum(r);

            // If we find an error, return false, otherwise go on to the rest.
            if (checksum.size() == 0) {
                if (adminstate != ReplicaStoreState.UPLOAD_STARTED
                    && adminstate != ReplicaStoreState.UPLOAD_FAILED) {
                    return false;
                }
            } else {
                if (adminstate != ReplicaStoreState.UPLOAD_COMPLETED) {
                    return false;
                }
                if (getAdminChecksum().length() == 0) {
                    return false;
                }
            }
        }

        // If we reach here, we either have no checksums anywhere or
        // admin has a checksum, which should then agree with the majority
        return isAdminCheckSumOk();
    }

    /**
     * Check if the file is missing from a bitarchive.
     *
     * @param bitarchive the bitarchive to check
     * @return true if the file is missing from the bitarchive
     */
    protected boolean fileIsMissing(Replica bitarchive) {
        return getBitarchiveChecksum(bitarchive).size() == 0;
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
        String referenceCheckSum = getReferenceCheckSum();
        log.trace("Reference-checksum for file '" + filename + "' is '"
                + referenceCheckSum + "'");
        if ("".equals(referenceCheckSum)) {
            return null;
        }

        for (Replica r : Replica.getKnown()) {
            String cs = getUniqueChecksum(r);
            if (referenceCheckSum.equals(cs)) {
                log.trace("Reference archive for file '" + filename + "' is '"
                        + r.getName() + "'");
                return r;
            }
        }

        log.trace("No reference archive found for file '" + filename + "'");
        return null;
    }

    /** Get a checksum that the whole bitarchive agrees upon, or else "".
     *
     * @param r A replica to get checksum for this file from
     * @return The checksum for this file in the replica, if all machines
     * that have that file agree, otherwise "".  If no checksums are found,
     * also returns "".
     *
     */
    public String getUniqueChecksum(Replica r) {
        ArgumentNotValid.checkNotNull(r, "Replica r");
        List<String> checksums = bitarchive2checksum.get(r);
        String checksum = null;
        for (String s : checksums) {
            if (checksum != null && !checksum.equals(s)) {
                return "";
            } else {
                checksum = s;
            }
        }
        if (checksum != null) {
            return checksum;
        } else {
            return "";
        }
    }

    /**
     * Retrieve checksum that the majority of checksum references
     * (bitarchives+admin) agree upon.
     *
     * @return the reference checksum or "" if no majority exists
     */
    public String getReferenceCheckSum() {
        // establish map from checksum to counter of occurences
        Map<String, Integer> checksumCounts = new HashMap<String, Integer>();
        checksumCounts.put(adminStatus.getChecksum(), 1);
        for (Replica baReplica : Replica.getKnown()) {
            String checksum = getUniqueChecksum(baReplica);
            if (checksumCounts.containsKey(checksum)) {
                checksumCounts.put(checksum, checksumCounts.get(checksum) + 1);
            } else {
                checksumCounts.put(checksum, 1);
            }
        }

        // Now determine if a checksum obtained at least half of the votes
        int majorityCount = (Replica.getKnown().size() + 1) / 2 + 1;
        for (Map.Entry<String, Integer> entry : checksumCounts.entrySet()) {
            log.trace("File '" + filename + "' checksum '" + entry.getKey()
                    + "' votes " + entry.getValue()
                    + "  majority count " + majorityCount);
            if (entry.getValue() >= majorityCount) {
                return entry.getKey();
            }
        }

        return "";
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
        String referenceCheckSum = getReferenceCheckSum();
        if ("".equals(referenceCheckSum)) {
            return true;
        }
        return adminStatus.getChecksum().equals(referenceCheckSum);
    }

    /** Returns a human-readable representation of this object.  Do not depend
     * on this format for anything automated, as it may change at any time.
     *
     * @return Description of this object.
     */
    public String toString() {
        String res = "PreservationStatus for '" + filename + "'\n";
        if (adminStatus != null) {
            res = res + "General store state: "
                    + adminStatus.getGeneralStoreState().getState() + " "
                    + adminStatus.getGeneralStoreState().getLastChanged()
                    + "\n";
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
