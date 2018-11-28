/*
 * #%L
 * Netarchivesuite - harvester - test
 * %%
 * Copyright (C) 2005 - 2018 The Royal Danish Library, 
 *             the National Library of France and the Austrian National Library.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 2.1 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */
package dk.netarkivet.harvester.indexserver.distribute;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import dk.netarkivet.common.distribute.Channels;
import dk.netarkivet.common.distribute.RemoteFile;
import dk.netarkivet.common.distribute.RemoteFileSettings;
import dk.netarkivet.common.distribute.TestRemoteFile;
import dk.netarkivet.common.distribute.indexserver.RequestType;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IllegalState;
import dk.netarkivet.common.exceptions.PermissionDenied;
import dk.netarkivet.common.utils.StringUtils;
import dk.netarkivet.harvester.distribute.HarvesterMessageHandler;
import dk.netarkivet.testutils.Serial;
import dk.netarkivet.testutils.StringAsserts;
import dk.netarkivet.testutils.preconfigured.MoveTestFiles;

/**
 * Unit tests for index request message.
 */

public class IndexRequestMessageTester {
    private MoveTestFiles mtf = new MoveTestFiles(TestInfo.ORIGINALS_DIR, TestInfo.WORKING_DIR);

    private static final Set<Long> JOB_SET = new HashSet<Long>(Arrays.asList(new Long[] {1L, 2L}));

    @Before
    public void setUp() {
        mtf.setUp();
    }

    @After
    public void tearDown() {
        mtf.tearDown();
    }

    /**
     * Test exceptions in constructor, and default parameters set correctly.
     */
    @Test
    public void testIndexRequestMessage() throws Exception {
        try {
            new IndexRequestMessage((RequestType) null, JOB_SET, null);
            fail("Should throw argument not valid");
        } catch (ArgumentNotValid e) {
            assertTrue("Should be right exception", e.getMessage().contains("requestType"));
        }
        try {
            new IndexRequestMessage(RequestType.CDX, null, null);
            fail("Should throw argument not valid");
        } catch (ArgumentNotValid e) {
            assertTrue("Should be right exception", e.getMessage().contains("jobSet"));
        }
        IndexRequestMessage irMsg = new IndexRequestMessage(RequestType.CDX, JOB_SET, null);
        assertEquals("Should preserve jobs", JOB_SET, irMsg.getRequestedJobs());
        assertEquals("Should preserve type", RequestType.CDX, irMsg.getRequestType());
        assertEquals("Should be right channel", Channels.getTheIndexServer(), irMsg.getTo());
        assertEquals("Should be right channel", Channels.getThisIndexClient(), irMsg.getReplyTo());
        assertNull("No found jobs yet", irMsg.getFoundJobs());
        assertNull("No index file found yet", irMsg.getResultFiles());

        Serial.assertTransientFieldsInitialized(irMsg);
    }

    /**
     * Test accept indeed calls the accept accepting indexrequestmessage.
     */
    @Test
    public void testAccept() throws Exception {
        IndexRequestMessage irMsg = new IndexRequestMessage(RequestType.CDX, JOB_SET, null);

        IndexRequestMessageHandler v = new IndexRequestMessageHandler();
        irMsg.accept(v);
        assertTrue("Should have called right method", v.b);
    }

    /**
     * Test setter/getter.
     */
    @Test
    public void testSetFoundJobs() throws Exception {
        IndexRequestMessage irMsg = new IndexRequestMessage(RequestType.CDX, JOB_SET, null);
        try {
            irMsg.setFoundJobs(null);
            fail("Should throw ArgumentNotValid");
        } catch (ArgumentNotValid e) {
            assertTrue("Should be the right exception", e.getMessage().contains("foundJobs"));
        }
        assertNull("No found jobs yet", irMsg.getFoundJobs());
        irMsg.setFoundJobs(JOB_SET);
        assertEquals("Should have jobs now.", JOB_SET, irMsg.getFoundJobs());
    }

    /**
     * Test setter/getter.
     */
    @Test
    public void testSetResultFiles() throws Exception {
        IndexRequestMessage irMsg = new IndexRequestMessage(RequestType.CDX, JOB_SET, null);
        try {
            irMsg.setResultFiles(null);
            fail("Should throw ArgumentNotValid");
        } catch (ArgumentNotValid e) {
            assertTrue("Should be the right exception", e.getMessage().contains("resultFile"));
        }
        assertNull("No index file yet", irMsg.getResultFiles());
        File tempFile = File.createTempFile("temp", "temp", TestInfo.WORKING_DIR);
        File tempFile2 = File.createTempFile("temp", "temp", TestInfo.WORKING_DIR);
        List<RemoteFile> resultFiles = new ArrayList<RemoteFile>(2);
        resultFiles.add(new TestRemoteFile(tempFile, false, false, false));
        resultFiles.add(new TestRemoteFile(tempFile2, false, false, false));
        irMsg.setResultFiles(resultFiles);
        assertEquals("Should have exactly two result files", 2, irMsg.getResultFiles().size());
        assertEquals("Should have index file .", tempFile.getName(), irMsg.getResultFiles().get(0).getName());
        assertEquals("Should have index file .", tempFile2.getName(), irMsg.getResultFiles().get(1).getName());
        assertTrue("Should be multiFile", irMsg.isIndexIsStoredInDirectory());
        try {
            irMsg.getResultFile();
            fail("Should not allow getting a single file");
        } catch (IllegalState e) {
            StringAsserts.assertStringContains("Should have multi in message", "multiple", e.getMessage());
        }
        try {
            irMsg.setResultFiles(resultFiles);
            fail("Should be impossible to set results files again");
        } catch (IllegalState e) {
            StringAsserts
                    .assertStringContains("Should mention already set", "already has result files", e.getMessage());
        }
    }

    @Test
    public void testSetResultFile() throws IOException {
        IndexRequestMessage irMsg = new IndexRequestMessage(RequestType.CDX, JOB_SET, null);
        try {
            irMsg.setResultFile(null);
            fail("Should throw ArgumentNotValid");
        } catch (ArgumentNotValid e) {
            assertTrue("Should be the right exception", e.getMessage().contains("resultFile"));
        }
        assertNull("No index file yet", irMsg.getResultFiles());
        File tempFile = File.createTempFile("temp", "temp", TestInfo.WORKING_DIR);
        irMsg.setResultFile(new TestRemoteFile(tempFile, false, false, false));
        assertEquals("Should have index file .", tempFile.getName(), irMsg.getResultFile().getName());
        assertFalse("Should be a single-file message", irMsg.isIndexIsStoredInDirectory());
        try {
            irMsg.getResultFiles();
            fail("Should not allow getting multiple files.");
        } catch (IllegalState e) {
            StringAsserts.assertStringContains("Should mention single file", "single", e.getMessage());
        }
        try {
            irMsg.setResultFile(new TestRemoteFile(tempFile, false, false, false));
            fail("Should be impossible to set results files again");
        } catch (IllegalState e) {
            StringAsserts
                    .assertStringContains("Should mention already set", "already has result files", e.getMessage());
        }
    }

    /**
     * Test NAS-2017. Test that included FTPServer information can be retrieved again.
     */
    @Test
    public void testMessageWithNonNullRemoteFileSettings() {

        RemoteFileSettings ftpSettings = new RemoteFileSettings("localhost", 25, "test", "test123");

        IndexRequestMessage irMsg = new IndexRequestMessage(RequestType.CDX, JOB_SET, ftpSettings);
        RemoteFileSettings ftpSettingsCopy = irMsg.getRemoteFileSettings();

        assertTrue(ftpSettings.getServerName().equals(ftpSettingsCopy.getServerName()));
        assertTrue(ftpSettings.getServerPort() == ftpSettingsCopy.getServerPort());
        assertTrue(ftpSettings.getUserName().equals(ftpSettingsCopy.getUserName()));
        assertTrue(ftpSettings.getUserPassword().equals(ftpSettingsCopy.getUserPassword()));
    }

    private static class IndexRequestMessageHandler extends HarvesterMessageHandler {

        private boolean b = false;

        public void visit(IndexRequestMessage msg) throws PermissionDenied {
            b = true;
        }
    }

    /**
     * Test serializability.
     *
     * @throws IOException
     * @throws ClassNotFoundException
     */
    @Test
    public void testSerializability() throws IOException, ClassNotFoundException {
        RemoteFileSettings ftpSettings = new RemoteFileSettings("localhost", 25, "test", "test123");
        IndexRequestMessage irMsg = new IndexRequestMessage(RequestType.CDX, JOB_SET, ftpSettings);
        IndexRequestMessage irMsg2 = Serial.serial(irMsg);
        assertEquals("Must deserialize to same state", relevantState(irMsg), relevantState(irMsg2));
        irMsg.setNotOk("AARGH");
        irMsg.setFoundJobs(JOB_SET);
        File tempFile = File.createTempFile("temp", "temp");
        tempFile.deleteOnExit();
        List<RemoteFile> resultFiles = new ArrayList<RemoteFile>(1);
        resultFiles.add(new TestRemoteFile(tempFile, false, false, false));
        irMsg.setResultFiles(resultFiles);
        irMsg2 = Serial.serial(irMsg);
        assertEquals("Must deserialize to same state", relevantState(irMsg), relevantState(irMsg2));
        // Try with non-serializable sets
        irMsg = new IndexRequestMessage(RequestType.CDX, new HashMap<Long, Long>().keySet(), ftpSettings);
        irMsg.setFoundJobs(new HashMap<Long, Long>().keySet());
        irMsg2 = Serial.serial(irMsg);
        assertEquals("Must deserialize to same state", relevantState(irMsg), relevantState(irMsg2));
    }

    private String relevantState(IndexRequestMessage irMsg) {
        return irMsg.toString() + StringUtils.conjoin(",", irMsg.getRequestedJobs())
                + StringUtils.conjoin(",", irMsg.getFoundJobs()) + irMsg.getRequestType() + irMsg.getResultFiles()
                + irMsg.isIndexIsStoredInDirectory();
    }

}
