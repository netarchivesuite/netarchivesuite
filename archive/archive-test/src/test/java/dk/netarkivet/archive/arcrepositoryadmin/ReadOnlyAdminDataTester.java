package dk.netarkivet.archive.arcrepositoryadmin;

import junit.framework.TestCase;
import dk.netarkivet.archive.ArchiveSettings;
import dk.netarkivet.common.exceptions.IllegalState;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.testutils.TestFileUtils;
import dk.netarkivet.testutils.preconfigured.ReloadSettings;

/**
 * Unit tests for the class ReadOnlyAdminData.
 */
@SuppressWarnings({ "deprecation"})
public class ReadOnlyAdminDataTester extends TestCase {
    ReloadSettings rs = new ReloadSettings();

    public ReadOnlyAdminDataTester(String s) {
        super(s);
    }

    public void setUp() {
        rs.setUp();
        TestFileUtils.copyDirectoryNonCVS(TestInfo.ORIGINALS_DIR,
                TestInfo.TEST_DIR);
        Settings.set(ArchiveSettings.DIRS_ARCREPOSITORY_ADMIN, TestInfo.TEST_DIR.getAbsolutePath());
    }

    public void tearDown() {
        FileUtils.removeRecursively(TestInfo.TEST_DIR);
        rs.tearDown();
    }

    public void testSynchronize() throws Exception {
        ReadOnlyAdminData ad = ReadOnlyAdminData.getInstance();

        try {
            ad.synchronize();
            fail("Should have thrown IllegalState if no file found.");
        } catch (IllegalState e) {
            // Should break if no file.
        }

        UpdateableAdminData updad = AdminData.getUpdateableInstance();
        ad.synchronize();
        assertEquals("Should start out with no files",
                0, ad.getAllFileNames().size());

        Thread.sleep(1000);
        updad.addEntry("foo", null, "bar"
        );
        assertEquals("Should not have noticed change without synch",
                0, ad.getAllFileNames().size());

        // Not a test of the method, but if the file does not look modifed,
        // synchronize will fail.  The sleep before update should ensure we
        // get beyond the granularity of file system datestamps.
        assertTrue("File should look modified",
                ad.lastModified < ad.adminDataFile.lastModified());

        ad.synchronize();
        assertEquals("Should have noticed new entry now",
                1, ad.getAllFileNames().size());

        updad.close();
        Thread.sleep(1000);

        // Also check that we clear the entries at update
        TestFileUtils.copyDirectoryNonCVS(
                TestInfo.NON_EMPTY_ADMIN_DATA_DIR_ORIG,
                TestInfo.TEST_DIR);
        ad.synchronize();
        // Now the "foo" file should be gone.
        assertEquals("Should see only new entries",
                5, ad.getAllFileNames().size());
    }
}
