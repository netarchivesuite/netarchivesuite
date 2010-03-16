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
package dk.netarkivet.archive.bitarchive.distribute;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;

import junit.framework.TestCase;
import dk.netarkivet.archive.ArchiveSettings;
import dk.netarkivet.archive.arcrepository.bitpreservation.ChecksumJob;
import dk.netarkivet.archive.bitarchive.BitarchiveMonitor;
import dk.netarkivet.archive.bitarchive.BitarchiveMonitorApplication;
import dk.netarkivet.archive.checksum.distribute.CorrectMessage;
import dk.netarkivet.archive.checksum.distribute.GetAllChecksumsMessage;
import dk.netarkivet.archive.checksum.distribute.GetAllFilenamesMessage;
import dk.netarkivet.archive.checksum.distribute.GetChecksumMessage;
import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.distribute.ChannelID;
import dk.netarkivet.common.distribute.Channels;
import dk.netarkivet.common.distribute.ChannelsTester;
import dk.netarkivet.common.distribute.JMSConnection;
import dk.netarkivet.common.distribute.JMSConnectionFactory;
import dk.netarkivet.common.distribute.JMSConnectionMockupMQ;
import dk.netarkivet.common.distribute.NetarkivetMessage;
import dk.netarkivet.common.distribute.RemoteFile;
import dk.netarkivet.common.distribute.RemoteFileFactory;
import dk.netarkivet.common.distribute.TestRemoteFile;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.NotImplementedException;
import dk.netarkivet.common.exceptions.PermissionDenied;
import dk.netarkivet.common.exceptions.UnknownID;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.RememberNotifications;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.utils.batch.FileBatchJob;
import dk.netarkivet.common.utils.batch.TestJob;
import dk.netarkivet.testutils.ClassAsserts;
import dk.netarkivet.testutils.FileAsserts;
import dk.netarkivet.testutils.ReflectUtils;
import dk.netarkivet.testutils.StringAsserts;
import dk.netarkivet.testutils.TestMessageListener;
import dk.netarkivet.testutils.preconfigured.MockupJMS;
import dk.netarkivet.testutils.preconfigured.MoveTestFiles;
import dk.netarkivet.testutils.preconfigured.PreserveStdStreams;
import dk.netarkivet.testutils.preconfigured.PreventSystemExit;
import dk.netarkivet.testutils.preconfigured.ReloadSettings;
import dk.netarkivet.testutils.preconfigured.UseTestRemoteFile;


/**
 * Unit tests for the BitarchiveMonitorServer class. 
 */
public class BitarchiveMonitorServerTester extends TestCase {

    static final ChannelID THE_BAMON = Channels.getTheBamon();
    static final ChannelID THE_ARCREPOS = Channels.getTheRepos();
    static final ChannelID ALL_BA = Channels.getAllBa();
    static final ChannelID ANY_BA = Channels.getAnyBa();


    /**
     * The BA monitor client and server used for testing
     */
    BitarchiveMonitorServer bam_server;
    JMSConnectionMockupMQ con;

    private MoveTestFiles mtf = new MoveTestFiles(TestInfo.BAMON_ORIGINALS,
                                                  TestInfo.BAMON_WORKING);
    private UseTestRemoteFile ulrf = new UseTestRemoteFile();
    private MockupJMS mjms = new MockupJMS();
    private ReloadSettings rls = new ReloadSettings();

    protected void setUp() {
        rls.setUp();
        mjms.setUp();
        ulrf.setUp();
        mtf.setUp();
        File commontempdir = new File(TestInfo.BAMON_WORKING, "commontempdir");
        commontempdir.mkdir();
        assertTrue("commontempdir not created", commontempdir.exists());
        JMSConnectionMockupMQ.useJMSConnectionMockupMQ();
        con = (JMSConnectionMockupMQ) JMSConnectionFactory.getInstance();
        Settings.set(ArchiveSettings.BITARCHIVE_BATCH_JOB_TIMEOUT, 
                String.valueOf(TestInfo.BITARCHIVE_BATCH_MESSAGE_TIMEOUT));
        Settings.set(ArchiveSettings.BITARCHIVE_HEARTBEAT_FREQUENCY,
                String.valueOf(TestInfo.BITARCHIVE_HEARTBEAT_FREQUENCY));
        Settings.set(ArchiveSettings.BITARCHIVE_ACCEPTABLE_HEARTBEAT_DELAY,
                     String.valueOf(
                             TestInfo.BITARCHIVE_ACCEPTABLE_HEARTBEAT_DELAY));
        Settings.set(CommonSettings.DIR_COMMONTEMPDIR,
                commontempdir.getAbsolutePath());
    }

    protected void tearDown() {
        if (bam_server != null) {
            bam_server.close();
        }
        mtf.tearDown();
        JMSConnectionMockupMQ.clearTestQueues();
        ulrf.tearDown();
        mjms.tearDown();
        rls.tearDown();
        RememberNotifications.resetSingleton();
        ChannelsTester.resetChannels();
    }

    /**
     * Verify that batch jobs do not have to wait for each other. In particular,
     * check that the postprocessing of one job can overtake the postprocessing
     * of another.
     */
    public void testParallelBatchJobs() throws InterruptedException {
        bam_server = BitarchiveMonitorServer.getInstance();
        TestMessageListener client = new TestMessageListener();
        JMSConnectionFactory.getInstance().setListener(Channels.getThisReposClient(), client);
        final MockupBitarchiveBatch mockupBitarchiveBatch
                = new MockupBitarchiveBatch("EnesteBitarkiv");
        final BlockingRF brf = new BlockingRF();

        // Make sure the bamon knows about our mockup bitarchive by sending a
        // heartbeat
        mockupBitarchiveBatch.heartBeat(bam_server);

        // Send a batch message 'job1'
        bam_server.visit(noActionBatchMessage("job1"));
        // Simulate reply for 'job1' in a version that blocks until job2 reply
        // arrives
        new Thread() {
            public void run() {
                bam_server.visit(mockupBitarchiveBatch.replyForLatestJob(brf));
            }
        }.start();
        // give a little time for it to get stuck
        synchronized (this) {
            wait(50);
        }
        // Send a batchmessage 'job2'
        bam_server.visit(noActionBatchMessage("job2"));
        // Simulate reply for 'job2' in a version that wakes up the blocked job
        new Thread() {
            public void run() {
                bam_server.visit(mockupBitarchiveBatch.replyForLatestJob(
                        brf.getWaker()));
            }
        }.start();

        // wait for it to be done
        synchronized (this) {
            wait(400);
        }
        ((JMSConnectionMockupMQ) JMSConnectionFactory.getInstance()).waitForConcurrentTasksToFinish();

        // check results
        assertEquals("Should list no failed jobs", null, brf.failed);
        assertEquals("Should have gotten 2 messages", 2,
                     client.getNumReceived());
        assertEquals("Should have gotten 0 Not OK messages, but got " + client.getNumNotOk(), 0, 
                client.getNumNotOk());
        assertEquals("Should have gotten 2 OK messages", 2, client.getNumOk());
        assertBatchResultIs(client, 0, "job2", BlockingRF.WAKER_CONTENT);
        assertBatchResultIs(client, 1, "job1", BlockingRF.STD_CONTENT);
    }

    /**
     * @return A batch message from THE_PRES with a job that does nothing.
     */
    private BatchMessage noActionBatchMessage(String id) {
        BatchMessage result =
                new BatchMessage(
                        Channels.getTheBamon(),
                        Channels.getThisReposClient(),
                        new FileBatchJob() {
                            public void initialize(OutputStream os) {
                            }

                            public boolean processFile(File file,
                                                       OutputStream os) {
                                return true;
                            }

                            public void finish(OutputStream os) {
                            }
                        },
                        Settings.get(CommonSettings.USE_REPLICA_ID));
        JMSConnectionMockupMQ.updateMsgID(result, id);
        return result;
    }

    /**
     * Assert that the index'th message in client is a BatchReply to a message
     * of id=replyOf and with content=expectedResult.
     */
    private void assertBatchResultIs(
            TestMessageListener client,
            int index,
            String replyOf,
            String expectedResult) {
        BatchReplyMessage brm = (BatchReplyMessage) client.getAllReceived().get(
                index);
        assertEquals("Batch-reply no. " + index + " should be response to job '"
                     + replyOf + "'",
                     replyOf, brm.getReplyOfId());
        File tmpFile = mtf.working(TestInfo.BAMON_TMP_FILE);
        brm.getResultFile().copyTo(tmpFile);
        assertEquals("Should forward batch reply unmodified",
                     expectedResult,
                     new String(FileUtils.readBinaryFile(tmpFile)));
        tmpFile.delete();
    }

    /**
     * Test that BitarchiveMonitorServer is a singleton.
     */
    public void testSingletonicity() {
        ClassAsserts.assertSingleton(BitarchiveMonitorServer.class);
    }


    /**
     * Verify that the BA monitor can receive a BatchMessage (on the THE_BAMON
     * queue) and forward it to the ALL_BA topic.
     */
    public void testBatchReceive() {
        TestJob job = new TestJob(
                "testBatchReceive_ID"); // job is used for carrying an id to recognize later
        NetarkivetMessage message = new BatchMessage(THE_BAMON,
                                                     job, Settings.get(
                CommonSettings.USE_REPLICA_ID));

        bam_server = new TestBitarchiveMonitorServer();

        //Simulate that a message is sent to the THE_BAMON queue:
        con.send(message);
        con.waitForConcurrentTasksToFinish();

        List<BatchMessage> receivedBatchMsgs
                = ((TestBitarchiveMonitorServer) bam_server).getBatchMsg();

        assertTrue(
                "BA Monitor server was expected to contain the BatchMessage: "
                + message, receivedBatchMsgs.contains(message));

        boolean isSentToBA_ALL = con.isSentToChannel(job, ALL_BA);
        assertTrue("Topic ALL_BA was expected to contain the batch message: "
                   + message, isSentToBA_ALL);
    }

    /**
     * Verify that the BA monitor can timeout if no reply is received within the
     * timeout limit.
     * @throws InterruptedException 
     */
    public void testBatchTimeout() throws InterruptedException {

        bam_server = new TestBitarchiveMonitorServer();

        String ba_App_Id = "BA_App_1";
        TestJob job = new TestJob("testBatchReceive_ID");
        job.setBatchJobTimeout(TestInfo.BITARCHIVE_BATCH_MESSAGE_TIMEOUT);

        // register a listener that simulates the arc repository
        TestListener arcrepos = new TestListener();
        con.setListener(Channels.getTheRepos(), arcrepos);

        // send a heartbeat to let monitor know that ba_App_Id is alive
        HeartBeatMessage hbm = new HeartBeatMessage(Channels.getTheBamon(),
                                                    ba_App_Id);
        con.send(hbm);
        con.waitForConcurrentTasksToFinish();

        // register a listener that simulates a bitarchive
        TestListener bitarchive = new TestListener();
        con.setListener(Channels.getAllBa(), bitarchive);

        // send a batch message to the monitor
        BatchMessage bm = new BatchMessage(THE_BAMON, Channels.getTheRepos(),
                                           job, Settings.get(
                CommonSettings.USE_REPLICA_ID));
        con.send(bm);
        con.waitForConcurrentTasksToFinish();

        // sleep a bit longer than the monitor's timeout
        try {
            Thread.sleep(TestInfo.BITARCHIVE_BATCH_MESSAGE_TIMEOUT + TestInfo
                    .JUST_A_BIT_LONGER);
        }
        catch (InterruptedException e) {
            fail("Unexpected interruption whilst sleeping before sending a "
                 + "late BatchEndedMessage");
        }

        // simulate a bitarchive that replies too late: send a
        // late batch ended message
        NetarkivetMessage bem = new BatchEndedMessage(THE_BAMON, ba_App_Id,
                bitarchive.getLastMessage().getID(), null);

        con.send(bem);
        con.waitForConcurrentTasksToFinish();
        
        synchronized(this) {
            wait(50);
        }

        if (arcrepos.getLastBatchReplyMessage() == null) {
            fail("BA Monitor never sent a BatchReplyMessage.");
        } else if (arcrepos.getLastBatchReplyMessage().isOk()) {
            fail("BA Monitor reports no error for timed out batch message.");
        } else {
            StringAsserts.assertStringContains(
                    "BA Monitor reports error but doesn't indicate that it is"
                    + " a time-out error.",
                    "timeout", arcrepos.getLastBatchReplyMessage().getErrMsg());
        }
    }


    /**
     * Verify that we can register a heartbeat from a bit archive application,
     * i.e. that we can receive a heartbeat message and register its data
     * (originating BA application and timestamp).
     */
    public void testReceiveHeartBeat() {
        bam_server = new TestBitarchiveMonitorServer();

        String ba_App_Id = "BA_App_1";
        NetarkivetMessage message = new HeartBeatMessage(THE_BAMON, ba_App_Id);

        con.send(message);
        con.waitForConcurrentTasksToFinish();

        List<HeartBeatMessage> receivedHeartBeatMsgs
                = ((TestBitarchiveMonitorServer) bam_server).getHeartBeatMsg();
        assertTrue(
                "BA Monitor server was expected to contain the HeartBeatMessage: "
                + message, receivedHeartBeatMsgs.contains(message));

        Set<String> runningBAApps = getLiveApps(bam_server);
        assertTrue("BA application with id: " + ba_App_Id
                   + " not found among live BA applications.",
                   runningBAApps.contains(ba_App_Id));
    }

    /**
     * Test that listener is registered and unregistered properly.
     */
    public void testListening() {
        testListeningPerReplica("ONE", "TWO");
        testListeningPerReplica("TWO", "ONE");
    }

    /**
     * Verify that the BitarchiveMonitorServer listens to the expected Channel
     * (local THE_BAMON), and that it stops listening after cleanup.
     *
     * @param replicaId  The replica for which a BitarchiveMonitorServer is
     *                      constructed and tested.
     * @param otherReplicaId A replica which the constructed BitarchiveMonitorServer
     *                      should NOT serve.
     */
    private void testListeningPerReplica(String replicaId,
                                          String otherReplicaId) {
        Settings.set(CommonSettings.USE_REPLICA_ID, replicaId);
        JMSConnectionMockupMQ jms
                = (JMSConnectionMockupMQ) JMSConnectionFactory.getInstance();
        BitarchiveMonitorServer bamon = BitarchiveMonitorServer.getInstance();
        assertTrue("The BAMON should listen to THE_BAMON",
                   jms.getListeners(Channels.getBaMonForReplica(
                           replicaId)).contains(bamon));
        assertEquals("Only the BAMON should listen to THE_BAMON",
                     1, con.getListeners(
                Channels.getBaMonForReplica(replicaId)).size());
        assertFalse(
                "The BAMON should not listen to THE_BAMON for other replicas",
                jms.getListeners(Channels.getBaMonForReplica(
                        otherReplicaId)).contains(bamon));
        bamon.cleanup();
        assertTrue("The BAMON should stop listening to THE_BAMON after cleanup",
                   jms.getListeners(Channels.getBaMonForReplica(
                           replicaId)).isEmpty());
        ChannelsTester.resetChannels();
    }

    /**
     * Verify that we can determine which BA applications are 'live' at a given
     * time. TODO: Make sure that we wait exactly long enough for a heartbeat.
     */
    public void testDeterminationOfLiveBAapps() {
        bam_server = BitarchiveMonitorServer.getInstance();

        String baAppId1 = TestInfo.BITARCHIVE_APP_DIR_1;
        NetarkivetMessage message = new HeartBeatMessage(THE_BAMON, baAppId1);
        con.send(message);
        message = new HeartBeatMessage(THE_BAMON, baAppId1);
        con.send(message);

        String baAppId2 = TestInfo.BITARCHIVE_SERVER_DIR_1;
        message = new HeartBeatMessage(THE_BAMON, baAppId2);
        con.send(message);

        con.waitForConcurrentTasksToFinish();

        // By now each BA application should have sent out at least one heartbeat:
        Set<String> runningBAApps = getLiveApps(bam_server);

        bam_server.close();
        assertTrue(
                "The BA monitor's list of running BA applications was expected to contain the id "
                + baAppId1 + " but only has " + runningBAApps,
                runningBAApps.contains(baAppId1));
        assertTrue(
                "The BA monitor's list of running BA applications was expected to contain the id "
                + baAppId2 + " but only has " + runningBAApps,
                runningBAApps.contains(baAppId2));
    }


    /**
     * Verify that we can process a BatchEndedMessage from a BA application i.e.
     * update the internal state of the BA monitor correctly.
     * TODO Update to reflect new API for batch output
     */
    public void testProcessBatchEndedMessage() {
        bam_server = new TestBitarchiveMonitorServer();
        Settings.set(ArchiveSettings.BITARCHIVE_SERVER_FILEDIR, TestInfo.BITARCHIVE_APP_DIR_1);
        Settings.set(CommonSettings.DIR_COMMONTEMPDIR, TestInfo.BITARCHIVE_SERVER_DIR_1);
        BitarchiveServer bas = BitarchiveServer.getInstance();

        //Make sure we know about the BA server
        HeartBeatMessage hbm = new HeartBeatMessage(THE_BAMON,
                                                    bas.getBitarchiveAppId());
        con.send(hbm);
        con.waitForConcurrentTasksToFinish();
        Set<String> runningBAApps = getLiveApps(bam_server);
        assertTrue(
                "The BA monitor's list of running BA applications was expected to contain: "
                + bas.getBitarchiveAppId(),
                runningBAApps.contains(bas.getBitarchiveAppId()));

        String jobMsgId = "some originatingBatchMsgId";
        BatchEndedMessage batchEndedMessage = new BatchEndedMessage(THE_BAMON,
                                                                    bas.getBitarchiveAppId(),
                                                                    jobMsgId,
                                                                    null);

        con.send(batchEndedMessage);
        con.waitForConcurrentTasksToFinish();

        bas.close();

        List<BatchEndedMessage> batchEndedMsg
                = ((TestBitarchiveMonitorServer) bam_server).getBatchEndedMsg();

        boolean found = false;
        for (BatchEndedMessage aBatchEndedMsg : batchEndedMsg) {
            BatchEndedMessage msg = (BatchEndedMessage) aBatchEndedMsg;
            if (msg.getBitarchiveID().equals(bas.getBitarchiveAppId())) {
                if (msg.getOriginatingBatchMsgID().equals(jobMsgId)) {
                    found = true;
                    break;
                }
            }
        }

        assertTrue(
                "Expected to find a BatchEndedMessage at BA Monitor, with BA Application Id: "
                + bas.getBitarchiveAppId() + " and BatchMessageId: " + jobMsgId,
                found);


    }

    /**
     * Invokes getRunningBAApplicationIds() through reflection.
     */
    private Set<String> getLiveApps(BitarchiveMonitorServer bamonServer) {
        try {
            Field bamonField = ReflectUtils.getPrivateField(
                    BitarchiveMonitorServer.class, "bamon");
            BitarchiveMonitor bamon
                    = (dk.netarkivet.archive.bitarchive.BitarchiveMonitor) bamonField.get(
                    bamonServer);
            Method m = ReflectUtils.getPrivateMethod(BitarchiveMonitor.class,
                                                     "getRunningBitarchiveIDs");
            Object result = m.invoke(bamon);
            return (Set<String>) result;
        } catch (Exception e) {
            e.printStackTrace();
            throw new PermissionDenied(
                    "Couldn't call getRunningBAApplicationIds.", e);
        }
    }

    /**
     * Verify that we are able to declare a batch job completed and a send
     * BatchReply to the requester.
     */
    public void testDeclareBatchJobCompleted() {
        bam_server = new TestBitarchiveMonitorServer();

        // Set up a BitarchiveServer:
        Settings.set(ArchiveSettings.BITARCHIVE_SERVER_FILEDIR, TestInfo.BITARCHIVE_APP_DIR_1);
        Settings.set(CommonSettings.DIR_COMMONTEMPDIR, TestInfo.BITARCHIVE_SERVER_DIR_1);
        BitarchiveServer bas1 = BitarchiveServer.getInstance();

        TestJob job = new TestJob(
                "testBatchReceive_ID"); // job is used for carrying an id to recognize later
        BatchMessage batchMessage = new BatchMessage(THE_BAMON,
                                                     Channels.getTheRepos(),
                                                     job, Settings.get(
                CommonSettings.USE_REPLICA_ID));

        TestBatchReplyListener batchReplyListener
                = new TestBatchReplyListener();
        con.setListener(THE_ARCREPOS, batchReplyListener);

        long timeout = System.currentTimeMillis() + 500L;

        con.send(batchMessage);
        con.waitForConcurrentTasksToFinish();

        while (System.currentTimeMillis() < timeout
               &&
               ((TestBitarchiveMonitorServer) bam_server).getBatchEndedMsg().size()
               < 1) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                // Early wake is fine.
            }
        }
        bas1.close();
        // Check that BA Monitor has received a BatchEndedMessage from both BitarchiveServer instances:

        List<BatchEndedMessage> batchEndedMsg
                = ((TestBitarchiveMonitorServer) bam_server).getBatchEndedMsg();
        boolean bas1Done = false;

        int batchEndedCount = 0;
        for (BatchEndedMessage beMsg : batchEndedMsg) {
            ++batchEndedCount;
            if (beMsg.getBitarchiveID().equals(bas1.getBitarchiveAppId())) {
                bas1Done = true;
            }
        }

        assertTrue(
                "The BA monitor expected, but did not receive BatchEndedMessage from BA Application:"
                + bas1.getBitarchiveAppId(), bas1Done);
        assertTrue(
                "The BA monitor received another number of messages than expected. Expected:"
                + batchEndedMsg.size() + " Received:" + batchEndedCount,
                batchEndedMsg.size() == batchEndedCount);

        // Sleeping for a few milliseconds gives the BAMon time to reply.
        int retries = 10;
        int tries = 0;
        while (tries < retries && batchReplyListener.batchReplyMsg == null) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                // Early wake-up is fine.
            }
            tries++;
        }

        /** Check that a BatchReplyMessage has been put out on THE_ARCREPOS queue: */
        BatchReplyMessage batchReplyMessage
                = batchReplyListener.getBatchReplyMsg();
        assertTrue("No batch reply found on the queue:" + THE_ARCREPOS,
                   (batchReplyMessage != null));
        assertTrue("The queue:" + THE_ARCREPOS
                   + " was expected to contain a BatchReplyMessage for BatchMessage: "
                   + batchMessage + " BatchReplyMessage was:"
                   + batchReplyMessage,
                   batchReplyMessage.getReplyOfId().equals(
                           batchMessage.getID()));
    }

    /**
     * Test that monitor can receive and aggregate data from more than one
     * BitarchiveServer and aggregate the data and upload.
     *
     * @throws ArgumentNotValid
     * @throws UnknownID
     * @throws IOFailure        it via RemoteFile
     */
    public void testBatchEndedMessageAggregation() throws InterruptedException {
        // Start the monitor
        BitarchiveMonitorServer bms = BitarchiveMonitorServer.getInstance();

        // Set up a listener on the reply queue for batch messages
        TestMessageListener listener = new TestMessageListener();
        con.setListener(Channels.getTheRepos(), listener);
        con.setListener(Channels.getAllBa(), listener);

        //File for reply
        File output_file = new File(TestInfo.BAMON_WORKING, "batch_output.txt");

        //Create a batch message
        BatchMessage bm = new BatchMessage(Channels.getTheBamon(),
                                           Channels.getTheRepos(),
                                           new ChecksumJob(), Settings.get(
                CommonSettings.USE_REPLICA_ID));
        JMSConnectionMockupMQ.updateMsgID(bm, "ID50");

        //Invent two BitarchiveServers and send heartbeats from them
        String baID1 = "BA1";
        HeartBeatMessage hbm = new HeartBeatMessage(Channels.getTheBamon(),
                                                    baID1);
        String baID2 = "BA2";
        HeartBeatMessage hbm2 = new HeartBeatMessage(Channels.getTheBamon(),
                                                     baID2);
        bms.visit(hbm);
        bms.visit(hbm2);
        con.waitForConcurrentTasksToFinish();

        //Trigger the bams with the batch message
        bms.visit(bm);
        con.waitForConcurrentTasksToFinish();

        //Now pick up the forwarded Batch message and get its ID
        String forwardedID =
                ((BatchMessage) listener.getAllReceived().get(0)).getID();

        //Now invent two BatchEndedMessages from the two mythical
        //BitarchiveServers
        File data1 = new File(TestInfo.BAMON_WORKING, "batch_output_1.txt");
        File data2 = new File(TestInfo.BAMON_WORKING, "batch_output_2.txt");
        RemoteFile rf1 = RemoteFileFactory.getInstance(data1, true, false, true);
        BatchEndedMessage bem1 = new BatchEndedMessage(Channels.getTheBamon(),
                                                       baID1, forwardedID, rf1);
        JMSConnectionMockupMQ.updateMsgID(bem1, "ID42");
        RemoteFile rf2 = RemoteFileFactory.getInstance(data2, true, false, true);
        BatchEndedMessage bem2 = new BatchEndedMessage(Channels.getTheBamon(),
                                                       baID2, forwardedID, rf2);
        JMSConnectionMockupMQ.updateMsgID(bem2, "ID54");
        bms.visit(bem1);
//        synchronized(this) {
 //           wait(50);
  //      }
        bms.visit(bem2);
        con.waitForConcurrentTasksToFinish();
        synchronized (this) {
            wait(200);
        }
        bms.close();

        //Now there should also be one BatchReplyMessage in the listener and the
        //monitor should have written the data to a remote file we can collect
        assertEquals("Should have received exactly two messages", 2,
                     listener.getNumReceived());
        List<NetarkivetMessage> l = listener.getAllReceived();
        BatchReplyMessage brmsg = null;
        for (NetarkivetMessage naMsg : l) {
            if (naMsg instanceof BatchReplyMessage) {
                brmsg = (BatchReplyMessage) naMsg;
                brmsg.getResultFile().copyTo(output_file);
            }
        }
        
        FileAsserts.assertFileNumberOfLines("Aggregated file should have two " +
                                            "lines", output_file, 2);
        FileAsserts.assertFileContains("File contents not as expected",
                                       "1", output_file);
        FileAsserts.assertFileContains("File contents not as expected",
                                       "2", output_file);

        //To be extra careful, check that the file length in bytes is as
        //expected
        int expected_length = FileUtils.readBinaryFile(data1).length +
                              FileUtils.readBinaryFile(data2).length;
        int actual_length = FileUtils.readBinaryFile(output_file).length;

        assertEquals("File length (in bytes) is not as expected",
                     expected_length, actual_length);

        // Also check that original files have been deleted
        assertTrue("First file should have been deleted",
                   ((TestRemoteFile) rf1).isDeleted());

        assertTrue("Second file should have been deleted",
                   ((TestRemoteFile) rf2).isDeleted());
    }

    /**
     * Testing GetAllChecksumMessage.
     */
    public void testGetAllChecksumMessage() throws InterruptedException, IOException {
        bam_server = BitarchiveMonitorServer.getInstance();
        
        // Set up a listener on the reply queue for batch messages
        TestMessageListener listener = new TestMessageListener();
        con.setListener(Channels.getTheRepos(), listener);
        
        String repId = Settings.get(CommonSettings.USE_REPLICA_ID);
        
        MockupBitarchiveBatch mbb = new MockupBitarchiveBatch(repId);
        mbb.heartBeat(bam_server);
        
        GetAllChecksumsMessage gacm = new GetAllChecksumsMessage(
                Channels.getTheBamon(), Channels.getTheRepos(), repId);

        JMSConnectionMockupMQ.updateMsgID(gacm, "gacm1");
        
        bam_server.visit(gacm);

        synchronized(this) {
            wait(50);
        }
        
        bam_server.visit(mbb.replyForLatestJob(RemoteFileFactory.getMovefileInstance(
                TestInfo.BATCH_ALL_CHECKSUM_OUTPUT_FILE)));

        con.waitForConcurrentTasksToFinish();
        
        synchronized(this) {
            wait(400);
        }

        assertEquals("The listener should have one message", 1, listener.getNumReceived());
        NetarkivetMessage msg = listener.getReceived();
        assertTrue("The following message should be a GetAllChecksumMessage reply: '" + msg + "'", 
                msg instanceof GetAllChecksumsMessage);

        GetAllChecksumsMessage returnMsg = (GetAllChecksumsMessage) msg;
        File tempFile = File.createTempFile("checksum", "all", TestInfo.BAMON_WORKING);
        returnMsg.getData(tempFile);
        
        List<String> batchOutput = FileUtils.readListFromFile(tempFile);
        
        assertEquals("The output from batch should have " + 2 + " lines, but it had " + batchOutput.size(), 
                2, batchOutput.size());
        
        String batchResult1 = "myArc1.arc##1234567890";
        String batchResult2 = "myArc2.arc##0987654321";
        
        assertTrue("The result '" + batchOutput.toString() + "' should contain '" + batchResult1, 
                batchOutput.toString().contains(batchResult1));
        assertTrue("The result '" + batchOutput.toString() + "' should contain '" + batchResult2 + "'", 
                batchOutput.toString().contains(batchResult2));
        
        tempFile.delete();
    }

    /**
     * Testing GetAllChecksumMessage.
     */
    public void testGetAllFilenamesMessage() throws InterruptedException, IOException {
        bam_server = BitarchiveMonitorServer.getInstance();
        
        // Set up a listener on the reply queue for batch messages
        TestMessageListener listener = new TestMessageListener();
        con.setListener(Channels.getTheRepos(), listener);
        
        String repId = Settings.get(CommonSettings.USE_REPLICA_ID);
        
        MockupBitarchiveBatch mbb = new MockupBitarchiveBatch(repId);
        mbb.heartBeat(bam_server);
        
        GetAllFilenamesMessage gafm = new GetAllFilenamesMessage(
                Channels.getTheBamon(), Channels.getTheRepos(), repId);

        JMSConnectionMockupMQ.updateMsgID(gafm, "gafm1");
        
        bam_server.visit(gafm);

        synchronized(this) {
            wait(50);
        }
        
        bam_server.visit(mbb.replyForLatestJob(RemoteFileFactory.getMovefileInstance(
                TestInfo.BATCH_ALL_FILENAMES_OUTPUT_FILE)));

        con.waitForConcurrentTasksToFinish();
        
        synchronized(this) {
            wait(200);
        }

        assertEquals("The listener should have one message", 1, listener.getNumReceived());
        NetarkivetMessage msg = listener.getReceived();
        assertTrue("The following message should be a GetAllChecksumMessage reply: '" + msg + "'", 
                msg instanceof GetAllFilenamesMessage);

        GetAllFilenamesMessage returnMsg = (GetAllFilenamesMessage) msg;
        File tempFile = File.createTempFile("filenames", "all", TestInfo.BAMON_WORKING);
        returnMsg.getData(tempFile);
        
        List<String> batchOutput = FileUtils.readListFromFile(tempFile);
        
        assertEquals("The output from batch should have " + 2 + " lines, but it had " + batchOutput.size(), 
                2, batchOutput.size());
        
        String batchResult1 = "file1.arc";
        String batchResult2 = "file2.arc";
        
        assertTrue("The result '" + batchOutput.toString() + "' should contain '" + batchResult1, 
                batchOutput.toString().contains(batchResult1));
        assertTrue("The result '" + batchOutput.toString() + "' should contain '" + batchResult2 + "'", 
                batchOutput.toString().contains(batchResult2));
        
        tempFile.delete();
    }

    /**
     * Testing GetChecksumMessage.
     */
    public void testGetChecksumMessage() throws InterruptedException, IOException {
        bam_server = BitarchiveMonitorServer.getInstance();
        
        // Set up a listener on the reply queue for batch messages
        TestMessageListener listener = new TestMessageListener();
        con.setListener(THE_ARCREPOS, listener);
        
        String repId = Settings.get(CommonSettings.USE_REPLICA_ID);
        
        MockupBitarchiveBatch mbb = new MockupBitarchiveBatch(repId);
        mbb.heartBeat(bam_server);
        
        GetChecksumMessage gcm = new GetChecksumMessage(Channels.getTheBamon(), 
                Channels.getTheRepos(), "requestedFile.arc", repId);

        JMSConnectionMockupMQ.updateMsgID(gcm, "gcm1");
        
        bam_server.visit(gcm);
        
        con.waitForConcurrentTasksToFinish();
        
        synchronized(this) {
            wait(50);
        }
        
        bam_server.visit(mbb.replyForLatestJob(RemoteFileFactory.getMovefileInstance(
                TestInfo.BATCH_ONE_CHECKSUM_OUTPUT_FILE)));
        
        synchronized(this) {
            wait(200);
        }

        con.waitForConcurrentTasksToFinish();
        
        assertEquals("The listener should have one message", 1, listener.getNumReceived());
        NetarkivetMessage msg = listener.getReceived();
        assertTrue("The following message should be a GetAllChecksumMessage reply: '" + msg + "'", 
                msg instanceof GetChecksumMessage);

        GetChecksumMessage returnMsg = (GetChecksumMessage) msg;
        
        assertEquals("Did not give expected arcfilename", returnMsg.getArcfileName(), "requestedFile.arc");
        assertEquals("Did not give expected checksum", returnMsg.getChecksum(), "0192837465");
    }

    /**
     * Tests the opportunity to correct a entry in the archive through CorrectMessage. 
     * @throws InterruptedException 
     */
    public void testCorrectMessage() throws InterruptedException {
        bam_server = BitarchiveMonitorServer.getInstance();
        
        TestMessageListener listener = new TestMessageListener();
        con.setListener(THE_BAMON, listener);
        
        // Mockup listener replacement of a bitarchive.
        TestMessageListener baListener = new TestMessageListener(); 
        con.setListener(ALL_BA, baListener);
        
        // Mockup listener replacement of the arc repository.
        TestMessageListener arcListener = new TestMessageListener();
        con.setListener(THE_ARCREPOS, arcListener);
        
        // retrieve replica id
        String repId = Settings.get(CommonSettings.USE_REPLICA_ID);

        String badChecksum = "error-123";
        String credentials = "exampleCredentials";
        
        RemoteFile rf = RemoteFileFactory.getCopyfileInstance(TestInfo.CORRECT_ARC_FILE);
        
        CorrectMessage cm = new CorrectMessage(THE_BAMON, THE_ARCREPOS, 
                badChecksum, rf, repId, credentials);
        
        JMSConnectionMockupMQ.updateMsgID(cm, "cm1");
        
        bam_server.visit(cm);
        
        con.waitForConcurrentTasksToFinish();
        
        synchronized(this) {
            wait(50);
        }
        
        NetarkivetMessage msg1 = baListener.getReceived();
        assertTrue("The message '" + msg1 + "' should be of the type RemoveAndGetFileMessage", 
                msg1 instanceof RemoveAndGetFileMessage);
        RemoveAndGetFileMessage ragfm = (RemoveAndGetFileMessage) msg1;
        
        ragfm.setFile(TestInfo.BAD_ARC_FILE);
        bam_server.visit(ragfm);
        
        con.waitForConcurrentTasksToFinish();
        
        synchronized(this) {
            wait(50);
        }
        
        NetarkivetMessage msg2 = baListener.getReceived();
        assertTrue("The message '" + msg2 + "' should be of the type UploadMessage",
                msg2 instanceof UploadMessage);
        UploadMessage um = (UploadMessage) msg2;
        
        bam_server.visit(um);
        
        con.waitForConcurrentTasksToFinish();
        
        synchronized(this) {
            wait(50);
        }
        
        NetarkivetMessage msg3 = arcListener.getReceived();
        assertTrue("The message '" + msg3 + "' should be of the type CorrectMessage",
                msg3 instanceof CorrectMessage);
        
        CorrectMessage res = (CorrectMessage) msg3;
        
        assertNotNull("The remoteFile in the reply to the CorrectMessage must not be null.", 
                res.getRemovedFile());
        assertEquals("The remoteFile should refer to the file 'file-2.arc'",
                "file-2.arc", res.getRemovedFile().getName());
    }
    
    /**
     * A mockup Bitarchive that knows how to send heartbeats and how to create a
     * reply of the last received batch job.
     */
    private static class MockupBitarchiveBatch extends TestMessageListener {
        private String id;

        public MockupBitarchiveBatch(String id) {
            this.id = id;
            JMSConnectionFactory.getInstance().setListener(Channels.getAllBa(), this);
        }

        public BatchEndedMessage replyForLatestJob(RemoteFile result) {
            return new BatchEndedMessage(
                    Channels.getTheBamon(),
                    id,
                    getIDOfLatestJob(),
                    result);
        }

        public void heartBeat(BitarchiveMonitorServer bamon) {
            bamon.visit(new HeartBeatMessage(Channels.getTheBamon(), id));
        }

        private String getIDOfLatestJob() {
            ((JMSConnectionMockupMQ) JMSConnectionFactory.getInstance()).waitForConcurrentTasksToFinish();
            NetarkivetMessage resMsg = getLastInstance(BatchMessage.class);
            assertNotNull("The last instance of BatchMessage should not be null.", resMsg);
            return resMsg.getID();
        }
    }
    
    /**
     * Cannot use NullRemoteFile in tests, as the BAMON just ignores
     * NullRemoteFiles. Therefore, we need another passive implementation of
     * RemoteFile.
     */
    abstract class AltNullRemoteFile implements RemoteFile {
        public void copyTo(File destFile) {
        }

        public void appendTo(OutputStream out) {
        }

        public InputStream getInputStream() {
            return null;
        }

        public void cleanup() {
        }

        public long getSize() {
            return 0;
        }

        public String getName() {
            return null;
        }

        public String getChecksum() {
            return null;
        }
    }

    /**
     * A RemoteFile that blocks for 5 seconds, then fails, on appendTo(). To
     * avoid this, call getWaker().appendTo().
     */
    private class BlockingRF extends AltNullRemoteFile {
        private static final String FIRST_STD_MESSAGE = "BLOCKING...";
        private static final String SECOND_STD_MESSAGE = "UNBLOCKED";
        public static final String STD_CONTENT = FIRST_STD_MESSAGE
                                                 + SECOND_STD_MESSAGE;
        public static final String WAKER_CONTENT = "WAKING SOON";
        public String failed = null;
        private boolean woken = false;

        public void appendTo(OutputStream out) {
            PrintStream pw = new PrintStream(out);
            pw.print(FIRST_STD_MESSAGE);
            try {
                synchronized (this) {
                    wait(5000);
                }
                if (!woken) {
                    failed = "Was not woken up";
                    return;
                }
                synchronized (this) {
                    wait(200);
                }
                pw.print(SECOND_STD_MESSAGE);
                pw.close();
            } catch (InterruptedException e) {
                failed = "Interrupted! " + woken;
            }
        }

        public RemoteFile getWaker() {
            return new AltNullRemoteFile() {
                public void appendTo(OutputStream out) {
                    PrintStream ps = new PrintStream(out);
                    ps.print(WAKER_CONTENT);
                    ps.close();
                    synchronized (BlockingRF.this) {
                        BlockingRF.this.woken = true;
                        BlockingRF.this.notifyAll();
                    }
                }
            };
        }
    }


    private class TestBitarchiveMonitorServer extends BitarchiveMonitorServer {
        protected int countmsgProcessed = 0;
        protected List<BatchMessage> batchMsg = new ArrayList<BatchMessage>();
        protected List<BatchEndedMessage> batchEndedMsg
                = new ArrayList<BatchEndedMessage>();
        protected List<HeartBeatMessage> heartBeatMsg
                = new ArrayList<HeartBeatMessage>();

        public void onMessage(Message msg) {
            synchronized (this) {
                ObjectMessage objMsg = (ObjectMessage) msg;
                try {

                    NetarkivetMessage netMsg
                            = (NetarkivetMessage) objMsg.getObject();

                    if (netMsg instanceof BatchMessage) {
                        batchMsg.add((BatchMessage) netMsg);
                    } else if (netMsg instanceof BatchEndedMessage) {
                        batchEndedMsg.add((BatchEndedMessage) netMsg);
                    } else if (netMsg instanceof HeartBeatMessage) {
                        heartBeatMsg.add((HeartBeatMessage) netMsg);
                    } else {
                        throw new UnknownID(
                                "TestBitarchiveMonitorServer unable to handle this message type: "
                                + msg);
                    }
                } catch (JMSException e) {
                    throw new UnknownID(
                            "JMSException thrown in TestBitarchiveMonitorServer",
                            e);
                }
            }
            ++countmsgProcessed;
            super.onMessage(msg);
        }

        protected List<BatchMessage> getBatchMsg() {
            return batchMsg;
        }

        protected List<BatchEndedMessage> getBatchEndedMsg() {
            return batchEndedMsg;
        }

        protected List<HeartBeatMessage> getHeartBeatMsg() {
            return heartBeatMsg;
        }

        protected BatchReplyMessage getBatchReplyMessage(String batchJobId) {
            throw new NotImplementedException(
                    "Should return the BatchReplyMessage, identified by this batchJobID: "
                    + batchJobId + ". Only for test purposes.");
        }

        protected int getCountmsgProcessed() {
            return countmsgProcessed;
        }


    } // end class TestBitarchiveMonitorServer

    private class TestListener implements MessageListener {
        private NetarkivetMessage lastMessage = null;
        private BatchReplyMessage lastBatchReplyMessage = null;

        public TestListener() {
        }

        public NetarkivetMessage getLastMessage() {
            return lastMessage;
        }

        public NetarkivetMessage getLastBatchReplyMessage() {
            return lastBatchReplyMessage;
        }

        public void onMessage(Message msg) {
            NetarkivetMessage content = JMSConnection.unpack(msg);
            if (content instanceof NetarkivetMessage) {
                lastMessage = (NetarkivetMessage) content;
            }
            if (content instanceof BatchReplyMessage) {
                lastBatchReplyMessage = (BatchReplyMessage) content;
            }
        }
    }

    private class TestBatchReplyListener implements MessageListener {
        private BatchReplyMessage batchReplyMsg;

        public TestBatchReplyListener() {
        }

        public BatchReplyMessage getBatchReplyMsg() {
            return batchReplyMsg;
        }

        public void onMessage(Message msg) {
            NetarkivetMessage content = JMSConnection.unpack(msg);
            if (content instanceof BatchReplyMessage) {
                batchReplyMsg = (BatchReplyMessage) content;
            }
        }
    } // end class TestMessageListener
    
    /**
     * Ensure, that the application dies if given the wrong input.
     */
    public void testApplication() {
        ReflectUtils.testUtilityConstructor(BitarchiveMonitorApplication.class);

        PreventSystemExit pse = new PreventSystemExit();
        PreserveStdStreams pss = new PreserveStdStreams(true);
        pse.setUp();
        pss.setUp();
        
        try {
            BitarchiveMonitorApplication.main(new String[]{"ERROR"});
            fail("It should throw an exception ");
        } catch (SecurityException e) {
            // expected !
        }

        pss.tearDown();
        pse.tearDown();
        
        assertEquals("Should give exit code 1", 1, pse.getExitValue());
        assertTrue("Should tell that no arguments are expected.", 
                pss.getOut().contains("This application takes no arguments"));
    }

}
