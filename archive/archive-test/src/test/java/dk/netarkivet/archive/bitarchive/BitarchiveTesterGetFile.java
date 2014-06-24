package dk.netarkivet.archive.bitarchive;

import java.io.File;
import java.io.IOException;

import dk.netarkivet.testutils.FileAsserts;
import dk.netarkivet.testutils.LogUtils;


public class BitarchiveTesterGetFile extends BitarchiveTestCase {
    private static final File ORIGINALS_DIR =
            new File(new File(TestInfo.DATA_DIR, "getFile"), "originals");

    public BitarchiveTesterGetFile(String s) {
        super(s);
    }

    protected File getOriginalsDir() {
        return ORIGINALS_DIR;
    }

    public void setUp() throws Exception {
        super.setUp();
    }

    public void tearDown() throws Exception {
        super.tearDown();
    }

    public void testGetFile_Failure() throws Exception {
        String arcFileID = "test";
        File result = archive.getFile(arcFileID);
        assertNull("Non-existing file should give null result", result);
        LogUtils.flushLogs(Bitarchive.class.getName());
        FileAsserts.assertFileContains("Log should mention non-success",
                                       arcFileID + "' not found",
                                       TestInfo.LOGFILE);
    }

    public void testGetFile_Success() throws IOException {
        String arcFileID = "Upload1.ARC";
        File result = archive.getFile(arcFileID);
        assertEquals("Result should be the expected file",
                     new File(TestInfo.FILE_DIR, arcFileID).getCanonicalPath(),
                     result.getCanonicalPath());
        LogUtils.flushLogs(Bitarchive.class.getName());
        FileAsserts.assertFileContains("Log should mention start",
                                       "Get file '" + arcFileID,
                                       TestInfo.LOGFILE);
        FileAsserts.assertFileContains("Log should mention success",
                                       "Getting file '" + result,
                                       TestInfo.LOGFILE);
    }
}
