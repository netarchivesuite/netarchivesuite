package dk.netarkivet.systemtest.performance;

import java.util.concurrent.TimeUnit;

import org.openqa.selenium.WebDriver;

import dk.netarkivet.systemtest.TestLogger;
import dk.netarkivet.systemtest.environment.TestEnvironmentController;
import dk.netarkivet.systemtest.environment.testGUIController;
import dk.netarkivet.systemtest.page.PageHelper;

/**
* A specialisation of LongRunningJob for web jobs with a TestEnvironmentController
*/
abstract class GenericWebJob extends LongRunningJob {
    protected final TestLogger log = new TestLogger(getClass());

    WebDriver driver;
    testGUIController testGUIController;
    protected AbstractStressTest stressTest;

    GenericWebJob(AbstractStressTest stressTest,
            TestEnvironmentController testController, WebDriver driver, Long startUpTime, Long waitingInterval,
            Long maxTime, String name) {
        super(startUpTime, waitingInterval, maxTime, name);
        this.driver = driver;
        this.testGUIController = new testGUIController(testController);
        driver.manage().timeouts().implicitlyWait(1, TimeUnit.SECONDS);
        String baseUrl = "http://" + testController.ENV
                .getGuiHost() + ":" + testController.ENV.getGuiPort();
        PageHelper.initialize(driver, baseUrl);
        testGUIController.waitForGUIToStart(600);
        stressTest.addFixture("Opening front page " + baseUrl);
        this.stressTest = stressTest;
    }
}
