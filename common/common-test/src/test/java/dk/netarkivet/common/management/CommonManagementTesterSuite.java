
package dk.netarkivet.common.management;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

/**
 * Testsuite for the package dk.netarkivet.common.management.
 *
 */

public class CommonManagementTesterSuite {
    /**
     * Create a test suite just for these tests.
     * @return this testsuite
     */
    public static Test suite() {
        TestSuite suite;
        suite = new TestSuite(CommonManagementTesterSuite.class.getName());
        CommonManagementTesterSuite.addToSuite(suite);
        return suite;
    }

    /**
     * Add the tests here.
     * @param suite The testsuite to be added
     */
    public static void addToSuite(TestSuite suite) {
        suite.addTestSuite(SingleMBeanObjectTester.class);
    }

    public static void main(String args[]) {
        String args2[] = {"-noloading", CommonManagementTesterSuite.class.getName()};
        TestRunner.main(args2);
    }
}
