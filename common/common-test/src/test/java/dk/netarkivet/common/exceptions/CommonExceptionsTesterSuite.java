
package dk.netarkivet.common.exceptions;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

/**
 * Testsuite for the package dk.netarkivet.common.exceptions.
 *
 */

public class CommonExceptionsTesterSuite {
    /**
     * Create a test suite just for these tests.
     * @return this testsuite
     */
    public static Test suite() {
        TestSuite suite;
        suite = new TestSuite(CommonExceptionsTesterSuite.class.getName());
        addToSuite(suite);
        return suite;
    }

    /**
     * Add the tests here.
     * @param suite The testsuite to be added
     */
    public static void addToSuite(TestSuite suite) {
        suite.addTestSuite(ExceptionsTester.class);
    }

    public static void main(String args[]) {
        String args2[] = {"-noloading", CommonExceptionsTesterSuite.class.getName()};
        TestRunner.main(args2);
    }
}
