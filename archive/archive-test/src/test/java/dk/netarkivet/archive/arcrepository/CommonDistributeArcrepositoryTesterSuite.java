
package dk.netarkivet.archive.arcrepository;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

/**
 * Unit-tester suite for the classes inside
 * package dk.netarkivet.common.distribute.arcrepository.
 */
public class CommonDistributeArcrepositoryTesterSuite {
    
    /**
     * Create a test suite just for these tests.
     * @return the created test.
     */
    public static Test suite() {
        TestSuite suite;
        suite = new TestSuite(
                CommonDistributeArcrepositoryTesterSuite.class.getName());
        CommonDistributeArcrepositoryTesterSuite.addToSuite(suite);
        return suite;
    }

    /**
     * Add the tests here.
     */
    public static void addToSuite(TestSuite suite) {
        // Sorted in alphabetical order
        suite.addTestSuite(ARCLookupTester.class);
        suite.addTestSuite(BatchStatusTester.class);
        suite.addTestSuite(BitarchiveRecordTester.class);
        suite.addTestSuite(LocalArcRepositoryClientTester.class);
        suite.addTestSuite(ReplicaTester.class);
        suite.addTestSuite(ReplicaTypeTester.class);
    }

    public static void main(String[] args) {
        String[] args2 = {"-noloading", 
                CommonDistributeArcrepositoryTesterSuite.class.getName()};
        TestRunner.main(args2);
    }
}
