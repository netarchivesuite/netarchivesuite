package dk.netarkivet.common.distribute.arcrepository;
/**
 * lc forgot to comment this!
 */

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.List;

import junit.framework.TestCase;

import dk.netarkivet.common.distribute.TestRemoteFile;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IllegalState;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.testutils.FileAsserts;
import dk.netarkivet.testutils.preconfigured.MoveTestFiles;


public class BatchStatusTester extends TestCase {
    MoveTestFiles mtf = new MoveTestFiles(TestInfo.ORIGINALS_DIR,
            TestInfo.WORKING_DIR);

    public BatchStatusTester(String s) {
        super(s);
    }

    public void setUp() {
        mtf.setUp();
    }

    public void tearDown() {
        mtf.tearDown();
    }

    public void testCopyResults() throws IOException {
        List<File> emptyList = Collections.emptyList();
        File tmpFile = new File(TestInfo.WORKING_DIR, "newFile");
        String fileContents = FileUtils.readFile(TestInfo.SAMPLE_FILE);
        TestRemoteFile lrf = new TestRemoteFile(TestInfo.SAMPLE_FILE, false,
                                                  false, false);
        BatchStatus bs = new BatchStatus("KB", emptyList, 1, lrf);
        bs.copyResults(tmpFile);
        FileAsserts.assertFileContains("Should have copied result contents",
                fileContents, tmpFile);
        assertTrue("Source (remote) file should be deleted", lrf.isDeleted());
        FileUtils.copyFile(tmpFile, TestInfo.SAMPLE_FILE);

        File noSuchFile = new File(TestInfo.WORKING_DIR, "noSuchFile");
        try {
            bs.copyResults(noSuchFile);
            fail("Should have thrown exception on already-read file");
        } catch (IllegalState e) {
            // expected
        }
        assertFalse("Should not have made a result file",
                noSuchFile.exists());

        lrf = new TestRemoteFile(TestInfo.EMPTY_FILE, false, false, false);
        bs = new BatchStatus("KB", emptyList, 1, lrf);
        bs.copyResults(tmpFile);
        assertEquals("Should have zero-length file", 0, tmpFile.length());
        assertTrue("Source (remote) file should be deleted", lrf.isDeleted());

        bs = new BatchStatus("KB", emptyList, 0, null);
        try {
            bs.copyResults(noSuchFile);
            fail("Should have thrown exception on missing file");
        } catch (IllegalState e) {
            // expected
        }
        assertFalse("Should not have made a result file",
                noSuchFile.exists());

        try {
            bs.copyResults(null);
            fail("Should have throw ArgumentNotValid");
        } catch (ArgumentNotValid e) {
            //expected
        }
        assertFalse("Should not have made a result file",
                noSuchFile.exists());
    }

    public void testAppendResults() throws IOException {
        List<File> emptyList = Collections.emptyList();
        String fileContents = FileUtils.readFile(TestInfo.SAMPLE_FILE);
        TestRemoteFile lrf = new TestRemoteFile(TestInfo.SAMPLE_FILE, false,
                                                  false, false);
        BatchStatus bs = new BatchStatus("KB", emptyList, 0, lrf);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        bs.appendResults(out);
        assertEquals("Should have same contents in outputstream",
                fileContents, out.toString());
        assertTrue("Should have deleted remotefile",
                lrf.isDeleted());

        try {
            bs.appendResults(new OutputStream() {
                public void write(int b) throws IOException {
                    fail("Should not write anything to outputstream");
                }
            });
            fail("Should have failed on already read file");
        } catch (IllegalState e) {
            // expected
        }

        lrf = new TestRemoteFile(TestInfo.EMPTY_FILE, false, false, false);
        bs = new BatchStatus("KB", emptyList, 1, lrf);
        bs.appendResults(new OutputStream() {
            public void write(int b) throws IOException {
                fail("Should not write anything to outputstream");
            }
        });

        bs = new BatchStatus("KB", emptyList, 0, null);
        try {
            bs.appendResults(new OutputStream() {
                public void write(int b) throws IOException {
                    fail("Should not write anything to outputstream");
                }
            });
            fail("Should have failed on no result file");
        } catch (IllegalState e) {
            // expected
        }

        try {
            bs.copyResults(null);
            fail("Should have throw ArgumentNotValid");
        } catch (ArgumentNotValid e) {
            //expected
        }

    }
    public void testHasResultFile() throws IOException {
        List<File> emptyList = Collections.emptyList();
        TestRemoteFile lrf = new TestRemoteFile(TestInfo.SAMPLE_FILE, false,
                                                  false, false);
        BatchStatus bs = new BatchStatus("KB", emptyList, 0, lrf);
        File tmpFile = new File(TestInfo.WORKING_DIR, "newFile");
        assertTrue("Should have result file when given", bs.hasResultFile());
        bs.copyResults(tmpFile);
        assertFalse("Should not have result file after copying",
                bs.hasResultFile());
        FileUtils.copyFile(tmpFile, TestInfo.SAMPLE_FILE);

        lrf = new TestRemoteFile(TestInfo.SAMPLE_FILE, false, false, false);
        bs = new BatchStatus("KB", emptyList, 0, lrf);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        assertTrue("Should have result file when given", bs.hasResultFile());
        bs.appendResults(out);
        assertFalse("Should not have result file after appending",
                bs.hasResultFile());

        bs = new BatchStatus("KB", emptyList, 0, null);
        assertFalse("Should not have result file with no-result BS",
                bs.hasResultFile());
    }
}