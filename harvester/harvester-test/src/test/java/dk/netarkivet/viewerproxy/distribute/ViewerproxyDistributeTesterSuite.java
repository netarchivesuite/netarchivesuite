
package dk.netarkivet.viewerproxy.distribute;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

/**
 * Unit-test suite for the classes in package
 * dk.netarkivet.viewerproxy.distribute.
 * 
 */
public class ViewerproxyDistributeTesterSuite {
    /**
     * Create a test suite just for these tests.
     */
    public static Test suite() {
        TestSuite suite;
        suite = new TestSuite(ViewerproxyDistributeTesterSuite.class.getName());
        ViewerproxyDistributeTesterSuite.addToSuite(suite);
        return suite;
    }

    /**
     * Add the tests here.
     */
    public static void addToSuite(TestSuite suite) {
        suite.addTestSuite(HTTPControllerServerTester.class);
        suite.addTestSuite(HTTPControllerClientTester.class);
    }

    public static void main(String args[]) {
        String args2[] = {"-noloading", ViewerproxyDistributeTesterSuite.class.getName()};
        TestRunner.main(args2);
    }
}
