
package dk.netarkivet.harvester.indexserver;

import java.io.File;

import junit.framework.TestCase;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.distribute.JMSConnectionMockupMQ;
import dk.netarkivet.common.distribute.arcrepository.ArcRepositoryClientFactory;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.testutils.TestFileUtils;
import dk.netarkivet.testutils.preconfigured.ReloadSettings;
import dk.netarkivet.testutils.preconfigured.UseTestRemoteFile;

/**
 * Version of TestCase that is used to test the cache system.
 */
public class CacheTestCase extends TestCase {
    private UseTestRemoteFile utrf = new UseTestRemoteFile();
    ReloadSettings rs = new ReloadSettings();

    public CacheTestCase(String string) {
        super(string);
    }

    public void setUp() throws Exception {
        super.setUp();
        rs.setUp();
        // This just is needed to allow an instance of CDXIndexCache to be made
        JMSConnectionMockupMQ.useJMSConnectionMockupMQ();
        utrf.setUp();
        TestFileUtils.copyDirectoryNonCVS(TestInfo.ORIGINALS_DIR,
                TestInfo.WORKING_DIR);
        Settings.set(CommonSettings.DIR_COMMONTEMPDIR, new File(TestInfo.WORKING_DIR, "tmp").getAbsolutePath());
        Settings.set(CommonSettings.CACHE_DIR, new File(TestInfo.WORKING_DIR, "cache").getAbsolutePath());
        FileUtils.createDir(new File(TestInfo.WORKING_DIR, "tmp"));
    }

    public void tearDown() throws Exception {
        FileUtils.removeRecursively(TestInfo.WORKING_DIR);
        ArcRepositoryClientFactory.getViewerInstance().close();
        utrf.tearDown();
        super.tearDown();
        rs.tearDown();
    }
}
