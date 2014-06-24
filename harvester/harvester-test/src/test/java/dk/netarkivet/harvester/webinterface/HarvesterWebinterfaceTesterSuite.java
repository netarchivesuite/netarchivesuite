package dk.netarkivet.harvester.webinterface;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

/**
 * A suite of harvester webinterface tests.
 */
public class HarvesterWebinterfaceTesterSuite {
    public static Test suite() {
        TestSuite suite;
        suite = new TestSuite(HarvesterWebinterfaceTesterSuite.class.getName());
        addToSuite(suite);
        return suite;
    }

    public static void addToSuite(TestSuite suite) {
        suite.addTestSuite(DomainDefinitionTester.class);
        //suite.addTestSuite(EventHarvestTester.class); Fails in Hudson
        // Not quite working with JSP compilation yet
        //suite.addTestSuite(HarveststatusPerdomainTester.class);
        suite.addTestSuite(HistorySiteSectionTester.class);
        suite.addTestSuite(ScheduleDefinitionTester.class);
        suite.addTestSuite(SelectiveHarvestUtilTester.class);
        suite.addTestSuite(SnapshotHarvestDefinitionTester.class);
        suite.addTestSuite(HarvestStatusTester.class);
    }

    public static void main(String args[]) {
        String args2[] = {"-noloading", HarvesterWebinterfaceTesterSuite.class.getName()};

        TestRunner.main(args2);
    }
}
