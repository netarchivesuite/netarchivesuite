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

import dk.netarkivet.common.exceptions.ArgumentNotValid;

/**
 * Enumeration of the possible replica types used for replicas.
 */
public enum ReplicaType {
    /** 
     * If no replica type has been set
     **/    
    NO_REPLICA_TYPE,
     /** 
     * bitarchive replica which contain files stored in repository
     **/    
    BITARCHIVE,
    /** 
     * Checksum replica which contains checksums of files in repository 
     **/
    CHECKSUM;

    /**
     * Helper method that gives a proper object from e.g. settings.
     *
     * @param rt a certain integer for a replica type
     * @return the ReplicaType related to a certain integer
     * @throws ArgumentNotValid If argument tu is invalid
     * (i.e. does not correspond to a TimeUnit)
     */
    public static ReplicaType fromOrdinal(int rt) {
        ArgumentNotValid.checkNotNull(rt, "rt");
        switch (rt) {
            case 0: return NO_REPLICA_TYPE;
            case 1: return BITARCHIVE;
            case 2: return CHECKSUM;
            default: throw new ArgumentNotValid("Invalid replica " + rt);
        }
    }
   
    /**
    * Helper method that gives a proper object from e.g. settings.
    *
    * @param s A string representing a ReplicaType.
    * @return the ReplicaType related to a certain string
    * @throws ArgumentNotValid If argument s is null
    */
    public static ReplicaType fromSetting(String s) {
        ArgumentNotValid.checkNotNull(s, "s");
        String t = s.toLowerCase();    
        if (t.equals("bitarchive")) {
            return BITARCHIVE;
        } else {
            if (t.equals("checksum")) {
                return CHECKSUM;
            } else {
                return NO_REPLICA_TYPE;
            }
        } 
    } 
}
