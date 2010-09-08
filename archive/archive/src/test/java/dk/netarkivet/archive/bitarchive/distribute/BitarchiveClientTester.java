/* $Id$
*  $Revision$
*  $Date$
*  $Author$
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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import junit.framework.TestCase;
import dk.netarkivet.archive.ArchiveSettings;
import dk.netarkivet.archive.checksum.distribute.CorrectMessage;
import dk.netarkivet.archive.checksum.distribute.GetAllChecksumsMessage;
import dk.netarkivet.archive.checksum.distribute.GetAllFilenamesMessage;
import dk.netarkivet.archive.checksum.distribute.GetChecksumMessage;
import dk.netarkivet.archive.distribute.ArchiveMessageHandler;
import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.distribute.ChannelID;
import dk.netarkivet.common.distribute.Channels;
import dk.netarkivet.common.distribute.ChannelsTester;
import dk.netarkivet.common.distribute.JMSConnectionFactory;
import dk.netarkivet.common.distribute.JMSConnectionMockupMQ;
import dk.netarkivet.common.distribute.NetarkivetMessage;
import dk.netarkivet.common.distribute.RemoteFileFactory;
import dk.netarkivet.common.distribute.arcrepository.BitarchiveRecord;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.PrintNotifications;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.utils.StreamUtils;
import dk.netarkivet.common.utils.batch.FileBatchJob;
import dk.netarkivet.testutils.FileAsserts;
import dk.netarkivet.testutils.MessageAsserts;
import dk.netarkivet.testutils.TestFileUtils;
import dk.netarkivet.testutils.preconfigured.ReloadSettings;
import dk.netarkivet.testutils.preconfigured.UseTestRemoteFile;


/**
 * Test bitarchive client and server As a number of tests only succeed if both
 * the client and server both operate correctly, both are tested together.
 */
public class BitarchiveClientTester extends TestCase {
    private UseTestRemoteFile rf = new UseTestRemoteFile();

    private static final String ARC_FILE_NAME = "Upload5.ARC";
    private static final File TEST_DIR = new File(
            "tests/dk/netarkivet/archive/bitarchive/distribute/data/");
    private static final File ORIGINALS_DIR = new File(TEST_DIR, "originals");
    private static final File WORKING_DIR = new File(TEST_DIR, "working");
    private static final File BITARCHIVE_DIR = new File(WORKING_DIR,
                                                        "bitarchive1");
    private static final File SERVER_DIR = new File(WORKING_DIR, "server");
    private static final File FILE_TO_UPLOAD = new File(
            new File(WORKING_DIR, "local_files"), ARC_FILE_NAME);
    private static final String ARC_RECORD_0 = "arc_record0.txt";
    private static final File ARC_RECORD_FILE = new File(WORKING_DIR,
                                                         ARC_RECORD_0);
    static final String BITARCHIVE_CREDENTIALS = "42";
    private static final File BATCH_OUTPUT_FILE = new File(WORKING_DIR,
                                                           "batch_output");

    private static final ChannelID THE_BAMON = Channels.getTheBamon();
    private static final ChannelID ALL_BA = Channels.getAllBa();
    private static final ChannelID ANY_BA = Channels.getAnyBa();

    /* The client and server used for testing. */
    BitarchiveClient bac;
    BitarchiveServer bas;
    BitarchiveMonitorServer bam;
    MessageTestHandler handler;

    /**
     * Number of ARC records in the file uploaded.
     */
    private static final int NUM_RECORDS = 21;
    private JMSConnectionMockupMQ con;
    ReloadSettings rs = new ReloadSettings();


    public BitarchiveClientTester(String sTestName) {
        super(sTestName);
    }

    public void setUp() {
        rs.setUp();
        JMSConnectionMockupMQ.useJMSConnectionMockupMQ();
        JMSConnectionMockupMQ.clearTestQueues();
        ChannelsTester.resetChannels();
        
        rf.setUp();

        TestFileUtils.copyDirectoryNonCVS(ORIGINALS_DIR, WORKING_DIR);

        handler = new MessageTestHandler();
        con = (JMSConnectionMockupMQ) JMSConnectionFactory.getInstance();
        con.setListener(Channels.getTheRepos(), handler);
        bac = BitarchiveClient.getInstance(ALL_BA, ANY_BA, THE_BAMON);

        Settings.set(CommonSettings.NOTIFICATIONS_CLASS, PrintNotifications.class.getName());
        Settings.set(ArchiveSettings.BITARCHIVE_SERVER_FILEDIR, BITARCHIVE_DIR.getAbsolutePath());
        Settings.set(CommonSettings.DIR_COMMONTEMPDIR, SERVER_DIR.getAbsolutePath());
        bas = BitarchiveServer.getInstance();
        bam = BitarchiveMonitorServer.getInstance();
    }

    /**
     * After test is done, remove the "archive".
     */
    public void tearDown() {
        bas.close();
        bac.close();
        bam.close();
        JMSConnectionMockupMQ.clearTestQueues();
        if (con != null) {
            con.cleanup();
        }
        //JMSConnection.getInstance().removeListener(Channels.getTheArcrepos(), handler);
        FileUtils.removeRecursively(WORKING_DIR);
        rf.tearDown();
        rs.tearDown();
    }

    /**
     * Bitarchive server uses mkdir instead of mkdirs to create the location of
     * admin data.
     */
    public void testBug34() {
        File serverdir = new File(SERVER_DIR, "/sub1/sub2/");
        Settings.set(ArchiveSettings.BITARCHIVE_SERVER_FILEDIR, BITARCHIVE_DIR.getAbsolutePath());
        Settings.set(CommonSettings.DIR_COMMONTEMPDIR, serverdir.getAbsolutePath());
        BitarchiveServer.getInstance().close();
        BitarchiveServer bas1 = BitarchiveServer.getInstance();
        assertTrue("The serverdir should exist now", serverdir.exists());
        bas1.close();
    }

    /**
     * Verify handling of invalid params for upload correct get and batch.
     */
    public void testInvalidParams() {
        try {
            bac.sendUploadMessage(null);
            fail("null not a valid argument");
        } catch (ArgumentNotValid e) {
            // expected
        }

        try {
            bac.sendBatchJob(null);
            fail("null not a valid argument");
        } catch (ArgumentNotValid e) {
            // expected
        }
        try {
            bac.get(null, 1);
            fail("null not a valid argument");
        } catch (ArgumentNotValid e) {
            // expected
        }

        try {
            bac.get("dummy.arc", -1);
            fail("null not a valid argument");
        } catch (ArgumentNotValid e) {
            // expected
        }
    }

    /**
     * Initiate upload and verify that corresponding upload message received by
     * onUpload message handler.
     */
    public void testUpload() {
        assertTrue("File to upload must exist: " + ARC_FILE_NAME,
                   FILE_TO_UPLOAD.exists());

        bac.sendUploadMessage(RemoteFileFactory.getInstance(FILE_TO_UPLOAD, true, false,
                                                 true));


        con.waitForConcurrentTasksToFinish();

        //assertEquals("1 messages received by the server", 1, bas.getCountMessages());
        assertEquals("One upload ack expected", 1, handler.getTotalCount());
        MessageAsserts.assertMessageOk("Upload ack should be ok",
                                       (NetarkivetMessage) handler.uploadMsg.get(
                                               0));
    }

    /**
     * Verify that it is possible to retrieve previously uploaded file.
     */
    public void testGetFile() {
        assertTrue("File to upload must exist: " + ARC_FILE_NAME,
                   FILE_TO_UPLOAD.exists());

        bac.sendUploadMessage(RemoteFileFactory.getInstance(FILE_TO_UPLOAD, true, false,
                                                 true));
        con.waitForConcurrentTasksToFinish();
        GetFileMessage msg = new GetFileMessage(ALL_BA,
                                                Channels.getTheRepos(),
                                                ARC_FILE_NAME,
                                                "ONE");
        bac.sendGetFileMessage(msg);

        con.waitForConcurrentTasksToFinish();

        //assertEquals("The server should receive an upload and a getFile message", 2, bas.getCountMessages());
        assertEquals("Upload and GetFile ack expected", 2,
                     handler.getTotalCount());

        MessageAsserts.assertMessageOk("Upload message ack should be ok",
                                       handler.uploadMsg.get(0));
        MessageAsserts.assertMessageOk("Get file message ack should be ok",
                                       handler.getfileMsg.get(0));

        GetFileMessage gmsg = handler.getfileMsg.get(0);
        File outputFile = new File(WORKING_DIR, "tempTestGetFile.arc");
        gmsg.getData(outputFile);
        byte[] datareceived = FileUtils.readBinaryFile(outputFile);
        byte[] datasend = FileUtils.readBinaryFile(FILE_TO_UPLOAD);
        boolean isok = Arrays.equals(datareceived, datasend);
        assertTrue(" verify the same data received as uploaded ", isok);
    }

    /**
     * Try to upload the same file twice and verify that corresponding error
     * message received by onUpload message handler.
     */
    public void testUploadTwice() {
        Settings.set(CommonSettings.REMOTE_FILE_CLASS,
                     "dk.netarkivet.common.distribute.TestRemoteFile");
        
        assertTrue("File to upload must exist: " + ARC_FILE_NAME,
                   FILE_TO_UPLOAD.exists());        
        
        bac.sendUploadMessage(RemoteFileFactory.getInstance(FILE_TO_UPLOAD, true, false,
                                                 true));
        bac.sendUploadMessage(RemoteFileFactory.getInstance(FILE_TO_UPLOAD, true, false,
                                                 true));

        con.waitForConcurrentTasksToFinish();

        assertEquals("Two upload messages expected", 2,
                     handler.getTotalCount());

        // now verify we get one successfull upload message and one error message
        UploadMessage msg1 = handler.uploadMsg.get(0);
        UploadMessage msg2 = handler.uploadMsg.get(1);
        int oks = 0;
        if (msg1.isOk()) {
            oks++;
        }
        if (msg2.isOk()) {
            oks++;
        }
        assertEquals("1 ok and 1 failure expected", 1, oks);
    }

    /**
     * Initiate get request and verify that correct data was received by onGet
     * message handler.
     * Initiate get request for data not in the archive and
     * verify correct error message was received by onGet message handler.
     *
     * @throws IOException
     */
    public void testGet() throws IOException {
        assertTrue("File to upload must exist: " + ARC_FILE_NAME,
                   FILE_TO_UPLOAD.exists());

        bac.sendUploadMessage(RemoteFileFactory.getInstance(FILE_TO_UPLOAD, true, false,
                                                 true));
        con.waitForConcurrentTasksToFinish();

        bac.get(ARC_FILE_NAME, 0);
        con.waitForConcurrentTasksToFinish();

        //assertEquals("2 message received by the server", 2, bas.getCountMessages());
        assertEquals("One get result and one upload messages expected", 2,
                     handler.getTotalCount());

        GetMessage msg = handler.getMsg.get(0);

        // verify correct data received
        BitarchiveRecord record = msg.getRecord();
        assertNotNull("ARC record should be non-null", record);
        assertEquals(ARC_FILE_NAME, record.getFile());

        byte[] contents = StreamUtils.inputStreamToBytes(
                record.getData(), (int) record.getLength());

        String targetcontents = FileUtils.readFile(ARC_RECORD_FILE);
        String scontent = new String(contents, "US-ASCII");
        BufferedReader targetcontentsReader = new BufferedReader(
                new StringReader(targetcontents));
        BufferedReader scontentReader = new BufferedReader(
                new StringReader(scontent));
        String s;
        while ((s = targetcontentsReader.readLine()) != null) {
            assertEquals(s, scontentReader.readLine());
        }
    }

    /**
     * Test the batch(BatchMessage) method. Initiate batch job and verify that
     * onBatch receives the corresponding message with correct result data from
     * the batch job.
     */
    public void testBatch1() {
        uploadInPreparationOfBatchTest();

        BatchMessage bMsg = new BatchMessage(THE_BAMON,
                                             Channels.getTheRepos(),
                                             new TestBatchJobRuns(),
                                             Settings.get(
                                                     CommonSettings.USE_REPLICA_ID));
        bac.sendBatchJob(bMsg);
        verifyBatchWentWell();
    }

    /**
     * Verify that the batch(ChannelID,FileBatchJob,RemoteFile) method does not
     * accept null parameters.
     */
    public void testBatch2NullParameters() {
        ChannelID chan = Channels.getTheRepos();
        FileBatchJob job = new TestBatchJobRuns();
        //Variable rf is initialized in the setup() method.
        try {
            bac.sendBatchJob(null, job);
            fail();
        } catch (ArgumentNotValid e) {
            //Expected
        }
        try {
            bac.sendBatchJob(chan, null);
            fail();
        } catch (ArgumentNotValid e) {
            //Expected
        }
    }

    /**
     * Verify that the batch(ChannelID,FileBatchJob,RemoteFile) method does not
     * accept null parameters.
     */
    public void testBatch2() {
        uploadInPreparationOfBatchTest();

        ChannelID chan = Channels.getTheRepos();
        FileBatchJob job = new TestBatchJobRuns();
        //Variable rf is initialized in the setup() method.
        bac.sendBatchJob(chan, job);
        verifyBatchWentWell();
    }

    /**
     * Utility method for the batch tests. Uploads a file and waits for the
     * operation to finish.
     */
    private void uploadInPreparationOfBatchTest() {
        bac.sendUploadMessage(RemoteFileFactory.getInstance(FILE_TO_UPLOAD, true, false,
                                                 true));
        con.waitForConcurrentTasksToFinish();
    }

    /**
     * Utility method for the batch tests. Waits for JMS tasks to finish, then
     * chekcs that - exactly one reply was generated - that the reply had its OK
     * flag set to true - that the output file contains a text that indicates
     * proper processing was done.
     * @throws Exception 
     */
    private void verifyBatchWentWell() {
        // Wait for up to 10 seconds to see if the message gets back
        int i = 0;
        while (handler.batchReplyMsg.size() == 0) {
            if (i >= 1000) {
                break;
            }
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                // We don't care.
            }
            i++;
        }

        assertEquals("Should have one reply", 1, handler.batchReplyMsg.size());
        BatchReplyMessage msg = handler.batchReplyMsg.get(0);
        if (!msg.isOk()) {
            fail("Batch operation expected to succeed, not give "
                 + msg.getErrMsg());
        }
        msg.getResultFile().copyTo(BATCH_OUTPUT_FILE);

        try {
            FileAsserts.assertFileContains("Expected record report not found: " 
                    + NUM_RECORDS + ", but found: " + FileUtils.readFile(BATCH_OUTPUT_FILE), 
                    "Records Processed = " + NUM_RECORDS, BATCH_OUTPUT_FILE);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    /**
     * Verify that a message has the correct id,destination and origin values.
     *
     * @param id   Expected id or "" to ignore
     * @param dest Expected destination or "" to ignore
     * @param org  Expected origin or "" to ignore
     * @param msg  Message to check
     * @return true if all checks succeeds else false
     */
    public boolean checkIdDestOrigin(String id, String dest, String org,
                                     NetarkivetMessage msg) {
        boolean res = true;

        if ((id.length() > 0) && (!id.equals(msg.getID()))) {
            res = false;
        }

        if ((dest.length() > 0) && (!dest.equals(msg.getTo()))) {
            res = false;
        }

        if ((org.length() > 0) && (!dest.equals(msg.getReplyTo()))) {
            res = false;
        }

        return res;
    }
    
    public void testNewMessages() {
        // make sure, that the listener 'handler' is the only one on the TheBamon queue
        BitarchiveMonitorServer.getInstance().close();
        con.setListener(Channels.getTheBamon(), handler);
        con.setListener(Channels.getAllBa(), handler);
        con.setListener(Channels.getAnyBa(), handler);
        
        assertEquals("The handler should be the only one listening to the queue TheBamon", 
                1, con.getListeners(Channels.getTheBamon()).size());
        // check GetChecksum through function
        NetarkivetMessage msg = bac.sendGetChecksumMessage(Channels.getError(), "filename.arc");
        con.waitForConcurrentTasksToFinish();
        
        assertEquals("One GetChecksumMessage expected to be sent.", 1, handler.getChecksumMsg.size());
        assertEquals("The received message should be one returned by the function", 
                msg, handler.getChecksumMsg.get(0));

        // check GetChecksum through message
        GetChecksumMessage csMsg = new GetChecksumMessage(Channels.getTheBamon(),
                Channels.getError(), "filename.arc", Settings.get(CommonSettings.USE_REPLICA_ID));
        bac.sendGetChecksumMessage(csMsg);
        con.waitForConcurrentTasksToFinish();
        
        assertEquals("Another GetChecksumMessage expected to be sent.", 2, handler.getChecksumMsg.size());
        assertEquals("The received message should be one returned by the function", 
                csMsg, handler.getChecksumMsg.get(1));
        
        // check GetAllChecksums through message
        GetAllChecksumsMessage gcsMsg = new GetAllChecksumsMessage(Channels.getTheBamon(),
                Channels.getError(), Settings.get(CommonSettings.USE_REPLICA_ID));
        bac.sendGetAllChecksumsMessage(gcsMsg);
        con.waitForConcurrentTasksToFinish();

        assertEquals("One GetAllChecksumsMessage expected to be sent.", 1, handler.checksumsMsg.size());
        assertEquals("The received message should be one returned by the function", 
                gcsMsg, handler.checksumsMsg.get(0));
        
        // check GetAllFilenames through message
        GetAllFilenamesMessage gfsMsg = new GetAllFilenamesMessage(Channels.getTheBamon(),
                Channels.getError(), Settings.get(CommonSettings.USE_REPLICA_ID));
        bac.sendGetAllFilenamesMessage(gfsMsg);
        con.waitForConcurrentTasksToFinish();
        
        assertEquals("One GetAllFilenamesMessage expected to be sent.", 1, handler.filenamesMsg.size());
        assertEquals("The received message should be one returned by the function", 
                gfsMsg, handler.filenamesMsg.get(0));

        // check Correct through message
        CorrectMessage corMsg = new CorrectMessage(Channels.getTheBamon(),
                Channels.getError(), "badChecksum", 
                RemoteFileFactory.getInstance(FILE_TO_UPLOAD, true, false, true),
                Settings.get(CommonSettings.USE_REPLICA_ID), "credentials");
        bac.sendCorrectMessage(corMsg);
        con.waitForConcurrentTasksToFinish();
        
        assertEquals("One CorrectMessage expected to be sent.", 1, handler.correctMsg.size());
        assertEquals("The received message should be one returned by the function", 
                corMsg, handler.correctMsg.get(0));
        
        // check RemoveAndGetFileMessage
        RemoveAndGetFileMessage ragfMsg = new RemoveAndGetFileMessage(
                Channels.getTheBamon(), Channels.getError(), "filename.arc",
                Settings.get(CommonSettings.USE_REPLICA_ID), "checksum", "credentials");
        bac.sendRemoveAndGetFileMessage(ragfMsg);
        con.waitForConcurrentTasksToFinish();
        
        assertEquals("One GetAndRemoveFileMessage expected, but was: " + handler.ragfMsg, 
                1, handler.ragfMsg.size());
        assertEquals("The received message should be one returned by the function",
                ragfMsg, handler.ragfMsg.get(0));
    }

    /* Receive and check messages */
    public class MessageTestHandler extends ArchiveMessageHandler {
        public List<UploadMessage> uploadMsg = new ArrayList<UploadMessage>();
        public List<GetMessage> getMsg = new ArrayList<GetMessage>();
        public List<BatchMessage> batchMsg = new ArrayList<BatchMessage>();
        public List<BatchReplyMessage> batchReplyMsg
                = new ArrayList<BatchReplyMessage>();
        public List<GetFileMessage> getfileMsg
                = new ArrayList<GetFileMessage>();
        public List<GetAllFilenamesMessage> filenamesMsg 
                = new ArrayList<GetAllFilenamesMessage>();
        public List<GetAllChecksumsMessage> checksumsMsg 
                = new ArrayList<GetAllChecksumsMessage>();
        public List<GetChecksumMessage> getChecksumMsg 
                = new ArrayList<GetChecksumMessage>();
        public List<CorrectMessage> correctMsg
                = new ArrayList<CorrectMessage>();
        public List<RemoveAndGetFileMessage> ragfMsg
                = new ArrayList<RemoveAndGetFileMessage>();

        public MessageTestHandler() {
            //System.out.println("MessageTestHandler initiated!");
        }

        public void visit(UploadMessage msg) {
            uploadMsg.add(msg);
        }

        public void visit(GetMessage msg) {
            getMsg.add(msg);
        }

        public void visit(GetFileMessage msg) {
            getfileMsg.add(msg);
        }

        public void visit(BatchMessage msg) {
            batchMsg.add(msg);
        }

        public void visit(BatchReplyMessage msg) {
            batchReplyMsg.add(msg);
        }
        
        public void visit(RemoveAndGetFileMessage msg) {
            ragfMsg.add(msg);
        }
        
        public void visit(GetAllFilenamesMessage msg) {
            filenamesMsg.add(msg);
        }

        public void visit(GetAllChecksumsMessage msg) {
            checksumsMsg.add(msg);
        }

        public void visit(GetChecksumMessage msg) {
            getChecksumMsg.add(msg);
        }

        public void visit(CorrectMessage msg) {
            correctMsg.add(msg);
        }

        synchronized public int getTotalCount() {
            return (uploadMsg.size() + getMsg.size()
                    + batchMsg.size() + getfileMsg.size()
                    + batchReplyMsg.size());
        }

        synchronized void receive(long ms) {
            try {
                this.wait(ms);
            } catch (InterruptedException e) {
                fail("should not be interupted");
            }
        }
    }
}
