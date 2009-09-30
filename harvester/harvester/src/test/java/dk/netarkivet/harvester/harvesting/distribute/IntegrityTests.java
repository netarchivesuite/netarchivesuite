/* File:    $Id$
 * Version: $Revision$
 * Date:    $Date$
 * Author:  $Author$
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

import javax.jms.Message;
import javax.jms.MessageListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.security.Permission;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.LogManager;

import dk.netarkivet.archive.indexserver.distribute.IndexRequestClient;
import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.distribute.ChannelID;
import dk.netarkivet.common.distribute.Channels;
import dk.netarkivet.common.distribute.ChannelsTester;
import dk.netarkivet.common.distribute.JMSConnection;
import dk.netarkivet.common.distribute.JMSConnectionFactory;
import dk.netarkivet.common.distribute.JMSConnectionMockupMQ;
import dk.netarkivet.common.distribute.NetarkivetMessage;
import dk.netarkivet.common.distribute.indexserver.RequestType;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.RememberNotifications;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.harvester.HarvesterSettings;
import dk.netarkivet.harvester.datamodel.DataModelTestCase;
import dk.netarkivet.harvester.datamodel.Job;
import dk.netarkivet.harvester.datamodel.JobDAO;
import dk.netarkivet.harvester.datamodel.JobStatus;
import dk.netarkivet.testutils.DatabaseTestUtils;
import dk.netarkivet.testutils.FileAsserts;
import dk.netarkivet.testutils.LogUtils;
import dk.netarkivet.testutils.ReflectUtils;
import dk.netarkivet.testutils.TestFileUtils;
import dk.netarkivet.testutils.TestUtils;
import dk.netarkivet.testutils.preconfigured.MockupIndexServer;
import dk.netarkivet.testutils.preconfigured.ReloadSettings;

/**
 * Integrity tests for the dk.harvester.harvesting.distribute 
 * package. Both tests assume an FTP server is running.
 */
public class IntegrityTests extends DataModelTestCase {
    /** The message to write to log when starting the server. */
    private static final String START_MESSAGE =
        "Starting HarvestControllerServer.";

    TestInfo info = new TestInfo();

    /* The client and server used for testing */
    HarvestControllerClient hcc;
    HarvestControllerServer hs;
    private JMSConnection con;
    private boolean done = false;
    MockupIndexServer mis = new MockupIndexServer(
            new File(TestInfo.ORIGINALS_DIR, "2-3-cache.zip"));
    ReloadSettings rs = new ReloadSettings();

    SecurityManager sm;

    public IntegrityTests(String sTestName) {
        super(sTestName);
    }

    public void setUp() throws Exception, SQLException, IllegalAccessException {
        FileUtils.removeRecursively(TestInfo.WORKING_DIR);
        rs.setUp();
        ChannelsTester.resetChannels();
        super.setUp();
        JMSConnectionMockupMQ.useJMSConnectionMockupMQ();
        con = JMSConnectionFactory.getInstance();
        FileUtils.removeRecursively(TestInfo.SERVER_DIR);
        TestInfo.WORKING_DIR.mkdirs();
        try {
            TestFileUtils.copyDirectoryNonCVS(TestInfo.ORIGINALS_DIR, 
                    TestInfo.WORKING_DIR);
        } catch (IOFailure e) {
            fail("Could not copy working-files to: " 
                    + TestInfo.WORKING_DIR.getAbsolutePath());
        }

        try {
            LogManager.getLogManager().readConfiguration(
                    new FileInputStream(TestInfo.TESTLOGPROP));
        } catch (IOException e) {
            fail("Could not load the testlog.prop file");
        }
//        TestUtils.resetDAOs();
        Settings.set(HarvesterSettings.HARVEST_CONTROLLER_SERVERDIR,
                     TestInfo.WORKING_DIR.getPath()
                         + "/harvestControllerServerDir");

        /* Do not send notification by email. Print them to STDOUT. */
        Settings.set(CommonSettings.NOTIFICATIONS_CLASS,
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
//        File databaseJarFile = new File(TestInfo.DATA_DIR, "fullhddb.jar");
//        DatabaseTestUtils.getHDDB(databaseJarFile, "fullhddb",
//                TestInfo.WORKING_DIR);
//        TestUtils.resetDAOs();
        mis.setUp();
        FileUtils.createDir(IndexRequestClient.getInstance(
                RequestType.DEDUP_CRAWL_LOG).getCacheDir());
     }

    /**
     * After test is done close test-objects.
     * @throws Exception
     */
    public void tearDown() throws Exception {
        super.tearDown();
        //Reset index request client listener
        Field field = ReflectUtils.getPrivateField(IndexRequestClient.class,
                                                   "synchronizer");
        field.set(null, null);
        mis.tearDown();
        if (hcc != null) {
            hcc.close();
        }
        if (hs != null) {
            hs.close();
        }
        DatabaseTestUtils.dropHDDB();
        FileUtils.removeRecursively(TestInfo.SERVER_DIR);
        ChannelsTester.resetChannels();
        TestUtils.resetDAOs();
        System.setSecurityManager(sm);
        rs.tearDown();
   }

    //This test tests that the HACO does not block (Bug 221).
    //It runs in the following steps:
    //1) A Haco is started, and it is checked it listens
    //2) A listener as added to the ArcRepos queue
    //3) A crawl job is started on the HACO
    //4) Sleeps until ArcRepos gets a store message, indicating doOneCrawl runs
    //5) Before replying, checks that no one listens to the haco queue
    //6) A listener listens to TheSched
    //7) The reply to the store is sent
    //8) Waits for message on the sched, indicating doOneCrawl ended
    //9) Checks that we listen for jobs again
    public void testListenersAddedAndRemoved() throws IOException {
        ChannelID hacoQueue = Channels.getAnyHighpriorityHaco();

        //Listener that waits for a message, notifies us, and then waits for
        //notification before continuing.
        //Used as arcrepository and scheduler
        MessageListener listenerDummy = new MessageListener() {
            public void onMessage(Message message) {
                NetarkivetMessage nMsg = JMSConnection.unpack(message);
                //wake people up when we get message
                synchronized(this) {
                    done = true;
                    notifyAll();
                    //then wait
                    try {
                        while(done) {
                                wait();
                        }
                    } catch (InterruptedException e) {
                        fail("Interrupted!!");
                    }
                }
                //reply when waken
                con.reply(nMsg);
            }
        };

        //Be ready to receive store messages, and block.
        con.setListener(Channels.getTheRepos(), listenerDummy);
        done = false;

        //Sanity test: Make sure we listen at the start
        List<MessageListener> listeners =
                ((JMSConnectionMockupMQ) con).getListeners(hacoQueue);
        assertEquals("The HACO should listen before job",
                     1, listeners.size());

        //Prepare job
        Job j = TestInfo.getJob();
        JobDAO.getInstance().create(j);
        j.setStatus(JobStatus.SUBMITTED);

        // Okay, ready to roll!
        // We send the job, and wait for the uploads now. We expect two files
        // to be uploaded.
        synchronized(listenerDummy) {
            //Send the job
            hcc.doOneCrawl(j, new ArrayList<MetadataEntry>());

            //wait until we know files are uploaded
            while (!done) {
                try {
                    //Wait until first store message is received
                    listenerDummy.wait();
                } catch (InterruptedException e) {
                    fail("interrupted");
                }
            }
            //Figure out how many more files to wait for.
            int nooffiles = new File(new File(TestInfo.WORKING_DIR, "harvestControllerServerDir").listFiles()[0], "arcs").listFiles().length; 

            for (int i = 0; i < nooffiles; i++) {
                //Wake up listener to reply to first store message
                done = false;
                listenerDummy.notifyAll();

                while (!done) {
                    try {
                        //Wait until next store message is received
                        listenerDummy.wait();
                    } catch (InterruptedException e) {
                        fail("interrupted");
                    }
                }
            }
        }

        //done listening for store replies
        con.removeListener(Channels.getTheRepos(), listenerDummy);
        //now listen for crawl ended
        con.setListener(Channels.getTheSched(), listenerDummy);

        // At this point we know we are during the upload process, because we
        // have blocked the process while waiting for upload replies. At that
        // point we should not yet have re-added the listener.

        //Check listener is not there anymore
        listeners = ((JMSConnectionMockupMQ) con).getListeners(hacoQueue);
        assertEquals("Noone should listen to the HACO queue",
                     0, listeners.size());

        //wake listener to send the store reply. Then wait for the scheduler to
        //receive the crawl done message.
        synchronized (listenerDummy) {
            // Wake listener, so it replies to the second store message
            done = false;
            listenerDummy.notifyAll();
            // Now wait for the scheduler queue to receive a message
            while (!done) {
                try {
                    listenerDummy.wait();
                } catch (InterruptedException e) {
                    fail("interrupted");
                }
            }
        }

        //Wait a little for the listener to be readded.
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            //Nevermind
        }

        //Check HACO listener is back
        listeners = ((JMSConnectionMockupMQ) con).getListeners(hacoQueue);
        assertEquals("The HACO should listen again",
                     1, listeners.size());

        //wake listener to let it die
        done = false;
        synchronized(listenerDummy) {
            listenerDummy.notifyAll();
        }
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
        // make a dummy job
        Job j = TestInfo.getJob();
        assertTrue("The order.xml for the job must have content!",
                j.getOrderXMLdoc().hasContent());

        JobDAO.getInstance().create(j);
        j.setStatus(JobStatus.SUBMITTED);

        //A dummy arcrepository that just replies
        MessageListener arcrepDummy = new MessageListener() {
            public void onMessage(Message message) {
                NetarkivetMessage nMsg = JMSConnection.unpack(message);
                con.reply(nMsg);
            }
        };
        con.setListener(Channels.getTheRepos(), arcrepDummy);


        // Use a test listener to make sure that all the expected messages
        // are received in sequence
        CrawlStatusMessageListener listener = new CrawlStatusMessageListener();
        con.setListener(Channels.getTheSched(), listener);
        //Submit the job
        //TODO ensure, that we have some alias-metadata to produce here
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
        FileAsserts.assertFileContains(
                "HarvestControllerServer should log starting with "
                                       + START_MESSAGE,
                                       START_MESSAGE, TestInfo.LOG_FILE
        );
        //
        // should have received two messages - one with status started and one
        // one with status done
        //
        assertEquals("Should have received two messages", 2,
                listener.status_codes.size());
        assertEquals("First message should be STATUS_STARTED",
                JobStatus.STARTED, listener.status_codes.get(0));
        assertEquals("Second message should be STATUS_DONE"
                + listener.messages.get(1).getHarvestErrorDetails(),
                JobStatus.DONE, listener.status_codes.get(1));
        //
        // Check that JobIDs are correct
        //
        assertEquals("JobIDs do not match for first message:",
                     j.getJobID().longValue(),
                     (listener.jobids.get(0)).longValue());
        assertEquals("JobIDs do not match for second message:",
                     j.getJobID().longValue(),
                     (listener.jobids.get(1)).longValue());
        //
        // Get the crawl log
        //
        CrawlStatusMessage csm = listener.messages.get(1);
        DomainHarvestReport dhr = csm.getDomainHarvestReport();
        assertTrue("Should not be empty", dhr.getDomainNames().size() > 0);
        assertTrue("Did not find expected domain crawled",
                   dhr.getByteCount("netarkivet.dk") > 0);
    }
}
