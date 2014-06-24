package dk.netarkivet.deploy;

import java.io.File;
import java.net.URL;

import junit.framework.TestCase;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.testutils.TestFileUtils;

public class CompleteSettingsTester extends TestCase {
    @Override
    public void setUp() {
        FileUtils.removeRecursively(TestInfo.WORKING_DIR);
        FileUtils.removeRecursively(TestInfo.TMPDIR);

        TestFileUtils.copyDirectoryNonCVS(TestInfo.ORIGINALS_DIR,
            TestInfo.WORKING_DIR);
    }

    @Override
    public void tearDown() {
        FileUtils.removeRecursively(TestInfo.WORKING_DIR);
        FileUtils.removeRecursively(TestInfo.TMPDIR);
    }

    /**
     * Rebuilds the file src/dk/netarkivet/deploy/default_settings.xml. Eg.
     * this is not a real test.
     */

    public void testCompleteSettings() throws Exception {
    	URL url = this.getClass().getClassLoader().getResource("");
        File file = new File(url.toURI());
    	// ToDo The generation of the complete settings file should be moved
        // to the build functionality directly.
    	File settingsFile = new File(file, "dk/netarkivet/deploy/complete_settings.xml");
    	BuildCompleteSettings.buildCompleteSettings(settingsFile.getPath());
    }
}
