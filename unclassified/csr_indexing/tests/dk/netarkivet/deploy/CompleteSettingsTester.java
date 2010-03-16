package dk.netarkivet.deploy;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import junit.framework.TestCase;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.testutils.ReflectUtils;
import dk.netarkivet.testutils.TestFileUtils;
import dk.netarkivet.testutils.preconfigured.PreserveStdStreams;
import dk.netarkivet.testutils.preconfigured.PreventSystemExit;

public class CompleteSettingsTester extends TestCase{
    private PreserveStdStreams pss = new PreserveStdStreams(true);
    private PreventSystemExit pse = new PreventSystemExit();

    public void setUp() {
        pss.setUp();
        pse.setUp();
        FileUtils.removeRecursively(TestInfo.WORKING_DIR);
        FileUtils.removeRecursively(TestInfo.TMPDIR);

        TestFileUtils.copyDirectoryNonCVS(TestInfo.ORIGINALS_DIR,
                TestInfo.WORKING_DIR);
    }

    public void tearDown() {
        FileUtils.removeRecursively(TestInfo.WORKING_DIR);
        FileUtils.removeRecursively(TestInfo.TMPDIR);
        // reset Settings to before
        pse.tearDown();
        pss.tearDown();
    }

    /**
     * Tests if the complete settings file is correctly created.
     * If this test fails, it is most likely because the file tests/dk/netarkivet/deploy/
     * data/originals/complete_settings/complete_settings.xml is obsolete.
     * You probably need to rebuild the file src/dk/netarkivet/deploy/complete_settings.xml
     * using the program dk.netarkivet.deploy.BuildCompleteSettings and replace the version
     * in ../originals/complete_settings/complete_settings.xml with the new version.
     */
    public void testCompleteSettings() {
        try {
            // the output directory is not automatically created,
            // hence create it before running.
            FileUtils.createDir(TestInfo.TMPDIR);

            String[] args = { TestInfo.FILE_COMPLETE_SETTINGS.getAbsolutePath() };
            BuildCompleteSettings.main(args);

            String differences = TestFileUtils.compareDirsText(
                    TestInfo.COMPLETE_SETTINGS_DIR, TestInfo.TMPDIR);

            if (differences.length() > 0) {
                pss.tearDown();
                System.out.println("testDeployTest");
                System.out.println(differences);
                pss.setUp();
            }

            assertEquals("No differences expected", 0, differences.length());
        } catch (Exception e) {
            // give error if exception caught.
            assertEquals(e.getMessage(), -1, 0);
        }
    }
    
    /**
     * Method for testing the constructor of the CompleteSettings class.
     */
    public void testConstructor() {
        ReflectUtils.testUtilityConstructor(BuildCompleteSettings.class);
    }
}
