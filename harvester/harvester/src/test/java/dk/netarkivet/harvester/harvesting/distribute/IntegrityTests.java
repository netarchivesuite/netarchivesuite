/* File:    $Id$
 * Version: $Revision$
 * Date:    $Date$
 * Author:  $Author$
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

package dk.netarkivet.harvester.harvesting.distribute;

import javax.jms.Message;
import javax.jms.MessageListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.Permission;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.LogManager;

import junit.framework.TestCase;

import dk.netarkivet.common.Settings;
import dk.netarkivet.common.distribute.ChannelID;
import dk.netarkivet.common.distribute.Channels;
import dk.netarkivet.common.distribute.ChannelsTester;
import dk.netarkivet.common.distribute.JMSConnection;
import dk.netarkivet.common.distribute.JMSConnectionFactory;
import dk.netarkivet.common.distribute.JMSConnectionTestMQ;
import dk.netarkivet.common.distribute.NetarkivetMessage;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.UnknownID;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.RememberNotifications;
import dk.netarkivet.harvester.datamodel.Job;
import dk.netarkivet.harvester.datamodel.JobDAO;
import dk.netarkivet.harvester.datamodel.JobPriority;
import dk.netarkivet.harvester.datamodel.JobStatus;
import dk.netarkivet.testutils.DBUtils;
import dk.netarkivet.testutils.FileAsserts;
import dk.netarkivet.testutils.LogUtils;
import dk.netarkivet.testutils.TestFileUtils;
import dk.netarkivet.testutils.TestUtils;
import dk.netarkivet.testutils.preconfigured.MockupIndexServer;

/**
 * lc forgot to comment this!
 *
 */

public class IntegrityTests extends TestCase{
    /** The message to write to log when starting the server */
    private static final String START_MESSAGE = "Starting HarvestControllerServer.";

    TestInfo info = new TestInfo();

    /* The client and server used for testing */
    HarvestControllerClient hcc;
    HarvestControllerServer hs;
    private JMSConnection con;
    private boolean done = false;
    MockupIndexServer mis = new MockupIndexServer(new File(TestInfo.ORIGINALS_DIR, "2-3-cache.zip"));

    SecurityManager sm;

    public IntegrityTests(String sTestName) {
        super(sTestName);
    }

    public void setUp() throws IOException, SQLException, IllegalAccessException {
        FileUtils.removeRecursively(TestInfo.SERVER_DIR);
        TestInfo.WORKING_DIR.mkdirs();
        try {
            TestFileUtils.copyDirectoryNonCVS(TestInfo.ORIGINALS_DIR, TestInfo.WORKING_DIR);
        } catch (IOFailure e) {
            fail("Could not copy working-files to: " + TestInfo.WORKING_DIR.getAbsolutePath());
        }

        try {
            LogManager.getLogManager().readConfiguration(new FileInputStream(TestInfo.TESTLOGPROP));
        } catch (IOException e) {
            fail("Could not load the testlog.prop file");
        }
        JMSConnectionTestMQ.useJMSConnectionTestMQ();
        con = JMSConnectionFactory.getInstance();
        ChannelsTester.resetChannels();
        TestUtils.resetDAOs();
        Settings.set(Settings.HARVEST_CONTROLLER_SERVERDIR,
                 TestInfo.WORKING_DIR.getPath() + "/harvestControllerServerDir");

        /** Do not send notification by email. Print them to STDOUT. */
        Settings.set(Settings.NOTIFICATIONS_CLASS,
                RememberNotifications.class.getName());

        hs = HarvestControllerServer.getInstance();
        hcc = HarvestControllerClient.getInstance();

        // Ensure that System.exit() is caught.
        sm = System.getSecurityManager();
        System.setSecurityManager(new SecurityManager() {
            public void checkPermission(Permission perm) {
                if (perm.getName().equals("exitVM")) {
                    throw new SecurityException("Thou shalt not exit in a "
                                                + "unit test");
                }
            }
        });
        // Copy database to working dir: TestInfo.WORKING_DIR
        File databaseJarFile = new File(TestInfo.DATA_DIR, "fullhddb.jar");
        DBUtils.getHDDB(databaseJarFile, TestInfo.WORKING_DIR);
        TestUtils.resetDAOs();
        mis.setUp();
     }

    /**
     * After test is done close test-objects.
     * @throws SQLException
     */
    public void tearDown() throws SQLException {
        mis.tearDown();
        if (hcc != null) {
            hcc.close();
        }
        if (hs != null) {
            hs.close();
        }
        DBUtils.dropHDDB();
        FileUtils.removeRecursively(TestInfo.SERVER_DIR);
        ChannelsTester.resetChannels();
        TestUtils.resetDAOs();
        Settings.reload();
        System.setSecurityManager(sm);
   }

    //This test tests that the HACO does not block (Bug 221).
    //It runs in the following steps:
    //1) A Haco is started, and it is checked it listens
    //2) A listener as added to the ArcRepos queue
    //3) A crawl job is started on the HACO
    //4) Sleeps until ArcRepos gets a store message, indicating doOneCrawl runs
    //5) Before replying, checks that noone listens to the haco queue
    //6) A listener listens to TheSched
    //7) The reply to the store is sent
    //8) Waits for message on the sched, indicating doOneCrawl ended
    //9) Checks that we listen for jobs again
    public void testListenersAddedAndRemoved() throws IOException {
        if (!TestUtils.runningAs("SVC")) {
            return;
        }
        fail("This unittest does not complete at the moment, so therefore we stop it now.");
        done = false;
        String priority2 = Settings.get(Settings.HARVEST_CONTROLLER_PRIORITY);
        ChannelID result2;
        if (priority2.equals(JobPriority.LOWPRIORITY.toString())) {
            result2 = Channels.getAnyLowpriorityHaco();
        } else
        {
            if (priority2.equals(JobPriority.HIGHPRIORITY.toString())) {
                result2 = Channels.getAnyHighpriorityHaco();
            } else
            throw new UnknownID(priority2 + " is not a valid priority");
        }
        List listeners =
                ((JMSConnectionTestMQ) con).getListeners(result2);
        assertEquals("The HACO should listen before job",
                     1, listeners.size());

        //Listener that waits for a message, notifies us, and then waits for
        //notification before continuing.
        //Used as arcrepository
        MessageListener listenerDummy = new MessageListener() {
            public void onMessage(Message message) {
                NetarkivetMessage nMsg = JMSConnection.unpack(message);
                //wake people up when we get message
                synchronized(this) {
                    done = true;
                    notifyAll();
                }
                //then wait
                try {
                    while(done) {
                        synchronized(this) {
                            wait();
                        }
                    }
                } catch (InterruptedException e) {
                    fail("Interrupted!!");
                }
                //reply when waken
                con.reply(nMsg);
            }
        };
        con.setListener(Channels.getTheArcrepos(), listenerDummy);

        //Send job
        Job j = TestInfo.getJob();
        JobDAO.getInstance().create(j);
        j.setStatus(JobStatus.SUBMITTED);
        //TODO: Fix the next line
        hcc.doOneCrawl(j, new ArrayList<MetadataEntry>());
        //wait until we know file is uploaded, listener will tell us so
        synchronized(listenerDummy) {
            while (!done) {
                try {
                    listenerDummy.wait();
                } catch (InterruptedException e) {
                    fail("interrupted");
                }
            }
        }
        //done listening for store reply
        con.removeListener(Channels.getTheArcrepos(), listenerDummy);
        //now listen for crawl ended
        con.setListener(Channels.getTheSched(), listenerDummy);

        //Check listener is not there
        String priority1 = Settings.get(Settings.HARVEST_CONTROLLER_PRIORITY);
        ChannelID result1;
        if (priority1.equals(JobPriority.LOWPRIORITY.toString())) {
            result1 = Channels.getAnyLowpriorityHaco();
        } else
        {
            if (priority1.equals(JobPriority.HIGHPRIORITY.toString())) {
                result1 = Channels.getAnyHighpriorityHaco();
            } else
            throw new UnknownID(priority1 + " is not a valid priority");
        }
        listeners =
                ((JMSConnectionTestMQ) con).getListeners(result1);
        assertEquals("Noone should listen to the HACO queue",
                     0, listeners.size());

        //wake listener to send the reply
        done = false;
        synchronized(listenerDummy) {
            listenerDummy.notifyAll();
        }

        //wait till we know job is done
        synchronized(listenerDummy) {
            while (!done) {
                try {
                    listenerDummy.wait();
                } catch (InterruptedException e) {
                    fail("interrupted");
                }
            }
        }

        //Check HACO listener is back
        String priority = Settings.get(Settings.HARVEST_CONTROLLER_PRIORITY);
        ChannelID result;
        if (priority.equals(JobPriority.LOWPRIORITY.toString())) {
            result = Channels.getAnyLowpriorityHaco();
        } else
        {
            if (priority.equals(JobPriority.HIGHPRIORITY.toString())) {
                result = Channels.getAnyHighpriorityHaco();
            } else
            throw new UnknownID(priority + " is not a valid priority");
        }
        listeners =
                ((JMSConnectionTestMQ) con).getListeners(result);
        assertEquals("The HACO should listen again",
                     1, listeners.size());

        //wake listener to let it die
        done = false;
        synchronized(listenerDummy) {
            listenerDummy.notifyAll();
        }
        con.removeListener(Channels.getTheSched(), listenerDummy);
    }

    /**
     * Checks that we can submit a crawl job, receive the expected
     * CrawlStatusMessages from a HarvestControllerServer, and that the
     * resulting crawl log is available and contains the expected data.
     * @throws IOException
     * @throws InterruptedException
     */
    public void testCrawlJob() throws IOException,
                                      InterruptedException {
        if (!TestUtils.runningAs("SVC")) {
            return;
        }
        // make a dummy job
        Job j = TestInfo.getJob();
        assertTrue("The order.xml for the job has no content!",
                j.getOrderXMLdoc().hasContent());

        JobDAO.getInstance().create(j);
        j.setStatus(JobStatus.SUBMITTED);

        //A dummy arcrepositry that just replies
        MessageListener arcrepDummy = new MessageListener() {
            public void onMessage(Message message) {
                NetarkivetMessage nMsg = JMSConnection.unpack(message);
                con.reply(nMsg);
            }
        };
        con.setListener(Channels.getTheArcrepos(), arcrepDummy);


        // Use a test listener to make sure that all the expected messages
        // are received in sequence
        CrawlStatusMessageListener listener = new CrawlStatusMessageListener();
        con.setListener(Channels.getTheSched(), listener);
        //Submit the job
        //TODO: ensure, that we have some alias-metadata to produce here
        List<MetadataEntry> metadata = new ArrayList<MetadataEntry>();
        hcc.doOneCrawl(j, metadata);
        //Note: Since this returns, we need to wait for replymessage
        synchronized(listener) {
            while (listener.messages.size() < 2) {
                listener.wait();
            }
        }
        //
        // Check requirement that we log crawl start
        //
        LogUtils.flushLogs(HarvestControllerServer.class.getName());
        FileAsserts.assertFileContains("HarvestControllerServer should log starting with "
                                       + START_MESSAGE,
                                       START_MESSAGE, TestInfo.LOG_FILE
        );
        //
        // should have received two messages - one with status started and one
        // one with status done
        //
        assertEquals("Should have received two messages", 2,
                     listener.status_codes.size());
        assertEquals("First message should be STATUS_STARTED", JobStatus.STARTED,
                     listener.status_codes.get(0)) ;
        assertEquals("Second message should be STATUS_DONE" + listener.messages.get(1).getHarvestErrorDetails(), JobStatus.DONE,
                     listener.status_codes.get(1)) ;
        //
        // Check that JobIDs are corrects
        //
        assertEquals("JobIDs do not match for first message:",
                     j.getJobID().longValue(),
                     (listener.jobids.get(0)).longValue()) ;
        assertEquals("JobIDs do not match for second message:",
                     j.getJobID().longValue(),
                     (listener.jobids.get(1)).longValue()) ;
        //
        // Get the crawl log
        //
        CrawlStatusMessage csm = listener.messages.get(1);
        DomainHarvestReport dhr = csm.getDomainHarvestReport();
        assertTrue("Should not be empty", dhr.getDomainNames().size() > 0);
        assertTrue("Did not find expected domain crawled",
                   dhr.getByteCount("www.netarkivet.dk") > 0);
    }


}
