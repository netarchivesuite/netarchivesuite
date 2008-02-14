/* File:                 $Id$
* Revision:         $Revision$
* Author:                $Author$
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
package dk.netarkivet.archive.bitarchive;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import dk.netarkivet.common.distribute.arcrepository.BatchStatus;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.StreamUtils;
import dk.netarkivet.common.utils.arc.BatchFilter;
import dk.netarkivet.common.utils.arc.FileBatchJob;
import dk.netarkivet.testutils.CollectionAsserts;
import static dk.netarkivet.testutils.CollectionUtils.list;
import dk.netarkivet.testutils.FileAsserts;


/**
 * This class tests the bitarchive's batch() method.
 */
public class BitarchiveTesterBatch extends BitarchiveTestCase {
    static final File ORIGINALS_DIR =
            new File(new File(TestInfo.DATA_DIR, "batch"), "originals");
    private static List<String> arcFiles = list("Upload3.ARC", "fyensdk.arc",
            "Upload1.ARC", "Upload2.ARC");
    private static int ARCHIVE_SIZE = arcFiles.size();

    /**
     * Construct a new tester object.
     */
    public BitarchiveTesterBatch(final String sTestName) {
        super(sTestName);
    }

    protected File getOriginalsDir() {
        return ORIGINALS_DIR;
    }

    /**
     * At start of test, set up an archive we can run against.
     */
    public void setUp() throws Exception {
        super.setUp();
        File fileDir = new File(TestInfo.WORKING_DIR, "filedir");
        for (String filename : arcFiles) {
            FileUtils.copyFile(new File(getOriginalsDir(), filename),
                    new File(fileDir, filename));
        }
    }

    /**
     * At end of test, remove any files we managed to upload.
     */
    public void tearDown() throws Exception {
        super.tearDown();
    }


    /**
     * Test null arguments
     */
    public void testBatchNoCode() {
        TestFileBatchJob job = new TestFileBatchJob();
        try {
            archive.batch(TestInfo.baAppId, null);
            fail("Null batch process should raise ArgumentNotValid");
        } catch (ArgumentNotValid e) {
            // Correct case
        }
    }


    /**
     * Test that exceptions thrown in the batch program are thrown
     * back to the caller
     */
    public void testBatchExceptionInBatch() {
        BatchStatus status =
                archive.batch(TestInfo.baAppId, new FileBatchJob() {
                    /**
                     * Get the filter on this batch job.
                     */
                    public BatchFilter getFilter() {
                        return BatchFilter.NO_FILTER;
                    }

                    public void initialize(OutputStream os) {
                    }

                    public boolean processFile(File f, OutputStream os) {
                        throw new IOFailure("Testing IO throws");
                    }

                    public void finish(OutputStream os) {
                    }
                });
        assertEquals("Status should show all files processed",
                     4, status.getNoOfFilesProcessed());
        assertEquals("Status should show all files failed",
                     4, status.getFilesFailed().size());
    }

    /**
     * est that the batch code actually runs, that it enters each of the
     * initialize() and finish() methods, and that process() is called at least
     * once.
     */
    public void testBatchCodeRuns() {
        TestFileBatchJob job = new TestFileBatchJob();
        BatchStatus lbs = archive.batch(TestInfo.baAppId, job);
        assertTrue("initialize() should been called on job", job.initialized);
        assertEquals("some calls should have been made to process()",
                arcFiles.size(), job.processedFileList.size());
        assertTrue("finish() should have been called on job", job.finished);
        lbs.getResultFile().copyTo(TestInfo.BATCH_OUTPUT_FILE);
        FileAsserts.assertFileContains("Did not log <Started> in output",
                "Started", TestInfo.BATCH_OUTPUT_FILE);
        FileAsserts.assertFileContains("Did not log <Processed> in output",
                "Processed", TestInfo.BATCH_OUTPUT_FILE);
        FileAsserts.assertFileContains("Did not log <Finished> in output",
                "Finished", TestInfo.BATCH_OUTPUT_FILE);
    }

    /**
     * Test that the batch code writes its expected output
     */
    public void testBatchCodeOutput() {
        FileBatchJob job = new TestFileBatchJob();
        BatchStatus lbs = archive.batch(TestInfo.baAppId, job);
        lbs.getResultFile().copyTo(TestInfo.BATCH_OUTPUT_FILE);
        FileAsserts.assertFileContains("Did not log <Started> in output",
                "Started", TestInfo.BATCH_OUTPUT_FILE);
        FileAsserts.assertFileContains("Did not log <Processed> in output",
                "Processed", TestInfo.BATCH_OUTPUT_FILE);
        FileAsserts.assertFileContains("Did not log <Finished> in output",
                "Finished", TestInfo.BATCH_OUTPUT_FILE);
    }

    /**
     * Test that the batch code runs once for each entry.
     * Both for multiple entries in a file and in several files.
     */
    public void testBatchCodeRunsAll() {
        TestFileBatchJob job = new TestFileBatchJob();
        archive.batch(TestInfo.baAppId, job);
        assertEquals("Number of processed files is incorrect",
                ARCHIVE_SIZE, job.processedFileList.size());
    }

    public void testBatchCodeFiltersWork() {
        TestFileBatchJob job = new TestFileBatchJob();
        assertBatchJobProcessesCorrectly("No filter", job,
                "fyensdk.arc", "Upload1.ARC", "Upload2.ARC", "Upload3.ARC");

        job = new TestFileBatchJob();
        job.processOnlyFileNamed("Upload2.ARC");
        assertBatchJobProcessesCorrectly("Single file", job, "Upload2.ARC");

        job = new TestFileBatchJob();
        job.processOnlyFilesNamed(list("Upload2.ARC", "fyensdk.arc"));
        assertBatchJobProcessesCorrectly("File list", job,
                "Upload2.ARC", "fyensdk.arc");

        job = new TestFileBatchJob();
        job.processOnlyFileNamed("Upload....");
        assertBatchJobProcessesCorrectly("Quotable regexp", job);

        job = new TestFileBatchJob();
        job.processOnlyFilesMatching("Upload..ARC");
        assertBatchJobProcessesCorrectly("Regexp with dot", job,
                "Upload2.ARC", "Upload3.ARC", "Upload1.ARC");

        job = new TestFileBatchJob();
        job.processOnlyFilesMatching(".*.ARC");
        assertBatchJobProcessesCorrectly("Starry regexp", job,
                "Upload2.ARC", "Upload3.ARC", "Upload1.ARC");

        job = new TestFileBatchJob();
        job.processOnlyFilesMatching("(Upload2|fyensdk).(arc|ARC)");
        assertBatchJobProcessesCorrectly("Quotable regexp", job,
                "Upload2.ARC", "fyensdk.arc");

        job = new TestFileBatchJob();
        job.processOnlyFilesMatching(list("Upload[23].ARC", "fy.*rc"));
        assertBatchJobProcessesCorrectly("Multiple regexps", job,
                "fyensdk.arc", "Upload2.ARC", "Upload3.ARC");

        job = new TestFileBatchJob();
        job.processOnlyFilesMatching(list("Upload[23].ARC", ".*3.ARC"));
        assertBatchJobProcessesCorrectly("Multiple regexps with overlap", job,
                "Upload2.ARC", "Upload3.ARC");


    }

    /** Test that illegal code (e.g. that tries to read outside of bitarchive
     * dir, or that tries to write anywhere) cannot be executed.
     */
    public void testIllegalCode() throws IOException {
        String fyensdk = FileUtils.readFile(new File(TestInfo.WORKING_DIR, "fyensdk.arc"));
        // A class that gets loaded from outside our normal area.
        final File evilClassFile = new File(TestInfo.WORKING_DIR, "EvilBatch.class");
        FileUtils.copyFile(new File("classes/dk/netarkivet/archive/bitarchive/EvilBatch.class"),
                           evilClassFile);
        InputStream in = new FileInputStream(evilClassFile);
        ByteArrayOutputStream out = new ByteArrayOutputStream((int) evilClassFile.length());
        StreamUtils.copyInputStreamToOutputStream(in, out);
        out.close();
        final byte[] fileBatchJobClass = out.toByteArray();
        Class c = new ClassLoader() {
            Class initialize() {
                return defineClass(null, fileBatchJobClass,
                                   0, fileBatchJobClass.length);
            }
        }.initialize();
        FileBatchJob job;
        try {
            job = (FileBatchJob) c.newInstance();
        } catch (InstantiationException e) {
            throw new IOFailure("Unable to initialise class", e);
        } catch (IllegalAccessException e) {
            throw new IOFailure("Illegal access for class", e);
        }
        BatchStatus lbs = archive.batch(TestInfo.baAppId, job);
        assertEquals("Batch should have processed four files",
                     4, lbs.getNoOfFilesProcessed());
        List<File> failedFiles = new ArrayList<File>();
        failedFiles.addAll(lbs.getFilesFailed());
        File fileDir = new File(TestInfo.WORKING_DIR, "filedir").getCanonicalFile();
        CollectionAsserts.assertListEquals("Batch should have ",
                                           failedFiles,
                                           new File(fileDir, "fyensdk.arc"),
                                           new File(fileDir, "Upload3.ARC"));
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        lbs.appendResults(result);
        assertEquals("Batch should have written only the legal part",
                     "Legal\n", result.toString());
        FileAsserts.assertFileContains("fyensdk.arc must not have been changed",
                                       fyensdk,
                                       new File(TestInfo.WORKING_DIR, "fyensdk.arc"));
    }
    private BatchStatus assertBatchJobProcessesCorrectly(String message,
                                                         TestFileBatchJob job,
                                                         String... files) {
        BatchStatus lbs = archive.batch(TestInfo.baAppId, job);
        for (String file : files) {
            assertTrue(message + ": Should have processed file " + file
                    + " but only had " + job.processedFileList,
                    job.processedFileList.contains(file));
        }
        assertEquals(message + ": Should have processed exactly "
                + files.length + " files",
                files.length, job.processedFileList.size());
        return lbs;
    }



    class TestFileBatchJob extends FileBatchJob {
        boolean initialized;
        boolean finished;
        public List<String> processedFileList = new ArrayList<String>();

        public BatchFilter getFilter() {
            return BatchFilter.NO_FILTER;
        }

        public void initialize(OutputStream os) {
            initialized = true;
            try {
                os.write("Started batch job\n".getBytes());
            } catch (IOException e) {
                throw new IOFailure("Cannot write to OutputStream ", e);
            }
        }

        public boolean processFile(File file, OutputStream os) {
            processedFileList.add(file.getName());
            try {
                os.write(("Processed file " + file.getName()).getBytes());
            } catch (IOException e) {
                throw new IOFailure("Cannot write to OutputStream ", e);
            }
            return true;
        }

        public void finish(OutputStream os) {
            finished = true;
            try {
                os.write("Batch Job Finished".getBytes());
            } catch (IOException e) {
                throw new IOFailure("Cannot write to OutputStream ", e);
            }
        }
    }
}
