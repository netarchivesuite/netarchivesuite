/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2007 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.archive.arcrepository.distribute.StoreMessage;
import dk.netarkivet.common.Settings;
import dk.netarkivet.common.distribute.arcrepository.BitArchiveStoreState;
import dk.netarkivet.common.distribute.arcrepository.Location;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.PermissionDenied;
import dk.netarkivet.common.exceptions.UnknownID;
import dk.netarkivet.common.utils.ApplicationUtils;

/**
 * Class for accessing and manipulating the administrative data for
 * the ArcRepository.
 * In the current implementation, it consists of a file with a number of lines
 * of the form: <filename> <checksum> <state> <timestamp-for-last-state-change>
 *     [,<bitarchive> <storestatus> <timestamp-for-last-state-change>]*
 *
 * This abstract class is overridden to give either a read/write or a readonly
 * version of this class.
 *
 */
public abstract class AdminData {
    private Log log = LogFactory.getLog(AdminData.class.getName());

    /** Admindata version.
     * VersionNumber is the current version
     * oldVersionNumber is the earlier but still valid version
     */
    protected static final String versionNumber = "0.4";
    private static final String oldVersionNumber = "0.3";
    /**
     * Map containing a mapping from arcfilename to ArcRepositoryEntry.
     */
    protected Map<String, ArcRepositoryEntry> storeEntries
            = new HashMap<String, ArcRepositoryEntry>();
    /**
     * General delimiter.
     * TODO: add constants class where these constants are placed
     */
    private static final String GENERAL_DELIMITER = " ";

    /** The directory where the admin data resides, currently the directory:
     * Settings.DIRS_ARCREPOSITORY_ADMIN. */
    protected File adminDir;
    
    /** The name of the admin file. */
    protected static final String ADMIN_FILE_NAME = "admin.data";

    /** List containing the names of all knownBitArchives.
     * This list is updated in the setState() method
     * But only used in the toString() method.
     */
    protected List<String> knownBitArchives = new ArrayList<String>();

    /** The File object for the admin data file. */
    final File adminDataFile;

    /** Common constructor for admin data. Reads current admin data from admin
     * data file.
     * @throws PermissionDenied if admin data directory is not accessible
     */
    protected AdminData() {
        this.adminDir
                = new File(Settings.get(Settings.DIRS_ARCREPOSITORY_ADMIN));
        ApplicationUtils.dirMustExist(adminDir);

        adminDataFile = new File(adminDir, AdminData.ADMIN_FILE_NAME);
        log.info("Using admin data file '" + adminDataFile.getAbsolutePath()
                + "'");

        if (adminDataFile.exists()) {
            read(); // Load admindata into StoreEntries Map
        } else {
            log.warn("AdminDataFile (" + adminDataFile.getPath()
                    + ") was not found.");
        }
    }

    /**
     * Returns the one and only AdminData instance.
     * @return the one and only AdminData instance.
     */
    public static synchronized UpdateableAdminData getUpdateableInstance() {
        return UpdateableAdminData.getInstance();
    }

    /**
     * Returns a read-only AdminData instance.
     * @return a read-only AdminData instance.
     */
    public static synchronized ReadOnlyAdminData getReadOnlyInstance() {
        // no Singleton returned
        return new ReadOnlyAdminData();
    }

    /**
     * Check, if there is an entry for a certain arcfile?
     * @param arcfileName A given arcfile
     * @return true, if there is an entry for the given arcfile
     */
    public boolean hasEntry(String arcfileName) {
        ArgumentNotValid.checkNotNullOrEmpty(arcfileName, "arcfileName");
        return storeEntries.containsKey(arcfileName);
    }

    /**
     * Return the ArcRepositoryEntry for a certain arcfileName.
     * Returns null, if not found.
     * @param arcfileName a certain filename
     * @return the ArcRepositoryEntry for a certain arcfileName
     */
    public ArcRepositoryEntry getEntry(String arcfileName) {
        return storeEntries.get(arcfileName);
    }

    /**
     * Tells whether there is a replyInfo associated with
     * the given arcfile. If the file is not registered,
     * a warning is logged and false is returned.
     *
     * @param arcfileName The arc file we want to reply a store request for.
     * @return Whether setReplyInfo() has been called
     *         (and the replyInfo hasn't been removed since).
     */
    public boolean hasReplyInfo(String arcfileName) {
        ArgumentNotValid.checkNotNullOrEmpty(arcfileName, "arcfileName");
        ArcRepositoryEntry entry = storeEntries.get(arcfileName);
        if (entry == null) {
            log.warn("No entry found in storeEntries for arcfilename: "
                    + arcfileName);
        }
        return entry != null && entry.hasReplyInfo();
    }

    /**
     * Returns whether or not a BitArchiveStoreState is registered for the given
     * ARC file at the given bit archive.
     *
     * @param arcfileName The file to retrieve the state for
     * @param bitArchive  The bitarchive to retrieve the state for
     * @return true if BitArchiveStoreState is registered, false otherwise.
     */
    public boolean hasState(String arcfileName, String bitArchive) {
        ArgumentNotValid.checkNotNullOrEmpty(arcfileName, "arcfileName");
        ArgumentNotValid.checkNotNullOrEmpty(bitArchive, "bitArchive");
        ArcRepositoryEntry entry = storeEntries.get(arcfileName);
        if (entry == null) {
            log.warn("No entry found in storeEntries for arcfilename: "
                    + arcfileName);
        }
        return entry != null && entry.hasStoreState(bitArchive);
    }

    /**
     * Retrieves the storage state of a file for a specific bitarchive.
     *
     * @param arcfileName The file to retrieve the state for
     * @param bitArchive  The bitarchive to retrieve the state for
     * @return The storage state
     * @throws UnknownID When no record exists
     */
    public BitArchiveStoreState getState(String arcfileName, String bitArchive)
            throws UnknownID {
        ArgumentNotValid.checkNotNullOrEmpty(arcfileName, "arcfileName");
        ArgumentNotValid.checkNotNullOrEmpty(bitArchive, "bitArchive");
        if (!hasState(arcfileName, bitArchive)) {
            throw new UnknownID("No store state recorded for '"
                    + arcfileName + "' in '" + bitArchive + "'");
        }
        return storeEntries.get(arcfileName).getStoreState(bitArchive);
    }

    /**
     * Get Checksum for a given arcfile.
     * @param arcfileName Unique reference to file for which to
     *  retrieve checksum
     * @return checksum the latest registered reference checksum or null,
     *         if no reference checksum is available
     * @throws UnknownID if the file is not registered
     */
    public String getCheckSum(String arcfileName) {
        ArgumentNotValid.checkNotNullOrEmpty(arcfileName, "arcfileName");
        if (!hasEntry(arcfileName)) {
            throw new UnknownID("Don't know anything about file '"
                    + arcfileName + "'");
        }
        return storeEntries.get(arcfileName).getChecksum();
    }

    /**
     * Reads the admin data from a file. If the data read is a valid old version
     * the it is converted to the new version and written to disk.
     * @throws IOFailure on trouble reading from file
     */
    protected void read() {
        try {
            BufferedReader reader = null;
            try {
                reader = new BufferedReader(new FileReader(adminDataFile));
                /*
                * Check version. When this check is done, we either have
                *  - dataVersion.equals(versionNumber)) && !validOldVersion, or
                *  - !dataVersion.equals(versionNumber)) && validOldVersion
                *  The latter applies if the data file was empty.
                */
                String dataVersion = oldVersionNumber;

                boolean validOldVersion = false;
                String tempVersion = reader.readLine();
                if (tempVersion != null) {
                    dataVersion = tempVersion;
                }
                if (dataVersion.equals(oldVersionNumber)) {
                    log.debug("admindata version: " + oldVersionNumber);
                    validOldVersion = true;
                }
                if (!dataVersion.equals(versionNumber) && !validOldVersion) {
                    throw new IOFailure("Invalid version" + dataVersion);
                }
                //Now read the data file, depending on version.
                if (dataVersion.equals(versionNumber)) {
                    log.debug("admindata version: " + versionNumber);
                    readCurrentVersion(reader);
                } else {
                    readValidOldVersion(reader);
                }
            } finally {
                if (reader != null) {
                    reader.close();
                }
            }
        } catch (FileNotFoundException e) {
            throw new IOFailure("AdminData couldn't find admin data file", e);
        } catch (IOException e) {
            throw new IOFailure("AdminData couldn't find admin data file", e);
        }
    }

    /** Read the valid old version (0.3) of the admin data.
     * The valid old version contains lines of the format
     * <filename> <checksum> [<bitarchive> <storestatus>]*
     * The same filename may occur multiple times, but must always
     * have the same checksum.  This indicates updates of the storestatus
     * for the file.  Updates to checksum happen only during 'correct'
     * operations and cause the entire file to be written, leaving the changed
     * entry with the new checksum only.
     * An entry-line is considered corrupt (!valid) if any of the
     * following occur:
     * There is no checksum.
     * There is a bitarchive with a missing or invalid status
     * The checksum does not match a previously found checksum.
     * NB: If we come upon a corrupt entry-line, the entry for the filename
     * in question is removed from admin.data
     *
     * @param reader The stream to read the input from.
     */
    private void readValidOldVersion(BufferedReader reader) {
        String s;
        String logMessage;
        try {
            while ((s = reader.readLine()) != null) {
                String[] parts = s.split(" ");
                boolean valid = true;
                String filename = parts[0];
                if (parts.length < 2 || parts.length % 2 != 0) {
                    logMessage = "Corrupt admin data file:  Too few or not "
                        + "an even number of fields for " + filename
                        + ": "  + s;
                    log.warn(logMessage);
                    valid = false;
                }
                if (parts.length > 1) {
                    String checksum = parts[1];
                    if (hasEntry(filename)) {
                        if (!checksum.equals(getCheckSum(filename))) {
                            log.warn("Wrong checksum encountered in"
                                    + " admin data for known file '" + filename
                                    + "': Old=" + getCheckSum(filename)
                                    + " New=" + checksum);
                            valid = false; // this means, that the existing entry is removed from admin.data
                        }
                    } else {
                        StoreMessage replyInfo = null;
                        storeEntries.put(filename,
                                new ArcRepositoryEntry(filename, checksum, replyInfo));
                    }
                } else { //parts.length == 1
                    if (hasEntry(filename)) {
                        log.debug("Entry is invalid, "
                                + "because no checksumstring found in line: "
                                + s);
                        //this means, that the existing entry
                        // is removed from admin.data
                        valid = false;
                    } else {
                        // Ignore this entry entirely, if not already
                        // entry for this filename
                        log.warn("This entry-line is ignored, "
                                + "because no checksumstring found in line: "
                                + s);
                        continue;
                    }
                }
                // If the entry is invalid, no reason to try parsing states
                if (valid) {
                    ArcRepositoryEntry entry = storeEntries.get(filename);
                    for (int i = 2; i < parts.length; i += 2) {
                        try {
                            entry.setStoreState(parts[i],
                                    BitArchiveStoreState.valueOf(parts[i + 1]));
                        } catch (IllegalArgumentException e) {
                            log.warn(
                                    "Corrupt admin data entry. ", e);
                            valid = false;
                            break;
                        }
                    }
                }
                // Note that the previous if could set valid to false
                if (!valid) {
                    log.warn("Entry for file '" + filename
                            + "' with checksum '"
                            + storeEntries.get(filename).getChecksum()
                            + "' is invalid and therefore removed after "
                            + "reading line with inconsistent information: "
                            + s);
                    storeEntries.remove(filename);
                }
            }
        } catch (IOException e) {
            final String message = "Failed to read admin data from '"
                                + adminDataFile.getPath() + "'";
            log.fatal(message);
            throw new IOFailure(message, e);
        }
    }

    /**
      * Read the current version (0.4) of the admin data.
      * The current version contains lines of the format
      * <filename> <checksum> <state> <timestamp-for-last-state-change>
       [,<bitarchive> <storestatus> <timestamp-for-last-state-change>]*
      *
      * The same filename may occur multiple times, but must always
      * have the same checksum.  This indicates updates of the storestatus
      * for the file.  Updates to checksum happen only during 'correct'
      * operations and cause the entire file to be written, leaving the
        changed entry with the new checksum only.
      * An entry is considered corrupt (!valid) if any of the following
      *  occur:
      *     - There is no checksum.
      *     - There is no state
      *     - timestamp-for-last-state-change is missing
      *     - There is a bitarchive with a missing or invalid status
      *     - The checksum does not match a previously found checksum.
      * NB: If we come upon a corrupt entry-line, the entry for the filename
      * in question is removed from admin.data
      *
      * @param reader The stream to read the input from.
      * @throws ArgumentNotValid If reader is null
      */

    private void readCurrentVersion(BufferedReader reader){
        ArgumentNotValid.checkNotNull(reader, "reader");
        String s;
        try {
            while ((s = reader.readLine()) != null) {

                //Split the line up in parts defined by
                //the ENTRY_COMPONENT_SEPARATOR_STRING
                String[] parts =
                    s.split(ArcRepositoryEntry.
                            ENTRY_COMPONENT_SEPARATOR_STRING);

                // parts[0] should now contain the
                // <filename> <checksum> <state> <timestamp-for-last-state-change>

                //For i=0,1.. : parts[1+i] contains the state-information
                // for the file on our bitarchives.

                String[] firstparts = parts[0].split(GENERAL_DELIMITER);

                if (firstparts.length != 4) {
                    String logMessage =
                        "Corrupt admin data file:  One of the components "
                        + "'<filename> <checksum> <state> "
                        + "<timestamp-for-last-state-change>' "
                        + "is missing from this line: " + s
                        + "\nIgnoring this line";
                    log.warn(logMessage);
                    continue; // ignore this linie, and go to next line
                }

                /**
                 * Parse the different components of filename> <checksum>
                 * <state> <timestamp-for-last-state-change>
                 */
                String filename = firstparts[0];
                String checksumString = firstparts[1];
                String stateString = firstparts[2];
                String timestampString = firstparts[3];
                log.trace("Found (filename, checksum, state, timestamp): "
                        + filename + "," + checksumString + " , "
                        + stateString + " , " + timestampString);

                BitArchiveStoreState state =
                    BitArchiveStoreState.valueOf(stateString);
                Long tempLong = Long.parseLong(timestampString);
                Date timestampAsDate = new Date(tempLong);
                
                // Check, if we already have entry for this filename
                if (hasEntry(filename)) { 

                    // check, if 'checksum' equals checksum-value in existing entry
                    if (!checksumString.equals(getCheckSum(filename))) {
                        log.warn("Wrong checksum encountered in admin data"
                                + " for known file '" + filename
                                + "': Old=" + getCheckSum(filename)
                                + " New=" + checksumString
                                + ". Entry removed from admin.data and "
                                + "the remaining line ignored: " + s);
                        storeEntries.remove(filename);
                        continue; // Stop processing, and go to next line
                    }
                } else {
                    // Add new entry for filename:
                    StoreMessage replyInfo = null;
                    storeEntries.put(filename,
                            new ArcRepositoryEntry(filename, checksumString,
                                    replyInfo));
                }

                // Parse the remaining parts[1..] array
                // Expected format: 
                // <bitarchive> <storestatus> <timestamp-for-last-state-change>
                ArcRepositoryEntry entry = getEntry(filename);
                for (int i = 1; i < parts.length; i++) {
                    String[] bitparts =  parts[i].split(GENERAL_DELIMITER);
                    if (bitparts.length != 3) {
                        final String message =
                            "Line incomplete. Expected 3 elements: <bitarchive>"
                            + "<storestatus> <timestamp-for-last-state-change>."
                            + " Found only " + bitparts.length
                            + " elements in line: " + s;
                        log.warn(message);

                    } else {
                        String bitarchiveString = bitparts[0];
                        String storestatusString = bitparts[1];
                        timestampString = bitparts[2];
                        state = BitArchiveStoreState.valueOf(storestatusString);
                        tempLong = Long.parseLong(timestampString);
                        timestampAsDate = new Date(tempLong);
                        entry.setStoreState(bitarchiveString, state,
                                timestampAsDate);
                    }
                }
            }
        } catch (IOException e) {
                final String message = "Failed to read admin data from '"
                    + adminDataFile.getPath() + "'";
                log.fatal(message);
                throw new IOFailure(message, e);
        }
    }

    /**
     * Returns a set of the all arcfile names in the repository.
     *
     * @return the set of files in the repository
     */
    public Set<String> getAllFileNames() {
        Set<String> knownFiles = new HashSet<String>();
        for (Map.Entry<String,ArcRepositoryEntry> entry
                : storeEntries.entrySet()) {
            knownFiles.add(entry.getKey());
        }
        return knownFiles;
    }

    /**
     * Returns a set of the names arcfile names that are in a given state for a
     * specific bitarchive in the repository.
     *
     * @param location the name of the BA
     * @param state the state to look for, e.g.
     *  BitArchiveStoreState.STATE_COMPLETED
     * @return the set of files in the repository with the given state
     */
    public Set<String> getAllFileNames(Location location,
                                       BitArchiveStoreState state) {
        ArgumentNotValid.checkNotNull(location, "Location location");
        ArgumentNotValid.checkNotNull(state, "BitArchiveStoreState state");
        String locationKey = location.getChannelID().getName();
        Set<String> completedFiles = new HashSet<String>();
        for (Map.Entry<String,ArcRepositoryEntry> entry
                : storeEntries.entrySet()) {
            if (entry.getValue().getStoreState(locationKey) == state) {
                completedFiles.add(entry.getKey());
            }
        }
        return completedFiles;
    }

    /**
     * Return info about current object as String.
     * @return info about current object as String.
     */
    public String toString() {
        StringBuffer out = new StringBuffer();
        out.append("\nAdminData:");
        out.append("\nKnown bitarchives:");
        out.append(knownBitArchives.toString());
        out.append(getAllFileNames().toString());
        return out.toString();
    }
}
