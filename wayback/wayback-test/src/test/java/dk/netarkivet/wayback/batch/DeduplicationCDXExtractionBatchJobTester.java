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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

import junit.framework.TestCase;
import org.archive.wayback.core.CaptureSearchResult;
import org.archive.wayback.resourceindex.cdx.CDXLineToSearchResultAdapter;

import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.batch.BatchLocalFiles;
import dk.netarkivet.testutils.TestFileUtils;
import dk.netarkivet.wayback.TestInfo;
import dk.netarkivet.wayback.batch.DeduplicationCDXExtractionBatchJob;

/**
 * Tests of the DeduplicationCDXExtractionBatchJob.
 */
public class DeduplicationCDXExtractionBatchJobTester extends TestCase {

    public static final String METADATA_FILENAME = "duplicate.metadata.arc";
    /** The two next files doesn't exist, therefore renamed from REAL to UNREAL */
    public static final String METADATA_FILENAME_UNREAL_1 = "124412-metadata-1.arc";
    public static final String METADATA_FILENAME_UNREAL_2 = "124399-metadata-1.arc";
    public static final String METADATA_FILENAME_REAL_1 = "1-metadata-1.warc";

    public void setUp() {
        FileUtils.removeRecursively(TestInfo.WORKING_DIR);
        TestFileUtils.copyDirectoryNonCVS(TestInfo.ORIGINALS_DIR, TestInfo.WORKING_DIR);
    }

    public void tearDown() {
        FileUtils.removeRecursively(TestInfo.WORKING_DIR);
        FileUtils.remove(TestInfo.LOG_FILE);
    }

    public void testInitialize() {
        DeduplicationCDXExtractionBatchJob job = new DeduplicationCDXExtractionBatchJob();
        job.initialize(new ByteArrayOutputStream());
    }

    public void testJob() throws IOException {
        File testFile = new File(TestInfo.WORKING_DIR, METADATA_FILENAME);
        assertTrue("file should exist", testFile.isFile());
        BatchLocalFiles files = new BatchLocalFiles(new File[]{testFile});       
        DeduplicationCDXExtractionBatchJob job = new DeduplicationCDXExtractionBatchJob();
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        files.run(job, os);
        os.flush();
        String results = os.toString();
        String[] cdx_lines = results.split("\\n");
        assertTrue("Expect some results", cdx_lines.length > 2);
        CDXLineToSearchResultAdapter adapter = new CDXLineToSearchResultAdapter();
        for (String cdx_line: cdx_lines) {
            CaptureSearchResult csr = adapter.adapt(cdx_line);
            assertNotNull("Expect a mime type for every result", csr.getMimeType());
        }
    }

    public void testJobRealOne() throws IOException {
        DeduplicationCDXExtractionBatchJob job = new DeduplicationCDXExtractionBatchJob();
        File arcFile =  new File(TestInfo.WORKING_DIR, METADATA_FILENAME_UNREAL_1);
        assertFalse("file shouldn't exist", arcFile.isFile());
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        job.initialize(os);
        job.processFile(arcFile, os);
        job.finish(os);
        Exception[] exceptions = job.getExceptionArray();
        assertTrue(exceptions.length == 1);
        //System.out.println("exception " + exceptions[0]);   
    }
    public void testJobRealTwo() throws IOException {
        DeduplicationCDXExtractionBatchJob job2 = new DeduplicationCDXExtractionBatchJob();
        File arcFile2 =  new File(TestInfo.WORKING_DIR, METADATA_FILENAME_UNREAL_2);
        assertFalse("file should not exist", arcFile2.isFile());
        ByteArrayOutputStream os2 = new ByteArrayOutputStream();
        job2.initialize(os2);
        job2.processFile(arcFile2, os2);
        job2.finish(os2);
        //os.writeTo(System.out);
        Exception[] exceptions = job2.getExceptionArray();
        assertTrue(exceptions.length == 1);
        //System.out.println("exception " + exceptions[0]);   
    }
    
    public void testJobRealWarc() throws IOException {
        DeduplicationCDXExtractionBatchJob job3 = new DeduplicationCDXExtractionBatchJob();
        File warcFile =  new File(TestInfo.WORKING_DIR, METADATA_FILENAME_REAL_1);
        assertTrue("file should exist", warcFile.isFile());
        ByteArrayOutputStream os3 = new ByteArrayOutputStream();
        job3.initialize(os3);
        job3.processFile(warcFile, os3);
        job3.finish(os3);
        //os3.writeTo(System.out);
        Exception[] exceptions = job3.getExceptionArray();
        assertTrue(exceptions.length == 0);
    }
}
