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
package dk.netarkivet.archive.arcrepositoryadmin;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import dk.netarkivet.archive.arcrepository.distribute.StoreMessage;
import dk.netarkivet.common.distribute.Channels;
import dk.netarkivet.common.distribute.arcrepository.Replica;
import dk.netarkivet.common.distribute.arcrepository.ReplicaStoreState;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IllegalState;
import dk.netarkivet.common.exceptions.UnknownID;

/**
 * The administrator class for the ArcRepository when dealing with an database
 * instead of a file (alternative to AdminData).
 */
public final class DatabaseAdmin implements Admin {

    /** The access interface to the administration database.*/
    private ReplicaCacheDatabase database;
    /** The current instance of this class, to avoid multiple instantiations.*/
    private static DatabaseAdmin instance;
    /** Administration of store messages. */
    private Map<String, StoreMessage> storeEntries = 
        new HashMap<String, StoreMessage>();
    
    /**
     * Constructor.
     * Initialises the access to the database.
     */
    private DatabaseAdmin() {
        database = ReplicaCacheDatabase.getInstance();
    }
    
    /**
     * Retrieval of a singleton DatabaseAdmin.
     * Ensures that this class is not instantiated multiple times. 
     * 
     * @return The current instance of this class.
     */
    public static synchronized DatabaseAdmin getInstance() {
        if(instance == null) {
            instance = new DatabaseAdmin();
        }
        return instance;
    }
    
    /**
     * Method for adding an entry for administration.
     * 
     * @param filename The name of the file to be stored.
     * @param msg The StoreMessage of the entry.
     * @param checksum The checksum of the entry.
     * @throws ArgumentNotValid If either the filename or checksum is either 
     * null or the empty string.
     */
    @Override
    public void addEntry(String filename, StoreMessage msg, String checksum) 
            throws ArgumentNotValid {
        ArgumentNotValid.checkNotNullOrEmpty(filename, "String filename");
        ArgumentNotValid.checkNotNullOrEmpty(checksum, "String checksum");

        // insert this into the entries map.
        storeEntries.put(filename, msg);
        
        // insert into database.
        database.insertNewFileForUpload(filename, checksum);
    }

    /**
     * Method for telling whether a file entry exists.
     * 
     * @param filename The name of the file, the existence of whose entry is to
     * be determined.
     * @return Whether the entry exists. 
     * @throws ArgumentNotValid If the filename is either null or empty.
     */
    @Override
    public boolean hasEntry(String filename) throws ArgumentNotValid {
        ArgumentNotValid.checkNotNullOrEmpty(filename, "String filename");
        
        // See if the file can be found in the database.
        return database.existsFileInDB(filename);
    }

    /**
     * Returns the ReplicaStoreState of a given file in a specific replica.
     * 
     * @param filename The name of the file for the ReplicaStoreState.
     * @param replicaChannelName The name of the identification channel for 
     * uniquely identifying the replica of for the ReplicaStoreState.
     * @return The ReplicaStoreState of a given file in a specific replica.
     * @throws ArgumentNotValid If the filename or the replica id is null or 
     * the empty string.
     */
    @Override
    public ReplicaStoreState getState(String filename, 
            String replicaChannelName) throws ArgumentNotValid {
        ArgumentNotValid.checkNotNullOrEmpty(filename, "String filename");
        ArgumentNotValid.checkNotNullOrEmpty(replicaChannelName, 
                "String replicaChannelName");
        
        Replica rep = Channels.retrieveReplicaFromIdentifierChannel(
                replicaChannelName);
        
        // retrieve the ReplicaStoreState from the database.
        return database.getReplicaStoreState(filename, rep.getId());
    }

    /**
     * Determines whether a given file in a specific replica has a valid 
     * replica store state. By valid means a replica store state other that
     * UNKNOWN_UPLOAD_STATE.
     * 
     * TODO Find out if the assumption that all upload states besides 
     * UNKNOWN_UPLOAD_STATE are acceptable!
     * 
     * @param filename The name of the file for the ReplicaStoreState.
     * @param repChannelId The identification channel of the replica for the 
     * ReplicaStoreState.
     * @return Whether a given file in a specific replica has a valid store 
     * state.
     * @throws ArgumentNotValid If either the filenames or the replica 
     * identification channel is null or the empty string.
     */
    @Override
    public boolean hasState(String filename, String repChannelId) 
            throws ArgumentNotValid {
        ArgumentNotValid.checkNotNullOrEmpty(filename, "String filename");
        ArgumentNotValid.checkNotNullOrEmpty(repChannelId, 
                "String repChannelId");
        
        // retrieve the replica
        Replica rep = Channels.retrieveReplicaFromIdentifierChannel(
                repChannelId);
        
        // retrieve the state for the entry for the replica and filename
        ReplicaStoreState state = database.getReplicaStoreState(filename, 
                rep.getId());
        
        // return whether the entry has a known upload state 
        return state != ReplicaStoreState.UNKNOWN_UPLOAD_STATE;
    }

    /**
     * Sets the store state of an entry to a specific value.
     * 
     * @param filename The name of the file for the entry.
     * @param repChannelId The identification channel of the replica for the 
     * entry. 
     * @param state The new state for the entry.
     * @throws ArgumentNotValid If the ReplicaStoreState is null, or if either
     * the filename or the replica identification channel is either null or 
     * the empty string.
     */
    @Override
    public void setState(String filename, String repChannelId,
            ReplicaStoreState state) throws ArgumentNotValid {
        ArgumentNotValid.checkNotNull(state, "ReplicaStoreState state");
        ArgumentNotValid.checkNotNullOrEmpty(filename, "String filename");
        ArgumentNotValid.checkNotNullOrEmpty(repChannelId, 
                "String repChannelId");
  
        // retrieve the replica
        Replica rep = Channels.retrieveReplicaFromIdentifierChannel(
                repChannelId);
        
        // update the database.
        database.setReplicaStoreState(filename, rep.getId(), state);
    }

    /**
     * Determines whether the StoreMessage of a given file exists.
     * 
     * @param filename The name of the file to which the existence of the 
     * StoreMessage should be determined. 
     * @return Whether the StoreMessage of the file exists.
     * @throws ArgumentNotValid If the filename is null or the empty string.
     */
    @Override
    public boolean hasReplyInfo(String filename) throws ArgumentNotValid {
        ArgumentNotValid.checkNotNullOrEmpty(filename, "String filename");
        
        // check if a entry for the file can be found in the map.
        return storeEntries.containsKey(filename);
    }

    /**
     * Retrieves the StoreMessage of a specific file.
     * 
     * @param filename The name of the file whose StoreMessage should be 
     * retrieved.
     * @return The StoreMessage corresponding to the file. A null is returned
     * if the corresponding StoreMessage is not found. 
     * @throws ArgumentNotValid If the filename is either null or the empty 
     * string.
     */
    @Override
    public StoreMessage removeReplyInfo(String filename) 
            throws ArgumentNotValid {
        ArgumentNotValid.checkNotNullOrEmpty(filename, "String filename");
        
        // extract the entry from the 
        return storeEntries.remove(filename);
    }

    /**
     * Assign a StoreMessage to a specific filename. If the filename is already
     * associated with a StoreMessage, then this StoreMessage will be 
     * overwritten by the new StoreMessage.
     * 
     * @param filename The name of the file to have a StoreMessage assigned. 
     * @param msg The StoreMessage to be assigned to a file.
     * @throws ArgumentNotValid If the StoreMessage is null or if the filename
     * is either null or the empty string.
     */
    @Override
    public void setReplyInfo(String filename, StoreMessage msg) 
            throws ArgumentNotValid {
        ArgumentNotValid.checkNotNull(msg, "StoreMessage msg");
        ArgumentNotValid.checkNotNullOrEmpty(filename, "String filename");
        
        // put into the map, and overwrite any existing mapping.
        storeEntries.put(filename, msg);
    }

    /**
     * Retrieves the checksum of a given file.
     * 
     * @param filename The name of the file, whose checksum should be retrieved.
     * @return The checksum of the file.
     * @throws ArgumentNotValid If the filename is either null or the empty 
     * string.
     */
    @Override
    public String getCheckSum(String filename) throws ArgumentNotValid {
        ArgumentNotValid.checkNotNullOrEmpty(filename, "String filename");
        
        // Ensure that we have the file requested.
        if (!hasEntry(filename)) {
            throw new UnknownID("Don't know anything about file '"
                    + filename + "'");
        }

        // Retrieve the checksum for a specific entry.
        return database.getChecksum(filename);
    }

    /**
     * Sets the checksum of a given file.
     * 
     * It should not be possible to change the checksum in the database through
     * arcrepository.
     *  
     * @param filename The name of the file to have the checksum changed. 
     * @param checksum The new checksum for the file.
     * @throws ArgumentNotValid If either the filename or the checksum is 
     * either null or the empty string.
     * @throws IllegalState Always, since it is not allowed for arcrepository 
     * to change the checksum of a completed upload.
     */
    @Override
    public void setCheckSum(String filename, String checksum) 
            throws ArgumentNotValid, IllegalState {
        ArgumentNotValid.checkNotNullOrEmpty(filename, "String filename");
        ArgumentNotValid.checkNotNullOrEmpty(checksum, "String checksum");
        
        // This will not be implemented.
        throw new IllegalState("It is not possible to change the checksum of a "
                + " file in the database! Only the checksum of a specific "
                + "replicafileinfo.");
    }

    /**
     * Retrieves a set of the names for all the known files.
     * 
     * @return A set of the names for all the known file.
     */
    @Override
    public Set<String> getAllFileNames() {
        // initialise the set
        Set<String> res = new HashSet<String>();
        // put the collection of filenames into the set.
        res.addAll(database.retrieveAllFilenames());
        // return the set.
        return res;
    }

    /**
     * Retrieves a set with the name of the files with a specific 
     * ReplicaStoreState in a specific replica.
     *  
     * @param rep The replica where the files belong.
     * @param state The ReplicaStoreState for the files.
     * @return A set with the names of the files with a specific 
     * ReplicaStoreState in a specific replica.
     * @throws ArgumentNotValid If the Replica or the ReplicaStoreState is null.
     */
    @Override
    public Set<String> getAllFileNames(Replica rep, ReplicaStoreState state) 
            throws ArgumentNotValid {
        ArgumentNotValid.checkNotNull(rep, "Replica rep");
        ArgumentNotValid.checkNotNull(state, "ReplicaStoreState state");
      
        // initialise the set
        Set<String> res = new HashSet<String>();
        // put the collection of filenames into the set.
        res.addAll(database.retrieveFilenamesForReplicaEntries(rep.getId(), 
                state));
        // return the set.
        return res;
    }

    /**
     * Close and cleanup of this class.
     */
    @Override
    public void close() {
        storeEntries.clear();
        database.cleanup();
        instance = null;
    }
}
