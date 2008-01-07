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

import dk.netarkivet.common.Settings;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.UnknownID;
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
        Settings.get(Settings.ENVIRONMENT_NAME);
    /**
     * httpPortNumber is the IP port that is reserved for this application's
     * HTTP communication (if applicable).
     * It is used for identifying a specific process on a given machine.
     * An example value is "8080".
     */
    private static final String httpPortNumber =
        Settings.get(Settings.HTTP_PORT_NUMBER);
    /**
     * Constants to make the semantics of parameters to our name constructors
     * more explicit.
     */
    static final String COMMON = "COMMON";
    static final boolean INCLUDE_IP = true;
    static final boolean NO_IP = false;
    static final boolean INCLUDE_PROC_ID = true;
    static final boolean NO_PROC_ID = false;
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
    * @param app The name of the applications listening to the channel.
    * @param locationName Name of the location, or ChannelID.COMMON if
    * channel shared by all locations.
    * @param useNodeId Whether that IP address of the local node should
    * be included in the channel name.
    * @param useProcId Whether process identifier (= IP port# used for
    * communication) should be included in the channel name.
    * @param isTopic Whether the Channel is a Topic or a Queue.
    * @throws UnknownID if looking up the local IP number failed.
    */
    ChannelID(String app, String locationName, boolean useNodeId,
        boolean useProcId, boolean isTopic) {
        this.name = constructName(app, locationName, useNodeId, useProcId);
        this.isTopic = isTopic;
    }

    /**
    * Constructs a channel name according to the specifications
    * in distributed_architecture.doc.
    * @param app The name of the applications listening to the channel.
    * @param locationName Name of the location, or ChannelID.COMMON if
    * channel common to both locations.
    * @param useNodeId Whether that IP address of the local node should
    * be included in the channel name.
    * @param useProcId Whether process identifier 
    * 	(= IP port# used for communication)
    * should be included in the channel name.
    * @return The properly concatenated channel name.
    * @throws UnknownID if looking up the local IP number failed.
    */
    private String constructName(String app, String locationName,
        boolean useNodeId, boolean useProcId) {
        String userId = environmentName;
        String id = "";
        if (useNodeId) {
            id = SystemUtils.getLocalIP().replace('.', '_');
            if (useProcId) {
                id += ("_" + httpPortNumber);
            }
        }
        return userId + "_" + locationName + "_" + app
            + (id.equals("") ? "" : ("_" + id));
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
     * Method used by Java serialization.
     * Our coding guidelines prescribes that this method should always
     * be implemented, even if it only calls the default method:
     * http://asterix.kb.dk/twiki/bin/view/Netarkiv/ImplementeringOgTestAfSerializable
     * See also "Effective Java", pages 219 and 224.
     * @param s
     * @throws IOFailure if Java could not deserialize the object.
     */
    private void readObject(ObjectInputStream s) {
        try {
            s.defaultReadObject();
        } catch (Exception e) {
            throw new IOFailure("Standard deserialization of ChannelID failed.",
                e);
        }
    }
    /**
     * Method used by Java serialization.
     * Our coding guidelines prescribes that this method should always
     * be implemented, even if it only calls the default method:
     * http://asterix.kb.dk/twiki/bin/view/Netarkiv/ImplementeringOgTestAfSerializable
     * See also "Effective Java", pages 219 and 224.
     * @param s
     * @throws IOFailure if Java could not serialize the object.
     */
    private void writeObject(ObjectOutputStream s) {
        try {
            s.defaultWriteObject();
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
}
