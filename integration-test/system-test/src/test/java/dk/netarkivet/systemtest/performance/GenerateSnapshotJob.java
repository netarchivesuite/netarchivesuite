package dk.netarkivet.systemtest.performance;

import static org.testng.Assert.assertTrue;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.RandomStringUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import dk.netarkivet.systemtest.environment.TestEnvironmentManager;

/**
* Created by csr on 30/10/14.
*/
class GenerateSnapshotJob extends GenericWebJob {

    String harvestName;

    GenerateSnapshotJob(StressTest stressTest, TestEnvironmentManager testEnvironmentManager, WebDriver driver,
            Long startUpTime,
            Long waitingInterval, Long maxTime, String name) {
        super(stressTest, testEnvironmentManager, driver, startUpTime, waitingInterval, maxTime, name);
    }

    @Override void startJob() {
        harvestName = RandomStringUtils.randomAlphabetic(6);
        stressTest.addStep("Creating snapshot harvest '" + harvestName + "'",
                "Expect to create a snapshot harvest definition.");
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

    @Override boolean isStarted() {
        gotoHarvestJobManagerLog(driver);
        final boolean contains = driver.getPageSource().contains(harvestName);
        assertTrue(contains, "Page should contain harvest name: " + harvestName);
        int jobsGenerated = extractJobCount(driver, harvestName);
        final boolean condition = jobsGenerated > 0;
        assertTrue(condition, "Should have generated at least one job by now for " + harvestName);
        return contains && condition;
    }

    @Override boolean isFinished() {
        Pattern finished = Pattern.compile(".*Created ([0-9]+) jobs.*[(](.{6})[)].*", Pattern.DOTALL);
        gotoHarvestJobManagerLog(driver);
        Matcher m = finished.matcher(driver.getPageSource());
        return m.matches();
    }

    @Override String getProgress() {
        return "Generated " + extractJobCount(driver, harvestName) + " jobs.";

    }

    private void gotoHarvestJobManagerLog(WebDriver driver) {
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

    private int extractJobCount(WebDriver driver, String harvestName) {
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
