
package dk.netarkivet.common.utils;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

/**
 * Sweet suite of utility tests.
 *
 */
public class MonitorCommonUtilsTesterSuite {
    public static Test suite() {
        TestSuite suite;
        suite = new TestSuite(MonitorCommonUtilsTesterSuite.class.getName());
        addToSuite(suite);
        return suite;
    }

    public static void addToSuite(TestSuite suite) {
        suite.addTestSuite(ApplicationUtilsTester.class);
    }

    public static void main(String[] args) {
        String[] args2 = {"-noloading", MonitorCommonUtilsTesterSuite.class.getName()};

        TestRunner.main(args2);
    }
}
