package dk.netarkivet.systemtest.performance;

import java.util.concurrent.TimeUnit;

import org.openqa.selenium.WebDriver;

import dk.netarkivet.systemtest.TestLogger;
import dk.netarkivet.systemtest.environment.GUIApplicationManager;
import dk.netarkivet.systemtest.environment.TestController;
import dk.netarkivet.systemtest.page.PageHelper;

/**
* A specialisation of LongRunningJob for web jobs with a TestController
*/
abstract class GenericWebJob extends LongRunningJob {
    protected final TestLogger log = new TestLogger(getClass());

    WebDriver driver;
    GUIApplicationManager GUIApplicationManager;
    protected AbstractStressTest stressTest;

    GenericWebJob(AbstractStressTest stressTest,
            TestController testController, WebDriver driver, Long startUpTime, Long waitingInterval,
            Long maxTime, String name) {
        super(startUpTime, waitingInterval, maxTime, name);
        this.driver = driver;
        this.GUIApplicationManager = new GUIApplicationManager(testController);
        driver.manage().timeouts().implicitlyWait(1, TimeUnit.SECONDS);
        String baseUrl = testController
                .getGuiHost() + ":" + testController.getGuiPort();
        PageHelper.initialize(driver, baseUrl);
        GUIApplicationManager.waitForGUIToStart(60);
        stressTest.addFixture("Opening front page " + baseUrl);
        this.stressTest = stressTest;
    }
}
