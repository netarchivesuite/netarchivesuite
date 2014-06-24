package dk.netarkivet.harvester.harvesting.frontier;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

/**
 * Test suite for the classes
 * in package dk.netarkivet.harvester.harvesting.
 */
public class HarvesterHarvestingFrontierTesterSuite {
    public static Test suite() {
        TestSuite suite;
        suite = new TestSuite(HarvesterHarvestingFrontierTesterSuite.class.getName());

        addToSuite(suite);

        return suite;
    }
    
    /**
     * Add tests to suite.
     * One line for each unit-test class in this testsuite.
     * @param suite the suite.
     */
    public static void addToSuite(TestSuite suite) {
        suite.addTestSuite(FullFrontierReportTest.class);
        suite.addTestSuite(FrontierReportFilterTest.class);
        suite.addTestSuite(FrontierReportLineTest.class);
        suite.addTestSuite(InMemoryFrontierReportTest.class);
    }

    public static void main(String[] args) {
        String[] args2 = {"-noloading", HarvesterHarvestingFrontierTesterSuite.class.getName()};

        TestRunner.main(args2);
        //junit.swingui.TestRunner.main(args2);
        try {
            Thread.sleep(100000000);
        } catch (InterruptedException e) {
            //just testing, remove block
        }
    }
}
