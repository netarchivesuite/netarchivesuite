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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.LogManager;

import junit.framework.TestCase;

import dk.netarkivet.common.Settings;
import dk.netarkivet.common.distribute.TestRemoteFile;
import dk.netarkivet.common.distribute.arcrepository.BatchStatus;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.PermissionDenied;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.arc.BatchFilter;
import dk.netarkivet.common.utils.arc.FileBatchJob;
import static dk.netarkivet.testutils.CollectionUtils.list;
import dk.netarkivet.testutils.FileAsserts;
import dk.netarkivet.testutils.preconfigured.UseTestRemoteFile;


/**
 * This class tests the bitarchive's batch() method.
 */
public class BitarchiveTesterBatch extends TestCase {
    private UseTestRemoteFile rf = new UseTestRemoteFile();

    static Bitarchive archive;

    /**
     * Construct a new tester object.
     */
    public BitarchiveTesterBatch(final String sTestName) {
        super(sTestName);
    }

    /**
     * At start of test, set up an archive we can run against.
     */
    public void setUp() throws PermissionDenied{
        FileUtils.removeRecursively(TestInfo.ARCHIVE_DIR);
        try {
            // This forces an emptying of the log file.
            FileInputStream fis = new FileInputStream(TestInfo.TESTLOGPROP);
            LogManager.getLogManager().readConfiguration(fis);
            fis.close();
        } catch (IOException e) {
            fail("Could not load the testlog.prop file: " + e);
        }
        FileUtils.createDir(TestInfo.ARCHIVE_DIR);
        Settings.set(Settings.REMOTE_FILE_CLASS, TestRemoteFile.class.getName());
        Settings.set(Settings.BITARCHIVE_SERVER_FILEDIR, TestInfo.ARCHIVE_DIR.getAbsolutePath());
        archive = Bitarchive.getInstance();
        for (String filename : TestInfo.arcFiles) {
            FileUtils.copyFile(new File(TestInfo.ORIGINALS_DIR, filename),
                    new File(new File(TestInfo.ARCHIVE_DIR, "filedir"), filename));
        }
        rf.setUp();
    }

    /**
     * At end of test, remove any files we managed to upload.
     */
    public void tearDown() {
        archive.close();
        FileUtils.removeRecursively(TestInfo.ARCHIVE_DIR);
        rf.tearDown();
        Settings.reload();
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
        try {
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
            fail("Failed to throw expected IOFailure");
        } catch (IOFailure e) {
            // expected
        }
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
                TestInfo.arcFiles.size(), job.processedFileList.size());
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
                TestInfo.ARCHIVE_SIZE, job.processedFileList.size());
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
