package dk.netarkivet.systemtest.performance;

import static org.testng.Assert.assertEquals;

import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import dk.netarkivet.systemtest.TestLogger;
import dk.netarkivet.systemtest.environment.TestEnvironment;

/**
 * Job to check checksums for the entire Checksum leg of the bitarchive. Progress is monitored by grepping in
 * the GUIWebApplication log files and via the web interface.
 */
class UpdateChecksumJob extends GenericWebJob {
    protected final TestLogger log = new TestLogger(getClass());

    String total = null;

    UpdateChecksumJob(AbstractStressTest stressTest1,
            WebDriver driver, Long startUpTime, Long waitingInterval,
            Long maxTime, String name) {
        super(stressTest1, stressTest1.testController, driver, startUpTime, waitingInterval, maxTime, name);
    }

    @Override void startJob() {
        stressTest.addFixture("Opening Bitpreservation page.");
        driver.manage().timeouts().pageLoadTimeout(10L, TimeUnit.MINUTES);
        driver.findElement(By.linkText("Bitpreservation")).click();
        stressTest.addStep("Updating checksum and filestatus for CS.", "Should result in a full checksum" +
                "job being started and completed.");
        WebElement updateLink = driver.findElement(By.linkText("Update checksum and filestatus for CS"));
        updateLink.click();
    }

    @Override boolean isStarted() {
        try {
            String output = stressTest.testController.runCommand(TestEnvironment.JOB_ADMIN_SERVER,
                    "grep 'Starting processing' ${HOME}/" + AbstractStressTest.ENV.getTESTX() + "/log/GUI*",
                    new int[] {0, 1});
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
            String output = stressTest.testController.runCommand(TestEnvironment.JOB_ADMIN_SERVER,
                    "grep 'Finished processing' ${HOME}/" + AbstractStressTest.ENV.getTESTX() + "/log/GUI*",
                    new int[] {0, 1});
            final String finishedS = ".*Finished processing of ([0-9]+) checksum entries.*";
            Pattern finishedP = Pattern.compile(finishedS, Pattern.DOTALL);
            final Matcher matcher = finishedP.matcher(output);
            if (matcher.matches()) {
                assertEquals(total, matcher.group(1), "Expect the number of entries to match before and after.");
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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
        //Might get here if the top log entry is something else. This doesn't really matter so just return
        //the empty string.
        return "";
    }
}
