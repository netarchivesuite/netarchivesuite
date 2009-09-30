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

package dk.netarkivet.archive.arcrepositoryadmin;

import java.io.PrintWriter;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.archive.arcrepository.distribute.StoreMessage;
import dk.netarkivet.common.distribute.arcrepository.ReplicaStoreState;
import dk.netarkivet.common.exceptions.ArgumentNotValid;

/**
 * This class contains the information that we keep about each file in the
 * arcrepository: Checksum and the store states for all bitarchives.
 *
 * TODO Maybe don't have the store state info for fully completed stores, or
 * else use a slimmer map for it.
 */

public class ArcRepositoryEntry {

    /**
     * The filename for this entry (only set in the constructor).
     */
    private String filename;

    /** The checksum of this file.
     * This field is persistent in the admin data file.
     * This field can never be null.
     * Note: AdminData.setChecksum() can change this value.
     * Note: ArcRepositoryEntry.setChecksum() can change this value.
     */
    private String md5sum;

    /** How the upload of this file into the bitarchives went.
     * This field is persistent in the admin data file.  After constructor
     * initialization, this field should only be set in case of a correct
     * operation (now or earlier).
     * Note: the value 2 below is a hint to the number of bitarchives
     * in our system.
     */
    private Map<String, ArchiveStoreState> storeStates =
            new HashMap<String, ArchiveStoreState>(2);

    /** The information used to reply about this entry being done.
     * Once a reply has been sent, this entry is set to null.
     * This field is not persistent.
     */
    private StoreMessage replyInfo;

    /**
     * String used to separate the different parts of the arcRepositoryEntry,
     * when we write the entry to persistent storage.
     * Make package private, so accessable from AdminData
     *
     */
    static final String ENTRY_COMPONENT_SEPARATOR_STRING = " , ";

    /**
     * General delimiter used several places.
     * Used to delimite the different storestates for the entry
     * in the output() method.
     */
    private static final String GENERAL_DELIMITER = " ";

    private Log log = LogFactory.getLog(ArcRepositoryEntry.class.getName());

    /** Create a new entry with given checksum and replyinfo.
     *
     * @param filename The filename for this entry
     * @param md5sum The checksum for this entry
     * @param replyInfo The one-use-only reply info chunk
     */
    ArcRepositoryEntry(String filename, String md5sum, StoreMessage replyInfo) {
        this.filename = filename;
        this.md5sum = md5sum;
        this.replyInfo = replyInfo;
    }

    /**
     * Get the ArchiveStoreState for the entry in general.
     * This is computed from the ArchiveStoreStates for the bitarchives.
     * <br>1. If no information about the bitarchives are available,
     *      the state UPLOAD_FAILED with timestamp=NOW is returned
     * <br>2. If there are information about one bitarchive,
     *      the state of this bitarchive is returned.
     * <br>3. If there are information from more than one bitarchive,
     *  A. if the state of one of the bitarchives equals UPLOAD_FAILED,
     *      the state UPLOAD_FAILED with the latest timestamp is returned
     *  B. else, find the lowest state of the N bitarchives:
     *      return this state together with the the latest timestamp
     * <p>
     * Note that the storestate and the timestamp might not come from the same
     * bitarchive.
     *
     * @return the current ArchiveStoreState for the entry in general
     */
    public ArchiveStoreState getGeneralStoreState() {
        Set<String> bitarchives = storeStates.keySet();
        // Check whether scenario 1.
        if (bitarchives.size() == 0) {
            return new ArchiveStoreState(ReplicaStoreState.UPLOAD_FAILED);
        }

        String[] bitarchiveNames = bitarchives.toArray(
                new String[bitarchives.size()]);
        // Check whether scenario 2.
        if (bitarchives.size() == 1){
           ArchiveStoreState ass = storeStates.get(bitarchiveNames[0]);
           return new ArchiveStoreState(ass.getState(), ass.getLastChanged());
        }

        // Scenario 3: If there are information from more than one bitarchive

        ArchiveStoreState ass = storeStates.get(bitarchiveNames[0]);
        Date lastChanged = ass.getLastChanged();
        boolean failState = false;
        ReplicaStoreState lowestStoreState = ass.getState();
        if (ass.getState().equals(ReplicaStoreState.UPLOAD_FAILED)) {
            failState = true;
        }
        for (int i = 1; i < bitarchiveNames.length; i++) {
            ArchiveStoreState tmpAss = storeStates.get(bitarchiveNames[i]);
            if (tmpAss.getState().ordinal() < lowestStoreState.ordinal()) {
                lowestStoreState = tmpAss.getState();
            }
            if (tmpAss.getState().equals(ReplicaStoreState.UPLOAD_FAILED)) {
                failState = true;
            }
            if (tmpAss.getLastChanged().after(lastChanged)) {
                lastChanged = tmpAss.getLastChanged();
            }
        }

        // Scenario 3A: if the state of one of the bitarchives equals
        // UPLOAD_FAILED.
        if (failState) {
            return new ArchiveStoreState(ReplicaStoreState.UPLOAD_FAILED,
                    lastChanged);
        }
        // Scenario 3B:
        //   B. else, find the lowest state of the N bitarchives:
        //     return this state together with the the latest timestamp

        return new ArchiveStoreState(lowestStoreState, lastChanged);
    }


    /**
     * Set the StoreState for a specific bitarchive
     * (set timestamp for last update to NOW).
     * @param ba a bitarchive
     * @param state the new StoreState for this bitarchive.
     */
    void setStoreState(String ba, ReplicaStoreState state) {
        ArchiveStoreState ass = new ArchiveStoreState(state);
        storeStates.put(ba, ass);
    }

    /**
     * Set the StoreState for a specific bitarchive
     * (set timestamp for last update to lastchanged).
     * @param baId a bitarchive
     * @param state the new StoreState for this bitarchive.
     * @param lastchanged the time for when the state was changed
     */
    void setStoreState(String baId, ReplicaStoreState state,
            Date lastchanged) {
        ArchiveStoreState ass = new ArchiveStoreState(state, lastchanged);
        storeStates.put(baId, ass);
    }


    /**
     * Get the StoreState for this entry for a given bitarchive or null if none.
     * @param baId a bitarchive id
     * @return the StoreState for a given bitarchive.
     */
    public ReplicaStoreState getStoreState(String baId) {
        ArgumentNotValid.checkNotNullOrEmpty(baId, "String baId");
        ArchiveStoreState ass = storeStates.get(baId);
        if (ass == null) {
            return null;
        }
        return ass.getState();
    }

    /**
     * Get the filename for this entry.
     * @return the filename for this entry
     */
    String getFilename() {
        return filename;
    }

    /**
     * Set the checksum for this entry.
     * @param checksum the new checksum for this entry
     */
    void setChecksum(String checksum) {
        ArgumentNotValid.checkNotNullOrEmpty(checksum, "String checksum");
        md5sum = checksum;
    }

    /**
     * Get the checksum for this entry.
     * @return the stored checksum for this entry
     */
    public String getChecksum() {
        return md5sum;
    }

    /** Get the reply info and remove it from the entry.
     * @return A reply info object that nobody else has gotten or will get.
     */
    StoreMessage getAndRemoveReplyInfo() {
        StoreMessage currentReplyInfo = this.replyInfo;
        // Reset replyInfo of this entry.
        this.replyInfo = null;
        return currentReplyInfo; // return value of replyInfo before being reset
    }
    /**
     * Returns information of whether a ReplyInfo object has been
     * stored with this entry.
     * @return true, if replyInfo is not null.
     */
    boolean hasReplyInfo() {
        return replyInfo != null;
    }

    /** Write this object to persistent storage.
    *
    * @param o A stream to write to.
    */
    void output(PrintWriter o) {
        o.print(filename + GENERAL_DELIMITER);
        o.print(md5sum);
        o.print(GENERAL_DELIMITER + getGeneralStoreState().toString());

        for (Map.Entry<String, ArchiveStoreState> entry 
        	: storeStates.entrySet()) {
            o.print(ENTRY_COMPONENT_SEPARATOR_STRING + entry.getKey()
                    + GENERAL_DELIMITER + entry.getValue());
        }
    }

    /**
     * Check, if a given bitArchive has a StoreState connected to it.
     * @param bitArchive a given bitarchive
     * @return true, if the given bitArchive has a StoreState connected to it.
     */
    boolean hasStoreState(String bitArchive) {
        return storeStates.containsKey(bitArchive);
    }

    /**
     * Set the replyInfo instance variable.
     * @param replyInfo The new value for the replyInfo variable.
     */
    void setReplyInfo(StoreMessage replyInfo) {
        if (this.replyInfo != null) {
            log.warn("Overwriting replyInfo '" + this.replyInfo
                    + "' with '" + replyInfo + "'");
        }
        this.replyInfo = replyInfo;
    }
}
