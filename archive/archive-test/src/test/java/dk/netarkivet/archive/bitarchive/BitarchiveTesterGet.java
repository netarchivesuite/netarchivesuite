/*
 * #%L
 * Netarchivesuite - archive - test
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
package dk.netarkivet.archive.bitarchive;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.junit.Test;

import dk.netarkivet.common.distribute.arcrepository.BitarchiveRecord;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.StreamUtils;

/**
 * This class tests the get() operation of the bit archive.
 */
public class BitarchiveTesterGet extends BitarchiveTestCase {
    private static final File ORIGINALS_DIR = new File(new File(TestInfo.DATA_DIR, "get"), "existing");
    /** The content of the first arc record (offset=0). */
    private static final String ARC_RECORD_0 = "arc_record0.txt";
    /** Store record 0 in this file, when reading from arc-files. */
    private static final String ARC_RECORD_0_TMP = "arc_record0.tmp";
    /** An ARC file that must not exist in the ARCHIVE_DIR directory. */
    static final String MISSING_ARC_FILE_NAME = "ShouldNotExist.ARC";
    /**
     * The name of the ARC file that we're reading. This file must not exist in the ARCHIVE_DIR directory.
     */
    static final String ARC_FILE_NAME = "Upload2.ARC";

    protected File getOriginalsDir() {
        return ORIGINALS_DIR;
    }

    /* **** Part one: Test that the parameters are legal **** */

    /**
     * Test that a file identifier is given.
     */
    @Test
    public void testGetNoFile() {
        try {
            archive.get(null, 0);
            fail("Null file pointer should have given an exception.");
        } catch (ArgumentNotValid e) {
            /* Expected case */
        } catch (Exception e) {
            fail("Null file pointer should have given ArgumentNotValid, not " + e);
        }
    }

    /**
     * Test that the offset is legal (i.e. >0).
     */
    @Test(expected = ArgumentNotValid.class)
    public void testGetIllegalOffset() {
        archive.get(ARC_FILE_NAME, -1);
        fail("Sub-zero offset should have given an exception.");
    }

    /* **** Part two: Test that errors are treated correctly **** */

    /**
     * Test that an unknown file gives an error.
     */
    @Test
    public void testGetUnknownFile() {
        BitarchiveRecord bar = archive.get(MISSING_ARC_FILE_NAME, -1);
        assertNull("Should not receive any record for unknown file, not " + bar, bar);
    }

    /**
     * Test that an index beyond the end of the ARC file gives an error.
     */
    @Test(expected = ArgumentNotValid.class)
    public void testGetIndexTooLarge() {
        archive.get(ARC_FILE_NAME, 10000000);
        fail("Too large offset should have given an exception.");
    }

    /**
     * Test that an index that does not fit an ARC entry gives an error.
     */
    @Test(expected = IOFailure.class)
    public void testGetIndexNotAligned() {
        archive.get(ARC_FILE_NAME, 1725);
        fail("Misaligned offset should have given an IOFailure.");
    }

    /* **** Part three: Test that correct code works **** */

    /**
     * Test that a correct query gives the correct file.
     *
     * @throws IOException
     */
    @Test
    public void testGetEntry() throws IOException {
        BitarchiveRecord record = archive.get(ARC_FILE_NAME, 0);
        assertNotNull("ARC record should be non-null", record);
        assertEquals("The arc file name should appear in the record.", ARC_FILE_NAME, record.getFile());
        // Write contents of record to ARC_RECORD_0_TMP
        File recordOFile = new File(TestInfo.WORKING_DIR, ARC_RECORD_0_TMP);
        OutputStream os = new FileOutputStream(recordOFile);
        record.getData(os);
        // read targetContents and foundContents from respectively
        // ARC_RECORD_0 ARC_RECORD_0_TMP
        String targetcontents = FileUtils.readFile(new File(TestInfo.WORKING_DIR, ARC_RECORD_0));
        String foundContents = FileUtils.readFile(new File(TestInfo.WORKING_DIR, ARC_RECORD_0_TMP));
        // verify that their contents are identical
        assertEquals("Strings targetcontents should have same length", targetcontents.length(), foundContents.length());
        assertEquals("The contents should be exactly the same", targetcontents, foundContents);
    }

    /**
     * Test that an empty entry is handled correctly.
     */
    @Test
    public void testGetEmptyEntry() {
        try {
            BitarchiveRecord record = archive.get(ARC_FILE_NAME, 37534);
            assertNotNull("ARC record should be non-null", record);
            assertEquals("ARC record should be for the right file", ARC_FILE_NAME, record.getFile());
            byte[] contents = StreamUtils.inputStreamToBytes(record.getData(), (int) record.getLength());
            assertEquals("There should be no contents", contents.length, 0);
        } catch (Exception e) {
            fail("Proper ARC file access should not give any exceptions, not " + e);
        }
    }

    /**
     * Test that an entry with 0xff bytes is read correctly.
     */
    @Test
    public void testGet0xFF() {
        try {
            BitarchiveRecord record = archive.get(ARC_FILE_NAME, 37650);
            assertNotNull("ARC record should be non-null", record);
            assertEquals("ARC record should be for the right file", ARC_FILE_NAME, record.getFile());
            byte[] contents = StreamUtils.inputStreamToBytes(record.getData(), (int) record.getLength());
            assertEquals("Contents length should match file", 17111, contents.length);
        } catch (Exception e) {
            fail("Proper ARC file access should not give any exceptions, not " + e);
        }
    }

    /* **** Part four: Test that bug 4 is fixed **** */

    /**
     * Test that a correct query gives the correct file and that the ArcRecord is closed so that the file can be deleted
     * afterwards.
     */
    @Test
    public void testArcRecordIsClosedAfterGet() {
        try {
            BitarchiveRecord record = archive.get(ARC_FILE_NAME, 0);
            assertNotNull("ARC record should be non-null", record);
            assertEquals("ARC record should be for the right file", ARC_FILE_NAME, record.getFile());

            // Write contents of record to ARC_RECORD_0_TMP
            File recordOFile = new File(TestInfo.WORKING_DIR, ARC_RECORD_0_TMP);
            OutputStream os = new FileOutputStream(recordOFile);
            record.getData(os);

            String targetcontents = FileUtils.readFile(new File(TestInfo.WORKING_DIR, ARC_RECORD_0));
            String foundContents = FileUtils.readFile(new File(TestInfo.WORKING_DIR, ARC_RECORD_0_TMP));
            assertEquals("Strings targetcontents should have same length",
                    targetcontents.length(), foundContents.length());
            assertEquals("Contents should be exactly as expected", targetcontents, foundContents);
        } catch (Exception e) {
            fail("Proper ARC file access should not give any exceptions, not " + e);
        }
        assertTrue("File should be deletable",
                FileUtils.removeRecursively(new File(new File(TestInfo.WORKING_DIR, "filedir"), ARC_FILE_NAME)));
    }
}
