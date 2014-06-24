
package dk.netarkivet.monitor.jmx;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

/**
 * Unittestersuite for the dk.netarkivet.monitor.jmx package.
 *
 */
public class MonitorJMXTesterSuite {
    /**
     * Create a test suite just for these tests.
     */
    public static Test suite() {
        TestSuite suite;
        suite = new TestSuite(MonitorJMXTesterSuite.class.getSimpleName());
        addToSuite(suite);
        return suite;
    }

    /**
     * Add the tests here.
     */
    public static void addToSuite(TestSuite suite) {
        suite.addTestSuite(CachingProxyConnectionFactoryTester.class);
        suite.addTestSuite(JMXUtilsTester.class);
    }

    public static void main(String args[]) {
        String args2[] = {"-noloading", MonitorJMXTesterSuite.class.getName()};
        TestRunner.main(args2);
    }
}
