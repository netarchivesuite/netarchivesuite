/* $Id$
 * $Revision$
 * $Author$
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

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.UnknownID;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.utils.SystemUtils;

/**
 * A class for representing the names of JMS queues.
 */
public class ChannelID implements Serializable {

    /**
     * The name of the enviroment in which this process is running.
     * It is used for prefixing all ChannelIDs.
     * An example value is "PROD".
     */
    private final String environmentName =
            Settings.get(CommonSettings.ENVIRONMENT_NAME);
    /**
     * application instance id is a setting for an application's
     * identification as a specific process on a given machine.
     * An example value is "BAONE"
     * Note that it is set to uppercase in order to ensure that a channel name
     * only has uppercase characters.
     */
    private static final String applicationInstanceId =
            Settings.get(CommonSettings.APPLICATION_INSTANCE_ID).toUpperCase();
    /**
     * application instance id is a setting for an application's
     * identification as a specific process on a given machine.
     * An example value is "BAONE".
     */
    private static final String applicationAbbreviation =
        getApplicationAbbreviation(
                    Settings.get(CommonSettings.APPLICATION_NAME));
    /**
     * Constants to make the semantics of parameters to our name constructors
     * more explicit.
     */
    static final String COMMON = "COMMON";
    static final boolean INCLUDE_IP = true;
    static final boolean NO_IP = false;
    static final boolean INCLUDE_APPLINST_ID = true;
    static final boolean NO_APPLINST_ID = false;
    static final boolean TOPIC = true;
    static final boolean QUEUE = false;
    /**
     * A ChannelID is identified by its name.
     * It has one bit of state information: is it a queue or a topic?
     */
    private String name;
    private boolean isTopic;

    /**
    * Constructor of channel names.
    * The constructor is package private because we should never use any
    * channels except the ones constructed by our friend Channels.java
    * @param appPref The prefix used for the applications listening 
    * to the channel.
    * @param replicaId Name of the replica, or ChannelID.COMMON if
    * channel shared by all replicas.
    * @param useNodeId Whether that IP address of the local node should
    * be included in the channel name.
    * @param useAppInstId Whether application instance id from settings 
    * should be included in the channel name.
    * @param isTopic Whether the Channel is a Topic or a Queue.
    * @throws UnknownID if looking up the local IP number failed.
    */
    ChannelID(String appPref, String replicaId, boolean useNodeId,
        boolean useAppInstId, boolean isTopic) {
        this.name = constructName(appPref, replicaId, useNodeId, useAppInstId);
        this.isTopic = isTopic;
    }

    /**
    * Constructs a channel name according to the specifications
    * of channels in the NetarchiveSuite Developer Manual.
    * @param appPref The prefix used for the applications listening 
    * to the channel.
    * @param replicaId Id of the replica, or ChannelID.COMMON if
    * channel common to all bitarchive replicas.
    * @param useNodeId Whether that IP address of the local node should
    * be included in the channel name.
    * @param useAppInstId Whether application instance id from settings 
    * should be included in the channel name.
    * @return The properly concatenated channel name.
    * @throws UnknownID if looking up the local IP number failed.
    */
    private String constructName(String appPref, String replicaId,
        boolean useNodeId, boolean useAppInstId) {
        String userId = environmentName;
        String id = "";
        if (useNodeId) {
            // Replace the '.' in the IP-address with '_'
            id = SystemUtils.getLocalIP().replace('.', '_');
            if (useAppInstId) {
                id += Channels.CHANNEL_PART_SEPARATOR 
                    + applicationAbbreviation;
                if (!applicationInstanceId.isEmpty()) {
                    id += (Channels.CHANNEL_PART_SEPARATOR 
                            + applicationInstanceId);
                }
            }
        }
        
        String resultingName = userId + Channels.CHANNEL_PART_SEPARATOR + replicaId 
            + Channels.CHANNEL_PART_SEPARATOR + appPref;
        if (!id.isEmpty()) {
            resultingName += Channels.CHANNEL_PART_SEPARATOR + id; 
        }
        
        return resultingName;
    }
    /**
     * Getter for the channel name.
     *
     * @return The name of the channel referred to by this object.
     */
    public String getName() {
        return name;
    }
    /**
    * Pretty-printer.
    * @return a nice String representation of the ChannelID.
    */
    public String toString() {
        return isTopic ? ("[Topic '" + name + "']") : ("[Queue '" + name
        + "']");
    }
    /**
     * Getter method for isTopic.
     * This method is package-private because it should only be used
     * by JMSConnection.
     * @return Whether this channel is a Topic. If not a Topic, it is a Queue.
     */
    boolean isTopic() {
        return isTopic;
    }
    /**
     * Method used by Java deserialization.
     * Our coding guidelines prescribes that this method should always
     * be implemented, even if it only calls the default method:
     * http://kb-prod-udv-001.kb.dk/twiki/bin/view/Netarkiv/ImplementeringOgTestAfSerializable
     * See also "Effective Java", pages 219 and 224.
     * @param ois the ObjectInputStream used to read in the object
     * @throws IOFailure if Java could not deserialize the object.
     */
    private void readObject(ObjectInputStream ois) {
        try {
            ois.defaultReadObject();
        } catch (Exception e) {
            throw new IOFailure("Standard deserialization of ChannelID failed.",
                e);
        }
    }
    /**
     * Method used by Java serialization.
     * Our coding guidelines prescribes that this method should always
     * be implemented, even if it only calls the default method:
     * http://kb-prod-udv-001.kb.dk/twiki/bin/view/Netarkiv/ImplementeringOgTestAfSerializable
     * See also "Effective Java", pages 219 and 224.
     * @param oos the ObjectOutputStream used to serialize the object.
     * @throws IOFailure if Java could not serialize the object.
     */
    private void writeObject(ObjectOutputStream oos) {
        try {
            oos.defaultWriteObject();
        } catch (Exception e) {
            throw new IOFailure("Standard serialization of ChannelID failed.",
                    e);
        }
    }
    /**
    * Implements equality check for ChannelIDs. Useful when these are used
    * as indexes in Java collections, for instance.
    * @param o The object to compare this object with.
    * @return Whether o and this should be considered the same ChannelID.
    */
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ChannelID)) {
            return false;
        }
        final ChannelID channelID = (ChannelID) o;
        if (isTopic != channelID.isTopic) {
            return false;
        }
        if (!name.equals(channelID.name)) {
            return false;
        }
        return true;
    }
    /**
    * Computes a hash code based on the channel name and whether it is a topic.
    * @return A hash code for this object.
    */
    public int hashCode() {
        int result;
        result = name.hashCode();
        result = (29 * result) + (isTopic ? 1 : 0);
        return result;
    }
    
    /**
     * Finds abbreviation for an application name.
     * The abbreviation is only calculated from the application name without
     * path. It is made from the uppercase letters in the name.
     * @param applName application name with full path
     * @return abbreviation for given application name.
     */
    private static String getApplicationAbbreviation(String applName) {
        ArgumentNotValid.checkNotNull(applName, "applName");
        //Strip path from name
        String[] p = applName.split("[.]");
        if (p.length <= 0) {
            return "";
        }
        String shortName = p[p.length - 1];
        //put uppercase letters into abbr
        String abbr = "";
        for (int i = 0; i< shortName.length(); i++) {
            if (Character.isUpperCase(shortName.charAt(i))) {
                abbr += shortName.substring(i, i + 1);
            }
        }
        //return found abbreviation
        return abbr;
    }
}
