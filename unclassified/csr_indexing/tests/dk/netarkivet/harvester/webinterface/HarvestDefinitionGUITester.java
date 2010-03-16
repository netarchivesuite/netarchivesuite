/*$Id$
* $Revision$
* $Date$
* $Author$
*
* The Netarchive Suite - Software to harvest and preserve websites
* Copyright 2004-2009 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
*
* This library is free software; you can redistribute it and/or
* modify it under the terms of the GNU Lesser General Public
* License as published by the Free Software Foundation; either
* version 2.1 of the License, or (at your option) any later version.
*
* This library is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
* Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public
* License along with this library; if not, write to the Free Software
* Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
*/
package dk.netarkivet.harvester.webinterface;

import java.io.IOException;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.distribute.JMSConnectionMockupMQ;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.webinterface.GUIWebServer;
import dk.netarkivet.common.webinterface.SiteSection;
import dk.netarkivet.harvester.HarvesterSettings;
import dk.netarkivet.harvester.datamodel.DataModelTestCase;
import dk.netarkivet.harvester.datamodel.TemplateDAO;
import dk.netarkivet.harvester.datamodel.TestInfo;
import dk.netarkivet.harvester.scheduler.HarvestScheduler;
import dk.netarkivet.testutils.FileAsserts;
import dk.netarkivet.testutils.LogUtils;
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
     * Test that we actually start and stop the HarvestScheduler.
     * @throws InterruptedException
     * @throws IOException
     */
    public void testSchedulerStarted() throws IOException, InterruptedException {
        gui = GUIWebServer.getInstance();
        Thread.sleep(1000);
        LogUtils.flushLogs(HarvestScheduler.class.getName());
        FileAsserts.assertFileContains("Scheduler startup should be in log",
                "Creating HarvestScheduler", TestInfo.LOG_FILE);
        Thread.sleep(1000);
        gui.cleanup();
        Thread.sleep(1000);
        LogUtils.flushLogs(HarvestScheduler.class.getName());
        FileAsserts.assertFileContains("Scheduler shutdown should be in log",
                "HarvestScheduler closing down", TestInfo.LOG_FILE);

        // If we get here without exceptions, everything has shut down nicely

        // Except some threads may be still hanging around, keeping the DB alive.
        Thread.sleep(1000);
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
         
         TemplateDAO dao = TemplateDAO.getInstance();
         // remove default order.xml from dao
        dao.delete(Settings.get(HarvesterSettings.DOMAIN_DEFAULT_ORDERXML));
         try {
             gui = GUIWebServer.getInstance();
             
             for (SiteSection s: SiteSection.getSections()) {
                 System.out.println(s);
             }
             fail("Should fail if default template is gone");
         } catch (IOFailure e) {
             // expected
         }
     }

}
