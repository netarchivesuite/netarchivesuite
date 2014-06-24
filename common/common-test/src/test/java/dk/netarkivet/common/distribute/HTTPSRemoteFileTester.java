package dk.netarkivet.common.distribute;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URL;

import junit.framework.TestCase;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.ChecksumCalculator;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.testutils.preconfigured.MoveTestFiles;
import dk.netarkivet.testutils.preconfigured.ReloadSettings;
import dk.netarkivet.testutils.preconfigured.UseTestRemoteFile;

/** Tests for HTTPSRemoteFile */

@SuppressWarnings({ "serial"})
public class HTTPSRemoteFileTester extends TestCase {
    MoveTestFiles mtf = new MoveTestFiles(TestInfo.ORIGINALS_DIR,
                                          TestInfo.WORKING_DIR);
    UseTestRemoteFile utrf = new UseTestRemoteFile();
    ReloadSettings rs = new ReloadSettings();

    public HTTPSRemoteFileTester(String s) {
        super(s);
    }

    public void setUp() {
        rs.setUp();
        utrf.setUp();
        mtf.setUp();
        // Make sure we're using the right HTTP remote file by closing old
        // registries.
        HTTPRemoteFileRegistry.getInstance().cleanup();
        HTTPSRemoteFileRegistry.getInstance().cleanup();
    }

    public void tearDown() {
        HTTPRemoteFileRegistry.getInstance().cleanup();
        HTTPSRemoteFileRegistry.getInstance().cleanup();
        mtf.tearDown();
        utrf.tearDown();
        rs.tearDown();
    }

    public void testCopyto() throws Exception {
        //Copying twice with multiple
        HTTPSRemoteFile rf = new ForceRemoteHTTPSRemoteFile(TestInfo.FILE1,
                                                            false, false, true);
        File tempFile = File
                .createTempFile("TEST", "COPYTO", TestInfo.WORKING_DIR);
        rf.copyTo(tempFile);
        String contents = FileUtils.readFile(TestInfo.FILE1);
        assertEquals("Files should be equal",
                     contents,
                     FileUtils.readFile(tempFile));
        assertTrue("Original file should still exist when not deletable",
                   TestInfo.FILE1.exists());

        File tempFile2 = File
                .createTempFile("TEST", "COPYTO", TestInfo.WORKING_DIR);
        rf.copyTo(tempFile2);
        assertEquals("Files should be equal, since multiple was true",
                     contents,
                     FileUtils.readFile(tempFile));
        assertTrue("Original file should still exist when not deletable",
                   TestInfo.FILE1.exists());

        //Copying twice without multiple
        rf = new ForceRemoteHTTPSRemoteFile(TestInfo.FILE1,
                                            false, false, false);
        tempFile = File
                .createTempFile("TEST", "COPYTO", TestInfo.WORKING_DIR);
        rf.copyTo(tempFile);
        assertEquals("Files should be equal",
                     contents,
                     FileUtils.readFile(tempFile));
        assertTrue("Original file should still exist when not deletable",
                   TestInfo.FILE1.exists());

        tempFile2 = File
                .createTempFile("TEST", "COPYTO", TestInfo.WORKING_DIR);
        try {
            rf.copyTo(tempFile2);
            fail("Multiple copies should not be allowed");
        } catch (IOFailure e) {
            //expected
        }

        //Copying with non-multiple and deletable
        rf = new ForceRemoteHTTPSRemoteFile(TestInfo.FILE1,
                                            false, true, false);
        tempFile = File
                .createTempFile("TEST", "COPYTO", TestInfo.WORKING_DIR);
        rf.copyTo(tempFile);
        assertEquals("Files should be equal",
                     contents,
                     FileUtils.readFile(tempFile));
        assertFalse("Original file shouldn't exist anymore",
                    TestInfo.FILE1.exists());
    }

    public void testCleanup() throws Exception {
        HTTPSRemoteFile rf = new ForceRemoteHTTPSRemoteFile(TestInfo.FILE1,
                                                            false, false, true);
        URL url = rf.url;
        HTTPSRemoteFileRegistry.getInstance().openConnection(url).getInputStream();
        rf.cleanup();
        assertTrue("File should still exist when not deletable",
                   TestInfo.FILE1.exists());
        try {
            HTTPSRemoteFileRegistry.getInstance().openConnection(url).getInputStream();
            fail("Should not be available any longer");
        } catch (FileNotFoundException e) {
            //expected
        }

        rf = new ForceRemoteHTTPSRemoteFile(TestInfo.FILE1,
                                            false, true, true);
        url = rf.url;
        HTTPSRemoteFileRegistry.getInstance().openConnection(url).getInputStream();
        rf.cleanup();
        assertFalse("File should not exist when deletable",
                    TestInfo.FILE1.exists());
        try {
            HTTPSRemoteFileRegistry.getInstance().openConnection(url).getInputStream();
            fail("Should not be available any longer");
        } catch (FileNotFoundException e) {
            //expected
        }
        //should not throw exception
        rf.cleanup();
    }

    public void testGetChecksum() throws Exception {
        HTTPSRemoteFile rf = new ForceRemoteHTTPSRemoteFile(TestInfo.FILE1,
                                                            false, false, true);
        assertEquals("Should get null (no checksum requested)",
                     null, rf.getChecksum());
        rf = new HTTPSRemoteFileTester.ForceRemoteHTTPSRemoteFile(TestInfo.FILE1,
                                           true, false, true);
        assertEquals("Should get right checksum",
                     ChecksumCalculator.calculateMd5(TestInfo.FILE1), rf.getChecksum());
    }

    private class ForceRemoteHTTPSRemoteFile extends HTTPSRemoteFile {
        public ForceRemoteHTTPSRemoteFile(File f, boolean useChecksums,
                                          boolean fileDeletable,
                                          boolean multipleDownloads) {
            super(f, useChecksums, fileDeletable, multipleDownloads);
        }

        protected boolean isLocal() {
            return false;
        }
    }
}
