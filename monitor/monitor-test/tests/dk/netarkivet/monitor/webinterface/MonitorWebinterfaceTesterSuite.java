
package dk.netarkivet.monitor.webinterface;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

/**
 * Unit tests for the package monitor.registry 
 * and monitor.registry.distribute
 *
 */
public class MonitorWebinterfaceTesterSuite {
    /**
     * Create a test suite just for these tests.
     */
    public static Test suite() {
        TestSuite suite;
        suite = new TestSuite(MonitorWebinterfaceTesterSuite.class.getSimpleName());
        addToSuite(suite);
        return suite;
    }

    /**
     * Add the tests here.
     */
    public static void addToSuite(TestSuite suite) {
        suite.addTestSuite(JMXStatusEntryTester.class);
        suite.addTestSuite(JMXSummaryUtilsTester.class);
    }

    public static void main(String args[]) {
        String args2[] = {"-noloading", MonitorWebinterfaceTesterSuite.class.getName()};
        TestRunner.main(args2);
    }
}
