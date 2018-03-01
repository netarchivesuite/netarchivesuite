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
package dk.netarkivet.archive.distribute;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import dk.netarkivet.archive.arcrepository.bitpreservation.AdminDataMessage;
import dk.netarkivet.archive.arcrepository.distribute.StoreMessage;
import dk.netarkivet.archive.bitarchive.distribute.BatchEndedMessage;
import dk.netarkivet.archive.bitarchive.distribute.BatchMessage;
import dk.netarkivet.archive.bitarchive.distribute.BatchReplyMessage;
import dk.netarkivet.archive.bitarchive.distribute.GetFileMessage;
import dk.netarkivet.archive.bitarchive.distribute.GetMessage;
import dk.netarkivet.archive.bitarchive.distribute.HeartBeatMessage;
import dk.netarkivet.archive.bitarchive.distribute.RemoveAndGetFileMessage;
import dk.netarkivet.archive.bitarchive.distribute.UploadMessage;
import dk.netarkivet.archive.checksum.distribute.CorrectMessage;
import dk.netarkivet.archive.checksum.distribute.GetAllChecksumsMessage;
import dk.netarkivet.archive.checksum.distribute.GetAllFilenamesMessage;
import dk.netarkivet.archive.checksum.distribute.GetChecksumMessage;
import dk.netarkivet.common.distribute.ChannelID;
import dk.netarkivet.common.distribute.Channels;
import dk.netarkivet.common.distribute.JMSConnectionMockupMQ;
import dk.netarkivet.common.distribute.RemoteFileFactory;
import dk.netarkivet.common.exceptions.PermissionDenied;
import dk.netarkivet.common.utils.ApplicationUtils;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.batch.ChecksumJob;
import dk.netarkivet.testutils.preconfigured.ReloadSettings;
import dk.netarkivet.testutils.preconfigured.UseTestRemoteFile;

public class ArchiveMessageHandlerTester {

    private TestMessageHandler tmh;
    ReloadSettings rs = new ReloadSettings();
    UseTestRemoteFile rf = new UseTestRemoteFile();

    @Before
    public void setUp() throws Exception {
        Channels.reset();
        // super.setUp();
        rs.setUp();
        rf.setUp();
        JMSConnectionMockupMQ.useJMSConnectionMockupMQ();
        JMSConnectionMockupMQ.clearTestQueues();
        tmh = new TestMessageHandler();
        ApplicationUtils.dirMustExist(FileUtils.getTempDir());
    }

    @After
    public void tearDown() throws Exception {
        // super.tearDown();
        rf.tearDown();
        rs.tearDown();
    }

    @Test
    public final void testOnMessage() {
        TestMessage testMessage = new TestMessage(Channels.getTheRepos(), Channels.getTheBamon(), "42");
        JMSConnectionMockupMQ.updateMsgID(testMessage, "ID89");
        tmh.onMessage(JMSConnectionMockupMQ.getObjectMessage(testMessage));
        assertEquals("Message should have been unpacked and accept() should have been called",
                testMessage.acceptCalled, 1);
    }

    /*
     * Class under test for void visit(BatchEndedMessage)
     */
    @Test
    public final void testVisitBatchEndedMessage() {
        try {
            tmh.visit(new BatchEndedMessage(Channels.getTheRepos(), "x", "x", null));
            fail("Should have thrown a permission denied.");
        } catch (PermissionDenied e) {
            // Expected
        }
    }

    /*
     * Class under test for void visit(BatchMessage)
     */
    @Test
    public final void testVisitBatchMessage() {
        try {
            tmh.visit(new BatchMessage(Channels.getTheRepos(), Channels.getTheBamon(), new ChecksumJob(), "42"));
            fail("Should have thrown a permission denied.");
        } catch (PermissionDenied e) {
            // Expected
        }
    }

    /*
     * Class under test for void visit(BatchReplyMessage)
     */
    @Test
    public final void testVisitBatchReplyMessage() {
        try {
            tmh.visit(new BatchReplyMessage(Channels.getTheRepos(), Channels.getTheBamon(), "x", 0,
                    new ArrayList<File>(), null));
            fail("Should have thrown a permission denied.");
        } catch (PermissionDenied e) {
            // Expected
        }
    }

    /*
     * Class under test for void visit(GetFileMessage)
     */
    @Test
    public final void testVisitGetFileMessage() {
        try {
            tmh.visit(new GetFileMessage(Channels.getTheRepos(), Channels.getTheBamon(), "x", "ONE"));
            fail("Should have thrown a permission denied.");
        } catch (PermissionDenied e) {
            // Expected
        }
    }

    /*
     * Class under test for void visit(GetMessage)
     */
    @Test
    public final void testVisitGetMessage() {
        try {
            tmh.visit(new GetMessage(Channels.getTheRepos(), Channels.getTheBamon(), "x", 0));
            fail("Should have thrown a permission denied.");
        } catch (PermissionDenied e) {
            // Expected
        }
    }

    /*
     * Class under test for void visit(HeartBeatMessage)
     */
    @Test
    public final void testVisitHeartBeatMessage() {
        try {
            tmh.visit(new HeartBeatMessage(Channels.getTheRepos(), "x"));
            fail("Should have thrown a permission denied.");
        } catch (PermissionDenied e) {
            // Expected
        }
    }

    /*
     * Class under test for void visit(StoreMessage)
     */
    @Test
    public final void testVisitStoreMessage() throws IOException {
        File fil = new File(FileUtils.getTempDir(), "X");
        String f = fil.getAbsolutePath();
        try {
            fil.createNewFile();
            tmh.visit(new StoreMessage(Channels.getError(), fil));
            fail("Should have thrown a permission denied for " + f);
        } catch (PermissionDenied e) {
            // Expected
            FileUtils.remove(fil);
        }
    }

    /*
     * Class under test for void visit(UploadMessage)
     */
    @Test
    public final void testVisitUploadMessage() throws IOException {
        File fil = new File(FileUtils.getTempDir(), "X");
        String f = fil.getAbsolutePath();
        try {
            fil.createNewFile();
            tmh.visit(new UploadMessage(Channels.getTheBamon(), Channels.getError(), RemoteFileFactory.getInstance(fil,
                    true, false, true)));
            fail("Should have thrown a permission denied for " + f);
        } catch (PermissionDenied e) {
            // Expected
            FileUtils.remove(fil);
        }
    }

    /*
     * Class under test for void visit(AdminDataMessage)
     */
    @Test
    public final void testAdminDataMessage() {
        try {
            tmh.visit(new AdminDataMessage("x", "y"));
            fail("Should have thrown a permission denied.");
        } catch (PermissionDenied e) {
            // Expected
        }
    }

    /*
     * Class under test for void visit(RemoveAndGetFileMessage)
     */
    @Test
    public final void testVisitRemoveAndGetFileMessage() {
        try {
            tmh.visit(new RemoveAndGetFileMessage(Channels.getTheBamon(), Channels.getError(), "filename", "replicaId",
                    "checksum", "credentials"));
            fail("Should have thrown a permission denied.");
        } catch (PermissionDenied e) {
            // Expected
        }
    }

    /*
     * Class under test for void visit(CorrectMessage)
     */
    @Test
    public final void testVisitCorrectMessage() throws IOException {
        File fil = new File(FileUtils.getTempDir(), "X");
        String f = fil.getAbsolutePath();
        try {
            fil.createNewFile();
            tmh.visit(new CorrectMessage(Channels.getTheBamon(), Channels.getError(), "badChecksum", RemoteFileFactory
                    .getInstance(fil, true, false, true), "replicaId", "credentials"));
            fail("Should have thrown a permission denied for " + f);
        } catch (PermissionDenied e) {
            // Expected
            FileUtils.remove(fil);
        }
    }

    /*
     * Class under test for void visit(GetChecksumMessage)
     */
    @Test
    public final void testVisitGetChecksumMessage() {
        try {
            tmh.visit(new GetChecksumMessage(Channels.getTheBamon(), Channels.getError(), "filename", "replicaId"));
            fail("Should have thrown a permission denied.");
        } catch (PermissionDenied e) {
            // Expected
        }
    }

    /*
     * Class under test for void visit(GetAllChecksumsMessage)
     */
    @Test
    public final void testVisitGetAllChecksumsMessage() {
        try {
            tmh.visit(new GetAllChecksumsMessage(Channels.getTheBamon(), Channels.getError(), "replicaId"));
            fail("Should have thrown a permission denied.");
        } catch (PermissionDenied e) {
            // Expected
        }
    }

    /*
     * Class under test for void visit(GetAllFilenamesMessage)
     */
    @Test
    public final void testVisitGetAllFilenamesMessage() {
        try {
            tmh.visit(new GetAllFilenamesMessage(Channels.getTheBamon(), Channels.getError(), "replicaId"));
            fail("Should have thrown a permission denied.");
        } catch (PermissionDenied e) {
            // Expected
        }
    }

    private static class TestMessageHandler extends ArchiveMessageHandler {
        public TestMessageHandler() {
        }
    }

    @SuppressWarnings({"unused", "serial"})
    private static class TestMessage extends ArchiveMessage {
        private String testID;
        public int acceptCalled = 0;

        public TestMessage(ChannelID to, ChannelID replyTo, String testID) {
            super(to, replyTo);
            this.testID = testID;
        }

        public void accept(ArchiveMessageVisitor v) {
            acceptCalled++;
        }

        public String getTestID() {
            return testID;
        }
    }
}
