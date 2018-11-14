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
import java.util.Arrays;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import dk.netarkivet.archive.ArchiveSettings;
import dk.netarkivet.archive.bitarchive.distribute.BitarchiveServer;
import dk.netarkivet.common.distribute.Channels;
import dk.netarkivet.common.distribute.RemoteFile;
import dk.netarkivet.common.distribute.RemoteFileFactory;
import dk.netarkivet.common.distribute.TestRemoteFile;
import dk.netarkivet.common.distribute.arcrepository.BitarchiveRecord;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.PermissionDenied;
import dk.netarkivet.common.utils.Settings;

/**
 * Unit test for Bitarchive API. The upload method is tested
 */
public class BitarchiveTesterUpload extends BitarchiveTestCase {
    /** The external interface. */
    private static BitarchiveServer server;

    /**
     * The directory from where we upload the ARC files.
     */
    private static final File ORIGINALS_DIR = new File(new File(TestInfo.DATA_DIR, "upload"), "originals");

    /**
     * The files that are uploaded during the tests and that must be removed afterwards.
     */
    private static final List<String> UPLOADED_FILES = Arrays.asList("Upload1.ARC", "Upload2.ARC", "Upload3.ARC");

    protected File getOriginalsDir() {
        return ORIGINALS_DIR;
    }

    /**
     * At start of test, set up an archive we can run against.
     */
    @Before
    public void setUp() throws Exception {
        super.setUp(); 
        try { 
        	Channels.getAllBa();
        } catch (Exception e){
        	fail("Channels.getAllBa should not throw Exception " + e);
        }
        server = BitarchiveServer.getInstance();
    }

    /**
     * At end of test, remove any files we managed to upload.
     */
    @After
    public void tearDown() throws Exception {
        if (server != null) {
            server.close();
        }
        super.tearDown();
    }

    /* **** Part one: Test that illegal parameters are handled correctly. ** */

    /**
     * Test that giving null for a filename gives the right exception.
     */
    @Test
    public void testUploadNoFile() {
        try {
            archive.upload(null, "NoFile");
            fail("Null file pointer should have given an exception.");
        } catch (ArgumentNotValid e) {
            /* Expected case */
        }
    }

    /* **** Part two: Test that errors are treated correctly. **** */

    /**
     * Uploading a file that does not exist should throw an ArgumentNotValid exception.
     */
    @Test
    public void testUploadMissingFile() {
        try {
            archive.upload(
                    RemoteFileFactory.getInstance(new File(ORIGINALS_DIR, "ShouldNotExist.ARC"), true, false, true),
                    "ShouldNotExist.ARC");
            fail("Non-existing file should have given an exception.");
        } catch (ArgumentNotValid e) {
            /* Expected case */
        }
    }

    /**
     * Uploading a file that has errors should throw an IOFailure exception.
     */
    @Test
    public void testUploadBadFile() {
        try {
            final RemoteFile arcfile = RemoteFileFactory.getInstance(new File(ORIGINALS_DIR, "Upload1.ARC"), true,
                    false, true);
            ((TestRemoteFile) arcfile).failsOnCopy = true;
            archive.upload(arcfile, "ShouldNotExist.ARC");
            fail("Non-existing file should have given an exception.");
        } catch (IOFailure e) {
            /* Expected case */
        }
    }

    /**
     * Test that uploading a directory throws an exception.
     */
    @Test
    public void testUploadNoDir() {
        try {
            archive.upload(RemoteFileFactory.getInstance(TestInfo.WORKING_DIR, true, false, true),
                    TestInfo.WORKING_DIR.getName());
            fail("Uploading directory should have given an exception.");
        } catch (ArgumentNotValid e) {
            /* Expected case */
        }
    }

    /**
     * Uploading a file that already exists in the archive should throw a FileAlreadyExists exception.
     */
    @Test
    public void testUploadAlreadyUploaded() {
        RemoteFile rf = new TestRemoteFile(new File(ORIGINALS_DIR, UPLOADED_FILES.get(0)), false, false, false);
        try {
            archive.upload(rf, UPLOADED_FILES.get(0));
            archive.upload(rf, UPLOADED_FILES.get(0));
            fail("Should throw exception when uploading " + "a file already present in the archive.");
        } catch (PermissionDenied e) {
            // This exception is expected
        }
    }

    /**
     * Uploading a file that exists (valid reference) and that does not exist in the archive.
     */
    @Test
    public void testUploadSuccess() {
        archive.upload(new TestRemoteFile(new File(ORIGINALS_DIR, UPLOADED_FILES.get(1)), false, false, false),
                UPLOADED_FILES.get(1));
    }

    /**
     * Verify that data do not exist in the archive before uploading the file and that data are part of the archive
     * after upload.
     *
     * @throws IOException If unable to close FileOutputStream.
     */
    @Test
    public void testUploadDataInArchive() throws IOException {

        String nameForFileToUpload = UPLOADED_FILES.get(2);
        long index = 0;
        assertNull("File should not be in archive before upload", archive.get(nameForFileToUpload, index));

        archive.upload(new TestRemoteFile(new File(ORIGINALS_DIR, nameForFileToUpload), false, false, false),
                nameForFileToUpload);
        BitarchiveRecord record = archive.get(nameForFileToUpload, index);
        assertNotNull("The newly uploaded file " + "should contain data.", record);
        assertEquals(nameForFileToUpload, record.getFile());
        // BitarchiveRecord.getData() now returns a InputStream
        // InputStream theData = record.getData();
        // byte[] contents = new byte[(int) record.getLength()];
        OutputStream os = null;
        try {
            File tmp = File.createTempFile("uploadtest-", ".tmp");
            os = new FileOutputStream(tmp);
            record.getData(os);
            assertEquals("Size of record should be equal to 1254", 1254, tmp.length());
        } catch (IOException e) {
            fail("Unable to write record to disk: " + e);
        } finally {
            if (os != null) {
                os.close();
            }
        }
        record = archive.get(nameForFileToUpload, 1573);
        assertNotNull("The newly uploaded file " + "should contain a record at offset 1573.", record);

        assertNotNull("The newly uploaded file " + "should contain data.", record);
        assertEquals(nameForFileToUpload, record.getFile());
    }

    /**
     * Verify that we upload into specified directory.
     */
    @Test
    public void testUploadUsesDir() {
        final File dir1 = new File(TestInfo.WORKING_DIR, "dir1");
        setupBitarchiveWithDirs(new String[] {dir1.getAbsolutePath(),});
        archive.upload(new TestRemoteFile(new File(ORIGINALS_DIR, UPLOADED_FILES.get(2)), false, false, false),
                UPLOADED_FILES.get(2));
        assertTrue("Should place file in directory " + dir1,
                new File(new File(dir1, "filedir"), UPLOADED_FILES.get(2)).exists());
    }

    private void setupBitarchiveWithDirs(final String[] dirpaths) {
        Settings.set(ArchiveSettings.BITARCHIVE_SERVER_FILEDIR, dirpaths);
        // Don't like the archive made in setup, try again:)
        archive.close();
        archive = Bitarchive.getInstance();
    }
}
