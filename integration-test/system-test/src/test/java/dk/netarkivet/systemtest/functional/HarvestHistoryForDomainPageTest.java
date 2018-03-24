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

import static org.testng.Assert.assertTrue;
import static org.testng.AssertJUnit.assertEquals;

import java.lang.reflect.Method;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import dk.netarkivet.systemtest.AbstractSystemTest;
import dk.netarkivet.systemtest.HarvestUtils;
import dk.netarkivet.systemtest.environment.TestEnvironment;
import dk.netarkivet.systemtest.page.HarvestHistoryPageHelper;
import dk.netarkivet.systemtest.page.PageHelper;

/** The tests here are run with a low priority, eg. they are run last thereby enabling other tests to run harvests
 * prior to the tests here. This will prevent the  calls to 'ensureNumberOfHarvestsForDefaultDomain' from having to
 * run alle the harvests needed here. */
public class HarvestHistoryForDomainPageTest extends AbstractSystemTest {

    @Test(priority=10, groups = {"guitest", "functest", "slow"})
    public void sortableHistoryTableTest() throws Exception {
        addDescription("Tests that the jobs listed on the 'Harvest History' page for a domain are"
                + "sortable by clicking on the .");
        addStep("Ensure that the at least two jobs have run for the default domain", "");
        HarvestUtils.ensureNumberOfHarvestsForDefaultDomain(2);

        addStep("Click the 'Harvest name' header link",
                "The table should now be sorted alphabetically according to harvest name.");
        HarvestUtils.gotoHarvestHistoryForDomain(HarvestUtils.DEFAULT_DOMAIN);
        PageHelper.clickLink(HarvestHistoryPageHelper.HARVEST_NAME_HEADER);
        PageHelper.waitForPageToLoad();
        assertColumnIsSorted(0, true);
        PageHelper.clickLink(HarvestHistoryPageHelper.HARVEST_NAME_HEADER);
        PageHelper.waitForPageToLoad();
        assertColumnIsSorted(0, false);

        addStep("Click the 'Run number' header link", "The table should now be sorted alphabetically according to"
                + " run number.");
        HarvestUtils.gotoHarvestHistoryForDomain(HarvestUtils.DEFAULT_DOMAIN);
        PageHelper.clickLink(HarvestHistoryPageHelper.RUN_NUMBER_HEADER);
        PageHelper.waitForPageToLoad();
        assertColumnIsSorted(1, true);
        PageHelper.clickLink(HarvestHistoryPageHelper.RUN_NUMBER_HEADER);
        PageHelper.waitForPageToLoad();
        assertColumnIsSorted(1, false);

        addStep("Click the 'Run ID' header link", "The table should now be sorted alphabetically according to"
                + " run ID.");
        HarvestUtils.gotoHarvestHistoryForDomain(HarvestUtils.DEFAULT_DOMAIN);
        PageHelper.clickLink(HarvestHistoryPageHelper.RUN_ID_HEADER);
        PageHelper.waitForPageToLoad();
        assertColumnIsSorted(2, true);
        PageHelper.clickLink(HarvestHistoryPageHelper.RUN_ID_HEADER);
        PageHelper.waitForPageToLoad();
        assertColumnIsSorted(2, false);

        addStep("Click the 'Configuration' header link", "The table should now be sorted alphabetically according to"
                + " Configuration.");
        HarvestUtils.gotoHarvestHistoryForDomain(HarvestUtils.DEFAULT_DOMAIN);
        PageHelper.clickLink(HarvestHistoryPageHelper.CONFIGURATION_HEADER);
        PageHelper.waitForPageToLoad();
        assertColumnIsSorted(3, true);
        PageHelper.clickLink(HarvestHistoryPageHelper.CONFIGURATION_HEADER);
        PageHelper.waitForPageToLoad();
        assertColumnIsSorted(3, false);

        addStep("Click the 'Start time' header link", "The table should now be sorted alphabetically according to"
                + " Start time.");
        HarvestUtils.gotoHarvestHistoryForDomain(HarvestUtils.DEFAULT_DOMAIN);
        PageHelper.clickLink(HarvestHistoryPageHelper.START_TIME_HEADER);
        PageHelper.waitForPageToLoad();
        assertColumnIsSorted(4, true);
        PageHelper.clickLink(HarvestHistoryPageHelper.START_TIME_HEADER);
        PageHelper.waitForPageToLoad();
        assertColumnIsSorted(4, false);

        addStep("Click the 'End time' header link", "The table should now be sorted alphabetically according to"
                + " end time.");
        HarvestUtils.gotoHarvestHistoryForDomain(HarvestUtils.DEFAULT_DOMAIN);
        PageHelper.clickLink(HarvestHistoryPageHelper.END_TIME_HEADER);
        PageHelper.waitForPageToLoad();
        assertColumnIsSorted(5, true);
        PageHelper.clickLink(HarvestHistoryPageHelper.END_TIME_HEADER);
        PageHelper.waitForPageToLoad();
        assertColumnIsSorted(5, false);

        addStep("Click the 'Bytes Harvested' header link",
                "The table should now be sorted alphabetically according to 'Bytes Harvested'.");
        HarvestUtils.gotoHarvestHistoryForDomain(HarvestUtils.DEFAULT_DOMAIN);
        PageHelper.clickLink(HarvestHistoryPageHelper.BYTES_HARVESTED_HEADER);
        PageHelper.waitForPageToLoad();
        assertColumnIsSorted(6, true);
        PageHelper.clickLink(HarvestHistoryPageHelper.BYTES_HARVESTED_HEADER);
        PageHelper.waitForPageToLoad();
        assertColumnIsSorted(6, false);

        addStep("Click the 'Documents Harvested' header link",
                "The table should now be sorted alphabetically according to 'Documents Harvested'.");
        HarvestUtils.gotoHarvestHistoryForDomain(HarvestUtils.DEFAULT_DOMAIN);
        PageHelper.clickLink(HarvestHistoryPageHelper.DOCUMENTS_HARVESTED_HEADER);
        PageHelper.waitForPageToLoad();
        assertColumnIsSorted(7, true);
        PageHelper.clickLink(HarvestHistoryPageHelper.DOCUMENTS_HARVESTED_HEADER);
        PageHelper.waitForPageToLoad();
        assertColumnIsSorted(7, false);

        addStep("Click the 'Stopped due to' header link",
                "The table should now be sorted alphabetically according to 'Stopped due to'.");
        HarvestUtils.gotoHarvestHistoryForDomain(HarvestUtils.DEFAULT_DOMAIN);
        PageHelper.clickLink(HarvestHistoryPageHelper.STOPPED_DUE_TO_HEADER);
        PageHelper.waitForPageToLoad();
        assertColumnIsSorted(8, true);
        PageHelper.clickLink(HarvestHistoryPageHelper.STOPPED_DUE_TO_HEADER);
        PageHelper.waitForPageToLoad();
        assertColumnIsSorted(8, false);
    }

    @Test(priority=10, groups = {"guitest", "functest", "slow"})
    public void historyTablePagingTest() throws Exception {
        addDescription("Testes that the paging functionality works correctly " + "for the harvest history");
        addStep("Ensure that at least harvests have finished for the default domain", "");
        HarvestUtils.ensureNumberOfHarvestsForDefaultDomain(3);
        List<HarvestHistoryPageHelper.HarvestHistoryEntry> harvestHistory = HarvestHistoryPageHelper
                .readHarvestHistory();
        int PAGE_SIZE = 2;
        setHarvestStatusPageSize(PAGE_SIZE);

        addStep("Set the page size to " + PAGE_SIZE, "");

        addStep("Goto the harvest history for the default domain",
                "Only 2 harvests should be listed and the next link should be enabled.");
        HarvestUtils.gotoHarvestHistoryForDomain(HarvestUtils.DEFAULT_DOMAIN);
        assertEquals(
                "Didn't find the expected 2 harvests on the first page",
                PageHelper.getWebDriver()
                        .findElements(By.xpath("//table[@class='selection_table']/tbody/tr[position()>1]")).size(), 2);

        addStep("Click the next link until the next link disappears",
                "All the pages should have been listed, 2 at a time");
        int harvestCounter = 0;
        int numberOfPages = (int) Math.ceil((float) harvestHistory.size() / PAGE_SIZE);
        for (int pageNumber = 1; pageNumber <= numberOfPages; pageNumber++) {
            List<WebElement> rows = PageHelper.getWebDriver().findElement(By.className("selection_table"))
                    .findElements(By.tagName("tr"));
            rows.remove(0); // Skip headers
            for (int rowNumber = 0; rowNumber < rows.size(); rowNumber++) {
                WebElement row = rows.get(rowNumber);
                assertEquals(new HarvestHistoryPageHelper.HarvestHistoryEntry(row),
                        harvestHistory.get(harvestCounter++));
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
                "All the pages should have been listed, " + "2 at a time backwards.");
        for (int pageNumber = numberOfPages; pageNumber >= 1; pageNumber--) {
            List<WebElement> rows = PageHelper.getWebDriver().findElement(By.className("selection_table"))
                    .findElements(By.tagName("tr"));
            rows.remove(0); // Skip headers
            for (int rowNumber = rows.size() - 1; rowNumber >= 0; rowNumber--) {
                WebElement row = rows.get(rowNumber);
                assertEquals(new HarvestHistoryPageHelper.HarvestHistoryEntry(row),
                        harvestHistory.get(--harvestCounter));
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

    @Test(priority=10, groups = {"guitest", "functest", "slow"})
    public void historySortedTablePagingTest() throws Exception {
        addDescription("Tests that sorting is maintained when paging through " + "the harvest history");
        addStep("Ensure that at least harvests have finished for the default domain", "");
        //Note that this would be more efficient if we ran SelectiveHarvestTest first as there would already be
        //enough harvest history of pligtaflevering.dk to do this test straight away.
        HarvestUtils.ensureNumberOfHarvestsForDefaultDomain(3);

        addStep("Click the 'End time' header link twice", "The table should now be sorted descending according to"
                + " End time. Remember the full list");
        HarvestUtils.gotoHarvestHistoryForDomain(HarvestUtils.DEFAULT_DOMAIN);
        PageHelper.clickLink(HarvestHistoryPageHelper.END_TIME_HEADER);
        PageHelper.clickLink(HarvestHistoryPageHelper.END_TIME_HEADER);
        PageHelper.waitForPageToLoad();
        List<HarvestHistoryPageHelper.HarvestHistoryEntry> harvestHistory = HarvestHistoryPageHelper
                .readHarvestHistory();
        int PAGE_SIZE = 2;
        setHarvestStatusPageSize(PAGE_SIZE);

        addStep("Set the page size to " + PAGE_SIZE, "");

        addStep("Goto the harvest history for the default domain",
                "Only 2 harvests should be listed and the next link should be enabled.");
        HarvestUtils.gotoHarvestHistoryForDomain(HarvestUtils.DEFAULT_DOMAIN);
        assertEquals(
                "Didn't find the expected 2 harvests on the first page", 2,
                PageHelper.getWebDriver()
                        .findElements(By.xpath("//table[@class='selection_table']/tbody/tr[position()>1]")).size());

        addStep("Click the 'End time' header link twice",
                "The table should now again be sorted descending according to End time.");
        HarvestUtils.gotoHarvestHistoryForDomain(HarvestUtils.DEFAULT_DOMAIN);
        PageHelper.clickLink(HarvestHistoryPageHelper.END_TIME_HEADER);
        PageHelper.clickLink(HarvestHistoryPageHelper.END_TIME_HEADER);
        PageHelper.waitForPageToLoad();

        addStep("Click the next link until the next link disappears", "All the pages should have been listed, "
                + "in the same order as when the full list was show.");
        int harvestCounter = 0;
        int numberOfPages = (int) Math.ceil((float) harvestHistory.size() / PAGE_SIZE);
        for (int pageNumber = 1; pageNumber <= numberOfPages; pageNumber++) {
            List<WebElement> rows = PageHelper.getWebDriver().findElement(By.className("selection_table"))
                    .findElements(By.tagName("tr"));
            rows.remove(0); // Skip headers
            for (int rowNumber = 0; rowNumber < rows.size(); rowNumber++) {
                WebElement row = rows.get(rowNumber);
                assertEquals(new HarvestHistoryPageHelper.HarvestHistoryEntry(row),
                        harvestHistory.get(harvestCounter++));
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
                "All the pages should have been listed, " + "2 at a time backwards.");
        for (int pageNumber = numberOfPages; pageNumber >= 1; pageNumber--) {
            List<WebElement> rows = PageHelper.getWebDriver().findElement(By.className("selection_table"))
                    .findElements(By.tagName("tr"));
            rows.remove(0); // Skip headers
            for (int rowNumber = rows.size() - 1; rowNumber >= 0; rowNumber--) {
                WebElement row = rows.get(rowNumber);
                assertEquals(new HarvestHistoryPageHelper.HarvestHistoryEntry(row),
                        harvestHistory.get(--harvestCounter));
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
        String originalFileName = "conf/settings_GUIApplication.xml.original";
        String newFileName = "conf/settings_GUIApplication.xml."+size;

        getTestController().runTestXCommand(TestEnvironment.JOB_ADMIN_SERVER,
                "if [ ! -f " + originalFileName + " ]; then "
                + " cp conf/settings_GUIApplication.xml " + originalFileName + ";fi");

        getTestController().runTestXCommand(TestEnvironment.JOB_ADMIN_SERVER,
                " cp conf/settings_GUIApplication.xml " + newFileName);

        //getTestController().runTestXCommand(TestEnvironment.JOB_ADMIN_SERVER,
        //        "cp conf/settings_GUIApplication.xml conf/settings_GUIApplication.xml.original");

        getTestController().replaceStringInFile(TestEnvironment.JOB_ADMIN_SERVER, newFileName,
                "</indexClient>", "</indexClient>" + "<webinterface><harvestStatus><defaultPageSize>" + size
                        + "</defaultPageSize></harvestStatus></webinterface>");


        getTestController().runTestXCommand(TestEnvironment.JOB_ADMIN_SERVER,
                        " cp " + newFileName + " conf/settings_GUIApplication.xml");

        TestGUIController.restartGUI();
    }

    @BeforeMethod(alwaysRun = true)
    @AfterMethod(alwaysRun = true)
    private void cleanupGUIConfiguration() {
        try {
            getTestController().runTestXCommand(TestEnvironment.JOB_ADMIN_SERVER,
                    "if [ -f conf/settings_GUIApplication.xml.original ]; then "
                            + "echo conf/settings_GUIApplication.xml.original exist, moving back.; "
                            + "conf/kill_GUIApplication.sh; sleep 20;"
                            + "cp conf/settings_GUIApplication.xml.original conf/settings_GUIApplication.xml; "
                            + " conf/start_GUIApplication.sh; " + "fi");
            TestGUIController.waitForGUIToStart(120);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void assertColumnIsSorted(int column, boolean ascending) {
        List<WebElement> rows = PageHelper.getWebDriver().findElements(
                By.xpath("//table[@class='selection_table']/tbody/tr[position()>1]"));
        String previousValue = null;
        for (WebElement rowElement : rows) {
            List<WebElement> rowCells = rowElement.findElements(By.xpath("td"));
            String value = rowCells.get(column).getText();
            if (previousValue != null) {
                if (ascending) {
                    assertTrue(value.compareTo(previousValue) <= 0, value + " should be listed after " + previousValue
                            + " for ascending");
                } else {
                    assertTrue(value.compareTo(previousValue) >= 0, value + " should be listed after " + previousValue
                            + " for descending");
                }
            }
        }
    }
}
