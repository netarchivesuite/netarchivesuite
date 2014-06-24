
package dk.netarkivet.monitor.registry;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

/**
 * Unit tests for the package monitor.registry 
 * and monitor.registry.distribute
 *
 */
public class MonitorRegistryTesterSuite {
    /**
     * Create a test suite just for these tests.
     */
    public static Test suite() {
        TestSuite suite;
        suite = new TestSuite(MonitorRegistryTesterSuite.class.getSimpleName());
        addToSuite(suite);
        return suite;
    }

    /**
     * Add the tests here.
     */
    public static void addToSuite(TestSuite suite) {
        suite.addTestSuite(MonitorRegistryServerTester.class);
    }

    public static void main(String args[]) {
        String args2[] = {"-noloading", MonitorRegistryTesterSuite.class.getName()};
        TestRunner.main(args2);
    }
}
