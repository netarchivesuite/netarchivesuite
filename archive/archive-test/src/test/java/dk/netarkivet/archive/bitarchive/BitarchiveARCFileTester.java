package dk.netarkivet.archive.bitarchive;

import java.io.File;

import junit.framework.TestCase;

import dk.netarkivet.common.exceptions.ArgumentNotValid;

/**
 * Unit tests for the class BitarchiveARCFile.
 *
 */
public class BitarchiveARCFileTester extends TestCase {
    private static final File EXISTING_FILE = 
        new File(TestInfo.ORIGINALS_DIR, "Upload2.ARC");
    private static final File NON__EXISTING__FILE = new File("Test2");

    public BitarchiveARCFileTester(String s) {
        super(s);
    }

    public void setUp() {
    }

    public void tearDown() {
    }

    public void testBitArchiveArcFile() throws Exception {
        try {
            new BitarchiveARCFile("Test", null);
            fail("Should fail on null argument");
        } catch (ArgumentNotValid e) {
            //expected
        }

        try {
            new BitarchiveARCFile(null, new File("Test"));
            fail("Should fail on null argument");
        } catch (ArgumentNotValid e) {
            //expected
        }

        try {
            new BitarchiveARCFile("", new File("Test"));
            fail("Should fail on empty argument");
        } catch (ArgumentNotValid e) {
            //expected
        }

        try {
            new BitarchiveARCFile("Test", new File(""));
            fail("Should fail on empty argument");
        } catch (ArgumentNotValid e) {
            //expected
        }

        BitarchiveARCFile f = new BitarchiveARCFile(
                "Test1", NON__EXISTING__FILE);
        assertEquals("File names should be the same",
                     f.getName(), "Test1");
        assertEquals("File paths should be the same",
                     f.getFilePath().getCanonicalPath(),
                     NON__EXISTING__FILE.getCanonicalPath());
        assertFalse("File should not exist", f.exists());

        BitarchiveARCFile f2 = new BitarchiveARCFile("Test1", EXISTING_FILE);
        assertFalse("File should exist", f.exists());
        assertEquals("Should get right file size", 136663, f2.getSize());

    }
}