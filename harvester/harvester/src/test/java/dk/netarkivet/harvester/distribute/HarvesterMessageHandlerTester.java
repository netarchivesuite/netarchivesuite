/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
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

package dk.netarkivet.harvester.distribute;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

import junit.framework.TestCase;

import dk.netarkivet.common.Settings;
import dk.netarkivet.common.distribute.ChannelID;
import dk.netarkivet.common.distribute.Channels;
import dk.netarkivet.common.distribute.JMSConnectionTestMQ;
import dk.netarkivet.common.distribute.TestObjectMessage;
import dk.netarkivet.common.exceptions.PermissionDenied;
import dk.netarkivet.harvester.datamodel.Job;
import dk.netarkivet.harvester.datamodel.JobStatus;
import dk.netarkivet.harvester.harvesting.distribute.CrawlStatusMessage;
import dk.netarkivet.harvester.harvesting.distribute.DoOneCrawlMessage;
import dk.netarkivet.harvester.harvesting.distribute.MetadataEntry;

public class HarvesterMessageHandlerTester extends TestCase {

    private TestMessageHandler tmh;

    protected void setUp() throws Exception {
        super.setUp();
        JMSConnectionTestMQ.useJMSConnectionTestMQ();
        JMSConnectionTestMQ.clearTestQueues();
        tmh = new TestMessageHandler();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        Settings.reload();
    }

    public final void testOnMessage() {
        TestMessage testMessage = new TestMessage(Channels.getTheArcrepos(), Channels.getTheBamon(), "42");
        JMSConnectionTestMQ.updateMsgID(testMessage, "ID89");
        tmh.onMessage(new TestObjectMessage(testMessage));
        assertEquals("Message should have been unpacked and accept() should have been called", testMessage.acceptCalled, 1);
    }

    /*
     * Class under test for void visit(CrawlStatusMessage)
     */
    public final void testVisitCrawlStatusMessage() {
        try {
            tmh.visit(new CrawlStatusMessage(100L,
                                             JobStatus.SUBMITTED));
            fail("Should have thrown a permission denied.");
        } catch (PermissionDenied e) {
            // Expected
        }
    }

    /*
     * Class under test for void visit(DoOneCrawlMessage)
     */
    public final void testVisitDoOneCrawlMessage()
            throws IllegalAccessException,
                   NoSuchMethodException,
                   InvocationTargetException,
                   InstantiationException {
        Job job = TestInfo.getJob();
        try {
            tmh.visit(new DoOneCrawlMessage(job, Channels.getTheArcrepos(),
                                            new ArrayList<MetadataEntry>()));
            fail("Should have thrown a permission denied.");
        } catch (PermissionDenied e) {
            // Expected
        }
    }

    private static class TestMessageHandler extends HarvesterMessageHandler {
        public TestMessageHandler() {}
    }

    private static class TestMessage extends HarvesterMessage {
        private String testID;
        public int acceptCalled = 0;

        public TestMessage(ChannelID to, ChannelID replyTo, String testID) {
            super(to, replyTo, "NetarkivetMessageTester.TestMessage");
            this.testID = testID;
        }

        public void accept(HarvesterMessageVisitor v) {
            acceptCalled++;
        }

        public String getTestID() {
            return testID;
        }
    }
}
