/* File:    $Id$
 * Version: $Revision$
 * Date:    $Date$
 * Author:  $Author$
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
package dk.netarkivet.archive.arcrepository;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Map;
import java.util.logging.LogManager;
import java.util.regex.Pattern;

import junit.framework.TestCase;

import dk.netarkivet.archive.arcrepository.bitpreservation.ChecksumJob;
import dk.netarkivet.archive.arcrepository.distribute.StoreMessage;
import dk.netarkivet.archive.arcrepositoryadmin.AdminData;
import dk.netarkivet.archive.arcrepositoryadmin.ArcRepositoryEntry;
import dk.netarkivet.archive.arcrepositoryadmin.UpdateableAdminData;
import dk.netarkivet.archive.bitarchive.distribute.BatchMessage;
import dk.netarkivet.archive.bitarchive.distribute.BatchReplyMessage;
import dk.netarkivet.archive.bitarchive.distribute.UploadMessage;
import dk.netarkivet.common.Settings;
import dk.netarkivet.common.distribute.Channels;
import dk.netarkivet.common.distribute.ChannelsTester;
import dk.netarkivet.common.distribute.JMSConnectionTestMQ;
import dk.netarkivet.common.distribute.NetarkivetMessage;
import dk.netarkivet.common.distribute.RemoteFileFactory;
import dk.netarkivet.common.distribute.arcrepository.BitArchiveStoreState;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.MD5;
import dk.netarkivet.testutils.FileAsserts;
import dk.netarkivet.testutils.GenericMessageListener;
import dk.netarkivet.testutils.LogUtils;
import dk.netarkivet.testutils.MessageAsserts;
import dk.netarkivet.testutils.preconfigured.UseTestRemoteFile;

/**
 * This class tests the store() method of ArcRepository.
 */
public class ArcRepositoryTesterStore extends TestCase {
    private UseTestRemoteFile rf = new UseTestRemoteFile();

    /**
     * The directory from where we upload the ARC files.
     */
    private static final File ORIGINALS_DIR = new File(ServerSetUp.TEST_DIR,
                                                       "originals");

    /**
     * The file that is uploaded during the test
     */
    private static final File STORABLE_FILE = new File(ORIGINALS_DIR,
                                                       "NetarchiveSuite-store1.arc");

    private ArcRepository arcRepos;
    private static final File LOG_FILE = new File(
            "tests/testlogs/netarkivtest.log");
    private static final File BATCH_RESULT = new File(ORIGINALS_DIR,
                                                      "checksum");
    private static final File BATCH_RESULT_WRONG = new File(ORIGINALS_DIR,
                                                            "checksumwrong");
    private static final File BATCH_RESULT_EMPTY = new File(ORIGINALS_DIR,
                                                            "checksumempty");
    private static final File TEST_DIR = 
    	new File (Settings.get(Settings.DIR_COMMONTEMPDIR));
    
    public ArcRepositoryTesterStore() {
    }

    /**
     * The directory used for controller admindata
     */
    private static final File ADMINDATA_DIR = new File(TEST_DIR, "admindata");

    /**
     * The bitarchive directory to work on.
     */
    static final File ARCHIVE_DIR = new File(TEST_DIR, "bitarchive");

    /**
     * The directory used for storing temporary files
     */
    private static final File TEMP_DIR = new File(TEST_DIR, "tempdir");

    public void setUp() throws IOException {
        Settings.reload();
        rf.setUp();
        Settings.set(Settings.ENVIRONMENT_LOCATION_NAMES, "SB");
        Settings.set(Settings.ENVIRONMENT_THIS_LOCATION, "SB");
        Settings.set(Settings.DIRS_ARCREPOSITORY_ADMIN,
                     ADMINDATA_DIR.getAbsolutePath());
        Settings.set(Settings.BITARCHIVE_SERVER_FILEDIR,
                     ARCHIVE_DIR.getAbsolutePath());
        Settings.set(Settings.DIR_COMMONTEMPDIR, TEMP_DIR.getAbsolutePath());

        JMSConnectionTestMQ.useJMSConnectionTestMQ();
        JMSConnectionTestMQ.clearTestQueues();

        ChannelsTester.resetChannels();

        FileUtils.removeRecursively(ARCHIVE_DIR);
        FileUtils.removeRecursively(TEMP_DIR);
        FileUtils.removeRecursively(ADMINDATA_DIR);
        FileUtils.createDir(ADMINDATA_DIR);
        FileUtils.createDir(TEMP_DIR);

        // Create a bit archive server that listens to archive events
        arcRepos = ArcRepository.getInstance();

        FileInputStream fis = new FileInputStream(
                "tests/dk/netarkivet/testlog.prop");
        LogManager.getLogManager().reset();
        FileUtils.removeRecursively(LOG_FILE);
        LogManager.getLogManager().readConfiguration(fis);
    }

    public void tearDown() {
        arcRepos.close();// close down ArcRepository Controller

        FileUtils.removeRecursively(ADMINDATA_DIR);
        FileUtils.removeRecursively(ARCHIVE_DIR);
        FileUtils.removeRecursively(TEMP_DIR);

        JMSConnectionTestMQ.clearTestQueues();
        rf.tearDown();
        Settings.reload();
    }

    /**
     * Tests the scenario where a file has been stored,
     * but the confirmation was lost, so that the harvester
     * will want to store the file again.
     *
     * If the first store() was succesful, the second one
     * should be too (providing the file name and the MD5 is the same).
     *
     * @throws InterruptedException
     */
    public void testStoreFileAlreadyStored() throws InterruptedException,
            IOException {
        //Set listeners
        JMSConnectionTestMQ con = (JMSConnectionTestMQ) JMSConnectionTestMQ.getInstance();
        GenericMessageListener gmlAnyBa = new GenericMessageListener();
        con.setListener(Channels.getAnyBa(), gmlAnyBa);
        GenericMessageListener gmlAllBa = new GenericMessageListener();
        con.setListener(Channels.getAllBa(), gmlAllBa);
        GenericMessageListener gmlSBBaMon = new GenericMessageListener();
        con.setListener(Channels.getBaMonForLocation("SB"), gmlSBBaMon);
        GenericMessageListener gmlHaco = new GenericMessageListener();
        con.setListener(Channels.getThisHaco(), gmlHaco);

        //Set admin state
        UpdateableAdminData adminData = AdminData.getUpdateableInstance();
        adminData.addEntry(STORABLE_FILE.getName(), null, MD5.generateMD5onFile(
                STORABLE_FILE));
        adminData.setState(STORABLE_FILE.getName(),
                           Channels.getBaMonForLocation("SB").getName(),
                           BitArchiveStoreState.UPLOAD_COMPLETED);

        //Store
        StoreMessage msg = new StoreMessage(Channels.getThisHaco(),
                                            STORABLE_FILE);
        JMSConnectionTestMQ.updateMsgID(msg, "Msg-id-0");
        arcRepos.store(msg.getRemoteFile(), msg);
        con.waitForConcurrentTasksToFinish();

        //Check expected outcome: empty queues
        assertEquals("No messages should be received by the bamon, but found "
                     + gmlSBBaMon.messagesReceived, 0,
                     gmlSBBaMon.messagesReceived.size());
        assertEquals("No messages should be received by any ba, but found "
                     + gmlAnyBa.messagesReceived, 0,
                     gmlAnyBa.messagesReceived.size());
        assertEquals("No messages should be received by any ba, but found "
                     + gmlAllBa.messagesReceived, 0,
                     gmlAllBa.messagesReceived.size());

        //Expected message
        assertEquals("One message should be replied", 1,
                     gmlHaco.messagesReceived.size());
        NetarkivetMessage replyMsg = gmlHaco.messagesReceived.get(0);
        assertTrue("The reply should be a store message but was "
                   + replyMsg.getClass(), replyMsg instanceof StoreMessage);
        StoreMessage reply = (StoreMessage) replyMsg;
        assertTrue("The reply should be okay", reply.isOk());
        assertEquals("The reply should be a reply to the right message",
                     "Msg-id-0", reply.getReplyOfId());

        //Check admin data
        ArcRepositoryEntry entry = adminData.getEntry(STORABLE_FILE.getName());
        assertEquals("Should have expected checksum",
                     MD5.generateMD5onFile(STORABLE_FILE), entry.getChecksum());

        //Check log for message
        LogUtils.flushLogs(ArcRepository.class.getName());
        FileAsserts.assertFileContains("Should have the log message",
                                       "Retrying store of already known file '"
                                       + STORABLE_FILE.getName()
                                       + "',"
                                       + " Already completed: "
                                       + true,
                                       LOG_FILE);
    }

    /**
     * Tests that we get a Not-OK message, if the file is known with other
     * checksum.
     * Also test that state is unchanged afterwards
     */
    public void testStoreOtherChecksum() {
        //Set listeners
        JMSConnectionTestMQ con = (JMSConnectionTestMQ) JMSConnectionTestMQ.getInstance();
        GenericMessageListener gmlAnyBa = new GenericMessageListener();
        con.setListener(Channels.getAnyBa(), gmlAnyBa);
        GenericMessageListener gmlAllBa = new GenericMessageListener();
        con.setListener(Channels.getAllBa(), gmlAllBa);
        GenericMessageListener gmlSBBaMon = new GenericMessageListener();
        con.setListener(Channels.getBaMonForLocation("SB"), gmlSBBaMon);
        GenericMessageListener gmlHaco = new GenericMessageListener();
        con.setListener(Channels.getThisHaco(), gmlHaco);

        //Set admin state
        UpdateableAdminData adminData = AdminData.getUpdateableInstance();
        adminData.addEntry(STORABLE_FILE.getName(), null, "FFFFFFFFFFFFFFFF");
        adminData.setState(STORABLE_FILE.getName(),
                           Channels.getBaMonForLocation("SB").getName(),
                           BitArchiveStoreState.UPLOAD_COMPLETED);

        //Store
        StoreMessage msg = new StoreMessage(Channels.getThisHaco(),
                                            STORABLE_FILE);
        JMSConnectionTestMQ.updateMsgID(msg, "Msg-id-0");
        arcRepos.store(msg.getRemoteFile(), msg);
        con.waitForConcurrentTasksToFinish();

        //Check expected outcome: empty queues
        assertEquals("No messages should be received by the bamon, but found "
                     + gmlSBBaMon.messagesReceived, 0,
                     gmlSBBaMon.messagesReceived.size());
        assertEquals("No messages should be received by any ba, but found "
                     + gmlAnyBa.messagesReceived, 0,
                     gmlAnyBa.messagesReceived.size());
        assertEquals("No messages should be received by any ba, but found "
                     + gmlAllBa.messagesReceived, 0,
                     gmlAllBa.messagesReceived.size());

        //Expected message
        assertEquals("One message should be replied", 1,
                     gmlHaco.messagesReceived.size());
        NetarkivetMessage replyMsg = gmlHaco.messagesReceived.get(0);
        assertTrue("The reply should be a store message but was "
                   + replyMsg.getClass(), replyMsg instanceof StoreMessage);
        StoreMessage reply = (StoreMessage) replyMsg;
        assertFalse("The reply should not be okay", reply.isOk());
        assertEquals("The reply should be a reply to the right message",
                     "Msg-id-0", reply.getReplyOfId());

        //Check admin data
        ArcRepositoryEntry entry = adminData.getEntry(STORABLE_FILE.getName());
        assertEquals("Should have expected checksum", "FFFFFFFFFFFFFFFF",
                     entry.getChecksum());
    }

    /**
     * Tests that if we call store with a new file, a store message is sent to
     * the bitarchives
     * Also test that state is now upload started for all bitarchives
     */
    public void testStoreNewFile() throws IOException {
        //Set listeners
        JMSConnectionTestMQ con = (JMSConnectionTestMQ) JMSConnectionTestMQ.getInstance();
        GenericMessageListener gmlAnyBa = new GenericMessageListener();
        con.setListener(Channels.getAnyBa(), gmlAnyBa);
        GenericMessageListener gmlAllBa = new GenericMessageListener();
        con.setListener(Channels.getAllBa(), gmlAllBa);
        GenericMessageListener gmlSBBaMon = new GenericMessageListener();
        con.setListener(Channels.getBaMonForLocation("SB"), gmlSBBaMon);
        GenericMessageListener gmlHaco = new GenericMessageListener();
        con.setListener(Channels.getThisHaco(), gmlHaco);

        //Set admin state
        UpdateableAdminData adminData = AdminData.getUpdateableInstance();

        //Store
        StoreMessage msg = new StoreMessage(Channels.getThisHaco(),
                                            STORABLE_FILE);
        JMSConnectionTestMQ.updateMsgID(msg, "Msg-id-0");
        arcRepos.store(msg.getRemoteFile(), msg);
        con.waitForConcurrentTasksToFinish();

        //Check expected outcome: empty queues
        assertEquals("No messages should be received by the bamon, but found "
                     + gmlSBBaMon.messagesReceived, 0,
                     gmlSBBaMon.messagesReceived.size());
        assertEquals("Upload message should be received by any ba, but found "
                     + gmlAnyBa.messagesReceived, 1,
                     gmlAnyBa.messagesReceived.size());
        assertEquals("No messages should be received by any ba, but found "
                     + gmlAllBa.messagesReceived, 0,
                     gmlAllBa.messagesReceived.size());
        assertEquals("No message should be replied", 0,
                     gmlHaco.messagesReceived.size());

        //Expected message
        NetarkivetMessage sentMsg = gmlAnyBa.messagesReceived.get(0);
        assertTrue("The message should be an upload message but was "
                   + sentMsg.getClass(), sentMsg instanceof UploadMessage);
        UploadMessage uploadMsg = (UploadMessage) sentMsg;
        assertTrue("The uploadMsg should be okay", uploadMsg.isOk());
        assertEquals("Should be for the right file", STORABLE_FILE.getName(),
                     uploadMsg.getArcfileName());

        //Check admin data
        ArcRepositoryEntry entry = adminData.getEntry(STORABLE_FILE.getName());
        assertEquals("Should have expected checksum",
                     MD5.generateMD5onFile(STORABLE_FILE), entry.getChecksum());
        assertEquals("Should have expected state",
                     BitArchiveStoreState.UPLOAD_STARTED,
                     entry.getGeneralStoreState().getState());
        assertEquals("Should have expected state", BitArchiveStoreState.UPLOAD_STARTED, entry.getStoreState(
                Channels.getBaMonForLocation("SB").getName()));
    }

    /**
     * Tests that if we call store with a file in state FAILED
     * a checksum job is submitted to the bitarchive.
     * Also test that state is changed to UPLOADED
     */
    public void testStoreFailedFile() throws IOException {
        JMSConnectionTestMQ con = (JMSConnectionTestMQ) JMSConnectionTestMQ.getInstance();
        GenericMessageListener gmlAnyBa = new GenericMessageListener();
        con.setListener(Channels.getAnyBa(), gmlAnyBa);
        GenericMessageListener gmlAllBa = new GenericMessageListener();
        con.setListener(Channels.getAllBa(), gmlAllBa);
        GenericMessageListener gmlSBBaMon = new GenericMessageListener();
        con.setListener(Channels.getBaMonForLocation("SB"), gmlSBBaMon);
        GenericMessageListener gmlHaco = new GenericMessageListener();
        con.setListener(Channels.getThisHaco(), gmlHaco);

        //Set admin state
        UpdateableAdminData adminData = AdminData.getUpdateableInstance();
        adminData.addEntry(STORABLE_FILE.getName(), null, MD5.generateMD5onFile(
                STORABLE_FILE));
        adminData.setState(STORABLE_FILE.getName(),
                           Channels.getBaMonForLocation("SB").getName(),
                           BitArchiveStoreState.UPLOAD_FAILED);

        //Store
        StoreMessage msg = new StoreMessage(Channels.getThisHaco(),
                                            STORABLE_FILE);
        JMSConnectionTestMQ.updateMsgID(msg, "Msg-id-0");
        arcRepos.store(msg.getRemoteFile(), msg);
        con.waitForConcurrentTasksToFinish();

        //Check expected outcome: empty queues
        assertEquals("No messages should be received by the bamon, but found "
                     + gmlSBBaMon.messagesReceived, 1,
                     gmlSBBaMon.messagesReceived.size());
        assertEquals("Upload message should be received by any ba, but found "
                     + gmlAnyBa.messagesReceived, 0,
                     gmlAnyBa.messagesReceived.size());
        assertEquals("No messages should be received by any ba, but found "
                     + gmlAllBa.messagesReceived, 0,
                     gmlAllBa.messagesReceived.size());
        assertEquals("No message should be replied", 0,
                     gmlHaco.messagesReceived.size());

        //Expected message
        NetarkivetMessage sentMsg = gmlSBBaMon.messagesReceived.get(0);
        assertTrue("The message should be a checksum message but was "
                   + sentMsg.getClass(), sentMsg instanceof BatchMessage);
        BatchMessage batchMsg = (BatchMessage) sentMsg;
        assertTrue("The batchMsg should be okay", batchMsg.isOk());
        assertTrue("Should be for the right sort of job",
                   batchMsg.getJob() instanceof ChecksumJob);
        ChecksumJob job = (ChecksumJob) batchMsg.getJob();
        assertEquals("Should be for the right file",
                     Pattern.quote(STORABLE_FILE.getName()),
                     job.getFilenamePattern().toString());

        //Check admin data
        ArcRepositoryEntry entry = adminData.getEntry(STORABLE_FILE.getName());
        assertEquals("Should have expected checksum",
                     MD5.generateMD5onFile(STORABLE_FILE), entry.getChecksum());
        assertEquals("Should have expected state",
                     BitArchiveStoreState.UPLOAD_STARTED,
                     entry.getGeneralStoreState().getState());
        assertEquals("Should have expected state", BitArchiveStoreState.UPLOAD_STARTED, entry.getStoreState(
                Channels.getBaMonForLocation("SB").getName()));


        //Check log for message
        LogUtils.flushLogs(ArcRepository.class.getName());
        FileAsserts.assertFileContains("Should have the log message",
                                       "Retrying store of already known file '"
                                       + STORABLE_FILE.getName()
                                       + "',"
                                       + " Already completed: "
                                       + false,
                                       LOG_FILE);
    }

    /**
     * Tests that if we call store with a file in state UPLOADED
     * a checksum job is submitted to the bitarchive.
     * Also test that state is still UPLOADED
     */
    public void testStoreUploadedFile() throws IOException {
        JMSConnectionTestMQ con = (JMSConnectionTestMQ) JMSConnectionTestMQ.getInstance();
        GenericMessageListener gmlAnyBa = new GenericMessageListener();
        con.setListener(Channels.getAnyBa(), gmlAnyBa);
        GenericMessageListener gmlAllBa = new GenericMessageListener();
        con.setListener(Channels.getAllBa(), gmlAllBa);
        GenericMessageListener gmlSBBaMon = new GenericMessageListener();
        con.setListener(Channels.getBaMonForLocation("SB"), gmlSBBaMon);
        GenericMessageListener gmlHaco = new GenericMessageListener();
        con.setListener(Channels.getThisHaco(), gmlHaco);

        //Set admin state
        UpdateableAdminData adminData = AdminData.getUpdateableInstance();
        adminData.addEntry(STORABLE_FILE.getName(), null, MD5.generateMD5onFile(
                STORABLE_FILE));
        adminData.setState(STORABLE_FILE.getName(),
                           Channels.getBaMonForLocation("SB").getName(),
                           BitArchiveStoreState.DATA_UPLOADED);

        //Store
        StoreMessage msg = new StoreMessage(Channels.getThisHaco(),
                                            STORABLE_FILE);
        JMSConnectionTestMQ.updateMsgID(msg, "Msg-id-0");
        arcRepos.store(msg.getRemoteFile(), msg);
        con.waitForConcurrentTasksToFinish();

        //Check expected outcome: empty queues
        assertEquals("No messages should be received by the bamon, but found "
                     + gmlSBBaMon.messagesReceived, 1,
                     gmlSBBaMon.messagesReceived.size());
        assertEquals("Upload message should be received by any ba, but found "
                     + gmlAnyBa.messagesReceived, 0,
                     gmlAnyBa.messagesReceived.size());
        assertEquals("No messages should be received by any ba, but found "
                     + gmlAllBa.messagesReceived, 0,
                     gmlAllBa.messagesReceived.size());
        assertEquals("No message should be replied", 0,
                     gmlHaco.messagesReceived.size());

        //Expected message
        NetarkivetMessage sentMsg = gmlSBBaMon.messagesReceived.get(0);
        assertTrue("The message should be a checksum message but was "
                   + sentMsg.getClass(), sentMsg instanceof BatchMessage);
        BatchMessage batchMsg = (BatchMessage) sentMsg;
        assertTrue("The batchMsg should be okay", batchMsg.isOk());
        assertTrue("Should be for the right sort of job",
                   batchMsg.getJob() instanceof ChecksumJob);
        ChecksumJob job = (ChecksumJob) batchMsg.getJob();
        assertEquals("Should be for the right file",
                     Pattern.quote(STORABLE_FILE.getName()),
                     job.getFilenamePattern().toString());

        //Check admin data
        ArcRepositoryEntry entry = adminData.getEntry(STORABLE_FILE.getName());
        assertEquals("Should have expected checksum",
                     MD5.generateMD5onFile(STORABLE_FILE), entry.getChecksum());
        assertEquals("Should have expected state",
                     BitArchiveStoreState.DATA_UPLOADED,
                     entry.getGeneralStoreState().getState());
        assertEquals("Should have expected state", BitArchiveStoreState.DATA_UPLOADED, entry.getStoreState(
                Channels.getBaMonForLocation("SB").getName()));


        //Check log for message
        LogUtils.flushLogs(ArcRepository.class.getName());
        FileAsserts.assertFileContains("Should have the log message",
                                       "Retrying store of already known file '"
                                       + STORABLE_FILE.getName()
                                       + "',"
                                       + " Already completed: "
                                       + false,
                                       LOG_FILE);
    }

    /**
     * Tests that if we call store with a file in state STARTED
     * a checksum job is submitted to the bitarchive.
     * Also test that state is still STARTED
     */
    public void testStoreStartedFile() throws IOException {
        JMSConnectionTestMQ con = (JMSConnectionTestMQ) JMSConnectionTestMQ.getInstance();
        GenericMessageListener gmlAnyBa = new GenericMessageListener();
        con.setListener(Channels.getAnyBa(), gmlAnyBa);
        GenericMessageListener gmlAllBa = new GenericMessageListener();
        con.setListener(Channels.getAllBa(), gmlAllBa);
        GenericMessageListener gmlSBBaMon = new GenericMessageListener();
        con.setListener(Channels.getBaMonForLocation("SB"), gmlSBBaMon);
        GenericMessageListener gmlHaco = new GenericMessageListener();
        con.setListener(Channels.getThisHaco(), gmlHaco);

        //Set admin state
        UpdateableAdminData adminData = AdminData.getUpdateableInstance();
        adminData.addEntry(STORABLE_FILE.getName(), null, MD5.generateMD5onFile(
                STORABLE_FILE));
        adminData.setState(STORABLE_FILE.getName(),
                           Channels.getBaMonForLocation("SB").getName(),
                           BitArchiveStoreState.UPLOAD_STARTED);

        //Store
        StoreMessage msg = new StoreMessage(Channels.getThisHaco(),
                                            STORABLE_FILE);
        JMSConnectionTestMQ.updateMsgID(msg, "Msg-id-0");
        arcRepos.store(msg.getRemoteFile(), msg);
        con.waitForConcurrentTasksToFinish();

        //Check expected outcome: empty queues
        assertEquals("No messages should be received by the bamon, but found "
                     + gmlSBBaMon.messagesReceived, 1,
                     gmlSBBaMon.messagesReceived.size());
        assertEquals("Upload message should be received by any ba, but found "
                     + gmlAnyBa.messagesReceived, 0,
                     gmlAnyBa.messagesReceived.size());
        assertEquals("No messages should be received by any ba, but found "
                     + gmlAllBa.messagesReceived, 0,
                     gmlAllBa.messagesReceived.size());
        assertEquals("No message should be replied", 0,
                     gmlHaco.messagesReceived.size());

        //Expected message
        NetarkivetMessage sentMsg = gmlSBBaMon.messagesReceived.get(0);
        assertTrue("The message should be a checksum message but was "
                   + sentMsg.getClass(), sentMsg instanceof BatchMessage);
        BatchMessage batchMsg = (BatchMessage) sentMsg;
        assertTrue("The batchMsg should be okay", batchMsg.isOk());
        assertTrue("Should be for the right sort of job",
                   batchMsg.getJob() instanceof ChecksumJob);
        ChecksumJob job = (ChecksumJob) batchMsg.getJob();
        assertEquals("Should be for the right file",
                     Pattern.quote(STORABLE_FILE.getName()),
                     job.getFilenamePattern().toString());

        //Check admin data
        ArcRepositoryEntry entry = adminData.getEntry(STORABLE_FILE.getName());
        assertEquals("Should have expected checksum",
                     MD5.generateMD5onFile(STORABLE_FILE), entry.getChecksum());
        assertEquals("Should have expected state",
                     BitArchiveStoreState.UPLOAD_STARTED,
                     entry.getGeneralStoreState().getState());
        assertEquals("Should have expected state", BitArchiveStoreState.UPLOAD_STARTED, entry.getStoreState(
                Channels.getBaMonForLocation("SB").getName()));


        //Check log for message
        LogUtils.flushLogs(ArcRepository.class.getName());
        FileAsserts.assertFileContains("Should have the log message",
                                       "Retrying store of already known file '"
                                       + STORABLE_FILE.getName()
                                       + "',"
                                       + " Already completed: "
                                       + false,
                                       LOG_FILE);
    }

    /**
     * Tests that if we get an OK from a bitarchive, we send a checksum job
     * to check the file.
     * Also test that state is data uploaded
     */
    public void testOnUploadMessageOK() throws IOException {
        JMSConnectionTestMQ con = (JMSConnectionTestMQ) JMSConnectionTestMQ.getInstance();
        GenericMessageListener gmlAnyBa = new GenericMessageListener();
        con.setListener(Channels.getAnyBa(), gmlAnyBa);
        GenericMessageListener gmlAllBa = new GenericMessageListener();
        con.setListener(Channels.getAllBa(), gmlAllBa);
        GenericMessageListener gmlSBBaMon = new GenericMessageListener();
        con.setListener(Channels.getBaMonForLocation("SB"), gmlSBBaMon);
        GenericMessageListener gmlHaco = new GenericMessageListener();
        con.setListener(Channels.getThisHaco(), gmlHaco);

        //Set admin state
        UpdateableAdminData adminData = AdminData.getUpdateableInstance();
        adminData.addEntry(STORABLE_FILE.getName(), null, MD5.generateMD5onFile(
                STORABLE_FILE));
        adminData.setState(STORABLE_FILE.getName(),
                           Channels.getBaMonForLocation("SB").getName(),
                           BitArchiveStoreState.UPLOAD_STARTED);

        //Deliver message
        UploadMessage msg = new UploadMessage(Channels.getAnyBa(), Channels.getTheArcrepos(),
                                              RemoteFileFactory.getInstance(
                                                      STORABLE_FILE, true, false,
                                                      true));
        JMSConnectionTestMQ.updateMsgID(msg, "Msg-id-0");
        arcRepos.onUpload(msg);
        con.waitForConcurrentTasksToFinish();

        //Check expected outcome: checksumjob
        assertEquals("'Checksum message should be received by the bamon, but found "
                     + gmlSBBaMon.messagesReceived, 1,
                     gmlSBBaMon.messagesReceived.size());
        assertEquals("Upload message should be received by any ba, but found "
                     + gmlAnyBa.messagesReceived, 0,
                     gmlAnyBa.messagesReceived.size());
        assertEquals("No messages should be received by any ba, but found "
                     + gmlAllBa.messagesReceived, 0,
                     gmlAllBa.messagesReceived.size());
        assertEquals("No message should be replied", 0,
                     gmlHaco.messagesReceived.size());

        //Expected message
        NetarkivetMessage sentMsg = gmlSBBaMon.messagesReceived.get(0);
        assertTrue("The message should be a checksum message but was "
                   + sentMsg.getClass(), sentMsg instanceof BatchMessage);
        BatchMessage batchMsg = (BatchMessage) sentMsg;
        assertTrue("The batchMsg should be okay", batchMsg.isOk());
        assertTrue("Should be for the right sort of job",
                   batchMsg.getJob() instanceof ChecksumJob);
        ChecksumJob job = (ChecksumJob) batchMsg.getJob();
        assertEquals("Should be for the right file",
                     Pattern.quote(STORABLE_FILE.getName()),
                     job.getFilenamePattern().toString());

        //Check admin data
        ArcRepositoryEntry entry = adminData.getEntry(STORABLE_FILE.getName());
        assertEquals("Should have expected checksum",
                     MD5.generateMD5onFile(STORABLE_FILE), entry.getChecksum());
        assertEquals("Should have expected state",
                     BitArchiveStoreState.DATA_UPLOADED,
                     entry.getGeneralStoreState().getState());
        assertEquals("Should have expected state", BitArchiveStoreState.DATA_UPLOADED, entry.getStoreState(
                Channels.getBaMonForLocation("SB").getName()));

    }

    /**
     * Tests that if we get a not OK from a bitarchive, we reply not OK
     * (no other bitarchive is waiting for upload replies).
     * Also test that state is upload failed
     */
    public void testOnUploadMessageNotOK() throws IOException {
        JMSConnectionTestMQ con = (JMSConnectionTestMQ) JMSConnectionTestMQ.getInstance();
        GenericMessageListener gmlAnyBa = new GenericMessageListener();
        con.setListener(Channels.getAnyBa(), gmlAnyBa);
        GenericMessageListener gmlAllBa = new GenericMessageListener();
        con.setListener(Channels.getAllBa(), gmlAllBa);
        GenericMessageListener gmlSBBaMon = new GenericMessageListener();
        con.setListener(Channels.getBaMonForLocation("SB"), gmlSBBaMon);
        GenericMessageListener gmlHaco = new GenericMessageListener();
        con.setListener(Channels.getThisHaco(), gmlHaco);

        //Set admin state
        UpdateableAdminData adminData = AdminData.getUpdateableInstance();
        adminData.addEntry(STORABLE_FILE.getName(), new StoreMessage(
                Channels.getThisHaco(), STORABLE_FILE), MD5.generateMD5onFile(
                        STORABLE_FILE));
        adminData.setState(STORABLE_FILE.getName(),
                           Channels.getBaMonForLocation("SB").getName(),
                           BitArchiveStoreState.UPLOAD_STARTED);

        //Deliver message
        UploadMessage msg = new UploadMessage(Channels.getAnyBa(), Channels.getTheArcrepos(),
                                              RemoteFileFactory.getInstance(
                                                      STORABLE_FILE, true, false,
                                                      true));
        msg.setNotOk("NOT OK!");
        JMSConnectionTestMQ.updateMsgID(msg, "Msg-id-0");
        arcRepos.onUpload(msg);
        con.waitForConcurrentTasksToFinish();

        //Check expected outcome: reply
        assertEquals("'Checksum message should be received by the bamon, but found "
                     + gmlSBBaMon.messagesReceived, 0,
                     gmlSBBaMon.messagesReceived.size());
        assertEquals("Upload message should be received by any ba, but found "
                     + gmlAnyBa.messagesReceived, 0,
                     gmlAnyBa.messagesReceived.size());
        assertEquals("No messages should be received by any ba, but found "
                     + gmlAllBa.messagesReceived, 0,
                     gmlAllBa.messagesReceived.size());
        assertEquals("One message should be replied", 1,
                     gmlHaco.messagesReceived.size());

        //Expected message
        NetarkivetMessage sentMsg = gmlHaco.messagesReceived.get(0);
        assertTrue("The message should be a store message but was "
                   + sentMsg.getClass(), sentMsg instanceof StoreMessage);
        StoreMessage storeMsg = (StoreMessage) sentMsg;
        assertFalse("The storeMsg should NOT be okay", storeMsg.isOk());
        assertEquals("Should be for the right file", STORABLE_FILE.getName(),
                     storeMsg.getArcfileName());

        //Check admin data
        ArcRepositoryEntry entry = adminData.getEntry(STORABLE_FILE.getName());
        assertEquals("Should have expected checksum",
                     MD5.generateMD5onFile(STORABLE_FILE), entry.getChecksum());
        assertEquals("Should have expected state",
                     BitArchiveStoreState.UPLOAD_FAILED,
                     entry.getGeneralStoreState().getState());
        assertEquals("Should have expected state", BitArchiveStoreState.UPLOAD_FAILED, entry.getStoreState(
                Channels.getBaMonForLocation("SB").getName()));
    }

    /**
     * Tests that a batch reply with correct checksum results in an OK message
     * (all bitarchives are OK)
     * Also test that state is completed
     */
    public void testOnBatchReplyOk() throws IOException, NoSuchFieldException,
            IllegalAccessException {
        JMSConnectionTestMQ con = (JMSConnectionTestMQ) JMSConnectionTestMQ.getInstance();
        GenericMessageListener gmlAnyBa = new GenericMessageListener();
        con.setListener(Channels.getAnyBa(), gmlAnyBa);
        GenericMessageListener gmlAllBa = new GenericMessageListener();
        con.setListener(Channels.getAllBa(), gmlAllBa);
        GenericMessageListener gmlSBBaMon = new GenericMessageListener();
        con.setListener(Channels.getBaMonForLocation("SB"), gmlSBBaMon);
        GenericMessageListener gmlHaco = new GenericMessageListener();
        con.setListener(Channels.getThisHaco(), gmlHaco);

        //Set admin state
        UpdateableAdminData adminData = AdminData.getUpdateableInstance();
        adminData.addEntry(STORABLE_FILE.getName(), new StoreMessage(
                Channels.getThisHaco(), STORABLE_FILE), MD5.generateMD5onFile(
                        STORABLE_FILE));
        adminData.setState(STORABLE_FILE.getName(),
                           Channels.getBaMonForLocation("SB").getName(),
                           BitArchiveStoreState.DATA_UPLOADED);

        //Put outstanding checksum in
        Field f = ArcRepository.class.getDeclaredField(
                "outstandingChecksumFiles");
        f.setAccessible(true);
        Map<String, String> outstandingChecksumFiles = (Map<String, String>) f.get(
                arcRepos);
        outstandingChecksumFiles.put("Msg-id-0", STORABLE_FILE.getName());

        //Deliver message
        BatchReplyMessage msg = new BatchReplyMessage(
                Channels.getTheArcrepos(), Channels.getBaMonForLocation("SB"), "Msg-id-0", 1,
                Collections.<File>emptyList(),
                RemoteFileFactory.getInstance(BATCH_RESULT, true, false, true));
        JMSConnectionTestMQ.updateMsgID(msg, "Msg-id-1");
        arcRepos.onBatchReply(msg);
        con.waitForConcurrentTasksToFinish();

        //Check expected outcome: reply
        assertEquals("'Checksum message should be received by the bamon, but found "
                     + gmlSBBaMon.messagesReceived, 0,
                     gmlSBBaMon.messagesReceived.size());
        assertEquals("Upload message should be received by any ba, but found "
                     + gmlAnyBa.messagesReceived, 0,
                     gmlAnyBa.messagesReceived.size());
        assertEquals("No messages should be received by any ba, but found "
                     + gmlAllBa.messagesReceived, 0,
                     gmlAllBa.messagesReceived.size());
        assertEquals("One message should be replied", 1,
                     gmlHaco.messagesReceived.size());

        //Expected message
        NetarkivetMessage sentMsg = gmlHaco.messagesReceived.get(0);
        assertTrue("The message should be a store message but was "
                   + sentMsg.getClass(), sentMsg instanceof StoreMessage);
        StoreMessage storeMsg = (StoreMessage) sentMsg;
        MessageAsserts.assertMessageOk("The storeMsg should be okay", storeMsg);
        assertEquals("Should be for the right file", STORABLE_FILE.getName(),
                     storeMsg.getArcfileName());

        //Check admin data
        ArcRepositoryEntry entry = adminData.getEntry(STORABLE_FILE.getName());
        assertEquals("Should have expected checksum",
                     MD5.generateMD5onFile(STORABLE_FILE), entry.getChecksum());
        assertEquals("Should have expected state",
                     BitArchiveStoreState.UPLOAD_COMPLETED,
                     entry.getGeneralStoreState().getState());
        assertEquals("Should have expected state", BitArchiveStoreState.UPLOAD_COMPLETED, entry.getStoreState(
                Channels.getBaMonForLocation("SB").getName()));
    }

    /**
     * Tests that a batch reply with un-correct checksum after an upload
     * results in a not OK message, unless some bitarchive is waiting for
     * replies
     * Also test that state is failed
     */
    public void testOnBatchReplyNotOkOnUpload() throws IOException,
            NoSuchFieldException, IllegalAccessException {
        JMSConnectionTestMQ con = (JMSConnectionTestMQ) JMSConnectionTestMQ.getInstance();
        GenericMessageListener gmlAnyBa = new GenericMessageListener();
        con.setListener(Channels.getAnyBa(), gmlAnyBa);
        GenericMessageListener gmlAllBa = new GenericMessageListener();
        con.setListener(Channels.getAllBa(), gmlAllBa);
        GenericMessageListener gmlSBBaMon = new GenericMessageListener();
        con.setListener(Channels.getBaMonForLocation("SB"), gmlSBBaMon);
        GenericMessageListener gmlHaco = new GenericMessageListener();
        con.setListener(Channels.getThisHaco(), gmlHaco);

        //Set admin state
        UpdateableAdminData adminData = AdminData.getUpdateableInstance();
        adminData.addEntry(STORABLE_FILE.getName(), new StoreMessage(
                Channels.getThisHaco(), STORABLE_FILE), MD5.generateMD5onFile(
                        STORABLE_FILE));
        adminData.setState(STORABLE_FILE.getName(),
                           Channels.getBaMonForLocation("SB").getName(),
                           BitArchiveStoreState.DATA_UPLOADED);

        //Put outstanding checksum in
        Field f = ArcRepository.class.getDeclaredField(
                "outstandingChecksumFiles");
        f.setAccessible(true);
        Map<String, String> outstandingChecksumFiles = (Map<String, String>) f.get(
                arcRepos);
        outstandingChecksumFiles.put("Msg-id-0", STORABLE_FILE.getName());

        //Deliver message
        BatchReplyMessage msg = new BatchReplyMessage(
                Channels.getTheArcrepos(), Channels.getBaMonForLocation("SB"), "Msg-id-0", 1,
                Collections.<File>emptyList(),
                RemoteFileFactory.getInstance(BATCH_RESULT_WRONG, true, false,
                                              true));
        JMSConnectionTestMQ.updateMsgID(msg, "Msg-id-1");
        arcRepos.onBatchReply(msg);
        con.waitForConcurrentTasksToFinish();

        //Check expected outcome: reply
        assertEquals("'Checksum message should be received by the bamon, but found "
                     + gmlSBBaMon.messagesReceived, 0,
                     gmlSBBaMon.messagesReceived.size());
        assertEquals("Upload message should be received by any ba, but found "
                     + gmlAnyBa.messagesReceived, 0,
                     gmlAnyBa.messagesReceived.size());
        assertEquals("No messages should be received by any ba, but found "
                     + gmlAllBa.messagesReceived, 0,
                     gmlAllBa.messagesReceived.size());
        assertEquals("One message should be replied", 1,
                     gmlHaco.messagesReceived.size());

        //Expected message
        NetarkivetMessage sentMsg = gmlHaco.messagesReceived.get(0);
        assertTrue("The message should be a store message but was "
                   + sentMsg.getClass(), sentMsg instanceof StoreMessage);
        StoreMessage storeMsg = (StoreMessage) sentMsg;
        assertFalse("The storeMsg should NOT be okay", storeMsg.isOk());
        assertEquals("Should be for the right file", STORABLE_FILE.getName(),
                     storeMsg.getArcfileName());

        //Check admin data
        ArcRepositoryEntry entry = adminData.getEntry(STORABLE_FILE.getName());
        assertEquals("Should have expected checksum",
                     MD5.generateMD5onFile(STORABLE_FILE), entry.getChecksum());
        assertEquals("Should have expected state",
                     BitArchiveStoreState.UPLOAD_FAILED,
                     entry.getGeneralStoreState().getState());
        assertEquals("Should have expected state", BitArchiveStoreState.UPLOAD_FAILED, entry.getStoreState(
                Channels.getBaMonForLocation("SB").getName()));
    }

    /**
     * Tests that a batch reply with no checksum for a file missing
     * upload reply (a retry) results in an attempt to store the file again
     * Also test that state is upload started
     */
    public void testOnBatchReplyNotOkOnRetry() throws IOException,
            NoSuchFieldException, IllegalAccessException {
        JMSConnectionTestMQ con = (JMSConnectionTestMQ) JMSConnectionTestMQ.getInstance();
        GenericMessageListener gmlAnyBa = new GenericMessageListener();
        con.setListener(Channels.getAnyBa(), gmlAnyBa);
        GenericMessageListener gmlAllBa = new GenericMessageListener();
        con.setListener(Channels.getAllBa(), gmlAllBa);
        GenericMessageListener gmlSBBaMon = new GenericMessageListener();
        con.setListener(Channels.getBaMonForLocation("SB"), gmlSBBaMon);
        GenericMessageListener gmlHaco = new GenericMessageListener();
        con.setListener(Channels.getThisHaco(), gmlHaco);

        //Set admin state
        UpdateableAdminData adminData = AdminData.getUpdateableInstance();
        adminData.addEntry(STORABLE_FILE.getName(), new StoreMessage(
                Channels.getThisHaco(), STORABLE_FILE), MD5.generateMD5onFile(
                        STORABLE_FILE));
        adminData.setState(STORABLE_FILE.getName(),
                           Channels.getBaMonForLocation("SB").getName(),
                           BitArchiveStoreState.DATA_UPLOADED);

        //Put outstanding checksum in
        Field f = ArcRepository.class.getDeclaredField(
                "outstandingChecksumFiles");
        f.setAccessible(true);
        Map<String, String> outstandingChecksumFiles = (Map<String, String>) f.get(
                arcRepos);
        outstandingChecksumFiles.put("Msg-id-0", STORABLE_FILE.getName());

        //Deliver message
        BatchReplyMessage msg = new BatchReplyMessage(
                Channels.getTheArcrepos(), Channels.getBaMonForLocation("SB"), "Msg-id-0", 0,
                Collections.<File>emptyList(),
                RemoteFileFactory.getInstance(BATCH_RESULT_EMPTY, true, false,
                                              true));
        JMSConnectionTestMQ.updateMsgID(msg, "Msg-id-1");
        arcRepos.onBatchReply(msg);
        con.waitForConcurrentTasksToFinish();

        //Check expected outcome: reply
        assertEquals("'Checksum message should be received by the bamon, but found "
                     + gmlSBBaMon.messagesReceived, 0,
                     gmlSBBaMon.messagesReceived.size());
        assertEquals("Upload message should be received by any ba, but found "
                     + gmlAnyBa.messagesReceived, 0,
                     gmlAnyBa.messagesReceived.size());
        assertEquals("No messages should be received by any ba, but found "
                     + gmlAllBa.messagesReceived, 0,
                     gmlAllBa.messagesReceived.size());
        assertEquals("One message should be replied", 1,
                     gmlHaco.messagesReceived.size());

        //Expected message
        NetarkivetMessage sentMsg = gmlHaco.messagesReceived.get(0);
        assertTrue("The message should be a store message but was "
                   + sentMsg.getClass(), sentMsg instanceof StoreMessage);
        StoreMessage storeMsg = (StoreMessage) sentMsg;
        assertFalse("The storeMsg should NOT be okay", storeMsg.isOk());
        assertEquals("Should be for the right file", STORABLE_FILE.getName(),
                     storeMsg.getArcfileName());

        //Check admin data
        ArcRepositoryEntry entry = adminData.getEntry(STORABLE_FILE.getName());
        assertEquals("Should have expected checksum",
                     MD5.generateMD5onFile(STORABLE_FILE), entry.getChecksum());
        assertEquals("Should have expected state",
                     BitArchiveStoreState.UPLOAD_FAILED,
                     entry.getGeneralStoreState().getState());
        assertEquals("Should have expected state", BitArchiveStoreState.UPLOAD_FAILED, entry.getStoreState(
                Channels.getBaMonForLocation("SB").getName()));
    }

    //TODO: Check that tests are exhaustive, and check more than one BA
}
