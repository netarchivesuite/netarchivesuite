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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.concurrent.atomic.AtomicInteger;

import org.archive.format.warc.WARCConstants;
import org.archive.io.ArchiveRecord;
import org.archive.io.ArchiveRecordHeader;
import org.archive.io.warc.WARCReader;
import org.archive.io.warc.WARCReaderFactory;
import org.archive.io.warc.WARCWriter;
import org.archive.io.warc.WARCWriterPoolSettings;
import org.archive.io.warc.WARCWriterPoolSettingsData;
import org.archive.uid.UUIDGenerator;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.arc.TestInfo;
import dk.netarkivet.testutils.FileAsserts;
import dk.netarkivet.testutils.TestFileUtils;

/**
 * Tests for class WARCUtils.
 */
@SuppressWarnings({"unused"})
public class WARCUtilsTester {

    private static File OUTFILE_WARC = new File(TestInfo.WORKING_DIR, "outFile.warc");
    private static File OUTFILE1_WARC = new File(TestInfo.WORKING_DIR, "outFile1.warc");
    private static File EMPTY_WARC = new File(TestInfo.WORKING_DIR, "input-empty.warc");
    private static File NONARC_WARC = new File(TestInfo.WORKING_DIR, "input-nonarc.warc");
    private static File INPUT_1_WARC = new File(TestInfo.WORKING_DIR, "input-1.warc");
    private static File INPUT_2_WARC = new File(TestInfo.WORKING_DIR, "input-2.warc");
    private static File INPUT_3_WARC = new File(TestInfo.WORKING_DIR, "input-3.warc");

    @Before
    public void setUp() throws Exception {
        TestFileUtils.copyDirectoryNonCVS(TestInfo.ORIGINALS_DIR, TestInfo.WORKING_DIR);
    }

    @After
    public void tearDown() throws Exception {
        FileUtils.removeRecursively(TestInfo.WORKING_DIR);
    }

    /**
     * Test that the insertARCFile method inserts the expected things.
     */
    @Test
    public void testInsertWARCFile() throws Exception {
        // Test illegal arguments first.
        try {
            WARCUtils.insertWARCFile(TestInfo.WORKING_DIR, null);
            fail("Should get ArgumentNotValid on null writer");
        } catch (ArgumentNotValid e) {
            // Expected
        }

        File outFile = OUTFILE_WARC;
        PrintStream stream = new PrintStream(outFile);
        WARCWriter writer = getTestWARCWriter(stream, outFile);
        FileAsserts.assertFileNumberOfLines("Should just have filedesc", outFile, 0);
        try {
            WARCUtils.insertWARCFile(null, writer);
            fail("Should get ArgumentNotValid on null writer");
        } catch (ArgumentNotValid e) {
            // Expected
        }
        writer.close();
        FileAsserts.assertFileNumberOfLines("Should be empty", outFile, 0);

    }

    /**
     * Encapsulate WARCWriter creation for test-purposes.
     *
     * @param stream the PrintStream
     * @param warcfile the destination warcfile
     * @return new WARCWriter
     * @throws IOException
     */
    public static WARCWriter getTestWARCWriter(PrintStream stream, File warcfile) throws IOException {
    	/*
        return new WARCWriterNAS(new AtomicInteger(), stream, warcfile, false, // Don't compress
                ArchiveUtils.get14DigitDate(System.currentTimeMillis()), // Use current time
                null // No particular file metadata to add
        );
        */
        WARCWriterPoolSettings settings = new WARCWriterPoolSettingsData(
        		WARCConstants.WARC_FILE_EXTENSION, null, WARCConstants.DEFAULT_MAX_WARC_FILE_SIZE, false,
        		null, null, new UUIDGenerator());
        return new WARCWriter(new AtomicInteger(), stream, warcfile, settings);
    }

    @Test
    public void testWarcCopy() {
        try {
            byte[] warcBytes = ("WARC/1.0\r\n"
                    + "WARC-Type: metadata\r\n"
                    + "WARC-Target-URI: metadata://netarkivet.dk/crawl/setup/duplicatereductionjobs?majorversion=1&minorversion=0&harvestid=1&harvestnum=59&jobid=86\r\n"
                    + "WARC-Date: 2012-08-24T11:42:55Z\r\n"
                    + "WARC-Record-ID: <urn:uuid:c93099e5-2304-487e-9ff2-41e3c01c2b51>\r\n"
                    + "WARC-Payload-Digest: sha1:SUCGMUVXDKVB5CS2NL4R4JABNX7K466U\r\n"
                    + "WARC-IP-Address: 207.241.229.39\r\n"
                    + "WARC-Concurrent-To: <urn:uuid:e7c9eff8-f5bc-4aeb-b3d2-9d3df99afb30>\r\n"
                    + "WARC-Concurrent-To: <urn:uuid:e7c9eff8-f5bc-4aeb-b3d2-9d3df99afb31>\r\n"
                    + "Content-Type: text/plain\r\n" + "Content-Length: 2\r\n" + "\r\n" + "85" + "\r\n" + "\r\n")
                    .getBytes();
            File orgFile = new File(TestInfo.WORKING_DIR, "original4copy.warc");
            FileUtils.writeBinaryFile(orgFile, warcBytes);

            File copiedFile = new File(TestInfo.WORKING_DIR, "copied.warc");
            WARCWriter writer = WARCUtils.createWARCWriter(copiedFile);
            WARCUtils.insertWARCFile(orgFile, writer);
            writer.close();

            byte[] bytes = FileUtils.readBinaryFile(copiedFile);
            // System.out.println( new String(bytes));

            WARCReader reader = WARCReaderFactory.get(copiedFile);
            assertNotNull(reader);
            ArchiveRecord record = reader.get();
            assertNotNull(record);
            ArchiveRecordHeader header = record.getHeader();
            assertNotNull(header);

            assertEquals("metadata", header.getHeaderValue("WARC-Type"));
            assertEquals(
                    "metadata://netarkivet.dk/crawl/setup/duplicatereductionjobs?majorversion=1&minorversion=0&harvestid=1&harvestnum=59&jobid=86",
                    header.getHeaderValue("WARC-Target-URI"));
            assertEquals("2012-08-24T11:42:55Z", header.getHeaderValue("WARC-Date"));
            assertEquals("<urn:uuid:c93099e5-2304-487e-9ff2-41e3c01c2b51>",
                    header.getHeaderValue("WARC-Record-ID"));
            assertEquals("sha1:SUCGMUVXDKVB5CS2NL4R4JABNX7K466U", header.getHeaderValue("WARC-Payload-Digest"));
            assertEquals("207.241.229.39", header.getHeaderValue("WARC-IP-Address"));
            assertEquals("<urn:uuid:e7c9eff8-f5bc-4aeb-b3d2-9d3df99afb31>",
                    header.getHeaderValue("WARC-Concurrent-To"));
            assertEquals("text/plain", header.getHeaderValue("Content-Type"));
            assertEquals("2", header.getHeaderValue("Content-Length"));
        } catch (IOException e) {
            e.printStackTrace();
            Assert.fail("Unexpected exception!");
        }
    }
}
