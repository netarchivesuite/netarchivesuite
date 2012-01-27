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

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import dk.netarkivet.systemtest.SystemTest;

/**
 * Test specification: http://netarchive.dk/suite/TEST1 .
 */
public class StressTest extends SystemTest {
    
    /**
     * Test specification: http://netarchive.dk/suite/It23JMXMailCheck .
     */
    @Test
    public void generalTest() throws Exception {
        addDescription("Test specification: http://netarchive.dk/suite/It23JMXMailCheck");
        addStep("Goto the HarvestDefinition page", "");
    }
    
    /**
     * Takes care of importing Netarkiv.dk production data in the databases.
     */
    @BeforeClass
    public void setupProductionDatabases() throws Exception  {
        environmentManager.runCommandWithEnvironment("ssh test@kb-test-adm-001.kb.dk rm -rf /tmp/fullhddb");
        environmentManager.runCommandWithEnvironment("ssh test@kb-test-adm-001.kb.dk rm -rf /tmp/adminDB");
        environmentManager.runCommandWithEnvironment("ssh test@kb-test-acs-001.kb.dk rm -rf /tmp/CS");
        environmentManager.runCommandWithEnvironment("scp -r /home/test/prod-backup/adminDB test@kb-test-adm-001.kb.dk:/tmp");
        environmentManager.runCommandWithEnvironment("scp -r /home/test/prod-backup/fullhddb test@kb-test-adm-001.kb.dk:/tmp", 3000);
        environmentManager.runCommandWithEnvironment("scp -r   /home/test/prod-backup/CS test@kb-test-acs-001.kb.dk:/tmp");
    }
}
