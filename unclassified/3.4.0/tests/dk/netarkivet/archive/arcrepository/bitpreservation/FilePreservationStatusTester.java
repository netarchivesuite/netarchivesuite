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
package dk.netarkivet.archive.arcrepository.bitpreservation;

/**
 * Tests of file preservation.
 */

import javax.jms.Message;
import javax.jms.MessageListener;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import dk.netarkivet.archive.arcrepositoryadmin.ArcRepositoryEntry;
import dk.netarkivet.archive.arcrepositoryadmin.MyArcRepositoryEntry;
import dk.netarkivet.archive.bitarchive.distribute.BatchMessage;
import dk.netarkivet.archive.bitarchive.distribute.BatchReplyMessage;
import dk.netarkivet.common.Settings;
import dk.netarkivet.common.distribute.Channels;
import dk.netarkivet.common.distribute.JMSConnection;
import dk.netarkivet.common.distribute.JMSConnectionFactory;
import dk.netarkivet.common.distribute.JMSConnectionTestMQ;
import dk.netarkivet.common.distribute.StringRemoteFile;
import dk.netarkivet.common.distribute.arcrepository.ArcRepositoryClientFactory;
import dk.netarkivet.common.distribute.arcrepository.Location;
import dk.netarkivet.common.distribute.arcrepository.PreservationArcRepositoryClient;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.testutils.CollectionUtils;
import dk.netarkivet.testutils.FileAsserts;
import dk.netarkivet.testutils.LogUtils;
import dk.netarkivet.testutils.preconfigured.UseTestRemoteFile;

public class FilePreservationStatusTester extends TestCase {
    private UseTestRemoteFile rf = new UseTestRemoteFile();

    FilePreservationStatus filePreservationStatus;
    private static final Location SB = Location.get("SB");
    private static final Location KB = Location.get("KB");
    private PreservationArcRepositoryClient arc;
    // Dummy value to test, that constructor for FilePreservationStatus
    // does not allow null arguments, so the null second argument to DummyFPS is replaced by
    // fooAdmindatum.
    private ArcRepositoryEntry fooAdmindatum = new MyArcRepositoryEntry("filename", "md5", null);


    protected void setUp() throws Exception {
        JMSConnectionTestMQ.useJMSConnectionTestMQ();
        rf.setUp();
        arc = ArcRepositoryClientFactory.getPreservationInstance();
    }

    protected void tearDown() throws Exception {
        arc.close();
        rf.tearDown();
        Settings.reload();
    }

    public void testGetChecksumMap() throws Exception {
        DummyFPS.arcrepresults.put(SB, CollectionUtils.list("string1"));
        DummyFPS.arcrepresults.put(KB, CollectionUtils.list("string2"));
        FilePreservationStatus fps = new DummyFPS("fooname", fooAdmindatum);
        assertEquals("Should have string1 for SB",
                "string1", fps.getBitarchiveChecksum(SB).get(0));
        assertEquals("Should have string2 for KB",
                "string2", fps.getBitarchiveChecksum(KB).get(0));

        DummyFPS.arcrepresults.put(SB, CollectionUtils.list("string3"));
        DummyFPS.arcrepresults.remove(KB);
        fps = new DummyFPS("foobar", fooAdmindatum);
        assertEquals("Should have string3 for SB",
                "string3", fps.getBitarchiveChecksum(SB).get(0));
        assertEquals("Should have illegal string2 for KB",
                0, fps.getBitarchiveChecksum(KB).size());
    }

    public void testGetBitarchiveChecksum() throws Exception {
        DummyBatchMessageReplyServer replyServer = new DummyBatchMessageReplyServer();

        // Test standard case
        replyServer.batchResult.put(SB, "foobar##md5-1");
        replyServer.batchResult.put(KB, "foobar##md5-2");
        FilePreservationStatus fps = new FilePreservationStatus("foobar", fooAdmindatum);
        assertEquals("Should have expected number of keys",
                2, fps.getChecksumMap().size());
        assertEquals("Should have expected size for SB",
                1, fps.getChecksumMap().get(SB).size());
        assertEquals("Should have expected value for SB",
                "md5-1", fps.getChecksumMap().get(SB).get(0));
        assertEquals("Should have expected size for KB",
                1, fps.getChecksumMap().get(KB).size());
        assertEquals("Should have expected value for KB",
                "md5-2", fps.getChecksumMap().get(KB).get(0));

        // Test fewer checksums
        replyServer.batchResult.clear();
        replyServer.batchResult.put(SB, "");
        fps = new FilePreservationStatus("foobar", fooAdmindatum);
        assertEquals("Should have expected number of keys",
                2, fps.getChecksumMap().size());
        assertEquals("Should have expected size for SB",
                0, fps.getChecksumMap().get(SB).size());
        assertEquals("Should have expected size for KB",
                0, fps.getChecksumMap().get(KB).size());
        LogUtils.flushLogs(getClass().getName());
        FileAsserts.assertFileNotContains("Should have no warning about SB",
                TestInfo.LOG_FILE, "while asking Location SB");
        FileAsserts.assertFileNotContains("Should have no warning about KB",
                TestInfo.LOG_FILE, "while asking Location KB");

        // Test malformed checksums
        replyServer.batchResult.clear();
        replyServer.batchResult.put(SB, "foobar#klaf");
        replyServer.batchResult.put(KB, "foobarf##klaff");
        fps = new FilePreservationStatus("foobar", fooAdmindatum);
        assertEquals("Should have expected number of keys",
                2, fps.getChecksumMap().size());
        assertEquals("Should have expected size for SB",
                0, fps.getChecksumMap().get(SB).size());
        assertEquals("Should have expected size for KB",
                0, fps.getChecksumMap().get(KB).size());
        LogUtils.flushLogs(getClass().getName());
        FileAsserts.assertFileContains("Should have warning about SB",
                "while asking Location SB", TestInfo.LOG_FILE);
        FileAsserts.assertFileContains("Should have warning about KB",
                "while asking Location KB", TestInfo.LOG_FILE);

        // Test extra checksums
        replyServer.batchResult.clear();
        replyServer.batchResult.put(SB, "barfu#klaf\nbarfu##klyf\nbarfu##knof");
        replyServer.batchResult.put(KB, "barfuf##klaff\nbarfu##klof\nbarfu##klof\nbarfu##klof");
        fps = new FilePreservationStatus("barfu", fooAdmindatum);
        assertEquals("Should have expected number of keys",
                2, fps.getChecksumMap().size());
        assertEquals("Should have expected size for SB",
                2, fps.getChecksumMap().get(SB).size());
        assertEquals("Should have expected size for KB",
                3, fps.getChecksumMap().get(KB).size());
        LogUtils.flushLogs(getClass().getName());
        FileAsserts.assertFileContains("Should have warning about SB",
                "while asking Location SB", TestInfo.LOG_FILE);
        FileAsserts.assertFileContains("Should have warning about KB",
                "while asking Location KB", TestInfo.LOG_FILE);

        // TODO: More funny cases
    }


    private static class DummyBatchMessageReplyServer implements MessageListener {
        public Map<Location, String> batchResult = new HashMap<Location, String>();
        JMSConnection conn = JMSConnectionFactory.getInstance();

        public DummyBatchMessageReplyServer() {
            conn.setListener(Channels.getTheArcrepos(), this);
        }

        public void close() {
            conn.removeListener(Channels.getTheArcrepos(), this);
        }

        public void onMessage(Message msg) {
            try {
                BatchMessage bMsg = (BatchMessage) JMSConnection.unpack(msg);
                String res = batchResult.get(Location.get(bMsg.getLocationName()));
                if (res != null) {
                    conn.reply(new BatchReplyMessage(bMsg.getTo(), bMsg.getReplyTo(),
                            bMsg.getID(), res.split("\n").length, new ArrayList<File>(),
                            new StringRemoteFile(res)));
                } else {
                    conn.reply(new BatchReplyMessage(bMsg.getTo(), bMsg.getReplyTo(),
                            bMsg.getID(), 0, new ArrayList<File>(), null));
                }
            } catch (IOFailure e) {}
        }
    }

    public void testGetChecksums() {
        DummyBatchMessageReplyServer replyServer = new DummyBatchMessageReplyServer();

        // Test standard case
        replyServer.batchResult.put(SB, "foobar##md5-1");
        replyServer.batchResult.put(KB, "foobar##md5-2");
        FilePreservationStatus fps = new FilePreservationStatus("foobar", fooAdmindatum);
        List<String> checksums = fps.getChecksums(Location.get("KB"));
        assertEquals("Should have one checksum for known file",
                1, checksums.size());
        assertEquals("Should have the right checksum",
                "md5-2", checksums.get(0));

        replyServer.batchResult.clear();
        fps = new FilePreservationStatus("fobar", fooAdmindatum);
        checksums = fps.getChecksums(Location.get("KB"));
        assertEquals("Should have no checksums for unknown file",
                0, checksums.size());
    }

    /** This is a subclass that overrides the getChecksum method to avoid
     * calling batch().
     */
    static class DummyFPS extends FilePreservationStatus {
        static Map<Location, List<String>> arcrepresults =
                new HashMap<Location, List<String>>();
        public DummyFPS(String filename, ArcRepositoryEntry entry) {
            super(filename, entry);
        }

        public List<String> getChecksums(Location ba) {
            if (arcrepresults.containsKey(ba)) {
                return arcrepresults.get(ba);
            } else {
                return CollectionUtils.list();
            }
        }
    }
}