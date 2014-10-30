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
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.jaccept.TestEventManager;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
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
        doUpdateFileStatus();
        doUpdateChecksumAndFileStatus();
        doIngestDomains();
        doGenerateSnapshot();
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

    //@AfterClass
    public void teardownTestEnvironment() throws Exception {
        if (false) {
            shutdownTest();
        }
    }

    private void doGenerateSnapshot() throws InterruptedException {
        WebDriver driver = new FirefoxDriver();
        String snapshotTimeDividerString = System.getProperty("stresstest.snapshottimedivider", "1");
        Integer snapshotTimeDivider = Integer.parseInt(snapshotTimeDividerString);
        if (snapshotTimeDivider != 1) {
            System.out.println("Dividing timescale for snapshot test by a factor " + snapshotTimeDivider + " (stresstest.snapshottimedivider).");
        }
        ApplicationManager applicationManager = new ApplicationManager(environmentManager);
        LongRunningJob snapshotJob = new GenerateSnapshotJob(this, environmentManager, driver,
                60*60*1000L/snapshotTimeDivider, 30*60*1000L/snapshotTimeDivider, 20*3600*1000L/snapshotTimeDivider, "SnapshotGenerationJob"
                );
        snapshotJob.run();
    }

    private void doIngestDomains() throws Exception {
        WebDriver driver = new FirefoxDriver();
        IngestDomainJob ingestDomainJob = new IngestDomainJob(this, driver, 60*3600*1000L);
        ingestDomainJob.run();

       /* String backupEnv = System.getProperty("systemtest.backupenv", "prod");
        ApplicationManager applicationManager = new ApplicationManager(environmentManager);
        driver.manage().timeouts().implicitlyWait(1, TimeUnit.SECONDS);
        String baseUrl = environmentManager.getGuiHost() + ":" + environmentManager.getGuiPort();
        PageHelper.initialize(driver, baseUrl);
        applicationManager.waitForGUIToStart(60);
        addFixture("Opening NAS front page.");
        File domainsFile = File.createTempFile("domains", "txt");
        addStep("Getting domain file", "The file should be downloaded");
        Process p = Runtime.getRuntime().exec("scp test@kb-prod-udv-001.kb.dk:" + backupEnv +"-backup/domain.*.txt " + domainsFile.getAbsolutePath());
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
        driver.close();*/
    }

    private void doUpdateFileStatus() throws Exception {
        WebDriver driver = new FirefoxDriver();
        ApplicationManager applicationManager = new ApplicationManager(environmentManager);
        UpdateFileStatusJob updateFileStatusJob = new UpdateFileStatusJob(this, driver, 0L, 5*60*1000L, 2*3600*1000L, "Update FileStatus Job");
        updateFileStatusJob.run();
    }

    private void doUpdateChecksumAndFileStatus() throws Exception {
        Long stepTimeout = 24*3600*1000L;
        String minStepTimeHoursString = System.getProperty("stresstest.minchecksumtime", "1");
        System.out.println("Checksum checking must take at least " + minStepTimeHoursString + " (stresstest.minchecksumtime) hours to complete.");
        Long minStepTime = Integer.parseInt(minStepTimeHoursString)*3600*1000L;

        UpdateChecksumJob updateChecksumJob = new UpdateChecksumJob(
                this,
                new ApplicationManager(environmentManager),
                new FirefoxDriver(),
                60*1000L,
                300*1000L,
                stepTimeout,
                "Update Checksum Job"
        );

        updateChecksumJob.run();
    }




}
