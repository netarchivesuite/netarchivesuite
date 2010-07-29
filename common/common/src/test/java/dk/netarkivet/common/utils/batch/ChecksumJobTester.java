/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2010 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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
package dk.netarkivet.common.utils.batch;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

import dk.netarkivet.common.utils.arc.TestInfo;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.utils.KeyValuePair;
import dk.netarkivet.common.utils.MD5;
import dk.netarkivet.common.utils.batch.BatchLocalFiles;
import dk.netarkivet.common.utils.batch.ChecksumJob;
import dk.netarkivet.testutils.Serial;
import dk.netarkivet.testutils.StringAsserts;
import dk.netarkivet.testutils.TestFileUtils;
import dk.netarkivet.testutils.preconfigured.MoveTestFiles;

/**
 * Unit tests for the class ChecksumJob.
 */
public class ChecksumJobTester extends TestCase {
    private MoveTestFiles mtf = new MoveTestFiles(TestInfo.ORIGINALS_DIR,
                                                 TestInfo.WORKING_DIR);

    public void setUp() throws Exception {
        super.setUp();
        mtf.setUp();
    }

    public void tearDown() throws Exception {
        mtf.tearDown();
        super.tearDown();
    }

    /**
     * Test that processFile correctly returns the checksums of the
     * files in the specified format.
     *
     * @throws IOException if unit test has trouble generating ref. checksums
     */
    public void testProcessFile() throws IOException {
        ChecksumJob job = new ChecksumJob();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        job.initialize(out);
        File[] inputfiles = new File(TestInfo.FAIL_ARCHIVE_DIR, "filedir")
            .listFiles(TestFileUtils.NON_CVS_DIRS_FILTER);
        Map<String,String> ourChecksums = new HashMap<String,String>();
        for (File inputfile : inputfiles) {
            job.processFile(inputfile, out);
            ourChecksums.put(inputfile.getName(),
                             MD5.generateMD5onFile(inputfile));
        }
        job.finish(out);

        // Check the results.
        String result = out.toString();
        String[] result_parts = result.split("\n");
        assertEquals("Must have as many lines in result as files processed."
                     + " Results is\n" + result + "\nOur checksums are \n"
                     + ourChecksums,
                     ourChecksums.size(), result_parts.length);
        for (String result_part : result_parts) {
            String[] line_parts = result_part.split(
                    ChecksumJob.STRING_FILENAME_SEPARATOR);
            assertEquals("Line in the checksum result must have two"
                         + " parts split by '"
                         + ChecksumJob.STRING_FILENAME_SEPARATOR + "', but was '"
                         + result_part + "' "
                         + " Results is\n" + result + "\nOur checksums are \n"
                         + ourChecksums,
                         2, line_parts.length);
            assertTrue("File '" + line_parts[0]
                       + "' from result must be in our checksum table."
                       + " Results is\n" + result + "\nOur checksums are \n"
                       + ourChecksums,
                       ourChecksums.containsKey(line_parts[0]));
            assertEquals("Checksum for '" + line_parts[0]
                         + "' should be correct"
                         + " Results is\n" + result + "\nOur checksums are \n"
                         + ourChecksums,
                         ourChecksums.get(line_parts[0]), line_parts[1]);
        }
    }

    /** Tests that makeLine works as expected. */
    public void testMakeLine() {
        assertEquals("Should generate lines as expected",
                     "A" + ChecksumJob.STRING_FILENAME_SEPARATOR + "b",
                     ChecksumJob.makeLine("A", "b"));
        try {
            ChecksumJob.makeLine(null, "a");
            fail("Should throw ANV on null parameter");
        } catch (ArgumentNotValid e) {
            //Expected
            StringAsserts.assertStringContains("Should mention parameter name",
                                               "filename", e.getMessage());
        }

        try {
            ChecksumJob.makeLine("a", null);
            fail("Should throw ANV on null parameter");
        } catch (ArgumentNotValid e) {
            //Expected
            StringAsserts.assertStringContains("Should mention parameter name",
                                               "checksum", e.getMessage());
        }

        try {
            ChecksumJob.makeLine("", "a");
            fail("Should throw ANV on empty parameter");
        } catch (ArgumentNotValid e) {
            //Expected
            StringAsserts.assertStringContains("Should mention parameter name",
                                               "filename", e.getMessage());
        }

        try {
            ChecksumJob.makeLine("a", "");
            fail("Should throw ANV on empty parameter");
        } catch (ArgumentNotValid e) {
            //Expected
            StringAsserts.assertStringContains("Should mention parameter name",
                                               "checksum", e.getMessage());
        }
    }

    public void testParseLine() {
        KeyValuePair<String, String> pair = ChecksumJob.parseLine(
                "a" + ChecksumJob.STRING_FILENAME_SEPARATOR + "b");
        assertEquals("Should get right key", "a", pair.getKey());
        assertEquals("Should get right value", "b", pair.getValue());

        try {
            ChecksumJob.parseLine(null);
            fail("Should throw ANV on null parameter");
        } catch (ArgumentNotValid e) {
            //Expected
            StringAsserts.assertStringContains("not on checksum output form",
                                               e.getMessage());
        }

        try {
            ChecksumJob.parseLine("x");
            fail("Should throw ANV on malformed parameter");
        } catch (ArgumentNotValid e) {
            //Expected
            StringAsserts.assertStringContains("not on checksum output form",
                                               e.getMessage());
        }

        try {
            ChecksumJob.parseLine("x" + ChecksumJob.STRING_FILENAME_SEPARATOR);
            fail("Should throw ANV on malformed parameter");
        } catch (ArgumentNotValid e) {
            //Expected
            StringAsserts.assertStringContains("not on checksum output form",
                                               e.getMessage());
        }

        try {
            ChecksumJob.parseLine("x" + ChecksumJob.STRING_FILENAME_SEPARATOR
                                  + ChecksumJob.STRING_FILENAME_SEPARATOR + "y");
            fail("Should throw ANV on malformed parameter");
        } catch (ArgumentNotValid e) {
            //Expected
            StringAsserts.assertStringContains("not on checksum output form",
                                               e.getMessage());
        }
    }

    /** Test that relevant state is preserved as expected, and that
     * extensions in readObject are honoured.
     *
     * @throws Exception on serialization trouble.
     */
    public void testSerializability() throws Exception {
        //make a job:
        ChecksumJob job = new ChecksumJob();

        //run the job to get some state
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        File[] inputfiles = new File(TestInfo.FAIL_ARCHIVE_DIR, "filedir")
            .listFiles(TestFileUtils.NON_CVS_DIRS_FILTER);
        inputfiles[0].setReadable(false);
        new BatchLocalFiles(inputfiles).run(job, out);

        //Serialize and deserialize
        ChecksumJob job2 = Serial.serial(job);

        //Finally, compare their visible states:
        assertEquals("After serialization state should be the same,"
                     + " but was befora:\n"
                     + relevantState(job) + "\n"
                     + "and after:"
                     + relevantState(job2),
                     relevantState(job), relevantState(job2));

        assertNotNull("Logger must be reinitialised after serialization",
                      job2.log);
    }

    private String relevantState(ChecksumJob job) {
        return "Checksum Job. Processed: " + job.getNoOfFilesProcessed()
               + " Failures: " + job.getFilesFailed()
                + " Pattern: " + job.getFilenamePattern().toString()
                + " Exceptions: " + job.getExceptions();
    }

}
