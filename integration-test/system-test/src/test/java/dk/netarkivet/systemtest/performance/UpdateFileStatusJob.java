package dk.netarkivet.systemtest.performance;

import java.util.concurrent.TimeUnit;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import dk.netarkivet.systemtest.TestLogger;
import dk.netarkivet.systemtest.page.PageHelper;

/**
* Runs a job to update the file status for the KB bitarchive.
*/
class UpdateFileStatusJob extends GenericWebJob {
    protected final TestLogger log = new TestLogger(getClass());

    UpdateFileStatusJob(AbstractStressTest stressTest,
            WebDriver driver, Long startUpTime, Long waitingInterval,
            Long maxTime, String name) {
        super(stressTest, stressTest.testController, driver, startUpTime, waitingInterval, maxTime, name);
    }

    @Override void startJob() {
        driver.manage().timeouts().implicitlyWait(1, TimeUnit.SECONDS);
        String baseUrl = stressTest.testController.ENV.getGuiHost() + ":" + stressTest.testController.ENV.getGuiPort();
        PageHelper.initialize(driver, baseUrl);
        stressTest.addStep("Opening bitpreservation section of GUI.",
                "The page should open and show the number of files in the archive.");
        driver.manage().timeouts().pageLoadTimeout(20L, TimeUnit.MINUTES);
        driver.findElement(By.linkText("Bitpreservation")).click();
        WebElement updateLink = driver.findElement(By.linkText("Update filestatus for KB"));
        updateLink.click();
    }

    @Override boolean isStarted() {
        return true;
    }

    /**
     * When the job is finished, the actual number of files found will be zero because all the files
     * are missing.
     * @return
     */
    @Override boolean isFinished() {
        driver.manage().timeouts().pageLoadTimeout(20L, TimeUnit.MINUTES);
        driver.findElement(By.linkText("Bitpreservation")).click();
        String idNumber = "KBN_number";
        String numberS = driver.findElement(By.id(idNumber)).getText();
        return numberS.equals("0");
    }

    @Override String getProgress() {
        String numberS = driver.findElement(By.id(idNumber)).getText();
        String missingS = driver.findElement(By.id(idMissing)).getText();
        return "Status files/missing = " + numberS + "/" + missingS;
    }

    String idNumber = "KBN_number";
    String idMissing = "KBN_missing";
}
