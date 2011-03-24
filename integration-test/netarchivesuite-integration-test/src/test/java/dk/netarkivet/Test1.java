/* File:            $Id$
 * Revision:        $Revision$
 * Author:          $Author$
 * Date:            $Date$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2010 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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
package dk.netarkivet;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.thoughtworks.selenium.DefaultSelenium;
import com.thoughtworks.selenium.Selenium;

/**
 * Test specification: http://netarchive.dk/suite/TEST1 .
 */
public class Test1 extends StandaloneTest {

    private Selenium selenium;

    @BeforeClass
    @Parameters( { "selenium.host", "selenium.port", "selenium.browser",
            "selenium.url" })
    public void startSelenium(@Optional("localhost")
    String host, @Optional("4444")
    String port, @Optional("*firefox")
    String browser, @Optional("http://kb-test-adm-001.kb.dk:8079/")
    String url) {
        this.selenium = new DefaultSelenium(host, Integer.parseInt(port),
                browser, "http://kb-test-adm-001.kb.dk:" + getPort() + "/");
        this.selenium.start();
    }

    @AfterClass(alwaysRun = true)
    public void stopSelenium() {
        this.selenium.stop();
    }

    /**
     * Test specification: http://netarchive.dk/suite/It23JMXMailCheck .
     */
    @Test
    public void step1() throws Exception {
        addDescription("Test specification: http://netarchive.dk/suite/It23JMXMailCheck");
        addStep("Goto the HarvestDefinition page", "");
        selenium.open("/HarvestDefinition/");
        addStep("Click the 'Systemstate link' in the left menu", "");
        selenium.click("link=Systemstate");
        selenium.waitForPageToLoad("10000");
        addStep(
                "Click the 'Overview of the system state' link in the left menu",
                "");
        selenium.click("link=Overview of the system state");
        selenium.waitForPageToLoad("3000");
        // We need to click the 'Instance id' link to differentiate between
        // instances of the same application running on the same machine
        addStep(
                "Click the 'Instance id' link (We need to do this to differentiate between "
                        + "instances of the same application running on the same machine)",
                "Verify that the the expected applications are running as they should.");
        selenium.click("link=Instance id");
        selenium.waitForPageToLoad("3000");

        int numberOfRows = selenium.getXpathCount(
                "//table[@id='system_state_table']/tbody/tr").intValue();
        Set<Application> expectedApplicationSet = new HashSet<Application>(
                Arrays.asList(NASSystemUtil.getApplications()));
        Set<Application> displayedApplicationSet = new HashSet<Application>();

        for (int rowCounter = 1; rowCounter < numberOfRows; rowCounter++) {
            System.out.println("Checking row "
                    + rowCounter
                    + ", value is: "
                    + selenium.getTable("system_state_table." + rowCounter
                            + ".0"));

            displayedApplicationSet.add(new Application(selenium
                    .getTable("system_state_table." + rowCounter + (".0")),
                    selenium.getTable("system_state_table." + rowCounter
                            + (".1")), selenium.getTable("system_state_table."
                            + rowCounter + (".2")), selenium
                            .getTable("system_state_table." + rowCounter
                                    + (".3")), selenium
                            .getTable("system_state_table." + rowCounter
                                    + (".4"))));
        }

        NASAssert.assertEquals(expectedApplicationSet, displayedApplicationSet);
    }

    /**
     * Test specification: http://netarchive.dk/suite/It10DefSelHarv .
     */
    @Test(dependsOnMethods = { "step1" })
    public void step2() throws Exception {

    }
}
