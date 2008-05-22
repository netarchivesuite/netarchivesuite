/* File:             $Id$
 * Revision:         $Revision$
 * Author:           $Author$
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
package dk.netarkivet.archive.arcrepository;

import javax.jms.Message;
import javax.jms.MessageListener;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import junit.framework.TestCase;

import dk.netarkivet.archive.bitarchive.distribute.BitarchiveServer;
import dk.netarkivet.archive.bitarchive.distribute.GetFileMessage;
import dk.netarkivet.archive.bitarchive.distribute.RemoveAndGetFileMessage;
import dk.netarkivet.common.Settings;
import dk.netarkivet.common.distribute.Channels;
import dk.netarkivet.common.distribute.ChannelsTester;
import dk.netarkivet.common.distribute.JMSConnection;
import dk.netarkivet.common.distribute.JMSConnectionFactory;
import dk.netarkivet.common.distribute.JMSConnectionTestMQ;
import dk.netarkivet.common.distribute.TestRemoteFile;
import dk.netarkivet.common.distribute.arcrepository.ArcRepositoryClientFactory;
import dk.netarkivet.common.distribute.arcrepository.BitarchiveRecord;
import dk.netarkivet.common.distribute.arcrepository.Location;
import dk.netarkivet.common.distribute.arcrepository.PreservationArcRepositoryClient;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.MD5;
import dk.netarkivet.common.utils.RememberNotifications;
import dk.netarkivet.testutils.TestFileUtils;
import dk.netarkivet.testutils.TestUtils;
import dk.netarkivet.testutils.preconfigured.UseTestRemoteFile;

/**
 * This class tests the Controller's get() method.
 */
public class ArcRepositoryTesterGet extends TestCase {
    private UseTestRemoteFile rf = new UseTestRemoteFile();

    /**
     * The test log directories for Controller and AdminData.
     */
    private static final File TEST_DIR = new File(
            "tests/dk/netarkivet/archive/arcrepository/data/get");

    private static final File ORIGINALS_DIR = new File(TEST_DIR, "originals");

    private static final File WORKING_DIR = new File(TEST_DIR, "working");

    private static final File BITARCHIVE_DIR = new File(WORKING_DIR,
            "bitarchive1");

    private static final File SERVER_DIR = new File(WORKING_DIR, "server1");

    /**
     * The test log directories for Controller and AdminData.
     */
    private static final File CLOG_DIR = new File(WORKING_DIR, "log/controller");

    private static final File ALOG_DIR = new File(WORKING_DIR, "log/admindata");

    public final File TESTLOGPROP = new File("tests/dk/netarkivet/testlog.prop");

    /**
     * List of files that can be used in the scripts (content of the
     * ORIGINALS_DIR)
     */
    private static final List<String> GETTABLE_FILES
    	= Arrays.asList(new String[] {"get1.ARC", "get2.ARC" });

    /** A bitarchive server to communicate with. */
    BitarchiveServer bitArchiveServer;

    /**
     * An ArcRepository that will mediate communication between this class and
     * the bitarchiver server.
     */
    ArcRepository arcRepository;

    /** A client for communicating with the ArcRepository. */
    PreservationArcRepositoryClient client;

    /**
     * Set up the test.
     */
    protected void setUp() {
        Settings.reload();
        ChannelsTester.resetChannels();
        JMSConnectionTestMQ.useJMSConnectionTestMQ();
        Settings.set(Settings.ENVIRONMENT_LOCATION_NAMES, "SB");
        Settings.set(Settings.ENVIRONMENT_THIS_LOCATION, "SB");
        ChannelsTester.resetChannels();

        rf.setUp();

        FileUtils.removeRecursively(WORKING_DIR);
        TestFileUtils.copyDirectoryNonCVS(ORIGINALS_DIR, WORKING_DIR);
        FileUtils.createDir(CLOG_DIR);
        FileUtils.createDir(ALOG_DIR);

        Settings.set(Settings.DIRS_ARCREPOSITORY_ADMIN, ALOG_DIR
                .getAbsolutePath());

        Settings.set(Settings.BITARCHIVE_SERVER_FILEDIR, BITARCHIVE_DIR
                .getAbsolutePath());
        Settings.set(Settings.DIR_COMMONTEMPDIR, SERVER_DIR
                .getAbsolutePath());
        bitArchiveServer = BitarchiveServer.getInstance();
        arcRepository = ArcRepository.getInstance();
        Settings.set(Settings.ARCREPOSITORY_GET_TIMEOUT, "1000");
        client = ArcRepositoryClientFactory.getPreservationInstance();
        Settings.set(Settings.NOTIFICATIONS_CLASS,
                     RememberNotifications.class.getName());
    }

    /**
     * Close all servers and clients, reset settings.
     */
    protected void tearDown() {
        client.close();
        arcRepository.close();
        bitArchiveServer.close();
        FileUtils.removeRecursively(WORKING_DIR);
        JMSConnectionTestMQ.clearTestQueues();
        Settings.reload();
        rf.tearDown();
        RememberNotifications.resetSingleton();
    }

    /**
     * This tests the get()-method for a non-existing-file.
     */
    public void testGetNonExistingFile() {
        BitarchiveRecord bar = client.get("nosuchfile.arc", (long) 0);
        assertNull("Should have retrieved null, not " + bar, bar);
    }

    /**
     * this tests the get()-method for an existing file.
     */
    public void testGetExistingFile() {
        BitarchiveRecord bar = client.get((String) GETTABLE_FILES.get(1),
                (long) 0);

        // not really much of a test as we just check for no exception
        // better tests follow later

        if (null == bar) {
            fail("Nullpointer should not be returned for existing file");
        }
    }

    /**
     * this tests get get()-method for an existing file - getting get File-name
     * out of the BitarchiveRecord.
     */
    public void testGetFile() throws IOException {
        arcRepository.close();
        DummyGetFileMessageReplyServer dServer
        	= new DummyGetFileMessageReplyServer();
        File result = new File(FileUtils.createUniqueTempDir(
        		WORKING_DIR, "testGetFile"), (String) GETTABLE_FILES.get(1));
        Location location = Location.get(Settings
                .get(Settings.ENVIRONMENT_THIS_LOCATION));
        client.getFile(GETTABLE_FILES.get(1), location, result);
        byte[] buffer = FileUtils.readBinaryFile(result);
        ((JMSConnectionTestMQ) JMSConnectionFactory.getInstance())
                .waitForConcurrentTasksToFinish();
        assertNotNull("Buffer should not be null", buffer);
        byte targetbuffer[] = FileUtils.readBinaryFile(new File(new File(
                BITARCHIVE_DIR, "filedir"), (String) GETTABLE_FILES.get(1)));
        assertTrue("Received data and uploaded must be equal", Arrays
                .equals(buffer, targetbuffer));
    }

    /**
     * this tests get get()-method for an existing file - getting get File-name
     * out of the BitarchiveRecord.
     */
    public void testRemoveAndGetFile() throws IOException {
        arcRepository.close();
        client.close();
        client = ArcRepositoryClientFactory.getPreservationInstance();
        new DummyRemoveAndGetFileMessageReplyServer();
        final File bitarchiveFiledir = new File(
        		Settings.get(Settings.BITARCHIVE_SERVER_FILEDIR),
        		"filedir");
        client.removeAndGetFile((String) GETTABLE_FILES.get(1),
                              Settings.get(Settings.ENVIRONMENT_THIS_LOCATION),
                                "42",
                                MD5.generateMD5onFile(
                                		new File(bitarchiveFiledir,
                                        (String) GETTABLE_FILES.get(1)))
                                        );
        File copyOfFile = new File(FileUtils.getTempDir(),
                                   (String) GETTABLE_FILES.get(1));
        assertTrue("Must have copied file to commontempdir",
                   copyOfFile.exists());

        byte[] buffer = FileUtils.readBinaryFile(copyOfFile);
        ((JMSConnectionTestMQ) JMSConnectionFactory.getInstance())
                .waitForConcurrentTasksToFinish();
        assertNotNull("Buffer should not be null", buffer);
        byte targetbuffer[] = FileUtils.readBinaryFile(new File(new File(
                BITARCHIVE_DIR, "filedir"), (String) GETTABLE_FILES.get(1)));
        assertTrue("Received data and uploaded must be equal", Arrays
                .equals(buffer, targetbuffer));

        assertEquals("Should have no remote files left on the server",
                     0, TestRemoteFile.remainingFiles().size());
    }

    /**
     * this tests the getting of actual data (assuming that the length is not
     * null) is the length of getData() > 0 the next test checks the first 55
     * chars !
     */
    public void testGetData() {
        BitarchiveRecord bar = client.get((String) GETTABLE_FILES.get(1),
                (long) 0);

        if (bar.getLength() == 0L) {
            fail("No data in BitarchiveRecord");
        } else {
            // BitarchiveRecord.getData() now returns a InputStream 
        	// instead of a byte[]
            String data = new String(TestUtils.inputStreamToBytes(bar.getData(),
            		(int) bar.getLength())).substring(0, 55);
            assertEquals("First 55 chars of data should be correct", data,
                    "<?xml version=\"1.0\" "
            		+ "encoding=\"UTF-8\" standalone=\"yes\"?>");
        }
    }

    /**
     * Test for index out of bounds.
     */
    public void testGetIndexOutOfBounds() {
        try {
            BitarchiveRecord bar = client.get((String) GETTABLE_FILES.get(1),
                    (long) 50000000);
            fail("Index out of bounds should throw exception, but " +
                    "gave " + bar);
        } catch (Exception e) {
            //expected
        }
    }

    /**
     * Test for index not pointing on ARC-record.
     */
    public void testGetIllegalIndex() {
        try {
            BitarchiveRecord bar = client.get((String) GETTABLE_FILES.get(1),
                    (long) 5000);
            fail("Illegal index should return null, not given " + bar);
        } catch (Exception e) {
            //expected
        }
    }

    public static class DummyGetFileMessageReplyServer implements
            MessageListener {

        JMSConnection conn = JMSConnectionFactory.getInstance();

        public DummyGetFileMessageReplyServer() {
            conn.setListener(Channels.getTheArcrepos(), this);
        }

        public void close() {
            conn.removeListener(Channels.getTheArcrepos(), this);
        }

        public void onMessage(Message msg) {
            GetFileMessage netMsg = (GetFileMessage) JMSConnection
                    .unpack(msg);
            netMsg.setFile(new File(new File(BITARCHIVE_DIR, "filedir"),
                    (String) GETTABLE_FILES.get(1)));
            conn.reply(netMsg);
        }
    }

    public static class DummyRemoveAndGetFileMessageReplyServer implements
            MessageListener {

        JMSConnection conn = JMSConnectionFactory.getInstance();

        public DummyRemoveAndGetFileMessageReplyServer() {
            conn.setListener(Channels.getTheArcrepos(), this);
        }

        public void close() {
            conn.removeListener(Channels.getTheArcrepos(), this);
        }

        public void onMessage(Message msg) {
            RemoveAndGetFileMessage netMsg 
            	= (RemoveAndGetFileMessage) JMSConnection.unpack(msg);
            netMsg.setFile(new File(new File(BITARCHIVE_DIR, "filedir"),
                    (String) GETTABLE_FILES.get(1)));
            conn.reply(netMsg);
        }
    }

}
