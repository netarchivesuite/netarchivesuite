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
package dk.netarkivet.archive.bitarchive.distribute;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.net.ftp.FTPClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.archive.ArchiveSettings;
import dk.netarkivet.archive.distribute.ArchiveMessageHandler;
import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.distribute.ChannelID;
import dk.netarkivet.common.distribute.Channels;
import dk.netarkivet.common.distribute.FTPRemoteFile;
import dk.netarkivet.common.distribute.JMSConnection;
import dk.netarkivet.common.distribute.JMSConnectionFactory;
import dk.netarkivet.common.distribute.JMSConnectionMockupMQ;
import dk.netarkivet.common.distribute.NetarkivetMessage;
import dk.netarkivet.common.distribute.RemoteFile;
import dk.netarkivet.common.distribute.RemoteFileFactory;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.RememberNotifications;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.utils.batch.ChecksumJob;
import dk.netarkivet.testutils.FileAsserts;
import dk.netarkivet.testutils.TestFileUtils;
import dk.netarkivet.testutils.TestMessageListener;
import dk.netarkivet.testutils.preconfigured.ReloadSettings;

/**
 * Test bitarchive client and server As a number of tests only succeed if both the client and server both operate
 * correctly, both are tested together.
 */
@Ignore("Needs to be run in deploy-test module according to junit 3 test suite.")
public class IntegrityTests {

    // A named logger for this class is retrieved
    protected final Logger logger = LoggerFactory.getLogger(IntegrityTests.class);

    private static final String ARC_FILE_NAME = "Upload5.ARC";
    private static final File TEST_DIR = new File("tests/dk/netarkivet/archive/bitarchive/distribute/data/");
    private static final File ORIGINALS_DIR = new File(TEST_DIR, "originals");
    private static final File WORKING_DIR = new File(TEST_DIR, "working");
    private static final File BITARCHIVE_DIR = new File(WORKING_DIR, "bitarchive1");
    private static final File SERVER_DIR = new File(WORKING_DIR, "server");
    private static final File LOCAL_FILES_DIR = new File(WORKING_DIR, "local_files");
    private static final File FILE_TO_UPLOAD = new File(LOCAL_FILES_DIR, ARC_FILE_NAME);
    static final String BITARCHIVE_CREDENTIALS = "42";
    private static final int LARGE_MESSAGE_COUNT = 10;
    private FTPClient theFTPClient;
    private ArrayList<String> upLoadedFiles = new ArrayList<String>();
    private static final String FILENAME_TO_GET = "Upload4.ARC";

    private static final File BASEDIR = new File("tests/dk/netarkivet/archive/bitarchive/distribute/data");
    private static final File ORIGINALS = new File(BASEDIR, "originals");
    private static final File WORKING = new File(BASEDIR, "working");

    static final ChannelID THE_BAMON = Channels.getTheBamon();
    private static final ChannelID ALL_BA = Channels.getAllBa();
    private static final ChannelID ANY_BA = Channels.getAnyBa();

    /* The client and server used for testing */
    BitarchiveClient bac;
    BitarchiveServer bas;
    BitarchiveMonitorServer bam;
    ReloadSettings rs = new ReloadSettings();

    @Before
    public void setUp() {
        rs.setUp();
        // new UseTestRemoteFile().setUp();

        Settings.set(ArchiveSettings.BITARCHIVE_BATCH_JOB_TIMEOUT, String.valueOf(2000));

        FileUtils.removeRecursively(WORKING);
        TestFileUtils.copyDirectoryNonCVS(ORIGINALS, WORKING);

        try {
            if (!TestInfo.UPLOADMESSAGE_TEMP_DIR.exists()) {
                TestInfo.UPLOADMESSAGE_TEMP_DIR.mkdirs();
            }

            FileUtils.removeRecursively(TestInfo.UPLOADMESSAGE_TEMP_DIR);

            TestFileUtils.copyDirectoryNonCVS(TestInfo.UPLOADMESSAGE_ORIGINALS_DIR, TestInfo.UPLOADMESSAGE_TEMP_DIR);
        } catch (Exception e) {
            fail("Could not setup configuration:" + e);
        }

        /** Read ftp-related settings from settings.xml. */
        final String ftpServerName = Settings.get(CommonSettings.FTP_SERVER_NAME);
        final int ftpServerPort = Integer.parseInt(Settings.get(CommonSettings.FTP_SERVER_PORT));
        final String ftpUserName = Settings.get(CommonSettings.FTP_USER_NAME);
        final String ftpUserPassword = Settings.get(CommonSettings.FTP_USER_PASSWORD);

        /** Connect to test ftp-server. */
        theFTPClient = new FTPClient();

        /*
         * try { theFTPClient.setFileType(FTPClient.BINARY_FILE_TYPE); } catch (IOException e) { throw new
         * IOFailure("Unable to set Transfer mode: " + e); }
         */

        try {
            theFTPClient.connect(ftpServerName, ftpServerPort);
            theFTPClient.login(ftpUserName, ftpUserPassword);
            theFTPClient.setFileType(FTPClient.BINARY_FILE_TYPE);
        } catch (SocketException e) {
            throw new IOFailure("Connect to " + ftpServerName + " failed", e.getCause());
        } catch (IOException e) {
            throw new IOFailure("Connect to " + ftpServerName + " failed", e.getCause());
        }

        JMSConnectionMockupMQ.useJMSConnectionMockupMQ();

        TestFileUtils.copyDirectoryNonCVS(ORIGINALS_DIR, WORKING_DIR);

        bac = BitarchiveClient.getInstance(ALL_BA, ANY_BA, THE_BAMON);

        Settings.set(ArchiveSettings.BITARCHIVE_SERVER_FILEDIR, BITARCHIVE_DIR.getAbsolutePath());
        Settings.set(CommonSettings.DIR_COMMONTEMPDIR, SERVER_DIR.getAbsolutePath());
        bas = BitarchiveServer.getInstance();
        bam = BitarchiveMonitorServer.getInstance();

        /** Do not send notification by email. Print them to STDOUT. */
        Settings.set(CommonSettings.NOTIFICATIONS_CLASS, RememberNotifications.class.getName());
    }

    /**
     * After test is done, remove the "archive".
     */
    @After
    public void tearDown() {

        FileUtils.removeRecursively(WORKING);
        /** delete all uploaded files on ftp-server and then disconnect. */
        Iterator<String> fileIterator = upLoadedFiles.iterator();

        try {
            while (fileIterator.hasNext()) {
                String currentUploadedFile = (String) fileIterator.next();

                if (currentUploadedFile != null) {
                    if (!theFTPClient.deleteFile(currentUploadedFile)) {
                        logger.warn("deleteFile operation failed on {}. Reply from ftpserver: {}", currentUploadedFile,
                                theFTPClient.getReplyString());
                    }
                }
            }

            if (!theFTPClient.logout()) {
                logger.warn("logout operation failed. Reply from ftp-server: {}", theFTPClient.getReplyString());
            }

            theFTPClient.disconnect();
        } catch (IOException e) {
            // throw new IOFailure("disconnect failed", e.getCause());
            e.printStackTrace();
        }

        FileUtils.removeRecursively(TestInfo.UPLOADMESSAGE_TEMP_DIR);
        bas.close();
        bac.close();
        bam.close();
        FileUtils.removeRecursively(WORKING_DIR);
        rs.tearDown();
    }

    /**
     * Test that monitor can receive and aggregate data from more than one BitarchiveServer and aggregate the data and
     * upload it via FTPRemoteFile.
     */
    @Test
    @Ignore("FIXME")
    // FIXME: test temporarily disabled
    public void testBatchEndedMessageAggregation() throws InterruptedException, IOException {
        Settings.set(CommonSettings.REMOTE_FILE_CLASS, FTPRemoteFile.class.getName());
        bas.close();
        JMSConnection con = JMSConnectionFactory.getInstance();

        // Set up a listener on the reply queue for batch messages
        TestMessageListener listener = new TestMessageListener();
        con.setListener(Channels.getTheRepos(), listener);
        con.setListener(Channels.getAllBa(), listener);

        // File for reply
        File output_file = new File(WORKING, "batch_output.txt");

        // Create a batch message
        BatchMessage bm = new BatchMessage(Channels.getTheBamon(), Channels.getTheRepos(), new ChecksumJob(),
                Settings.get(CommonSettings.USE_REPLICA_ID));
        JMSConnectionMockupMQ.updateMsgID(bm, "testmsgid0");

        // Invent two BitarchiveServers and send heartbeats from them
        String baID1 = "BA1";
        HeartBeatMessage hbm = new HeartBeatMessage(Channels.getTheBamon(), baID1);
        JMSConnectionMockupMQ.updateMsgID(hbm, "heartbeat1");
        String baID2 = "BA2";
        HeartBeatMessage hbm2 = new HeartBeatMessage(Channels.getTheBamon(), baID2);
        JMSConnectionMockupMQ.updateMsgID(hbm2, "heartbeat2");
        bam.visit(hbm);
        bam.visit(hbm2);

        // Trigger the bams with the batch message
        bam.visit(bm);
        ((JMSConnectionMockupMQ) con).waitForConcurrentTasksToFinish();

        // Now pick up the forwarded Batch message and get its ID
        String forwardedID = ((BatchMessage) listener.getAllReceived().get(0)).getID();

        // Now invent two BatchEndedMessages from the two mythical
        // BitarchiveServers
        File data1 = new File(WORKING, "batch_output_1.txt");
        File data2 = new File(WORKING, "batch_output_2.txt");
        RemoteFile rf1 = RemoteFileFactory.getInstance(data1, true, false, true);
        BatchEndedMessage bem1 = new BatchEndedMessage(Channels.getTheBamon(), baID1, forwardedID, rf1);
        RemoteFile rf2 = RemoteFileFactory.getInstance(data2, true, false, true);
        BatchEndedMessage bem2 = new BatchEndedMessage(Channels.getTheBamon(), baID2, forwardedID, rf2);

        JMSConnectionMockupMQ.updateMsgID(bem1, "testmsgid1");
        bam.visit(bem1);
        JMSConnectionMockupMQ.updateMsgID(bem2, "testmsgid2");
        bam.visit(bem2);

        Thread.sleep(3000);
        ((JMSConnectionMockupMQ) con).waitForConcurrentTasksToFinish();

        // Now there should also one BatchReplyMessage in the listener and the
        // monitor should have written the data to a remote file we can collect
        assertEquals("Should have received exactly two messages, but got " + listener.getAllReceived(), 2,
                listener.getNumReceived());
        for (NetarkivetMessage message : listener.getAllReceived()) {
            if (message instanceof BatchReplyMessage) {
                ((BatchReplyMessage) message).getResultFile().copyTo(output_file);
            }
        }

        FileAsserts.assertFileNumberOfLines(
                "Aggregated file should have two " + "lines. Content is: " + FileUtils.readFile(output_file),
                output_file, 2);
        FileAsserts.assertFileContains("File contents not as expected", "1", output_file);
        FileAsserts.assertFileContains("File contents not as expected", "2", output_file);

        // To be extra careful, check that the file length in bytes is as
        // expected
        int expected_length = FileUtils.readBinaryFile(data1).length + FileUtils.readBinaryFile(data2).length;
        int actual_length = FileUtils.readBinaryFile(output_file).length;
        assertEquals("File length (in bytes) is not as expected", expected_length, actual_length);
        // Check that the remote files have been deleted on the server
        File a_file = new File(WORKING, "dummy.txt");
        try {
            rf1.copyTo(a_file);
            fail("File should have been deleted from server: " + rf1.toString());
        } catch (IOFailure e) {
            // expected
        }
        try {
            rf2.copyTo(a_file);
            fail("File should have been deleted from server: " + rf2.toString());
        } catch (IOFailure e) {
            // expected
        }
        con.removeListener(Channels.getTheRepos(), listener);
        con.removeListener(Channels.getAllBa(), listener);
    }

    /**
     * Verify that multiple messages sent almost simultaneously works.
     */
    @Test
    @Ignore("FIXME")
    // FIXME: test temporarily disabled
    public void testLotsOfMessages() {
        MessageTestHandler handler = new MessageTestHandler();
        JMSConnectionFactory.getInstance().setListener(Channels.getTheRepos(), handler);
        JMSConnectionFactory.getInstance().setListener(Channels.getThisReposClient(), handler);

        assertTrue("File to upload must exist: " + FILE_TO_UPLOAD, FILE_TO_UPLOAD.exists());

        int beforeCount = Thread.activeCount();

        for (int i = 0; i < LARGE_MESSAGE_COUNT; ++i) {
            // System.out.println("Sending message #" + i);
            bac.get(FILENAME_TO_GET, 0);
            bac.sendUploadMessage(RemoteFileFactory.getInstance(FILE_TO_UPLOAD, true, false, true), "dummy-precomputed-checksum"); // only
            // first
            // upload
            // will
            // succeed
            BatchMessage bMsg = new BatchMessage(THE_BAMON, Channels.getThisReposClient(), new TestBatchJobRuns(),
                    Settings.get(CommonSettings.USE_REPLICA_ID));
            bac.sendBatchJob(bMsg);
            RemoveAndGetFileMessage rMsg = new RemoveAndGetFileMessage(Channels.getTheRepos(),
                    Channels.getThisReposClient(), FILENAME_TO_GET, "ONE", "FFFF", "42");
            bac.sendRemoveAndGetFileMessage(rMsg);
        }
        // System.out.println("Sending messages done");
        System.out.println("Sleeping until active threads are equal to " + beforeCount);
        long maxAllowedExecutionTime = 300000; // Only run this test for max. 5
        // minutes.
        long starttime = System.currentTimeMillis();
        while (Thread.activeCount() > beforeCount && System.currentTimeMillis() < starttime + maxAllowedExecutionTime) {
            // System.out.println("Active count:" + Thread.activeCount());
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
        }
        if (Thread.activeCount() > beforeCount) {
            fail("Should only be " + beforeCount + ", but was " + Thread.activeCount());
        }
        System.out.println("Waiting for concurrent tasks to finish..");
        ((JMSConnectionMockupMQ) JMSConnectionFactory.getInstance()).waitForConcurrentTasksToFinish();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            // ignore
        }
        System.out.println("Waiting for concurrent tasks to finish..");
        ((JMSConnectionMockupMQ) JMSConnectionFactory.getInstance()).waitForConcurrentTasksToFinish();

        // assertEquals("Number of messages received by the server", 4 *
        // LARGE_MESSAGE_COUNT, bas.getCountMessages());

        assertEquals("Expecting " + LARGE_MESSAGE_COUNT + " get messages, but got " + handler.getMsg,
                LARGE_MESSAGE_COUNT, handler.getMsg.size());
        assertEquals("Expecting " + LARGE_MESSAGE_COUNT + " upload messages, but got " + handler.uploadMsg,
                LARGE_MESSAGE_COUNT, handler.uploadMsg.size());
        assertEquals("Expecting " + LARGE_MESSAGE_COUNT + " correct messages, but got " + handler.removeMsg,
                LARGE_MESSAGE_COUNT, handler.removeMsg.size());
        assertEquals("Expecting " + LARGE_MESSAGE_COUNT + " batch reply messages, but got " + handler.batchReplyMsg,
                LARGE_MESSAGE_COUNT, handler.batchReplyMsg.size());
        assertEquals("Expecting to receive all messages", LARGE_MESSAGE_COUNT * 4, handler.getTotalCount());

        // all get's expected to succeed
        int countok = 0;

        for (int i = 0; i < LARGE_MESSAGE_COUNT; ++i) {
            GetMessage msg = handler.getMsg.get(i);

            if (msg.isOk()) {
                ++countok;
            }
        }

        assertEquals("Expected number of correct get messages, but got " + handler.getMsg, LARGE_MESSAGE_COUNT, countok);

        // one upload expected to succeed
        countok = 0;

        for (int i = 0; i < LARGE_MESSAGE_COUNT; ++i) {
            UploadMessage msg = handler.uploadMsg.get(i);

            if (msg.isOk()) {
                ++countok;
            }
        }

        assertEquals("Expected number of correct upload messages, but got " + handler.uploadMsg, 1, countok);

        // all batches expected to succeed
        countok = 0;

        for (int i = 0; i < LARGE_MESSAGE_COUNT; ++i) {
            BatchReplyMessage msg = handler.batchReplyMsg.get(i);

            if (msg.isOk()) {
                ++countok;
            }
        }

        assertEquals("Expected number of correct BatchReply messages, but got " + handler.batchReplyMsg,
                LARGE_MESSAGE_COUNT, countok);

        // all correct's expected to fail
        countok = 0;

        for (int i = 0; i < LARGE_MESSAGE_COUNT; ++i) {
            RemoveAndGetFileMessage msg = handler.removeMsg.get(i);

            if (!msg.isOk()) {
                ++countok;
            }
        }

        assertEquals("Expected number of failed remove messages, but got " + handler.removeMsg, LARGE_MESSAGE_COUNT,
                countok);

    }

    /**
     * Test construction of UploadMessage.
     */
    @Test
    @Ignore("FIXME")
    // FIXME: test temporarily disabled
    public void testConstruction() {
        ChannelID to = Channels.getAllBa();
        ChannelID reply = Channels.getThisReposClient();
        File testARCFile = TestInfo.UPLOADMESSAGE_TESTFILE_1;
        long fileSize = testARCFile.length();

        try {
            /** Upload testfile to ftp-server. */
            InputStream in = new FileInputStream(testARCFile);
            assertTrue("Store operation failed", theFTPClient.storeFile(testARCFile.getName(), in));
            in.close();
            upLoadedFiles.add(testARCFile.getName());
            logger.debug("testConstruction: Storing file '{}' on ftp-server", testARCFile.getName());
            UploadMessage um = new UploadMessage(to, reply, RemoteFileFactory.getInstance(testARCFile, true, false,
                    true));
            assertTrue("uploadMessage should not be null", um != null);
            upLoadedFiles.add(testARCFile.getName());
        } catch (IOException e) {
            throw new IOFailure("Creation of UploadMessage failed", e);
        }

        /**
         * test, if original ARC file still exists, and has the same size as before
         */
        assertEquals("Test-file has been modified!!", fileSize, testARCFile.length());
    }

    /* Receive and check messages */
    public class MessageTestHandler extends ArchiveMessageHandler {
        public List<UploadMessage> uploadMsg = new ArrayList<UploadMessage>();
        public List<GetMessage> getMsg = new ArrayList<GetMessage>();
        public List<RemoveAndGetFileMessage> removeMsg = new ArrayList<RemoveAndGetFileMessage>();
        public List<BatchMessage> batchMsg = new ArrayList<BatchMessage>();
        public List<BatchReplyMessage> batchReplyMsg = new ArrayList<BatchReplyMessage>();
        public List<GetFileMessage> getfileMsg = new ArrayList<GetFileMessage>();

        public MessageTestHandler() {
        }

        synchronized public void visit(UploadMessage msg) {
            uploadMsg.add(msg);
        }

        synchronized public void visit(GetMessage msg) {
            getMsg.add(msg);
        }

        synchronized public void visit(GetFileMessage msg) {
            getfileMsg.add(msg);
        }

        synchronized public void visit(RemoveAndGetFileMessage msg) {
            removeMsg.add(msg);
        }

        synchronized public void visit(BatchMessage msg) {
            batchMsg.add(msg);
        }

        synchronized public void visit(BatchReplyMessage msg) {
            batchReplyMsg.add(msg);
        }

        synchronized public int getTotalCount() {
            return (uploadMsg.size() + getMsg.size() + removeMsg.size() + batchMsg.size() + getfileMsg.size()
                    + batchReplyMsg.size());
        }
    }
}
