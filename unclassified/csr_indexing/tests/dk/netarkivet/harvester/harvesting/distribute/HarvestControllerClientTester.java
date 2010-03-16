/* File: $Id$
 * Revision: $Revision$
 * Author: $Author$
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
package dk.netarkivet.harvester.harvesting.distribute;


import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.LogManager;

import junit.framework.TestCase;

import dk.netarkivet.common.distribute.ChannelID;
import dk.netarkivet.common.distribute.Channels;
import dk.netarkivet.common.distribute.ChannelsTester;
import dk.netarkivet.common.distribute.JMSConnection;
import dk.netarkivet.common.distribute.JMSConnectionFactory;
import dk.netarkivet.common.distribute.JMSConnectionMockupMQ;
import dk.netarkivet.common.distribute.NetarkivetMessage;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.UnknownID;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.harvester.datamodel.JobPriority;
import dk.netarkivet.testutils.StringAsserts;
import dk.netarkivet.testutils.TestFileUtils;
import dk.netarkivet.testutils.preconfigured.ReloadSettings;


/**
 * Test HarvestControllerClient
 */
public class HarvestControllerClientTester extends TestCase {

    TestInfo info = new TestInfo();

    /* The client and dummy-servers used for testing */
    HarvestControllerClient hcc;
    private List<MetadataEntry> metadata = new ArrayList<MetadataEntry>();
    ReloadSettings rs = new ReloadSettings();


    public HarvestControllerClientTester(String sTestName) {
        super(sTestName);
    }

    /**
     * Sets up the U-test by constructing a HarvestControllerClient and a DummyServer
     * And reloading the testlog.prop-file to ensure a clean log-file at each test !
     */
    public void setUp() throws JMSException, GeneralSecurityException,
            IOException, SQLException, IllegalAccessException,
            NoSuchFieldException, ClassNotFoundException {
        rs.setUp();
        JMSConnectionMockupMQ.useJMSConnectionMockupMQ();
        ChannelsTester.resetChannels();
        TestInfo.WORKING_DIR.mkdirs();
        TestFileUtils.copyDirectoryNonCVS(TestInfo.ORIGINALS_DIR, TestInfo.WORKING_DIR);
        FileInputStream fis = new FileInputStream(TestInfo.TESTLOGPROP);
        LogManager.getLogManager().readConfiguration(fis);
        fis.close();
        hcc = HarvestControllerClient.getInstance();
    }

    /**
     * After test is done close test-objects.
     */
    public void tearDown() throws SQLException, IllegalAccessException, NoSuchFieldException {
        if (hcc != null) {
            hcc.close();
        }
        FileUtils.removeRecursively(TestInfo.WORKING_DIR);
        ChannelsTester.resetChannels();
        rs.tearDown();
    }

    /**
     * Test sending + check that we send
     * a message
     * Uses MessageTestHandler()
     */
    public void testSending() {
        DoOneCrawlMessageListener listener = new DoOneCrawlMessageListener();
        ChannelID result;
        result = Channels.getAnyHighpriorityHaco();
        JMSConnectionFactory.getInstance().setListener(result, listener);
        hcc.doOneCrawl(TestInfo.getJob(), metadata);
        ((JMSConnectionMockupMQ) JMSConnectionFactory.getInstance()).waitForConcurrentTasksToFinish();
        assertEquals("the server should have received exactly 1 message", 1, listener.messages.size());
    }

    /**
     * Test sending + check that we send
     * a message
     * Uses MessageTestHandler()
     */
    public void testSendingToCorrectQueue() {
        //listen to both priority queues
        DoOneCrawlMessageListener listener0 = new DoOneCrawlMessageListener();
        DoOneCrawlMessageListener listener1 = new DoOneCrawlMessageListener();
        ChannelID result1;
        if (JobPriority.HIGHPRIORITY.toString().equals(JobPriority.LOWPRIORITY.toString())) {
            result1 = Channels.getAnyLowpriorityHaco();
        } else
        {
            if (JobPriority.HIGHPRIORITY.toString().equals(JobPriority.HIGHPRIORITY.toString())) {
                result1 = Channels.getAnyHighpriorityHaco();
            } else
            {
                throw new UnknownID(JobPriority.HIGHPRIORITY.toString() + " is not a valid priority");
            }
        }
        JMSConnectionFactory.getInstance().setListener(result1, listener0);
        ChannelID result;
        if (JobPriority.LOWPRIORITY.toString().equals(JobPriority.LOWPRIORITY.toString())) {
            result = Channels.getAnyLowpriorityHaco();
        } else
        {
            if (JobPriority.LOWPRIORITY.toString().equals(JobPriority.HIGHPRIORITY.toString())) {
                result = Channels.getAnyHighpriorityHaco();
            } else
            {
                throw new UnknownID(JobPriority.LOWPRIORITY.toString() + " is not a valid priority");
            }
        }
        JMSConnectionFactory.getInstance().setListener(result, listener1);

        //send a high priority job
        hcc.doOneCrawl(TestInfo.getJob(), metadata);
        ((JMSConnectionMockupMQ) JMSConnectionFactory.getInstance()).waitForConcurrentTasksToFinish();
        assertEquals("The HIGHPRIORITY server should have received exactly 1 message", 1, listener0.messages.size());
        assertEquals("The LOWPRIORITY server should have received exactly 0 messages", 0, listener1.messages.size());

        //reset messages
        listener0.messages = new ArrayList<DoOneCrawlMessage>();
        listener1.messages = new ArrayList<DoOneCrawlMessage>();

        //send a low priority job
        hcc.doOneCrawl(TestInfo.getJobLowPriority(), metadata);
        ((JMSConnectionMockupMQ) JMSConnectionFactory.getInstance()).waitForConcurrentTasksToFinish();
        assertEquals("The HIGHPRIORITY server should have received exactly 0 message", 0, listener0.messages.size());
        assertEquals("The LOWPRIORITY server should have received exactly 1 messages", 1, listener1.messages.size());
    }

    /**
     * Verify handling of NULL value for Job
     * Uses MessageTestHandler()
     */
    public void testNullJob() {
        try {
            hcc.doOneCrawl(null, metadata);
            fail("Should throw ArgumentNotValid on NULL job");
        } catch (ArgumentNotValid e) {
            // expected case
        }
    }

    /**
     * Verify the logger logs every call to all messages available
     * Uses the Clients own MessageHandler
     */
    public void testLogSendingMessage() throws IOException {
        hcc.doOneCrawl(TestInfo.getJob(), metadata);
        ((JMSConnectionMockupMQ) JMSConnectionFactory.getInstance()).waitForConcurrentTasksToFinish();

        StringAsserts.assertStringContains(
                "Logfile does has NOT logged the sending of a DoOneCrawlMessage",
                HarvestControllerClient.sendMessage,
                FileUtils.readFile(TestInfo.LOG_FILE));
    }

    /**
     * Utility class to listen to and record all CrawlStatusMessages
     */
    public class DoOneCrawlMessageListener implements MessageListener {
        public List<DoOneCrawlMessage> messages = new ArrayList<DoOneCrawlMessage>();

        public void onMessage(Message message) {
            NetarkivetMessage naMsg = JMSConnection.unpack(message);
            if (naMsg instanceof DoOneCrawlMessage) {
                DoOneCrawlMessage csm = (DoOneCrawlMessage) naMsg;
                messages.add(csm);
            }
        }
    }
}
