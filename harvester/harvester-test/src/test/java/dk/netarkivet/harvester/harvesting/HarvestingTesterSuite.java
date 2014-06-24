package dk.netarkivet.harvester.harvesting;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.textui.TestRunner;
import dk.netarkivet.harvester.harvesting.extractor.ExtractorOAITest;

/**
 * Test suite for the classes
 * in package dk.netarkivet.harvester.harvesting.
 */
public class HarvestingTesterSuite {
    public static Test suite() {
        TestSuite suite;
        suite = new TestSuite(HarvestingTesterSuite.class.getName());

        addToSuite(suite);

        return suite;
    }
    
    /**
     * Add tests to suite.
     * One line for each unit-test class in this testsuite.
     * @param suite the suite.
     */
    public static void addToSuite(TestSuite suite) {
        suite.addTestSuite(DomainnameQueueAssignmentPolicyTester.class);
        // FIXME: Heritrix launcher not yet ported to new layout.
        // suite.addTestSuite(HeritrixControllerFactoryTester.class);
        suite.addTestSuite(HarvestControllerTester.class);
        suite.addTestSuite(HarvestDocumentationTester.class);
        suite.addTestSuite(LegacyHarvestReportTester.class);
        suite.addTestSuite(HeritrixFilesTester.class);
        suite.addTestSuite(HeritrixLauncherTester.class);
        suite.addTestSuite(IngestableFilesTester.class);
        suite.addTestSuite(OnNSDomainsDecideRuleTester.class);
        suite.addTestSuite(ExtractorOAITest.class);
        suite.addTestSuite(MetadataFileWriterTester.class);
        suite.addTestSuite(WARCWriterProcessorTester.class);
    }

    public static void main(String[] args) {
        String[] args2 = {"-noloading", HarvestingTesterSuite.class.getName()};

        TestRunner.main(args2);
        //junit.swingui.TestRunner.main(args2);
        try {
            Thread.sleep(100000000);
        } catch (InterruptedException e) {
            //just testing, remove block
        }
    }
}
