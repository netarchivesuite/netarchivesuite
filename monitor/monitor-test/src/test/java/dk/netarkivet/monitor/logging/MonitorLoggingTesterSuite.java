
package dk.netarkivet.monitor.logging;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

/**
 * Testsuite for the package dk.netarkivet.common.logging.
 *
 */

public class MonitorLoggingTesterSuite {
    /**
     * Create a test suite just for these tests.
     * @return this testsuite
     */
    public static Test suite() {
        TestSuite suite;
        suite = new TestSuite(MonitorLoggingTesterSuite.class.getName());
        MonitorLoggingTesterSuite.addToSuite(suite);
        return suite;
    }

    /**
     * Add the tests here.
     * @param suite The testsuite to be added
     */
    public static void addToSuite(TestSuite suite) {
        suite.addTestSuite(CachingLogHandlerTester.class);
    }

    public static void main(String args[]) {
        String args2[] = {"-noloading", MonitorLoggingTesterSuite.class.getName()};
        TestRunner.main(args2);
    }
}
