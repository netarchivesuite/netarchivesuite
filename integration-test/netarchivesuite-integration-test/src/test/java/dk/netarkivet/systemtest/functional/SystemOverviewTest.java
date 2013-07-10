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
import dk.netarkivet.systemtest.page.PageHelper.MenuPages;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
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
        addStep("Goto the Systemstate page", "");
        PageHelper.gotoPage(MenuPages.OverviewOfTheSystemState);
        // We need to click the 'Instance id' link to differentiate between
        // instances of the same application running on the same machine
        addStep("Click the 'Instance id' link (We need to do this to differentiate between "
                        + "instances of the same application running on the same machine)",
                "Verify that the the expected applications are running as they should.");
        driver.findElement(By.linkText("Instance id")).click();
        PageHelper.waitForPageToLoad();
        Set<Application> expectedApplicationSet = new HashSet<Application>(
                Arrays.asList(NASSystemUtil.getApplications()));
        Set<Application> displayedApplicationSet = new HashSet<Application>();
        Thread.sleep(5000);
        WebElement table = PageHelper.getWebDriver().findElement(By.id("system_state_table"));
        List<WebElement> tr_collection = table.findElements(By.tagName("tr"));
        for (int rowCounter = 1; rowCounter < tr_collection.size(); rowCounter++) {
            WebElement row = tr_collection.get(rowCounter);
            List<WebElement> rowCells = row.findElements(By.xpath("td"));
            String machine = rowCells.get(0).getText();
            String application = rowCells.get(1).getText();
            String instance_Id = rowCells.get(2).getText();
            String priority = rowCells.get(3).getText();
            String replica = rowCells.get(4).getText();
            log.debug("Checking row " + rowCounter + ", value is: " + machine + ": " + application);

            displayedApplicationSet.add(new Application(
                    machine, application, instance_Id, priority,replica
            ));
        }

        NASAssert.assertEquals(expectedApplicationSet, displayedApplicationSet);
    }
}
