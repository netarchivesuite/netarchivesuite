
package dk.netarkivet.viewerproxy.webinterface;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.textui.TestRunner;


/**
 * Unit test-suite covering all the classes in the
 * dk.netarkivet.viewerproxy.webinterface package.
 *
 */
public class ViewerproxyWebinterfaceTesterSuite {
    /**
     * Create a test suite just for these tests.
     */
    public static Test suite() {
        TestSuite suite;
        suite = new TestSuite(ViewerproxyWebinterfaceTesterSuite.class.getName());
        addToSuite(suite);
        return suite;
    }

    /**
     * Add the tests here.
     */
    public static void addToSuite(TestSuite suite) {
        suite.addTestSuite(ReportingTester.class);
        suite.addTestSuite(ReportingWarcTester.class);
    }

    public static void main(String[] args) {
        String[] args2 = {"-noloading", ViewerproxyWebinterfaceTesterSuite.class.getName()};
        TestRunner.main(args2);
    }
}
