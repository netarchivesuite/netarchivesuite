/* $Id$
 * $Date$
 * $Revision$
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
package dk.netarkivet.common.utils.arc;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import junit.framework.TestCase;

/**
 */
public class BatchLocalFilesTester extends TestCase {
    //Reference to test files:
    private static final String INPUT_DIR = "tests/dk/netarkivet/arcutils/data/input/";
    private static final String[] TEST_FILE_NAMES = {
            "Reader1.cdx", "Reader2.cdx", "Reader3.cdx"
        };

    //The number of test files:
    private static final int FILES = TEST_FILE_NAMES.length;

    //For making a File[] out of TEST_FILE_NAMES:
    private File[] testFiles;

    //Own instance of BatchLocalARCFiles
    private BatchLocalFiles blf;

    //Method call counters:
    private int initialized;
    private int processed;
    private int finished;

   // Output stream for batch job
    OutputStream os;

    /**
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
        initialized = 0;
        processed = 0;
        finished = 0;
        testFiles = new File[TEST_FILE_NAMES.length];
        for (int i = 0; i < TEST_FILE_NAMES.length; i++) {
            testFiles[i] = new File(INPUT_DIR + TEST_FILE_NAMES[i]);
        }
        blf = new BatchLocalFiles(testFiles);
        os = new ByteArrayOutputStream();
    }
    /**
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception {
        super.tearDown();
    }
    /**
    * Tests ordinary, non-failing execution of a batch job.
    */
    public void testOrdinaryRun() {
        blf.run(new TestBatchJob(), new ByteArrayOutputStream() );
        assertEquals(1, initialized);
        assertEquals(FILES, processed);
        assertEquals(1, finished);
    }
    /**
    * Tests that a job throwing Exception during initialize()
    * does not get executed, and that a representation of the Exception is thrown.
    */
    public void testOneJob_ExceptionInInitialize() {
        FileBatchJob job = new TestBatchJob() {
                public void initialize(OutputStream os) {
                    throw new RuntimeException(
                        "testOneJob_ExceptionInInitialize");
                }
            };
        try {
            blf.run(job, os);
            fail("Should have thrown an Exception");
        } catch (Exception e) {
            assertEquals(0, initialized);
            assertEquals(0, processed);
            assertEquals(0, finished);
        }
    }
    /**
     * Verfies that thrown Exceptions in process interrupt the processing
     * rather than being caught
     */
    public void testOneJob_ExceptionInProcess() {
        FileBatchJob job = new TestBatchJob() {
                private boolean done = false;

                public boolean processFile(File file, OutputStream os) {
                    if ((processed == 2) && !done) {
                        done = true;
                        throw new RuntimeException(
                            "testOneJob_ExceptionInProcess");
                    }
                    return super.processFile(file, new ByteArrayOutputStream());
                }
            };
        try {
            blf.run(job, os);
            fail("Should have thrown an Exception");
        } catch (Exception e) {
            assertEquals(1, initialized);
            assertEquals(2, processed);
            assertEquals(0, finished);
        }
    }
    /**
    * Verifies that an Exception thrown during finish()
    * gets thrown properly.
    */
    public void testOneJob_ExceptionInFinish() {
        FileBatchJob job = new TestBatchJob() {
                public void finish(OutputStream os) {
                    throw new RuntimeException("testOneJob_ExceptionInFinish");
                }
            };
        try {
            blf.run(job, os);
            fail("Should have thrown an Exception");
        } catch (Exception e) {
            assertEquals(1, initialized);
            assertEquals(FILES, processed);
            assertEquals(0, finished);
        }
    }
    /**
     * Verify that batch jobs sequentially does not disturb the results
     * of the second job.
     */
    public void testSequentialRuns() {
        testOrdinaryRun();
        initialized = 0;
        processed = 0;
        finished = 0;
        testOrdinaryRun();
    }
    /**
     * Verify that the job gets exposed to the right set of files,
     * and only once to each. It is not a requirement that the
     * given order of the files is preserved.
     */
    public void testFilesPresented() {
        FileBatchJob job = new TestBatchJob() {
                private Set checkList = new HashSet(Arrays.asList(testFiles));

                public boolean processFile(File file, OutputStream os) {
                    boolean found = checkList.remove(file);
                    assertTrue("Should have found " + file.toString(),found);
                    return super.processFile(file, os);
                }
                public void finish() {
                    assertTrue("Expected empty list but found " +
                    checkList.toString(), checkList.isEmpty());
                    super.finish(new ByteArrayOutputStream());
                }
            };
        blf.run(job, os);
        assertEquals(1, initialized);
        assertEquals(FILES, processed);
        assertEquals(1, finished);
    }


    /**
    * Tests ordinary, non-failing execution of a batch job with a
    * specified filename.
    */
    public void testSpecifiedFilenameRun() {
        TestBatchJob tbj = new TestBatchJob();
        tbj.processOnlyFileNamed(TEST_FILE_NAMES[1]);
        blf.run(tbj, new ByteArrayOutputStream() );
        assertEquals(1, initialized);
        assertEquals(1, processed);
        assertEquals(1, finished);
    }

    /**
     * A very simple FileBatchJob that simply counts relevant
     * method calls in the parents class's designated fields.
     */
    private class TestBatchJob extends FileBatchJob {

       /**
         * Increases the initialized counter by 1.
         */
        public void initialize(OutputStream os) {
            initialized++;
        }
        /**
         * Increases the finished counter by 1.
         */
        public void finish(OutputStream os) {
            finished++;
        }
        /**
         * Increases the processed counter by 1.
         */
        public boolean processFile(File file, OutputStream os) {
            processed++;
            return true;
        }
    }
}
