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

package dk.netarkivet.archive.arcrepository.bitpreservation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import dk.netarkivet.archive.arcrepositoryadmin.ArcRepositoryEntry;
import dk.netarkivet.common.distribute.arcrepository.Replica;
import dk.netarkivet.common.distribute.arcrepository.ReplicaStoreState;
import dk.netarkivet.common.distribute.arcrepository.ReplicaType;
import dk.netarkivet.testutils.ReflectUtils;

public class FilePreservationStateTester {

    /**
     * Tests the standard way of using the FilePreservationState.
     *
     * @throws NoSuchMethodException
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    @Test
    public void testState() throws NoSuchMethodException, IllegalArgumentException, IllegalAccessException,
            InvocationTargetException {
        Replica r1 = Replica.getReplicaFromId("ONE");
        Replica r2 = Replica.getReplicaFromId("TWO");
        Replica r3 = Replica.getReplicaFromId("THREE");

        ArcRepositoryEntry are = new ArcRepositoryEntry("filename", "checksum", null);

        Method setState = ReflectUtils.getPrivateMethod(ArcRepositoryEntry.class, "setStoreState", String.class,
                ReplicaStoreState.class);
        setState.invoke(are, r1.getIdentificationChannel().getName(), ReplicaStoreState.UPLOAD_COMPLETED);
        setState.invoke(are, r2.getIdentificationChannel().getName(), ReplicaStoreState.UPLOAD_COMPLETED);
        setState.invoke(are, r3.getIdentificationChannel().getName(), ReplicaStoreState.UPLOAD_FAILED);

        Map<Replica, List<String>> checksumMap = new HashMap<Replica, List<String>>();

        List<String> c1 = new ArrayList<String>();
        c1.add("checksum");
        checksumMap.put(r1, c1);

        List<String> c2 = new ArrayList<String>();
        c2.add("checksum");
        checksumMap.put(r2, c2);

        List<String> c3 = new ArrayList<String>();
        c3.add("muskcehc");
        checksumMap.put(r3, c3);

        // only contains replica 1 and 3.
        FilePreservationState fps = new FilePreservationState("filename", are, checksumMap);

        // check function getBitarchiveChecksum
        assertEquals("Replica ONE should contain 1 checksum value", "[checksum]", fps.getReplicaChecksum(r1).toString());
        assertEquals("Replica TWO should contain 1 checksum value", "[checksum]", fps.getReplicaChecksum(r2).toString());
        assertEquals("Replica THREE should contain 1 checksum value", "[muskcehc]", fps.getReplicaChecksum(r3)
                .toString());

        // check function getAdminDataChecksum
        assertEquals("The admin data should have the common checksum", "checksum", fps.getAdminChecksum());

        // check function getAdminBitarchiveState
        assertEquals("Replica ONE should have UPLOAD_COMPLETED", ReplicaStoreState.UPLOAD_COMPLETED.toString(),
                fps.getAdminReplicaState(r1));
        assertEquals("Replica TWO should have UPLOAD_COMPLETED", ReplicaStoreState.UPLOAD_COMPLETED.toString(),
                fps.getAdminReplicaState(r2));
        assertEquals("Replica THREE should have UPLOAD_FAILED", ReplicaStoreState.UPLOAD_FAILED.toString(),
                fps.getAdminReplicaState(r3));

        // check function isAdminDataOk
        assertFalse("Admin data should not be ok", fps.isAdminDataOk());

        // check function fileIsMissing
        assertFalse("Replica ONE should not be missing the file", fps.fileIsMissing(r1));
        assertFalse("Replica TWO should not be missing the file", fps.fileIsMissing(r2));
        assertFalse("Replica THREE should not be missing the file", fps.fileIsMissing(r3));

        // check function getReferenceBitarchive
        Replica refRep = fps.getReferenceBitarchive();
        assertNotNull("A reference replica should be found", refRep);
        assertEquals("The reference replica should be a bitarchive replica", ReplicaType.BITARCHIVE, refRep.getType());

        // check function getUniqueChecksum
        assertEquals("Replica ONE should have unique checksum 'checksum'", "checksum", fps.getUniqueChecksum(r1));
        assertEquals("Replica TWO should have unique checksum 'checksum'", "checksum", fps.getUniqueChecksum(r2));
        assertEquals("Replica THREE should have unique checksum 'muskcehc'", "muskcehc", fps.getUniqueChecksum(r3));

        // check function getReferenceChecksum
        String refChecksum = fps.getReferenceCheckSum();
        assertEquals("The reference checksum should be 'checksum'", "checksum", refChecksum);
        assertEquals("Replica ONE should have the reference checksum.", refChecksum, fps.getUniqueChecksum(r1));
        assertEquals("Replica TWO should have the reference checksum.", refChecksum, fps.getUniqueChecksum(r2));
        assertNotSame("Replica THREE should not have the reference checksum", refChecksum, fps.getUniqueChecksum(r3));

        // check function isAdminCheckSumOk
        assertTrue("The admin checksum should be ok.", fps.isAdminCheckSumOk());

        // check function getFilename
        assertEquals("It should be the file 'filename'", "filename", fps.getFilename());

        // check function toString
        String content = fps.toString();
        assertTrue("It should contain the filename",
                content.contains("PreservationStatus for '" + fps.getFilename() + "'"));
        assertTrue("It should contain the General store state", content.contains("General store state: "));
    }

    @Test
    public void testError() throws NoSuchMethodException, IllegalArgumentException, IllegalAccessException,
            InvocationTargetException {
        Replica r1 = Replica.getReplicaFromId("ONE");
        Replica r2 = Replica.getReplicaFromId("TWO");
        Replica r3 = Replica.getReplicaFromId("THREE");

        ArcRepositoryEntry are = new ArcRepositoryEntry("filename", "checksum", null);

        Method setState = ReflectUtils.getPrivateMethod(ArcRepositoryEntry.class, "setStoreState", String.class,
                ReplicaStoreState.class);
        setState.invoke(are, r1.getIdentificationChannel().getName(), ReplicaStoreState.UPLOAD_FAILED);
        setState.invoke(are, r3.getIdentificationChannel().getName(), ReplicaStoreState.UPLOAD_COMPLETED);

        Map<Replica, List<String>> checksumMap = new HashMap<Replica, List<String>>();

        List<String> c1 = new ArrayList<String>();
        c1.add("checksum");
        checksumMap.put(r1, c1);

        List<String> c3 = new ArrayList<String>();
        c3.add("muskcehc");
        checksumMap.put(r3, c3);

        // only contains replica 1 and 3.
        FilePreservationState fps = new FilePreservationState("filename", are, checksumMap);

        // getBitarchiveChecksum
        assertEquals("Replica TWO should not have any checksums, thus expected empty list.", Collections.emptyList(),
                fps.getReplicaChecksum(r2));

        // getAdminBitarchiveState
        assertEquals("Replica TWO should not have a state", "No state", fps.getAdminReplicaState(r2));

        // isAdminDataOk
        assertFalse("AdminData should not be ok", fps.isAdminDataOk());

        // getReferenceBitarchive
        assertNull("No reference bitarchive should be found", fps.getReferenceBitarchive());

        // getReferenceCheckSum
        assertTrue("The reference checksum should be emtpy", fps.getReferenceCheckSum().isEmpty());
    }

    // check isAdminDataOk again!
    @Test
    public void testIsAdminDataOk() throws NoSuchMethodException, IllegalArgumentException, IllegalAccessException,
            InvocationTargetException {
        // scenario 1. empty checksum and admin-state completed.
        Replica r1 = Replica.getReplicaFromId("ONE");
        Replica r2 = Replica.getReplicaFromId("TWO");
        Replica r3 = Replica.getReplicaFromId("THREE");

        ArcRepositoryEntry are = new ArcRepositoryEntry("filename", "checksum", null);

        Method setState = ReflectUtils.getPrivateMethod(ArcRepositoryEntry.class, "setStoreState", String.class,
                ReplicaStoreState.class);
        setState.invoke(are, r1.getIdentificationChannel().getName(), ReplicaStoreState.UPLOAD_FAILED);
        setState.invoke(are, r3.getIdentificationChannel().getName(), ReplicaStoreState.UPLOAD_COMPLETED);

        Map<Replica, List<String>> checksumMap = new HashMap<Replica, List<String>>();

        List<String> c1 = new ArrayList<String>();
        checksumMap.put(r1, c1);

        FilePreservationState fps = new FilePreservationState("filename", are, checksumMap);

        assertFalse("Scenario 1: empty checksum and admin-state completed", fps.isAdminDataOk());

        // scenario 2. checksum but admin-state not completed.
        are = new ArcRepositoryEntry("filename", "checksum", null);
        setState.invoke(are, r1.getIdentificationChannel().getName(), ReplicaStoreState.UPLOAD_FAILED);
        checksumMap = new HashMap<Replica, List<String>>();
        c1 = new ArrayList<String>();
        c1.add("checksum");
        checksumMap.put(r1, c1);
        fps = new FilePreservationState("filename", are, checksumMap);

        assertFalse("Scenario 2: checksum but admin-state not completed", fps.isAdminDataOk());

        // scenario 3. empty string as admin checksum.
        are = new ArcRepositoryEntry("filename", "", null);
        setState.invoke(are, r1.getIdentificationChannel().getName(), ReplicaStoreState.UPLOAD_COMPLETED);
        checksumMap = new HashMap<Replica, List<String>>();
        c1 = new ArrayList<String>();
        c1.add("checksum");
        checksumMap.put(r1, c1);
        fps = new FilePreservationState("filename", are, checksumMap);

        assertFalse("Scenario 3: empty string as admin checksum", fps.isAdminDataOk());

        // scenario 4. admin data OK
        are = new ArcRepositoryEntry("filename", "checksum", null);
        setState.invoke(are, r1.getIdentificationChannel().getName(), ReplicaStoreState.UPLOAD_COMPLETED);
        setState.invoke(are, r2.getIdentificationChannel().getName(), ReplicaStoreState.UPLOAD_COMPLETED);
        setState.invoke(are, r3.getIdentificationChannel().getName(), ReplicaStoreState.UPLOAD_COMPLETED);
        checksumMap = new HashMap<Replica, List<String>>();
        c1 = new ArrayList<String>();
        c1.add("checksum");
        checksumMap.put(r1, c1);
        List<String> c2 = new ArrayList<String>();
        c2.add("checksum");
        checksumMap.put(r2, c2);
        List<String> c3 = new ArrayList<String>();
        c3.add("checksum");
        checksumMap.put(r3, c3);
        fps = new FilePreservationState("filename", are, checksumMap);

        assertTrue("Scenario 4: Admin data OK", fps.isAdminDataOk());

    }

    // check getReferenceBitarchive again!
    @Test
    public void testGetReferenceBitarchive() throws NoSuchMethodException, IllegalArgumentException,
            IllegalAccessException, InvocationTargetException, NoSuchFieldException {
        // scenario 1: no reference checksum.
        Replica r1 = Replica.getReplicaFromId("ONE");
        Replica r2 = Replica.getReplicaFromId("TWO");
        Replica r3 = Replica.getReplicaFromId("THREE");

        ArcRepositoryEntry are = new ArcRepositoryEntry("filename", "checksum", null);

        Method setState = ReflectUtils.getPrivateMethod(ArcRepositoryEntry.class, "setStoreState", String.class,
                ReplicaStoreState.class);
        setState.invoke(are, r1.getIdentificationChannel().getName(), ReplicaStoreState.UPLOAD_FAILED);
        setState.invoke(are, r3.getIdentificationChannel().getName(), ReplicaStoreState.UPLOAD_COMPLETED);

        Map<Replica, List<String>> checksumMap = new HashMap<Replica, List<String>>();

        List<String> c1 = new ArrayList<String>();
        checksumMap.put(r1, c1);

        FilePreservationState fps = new FilePreservationState("filename", are, checksumMap);

        assertNull("Scenario 1: No reference checksum", fps.getReferenceBitarchive());
        assertEquals("Scenario 1: No reference checksum", "", fps.getReferenceCheckSum());

        // scenario 2. No bitarchive with reference checksum
        are = new ArcRepositoryEntry("filename", "checksum", null);
        setState.invoke(are, r1.getIdentificationChannel().getName(), ReplicaStoreState.UPLOAD_FAILED);
        setState.invoke(are, r2.getIdentificationChannel().getName(), ReplicaStoreState.UPLOAD_COMPLETED);
        setState.invoke(are, r3.getIdentificationChannel().getName(), ReplicaStoreState.UPLOAD_COMPLETED);

        checksumMap = new HashMap<Replica, List<String>>();
        List<String> c2 = new ArrayList<String>();
        c2.add("checksum");
        checksumMap.put(r2, c2);
        List<String> c3 = new ArrayList<String>();
        c3.add("checksum");
        checksumMap.put(r3, c3);
        fps = new FilePreservationState("filename", are, checksumMap);

        // change the replica type of replica 2 to checksum replica.
        Field repType = ReflectUtils.getPrivateField(Replica.class, "type");
        repType.set(r2, ReplicaType.CHECKSUM);

        assertNull("Scenario 2: No bitarchive with reference checksum", fps.getReferenceBitarchive());
        assertEquals("Scenario 2: The should be a reference checksum", "checksum", fps.getReferenceCheckSum());

        // reinitialize replicas
        Field reps = ReflectUtils.getPrivateField(Replica.class, "knownReplicas");
        reps.set(r1, null);
        Method reinitRep = ReflectUtils.getPrivateMethod(Replica.class, "initializeKnownReplicasList", new Class[] {});
        reinitRep.invoke(r1, new Object[] {});

        assertEquals(Replica.getReplicaFromId("TWO").getType(), ReplicaType.BITARCHIVE);
    }

    // test getUniqueChecksum again!
    @Test
    public void testGetUniqueChecksum() throws NoSuchMethodException, IllegalArgumentException, IllegalAccessException,
            InvocationTargetException {
        // scenario 1: two differen checksums for replica
        Replica r1 = Replica.getReplicaFromId("ONE");

        ArcRepositoryEntry are = new ArcRepositoryEntry("filename", "checksum", null);

        Method setState = ReflectUtils.getPrivateMethod(ArcRepositoryEntry.class, "setStoreState", String.class,
                ReplicaStoreState.class);
        setState.invoke(are, r1.getIdentificationChannel().getName(), ReplicaStoreState.UPLOAD_COMPLETED);

        Map<Replica, List<String>> checksumMap = new HashMap<Replica, List<String>>();

        List<String> c1 = new ArrayList<String>();
        c1.add("checksum");
        c1.add("muskcehc");
        checksumMap.put(r1, c1);

        FilePreservationState fps = new FilePreservationState("filename", are, checksumMap);

        assertEquals("Scenario 1: two checksums for replica", "", fps.getUniqueChecksum(r1));

        // Scenario 2: no checksum for replica.
        are = new ArcRepositoryEntry("filename", "checksum", null);
        setState.invoke(are, r1.getIdentificationChannel().getName(), ReplicaStoreState.UPLOAD_COMPLETED);
        checksumMap = new HashMap<Replica, List<String>>();
        c1 = new ArrayList<String>();
        checksumMap.put(r1, c1);
        fps = new FilePreservationState("filename", are, checksumMap);

        assertEquals("Scenario 2: no checksum for replica", "", fps.getUniqueChecksum(r1));

    }
}
