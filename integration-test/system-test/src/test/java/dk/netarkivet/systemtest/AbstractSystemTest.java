package dk.netarkivet.systemtest;

import dk.netarkivet.systemtest.environment.DefaultTestEnvironment;
import dk.netarkivet.systemtest.environment.TestEnvironmentController;
import dk.netarkivet.systemtest.environment.TestEnvironment;

/**
 * Abstract superclass for all the selenium-based system tests.
 */
public abstract class AbstractSystemTest extends SeleniumTest {

    static TestEnvironment testEnvironment = new DefaultTestEnvironment(
            getTestX(),
            null,
            null,
            8071,
            TestEnvironment.JOB_ADMIN_SERVER,
            null
    );
    static TestEnvironmentController testController = new TestEnvironmentController(testEnvironment);

    public AbstractSystemTest() {
        super(testController);
    }

    /**
     * Identifies the test on the test system. More concrete this value will be used for the test environment variable.
     */
    protected static String getTestX() {
        return System.getProperty("deployable.postfix", "SystemTest");
    }
}
