/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
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
package dk.netarkivet.archive.arcrepository.bitpreservation;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.MD5;
import dk.netarkivet.testutils.TestFileUtils;

/**
 * Unit tests for the class ChecksumJob.
 */
public class ChecksumJobTester extends TestCase {
    public ChecksumJobTester(String s) {
        super(s);
    }

    public void setUp() {
        TestFileUtils.copyDirectoryNonCVS(TestInfo.ORIGINALS_DIR,
                TestInfo.WORKING_DIR);
    }

    public void tearDown() {
        FileUtils.removeRecursively(TestInfo.WORKING_DIR);
    }

    /**
     * Test that processFile correctly returns the checksums of the
     * files in the specified format (which sucks).
     *
     * @throws Exception
     */
    public void testProcessFile() throws Exception {
        ChecksumJob job = new ChecksumJob();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        job.initialize(out);
        File[] inputfiles = new File(TestInfo.FAIL_ARCHIVE_DIR, "filedir")
            .listFiles(TestFileUtils.NON_CVS_DIRS_FILTER);
        Map<String,String> ourChecksums = new HashMap<String,String>();
        for (int i = 0; i < inputfiles.length; i++) {
            job.processFile(inputfiles[i], out);
            try {
                ourChecksums.put(inputfiles[i].getName(),
                        MD5.generateMD5onFile(inputfiles[i]));
            } catch(Exception e) {
                //okay, should just be a failed file
            }
        }
        job.finish(out);
        out.close();

        // Cannot check # of files processed, as that is counted outside
        // processFile now.  So just check the results.

        String result = out.toString();
        String[] result_parts = result.split("\n");
        assertEquals("Must have as many lines in result as files processed",
                ourChecksums.size(), result_parts.length);
        for (int i = 0; i < result_parts.length; i++) {
            String[] line_parts = result_parts[i].split(
                    dk.netarkivet.archive.arcrepository.bitpreservation.Constants.STRING_FILENAME_SEPARATOR);
            assertEquals("Line " + i + " in the checksum result must have two parts split by "
                    + dk.netarkivet.archive.arcrepository.bitpreservation.Constants.STRING_FILENAME_SEPARATOR,
                    2, line_parts.length);
            assertTrue("File '" + line_parts[0] + "' must be in our checksum table",
                    ourChecksums.containsKey(line_parts[0]));
            assertEquals("Checksum for '" + line_parts[0] + "' should be correct",
                    ourChecksums.get(line_parts[0]), line_parts[1]);
        }
    }

    public void testSerializability() throws IOException,
                                             ClassNotFoundException {
        //make a job:
        ChecksumJob job = new ChecksumJob();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        job.initialize(out);
        File[] inputfiles = new File(TestInfo.FAIL_ARCHIVE_DIR, "filedir")
            .listFiles(TestFileUtils.NON_CVS_DIRS_FILTER);
        Map<String,String> ourChecksums = new HashMap<String,String>();
        for (int i = 0; i < inputfiles.length; i++) {
            job.processFile(inputfiles[i], out);
            ourChecksums.put(inputfiles[i].getName(), MD5.generateMD5onFile(
                    inputfiles[i]));
        }
        job.finish(out);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream ous = new ObjectOutputStream(baos);
        ous.writeObject(job);
        ous.close();
        baos.close();
        ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(
                baos.toByteArray()));
        ChecksumJob job2;
        job2 = (ChecksumJob) ois.readObject();
        //Finally, compare their visible states:
        assertEquals("After serialization the states differed:\n"
                     + relevantState(job) + "\n"
                     + relevantState(job2),
                     relevantState(job), relevantState(job2));

        assertNotNull("Logget must be reinitialised after serialization",
                      job2.log);
    }

    private String relevantState(ChecksumJob job) {
        return "Job. processed:" + job.getNoOfFilesProcessed()
               + "Failures" + job.getFilesFailed();
    }

}
