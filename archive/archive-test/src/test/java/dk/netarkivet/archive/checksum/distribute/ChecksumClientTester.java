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
package dk.netarkivet.archive.checksum.distribute;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import dk.netarkivet.archive.bitarchive.distribute.BatchMessage;
import dk.netarkivet.archive.bitarchive.distribute.GetFileMessage;
import dk.netarkivet.archive.bitarchive.distribute.GetMessage;
import dk.netarkivet.archive.bitarchive.distribute.RemoveAndGetFileMessage;
import dk.netarkivet.archive.bitarchive.distribute.UploadMessage;
import dk.netarkivet.archive.distribute.ArchiveMessageHandler;
import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.distribute.Channels;
import dk.netarkivet.common.distribute.JMSConnectionFactory;
import dk.netarkivet.common.distribute.JMSConnectionMockupMQ;
import dk.netarkivet.common.distribute.NetarkivetMessage;
import dk.netarkivet.common.distribute.RemoteFileFactory;
import dk.netarkivet.common.distribute.arcrepository.ReplicaType;
import dk.netarkivet.common.exceptions.IllegalState;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.utils.batch.ChecksumJob;
import dk.netarkivet.testutils.ClassAsserts;
import dk.netarkivet.testutils.preconfigured.MoveTestFiles;
import dk.netarkivet.testutils.preconfigured.ReloadSettings;
import dk.netarkivet.testutils.preconfigured.UseTestRemoteFile;

public class ChecksumClientTester {

    UseTestRemoteFile rf = new UseTestRemoteFile();
    ReloadSettings rs = new ReloadSettings();
    MoveTestFiles mtf = new MoveTestFiles(TestInfo.ORIGINAL_DIR, TestInfo.WORK_DIR);
    MessageTestHandler handler;
    JMSConnectionMockupMQ con;

    @Before
    public void setUp() {
        rs.setUp();
        rf.setUp();
        mtf.setUp();
        JMSConnectionMockupMQ.useJMSConnectionMockupMQ();
        Channels.reset();

        Settings.set(CommonSettings.USE_REPLICA_ID, "THREE");

        con = (JMSConnectionMockupMQ) JMSConnectionFactory.getInstance();
        handler = new MessageTestHandler();

        con.setListener(Channels.getTheCR(), handler);
    }

    @After
    public void tearDown() {
        JMSConnectionMockupMQ.clearTestQueues();

        mtf.tearDown();
        rf.tearDown();
        rs.tearDown();
    }

    @Test
    public void testClient() {
        ClassAsserts.assertPrivateConstructor(ChecksumClient.class);
    }

    @Test
    public void testFails() {
        ChecksumClient cc = ChecksumClient.getInstance(Channels.getTheCR());

        // Test batch through a batch message.
        try {
            BatchMessage bm = new BatchMessage(Channels.getTheCR(), new ChecksumJob(),
                    Settings.get(CommonSettings.USE_REPLICA_ID));
            cc.sendBatchJob(bm);
            fail("This should not be allowed.");
        } catch (IllegalState e) {
            assertTrue("The error should say that it is impossible to send " + "batch messages to a checksum replica: "
                    + e.getMessage(), e.getMessage().contains("Trying to execute the batchjob"));
        }

        // Test batch through batch function.
        try {
            cc.sendBatchJob(Channels.getTheCR(), new ChecksumJob());
            fail("This should not be allowed.");
        } catch (IllegalState e) {
            assertTrue("The error should say that it is impossible to send " + "batch messages to a checksum replica: "
                    + e.getMessage(), e.getMessage().contains("Trying to execute the batchjob"));
        }

        // Test GetMessage
        try {
            GetMessage gm = new GetMessage(Channels.getTheCR(), Channels.getError(), "filename.arc", 0);
            cc.sendGetMessage(gm);
            fail("This should not be allowed.");
        } catch (IllegalState e) {
            assertTrue("The error should say that it is impossible to send " + "GetMessages to a checksum replica: "
                    + e.getMessage(), e.getMessage().contains("A checksum replica cannot handle a GetMessage"));
        }

        // Test GetFileMessage
        try {
            GetFileMessage gfm = new GetFileMessage(Channels.getTheCR(), Channels.getError(), "filename.arc",
                    Settings.get(CommonSettings.USE_REPLICA_ID));
            cc.sendGetFileMessage(gfm);
            fail("This should not be allowed.");
        } catch (IllegalState e) {
            assertTrue("The erro should say that it is impossible to send GetFileMessages to a checksum replica", e
                    .getMessage().contains("A checksum replica cannot handle a GetFileMessage"));
        }

        // Test RemoveAndGetFileMessage
        try {
            RemoveAndGetFileMessage ragfm = new RemoveAndGetFileMessage(Channels.getTheCR(), Channels.getError(),
                    "filename.arc", Settings.get(CommonSettings.USE_REPLICA_ID), "checksum", "credentials");
            cc.sendRemoveAndGetFileMessage(ragfm);
            fail("This should not be allowed.");
        } catch (IllegalState e) {
            assertTrue("The erro should say that it is impossible to send GetFileMessages to a checksum replica", e
                    .getMessage().contains("A checksum replica cannot handle a RemoveAndGetFileMessage"));
        }
    }

    @Test
    public void testValid() {
        ChecksumClient cc = ChecksumClient.getInstance(Channels.getTheCR());

        assertEquals("A checksum replica should be of the type CHECKSUM", cc.getType(), ReplicaType.CHECKSUM);

        // check upload
        cc.sendUploadMessage(RemoteFileFactory.getInstance(TestInfo.UPLOADMESSAGE_TESTFILE_1, true, false, true), "dummy-checksum");
        con.waitForConcurrentTasksToFinish();

        assertEquals("One upload message expected to be sent.", 1, handler.uploadMsg.size());

        // check GetChecksum through function
        NetarkivetMessage msg = cc.sendGetChecksumMessage(Channels.getError(), "filename.arc");
        con.waitForConcurrentTasksToFinish();

        assertEquals("One GetChecksumMessage expected to be sent.", 1, handler.getChecksumMsg.size());
        assertEquals("The received message should be one returned by the function", msg, handler.getChecksumMsg.get(0));

        // check GetChecksum through message
        GetChecksumMessage csMsg = new GetChecksumMessage(Channels.getTheCR(), Channels.getError(), "filename.arc",
                Settings.get(CommonSettings.USE_REPLICA_ID));
        cc.sendGetChecksumMessage(csMsg);
        con.waitForConcurrentTasksToFinish();

        assertEquals("Another GetChecksumMessage expected to be sent.", 2, handler.getChecksumMsg.size());
        assertEquals("The received message should be one returned by the function", csMsg,
                handler.getChecksumMsg.get(1));

        // check GetAllChecksums through message
        GetAllChecksumsMessage gcsMsg = new GetAllChecksumsMessage(Channels.getTheCR(), Channels.getError(),
                Settings.get(CommonSettings.USE_REPLICA_ID));
        cc.sendGetAllChecksumsMessage(gcsMsg);
        con.waitForConcurrentTasksToFinish();

        assertEquals("One GetAllChecksumsMessage expected to be sent.", 1, handler.checksumsMsg.size());
        assertEquals("The received message should be one returned by the function", gcsMsg, handler.checksumsMsg.get(0));

        // check GetAllFilenames through message
        GetAllFilenamesMessage gfsMsg = new GetAllFilenamesMessage(Channels.getTheCR(), Channels.getError(),
                Settings.get(CommonSettings.USE_REPLICA_ID));
        cc.sendGetAllFilenamesMessage(gfsMsg);
        con.waitForConcurrentTasksToFinish();

        assertEquals("One GetAllFilenamesMessage expected to be sent.", 1, handler.filenamesMsg.size());
        assertEquals("The received message should be one returned by the function", gfsMsg, handler.filenamesMsg.get(0));

        // check Correct through message
        CorrectMessage corMsg = new CorrectMessage(Channels.getTheCR(), Channels.getError(), "badChecksum",
                RemoteFileFactory.getInstance(TestInfo.UPLOADMESSAGE_TESTFILE_1, true, false, true),
                Settings.get(CommonSettings.USE_REPLICA_ID), "credentials");
        cc.sendCorrectMessage(corMsg);
        con.waitForConcurrentTasksToFinish();

        assertEquals("One CorrectMessage expected to be sent.", 1, handler.correctMsg.size());
        assertEquals("The received message should be one returned by the function", corMsg, handler.correctMsg.get(0));

    }

    public class MessageTestHandler extends ArchiveMessageHandler {
        public List<UploadMessage> uploadMsg = new ArrayList<UploadMessage>();
        public List<GetAllFilenamesMessage> filenamesMsg = new ArrayList<GetAllFilenamesMessage>();
        public List<GetAllChecksumsMessage> checksumsMsg = new ArrayList<GetAllChecksumsMessage>();
        public List<GetChecksumMessage> getChecksumMsg = new ArrayList<GetChecksumMessage>();
        public List<CorrectMessage> correctMsg = new ArrayList<CorrectMessage>();

        public MessageTestHandler() {
            // System.out.println("MessageTestHandler initiated!");
        }

        public void visit(UploadMessage msg) {
            uploadMsg.add(msg);
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
            return (uploadMsg.size() + filenamesMsg.size() + checksumsMsg.size() + getChecksumMsg.size());
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
