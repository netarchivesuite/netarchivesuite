/* $Id$
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
package dk.netarkivet.archive.indexserver.distribute;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import junit.framework.TestCase;

import dk.netarkivet.archive.distribute.ArchiveMessageHandler;
import dk.netarkivet.common.distribute.Channels;
import dk.netarkivet.common.distribute.TestRemoteFile;
import dk.netarkivet.common.distribute.RemoteFile;
import dk.netarkivet.common.distribute.indexserver.RequestType;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IllegalState;
import dk.netarkivet.common.exceptions.PermissionDenied;
import dk.netarkivet.common.utils.StringUtils;
import dk.netarkivet.testutils.Serial;
import dk.netarkivet.testutils.StringAsserts;
import dk.netarkivet.testutils.preconfigured.MoveTestFiles;


/**
 * Unit tests for index request message.
 */

public class IndexRequestMessageTester extends TestCase {
    private MoveTestFiles mtf = new MoveTestFiles(TestInfo.DATA_DIR,
            TestInfo.WORKING_DIR);

    private static final Set<Long> JOB_SET = new HashSet<Long>(
            Arrays.asList(new Long[]{1L, 2L}));

    public IndexRequestMessageTester(String s) {
        super(s);
    }

    public void setUp() {
        mtf.setUp();
    }

    public void tearDown() {
        mtf.tearDown();
    }

    /**
     * Test exceptions in constructor, and default parameters set correctly.
     */
    public void testIndexRequestMessage() throws Exception {
        try {
            new IndexRequestMessage(null, JOB_SET);
            fail("Should throw argument not valid");
        } catch (ArgumentNotValid e) {
            assertTrue("Should be right exception",
                       e.getMessage().contains("requestType"));
        }
        try {
            new IndexRequestMessage(RequestType.CDX, null);
            fail("Should throw argument not valid");
        } catch (ArgumentNotValid e) {
            assertTrue("Should be right exception",
                       e.getMessage().contains("jobSet"));
        }
        IndexRequestMessage irMsg
                = new IndexRequestMessage(RequestType.CDX, JOB_SET);
        assertEquals("Should preserve jobs", JOB_SET,
                     irMsg.getRequestedJobs());
        assertEquals("Should preserve type", RequestType.CDX,
                     irMsg.getRequestType());
        assertEquals("Should be right channel", Channels.getTheIndexServer(),
                     irMsg.getTo());
        assertEquals("Should be right channel", Channels.getThisIndexClient(),
                     irMsg.getReplyTo());
        assertNull("No found jobs yet", irMsg.getFoundJobs());
        assertNull("No index file found yet", irMsg.getResultFiles());

        Serial.assertTransientFieldsInitialized(irMsg);
    }

    /**
     * Test accept indeed calls the accept accepting indexrequestmessage.
     */
    public void testAccept() throws Exception {
        IndexRequestMessage irMsg
                = new IndexRequestMessage(RequestType.CDX, JOB_SET);

        IndexRequestMessageHandler v
                = new IndexRequestMessageHandler();
        irMsg.accept(v);
        assertTrue("Should have called right method", v.b);
    }

    /**
     * Test setter/getter.
     */
    public void testSetFoundJobs() throws Exception {
        IndexRequestMessage irMsg
                = new IndexRequestMessage(RequestType.CDX, JOB_SET);
        try {
            irMsg.setFoundJobs(null);
            fail("Should throw ArgumentNotValid");
        } catch (ArgumentNotValid e) {
            assertTrue("Should be the right exception",
                       e.getMessage().contains("foundJobs"));
        }
        assertNull("No found jobs yet", irMsg.getFoundJobs());
        irMsg.setFoundJobs(JOB_SET);
        assertEquals("Should have jobs now.", JOB_SET, irMsg.getFoundJobs());
    }

    /**
     * Test setter/getter.
     */
    public void testSetResultFiles() throws Exception {
        IndexRequestMessage irMsg
                = new IndexRequestMessage(RequestType.CDX, JOB_SET);
        try {
            irMsg.setResultFiles(null);
            fail("Should throw ArgumentNotValid");
        } catch (ArgumentNotValid e) {
            assertTrue("Should be the right exception",
                       e.getMessage().contains("resultFile"));
        }
        assertNull("No index file yet", irMsg.getResultFiles());
        File tempFile = File.createTempFile("temp", "temp", TestInfo.WORKING_DIR);
        File tempFile2 = File.createTempFile("temp", "temp", TestInfo.WORKING_DIR);
        List<RemoteFile> resultFiles = new ArrayList<RemoteFile>(2);
        resultFiles.add(new TestRemoteFile(tempFile, false, false, false));
        resultFiles.add(new TestRemoteFile(tempFile2, false, false, false));
        irMsg.setResultFiles(resultFiles);
        assertEquals("Should have exactly two result files",
                2, irMsg.getResultFiles().size());
        assertEquals("Should have index file .", tempFile.getName(),
                irMsg.getResultFiles().get(0).getName());
        assertEquals("Should have index file .", tempFile2.getName(),
                irMsg.getResultFiles().get(1).getName());
        assertTrue("Should be multiFile", irMsg.isIndexIsStoredInDirectory());
        try {
            irMsg.getResultFile();
            fail("Should not allow getting a single file");
        } catch (IllegalState e) {
            StringAsserts.assertStringContains("Should have multi in message",
                    "multiple", e.getMessage());
        }
        try {
            irMsg.setResultFiles(resultFiles);
            fail("Should be impossible to set results files again");
        } catch (IllegalState e) {
            StringAsserts.assertStringContains("Should mention already set",
                    "already has result files", e.getMessage());
        }
    }

    public void testSetResultFile() throws IOException {
        IndexRequestMessage irMsg
                = new IndexRequestMessage(RequestType.CDX, JOB_SET);
        try {
            irMsg.setResultFile(null);
            fail("Should throw ArgumentNotValid");
        } catch (ArgumentNotValid e) {
            assertTrue("Should be the right exception",
                    e.getMessage().contains("resultFile"));
        }
        assertNull("No index file yet", irMsg.getResultFiles());
        File tempFile = File.createTempFile("temp", "temp", TestInfo.WORKING_DIR);
        irMsg.setResultFile(new TestRemoteFile(tempFile, false, false, false));
        assertEquals("Should have index file .", tempFile.getName(),
                irMsg.getResultFile().getName());
        assertFalse("Should be a single-file message", irMsg.isIndexIsStoredInDirectory());
        try {
            irMsg.getResultFiles();
            fail("Should not allow getting multiple files.");
        } catch (IllegalState e) {
            StringAsserts.assertStringContains("Should mention single file",
                    "single", e.getMessage());
        }
        try {
            irMsg.setResultFile(new TestRemoteFile(tempFile, false, false,
                                                    false));
            fail("Should be impossible to set results files again");
        } catch (IllegalState e) {
            StringAsserts.assertStringContains("Should mention already set",
                    "already has result files", e.getMessage());
        }
    }

    private static class IndexRequestMessageHandler
            extends ArchiveMessageHandler {

        private boolean b = false;

        public void visit(IndexRequestMessage msg) throws PermissionDenied {
            b = true;
        }
    }

    /** Test serializability
     *
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public void testSerializability()
            throws IOException, ClassNotFoundException {
        IndexRequestMessage irMsg
                = new IndexRequestMessage(RequestType.CDX, JOB_SET);
        IndexRequestMessage irMsg2 = Serial.serial(irMsg);
        assertEquals("Must deserialize to same state",
                     relevantState(irMsg), relevantState(irMsg2));
        irMsg.setNotOk("AARGH");
        irMsg.setFoundJobs(JOB_SET);
        File tempFile = File.createTempFile("temp", "temp");
        tempFile.deleteOnExit();
        List<RemoteFile> resultFiles = new ArrayList<RemoteFile>(1);
        resultFiles.add(new TestRemoteFile(tempFile, false, false, false));
        irMsg.setResultFiles(resultFiles);
        irMsg2 = Serial.serial(irMsg);
        assertEquals("Must deserialize to same state",
                     relevantState(irMsg), relevantState(irMsg2));
        //Try with non-serializable sets
        irMsg = new IndexRequestMessage(RequestType.CDX,
                                        new HashMap<Long,Long>().keySet());
        irMsg.setFoundJobs(new HashMap<Long,Long>().keySet());
        irMsg2 = Serial.serial(irMsg);
        assertEquals("Must deserialize to same state",
                     relevantState(irMsg), relevantState(irMsg2));
    }

    private String relevantState(IndexRequestMessage irMsg) {
        return irMsg.toString()
                + StringUtils.conjoin(irMsg.getRequestedJobs(), ",")
                + StringUtils.conjoin(irMsg.getFoundJobs(), ",")
                + irMsg.getRequestType() + irMsg.getResultFiles()
                + irMsg.isIndexIsStoredInDirectory();
    }

}