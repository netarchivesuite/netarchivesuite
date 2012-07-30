/* File:            $Id$
 * Revision:        $Revision$
 * Author:          $Author$
 * Date:            $Date$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2012 The Royal Danish Library, the Danish State and
 * University Library, the National Library of France and the Austrian
 * National Library.
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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.openqa.selenium.By;
import org.testng.annotations.Test;

/**
 * Test specification: http://netarchive.dk/suite/TEST1 .
 */
public class SystemOverviewTest extends SeleniumTest {

    /**
     * Test specification: http://netarchive.dk/suite/It23JMXMailCheck .
     */
    @Test (groups = {"guitest","functest"})
    public void generalTest() throws Exception {
        addDescription("Test specification: http://netarchive.dk/suite/It23JMXMailCheck");
        addStep("Goto the HarvestDefinition page", "");
        driver.get(baseUrl + "/HarvestDefinition/");
        addStep("Click the 'Systemstate link' in the left menu", "The system state page is displayed");
        driver.findElement(By.linkText("Systemstate")).click();
        // We need to click the 'Instance id' link to differentiate between
        // instances of the same application running on the same machine
        addStep("Click the 'Instance id' link (We need to do this to differentiate between "
                        + "instances of the same application running on the same machine)",
                "Verify that the the expected applications are running as they should.");
        driver.findElement(By.linkText("Instance id")).click();
        int numberOfRows = selenium.getXpathCount(
                "//table[@id='system_state_table']/tbody/tr").intValue();
        Set<Application> expectedApplicationSet = new HashSet<Application>(
                Arrays.asList(NASSystemUtil.getApplications()));
        Set<Application> displayedApplicationSet = new HashSet<Application>();

        for (int rowCounter = 1; rowCounter < numberOfRows; rowCounter++) {
            log.debug("Checking row " + rowCounter + ", value is: "
                    + selenium.getTable("system_state_table." + rowCounter
                          + ".0") + ": "
                    + selenium.getTable("system_state_table." + rowCounter
                                    + ".1"));

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
}
