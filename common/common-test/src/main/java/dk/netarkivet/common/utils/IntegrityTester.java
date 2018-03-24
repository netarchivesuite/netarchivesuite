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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.io.IOUtils;
import org.junit.Ignore;
import org.junit.Test;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.Constants;
import dk.netarkivet.common.distribute.FTPRemoteFile;
import dk.netarkivet.common.distribute.RemoteFile;
import dk.netarkivet.common.distribute.RemoteFileFactory;
import dk.netarkivet.testutils.FileAsserts;
import dk.netarkivet.testutils.TestFileUtils;
import dk.netarkivet.testutils.preconfigured.ReloadSettings;

/**
 * Integrity tests for the package dk.netarkivet.common.utils.
 */
@Ignore("Needs to be run in deploy-test module according to junit 3 test suite")
public class IntegrityTester {
    ReloadSettings rs = new ReloadSettings();
    private static final File BASE_DIR = new File("tests/dk/netarkivet/common/utils");
    private static final File ORIGINALS = new File(BASE_DIR, "fileutils_data");
    private static final File WORKING = new File(BASE_DIR, "working");
    private static final File SUBDIR = new File(WORKING, "subdir");
    private static final File SUBDIR2 = new File(WORKING, "subdir2");
    private static final File SUBDIR3 = new File(WORKING, "subdir3");
    private static final int BLOCKSIZE = 32768;
    private static final long LARGE = ((long) Integer.MAX_VALUE) + 1L;
    public static final String LARGE_FILE = "largeFile";

    public void setUp() {
        rs.setUp();
        FileUtils.removeRecursively(WORKING);
        TestFileUtils.copyDirectoryNonCVS(ORIGINALS, WORKING);
        /** Do not send notifications by email. Print them to STDOUT. */
        Settings.set(CommonSettings.NOTIFICATIONS_CLASS, RememberNotifications.class.getName());
    }

    public void tearDown() {
        FileUtils.removeRecursively(WORKING);
        rs.tearDown();
    }

    /**
     * test that FileUtils.append can append between two remote files using ftp.
     */
    @Test
    public void failingTestAppendRemoteFiles() throws IOException {
        Settings.set(CommonSettings.REMOTE_FILE_CLASS, FTPRemoteFile.class.getName());
        File in_1 = new File(WORKING, "append_data/file1");
        File in_2 = new File(WORKING, "append_data/file2");
        File out_file = new File(WORKING, "append_data/output");
        RemoteFile rf1 = RemoteFileFactory.getInstance(in_1, true, false, true);
        RemoteFile rf2 = RemoteFileFactory.getInstance(in_2, true, false, true);
        OutputStream out = new FileOutputStream(out_file);
        rf1.appendTo(out);
        rf2.appendTo(out);
        out.close();
        FileAsserts.assertFileNumberOfLines("File has wrong number of lines", out_file, 2);
        FileAsserts.assertFileContains("Missing content", "1", out_file);
        FileAsserts.assertFileContains("Missing content", "2", out_file);
    }

    /** Test that files larger than 2GB can be copied! */
    @Test
    public void failingTestCopyLargeFiles() throws IOException {
        byte[] block = new byte[BLOCKSIZE];
        SUBDIR.mkdirs();
        File largeFile = new File(SUBDIR, LARGE_FILE);
        OutputStream os = null;
        try {
            os = new BufferedOutputStream(new FileOutputStream(largeFile));
            System.out.println("Creating file - this will take a long time");
            for (long l = 0; l < LARGE / ((long) BLOCKSIZE) + 1L; l++) {
                os.write(block);
            }
            System.out.println("Copying file - this will take a long time");
            FileUtils.copyDirectory(SUBDIR, SUBDIR2);
            File file1 = new File(SUBDIR, LARGE_FILE);
            File file2 = new File(SUBDIR2, LARGE_FILE);
            assertEquals("Should have same file sizes", file1.length(), file2.length());
        } finally {
            IOUtils.closeQuietly(os);
        }
    }

    /**
     * This tests that we are actually able to write and read more than 4GB worth of data using GZip.
     *
     * @throws IOException
     */
    @Test
    public void failingTestGzipLargeFile() throws IOException {
        byte[] block = new byte[BLOCKSIZE];
        File largeFile = new File(WORKING, LARGE_FILE);
        OutputStream os = new GZIPOutputStream(new BufferedOutputStream(new FileOutputStream(largeFile)));
        System.out.println("Creating " + 5 * LARGE + " bytes file " + "- this will take a long time");
        block[1] = 'a';
        block[2] = 'b';
        for (long l = 0; l < 5 * LARGE / ((long) BLOCKSIZE) + 1L; l++) {
            os.write(block);
        }
        os.close();

        InputStream is = new LargeFileGZIPInputStream(new BufferedInputStream(new FileInputStream(largeFile)));
        System.out.println("Reading " + 5 * LARGE + " bytes file " + "- this will take a long time");
        byte[] buf = new byte[BLOCKSIZE];
        for (long l = 0; l < 5 * LARGE / ((long) BLOCKSIZE) + 1L; l++) {
            int totalRead = 0;
            int read = 0;
            while (totalRead != block.length && read != -1) {
                read = is.read(buf, totalRead, buf.length - totalRead);
                totalRead += read;
            }
            assertEquals("Should have read full length of block " + l, block.length, totalRead);
            for (int i = 0; i < 8; i++) {
                assertEquals("Read block " + l + " should be equals at " + i, block[i], buf[i]);
            }
        }
        assertEquals("This should be the end of the stream.", -1, is.read());
        is.close();
    }

    /** Test that files larger than 2GB can be gzipped and gunzipped! */
    @Test
    public void failingTestGZipGUnZipLargeFiles() throws IOException {
        byte[] block = new byte[BLOCKSIZE];
        SUBDIR.mkdirs();
        File largeFile = new File(SUBDIR, LARGE_FILE);
        OutputStream os = null;
        try {
            os = new FileOutputStream(largeFile);
            System.out.println("Creating file - this will take a long time");
            for (long l = 0; l < LARGE / ((long) BLOCKSIZE) + 1L; l++) {
                os.write(block);
            }
            System.out.println("Zipping file - this will take a long time");
            ZipUtils.gzipFiles(SUBDIR, SUBDIR2);
            System.out.println("UnZipping file - this will take a long time");
            ZipUtils.gunzipFiles(SUBDIR2, SUBDIR3);
            File file1 = new File(SUBDIR, LARGE_FILE);
            File file2 = new File(SUBDIR3, LARGE_FILE);
            assertEquals("Should have same file sizes", file1.length(), file2.length());
        } finally {
            IOUtils.closeQuietly(os);
        }
    }

    /**
     * Test reading two large files: One that should unzip OK, one that should fail with wrong CRC checksum.
     *
     * @throws IOException
     */
    @Test
    public void failingTestLargeGZIPReadLargeFiles() throws IOException {
        LargeFileGZIPInputStream largeFileGZIPInputStream = new LargeFileGZIPInputStream(new FileInputStream(new File(
                BASE_DIR, "data/largeFileWrongCRC/largeFile.gz")));
        try {
            byte[] buffer = new byte[Constants.IO_BUFFER_SIZE];
            while ((largeFileGZIPInputStream.read(buffer)) > 0) {
                // just carry on.
            }
            largeFileGZIPInputStream.close();
            // Unfortunately this doesn't work. Let's look forward to Java 1.6.0.
            // fail("Should throw exception on wrong CRC");
        } catch (IOException e) {
            // expected... but
            // Unfortunately this doesn't work. Let's look forward to Java 1.6.0.
            assertEquals("Must be the right exception, not " + ExceptionUtils.getStackTrace(e), "Corrupt GZIP trailer",
                    e.getMessage());
        }

        largeFileGZIPInputStream = new LargeFileGZIPInputStream(new FileInputStream(new File(BASE_DIR,
                "data/largeFile/largeFile.gz")));
        byte[] buffer = new byte[Constants.IO_BUFFER_SIZE];
        while ((largeFileGZIPInputStream.read(buffer)) > 0) {
            // just carry on.
        }
        largeFileGZIPInputStream.close();
    }
}
