package dk.netarkivet.wayback;

import dk.netarkivet.wayback.aggregator.AggregationWorkerTest;
import dk.netarkivet.wayback.aggregator.IndexAggregatorTest;
import dk.netarkivet.wayback.batch.DeduplicateToCDXAdapterTester;
import dk.netarkivet.wayback.batch.DeduplicationCDXExtractionBatchJobTester;
import dk.netarkivet.wayback.batch.WaybackCDXExtractionArcAndWarcBatchJobTester;
import dk.netarkivet.wayback.indexer.ArchiveFileDAOTester;
import dk.netarkivet.wayback.indexer.ArchiveFileTester;
import dk.netarkivet.wayback.indexer.FileNameHarvesterTester;
import dk.netarkivet.wayback.indexer.HibernateUtilTester;
import dk.netarkivet.wayback.indexer.IndexerQueueTester;
import dk.netarkivet.wayback.indexer.WaybackIndexerTester;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

/**
 * Created by IntelliJ IDEA. User: csr Date: Aug 26, 2009 Time: 10:32:46 AM To
 * change this template use File | Settings | File Templates.
 */
public class WaybackTesterSuite {
    /**
     * Create a test suite just for these tests.
     * @return this testsuite
     */
    public static Test suite() {
        TestSuite suite;
        suite = new TestSuite(WaybackTesterSuite.class.getName());
        WaybackTesterSuite.addToSuite(suite);
        return suite;
    }

    /**
     * Add the tests here.
     * @param suite The testsuite to be added
     */
    public static void addToSuite(TestSuite suite) {
        suite.addTestSuite(ArchiveFileTester.class);
        suite.addTestSuite(ArchiveFileDAOTester.class);
        suite.addTestSuite(WaybackCDXExtractionArcAndWarcBatchJobTester.class);
        suite.addTestSuite(FileNameHarvesterTester.class);
        suite.addTestSuite(IndexerQueueTester.class);
        suite.addTestSuite(NetarchiveResourceStoreTester.class);
        suite.addTestSuite(UrlCanonicalizerFactoryTester.class);
        suite.addTestSuite(DeduplicateToCDXAdapterTester.class);
        suite.addTestSuite(DeduplicationCDXExtractionBatchJobTester.class);
        suite.addTestSuite(DeduplicateToCDXApplicationTester.class);
        suite.addTestSuite(HibernateUtilTester.class);
        suite.addTestSuite(WaybackIndexerTester.class);
        suite.addTestSuite(IndexAggregatorTest.class);
        suite.addTestSuite(AggregationWorkerTest.class);
        suite.addTestSuite(LRUCacheTester.class);
    }

    public static void main(String args[]) {
        String args2[] = {"-noloading", WaybackTesterSuite.class.getName()};
        TestRunner.main(args2);
    }
}
