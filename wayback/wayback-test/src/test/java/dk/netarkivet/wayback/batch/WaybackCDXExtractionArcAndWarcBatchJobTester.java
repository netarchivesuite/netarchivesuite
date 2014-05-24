/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
 *
 * Copyright 2004-2012 The Royal Danish Library, the Danish State and
 * University Library, the National Library of France and the Austrian
 * National Library.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 */

package dk.netarkivet.wayback.batch;

import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.logging.LogManager;

import junit.framework.TestCase;

import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.batch.BatchLocalFiles;
import dk.netarkivet.common.utils.warc.WARCBatchJob;
import dk.netarkivet.common.utils.arc.ARCBatchJob;
import dk.netarkivet.testutils.LogUtils;
import dk.netarkivet.wayback.TestInfo;
import dk.netarkivet.wayback.batch.WaybackCDXExtractionARCBatchJob;
import dk.netarkivet.wayback.batch.WaybackCDXExtractionWARCBatchJob;


/**
 * Unittests for the batchjob WaybackCDXExtractionARCBatchJob
 * and WaybackCDXExtractionWARCBatchJob.
 */
public class WaybackCDXExtractionArcAndWarcBatchJobTester extends TestCase {

    private BatchLocalFiles blaf;
    private BatchLocalFiles blafWarc;
     

    public void setUp() throws Exception {
        super.setUp();
        File file = new File(
                "tests/dk/netarkivet/wayback/data/originals/arcfile_withredirects.arc");
        File warcfile = new File(
                "tests/dk/netarkivet/wayback/data/originals/warcfile_withredirects.warc");
        assertTrue("ArcFile should exist: '"
                + file.getAbsolutePath() + "'", file.exists());
        assertTrue("WarcFile should exist: '" + warcfile.getAbsolutePath()
                + "'", warcfile.exists());
        blaf = new BatchLocalFiles(new File[] {file});
        blafWarc = new BatchLocalFiles(new File[] {warcfile});
        FileInputStream fis = new FileInputStream("tests/dk/netarkivet/testlog.prop");
        LogManager.getLogManager().readConfiguration(fis);
        fis.close();
    }

    public void testARCProcess() throws IOException {
        ARCBatchJob job = new WaybackCDXExtractionARCBatchJob();
        OutputStream os = new ByteArrayOutputStream();
        blaf.run(job,os);
        os.flush();
        //System.out.println(os);
        assertFalse("expect a non-empty output", os.toString() == null || os.toString().length()==0);
        assertTrue("Should be no exceptions", job.getExceptions().isEmpty());
        // Check log for "Could not parse errors
        LogUtils.flushLogs(WaybackCDXExtractionARCBatchJob.class.getName());

        if (TestInfo.LOG_FILE.exists()) {
            String logtxt = FileUtils.readFile(TestInfo.LOG_FILE);
            assertNotStringContains("Batchjob results in 'could not parse' errors.",
                logtxt, "Could not parse");
        } else {
            fail("Logging disabled or wrong path to logfile: " 
                    + TestInfo.LOG_FILE.getAbsolutePath());
        }
    }
    
    /** Asserts that a source string does not contain a given string, and prints
     * out the source string if the target string is found.
     *
     * @param msg An explanatory message
     * @param src A string to search through
     * @param str A string to search for
     */
    private void assertNotStringContains(String msg, String src, String str) {
        int index = src.indexOf(str);
        if (index != -1) {
            System.out.println("Actual string: ");
            System.out.println(src);
            assertEquals(msg, -1, index);
        }
    }

    
    
    public void testWARCProcess() throws IOException {
        WARCBatchJob job = new WaybackCDXExtractionWARCBatchJob();
        OutputStream os = new ByteArrayOutputStream();
        blafWarc.run(job,os);
        os.flush();
        //System.out.println(os);
        assertFalse("expect a non-empty output", os.toString() == null || os.toString().length()==0);
        assertTrue("Should be no exceptions", job.getExceptions().isEmpty());
    }
}
