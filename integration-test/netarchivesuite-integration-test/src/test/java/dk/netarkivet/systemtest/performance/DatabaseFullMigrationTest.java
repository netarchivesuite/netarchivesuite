package dk.netarkivet.systemtest.performance;

import dk.netarkivet.systemtest.environment.ApplicationManager;
import dk.netarkivet.systemtest.functional.DomainsPageTest;
import dk.netarkivet.systemtest.functional.ExtendedFieldTest;
import dk.netarkivet.systemtest.functional.HarvestHistoryForDomainPageTest;
import dk.netarkivet.systemtest.page.PageHelper;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Created by csr on 5/8/14.
 */
public class DatabaseFullMigrationTest extends StressTest {

    @Test(groups = {"guitest","functest"})
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
        domainsPageTest.usedConfigurationsTest(driver, (new Date()).getTime()+".dk");
        ExtendedFieldTest extendedFieldTest = new ExtendedFieldTest();
        extendedFieldTest.extendedDomainStringFieldTest(driver,  (new Date()).getTime() + "");
        //Add dependency injection of EnvironmentManager so this can work:
        //HarvestHistoryForDomainPageTest harvestHistoryForDomainPageTest = new HarvestHistoryForDomainPageTest();
        //harvestHistoryForDomainPageTest.historySortedTablePagingTest();
        addStep("Opening bitpreservation section of GUI.", "The page should open and show the number of files in the archive.");
        driver.manage().timeouts().pageLoadTimeout(10L, TimeUnit.MINUTES);
        driver.findElement(By.linkText("Bitpreservation")).click();
        driver.getPageSource().matches("Number of files:.*[0-9]+");
        driver.close();
    }

}

