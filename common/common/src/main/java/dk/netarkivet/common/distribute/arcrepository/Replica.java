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
 * This class encapsulates the bitarchive or checksum replicas.
 * It guarantees that there is only one Replica object per replica id/name.
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
    /** List of the replicas we know of. This list is initialized by the
     * "first" call to initializeKnownReplicasList(). */
    private static Map<String, Replica> knownReplicas;

    /** Private constructor that makes a new Replica object.  These will
     * all be stored in the knownReplicas map.
     *
     * @param repId Id of the replica (e.g. One)
     * @param repName Name of the replica (e.g. ReplicaOne)
     * @param repType Type of the replica (e.g. biarchive)
     */
    private Replica(String repId, String repName, ReplicaType repType) {
        this.id = repId;
        this.name = repName;
        this.type = repType;
    }

    /** Initialize the list of known replicas from settings.
     * This must be called before using known, but after settings are loaded.
     */
    private static void initializeKnownReplicasList() {
        if (knownReplicas == null) {
            String[] replicaIds 
                = Settings.getAll(CommonSettings.REPLICA_IDS);
            knownReplicas = new HashMap<String, Replica>(replicaIds.length);
            StringTree<String> replicas 
                = Settings.getTree(CommonSettings.REPLICAS_SETTINGS);
            List<StringTree<String>> replicaList = replicas.getSubTrees(
                    CommonSettings.REPLICA_TAG);
            for (StringTree<String> replicaTree : replicaList) {
                String replicaId = replicaTree.getValue(
                        CommonSettings.REPLICAID_TAG);
                knownReplicas.put(
                    replicaId, 
                    new Replica(
                            replicaId, 
                            replicaTree.getValue(
                               CommonSettings.REPLICANAME_TAG),
                            ReplicaType.fromSetting(
                                replicaTree.getValue(
                                   CommonSettings.REPLICATYPE_TAG))
                         )
                );
            }
        }
    }

    /** Get an object representing the replica with the given id.
     *
     * @param id The given name of an replica
     * @return an object representing the replica with the given id
     * @throws UnknownID if no replica is known with the given id
     */
    public static Replica getReplicaFromId(String id) {
        ArgumentNotValid.checkNotNullOrEmpty(id, "String id");
        initializeKnownReplicasList();
        if (!knownReplicas.containsKey(id)) {
            String message = "Can't find replica with id '" + id
                    + "', only know of " + knownReplicas.keySet();
            log.debug(message);
            throw new UnknownID(message);
        }
        return knownReplicas.get(id);
    }

    /** Get an object representing the replica with the given name.
    *
     * @param name The given name of an replica
     * @return an object representing the replica with the given name
     * @throws UnknownID if no replica is known with the given name
    */
    public static Replica getReplicaFromName(String name) {
        ArgumentNotValid.checkNotNullOrEmpty(name, "String name");
        initializeKnownReplicasList();
        // Note that this null value will never be returned.
        // will always be replaced by non-null value OR the method
        // will throw an UnknownID exception.
        Replica resRep = null; 
        boolean found = false; 
        for (Replica rep : knownReplicas.values()) {
            found = rep.getName().equals(name);
            if (found) { 
                resRep = rep;
                break;
            }
        }
        if (!found) {
           String message = "Can't find replica with name '" + name
                   + "', only know of names for " + knownReplicas.keySet();
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
        initializeKnownReplicasList();
        boolean found = false;
        for (String s : knownReplicas.keySet()) {
            found = knownReplicas.get(s).getName().equals(name);
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
        initializeKnownReplicasList();
        boolean found = false;
        for (String s : knownReplicas.keySet()) {
            found = s.equals(id);
            if (found) { break; }
        }
        return found;
    }   

    /** 
     * Get all known replicas.
     * @return A unmodifiable view of the currently known replicas.
     */
    public static Collection<Replica> getKnown() {
        initializeKnownReplicasList();
        return Collections.unmodifiableCollection(knownReplicas.values());
    }
    
    /**
     * Get all known replicas as ids.
     * @return all known replicas as ids
     */
    public static String[] getKnownIds() {
        initializeKnownReplicasList();
        String[] knownIds = new String[knownReplicas.keySet().size()];
        int index = 0;
        for (String s : knownReplicas.keySet()) {
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
        initializeKnownReplicasList();
        String[] knownNames = new String[knownReplicas.keySet().size()];
        int index = 0;
        for (String s : knownReplicas.keySet()) {
            knownNames[index] = knownReplicas.get(s).getName();
            index++;
        }
        return knownNames;
    }

    /** Get the type of this replica.
    *
    * @return The type of this replica (bitarchive or checksum).
    */
   public ReplicaType getType() {
       return type;
   }

   /** Get the id of this replica.
    *
    * @return The id of this replica (also used in queues).
    */
   public String getId() {
       return id;
   }

   /** Get the name of this replica.
     *
     * @return The name of this replica is known as in interface.
     */
    public String getName() {
        return name;
    }

    /** 
     * Get the identification channel that corresponds to this replica.
     * Please do not parse its name!
     *
     * @return The BaMon ChannelID of this replica.
     */
    public ChannelID getIdentificationChannel() {
        // This is the channel used by the connected replicas in the
        // ArcRepository.
        if (type == ReplicaType.BITARCHIVE) {
            return Channels.getBaMonForReplica(id);
        } else if (type == ReplicaType.CHECKSUM) {
            return Channels.getTheCrForReplica(id);
        } else {
            throw new UnknownID("No channel for replica " + toString());
        }
    }

    /** Returns a human-readable representation of the object.
     *
     * @return An arbitrary string version of the object.  Do not depend on
     * its format.
     */
    public String toString() {
        return type.toString() + "Replica (" + id + ") "+ name;
    }
}
