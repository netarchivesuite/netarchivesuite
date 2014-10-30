package dk.netarkivet.systemtest.performance;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import dk.netarkivet.systemtest.environment.ApplicationManager;
import dk.netarkivet.systemtest.environment.TestEnvironment;

/**
* Created by csr on 30/10/14.
*/
class UpdateChecksumJob extends GenericWebJob {

    String total = null;

    UpdateChecksumJob(StressTest databaseFullMigrationTest,
            ApplicationManager applicationManager, WebDriver driver, Long startUpTime, Long waitingInterval,
            Long maxTime, String name) {
        super(databaseFullMigrationTest, databaseFullMigrationTest.environmentManager, driver, startUpTime, waitingInterval, maxTime, name);
    }

    @Override void startJob() {
        driver.manage().timeouts().pageLoadTimeout(10L, TimeUnit.MINUTES);
        driver.findElement(By.linkText("Bitpreservation")).click();
        WebElement updateLink = driver.findElement(By.linkText("Update checksum and filestatus for CS"));
        updateLink.click();
    }

    @Override boolean isStarted() {
        try {
            String output = stressTest.environmentManager.runCommand(TestEnvironment.JOB_ADMIN_SERVER, "grep 'Starting processing' ${HOME}/" + StressTest.TESTNAME+ "/log/GUI*", new int[]{0,1});
            final String startedS = ".*Starting processing of ([0-9]+) checksum entries.*";
            Pattern startedP = Pattern.compile(startedS, Pattern.DOTALL);
            final Matcher matcher = startedP.matcher(output);
                            if (matcher.matches()) {
                                total = matcher.group(1);
                                return true;
                            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return false;
    }

    @Override boolean isFinished() {
        try {
            String output = stressTest.environmentManager.runCommand(TestEnvironment.JOB_ADMIN_SERVER, "grep 'Finished processing' ${HOME}/" + StressTest.TESTNAME+ "/log/GUI*", new int[]{0,1});
            final String finishedS = ".*Finished processing of ([0-9]+) checksum entries.*";
            Pattern finishedP = Pattern.compile(finishedS, Pattern.DOTALL);
            final Matcher matcher = finishedP.matcher(output);
            if (matcher.matches()) {
                total = matcher.group(1);
                return true;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return false;
    }

    private void gotoGUILog() {
        driver.findElement(By.linkText("Systemstate")).click();
        driver.findElement(By.linkText("GUIApplication")).click();
        List<WebElement> elements = driver.findElements(By.linkText("show all"));
        WebElement showElement = null;
        for (WebElement element: elements) {
            if (element.getAttribute("href").contains("index=*")) {
                showElement = element;
            }
        }
        showElement.click();
    }

    @Override String getProgress() {
        driver.findElement(By.linkText("Systemstate")).click();
        driver.findElement(By.linkText("GUIApplication")).click();
        final String progressS = ".*Processed checksum list entry number ([0-9]+).*";
        Pattern progressP = Pattern.compile(progressS, Pattern.DOTALL);
        WebElement webElement = driver.findElement(By.tagName("pre"));
        if (webElement != null) {
            Matcher matcher = progressP.matcher(webElement.getText());
            if (matcher.matches()) {
                return "Processed " + matcher.group(1) + " out of " + total;
            }
        }
        return null;
    }
}
