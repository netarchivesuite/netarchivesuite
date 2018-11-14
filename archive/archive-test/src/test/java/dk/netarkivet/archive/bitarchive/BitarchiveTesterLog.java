/*
 * #%L
 * Netarchivesuite - archive - test
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
package dk.netarkivet.archive.bitarchive;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

import org.archive.io.arc.ARCRecord;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import dk.netarkivet.common.distribute.TestRemoteFile;
import dk.netarkivet.common.utils.arc.ARCBatchJob;
import dk.netarkivet.common.utils.batch.ARCBatchFilter;
import dk.netarkivet.testutils.LogbackRecorder;

/**
 * Unit test for Bitarchive API.
 * The logging of bitarchive operations is tested
 */
@SuppressWarnings({"serial"})
public class BitarchiveTesterLog extends BitarchiveTestCase {
    private static File EXISTING_ARCHIVE_DIR = new File("tests/dk/netarkivet/archive/bitarchive/data/log/existing/");
    private static String ARC_FILE_NAME1 = "Upload1.ARC";
    private static String ARC_FILE_NAME2 = "Upload2.ARC";
    private static String ARC_FILE_NAME3 = "Upload3.ARC";
    private static File ARC_FILE = new File("tests/dk/netarkivet/archive/bitarchive/data/log/originals/",
            ARC_FILE_NAME1);

    protected File getOriginalsDir() {
        return EXISTING_ARCHIVE_DIR;
    }

    @Before
    public void setUp() throws Exception {
        super.setUp();
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Asserts that a source string does not contain a given string, and prints out the source string if the target
     * string is found.
     *
     * @param msg An explanatory message
     * @param src A string to search through
     * @param str A string to search for
     */
    /*
     * // TODO remove old log code private void assertNotStringContains(String msg, String src, String str) { int index
     * = src.indexOf(str); if (index != -1) { System.out.println("Actual string: "); System.out.println(src);
     * assertEquals(msg, -1, index); } }
     */

    /**
     * Test logging of upload command
     *
     * @throws IOException
     */
    @Test
    public void testLogUpload() throws IOException {
        LogbackRecorder lr = LogbackRecorder.startRecorder();
        lr.assertLogNotContains("Log does not contain file before uploading.", ARC_FILE_NAME1);
        
        // Ensure enough free space
        BitarchiveAdmin admin = BitarchiveAdmin.getInstance();
        if (!admin.hasEnoughSpace()) {
        	System.err.println("Skipping test. Not enough space on disk to perform test");
        	return;
        }
        archive.upload(new TestRemoteFile(ARC_FILE, false, false, false), ARC_FILE.getName());

        lr.assertLogContains("Log contains file after uploading.", ARC_FILE_NAME1);
        lr.assertLogContains("Log contains the word upload after uploading.", "upload");
        lr.stopRecorder();
    }

    /**
     * test logging of get
     */
    @Test
    public void testLogGet() throws Exception {
        LogbackRecorder lr = LogbackRecorder.startRecorder();
        // String logtxt = FileUtils.readFile(LOG_FILE);
        // assertNotStringContains("Unuploaded files are not mentioned in the log.", logtxt, ARC_FILE_NAME2); // clean
        // log
        lr.assertLogNotContains("Unuploaded files are not mentioned in the log.", ARC_FILE_NAME2);

        archive.get(ARC_FILE_NAME2, 0);

        // FileAsserts.assertFileContains("Log contains file after getting.", ARC_FILE_NAME2, LOG_FILE);
        // FileAsserts.assertFileContains("Log contains the word get after getting.", "get", LOG_FILE);
        lr.assertLogContains("Log contains file after getting.", "GET: " + ARC_FILE_NAME2 + ":0");
        lr.assertLogContains("Log contains the word get after getting.", "GET: Got 1330 bytes of data from "
                + ARC_FILE_NAME2 + ":0");
        lr.stopRecorder();
    }

    /**
     * test logging of get
     */
    @Test
    public void testLogNotGet() throws Exception {
        LogbackRecorder lr = LogbackRecorder.startRecorder();
        lr.assertLogNotContains("Unuploaded files are not mentioned in the log.", ARC_FILE_NAME3);

        archive.get(ARC_FILE_NAME3, 0);

        lr.assertLogContains("Log contains file after getting.", "GET: " + ARC_FILE_NAME3 + ":0");
        lr.assertLogContains("Log contains the word get after not getting.",
                "Get request for file not on this machine: " + ARC_FILE_NAME3);
        lr.stopRecorder();
    }

    @Test
    public void testLogBatch() throws Exception {
        LogbackRecorder lr = LogbackRecorder.startRecorder();
        lr.assertLogNotContains("Batch not mentioned in log before run", "Batch");
        // Run the empty batch job.
        try {
            archive.batch(TestInfo.baAppId, new ARCBatchJob() {
                public ARCBatchFilter getFilter() {
                    return ARCBatchFilter.NO_FILTER;
                }

                public void initialize(OutputStream os) {
                }

                public void processRecord(ARCRecord record, OutputStream os) {
                }

                public void finish(OutputStream os) {
                }
            });
        } catch (Exception e) {
            fail("Batching should not throw " + e);
        }
        // FileAsserts.assertFileContains("Log should have batch status", "0 failures in processing 0 files", LOG_FILE);
        lr.assertLogContains("Log contains the word 'Batch'.", "Batch");
        lr.assertLogContains("Log contains the phrase 'Batch: Job'.", "Batch: Job");
        lr.assertLogContains("Log should have start indicator", "Starting batch job");
        lr.assertLogContains("Log should have end indicator", "Finished batch job");
        lr.assertLogContains("Log should have batch status", "2 failures in processing 2 files");
        lr.stopRecorder();
    }

}
