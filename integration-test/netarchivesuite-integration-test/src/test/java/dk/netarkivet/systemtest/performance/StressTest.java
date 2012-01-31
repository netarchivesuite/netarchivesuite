/* File:            $Id$
 * Revision:        $Revision$
 * Author:          $Author$
 * Date:            $Date$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2011 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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
package dk.netarkivet.systemtest.performance;

import org.jaccept.structure.ExtendedTestCase;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import dk.netarkivet.systemtest.TestEnvironment;
import dk.netarkivet.systemtest.TestEnvironmentManager;

/**
 * Test specification: https://sbforge.org/display/NAS/TEST+7.
 */
public class StressTest extends ExtendedTestCase {
    /** Handles the bash command functionality in the test environment. */
    protected TestEnvironmentManager environmentManager;
    
    /**
     * Test specification: http://netarchive.dk/suite/It23JMXMailCheck .
     */
    @Test (groups = {"stresstest"})
    public void generalTest() throws Exception {
        addDescription("Test specification: https://sbforge.org/display/NAS/TEST+7");
    }

    @BeforeTest (alwaysRun=true)
    public void setupTest() {
        environmentManager = new TestEnvironmentManager("Stresstest", 8072);
    }

    @BeforeClass
    public void setupClass() throws Exception {
        shutdownPreviousTest();
        //fetchProductionData();
        deployTestComponents();
        updateTestDatabasesWithProdData();
    }
    
    private void shutdownPreviousTest()  throws Exception{
        addStep("Shutting down any previously running test.", "");
        environmentManager.runCommand("stop_test.sh");
    }
    
    private void fetchProductionData() throws Exception {
        addStep("Copying production databases to the relevant test servers.", "");
        environmentManager.runCommand("ssh test@kb-test-adm-001.kb.dk rm -rf /tmp/fullhddb");
        environmentManager.runCommand("ssh test@kb-test-adm-001.kb.dk rm -rf /tmp/adminDB");
        environmentManager.runCommand("ssh test@kb-test-acs-001.kb.dk rm -rf /tmp/CS");
        environmentManager.runCommand("scp -r /home/test/prod-backup/adminDB test@kb-test-adm-001.kb.dk:/tmp");
        environmentManager.runCommand("scp -r /home/test/prod-backup/fullhddb test@kb-test-adm-001.kb.dk:/tmp", 3000);
        environmentManager.runCommand("scp -r   /home/test/prod-backup/CS test@kb-test-acs-001.kb.dk:/tmp");
    }
    
    private void deployTestComponents() throws Exception {
        addStep("Installing components.", "");
        environmentManager.runCommand("install_test.sh");
    }
    
    private void updateTestDatabasesWithProdData() throws Exception {
        addStep("Updating test databases with production data", "");
        environmentManager.runCommand(TestEnvironment.JOB_ADMIN_SERVER, 
                "rm -rf /home/test/$TESTX/harvestDatabase/fullhddb");
        environmentManager.runCommand(TestEnvironment.JOB_ADMIN_SERVER, 
                "ln -s /tmp/fullhddb /home/test/$TESTX/harvestDatabase/fullhddb");
        environmentManager.runCommand(TestEnvironment.ARCHIVE_ADMIN_SERVER, 
                "rm -rf /home/test/$TESTX/adminDB");
        environmentManager.runCommand(TestEnvironment.ARCHIVE_ADMIN_SERVER, 
                "ln -s /tmp/adminDB /home/test/$TESTX/adminDB");
    }
}
