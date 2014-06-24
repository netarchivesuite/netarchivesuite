
package dk.netarkivet.archive.distribute;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

/**
 *  Unit-tester suite for the tests for package dk.netarkivet.archive.distribute.
 *
 */
public class ArchiveDistributeTesterSuite {
    /**
     * Create a test suite just for these tests.
     */
    public static Test suite() {
        TestSuite suite;
        suite = new TestSuite(ArchiveDistributeTesterSuite.class.getName());
        ArchiveDistributeTesterSuite.addToSuite(suite);
        return suite;
    }

    /**
     * Add the tests here.
     */
    public static void addToSuite(TestSuite suite) {
        suite.addTestSuite(ArchiveMessageHandlerTester.class);
        suite.addTestSuite(ReplicaClientFactoryTester.class);
    }

    public static void main(String args[]) {
        String args2[] = {"-noloading", ArchiveDistributeTesterSuite.class.getName()};
        TestRunner.main(args2);
    }
}
