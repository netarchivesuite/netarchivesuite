package dk.netarkivet.systemtest.performance;

import static org.testng.Assert.assertTrue;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import dk.netarkivet.systemtest.environment.TestEnvironment;
import dk.netarkivet.systemtest.environment.TestEnvironmentController;
import org.apache.commons.lang.RandomStringUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import dk.netarkivet.systemtest.TestLogger;

/**
* Job to generate harvest jobs for a snapshot harvest. According to the test description, this should take up to nine
* hours for a production load.
*/
class GenerateSnapshotJob extends GenericWebJob {
    protected final TestLogger log = new TestLogger(getClass());

    String harvestName;
    String report;

    GenerateSnapshotJob(AbstractStressTest stressTest, TestEnvironmentController testController, WebDriver driver,
            Long startUpTime,
            Long waitingInterval, Long maxTime, String name) {
        super(stressTest, testController, driver, startUpTime, waitingInterval, maxTime, name);
    }

    @Override void startJob() {
        harvestName = RandomStringUtils.randomAlphabetic(6);
        stressTest.addStep("Creating snapshot harvest '" + harvestName + "'",
                "Expect to create a snapshot harvest definition.");
        driver.findElement(By.linkText("Definitions")).click();
        driver.findElement(By.linkText("Snapshot Harvests")).click();
        final WebElement element = driver.findElement(By.partialLinkText("Create new"));
        element.click();
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
        if (submit == null) {
            throw new RuntimeException("Failed to find submit button for the snapshot job on the relevat page. The GUI application"
                    + " has probably died.");
        }
        submit.submit();
        stressTest.addStep("Activate harvest", "Expect to generate a substantial number of jobs.");
        List<WebElement> allForms = driver.findElements(By.tagName("form"));
        WebElement activationForm = null;
        for (WebElement form: allForms) {
            if (form.findElement(By.tagName("input")).getAttribute("value").equals(harvestName)){
                activationForm = form;
            }
        }
        activationForm.findElement(By.linkText("Activate")).click();
    }

    /**
     * Checks that at least one job has been created for the harvest.
     * @return true if job creation has started.
     */
    @Override boolean isStarted() {
        int numberOfJobsGenerated = extractJobCount();
        final boolean atLeastOneJobGenerated = numberOfJobsGenerated > 0;
        if (!atLeastOneJobGenerated) {
            log.warn("Should have generated at least one job for {} by now.", harvestName);
        }
        return atLeastOneJobGenerated;
    }

    /**
     * Looks for a log statement like "Created 212 jobs for harvest hgj8hy".
     * @return
     */
    @Override boolean isFinished() {
        try {
            String output = stressTest.testController.runCommand(TestEnvironment.JOB_ADMIN_SERVER,
                    "grep 'Created' ${HOME}/" + AbstractStressTest.ENV.getTESTX() + "/log/HarvestJobManager*",
                    new int[] {0, 1});
            Pattern finished = Pattern.compile(".*Created ([0-9]+) jobs([^<>]*)[(]" + harvestName + "[)].*",
                    Pattern.DOTALL);
            final Matcher matcher = finished.matcher(output);
            if (matcher.matches()) {
                report = "Snapshot generation finished with " + matcher.group(1) + " jobs generated for " + harvestName;
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override String getProgress() {
        return "Generated " + extractJobCount() + " jobs.";
    }

    /**
     * Opens the page Listing  all (ie the last 100) log messages for the HarvestJobManager application.
     */
    private void gotoHarvestJobManagerLog() {
        driver.findElement(By.linkText("Systemstate")).click();
        driver.findElement(By.linkText("HarvestJobManagerApplication")).click();
        List<WebElement> showAllLinks = driver.findElements(By.partialLinkText("show all"));
        WebElement requiredLink = null;
        for (WebElement element: showAllLinks) {
            if (element.getAttribute("href").contains("index=*")) {
                requiredLink = element;
            }
        }
        requiredLink.click();
    }

    /**
     * Count the number of jobs created for this harvest so far.
     * @return The number of jobs created so far.
     */
    private int extractJobCount() {
        driver.findElement(By.linkText("Harvest status")).click();
        WebElement select = driver.findElement(By.name("JOB_STATUS"));
        List<WebElement> options = driver.findElements(By.tagName("option"));
        for (WebElement option: options) {
            if (option.getAttribute("value").equals("ALL")) {
                option.click();
            }
        }
        driver.findElement(By.name("HARVEST_NAME")).sendKeys(harvestName);
        driver.findElement(By.name("PAGE_SIZE")).clear();
        driver.findElement(By.name("PAGE_SIZE")).sendKeys("1000");
        driver.findElement(By.name("upload")).click();
        return driver.findElements(By.linkText(harvestName)).size();
    }
}
