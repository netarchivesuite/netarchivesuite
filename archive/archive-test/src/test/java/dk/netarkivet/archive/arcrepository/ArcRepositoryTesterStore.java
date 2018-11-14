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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import dk.netarkivet.archive.ArchiveSettings;
import dk.netarkivet.archive.arcrepository.distribute.StoreMessage;
import dk.netarkivet.archive.arcrepositoryadmin.AdminData;
import dk.netarkivet.archive.arcrepositoryadmin.ArcRepositoryEntry;
import dk.netarkivet.archive.arcrepositoryadmin.UpdateableAdminData;
import dk.netarkivet.archive.bitarchive.distribute.BatchReplyMessage;
import dk.netarkivet.archive.bitarchive.distribute.UploadMessage;
import dk.netarkivet.archive.checksum.distribute.GetChecksumMessage;
import dk.netarkivet.archive.distribute.ReplicaClient;
import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.distribute.Channels;
import dk.netarkivet.common.distribute.JMSConnectionMockupMQ;
import dk.netarkivet.common.distribute.NetarkivetMessage;
import dk.netarkivet.common.distribute.RemoteFileFactory;
import dk.netarkivet.common.distribute.arcrepository.Replica;
import dk.netarkivet.common.distribute.arcrepository.ReplicaStoreState;
import dk.netarkivet.common.exceptions.UnknownID;
import dk.netarkivet.common.utils.ChecksumCalculator;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.testutils.GenericMessageListener;
import dk.netarkivet.testutils.LogbackRecorder;
import dk.netarkivet.testutils.MessageAsserts;
import dk.netarkivet.testutils.preconfigured.ReloadSettings;
import dk.netarkivet.testutils.preconfigured.UseTestRemoteFile;

/** This class tests the store() method of ArcRepository. */
@SuppressWarnings({"deprecation", "unchecked"})
public class ArcRepositoryTesterStore {

    private UseTestRemoteFile rf = new UseTestRemoteFile();

    /** The directory from where we upload the ARC files. */
    private static final File ORIGINALS_DIR = new File(ServerSetUp.TEST_DIR, "originals");

    /** The file that is uploaded during the test. */
    private static final File STORABLE_FILE = new File(ORIGINALS_DIR, "NetarchiveSuite-store1.arc");

    private ArcRepository arcRepos;
    private static final File BATCH_RESULT = new File(ORIGINALS_DIR, "checksum");
    private static final File BATCH_RESULT_WRONG = new File(ORIGINALS_DIR, "checksumwrong");
    private static final File BATCH_RESULT_EMPTY = new File(ORIGINALS_DIR, "checksumempty");
    private static final File TEST_DIR = Settings.getFile(CommonSettings.DIR_COMMONTEMPDIR);

    public ArcRepositoryTesterStore() {
    }

    /** The directory used for controller admindata. */
    private static final File ADMINDATA_DIR = new File(TEST_DIR, "admindata");

    /** The bitarchive directory to work on. */
    static final File ARCHIVE_DIR = new File(TEST_DIR, "bitarchive");

    /** The directory used for storing temporary files. */
    private static final File TEMP_DIR = new File(TEST_DIR, "tempdir");

    ReloadSettings rs = new ReloadSettings();

    @Before
    public void setUp() throws IOException {
        rs.setUp();
        rf.setUp();
        Settings.set(ArchiveSettings.DIRS_ARCREPOSITORY_ADMIN, ADMINDATA_DIR.getAbsolutePath());
        Settings.set(ArchiveSettings.BITARCHIVE_SERVER_FILEDIR, ARCHIVE_DIR.getAbsolutePath());
        Settings.set(CommonSettings.DIR_COMMONTEMPDIR, TEMP_DIR.getAbsolutePath());

        JMSConnectionMockupMQ.useJMSConnectionMockupMQ();
        JMSConnectionMockupMQ.clearTestQueues();

        Channels.reset();

        FileUtils.removeRecursively(ARCHIVE_DIR);
        FileUtils.removeRecursively(TEMP_DIR);
        FileUtils.removeRecursively(ADMINDATA_DIR);
        FileUtils.createDir(ADMINDATA_DIR);
        FileUtils.createDir(TEMP_DIR);
        Channels.reset();
        // Create a bit archive server that listens to archive events
        arcRepos = ArcRepository.getInstance();
    }

    @After
    public void tearDown() {
        arcRepos.close();// close down ArcRepository Controller

        FileUtils.removeRecursively(ADMINDATA_DIR);
        FileUtils.removeRecursively(ARCHIVE_DIR);
        FileUtils.removeRecursively(TEMP_DIR);

        JMSConnectionMockupMQ.clearTestQueues();
        rf.tearDown();
        rs.tearDown();
    }

    /**
     * Tests whether the replica client can be retrieved for every replica. And confirms that the correct error is send,
     * if a wrong replica is tried to be retrieved.
     */
    @Test
    public void testReplicaClientRetrieval() {
        for (Replica rep : Replica.getKnown()) {
            ReplicaClient repClient = arcRepos.getReplicaClientFromReplicaId(rep.getId());

            assertNotNull("The replica client must not be null.", repClient);
            assertEquals("The replica and the replica client must of the same type.", rep.getType(),
                    repClient.getType());
        }

        String wrongReplicaId = "ERROR";
        try {
            arcRepos.getReplicaClientFromReplicaId(wrongReplicaId);
            fail("There should be no replica with ID '" + wrongReplicaId + "'.");
        } catch (UnknownID e) {
            assertTrue("The error message should have the right format.",
                    e.getMessage().contains("Can't find replica with id '" + wrongReplicaId + "'"));
        }
    }

    /**
     * Tests the scenario where a file has been stored, but the confirmation was lost, so that the harvester will want
     * to store the file again.
     * <p>
     * If the first store() was successful, the second one should be too (providing the file name and the MD5 is the
     * same).
     *
     * @throws InterruptedException
     */
    @Test
    public void testStoreFileAlreadyStored() throws InterruptedException, IOException {
        LogbackRecorder lr = LogbackRecorder.startRecorder();
        // Set listeners
        JMSConnectionMockupMQ con = (JMSConnectionMockupMQ) JMSConnectionMockupMQ.getInstance();
        GenericMessageListener gmlAnyBa = new GenericMessageListener();
        con.setListener(Channels.getAnyBa(), gmlAnyBa);
        GenericMessageListener gmlAllBa = new GenericMessageListener();
        con.setListener(Channels.getAllBa(), gmlAllBa);
        GenericMessageListener gmlOneBaMon = new GenericMessageListener();
        con.setListener(Channels.retrieveReplicaChannelFromReplicaId("ONE"), gmlOneBaMon);
        GenericMessageListener gmlHaco = new GenericMessageListener();
        con.setListener(Channels.getThisReposClient(), gmlHaco);

        // Set admin state
        UpdateableAdminData adminData = AdminData.getUpdateableInstance();
        adminData.addEntry(STORABLE_FILE.getName(), null, ChecksumCalculator.calculateMd5(STORABLE_FILE));
        adminData.setState(STORABLE_FILE.getName(), Channels.retrieveReplicaChannelNameFromReplicaId("ONE"),
                ReplicaStoreState.UPLOAD_COMPLETED);
        adminData.setState(STORABLE_FILE.getName(), Channels.retrieveReplicaChannelNameFromReplicaId("TWO"),
                ReplicaStoreState.UPLOAD_COMPLETED);
        adminData.setState(STORABLE_FILE.getName(), Channels.retrieveReplicaChannelNameFromReplicaId("THREE"),
                ReplicaStoreState.UPLOAD_COMPLETED);

        // Store
        StoreMessage msg = new StoreMessage(Channels.getThisReposClient(), STORABLE_FILE);
        JMSConnectionMockupMQ.updateMsgID(msg, "Msg-id-0");
        arcRepos.store(msg.getRemoteFile(), msg);
        con.waitForConcurrentTasksToFinish();

        // Check expected outcome: empty queues
        assertEquals("No messages should be received by the bamon, but found " + gmlOneBaMon.messagesReceived, 0,
                gmlOneBaMon.messagesReceived.size());
        assertEquals("No messages should be received by any ba, but found " + gmlAnyBa.messagesReceived, 0,
                gmlAnyBa.messagesReceived.size());
        assertEquals("No messages should be received by all ba, but found " + gmlAllBa.messagesReceived, 0,
                gmlAllBa.messagesReceived.size());

        // Expected message
        assertEquals("One message should be replied", 1, gmlHaco.messagesReceived.size());
        NetarkivetMessage replyMsg = gmlHaco.messagesReceived.get(0);
        assertTrue("The reply should be a store message but was " + replyMsg.getClass(),
                replyMsg instanceof StoreMessage);
        StoreMessage reply = (StoreMessage) replyMsg;
        assertTrue("The reply should be okay", reply.isOk());
        assertEquals("The reply should be a reply to the right message", "Msg-id-0", reply.getReplyOfId());

        // Check admin data
        ArcRepositoryEntry entry = adminData.getEntry(STORABLE_FILE.getName());
        assertEquals("Should have expected checksum", ChecksumCalculator.calculateMd5(STORABLE_FILE),
                entry.getChecksum());

        // Check log for message
        lr.assertLogContains("Should have the log message",
                "Retrying store of already known file '" + STORABLE_FILE.getName() + "'," + " Already completed: "
                        + true);
        lr.stopRecorder();
    }

    /**
     * Tests that we get a Not-OK message, if the file is known with other checksum. Also test that state is unchanged
     * afterwards
     */
    @Test
    public void testStoreOtherChecksum() {
        // Set listeners
        JMSConnectionMockupMQ con = (JMSConnectionMockupMQ) JMSConnectionMockupMQ.getInstance();
        GenericMessageListener gmlAnyBa = new GenericMessageListener();
        con.setListener(Channels.getAnyBa(), gmlAnyBa);
        GenericMessageListener gmlAllBa = new GenericMessageListener();
        con.setListener(Channels.getAllBa(), gmlAllBa);
        GenericMessageListener gmlOneBaMon = new GenericMessageListener();
        con.setListener(Channels.retrieveReplicaChannelFromReplicaId("ONE"), gmlOneBaMon);
        GenericMessageListener gmlHaco = new GenericMessageListener();
        con.setListener(Channels.getThisReposClient(), gmlHaco);

        // Set admin state
        UpdateableAdminData adminData = AdminData.getUpdateableInstance();
        adminData.addEntry(STORABLE_FILE.getName(), null, "FFFFFFFFFFFFFFFF");
        adminData.setState(STORABLE_FILE.getName(), Channels.retrieveReplicaChannelNameFromReplicaId("ONE"),
                ReplicaStoreState.UPLOAD_COMPLETED);

        // Store
        StoreMessage msg = new StoreMessage(Channels.getThisReposClient(), STORABLE_FILE);
        JMSConnectionMockupMQ.updateMsgID(msg, "Msg-id-0");
        arcRepos.store(msg.getRemoteFile(), msg);
        con.waitForConcurrentTasksToFinish();

        // Check expected outcome: empty queues
        assertEquals("No messages should be received by the bamon, but found " + gmlOneBaMon.messagesReceived, 0,
                gmlOneBaMon.messagesReceived.size());
        assertEquals("No messages should be received by any ba, but found " + gmlAnyBa.messagesReceived, 0,
                gmlAnyBa.messagesReceived.size());
        assertEquals("No messages should be received by any ba, but found " + gmlAllBa.messagesReceived, 0,
                gmlAllBa.messagesReceived.size());

        // Expected message
        assertEquals("One message should be replied", 1, gmlHaco.messagesReceived.size());
        NetarkivetMessage replyMsg = gmlHaco.messagesReceived.get(0);
        assertTrue("The reply should be a store message but was " + replyMsg.getClass(),
                replyMsg instanceof StoreMessage);
        StoreMessage reply = (StoreMessage) replyMsg;
        assertFalse("The reply should not be okay", reply.isOk());
        assertEquals("The reply should be a reply to the right message", "Msg-id-0", reply.getReplyOfId());

        // Check admin data
        ArcRepositoryEntry entry = adminData.getEntry(STORABLE_FILE.getName());
        assertEquals("Should have expected checksum", "FFFFFFFFFFFFFFFF", entry.getChecksum());
    }

    /**
     * Tests that if we call store with a new file, a store message is sent to the bitarchives. Also tests that state is
     * now UPLOAD_STARTED for all bitarchives
     */
    @Test
    public void testStoreNewFile() throws IOException {
        // Set listeners
        JMSConnectionMockupMQ con = (JMSConnectionMockupMQ) JMSConnectionMockupMQ.getInstance();
        GenericMessageListener gmlAnyBa = new GenericMessageListener();
        con.setListener(Channels.getAnyBa(), gmlAnyBa);
        GenericMessageListener gmlAllBa = new GenericMessageListener();
        con.setListener(Channels.getAllBa(), gmlAllBa);
        GenericMessageListener gmlOneBaMon = new GenericMessageListener();
        con.setListener(Channels.retrieveReplicaChannelFromReplicaId("ONE"), gmlOneBaMon);
        GenericMessageListener gmlHaco = new GenericMessageListener();
        con.setListener(Channels.getThisReposClient(), gmlHaco);

        // Set admin state
        UpdateableAdminData adminData = AdminData.getUpdateableInstance();

        // Store
        StoreMessage msg = new StoreMessage(Channels.getThisReposClient(), STORABLE_FILE);
        JMSConnectionMockupMQ.updateMsgID(msg, "Msg-id-0");
        arcRepos.store(msg.getRemoteFile(), msg);
        con.waitForConcurrentTasksToFinish();

        // Check expected outcome: empty queues
        assertEquals("No messages should be received by the bamon, but found " + gmlOneBaMon.messagesReceived, 0,
                gmlOneBaMon.messagesReceived.size());
        assertEquals("Upload message should be received by any ba, but found " + gmlAnyBa.messagesReceived, 1,
                gmlAnyBa.messagesReceived.size());
        assertEquals("No messages should be received by any ba, but found " + gmlAllBa.messagesReceived, 0,
                gmlAllBa.messagesReceived.size());
        assertEquals("No message should be replied, but recieved: " + gmlHaco.messagesReceived, 0,
                gmlHaco.messagesReceived.size());

        // Expected message
        NetarkivetMessage sentMsg = gmlAnyBa.messagesReceived.get(0);
        assertTrue("The message should be an upload message but was " + sentMsg.getClass(),
                sentMsg instanceof UploadMessage);
        UploadMessage uploadMsg = (UploadMessage) sentMsg;
        assertTrue("The uploadMsg should be okay", uploadMsg.isOk());
        assertEquals("Should be for the right file", STORABLE_FILE.getName(), uploadMsg.getArcfileName());

        // Check admin data
        ArcRepositoryEntry entry = adminData.getEntry(STORABLE_FILE.getName());
        assertEquals("Should have expected checksum", ChecksumCalculator.calculateMd5(STORABLE_FILE),
                entry.getChecksum());
        assertEquals("Should have expected state", ReplicaStoreState.UPLOAD_STARTED, entry.getGeneralStoreState()
                .getState());
        assertEquals("Should have expected state", ReplicaStoreState.UPLOAD_STARTED,
                entry.getStoreState(Channels.retrieveReplicaChannelNameFromReplicaId("ONE")));
    }

    /**
     * Tests that if we call store with a file in state FAILED a checksum job is submitted to the bitarchive. Also test
     * that state is changed to UPLOADED
     */
    @Test
    public void testStoreFailedFile() throws IOException {
        LogbackRecorder lr = LogbackRecorder.startRecorder();
        JMSConnectionMockupMQ con = (JMSConnectionMockupMQ) JMSConnectionMockupMQ.getInstance();
        GenericMessageListener gmlAnyBa = new GenericMessageListener();
        con.setListener(Channels.getAnyBa(), gmlAnyBa);
        GenericMessageListener gmlAllBa = new GenericMessageListener();
        con.setListener(Channels.getAllBa(), gmlAllBa);
        GenericMessageListener gmlOneBaMon = new GenericMessageListener();
        con.setListener(Channels.retrieveReplicaChannelFromReplicaId("ONE"), gmlOneBaMon);
        GenericMessageListener gmlHaco = new GenericMessageListener();
        con.setListener(Channels.getThisReposClient(), gmlHaco);

        // Set admin state
        UpdateableAdminData adminData = AdminData.getUpdateableInstance();
        adminData.addEntry(STORABLE_FILE.getName(), null, ChecksumCalculator.calculateMd5(STORABLE_FILE));
        adminData.setState(STORABLE_FILE.getName(), Channels.retrieveReplicaChannelNameFromReplicaId("ONE"),
                ReplicaStoreState.UPLOAD_FAILED);
        adminData.setState(STORABLE_FILE.getName(), Channels.retrieveReplicaChannelNameFromReplicaId("TWO"),
                ReplicaStoreState.UPLOAD_COMPLETED);

        // Store
        StoreMessage msg = new StoreMessage(Channels.getThisReposClient(), STORABLE_FILE);
        JMSConnectionMockupMQ.updateMsgID(msg, "Msg-id-0");
        arcRepos.store(msg.getRemoteFile(), msg);
        con.waitForConcurrentTasksToFinish();

        // Check expected outcome: empty queues
        assertEquals("No messages should be received by the bamon, but found " + gmlOneBaMon.messagesReceived, 1,
                gmlOneBaMon.messagesReceived.size());
        assertEquals("Upload message should be received by any ba, but found " + gmlAnyBa.messagesReceived, 0,
                gmlAnyBa.messagesReceived.size());
        assertEquals("No messages should be received by any ba, but found " + gmlAllBa.messagesReceived, 0,
                gmlAllBa.messagesReceived.size());
        assertEquals("No message should be replied: " + gmlHaco.messagesReceived, 0, gmlHaco.messagesReceived.size());

        // Expected message
        NetarkivetMessage sentMsg = gmlOneBaMon.messagesReceived.get(0);
        assertTrue("The message should be a checksum message but was " + sentMsg.getClass(),
                sentMsg instanceof GetChecksumMessage);
        GetChecksumMessage gcm = (GetChecksumMessage) sentMsg;
        assertTrue("The checksum message should be okay", gcm.isOk());
        assertEquals(
                "The GetChecksumMessage should ask for the file '" + STORABLE_FILE.getName() + "', not '"
                        + gcm.getArcfileName(), STORABLE_FILE.getName(), gcm.getArcfileName());

        // Check admin data
        ArcRepositoryEntry entry = adminData.getEntry(STORABLE_FILE.getName());
        assertEquals("Should have expected checksum", ChecksumCalculator.calculateMd5(STORABLE_FILE),
                entry.getChecksum());
        assertEquals("Should have expected state", ReplicaStoreState.UPLOAD_STARTED, entry.getGeneralStoreState()
                .getState());
        assertEquals("Should have expected state", ReplicaStoreState.UPLOAD_STARTED,
                entry.getStoreState(Channels.retrieveReplicaChannelNameFromReplicaId("ONE")));

        // Check log for message
        lr.assertLogContains("Should have the log message",
                "Retrying store of already known file '" + STORABLE_FILE.getName() + "', Already completed: " + false);
        lr.stopRecorder();
    }

    /**
     * Tests that if we call store with a file in state UPLOADED a checksum job is submitted to the bitarchive. Also
     * test that state is still UPLOADED
     */
    @Test
    @Ignore("Fragile test: fails in eclipse, but success when run using maven clean install")
    public void testStoreUploadedFile() throws IOException {
        LogbackRecorder lr = LogbackRecorder.startRecorder();
        JMSConnectionMockupMQ con = (JMSConnectionMockupMQ) JMSConnectionMockupMQ.getInstance();
        GenericMessageListener gmlAnyBa = new GenericMessageListener();
        con.setListener(Channels.getAnyBa(), gmlAnyBa);
        GenericMessageListener gmlAllBa = new GenericMessageListener();
        con.setListener(Channels.getAllBa(), gmlAllBa);
        GenericMessageListener gmlOneBaMon = new GenericMessageListener();
        con.setListener(Channels.retrieveReplicaChannelFromReplicaId("ONE"), gmlOneBaMon);
        GenericMessageListener gmlHaco = new GenericMessageListener();
        con.setListener(Channels.getThisReposClient(), gmlHaco);

        // Set admin state
        UpdateableAdminData adminData = AdminData.getUpdateableInstance();
        adminData.addEntry(STORABLE_FILE.getName(), null, ChecksumCalculator.calculateMd5(STORABLE_FILE));
        adminData.setState(STORABLE_FILE.getName(), Channels.retrieveReplicaChannelNameFromReplicaId("ONE"),
                ReplicaStoreState.DATA_UPLOADED);

        // Store
        StoreMessage msg = new StoreMessage(Channels.getThisReposClient(), STORABLE_FILE);
        JMSConnectionMockupMQ.updateMsgID(msg, "Msg-id-0");
        arcRepos.store(msg.getRemoteFile(), msg);
        con.waitForConcurrentTasksToFinish();

        // Check expected outcome: empty queues
        assertEquals("No messages should be received by the bamon, but found " + gmlOneBaMon.messagesReceived, 1,
                gmlOneBaMon.messagesReceived.size());
        assertEquals("Upload message should be received by any ba, but found " + gmlAnyBa.messagesReceived, 0,
                gmlAnyBa.messagesReceived.size());
        assertEquals("No messages should be received by any ba, but found " + gmlAllBa.messagesReceived, 0,
                gmlAllBa.messagesReceived.size());
        assertEquals("No message should be replied", 0, gmlHaco.messagesReceived.size());

        // Expected message
        NetarkivetMessage sentMsg = gmlOneBaMon.messagesReceived.get(0);
        assertTrue("The message should be a checksum message but was " + sentMsg.getClass(),
                sentMsg instanceof GetChecksumMessage);
        GetChecksumMessage gcm = (GetChecksumMessage) sentMsg;
        assertTrue("The checksum message should be okay", gcm.isOk());
        assertEquals("Should be for the right file", STORABLE_FILE.getName(), gcm.getArcfileName());

        // Check admin data
        ArcRepositoryEntry entry = adminData.getEntry(STORABLE_FILE.getName());
        assertEquals("Should have expected checksum", ChecksumCalculator.calculateMd5(STORABLE_FILE),
                entry.getChecksum());
        assertEquals(
                "Should have expected state",
                ReplicaStoreState.DATA_UPLOADED,
                adminData.getState(STORABLE_FILE.getName(), Replica.getReplicaFromId("ONE").getIdentificationChannel()
                        .getName()));
        // entry.getGeneralStoreState().getState());
        assertEquals("Should have expected state", ReplicaStoreState.DATA_UPLOADED,
                entry.getStoreState(Channels.retrieveReplicaChannelNameFromReplicaId("ONE")));

        // Check log for message
        lr.assertLogContains("Should have the log message",
                "Retrying store of already known file '" + STORABLE_FILE.getName() + "', Already completed: " + false);
        lr.stopRecorder();
    }

    /**
     * Tests that if we call store with a file in state STARTED a checksum job is submitted to the bitarchive. Also test
     * that state is still STARTED
     */
    @Test
    public void testStoreStartedFile() throws IOException {
        LogbackRecorder lr = LogbackRecorder.startRecorder();
        JMSConnectionMockupMQ con = (JMSConnectionMockupMQ) JMSConnectionMockupMQ.getInstance();
        GenericMessageListener gmlAnyBa = new GenericMessageListener();
        con.setListener(Channels.getAnyBa(), gmlAnyBa);
        GenericMessageListener gmlAllBa = new GenericMessageListener();
        con.setListener(Channels.getAllBa(), gmlAllBa);
        GenericMessageListener gmlOneBaMon = new GenericMessageListener();
        con.setListener(Channels.retrieveReplicaChannelFromReplicaId("ONE"), gmlOneBaMon);
        GenericMessageListener gmlHaco = new GenericMessageListener();
        con.setListener(Channels.getThisReposClient(), gmlHaco);

        // Set admin state
        UpdateableAdminData adminData = AdminData.getUpdateableInstance();
        adminData.addEntry(STORABLE_FILE.getName(), null, ChecksumCalculator.calculateMd5(STORABLE_FILE));
        adminData.setState(STORABLE_FILE.getName(), Channels.retrieveReplicaChannelNameFromReplicaId("ONE"),
                ReplicaStoreState.UPLOAD_STARTED);
        adminData.setState(STORABLE_FILE.getName(), Channels.retrieveReplicaChannelNameFromReplicaId("TWO"),
                ReplicaStoreState.UPLOAD_STARTED);

        // Store
        StoreMessage msg = new StoreMessage(Channels.getThisReposClient(), STORABLE_FILE);
        JMSConnectionMockupMQ.updateMsgID(msg, "Msg-id-0");
        arcRepos.store(msg.getRemoteFile(), msg);
        con.waitForConcurrentTasksToFinish();

        // Check expected outcome: empty queues
        assertEquals("No messages should be received by the bamon, but found " + gmlOneBaMon.messagesReceived, 1,
                gmlOneBaMon.messagesReceived.size());
        assertEquals("Upload message should be received by any ba, but found " + gmlAnyBa.messagesReceived, 0,
                gmlAnyBa.messagesReceived.size());
        assertEquals("No messages should be received by any ba, but found " + gmlAllBa.messagesReceived, 0,
                gmlAllBa.messagesReceived.size());
        assertEquals("No message should be replied", 0, gmlHaco.messagesReceived.size());

        // Expected message
        NetarkivetMessage sentMsg = gmlOneBaMon.messagesReceived.get(0);
        assertTrue("The message should be a checksum message but was " + sentMsg.getClass(),
                sentMsg instanceof GetChecksumMessage);
        GetChecksumMessage gcm = (GetChecksumMessage) sentMsg;
        assertTrue("The batchMsg should be okay", gcm.isOk());
        assertEquals("Should be for the right file", STORABLE_FILE.getName(), gcm.getArcfileName());

        // Check admin data
        ArcRepositoryEntry entry = adminData.getEntry(STORABLE_FILE.getName());
        assertEquals("Should have expected checksum", ChecksumCalculator.calculateMd5(STORABLE_FILE),
                entry.getChecksum());
        assertEquals("Should have expected state", ReplicaStoreState.UPLOAD_STARTED, entry.getGeneralStoreState()
                .getState());
        assertEquals("Should have expected state", ReplicaStoreState.UPLOAD_STARTED,
                entry.getStoreState(Channels.retrieveReplicaChannelNameFromReplicaId("ONE")));

        // Check log for message
        lr.assertLogContains("Should have the log message",
                "Retrying store of already known file '" + STORABLE_FILE.getName() + "', Already completed: " + false);
        lr.stopRecorder();
    }

    /**
     * Tests that if we get an OK from a bitarchive, we send a checksum job to check the file. Also test that state is
     * data uploaded
     */
    @Test
    @Ignore("Fails: java.lang.AssertionError: Checksum message should be sent to replica 'ONE', but replica 'TWO' got: [msg9: To DEV_TWO_THE_BAMON ReplyTo DEV_COMMON_THE_REPOS OK Arcfiles: NetarchiveSuite-store1.arc, ReplicaId: TWO, Checksum: null] expected:<0> but was:<1>")
    // FIXME: test fails currently
    public void testOnUploadMessageOK() throws IOException {
        JMSConnectionMockupMQ con = (JMSConnectionMockupMQ) JMSConnectionMockupMQ.getInstance();
        GenericMessageListener gmlAnyBa = new GenericMessageListener();
        con.setListener(Channels.getAnyBa(), gmlAnyBa);
        GenericMessageListener gmlAllBa = new GenericMessageListener();
        con.setListener(Channels.getAllBa(), gmlAllBa);
        GenericMessageListener gmlOneBaMon = new GenericMessageListener();
        con.setListener(Channels.retrieveReplicaChannelFromReplicaId("ONE"), gmlOneBaMon);
        GenericMessageListener gmlTwoBaMon = new GenericMessageListener();
        con.setListener(Channels.retrieveReplicaChannelFromReplicaId("TWO"), gmlTwoBaMon);
        GenericMessageListener gmlHaco = new GenericMessageListener();
        con.setListener(Channels.getThisReposClient(), gmlHaco);

        // Set admin state
        UpdateableAdminData adminData = AdminData.getUpdateableInstance();
        adminData.addEntry(STORABLE_FILE.getName(), null, ChecksumCalculator.calculateMd5(STORABLE_FILE));
        adminData.setState(STORABLE_FILE.getName(), Channels.retrieveReplicaChannelNameFromReplicaId("ONE"),
                ReplicaStoreState.UPLOAD_STARTED);

        // Deliver message
        UploadMessage msg = new UploadMessage(Channels.getAnyBa(), Channels.getTheRepos(),
                RemoteFileFactory.getInstance(STORABLE_FILE, true, false, true));
        JMSConnectionMockupMQ.updateMsgID(msg, "Msg-id-0");
        arcRepos.onUpload(msg);
        con.waitForConcurrentTasksToFinish();

        // Check expected outcome: checksumjob
        assertEquals("Checksum message should be sent to replica 'ONE', but replica 'TWO' got: "
                + gmlTwoBaMon.messagesReceived, 0, gmlTwoBaMon.messagesReceived.size());
        assertEquals(
                "'1 checksum message should be received by bamon 'ONE', but found " + gmlOneBaMon.messagesReceived, 1,
                gmlOneBaMon.messagesReceived.size());
        assertEquals("Upload message should be received by any ba, but found " + gmlAnyBa.messagesReceived, 0,
                gmlAnyBa.messagesReceived.size());
        assertEquals("No messages should be received by any ba, but found " + gmlAllBa.messagesReceived, 0,
                gmlAllBa.messagesReceived.size());
        assertEquals("No message should be sent to the HACO channel, but received: " + gmlHaco.messagesReceived, 0,
                gmlHaco.messagesReceived.size());

        // Expected message
        NetarkivetMessage sentMsg = gmlOneBaMon.messagesReceived.get(0);
        assertTrue("The message should be a checksum message but was " + sentMsg.getClass(),
                sentMsg instanceof GetChecksumMessage);
        GetChecksumMessage gcm = (GetChecksumMessage) sentMsg;
        assertTrue("The checksum message should be okay", gcm.isOk());
        assertEquals("Should be for the right file", STORABLE_FILE.getName(), gcm.getArcfileName());

        // Check admin data
        ArcRepositoryEntry entry = adminData.getEntry(STORABLE_FILE.getName());
        assertEquals("Should have expected checksum", ChecksumCalculator.calculateMd5(STORABLE_FILE),
                entry.getChecksum());
        assertEquals("Should have expected state", ReplicaStoreState.DATA_UPLOADED, entry.getGeneralStoreState()
                .getState());
        assertEquals("Should have expected state", ReplicaStoreState.DATA_UPLOADED,
                entry.getStoreState(Channels.retrieveReplicaChannelNameFromReplicaId("ONE")));
        assertNull("Replica 'TWO' should not have any entries for the file '" + STORABLE_FILE.getName(),
                entry.getStoreState(Channels.retrieveReplicaChannelNameFromReplicaId("TWO")));
        assertNull("Replica 'THREE' should not have any entries for the file '" + STORABLE_FILE.getName(),
                entry.getStoreState(Channels.retrieveReplicaChannelNameFromReplicaId("THREE")));
    }

    /**
     * Tests that if we get a not OK from a bitarchive, we reply not OK (no other bitarchive is waiting for upload
     * replies). Also test that state is upload failed
     */
    @Test
    @Ignore("FIXME:java.lang.AssertionError: One message should be replied expected:<1> but was:<0>")
    // FIXME: test fails due to error: java.lang.AssertionError: One message should be replied expected:<1> but was:<0>")
    public void testOnUploadMessageNotOK() throws IOException {
        JMSConnectionMockupMQ con = (JMSConnectionMockupMQ) JMSConnectionMockupMQ.getInstance();
        GenericMessageListener gmlAnyBa = new GenericMessageListener();
        con.setListener(Channels.getAnyBa(), gmlAnyBa);
        GenericMessageListener gmlAllBa = new GenericMessageListener();
        con.setListener(Channels.getAllBa(), gmlAllBa);
        GenericMessageListener gmlOneBaMon = new GenericMessageListener();
        con.setListener(Channels.retrieveReplicaChannelFromReplicaId("ONE"), gmlOneBaMon);
        // GenericMessageListener gmlTwoBaMon = new GenericMessageListener();
        con.setListener(Channels.retrieveReplicaChannelFromReplicaId("TWO"), gmlOneBaMon);
        GenericMessageListener gmlHaco = new GenericMessageListener();
        con.setListener(Channels.getThisReposClient(), gmlHaco);

        // Set admin state
        UpdateableAdminData adminData = AdminData.getUpdateableInstance();
        StoreMessage message = new StoreMessage(Channels.getThisReposClient(), STORABLE_FILE);
        JMSConnectionMockupMQ.updateMsgID(message, "Store-1");
        adminData.addEntry(STORABLE_FILE.getName(), message, ChecksumCalculator.calculateMd5(STORABLE_FILE));
        adminData.setState(STORABLE_FILE.getName(), Channels.retrieveReplicaChannelNameFromReplicaId("ONE"),
                ReplicaStoreState.UPLOAD_STARTED);
        adminData.setState(STORABLE_FILE.getName(), Channels.retrieveReplicaChannelNameFromReplicaId("TWO"),
                ReplicaStoreState.UPLOAD_COMPLETED);

        // Deliver message
        UploadMessage msg = new UploadMessage(Channels.getAnyBa(), Channels.getTheRepos(),
                RemoteFileFactory.getInstance(STORABLE_FILE, true, false, true));
        msg.setNotOk("NOT OK!");
        JMSConnectionMockupMQ.updateMsgID(msg, "Msg-id-0");
        arcRepos.onUpload(msg);
        con.waitForConcurrentTasksToFinish();

        // Check expected outcome: reply
        assertEquals("'Checksum message should be received by the bamon, but found " + gmlOneBaMon.messagesReceived, 0,
                gmlOneBaMon.messagesReceived.size());
        assertEquals("Upload message should be received by any ba, but found " + gmlAnyBa.messagesReceived, 0,
                gmlAnyBa.messagesReceived.size());
        assertEquals("No messages should be received by any ba, but found " + gmlAllBa.messagesReceived, 0,
                gmlAllBa.messagesReceived.size());
        assertEquals("One message should be replied", 1, gmlHaco.messagesReceived.size());

        // Expected message
        NetarkivetMessage sentMsg = gmlHaco.messagesReceived.get(0);
        assertTrue("The message should be a store message but was " + sentMsg.getClass(),
                sentMsg instanceof StoreMessage);
        StoreMessage storeMsg = (StoreMessage) sentMsg;
        assertFalse("The storeMsg should NOT be okay", storeMsg.isOk());
        assertEquals("Should be for the right file", STORABLE_FILE.getName(), storeMsg.getArcfileName());

        // Check admin data
        ArcRepositoryEntry entry = adminData.getEntry(STORABLE_FILE.getName());
        assertEquals("Should have expected checksum", ChecksumCalculator.calculateMd5(STORABLE_FILE),
                entry.getChecksum());
        assertEquals("Should have expected state", ReplicaStoreState.UPLOAD_FAILED, entry.getGeneralStoreState()
                .getState());
        assertEquals("Should have expected state", ReplicaStoreState.UPLOAD_FAILED,
                entry.getStoreState(Channels.retrieveReplicaChannelNameFromReplicaId("ONE")));
    }

    /**
     * Tests that a batch reply with correct checksum results in an OK message (all bitarchives are OK) Also test that
     * state is completed
     */
    @Test
    public void testOnBatchReplyOk() throws IOException, NoSuchFieldException, IllegalAccessException {
        JMSConnectionMockupMQ con = (JMSConnectionMockupMQ) JMSConnectionMockupMQ.getInstance();
        GenericMessageListener gmlAnyBa = new GenericMessageListener();
        con.setListener(Channels.getAnyBa(), gmlAnyBa);
        GenericMessageListener gmlAllBa = new GenericMessageListener();
        con.setListener(Channels.getAllBa(), gmlAllBa);
        GenericMessageListener gmlOneBaMon = new GenericMessageListener();
        con.setListener(Channels.retrieveReplicaChannelFromReplicaId("ONE"), gmlOneBaMon);
        GenericMessageListener gmlRepos = new GenericMessageListener();
        con.setListener(Channels.getThisReposClient(), gmlRepos);

        // Set admin state
        UpdateableAdminData adminData = AdminData.getUpdateableInstance();
        StoreMessage message = new StoreMessage(Channels.getThisReposClient(), STORABLE_FILE);
        JMSConnectionMockupMQ.updateMsgID(message, "Store-1");
        adminData.addEntry(STORABLE_FILE.getName(), message, ChecksumCalculator.calculateMd5(STORABLE_FILE));
        adminData.setState(STORABLE_FILE.getName(), Channels.retrieveReplicaChannelFromReplicaId("ONE").getName(),
                ReplicaStoreState.DATA_UPLOADED);
        adminData.setState(STORABLE_FILE.getName(), Channels.retrieveReplicaChannelFromReplicaId("TWO").getName(),
                ReplicaStoreState.UPLOAD_COMPLETED);
        adminData.setState(STORABLE_FILE.getName(), Channels.retrieveReplicaChannelFromReplicaId("THREE").getName(),
                ReplicaStoreState.UPLOAD_COMPLETED);

        // Put outstanding checksum in
        Field f = ArcRepository.class.getDeclaredField("outstandingChecksumFiles");
        f.setAccessible(true);
        Map<String, String> outstandingChecksumFiles = (Map<String, String>) f.get(arcRepos);
        outstandingChecksumFiles.put("Msg-id-0", STORABLE_FILE.getName());

        // Deliver message
        BatchReplyMessage msg = new BatchReplyMessage(Channels.getTheRepos(),
                Channels.retrieveReplicaChannelFromReplicaId("ONE"), "Msg-id-0", 1, Collections.<File>emptyList(),
                RemoteFileFactory.getInstance(BATCH_RESULT, true, false, true));
        JMSConnectionMockupMQ.updateMsgID(msg, "Msg-id-1");
        arcRepos.onBatchReply(msg);
        con.waitForConcurrentTasksToFinish();

        // Check expected outcome: reply
        assertEquals("'Checksum message should be received by the bamon, but found " + gmlOneBaMon.messagesReceived, 0,
                gmlOneBaMon.messagesReceived.size());
        assertEquals("Upload message should be received by any ba, but found " + gmlAnyBa.messagesReceived, 0,
                gmlAnyBa.messagesReceived.size());
        assertEquals("No messages should be received by any ba, but found " + gmlAllBa.messagesReceived, 0,
                gmlAllBa.messagesReceived.size());
        assertEquals("One message should be replied", 1, gmlRepos.messagesReceived.size());

        // Expected message
        NetarkivetMessage sentMsg = gmlRepos.messagesReceived.get(0);
        assertTrue("The message should be a store message but was " + sentMsg.getClass(),
                sentMsg instanceof StoreMessage);
        StoreMessage storeMsg = (StoreMessage) sentMsg;
        MessageAsserts.assertMessageOk("The storeMsg should be okay", storeMsg);
        assertEquals("Should be for the right file", STORABLE_FILE.getName(), storeMsg.getArcfileName());

        // Check admin data
        ArcRepositoryEntry entry = adminData.getEntry(STORABLE_FILE.getName());
        assertEquals("Should have expected checksum", ChecksumCalculator.calculateMd5(STORABLE_FILE),
                entry.getChecksum());
        assertEquals("Should have expected state", ReplicaStoreState.UPLOAD_COMPLETED, entry.getGeneralStoreState()
                .getState());
        assertEquals("Should have expected state", ReplicaStoreState.UPLOAD_COMPLETED,
                entry.getStoreState(Channels.retrieveReplicaChannelNameFromReplicaId("ONE")));
    }

    /**
     * Tests that a batch reply with un-correct checksum after an upload results in a not OK message, unless some
     * bitarchive is waiting for replies Also test that state is failed
     */
    @Test
    public void testOnBatchReplyNotOkOnUpload() throws IOException, NoSuchFieldException, IllegalAccessException {
        JMSConnectionMockupMQ con = (JMSConnectionMockupMQ) JMSConnectionMockupMQ.getInstance();
        GenericMessageListener gmlAnyBa = new GenericMessageListener();
        con.setListener(Channels.getAnyBa(), gmlAnyBa);
        GenericMessageListener gmlAllBa = new GenericMessageListener();
        con.setListener(Channels.getAllBa(), gmlAllBa);
        GenericMessageListener gmlOneBaMon = new GenericMessageListener();
        con.setListener(Channels.retrieveReplicaChannelFromReplicaId("ONE"), gmlOneBaMon);
        GenericMessageListener gmlHaco = new GenericMessageListener();
        con.setListener(Channels.getThisReposClient(), gmlHaco);

        // Set admin state
        UpdateableAdminData adminData = AdminData.getUpdateableInstance();
        StoreMessage message = new StoreMessage(Channels.getThisReposClient(), STORABLE_FILE);
        JMSConnectionMockupMQ.updateMsgID(message, "Store-1");
        adminData.addEntry(STORABLE_FILE.getName(), message, ChecksumCalculator.calculateMd5(STORABLE_FILE));
        adminData.setState(STORABLE_FILE.getName(), Channels.retrieveReplicaChannelNameFromReplicaId("ONE"),
                ReplicaStoreState.DATA_UPLOADED);

        // Put outstanding checksum in
        Field f = ArcRepository.class.getDeclaredField("outstandingChecksumFiles");
        f.setAccessible(true);
        Map<String, String> outstandingChecksumFiles = (Map<String, String>) f.get(arcRepos);
        outstandingChecksumFiles.put("Msg-id-0", STORABLE_FILE.getName());

        // Deliver message
        BatchReplyMessage msg = new BatchReplyMessage(Channels.getTheRepos(),
                Channels.retrieveReplicaChannelFromReplicaId("ONE"), "Msg-id-0", 1, Collections.<File>emptyList(),
                RemoteFileFactory.getInstance(BATCH_RESULT_WRONG, true, false, true));
        JMSConnectionMockupMQ.updateMsgID(msg, "Msg-id-1");
        arcRepos.onBatchReply(msg);
        con.waitForConcurrentTasksToFinish();

        // Check expected outcome: reply
        assertEquals("'Checksum message should be received by the bamon, but found " + gmlOneBaMon.messagesReceived, 0,
                gmlOneBaMon.messagesReceived.size());
        assertEquals("Upload message should be received by any ba, but found " + gmlAnyBa.messagesReceived, 0,
                gmlAnyBa.messagesReceived.size());
        assertEquals("No messages should be received by any ba, but found " + gmlAllBa.messagesReceived, 0,
                gmlAllBa.messagesReceived.size());
        assertEquals("One message should be replied", 1, gmlHaco.messagesReceived.size());

        // Expected message
        NetarkivetMessage sentMsg = gmlHaco.messagesReceived.get(0);
        assertTrue("The message should be a store message but was " + sentMsg.getClass(),
                sentMsg instanceof StoreMessage);
        StoreMessage storeMsg = (StoreMessage) sentMsg;
        assertFalse("The storeMsg should NOT be okay", storeMsg.isOk());
        assertEquals("Should be for the right file", STORABLE_FILE.getName(), storeMsg.getArcfileName());

        // Check admin data
        ArcRepositoryEntry entry = adminData.getEntry(STORABLE_FILE.getName());
        assertEquals("Should have expected checksum", ChecksumCalculator.calculateMd5(STORABLE_FILE),
                entry.getChecksum());
        assertEquals("Should have expected state", ReplicaStoreState.UPLOAD_FAILED, entry.getGeneralStoreState()
                .getState());
        assertEquals("Should have expected state", ReplicaStoreState.UPLOAD_FAILED,
                entry.getStoreState(Channels.retrieveReplicaChannelNameFromReplicaId("ONE")));
    }

    /**
     * Tests that a batch reply with no checksum for a file missing upload reply (a retry) results in an attempt to
     * store the file again Also test that state is upload started
     */
    @Test
    public void testOnBatchReplyNotOkOnRetry() throws IOException, NoSuchFieldException, IllegalAccessException {
        JMSConnectionMockupMQ con = (JMSConnectionMockupMQ) JMSConnectionMockupMQ.getInstance();
        GenericMessageListener gmlAnyBa = new GenericMessageListener();
        con.setListener(Channels.getAnyBa(), gmlAnyBa);
        GenericMessageListener gmlAllBa = new GenericMessageListener();
        con.setListener(Channels.getAllBa(), gmlAllBa);
        GenericMessageListener gmlOneBaMon = new GenericMessageListener();
        con.setListener(Channels.retrieveReplicaChannelFromReplicaId("ONE"), gmlOneBaMon);
        GenericMessageListener gmlHaco = new GenericMessageListener();
        con.setListener(Channels.getThisReposClient(), gmlHaco);

        // Set admin state
        UpdateableAdminData adminData = AdminData.getUpdateableInstance();
        StoreMessage message = new StoreMessage(Channels.getThisReposClient(), STORABLE_FILE);
        JMSConnectionMockupMQ.updateMsgID(message, "Store-1");
        adminData.addEntry(STORABLE_FILE.getName(), message, ChecksumCalculator.calculateMd5(STORABLE_FILE));
        adminData.setState(STORABLE_FILE.getName(), Channels.retrieveReplicaChannelNameFromReplicaId("ONE"),
                ReplicaStoreState.DATA_UPLOADED);

        // Put outstanding checksum in
        Field f = ArcRepository.class.getDeclaredField("outstandingChecksumFiles");
        f.setAccessible(true);
        Map<String, String> outstandingChecksumFiles = (Map<String, String>) f.get(arcRepos);
        outstandingChecksumFiles.put("Msg-id-0", STORABLE_FILE.getName());

        // Deliver message
        BatchReplyMessage msg = new BatchReplyMessage(Channels.getTheRepos(),
                Channels.retrieveReplicaChannelFromReplicaId("ONE"), "Msg-id-0", 0, Collections.<File>emptyList(),
                RemoteFileFactory.getInstance(BATCH_RESULT_EMPTY, true, false, true));
        JMSConnectionMockupMQ.updateMsgID(msg, "Msg-id-1");
        arcRepos.onBatchReply(msg);
        con.waitForConcurrentTasksToFinish();

        // Check expected outcome: reply
        assertEquals("'Checksum message should be received by the bamon, but found " + gmlOneBaMon.messagesReceived, 0,
                gmlOneBaMon.messagesReceived.size());
        assertEquals("Upload message should be received by any ba, but found " + gmlAnyBa.messagesReceived, 0,
                gmlAnyBa.messagesReceived.size());
        assertEquals("No messages should be received by any ba, but found " + gmlAllBa.messagesReceived, 0,
                gmlAllBa.messagesReceived.size());
        assertEquals("One message should be replied", 1, gmlHaco.messagesReceived.size());

        // Expected message
        NetarkivetMessage sentMsg = gmlHaco.messagesReceived.get(0);
        assertTrue("The message should be a store message but was " + sentMsg.getClass(),
                sentMsg instanceof StoreMessage);
        StoreMessage storeMsg = (StoreMessage) sentMsg;
        assertFalse("The storeMsg should NOT be okay", storeMsg.isOk());
        assertEquals("Should be for the right file", STORABLE_FILE.getName(), storeMsg.getArcfileName());

        // Check admin data
        ArcRepositoryEntry entry = adminData.getEntry(STORABLE_FILE.getName());
        assertEquals("Should have expected checksum", ChecksumCalculator.calculateMd5(STORABLE_FILE),
                entry.getChecksum());
        assertEquals("Should have expected state", ReplicaStoreState.UPLOAD_FAILED, entry.getGeneralStoreState()
                .getState());
        assertEquals("Should have expected state", ReplicaStoreState.UPLOAD_FAILED,
                entry.getStoreState(Channels.retrieveReplicaChannelNameFromReplicaId("ONE")));
    }

    // TODO: Check that tests are exhaustive, and check more than one BA
}
