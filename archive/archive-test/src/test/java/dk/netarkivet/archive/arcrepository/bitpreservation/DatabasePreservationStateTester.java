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
import static org.junit.Assert.fail;

import java.sql.Date;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.junit.Test;

import dk.netarkivet.archive.arcrepositoryadmin.ChecksumStatus;
import dk.netarkivet.archive.arcrepositoryadmin.FileListStatus;
import dk.netarkivet.archive.arcrepositoryadmin.ReplicaFileInfo;
import dk.netarkivet.common.distribute.arcrepository.Replica;
import dk.netarkivet.common.distribute.arcrepository.ReplicaStoreState;
import dk.netarkivet.common.distribute.arcrepository.ReplicaType;
import dk.netarkivet.common.exceptions.IllegalState;

public class DatabasePreservationStateTester {

    @Test
    public void testState() {

        Replica r1 = Replica.getReplicaFromId("ONE");
        Replica r2 = Replica.getReplicaFromId("TWO");
        Replica r3 = Replica.getReplicaFromId("THREE");

        long guid = 0L;
        long fileId = 1L;
        long segmentId = 0L;
        String cs = "checksum";
        Date now = new Date(Calendar.getInstance().getTimeInMillis());

        List<ReplicaFileInfo> rfis = new ArrayList<ReplicaFileInfo>();
        rfis.add(new ReplicaFileInfo(guid, "ONE", fileId, segmentId, cs, ReplicaStoreState.UPLOAD_COMPLETED.ordinal(),
                FileListStatus.OK.ordinal(), ChecksumStatus.OK.ordinal(), now, now));
        rfis.add(new ReplicaFileInfo(guid, "TWO", fileId, segmentId, cs, ReplicaStoreState.UPLOAD_COMPLETED.ordinal(),
                FileListStatus.OK.ordinal(), ChecksumStatus.OK.ordinal(), now, now));
        rfis.add(new ReplicaFileInfo(guid, "THREE", fileId, segmentId, "muskcehc", ReplicaStoreState.UPLOAD_FAILED
                .ordinal(), FileListStatus.MISSING.ordinal(), ChecksumStatus.CORRUPT.ordinal(), now, now));

        DatabasePreservationState dps = new DatabasePreservationState("filename", rfis);

        // check function getBitarchiveChecksum
        assertEquals("The checksum for replica ONE should be 'checksum'", "checksum", dps.getReplicaChecksum(r1).get(0));
        assertEquals("The checksum for replica TWO should be 'checksum'", "checksum", dps.getReplicaChecksum(r2).get(0));
        assertEquals("Replica THREE is missing the file, and thus no checksum", 0, dps.getReplicaChecksum(r3).size());

        // check function getAdminChecksum
        assertEquals("No admin data for database preservation state, thus no checksum", "NO ADMIN CHECKSUM!",
                dps.getAdminChecksum());

        // check function getAdminBitarchiveState
        assertEquals("Replica ONE should have state UPLOAD_COMPLETE", ReplicaStoreState.UPLOAD_COMPLETED.toString(),
                dps.getAdminReplicaState(r1));
        assertEquals("Replica TWO should have state UPLOAD_COMPLETE", ReplicaStoreState.UPLOAD_COMPLETED.toString(),
                dps.getAdminReplicaState(r2));
        assertEquals("Replica THREE should have state UPLOAD_FAILED", ReplicaStoreState.UPLOAD_FAILED.toString(),
                dps.getAdminReplicaState(r3));

        // check function isAdminDataOk
        assertTrue("The admin data should be ok, since it does not exist.", dps.isAdminDataOk());

        // check function getReferenceBitarchive
        Replica refRep = dps.getReferenceBitarchive();
        assertNotNull("The reference replica should not be null", refRep);
        assertEquals("The reference replica should be a bitarchive", ReplicaType.BITARCHIVE, refRep.getType());

        // check function getUniqueChecksum
        assertEquals("The checksum for replica ONE should be 'checksum'", "checksum", dps.getUniqueChecksum(r1));
        assertEquals("The checksum for replica TWO should be 'checksum'", "checksum", dps.getUniqueChecksum(r2));
        assertTrue("Replica THREE is missing the file, and thus empty checksum", dps.getUniqueChecksum(r3).isEmpty());

        // check function fileIsMissing
        assertFalse("Replica ONE should not be missing the file", dps.fileIsMissing(r1));
        assertTrue("Replica THREE should be missing the file", dps.fileIsMissing(r3));

        // check function getReferenceChecksum
        String refChecksum = dps.getReferenceCheckSum();
        assertEquals("The reference checksum should be 'checksum'", "checksum", refChecksum);
        assertEquals("Replica ONE should have the reference checksum.", refChecksum, dps.getUniqueChecksum(r1));
        assertEquals("Replica TWO should have the reference checksum.", refChecksum, dps.getUniqueChecksum(r2));
        assertNotSame("Replica THREE should not have the reference checksum", refChecksum, dps.getUniqueChecksum(r3));

        // check function isAdminChecksumOk
        assertTrue("The admin data should be OK", dps.isAdminCheckSumOk());

        // check function getFilename
        String filename = dps.getFilename();
        assertEquals("The filename should be 'filename'", "filename", filename);

        // check function toString
        String content = dps.toString();
        assertTrue("Should have the filename", content.contains(filename));
        assertTrue("Should have the id of replica ONE", content.contains(r1.getId()));
        assertTrue("Should have the id of replica TWO", content.contains(r2.getId()));
        assertTrue("Should have the id of replica THREE", content.contains(r3.getId()));

    }

    @Test
    public void testError() {
        long guid = 0L;
        long fileId = 1L;
        long segmentId = 0L;
        String cs = "checksum";
        Date now = new Date(Calendar.getInstance().getTimeInMillis());

        List<ReplicaFileInfo> rfis = new ArrayList<ReplicaFileInfo>();
        rfis.add(new ReplicaFileInfo(guid, "ONE", fileId, segmentId, cs, ReplicaStoreState.UPLOAD_FAILED.ordinal(),
                FileListStatus.MISSING.ordinal(), ChecksumStatus.CORRUPT.ordinal(), now, now));
        rfis.add(new ReplicaFileInfo(guid, "TWO", fileId, segmentId, "wrongChecksum", ReplicaStoreState.UPLOAD_FAILED
                .ordinal(), FileListStatus.MISSING.ordinal(), ChecksumStatus.CORRUPT.ordinal(), now, now));
        rfis.add(new ReplicaFileInfo(guid, "THREE", fileId, segmentId, "muskcehc", ReplicaStoreState.UPLOAD_COMPLETED
                .ordinal(), FileListStatus.OK.ordinal(), ChecksumStatus.OK.ordinal(), now, now));

        DatabasePreservationState dps = new DatabasePreservationState("filename", rfis);

        // test error in getReferenceBitarchive
        assertNull("Only checksum replica is ok, thus not reference bitarchive.", dps.getReferenceBitarchive());

        // getReferenceChecksum
        try {
            dps.getReferenceCheckSum();
            fail("No reference checksum should be found.");
        } catch (IllegalState e) {
            // expected.
        }
    }
}
