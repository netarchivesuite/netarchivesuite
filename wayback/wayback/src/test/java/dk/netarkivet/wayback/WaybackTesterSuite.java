package dk.netarkivet.wayback;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

/**
 * Created by IntelliJ IDEA. User: csr Date: Aug 26, 2009 Time: 10:32:46 AM To
 * change this template use File | Settings | File Templates.
 */
public class WaybackTesterSuite {
            /**
     * Create a test suite just for these tests.
     * @return this testsuite
     */
    public static Test suite() {
        TestSuite suite;
        suite = new TestSuite(WaybackTesterSuite.class.getName());
        WaybackTesterSuite.addToSuite(suite);
        return suite;
    }

    /**
     * Add the tests here.
     * @param suite The testsuite to be added
     */
    public static void addToSuite(TestSuite suite) {
        suite.addTestSuite(ExtractWaybackCDXBatchJobTester.class);
        //suite.addTestSuite(NetarchiveResourceStoreTester.class);
        //suite.addTestSuite(UrlCanonicalizerFactoryTester.class);
        suite.addTestSuite(DeduplicateToCDXAdapterTester.class);
        suite.addTestSuite(ExtractDeduplicateCDXBatchJobTester.class);
        suite.addTestSuite(DeduplicateToCDXApplicationTester.class);
    }

    public static void main(String args[]) {
        String args2[] = {"-noloading", WaybackTesterSuite.class.getName()};
        TestRunner.main(args2);
    }
}
