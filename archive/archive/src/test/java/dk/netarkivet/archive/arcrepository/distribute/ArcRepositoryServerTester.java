/*$Id$
 * $Revision$
 * $Date$
 * $Author$
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
package dk.netarkivet.archive.arcrepository.distribute;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import junit.framework.TestCase;

import dk.netarkivet.archive.ArchiveSettings;
import dk.netarkivet.archive.arcrepository.ArcRepository;
import dk.netarkivet.archive.bitarchive.distribute.BatchMessage;
import dk.netarkivet.archive.bitarchive.distribute.GetMessage;
import dk.netarkivet.archive.bitarchive.distribute.TestBatchJobRuns;
import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.distribute.Channels;
import dk.netarkivet.common.distribute.JMSConnection;
import dk.netarkivet.common.distribute.JMSConnectionFactory;
import dk.netarkivet.common.distribute.JMSConnectionMockupMQ;
import dk.netarkivet.common.distribute.JMSConnectionTester;
import dk.netarkivet.common.distribute.JMSConnectionTester.DummyServer;
import dk.netarkivet.common.distribute.NullRemoteFile;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.testutils.TestFileUtils;
import dk.netarkivet.testutils.TestMessageListener;
import dk.netarkivet.testutils.preconfigured.ReloadSettings;

/**
 * Unit tests for the class ArcRepositoryServer.
 */
public class ArcRepositoryServerTester extends TestCase {
    /**
     * The test log directories for Controller and AdminData.
     */
    private static final File TEST_DIR = new File(
            "tests/dk/netarkivet/archive/arcrepository/data/get");

    private static final File ORIGINALS_DIR = new File(TEST_DIR, "originals");

    private static final File WORKING_DIR = new File(TEST_DIR, "working");

    private static final File BITARCHIVE_DIR = new File(WORKING_DIR,
                                                        "bitarchive1");

    /**
     * The test log directories for Controller and AdminData.
     */
    private static final File CLOG_DIR = new File(WORKING_DIR,
                                                  "log/controller");

    private static final File ALOG_DIR = new File(WORKING_DIR, "log/admindata");

    /**
     *
     */
    public final File TESTLOGPROP = new File(
            "tests/dk/netarkivet/testlog.prop");

    /**
     * The files that are uploaded during the tests and that must be removed
     * afterwards.
     */
    private static final List<String> STORABLE_FILES = Arrays.asList(
            new String[]{"get1.ARC", "get2.ARC"});

    private File file;

    private JMSConnection con;

    private JMSConnectionTester.DummyServer dummyServer;

    ReloadSettings rs = new ReloadSettings();

    protected void setUp() throws Exception {
        rs.setUp();
        Settings.set(CommonSettings.REMOTE_FILE_CLASS,
                     "dk.netarkivet.common.distribute.TestRemoteFile");
        JMSConnectionMockupMQ.useJMSConnectionMockupMQ();

        FileUtils.removeRecursively(WORKING_DIR);
        TestFileUtils.copyDirectoryNonCVS(ORIGINALS_DIR, WORKING_DIR);
        FileUtils.createDir(CLOG_DIR);
        FileUtils.createDir(ALOG_DIR);

        Settings.set(ArchiveSettings.DIRS_ARCREPOSITORY_ADMIN, ALOG_DIR
                .getAbsolutePath());
        con = JMSConnectionFactory.getInstance();
        dummyServer = new JMSConnectionTester.DummyServer();
        con.setListener(Channels.getError(), dummyServer);
    }

    protected void tearDown() throws Exception {
        FileUtils.removeRecursively(WORKING_DIR);
        con.removeListener(Channels.getError(), dummyServer);
        rs.tearDown();
    }

    /**
     * Test visit() StoreMessage methods arguments.
     */
    public void testVisitStoreMessageArgumentsNotNull() {
        file = new File(new File(BITARCHIVE_DIR, "filedir"),
                        STORABLE_FILES.get(0).toString());

        ArcRepository arc = ArcRepository.getInstance();

        /**
         * Test if ArgumentNotValid is thrown if null is given as parameter
         */
        try {
            new ArcRepositoryServer(arc).visit((StoreMessage) null);
            fail("Should throw ArgumentNotValid exception");
        } catch (ArgumentNotValid e) {
            // Expected
        }
        arc.close();
    }

    /**
     * Test visit() BatchMessage methods arguments.
     */
    public void testVisitBatchMessageArgumentsNotNull() {
        ArcRepository arc = ArcRepository.getInstance();

        /**
         * Test if ArgumentNotValid is thrown if null is given as parameter
         * parameter
         */
        try {
            new ArcRepositoryServer(arc).visit((BatchMessage) null);
            fail("Should throw ArgumentNotValid exception");
        } catch (ArgumentNotValid e) {
            // Expected
        }
        arc.close();

    }

    /**
     * Test a BatchMessage is sent to the_bamon queue.
     */
    public void testVisitBatchMessage() {
        // Create dummy server and listen on the TheArcrepos queue
        DummyServer serverTheBamonQueue = new DummyServer();
        serverTheBamonQueue.reset();
        con.setListener(Channels.getTheBamon(), serverTheBamonQueue);

        ArcRepository arc = ArcRepository.getInstance();
        BatchMessage msg = new BatchMessage(Channels.getTheBamon(), Channels
                .getError(), new TestBatchJobRuns(), Settings.get(
                CommonSettings.USE_REPLICA_ID));

        new ArcRepositoryServer(arc).visit(msg);

        ((JMSConnectionMockupMQ) con).waitForConcurrentTasksToFinish();

        assertEquals("Server should have received 1 message", 1,
                     serverTheBamonQueue.msgReceived);
        arc.close();
    }

    /**
     * Test message is sent and returned, and set "Not OK" if an error occurs.
     */
    public void testStoreNoSuchFile() {
        file = new File(BITARCHIVE_DIR, "NO_SUCH_FILE");
        ArcRepository arc = ArcRepository.getInstance();
        try {
            new StoreMessage(Channels.getError(), file);
            fail("Should get error making a storemessage with "
                    + "non-existing file");
        } catch (ArgumentNotValid e) {
            //expected
        }

        Settings.set(CommonSettings.REMOTE_FILE_CLASS, NullRemoteFile.class.getName());

        file = new File(new File(BITARCHIVE_DIR, "filedir"),
                        STORABLE_FILES.get(0).toString());
        StoreMessage msg = new StoreMessage(Channels.getError(), file);
        JMSConnectionMockupMQ.updateMsgID(msg, "store1");

        dummyServer.reset();

        new ArcRepositoryServer(arc).visit(msg);

        ((JMSConnectionMockupMQ) JMSConnectionMockupMQ.getInstance())
                .waitForConcurrentTasksToFinish();

        assertFalse("Message should have been tagged NotOK", msg.isOk());
        assertEquals("Server should have received 1 incorrect message",
                     dummyServer.msgNotOK, 1);
        arc.close();
    }

    /**
     * Test message is sent and returned, and set "OK" if no errors occurs.
     */
    public void testStore() {
        file = new File(new File(BITARCHIVE_DIR, "filedir"),
                        STORABLE_FILES.get(0).toString());
        ArcRepository arc = ArcRepository.getInstance();
        StoreMessage msg = new StoreMessage(Channels.getError(), file);
        JMSConnectionMockupMQ.updateMsgID(msg, "store1");

        new ArcRepositoryServer(arc).visit(msg);
        assertTrue("Message should have been tagged OK", msg.isOk());
        arc.close();
    }

    /**
     * Test message is resent.
     */
    public void testGet() {
        file = new File(BITARCHIVE_DIR, STORABLE_FILES.get(0).toString());
        GetMessage msg = new GetMessage(Channels.getTheRepos(), Channels
                .getError(), "", 0);
        JMSConnectionMockupMQ testCon = (JMSConnectionMockupMQ) JMSConnectionMockupMQ
                .getInstance();
        TestMessageListener listener = new TestMessageListener();
        testCon.setListener(Channels.getAllBa(), listener);
        new ArcRepositoryServer(ArcRepository.getInstance()).visit(msg);
        testCon.waitForConcurrentTasksToFinish();
        assertEquals("Message should have been sent to the bitarchive queue",
                     1, listener.getNumReceived());
    }
}