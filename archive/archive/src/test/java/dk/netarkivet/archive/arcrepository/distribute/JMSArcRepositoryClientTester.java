/*$Id$
* $Revision$
* $Date$
* $Author$
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
package dk.netarkivet.archive.arcrepository.distribute;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;
import junit.framework.TestListener;

import org.archive.io.arc.ARCConstants;
import org.archive.io.arc.ARCRecord;
import org.archive.io.arc.ARCRecordMetaData;

import dk.netarkivet.archive.arcrepository.bitpreservation.AdminDataMessage;
import dk.netarkivet.archive.arcrepository.bitpreservation.TestInfo;
import dk.netarkivet.archive.bitarchive.distribute.BatchMessage;
import dk.netarkivet.archive.bitarchive.distribute.BatchReplyMessage;
import dk.netarkivet.archive.bitarchive.distribute.GetFileMessage;
import dk.netarkivet.archive.bitarchive.distribute.GetMessage;
import dk.netarkivet.archive.checksum.distribute.CorrectMessage;
import dk.netarkivet.archive.checksum.distribute.GetAllChecksumsMessage;
import dk.netarkivet.archive.checksum.distribute.GetAllFilenamesMessage;
import dk.netarkivet.archive.checksum.distribute.GetChecksumMessage;
import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.distribute.ChannelID;
import dk.netarkivet.common.distribute.Channels;
import dk.netarkivet.common.distribute.JMSConnection;
import dk.netarkivet.common.distribute.JMSConnectionFactory;
import dk.netarkivet.common.distribute.JMSConnectionMockupMQ;
import dk.netarkivet.common.distribute.NetarkivetMessage;
import dk.netarkivet.common.distribute.NullRemoteFile;
import dk.netarkivet.common.distribute.RemoteFile;
import dk.netarkivet.common.distribute.RemoteFileFactory;
import dk.netarkivet.common.distribute.Synchronizer;
import dk.netarkivet.common.distribute.TestRemoteFile;
import dk.netarkivet.common.distribute.arcrepository.ArcRepositoryClientFactory;
import dk.netarkivet.common.distribute.arcrepository.BatchStatus;
import dk.netarkivet.common.distribute.arcrepository.BitarchiveRecord;
import dk.netarkivet.common.distribute.arcrepository.HarvesterArcRepositoryClient;
import dk.netarkivet.common.distribute.arcrepository.PreservationArcRepositoryClient;
import dk.netarkivet.common.distribute.arcrepository.Replica;
import dk.netarkivet.common.distribute.arcrepository.ReplicaStoreState;
import dk.netarkivet.common.distribute.arcrepository.ViewerArcRepositoryClient;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.NotificationsFactory;
import dk.netarkivet.common.utils.RememberNotifications;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.utils.StreamUtils;
import dk.netarkivet.common.utils.batch.FileBatchJob;
import dk.netarkivet.testutils.CollectionAsserts;
import dk.netarkivet.testutils.FileAsserts;
import dk.netarkivet.testutils.ReflectUtils;
import dk.netarkivet.testutils.StringAsserts;
import dk.netarkivet.testutils.TestFileUtils;
import dk.netarkivet.testutils.preconfigured.ReloadSettings;
import dk.netarkivet.testutils.preconfigured.UseTestRemoteFile;

/**
 */
public class JMSArcRepositoryClientTester extends TestCase {

    private static final File BASEDIR =
            new File(
                    "tests/dk/netarkivet/archive/arcrepository/distribute/data");

    private static final File ORIGINALS = new File(BASEDIR, "originals");

    private static final File WORKING = new File(BASEDIR, "working");

    private static final File ARCDIR = new File(WORKING, "local_files");
    private static final File ARCFILE =
            new File(ARCDIR, "Upload2.ARC");
    
    private static final File ALL_CHECKSUM_FILE = 
        new File(WORKING, "all.checksum");
    private static final File ALL_FILENAME_FILE = 
        new File(WORKING, "all.filename");
    private static final File SEND_CORRECT_FILE =
        new File(WORKING, "send.correct");
    private static final File RES_CORRECT_FILE =
        new File(WORKING, "res.correct");


    JMSArcRepositoryClient arc;
    private JMSArcRepositoryClient arcrepos;
    UseTestRemoteFile utrf = new UseTestRemoteFile();
    ReloadSettings rs = new ReloadSettings();

    protected void setUp() throws Exception {
        rs.setUp();
        utrf.setUp();
        FileUtils.removeRecursively(WORKING);
        TestFileUtils.copyDirectoryNonCVS(ORIGINALS, WORKING);
        JMSConnectionMockupMQ.useJMSConnectionMockupMQ();
        Settings.set(CommonSettings.NOTIFICATIONS_CLASS,
                     RememberNotifications.class.getName());
        Settings.set(JMSArcRepositoryClient.ARCREPOSITORY_GET_TIMEOUT, "1000");
        arc = (JMSArcRepositoryClient) ArcRepositoryClientFactory.getPreservationInstance();

    }

    protected void tearDown() throws Exception {
        if (arc != null) {
            try {
                arc.close();
            } catch (ArgumentNotValid e) {
                // Just ignore, happens because we fiddle with internal state in 
                // a few tests. Make sure we invalidate the instance, at least.
                Field field = ReflectUtils.getPrivateField(
                        JMSArcRepositoryClient.class, "instance");
                field.set(null, null);

            }
        }
        if (arcrepos != null) {
            arcrepos.close();
        }
        utrf.tearDown();
        FileUtils.removeRecursively(WORKING);
        RememberNotifications.resetSingleton();
        rs.tearDown();
    }

    /** Verify that the JMSArcRepositoryClient class has no public constructor. */
    public void testNoPublicConstructor() {
        Constructor[] ctors = JMSArcRepositoryClient.class.getConstructors();
        assertEquals("Found public constructors for JMSArcRepositoryClient.", 0,
                     ctors.length);
    }

    /**
     * Tests the correct object is returned when getPreservationInstance is
     * called.
     */
    public void testGetPreservationInstance() {
        PreservationArcRepositoryClient arcrep
                = ArcRepositoryClientFactory.getPreservationInstance();
        assertTrue("Must return an instance of PreservationArcRepositoryClient,"
                   + " not " + arcrep.getClass(),
                   arcrep instanceof PreservationArcRepositoryClient);
    }

    /** Tests the correct object is returned when getViewerInstance is called. */
    public void testGetViewerInstance() {
        assertTrue("Must return an instance of ViewerArcRepositoryClient",
                   (arcrepos
                           = (JMSArcRepositoryClient) ArcRepositoryClientFactory.getViewerInstance()) instanceof
                           ViewerArcRepositoryClient);
    }

    /** Tests the correct object is returned when getHacoInstance is called. */
    public void testGetHacoInstance() {
        assertTrue("Must return an instance of HarvesterArcRepositoryClient",
                   ArcRepositoryClientFactory.getHarvesterInstance() instanceof
                           HarvesterArcRepositoryClient);
    }

    /** Test get() methods arguments. */
    public void testGetArgumentsNotNull() {
        /**
         * Test if ArgumentNotValid is thrown if null
         * is given as first parameter
         */
        try {
            arc.get(null, 0);
            fail("Should throw ArgumentNotValid exception");
        } catch (ArgumentNotValid e) {
            // Expected
        }

        /**
         * Test if ArgumentNotValid is thrown if a negative value
         * is given as second parameter
         */
        try {
            arc.get("dummy.arc", -5);
            fail("Should throw ArgumentNotValid exception");
        } catch (ArgumentNotValid e) {
            // Expected
        }
    }

    /**
     * This tests the get()-method returns a BitarchiveRecord via JMS. The reply
     * record should contain a string: <code>filename+" "+index</code>.
     */
    public void testGet() {
        DummyGetMessageReplyServer replyServer
                = new DummyGetMessageReplyServer();
        String filename = "dummy.arc";
        long index = 0;
        replyServer.setBitarchiveRecord(null);
        BitarchiveRecord bar = arc.get(filename, index);
        assertNotNull("The reply should not be null", bar);
        // BitarchiveRecord.getData() now returns a InputStream instead of a byte[]
        InputStream theData = bar.getData();
        byte[] contents = StreamUtils.inputStreamToBytes(theData,
                                                       (int) bar.getLength());
        assertEquals("The reply doesn't contain the correct data",
                     filename + " " + index, new String(contents));
        assertEquals("The reply should come from the DummyServer",
                     bar, replyServer.getBitarchiveRecord());
        replyServer.close();
    }

    /**
     * This tests the getFile()-method returns a file via JMS. The reply file
     * should contain a string: <code>filename+" "+index</code>.
     *
     * @throws IOException if arc throws one
     */
    public void testGetFile() throws IOException {
        DummyGetFileMessageReplyServer replyServer =
                new DummyGetFileMessageReplyServer(ARCDIR);
        String filename = "Upload2.ARC";
        File toFile = new File(WORKING, "newFile.arc");
        Replica replica =
                Replica.getReplicaFromId(Settings.get(
                        CommonSettings.USE_REPLICA_ID));
        arc.getFile(filename, replica, toFile);
        assertTrue("Result file should exist", toFile.exists());
        assertEquals("Result file should contain right text",
                     FileUtils.readFile(new File(ARCDIR, filename)),
                     FileUtils.readFile(toFile));

        toFile = new File(WORKING, "newFile2.arc");
        try {
            arc.getFile("fnord", replica, toFile);
            fail("Should throw exception on failure");
        } catch (IOFailure e) {
            // expected
        }
        assertFalse("Result file should not exist for missing arcfile",
                    toFile.exists());

        try {
            arc.getFile(null, replica, toFile);
            fail("Should throw exception on null arg");
        } catch (ArgumentNotValid e) {
            //expected
        }

        try {
            arc.getFile("", replica, toFile);
            fail("Should throw exception on empty arg");
        } catch (ArgumentNotValid e) {
            //expected
        }

        try {
            arc.getFile("foo", null, toFile);
            fail("Should throw exception on null arg");
        } catch (ArgumentNotValid e) {
            //expected
        }

        try {
            arc.getFile("foo", replica, null);
            fail("Should throw exception on null arg");
        } catch (ArgumentNotValid e) {
            //expected
        }


        replyServer.close();
    }

    /** This tests the get()-method when it times out waiting for a reply. */
    public void testGetTimeout() {

        DummyGetMessageReplyServer replyServer
                = new DummyGetMessageReplyServer();
        replyServer.noReply = true;
        String filename = "dummy.arc";
        long index = 0;
        replyServer.setBitarchiveRecord(null);
        BitarchiveRecord bar = arc.get(filename, index);
        assertNull("The reply should be null", bar);
        replyServer.close();
    }

    /**
     * Testing that a message is sent via JMS and a ArgumentNotValid is thrown
     * when message is incorrect.
     *
     * @throws IOException if we cant create new file
     */
    public void testStoreMessageNotOK() throws IOException {
        DummyStoreMessageReplyServer replyServer
                = new DummyStoreMessageReplyServer();
        PreservationArcRepositoryClient arc
                = ArcRepositoryClientFactory.getPreservationInstance();
        File f = new File(WORKING, "notok.arc");
        if (f.createNewFile()) {
            try {
                arc.store(f);
                fail("Exception expected when submitting notok.arc file");
            } catch (IOFailure e) {
                // Expected
            }
            replyServer.close();
            CollectionAsserts.assertListEquals(
                    "Should have no remaining remote files after failure",
                    new ArrayList<RemoteFile>(TestRemoteFile.remainingFiles()));
        } else {
            fail("Can't create new file");
        }
    }

    /**
     * tests that a successful store reply will result in the RemoteFile being
     * deleted.
     *
     * @throws InterruptedException if ...
     */
    public void testStoreDelete() throws InterruptedException {
        // Set a listener on PRES
        Settings.set(CommonSettings.DIR_COMMONTEMPDIR, "tests/commontempdir");
        GenericMessageListener listener = new GenericMessageListener();
        JMSConnection con = JMSConnectionFactory.getInstance();
        con.setListener(Channels.getTheRepos(), listener);
        final boolean[] done = new boolean[]{false};
        // Send a store message in a thread
        Thread t = new Thread() {
            public void run() {
                arc.store(ARCFILE);
                synchronized (this) {
                    done[0] = true;
                    notifyAll();
                }
            }
        };
        t.start();
        // Loop until the message is received at the listener
        while (listener.messagesReceived.isEmpty()) {
            Thread.sleep(10);
        }
        // Get the store message
        StoreMessage sm = (StoreMessage) listener.messagesReceived.get(0);
        // Send the message back to indicate a succesful store
        con.resend(sm, Channels.getThisReposClient());
        // Let the other thread process the message
        ((JMSConnectionMockupMQ) con).waitForConcurrentTasksToFinish();
        // And check that the file has been deleted
        synchronized (t) {
            try {
                while (!done[0]) {
                    t.wait();
                }
            } catch (InterruptedException e) {
                fail("Interrupted");
            }
        }
        assertFalse(ARCFILE.getAbsolutePath() + " should be deleted after " +
                    "successful store", ARCFILE.exists());
        String[] tempfiles = new File("tests/commontempdir").list();
        if (tempfiles != null) {
            for (String tempfile : tempfiles) {
                StringAsserts.assertStringNotContains(
                        "Should not have a temp file left from uploading "
                        + ARCFILE,
                        ARCFILE.getName(), tempfile);
            }
        }
    }

    /** Test batch() methods arguments. */
    public void testBatchArgumentsNotNull() {
        FileBatchJob batchJob = new FileBatchJob() {
            public void finish(OutputStream os) {
            }

            public boolean processFile(File file, OutputStream os) {
                return true;
            }

            public void initialize(OutputStream os) {
            }
        };

        /**
         * Test if ArgumentNotValid is thrown if null
         * is given as first parameter
         */
        try {
            arc.batch(null,
                      Settings.get(CommonSettings.USE_REPLICA_ID));
            fail("Should throw ArgumentNotValid exception");
        } catch (ArgumentNotValid e) {
            // Expected
        }

        /**
         * Test if ArgumentNotValid is thrown if null
         * is given as third parameter
         */

        try {
            arc.batch(batchJob, null);
            fail("Should throw ArgumentNotValid exception");
        } catch (ArgumentNotValid e) {
            // Expected
        }
    }


    /** Test JMSArcRepositoryClient.batch is distributed. */
    public void testBatch() {
        new DummyBatchMessageReplyServer();

        FileBatchJob batchJob = new FileBatchJob() {
            public void finish(OutputStream os) {
            }

            public boolean processFile(File file, OutputStream os) {
                return true;
            }

            public void initialize(OutputStream os) {
            }
        };
        BatchStatus lbStatus = arc.batch(batchJob, Settings.get(
                CommonSettings.USE_REPLICA_ID));
        assertEquals("Number of files should have been set by the server to 42",
                     42, lbStatus.getNoOfFilesProcessed());
    }

    /**
     * Tests StoreRetreies
     *
     * @throws IOException if creation of files fails
     */
    public void testStoreRetries() throws IOException {
        DummyStoreMessageReplyServer ar = new DummyStoreMessageReplyServer();

        File fail1 = new File(WORKING, "fail1");
        File fail2 = new File(WORKING, "fail2");
        File fail3 = new File(WORKING, "fail3");
        if (!fail1.createNewFile() || !fail2.createNewFile()
            || !fail3.createNewFile()) {
            fail("Can't create files");
        }
        arc.store(fail1);
        assertEquals("Should have succeeded on second try", 2, ar.received);
        ar.reset();
        arc.store(fail2);
        assertEquals("Should have succeeded on third try", 3, ar.received);
        ar.reset();
        try {
            arc.store(fail3);
            ar.reset();
            fail("Expected IO failure if failed more than three times");
        } catch (IOFailure e) {
            //expected
        }
        assertEquals("Should have tried three times and failed", 3,
                     ar.received);
        CollectionAsserts.assertListEquals(
                "Should have no remaining files after failed retries",
                new ArrayList<RemoteFile>(TestRemoteFile.remainingFiles()));
    }

    public void testStoreTimeouts() throws IOException, InterruptedException {
        //timeout after 1 millisecond
        //not - no listeners

        Settings.set(JMSArcRepositoryClient.ARCREPOSITORY_STORE_TIMEOUT, "1");
        arc.close();
        arc
                = (JMSArcRepositoryClient) ArcRepositoryClientFactory.getHarvesterInstance();
        final boolean[] ok = new boolean[]{false};
        new Thread() {
            public void run() {
                try {
                    arc.store(ARCFILE);
                } catch (IOFailure e) {
                    //expected. timeout.
                }
                ok[0] = true;
                synchronized (JMSArcRepositoryClientTester.this) {
                    JMSArcRepositoryClientTester.this.notify();
                }

            }
        }.start();

        synchronized (this) {
            wait(3200);
        }

        assertTrue("Store should have timed out and ended!", ok[0]);
        CollectionAsserts.assertListEquals(
                "Should have no remaining files after exception",
                new ArrayList<RemoteFile>(TestRemoteFile.remainingFiles()));
    }

    /**
     * Tests that locally generated exceptions in the JMSArcRepositoryClient
     * gives a message.  See bug #867
     *
     * @throws NoSuchFieldException   if field doens't exists
     * @throws IllegalAccessException if access denied
     */
    public void testStoreException()
            throws NoSuchFieldException, IllegalAccessException {
        // Smashing the replyQ makes the inside of the store loop throw
        // an exception that should be caught.
        RememberNotifications notified
                = (RememberNotifications) NotificationsFactory.getInstance();
        Field arcReplyQ = ReflectUtils.getPrivateField(
                JMSArcRepositoryClient.class,
                "replyQ");
        ChannelID replyQ = (ChannelID) arcReplyQ.get(arc);
        arcReplyQ.set(arc, null);
        // Must remove listener from replyQ or tearDown dies.
        JMSConnectionMockupMQ testMQ
                = ((JMSConnectionMockupMQ) JMSConnectionMockupMQ.getInstance());
        List<MessageListener> listeners = testMQ.getListeners(replyQ);
        for (MessageListener listener : listeners) {
            testMQ.removeListener(replyQ, listener);
        }
        try {
            arc.store(ARCFILE);
            fail("Should have an IOFailure after forcing internal exception");
        } catch (IOFailure e) {
            StringAsserts.assertStringMatches(
                    "Should have received a notification on client errors",
                    "Client-side exception occurred.*attempt number 1 of 3.*attempt number 2 of 3",
                    notified.message);
        }
    }

    /**
     * Test that remote files are cleaned up after exceptions. Bug #1080
     *
     * @throws IllegalAccessException if field doens't exists
     * @throws NoSuchFieldException   if access denied
     */
    public void testStoreFailed()
            throws NoSuchFieldException, IllegalAccessException {
        // Set Synchronizers request field to null to get an appropriately
        // late exception.
        Field requests = ReflectUtils.getPrivateField(Synchronizer.class,
                                                      "requests");
        requests.set(arc, null);
        try {
            arc.store(ARCFILE);
            fail("Should have an IOFailure after forcing internal exception");
        } catch (IOFailure e) {
            // Expected to happen
        }
        CollectionAsserts.assertListEquals(
                "Should have no remaining files after exception",
                new ArrayList<RemoteFile>(TestRemoteFile.remainingFiles()));
    }
    
    public void testUpdateAdminData1() throws InterruptedException {
        JMSConnectionMockupMQ jmsCon = 
            (JMSConnectionMockupMQ) JMSConnectionMockupMQ.getInstance();
        
        GenericMessageListener listener = new GenericMessageListener();
        jmsCon.setListener(Channels.getTheRepos(), listener);
        
        Thread runner = new Thread() {
            public void run() {
                arc.updateAdminData("filename", "ONE", ReplicaStoreState.DATA_UPLOADED);
            }
        };
        runner.start();
        
        synchronized (this) {
            wait(50);
        }
        jmsCon.waitForConcurrentTasksToFinish();
        
        assertEquals("Expected 1 message on TheRepos queue", 
                1, listener.messagesReceived.size());
        NetarkivetMessage msg = listener.messagesReceived.remove(0);
        assertTrue("Expected type of AdminDataMessage but was '"
                + msg.getClass() + "'", msg instanceof AdminDataMessage);
        AdminDataMessage adm = (AdminDataMessage) msg;
        assertFalse("It should not be a change checksum admin data message", 
                adm.isChangeChecksum());
        assertTrue("It should be a change store state admin data message", 
                adm.isChangeStoreState());
    }
    
    public void testUpdateAdminData2() throws InterruptedException {
        JMSConnectionMockupMQ jmsCon = 
            (JMSConnectionMockupMQ) JMSConnectionMockupMQ.getInstance();
        
        GenericMessageListener listener = new GenericMessageListener();
        jmsCon.setListener(Channels.getTheRepos(), listener);
        
        Thread runner = new Thread() {
            public void run() {
                arc.updateAdminChecksum("filename", "checksum");
            }
        };
        runner.start();
        
        synchronized (this) {
            wait(50);
        }
        jmsCon.waitForConcurrentTasksToFinish();
        
        assertEquals("Expected 1 message on TheRepos queue", 
                1, listener.messagesReceived.size());
        NetarkivetMessage msg = listener.messagesReceived.remove(0);
        assertTrue("Expected type of AdminDataMessage but was '"
                + msg.getClass() + "'", msg instanceof AdminDataMessage);
        AdminDataMessage adm = (AdminDataMessage) msg;
        assertTrue("It should be a change checksum admin data message", 
                adm.isChangeChecksum());
        assertFalse("It should not be a change store state admin data message", 
                adm.isChangeStoreState());
    }
    
    /**
     * Check whether it handles a get all checksums call correctly.
     */
    public void testAllChecksums() throws InterruptedException, IOException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
        final File result = new File(WORKING, "all.checksum");
        result.createNewFile();
        JMSConnectionMockupMQ jmsCon = 
            (JMSConnectionMockupMQ) JMSConnectionMockupMQ.getInstance();
        
        GenericMessageListener listener = new GenericMessageListener();
        jmsCon.setListener(Channels.getTheRepos(), listener);
        
        Thread runner = new Thread() {
            public void run() {
                File res = arc.getAllChecksums("ONE");
                FileUtils.moveFile(res, result);
            }
        };
        runner.start();
        
        synchronized (this) {
            wait(50);
        }
        jmsCon.waitForConcurrentTasksToFinish();
        
        // retrieve the message
        assertEquals("Expected 1 message on TheRepos queue", 
                1, listener.messagesReceived.size());
        NetarkivetMessage msg = listener.messagesReceived.remove(0);
        assertTrue("Expected type of GetAllChecksumsMessage but was '"
                + msg.getClass() + "'", msg instanceof GetAllChecksumsMessage);
        GetAllChecksumsMessage gacm = (GetAllChecksumsMessage) msg;
        // give a file to the message and reply 
        //gacm.setFile(ARCFILE);
        Field rf = ReflectUtils.getPrivateField(GetAllChecksumsMessage.class, "rf");
        rf.set(gacm, RemoteFileFactory.getCopyfileInstance(ALL_CHECKSUM_FILE));
        jmsCon.reply(gacm);
        
        synchronized (this) {
            wait(250);
        }
        jmsCon.waitForConcurrentTasksToFinish();
        
        // check whether result is the same.
        String expected = FileUtils.readFile(ALL_CHECKSUM_FILE);
        String received = FileUtils.readFile(result);
        
        assertNotNull("Expected should not be null", expected);
        assertFalse("Expected should not be an empty string.", expected.isEmpty());
        assertNotNull("Received should not be null", received);
        assertFalse("Received should not be an empty string.", received.isEmpty());
        
        assertEquals("Expected and received should have the same content.", 
                expected, received);
    }

    /**
     * Check whether it handles a get all filenames call correctly.
     */
    public void testAllFilenames() throws InterruptedException, IOException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
        final File result = new File(WORKING, "all.filename");
        result.createNewFile();
        JMSConnectionMockupMQ jmsCon = 
            (JMSConnectionMockupMQ) JMSConnectionMockupMQ.getInstance();
        
        GenericMessageListener listener = new GenericMessageListener();
        jmsCon.setListener(Channels.getTheRepos(), listener);
        
        Thread runner = new Thread() {
            public void run() {
                File res = arc.getAllFilenames("ONE");
                FileUtils.moveFile(res, result);
            }
        };
        runner.start();
        
        synchronized (this) {
            wait(50);
        }
        jmsCon.waitForConcurrentTasksToFinish();
        
        // retrieve the message
        assertEquals("Expected 1 message on TheRepos queue", 
                1, listener.messagesReceived.size());
        NetarkivetMessage msg = listener.messagesReceived.remove(0);
        assertTrue("Expected type of GetAllFilenamesMessage but was '"
                + msg.getClass() + "'", msg instanceof GetAllFilenamesMessage);
        GetAllFilenamesMessage gafm = (GetAllFilenamesMessage) msg;
        // give a file to the message and reply 
        //gacm.setFile(ARCFILE);
        Field rf = ReflectUtils.getPrivateField(GetAllFilenamesMessage.class, "remoteFile");
        rf.set(gafm, RemoteFileFactory.getCopyfileInstance(ALL_FILENAME_FILE));
        jmsCon.reply(gafm);
        
        synchronized (this) {
            wait(250);
        }
        jmsCon.waitForConcurrentTasksToFinish();
        
        // check whether result is the same.
        String expected = FileUtils.readFile(ALL_FILENAME_FILE);
        String received = FileUtils.readFile(result);
        
        assertNotNull("Expected should not be null", expected);
        assertFalse("Expected should not be an empty string.", expected.isEmpty());
        assertNotNull("Received should not be null", received);
        assertFalse("Received should not be an empty string.", received.isEmpty());
        
        assertEquals("Expected and received should have the same content.", 
                expected, received);
    }

    public void testGetChecksum() throws InterruptedException, IOException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
        final File result = new File(WORKING, "get.checksum");
        result.createNewFile();
        JMSConnectionMockupMQ jmsCon = 
            (JMSConnectionMockupMQ) JMSConnectionMockupMQ.getInstance();
        
        GenericMessageListener listener = new GenericMessageListener();
        jmsCon.setListener(Channels.getTheRepos(), listener);
        
        Thread runner = new Thread() {
            public void run() {
                String res = arc.getChecksum("ONE", "filename");
                FileUtils.writeBinaryFile(result, res.getBytes());
            }
        };
        runner.start();
        
        synchronized (this) {
            wait(50);
        }
        jmsCon.waitForConcurrentTasksToFinish();
        
        // retrieve the message
        assertEquals("Expected 1 message on TheRepos queue", 
                1, listener.messagesReceived.size());
        NetarkivetMessage msg = listener.messagesReceived.remove(0);
        assertTrue("Expected type of GetChecksumMessage but was '"
                + msg.getClass() + "'", msg instanceof GetChecksumMessage);
        GetChecksumMessage gcm = (GetChecksumMessage) msg;
        // give a file to the message and reply 
        assertEquals("Unexpected filename", "filename", gcm.getArcfileName());
        gcm.setChecksum("checksum");
        jmsCon.reply(gcm);
        
        synchronized (this) {
            wait(250);
        }
        jmsCon.waitForConcurrentTasksToFinish();
        
        String res = FileUtils.readFile(result);
        assertNotNull("The result should not be null", res);
        assertFalse("The result should have content", res.isEmpty());
        
        assertEquals("Unexpected checksum sent back", "checksum", res);
    }


    public void testCorrect() throws InterruptedException, IOException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
        final File result = new File(WORKING, "correct.file");
        result.createNewFile();
        JMSConnectionMockupMQ jmsCon = 
            (JMSConnectionMockupMQ) JMSConnectionMockupMQ.getInstance();
        
        GenericMessageListener listener = new GenericMessageListener();
        jmsCon.setListener(Channels.getTheRepos(), listener);
        
        Thread runner = new Thread() {
            public void run() {
                File res = arc.correct("ONE", "checksum", SEND_CORRECT_FILE, 
                        "credentials");
                FileUtils.copyFile(res, result);
            }
        };
        runner.start();
        
        synchronized (this) {
            wait(100);
        }
        jmsCon.waitForConcurrentTasksToFinish();
        
        // retrieve the message
        assertEquals("Expected 1 message on TheRepos queue", 
                1, listener.messagesReceived.size());
        NetarkivetMessage msg = listener.messagesReceived.remove(0);
        assertTrue("Expected type of CorrectMessage but was '"
                + msg.getClass() + "'", msg instanceof CorrectMessage);
        CorrectMessage cm = (CorrectMessage) msg;
        // give a file to the message and reply 
        assertEquals("Unexpected filename", SEND_CORRECT_FILE.getName(), 
                cm.getArcfileName());
        assertEquals("unexpected credentials.", 
                "credentials", cm.getCredentials());

        File correctFile = new File(FileUtils.getTempDir(), "correct.file");
        cm.getData(correctFile);
        String cfContent = FileUtils.readFile(correctFile);
        String cfExpected = FileUtils.readFile(SEND_CORRECT_FILE);
        
        assertEquals("The correct message should have the expected content", 
                cfExpected, cfContent);
        
        cm.setRemovedFile(RemoteFileFactory.getCopyfileInstance(RES_CORRECT_FILE));
        jmsCon.reply(cm);
        
        synchronized (this) {
            wait(250);
        }
        jmsCon.waitForConcurrentTasksToFinish();
        
        String res = FileUtils.readFile(result);
        String expected = FileUtils.readFile(RES_CORRECT_FILE);
        assertNotNull("The result should not be null", res);
        assertFalse("The result should have content", res.isEmpty());

        assertEquals("Unexpected removed file content", expected, res);
    }

    private static class DummyBatchMessageReplyServer
            implements MessageListener {

        JMSConnection conn = JMSConnectionFactory.getInstance();

        public DummyBatchMessageReplyServer() {
            conn.setListener(Channels.getTheRepos(), this);
        }

        public void close() {
            conn.removeListener(Channels.getTheRepos(), this);
        }

        public void onMessage(Message msg) {
            try {
                BatchMessage bMsg = (BatchMessage) JMSConnection.unpack(msg);
                conn.send(
                        new BatchReplyMessage(bMsg.getReplyTo(), bMsg.getTo(),
                                              bMsg.getID(), 42,
                                              new ArrayList<File>(),
                                              new NullRemoteFile()));
            } catch (IOFailure e) {
                // can't clean up
            }
        }
    }

    private static class DummyStoreMessageReplyServer
            implements MessageListener {

        JMSConnection conn = JMSConnectionFactory.getInstance();
        private int failTimes = 0;
        public int received = 0;

        public void reset() {
            failTimes = 0;
            received = 0;
        }

        public DummyStoreMessageReplyServer() {
            conn.setListener(Channels.getTheRepos(), this);
        }

        public void close() {
            conn.removeListener(Channels.getTheRepos(), this);
        }

        public void onMessage(Message msg) {
            try {
                StoreMessage netMsg = (StoreMessage) JMSConnection.unpack(msg);
                received++;
                if (netMsg.getArcfileName().equals("notok.arc")) {
                    netMsg.setNotOk("received notok.arc message");
                }
                if (netMsg.getArcfileName().startsWith("fail")) {
                    int i = Integer.parseInt(
                            netMsg.getArcfileName().substring(4));
                    if (failTimes < i) {
                        failTimes++;
                        netMsg.setNotOk("received notok.arc message");
                    }
                }
                conn.reply(netMsg);
            } catch (IOFailure e) {
                // can't clean up
            }
        }

    }

    private static class DummyGetMessageReplyServer implements MessageListener {
        JMSConnection conn = JMSConnectionFactory.getInstance();
        private BitarchiveRecord bar;
        public boolean noReply = false;

        public DummyGetMessageReplyServer() {
            conn.setListener(Channels.getTheRepos(), this);
        }

        public void close() {
            conn.removeListener(Channels.getTheRepos(), this);
        }

        public void onMessage(Message msg) {
            if (noReply) {
                return;
            }
            try {
                GetMessage netMsg = (GetMessage) JMSConnection.unpack(msg);

                final Map<String, Object> metadata
                        = new HashMap<String, Object>();
                for (Object aREQUIRED_VERSION_1_HEADER_FIELDS : ARCConstants.REQUIRED_VERSION_1_HEADER_FIELDS) {
                    String field = (String) aREQUIRED_VERSION_1_HEADER_FIELDS;
                    metadata.put(field, "");
                }
                metadata.put(ARCConstants.ABSOLUTE_OFFSET_KEY,
                             0L); // Offset not stored as String but as Long
                byte[] encodedKey = encode(netMsg.getArcFile(),
                                           netMsg.getIndex());
                try {
                    //final ARCRecordMetaData meta = new ARCRecordMetaData(
                    //        new File(netMsg.getArcFile()),metadata);
                    final ARCRecordMetaData meta = new ARCRecordMetaData(
                            netMsg.getArcFile(), metadata);
                    // TODO replace this by something else???? (ARCConstants.LENGTH_HEADER_FIELD_KEY)
                    // does not exist in Heritrix 1.10+
                    //metadata.put(ARCConstants.LENGTH_HEADER_FIELD_KEY,
                    //        "" + encodedKey.length);
                    //Note: ARCRecordMetadata.getLength() now reads the contents of the LENGTH_FIELD_KEY
                    // instead of LENGTH_HEADER_FIELD_KEY
                    metadata.put(ARCConstants.LENGTH_FIELD_KEY,
                                 Integer.toString(encodedKey.length));

//                    setBitarchiveRecord(new BitarchiveRecord(netMsg
//                            .getArcFile(), new ARCRecord(
//                            new ByteArrayInputStream(encodedKey),
//                            meta)));
                    setBitarchiveRecord(new BitarchiveRecord(new ARCRecord(
                            new ByteArrayInputStream(encodedKey),
                            meta)));
                    netMsg.setRecord(bar);


                } catch (IOException e) {
                    throw new Error(e);
                }

                conn.reply(netMsg);
            } catch (IOFailure e) {
                // can't clean up
            }
        }

        public BitarchiveRecord getBitarchiveRecord() {
            return bar;
        }

        public void setBitarchiveRecord(BitarchiveRecord bar) {
            this.bar = bar;
        }

        private byte[] encode(String arcFile, long index) {
            String s = arcFile + " " + index;
            return s.getBytes();
        }

    }

    private static class DummyGetFileMessageReplyServer
            implements MessageListener {
        JMSConnection conn = JMSConnectionFactory.getInstance();
        public boolean noReply = false;
        private File dir;

        public DummyGetFileMessageReplyServer(File dir) {
            this.dir = dir;
            conn.setListener(Channels.getTheRepos(), this);
        }

        public void close() {
            conn.removeListener(Channels.getTheRepos(), this);
        }

        public void onMessage(Message msg) {
            if (noReply) {
                return;
            }
            GetFileMessage netMsg = (GetFileMessage) JMSConnection.unpack(msg);
            try {

                File arcFile = new File(dir, netMsg.getArcfileName());
                netMsg.setFile(arcFile);
            } catch (IOFailure e) {
                netMsg.setNotOk(
                        "Couldn't find arcfile " + netMsg.getArcfileName());
            } catch (ArgumentNotValid e) {
                netMsg.setNotOk(
                        "Couldn't find arcfile " + netMsg.getArcfileName());
            }
            conn.reply(netMsg);
        }
    }

    /**
     * A generic message listener class which just stores a list of all messages
     * it receives
     */
    public static class GenericMessageListener implements MessageListener {
        public ArrayList<NetarkivetMessage> messagesReceived =
                new ArrayList<NetarkivetMessage>();

        public void onMessage(Message message) {
            try {
                NetarkivetMessage naMsg =
                        (NetarkivetMessage)
                                ((ObjectMessage) message).getObject();
                messagesReceived.add(naMsg);
            } catch (JMSException e) {
                throw new IOFailure("JMSError: ", e);
            }
        }
    }

}
