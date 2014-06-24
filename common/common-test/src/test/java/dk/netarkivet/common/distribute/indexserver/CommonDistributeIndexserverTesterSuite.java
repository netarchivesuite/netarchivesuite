
package dk.netarkivet.common.distribute.indexserver;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

/**
 * Collection of unittests of classes in the
 *  dk.netarkivet.common.distribute.indexserver package.
 */
public class CommonDistributeIndexserverTesterSuite {
    /**
     * Create a test suite just for these tests.
     */
    public static Test suite() {
        TestSuite suite;
        suite = new TestSuite(
                CommonDistributeIndexserverTesterSuite.class.getName());
        addToSuite(suite);
        return suite;
    }

    /**
     * Add the tests here
     */
    public static void addToSuite(TestSuite suite) {
        suite.addTestSuite(TrivialJobIndexCacheTester.class);
    }

    public static void main(String args[]) {
        String args2[] = {"-noloading",
                CommonDistributeIndexserverTesterSuite.class.getName()};
        TestRunner.main(args2);
    }
}
