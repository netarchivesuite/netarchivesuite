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
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301 USA
 */

package dk.netarkivet.common.distribute;

import java.util.Collection;

import org.mortbay.log.Log;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.distribute.arcrepository.Replica;
import dk.netarkivet.common.distribute.arcrepository.ReplicaType;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IllegalState;
import dk.netarkivet.common.exceptions.UnknownID;
import dk.netarkivet.common.utils.Settings;

/**
 * This singleton class is in charge of giving out the correct channels.
 */

public class Channels {
      
   /**
    * Channel type prefixes for the current set of channels. 
    */
    private static final String ALLBA_CHANNEL_PREFIX = "ALL_BA";
    private static final String ANYBA_CHANNEL_PREFIX = "ANY_BA";
    private static final String THEBAMON_CHANNEL_PREFIX = "THE_BAMON";
    private static final String THESCHED_CHANNEL_PREFIX = "THE_SCHED";
    private static final String THEREPOS_CHANNEL_PREFIX = "THE_REPOS";
    private static final String ANYLOWHACO_CHANNEL_PREFIX 
        = "ANY_LOWPRIORITY_HACO";
    private static final String ANYHIGHHACO_CHANNEL_PREFIX
        = "ANY_HIGHPRIORITY_HACO";
    private static final String THISREPOSCLIENT_CHANNEL_PREFIX
        = "THIS_REPOS_CLIENT";
    private static final String ERROR_CHANNEL_PREFIX = "ERROR";
    private static final String INDEXSERVER_CHANNEL_PREFIX = "INDEX_SERVER";
    private static final String THISINDEXCLIENT_CHANNEL_PREFIX 
        = "THIS_INDEX_CLIENT";
    private static final String MONITOR_CHANNEL_PREFIX = "MONITOR";
    private static final String THECR_CHANNEL_PREFIX = "THE_CR";

    /** Channel part separator. */
    public static final String CHANNEL_PART_SEPARATOR = "_";
    
    /**
     * The one existing instance of the Channels object. Not accessible from the
     * outside at all.
     */
    private static Channels instance;

    /**
     * Accessor for singleton internally.
     *
     * @return the <code>Channels</code> object for this singleton.
     */
    private static Channels getInstance() {
        if (instance == null) {
            instance = new Channels();
        }
        return instance;
    }

    /**
     * Contains the collection of replicas.
     */
    private final Collection<Replica> replicas = Replica.getKnown();

    /**
     * This is the container for the replica which is used by applications
     * that only communicate with local processes.
     */
    private final Replica useReplica = Replica.getReplicaFromId(
            Settings.get(CommonSettings.USE_REPLICA_ID));
    
    /** 
     * The constructor of Channels class. 
     * Validates that the current value of the setting USE_REPLICA_ID
     * corresponds to one of the replicas listed in the settings.
     * Furthermore we here fill content in the ALL_BA_ARRAY, ANY_BA_ARRAY,
     * THE_BAMON_ARRAY, and initialize ALL_BA, ANY_BA, and THE_BAMON.
     * 
     * @throws UnknownID If one of the replicas has an unhandled replica type.
     */
    private Channels() {
        // index count
        int i = 0;
        int useReplicaIndex = -1;
        // go through all replicas and initialize their channels.
        for(Replica rep : replicas) {
            if(rep.getType() == ReplicaType.BITARCHIVE) {
                // Bitarchive has 'ALL_BA', 'ANY_BA' and 'THE_BAMON'.
                ALL_BA_ARRAY[i] = new ChannelID(ALLBA_CHANNEL_PREFIX,
                        rep.getId(), ChannelID.NO_IP,
                        ChannelID.NO_APPLINST_ID, ChannelID.TOPIC);
                ANY_BA_ARRAY[i] = new ChannelID(ANYBA_CHANNEL_PREFIX,
                        rep.getId(), ChannelID.NO_IP,
                        ChannelID.NO_APPLINST_ID, ChannelID.QUEUE);
                THE_BAMON_ARRAY[i] = new ChannelID(THEBAMON_CHANNEL_PREFIX,
                        rep.getId(), ChannelID.NO_IP,
                        ChannelID.NO_APPLINST_ID, ChannelID.QUEUE);
                THE_CR_ARRAY[i] = null;
            } else if(rep.getType() == ReplicaType.CHECKSUM){
                // Checksum has only 'THE_CR'.
                ALL_BA_ARRAY[i] = null;
                ANY_BA_ARRAY[i] = null;
                THE_BAMON_ARRAY[i] = null;
                THE_CR_ARRAY[i] = new ChannelID(THECR_CHANNEL_PREFIX,
                        rep.getId(), ChannelID.NO_IP,
                        ChannelID.NO_APPLINST_ID, ChannelID.QUEUE);
            } else {
                // Throw an exception when unknown replica type.
                throw new UnknownID("The replica '" + rep + "' does not have "
                        + "a valid replica type.");
            }
            
            // find the 'useReplica'
            if(rep == useReplica) {
                useReplicaIndex = i;
            }
            
            i++;
        }
        
        // validate the index of the useReplica
        if(useReplicaIndex < 0 || useReplicaIndex >= replicas.size()) {
            // issue an error, if the use replica could not be found. 
            throw new ArgumentNotValid(
                    "The useReplica '" + useReplica + "' was not found in the "
                    + "list of replicas: '" + replicas + "'.");
        }
        
        // set the channels for the useReplica
        ALL_BA = ALL_BA_ARRAY[useReplicaIndex];
        ANY_BA = ANY_BA_ARRAY[useReplicaIndex];
        THE_BAMON = THE_BAMON_ARRAY[useReplicaIndex];
        THE_CR = THE_CR_ARRAY[useReplicaIndex];
    }
    
    /**
     * Method for retrieving the list of replicas used for the channels.
     * The replica ids are in the same order as their channels.  
     * 
     * @return The replica ids in the same order as their channels.
     */
    public static Collection<Replica> getReplicas() {
        return getInstance().replicas;
    }
    
    /**
     * Returns the queue on which HarvestControllers reply with status messages
     * to the HarvestScheduler.
     *
     * @return the <code>ChannelID</code> object for the queue on which
     *         HarvestControllers reply with status messages to the
     *         HarvestScheduler
     */
    public static ChannelID getTheSched() {
        return getInstance().THE_SCHED;
    }

    private final ChannelID THE_SCHED = new ChannelID(THESCHED_CHANNEL_PREFIX,
            ChannelID.COMMON, ChannelID.NO_IP, ChannelID.NO_APPLINST_ID,
            ChannelID.QUEUE);

    /**
     * Returns the queue which is used by the scheduler to send doOneCrawl to
     * Harvest Controllers of high priority (selective harvests).
     *
     * @return That channel (queue)
     */
    public static ChannelID getAnyHighpriorityHaco() {
        return getInstance().ANY_HIGHPRIORITY_HACO;
    }

    /**
     * Returns the queue which is used by the scheduler to send doOneCrawl to
     * Harvest Controllers of low priority (snapshot harvests).
     *
     * @return That channel (queue)
     */
    public static ChannelID getAnyLowpriorityHaco() {
        return getInstance().ANY_LOWPRIORITY_HACO;
    }

    private final ChannelID ANY_LOWPRIORITY_HACO = new ChannelID(
            ANYLOWHACO_CHANNEL_PREFIX, ChannelID.COMMON, ChannelID.NO_IP,
            ChannelID.NO_APPLINST_ID, ChannelID.QUEUE);

    private final ChannelID ANY_HIGHPRIORITY_HACO = new ChannelID(
            ANYHIGHHACO_CHANNEL_PREFIX, ChannelID.COMMON, ChannelID.NO_IP,
            ChannelID.NO_APPLINST_ID, ChannelID.QUEUE);

    /**
     * Returns the one-per-client queue on which client receives replies from
     * the arcrepository.
     *
     * @return the <code>ChannelID</code> object for this queue.
     */
    public static ChannelID getThisReposClient() {
        return getInstance().THIS_REPOS_CLIENT;
    }

    private final ChannelID THIS_REPOS_CLIENT = new ChannelID(
            THISREPOSCLIENT_CHANNEL_PREFIX, ChannelID.COMMON,
            ChannelID.INCLUDE_IP, ChannelID.INCLUDE_APPLINST_ID,
            ChannelID.QUEUE);

    /**
     * Returns the queue on which all messages to the Repository are sent.
     *
     * @return the <code>ChannelID</code> object for this queue.
     */
    public static ChannelID getTheRepos() {
        return getInstance().THE_REPOS;
    }

    private final ChannelID THE_REPOS = new ChannelID(THEREPOS_CHANNEL_PREFIX,
            ChannelID.COMMON, ChannelID.NO_IP, ChannelID.NO_APPLINST_ID,
            ChannelID.QUEUE);

    /**
     * Returns BAMON channels for every known bitarchive (replica).
     *
     * @return An array of BAMON channels - one per bitarchive (replica)
     */
    public static final ChannelID[] getAllArchives_BAMONs() {
        return getInstance().THE_BAMON_ARRAY;
    }

    private final ChannelID[] THE_BAMON_ARRAY =
        new ChannelID[replicas.size()];

    /**
     * Returns the queue for sending messages to bitarchive monitors.
     *
     * @return the <code>ChannelID</code> object for this queue.
     * @throws IllegalState If the current replica is not a checksum replica.
     */
    public static ChannelID getTheBamon() throws IllegalState {
        ChannelID res = getInstance().THE_BAMON;

        if (res == null) {
            throw new IllegalState(
                    "The channel for the bitarchive monitor "
                            + " cannot to be retrieved for replica '"
                            + getInstance().useReplica
                            + "'.");
        }

        return res;
    }

    /**
     * Implementation notice: This cannot be initialized directly in the field,
     * as it uses THE_BAMON_ARRAY, which is initialized in the constructor.
     */
    private final ChannelID THE_BAMON;
    
    /**
     * Returns the channels for the all Checksum replicas.
     * 
     * @return An array of THE_CR channels - one for each replica, though only
     * the checksum replicas have values (the others are null). 
     */
    public static final ChannelID[] getAllArchives_CRs() {
        return getInstance().THE_CR_ARRAY;
    }
    
    /** The array containing the 'THE_CR' channels.*/
    private final ChannelID[] THE_CR_ARRAY
        = new ChannelID[replicas.size()];
   
    /** 
     * Method for retrieving the 'THE_CR' channel for this replica.
     * If the replica is not a checksum replica, then an error is thrown.
     *  
     * @return the 'THE_CR' channel for this replica. 
     * @throws IllegalState If the current replica is not a checksum replica.
     */
    public static ChannelID getTheCR() throws IllegalState {
        ChannelID res = getInstance().THE_CR;

        if (res == null) {
            throw new IllegalState("A bitarchive replica does not have the "
                    + "channel for communicating with a checksum replica.");
        }

        return res;
    }
    
    /** The 'THE_CR' channel for this replica. This has the value 'null' if 
     * the replica is not a checksum replica.*/
    private final ChannelID THE_CR;

    /**
     * Returns ALL_BA channels for every known bitarchive.
     *
     * @return An array of ALL_BA channels - one per bitarchive
     */
    public static final ChannelID[] getAllArchives_ALL_BAs() {
        return getInstance().ALL_BA_ARRAY;
    }

    /**
     * ALL_BA is the topic on which a Bitarchive client publishes get, correct
     * and batch messages to all connected Bitarchive machines. The following is
     * the list of ALL_BA for all archives (i.e. archive replicas).
     */
    private final ChannelID[] ALL_BA_ARRAY
        = new ChannelID[replicas.size()];

    /**
     * Returns the topic that all bitarchive machines on this replica
     * are listening on.
     *
     * @return A topic channel that reaches all local bitarchive machines
     * @throws IllegalState If the current replica is not a bitarchive replica.
     */
    public static ChannelID getAllBa() throws IllegalState {
        ChannelID res = getInstance().ALL_BA;

        if (res == null) {
            throw new IllegalState("A checksum replica does not have the "
                    + "channels for communicating with a bitarchive replica.");
        }

        return res;
    }

    /**
     * Implementation notice: This cannot be initialized directly in the field,
     * as it uses ALL_BA_ARRAY, which is initialized in the constructor.
     */
    private final ChannelID ALL_BA;

    /**
     * Returns ANY_BA channels for every known bitarchive.
     *
     * @return An array of ANY_BA channels - one per bitarchive
     */
    public static final ChannelID[] getAllArchives_ANY_BAs() {
        return getInstance().ANY_BA_ARRAY;
    }

    /**
     * Queue on which upload requests are sent out to bitarchive servers. The
     * following is the list of ANY_BA for all archives.
     */
    private final ChannelID[] ANY_BA_ARRAY
        = new ChannelID[replicas.size()];

    /**
     * Returns the channel where exactly one of all the bitarchive machines at
     * this replica will get the message.
     *
     * @return A queue channel that reaches one of the local bitarchive
     *         machines.
     * @throws IllegalState If the current replica is not a bitarchive replica.
     */
    public static ChannelID getAnyBa() throws IllegalState {
        ChannelID res = getInstance().ANY_BA;

        if (res == null) {
            throw new IllegalState("A checksum replica does not have the "
                    + "channels for communicating with a bitarchive replica.");
        }

        return res;
    }

    /**
     * Implementation notice: This cannot be initialized directly in the field,
     * as it uses ANY_BA_ARRAY, which is initialized in the constructor.
     */
    private final ChannelID ANY_BA;

    /**
     * Returns the queue on which to put errors which are not handled elsewhere.
     *
     * @return the <code>ChannelID</code> object for this queue.
     */
    public static ChannelID getError() {
        return getInstance().ERROR;
    }

    private final ChannelID ERROR = new ChannelID(ERROR_CHANNEL_PREFIX,
            ChannelID.COMMON, ChannelID.NO_IP, ChannelID.NO_APPLINST_ID,
            ChannelID.QUEUE);

    /**
     * Given an replica, returns the BAMON queue to which batch jobs
     * must be sent in order to run them on that bitarchive.
     *
     * @param replicaId The id of the replica
     * @return the channel
     * @throws ArgumentNotValid
     *             if the replicaId is null, unknown, or empty string
     */
    public static ChannelID getBaMonForReplica(String replicaId)
            throws ArgumentNotValid {
        ArgumentNotValid.checkNotNullOrEmpty(replicaId, "replicaId");
        ChannelID[] bamons = getAllArchives_BAMONs();
        for (ChannelID bamon : bamons) {
            if (bamon != null && bamon.getName().equals(
                    Settings.get(CommonSettings.ENVIRONMENT_NAME)
                            + CHANNEL_PART_SEPARATOR + replicaId
                            + CHANNEL_PART_SEPARATOR 
                            + THEBAMON_CHANNEL_PREFIX)) {
                return bamon;
            }
        }
        throw new ArgumentNotValid("Did not find a BAMON queue for '"
                + replicaId + "'");
    }
    
    public static ChannelID getTheCrForReplica(String replicaId) {
        ArgumentNotValid.checkNotNullOrEmpty(replicaId, "String replicaId");
        ChannelID[] crs = getAllArchives_CRs();
        for (ChannelID cr : crs) {
            if (cr != null
                    && cr.getName().equals(
                            Settings.get(CommonSettings.ENVIRONMENT_NAME)
                                    + CHANNEL_PART_SEPARATOR + replicaId
                                    + CHANNEL_PART_SEPARATOR
                                    + THECR_CHANNEL_PREFIX)) {
                return cr;
            }
        }
        throw new ArgumentNotValid("Did not find a checksum queue for '"
                + replicaId + "'");
    }
    
    /**
     * Method for extracting the replica from the name of the identifier 
     * channel.
     * 
     * @param channelName The name of the identification channel for the 
     * replica.
     * @return Replica who the identification channel belong to.
     * @throws UnknownID If the replicaId does not point to a know replica.
     * @throws ArgumentNotValid If the channelName is either null or empty.
     */
    public static Replica retrieveReplicaFromIdentifierChannel(
            String channelName) throws UnknownID, ArgumentNotValid {
        ArgumentNotValid.checkNotNullOrEmpty(channelName, "String channelName");
        if (channelName.contains(THECR_CHANNEL_PREFIX)) {
            // environmentName ## replicaId ## THE_CR
            String[] parts = channelName.split(CHANNEL_PART_SEPARATOR);
            return Replica.getReplicaFromId(parts[1]);
        } else if (channelName.contains(THEBAMON_CHANNEL_PREFIX)) {
            // environmentName ## replicaId ## THE_BAMON
            String[] parts = channelName.split(CHANNEL_PART_SEPARATOR);
            return Replica.getReplicaFromId(parts[1]);
        }

        String errMsg = "The current channel name, '" + channelName 
                + "' does not refer to an identification channel";
        Log.warn(errMsg);
        throw new UnknownID(errMsg);
    }
    
    /**
     * The method for retrieving the name of the identification channel for
     * a replica based on the Id of this replica. 
     *  
     * @param replicaId The id for the replica whose identification channel
     * name should be retrieved.
     * @return The name of the identification channel for the replica.
     * @throws UnknownID If no replica with the given replica id is known.
     * @throws ArgumentNotValid If the replicaId is null or empty.
     */
    public static String retrieveReplicaChannelNameFromReplicaId(
            String replicaId) throws UnknownID, ArgumentNotValid {
        ArgumentNotValid.checkNotNullOrEmpty(replicaId, "String replicaId");
        return Replica.getReplicaFromId(replicaId).getIdentificationChannel().getName();
    }
    
    /**
     * The method for retrieving the identification channel for a replica 
     * based on the Id of this replica. 
     *  
     * @param replicaId The id for the replica whose identification channel
     * name should be retrieved.
     * @return The identification channel for the replica.
     * @throws UnknownID If no replica with the given replica id is known.
     * @throws ArgumentNotValid If the replicaId is null or empty.
     */
    public static ChannelID retrieveReplicaChannelFromReplicaId(String replicaId)
            throws UnknownID, ArgumentNotValid {
        ArgumentNotValid.checkNotNullOrEmpty(replicaId, "String replicaId");
        return Replica.getReplicaFromId(replicaId).getIdentificationChannel();
    }

    /**
     * Returns the queue for sending messages to the IndexServer
     * application.
     *
     * @return the <code>ChannelID</code> object for this queue.
     */
    public static ChannelID getTheIndexServer() {
        return getInstance().THE_INDEX_SERVER;
    }

    private final ChannelID THE_INDEX_SERVER = new ChannelID(
            INDEXSERVER_CHANNEL_PREFIX,
            ChannelID.COMMON,
            ChannelID.NO_IP,
            ChannelID.NO_APPLINST_ID,
            ChannelID.QUEUE);

    /**
     * Returns the queue for getting responses from the IndexServer
     * application.
     *
     * @return the <code>ChannelID</code> object for this queue.
     */
    public static ChannelID getThisIndexClient() {
        return getInstance().THIS_INDEX_CLIENT;
    }

    //TODO Should we use client channels for all our servers?
    private final ChannelID THIS_INDEX_CLIENT = new ChannelID(
            THISINDEXCLIENT_CHANNEL_PREFIX, 
            ChannelID.COMMON,
            ChannelID.INCLUDE_IP,
            ChannelID.INCLUDE_APPLINST_ID,
            ChannelID.QUEUE);


    /** Return the queue for the monitor registry.
     *
     * @return the <code>ChannelID</code> object for the queue.
     */
    public static ChannelID getTheMonitorServer() {
        return getInstance().THE_MONITOR_SERVER;
    }

    private final ChannelID THE_MONITOR_SERVER = new ChannelID(
            MONITOR_CHANNEL_PREFIX,
            ChannelID.COMMON,
            ChannelID.NO_IP,
            ChannelID.NO_APPLINST_ID,
            ChannelID.QUEUE);

    /**
     * Reset the instance to re-read the settings. Only for use in tests.
     */
    static void reset() {
        instance = null;
    }

    /**
     * Is a given name a ChannelName for a Topic or a Queue.
     * @param name a given name
     * @return true, if arg name contains the string "_ALL_"
     */
    public static boolean isTopic(String name) {
        ArgumentNotValid.checkNotNullOrEmpty(name, "String name"); 
        return name.contains("_ALL_");
    }
}
