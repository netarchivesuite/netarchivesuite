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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

import junit.framework.TestCase;
import org.archive.io.arc.ARCRecord;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.testutils.TestFileUtils;

/**
 * Unit tests for the class ARCBatchJob.
 */
public class ARCBatchJobTester extends TestCase {
    //Reference to test files:
    private static final File ARC_DIR = new File(
            "tests/dk/netarkivet/common/utils/arc/data/working/");
    private static final File ORIGINALS = new File(
            "tests/dk/netarkivet/common/utils/arc/data/input/");
    private static final File ARC_FILE = new File(ARC_DIR, "fyensdk.arc");
    private static final File ARC_GZ_FILE = new File(ARC_DIR, 
            "NetarchiveSuite-netarkivet.arc.gz");
    private static final File NON_EXISTENT_FILE
        = new File(ARC_DIR, "no_such_file.arc");

    //Feature of the last record of fyensdk.arc:
    private static final String LAST_URL = "http://www.fyens.dk/picturecache"
        + "/imageseries/getpicture.php?Width=100&pictureid=400";

    //Method call counters:
    private int processed;

    //Member for remembering which ShareableRecord was last processed:
    private String lastSeenURL;

    //For instigating Exceptions:
    private Exception testException = new Exception("Test-Exception");
    private static final int TOTAL_RECORDS = 190;
    private static final int RECORDS_PROCESSED_BEFORE_EXCEPTION = 180;

    /**
    * Utility method for printing Exception arrays on System.out.
    * @param es The Exception array to be printed.
    */
    private void printExceptions(Exception[] es) {
        if (es.length > 0) {
            System.out.println();
        }
        for (int i = 0; i < es.length; i++) {
            es[i].printStackTrace();
        }
    }
    /**
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
        FileUtils.removeRecursively(ARC_DIR);
        TestFileUtils.copyDirectoryNonCVS(ORIGINALS, ARC_DIR);
        processed = 0;
        //testFile = new File(ARC_DIR + ARC_FILE_NAME);
        //arcgzFile = new File(ARC_DIR + ARC_GZ_FILE_NAME);
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
        TestARCBatchJob job = new TestARCBatchJob();
        job.processFile(ARC_FILE, new ByteArrayOutputStream());
        Exception[] es = job.getExceptions();
        printExceptions(es);
        assertEquals("Should have processed 190 entries in normal run",
                TOTAL_RECORDS, processed);
        assertEquals("Should not have seen any exceptions in normal run",
                0, es.length);
    }

    /**
     * Verify that the given ShareableARCRecord contains the right record.
     */
    public void testContent() {
        TestARCBatchJob job = new TestARCBatchJob();
        job.processFile(ARC_FILE, new ByteArrayOutputStream());
        assertEquals("Should get the expected record last",
                LAST_URL, lastSeenURL);
    }

    /**
     * Verifies that thrown Exceptions in process get collected
     * TODO Check more error conditions -- the exception handling is tricky!
     */
    public void testOneJob_ExceptionInProcess() {
        ARCBatchJob job = new TestARCBatchJob() {
                public void processRecord(ARCRecord record, OutputStream os) {
                    super.processRecord(record, new ByteArrayOutputStream());
                    if (!((processed - 1) 
                            < RECORDS_PROCESSED_BEFORE_EXCEPTION)) {
                        throw new ArgumentNotValid(
                                "testOneJob_ExceptionInProcess");
                    }
                }
            };
        job.processFile(ARC_FILE, new ByteArrayOutputStream());
        Exception[] es = job.getExceptions();
        assertEquals("Should have gotten through all records",
                TOTAL_RECORDS, processed);
        final int numExceptions = TOTAL_RECORDS
            - RECORDS_PROCESSED_BEFORE_EXCEPTION;
        if (numExceptions != es.length) {
            printExceptions(es);
        }
        assertEquals("Exceptions list should have one entry per failing record",
                numExceptions, es.length);
        for (int i = 0; i < numExceptions; i++) {
            assertTrue("Exception should be of type ArgumentNotValid",
                    es[i] instanceof ArgumentNotValid);
        }
    }
    /**
     * Verifies that all possible filters are respected.
     */
    public void testFiltering() {
        /* We do not need to verify that BatchFilter.NO_FILTER is respected,
        * as this is done in testBatchARCFiles().
        */
        ARCBatchJob job = new TestARCBatchJob() {
                public BatchFilter getFilter() {
                    return BatchFilter.EXCLUDE_FILE_HEADERS;
                }
            };
        job.processFile(ARC_FILE, new ByteArrayOutputStream());
        Exception[] es = job.getExceptions();
        printExceptions(es);
        assertEquals("Should have processed all but one records",
                TOTAL_RECORDS - 1, processed);
        assertEquals("Filtered batch should not throw any exceptions",
                0, es.length);
    }
    public void testSequentialRuns() {
        testOrdinaryRun();
        processed = 0;
        testOrdinaryRun();
    }
    /**
     * Verify that ARCBatchJob objects can be serialized and deserialized
     * without harm.
     */
    public void testSerializability() {
        //Take two jobs: one for study and one for reference.
        SerializableARCBatchJob job1 = new SerializableARCBatchJob();
        SerializableARCBatchJob job2 = new SerializableARCBatchJob();

        //Work on both jobs ordinarily:
        job1.initialize(new ByteArrayOutputStream());
        job2.initialize(new ByteArrayOutputStream());
        doStuff(job1);
        doStuff(job2);
        //Now serialize and deserialize the studied job (but NOT the reference):
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream ous = new ObjectOutputStream(baos);
            ous.writeObject(job1);
            ous.close();
            baos.close();
            ObjectInputStream ois = new ObjectInputStream(
                    new ByteArrayInputStream(
                        baos.toByteArray()));
            job1 = (SerializableARCBatchJob) ois.readObject();
        } catch (IOException e) {
            fail(e.toString());
        } catch (ClassNotFoundException e) {
            fail(e.toString());
        }

        //Then, work on both jobs again (finishing them properly):
        doStuff(job1);
        doStuff(job2);
        job1.finish(new ByteArrayOutputStream());
        job2.finish(new ByteArrayOutputStream());
        //Finally, compare their visible states:
        Exception[] state1 = job1.getExceptions();
        Exception[] state2 = job2.getExceptions();
        assertEquals("The two jobs should have the same number of "
                + "registered exceptions.",
          state1.length, state2.length);
        for(int i = 0; i < state1.length; i++){
          assertEquals("Found differing registered exceptions: "
              + state1[i].toString() + state2[i].toString(),
            state1[i].toString(),
            state2[i].toString());
        }
    }
    /**
    * Makes the given job process a few null records and handle an Exception.
    * @param job the given job
    */
    private void doStuff(SerializableARCBatchJob job) {
        job.processRecord(null, new ByteArrayOutputStream());
        job.handleException(testException, new File("aFile"), 0L);
        job.processRecord(null, new ByteArrayOutputStream());
    }
    /**
     * Verify that we can also process arc.gz files.
     */
    public void testProcessCompressedFile() {
        TestARCBatchJob job = new TestARCBatchJob();
        job.processFile(ARC_GZ_FILE, new ByteArrayOutputStream());
        Exception[] es = job.getExceptions();
        printExceptions(es);
        assertEquals("Batching compressed file should give expected "
                + "number of records",
                66, processed);
        assertEquals("Batching compressed file should not throw exceptions",
                0, es.length);
    }

    /**
     *  Test failure mode when file does not exist.
     */
    public void testNonExistentFile() {
        TestARCBatchJob job = new TestARCBatchJob();
        boolean success =
                job.processFile(NON_EXISTENT_FILE, new ByteArrayOutputStream());
        assertEquals("Should record exactly one exception",
                job.getExceptions().length, 1);
        assertFalse("Should fail on missing file", success);
    }


    /**
     * A very simple ARCBatchJob that simply counts relevant
     * method calls in the parents class's designated fields.
     * It also exposes ARCBatchJob's internal list of Exceptions.
     */
    private class TestARCBatchJob extends ARCBatchJob {
        /**
         * @see ARCBatchJob#getFilter()
         * @return A filter that allows all records.
         */
        public BatchFilter getFilter() {
            return BatchFilter.NO_FILTER;
        }

        /**
          * Does nothing.
          */
        public void initialize(OutputStream os) {
        }
        /**
         * Does nothing.
         */
        public void finish(OutputStream os) {
        }
        /**
         * Increases the processed counter by 1
         * and records the record URL.
         */
        public void processRecord(ARCRecord record, OutputStream os) {
            processed++;
            lastSeenURL = record.getMetaData().getUrl();

        }
        public Exception[] getExceptions() {
            return super.getExceptions();
        }
    }

    private static class SerializableARCBatchJob extends ARCBatchJob {
        /**
         * @see ARCBatchJob#getFilter()
         * @return A filter that allows all records.
         */
        public BatchFilter getFilter() {
            return BatchFilter.NO_FILTER;
        }

        /**
         * Does nothing.
          */
        public void initialize(OutputStream os) {
        }
        /**
         * Does nothing.
         */
        public void finish(OutputStream os) {
        }
        /**
         * Does nothing.
         */
        public void processRecord(ARCRecord record, OutputStream os) {
        }

        public void handleException(Exception e, File arcfile, long index) {
            super.handleException(e, arcfile, index);
        }

        public Exception[] getExceptions() {
            return super.getExceptions();
        }
    }
}
