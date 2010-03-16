/* File:        $Id: AdminData.java 1042 2009-09-30 18:12:50Z kfc $
 * Revision:    $Revision: 1042 $
 * Author:      $Author: kfc $
 * Date:        $Date: 2009-09-30 20:12:50 +0200 (Wed, 30 Sep 2009) $
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

package dk.netarkivet.archive.arcrepositoryadmin;

import java.util.Set;

import dk.netarkivet.archive.arcrepository.distribute.StoreMessage;
import dk.netarkivet.common.distribute.arcrepository.Replica;
import dk.netarkivet.common.distribute.arcrepository.ReplicaStoreState;

/**
 * The interface for the administration of the storage.
 * This can be either a data file or a database.
 */
public interface Admin {
    /**
     * Method for telling whether a file entry exists.
     * 
     * @param filename The name of the file, the existence of whose entry is to
     * be determined.
     * @return Whether the entry exists. 
     */
    boolean hasEntry(String filename);
    /**
     * Method for adding an entry for administration.
     * 
     * @param filename The name of the file to be stored.
     * @param msg The StoreMessage of the entry.
     * @param checksum The checksum of the entry.
     */
    void addEntry(String filename, StoreMessage msg, String checksum);
    
    /**
     * Retrieves the checksum of a given file.
     * 
     * @param filename The name of the file, whose checksum should be retrieved.
     * @return The checksum of the file.
     */
    String getCheckSum(String filename);
    /**
     * Sets the checksum of a given file.
     * TODO Should it really be possible to change the checksum through 
     * arcrepository?
     *  
     * @param filename The name of the file to have the checksum changed. 
     * @param checksum The new checksum for the file.
     */
    void setCheckSum(String filename, String checksum);
    
    /**
     * Determines whether the StoreMessage of a given file exists.
     * 
     * @param filename The name of the file to which the existence of the 
     * StoreMessage should be determined. 
     * @return Whether the StoreMessage of the file exists.
     */
    boolean hasReplyInfo(String filename);
    /**
     * Assign a StoreMessage to a specific file.
     * 
     * @param filename The name of the file to have a StoreMessage assigned. 
     * @param msg The StoreMessage to be assigned to a file.
     */
    void setReplyInfo(String filename, StoreMessage msg);
    /**
     * Retrieves the StoreMessage of a specific file.
     * 
     * @param filename The name of the file whose StoreMessage should be 
     * retrieved.
     * @return The StoreMessage corresponding to the file.
     */
    StoreMessage removeReplyInfo(String filename);
    
    /**
     * Returns the ReplicaStoreState of a given file in a specific replica.
     * 
     * @param filename The name of the file for the ReplicaStoreState.
     * @param replicaChannelName The name of the identification channel for 
     * the replica of for the ReplicaStoreState.
     * @return The ReplicaStoreState of a given file in a specific replica.
     */
    ReplicaStoreState getState(String filename, String replicaChannelName);
    /**
     * Determines whether a given file in a specific replica has a valid 
     * store state.
     * TODO find out which states are acceptable!
     *  
     * @param filename The name of the file for the ReplicaStoreState.
     * @param repChannelId The identification channel of the replica for the 
     * ReplicaStoreState.
     * @return Whether a given file in a specific replica has a valid store 
     * state.
     */
    boolean hasState(String filename, String repChannelId);
    /**
     * Sets the store state of an entry to a specific value.
     * 
     * @param filename The name of the file for the entry.
     * @param repChannelId The identification channel of the replica for the 
     * entry. 
     * @param state The new state for the entry.
     */
    void setState(String filename, String repChannelId, 
            ReplicaStoreState state);

    /**
     * Retrieves a set of the names for all the known files.
     * 
     * @return A set of the names for all the known file.
     */
    Set<String> getAllFileNames();
    
    /**
     * Retrieves a set with the name of the files with a specific 
     * ReplicaStoreState in a specific replica.
     *  
     * @param rep The replica where the files belong.
     * @param state The ReplicaStoreState for the files.
     * @return A set with the names of the files with a specific 
     * ReplicaStoreState in a specific replica.
     */
    Set<String> getAllFileNames(Replica rep, ReplicaStoreState state);
    
    /**
     * Close and cleanup of this class.
     */
    void close();
}
