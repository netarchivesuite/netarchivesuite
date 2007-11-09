/* File: $Id$
 * Revision: $Revision$
 * Author: $Author$
 * Date: $Date$
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
package dk.netarkivet.harvester.scheduler;

import javax.jms.Message;
import javax.jms.MessageListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.logging.LogManager;

import junit.framework.TestCase;

import dk.netarkivet.common.Settings;
import dk.netarkivet.common.distribute.ChannelID;
import dk.netarkivet.common.distribute.Channels;
import dk.netarkivet.common.distribute.JMSConnection;
import dk.netarkivet.common.distribute.JMSConnectionFactory;
import dk.netarkivet.common.distribute.JMSConnectionTestMQ;
import dk.netarkivet.common.distribute.NetarkivetMessage;
import dk.netarkivet.common.exceptions.UnknownID;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.IteratorUtils;
import dk.netarkivet.harvester.datamodel.DataModelTestCase;
import dk.netarkivet.harvester.datamodel.Domain;
import dk.netarkivet.harvester.datamodel.DomainConfiguration;
import dk.netarkivet.harvester.datamodel.DomainDAO;
import dk.netarkivet.harvester.datamodel.HarvestDefinitionDAOTester;
import dk.netarkivet.harvester.datamodel.Job;
import dk.netarkivet.harvester.datamodel.JobDAO;
import dk.netarkivet.harvester.datamodel.JobPriority;
import dk.netarkivet.harvester.datamodel.JobStatus;
import dk.netarkivet.harvester.datamodel.JobStatusInfo;
import dk.netarkivet.harvester.harvesting.distribute.DoOneCrawlMessage;
import dk.netarkivet.harvester.harvesting.distribute.MetadataEntry;
import dk.netarkivet.harvester.webinterface.DomainDefinition;
import dk.netarkivet.testutils.ClassAsserts;
import dk.netarkivet.testutils.DBUtils;
import dk.netarkivet.testutils.ReflectUtils;
import dk.netarkivet.testutils.TestFileUtils;
import dk.netarkivet.testutils.TestUtils;

/**
 * Test HarvestControllerServer
 */
public class HarvestSchedulerTester extends TestCase {

    TestInfo info = new TestInfo();

    /* The harvestScheduler used for testing */
    HarvestScheduler hsch;

    public HarvestSchedulerTester(String sTestName) {
        super(sTestName);
    }

    public void setUp() throws SQLException, IllegalAccessException,
            IOException, NoSuchFieldException, ClassNotFoundException {
        JMSConnectionTestMQ.useJMSConnectionTestMQ();
        FileUtils.removeRecursively(TestInfo.WORKING_DIR);
        TestInfo.WORKING_DIR.mkdirs();
        TestFileUtils.copyDirectoryNonCVS(TestInfo.ORIGINALS_DIR,
                TestInfo.WORKING_DIR);
        FileInputStream fis = new FileInputStream(TestInfo.TESTLOGPROP);
        LogManager.getLogManager().readConfiguration(fis);
        fis.close();
        Settings.set(Settings.DB_URL, "jdbc:derby:"
                + TestInfo.WORKING_DIR.getCanonicalPath() + "/fullhddb");
        DBUtils.getHDDB(new File(TestInfo.BASEDIR, "fullhddb.jar"),
                TestInfo.WORKING_DIR);
        TestUtils.resetDAOs();
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
        DBUtils.dropHDDB();
        FileUtils.removeRecursively(TestInfo.WORKING_DIR);
        Settings.reload();
        TestUtils.resetDAOs();
    }

    /**
     * Testing close() For the moment, we only test that this does not throw an
     * exception.
     * @throws InterruptedException
     * @throws IllegalAccessException
     * @throws NoSuchFieldException
     * @throws NoSuchMethodException
     * @throws InvocationTargetException
     */
    public void testClose()
            throws InterruptedException, IllegalAccessException,
                   NoSuchFieldException, NoSuchMethodException,
                   InvocationTargetException {
        hsch = getSchedulerInstance();
        hsch.close();
        hsch = null;
        /* TODO: Test that JMS connection has been closed properly. */
    }

    /**
     * Test that running the scheduler creates certain jobs.
     * @throws InterruptedException
     * @throws IllegalAccessException
     * @throws NoSuchFieldException
     * @throws NoSuchMethodException
     * @throws InvocationTargetException
     */
    public void testRun()
            throws InterruptedException, IllegalAccessException,
                   NoSuchFieldException, NoSuchMethodException,
                   InvocationTargetException {
        JobDAO dao = JobDAO.getInstance();
        assertEquals("Should have no jobs before start", 0, dao.getCountJobs());
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
        JMSConnectionTestMQ.getInstance().setListener(result, hacoListener);
        hsch = getSchedulerInstance();
        assertEquals("Should have created one job, but got " + dao.getAll(),
                     1, dao.getCountJobs());
        ((JMSConnectionTestMQ) JMSConnectionFactory.getInstance())
                .waitForConcurrentTasksToFinish();
        List jobs = IteratorUtils.toList(dao.getAll(JobStatus.NEW));
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
     * @throws InterruptedException
     * @throws IllegalAccessException
     * @throws NoSuchFieldException
     * @throws NoSuchMethodException
     * @throws InvocationTargetException
     */
    public void testCreateInstance()
            throws InterruptedException, IllegalAccessException,
                   NoSuchFieldException, NoSuchMethodException, InvocationTargetException {
        JobDAO dao = JobDAO.getInstance();
        assertEquals("Should have no jobs before start", 0, dao.getCountJobs());
        hsch = getSchedulerInstance();
        hsch.close();
        assertTrue("Should have created a job", dao.getCountJobs() > 0);
    }

    private HarvestScheduler getSchedulerInstance()
            throws InterruptedException, IllegalAccessException,
                   NoSuchFieldException, NoSuchMethodException, InvocationTargetException {
        final HarvestScheduler instance = HarvestScheduler.getInstance();
        HarvestDefinitionDAOTester.waitForJobGeneration();
        Method m = HarvestScheduler.class.getDeclaredMethod("submitNewJobs", new Class[0]);
        m.setAccessible(true);
        m.invoke(instance, new Object[0]);
        return instance;
    }

    public void testGetHoursPassedSince() throws NoSuchMethodException,
                                                 IllegalAccessException,
                                                 NoSuchFieldException,
                                                 InvocationTargetException,
                                                 InterruptedException {
        hsch = getSchedulerInstance();
        Method getHoursPassedSince = hsch.getClass().getDeclaredMethod("getHoursPassedSince", Date.class);
        getHoursPassedSince.setAccessible(true);
        Calendar c = new GregorianCalendar();
        c.add(Calendar.MINUTE, 45);
        Object result = getHoursPassedSince.invoke(hsch, c.getTime());
        assertEquals("Should return -1 on date after now", new Integer(-1), result);
        c.add(Calendar.HOUR,  -1);
        result = getHoursPassedSince.invoke(hsch, c.getTime());
        assertEquals("Should return 0 on date close to now", new Integer(0), result);
        c.add(Calendar.HOUR,  -12);
        result = getHoursPassedSince.invoke(hsch, c.getTime());
        assertEquals("Should return 12 on date 12 hours before", new Integer(12), result);
    }

    /**
     * Test that HarvestScheduler is a singleton.
     * @throws InterruptedException
     */
    public void testSingletonicity() throws InterruptedException,
                                            IllegalAccessException, NoSuchFieldException,
                                            NoSuchMethodException, InvocationTargetException {
        ClassAsserts.assertSingleton(HarvestScheduler.class);
        // There are threads left over that can disturb the database
        // test setup.  Must wait for them to end.
        Thread.sleep(1000);

        HarvestDefinitionDAOTester.waitForJobGeneration();
        Method m = HarvestScheduler.class.getDeclaredMethod("submitNewJobs", new Class[0]);
        m.setAccessible(true);
        m.invoke((hsch = getSchedulerInstance()), new Object[0]);
    }

    /** Test that runNewJobs skips bad jobs without crashing (bug #627)
     */
    public void testSubmitNewJobs() throws Exception {
        Method m = ReflectUtils.getPrivateMethod(HarvestScheduler.class,
                                                 "submitNewJobs");
        // Create a bad job.
        hsch = getSchedulerInstance();
        final DomainDAO dao = DomainDAO.getInstance();
        DomainConfiguration cfg = dao.getAllDomains().next().getDefaultConfiguration();
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
        assertEquals("Bad job should still be new (can't update without reading)",
                     bad.getJobID(), jdao.getAllJobIds(JobStatus.NEW).next());
        assertEquals("Good job should have been scheduled",
                     JobStatus.SUBMITTED, newGood.getStatus());
        // TODO: Should also check that a readable but unschedulable job fails,
        // that would require a connection that throws exceptions sometimes.
    }

    /** Test that runNewJobs skips bad jobs without crashing (bug #627)
     */
    public void testSubmitNewJobsMakesAliasInfo() throws Exception {
        Method m = ReflectUtils.getPrivateMethod(HarvestScheduler.class,
                                                 "submitNewJobs");
        hsch = getSchedulerInstance();
        //Get rid of the existing new job
        m.invoke(hsch);

        //Add a listender to see what is sent
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
        JMSConnectionTestMQ.getInstance().setListener(result, hacoListener);

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
        Job job = Job.createJob(
                5678L, dc1, 0);
        job.addConfiguration(dc2);
        JobDAO.getInstance().create(job);

        //Run method
        m.invoke(hsch);
        ((JMSConnectionTestMQ) JMSConnectionTestMQ.getInstance())
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

    /** Test that runNewJobs skips bad jobs without crashing (bug #627)
     */
    public void testSubmitNewJobsMakesDuplicateReductionInfo() throws Exception {
        Method m = ReflectUtils.getPrivateMethod(HarvestScheduler.class,
                                                 "submitNewJobs");
        hsch = getSchedulerInstance();
        //Get rid of the existing new job
        m.invoke(hsch);

        //Make some jobs to submit
        DataModelTestCase.createTestJobs();

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
        JMSConnectionTestMQ.getInstance().setListener(result1, hacoListener);
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
        JMSConnectionTestMQ.getInstance().setListener(result, hacoListener);

        //Run method
        m.invoke(hsch);
        ((JMSConnectionTestMQ) JMSConnectionTestMQ.getInstance())
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
                     + "&harvestid=47&harvestnum=0&jobid=15",
                     metadataEntry.getURL());
        assertEquals("Should have right data",
                     "8,9,10,11,12,13",
                     new String(metadataEntry.getData()));
    }

    public void testRescheduleJobs() throws NoSuchMethodException,
                                            IllegalAccessException,
                                            NoSuchFieldException,
                                            InvocationTargetException,
                                            InterruptedException {
        hsch = HarvestScheduler.getInstance();
        HarvestDefinitionDAOTester.waitForJobGeneration();

        final DomainDAO dao = DomainDAO.getInstance();
        DomainConfiguration cfg = dao.getAllDomains().next().getDefaultConfiguration();
        final JobDAO jdao = JobDAO.getInstance();
        for (JobStatus status : JobStatus.values()) {
            Job newJob = Job.createJob(42L, cfg, 1);
            newJob.setStatus(status);
            jdao.create(newJob);
        }
        List<JobStatusInfo> oldInfos = jdao.getStatusInfo();
        // Since initial DB contains one NEW job, we now have one of each
        // status plus one extra NEW.

        Method rescheduleJobs = ReflectUtils.getPrivateMethod(HarvestScheduler.class,
                                                              "rescheduleJobs");
        rescheduleJobs.invoke(hsch);
        List<JobStatusInfo> newInfos = jdao.getStatusInfo();
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

        public List getAllReceived() {
            return received;
        }
    } // end class TestMessageListener
}
