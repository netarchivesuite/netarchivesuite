package dk.netarkivet.archive.checksum.distribute;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

public class ArchiveChecksumDistributeTesterSuite {
    public static Test suite() {
        TestSuite suite;
        suite = new TestSuite(ArchiveChecksumDistributeTesterSuite.class.getName());

        addToSuite(suite);

        return suite;
    }

    public static void addToSuite(TestSuite suite) {
        suite.addTestSuite(FileChecksumServerTester.class);
        suite.addTestSuite(ChecksumClientTester.class);
    }

    public static void main(String args[]) {
        String args2[] = { "-noloading",
                ArchiveChecksumDistributeTesterSuite.class.getName() };
        TestRunner.main(args2);
    }

}
