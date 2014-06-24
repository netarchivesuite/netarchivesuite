
package dk.netarkivet.archive.tools;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

/**
 * Unit-tester suite for the tests for package dk.netarkivet.archive.tools.
 *
 */
public class ArchiveToolsTesterSuite {
    /**
     * Create a test suite just for these tests.
     */
    public static Test suite() {
        TestSuite suite;
        suite = new TestSuite(ArchiveToolsTesterSuite.class.getName());
        ArchiveToolsTesterSuite.addToSuite(suite);
        return suite;
    }

    /**
     * Add the tests here.
     */
    public static void addToSuite(TestSuite suite) {
        suite.addTestSuite(GetFileTester.class);
        suite.addTestSuite(GetRecordTester.class);
        suite.addTestSuite(ReestablishAdminDatabaseTester.class);
        suite.addTestSuite(RunBatchTester.class);
        suite.addTestSuite(UploadTester.class);
    }

    public static void main(String args[]) {
        String args2[] = {"-noloading", ArchiveToolsTesterSuite.class.getName()};
        TestRunner.main(args2);
    }
}
