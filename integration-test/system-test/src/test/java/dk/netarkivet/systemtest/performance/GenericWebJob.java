package dk.netarkivet.systemtest.performance;

import java.util.concurrent.TimeUnit;

import org.openqa.selenium.WebDriver;

import dk.netarkivet.systemtest.TestLogger;
import dk.netarkivet.systemtest.environment.ApplicationManager;
import dk.netarkivet.systemtest.environment.TestEnvironmentManager;
import dk.netarkivet.systemtest.page.PageHelper;

/**
* A specialisation of LongRunningJob for web jobs with a TestEnvironmentManager
*/
abstract class GenericWebJob extends LongRunningJob {
    protected final TestLogger log = new TestLogger(getClass());

    WebDriver driver;
    ApplicationManager applicationManager;
    protected StressTest stressTest;

    GenericWebJob(StressTest stressTest,
            TestEnvironmentManager testEnvironmentManager, WebDriver driver, Long startUpTime, Long waitingInterval,
            Long maxTime, String name) {
        super(startUpTime, waitingInterval, maxTime, name);
        this.driver = driver;
        this.applicationManager = new ApplicationManager(testEnvironmentManager);
        driver.manage().timeouts().implicitlyWait(1, TimeUnit.SECONDS);
        String baseUrl = testEnvironmentManager
                .getGuiHost() + ":" + testEnvironmentManager.getGuiPort();
        PageHelper.initialize(driver, baseUrl);
        applicationManager.waitForGUIToStart(60);
        stressTest.addFixture("Opening front page " + baseUrl);
        this.stressTest = stressTest;
    }
}
