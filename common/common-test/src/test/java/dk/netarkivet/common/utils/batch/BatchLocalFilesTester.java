/*
 * #%L
 * Netarchivesuite - common - test
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
package dk.netarkivet.common.utils.batch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

/**
 * Unit test for BatchLocalFiles.
 */
@SuppressWarnings({"serial"})
public class BatchLocalFilesTester {
    // Reference to test files:
    private static final String INPUT_DIR = "tests/dk/netarkivet/arcutils/data/input/";
    private static final String[] TEST_FILE_NAMES = {"Reader1.cdx", "Reader2.cdx", "Reader3.cdx"};

    // The number of test files:
    private static final int FILES = TEST_FILE_NAMES.length;

    // For making a File[] out of TEST_FILE_NAMES:
    private File[] testFiles;

    // Own instance of BatchLocalARCFiles
    private BatchLocalFiles blf;

    // Method call counters:
    private int initialized;
    private int processed;
    private int finished;

    // Output stream for batch job
    OutputStream os;

    @Before
    public void setUp() throws Exception {
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
     * Tests ordinary, non-failing execution of a batch job.
     */
    @Test
    public void testOrdinaryRun() {
        blf.run(new TestBatchJob(), new ByteArrayOutputStream());
        assertEquals(1, initialized);
        assertEquals(FILES, processed);
        assertEquals(1, finished);
    }

    /**
     * Tests that a job throwing Exception during initialize() does not get executed, and that a representation of the
     * Exception is thrown.
     */
    @Test
    public void testOneJob_ExceptionInInitialize() {
        FileBatchJob job = new TestBatchJob() {
            public void initialize(OutputStream os) {
                throw new RuntimeException("testOneJob_ExceptionInInitialize");
            }
        };
        blf.run(job, os);
        assertEquals("Initialize should have failed before counting", 0, initialized);
        assertEquals("Failing initialize should have prevented processing", 0, processed);
        assertEquals("Finish should still be called after failed initiailize", 1, finished);
    }

    /**
     * Verifies that thrown Exceptions in process does not interrupt the processing but is caught and collected.
     */
    @Test
    public void testOneJob_ExceptionInProcess() {
        FileBatchJob job = new TestBatchJob() {
            private boolean done = false;

            public boolean processFile(File file, OutputStream os) {
                try {
                    if ((processed == 2) && !done) {
                        done = true;
                        throw new RuntimeException("testOneJob_ExceptionInProcess");
                    }
                } catch (RuntimeException e) {
                    addException(file, 0, 0, e);
                }
                return super.processFile(file, new ByteArrayOutputStream());
            }
        };
        blf.run(job, os);
        assertEquals("Should have called initialize", 1, initialized);
        assertEquals("Should have called process twice", 3, processed);
        assertEquals("Should have called finish", 1, finished);
        assertEquals("Should have one exception collected", 1, job.getExceptions().size());
    }

    /**
     * Verifies that an Exception thrown during finish() gets thrown properly.
     */
    @Test
    public void testOneJob_ExceptionInFinish() {
        FileBatchJob job = new TestBatchJob() {
            public void finish(OutputStream os) {
                try {
                    throw new RuntimeException("testOneJob_ExceptionInFinish");
                } catch (RuntimeException e) {
                    addException(new File("."), 0, 0, e);
                }
            }
        };
        blf.run(job, os);
        assertEquals("Should have called initialize", 1, initialized);
        assertEquals("Should have processed all files", FILES, processed);
        assertEquals("Should not have counted finish call", 0, finished);
        assertEquals("Should have one exception collected", 1, job.getExceptions().size());
    }

    /**
     * Verify that batch jobs sequentially does not disturb the results of the second job.
     */
    @Test
    public void testSequentialRuns() {
        testOrdinaryRun();
        initialized = 0;
        processed = 0;
        finished = 0;
        testOrdinaryRun();
    }

    /**
     * Verify that the job gets exposed to the right set of files, and only once to each. It is not a requirement that
     * the given order of the files is preserved.
     */
    @Test
    public void testFilesPresented() {
        FileBatchJob job = new TestBatchJob() {
            private Set<File> checkList = new HashSet<File>(Arrays.asList(testFiles));

            public boolean processFile(File file, OutputStream os) {
                boolean found = checkList.remove(file);
                assertTrue("Should have found " + file.toString(), found);
                return super.processFile(file, os);
            }

            public void finish(OutputStream os) {
                assertTrue("Expected empty list but found " + checkList.toString(), checkList.isEmpty());
                super.finish(new ByteArrayOutputStream());
            }
        };
        blf.run(job, os);
        assertEquals(1, initialized);
        assertEquals(FILES, processed);
        assertEquals(1, finished);
    }

    /**
     * Tests ordinary, non-failing execution of a batch job with a specified filename.
     */
    @Test
    public void testSpecifiedFilenameRun() {
        TestBatchJob tbj = new TestBatchJob();
        tbj.processOnlyFileNamed(TEST_FILE_NAMES[1]);
        blf.run(tbj, new ByteArrayOutputStream());
        assertEquals(1, initialized);
        assertEquals(1, processed);
        assertEquals(1, finished);
    }

    /**
     * A very simple FileBatchJob that simply counts relevant method calls in the parents class's designated fields.
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
