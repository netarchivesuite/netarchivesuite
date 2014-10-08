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

import static org.testng.Assert.fail;

import java.util.concurrent.TimeUnit;

import org.jaccept.TestEventManager;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import dk.netarkivet.systemtest.environment.ApplicationManager;
import dk.netarkivet.systemtest.environment.TestEnvironmentManager;
import dk.netarkivet.systemtest.page.PageHelper;

@SuppressWarnings("unused")
public class DatabaseFullMigrationTest extends StressTest {

    @Test(groups = {"guitest", "performancetest"})
    public void dbFullMigrationTest() throws Exception {
        addDescription("Test complete backup-database ingest from production produces a functional NAS system.");
        //doUpdateFileStatus();
        doUpdateChecksumAndFileStatus();
    }

    @BeforeClass
    public void setupTestEnvironment() throws Exception {
        if (false) {
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
        if (false) {
            shutdownTest();
        }
    }

    private void doUpdateFileStatus() throws Exception {
        Long stepTimeout = 2*3600*1000L;
        WebDriver driver = new FirefoxDriver();
        ApplicationManager applicationManager = new ApplicationManager(environmentManager);
        driver.manage().timeouts().implicitlyWait(1, TimeUnit.SECONDS);
        String baseUrl = environmentManager.getGuiHost() + ":" + environmentManager.getGuiPort();
        PageHelper.initialize(driver, baseUrl);
        applicationManager.waitForGUIToStart(60);
        addFixture("Opening NAS front page.");
        addStep("Opening bitpreservation section of GUI.",
                "The page should open and show the number of files in the archive.");
        driver.manage().timeouts().pageLoadTimeout(10L, TimeUnit.MINUTES);
        driver.findElement(By.linkText("Bitpreservation")).click();
        WebElement updateLink = driver.findElement(By.linkText("Update filestatus for KB"));
        String idNumber = "KBN_number";
        String idMissing = "KBN_missng";
        String numberS = driver.findElement(By.id(idNumber)).getText();
        String missingS = driver.findElement(By.id(idMissing)).getText();
        System.out.println("Status files/missing = " + numberS + "/" + missingS);
        updateLink.click();
        Long startTime = System.currentTimeMillis();
        long timeRun = System.currentTimeMillis() - startTime;
        while (!numberS.equals("0")) {
            Thread.sleep(300000L);
            driver.findElement(By.linkText("Bitpreservation")).click();
            numberS = driver.findElement(By.id(idNumber)).getText();
            missingS = driver.findElement(By.id(idMissing)).getText();
            System.out.println("Status files/missing = " + numberS + "/" + missingS);
            timeRun = System.currentTimeMillis() - startTime;
            System.out.println("Time elapsed " + timeRun /1000 + "s.");
            if (timeRun > stepTimeout) {
                fail("Failed to update file status for whole archive after " + timeRun/1000 + "s.");
            }
        }
        TestEventManager.getInstance().addResult("File status successfully updated after " + timeRun /1000 + "s.");
        driver.close();
    }

    private void doUpdateChecksumAndFileStatus() throws Exception {
        Long stepTimeout = 2*3600*1000L;
        WebDriver driver = new FirefoxDriver();
        ApplicationManager applicationManager = new ApplicationManager(environmentManager);
        driver.manage().timeouts().implicitlyWait(1, TimeUnit.SECONDS);
        String baseUrl = environmentManager.getGuiHost() + ":" + environmentManager.getGuiPort();
        PageHelper.initialize(driver, baseUrl);
        applicationManager.waitForGUIToStart(60);
        addFixture("Opening NAS front page.");
        addStep("Opening bitpreservation section of GUI.",
                "The page should open and show the number of files in the archive.");
        driver.manage().timeouts().pageLoadTimeout(10L, TimeUnit.MINUTES);
        driver.findElement(By.linkText("Bitpreservation")).click();
        WebElement updateLink = driver.findElement(By.linkText("Update checksum and filestatus for CS"));
        /*String idNumber = "KBN_number";
        String idMissing = "KBN_missng";
        String numberS = driver.findElement(By.id(idNumber)).getText();
        String missingS = driver.findElement(By.id(idMissing)).getText();
        System.out.println("Status files/missing = " + numberS + "/" + missingS);*/
        updateLink.click();
       /* Long startTime = System.currentTimeMillis();
        long timeRun = System.currentTimeMillis() - startTime;
        while (!numberS.equals("0")) {
            Thread.sleep(300000L);
            driver.findElement(By.linkText("Bitpreservation")).click();
            numberS = driver.findElement(By.id(idNumber)).getText();
            missingS = driver.findElement(By.id(idMissing)).getText();
            System.out.println("Status files/missing = " + numberS + "/" + missingS);
            timeRun = System.currentTimeMillis() - startTime;
            System.out.println("Time elapsed " + timeRun /1000 + "s.");
            if (timeRun > stepTimeout) {
                fail("Failed to update file status for whole archive after " + timeRun/1000 + "s.");
            }
        }
        TestEventManager.getInstance().addResult("File status successfully updated after " + timeRun /1000 + "s.");
        driver.close();*/
    }




}
