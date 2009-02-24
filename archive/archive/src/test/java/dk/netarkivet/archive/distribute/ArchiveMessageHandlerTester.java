/*$Id$
* $Revision$
* $Date$
* $Author$
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
package dk.netarkivet.archive.distribute;

import java.io.File;
import java.util.ArrayList;

import junit.framework.TestCase;

import dk.netarkivet.archive.arcrepository.bitpreservation.ChecksumJob;
import dk.netarkivet.archive.bitarchive.distribute.BatchEndedMessage;
import dk.netarkivet.archive.bitarchive.distribute.BatchMessage;
import dk.netarkivet.archive.bitarchive.distribute.BatchReplyMessage;
import dk.netarkivet.archive.bitarchive.distribute.GetFileMessage;
import dk.netarkivet.archive.bitarchive.distribute.GetMessage;
import dk.netarkivet.archive.bitarchive.distribute.HeartBeatMessage;
import dk.netarkivet.common.distribute.ChannelID;
import dk.netarkivet.common.distribute.Channels;
import dk.netarkivet.common.distribute.JMSConnectionTestMQ;
import dk.netarkivet.common.distribute.TestObjectMessage;
import dk.netarkivet.common.exceptions.PermissionDenied;
import dk.netarkivet.testutils.preconfigured.ReloadSettings;

public class ArchiveMessageHandlerTester extends TestCase {

    private TestMessageHandler tmh;
    ReloadSettings rs = new ReloadSettings();

    protected void setUp() throws Exception {
        super.setUp();
        rs.setUp();
        JMSConnectionTestMQ.useJMSConnectionTestMQ();
        JMSConnectionTestMQ.clearTestQueues();
        tmh = new TestMessageHandler();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        rs.tearDown();
    }

    public final void testOnMessage() {
        TestMessage testMessage = new TestMessage(Channels.getTheRepos(), Channels.getTheBamon(), "42");
        JMSConnectionTestMQ.updateMsgID(testMessage, "ID89");
        tmh.onMessage(new TestObjectMessage(testMessage));
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


    private static class TestMessageHandler extends ArchiveMessageHandler {
        public TestMessageHandler() {}
    }

    private static class TestMessage extends ArchiveMessage {
        private String testID;
        public int acceptCalled = 0;

        public TestMessage(ChannelID to, ChannelID replyTo, String testID) {
            super(to, replyTo, "NetarkivetMessageTester.TestMessage");
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
