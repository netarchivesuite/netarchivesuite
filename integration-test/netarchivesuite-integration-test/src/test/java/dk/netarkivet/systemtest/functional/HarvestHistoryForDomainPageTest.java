/* File:            $Id$
 * Revision:        $Revision$
 * Author:          $Author$
 * Date:            $Date$
 *
 * The Netarchive Suite - Software to domain and preserve websites
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

import java.lang.reflect.Method;
import java.util.List;

import dk.netarkivet.systemtest.HarvestUtils;
import dk.netarkivet.systemtest.SeleniumTest;
import dk.netarkivet.systemtest.environment.TestEnvironment;
import dk.netarkivet.systemtest.page.HarvestHistoryPageHelper;
import dk.netarkivet.systemtest.page.PageHelper;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.testng.Assert.assertTrue;
import static org.testng.AssertJUnit.assertEquals;

public class HarvestHistoryForDomainPageTest extends SeleniumTest {;

    @BeforeMethod(alwaysRun=true)
    public void setup(Method method) {
    }

    //@Test(groups = {"guitest","functest", "slow"})
    public void sortableHistoryTableTest() throws Exception {
        addDescription("Tests that the jobs listed on the 'Harvest History' page for a domain are" +
                "sortable by clicking on the .");
        addStep("Ensure that the at least two jobs have run for the default domain",
                "");
        HarvestUtils.ensureNumberOfHarvestsForDefaultDomain(2);

        addStep("Click the 'Harvest name' header link",
                "The headers should now be sorted alphabetically according to harvest name.");
        HarvestUtils.gotoHarvestHistoryForDomain(HarvestUtils.DEFAULT_DOMAIN);
        PageHelper.clickLink(HarvestHistoryPageHelper.HARVEST_NAME_HEADER);
        //assertColumnIsSorted(0, true);
        PageHelper.clickLink(HarvestHistoryPageHelper.HARVEST_NAME_HEADER);
        //assertColumnIsSorted(0, false);
    }

    @Test(groups = {"guitest","functest", "slow"})
    public void historyTablePagingTest() throws Exception {
        addStep("Ensure that at least harvests have finished for the default domain","");
        HarvestUtils.ensureNumberOfHarvestsForDefaultDomain(3);
        List<HarvestHistoryPageHelper.HarvestHistoryEntry> harvestHistory = HarvestHistoryPageHelper.readHarvestHistory();
        int PAGE_SIZE = 2;
        setHarvestStatusPageSize(PAGE_SIZE);

        addStep("Set the page size to " + PAGE_SIZE,"");

        addStep("Goto the harvest history for the default domain",
                "Only 2 harvests should be listed and the next link should be enabled.");
        HarvestUtils.gotoHarvestHistoryForDomain(HarvestUtils.DEFAULT_DOMAIN);
        assertEquals("Didn't find the expected 2 harvests on the first page",
                PageHelper.getWebDriver().findElements(By.xpath(
                        "//table[@class='selection_table']/tbody/tr[position()>1]")).size(), 2);

        addStep("Click the next link until the next link disappears",
                "All the pages should have been listed, 2 at a time");
        int harvestCounter = 0;
        int numberOfPages = (int)Math.ceil(harvestHistory.size()/PAGE_SIZE);
        for (int pageNumber = 1; pageNumber <= numberOfPages ; pageNumber++) {
            List<WebElement> rows =
                    PageHelper.getWebDriver().findElement(By.className("selection_table")).findElements(By.tagName("tr"));
            rows.remove(0); //Skip headers
            for (int rowNumber = 0; rowNumber < rows.size() ; rowNumber++) {
                WebElement row = rows.get(rowNumber);
                assertEquals(new HarvestHistoryPageHelper.HarvestHistoryEntry(row), harvestHistory.get(harvestCounter++));
            }
            boolean lastPage = pageNumber == numberOfPages;
            if (!lastPage) {
                driver.findElement(By.linkText("next")).click();
                PageHelper.waitForPageToLoad();
            } else {
                assertEquals("Not all harvests were found in the pages.", harvestCounter, harvestHistory.size());
                assertTrue(driver.findElements(By.linkText("next")).isEmpty());
            }
        }

        addStep("Click the previous link until the previous link disappears.",
                "All the pages should have been listed, 2 at a time backwords.");
        for (int pageNumber = numberOfPages; pageNumber >= 1;pageNumber--) {
            List<WebElement> rows =
                PageHelper.getWebDriver().findElement(By.className("selection_table")).findElements(By.tagName("tr"));
            rows.remove(0); //Skip headers
            for (int rowNumber = rows.size()-1; rowNumber >= 0 ; rowNumber--) {
                WebElement row = rows.get(rowNumber);
                assertEquals(new HarvestHistoryPageHelper.HarvestHistoryEntry(row), harvestHistory.get(--harvestCounter));
            }
            boolean firstPage = pageNumber == 1;
            if (!firstPage) {
                driver.findElement(By.linkText("previous")).click();
                PageHelper.waitForPageToLoad();
            } else {
                assertEquals("Not all harvests where found in the pages.", harvestCounter, 0);
                assertTrue(driver.findElements(By.linkText("previous")).isEmpty());
            }
        }
    }

    private void setHarvestStatusPageSize(int size) throws Exception {
        environmentManager.runTestXCommand(TestEnvironment.JOB_ADMIN_SERVER,
                "cp conf/settings_GUIApplication.xml conf/settings_GUIApplication.xml.original");
        environmentManager.replaceStringInFile(TestEnvironment.JOB_ADMIN_SERVER, "conf/settings_GUIApplication.xml",
                "</indexClient>",
                "</indexClient>" +
                "<webinterface><harvestStatus><defaultPageSize>" +
                        size
                 + "</defaultPageSize></harvestStatus></webinterface>"
        );

        applicationManager.restartGUI();
    }

    @BeforeMethod(alwaysRun=true)
    @AfterMethod(alwaysRun=true)
    private void cleanupGUIConfiguration() {
        try {
            environmentManager.runTestXCommand(TestEnvironment.JOB_ADMIN_SERVER,
                    "if [ -f conf/settings_GUIApplication.xml.original ]; then " +
                            "echo conf/settings_GUIApplication.xml.original exist, moving back.; " +
                            "conf/kill_GUIApplication.sh; " +
                            "mv conf/settings_GUIApplication.xml.original conf/settings_GUIApplication.xml; " +
                            " conf/start_GUIApplication.sh; " +
                     "fi");
            applicationManager.waitForGUIToStart(10);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}