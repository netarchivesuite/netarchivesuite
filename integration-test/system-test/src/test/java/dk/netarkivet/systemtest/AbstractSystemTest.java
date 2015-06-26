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
            getPort(),
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
        String systemEnvTestX = System.getenv("TESTX");
        return systemEnvTestX != null ? systemEnvTestX : "SystemTest";
    }

    protected static int getPort() {
        String systemEnvPort = System.getenv("PORT");
        return Integer.parseInt(systemEnvPort != null ? systemEnvPort : "8079");
    }
}
