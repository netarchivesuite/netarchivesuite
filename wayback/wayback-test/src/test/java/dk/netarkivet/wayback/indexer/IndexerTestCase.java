package dk.netarkivet.wayback.indexer;

import java.io.File;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.distribute.arcrepository.ArcRepositoryClientFactory;
import dk.netarkivet.common.distribute.arcrepository.LocalArcRepositoryClient;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.testutils.TestFileUtils;
import dk.netarkivet.testutils.preconfigured.ReloadSettings;
import dk.netarkivet.wayback.TestInfo;
import dk.netarkivet.wayback.WaybackSettings;

import junit.framework.TestCase;

public class IndexerTestCase extends TestCase {
    private String oldClient = System.getProperty(CommonSettings.ARC_REPOSITORY_CLIENT);
    private String oldFileDir = System.getProperty("settings.common.arcrepositoryClient.fileDir");
    protected static File tempdir = new File(Settings.get(WaybackSettings.WAYBACK_INDEX_TEMPDIR));

    ReloadSettings rs = new ReloadSettings();

    public void setUp() {
        rs.setUp();
        System.setProperty(WaybackSettings.HIBERNATE_HBM2DDL_AUTO, "create-drop");
        HibernateUtil.getSession().getSessionFactory().close();
        FileUtils.removeRecursively(TestInfo.WORKING_DIR);
        TestFileUtils.copyDirectoryNonCVS(TestInfo.ORIGINALS_DIR, TestInfo.WORKING_DIR);
        System.setProperty(CommonSettings.ARC_REPOSITORY_CLIENT, "dk.netarkivet.common.distribute.arcrepository.LocalArcRepositoryClient");
        System.setProperty("settings.common.arcrepositoryClient.fileDir", TestInfo.FILE_DIR.getAbsolutePath());
        System.setProperty(CommonSettings.REMOTE_FILE_CLASS, "dk.netarkivet.common.distribute.TestRemoteFile");
        assertTrue(ArcRepositoryClientFactory.getPreservationInstance() instanceof LocalArcRepositoryClient);
    }

    public void tearDown() {
        HibernateUtil.getSession().getSessionFactory().close();
        FileUtils.removeRecursively(TestInfo.WORKING_DIR);
        FileUtils.remove(TestInfo.LOG_FILE);
        if (oldClient != null) {
            System.setProperty(CommonSettings.ARC_REPOSITORY_CLIENT, oldClient);
        } else {
            System.setProperty(CommonSettings.ARC_REPOSITORY_CLIENT, "");
        }
        if (oldFileDir != null ) {
            System.setProperty("settings.common.arcrepositoryClient.fileDir", oldFileDir);
        } else {
            System.setProperty("settings.common.arcrepositoryClient.fileDir", "");
        }
        rs.tearDown();
    }

    public void testNothing() {
        assertTrue("This is here to stop junit complaining that there are no "
                   + "tests in this class", true);
    }
}
