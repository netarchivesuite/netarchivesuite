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
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.common.Settings;
import dk.netarkivet.common.distribute.ChannelID;
import dk.netarkivet.common.distribute.Channels;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.UnknownID;

/**
 * This class encapsulates the bitarchive locations.  It guarantees that
 * there is only one Location object per location name.
 */
public class Location {
    /** the class log. */
    private static Log log = LogFactory.getLog(Location.class.getName());
    /** The name of this location. */
    private final String name;
    /** List of the locations we know of. */
    private static Map<String, Location> known;

    /** Private constructor that makes a new Location object.  These will
     * all be stored in the known map.
     *
     * @param name Name of the location (e.g. SB)
     */
    private Location(String name) {
        this.name = name;
    }

    /** Initialize the list of known locations from settings.
     * This must be called before using known, but after settings are loaded.
     */
    private static void initializeKnownList() {
        if (known == null) {
            known = new HashMap<String, Location>(2);
            for (String location
                    : Settings.getAll(Settings.ENVIRONMENT_LOCATION_NAMES)) {
                known.put(location, new Location(location));
            }
        }
    }

    /** Get an object representing the location with the given name.
     *
     * @param name The given name of an location
     * @return an object representing the location with the given name
     */
    public static Location get(String name) {
        initializeKnownList();
        if (name == null || !known.containsKey(name)) {
            String message = "Can't find bitarchive '" + name
                    + "', only know of " + known.keySet();
            log.debug(message);
            throw new UnknownID(message);
        }
        return known.get(name);
    }

    /** Check, if a given name is a location name.
     * @param name a given name
     * @return true, if the given name is a location name, false otherwise
     */
    public static boolean isKnownLocation(String name) {
        ArgumentNotValid.checkNotNullOrEmpty(name, "String name");
        initializeKnownList();
        return known.containsKey(name);
    }
    

    /** 
     * Get all known locations.
     * @return A unmodifiable view of the currently known locations.
     */
    public static Collection<Location> getKnown() {
        initializeKnownList();
        return Collections.unmodifiableCollection(known.values());
    }
    
    /**
     * Get all known locations as names.
     * @return all known locations as names
     */
    public static String[] getKnownNames() {
        initializeKnownList();
        String[] knownNames = new String[known.keySet().size()];
        int index = 0;
        for (String s : known.keySet()) {
            knownNames[index] = s;
            index++;
        }
        return knownNames;
    }

    /** Get the name of an location.
     *
     * @return The name that this location is known as in queues etc.
     */
    public String getName() {
        return name;
    }

    /** Get the BaMon channel id that corresponds to this location.
     *
     * @return The BaMon ChannelID of this location.
     * Please do not parse its name!
     */
    public ChannelID getChannelID() {
        return Channels.getBaMonForLocation(name);
    }

    /** Returns a human-readable representation of the object.
     *
     * @return An arbitrary string version of the object.  Do not depend on
     * its format.
     */
    public String toString() {
        return "Location " + name;
    }
}
