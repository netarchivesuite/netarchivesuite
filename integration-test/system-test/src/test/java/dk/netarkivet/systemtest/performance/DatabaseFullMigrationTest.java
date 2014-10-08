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

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.io.File;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.RandomStringUtils;
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
        //doUpdateChecksumAndFileStatus();
        //doIngestDomains();
        doGenerateSnapshot();
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

    private void doGenerateSnapshot() {
        WebDriver driver = new FirefoxDriver();
        ApplicationManager applicationManager = new ApplicationManager(environmentManager);
        driver.manage().timeouts().implicitlyWait(1, TimeUnit.SECONDS);
        String baseUrl = environmentManager.getGuiHost() + ":" + environmentManager.getGuiPort();
        PageHelper.initialize(driver, baseUrl);
        applicationManager.waitForGUIToStart(60);
        addFixture("Opening NAS front page.");
        String harvestName = RandomStringUtils.randomAlphabetic(6);
        addStep("Creating snapshot harvest '" + harvestName + "'", "Expect to create a snapshot harvest definition.");
        driver.findElement(By.linkText("Definitions")).click();
        driver.findElement(By.linkText("Snapshot Harvests")).click();
        driver.findElement(By.partialLinkText("Create new")).click();
        List<WebElement> inputs = driver.findElements(By.tagName("input"));
        WebElement submit = null;
        for (WebElement input: inputs) {
            String name = input.getAttribute("name");
            String type = input.getAttribute("type");
            if ("submit".equals(type)) {
                submit = input;
            }
            if ("harvestname".equals(name)) {
                input.sendKeys(harvestName);
            }
            if ("snapshot_byte_limit".equals(name)) {
                input.clear();
                input.sendKeys("100000");
            }
        }
        submit.submit();
        addStep("Activate harvest", "Expect to generate a substantial number of jobs.");
        List<WebElement> allForms = driver.findElements(By.tagName("form"));
        for (WebElement form: allForms) {
            if (form.findElement(By.tagName("input")).getAttribute("value").equals(harvestName)){
                form.findElement(By.linkText("Activate")).click();
            }
        }
    }


    private void doIngestDomains() throws Exception {
        WebDriver driver = new FirefoxDriver();
        ApplicationManager applicationManager = new ApplicationManager(environmentManager);
        driver.manage().timeouts().implicitlyWait(1, TimeUnit.SECONDS);
        String baseUrl = environmentManager.getGuiHost() + ":" + environmentManager.getGuiPort();
        PageHelper.initialize(driver, baseUrl);
        applicationManager.waitForGUIToStart(60);
        addFixture("Opening NAS front page.");
        File domainsFile = File.createTempFile("domains", "txt");
        addStep("Getting domain file", "The file should be downloaded");
        Process p = Runtime.getRuntime().exec("scp test@kb-prod-udv-001.kb.dk:prod-backup/domain.*.txt " + domainsFile.getAbsolutePath());
        int returnCode = p.waitFor();
        assertEquals(returnCode, 0, "Return code from scp command is " + returnCode);
        assertTrue(domainsFile.length() > 100000L, "Domain file " + domainsFile.getAbsolutePath() + " is too short");
        addStep("Ingesting domains from " + domainsFile.getAbsolutePath(), "Expect to see domain generation.");
        driver.findElement(By.linkText("Definitions")).click();
        driver.findElement(By.linkText("Create Domain")).click();
        List<WebElement> elements = driver.findElements(By.name("domainlist"));
        for (WebElement element: elements) {
            if (element.getAttribute("type").equals("file")) {
                element.sendKeys(domainsFile.getAbsolutePath());
            }
        }
       elements = driver.findElements(By.tagName("input"));
        for (WebElement element: elements) {
            if (element.getAttribute("type").equals("submit") && element.getAttribute("value").equals("Ingest")) {
                element.submit();
            }
        }
        assertTrue(driver.getPageSource().contains("Ingesting done"), "Page should contain text 'Ingesting done'");
        TestEventManager.getInstance().addResult("Domains ingested");
        driver.close();
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
        Long stepTimeout = 24*3600*1000L;
        Long minStepTime = 1*3600*1000L;
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
        updateLink.click();
        Long startTime = System.currentTimeMillis();
        String progressS = ".*list entry number\\s([0-9]+).*";
        Pattern progress = Pattern.compile(progressS, Pattern.DOTALL);
        String finishedS = ".*Replying GetAllChecksumsMessage:.*";
        Pattern finished = Pattern.compile(finishedS, Pattern.DOTALL);
        boolean isFinished = false;
        while (!isFinished) {
            Long timeElapsed = System.currentTimeMillis() - startTime;
            if (timeElapsed > stepTimeout) {
                fail("Checksum checking took longer than the permitted " + stepTimeout/1000 + "s.");
            }
            driver.findElement(By.linkText("Systemstate")).click();
            List<WebElement> logMessages =  driver.findElements(By.tagName("pre"));
            for (WebElement webElement: logMessages) {
                if (finished.matcher(webElement.getText()).matches()) {
                    isFinished = true;
                    if (timeElapsed < minStepTime) {
                        fail("Checksum checking took less than the minimum expected time: " +
                                timeElapsed/1000 + "s instead of minimum " + minStepTime/1000 + "s.");
                    }
                    TestEventManager.getInstance().addResult("Checksum checking completed in " + timeElapsed/1000 + "s.");
                }
            }
            if (!isFinished) {
                for (WebElement webElement: logMessages) {
                    Matcher matcher = progress.matcher(webElement.getText());
                    if (matcher.matches()) {
                        System.out.println("Checksum processed " + matcher.group(1) + " files after " + timeElapsed/1000 + "s." );
                    }
                }
            }
            if (!isFinished) {
                Thread.sleep(10 * 1000L);
            }
        }
        driver.close();
    }




}
