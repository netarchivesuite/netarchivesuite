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
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import dk.netarkivet.systemtest.NASAssert;
import dk.netarkivet.systemtest.environment.ApplicationManager;
import dk.netarkivet.systemtest.environment.TestEnvironment;
import dk.netarkivet.systemtest.page.DomainWebTestHelper;
import dk.netarkivet.systemtest.page.PageHelper;
import dk.netarkivet.systemtest.page.SelectiveHarvestPageHelper;

@SuppressWarnings("static-access")
public class DatabaseMigrationSanityTest extends StressTest {

    /**
     * Basic sanity test that the current production database can be
     * consistently upgraded with the latest NAS software. This test is designed
     * to be cheap to run so it can easily be tested on any snapshot.
     */
    @Test(groups = { "performancetest" })
    public void dbMigrationSanityTest() throws Exception {
        addDescription("Test that database schema ingest from production produces a functional NAS system.");
        doStuff();
    }

    @BeforeClass
    public void setupTestEnvironment() throws Exception {
        if (true) {
            shutdownPreviousTest();
            fetchProductionData();
            deployComponents();
            replaceDatabasesWithProd(true);
            upgradeHarvestDatabase();
            startTestSystem();
            copyTestfiles();
            uploadFiles();
        }
    }

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
        PageHelper.gotoPage(PageHelper.MenuPages.AliasSummary.Frontpage);
        addStep("Ingest some domains", "The domains should be created.");
        DomainWebTestHelper.createDomain(new String[] { "netarkivet.dk", "kb.dk", "kaarefc.dk" });
        WebElement element = null;
        try {
            element = driver.findElement(By.tagName("h4"));
        } catch (Exception e) {
            element = driver.findElement(By.id("message"));
        }
        NASAssert.assertTrue(element.getText().contains("These domains have now been created")
                || element.getText().contains("already exist"));
        addStep("Opening bitpreservation section of GUI.",
                "The page should open and show the number of files in the archive.");
        driver.findElement(By.linkText("Bitpreservation")).click();
        driver.getPageSource().matches("Number of files:.*[0-9]{2}");
        addStep("Create a selective harvest", "The harvest should be created successfully and be listed in the HD list");
        String harvestId = "harvest_" + (new Date()).getTime();
        SelectiveHarvestPageHelper.createSelectiveHarvest(harvestId, "a harvest", new String[] { "netarkivet.dk",
                "kb.dk" });
        NASAssert.assertTrue(driver.getPageSource().contains(harvestId), harvestId
                + " not found in harvest list after creation");

    }

    private void copyTestfiles() throws Exception {
        addFixture("Copy test arcrepository data over to admin machine.");
        environmentManager.runCommand("scp -r ${HOME}/bitarchive_testdata kb-test-adm-001:");
    }

    private void uploadFiles() throws Exception {
        environmentManager.runTestXCommand(TestEnvironment.JOB_ADMIN_SERVER,
                "chmod 755 ${HOME}/bitarchive_testdata/upload.sh");
        addFixture("Upload arcfiles to arcrepository.");
        environmentManager.runTestXCommand(TestEnvironment.JOB_ADMIN_SERVER, "${HOME}/bitarchive_testdata/upload.sh "
                + environmentManager.getTESTX() + " arcfiles");
        addFixture("Upload warcfiles to arcrepository.");
        environmentManager.runTestXCommand(TestEnvironment.JOB_ADMIN_SERVER, "${HOME}/bitarchive_testdata/upload.sh "
                + environmentManager.getTESTX() + " warcfiles");
    }

}
