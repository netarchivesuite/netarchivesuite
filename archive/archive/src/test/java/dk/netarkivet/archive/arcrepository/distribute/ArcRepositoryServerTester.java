/*$Id$
 * $Revision$
 * $Date$
 * $Author$
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
package dk.netarkivet.archive.arcrepository.distribute;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;
import dk.netarkivet.archive.ArchiveSettings;
import dk.netarkivet.archive.arcrepository.ArcRepository;
import dk.netarkivet.archive.arcrepository.bitpreservation.AdminDataMessage;
import dk.netarkivet.archive.arcrepositoryadmin.AdminData;
import dk.netarkivet.archive.bitarchive.distribute.BatchMessage;
import dk.netarkivet.archive.bitarchive.distribute.BatchReplyMessage;
import dk.netarkivet.archive.bitarchive.distribute.GetFileMessage;
import dk.netarkivet.archive.bitarchive.distribute.GetMessage;
import dk.netarkivet.archive.bitarchive.distribute.RemoveAndGetFileMessage;
import dk.netarkivet.archive.bitarchive.distribute.TestBatchJobRuns;
import dk.netarkivet.archive.bitarchive.distribute.UploadMessage;
import dk.netarkivet.archive.checksum.distribute.CorrectMessage;
import dk.netarkivet.archive.checksum.distribute.GetAllChecksumsMessage;
import dk.netarkivet.archive.checksum.distribute.GetAllFilenamesMessage;
import dk.netarkivet.archive.checksum.distribute.GetChecksumMessage;
import dk.netarkivet.archive.distribute.ReplicaClient;
import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.distribute.Channels;
import dk.netarkivet.common.distribute.JMSConnection;
import dk.netarkivet.common.distribute.JMSConnectionFactory;
import dk.netarkivet.common.distribute.JMSConnectionMockupMQ;
import dk.netarkivet.common.distribute.JMSConnectionTester;
import dk.netarkivet.common.distribute.NullRemoteFile;
import dk.netarkivet.common.distribute.RemoteFile;
import dk.netarkivet.common.distribute.RemoteFileFactory;
import dk.netarkivet.common.distribute.JMSConnectionTester.DummyServer;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.NotImplementedException;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.utils.batch.ChecksumJob;
import dk.netarkivet.testutils.TestFileUtils;
import dk.netarkivet.testutils.TestMessageListener;
import dk.netarkivet.testutils.preconfigured.ReloadSettings;
import dk.netarkivet.testutils.preconfigured.UseTestRemoteFile;

/**
 * Unit tests for the class ArcRepositoryServer.
 */
public class ArcRepositoryServerTester extends TestCase {
    /**
     * The test log directories for Controller and AdminData.
     */
    private static final File TEST_DIR = new File(
            "tests/dk/netarkivet/archive/arcrepository/data/get");

    private static final File ORIGINALS_DIR = new File(TEST_DIR, "originals");

    private static final File WORKING_DIR = new File(TEST_DIR, "working");

    private static final File BITARCHIVE_DIR = new File(WORKING_DIR,
                                                        "bitarchive1");

    /**
     * The test log directories for Controller and AdminData.
     */
    private static final File CLOG_DIR = new File(WORKING_DIR,
                                                  "log/controller");

    private static final File ALOG_DIR = new File(WORKING_DIR, "log/admindata");

    /**
     *
     */
    public final File TESTLOGPROP = new File(
            "tests/dk/netarkivet/testlog.prop");

    /**
     * The files that are uploaded during the tests and that must be removed
     * afterwards.
     */
    private static final List<String> STORABLE_FILES = Arrays.asList(
            new String[]{"get1.ARC", "get2.ARC"});

    private File file;

    private JMSConnection con;

    private JMSConnectionTester.DummyServer dummyServer;

    ReloadSettings rs = new ReloadSettings();
    UseTestRemoteFile rf = new UseTestRemoteFile();

    protected void setUp() throws Exception {
        rs.setUp();
        rf.setUp();
        JMSConnectionMockupMQ.useJMSConnectionMockupMQ();

        FileUtils.removeRecursively(WORKING_DIR);
        TestFileUtils.copyDirectoryNonCVS(ORIGINALS_DIR, WORKING_DIR);
        FileUtils.createDir(CLOG_DIR);
        FileUtils.createDir(ALOG_DIR);

        Settings.set(ArchiveSettings.DIRS_ARCREPOSITORY_ADMIN, 
                ALOG_DIR.getAbsolutePath());
        con = JMSConnectionFactory.getInstance();
        dummyServer = new JMSConnectionTester.DummyServer();
        con.setListener(Channels.getError(), dummyServer);
    }

    protected void tearDown() throws Exception {
        AdminData.getUpdateableInstance().close();
        FileUtils.removeRecursively(WORKING_DIR);
        con.removeListener(Channels.getError(), dummyServer);
        rf.tearDown();
        rs.tearDown();
    }

    /**
     * Test visit() StoreMessage methods arguments.
     */
    public void testVisitNulls() {
        ArcRepository arc = ArcRepository.getInstance();
        ArcRepositoryServer arcServ = new ArcRepositoryServer(arc);

        // Test StoreMessage
        try {
            arcServ.visit((StoreMessage) null);
            fail("Should throw ArgumentNotValid exception");
        } catch (ArgumentNotValid e) {
            // Expected
        }
        
        // Test RemoveAndGetFileMessage
        try {
            arcServ.visit((RemoveAndGetFileMessage) null);
            fail("Should throw ArgumentNotValid exception");
        } catch (ArgumentNotValid e) {
            // Expected
        }
        
        // Test AdminDataMessage
        try {
            arcServ.visit((AdminDataMessage) null);
            fail("Should throw ArgumentNotValid exception");
        } catch (ArgumentNotValid e) {
            // Expected
        }

        // Test UploadMessage
        try {
            arcServ.visit((UploadMessage) null);
            fail("Should throw ArgumentNotValid exception");
        } catch (ArgumentNotValid e) {
            // Expected
        }

        // Test BatchReplyMessage
        try {
            arcServ.visit((BatchReplyMessage) null);
            fail("Should throw ArgumentNotValid exception");
        } catch (ArgumentNotValid e) {
            // Expected
        }
        
        // Test BatchMessage
        try {
            arcServ.visit((BatchMessage) null);
            fail("Should throw ArgumentNotValid exception");
        } catch (ArgumentNotValid e) {
            // Expected
        }

        // Test GetMessage
        try {
            arcServ.visit((GetMessage) null);
            fail("Should throw ArgumentNotValid exception");
        } catch (ArgumentNotValid e) {
            // Expected
        }

        // Test GetFileMessage
        try {
            arcServ.visit((GetFileMessage) null);
            fail("Should throw ArgumentNotValid exception");
        } catch (ArgumentNotValid e) {
            // Expected
        }
        
        // Test GetAllFilenamesMessage
        try {
            arcServ.visit((GetAllFilenamesMessage) null);
            fail("Should throw ArgumentNotValid exception");
        } catch (ArgumentNotValid e) {
            // Expected
        }

        // Test GetAllChecksumsMessage
        try {
            arcServ.visit((GetAllChecksumsMessage) null);
            fail("Should throw ArgumentNotValid exception");
        } catch (ArgumentNotValid e) {
            // Expected
        }

        // Test GetChecksumMessage
        try {
            arcServ.visit((GetChecksumMessage) null);
            fail("Should throw ArgumentNotValid exception");
        } catch (ArgumentNotValid e) {
            // Expected
        }

        // Test CorrectMessage
        try {
            arcServ.visit((CorrectMessage) null);
            fail("Should throw ArgumentNotValid exception");
        } catch (ArgumentNotValid e) {
            // Expected
        }
        
        arc.close();
    }

    /**
     * Test a BatchMessage is sent to the_bamon queue.
     */
    public void testVisitBatchMessage() {
        // Create dummy server and listen on the TheArcrepos queue
        DummyServer serverTheBamonQueue = new DummyServer();
        serverTheBamonQueue.reset();
        con.setListener(Channels.getTheBamon(), serverTheBamonQueue);

        ArcRepository arc = ArcRepository.getInstance();
        BatchMessage msg = new BatchMessage(Channels.getTheBamon(), Channels
                .getError(), new TestBatchJobRuns(), Settings.get(
                CommonSettings.USE_REPLICA_ID));

        new ArcRepositoryServer(arc).visit(msg);

        ((JMSConnectionMockupMQ) con).waitForConcurrentTasksToFinish();

        assertEquals("Server should have received 1 message", 1,
                     serverTheBamonQueue.msgReceived);
        arc.close();
    }

    /**
     * Test message is sent and returned, and set "Not OK" if an error occurs.
     */
    public void testStoreNoSuchFile() {
        file = new File(BITARCHIVE_DIR, "NO_SUCH_FILE");
        ArcRepository arc = ArcRepository.getInstance();
        try {
            new StoreMessage(Channels.getError(), file);
            fail("Should get error making a storemessage with "
                    + "non-existing file");
        } catch (ArgumentNotValid e) {
            //expected
        }

        Settings.set(CommonSettings.REMOTE_FILE_CLASS, NullRemoteFile.class.getName());

        file = new File(new File(BITARCHIVE_DIR, "filedir"),
                        STORABLE_FILES.get(0).toString());
        StoreMessage msg = new StoreMessage(Channels.getError(), file);
        JMSConnectionMockupMQ.updateMsgID(msg, "store1");

        dummyServer.reset();

        new ArcRepositoryServer(arc).visit(msg);

        ((JMSConnectionMockupMQ) JMSConnectionMockupMQ.getInstance())
                .waitForConcurrentTasksToFinish();

        assertFalse("Message should have been tagged NotOK", msg.isOk());
        assertEquals("Server should have received 1 incorrect message",
                     dummyServer.msgNotOK, 1);
        arc.close();
    }

    /**
     * Test message is sent and returned, and set "OK" if no errors occurs.
     */
    public void testStore() {
        file = new File(new File(BITARCHIVE_DIR, "filedir"),
                        STORABLE_FILES.get(0).toString());
        ArcRepository arc = ArcRepository.getInstance();
        StoreMessage msg = new StoreMessage(Channels.getError(), file);
        JMSConnectionMockupMQ.updateMsgID(msg, "store1");

        new ArcRepositoryServer(arc).visit(msg);
        assertTrue("Message should have been tagged OK", msg.isOk());
        arc.close();
    }

    /**
     * Test message is resent.
     */
    public void testGet() {
        file = new File(BITARCHIVE_DIR, STORABLE_FILES.get(0).toString());
        GetMessage msg = new GetMessage(Channels.getTheRepos(), Channels
                .getError(), "", 0);
        JMSConnectionMockupMQ testCon = (JMSConnectionMockupMQ) JMSConnectionMockupMQ
                .getInstance();
        TestMessageListener listener = new TestMessageListener();
        testCon.setListener(Channels.getAllBa(), listener);
        ArcRepositoryServer arc = 
            new ArcRepositoryServer(ArcRepository.getInstance());
        arc.visit(msg);
        testCon.waitForConcurrentTasksToFinish();
        assertEquals("Message should have been sent to the bitarchive queue",
                     1, listener.getNumReceived());
        
        arc.close();
    }

    public void testVisitBadMessages() throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, InterruptedException {
        file = new File(new File(BITARCHIVE_DIR, "filedir"),
                STORABLE_FILES.get(0).toString());
        int getReplicaClientValue = 0;

        TestArcRepository arc = new TestArcRepository();
        ArcRepositoryServer arcServ = new ArcRepositoryServer(arc);
        
        JMSConnectionMockupMQ testCon = (JMSConnectionMockupMQ) JMSConnectionMockupMQ
        .getInstance();
        TestMessageListener listenReposClient = new TestMessageListener();
        testCon.setListener(Channels.getThisReposClient(), listenReposClient);
        TestMessageListener listenError = new TestMessageListener();
        testCon.setListener(Channels.getError(), listenError);
        
        // An error reply should be sent to the error queue.
        StoreMessage storeMsg = new StoreMessage(Channels.getError(), file);
        JMSConnectionMockupMQ.updateMsgID(storeMsg, "storeMsg1");
        arcServ.visit(storeMsg);
        assertTrue("The function 'store' should have been called",
                arc.calls.containsKey("store"));
        assertEquals("The function 'store' should have been called once.", 
                Integer.valueOf(1), arc.calls.get("store"));
//        testCon.waitForConcurrentTasksToFinish();
//        try {
//            synchronized (this) {
//                wait(500);
//            }
//        } catch (InterruptedException e) {
//            fail("" + e.getMessage());
//        }
//        testCon.waitForConcurrentTasksToFinish();
//        
//        assertEquals("One message should have been received on the Error queue", 
//                1, listenError.getNumReceived());
//        NetarkivetMessage reply = listenError.getReceived();
//        listenError.reset();
//        assertTrue("It should be a reply of the type StoreMessage",
//                reply instanceof StoreMessage);
//        assertEquals("The same store message should be received as reply.", 
//                storeMsg.getID(), reply.getID());
//        assertFalse("The message should not be ok", reply.isOk());
        
        // An error reply should be sent to the error queue.
        RemoveAndGetFileMessage ragf = new RemoveAndGetFileMessage(
              Channels.getTheRepos(), Channels.getError(), "filename", 
              "ONE", "checksum", "credentials");
        JMSConnectionMockupMQ.updateMsgID(ragf, "removeAndGetFile1");
        arcServ.visit(ragf);
        assertTrue("The function 'removeAndGetFile' should have been called",
                arc.calls.containsKey("removeAndGetFile"));
        assertEquals("The function 'removeAndGetFile' should have been called once.", 
                Integer.valueOf(1), arc.calls.get("removeAndGetFile"));
        
        // admin data message sends and reply
        AdminDataMessage ad = new AdminDataMessage("filename", "checksum");
        JMSConnectionMockupMQ.updateMsgID(ad, "adminData1");
        arcServ.visit(ad);
        assertTrue("The function 'updateAdminData' should have been called",
                arc.calls.containsKey("updateAdminData"));
        assertEquals("The function 'updateAdminData' should have been called once.", 
                Integer.valueOf(1), arc.calls.get("updateAdminData"));
        testCon.waitForConcurrentTasksToFinish();
        assertEquals("It should be a reply to the admin data message", 
                ad.getID(), listenReposClient.getAllReceived().get(0).getID());
        
        // test Upload message
        UploadMessage upmsg = new UploadMessage(Channels.getTheRepos(), 
                Channels.getError(), RemoteFileFactory.getCopyfileInstance(file));
        arcServ.visit(upmsg);
        assertTrue("The function 'onUpload' should have been called",
                arc.calls.containsKey("onUpload"));
        assertEquals("The function 'onUpload' should have been called once.", 
                Integer.valueOf(1), arc.calls.get("onUpload"));
        
        // test BatchReplyMessage
        BatchReplyMessage brmsg = new BatchReplyMessage(Channels.getTheRepos(), 
                Channels.getError(), "originatingBatchMsgId", 0, 
                Collections.<File>emptyList(), 
                RemoteFileFactory.getCopyfileInstance(file));
        arcServ.visit(brmsg);
        assertTrue("The function 'onBatchReply' should have been called",
                arc.calls.containsKey("onBatchReply"));
        assertEquals("The function 'onBatchReply' should have been called once.", 
                Integer.valueOf(1), arc.calls.get("onBatchReply"));

        // test BatchMessage
        BatchMessage bm = new BatchMessage(Channels.getThisIndexClient(), 
                new ChecksumJob(), "ONE");
        JMSConnectionMockupMQ.updateMsgID(bm, "bm1");
        arcServ.visit(bm);
        assertTrue("The function 'getReplicaClientFromReplicaId' should have been called",
                arc.calls.containsKey("getReplicaClientFromReplicaId"));
        assertEquals("The function 'getReplicaClientFromReplicaId' should have been called once.", 
                Integer.valueOf(++getReplicaClientValue), arc.calls.get("getReplicaClientFromReplicaId"));
        
        // test GetMessage
        GetMessage gm = new GetMessage(Channels.getThisIndexClient(), 
                Channels.getError(), "filename", 0L);
        JMSConnectionMockupMQ.updateMsgID(gm, "gm1");
        arcServ.visit(gm);
        assertTrue("The function 'getReplicaClientFromReplicaId' should have been called",
                arc.calls.containsKey("getReplicaClientFromReplicaId"));
        assertEquals("The function 'getReplicaClientFromReplicaId' should have been called once.", 
                Integer.valueOf(++getReplicaClientValue), arc.calls.get("getReplicaClientFromReplicaId"));

        // test GetFileMessage
        GetFileMessage gfm = new GetFileMessage(Channels.getThisIndexClient(), 
                Channels.getError(), "filename", "ONE");
        JMSConnectionMockupMQ.updateMsgID(gfm, "gfm1");
        arcServ.visit(gfm);
        assertTrue("The function 'getReplicaClientFromReplicaId' should have been called",
                arc.calls.containsKey("getReplicaClientFromReplicaId"));
        assertEquals("The function 'getReplicaClientFromReplicaId' should have been called once.", 
                Integer.valueOf(++getReplicaClientValue), arc.calls.get("getReplicaClientFromReplicaId"));

        // test GetAllFilenamesMessage
        GetAllFilenamesMessage gafm = new GetAllFilenamesMessage(Channels.getThisIndexClient(), 
                Channels.getError(), "ONE");
        JMSConnectionMockupMQ.updateMsgID(gafm, "gafm1");
        arcServ.visit(gafm);
        assertTrue("The function 'getReplicaClientFromReplicaId' should have been called",
                arc.calls.containsKey("getReplicaClientFromReplicaId"));
        assertEquals("The function 'getReplicaClientFromReplicaId' should have been called once.", 
                Integer.valueOf(++getReplicaClientValue), arc.calls.get("getReplicaClientFromReplicaId"));
        
        // test GetAllChecksumsMessage
        GetAllChecksumsMessage gacm = new GetAllChecksumsMessage(Channels.getThisIndexClient(), 
                Channels.getError(), "ONE");
        JMSConnectionMockupMQ.updateMsgID(gacm, "gacm1");
        arcServ.visit(gacm);
        assertTrue("The function 'getReplicaClientFromReplicaId' should have been called",
                arc.calls.containsKey("getReplicaClientFromReplicaId"));
        assertEquals("The function 'getReplicaClientFromReplicaId' should have been called once.", 
                Integer.valueOf(++getReplicaClientValue), arc.calls.get("getReplicaClientFromReplicaId"));

        // test GetChecksumMessage 1
        GetChecksumMessage gcm = new GetChecksumMessage(Channels.getThisIndexClient(), 
                Channels.getError(), "filename", "ONE");
        JMSConnectionMockupMQ.updateMsgID(gcm, "gcm1");
        arcServ.visit(gcm);
        assertTrue("The function 'getReplicaClientFromReplicaId' should have been called",
                arc.calls.containsKey("getReplicaClientFromReplicaId"));
        assertEquals("The function 'getReplicaClientFromReplicaId' should have been called once.", 
                Integer.valueOf(++getReplicaClientValue), arc.calls.get("getReplicaClientFromReplicaId"));

        // test GetChecksumMessage 2
        GetChecksumMessage gcm2 = new GetChecksumMessage(Channels.getThisIndexClient(), 
                Channels.getError(), "ONE", "filename");
        JMSConnectionMockupMQ.updateMsgID(gcm2, "gcm2");
        gcm2.setIsReply();
        arcServ.visit(gcm2);
        assertTrue("The function 'onChecksumReply' should have been called",
                arc.calls.containsKey("onChecksumReply"));
        assertEquals("The function 'onChecksumReply' should have been called once.", 
                Integer.valueOf(1), arc.calls.get("onChecksumReply"));

        // test CorrectMessage
        CorrectMessage cm = new CorrectMessage(Channels.getThisIndexClient(), 
                Channels.getError(), "badChecksum", 
                RemoteFileFactory.getCopyfileInstance(file), "ONE", "Credentials");
        JMSConnectionMockupMQ.updateMsgID(cm, "correct1");
        arcServ.visit(cm);
        assertTrue("The function 'getReplicaClientFromReplicaId' should have been called",
                arc.calls.containsKey("getReplicaClientFromReplicaId"));
        assertEquals("The function 'getReplicaClientFromReplicaId' should have been called once.", 
                Integer.valueOf(++getReplicaClientValue), arc.calls.get("getReplicaClientFromReplicaId"));

        arcServ.close();
    }

    public class TestArcRepository extends ArcRepository {
        
        public Map<String, Integer> calls = new HashMap<String, Integer>();
        
        public TestArcRepository() {
            super();
        }
        
        private void addCall(String call) {
            if(calls.containsKey(call)) {
                Integer num = calls.get(call);
                num += 1;
                calls.put(call, num);
            } else {
                calls.put(call, Integer.valueOf(1));
            }
        }
        
        public void store(RemoteFile rf, StoreMessage replyInfo) {
            addCall("store");
            throw new NotImplementedException("TESTING");
        }

        public void removeAndGetFile(RemoveAndGetFileMessage msg) {
            addCall("removeAndGetFile");
            throw new NotImplementedException("TESTING");
        }

        public void updateAdminData(AdminDataMessage msg) {
            addCall("updateAdminData");
            throw new NotImplementedException("TESTING");
        }
        
        public void onUpload(UploadMessage msg) {
            addCall("onUpload");
            throw new NotImplementedException("TESTING");
        }
        
        public void onBatchReply(BatchReplyMessage msg) {
            addCall("onBatchReply");
            throw new NotImplementedException("TESTING");
        }
        
        public ReplicaClient getReplicaClientFromReplicaId(String repId) {
            addCall("getReplicaClientFromReplicaId");
            return null;
        }
        
        public void onChecksumReply(GetChecksumMessage msg) {
            addCall("onChecksumReply");
            throw new NotImplementedException("TESTING");
        }
    }
}