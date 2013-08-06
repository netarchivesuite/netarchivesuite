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
package dk.netarkivet.systemtest.functional;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import dk.netarkivet.systemtest.Application;
import dk.netarkivet.systemtest.NASAssert;
import dk.netarkivet.systemtest.NASSystemUtil;
import dk.netarkivet.systemtest.SeleniumTest;
import dk.netarkivet.systemtest.page.PageHelper;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.testng.annotations.Test;

import static org.testng.Assert.fail;

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
        Set<Application> expectedApplicationSet = new HashSet<Application>(
                Arrays.asList(NASSystemUtil.getApplications()));
        int numberOfApps = expectedApplicationSet.size();
        int MAX_SECONDS_TO_WAIT = 120;
        int WAIT_INTERVAL = 10;
        addStep("Goto the Systemstate page and wait for the extected number of applications to appear.", expectedApplicationSet.size() +
                " should appear within " + MAX_SECONDS_TO_WAIT);
        for (int waitedSeconds = 0; waitedSeconds <= MAX_SECONDS_TO_WAIT; waitedSeconds = waitedSeconds + WAIT_INTERVAL) {
            PageHelper.reloadSubPage("Status/Monitor-JMXsummary.jsp");
            int numberOfAppsInOverview = retrieveSystemOverviewRows().size();
            log.debug(retrieveSystemOverviewRows().size() + "/" + numberOfApps + " apps appeared " +
                    "in " + waitedSeconds + " seconds");
            if (numberOfAppsInOverview >= numberOfApps) {
                break;
            }
            if (waitedSeconds == MAX_SECONDS_TO_WAIT) {
                fail("Only " + numberOfAppsInOverview + " appeared in " + waitedSeconds + " seconds, expected " +
                        numberOfApps);
            }
            Thread.sleep(WAIT_INTERVAL*1000);
        }

        // We need to click the 'Instance id' link to differentiate between
        // instances of the same application running on the same machine
        addStep("Click the 'Instance id' link (We need to do this to differentiate between "
                        + "instances of the same application running on the same machine)",
                "Verify that the the expected applications are running as they should.");
        driver.findElement(By.linkText("Instance id")).click();
        PageHelper.waitForPageToLoad();
        Set<Application> displayedApplicationSet = new HashSet<Application>();
        Thread.sleep(5000);
        WebElement table = PageHelper.getWebDriver().findElement(By.id("system_state_table"));
        List<WebElement> tr_collection = table.findElements(By.tagName("tr"));
        int rowCounter = 1;
        for (WebElement row:retrieveSystemOverviewRows()) {
            List<WebElement> rowCells = row.findElements(By.xpath("td"));
            String machine = rowCells.get(0).getText();
            String application = rowCells.get(1).getText();
            String instance_Id = rowCells.get(2).getText();
            String priority = rowCells.get(3).getText();
            String replica = rowCells.get(4).getText();
            log.debug("Checking row " + rowCounter + ", value is: " + machine + ": " + application);
            rowCounter++;
            displayedApplicationSet.add(new Application(
                    machine, application, instance_Id, priority,replica
            ));
        }

        NASAssert.assertEquals(expectedApplicationSet, displayedApplicationSet);
    }

    /**
     * Reads and return the rows of the system overview table excluding the headers.
     * @return The rows of the system overview table.
     */
    private List<WebElement> retrieveSystemOverviewRows() {
        WebElement table = PageHelper.getWebDriver().findElement(By.id("system_state_table"));
        List<WebElement> tr_collection = table.findElements(By.tagName("tr"));
        tr_collection.remove(0);
        return tr_collection;
    }
}
