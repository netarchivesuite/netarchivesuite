package dk.netarkivet.archive.bitarchive;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.textui.TestRunner;


public class ArchiveBitarchiveTesterSuite {
    public static Test suite() {
        TestSuite suite;
        suite = new TestSuite(ArchiveBitarchiveTesterSuite.class.getName());

        addToSuite(suite);

        return suite;
    }

    public static void addToSuite(TestSuite suite) {
        suite.addTestSuite(BatchMessageTester.class);
        suite.addTestSuite(BitarchiveAdminTester.class);
        suite.addTestSuite(BitarchiveARCFileTester.class);
        suite.addTestSuite(BitarchiveMonitorTester.class);
        suite.addTestSuite(BitarchiveTesterAdmin.class);
        suite.addTestSuite(BitarchiveTesterBatch.class);
        suite.addTestSuite(BitarchiveTesterCTOR.class);
        suite.addTestSuite(BitarchiveTesterGet.class);
        suite.addTestSuite(BitarchiveTesterLog.class);
        suite.addTestSuite(BitarchiveTesterUpload.class);
    }

    public static void main(String args[]) {
        String args2[] = {"-noloading", ArchiveBitarchiveTesterSuite.class.getName()};

        TestRunner.main(args2);
        //junit.swingui.TestRunner.main(args2);
    }
}