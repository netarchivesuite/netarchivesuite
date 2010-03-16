/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2009 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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

import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.logging.LogManager;

import junit.framework.TestCase;

import dk.netarkivet.common.distribute.ChannelID;
import dk.netarkivet.common.distribute.Channels;
import dk.netarkivet.common.distribute.JMSConnectionMockupMQ;
import dk.netarkivet.common.distribute.NetarkivetMessage;
import dk.netarkivet.common.exceptions.PermissionDenied;
import dk.netarkivet.harvester.datamodel.Job;
import dk.netarkivet.harvester.datamodel.JobStatus;
import dk.netarkivet.harvester.harvesting.distribute.CrawlStatusMessage;
import dk.netarkivet.harvester.harvesting.distribute.DoOneCrawlMessage;
import dk.netarkivet.harvester.harvesting.distribute.MetadataEntry;
import dk.netarkivet.testutils.FileAsserts;
import dk.netarkivet.testutils.LogUtils;
import dk.netarkivet.testutils.preconfigured.ReloadSettings;

public class HarvesterMessageHandlerTester extends TestCase {

    private TestMessageHandler tmh;
    ReloadSettings rs = new ReloadSettings();

    protected void setUp() throws Exception {
        super.setUp();
        rs.setUp();
        JMSConnectionMockupMQ.useJMSConnectionMockupMQ();
        JMSConnectionMockupMQ.clearTestQueues();
        tmh = new TestMessageHandler();
        // Allow reading of the log
        FileInputStream fis = new FileInputStream("tests/dk/netarkivet/testlog.prop");
        LogManager.getLogManager().reset();
        LogManager.getLogManager().readConfiguration(fis);
        fis.close();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        rs.tearDown();
    }

    public final void testOnMessage() {
        TestMessage testMessage = new TestMessage(Channels.getTheRepos(), Channels.getTheBamon(), "42");
        JMSConnectionMockupMQ.updateMsgID(testMessage, "ID89");
        tmh.onMessage(JMSConnectionMockupMQ.getObjectMessage(testMessage));
        assertEquals("Message should have been unpacked and accept() should have been called", testMessage.acceptCalled, 1);
        // test that tmh.onMessage issues a "Invalid message type" warning, if the message embeddded in the TestObjectMessage is not a
        // HarvesterMessage
        IllegalTestMessage illegalMessage = new IllegalTestMessage(Channels.getTheRepos(), Channels.getTheBamon(), "43");
        JMSConnectionMockupMQ.updateMsgID(illegalMessage, "ID90");
        
        tmh.onMessage(JMSConnectionMockupMQ.getObjectMessage(illegalMessage));
        File logfile = new File("tests/testlogs/netarkivtest.log");
        LogUtils.flushLogs(HarvesterMessageHandler.class.getName());
        FileAsserts.assertFileContains("Log should have given warning",
                "WARNING: Invalid message type", logfile);
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
            tmh.visit(new DoOneCrawlMessage(job, Channels.getTheRepos(),
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
            super(to, replyTo);
            this.testID = testID;
        }

        public void accept(HarvesterMessageVisitor v) {
            acceptCalled++;
        }

        public String getTestID() {
            return testID;
        }
    }
    
    private static class IllegalTestMessage extends NetarkivetMessage {
        private String testID;
        public int acceptCalled = 0;

        public IllegalTestMessage(ChannelID to, ChannelID replyTo, String testID) {
            super(to, replyTo);
            this.testID = testID;
        }

        public void accept(HarvesterMessageVisitor v) {
            System.out.println("Spurious call with visitor " + v);
            acceptCalled++;
        }

        public String getTestID() {
            return testID;
        }
    }

    
    
    
}
