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
package dk.netarkivet.archive.distribute;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;

import junit.framework.TestCase;

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
import dk.netarkivet.archive.indexserver.distribute.IndexRequestMessage;
import dk.netarkivet.common.distribute.ChannelID;
import dk.netarkivet.common.distribute.Channels;
import dk.netarkivet.common.distribute.ChannelsTester;
import dk.netarkivet.common.distribute.JMSConnectionMockupMQ;
import dk.netarkivet.common.distribute.RemoteFileFactory;
import dk.netarkivet.common.distribute.indexserver.RequestType;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.PermissionDenied;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.batch.ChecksumJob;
import dk.netarkivet.testutils.preconfigured.ReloadSettings;
import dk.netarkivet.testutils.preconfigured.UseTestRemoteFile;

public class ArchiveMessageHandlerTester extends TestCase {

    private TestMessageHandler tmh;
    ReloadSettings rs = new ReloadSettings();
    UseTestRemoteFile rf = new UseTestRemoteFile();

    protected void setUp() throws Exception {
        ChannelsTester.resetChannels();
        super.setUp();
        rs.setUp();
        rf.setUp();
        JMSConnectionMockupMQ.useJMSConnectionMockupMQ();
        JMSConnectionMockupMQ.clearTestQueues();
        tmh = new TestMessageHandler();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        rf.tearDown();
        rs.tearDown();
    }

    public final void testOnMessage() {
        TestMessage testMessage = new TestMessage(Channels.getTheRepos(), Channels.getTheBamon(), "42");
        JMSConnectionMockupMQ.updateMsgID(testMessage, "ID89");
        tmh.onMessage(JMSConnectionMockupMQ.getObjectMessage(testMessage));
        assertEquals("Message should have been unpacked and accept() should have been called", testMessage.acceptCalled, 1);
    }

    /*
     * Class under test for void visit(BatchEndedMessage)
     */
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
    public final void testVisitBatchReplyMessage() {
        try {
            tmh.visit(new BatchReplyMessage(Channels.getTheRepos(), Channels.getTheBamon(), "x",
                    0, new ArrayList<File>(), null));
            fail("Should have thrown a permission denied.");
        } catch (PermissionDenied e) {
            // Expected
        }
    }

    /*
     * Class under test for void visit(GetFileMessage)
     */
    public final void testVisitGetFileMessage() {
        try {
            tmh.visit(new GetFileMessage(Channels.getTheRepos(), Channels.getTheBamon(), "x",
            "ONE"));
            fail("Should have thrown a permission denied.");
        } catch (PermissionDenied e) {
            // Expected
        }
    }

    /*
     * Class under test for void visit(GetMessage)
     */
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
    public final void testVisitStoreMessage() {
        File fil = new File(FileUtils.getTempDir(), "X");
        try {
            fil.createNewFile();
            tmh.visit(new StoreMessage(Channels.getError(), fil));
            fail("Should have thrown a permission denied.");
        } catch (IOException e) {
            throw new IOFailure("Unexpected exception", e);
        } catch (PermissionDenied e) {
            // Expected
            FileUtils.remove(fil);
        }
    }

    /*
     * Class under test for void visit(UploadMessage)
     */
    public final void testVisitUploadMessage() {
        File fil = new File(FileUtils.getTempDir(), "X");
        try {
            fil.createNewFile();
            tmh.visit(new UploadMessage(Channels.getTheBamon(), Channels.getError(), RemoteFileFactory.getInstance(fil, true, false, true)));
            fail("Should have thrown a permission denied.");
        } catch (IOException e) {
            throw new IOFailure("Unexpected exception", e);
        } catch (PermissionDenied e) {
            // Expected
            FileUtils.remove(fil);
        }
    }

    /*
     * Class under test for void visit(AdminDataMessage)
     */
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
    public final void testVisitRemoveAndGetFileMessage() {
        try {
            tmh.visit(new RemoveAndGetFileMessage(Channels.getTheBamon(), Channels.getError(), 
                    "filename", "replicaId", "checksum", "credentials"));
            fail("Should have thrown a permission denied.");
        } catch (PermissionDenied e) {
            // Expected
        }
    }

    /*
     * Class under test for void visit(IndexRequestMessage)
     */
    public final void testVisitIndexRequestMessage() {
        try {
            tmh.visit(new IndexRequestMessage(RequestType.CDX, new HashSet<Long>()));
            fail("Should have thrown a permission denied.");
        } catch (PermissionDenied e) {
            // Expected
        }
    }

    /*
     * Class under test for void visit(CorrectMessage)
     */
    public final void testVisitCorrectMessage() {
        File fil = new File(FileUtils.getTempDir(), "X");
        try {
            fil.createNewFile();
            tmh.visit(new CorrectMessage(Channels.getTheBamon(), Channels.getError(), 
                    "badChecksum", RemoteFileFactory.getInstance(fil, true, false, true),
                    "replicaId", "credentials"));
            fail("Should have thrown a permission denied.");
        } catch (IOException e) {
            throw new IOFailure("Unexpected exception", e);
        } catch (PermissionDenied e) {
            // Expected
            FileUtils.remove(fil);
        }
    }

    /*
     * Class under test for void visit(GetChecksumMessage)
     */
    public final void testVisitGetChecksumMessage() {
        try {
            tmh.visit(new GetChecksumMessage(Channels.getTheBamon(), 
                    Channels.getError(), "filename", "replicaId"));
            fail("Should have thrown a permission denied.");
        } catch (PermissionDenied e) {
            // Expected
        }
    }
    
    /*
     * Class under test for void visit(GetAllChecksumsMessage)
     */
    public final void testVisitGetAllChecksumsMessage() {
        try {
            tmh.visit(new GetAllChecksumsMessage(Channels.getTheBamon(), 
                    Channels.getError(), "replicaId"));
            fail("Should have thrown a permission denied.");
        } catch (PermissionDenied e) {
            // Expected
        }
    }

    /*
     * Class under test for void visit(GetAllFilenamesMessage)
     */
    public final void testVisitGetAllFilenamesMessage() {
        try {
            tmh.visit(new GetAllFilenamesMessage(Channels.getTheBamon(), 
                    Channels.getError(), "replicaId"));
            fail("Should have thrown a permission denied.");
        } catch (PermissionDenied e) {
            // Expected
        }
    }

    private static class TestMessageHandler extends ArchiveMessageHandler {
        public TestMessageHandler() {}
    }

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
