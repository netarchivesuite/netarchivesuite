
package dk.netarkivet.viewerproxy;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.textui.TestRunner;


/**
 * Unit test-suite covering all the classes in the
 * dk.netarkivet.viewerproxy package. 
 *
 */
public class ViewerProxyTesterSuite {
    /**
     * Create a test suite just for these tests.
     */
    public static Test suite() {
        TestSuite suite;
        suite = new TestSuite(ViewerProxyTesterSuite.class.getName());
        addToSuite(suite);
        return suite;
    }

    /**
     * Add the tests here.
     */
    public static void addToSuite(TestSuite suite) {
        suite.addTestSuite(ARCArchiveAccessTester.class);
        suite.addTestSuite(CommandResolverTester.class);
        suite.addTestSuite(CrawlLogLinesMatchingRegexpTester.class);
        suite.addTestSuite(DelegatingControllerTester.class);
        suite.addTestSuite(GetDataResolverTester.class);
        suite.addTestSuite(MissingURIRecorderTester.class);
        suite.addTestSuite(NotifyingURIResolverTester.class);
        suite.addTestSuite(UnknownCommandResolverTester.class);
        suite.addTestSuite(URIObserverTester.class);
	suite.addTestSuite(ViewerProxyTester.class);
        suite.addTestSuite(WebProxyTester.class);
    }

    public static void main(String[] args) {
        String[] args2 = {"-noloading", ViewerProxyTesterSuite.class.getName()};
        TestRunner.main(args2);
    }
}
