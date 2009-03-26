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
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301 USA
 */

package dk.netarkivet.common.distribute;

import java.util.Arrays;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
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
     * The following fields are read from settings.xml. allReplicaIds is the
     * list of all replica ids in the environment. It is used for
     * applications that need to communicate with e.g. all bitarchives. An
     * example value is {"ONE","TWO"}.
     */
    private final String[] allReplicaIds = Settings.getAll(
            CommonSettings.REPLICA_IDS);

    /**
     * useReplicaId is the id of the replica, used for applications
     * that only communicate with local processes. An example value is "ONE".
     */
    private final String useReplicaId = Settings.get(
            CommonSettings.USE_REPLICA_ID);

    /** The index of use replica id n the allReplicas list. */
    private final int indexOfUseReplicaId = Arrays.asList(allReplicaIds)
            .indexOf(useReplicaId);
    
    /** The constructor of Channels class. 
     *  Validates that the current value of the setting USE_REPLICA_ID
     *  corresponds to one of the replicas listed in the settings.
     *  Furthermore we here fill content in the ALL_BA_ARRAY, ANY_BA_ARRAY,
     *  THE_BAMON_ARRAY, and initialize ALL_BA, ANY_BA, and THE_BAMON.
     *  
     */
    private Channels() {
        if (indexOfUseReplicaId < 0) {
             throw new ArgumentNotValid("Bad replicas " 
                        + "useReplica: '" + useReplicaId + "'");
        }

        for (int i = 0; i < allReplicaIds.length; i++) {
            ALL_BA_ARRAY[i] = new ChannelID(ALLBA_CHANNEL_PREFIX,
                    allReplicaIds[i], ChannelID.NO_IP,
                    ChannelID.NO_APPLINST_ID, ChannelID.TOPIC);
        }
        ALL_BA = ALL_BA_ARRAY[indexOfUseReplicaId];
        for (int i = 0; i < allReplicaIds.length; i++) {
            ANY_BA_ARRAY[i] = new ChannelID(ANYBA_CHANNEL_PREFIX,
                    allReplicaIds[i], ChannelID.NO_IP,
                    ChannelID.NO_APPLINST_ID, ChannelID.QUEUE);
        }
        ANY_BA = ANY_BA_ARRAY[indexOfUseReplicaId];
        for (int i = 0; i < allReplicaIds.length; i++) {
            THE_BAMON_ARRAY[i] = new ChannelID(THEBAMON_CHANNEL_PREFIX,
                    allReplicaIds[i], ChannelID.NO_IP,
                    ChannelID.NO_APPLINST_ID, ChannelID.QUEUE);
        }
        THE_BAMON = THE_BAMON_ARRAY[indexOfUseReplicaId];
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
        new ChannelID[allReplicaIds.length];

    /**
     * Returns the queue for sending messages to bitarchive monitors.
     *
     * @return the <code>ChannelID</code> object for this queue.
     */
    public static ChannelID getTheBamon() {
        return getInstance().THE_BAMON;
    }

    /**
     * Implementation notice: This cannot be initialized directly in the field,
     * as it uses THE_BAMON_ARRAY, which is initialized in the constructor.
     */
    private final ChannelID THE_BAMON;

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
        = new ChannelID[allReplicaIds.length];

    /**
     * Returns the topic that all bitarchive machines on this replica
     * are listening on.
     *
     * @return A topic channel that reaches all local bitarchive machines
     */
    public static ChannelID getAllBa() {
        return getInstance().ALL_BA;
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
        = new ChannelID[allReplicaIds.length];

    /**
     * Returns the channel where exactly one of all the bitarchive machines at
     * this replica will get the message.
     *
     * @return A queue channel that reaches one of the local bitarchive
     *         machines.
     */
    public static ChannelID getAnyBa() {
        return getInstance().ANY_BA;
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
            if (bamon.getName().equals(
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
