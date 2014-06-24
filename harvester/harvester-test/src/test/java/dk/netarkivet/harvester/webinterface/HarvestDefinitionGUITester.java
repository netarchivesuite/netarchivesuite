package dk.netarkivet.harvester.webinterface;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.distribute.JMSConnectionMockupMQ;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.webinterface.GUIWebServer;
import dk.netarkivet.harvester.datamodel.DataModelTestCase;
import dk.netarkivet.harvester.datamodel.TestInfo;
import dk.netarkivet.testutils.preconfigured.ReloadSettings;

/**
 * Unit-test for the GUIWebServer class when the DefinitionsSiteSection
 * is loaded.
 * FIXME Some of these tests can be merged into the GUIWebServerTester.
 */
public class HarvestDefinitionGUITester extends DataModelTestCase {
    private GUIWebServer gui;

    public HarvestDefinitionGUITester(String s) {
        super(s);
    }
    ReloadSettings rs = new ReloadSettings();

    public void setUp() throws Exception {
        rs.setUp();
        super.setUp();
        // Add a DefinitionsSiteSection to the list of Sitesections being loaded
        // when GUIWebServer starts.
        Settings.set(
                CommonSettings.SITESECTION_WEBAPPLICATION,
                TestInfo.HARVESTDEFINITION_JSP_DIR);
        Settings.set(
                CommonSettings.SITESECTION_CLASS,
                TestInfo.HARVESTDEFINITION_SITESECTIONCLASS);

        JMSConnectionMockupMQ.useJMSConnectionMockupMQ();
    }

    public void tearDown() throws Exception {
        super.tearDown();
        if (gui != null) {
            gui.cleanup();
        }
        rs.tearDown();
    }

    public void testSettingsWebappFault() {
        try {
            Settings.set(CommonSettings.SITESECTION_WEBAPPLICATION,
                         "not_a_webapp");
            gui = GUIWebServer.getInstance();
            fail("Should throw an error on invalid webapp");
        } catch (IOFailure e) {
            //expected
        }
    }

    public void testSettingsPortFault() {
        try {
            Settings.set(CommonSettings.HTTP_PORT_NUMBER, "not_a_number");
            gui = GUIWebServer.getInstance();
            fail("Should throw an error on invalid port number");
        } catch (NumberFormatException e) {
            //expected
        }
    }

    public void testSettingsPortWrong() {
        Settings.set(CommonSettings.HTTP_PORT_NUMBER, "42");
        try {
            gui = GUIWebServer.getInstance();
            fail("Should throw an error on invalid port number");
        } catch (IOFailure e) {
            //expected
        }
    }

    /**
     * Test, that GUIWebServer.getInstance throws UnknownID exception
     * if templates tables does not contain the template with name
     * Settings.DOMAIN_DEFAULT_ORDERXML
     * This tests the partial solution of bug 916
     * @throws InterruptedException
     */
     public void testExitWithoutDefaultTemplateInTemplatesTable() throws InterruptedException {
         String[] webApps = Settings.getAll(
                 CommonSettings.SITESECTION_WEBAPPLICATION);
         boolean harvestdefinitionFound = false;
         for (String webapp : webApps) {
             if (webapp.contains("HarvestDefinition")) {
                 harvestdefinitionFound = true;
                 break;
             }
         }
         assertTrue("Test-requirement not met: "
                 + "DefinitionsSiteSection not in default settings",
                 harvestdefinitionFound);
     }

}
