
package dk.netarkivet.harvester.indexserver;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

/**
 * Sweet!  The suite for running indexservertests is auto-setup!
 *
 */

public class ArchiveIndexServerTesterSuite {
    /**
     * Create a test suite just for these tests.
     */
    public static Test suite() {
        TestSuite suite;
        suite = new TestSuite(ArchiveIndexServerTesterSuite.class.getSimpleName());
        addToSuite(suite);
        return suite;
    }

    /**
     * Add the tests here
     */
    public static void addToSuite(TestSuite suite) {
        suite.addTestSuite(CDXIndexCacheTester.class);
        suite.addTestSuite(CDXOriginCrawlLogIteratorTester.class);
        suite.addTestSuite(CombiningMultiFileBasedCacheTester.class);
        suite.addTestSuite(CrawlLogIndexCacheTester.class);
        suite.addTestSuite(DedupCrawlLogIndexCacheTester.class);
        suite.addTestSuite(GetMetadataArchiveBatchJobTester.class);
        suite.addTestSuite(MultiFileBasedCacheTester.class);
        suite.addTestSuite(RawMetadataCacheTester.class);
        suite.addTestSuite(IndexServerTester.class);
    }

    public static void main(String args[]) {
        String args2[] = {"-noloading", ArchiveIndexServerTesterSuite.class.getName()};
        TestRunner.main(args2);
    }
}
