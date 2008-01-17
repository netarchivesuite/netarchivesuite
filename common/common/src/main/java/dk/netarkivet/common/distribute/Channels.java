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

package dk.netarkivet.common.distribute;

import java.util.Arrays;

import dk.netarkivet.common.Settings;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.PermissionDenied;

/**
 * This singleton class is in charge of giving out the correct channels.
 */

public class Channels {
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
     * The following fields are read from settings.xml. allLocations is the list
     * of all locations in the environment. It is used for
     * applications that need to communicate with e.g. all bit archives. An
     * example value is {"KB","SB"}.
     */
    private final String[] allLocations = Settings
            .getAll(Settings.ENVIRONMENT_LOCATION_NAMES);

    /**
     * thisLocation is the local location, used for applications
     * that only communicate with local processes. An example value is "KB".
     */
    private final String thisLocation = Settings
            .get(Settings.ENVIRONMENT_THIS_LOCATION);

    /** The index of this location in the allLocations list. */
    private final int indexOfThisLocation = Arrays.asList(allLocations)
            .indexOf(thisLocation);

    private Channels() {
        if (indexOfThisLocation < 0) {
            throw new ArgumentNotValid("Bad location '" + thisLocation + "'");
        }
        for (int i = 0; i < allLocations.length; i++) {
            ALL_BA_ARRAY[i] = new ChannelID("ALL_BA", allLocations[i],
                    ChannelID.NO_IP, ChannelID.NO_PROC_ID, ChannelID.TOPIC);
        }
        ALL_BA = ALL_BA_ARRAY[indexOfThisLocation];
        for (int i = 0; i < allLocations.length; i++) {
            ANY_BA_ARRAY[i] = new ChannelID("ANY_BA", allLocations[i],
                    ChannelID.NO_IP, ChannelID.NO_PROC_ID, ChannelID.QUEUE);
        }
        ANY_BA = ANY_BA_ARRAY[indexOfThisLocation];
        for (int i = 0; i < allLocations.length; i++) {
            THE_BAMON_ARRAY[i] = new ChannelID("THE_BAMON", allLocations[i],
                    ChannelID.NO_IP, ChannelID.NO_PROC_ID, ChannelID.QUEUE);
        }
        THE_BAMON = THE_BAMON_ARRAY[indexOfThisLocation];
    }

    /* ******** Sort in order of the Distributed Architecture paper ******** */
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

    private final ChannelID THE_SCHED = new ChannelID("THE_SCHED",
            ChannelID.COMMON, ChannelID.NO_IP, ChannelID.NO_PROC_ID,
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
            "ANY_LOWPRIORITY_HACO", ChannelID.COMMON, ChannelID.NO_IP,
            ChannelID.NO_PROC_ID, ChannelID.QUEUE);

    private final ChannelID ANY_HIGHPRIORITY_HACO = new ChannelID(
            "ANY_HIGHPRIORITY_HACO", ChannelID.COMMON, ChannelID.NO_IP,
            ChannelID.NO_PROC_ID, ChannelID.QUEUE);

    /**
     * Returns the one-per-HACO queue on which HaCo receives replies from the
     * arcrepository.
     *
     * @return the <code>ChannelID</code> object for this queue.
     */
    public static ChannelID getThisHaco() {
        return getInstance().THIS_HACO;
    }

    private final ChannelID THIS_HACO = new ChannelID("THIS_HACO",
            ChannelID.COMMON, ChannelID.INCLUDE_IP, ChannelID.INCLUDE_PROC_ID,
            ChannelID.QUEUE);

    /**
     * Returns the queue on which all messages to the ArcRepository are sent.
     *
     * @return the <code>ChannelID</code> object for this queue.
     */
    public static ChannelID getTheArcrepos() {
        return getInstance().THE_ARCREPOS;
    }

    private final ChannelID THE_ARCREPOS = new ChannelID("THE_ARCREPOS",
            ChannelID.COMMON, ChannelID.NO_IP, ChannelID.NO_PROC_ID,
            ChannelID.QUEUE);

    /**
     * Returns BAMON channels for every known bitarchive (location).
     *
     * @return An array of BAMON channels - one per bitarchive (location)
     */
    public static final ChannelID[] getAllArchives_BAMONs() {
        return getInstance().THE_BAMON_ARRAY;
    }

    private final ChannelID[] THE_BAMON_ARRAY =
        new ChannelID[allLocations.length];

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
     * the list of ALL_BA for all archives (i.e. archive locations).
     */
    private final ChannelID[] ALL_BA_ARRAY = new ChannelID[allLocations.length];

    /**
     * Returns the topic that all bitarchive machines on this location
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
    private final ChannelID[] ANY_BA_ARRAY = new ChannelID[allLocations.length];

    /**
     * Returns the channel where exactly one of all the bitarchive machines at
     * this location will get the message.
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

    private final ChannelID ERROR = new ChannelID("ERROR", ChannelID.COMMON,
            ChannelID.NO_IP, ChannelID.NO_PROC_ID, ChannelID.QUEUE);

    /**
     * Given an location, returns the BAMON queue to which batch jobs
     * must be sent in order to run them on that locations bitarchive.
     *
     * @param location the location
     * @return the channel
     * @throws ArgumentNotValid
     *             if the location is null, unknown, or empty string
     */
    public static ChannelID getBaMonForLocation(String location)
            throws ArgumentNotValid {
    	ArgumentNotValid.checkNotNullOrEmpty(location, "location");
        ChannelID[] bamons = getAllArchives_BAMONs();
        for (ChannelID bamon : bamons) {
            if ((bamon.getName().split("_")[1]).equals(location)) {
                return bamon;
            }
        }
        throw new ArgumentNotValid("Did not find a BAMON queue for "
                + location);
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
            "INDEX_SERVER",
            ChannelID.COMMON,
            ChannelID.NO_IP,
            ChannelID.NO_PROC_ID,
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

    //TODO: Should we use client channels for all our servers?
    private final ChannelID THIS_INDEX_CLIENT = new ChannelID(
            "INDEX_CLIENT",
            ChannelID.COMMON,
            ChannelID.INCLUDE_IP,
            ChannelID.INCLUDE_PROC_ID,
            ChannelID.QUEUE);

    /**
     * Reset the instance to re-read the settings. Only for use in tests.
     */
    static void reset() {
        instance = null;
    }
}
