/* File: $Id$
 * Revision: $Revision$
 * Author: $Author$
 * Date: $Date$
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
package dk.netarkivet.harvester.scheduler;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
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
import dk.netarkivet.common.distribute.NetarkivetMessage;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.IteratorUtils;
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
import dk.netarkivet.harvester.datamodel.JobStatusInfo;
import dk.netarkivet.harvester.harvesting.distribute.DoOneCrawlMessage;
import dk.netarkivet.harvester.harvesting.distribute.JobChannelUtil;
import dk.netarkivet.harvester.harvesting.distribute.MetadataEntry;
import dk.netarkivet.harvester.scheduler.HarvestJobGenerator.JobGeneratorTask;
import dk.netarkivet.harvester.webinterface.DomainDefinition;
import dk.netarkivet.harvester.webinterface.HarvestStatusQuery;
import dk.netarkivet.testutils.ReflectUtils;
import dk.netarkivet.testutils.StringAsserts;
import dk.netarkivet.testutils.TestFileUtils;
import dk.netarkivet.testutils.preconfigured.MockupJMS;
import dk.netarkivet.testutils.preconfigured.ReloadSettings;

/**
 * Test HarvestScheduler class.
 */
public class HarvestSchedulerTester extends TestCase {
    TestInfo info = new TestInfo();

    /** The harvestScheduler used for testing. */
    HarvestScheduler harvestScheduler;
    JobGeneratorTask jobGeneratorTask = new JobGeneratorTask();

    ReloadSettings reloadSettings = new ReloadSettings();
    MockupJMS jmsConnection = new MockupJMS();
    private QueueReceiver messageReceiver;

    private List<MetadataEntry> metadata = new ArrayList<MetadataEntry>();

    public HarvestSchedulerTester(String sTestName) {
        super(sTestName);
    }

    public void setUp() throws Exception {
        reloadSettings.setUp();
        jmsConnection.setUp();
        FileUtils.removeRecursively(TestInfo.WORKING_DIR);
        TestInfo.WORKING_DIR.mkdirs();
        TestFileUtils.copyDirectoryNonCVS(TestInfo.ORIGINALS_DIR,
                TestInfo.WORKING_DIR);
        FileInputStream testLogPropertiesStream = new FileInputStream(
                TestInfo.TESTLOGPROP);
        LogManager.getLogManager().readConfiguration(testLogPropertiesStream);
        testLogPropertiesStream.close();
        Settings.set(CommonSettings.DB_URL, "jdbc:derby:"
                + TestInfo.WORKING_DIR.getCanonicalPath() + "/fullhddb");
        DatabaseTestUtils.getHDDB(new File(TestInfo.BASEDIR, "fullhddb.jar"),
                "fullhddb", TestInfo.WORKING_DIR);

        TestUtils.resetDAOs();

        Settings.set(CommonSettings.NOTIFICATIONS_CLASS,
                RememberNotifications.class.getName());

        harvestScheduler = new HarvestScheduler();

        HarvestJobGeneratorTest.generateJobs();
        
        messageReceiver = createMessageReceiver();
    }

    /**
     * After test is done close test-objects.
     */
    public void tearDown() throws SQLException, IllegalAccessException,
            NoSuchFieldException {
        harvestScheduler.shutdown();
        DatabaseTestUtils.dropHDDB();
        FileUtils.removeRecursively(TestInfo.WORKING_DIR);
        TestUtils.resetDAOs();
        jmsConnection.tearDown();
        reloadSettings.tearDown();
    }

    /**
     * Test that running the scheduler creates certain jobs.
     * 
     * @throws Exception If HarvestScheduler throws exception
     */
    public void testBeginDispatching() throws Exception {
        JobDAO dao = JobDAO.getInstance();

        TestMessageListener hacoListener = new TestMessageListener();
        
        JMSConnectionMockupMQ.getInstance().setListener(
                JobChannelUtil.getChannel(JobPriority.HIGHPRIORITY), 
                hacoListener);

        startHarvestScheduler();

        assertEquals(
                "Should have created one job after starting job dispatching",
                1, dao.getCountJobs());
        ((JMSConnectionMockupMQ) JMSConnectionFactory.getInstance())
                .waitForConcurrentTasksToFinish();
        List<Job> jobs = IteratorUtils.toList(dao.getAll(JobStatus.NEW));
        assertEquals("No jobs should be left with status new", 0, jobs.size());
        assertEquals("One job should have been created and submitted", 1,
                IteratorUtils.toList(dao.getAll(JobStatus.SUBMITTED)).size());
        assertNotNull("Should have received a message", hacoListener
                .getReceived());
        assertTrue("Message received should be a DoOneCrawlMessage",
                hacoListener.getReceived() instanceof DoOneCrawlMessage);
        assertEquals("Should have received exactly one message", 1,
                hacoListener.getNumReceived());
    }

    /**
     * Test private method getHoursPassedSince().
     * 
     * @throws Exception
     *             if HarvestScheduler throws exception
     */
    public void testGetHoursPassedSince() throws Exception {
        Calendar calendar = new GregorianCalendar();
        calendar.add(Calendar.MINUTE, 45);
        assertEquals("Should return -1 on date after now", -1,
                getHoursPassedSince(calendar.getTime()));

        calendar.add(Calendar.HOUR, -1);
        assertEquals("Should return 0 on date close to now", 0,
                getHoursPassedSince(calendar.getTime()));

        calendar.add(Calendar.HOUR, -12);
        assertEquals("Should return 12 on date 12 hours before", 12,
                getHoursPassedSince(calendar.getTime()));
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
        clearNewJobs();
        // Create a bad job.
        final DomainDAO dao = DomainDAO.getInstance();
        Iterator<Domain> domainsIterator = dao.getAllDomains();
        assertTrue("Should be at least one domain in domains table",
                domainsIterator.hasNext());
        DomainConfiguration cfg = domainsIterator.next()
                .getDefaultConfiguration();
        DataModelTestCase.addHarvestDefinitionToDatabaseWithId(7000L);
        Job bad = Job.createJob(7000L, cfg, 1);
        bad.setStatus(JobStatus.NEW);
        bad.setActualStart(new Date());
        Field stopField = ReflectUtils.getPrivateField(Job.class, "actualStop");
        Date early = new Date();
        early.setTime(early.getTime() - 10000);
        stopField.set(bad, early);
        final JobDAO jdao = JobDAO.getInstance();
        jdao.create(bad);
        submitNewJobs();
        messageReceiver.receiveNoWait();
        
        Job good = Job.createJob(1L, cfg, 1);
        good.setStatus(JobStatus.NEW);
        jdao.create(good);
        submitNewJobs();
        Job newGood = jdao.read(good.getJobID());
        assertEquals("Good job should have been scheduled",
                JobStatus.SUBMITTED, newGood.getStatus());

        // TODO: Should also check that a readable but unschedulable job fails,
        // that would require a connection that throws exceptions sometimes.
    }

    /**
     * Test that runNewJobs generates correct alias information for the job.
     * 
     * @throws Exception
     *             if HarvestScheduler throws exception
     */
    public void testSubmitNewJobsMakesAliasInfo() throws Exception {
        clearNewJobs();

        // Add a listener to see what is sent
        TestMessageListener hacoListener = new TestMessageListener();

        JMSConnectionMockupMQ.getInstance().setListener(
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

        submitNewJobs();

        ((JMSConnectionMockupMQ) JMSConnectionMockupMQ.getInstance())
                .waitForConcurrentTasksToFinish();

        assertEquals("Haco listener should have received one message", 1,
                hacoListener.getNumReceived());
        DoOneCrawlMessage crawlMessage = (DoOneCrawlMessage) hacoListener
                .getReceived();
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
                        + "&harvestid=5678&harvestnum=0&jobid=2", metadataEntry
                        .getURL());
        assertEquals("Should have right data",
                "alias3.dk is an alias for dr.dk\n"
                        + "alias1.dk is an alias for kb.dk\n"
                        + "alias2.dk is an alias for kb.dk\n", new String(
                        metadataEntry.getData()));
    }

    /**
     * Test that runNewJobs makes correct duplication reduction information.
     * 
     * @throws Exception
     *             if HarvestScheduler throws exception
     */
    public void testSubmitNewJobsMakesDuplicateReductionInfo() 
    throws Exception {
        clearNewJobs();

        // Make some jobs to submit
        // Assume 1st jobId is 2, and lastId is 15
        DataModelTestCase.createTestJobs(2L, 15L);

        // Add a listener to see what is sent
        TestMessageListener hacoListener = new TestMessageListener();
        
        JMSConnectionMockupMQ.getInstance().setListener(
                JobChannelUtil.getChannel(JobPriority.HIGHPRIORITY), 
                hacoListener);
        JMSConnectionMockupMQ.getInstance().setListener(
                JobChannelUtil.getChannel(JobPriority.LOWPRIORITY), 
                hacoListener);

        submitNewJobs();
        ((JMSConnectionMockupMQ) JMSConnectionMockupMQ.getInstance())
                .waitForConcurrentTasksToFinish();

        // Check result
        assertEquals("Haco listener should have received all messages", 14,
                hacoListener.getNumReceived());
        DoOneCrawlMessage crawlMessage = (DoOneCrawlMessage) hacoListener
                .getReceived();
        assertEquals("Should have 1 metadata entry in last received message",
                1, crawlMessage.getMetadata().size());
        MetadataEntry metadataEntry = crawlMessage.getMetadata().get(0);
        assertNotNull("Should have 1 metadata entry", metadataEntry);
        assertEquals("Should have mimetype text/plain", "text/plain",
                metadataEntry.getMimeType());
        assertEquals("Should have right url",
                "metadata://netarkivet.dk/crawl/setup/duplicatereductionjobs"
                        + "?majorversion=1&minorversion=0"
                        + "&harvestid=6&harvestnum=0&jobid=15", metadataEntry
                        .getURL());
        assertEquals("Should have right data", "8,9,10,11,12,13", new String(
                metadataEntry.getData()));
    }

    public void testStoppedOldJobs() throws Exception {
        final DomainDAO dao = DomainDAO.getInstance();
        Iterator<Domain> domainsIterator = dao.getAllDomains();
        assertTrue("Should be at least one domain in domains table",
                domainsIterator.hasNext());
        DomainConfiguration cfg = domainsIterator.next()
                .getDefaultConfiguration();
        final JobDAO jdao = JobDAO.getInstance();

        final Long harvestID = 1L;
        // Verify that harvestDefinition with ID=1L exists
        assertTrue("harvestDefinition with ID=" + harvestID
                + " does not exist, but should have", HarvestDefinitionDAO
                .getInstance().exists(harvestID));
        // Create 6 jobs - with start time minus 60*60*24*7 +1
        // (one week plus one second)
        for (int i = 0; i < 6; i++) {
            Job newJob = Job.createJob(harvestID, cfg, 1);
            newJob.setActualStart(new Date((new Date()).getTime()
                    - (604801 * 1000)));
            newJob.setStatus(JobStatus.STARTED);
            jdao.create(newJob);
        }
        // Create 6 new jobs with now
        for (int i = 0; i < 6; i++) {
            Job newJob = Job.createJob(harvestID, cfg, 1);
            newJob.setActualStart(new Date());
            newJob.setStatus(JobStatus.STARTED);
            jdao.create(newJob);
        }
        List<JobStatusInfo> oldInfos = jdao.getStatusInfo(
                new HarvestStatusQuery()).getJobStatusInfo();
        // Since initial DB contains one NEW job, we now have one of each
        // status plus one extra NEW (i.e. 7 jobs).
        assertTrue("There should have been 13 jobs now, but there was "
                + oldInfos.size(), oldInfos.size() == 13);

        Iterator<Long> ids = jdao.getAllJobIds(JobStatus.STARTED);
        int size = 0;
        while (ids.hasNext()) {
            ids.next();
            size++;
        }
        assertTrue("There should be 12 jobs with status STARTED, there are "
                + size, size == 12);
        harvestScheduler.dispatchJobs();

        // check that we have 6 failed and 6 submitted job after we have stopped
        // old jobs
        ids = jdao.getAllJobIds(JobStatus.STARTED);
        size = 0;
        while (ids.hasNext()) {
            ids.next();
            size++;
        }
        assertTrue("There should be 6 jobs with status STARTED, there are "
                + size, size == 6);
        ids = jdao.getAllJobIds(JobStatus.FAILED);
        size = 0;
        while (ids.hasNext()) {
            ids.next();
            size++;
        }
        assertTrue("There should be 6 jobs with status FAILED, there are "
                + size, size == 6);

    }

    /**
     * Unit test testing the private method rescheduleJob.
     * 
     * @throws Exception
     *             if HarvestScheduler throws exception
     */
    public void testRescheduleSubmittedJobs() throws Exception {
        final DomainDAO dao = DomainDAO.getInstance();
        Iterator<Domain> domainsIterator = dao.getAllDomains();
        assertTrue("Should be at least one domain in domains table",
                domainsIterator.hasNext());
        DomainConfiguration cfg = domainsIterator.next()
                .getDefaultConfiguration();
        final JobDAO jdao = JobDAO.getInstance();

        final Long harvestID = 1L;
        // Verify that harvestDefinition with ID=1L exists
        assertTrue("harvestDefinition with ID=" + harvestID
                + " does not exist, but should have", HarvestDefinitionDAO
                .getInstance().exists(harvestID));
        // Create 6 jobs, one in each JobStatus:
        // (NEW, SUBMITTED, STARTED, DONE, FAILED, RESUBMITTED)
        for (JobStatus status : JobStatus.values()) {
            Job newJob = Job.createJob(harvestID, cfg, 1);
            newJob.setStatus(status);
            jdao.create(newJob);
        }

        List<JobStatusInfo> oldInfos = jdao.getStatusInfo(
                new HarvestStatusQuery()).getJobStatusInfo();
        // Since initial DB contains one NEW job, we now have one of each
        // status plus one extra NEW (i.e. 7 jobs).

        assertTrue("There should have been 7 jobs now, but there was "
                + oldInfos.size(), oldInfos.size() == 7);

        rescheduleSubmittedJobs();
        List<JobStatusInfo> newInfos = jdao.getStatusInfo(
                new HarvestStatusQuery()).getJobStatusInfo();
        // Check that all old jobs are there, with one changed status
        OLDS: for (JobStatusInfo oldInfo : oldInfos) {
            for (JobStatusInfo newInfo : newInfos) {
                if (newInfo.getJobID() == oldInfo.getJobID()) {
                    if (oldInfo.getStatus() == JobStatus.SUBMITTED) {
                        assertEquals("SUBMITTED job should be RESUBMITTED",
                                JobStatus.RESUBMITTED, newInfo.getStatus());
                    } else {
                        assertEquals("Non-SUBMITTED job should be unchanged",
                                oldInfo.getStatus(), newInfo.getStatus());
                    }
                    continue OLDS;
                }
            }
            fail("Job " + oldInfo + " has disappeared!");
        }

        // Check that a new job is there, in status submitted
        boolean foundNewJob = false;
        NEWS: for (JobStatusInfo newInfo : newInfos) {
            for (JobStatusInfo oldInfo : oldInfos) {
                if (newInfo.getJobID() == oldInfo.getJobID()) {
                    continue NEWS;
                }
            }
            // This new job was not found in old jobs list.
            foundNewJob = true;
            assertEquals("Newly created job should be in status NEW",
                    JobStatus.NEW, newInfo.getStatus());
        }
        assertTrue("Should have found new job", foundNewJob);
    }

    /**
     * Verifies that new crawler jobs are only dispatched when the message queue
     *  to the harvest servers are empty
     * @throws Exception 
     */
    public void testJitHarvestJobDispatching() throws Exception {
        clearNewJobs();

        assertEquals("Message queue should be empty", 
                0, countQueueMessages());
        
        Job firstJob = createJob(JobStatus.NEW);        
        submitNewJobs();

        assertEquals("Message queue should have received a message", 
                1, countQueueMessages());
        
        Job secondJob = createJob(JobStatus.NEW);        
        submitNewJobs();
        assertEquals("New job should not have been submittet to non-empty " +
        		"message queue", 
                1, countQueueMessages());   
        assertEquals("Second job should still have status new",
                JobStatus.NEW, secondJob.getStatus());
        
        messageReceiver.receiveNoWait();
        assertEquals("Message should have been removed from queue", 
        0, countQueueMessages());         

        submitNewJobs();
        assertEquals("Message queue should have received a message for the " +
        		"next job", 1, countQueueMessages());
        assertEquals("Second job should have been marked as submitted",
                JobStatus.NEW, secondJob.getStatus());
    }
    

    /**
     * Test sending + check that we send a message
     * Uses MessageTestHandler()
     */
    public void testSendingToCorrectQueue() {
        //listen to both priority queues
        DoOneCrawlMessageListener highPriorityListener = new DoOneCrawlMessageListener();
        JMSConnectionFactory.getInstance().setListener(JobChannelUtil.getChannel(JobPriority.HIGHPRIORITY), highPriorityListener);
        
        DoOneCrawlMessageListener lowPriorityListener = new DoOneCrawlMessageListener();
        JMSConnectionFactory.getInstance().setListener(JobChannelUtil.getChannel(JobPriority.LOWPRIORITY), lowPriorityListener);
        
        //send a high priority job
        harvestScheduler.doOneCrawl(TestInfo.getJob(), metadata);
        ((JMSConnectionMockupMQ) JMSConnectionFactory.getInstance()).waitForConcurrentTasksToFinish();
        assertEquals("The HIGHPRIORITY server should have received exactly 1 message", 1, highPriorityListener.messages.size());
        assertEquals("The LOWPRIORITY server should have received exactly 0 messages", 0, lowPriorityListener.messages.size());

        //reset messages
        highPriorityListener.messages = new ArrayList<DoOneCrawlMessage>();
        lowPriorityListener.messages = new ArrayList<DoOneCrawlMessage>();

        //send a low priority job
        harvestScheduler.doOneCrawl(TestInfo.getJobLowPriority(), metadata);
        ((JMSConnectionMockupMQ) JMSConnectionFactory.getInstance()).waitForConcurrentTasksToFinish();
        assertEquals("The HIGHPRIORITY server should have received exactly 0 message", 0, highPriorityListener.messages.size());
        assertEquals("The LOWPRIORITY server should have received exactly 1 messages", 1, lowPriorityListener.messages.size());
    }

    /**
     * Verify handling of NULL value for Job
     * Uses MessageTestHandler()
     */
    public void testNullJob() {
        try {
            harvestScheduler.doOneCrawl(null, metadata);
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
        harvestScheduler.doOneCrawl(TestInfo.getJob(), metadata);
        ((JMSConnectionMockupMQ) JMSConnectionFactory.getInstance()).waitForConcurrentTasksToFinish();

        StringAsserts.assertStringContains(
                "Logfile does has NOT logged the sending of a DoOneCrawlMessage",
                "Send crawl request",
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
    
    /**
     * MessageListener used locally to intercept messages sent
     * by the HarvestScheduler.
     */
    private class TestMessageListener implements MessageListener {
        private List<NetarkivetMessage> received = 
            new ArrayList<NetarkivetMessage>();

        public void onMessage(Message msg) {
            synchronized (this) {
                NetarkivetMessage content = JMSConnection.unpack(msg);
                received.add(content);
                this.notifyAll();
            }
        }

        public NetarkivetMessage getReceived() {
            return received.get(received.size() - 1);
        }

        public int getNumReceived() {
            return received.size();
        }

        public List<NetarkivetMessage> getAllReceived() {
            return received;
        }
    }

    /**
     * Clears all new Jobs in the database by submitting these. The resulting  
     * job message is read from the queue to so the queue is empty.
     * @throws Exception
     */
    private void clearNewJobs() throws Exception {
        submitNewJobs();
        while (countQueueMessages() > 0 ) {
            messageReceiver.receiveNoWait();
        }

    }

    /**
     * Calls the <code>submitNewJobs</code> method on the current
     * harvestScheduler test instance
     * 
     * @throws Exception
     */
    private void submitNewJobs() throws Exception {
        HarvestJobGeneratorTest.waitForJobGeneration();
        ReflectUtils.getPrivateMethod(HarvestScheduler.class, "submitNewJobs")
                .invoke(harvestScheduler);
        ((JMSConnectionMockupMQ) JMSConnectionMockupMQ.getInstance())
        .waitForConcurrentTasksToFinish();
    }

    /**
     * Calls the <code>rescheduleSubmittedJobs</code> method on the current
     * harvestScheduler test instance
     * 
     * @throws Exception
     */
    private void rescheduleSubmittedJobs() throws Exception {
        ReflectUtils.getPrivateMethod(HarvestScheduler.class,
                "rescheduleSubmittedJobs").invoke(harvestScheduler);
    }

    /**
     * Calls the <code>getHoursPassedSince</code> method on the current
     * harvestScheduler test instance
     * 
     * @param date
     *            The date to use in the method call
     * @throws Exception
     */
    private int getHoursPassedSince(Date date) throws Exception {
        Method getHoursPassedSinceMethod = harvestScheduler.getClass()
                .getDeclaredMethod("getHoursPassedSince", Date.class);
        getHoursPassedSinceMethod.setAccessible(true);
        return ((Integer) getHoursPassedSinceMethod.invoke(harvestScheduler,
                date)).intValue();
    }

    private void startHarvestScheduler() throws InterruptedException {
        harvestScheduler.start();
        Thread.sleep(3000); //ToDo Let's try to find a more event driven wait
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
     * Creates a <code>QueueReceiver</code> which removes messages from the 
     * queue when receive is called (using the test listener will not remove 
     * messages from the queue)
     * @return
     * @throws JMSException
     */
    private QueueReceiver createMessageReceiver() throws JMSException {
        QueueSession qSession = 
            JMSConnectionMockupMQ.getInstance().getQueueSession();
        ChannelID channelId =  
            JobChannelUtil.getChannel(JobPriority.HIGHPRIORITY);
        Queue queue = qSession.createQueue(channelId.getName());
        return qSession.createReceiver(queue);
    }
    
    private int countQueueMessages() throws JMSException {
        ChannelID channelId =  
            JobChannelUtil.getChannel(JobPriority.HIGHPRIORITY);
        QueueBrowser qBrowser = 
            JMSConnectionMockupMQ.getInstance().createQueueBrowser(channelId);
        Enumeration messageEnumeration = qBrowser.getEnumeration();
        int numberOfMessages = 0;
        while (messageEnumeration.hasMoreElements()) {
            messageEnumeration.nextElement();
            numberOfMessages++;
        }
        return numberOfMessages;
    }
}
