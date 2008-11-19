/*$Id$
* $Revision$
* $Date$
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
package dk.netarkivet.common.utils.cdx;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import junit.framework.TestCase;

import org.archive.io.ArchiveReader;
import org.archive.io.ArchiveReaderFactory;
import org.archive.io.ArchiveRecord;

import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.batch.BatchLocalFiles;
import dk.netarkivet.common.utils.batch.FileBatchJob.ExceptionOccurrence;

/**
 * Test class used for investigating CDX generation from WARC-files 
 * using a modified ExtractCDXJob class.
 * 
 */
public class ExtractCDXFromWarcJobTester extends TestCase {
    
    //Shared instance of BatchLocalFiles
    private BatchLocalFiles warcBlaf;
    
    //Our main instance of ExtractCDXFromWarcJob:
    private ExtractCDXFromWarcJob warcJob;
    
    //A useful counter:
    private int processed;

    /**
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
        processed = 0;
        warcBlaf = new BatchLocalFiles(new File[]{
                TestInfo.WARC_FILE1,
                TestInfo.WARC_FILE2,
                TestInfo.WARC_FILE3,
        });
 
        FileUtils.createDir(TestInfo.CDX_DIR);
        
    }

    protected void tearDown() {
        FileUtils.removeRecursively(TestInfo.CDX_DIR);
    }

    /**
     * Verify that construction succeeds regardless of the parameters.
     */
    public void testConstructor() {
        warcJob = new ExtractCDXFromWarcJob();
        warcJob = new ExtractCDXFromWarcJob(true);
        warcJob = new ExtractCDXFromWarcJob(false);
    }

    /**
     * Verify that the job runs without problems and visits all relevant
     * records.
     */
    public void testRun() throws IOException {
        warcJob = new ExtractCDXFromWarcJob() {
            public void processRecord(ArchiveRecord sar, OutputStream os) {
                super.processRecord(sar, new ByteArrayOutputStream());
                processed++;
            }
        };
        OutputStream os = new FileOutputStream(TestInfo.TEMP_FILE);
        warcBlaf.run(warcJob, os);
        os.close();
        Exception[] es = warcJob.getExceptionArray();
        printExceptions(es);
        assertEquals("No exceptions should be thrown", 0, es.length);
        assertEquals("The correct number of records should be processed",
                     TestInfo.NUM_RECORDS, processed);
    }
    
    public void testExtractCDXJobWithWarcfilesExcludeChecsum() throws Exception {
        warcJob = new ExtractCDXFromWarcJob(false);
        OutputStream os = new ByteArrayOutputStream();
        assertFalse("The to-be-generated file should not exist aforehand",
                    TestInfo.CDX_FILE.exists());
        os = new FileOutputStream(TestInfo.CDX_FILE);
        warcBlaf.run(warcJob, os);
        os.close();
        List<ExceptionOccurrence> exceptions = warcJob.getExceptions();
        for (ExceptionOccurrence eo : exceptions) {
            System.out.println("Exception: " + eo.getException());
        }
        assertTrue(warcJob.getExceptions().isEmpty());
        
        System.out.println(FileUtils.readFile(TestInfo.CDX_FILE));
    }

    public void testExtractCDXJobWithWarcfilesIncludeChecksum() throws Exception {
        warcJob = new ExtractCDXFromWarcJob(true);
        OutputStream os = new ByteArrayOutputStream();
        assertFalse("The to-be-generated file should not exist aforehand",
                    TestInfo.CDX_FILE.exists());
        os = new FileOutputStream(TestInfo.CDX_FILE);
        warcBlaf.run(warcJob, os);
        os.close();
        List<ExceptionOccurrence> exceptions = warcJob.getExceptions();
        for (ExceptionOccurrence eo : exceptions) {
            System.out.println("Exception: " + eo.getException());
        }
        //assertFalse(warcJob.getExceptions().isEmpty());
        
        System.out.println(FileUtils.readFile(TestInfo.CDX_FILE));
    }
    
    
    
    public void testWarcIteration() throws Exception {
        warcJob = new ExtractCDXFromWarcJob() {
            public void processRecord(ArchiveRecord sar, OutputStream os) {
                super.processRecord(sar, new ByteArrayOutputStream());
                processed++;
            }
        };
        OutputStream os = new ByteArrayOutputStream();
        assertFalse("The to-be-generated file should not exist aforehand",
                    TestInfo.CDX_FILE.exists());
        os = new FileOutputStream(TestInfo.CDX_FILE);
        warcBlaf.run(warcJob, os);
        os.close();
        
    }
    
    /**
     * Test whether the class is really Serializable.
     */
    public void testSerializability()
            throws IOException, ClassNotFoundException {
        //Take two jobs: one for study and one for reference.
        ExtractCDXFromWarcJob job1 = new StubbornJob();
        ExtractCDXFromWarcJob job2 = new StubbornJob();
        //Now serialize and deserialize the studied job (but NOT the reference):
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream ous = new ObjectOutputStream(baos);
        ous.writeObject(job1);
        ous.close();
        baos.close();
        ObjectInputStream ois = new ObjectInputStream(
                new ByteArrayInputStream(baos.toByteArray()));
        job1 = (ExtractCDXFromWarcJob) ois.readObject();
        //Finally, compare their outputs:
        ByteArrayOutputStream baos1 = new ByteArrayOutputStream();
        ByteArrayOutputStream baos2 = new ByteArrayOutputStream();
        //Run both jobs ordinarily:
        warcBlaf.run(job1, baos1);
        warcBlaf.run(job2, baos2);
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

    public void testWarcReading() throws Exception{     
    
        ArchiveReader archiveReader = ArchiveReaderFactory.get(TestInfo.WARC_FILE1);
        
        Iterator<? extends ArchiveRecord> it = archiveReader.iterator();
        assertTrue("Warc should contains records", it.hasNext());
        while (it.hasNext()) {
            ArchiveRecord next = it.next();
            System.out.println("mimetype:" + next.getHeader().getMimetype());
            System.out.println("url:" + next.getHeader().getUrl());
        }
    }
    
    
    /**
     * A class used in testing Serializability. For this test, we need a job
     * that doesn't finish until asked twice
     */
    private static class StubbornJob extends ExtractCDXFromWarcJob {
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