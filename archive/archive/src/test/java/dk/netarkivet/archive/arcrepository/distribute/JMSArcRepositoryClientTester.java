/*$Id$
* $Revision$
* $Date$
* $Author$
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
import org.archive.io.arc.ARCConstants;
import org.archive.io.arc.ARCRecord;
import org.archive.io.arc.ARCRecordMetaData;

import dk.netarkivet.archive.bitarchive.distribute.BatchMessage;
import dk.netarkivet.archive.bitarchive.distribute.BatchReplyMessage;
import dk.netarkivet.archive.bitarchive.distribute.GetFileMessage;
import dk.netarkivet.archive.bitarchive.distribute.GetMessage;
import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.distribute.ChannelID;
import dk.netarkivet.common.distribute.Channels;
import dk.netarkivet.common.distribute.JMSConnection;
import dk.netarkivet.common.distribute.JMSConnectionFactory;
import dk.netarkivet.common.distribute.JMSConnectionTestMQ;
import dk.netarkivet.common.distribute.NetarkivetMessage;
import dk.netarkivet.common.distribute.NullRemoteFile;
import dk.netarkivet.common.distribute.RemoteFile;
import dk.netarkivet.common.distribute.Synchronizer;
import dk.netarkivet.common.distribute.TestRemoteFile;
import dk.netarkivet.common.distribute.arcrepository.ArcRepositoryClientFactory;
import dk.netarkivet.common.distribute.arcrepository.BatchStatus;
import dk.netarkivet.common.distribute.arcrepository.BitarchiveRecord;
import dk.netarkivet.common.distribute.arcrepository.HarvesterArcRepositoryClient;
import dk.netarkivet.common.distribute.arcrepository.Replica;
import dk.netarkivet.common.distribute.arcrepository.PreservationArcRepositoryClient;
import dk.netarkivet.common.distribute.arcrepository.ViewerArcRepositoryClient;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.NotificationsFactory;
import dk.netarkivet.common.utils.RememberNotifications;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.utils.batch.FileBatchJob;
import dk.netarkivet.testutils.CollectionAsserts;
import dk.netarkivet.testutils.ReflectUtils;
import dk.netarkivet.testutils.StringAsserts;
import dk.netarkivet.testutils.TestFileUtils;
import dk.netarkivet.testutils.TestUtils;
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


    JMSArcRepositoryClient arc;
    private JMSArcRepositoryClient arcrepos;
    private UseTestRemoteFile utrf = new UseTestRemoteFile();
    ReloadSettings rs = new ReloadSettings();

    protected void setUp() throws Exception {
        rs.setUp();
        FileUtils.removeRecursively(WORKING);
        TestFileUtils.copyDirectoryNonCVS(ORIGINALS, WORKING);
        JMSConnectionTestMQ.useJMSConnectionTestMQ();
        utrf.setUp();
        Settings.set(CommonSettings.NOTIFICATIONS_CLASS, RememberNotifications.class.getName());
        Settings.set(JMSArcRepositoryClient.ARCREPOSITORY_GET_TIMEOUT, "1000");
        arc = (JMSArcRepositoryClient) ArcRepositoryClientFactory.getPreservationInstance();

    }

    protected void tearDown() throws Exception {
        if (arc != null) {
            arc.close();
        }
        if (arcrepos != null) {
            arcrepos.close();
        }
        utrf.tearDown();
        FileUtils.removeRecursively(WORKING);
        RememberNotifications.resetSingleton();
        rs.tearDown();
    }

    /**
     * Verify that the JMSArcRepositoryClient class has no public constructor.
     */
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

    /**
     * Tests the correct object is returned when getViewerInstance is called.
     */
    public void testGetViewerInstance() {
        assertTrue("Must return an instance of ViewerArcRepositoryClient",
                   (arcrepos
                           = (JMSArcRepositoryClient) ArcRepositoryClientFactory.getViewerInstance()) instanceof
                           ViewerArcRepositoryClient);
    }

    /**
     * Tests the correct object is returned when getHacoInstance is called.
     */
    public void testGetHacoInstance() {
        assertTrue("Must return an instance of HarvesterArcRepositoryClient",
                   ArcRepositoryClientFactory.getHarvesterInstance() instanceof
                           HarvesterArcRepositoryClient);
    }

    /**
     * Test get() methods arguments.
     */
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
        byte[] contents = TestUtils.inputStreamToBytes(theData,
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
     * @throws IOException
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

    /**
     * This tests the get()-method when it times out waiting for a reply.
     */
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
     * @throws IOException
     */
    public void testStoreMessageNotOK() throws IOException {
        DummyStoreMessageReplyServer replyServer
                = new DummyStoreMessageReplyServer();
        PreservationArcRepositoryClient arc
                = ArcRepositoryClientFactory.getPreservationInstance();
        File f = new File(WORKING, "notok.arc");
        f.createNewFile();
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
    }

    /**
     * tests that a successful store reply will result in the RemoteFile being
     * deleted.
     *
     * @throws InterruptedException
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
        ((JMSConnectionTestMQ) con).waitForConcurrentTasksToFinish();
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

    /**
     * Test batch() methods arguments.
     */
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


    /**
     * Test JMSArcRepositoryClient.batch is distributed.
     */
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

    public void testStoreRetries() throws IOException {
        DummyStoreMessageReplyServer ar = new DummyStoreMessageReplyServer();

        File fail1 = new File(WORKING, "fail1");
        fail1.createNewFile();
        File fail2 = new File(WORKING, "fail2");
        fail2.createNewFile();
        File fail3 = new File(WORKING, "fail3");
        fail3.createNewFile();
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
        arc = (JMSArcRepositoryClient) ArcRepositoryClientFactory.getHarvesterInstance();
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
            wait(3000);
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
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
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
        JMSConnectionTestMQ testMQ = JMSConnectionTestMQ.getInstance();
        List<MessageListener> listeners = testMQ.getListeners(replyQ);
        testMQ.removeListener(replyQ, listeners.get(0));
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

    /** Test that remote files are cleaned up after exceptions. Bug #1080 */
    public void testStoreFailed()
            throws NoSuchFieldException, IllegalAccessException {
        // Set Synchronizers request field to null to get an appropriately
        // late exception.
        Field requests = ReflectUtils.getPrivateField(Synchronizer.class, "requests");
        requests.set(arc, null);
        try {
            arc.store(ARCFILE);
            fail("Should have an IOFailure after forcing internal exception");
        } catch (NullPointerException e) {
            // Expected to happen
        }
        CollectionAsserts.assertListEquals(
                "Should have no remaining files after exception",
                new ArrayList<RemoteFile>(TestRemoteFile.remainingFiles()));
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
                conn.reply(
                        new BatchReplyMessage(bMsg.getTo(), bMsg.getReplyTo(),
                                              bMsg.getID(), 42,
                                              new ArrayList<File>(),
                                              new NullRemoteFile()));
            } catch (IOFailure e) {
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
                metadata.put(ARCConstants.ABSOLUTE_OFFSET_KEY, new Long(
                        0L)); // Offset not stored as String but as Long
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
