/*
 * #%L
 * NetarchiveSuite System test
 * %%
 * Copyright (C) 2005 - 2018 The Royal Danish Library, 
 *             the National Library of France and the Austrian National Library.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 2.1 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */
package dk.netarkivet.systemtest.functional;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.testng.annotations.Test;

import dk.netarkivet.systemtest.AbstractSystemTest;
import dk.netarkivet.systemtest.Application;
import dk.netarkivet.systemtest.NASAssert;
import dk.netarkivet.systemtest.NASSystemUtil;
import dk.netarkivet.systemtest.page.PageHelper;

/**
 * Test specification: http://netarchive.dk/suite/TEST1 .
 */
@SuppressWarnings({"unused"})
public class SystemOverviewTest extends AbstractSystemTest {

    /**
     * Test specification: http://netarchive.dk/suite/It23JMXMailCheck .
     */
    @Test(groups = {"guitest", "functest"})
    public void generalTest() throws Exception {
        addDescription("Test specification: http://netarchive.dk/suite/It23JMXMailCheck");
        Set<Application> expectedApplicationSet = new HashSet<Application>(Arrays.asList(NASSystemUtil
                .getApplications()));
        int numberOfApps = expectedApplicationSet.size();
        int MAX_SECONDS_TO_WAIT = 120;
        int WAIT_INTERVAL = 10;
        addStep("Goto the Systemstate page and wait for the extected number of applications to appear.",
                expectedApplicationSet.size() + " should appear within " + MAX_SECONDS_TO_WAIT);
        for (int waitedSeconds = 0; waitedSeconds <= MAX_SECONDS_TO_WAIT; waitedSeconds = waitedSeconds + WAIT_INTERVAL) {
            PageHelper.reloadSubPage("Status/Monitor-JMXsummary.jsp");
            int numberOfAppsInOverview = retrieveSystemOverviewRows().size();
            log.debug(retrieveSystemOverviewRows().size() + "/" + numberOfApps + " apps appeared " + "in "
                    + waitedSeconds + " seconds");
            if (numberOfAppsInOverview >= numberOfApps) {
                break;
            }
            Thread.sleep(WAIT_INTERVAL * 1000);
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
        for (WebElement row : retrieveSystemOverviewRows()) {
            List<WebElement> rowCells = row.findElements(By.xpath("td"));
            String machine = rowCells.get(0).getText();
            String application = rowCells.get(1).getText();
            String instance_Id = rowCells.get(2).getText();
            String channel = rowCells.get(3).getText();
            String replica = rowCells.get(4).getText();
            log.debug("Checking row " + rowCounter + ", value is: " + machine + ": " + application);
            rowCounter++;
            displayedApplicationSet.add(new Application(machine, application, instance_Id, channel, replica));
        }

        NASAssert.assertEquals(expectedApplicationSet, displayedApplicationSet);
    }

    /**
     * Reads and return the rows of the system overview table excluding the headers.
     *
     * @return The rows of the system overview table.
     */
    private List<WebElement> retrieveSystemOverviewRows() {
        WebElement table = PageHelper.getWebDriver().findElement(By.id("system_state_table"));
        List<WebElement> tr_collection = table.findElements(By.tagName("tr"));
        tr_collection.remove(0);
        return tr_collection;
    }
}
