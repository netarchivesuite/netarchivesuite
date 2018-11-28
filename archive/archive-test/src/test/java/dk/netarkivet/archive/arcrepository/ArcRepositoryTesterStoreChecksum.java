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

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import dk.netarkivet.archive.arcrepository.distribute.StoreMessage;
import dk.netarkivet.archive.arcrepositoryadmin.AdminData;
import dk.netarkivet.archive.arcrepositoryadmin.UpdateableAdminData;
import dk.netarkivet.archive.bitarchive.distribute.BitarchiveClient;
import dk.netarkivet.common.distribute.Channels;
import dk.netarkivet.common.distribute.RemoteFile;
import dk.netarkivet.common.distribute.TestRemoteFile;
import dk.netarkivet.common.distribute.arcrepository.ReplicaStoreState;
import dk.netarkivet.common.exceptions.UnknownID;
import dk.netarkivet.common.utils.ChecksumCalculator;
import dk.netarkivet.testutils.preconfigured.ReloadSettings;
import dk.netarkivet.testutils.preconfigured.UseTestRemoteFile;

/**
 * Unit tests for the ArcRepository class.
 */
@SuppressWarnings({"deprecation", "unchecked"})
public class ArcRepositoryTesterStoreChecksum {

    private UseTestRemoteFile rf = new UseTestRemoteFile();

    private static final File TEST_DIR = new File("tests/dk/netarkivet/archive/arcrepository/data/store");

    /**
     * The directory from where we upload the ARC files.
     */
    private static final File ORIGINALS_DIR = new File(TEST_DIR, "originals");
    /**
     * The files that are uploaded during the tests and that must be removed afterwards.
     */
    private static final String[] STORABLE_FILES = new String[] {"NetarchiveSuite-store1.arc",
            "NetarchiveSuite-store2.arc"};

    ArcRepository arcRepos;

    ReloadSettings rs = new ReloadSettings();

    public ArcRepositoryTesterStoreChecksum() {
    }

    @Before
    public void setUp() {
        rs.setUp();
        Channels.reset();
        ServerSetUp.setUp();
        arcRepos = ServerSetUp.getArcRepository();
        rf.setUp();
    }

    @After
    public void tearDown() {
        rf.tearDown();
        ServerSetUp.tearDown();
        rs.tearDown();
    }

    /**
     * Tests if the store operation generates and stores a valid checksum in the reference table (AdminData).
     */
    @Test
    @Ignore("Upload of 'NetarchiveSuite-store1.arc' timed out on Ubuntu")
    public void testStoreCompletedChecksum() throws IOException {
        File file = new File(ORIGINALS_DIR, STORABLE_FILES[0]);
        String orgCheckSum = ChecksumCalculator.calculateMd5(file);

        UpdateableAdminData adminData = AdminData.getUpdateableInstance();
        adminData.addEntry(file.getName(), new StoreMessage(Channels.getThisReposClient(), file),
                ChecksumCalculator.calculateMd5(file));
        adminData.setState(file.getName(), Channels.retrieveReplicaChannelFromReplicaId("TWO").getName(),
                ReplicaStoreState.UPLOAD_COMPLETED);
        adminData.setState(file.getName(), Channels.retrieveReplicaChannelFromReplicaId("THREE").getName(),
                ReplicaStoreState.UPLOAD_COMPLETED);

        StoreMessage msg = new StoreMessage(Channels.getError(), file);
        arcRepos.store(msg.getRemoteFile(), msg);
        UploadWaiting.waitForUpload(file, this);
        String refTableSum = UpdateableAdminData.getUpdateableInstance().getCheckSum(file.getName());
        assertEquals(refTableSum, orgCheckSum);
    }

    /**
     * Tests that Controller.getCheckSum() behaves as expected when using a reference to a non-stored file.
     */
    @Test
    public void testGetChecksumNotStoredFile() {
        File file = new File(ORIGINALS_DIR, STORABLE_FILES[0]);
        // do nothing with file - e.g. not storing it
        // thus checksum reference table should not contain an entry for
        // the file, i.e. getCheckSum() should return null:
        try {
            UpdateableAdminData.getUpdateableInstance().getCheckSum(file.getName());
            fail("Should throw UnknownID when getting non-existing checksum");
        } catch (UnknownID e) {
            // Expected
        }
    }

    /**
     * Tests if an attempt to store an already uploaded/stored file produces the expected behavior: a PermissionDenied
     * should be thrown, and the original entry in checksum reference table remains unaffected.
     */
    @Test
    @Ignore("Upload of 'NetarchiveSuite-store1.arc' timed out on Ubuntu")
    public void testStoreFailedAlreadyUploadedChecksum() {
        File file = null;
        String orgCheckSum = null;
        String storedCheckSum = null;
        try {
            file = new File(ORIGINALS_DIR, STORABLE_FILES[0]);
            orgCheckSum = ChecksumCalculator.calculateMd5(file);
            UpdateableAdminData adminData = AdminData.getUpdateableInstance();
            adminData.addEntry(file.getName(), new StoreMessage(Channels.getThisReposClient(), file),
                    ChecksumCalculator.calculateMd5(file));
            adminData.setState(file.getName(), Channels.retrieveReplicaChannelFromReplicaId("TWO").getName(),
                    ReplicaStoreState.UPLOAD_COMPLETED);
            adminData.setState(file.getName(), Channels.retrieveReplicaChannelFromReplicaId("THREE").getName(),
                    ReplicaStoreState.UPLOAD_COMPLETED);

            // JMSConnection con = JMSConnectionFactory.getInstance();
            StoreMessage msg = new StoreMessage(Channels.getError(), file);
            arcRepos.store(msg.getRemoteFile(), msg);
            UploadWaiting.waitForUpload(file, this);
            String refTableSum = UpdateableAdminData.getUpdateableInstance().getCheckSum(file.getName());
            assertEquals("Stored checksum and reference checksum should be equal", refTableSum, orgCheckSum);
            storedCheckSum = refTableSum;
            // attempting to upload/store the file again:
            msg = new StoreMessage(Channels.getError(), file);
            arcRepos.store(msg.getRemoteFile(), msg);
            UploadWaiting.waitForUpload(file, this);
            fail("Should throw an PermissionDenied here!");
        } catch (dk.netarkivet.common.exceptions.PermissionDenied e) {
            String refTableSum = UpdateableAdminData.getUpdateableInstance().getCheckSum(file.getName());
            // the checksum stored in reference table (during first store
            // operation) should be unaffected
            // by this second attempt to store the file:
            assertEquals("Stored checksum and reference checksum should be equal", refTableSum, storedCheckSum);
            // } catch (IOFailure e) {
            // e.printStackTrace();
            // fail("Unexpected IOException thrown "
            // + "while trying to re-upload file: " + e);
        }

    }

    /**
     * Check what happens if we're being sent a checksum while uploading. Test for bug #410.
     */
    @Test
    public void testStoreChecksumWhileUploading() throws NoSuchMethodException, IllegalAccessException,
            InvocationTargetException, NoSuchFieldException {

        final String correctChecksum = "correct checksum";
        final String ba1Name = "ba1";
        final String ba2Name = "ba2";

        UpdateableAdminData ad = UpdateableAdminData.getUpdateableInstance();

        String arcFileName = "store1a.ARC";
        // ArchiveStoreState generalState
        // = new ArchiveStoreState(BitArchiveStoreState.UPLOAD_STARTED);
        ad.addEntry(arcFileName, null, correctChecksum);
        ad.setState(arcFileName, ba1Name, ReplicaStoreState.UPLOAD_STARTED);
        ad.setState(arcFileName, ba2Name, ReplicaStoreState.DATA_UPLOADED);

        Method m = ArcRepository.class.getDeclaredMethod("processCheckSum", new Class[] {String.class, String.class,
                String.class, String.class, boolean.class});
        m.setAccessible(true);
        m.invoke(arcRepos, new Object[] {arcFileName, ba1Name, correctChecksum, correctChecksum, true});
        assertEquals("Should be in state STORE_COMPLETED after correct checksum", ReplicaStoreState.UPLOAD_COMPLETED,
                ad.getState(arcFileName, ba1Name));

        arcFileName = "store1b.ARC";

        ad.addEntry(arcFileName, null, correctChecksum);
        ad.setState(arcFileName, ba1Name, ReplicaStoreState.UPLOAD_STARTED);
        ad.setState(arcFileName, ba2Name, ReplicaStoreState.DATA_UPLOADED);
        m.invoke(arcRepos, new Object[] {arcFileName, ba1Name, correctChecksum, "wrong checksum", true});
        assertEquals("Should go into UPLOAD_FAILED without outstanding remotefile", ReplicaStoreState.UPLOAD_FAILED,
                ad.getState(arcFileName, ba1Name));

        arcFileName = "NetarchiveSuite-store2.arc";
        ad.addEntry(arcFileName, null, correctChecksum);
        ad.setState(arcFileName, ba1Name, ReplicaStoreState.UPLOAD_STARTED);
        ad.setState(arcFileName, ba2Name, ReplicaStoreState.DATA_UPLOADED);
        Field f = ArcRepository.class.getDeclaredField("outstandingRemoteFiles");
        f.setAccessible(true);
        Map<String, RemoteFile> outstandingRemoteFiles = (Map<String, RemoteFile>) f.get(arcRepos);
        f = ArcRepository.class.getDeclaredField("connectedReplicas");
        f.setAccessible(true);
        Map<String, BitarchiveClient> connectedBitarchives = (Map<String, BitarchiveClient>) f.get(arcRepos);
        connectedBitarchives.put(ba1Name,
                BitarchiveClient.getInstance(Channels.getAllBa(), Channels.getAnyBa(), Channels.getTheBamon()));
        // Have to use a real file here, as startUpload will grab the name
        outstandingRemoteFiles.put(arcFileName, new TestRemoteFile(new File(ORIGINALS_DIR, STORABLE_FILES[1]), false,
                false, false));
        m.invoke(arcRepos, new Object[] {arcFileName, ba1Name, correctChecksum, "wrong checksum", true});
        assertEquals("Wrong checksum should always result in upload failure", ReplicaStoreState.UPLOAD_FAILED,
                ad.getState(STORABLE_FILES[1], ba1Name));
    }
}
