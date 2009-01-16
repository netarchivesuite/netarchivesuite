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

package dk.netarkivet.common.distribute.arcrepository;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.distribute.ChannelID;
import dk.netarkivet.common.distribute.Channels;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.UnknownID;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.utils.StringTree;

/**
 * This class encapsulates the bitarchive or chacksum replicas.  It guarantees that
 * there is only one Replica object per replica id/name.
 */
public class Replica {
    /** the class log. */
    private static Log log = LogFactory.getLog(Replica.class.getName());
    /** The id of this replica. */
    private final String id;
    /** The name of this replica. */
    private final String name;
    /** The type of this replica (checksum or bitarchive). */
    private final ReplicaType type;
    /** List of the replicas we know of. */
    private static Map<String, Replica> known;

    /** Private constructor that makes a new Replica object.  These will
     * all be stored in the known map.
     *
     * @param id Id of the replica (e.g. SB)
     * @param name Name of the replica (e.g. SBR)
     * @param type Type of the replica (e.g. biarchive)
     */
    private Replica(String repId, String repName, ReplicaType repType) {
        this.id = repId;
        this.name = repName;
        this.type = repType;
    }

    /** Initialize the list of known replicas from settings.
     * This must be called before using known, but after settings are loaded.
     */
    private static void initializeKnownList() {
        if (known == null) {
            String[] replicaIds 
                = Settings.getAll(CommonSettings.ENVIRONMENT_REPLICA_IDS);
            known = new HashMap<String, Replica>(replicaIds.length);
            StringTree<String> replicas 
                = Settings.getTree(CommonSettings.ENVIRONMENT_REPLICAS_PATH);
            List<StringTree<String>> replicaList = replicas.getSubTrees("replica");
            for (StringTree<String> replicaTree : replicaList) {
                String replicaId = replicaTree.getValue(CommonSettings.ENVIRONMENT_REPLICAID_TAG);
                known.put(
                    replicaId, 
                    new Replica(
                            replicaId, 
                            replicaTree.getValue(
                               CommonSettings.ENVIRONMENT_REPLICANAME_TAG),
                            ReplicaType.fromSetting(
                                replicaTree.getValue(
                                   CommonSettings.ENVIRONMENT_REPLICATYPE_TAG))
                         )
                );
            }
        }
    }

    /** Get an object representing the replica with the given id.
     *
     * @param name The given name of an replica
     * @return an object representing the replica with the given id
     */
    public static Replica getReplicaFromId(String id) {
        initializeKnownList();
        if (id == null || !known.containsKey(id)) {
            String message = "Can't find replica with id '" + id
                    + "', only know of " + known.keySet();
            log.debug(message);
            throw new UnknownID(message);
        }
        return known.get(id);
    }

    /** Get an object representing the replica with the given name.
    *
     * @param name The given name of an replica
     * @return an object representing the replica with the given name
    */
    public static Replica getReplicaFromName(String name) {
        ArgumentNotValid.checkNotNullOrEmpty(name, "String name");
        initializeKnownList();
        Replica resRep = new Replica("NONE","NONE", ReplicaType.NO_REPLICA_TYPE);
        boolean found = false; 
        for (Replica rep : known.values()) {
            found = rep.getName().equals(name);
            if (found) { 
                resRep = rep;
                break;
            }
        }
        if (!found) {
           String message = "Can't find replica with name '" + name
                   + "', only know of names for " + known.keySet();
           log.debug(message);
           throw new UnknownID(message);
        }
        return resRep;
   }

   /** Check, if a given name is a replica name.
     * @param name a given name
     * @return true, if the given name is a replica name, false otherwise
     */
    public static boolean isKnownReplicaName(String name) {
        ArgumentNotValid.checkNotNullOrEmpty(name, "String name");
        initializeKnownList();
        boolean found = false;
        for (String s : known.keySet()) {
            found = known.get(s).getName().equals(name);
            if (found) { break; }
        }
        return found;
    }   

    /** Check, if a given id is a replica id.
     * @param id a given id
     * @return true, if the given id is a replica id, false otherwise
     */
    public static boolean isKnownReplicaId(String id) {
        ArgumentNotValid.checkNotNullOrEmpty(id, "String id");
        initializeKnownList();
        boolean found = false;
        for (String s : known.keySet()) {
            found = s.equals(id);
            if (found) { break; }
        }
        return found;
    }   

    /** 
     * Get all known replcias.
     * @return A unmodifiable view of the currently known replicas.
     */
    public static Collection<Replica> getKnown() {
        initializeKnownList();
        return Collections.unmodifiableCollection(known.values());
    }
    
    /**
     * Get all known replica as ids.
     * @return all known replicas as ids
     */
    public static String[] getKnownIds() {
        initializeKnownList();
        String[] knownIds = new String[known.keySet().size()];
        int index = 0;
        for (String s : known.keySet()) {
            knownIds[index] = s;
            index++;
        }
        return knownIds;
    }

    /**
     * Get all known replica as names.
     * @return all known replicas as names
     */
    public static String[] getKnownNames() {
        initializeKnownList();
        String[] knownNames = new String[known.keySet().size()];
        int index = 0;
        for (String s : known.keySet()) {
            knownNames[index] = known.get(s).getName();
            index++;
        }
        return knownNames;
    }

    /** Get the type of an replica.
    *
    * @return The type that this replica (bitarchive or checksum).
    */
   public ReplicaType getType() {
       return type;
   }

   /** Get the id of an replica.
    *
    * @return The id that this replica (also used in queues).
    */
   public String getId() {
       return id;
   }

   /** Get the name of an replica.
     *
     * @return The name that this replica is known as in interface.
     */
    public String getName() {
        return name;
    }

    /** Get the BaMon channel id that corresponds to this replica.
     *
     * @return The BaMon ChannelID of this replica.
     * Please do not parse its name!
     */
    public ChannelID getChannelID() {
        return Channels.getBaMonForReplica(id);
    }

    /** Returns a human-readable representation of the object.
     *
     * @return An arbitrary string version of the object.  Do not depend on
     * its format.
     */
    public String toString() {
        return type.toString() + "Replica (" + id + ") "+ name;
    }
    
    /** This resets the list of known replicas.
     * This forces a new read of the settings, next time
     * one of the other static methods are used. 
     */ 
    public static void resetKnownList() {
        known = null;
    }
}
