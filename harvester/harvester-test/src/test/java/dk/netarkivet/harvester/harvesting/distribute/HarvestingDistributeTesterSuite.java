package dk.netarkivet.harvester.harvesting.distribute;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

/**
 * Unit testersuite for the dk.netarkivet.harvester.harvesting.distribute
 * package.
 */
public class HarvestingDistributeTesterSuite {
    
    public static Test suite() {
        TestSuite suite;
        suite = new TestSuite("HarvestingDistributeTesterSuite");
        addToSuite(suite);
        return suite;
    }

    public static void addToSuite(TestSuite suite) {
        suite.addTestSuite(CrawlProgressMessageTester.class);
        suite.addTestSuite(CrawlStatusMessageTester.class);
        suite.addTestSuite(DomainStatsTester.class);
        suite.addTestSuite(DoOneCrawlMessageTester.class);
        suite.addTestSuite(HarvestControllerServerTester.class);
        suite.addTestSuite(JobEndedMessageTester.class);
        suite.addTestSuite(MetadataEntryTester.class);
        suite.addTestSuite(PersistentJobDataTester.class);
    }

    public static void main(String[] args) {
        String[] args2 = {
                "-noloading", "dk.netarkivet.harvester.harvesting.distribute."
                + "HarvestingDistributeTesterSuite"};
        TestRunner.main(args2);
    }
}