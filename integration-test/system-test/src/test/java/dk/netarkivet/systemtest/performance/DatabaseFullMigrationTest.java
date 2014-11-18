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

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import dk.netarkivet.systemtest.TestLogger;
import dk.netarkivet.systemtest.environment.GUIApplicationManager;

/**
 * Tests to be run against the full production-load database.
 */
@SuppressWarnings("unused")
public class DatabaseFullMigrationTest extends AbstractStressTest {
    protected final TestLogger log = new TestLogger(getClass());

    @Test
    public void testUpdateFileStatus() throws Exception {
        addDescription("Test updating the file status for all files in one bitarchive replica. This procedure takes around one hour to run for " +
                "a full production load. Because there are no actual files present in the test system, the test will eventually show that all files are " +
                "missing in this replica.");
        doUpdateFileStatus();
    }

    @Test
    public void testUpdateChecksumStatus() throws Exception {
        addDescription("Test updating checksum status for all files in one checksum-replica. This takes about four hours to run in a full production load.");
        doUpdateChecksumAndFileStatus();
    }

    @Test
    public void testIngestDomains() throws Exception {
        addDescription("Test ingesting domains from a textual list of about 2 million domains. This is not a particularly heavy" +
                " operation but tests some browser functionality which is not easily testable elsewhere - specifically that the 'keep-alive' " +
                "functionality allows the browser to follow the complete upload/ingest of all the domains without timing out.");
        doIngestDomains();
    }

    @Test
    public void testGenerateSnapshot() throws Exception {
        addDescription("Test generating snapshot jobs with a maximum number of bytes per domain of 100 000. This takes about ten hours to complete. The" +
                " number of jobs generated is determined roughly by the parameter settings.harvester.scheduler.jobGen.domainConfigSubsetSize which is" +
                " set to 10000 by default. Ie there is a maximum of 10000 domains per job, although there are also a small number of jobs with much fewer domains" +
                " where these have specialised configurations.");
        doGenerateSnapshot();
    }


    @BeforeClass
    public void setupTestEnvironment() throws Exception {
        shutdownPreviousTest();
        fetchProductionData();
        deployComponents();
        replaceDatabasesWithProd(false);
        upgradeHarvestDatabase();
        startTestSystem();
    }

    private void doGenerateSnapshot() throws InterruptedException {
        WebDriver driver = new FirefoxDriver();
        String snapshotTimeDividerString = System.getProperty("stresstest.snapshottimedivider", "1");
        Integer snapshotTimeDivider = Integer.parseInt(snapshotTimeDividerString);
        if (snapshotTimeDivider != 1) {
            log.info("Dividing timescale for snapshot test by a factor {} (stresstest.snapshottimedivider).", snapshotTimeDivider);
        }
        GUIApplicationManager GUIApplicationManager = new GUIApplicationManager(testController);
        LongRunningJob snapshotJob = new GenerateSnapshotJob(this, testController, driver,
                60*60*1000L/snapshotTimeDivider, 30*60*1000L/snapshotTimeDivider, 20*3600*1000L/snapshotTimeDivider, "SnapshotGenerationJob"
                );
        snapshotJob.run();
    }

    private void doIngestDomains() throws Exception {
        WebDriver driver = new FirefoxDriver();
        IngestDomainJob ingestDomainJob = new IngestDomainJob(this, driver, 60*3600*1000L);
        ingestDomainJob.run();
    }

    private void doUpdateFileStatus() throws Exception {
        WebDriver driver = new FirefoxDriver();
        GUIApplicationManager GUIApplicationManager = new GUIApplicationManager(testController);
        UpdateFileStatusJob updateFileStatusJob = new UpdateFileStatusJob(this, driver, 0L, 5*60*1000L, 2*3600*1000L, "Update FileStatus Job");
        updateFileStatusJob.run();
    }

    private void doUpdateChecksumAndFileStatus() throws Exception {
        Long stepTimeout = 24*3600*1000L;
        String minStepTimeHoursString = System.getProperty("stresstest.minchecksumtime", "1");
        log.debug("Checksum checking must take at least {} (stresstest.minchecksumtime) hours to complete.", minStepTimeHoursString);
        Long minStepTime = Integer.parseInt(minStepTimeHoursString)*3600*1000L;
        UpdateChecksumJob updateChecksumJob = new UpdateChecksumJob(
                this,
                new FirefoxDriver(),
                60*1000L,
                300*1000L,
                stepTimeout,
                "Update Checksum Job"
        );
        updateChecksumJob.run();
    }
}
