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

import javax.jms.Message;
import javax.jms.MessageListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import java.util.logging.LogManager;

import junit.framework.TestCase;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.distribute.ChannelID;
import dk.netarkivet.common.distribute.Channels;
import dk.netarkivet.common.distribute.JMSConnection;
import dk.netarkivet.common.distribute.JMSConnectionFactory;
import dk.netarkivet.common.distribute.JMSConnectionMockupMQ;
import dk.netarkivet.common.distribute.NetarkivetMessage;
import dk.netarkivet.common.exceptions.UnknownID;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.IteratorUtils;
import dk.netarkivet.common.utils.RememberNotifications;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.harvester.datamodel.DataModelTestCase;
import dk.netarkivet.harvester.datamodel.Domain;
import dk.netarkivet.harvester.datamodel.DomainConfiguration;
import dk.netarkivet.harvester.datamodel.DomainDAO;
import dk.netarkivet.harvester.datamodel.HarvestDefinitionDAO;
import dk.netarkivet.harvester.datamodel.HarvestDefinitionDAOTester;
import dk.netarkivet.harvester.datamodel.Job;
import dk.netarkivet.harvester.datamodel.JobDAO;
import dk.netarkivet.harvester.datamodel.JobPriority;
import dk.netarkivet.harvester.datamodel.JobStatus;
import dk.netarkivet.harvester.datamodel.JobStatusInfo;
import dk.netarkivet.harvester.harvesting.distribute.DoOneCrawlMessage;
import dk.netarkivet.harvester.harvesting.distribute.MetadataEntry;
import dk.netarkivet.harvester.webinterface.DomainDefinition;
import dk.netarkivet.harvester.webinterface.HarvestStatusQuery;
import dk.netarkivet.testutils.ClassAsserts;
import dk.netarkivet.testutils.DatabaseTestUtils;
import dk.netarkivet.testutils.ReflectUtils;
import dk.netarkivet.testutils.TestFileUtils;
import dk.netarkivet.testutils.TestUtils;
import dk.netarkivet.testutils.preconfigured.MockupJMS;
import dk.netarkivet.testutils.preconfigured.ReloadSettings;

/**
 * Test HarvestScheduler class.
 */
public class HarvestSchedulerTester extends TestCase {

    TestInfo info = new TestInfo();

    /** The harvestScheduler used for testing. */
    HarvestScheduler hsch;

    ReloadSettings rs = new ReloadSettings();
    MockupJMS mj = new MockupJMS();
    
    
    public HarvestSchedulerTester(String sTestName) {
        super(sTestName);
    }

    public void setUp() throws SQLException, IllegalAccessException,
            IOException, NoSuchFieldException, ClassNotFoundException {
        rs.setUp();
        mj.setUp();
        FileUtils.removeRecursively(TestInfo.WORKING_DIR);
        TestInfo.WORKING_DIR.mkdirs();
        TestFileUtils.copyDirectoryNonCVS(TestInfo.ORIGINALS_DIR,
                TestInfo.WORKING_DIR);
        FileInputStream fis = new FileInputStream(TestInfo.TESTLOGPROP);
        LogManager.getLogManager().readConfiguration(fis);
        fis.close();
        Settings.set(CommonSettings.DB_URL, "jdbc:derby:"
                + TestInfo.WORKING_DIR.getCanonicalPath() + "/fullhddb");
        DatabaseTestUtils.getHDDB(new File(TestInfo.BASEDIR,"fullhddb.jar"),
                "fullhddb",
                TestInfo.WORKING_DIR);
        TestUtils.resetDAOs();
        Settings.set(CommonSettings.NOTIFICATIONS_CLASS,
                RememberNotifications.class.getName());
    }

    /**
     * After test is done close test-objects.
     * @throws SQLException
     * @throws IllegalAccessException
     * @throws NoSuchFieldException
     */
    public void tearDown() throws SQLException, IllegalAccessException,
            NoSuchFieldException {
        if (hsch != null) {
            hsch.close();
        }
        DatabaseTestUtils.dropHDDB();
        FileUtils.removeRecursively(TestInfo.WORKING_DIR);
        TestUtils.resetDAOs();
        mj.tearDown();
        rs.tearDown();
    }

    /**
     * Testing close() For the moment, we only test that this does not throw an
     * exception.
     * @throws Exception if HarvestScheduler throws exception
     */
    public void testClose() throws Exception {
        hsch = submitNewJobsAndGetSchedulerInstance();
        hsch.close();
        hsch = null;
        /* TODO: Test that JMS connection has been closed properly. */
    }

    /**
     * Test that running the scheduler creates certain jobs.
     * @throws Exception if HarvestScheduler throws exception
     */
    public void testRun() throws Exception {
        JobDAO dao = JobDAO.getInstance();
        assertEquals("Should have no jobs before start", 0, dao.getCountJobs());
        TestMessageListener hacoListener = new TestMessageListener();
        ChannelID result;
        if (JobPriority.HIGHPRIORITY.toString().equals(JobPriority.LOWPRIORITY.toString())) {
            result = Channels.getAnyLowpriorityHaco();
        } else {
            if (JobPriority.HIGHPRIORITY.toString().equals(JobPriority.HIGHPRIORITY.toString())) {
                result = Channels.getAnyHighpriorityHaco();
            } else {
                throw new UnknownID(JobPriority.HIGHPRIORITY.toString() + " is not a valid priority");
            }
        }
        JMSConnectionMockupMQ.getInstance().setListener(result, hacoListener);
        hsch = submitNewJobsAndGetSchedulerInstance();
        assertEquals("Should have created one job, but got " 
                    + dao.getCountJobs(),
                1, dao.getCountJobs());
        ((JMSConnectionMockupMQ) JMSConnectionFactory.getInstance())
        .waitForConcurrentTasksToFinish();
        List<Job> jobs = IteratorUtils.toList(dao.getAll(JobStatus.NEW));
        assertEquals("No jobs should be left with status new, but got " + jobs,
                0, jobs.size());
        assertEquals("One job should have been created and submitted",
                1, IteratorUtils.toList(dao.getAll(JobStatus.SUBMITTED)).size());
        assertNotNull("Should have received a message", hacoListener
                .getReceived());
        assertTrue("Message received should be a DoOneCrawlMessage",
                hacoListener.getReceived() instanceof DoOneCrawlMessage);
        assertEquals("Should have received exactly one message, but got "
                + hacoListener.getAllReceived(), 1, hacoListener
                .getNumReceived());
    }

    /**
     * Test that the getInstance method works.
     * @throws Exception if HarvestScheduler throws exception
     */
    public void testCreateInstance() throws Exception {
        JobDAO dao = JobDAO.getInstance();
        assertEquals("Should have no jobs before start", 0, dao.getCountJobs());
        hsch = submitNewJobsAndGetSchedulerInstance();
        hsch.close();
        assertTrue("Should have created a job", dao.getCountJobs() > 0);
    }

    /**
     * Submit new jobs and return Scheduler instance.
     * @return a Scheduler instance.
     * @throws Exception if HarvestScheduler throws exception
     */
    private HarvestScheduler submitNewJobsAndGetSchedulerInstance() throws Exception {
        final HarvestScheduler instance = HarvestScheduler.getInstance();
        HarvestDefinitionDAOTester.waitForJobGeneration();
        Method m = HarvestScheduler.class.getDeclaredMethod("submitNewJobs", new Class[0]);
        m.setAccessible(true);
        m.invoke(instance, new Object[0]);
        return instance;
    }

    /**
     * Test private method getHoursPassedSince().
     * @throws Exception if HarvestScheduler throws exception
     */
    public void testGetHoursPassedSince() throws Exception {
        hsch = submitNewJobsAndGetSchedulerInstance();
        Method getHoursPassedSince = hsch.getClass().getDeclaredMethod("getHoursPassedSince", Date.class);
        getHoursPassedSince.setAccessible(true);
        Calendar c = new GregorianCalendar();
        c.add(Calendar.MINUTE, 45);
        Object result = getHoursPassedSince.invoke(hsch, c.getTime());
        assertEquals("Should return -1 on date after now", -1, result);
        c.add(Calendar.HOUR,  -1);
        result = getHoursPassedSince.invoke(hsch, c.getTime());
        assertEquals("Should return 0 on date close to now", 0, result);
        c.add(Calendar.HOUR,  -12);
        result = getHoursPassedSince.invoke(hsch, c.getTime());
        assertEquals("Should return 12 on date 12 hours before", 12, result);
    }


    /**
     * Test that HarvestScheduler is a singleton.
     * @throws Exception if HarvestScheduler throws exception
     */
    public void testSingletonicity() throws Exception {
        ClassAsserts.assertSingleton(HarvestScheduler.class);
        // There are threads left over that can disturb the database
        // test setup.  Must wait for them to end.
        Thread.sleep(1000);

        HarvestDefinitionDAOTester.waitForJobGeneration();
        Method m = HarvestScheduler.class.getDeclaredMethod("submitNewJobs", new Class[0]);
        m.setAccessible(true);
        m.invoke((hsch = submitNewJobsAndGetSchedulerInstance()), new Object[0]);
    }

    /** Test that runNewJobs skips bad jobs without crashing (bug #627).
     * TODO The setActualStop/setActualStart no longer throws exception, so we need to find a way making jobs bad
     * @throws Exception if HarvestScheduler throws exception
     */
    public void testSubmitNewJobs() throws Exception {
        Method m = ReflectUtils.getPrivateMethod(HarvestScheduler.class,
                                                 "submitNewJobs");
        // Create a bad job.
        hsch = submitNewJobsAndGetSchedulerInstance();
        final DomainDAO dao = DomainDAO.getInstance();
        Iterator<Domain> domainsIterator = dao.getAllDomains();
        assertTrue("Should be at least one domain in domains table",
                domainsIterator.hasNext());
        DomainConfiguration cfg = domainsIterator.next().getDefaultConfiguration();
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
        Job good = Job.createJob(1L, cfg, 1);
        good.setStatus(JobStatus.NEW);
        jdao.create(good);
        m.invoke(hsch);
        Job newGood = jdao.read(good.getJobID());
        // Commented out, as inconsistent actualStop/actualStart no longer prevents the job from 
        //   being submitted.
        
        // Iterator<Long> iterator = jdao.getAllJobIds(JobStatus.NEW);
        // assertTrue("No new jobs available: Bad job should still be new", iterator.hasNext());
        // assertEquals("Bad job should still be new (can't update without reading)",
        //               bad.getJobID(), jdao.getAllJobIds(JobStatus.NEW).next());
        assertEquals("Good job should have been scheduled",
                     JobStatus.SUBMITTED, newGood.getStatus());
        
        // TODO: Should also check that a readable but unschedulable job fails,
        // that would require a connection that throws exceptions sometimes.
    }

    /** 
     * Test that runNewJobs generates correct alias information for the job.
     * @throws Exception if HarvestScheduler throws exception
     */
    public void testSubmitNewJobsMakesAliasInfo() throws Exception {
        Method m = ReflectUtils.getPrivateMethod(HarvestScheduler.class,
                                                 "submitNewJobs");
        hsch = submitNewJobsAndGetSchedulerInstance();
        //Get rid of the existing new job
        m.invoke(hsch);

        //Add a listener to see what is sent
        TestMessageListener hacoListener = new TestMessageListener();
        ChannelID result;
        if (JobPriority.HIGHPRIORITY.toString().equals(JobPriority.LOWPRIORITY.toString())) {
            result = Channels.getAnyLowpriorityHaco();
        } else
        {
            if (JobPriority.HIGHPRIORITY.toString().equals(JobPriority.HIGHPRIORITY.toString())) {
                result = Channels.getAnyHighpriorityHaco();
            } else
            {
                throw new UnknownID(JobPriority.HIGHPRIORITY.toString() + " is not a valid priority");
            }
        }
        JMSConnectionMockupMQ.getInstance().setListener(result, hacoListener);

        //Create the following domains:
        //kb.dk with aliases alias1.dk and alias2.dk
        //dr.dk with alias alias3.dk
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

        //Make a job from dr.dk and kb.dk
        DataModelTestCase.addHarvestDefinitionToDatabaseWithId(5678L);
        Job job = Job.createJob(
                5678L, dc1, 0);
        job.addConfiguration(dc2);
        JobDAO.getInstance().create(job);

        //Run method
        m.invoke(hsch);
        ((JMSConnectionMockupMQ) JMSConnectionMockupMQ.getInstance())
                .waitForConcurrentTasksToFinish();

        //Check result
        assertEquals("Haco listener should have received one message",
                     1, hacoListener.getNumReceived());
        DoOneCrawlMessage crawlMessage =
                (DoOneCrawlMessage) hacoListener.getReceived();
        assertEquals("Should have 1 metadata entry, but got "
                + crawlMessage.getMetadata(),
                     1, crawlMessage.getMetadata().size());
        MetadataEntry metadataEntry = crawlMessage.getMetadata().get(0);
        assertNotNull("Should have 1 metadata entry", metadataEntry);
        assertEquals("Should have mimetype text/plain",
                     "text/plain", metadataEntry.getMimeType());
        assertEquals("Should have right url",
                     "metadata://netarkivet.dk/crawl/setup/aliases"
                     + "?majorversion=1&minorversion=0"
                     + "&harvestid=5678&harvestnum=0&jobid=2",
                     metadataEntry.getURL());
        assertEquals("Should have right data",
                     "alias3.dk is an alias for dr.dk\n"
                     + "alias1.dk is an alias for kb.dk\n"
                     + "alias2.dk is an alias for kb.dk\n",
                     new String(metadataEntry.getData()));
    }

    /** 
     * Test that runNewJobs makes correct duplication reduction information.
     * @throws Exception if HarvestScheduler throws exception
     */
    public void testSubmitNewJobsMakesDuplicateReductionInfo() throws Exception {
        Method m = ReflectUtils.getPrivateMethod(HarvestScheduler.class,
                                                 "submitNewJobs");
        hsch = submitNewJobsAndGetSchedulerInstance();
        //Get rid of the existing new job
        m.invoke(hsch);

        //Make some jobs to submit
        //Assume 1st jobId is 2, and lastId is 15
        DataModelTestCase.createTestJobs(2L, 15L);

        //Add a listener to see what is sent
        TestMessageListener hacoListener = new TestMessageListener();
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
        JMSConnectionMockupMQ.getInstance().setListener(result1, hacoListener);
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
        JMSConnectionMockupMQ.getInstance().setListener(result, hacoListener);

        //Run method
        m.invoke(hsch);
        ((JMSConnectionMockupMQ) JMSConnectionMockupMQ.getInstance())
                .waitForConcurrentTasksToFinish();

        //Check result
        assertEquals("Haco listener should have received all messages",
                     14, hacoListener.getNumReceived());
        DoOneCrawlMessage crawlMessage =
                (DoOneCrawlMessage) hacoListener.getReceived();
        assertEquals("Should have 1 metadata entry in last received message",
                     1, crawlMessage.getMetadata().size());
        MetadataEntry metadataEntry = crawlMessage.getMetadata().get(0);
        assertNotNull("Should have 1 metadata entry", metadataEntry);
        assertEquals("Should have mimetype text/plain",
                     "text/plain", metadataEntry.getMimeType());
        assertEquals("Should have right url",
                     "metadata://netarkivet.dk/crawl/setup/duplicatereductionjobs"
                     + "?majorversion=1&minorversion=0"
                     + "&harvestid=6&harvestnum=0&jobid=15",
                     metadataEntry.getURL());
        assertEquals("Should have right data",
                     "8,9,10,11,12,13",
                     new String(metadataEntry.getData()));
    }

    public void testStoppedOldJobs() throws Exception {
        hsch = HarvestScheduler.getInstance();
        HarvestDefinitionDAOTester.waitForJobGeneration();

        final DomainDAO dao = DomainDAO.getInstance();
        Iterator<Domain> domainsIterator = dao.getAllDomains();
        assertTrue("Should be at least one domain in domains table",
                domainsIterator.hasNext());
        DomainConfiguration cfg = domainsIterator.next().getDefaultConfiguration();
        final JobDAO jdao = JobDAO.getInstance();
        
        final Long harvestID = 1L;
        // Verify that harvestDefinition with ID=1L exists
        assertTrue("harvestDefinition with ID=" + harvestID
                + " does not exist, but should have",
                HarvestDefinitionDAO.getInstance().exists(harvestID));
        // Create 6 jobs - with start time minus 60*60*24*7 +1
        // (one week plus one second)
        for (int i=0; i<6; i++) {
            Job newJob = Job.createJob(harvestID, cfg, 1);
            newJob.setActualStart(new Date( (new Date()).getTime() - (604801*1000) ));
            newJob.setStatus(JobStatus.STARTED);
            jdao.create(newJob);
        }
        // Create 6 new jobs with now
        for (int i=0; i<6; i++) {
            Job newJob = Job.createJob(harvestID, cfg, 1);
            newJob.setActualStart(new Date());
            newJob.setStatus(JobStatus.STARTED);
            jdao.create(newJob);
        }
        List<JobStatusInfo> oldInfos = 
        	jdao.getStatusInfo(new HarvestStatusQuery()).getJobStatusInfo();
        // Since initial DB contains one NEW job, we now have one of each
        // status plus one extra NEW (i.e. 7 jobs).
        assertTrue("There should have been 13 jobs now, but there was "
                + oldInfos.size(), oldInfos.size() == 13);

        Iterator<Long> ids = jdao.getAllJobIds(JobStatus.STARTED);
        int size = 0;
        while(ids.hasNext()) {
            ids.next();
            size++;
        }
        assertTrue("There should be 12 jobs with status STARTED, there are " + size, size == 12);
        hsch.run();

        // check that we have 6 failed and 6 submitted job after we have stopped
        // old jobs
        ids = jdao.getAllJobIds(JobStatus.STARTED);
        size = 0;
        while(ids.hasNext()) {
            ids.next();
            size++;
        }
        assertTrue("There should be 6 jobs with status STARTED, there are " + size, size == 6);
        ids = jdao.getAllJobIds(JobStatus.FAILED);
        size = 0;
        while(ids.hasNext()) {
            ids.next();
            size++;
        }
        assertTrue("There should be 6 jobs with status FAILED, there are " + size, size == 6);
        
    }

    /**
     * Unit test testing the private method rescheduleJob.
     * @throws Exception if HarvestScheduler throws exception
     */
    public void testRescheduleJobs() throws Exception {
        hsch = HarvestScheduler.getInstance();
        HarvestDefinitionDAOTester.waitForJobGeneration();

        final DomainDAO dao = DomainDAO.getInstance();
        Iterator<Domain> domainsIterator = dao.getAllDomains();
        assertTrue("Should be at least one domain in domains table",
                domainsIterator.hasNext());
        DomainConfiguration cfg = domainsIterator.next().getDefaultConfiguration();
        final JobDAO jdao = JobDAO.getInstance();
        
        final Long harvestID = 1L;
        // Verify that harvestDefinition with ID=1L exists
        assertTrue("harvestDefinition with ID=" + harvestID 
                + " does not exist, but should have",
                HarvestDefinitionDAO.getInstance().exists(harvestID));
        // Create 6 jobs, one in each JobStatus:
        // (NEW, SUBMITTED, STARTED, DONE, FAILED, RESUBMITTED) 
        for (JobStatus status : JobStatus.values()) {
            Job newJob = Job.createJob(harvestID, cfg, 1); 
            newJob.setStatus(status);
            jdao.create(newJob);
        }

        List<JobStatusInfo> oldInfos = 
        	jdao.getStatusInfo(new HarvestStatusQuery()).getJobStatusInfo();
        // Since initial DB contains one NEW job, we now have one of each
        // status plus one extra NEW (i.e. 7 jobs).
        
        assertTrue("There should have been 7 jobs now, but there was "
                + oldInfos.size(), oldInfos.size() == 7);

        Method rescheduleJobs = ReflectUtils.getPrivateMethod(HarvestScheduler.class,
                                                              "rescheduleJobs");
        rescheduleJobs.invoke(hsch);
        List<JobStatusInfo> newInfos = 
        	jdao.getStatusInfo(new HarvestStatusQuery()).getJobStatusInfo();
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
     * MessageListener used locally to intercept messages sent
     * by the HarvestScheduler.
     */
    private class TestMessageListener implements MessageListener {
        private List<NetarkivetMessage> received = new ArrayList<NetarkivetMessage>();

        public TestMessageListener() {
        }

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
    } // end class TestMessageListener
}
