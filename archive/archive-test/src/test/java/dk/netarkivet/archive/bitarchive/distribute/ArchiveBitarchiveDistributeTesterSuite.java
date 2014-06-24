
package dk.netarkivet.archive.bitarchive.distribute;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

/**
 * 
 * Unit tests for the package dk.netarkivet.archive.bitarchive.distribute.
 */
public class ArchiveBitarchiveDistributeTesterSuite {
    /**
     * Create a test suite just for these tests.
     */
    public static Test suite() {
        TestSuite suite;
        suite = new TestSuite(
                ArchiveBitarchiveDistributeTesterSuite.class.getName());
        addToSuite(suite);
        return suite;
    }

    /**
     * Add the tests here
     */
    public static void addToSuite(TestSuite suite) {
        suite.addTestSuite(BatchEndedMessageTester.class);
        suite.addTestSuite(BitarchiveClientTester.class);
        suite.addTestSuite(BitarchiveMonitorServerTester.class);
        suite.addTestSuite(BitarchiveServerTester.class);
        suite.addTestSuite(HeartBeatMessageTester.class);
        suite.addTestSuite(UploadMessageTester.class);
        suite.addTestSuite(GetFileMessageTester.class);
    }

    public static void main(String args[]) {
        String args2[] = {"-noloading", 
                ArchiveBitarchiveDistributeTesterSuite.class.getName()};
        TestRunner.main(args2);
    }
}
