/* File:    $Id$
 * Version: $Revision$
 * Date:    $Date$
 * Author:  $Author$
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
import java.io.FileOutputStream;
import java.io.OutputStream;

import junit.framework.TestCase;

import dk.netarkivet.common.Settings;
import dk.netarkivet.common.distribute.JMSConnectionTestMQ;
import dk.netarkivet.common.distribute.arcrepository.BitarchiveRecord;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.testutils.TestFileUtils;
import dk.netarkivet.testutils.TestUtils;

/**
 * This class tests the get() operation of the bit archive.
 *
 */
public class BitarchiveTesterGet extends TestCase {
    /**
     * The archive directory to work on.
     */
    private static final File ARCHIVE_DIR = new File(
            "tests/dk/netarkivet/archive/bitarchive/data/get/working/");

    /**
     * The name of the ARC file that we're reading. This file must not exist in
     * the ARCHIVE_DIR directory.
     */
    private static final String ARC_FILE_NAME = "Upload2.ARC";

    private static final File ORIGINAL_ARCHIVE_DIR = new File(
            "tests/dk/netarkivet/archive/bitarchive/data/get/existing/");

    /** The content of the first arc record (offset=0). */
    private static final String ARC_RECORD_0 = "arc_record0.txt";
    
    /** Store record 0 in this file, when reading from arc-files. */
    private static final String ARC_RECORD_0_TMP = "arc_record0.tmp";
    

    /** An ARC file that must not exist in the ARCHIVE_DIR directory. */
    private static final String MISSING_ARC_FILE_NAME = "ShouldNotExist.ARC";

    /** The archive that this test queries. */
    private static Bitarchive archive;

    /**
     * Create a new test object.
     *
     * @param sTestName
     *            Name of this test.
     */
    public BitarchiveTesterGet(final String sTestName) {
        super(sTestName);
    }

    public void setUp() {
        JMSConnectionTestMQ.useJMSConnectionTestMQ();
        FileUtils.removeRecursively(ARCHIVE_DIR);
        try {
            // Copy over the "existing" bit archive.
            TestFileUtils
                    .copyDirectoryNonCVS(ORIGINAL_ARCHIVE_DIR, ARCHIVE_DIR);
            Settings.set(Settings.BITARCHIVE_SERVER_FILEDIR, ARCHIVE_DIR
                    .getAbsolutePath());
            archive = Bitarchive.getInstance();
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    /**
     * After test is done, remove the "archive".
     */
    public void tearDown() {
        archive.close();
        FileUtils.removeRecursively(ARCHIVE_DIR);
        JMSConnectionTestMQ.clearTestQueues();
        Settings.reload();
    }

    /* **** Part one: Test that the parameters are legal **** */
    /**
     * Test that a file identifier is given.
     *
     */
    public void testGetNoFile() {
        try {
            archive.get(null, 0);
            fail("Null file pointer should have given an exception.");
        } catch (ArgumentNotValid e) {
            /* Expected case */
        } catch (Exception e) {
            fail("Null file pointer should have given ArgumentNotValid, not "
                    + e);
        }
    }

    /**
     * Test that the offset is legal (i.e. >0).
     *
     */
    public void testGetIllegalOffset() {
        try {
            archive.get(ARC_FILE_NAME, -1);
            fail("Sub-zero offset should have given an exception.");
        } catch (ArgumentNotValid e) {
            /* Expected case */
        } catch (Exception e) {
            fail("Sub-zero offset should have given ArgumentNotValid, not " + e);
        }
    }

    /* **** Part two: Test that errors are treated correctly **** */
    /**
     * Test that an unknown file gives an error.
     *
     */
    public void testGetUnknownFile() {
        BitarchiveRecord bar = archive.get(MISSING_ARC_FILE_NAME, -1);
        assertNull("Should not receive any record for unknown file, not " +
                bar, bar);
    }

    /**
     * Test that an index beyond the end of the ARC file gives an error.
     *
     */
    public void testGetIndexTooLarge() {
        try {
            archive.get(ARC_FILE_NAME, 10000000);
            fail("Too large offset should have given an exception.");
        } catch (ArgumentNotValid e) {
            /* Expected case */
        } catch (Exception e) {
            fail("Too large offset should have given ArgumentNotValid, not "
                    + e);
        }
    }

    /**
     * Test that an index that does not fit an ARC entry gives an error.
     *
     */
    public void testGetIndexNotAligned() {
        try {
            archive.get(ARC_FILE_NAME, 1725);
            fail("Misaligned offset should have given an IOFailure.");
        } catch (IOFailure e) {
            /* Expected */
        }
    }

    /* **** Part three: Test that correct code works **** */
    /**
     * Test that a correct query gives the correct file.
     *
     */
    public void testGetEntry() {
        try {
            BitarchiveRecord record = archive.get(ARC_FILE_NAME, 0);
            assertNotNull("ARC record should be non-null", record);
            assertEquals(ARC_FILE_NAME, record.getFile());
            // Write contents of record to ARC_RECORD_0_TMP
            File recordOFile = new File(ARCHIVE_DIR, ARC_RECORD_0_TMP);
            OutputStream os = new FileOutputStream(recordOFile);
            record.getData(os);
            // read targetContents and foundContents from respectively
            // ARC_RECORD_0 ARC_RECORD_0_TMP
            String targetcontents = FileUtils.readFile(new File(ARCHIVE_DIR,
                    ARC_RECORD_0));
            String foundContents = FileUtils.readFile(new File(ARCHIVE_DIR,
                    ARC_RECORD_0_TMP));
            // verify that their contents are identical
            assertTrue("Strings targetcontents (length = "
                    + targetcontents.length() + ") and foundContents (length="
                    + foundContents.length() + ") should have same length",
                    targetcontents.length() == foundContents.length());
            assertEquals(targetcontents, foundContents);
        } catch (Exception e) {
            fail("Proper ARC file access should not give any exceptions, not "
                    + e);
        }
    }

    /**
     * Test that an empty entry is handled correctly.
     */
    public void testGetEmptyEntry() {
        try {
            BitarchiveRecord record = archive.get(ARC_FILE_NAME, 37534);
            assertNotNull("ARC record should be non-null", record);
            assertEquals(ARC_FILE_NAME, record.getFile());
            byte[] contents = TestUtils.inputStreamToBytes(
                    record.getData(), (int) record.getLength());
            assertEquals(contents.length, 0);
        } catch (Exception e) {
            fail("Proper ARC file access should not give any exceptions, not "
                    + e);
        }
    }

    /**
     * Test that an entry with 0xff bytes is read correctly.
     *
     */
    public void testGet0xFF() {
        try {
            BitarchiveRecord record = archive.get(ARC_FILE_NAME, 37650);
            assertNotNull("ARC record should be non-null", record);
            assertEquals(ARC_FILE_NAME, record.getFile());
            byte[] contents = TestUtils.inputStreamToBytes(
                    record.getData(), (int) record.getLength());
            assertEquals(17111, contents.length);
        } catch (Exception e) {
            fail("Proper ARC file access should not give any exceptions, not "
                    + e);
        }
    }

    /* **** Part four: Test that bug 4 is fixed **** */
    /**
     * Test that a correct query gives the correct file and that the
     * ArcRecord is closed so that the file can be deleted afterwards.
     */
    public void testArcRecordIsClosedAfterGet() {
        try {
            BitarchiveRecord record = archive.get(ARC_FILE_NAME, 0);
            assertNotNull("ARC record should be non-null", record);
            assertEquals(ARC_FILE_NAME, record.getFile());
            
            // Write contents of record to ARC_RECORD_0_TMP
            File recordOFile = new File(ARCHIVE_DIR, ARC_RECORD_0_TMP);
            OutputStream os = new FileOutputStream(recordOFile);
            record.getData(os);
            // read targetContents and foundContents from respectively
            // ARC_RECORD_0 ARC_RECORD_0_TMP
            String targetcontents = FileUtils.readFile(new File(ARCHIVE_DIR,
                    ARC_RECORD_0));
            String foundContents = FileUtils.readFile(new File(ARCHIVE_DIR,
                    ARC_RECORD_0_TMP));
            // verify that their contents are identical
            assertTrue("Strings targetcontents (length = "
                    + targetcontents.length() + ") and foundContents (length="
                    + foundContents.length() + ") should have same length",
                    targetcontents.length() == foundContents.length());
            assertEquals(targetcontents, foundContents);
        } catch (Exception e) {
            fail("Proper ARC file access should not give any exceptions, not "
                    + e);
        }
        assertTrue("File should be deletable",
                   FileUtils.removeRecursively(
                           new File(new File(ARCHIVE_DIR, "filedir"),
                                    ARC_FILE_NAME)));
    }
}
