/*
 * #%L
 * Netarchivesuite - archive - test
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
package dk.netarkivet.archive.arcrepository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;

import dk.netarkivet.common.distribute.arcrepository.ReplicaType;
import dk.netarkivet.common.exceptions.ArgumentNotValid;

/** Tests of the ReplicaType enum class. */

public class ReplicaTypeTester {

    @Test
    public void testFromOrdinal() {
        assertEquals(ReplicaType.NO_REPLICA_TYPE, ReplicaType.fromOrdinal(0));
        assertEquals(ReplicaType.BITARCHIVE, ReplicaType.fromOrdinal(1));
        assertEquals(ReplicaType.CHECKSUM, ReplicaType.fromOrdinal(2));
        try {
            ReplicaType.fromOrdinal(3);
            fail("Should throw ArgumentNotValid. Has ReplicaType been changed");
        } catch (ArgumentNotValid e) {
            // Expected
        }
    }

    @Test
    public void testFromSetting() {
        try {
            ReplicaType.fromSetting(null);
        } catch (ArgumentNotValid e) {
            // Expected
        }

        assertEquals(ReplicaType.BITARCHIVE, ReplicaType.fromSetting(ReplicaType.BITARCHIVE_REPLICATYPE_AS_STRING));
        assertEquals(ReplicaType.CHECKSUM, ReplicaType.fromSetting(ReplicaType.CHECKSUM_REPLICATYPE_AS_STRING));
        assertEquals(ReplicaType.NO_REPLICA_TYPE, ReplicaType.fromSetting(""));
        assertEquals(ReplicaType.NO_REPLICA_TYPE, ReplicaType.fromSetting("not yet introduced type"));

    }
}
