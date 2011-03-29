/*$Id$
* $Revision$
* $Date$
* $Author$
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
package dk.netarkivet.common.utils.arc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Date;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.TestCase;

import org.archive.io.ArchiveReader;
import org.archive.io.ArchiveRecord;
import org.archive.io.ArchiveRecordHeader;
import org.archive.io.arc.ARCReader;
import org.archive.io.arc.ARCReaderFactory;
import org.archive.io.arc.ARCRecord;
import org.archive.io.arc.ARCWriter;
import org.archive.util.ArchiveUtils;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.testutils.FileAsserts;
import dk.netarkivet.testutils.TestFileUtils;

/**
 * Tests for class ARCUtils.
 */
public class ARCUtilsTester extends TestCase {
    
    private static File OUTFILE_ARC = new File(TestInfo.WORKING_DIR, "outFile.arc");
    private static File OUTFILE1_ARC = new File(TestInfo.WORKING_DIR, "outFile1.arc");
    private static File EMPTY_ARC = new File(TestInfo.WORKING_DIR, "input-empty.arc");
    private static File NONARC_ARC = new File(TestInfo.WORKING_DIR, "input-nonarc.arc");
    private static File INPUT_1_ARC= new File(TestInfo.WORKING_DIR, "input-1.arc");
    private static File INPUT_2_ARC= new File(TestInfo.WORKING_DIR, "input-2.arc");
    private static File INPUT_3_ARC= new File(TestInfo.WORKING_DIR, "input-3.arc");
    

    public ARCUtilsTester(String s) {
        super(s);
    }

    public void setUp() throws Exception {
        TestFileUtils.copyDirectoryNonCVS(TestInfo.ORIGINALS_DIR,
                TestInfo.WORKING_DIR);
        super.setUp();
    }

    public void tearDown() throws Exception {
        super.tearDown();
        FileUtils.removeRecursively(TestInfo.WORKING_DIR);
    }

    /** Test that the insertARCFile method inserts the expected things.
     *
     * @throws Exception
     */
    public void testInsertARCFile() throws Exception {
        // Test illegal arguments first.
        try {
            ARCUtils.insertARCFile(TestInfo.WORKING_DIR, null);
            fail("Should get ArgumentNotValid on null writer");
        } catch (ArgumentNotValid e) {
            // Expected
        }

        File outFile = OUTFILE_ARC;
        PrintStream stream = new PrintStream(outFile);
        ARCWriter writer = getTestARCWriter(stream, outFile);
        FileAsserts.assertFileNumberOfLines("Should just have filedesc",
                outFile, TestInfo.LINES_IN_FILEDESC);
        try {
            ARCUtils.insertARCFile(null, writer);
            fail("Should get ArgumentNotValid on null writer");
        } catch (ArgumentNotValid e) {
            // Expected
        }
        writer.close();
        FileAsserts.assertFileNumberOfLines("Should just have filedesc",
                outFile, TestInfo.LINES_IN_FILEDESC);

        stream = new PrintStream(outFile);
        writer = getTestARCWriter(stream, outFile);
        try {
            ARCUtils.insertARCFile(TestInfo.WORKING_DIR, writer);
            fail("Should get IOFailure when trying to read directory");
        } catch (IOFailure e) {
            // Expected
        }
        writer.close();
        FileAsserts.assertFileNumberOfLines("Should just have filedesc",
                outFile, TestInfo.LINES_IN_FILEDESC);

        stream = new PrintStream(outFile);
        writer = getTestARCWriter(stream, outFile);
        try {
            ARCUtils.insertARCFile(
                    new File(TestInfo.WORKING_DIR, "does_not_exist"), writer
            );
            fail("Should get IOFailure when trying to read missing file");
        } catch (IOFailure e) {
            // Expected
        }
        writer.close();
        FileAsserts.assertFileNumberOfLines("Should just have filedesc",
                outFile, TestInfo.LINES_IN_FILEDESC);

        stream = new PrintStream(outFile);
        writer = getTestARCWriter(stream, outFile);
        try {
            ARCUtils.insertARCFile(EMPTY_ARC, writer);
            fail("Should not accept empty file");
        } catch (IOFailure e) {
            // Expected
        }
        writer.close();
        FileAsserts.assertFileNumberOfLines("Should just have filedesc",
                outFile, TestInfo.LINES_IN_FILEDESC);

        stream = new PrintStream(outFile);
        writer = getTestARCWriter(stream, outFile);
        try {
            ARCUtils.insertARCFile(NONARC_ARC, writer);
            fail("Should not accept non-arc file");
        } catch (IOFailure e) {
            // Expected
        }
        writer.close();
        FileAsserts.assertFileNumberOfLines("Should just have filedesc",
                outFile, TestInfo.LINES_IN_FILEDESC);

        stream = new PrintStream(outFile);
        writer = getTestARCWriter(stream, outFile);
        try {
            ARCUtils.insertARCFile(
                    new File(TestInfo.WORKING_DIR, "input-illegal.arc"), writer
            );
            fail("Should not accept arc file w/o filedesc: header");
        } catch (IOFailure e) {
            // Expected
        }
        writer.close();
        FileAsserts.assertFileNumberOfLines("Should just have filedesc",
                outFile, TestInfo.LINES_IN_FILEDESC);

        stream = new PrintStream(outFile);
        writer = getTestARCWriter(stream, outFile);
        ARCUtils.insertARCFile(INPUT_2_ARC, writer);
        writer.close();
        FileAsserts.assertFileNumberOfLines("Should just have filedesc",
                outFile, TestInfo.LINES_IN_FILEDESC);

        stream = new PrintStream(outFile);
        writer = getTestARCWriter(stream, outFile);
        ARCUtils.insertARCFile(INPUT_1_ARC, writer);
        writer.close();
        FileAsserts.assertFileNumberOfLines("Should have filedesc and entries",
                outFile, TestInfo.LINES_IN_FILEDESC
                + TestInfo.NON_FILEDESC_LINES_IN_INPUT_1);
        
        // Test adding more than once
        stream = new PrintStream(outFile);
        writer = getTestARCWriter(stream, outFile);
        ARCUtils.insertARCFile(INPUT_1_ARC, writer);
        ARCUtils.insertARCFile(INPUT_3_ARC, writer);
        ARCUtils.insertARCFile(INPUT_1_ARC, writer);
        writer.close();
        FileAsserts.assertFileNumberOfLines("Should have filedesc and entries",
                outFile, TestInfo.LINES_IN_FILEDESC
                + TestInfo.NON_FILEDESC_LINES_IN_INPUT_1
                + TestInfo.NON_FILEDESC_LINES_IN_INPUT_3
                + TestInfo.NON_FILEDESC_LINES_IN_INPUT_1);
        
        // verify thar insertARCFile inserts correct metadata for the Records contained in the ARCRecord
        stream = new PrintStream(outFile);
        writer = getTestARCWriter(stream, outFile);
        ARCUtils.insertARCFile(INPUT_3_ARC, writer);
        writer.close();
        // outFile should now contain one record with the following header-line:
        //   http://www.pligtaflevering.dk/space.gif 130.226.231.6 20060203105426 image/gif 301
        ArchiveRecord ar = findFirstRecordWithUri(outFile, 
                "http://www.pligtaflevering.dk/space.gif");
        if (ar == null) {
            fail("required record not found");
        }
        assertEquals("inserted record has wrong date in header", "20060203105426", ar.getHeader().getDate());
    }

    private ArchiveRecord findFirstRecordWithUri(File f, String uri) 
    throws IOException {
        
        ArchiveReader r = ARCReaderFactory.get(f);
        
        Iterator<ArchiveRecord> it = r.iterator();
        ArchiveRecord record = it.next(); //Skip ARC file header
        // ARCReaderFactory guarantees the first record exists and is a
        // filedesc, or it would throw exception
        
        // next record should contain INPUT_1_ARC
        while (it.hasNext()) {
            record = it.next();
            if (record.getHeader().getUrl().equals(uri)){
                return record;
            }
        }
        return null;
    }
   
    
    
    /**
     * Test, that ARCUtils#writeFileToARC() works
     * Test, that bug 914 is fixed.
     * @throws Exception If file not found
     */
    public void testWriteFileToARC() throws Exception {
        File outFile = OUTFILE_ARC;
        PrintStream stream = new PrintStream(outFile);
        ARCWriter aw = getTestARCWriter(stream,outFile);
        ARCUtils.writeFileToARC(aw, INPUT_1_ARC, "metadata://tests.netarkivet.dk","text/plain");
        aw.close();
        if (stream != null) {
            stream.close();
        }
        // retrieve inserted record
        ARCReader r = ARCReaderFactory.get(OUTFILE_ARC);
        Iterator<ArchiveRecord> it = r.iterator();
        ARCRecord record = (ARCRecord) it.next(); //Skip ARC file header
        // ARCReaderFactory guarantees the first record exists and is a
        // filedesc, or it would throw exception
        
        // next record should contain INPUT_1_ARC
        if (!it.hasNext()) {
            r.close();
            fail("Should contain more than the header record");
        }
        
        record = (ARCRecord) it.next();
        // ensure that the date retrieved from the header is equivalent to the lastmodified value of the inserted file
        // Note: the date is written in IA_notation: 
        long lastmodified = INPUT_1_ARC.lastModified();
        assertTrue("Lastmodified of inserted file should be same as getHeader().getDate()",
        new Date(lastmodified).equals(
                ArchiveUtils.getDate(record.getHeader().getDate())));
        
        //Verify, that payload object is identical with INPUT_1_ARC
        OutputStream os = new FileOutputStream(OUTFILE1_ARC);
        record.dump(os);
        assertEquals("inserted record should have same size as dumped record", OUTFILE1_ARC.length(), INPUT_1_ARC.length());
        assertTrue("inserted record should have same contents as dumped record", FileUtils.readFile(OUTFILE1_ARC.getAbsoluteFile()).equals(
                FileUtils.readFile(INPUT_1_ARC.getAbsoluteFile())));
    }

    /**
     * Test, that ARCFile has correct header including correct date 
     * (Bug 987).
     * @throws Exception
     */
    public void testCreateARCWriter() throws Exception {
        ARCWriter aw = ARCUtils.createARCWriter(OUTFILE_ARC);
        aw.close();
        ARCReader r = ARCReaderFactory.get(OUTFILE_ARC);
        ArchiveRecordHeader arh = r.iterator().next().getHeader();
        assertFalse("Date in ARC header should never be the string 'null'", arh.getDate().equals("null"));
        //todo: check that written date is close to current date
        assertTrue("Header-mimetype should be text/plain", arh.getMimetype().equals("text/plain"));
    }

    /**
     * Tests, that ARCUtils.getToolsARCWriter create a proper arcfile with
     * correct header information.
     * @throws Exception
     */
    public void testCreateToolsARCWriter() throws Exception {
        File outFile = OUTFILE_ARC;
        PrintStream stream = new PrintStream(outFile);
        ARCWriter aw = ARCUtils.getToolsARCWriter(stream, OUTFILE_ARC);
        //ARCWriter aw = ARCUtils.getToolsARCWriter(System.out, OUTFILE_ARC);
        aw.close();
        ARCReader r = ARCReaderFactory.get(OUTFILE_ARC);
        ArchiveRecordHeader arh = r.iterator().next().getHeader();
        assertFalse("Date in ARC header should never be the string 'null'", 
                arh.getDate().equals("null"));
        //todo: check that written date is close to current date
        assertTrue("Header-mimetype should be text/plain", arh.getMimetype().equals("text/plain"));
    }

    
    /** Encapsulate ARCWriter creation for test-purposes.
     * @param stream the PrintStream
     * @param arcfile the destination arcfile
     * @throws IOException
     * @return new ARCWriter 
     */
    public static ARCWriter getTestARCWriter(PrintStream stream, File arcfile)
    throws IOException {
        return 
            new ARCWriter(new AtomicInteger(), stream, arcfile, 
                    false, //Don't compress
                    ArchiveUtils.get14DigitDate(System.currentTimeMillis()), //Use current time
                    null //No particular file metadata to add
                    );
    }
    
}