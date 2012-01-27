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
package dk.netarkivet.systemtest;

import org.jaccept.structure.ExtendedTestCase;
import org.testng.annotations.BeforeTest;

public class SystemTest extends ExtendedTestCase {
    protected TestEnvironmentManager environmentManager;
    
    @BeforeTest (alwaysRun=true)
    public void setupTest() {
        startTestSystem();
    }
    
    private void startTestSystem() {
        if (System.getProperty("systemtest.deploy", "false").equals("true")) {
            try {
                environmentManager.runCommandWithEnvironment(getStartupScript());
            } catch (Exception e) {
                throw new RuntimeException("Failed to start test system");
            }
        }
    }

    /**
     * Defines the default test system startup script to run. May be overridden by 
     * subclasses classes.
     * @return The startup script to run.
     */
    protected String getStartupScript() {
        return "all_test_db.sh";
    }

}