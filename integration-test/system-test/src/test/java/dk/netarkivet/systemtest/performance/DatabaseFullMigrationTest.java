/*
 * #%L
 * NetarchiveSuite System test
 * %%
 * Copyright (C) 2005 - 2014 The Royal Danish Library, the Danish State and University Library,
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
package dk.netarkivet.systemtest.performance;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import dk.netarkivet.systemtest.environment.ApplicationManager;
import dk.netarkivet.systemtest.functional.DomainsPageTest;
import dk.netarkivet.systemtest.functional.ExtendedFieldTest;
import dk.netarkivet.systemtest.page.PageHelper;

@SuppressWarnings("unused")
public class DatabaseFullMigrationTest extends StressTest {

    @Test(groups = { "guitest", "performancetest" })
    public void dbFullMigrationTest() throws Exception {
        addDescription("Test complete backup-database ingest from production produces a functional NAS system.");
        doStuff();
    }

    @BeforeClass
    public void setupTestEnvironment() throws Exception {
        if (true) {
            shutdownPreviousTest();
            fetchProductionData();
            deployComponents();
            replaceDatabasesWithProd(false);
            upgradeHarvestDatabase();
            startTestSystem();

        }
    }

    @AfterClass
    public void teardownTestEnvironment() throws Exception {
        if (true) {
            shutdownTest();
        }
    }

    private void doStuff() throws Exception {
        WebDriver driver = new FirefoxDriver();
        ApplicationManager applicationManager = new ApplicationManager(environmentManager);
        driver.manage().timeouts().implicitlyWait(1, TimeUnit.SECONDS);
        String baseUrl = environmentManager.getGuiHost() + ":" + environmentManager.getGuiPort();
        PageHelper.initialize(driver, baseUrl);
        applicationManager.waitForGUIToStart(60);
        addFixture("Opening NAS front page.");
        DomainsPageTest domainsPageTest = new DomainsPageTest();
        domainsPageTest.usedConfigurationsTest(driver, (new Date()).getTime() + ".dk");
        ExtendedFieldTest extendedFieldTest = new ExtendedFieldTest();
        extendedFieldTest.extendedDomainStringFieldTest(driver, (new Date()).getTime() + "");
        // Add dependency injection of EnvironmentManager so this can work:
        // HarvestHistoryForDomainPageTest harvestHistoryForDomainPageTest = new
        // HarvestHistoryForDomainPageTest();
        // harvestHistoryForDomainPageTest.historySortedTablePagingTest();
        addStep("Opening bitpreservation section of GUI.",
                "The page should open and show the number of files in the archive.");
        driver.manage().timeouts().pageLoadTimeout(10L, TimeUnit.MINUTES);
        driver.findElement(By.linkText("Bitpreservation")).click();
        driver.getPageSource().matches("Number of files:.*[0-9]+");
        driver.close();
    }

}
