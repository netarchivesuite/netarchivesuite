/*
 * #%L
 * Netarchivesuite - common
 * %%
 * Copyright (C) 2005 - 2018 The Royal Danish Library, 
 *             the National Library of France and the Austrian National Library.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 2.1 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */
package dk.netarkivet.common.distribute.arcrepository;

import dk.netarkivet.common.exceptions.ArgumentNotValid;

/**
 * Enumeration of the possible replica types used for replicas.
 */
public enum ReplicaType {

    /** If no replica type has been set. */
    NO_REPLICA_TYPE,
    /** bitarchive replica which contain files stored in repository. */
    BITARCHIVE,
    /** Checksum replica which contains checksums of files in repository. */
    CHECKSUM;

    /** String representation of the ReplicaType.BITARCHIVE. */
    public static final String BITARCHIVE_REPLICATYPE_AS_STRING = "bitarchive";

    /** String representation of the ReplicaType.CHECKSUM. */
    public static final String CHECKSUM_REPLICATYPE_AS_STRING = "checksum";

    /**
     * Helper method that gives a proper object from e.g. settings.
     *
     * @param ordinal a certain integer for a replica type
     * @return the ReplicaType related to a certain integer
     * @throws ArgumentNotValid If argument rt does not correspond to a ReplicaType
     */
    public static ReplicaType fromOrdinal(int ordinal) {
        switch (ordinal) {
        case 0:
            return NO_REPLICA_TYPE;
        case 1:
            return BITARCHIVE;
        case 2:
            return CHECKSUM;
        default:
            throw new ArgumentNotValid("Invalid replica type with number " + ordinal);
        }
    }

    /**
     * Helper method that gives a proper object from e.g. settings.
     *
     * @param s A string representing a ReplicaType.
     * @return the ReplicaType related to a certain string; if the string does not correspond to a known replicatype, it
     * returns NO_REPLICA_TYPE
     * @throws ArgumentNotValid If argument s is null
     */
    public static ReplicaType fromSetting(String s) {
        ArgumentNotValid.checkNotNull(s, "s");
        String t = s.toLowerCase();
        if (t.equals(BITARCHIVE_REPLICATYPE_AS_STRING)) {
            return BITARCHIVE;
        } else {
            if (t.equals(CHECKSUM_REPLICATYPE_AS_STRING)) {
                return CHECKSUM;
            } else {
                return NO_REPLICA_TYPE;
            }
        }
    }

}
