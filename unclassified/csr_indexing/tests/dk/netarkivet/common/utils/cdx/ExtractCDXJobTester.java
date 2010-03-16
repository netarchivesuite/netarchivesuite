/*$Id$
* $Revision$
* $Date$
* $Author$
*
* The Netarchive Suite - Software to harvest and preserve websites
* Copyright 2004-2009 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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
package dk.netarkivet.common.utils.cdx;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.util.Arrays;

import junit.framework.TestCase;

import org.archive.io.arc.ARCRecord;

import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.batch.BatchLocalFiles;

public class ExtractCDXJobTester extends TestCase {
    
    //Shared instance of BatchLocalFiles
    private BatchLocalFiles arcBlaf;
        
    //Our main instance of ExtractCDXJob:
    private ExtractCDXJob job;

    //A useful counter:
    private int processed;

    /**
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
        processed = 0;
        arcBlaf = new BatchLocalFiles(new File[]{TestInfo.ARC_FILE1,
                new File(TestInfo.ARC_DIR, "input-2.arc"),
                new File(TestInfo.ARC_DIR, "input-3.arc")});
        FileUtils.createDir(TestInfo.CDX_DIR);        
    }

    protected void tearDown() {
        FileUtils.removeRecursively(TestInfo.CDX_DIR);
    }

    /**
     * Verify that construction succeeds regardless of the parameters.
     */
    public void testConstructor() {
        job = new ExtractCDXJob();
        job = new ExtractCDXJob(true);
        job = new ExtractCDXJob(false);
    }

    /**
     * Verify that the job runs without problems and visits all relevant
     * records.
     */
    public void testRun() throws IOException {
        job = new ExtractCDXJob() {
            public void processRecord(ARCRecord sar, OutputStream os) {
                super.processRecord(sar, new ByteArrayOutputStream());
                processed++;
            }
        };
        OutputStream os = new FileOutputStream(TestInfo.TEMP_FILE);
        arcBlaf.run(job, os);
        os.close();
        Exception[] es = job.getExceptionArray();
        printExceptions(es);
        assertEquals("No exceptions should be thrown", 0, es.length);
        assertEquals("The correct number of records should be processed",
                     TestInfo.NUM_RECORDS, processed);
    }

    /**
     * Test the output of CDX data. It is not a requirement that this operation
     * can be performed several times in a row; the job is allowed to let go of
     * the CDX data after successfully writing it out.
     */
    public void testDumpCDX() throws IOException {
        job = new ExtractCDXJob();
        OutputStream os = new ByteArrayOutputStream();
        assertFalse("The to-be-generated file should not exist aforehand",
                    TestInfo.CDX_FILE.exists());
        os = new FileOutputStream(TestInfo.CDX_FILE);
        arcBlaf.run(job, os);
        Exception[] es = job.getExceptionArray();
        os.close();
        //TODO: Should test for length or content instead:
        assertTrue("The generated file must exist", TestInfo.CDX_FILE.exists());
        String tpath = FileUtils.readFile(TestInfo.CORRECT_CDX_FILE);
        BufferedReader expectedReader = new BufferedReader(
                new StringReader(tpath));
        BufferedReader resultsReader = new BufferedReader(
                new FileReader(TestInfo.CDX_FILE));
        String s;
        while ((s = expectedReader.readLine()) != null) {
            assertEquals("The contents of the file should be correct", s,
                         resultsReader.readLine());
        }
        expectedReader.close();
        resultsReader.close();
    }
    
    /*
     * The CDX content itself is not tested. The requirement on that is that it
     * is compatible with existing tools. There is an external test for that.
     */
    public void testMain() throws IOException {
        File[] arcFiles = new File[]{TestInfo.ARC_FILE1};

        ExtractCDXJob job = new ExtractCDXJob();
        BatchLocalFiles blaf = new BatchLocalFiles(arcFiles);
        blaf.run(job, new ByteArrayOutputStream());

        assertEquals("No exceptions expected", 0,
                job.getExceptionArray().length);
    }

    /**
     * Test whether the class is really Serializable.
     */
    public void testSerializability()
            throws IOException, ClassNotFoundException {
        //Take two jobs: one for study and one for reference.
        ExtractCDXJob job1 = new StubbornJob();
        ExtractCDXJob job2 = new StubbornJob();
        //Now serialize and deserialize the studied job (but NOT the reference):
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream ous = new ObjectOutputStream(baos);
        ous.writeObject(job1);
        ous.close();
        baos.close();
        ObjectInputStream ois = new ObjectInputStream(
                new ByteArrayInputStream(baos.toByteArray()));
        job1 = (ExtractCDXJob) ois.readObject();
        //Finally, compare their outputs:
        ByteArrayOutputStream baos1 = new ByteArrayOutputStream();
        ByteArrayOutputStream baos2 = new ByteArrayOutputStream();
        //Run both jobs ordinarily:
        arcBlaf.run(job1, baos1);
        arcBlaf.run(job2, baos2);
        baos1.close();
        baos2.close();

        byte[] b1 = baos1.toByteArray();
        byte[] b2 = baos2.toByteArray();
        for (int i = 0; i < b1.length; ++i) {
            if (b1[i] != b2[i]) {
                System.out.println("b1=" + b1[i]);
                System.out.println("b2=" + b2[i]);
                fail("\nDifference at position " + i);
            } else {
                //System.out.println(b1[i]);
            }
        }
        assertTrue("Output from cdx jobs should be the same",
                   Arrays.equals(baos1.toByteArray(), baos2.toByteArray()));
    }
    
    /**
     * A class used in testing Serializability. For this test, we need a job
     * that doesn't finish until asked twice
     */
    private static class StubbornJob extends ExtractCDXJob {
        boolean askedBefore = false;

        public void finish(OutputStream os) {
            if (askedBefore) {
                super.finish(os);
            } else {
                askedBefore = true;
            }
        }
    }

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
     * Helper method (no longer used).
     * @param psWord
     * @param psReplace
     * @param psNewSeg
     * @return
     */
    public static String replace(String psWord, String psReplace,
            String psNewSeg) {
        StringBuffer lsNewStr = new StringBuffer();
        int liFound = 0;
        int liLastPointer = 0;

        do {

            liFound = psWord.indexOf(psReplace, liLastPointer);

            if (liFound < 0) {
                lsNewStr.append(psWord
                        .substring(liLastPointer, psWord.length()));
            } else {

                if (liFound > liLastPointer) {
                    lsNewStr.append(psWord.substring(liLastPointer, liFound));
                }

                lsNewStr.append(psNewSeg);
                liLastPointer = liFound + psReplace.length();
            }

        } while (liFound > -1);

        return lsNewStr.toString();
    }
}