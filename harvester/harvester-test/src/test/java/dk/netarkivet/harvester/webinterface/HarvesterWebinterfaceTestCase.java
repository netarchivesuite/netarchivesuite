
package dk.netarkivet.harvester.webinterface;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.webinterface.WebinterfaceTestCase;
import dk.netarkivet.harvester.datamodel.DBSpecifics;
import dk.netarkivet.harvester.datamodel.DatabaseTestUtils;
import dk.netarkivet.harvester.datamodel.GlobalCrawlerTrapListDBDAO;
import dk.netarkivet.harvester.datamodel.HarvestDAOUtils;
import dk.netarkivet.testutils.TestFileUtils;
import dk.netarkivet.testutils.preconfigured.ReloadSettings;

import java.io.File;

/**
 * A TestCase subclass specifically tailored to test webinterface classes,
 * primarily the classes in dk.netarkivet.harvester.webinterface:
 * HarvestStatusTester, EventHarvestTester, DomainDefinitionTester,
 * ScheduleDefinitionTester, SnapshotHarvestDefinitionTester but also
 * dk.netarkivet.archive.webinterface.BitpreserveFileStatusTester
 */
public class HarvesterWebinterfaceTestCase extends WebinterfaceTestCase {
    static final File HARVEST_DEFINITION_BASEDIR
            = new File(TestInfo.WORKING_DIR, "harvestdefinitionbasedir");
    ReloadSettings rs = new ReloadSettings();

    public HarvesterWebinterfaceTestCase(String s) {
        super(s);
    }

    public void setUp() throws Exception {
        super.setUp();
        rs.setUp();
        TestFileUtils.copyDirectoryNonCVS(TestInfo.ORIGINALS_DIR,
                TestInfo.WORKING_DIR);
        HarvestDAOUtils.resetDAOs();
        GlobalCrawlerTrapListDBDAO.reset();

        Settings.set(CommonSettings.DB_BASE_URL, "jdbc:derby:"
                                            + HARVEST_DEFINITION_BASEDIR.getCanonicalPath()
                                            + "/fullhddb");
        DatabaseTestUtils.getHDDB(TestInfo.DBFILE, "fullhddb",
                                  HARVEST_DEFINITION_BASEDIR);
        DBSpecifics.getInstance().updateTables();
    }

    public void tearDown() throws Exception {
        DatabaseTestUtils.dropHDDB();
      HarvestDAOUtils.resetDAOs();
      GlobalCrawlerTrapListDBDAO.reset();
        FileUtils.removeRecursively(TestInfo.WORKING_DIR);
        rs.tearDown();
        super.tearDown();
    }

}
   