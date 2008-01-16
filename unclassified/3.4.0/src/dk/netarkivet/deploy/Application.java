/* $Id: ItConfigParser.java 27 2007-07-30 08:38:07Z lars $
 * $Revision: 27 $
 * $Date: 2007-07-30 10:38:07 +0200 (Mon, 30 Jul 2007) $
 * $Author: lars $
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

package dk.netarkivet.deploy;

import java.util.Map;
import java.util.HashMap;

/** A class that holds information about a single application in a location. */
public class Application {
    /** Enumeration of the allowable application types.  Currently only
     * indexserver is really handled properly.
     */
    enum Type {
        harvester, harvestdefinition, viewerproxy, indexserver, bitarchive,
        arcrepository, bitarchivemonitor
    };

    /** The application type that this application is an instance of.
     */
    private final Type type;

    /** A map of properties read from XML.
     */
    Map<String, String> properties = new HashMap<String, String>();

    /** Constructor for an application object.
     *
     * @param type Must be one of the application types allowed in the enum
     * above.
     */
    Application(String type) {
        this.type = Type.valueOf(type);
    }

    /** Add a property for this application.
     *
     * @param key Property key.  This can be assumed to be a legal XML
     * tag or attribute name
     * @param value Property value. This can be any string.
     */
    public void addProperty(String key, String value) {
        properties.put(key, value);
    }

    /** Get the type of this application.
     * @return Type of the application.  This value can be used with ==.
     */
    public Type getType() {
        return type;
    }
}
