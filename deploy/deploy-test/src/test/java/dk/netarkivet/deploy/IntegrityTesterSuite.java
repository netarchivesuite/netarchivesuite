
package dk.netarkivet.deploy;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

/**
 * This class runs all the integrity tests.
 *
 */
public class IntegrityTesterSuite {
    public static void addToSuite(TestSuite suite) {
        // FIXME:  deploy cannot see test code in other modules.
        suite.addTestSuite(dk.netarkivet.common.distribute.IntegrityTestSuite.class);
        suite.addTestSuite(dk.netarkivet.common.distribute.IntegrityTestsFTP.class);
        suite.addTestSuite(dk.netarkivet.common.distribute.IntegrityTestsFTPRemoteFile.class);
//        suite.addTestSuite(dk.netarkivet.harvester.harvesting.distribute.IntegrityTests.class);
//        suite.addTestSuite(dk.netarkivet.archive.bitarchive.distribute.IntegrityTests.class);
        suite.addTestSuite(dk.netarkivet.common.webinterface.IntegrityTests.class);
        suite.addTestSuite(dk.netarkivet.common.utils.IntegrityTester.class);
//        suite.addTestSuite(dk.netarkivet.harvester.harvesting.distribute.HangingListenerTest.class);
    }

    public static Test suite() {
        TestSuite suite;
        suite = new TestSuite("IntegrityTesterSuite");

        addToSuite(suite);

        return suite;
    }

    public static void main(String[] args) {
        String[] args2 = {"-noloading", "dk.netarkivet.IntegrityTesterSuite"};
        TestRunner.main(args2);
    }}
