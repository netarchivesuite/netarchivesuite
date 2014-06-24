package dk.netarkivet.archive.checksum;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

/**
 * Suite of unittests for the classes in the
 * dk.netarkivet.archive.checksum and dk.netarkivet.archive.checksum.distribute
 * package.
 */
public class ArchiveChecksumTesterSuite {
    public static Test suite() {
        TestSuite suite;
        suite = new TestSuite(ArchiveChecksumTesterSuite.class.getName());

        addToSuite(suite);

        return suite;
    }

    public static void addToSuite(TestSuite suite) {
        suite.addTestSuite(FileChecksumArchiveTester.class);
        suite.addTestSuite(DatabaseChecksumTester.class);
    }

    public static void main(String args[]) {
        String args2[] = { "-noloading",
                ArchiveChecksumTesterSuite.class.getName() };
        TestRunner.main(args2);
    }
}