/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2010 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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

package dk.netarkivet.archive.bitarchive.distribute;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.logging.LogManager;

import javax.jms.Message;
import javax.jms.MessageListener;

import junit.framework.TestCase;
import dk.netarkivet.archive.ArchiveSettings;
import dk.netarkivet.archive.arcrepository.bitpreservation.ChecksumJob;
import dk.netarkivet.archive.bitarchive.BitarchiveApplication;
import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.distribute.ChannelID;
import dk.netarkivet.common.distribute.Channels;
import dk.netarkivet.common.distribute.JMSConnection;
import dk.netarkivet.common.distribute.JMSConnectionFactory;
import dk.netarkivet.common.distribute.JMSConnectionMockupMQ;
import dk.netarkivet.common.distribute.NetarkivetMessage;
import dk.netarkivet.common.distribute.RemoteFile;
import dk.netarkivet.common.distribute.RemoteFileFactory;
import dk.netarkivet.common.distribute.TestRemoteFile;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.RememberNotifications;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.testutils.ClassAsserts;
import dk.netarkivet.testutils.FileAsserts;
import dk.netarkivet.testutils.LogUtils;
import dk.netarkivet.testutils.MessageAsserts;
import dk.netarkivet.testutils.ReflectUtils;
import dk.netarkivet.testutils.TestFileUtils;
import dk.netarkivet.testutils.preconfigured.PreserveStdStreams;
import dk.netarkivet.testutils.preconfigured.PreventSystemExit;
import dk.netarkivet.testutils.preconfigured.ReloadSettings;
import dk.netarkivet.testutils.preconfigured.UseTestRemoteFile;

/**
 * Unit tests for the BitarchiveServer class.
 */
public class BitarchiveServerTester extends TestCase {
    private UseTestRemoteFile utrf = new UseTestRemoteFile();

    BitarchiveServer bas;

    private static final File WORKING = TestInfo.UPLOADMESSAGE_TEMP_DIR;
    private static final File BITARCHIVE1 = TestInfo.BA1_MAINDIR;
    private static final File SERVER1 = TestInfo.SERVER1_DIR;
    private static final String[] dirs = {
            WORKING.getAbsolutePath()+"m_bitarchive",
            WORKING.getAbsolutePath()+"n_bitarchive",
            WORKING.getAbsolutePath()+"o_bitarchive",
            WORKING.getAbsolutePath()+"p_bitarchive",
    };
    ReloadSettings rs = new ReloadSettings();

    protected void setUp() throws IOException {
        rs.setUp();
        JMSConnectionMockupMQ.useJMSConnectionMockupMQ();
        FileInputStream fis = new FileInputStream(TestInfo.TESTLOGPROP);
        LogManager.getLogManager().
                readConfiguration(fis);
        fis.close();
        utrf.setUp();
        File tmpdir = new File(TestInfo.UPLOADMESSAGE_TEMP_DIR, 
                "commontempdir");
        FileUtils.removeRecursively(WORKING);
        TestFileUtils.copyDirectoryNonCVS(
                TestInfo.UPLOADMESSAGE_ORIGINALS_DIR, WORKING);
        Settings.set(CommonSettings.NOTIFICATIONS_CLASS, RememberNotifications.class.getName());
        Settings.set(ArchiveSettings.BITARCHIVE_SERVER_FILEDIR, dirs);
        Settings.set(CommonSettings.DIR_COMMONTEMPDIR,
                tmpdir.getAbsolutePath());
    }

    protected void tearDown() {
        if (bas != null) {
            bas.close();
        }
        FileUtils.removeRecursively(WORKING);
        JMSConnectionMockupMQ.clearTestQueues();
        utrf.tearDown();
        for (String dir: dirs) {
            FileUtils.removeRecursively(new File(dir));
        }
        RememberNotifications.resetSingleton();
        rs.tearDown();
    }

    /**
     * Test that BitarchiveServer is a singleton.
     */
    public void testSingletonicity() {
        Settings.set(ArchiveSettings.BITARCHIVE_SERVER_FILEDIR, dirs);
        ClassAsserts.assertSingleton(BitarchiveServer.class);
    }


    /**
     * Test that the BitarchiveServer outputs logging information.
     * This verifies the fix of bug #99.
     *
     * @throws IOException If unable to read the logfile.
     */
    public void testLogging() throws IOException {
        Settings.set(ArchiveSettings.BITARCHIVE_SERVER_FILEDIR, TestInfo.BITARCHIVE_APP_DIR_1);
        Settings.set(CommonSettings.DIR_COMMONTEMPDIR, TestInfo.BITARCHIVE_SERVER_DIR_1);
        bas = BitarchiveServer.getInstance();
        bas.close();
        LogUtils.flushLogs("BitarchiveServer");
        String log = FileUtils.readFile(TestInfo.LOG_FILE);
        assertFalse("Should have non-empty log", log.equals(""));
        FileAsserts.assertFileContains(
                "Log should show bitarchive server created",
                "Created bitarchive server", TestInfo.LOG_FILE);
    }

    /**
     * Test that a BitarchiveServer is removed as listener of the ANY_BA queue
     * when trying to upload a file that cannot fit in the archive.
     *
     * We currently don't resend the message, but just reply.
     * @throws InterruptedException 
     */
    public void testVisitUploadMessage() throws InterruptedException {
        SERVER1.mkdirs();

        // Set to just over the minimum size guaranteed.        
        Settings.set(ArchiveSettings.BITARCHIVE_MIN_SPACE_LEFT, "" + (FileUtils.getBytesFree(SERVER1) - 12000));
        Settings.set(CommonSettings.DIR_COMMONTEMPDIR, SERVER1.getAbsolutePath());
        Settings.set(ArchiveSettings.BITARCHIVE_SERVER_FILEDIR, BITARCHIVE1.getAbsolutePath());

        bas = BitarchiveServer.getInstance();
        ChannelID arcReposQ = Channels.getTheRepos();
        ChannelID anyBa = Channels.getAnyBa();
        JMSConnectionMockupMQ conn = (JMSConnectionMockupMQ) JMSConnectionFactory
                .getInstance();

        GenericMessageListener listener = new GenericMessageListener();
        conn.setListener(arcReposQ, listener);

        // Check if BitarchiveServer bas1 is removed as listener on ANY_BA queue:
        int expectedListeners = 1;
        assertEquals("Number of listeners on queue " + anyBa + " should be "
                + expectedListeners + " before upload.",
                expectedListeners, conn.getListeners(anyBa).size());

        File testFile = TestInfo.UPLOADMESSAGE_TESTFILE_1;
        RemoteFile rf = RemoteFileFactory.getInstance(
                testFile, false, false, true);
        UploadMessage msg = new UploadMessage(anyBa, arcReposQ, rf);
        JMSConnectionMockupMQ.updateMsgID(msg, "upload1");

        bas.visit(msg);

        conn.waitForConcurrentTasksToFinish();
        
        expectedListeners = 0;
        assertEquals("Number of listeners on queue " + anyBa
                + " should be " + expectedListeners + " after upload.",
                expectedListeners, conn.getListeners(anyBa).size());

        // Check that UploadMessage has been replied to arcrepos queue.
        // It should have been received by GenericMessageListener:
        assertTrue("Should have received at least one message on arcRepos q",
                listener.messagesReceived.size() >= 1);

        assertEquals("Reposted message should be identical to original "
                + "UploadMessage.",
                msg, listener.messagesReceived.get(0));

    }

    /**
     * Test that we don't listen on ANY_BA if we are out of space.
     */
    public void testCTor() {
        // Set to just over the minimum size guaranteed.
        Settings.set(ArchiveSettings.BITARCHIVE_SERVER_FILEDIR, dirs);
        Settings.set(ArchiveSettings.BITARCHIVE_MIN_SPACE_LEFT, "" + (FileUtils.getBytesFree(WORKING) + 1));

        bas = BitarchiveServer.getInstance();
        ChannelID anyBa = Channels.getAnyBa();
        JMSConnectionMockupMQ conn = (JMSConnectionMockupMQ) JMSConnectionFactory
                .getInstance();
        assertEquals("We should not listen to " + anyBa
                + " if we are out of space",
                0, conn.getListeners(anyBa).size());
        LogUtils.flushLogs(BitarchiveServer.class.getName());
        FileAsserts.assertFileContains("Log file should have warning about "
                + "having no space",
                "WARNING", TestInfo.LOG_FILE);
    }

    /**
     * Test that a BitarchiveServer is removed as listener of the ANY_BA queue
     * when a directory disappears.
     *
     * We currently don't resend the message, but just reply.
     */
    public void testVisitUploadMessageDiskcrash() {
        // Set to just over the minimum size guaranteed.
        Settings.set(CommonSettings.DIR_COMMONTEMPDIR, SERVER1.getAbsolutePath());
        Settings.set(ArchiveSettings.BITARCHIVE_SERVER_FILEDIR, BITARCHIVE1.getAbsolutePath());

        bas = BitarchiveServer.getInstance();
        ChannelID arcReposQ = Channels.getTheRepos();
        ChannelID anyBa = Channels.getAnyBa();
        JMSConnectionMockupMQ conn = (JMSConnectionMockupMQ) JMSConnectionFactory
                .getInstance();

        GenericMessageListener listener = new GenericMessageListener();
        conn.setListener(arcReposQ, listener);

        int expectedListeners = 1;
        assertEquals("Number of listeners on queue " + anyBa + " should be " 
                + expectedListeners + " before upload.",
                expectedListeners, conn.getListeners(anyBa).size());

        File testFile = TestInfo.UPLOADMESSAGE_TESTFILE_1;
        RemoteFile rf = TestRemoteFile.getInstance(
                testFile, false, false, true);
        UploadMessage msg = new UploadMessage(anyBa, arcReposQ, rf);
        JMSConnectionMockupMQ.updateMsgID(msg, "upload1");

        bas.visit(msg);
        conn.waitForConcurrentTasksToFinish();
        expectedListeners = 1;
        assertEquals("Number of listeners on queue " + anyBa
                + " should still be " + expectedListeners + " after upload.",
                expectedListeners, conn.getListeners(anyBa).size());

        // Check that UploadMessage has been replied to arcrepos queue.
        // It should have been received by GenericMessageListener:
        assertTrue("Should have received at least one messages on arcRepos q",
                listener.messagesReceived.size() >= 1);

        //Now crash the disk
        FileUtils.removeRecursively(BITARCHIVE1);

        bas.visit(msg);
        conn.waitForConcurrentTasksToFinish();
        expectedListeners = 0;
        assertEquals("Number of listeners on queue " + anyBa + " should be "
                + expectedListeners + " after upload fail.",
                expectedListeners, conn.getListeners(anyBa).size());

        // Check that UploadMessage has been replied to arcrepos queue.
        // It should have been received by GenericMessageListener:
        assertTrue("Should have received at least two messages on arcRepos q",
                listener.messagesReceived.size() >= 2);
    }

    public void testListenerNotRemovedOnErrors() {
        bas = BitarchiveServer.getInstance();
        ChannelID arcReposQ = Channels.getTheRepos();
        ChannelID anyBa = Channels.getAnyBa();
        JMSConnectionMockupMQ conn = (JMSConnectionMockupMQ) JMSConnectionFactory
                .getInstance();

        GenericMessageListener listener = new GenericMessageListener();
        conn.setListener(arcReposQ, listener);

        int expectedListeners = 1;
        assertEquals("Number of listeners on queue " + anyBa 
                + " should be " + expectedListeners + " before upload.",
                expectedListeners, conn.getListeners(anyBa).size());

        File testFile = TestInfo.UPLOADMESSAGE_TESTFILE_1;
        RemoteFile rf = TestRemoteFile.getInstance(
                testFile, false, false, true);
        ((TestRemoteFile) rf).failsOnCopy = true;
        UploadMessage msg = new UploadMessage(anyBa, arcReposQ, rf);
        JMSConnectionMockupMQ.updateMsgID(msg, "upload1");

        bas.visit(msg);
        conn.waitForConcurrentTasksToFinish();

        assertEquals("Number of listeners on queue " + anyBa
                + " should still be " + expectedListeners + " after upload.",
                expectedListeners, conn.getListeners(anyBa).size());

        // Check that UploadMessage has been replied to arcrepos queue.
        // It should have been received by GenericMessageListener:
        assertTrue("Should have received at least one message on arcRepos q",
                listener.messagesReceived.size() >= 1);

        assertEquals("Reposted message should be identical to original UploadMessage.",
                msg, listener.messagesReceived.get(0));

        assertFalse("The message reposted should not be okay",
                   ((NetarkivetMessage) 
                           listener.messagesReceived.get(0)).isOk());
    }


    /**
     * Test the normal operation of getting a record of a file which is present.
     */
    public void testVisitGetMessage() {
        Settings.set(ArchiveSettings.BITARCHIVE_SERVER_FILEDIR, BITARCHIVE1.getAbsolutePath());
        Settings.set(CommonSettings.DIR_COMMONTEMPDIR, SERVER1.getAbsolutePath());
        bas = BitarchiveServer.getInstance();
        GenericMessageListener listener = new GenericMessageListener();
        JMSConnectionMockupMQ con = (JMSConnectionMockupMQ) JMSConnectionFactory
                .getInstance();
        con.setListener(Channels.getTheRepos(), listener);
        // Construct a get message for a file in the bitarchive
        final long arcfileOffset = 3L;
        GetMessage msg = new GetMessage(Channels.getAllBa(),
                Channels.getTheRepos(), "NetarchiveSuite-upload1.arc",
                arcfileOffset);
        JMSConnectionMockupMQ.updateMsgID(msg, "AnId");
        bas.visit(msg);
        con.waitForConcurrentTasksToFinish();
        // Should now be one reply message in the listener, containing the
        // requested arc record
        assertEquals("Should have received exactly one message", 1,
                listener.messagesReceived.size());
        GetMessage replyMsg = (GetMessage) listener.messagesReceived.get(0);
        assertTrue("Reply message should be ok", replyMsg.isOk());
        assertTrue("Reply should contain non-trivial amount of data",
                replyMsg.getRecord().getLength() > 1);
    }

    /**
     * Test the normal operation of trying to get a record of a file which is
     * not present on this bitarchive.
     */
    public void testVisitGetMessageNoSuchFile() {
        Settings.set(ArchiveSettings.BITARCHIVE_SERVER_FILEDIR, BITARCHIVE1.getAbsolutePath());
        Settings.set(CommonSettings.DIR_COMMONTEMPDIR, SERVER1.getAbsolutePath());
        bas = BitarchiveServer.getInstance();
        GenericMessageListener listener = new GenericMessageListener();
        JMSConnectionMockupMQ con = (JMSConnectionMockupMQ) JMSConnectionFactory
                .getInstance();
        con.setListener(Channels.getTheRepos(), listener);
        // Construct a get message for a file in the bitarchive
        final long arcfileOffset = 3L;
        GetMessage msg = new GetMessage(Channels.getAllBa(),
                Channels.getTheRepos(), "Upload2.ARC", arcfileOffset);
        JMSConnectionMockupMQ.updateMsgID(msg, "AnId");
        bas.visit(msg);
        con.waitForConcurrentTasksToFinish();
        // Should now be no messages in listener
        assertEquals("Should have received no messages", 0,
                listener.messagesReceived.size());
    }


    /**
     * Test getting an arcrecord of a file which exists but a record which does
     * not.
     */
    public void testVisitGetMessageNoSuchRecord() {
        Settings.set(ArchiveSettings.BITARCHIVE_SERVER_FILEDIR, BITARCHIVE1.getAbsolutePath());
        Settings.set(CommonSettings.DIR_COMMONTEMPDIR, SERVER1.getAbsolutePath());
        bas = BitarchiveServer.getInstance();
        GenericMessageListener listener = new GenericMessageListener();
        JMSConnectionMockupMQ con = (JMSConnectionMockupMQ) JMSConnectionFactory
                .getInstance();
        con.setListener(Channels.getTheRepos(), listener);
        // Construct a get message for a file in the bitarchive
        final long arcfileOffset = 300L;
        GetMessage msg = new GetMessage(Channels.getAllBa(),
                Channels.getTheRepos(), "NetarchiveSuite-upload1.arc",
                arcfileOffset);
        JMSConnectionMockupMQ.updateMsgID(msg, "AnId");
        bas.visit(msg);
        con.waitForConcurrentTasksToFinish();
        // Should now be one not-ok reply message in the listener
        assertEquals("Should have received exactly one message", 1,
                listener.messagesReceived.size());
        GetMessage replyMsg = (GetMessage) listener.messagesReceived.get(0);
        assertFalse("Reply message should not be ok", replyMsg.isOk());
        assertNull("Reply should contain no data",
                replyMsg.getRecord());
    }

    /**
     * Pass a batch message to BitarchiveServer and test that it replies with an
     * appropriate BatchEndedMessage.
     */
    public void testVisitBatchMessage() throws InterruptedException {
        Settings.set(ArchiveSettings.BITARCHIVE_SERVER_FILEDIR, BITARCHIVE1.getAbsolutePath());
        Settings.set(CommonSettings.DIR_COMMONTEMPDIR, SERVER1.getAbsolutePath());
        bas = BitarchiveServer.getInstance();
        GenericMessageListener listener = new GenericMessageListener();
        JMSConnection con = JMSConnectionFactory.getInstance();
        con.setListener(Channels.getTheBamon(), listener);

        //Construct a BatchMessage to do a checksum job and pass it to
        //the Bitarchive
        BatchMessage bm =
                new BatchMessage(Channels.getTheBamon(),
                        new ChecksumJob(),
                        Settings.get(CommonSettings.USE_REPLICA_ID));

        JMSConnectionMockupMQ.updateMsgID(bm, "ID45");
        bas.visit(bm);
        int batchTries = 0;
        boolean threadAlive;
        do {
            Thread.sleep(100);
            Thread[] threads = new Thread[Thread.activeCount()];
            Thread.enumerate(threads);
            threadAlive = false;
            for (Thread thread : threads) {
                if (thread != null && thread.getName().startsWith("Batch-")) {
                    threadAlive = true;
                }
            }
        } while (threadAlive && batchTries++ < 100);
        ((JMSConnectionMockupMQ) con).waitForConcurrentTasksToFinish();

        // Listener should have received one BatchEndedMessage and a bunch
        // of Heartbeat messages
        assertTrue("Should have received at least one message",
                listener.messagesReceived.size() >= 1);
        Iterator<NetarkivetMessage> i = listener.messagesReceived.iterator();
        BatchEndedMessage bem = null;
        while (i.hasNext()) {
            Object o = i.next();
            if (o instanceof BatchEndedMessage) {
                assertNull("Found two BatchEndedMessages:\n" + bem + "\nand\n"
                        + o.toString(), bem);
                bem = (BatchEndedMessage) o;
            }
        }

        // BatchEndedMessage should contain the appropriate RemoteFile
        TestRemoteFile rf = (TestRemoteFile) bem.getRemoteFile();

        // Check contents of file
        FileAsserts.assertFileNumberOfLines("Should be two lines in file",
                rf.getFile(), 2);
    }

    /** Test that batch messages can run concurrently. 
     * @throws IOException If unable to read a file. 
     */
    public void testVisitBatchMessageThreaded() throws IOException {
        Settings.set(ArchiveSettings.BITARCHIVE_SERVER_FILEDIR, BITARCHIVE1.getAbsolutePath());
        Settings.set(CommonSettings.DIR_COMMONTEMPDIR, SERVER1.getAbsolutePath());
        bas = BitarchiveServer.getInstance();
        GenericMessageListener listener = new GenericMessageListener();
        JMSConnection con = JMSConnectionFactory.getInstance();
        con.setListener(Channels.getTheBamon(), listener);

        //Construct a BatchMessage to do a dummy job that writes the
        // time at the end
        class TimedChecksumJob extends ChecksumJob {
            public void finish(OutputStream o) {
                PrintStream ps = new PrintStream(o);
                ps.println(new Date().getTime());
            }
            public boolean processFile(File f, OutputStream o) {
                return true;
            }
        };
        BatchMessage bm1 =
                new BatchMessage(Channels.getTheBamon(),
                        new TimedChecksumJob() {
                            public void initialize(OutputStream o) {
                                try {
                                    Thread.sleep(10);
                                } catch (InterruptedException e) {
                                    // Not likely, not dangerous.
                                }
                            }
                        },
                        Settings.get(CommonSettings.USE_REPLICA_ID));
        BatchMessage bm2 =
                new BatchMessage(Channels.getTheBamon(),
                        new TimedChecksumJob(),
                        Settings.get(CommonSettings.USE_REPLICA_ID));

        JMSConnectionMockupMQ.updateMsgID(bm1, "ID45");
        JMSConnectionMockupMQ.updateMsgID(bm2, "ID46");
        int beforeCount = Thread.activeCount();
        bas.visit(bm1);
        bas.visit(bm2);
        boolean keepGoing = false;
        do {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                // Don't care
            }
            Thread[] threads = new Thread[Thread.activeCount()];
            if (threads.length > beforeCount) {
                continue;
            }
            Thread.enumerate(threads);
            for (Thread t : threads) {
                if (t.getName().startsWith("Batch-")) {
                    keepGoing = true;
                    break;
                }
            }
        } while (keepGoing);

        ((JMSConnectionMockupMQ) con).waitForConcurrentTasksToFinish();

        // Listener should have received one BatchEndedMessage and a bunch
        // of Heartbeat messages
        assertTrue("Should have received at least two messages",
                listener.messagesReceived.size() >= 2);
        long time45 = -1;
        long time46 = -1;
        for (NetarkivetMessage m : listener.messagesReceived) {
            if (m instanceof BatchEndedMessage) {
                BatchEndedMessage bem = (BatchEndedMessage) m;
                String fileContents =
                        FileUtils.readFile(
                                ((TestRemoteFile)
                                        bem.getRemoteFile()).getFile());
                long time = Long.parseLong(fileContents.trim());
                if (bem.getOriginatingBatchMsgID().equals("ID45")) {
                    time45 = time;
                } else if (bem.getOriginatingBatchMsgID().equals("ID46")) {
                    time46 = time;
                } else {
                    fail("Unexpected message " + m);
                }
            }
        }
        assertTrue("Time45 should be after time46",
                time45 > time46);
    }

    /** Test that a visit(RemoveAndGetMessage) call actually
     * removes (moves) the file.
     * @throws Exception
     */
    public void testVisitRemoveAndGetFileMessage() throws Exception {
        String arcFile = TestInfo.BA1_FILENAME;
        String dummyReplicaId = "ONE";
        String checksum = TestInfo.BA1_CHECKSUM;
        String credentials = Settings.get(
                ArchiveSettings.ENVIRONMENT_THIS_CREDENTIALS);
        Settings.set(ArchiveSettings.BITARCHIVE_SERVER_FILEDIR, BITARCHIVE1.getAbsolutePath());
        Settings.set(CommonSettings.DIR_COMMONTEMPDIR, SERVER1.getAbsolutePath());
        bas = BitarchiveServer.getInstance();
        File baFile = TestInfo.BA1_ORG_FILE;
        File backupFile = TestInfo.BA1_ATTIC_FILE;
        assertTrue("File should exist before removal",
                baFile.exists());
        assertFalse("Backup file should not exist before removal",
                backupFile.exists());

        //Verify that operation fails if the filename is wrong:
        RemoveAndGetFileMessage m1 =
                new RemoveAndGetFileMessage(Channels.getTheRepos(), 
                        Channels.getThisReposClient(),
                        arcFile + "-NOT",
                        dummyReplicaId,
                        checksum,
                        credentials);
        bas.visit(m1);
        assertTrue("File should exist after notfound",
                baFile.exists());
        assertFalse("Backup file should not exist after notfound",
                backupFile.exists());

        //Verify that operation fails if the checksum is wrong:
        RemoveAndGetFileMessage m2 =
                new RemoveAndGetFileMessage(Channels.getTheRepos(), 
                        Channels.getThisReposClient(),
                        arcFile,
                        dummyReplicaId,
                        checksum + "-NOT",
                        credentials);
        JMSConnectionMockupMQ.updateMsgID(m2, "correct1");
        bas.visit(m2);
        assertTrue("File should exist after remove attempt with wrong checksum",
                baFile.exists());
        assertFalse("Backup file should not exist after failed operation.",
                backupFile.exists());
        LogUtils.flushLogs(BitarchiveServer.class.getName());
        FileAsserts.assertFileContains("Log should have given warning",
                "checksum mismatch", TestInfo.LOG_FILE);
        assertFalse("Message should have notOk status",
                m2.isOk());

        //Verify that operation fails if credentials are wrong:
        RemoveAndGetFileMessage m3 =
                new RemoveAndGetFileMessage(Channels.getTheRepos(), 
                        Channels.getThisReposClient(),
                        arcFile,
                        dummyReplicaId,
                        checksum,
                        credentials + "-NOT");
        JMSConnectionMockupMQ.updateMsgID(m3, "correct3");
        bas.visit(m3);
        assertTrue("File should exist after remove attempt with wrong creds",
                baFile.exists());
        assertFalse("Backup file should not exist after failed operation.",
                backupFile.exists());
        LogUtils.flushLogs(BitarchiveServer.class.getName());
        FileAsserts.assertFileContains("Log should have given warning",
                "wrong credentials", TestInfo.LOG_FILE);
        assertFalse("Message should have notOk status",
                m3.isOk());

        //Verify that operation succeeds in the correct case:
        RemoveAndGetFileMessage m4 =
                new RemoveAndGetFileMessage(Channels.getTheRepos(), 
                        Channels.getThisReposClient(), 
                        arcFile,
                        dummyReplicaId,
                        checksum,
                        credentials);
        long len = baFile.length();
        JMSConnectionMockupMQ.updateMsgID(m4, "correct4");

        bas.visit(m4);

        MessageAsserts.assertMessageOk("Message should say OK", m4);
        assertFalse("File should not exist after removal",
                baFile.exists());
        assertTrue("Backup file should exist after removal",
                backupFile.exists());
        LogUtils.flushLogs(BitarchiveServer.class.getName());
        FileAsserts.assertFileContains("Log should have given warning",
                "Removed file", TestInfo.LOG_FILE);
        File f = m4.getData();
        assertNotNull("Msg should have file set", f);
        assertEquals("File should have proper contents", len, f.length());

        assertEquals("Should have no remote files left on the server",
                     0, TestRemoteFile.remainingFiles().size());

        assertTrue("The message should refer to the current replica id.", 
                m4.getReplicaId().contains(Settings.get(CommonSettings.USE_REPLICA_ID)));
        m4.clearBuffer();
        m4.accept(bas);
        try {
            m4.getData();
            fail("This should throw an IOFailure, since the data has just been removed.");
        } catch (IOFailure e) {
            // expected
        }
        
    }
    
    public void testHeartBeatSender() throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
        BitarchiveServer bas = BitarchiveServer.getInstance();
        
        Field hbs = ReflectUtils.getPrivateField(BitarchiveServer.class, "heartBeatSender");
        
        // ensure, that a HeartBeatSender is created.
        assertNotNull("The HeartBeatSender should not be null.", hbs.get(bas));
        // ensure, that the HeartBeatSender refers to the BitarchiveServer in the text.
        assertTrue("The HeartBeatSender should refer to the bitarchive server in the text", 
                hbs.get(bas).toString().contains(bas.toString()));
    }
    
    /**
     * Ensure, that the application dies if given the wrong input.
     */
    public void testApplication() {
        ReflectUtils.testUtilityConstructor(BitarchiveApplication.class);

        PreventSystemExit pse = new PreventSystemExit();
        PreserveStdStreams pss = new PreserveStdStreams(true);
        pse.setUp();
        pss.setUp();
        
        try {
            BitarchiveApplication.main(new String[]{"ERROR"});
            fail("It should throw an exception ");
        } catch (SecurityException e) {
            // expected !
        }

        pss.tearDown();
        pse.tearDown();
        
        assertEquals("Should give exit code 1", 1, pse.getExitValue());
        assertTrue("Should tell that no arguments are expected.", 
                pss.getOut().contains("This application takes no arguments"));
    }

    /**
     * A generic message listener class which just stores a list of all
     * messages it receives.
     */
    public static class GenericMessageListener implements MessageListener {
        /**
         * List of messages received.
         */
        public ArrayList<NetarkivetMessage> messagesReceived =
                new ArrayList<NetarkivetMessage>();

        /**
         * Handle the message.
         * @param message the given message
         */
        public void onMessage(Message message) {
            NetarkivetMessage naMsg = JMSConnection.unpack(message);
            messagesReceived.add(naMsg);
        }
    }
}
