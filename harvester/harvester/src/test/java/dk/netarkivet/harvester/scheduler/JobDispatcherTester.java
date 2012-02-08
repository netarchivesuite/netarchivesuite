/* File: $Id$
 * Revision: $Revision$
 * Author: $Author$
 * Date: $Date$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2012 The Royal Danish Library, the Danish State and
 * University Library, the National Library of France and the Austrian
 * National Library.
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
package dk.netarkivet.harvester.scheduler;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.LogManager;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.Queue;
import javax.jms.QueueBrowser;
import javax.jms.QueueReceiver;
import javax.jms.QueueSession;

import junit.framework.TestCase;
import dk.netarkivet.TestUtils;
import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.distribute.ChannelID;
import dk.netarkivet.common.distribute.JMSConnection;
import dk.netarkivet.common.distribute.JMSConnectionFactory;
import dk.netarkivet.common.distribute.JMSConnectionMockupMQ;
import dk.netarkivet.common.distribute.JMSConnectionMockupMQ.TestObjectMessage;
import dk.netarkivet.common.distribute.NetarkivetMessage;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.RememberNotifications;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.harvester.datamodel.DataModelTestCase;
import dk.netarkivet.harvester.datamodel.DatabaseTestUtils;
import dk.netarkivet.harvester.datamodel.Domain;
import dk.netarkivet.harvester.datamodel.DomainConfiguration;
import dk.netarkivet.harvester.datamodel.DomainDAO;
import dk.netarkivet.harvester.datamodel.HarvestDefinitionDAO;
import dk.netarkivet.harvester.datamodel.Job;
import dk.netarkivet.harvester.datamodel.JobDAO;
import dk.netarkivet.harvester.datamodel.JobPriority;
import dk.netarkivet.harvester.datamodel.JobStatus;
import dk.netarkivet.harvester.harvesting.distribute.DoOneCrawlMessage;
import dk.netarkivet.harvester.harvesting.distribute.JobChannelUtil;
import dk.netarkivet.harvester.harvesting.distribute.MetadataEntry;
import dk.netarkivet.harvester.webinterface.DomainDefinition;
import dk.netarkivet.testutils.StringAsserts;
import dk.netarkivet.testutils.TestFileUtils;
import dk.netarkivet.testutils.preconfigured.MockupJMS;
import dk.netarkivet.testutils.preconfigured.ReloadSettings;

/**
 * Test JobDispatcher class.
 */
public class JobDispatcherTester extends TestCase {
    /** The JobDispatcher used for testing. */
    JobDispatcher jobDispatcher;

    ReloadSettings reloadSettings = new ReloadSettings();
    MockupJMS jms = new MockupJMS();

    public JobDispatcherTester(String sTestName) {
        super(sTestName);
    }

    public void setUp() throws Exception {
        reloadSettings.setUp();
        jms.setUp();
        FileUtils.removeRecursively(TestInfo.WORKING_DIR);
        TestInfo.WORKING_DIR.mkdirs();
        TestFileUtils.copyDirectoryNonCVS(TestInfo.ORIGINALS_DIR,
                TestInfo.WORKING_DIR);
        FileInputStream testLogPropertiesStream = new FileInputStream(
                TestInfo.TESTLOGPROP);
        LogManager.getLogManager().readConfiguration(testLogPropertiesStream);
        testLogPropertiesStream.close();
        Settings.set(CommonSettings.DB_BASE_URL, "jdbc:derby:"
                + TestInfo.WORKING_DIR.getCanonicalPath() + "/fullhddb");
        DatabaseTestUtils.getHDDB(new File(TestInfo.BASEDIR, "fullhddb.jar"),
                "fullhddb", TestInfo.WORKING_DIR);

        Settings.set(CommonSettings.NOTIFICATIONS_CLASS,
                RememberNotifications.class.getName());

        HarvestDefinitionDAO.getInstance();
        jobDispatcher = new JobDispatcher(jms.getJMSConnection());

    }

    /**
     * After test is done close test-objects.
     */
    public void tearDown() throws SQLException, IllegalAccessException,
            NoSuchFieldException {
        DatabaseTestUtils.dropHDDB();
        FileUtils.removeRecursively(TestInfo.WORKING_DIR);
        TestUtils.resetDAOs();
        jms.tearDown();
        reloadSettings.tearDown();
    }

    /**
     * Test that runNewJobs skips bad jobs without crashing (bug #627). TODO The
     * setActualStop/setActualStart no longer throws exception, so we need to
     * find a way making jobs bad
     *
     * @throws Exception
     *             if HarvestScheduler throws exception
     */
    public void testSubmitNewJobs() throws Exception {

        createMessageReceiver(JobPriority.HIGHPRIORITY).receiveNoWait();

        Job job = createJob();
        jobDispatcher.submitNextNewJob(JobPriority.HIGHPRIORITY);
        Job rereadJob = JobDAO.getInstance().read(job.getJobID());
        assertEquals("Good job should have been scheduled",
                JobStatus.SUBMITTED, rereadJob.getStatus());
    }

    /**
     * Test that runNewJobs generates correct alias information for the job.
     *
     * @throws Exception
     *             if HarvestScheduler throws exception
     */
    public void testSubmitNewJobsMakesAliasInfo() throws Exception {
        // Add a listener to see what is sent
        TestMessageListener hacoListener = new TestMessageListener();

        jms.getJMSConnection().setListener(
                JobChannelUtil.getChannel(JobPriority.HIGHPRIORITY),
                hacoListener);

        // Create the following domains:
        // kb.dk with aliases alias1.dk and alias2.dk
        // dr.dk with alias alias3.dk
        DomainDefinition.createDomains("alias1.dk", "alias2.dk", "alias3.dk",
                "kb.dk", "dr.dk");
        DomainDAO ddao = DomainDAO.getInstance();
        Domain d = ddao.read("alias1.dk");
        d.updateAlias("kb.dk");
        ddao.update(d);
        d = ddao.read("alias2.dk");
        d.updateAlias("kb.dk");
        ddao.update(d);
        d = ddao.read("alias3.dk");
        d.updateAlias("dr.dk");
        ddao.update(d);
        d = ddao.read("kb.dk");
        DomainConfiguration dc1 = d.getDefaultConfiguration();
        d = ddao.read("dr.dk");
        DomainConfiguration dc2 = d.getDefaultConfiguration();

        // Make a job from dr.dk and kb.dk
        DataModelTestCase.addHarvestDefinitionToDatabaseWithId(5678L);
        Job job = Job.createJob(5678L, dc1, 0);
        job.addConfiguration(dc2);
        JobDAO.getInstance().create(job);

        jobDispatcher.submitNextNewJob(JobPriority.HIGHPRIORITY);

        DoOneCrawlMessage crawlMessage = (DoOneCrawlMessage) 
                hacoListener.getReceived();
        assertEquals("Should have 1 metadata entry, but got "
                + crawlMessage.getMetadata(), 1, crawlMessage.getMetadata()
                .size());
        MetadataEntry metadataEntry = crawlMessage.getMetadata().get(0);
        assertNotNull("Should have 1 metadata entry", metadataEntry);
        assertEquals("Should have mimetype text/plain", "text/plain",
                metadataEntry.getMimeType());
        assertEquals("Should have right url",
                "metadata://netarkivet.dk/crawl/setup/aliases"
                        + "?majorversion=1&minorversion=0"
                        + "&harvestid=5678&harvestnum=0&jobid=1", metadataEntry
                        .getURL());
        assertEquals("Should have right data",
                "alias3.dk is an alias for dr.dk\n"
                        + "alias1.dk is an alias for kb.dk\n"
                        + "alias2.dk is an alias for kb.dk\n", new String(
                        metadataEntry.getData()));
    }

    /**
     * Test that runNewJobs makes correct duplication reduction information.
     */
    public void testSubmitNewJobsMakesDuplicateReductionInfo()
    throws Exception {
        QueueReceiver messageReceiver =
            createMessageReceiver(JobPriority.LOWPRIORITY);

        // Make some jobs to submit
        // Assume 1st jobId is 2, and lastId is 15
        DataModelTestCase.createTestJobs(1L, 14L);

        // Submit all the jobs, and hold on to the last one
        DoOneCrawlMessage crawlMessage = null;
        int counter = 0;
        jobDispatcher.submitNextNewJob(JobPriority.LOWPRIORITY);
        while (countQueueMessages(JobPriority.LOWPRIORITY) > 0) {
            counter++;
            TestObjectMessage testObjectMessage = ((TestObjectMessage)
                    messageReceiver.receiveNoWait());
            crawlMessage = (DoOneCrawlMessage)testObjectMessage.getObject();
            jobDispatcher.submitNextNewJob(JobPriority.LOWPRIORITY);
        }
        // Check result
        assertEquals("Should have received all low priority messages", 8,
                counter);
        assertEquals("Should have 1 metadata entry in last received message",
                1, crawlMessage.getMetadata().size());
        MetadataEntry metadataEntry = crawlMessage.getMetadata().get(0);
        assertNotNull("Should have 1 metadata entry", metadataEntry);
        assertEquals("Should have mimetype text/plain", "text/plain",
                metadataEntry.getMimeType());
        assertEquals("Should have right url",
                "metadata://netarkivet.dk/crawl/setup/duplicatereductionjobs"
                        + "?majorversion=1&minorversion=0"
                        + "&harvestid=6&harvestnum=0&jobid=14", metadataEntry
                        .getURL());
        assertEquals("Should have right data", "7,8,9,10,11,12", new String(
                metadataEntry.getData()));
    }

    /**
     * Test sending + check that we send a message
     * Uses MessageTestHandler()
     */
    public void testSendingToCorrectQueue() {
        //listen to both priority queues
        DoOneCrawlMessageListener highPriorityListener =
            new DoOneCrawlMessageListener();
        JMSConnectionFactory.getInstance().setListener(
                JobChannelUtil.getChannel(JobPriority.HIGHPRIORITY),
                highPriorityListener);

        DoOneCrawlMessageListener lowPriorityListener =
            new DoOneCrawlMessageListener();
        JMSConnectionFactory.getInstance().setListener(JobChannelUtil.
                getChannel(JobPriority.LOWPRIORITY), lowPriorityListener);

        //send a high priority job
        jobDispatcher.doOneCrawl(TestInfo.getJob(),
                "test", "test", "test", new ArrayList<MetadataEntry>());
        ((JMSConnectionMockupMQ) JMSConnectionFactory.getInstance()).
        waitForConcurrentTasksToFinish();
        assertEquals("The HIGHPRIORITY server should have received exactly 1 " +
        		"message", 1, highPriorityListener.messages.size());
        assertEquals("The LOWPRIORITY server should have received exactly 0 " +
        		"messages", 0, lowPriorityListener.messages.size());

        //reset messages
        highPriorityListener.messages = new ArrayList<DoOneCrawlMessage>();
        lowPriorityListener.messages = new ArrayList<DoOneCrawlMessage>();

        //send a low priority jobList
        jobDispatcher.doOneCrawl(TestInfo.getJobLowPriority(),
                "test", "test", "test", new ArrayList<MetadataEntry>());
        ((JMSConnectionMockupMQ) JMSConnectionFactory.getInstance()).
        waitForConcurrentTasksToFinish();
        assertEquals("The HIGHPRIORITY server should have received exactly 0 " +
        		"message", 0, highPriorityListener.messages.size());
        assertEquals("The LOWPRIORITY server should have received exactly 1 " +
        		"messages", 1, lowPriorityListener.messages.size());
    }

    /**
     * Verify handling of NULL value for Job
     * Uses MessageTestHandler()
     */
    public void testNullJob() {
        try {
            jobDispatcher.doOneCrawl(null, "test", "test", "test", 
                    new ArrayList<MetadataEntry>());
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
        jobDispatcher.doOneCrawl(TestInfo.getJob(),
                "test", "test", "test", new ArrayList<MetadataEntry>());
        ((JMSConnectionMockupMQ) JMSConnectionFactory.getInstance()).
        waitForConcurrentTasksToFinish();

        StringAsserts.assertStringContains(
                "Logfile does has NOT logged the sending of a " +
                "DoOneCrawlMessage", "Send crawl request",
                FileUtils.readFile(TestInfo.LOG_FILE));
    }

    /**
     * Utility class to listen to and record all CrawlStatusMessages
     */
    public class DoOneCrawlMessageListener implements MessageListener {
        public List<DoOneCrawlMessage> messages =
            new ArrayList<DoOneCrawlMessage>();

        public void onMessage(Message message) {
            NetarkivetMessage naMsg = JMSConnection.unpack(message);
            if (naMsg instanceof DoOneCrawlMessage) {
                DoOneCrawlMessage csm = (DoOneCrawlMessage) naMsg;
                messages.add(csm);
            }
        }
    }

    /**
     * MessageListener used locally to intercept messages sent
     * by the HarvestScheduler.
     */
    private class TestMessageListener implements MessageListener {
        private BlockingQueue<NetarkivetMessage> received =
            new LinkedBlockingQueue<NetarkivetMessage>();

        public void onMessage(Message msg) {
            synchronized (this) {
                NetarkivetMessage content = JMSConnection.unpack(msg);
                received.add(content);
                this.notifyAll();
            }
        }

        /**
         * @return Any received message. <p>
         * 
         * Will wait up to 1 second for a message. This is to handle any 
         * asynchronicity in the message dispatching.   
         */
        public NetarkivetMessage getReceived() {
            try {
                return received.poll(1, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                throw new RuntimeException("Why did this happen?");
            }
        }

        public int getNumReceived() {
            return received.size();
        }
    }

    /**
     * Creates a high priority job in to database.
     * @param status The Job status to assign the job
     */
    private Job createJob(JobStatus status) {
        Iterator<Domain> domainsIterator =
            DomainDAO.getInstance().getAllDomains();
        DomainConfiguration cfg =
            domainsIterator.next().getDefaultConfiguration();
        final JobDAO jdao = JobDAO.getInstance();

        final Long harvestID = 1L;
        Job newJob = Job.createJob(harvestID, cfg, 1);
        newJob.setStatus(status);
        jdao.create(newJob);
        jdao.getAllJobIds(status);
        return newJob;
    }
    
    /**
     * Creates a new high priority job in to database.
     * @param status The Job status to assign the job
     */
    private Job createJob() {
        return createJob(JobStatus.NEW);
    }

    /**
     * Creates a <code>QueueReceiver</code> which removes messages from the
     * queue when receive is called (using the test listener will not remove
     * messages from the queue)
     * @return
     * @throws JMSException
     */
    private QueueReceiver createMessageReceiver(JobPriority priority)
    throws JMSException {
        QueueSession qSession =
            JMSConnectionMockupMQ.getInstance().getQueueSession();
        ChannelID channelId =
            JobChannelUtil.getChannel(priority);
        Queue queue = qSession.createQueue(channelId.getName());
        return qSession.createReceiver(queue);
    }

    private int countQueueMessages(JobPriority priority) throws JMSException {
        ChannelID channelId =
            JobChannelUtil.getChannel(priority);
        QueueBrowser qBrowser =
            JMSConnectionMockupMQ.getInstance().createQueueBrowser(channelId);
        Enumeration<?> messageEnumeration = qBrowser.getEnumeration();
        int numberOfMessages = 0;
        while (messageEnumeration.hasMoreElements()) {
            messageEnumeration.nextElement();
            numberOfMessages++;
        }
        return numberOfMessages;
    }
}
