/*
 * #%L
 * Netarchivesuite - wayback - test
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

package dk.netarkivet.wayback.batch;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

import org.junit.Before;
import org.junit.Test;

import dk.netarkivet.common.utils.arc.ARCBatchJob;
import dk.netarkivet.common.utils.batch.BatchLocalFiles;
import dk.netarkivet.common.utils.warc.WARCBatchJob;
import dk.netarkivet.testutils.LogbackRecorder;

/**
 * Unittests for the batchjob WaybackCDXExtractionARCBatchJob and WaybackCDXExtractionWARCBatchJob.
 */
public class WaybackCDXExtractionArcAndWarcBatchJobTester {

    private BatchLocalFiles blaf;
    private BatchLocalFiles blafWarc;

    @Before
    public void setUp() throws Exception {
        File file = new File("tests/dk/netarkivet/wayback/data/originals/arcfile_withredirects.arc");
        File warcfile = new File("tests/dk/netarkivet/wayback/data/originals/warcfile_withredirects.warc");
        assertTrue("ArcFile should exist: '" + file.getAbsolutePath() + "'", file.exists());
        assertTrue("WarcFile should exist: '" + warcfile.getAbsolutePath() + "'", warcfile.exists());
        blaf = new BatchLocalFiles(new File[] {file});
        blafWarc = new BatchLocalFiles(new File[] {warcfile});
    }

    @Test
    public void testARCProcess() throws IOException {
        LogbackRecorder lr = LogbackRecorder.startRecorder();
        ARCBatchJob job = new WaybackCDXExtractionARCBatchJob();
        OutputStream os = new ByteArrayOutputStream();
        blaf.run(job, os);
        os.flush();
        // System.out.println(os);
        assertFalse("expect a non-empty output", os.toString() == null || os.toString().length() == 0);
        assertTrue("Should be no exceptions", job.getExceptions().isEmpty());
        // Check log for "Could not parse errors
        lr.assertLogNotContains("Batchjob results in 'could not parse' errors.", "Could not parse");
        lr.stopRecorder();
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
     * private void assertNotStringContains(String msg, String src, String str) { int index = src.indexOf(str); if
     * (index != -1) { System.out.println("Actual string: "); System.out.println(src); assertEquals(msg, -1, index); } }
     */
    @Test
    public void testWARCProcess() throws IOException {
        WARCBatchJob job = new WaybackCDXExtractionWARCBatchJob();
        OutputStream os = new ByteArrayOutputStream();
        blafWarc.run(job, os);
        os.flush();
        // System.out.println(os);
        assertFalse("expect a non-empty output", os.toString() == null || os.toString().length() == 0);
        assertTrue("Should be no exceptions", job.getExceptions().isEmpty());
    }
}
