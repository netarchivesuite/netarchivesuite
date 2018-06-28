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
package dk.netarkivet.common.utils.warc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.List;

import org.archive.io.warc.WARCRecord;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.arc.ARCBatchJob;
import dk.netarkivet.common.utils.archive.ArchiveHeaderBase;
import dk.netarkivet.common.utils.archive.ArchiveRecordBase;
import dk.netarkivet.common.utils.archive.HeritrixArchiveRecordWrapper;
import dk.netarkivet.common.utils.batch.FileBatchJob;
import dk.netarkivet.common.utils.batch.WARCBatchFilter;
import dk.netarkivet.testutils.TestFileUtils;
import junit.framework.TestCase;

/**
 * Unit tests for the class WARCBatchJob.
 */
@SuppressWarnings({"serial"})
public class WARCBatchJobTester {

    // Reference to test files:
    private static final File WARC_DIR = new File("tests/dk/netarkivet/common/utils/warc/data/working/");
    private static final File ORIGINALS = new File("tests/dk/netarkivet/common/utils/warc/data/input/");
    private static final File WARC_FILE = new File(WARC_DIR, "fyensdk.warc");
    private static final File WARC_GZ_FILE = new File(WARC_DIR, "NetarchiveSuite-netarkivet.warc.gz");
    private static final File NON_EXISTENT_FILE = new File(WARC_DIR, "no_such_file.warc");

    // Feature of the last record of fyensdk.arc:
    private static final String LAST_URL = "http://www.fyens.dk/picturecache"
            + "/imageseries/getpicture.php?Width=100&pictureid=400";

    // Method call counters:
    private int processed;

    // Member for remembering which ShareableRecord was last processed:
    private String lastSeenURL;

    // For instigating Exceptions:
    private Exception testException = new Exception("Test-Exception");
    private static final int TOTAL_RECORDS = 191;
    private static final int RECORDS_PROCESSED_BEFORE_EXCEPTION = 180;

    /**
     * Utility method for printing Exception arrays on System.out.
     *
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
    @Before
    public void setUp() throws Exception {
        FileUtils.removeRecursively(WARC_DIR);
        TestFileUtils.copyDirectoryNonCVS(ORIGINALS, WARC_DIR);
        processed = 0;
        // testFile = new File(ARC_DIR + ARC_FILE_NAME);
        // arcgzFile = new File(ARC_DIR + ARC_GZ_FILE_NAME);
    }

    /**
     * Tests ordinary, non-failing execution of a batch job.
     */
    @Test
    public void testOrdinaryRun() {
        TestWARCBatchJob job = new TestWARCBatchJob();
        job.processFile(WARC_FILE, new ByteArrayOutputStream());
        Exception[] es = job.getExceptionArray();
        printExceptions(es);
        assertEquals("Should have processed 190 entries in normal run", TOTAL_RECORDS, processed);
        assertEquals("Should not have seen any exceptions in normal run", 0, es.length);
    }

    /**
     * Verify that the given ShareableARCRecord contains the right record.
     */
    @Test
    public void testContent() {
        TestWARCBatchJob job = new TestWARCBatchJob();
        job.processFile(WARC_FILE, new ByteArrayOutputStream());
        assertEquals("Should get the expected record last", LAST_URL, lastSeenURL);
    }

    /**
     * Verifies that thrown Exceptions in process get collected TODO Check more error conditions -- the exception
     * handling is tricky!
     */
    @Test
    public void testOneJob_ExceptionInProcess() {
        WARCBatchJob job = new TestWARCBatchJob() {
            public void processRecord(WARCRecord record, OutputStream os) {
                super.processRecord(record, new ByteArrayOutputStream());
                if (!((processed - 1) < RECORDS_PROCESSED_BEFORE_EXCEPTION)) {
                    throw new ArgumentNotValid("testOneJob_ExceptionInProcess");
                }
            }
        };
        job.processFile(WARC_FILE, new ByteArrayOutputStream());
        Exception[] es = job.getExceptionArray();
        assertEquals("Should have gotten through all records", TOTAL_RECORDS, processed);
        final int numExceptions = TOTAL_RECORDS - RECORDS_PROCESSED_BEFORE_EXCEPTION;
        if (numExceptions != es.length) {
            printExceptions(es);
        }
        assertEquals("Exceptions list should have one entry per failing record", numExceptions, es.length);
        for (int i = 0; i < numExceptions; i++) {
            assertTrue("Exception should be of type ArgumentNotValid", es[i] instanceof ArgumentNotValid);
        }
    }

    /**
     * Verifies that all possible filters are respected.
     */
    @Test
    public void testFiltering() {
        /*
         * We do not need to verify that BatchFilter.NO_FILTER is respected, as this is done in testBatchARCFiles().
         */
        WARCBatchJob job = new TestWARCBatchJob() {
            public WARCBatchFilter getFilter() {
                return WARCBatchFilter.EXCLUDE_NON_RESPONSE_RECORDS;
            }
        };
        job.processFile(WARC_FILE, new ByteArrayOutputStream());
        Exception[] es = job.getExceptionArray();
        printExceptions(es);
        assertEquals("Should have processed all but one records", TOTAL_RECORDS - 2, processed); // Minus
        // warcinfo
        // and
        // metadata
        assertEquals("Filtered batch should not throw any exceptions", 0, es.length);
    }

    @Test
    public void testSequentialRuns() {
        testOrdinaryRun();
        processed = 0;
        testOrdinaryRun();
    }

    /**
     * Verify that ARCBatchJob objects can be serialized and deserialized without harm.
     */
    @Test
    public void testSerializability() {
        // Take two jobs: one for study and one for reference.
        SerializableWARCBatchJob job1 = new SerializableWARCBatchJob();
        SerializableWARCBatchJob job2 = new SerializableWARCBatchJob();

        // Work on both jobs ordinarily:
        job1.initialize(new ByteArrayOutputStream());
        job2.initialize(new ByteArrayOutputStream());
        doStuff(job1);
        doStuff(job2);
        // Now serialize and deserialize the studied job (but NOT the
        // reference):
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream ous = new ObjectOutputStream(baos);
            ous.writeObject(job1);
            ous.close();
            baos.close();
            ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray()));
            job1 = (SerializableWARCBatchJob) ois.readObject();
        } catch (IOException e) {
            fail(e.toString());
        } catch (ClassNotFoundException e) {
            fail(e.toString());
        }

        // Then, work on both jobs again (finishing them properly):
        doStuff(job1);
        doStuff(job2);
        job1.finish(new ByteArrayOutputStream());
        job2.finish(new ByteArrayOutputStream());
        // Finally, compare their visible states:
        List<FileBatchJob.ExceptionOccurrence> state1 = job1.getExceptions();
        List<FileBatchJob.ExceptionOccurrence> state2 = job2.getExceptions();

        assertEquals("The two jobs should have the same number of " + "registered exceptions.", state1.size(),
                state2.size());
        for (int i = 0; i < state1.size(); i++) {
            assertEquals("Found differing registered exceptions: " + state1.get(i).toString()
                    + state2.get(i).toString(), state1.get(i).toString(), state2.get(i).toString());
        }
    }

    /**
     * Makes the given job process a few null records and handle an Exception.
     *
     * @param job the given job
     */
    private void doStuff(SerializableWARCBatchJob job) {
        job.processRecord(null, new ByteArrayOutputStream());
        job.handleException(testException, new File("aFile"), 0L);
        job.processRecord(null, new ByteArrayOutputStream());
    }

    /**
     * Verify that we can also process arc.gz files. FIXME Broken by http://sbforge.org/jira/browse/NAS-1918
     */
    @Test
    @Ignore("Broken by http://sbforge.org/jira/browse/NAS-1918")
    public void failstestProcessCompressedFile() {
        TestWARCBatchJob job = new TestWARCBatchJob();
        job.processFile(WARC_GZ_FILE, new ByteArrayOutputStream());
        Exception[] es = job.getExceptionArray();
        printExceptions(es);
        assertEquals("Batching compressed file should give expected " + "number of records", 66, processed);
        assertEquals("Batching compressed file should not throw exceptions", 0, es.length);
    }

    /**
     * Test failure mode when file does not exist.
     */
    @Test
    public void testNonExistentFile() {
        TestWARCBatchJob job = new TestWARCBatchJob();
        boolean success = job.processFile(NON_EXISTENT_FILE, new ByteArrayOutputStream());
        assertEquals("Should record exactly one exception", job.getExceptionArray().length, 1);
        assertFalse("Should fail on missing file", success);
    }

    /**
     * A very simple ARCBatchJob that simply counts relevant method calls in the parents class's designated fields. It
     * also exposes ARCBatchJob's internal list of Exceptions.
     */
    private class TestWARCBatchJob extends WARCBatchJob {
        /**
         * @return A filter that allows all records.
         * @see ARCBatchJob#getFilter()
         */
        public WARCBatchFilter getFilter() {
            return WARCBatchFilter.NO_FILTER;
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
         * Increases the processed counter by 1 and records the record URL.
         */
        public void processRecord(WARCRecord wr, OutputStream os) {
            processed++;
            ArchiveRecordBase record = new HeritrixArchiveRecordWrapper(wr);
            ArchiveHeaderBase header = record.getHeader();
            lastSeenURL = header.getUrl();

        }

        public Exception[] getExceptionArray() {
            return super.getExceptionArray();
        }

    }

    private static class SerializableWARCBatchJob extends WARCBatchJob {
        /**
         * @return A filter that allows all records.
         * @see ARCBatchJob#getFilter()
         */
        public WARCBatchFilter getFilter() {
            return WARCBatchFilter.NO_FILTER;
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
        public void processRecord(WARCRecord record, OutputStream os) {
        }

        public void handleException(Exception e, File arcfile, long index) {
            super.handleException(e, arcfile, index);
        }

        public Exception[] getExceptionArray() {
            return super.getExceptionArray();
        }
    }

}
