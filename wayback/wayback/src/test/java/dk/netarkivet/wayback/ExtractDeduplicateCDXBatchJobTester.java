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

package dk.netarkivet.wayback;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

import junit.framework.TestCase;
import org.archive.wayback.core.CaptureSearchResult;
import org.archive.wayback.resourceindex.cdx.CDXLineToSearchResultAdapter;

import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.arc.ARCBatchJob;
import dk.netarkivet.common.utils.batch.BatchLocalFiles;
import dk.netarkivet.testutils.TestFileUtils;
import dk.netarkivet.wayback.batch.ExtractDeduplicateCDXBatchJob;

/**
 * csr forgot to comment this!
 *
 */

public class ExtractDeduplicateCDXBatchJobTester extends TestCase {

    public static final String METADATA_FILENAME = "duplicate.metadata.arc";
    public static final String METADATA_FILENAME_REAL_1 = "124412-metadata-1.arc";
    public static final String METADATA_FILENAME_REAL_2 = "124399-metadata-1.arc";


    public void setUp() {
        FileUtils.removeRecursively(TestInfo.WORKING_DIR);
        TestFileUtils.copyDirectoryNonCVS(TestInfo.ORIGINALS_DIR, TestInfo.WORKING_DIR);
    }


    public void tearDown() {

        FileUtils.removeRecursively(TestInfo.WORKING_DIR);
        FileUtils.remove(TestInfo.LOG_FILE);
    }

    public void testInitialize() {
        ARCBatchJob job = new ExtractDeduplicateCDXBatchJob();
        job.initialize(new ByteArrayOutputStream());
    }

    public void testJob() throws IOException {
        BatchLocalFiles files = new BatchLocalFiles(new File[] {new File(
                TestInfo.WORKING_DIR, METADATA_FILENAME)});
        ARCBatchJob job = new ExtractDeduplicateCDXBatchJob();
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

    public void testJobReal() throws IOException {
        ARCBatchJob job = new ExtractDeduplicateCDXBatchJob();
        File arcFile =  new File(TestInfo.WORKING_DIR, METADATA_FILENAME_REAL_1);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        job.initialize(os);
        job.processFile(arcFile, os);
        job.finish(os);
        ARCBatchJob job2 = new ExtractDeduplicateCDXBatchJob();
        File arcFile2 =  new File(TestInfo.WORKING_DIR, METADATA_FILENAME_REAL_2);
        ByteArrayOutputStream os2 = new ByteArrayOutputStream();
        job2.initialize(os2);
        job2.processFile(arcFile2, os2);
        job2.finish(os2);
        //os.writeTo(System.out);
        job2.getExceptionArray();
    }

}
