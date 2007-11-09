/*$Id$
* $Revision$
* $Date$
* $Author$
*
* The Netarchive Suite - Software to harvest and preserve websites
* Copyright 2004-2007 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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
import java.lang.reflect.Constructor;

import dk.netarkivet.common.Settings;
import dk.netarkivet.common.distribute.JMSConnectionTestMQ;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.UnknownID;
import dk.netarkivet.harvester.datamodel.DataModelTestCase;
import dk.netarkivet.harvester.datamodel.TemplateDAO;
import dk.netarkivet.harvester.datamodel.TestInfo;
import dk.netarkivet.harvester.scheduler.HarvestScheduler;
import dk.netarkivet.testutils.ClassAsserts;
import dk.netarkivet.testutils.FileAsserts;
import dk.netarkivet.testutils.LogUtils;

/**
 * @version $Id$
 */
public class HarvestDefinitionGUITester extends DataModelTestCase {
    private HarvestDefinitionGUI gui;

    public HarvestDefinitionGUITester(String s) {
        super(s);
    }

    public void setUp() throws Exception {
        super.setUp();
        Settings.set(Settings.SITESECTION_WEBAPPLICATION,
                dk.netarkivet.harvester.datamodel.TestInfo.HARVESTDEFINITION_JSP_DIR);
        Settings.set(Settings.SITESECTION_DEPLOYPATH,
                dk.netarkivet.harvester.datamodel.TestInfo.HARVESTDEFINITION_WEBBASE);
        JMSConnectionTestMQ.useJMSConnectionTestMQ();
    }

    public void tearDown() throws Exception {
        super.tearDown();
        if (gui != null) {
            gui.close();
        }
    }

    public void testSettingsWebappFault() {
        try {
            Settings.set(Settings.SITESECTION_WEBAPPLICATION, "not_a_webapp");
            gui = HarvestDefinitionGUI.getInstance();
            fail("Should throw an error on invalid webapp");
        } catch (IOFailure e) {
            //expected
        }
    }

    public void testSettingsPortFault() {
        try {
            Settings.set(Settings.HTTP_PORT_NUMBER, "not_a_number");
            gui = HarvestDefinitionGUI.getInstance();
            fail("Should throw an error on invalid port number");
        } catch (NumberFormatException e) {
            //expected
        }
    }

    public void testSettingsPortWrong() {
        Settings.set(Settings.HTTP_PORT_NUMBER, "42");
        try {
            gui = HarvestDefinitionGUI.getInstance();
            fail("Should throw an error on invalid port number");
        } catch (IOFailure e) {
            //expected
        }
    }

    public void testSettingsWrongContext() {
        Settings.set(Settings.SITESECTION_DEPLOYPATH, "not_a_context");
        try {
            gui = HarvestDefinitionGUI.getInstance();
            fail("Should throw an error on invalid context");
        } catch (IOFailure e) {
            //expected
        }
    }

    /**
     * Verify that the HarvestDefinitionGUI class has no public constructor.
     */
    public void testNoPublicConstructor() {
        Constructor[] ctors = HarvestDefinitionGUI.class.getConstructors();
        assertEquals("Found public constructors for BitarchiveServer.", 0, ctors.length);
    }

    /**
     * Test that HarvestDefinitionGUI is a singleton.
     * @throws NoSuchFieldException
     * @throws InterruptedException
     */
    public void testSingletonicity() throws NoSuchFieldException, InterruptedException {
        gui = ClassAsserts.assertSingleton(HarvestDefinitionGUI.class);

        // Need to wait a bit here for the sc<heduler threads to stop
        // Unfortunately, there are now up to three different instances
        // of HarvestScheduler running around, so finding the right thing to
        // wait for is tricky.  Trying just a little wait.
        Thread.sleep(1000);
    }

    /**
     * Test that we actually start and stop the HarvestScheduler.
     * @throws InterruptedException
     * @throws IOException
     */
    public void testSchedulerStarted() throws IOException, InterruptedException {
        gui = HarvestDefinitionGUI.getInstance();
        LogUtils.flushLogs(HarvestScheduler.class.getName());
        FileAsserts.assertFileContains("Scheduler startup should be in log",
                "Creating HarvestScheduler", TestInfo.LOG_FILE);
        Thread.sleep(1000);
        gui.close();
        LogUtils.flushLogs(HarvestScheduler.class.getName());
        FileAsserts.assertFileContains("Scheduler shutdown should be in log",
                "HarvestScheduler closing down", TestInfo.LOG_FILE);

        // If we get here without exceptions, everything has shut down nicely

        // Except some threads still hang around, keeping the DB alive.
        Thread.sleep(1000);
    }
    
    /**
     * Test, that HarvestDefinitionGUI.getInstance throws UnknownID exception
     * if templates tables does not contain the template with name 
     * Settings.DOMAIN_DEFAULT_ORDERXML
     * This tests the partial solution of bug 916
     * @throws InterruptedException
     */
     public void testExitWithoutDefaultTemplateInTemplatesTable() throws InterruptedException {
         TemplateDAO dao = TemplateDAO.getInstance();
         // remove default order.xml from dao
         dao.delete(Settings.get(Settings.DOMAIN_DEFAULT_ORDERXML));
         try {
             gui = HarvestDefinitionGUI.getInstance();
             fail("Should fail if default template is gone");
         } catch (UnknownID e) {
             // expected
         }
     }
}
