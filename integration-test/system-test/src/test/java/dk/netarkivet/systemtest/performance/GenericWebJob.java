package dk.netarkivet.systemtest.performance;

import java.util.concurrent.TimeUnit;

import org.openqa.selenium.WebDriver;

import dk.netarkivet.systemtest.TestLogger;
import dk.netarkivet.systemtest.environment.TestEnvironmentController;
import dk.netarkivet.systemtest.environment.TestGUIController;
import dk.netarkivet.systemtest.page.PageHelper;

/**
* A specialisation of LongRunningJob for web jobs with a TestEnvironmentController
*/
abstract class GenericWebJob extends LongRunningJob {
    protected final TestLogger log = new TestLogger(getClass());

    WebDriver driver;
    TestGUIController TestGUIController;
    protected AbstractStressTest stressTest;

    GenericWebJob(AbstractStressTest stressTest,
            TestEnvironmentController testController, WebDriver driver, Long startUpTime, Long waitingInterval,
            Long maxTime, String name) {
        super(startUpTime, waitingInterval, maxTime, name);
        this.driver = driver;
        this.TestGUIController = new TestGUIController(testController);
        driver.manage().timeouts().implicitlyWait(1, TimeUnit.SECONDS);
        String baseUrl = testController.ENV
                .getGuiHost() + ":" + testController.ENV.getGuiPort();
        PageHelper.initialize(driver, baseUrl);
        TestGUIController.waitForGUIToStart(60);
        stressTest.addFixture("Opening front page " + baseUrl);
        this.stressTest = stressTest;
    }
}
