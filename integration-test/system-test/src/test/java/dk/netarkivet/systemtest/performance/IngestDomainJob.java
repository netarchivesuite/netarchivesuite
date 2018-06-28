package dk.netarkivet.systemtest.performance;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.testng.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import dk.netarkivet.systemtest.TestLogger;
import dk.netarkivet.systemtest.environment.TestEnvironmentController;
import dk.netarkivet.systemtest.environment.TestGUIController;
import dk.netarkivet.systemtest.page.PageHelper;

/**
* Job to ingest a domain list from a file on the local machine via the web interface. Although implemented as
* a LongRunningJob, this actually runs as a single browser operation so the job should already be completed when startJob()
 * returns.
*/
class IngestDomainJob extends GenericWebJob {
    protected final TestLogger log = new TestLogger(getClass());

    public IngestDomainJob(AbstractStressTest stressTest, WebDriver webDriver, Long maxTime) {
          super(stressTest, stressTest.testController, webDriver, 0L, 60L, maxTime, "Ingest Domain Job");
    }

    @Override void startJob() {
        String backupEnv = System.getProperty("systemtest.backupenv", "prod");
        TestEnvironmentController testController = stressTest.testController;
        TestGUIController TestGUIController = new TestGUIController(testController);
        driver.manage().timeouts().implicitlyWait(1, TimeUnit.SECONDS);
        String baseUrl = testController.ENV.getGuiHost() + ":" + testController.ENV.getGuiPort();
        PageHelper.initialize(driver, baseUrl);
        TestGUIController.waitForGUIToStart(60);
        stressTest.addFixture("Opening initial page " + baseUrl);
        File domainsFile = null;
        try {
            domainsFile = File.createTempFile("domains", "txt");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        stressTest.addStep("Getting domain file", "The file should be downloaded");
        int returnCode = 0;
        //final String command = "scp test@kb-prod-udv-001.kb.dk:" + backupEnv + "-backup/domain.*.txt " + domainsFile
        //        .getAbsolutePath();
        final String command = "scp devel@kb-prod-udv-001.kb.dk:prod-backup/domain.*.txt " + domainsFile
                .getAbsolutePath();
        try {
            Process p = Runtime.getRuntime().exec(
                    command);
            returnCode = p.waitFor();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        assertEquals(returnCode, 0, "Return code from scp command " + command + " is " + returnCode);
        stressTest.addStep("Checking for existence of domains file", "The file should exist.");
        assertThat("Domain file " + domainsFile.getAbsolutePath() + " is too short", domainsFile.length(), greaterThan(10000L));
        stressTest.addStep("Ingesting domains from " + domainsFile.getAbsolutePath(),
                "Expect to see domain generation.");
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
    }

    @Override boolean isStarted() {
         return true;
    }

    @Override boolean isFinished() {
        return driver.getPageSource().contains("Ingesting done");
    }

    @Override String getProgress() {
        return "";
    }
}
