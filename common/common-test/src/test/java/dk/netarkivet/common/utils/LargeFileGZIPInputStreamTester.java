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
package dk.netarkivet.common.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.junit.After;
import org.junit.Test;

/**
 * Test the worked-around GZIPInputStream on some small files.
 */
public class LargeFileGZIPInputStreamTester {
    private final static File BASE_DIR = new File("tests/dk/netarkivet/common/utils");
    private final static File ORIGINALS = new File(BASE_DIR, "data");
    private final static File WORKING = new File(BASE_DIR, "working");

    @After
    public void tearDown() {
        FileUtils.removeRecursively(WORKING);
    }

    /**
     * Tests read on four small files: 1) A gzipped small file that should unzip succesfully and contain the string
     * "text\n". 2) A small file with wrong checksum 3) A small file with wrong length 4) A small file cut off abruptly
     * in trailer
     *
     * @throws Exception
     */
    @Test
    public void testRead() throws Exception {
        LargeFileGZIPInputStream largeFileGZIPInputStream = new LargeFileGZIPInputStream(new FileInputStream(new File(
                ORIGINALS, "smallFile/smallFile.gz")));
        byte[] buffer = new byte[10];
        assertEquals("Should read 5 bytes", 5, largeFileGZIPInputStream.read(buffer));
        assertEquals("Should be the right string", "test\n", new String(buffer, 0, 5));
        assertEquals("Should be at EOF", -1, largeFileGZIPInputStream.read(buffer));

        int bytesRead;

        largeFileGZIPInputStream = new LargeFileGZIPInputStream(new FileInputStream(new File(ORIGINALS,
                "smallFileWrongCRC/smallFile.gz")));
        try {
            while ((bytesRead = largeFileGZIPInputStream.read(buffer)) > 0) {
                // just carry on.
            }
            assertFalse(bytesRead > 0);
            largeFileGZIPInputStream.close();
            fail("Should throw exception on wrong CRC");
        } catch (IOException e) {
            assertEquals("Must be the right exception, not " + ExceptionUtils.getStackTrace(e), "Corrupt GZIP trailer",
                    e.getMessage());
            // expected
        }

        largeFileGZIPInputStream = new LargeFileGZIPInputStream(new FileInputStream(new File(ORIGINALS,
                "smallFileWrongLength/smallFile.gz")));
        try {
            while ((bytesRead = largeFileGZIPInputStream.read(buffer)) > 0) {
                // just carry on.
            }
            assertFalse(bytesRead > 0);
            largeFileGZIPInputStream.close();
            fail("Should throw exception on wrong Length");
        } catch (IOException e) {
            assertEquals("Must be the right exception, not " + ExceptionUtils.getStackTrace(e), "Corrupt GZIP trailer",
                    e.getMessage());
            // expected
        }

        largeFileGZIPInputStream = new LargeFileGZIPInputStream(new FileInputStream(new File(ORIGINALS,
                "smallFileMissingLength/smallFile.gz")));
        try {
            while ((bytesRead = largeFileGZIPInputStream.read(buffer)) > 0) {
                // just carry on.
            }
            assertFalse(bytesRead > 0);
            largeFileGZIPInputStream.close();
            fail("Should throw exception on missing Length");
        } catch (IOException e) {
            // expected - in this case we don't know the exact exception
        }
    }
}
