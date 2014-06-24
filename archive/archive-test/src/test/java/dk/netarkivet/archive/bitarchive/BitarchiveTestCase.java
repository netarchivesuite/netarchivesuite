package dk.netarkivet.archive.bitarchive;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.logging.LogManager;

import junit.framework.TestCase;

import dk.netarkivet.archive.ArchiveSettings;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.testutils.TestFileUtils;
import dk.netarkivet.testutils.preconfigured.MockupJMS;
import dk.netarkivet.testutils.preconfigured.ReloadSettings;
import dk.netarkivet.testutils.preconfigured.UseTestRemoteFile;

/** A collection of setup/teardown stuff usable by most bitarchive tests.. */
public abstract class BitarchiveTestCase extends TestCase {
    private UseTestRemoteFile rf = new UseTestRemoteFile();
    protected static Bitarchive archive;
    ReloadSettings rs = new ReloadSettings();

    MockupJMS mj = new MockupJMS();
    /** Make a new BitarchiveTestCase using the given directory for originals.
     *
     * @param s Name of the test.
     */
    public BitarchiveTestCase(String s) {
        super(s);
    }

    protected abstract File getOriginalsDir();

    public void setUp() throws Exception {
        super.setUp();
        rs.setUp();
        mj.setUp();
        FileUtils.removeRecursively(TestInfo.WORKING_DIR);
        try {
            // This forces an emptying of the log file.
            FileInputStream fis = new FileInputStream(TestInfo.TESTLOGPROP);
            LogManager.getLogManager().readConfiguration(fis);
            fis.close();
        } catch (IOException e) {
            fail("Could not load the testlog.prop file: " + e);
        }
        try {
            // Copy over the "existing" bit archive.
            TestFileUtils.copyDirectoryNonCVS(getOriginalsDir(),
                                              TestInfo.WORKING_DIR);
            Settings.set(ArchiveSettings.BITARCHIVE_SERVER_FILEDIR, TestInfo.WORKING_DIR.getAbsolutePath());
            archive = Bitarchive.getInstance();
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
        rf.setUp();
    }

    public void tearDown() throws Exception {
        if (archive != null) {
            archive.close();
        }
        FileUtils.removeRecursively(TestInfo.WORKING_DIR);
        mj.tearDown();
        rf.tearDown();
        rs.tearDown();
        super.tearDown();
    }
}
