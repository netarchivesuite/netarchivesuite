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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collection;

import org.junit.Test;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.distribute.Channels;
import dk.netarkivet.common.distribute.arcrepository.Replica;
import dk.netarkivet.common.distribute.arcrepository.ReplicaType;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.UnknownID;
import dk.netarkivet.common.utils.Settings;

/**
 * Tests of the Location class.
 */
public class ReplicaTester {
    private String[] knownTestIds;

    public ReplicaTester() {
        if (knownTestIds == null) {
            knownTestIds = Settings.getAll(CommonSettings.REPLICA_IDS);
        }
    }

    // initializeKnownList is tested in the test of get
    // getName() is tested in the test of get
    @Test
    public void testGet() {
        assertTrue("Differences in sizes of replica maps", knownTestIds.length == Replica.getKnown().size());
        for (int i = 0; i < knownTestIds.length; i++) {
            assertEquals("Replica id " + knownTestIds[i] + " not known in replica map.",
                    Replica.getReplicaFromId(knownTestIds[i]).getId(), knownTestIds[i]);
        }
        try {
            Replica.getReplicaFromId("XYZXYZ");
            fail("Should have thrown exception on UnknownID exception");
        } catch (UnknownID e) {
            // expected
        }
    }

    @Test
    public void testIsKnownReplica() {
        // Replica ids
        for (int i = 0; i < knownTestIds.length; i++) {
            assertTrue("Replica id " + knownTestIds[i] + " not known in Location map.",
                    Replica.isKnownReplicaId(knownTestIds[i]));
        }
        if (Replica.isKnownReplicaId("XYZXYZ")) {
            fail("Says it knows Replica id XYZXYZ");
        }
        try {
            String s = "";
            Replica.isKnownReplicaId(s);
        } catch (ArgumentNotValid e) {
            // expected
        }
        try {
            Replica.isKnownReplicaId(null);
        } catch (ArgumentNotValid e) {
            // expected
        }

        // Replica names
        String[] knownTestNames = Replica.getKnownNames();
        for (int i = 0; i < knownTestNames.length; i++) {
            assertTrue("Replica name " + knownTestNames[i] + " not known in Replica map.",
                    Replica.isKnownReplicaName(knownTestNames[i]));
        }
        if (Replica.isKnownReplicaName("XYZXYZ")) {
            fail("Says it knows Replica name XYZXYZ");
        }
        try {
            String s = "";
            Replica.isKnownReplicaName(s);
        } catch (ArgumentNotValid e) {
            // expected
        }
        try {
            Replica.isKnownReplicaName(null);
        } catch (ArgumentNotValid e) {
            // expected
        }
    }

    @Test
    public void testGetKnown() {
        // Collection<Location>
        int i;
        boolean found;
        Collection<Replica> col = Replica.getKnown();

        found = false;
        assertTrue("Differences in sizes of count of known names", col.size() == knownTestIds.length);

        for (i = 0; i < knownTestIds.length; i++) {
            for (Replica r : col) {
                if (knownTestIds[i].equals(r.getId())) {
                    found = true;
                }
                ;
                if (found) {
                    break;
                }
            }
            ;
            assertTrue("Location id " + knownTestIds[i] + " was not found in known replicas.", found);
        }

        found = false;
        for (Replica r : col) {
            for (i = 0; i < knownTestIds.length; i++) {
                if (knownTestIds[i].equals(r.getId())) {
                    found = true;
                }
                ;
                if (found) {
                    break;
                }
            }
            ;
            assertTrue("Replica id " + knownTestIds[i] + " was not found in test replicas.", found);
        }
    }

    @Test
    public void testGetKnownIds() {
        boolean found;
        int i;
        int j;
        String[] ki = Replica.getKnownIds();

        assertTrue("Differences in sizes of count of known ids", ki.length == knownTestIds.length);

        found = false;
        for (i = 0; i < knownTestIds.length; i++) {
            for (j = 0; j < ki.length; j++) {
                if (knownTestIds[i].equals(ki[j])) {
                    found = true;
                }
                ;
                if (found) {
                    break;
                }
            }
            ;
            assertTrue("Replica id " + knownTestIds[i] + " was not found in known ids for replica.", found);
        }
        ;

        found = false;
        for (i = 0; i < knownTestIds.length; i++) {
            for (j = 0; j < ki.length; j++) {
                if (knownTestIds[i].equals(ki[j])) {
                    found = true;
                }
                ;
                if (found) {
                    break;
                }
            }
            ;
            assertTrue("Replica id " + knownTestIds[i] + " was not found in known ids for test replicas.", found);
        }
        ;
    }

    @Test
    public void testGetChannelID() {
        for (Replica rep : Replica.getKnown()) {
            if (rep.getType() == ReplicaType.CHECKSUM) {
                assertEquals("The identification channel for '" + rep + "' a checksum channel.",
                        Channels.getTheCrForReplica(rep.getId()), rep.getIdentificationChannel());
            } else {
                assertEquals("If the replica type is not Checksum, then it must be Bitarchive", rep.getType(),
                        ReplicaType.BITARCHIVE);
                assertEquals("The identification channel for '" + rep + "' a bitarchive channel.",
                        Channels.getBaMonForReplica(rep.getId()), rep.getIdentificationChannel());
            }
        }
    }

    @Test
    public void testToString() {
        for (Replica replica : Replica.getKnown()) {
            assertTrue("The replica.toString() must contain the replica type for replica: " + replica, replica
                    .toString().contains(replica.getType().name()));
            assertTrue("The replica.toString() must contain the replica name for replica: " + replica, replica
                    .toString().contains(replica.getId()));
            assertTrue("The replica.toString() must contain the replica id for replica: " + replica, replica.toString()
                    .contains(replica.getName()));
        }
    }
}
