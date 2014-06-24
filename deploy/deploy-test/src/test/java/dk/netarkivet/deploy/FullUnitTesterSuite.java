
package dk.netarkivet.deploy;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

import dk.netarkivet.common.CleanupSuite;

/**
 * This class runs the deploy unit tests. Maven runs the unit tests for the
 * other modules.
 */
public class FullUnitTesterSuite {
    public static void addToSuite(TestSuite suite) {


//        dk.netarkivet.common.UnitTesterSuite.addToSuite(suite);
//        dk.netarkivet.harvester.UnitTesterSuite.addToSuite(suite);
//        dk.netarkivet.archive.UnitTesterSuite.addToSuite(suite);
//        dk.netarkivet.viewerproxy.UnitTesterSuite.addToSuite(suite);
//        dk.netarkivet.monitor.UnitTesterSuite.addToSuite(suite);
//        dk.netarkivet.wayback.UnitTesterSuite.addToSuite(suite);
        dk.netarkivet.deploy.UnitTesterSuite.addToSuite(suite);
        /*
         * Dummy testersuite to cleanup after the tests.
         */
        CleanupSuite.addToSuite(suite);
    }

    public static Test suite() {
        TestSuite suite;
        suite = new TestSuite(FullUnitTesterSuite.class.getName());

        addToSuite(suite);

        return suite;
    }

    public static void main(String[] args) {
        String[] args2 = {"-noloading", FullUnitTesterSuite.class.getName()};
        TestRunner.main(args2);
    }
}
