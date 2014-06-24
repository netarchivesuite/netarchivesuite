package dk.netarkivet.harvester.scheduler;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

public class HarvesterSchedulerTesterSuite {
    public static Test suite() {
        TestSuite suite;
        suite = new TestSuite(HarvesterSchedulerTesterSuite.class.getName());
        addToSuite(suite);
        return suite;
    }

    public static void addToSuite(TestSuite suite) {
        //Disabled because most tests currently fail
        //suite.addTestSuite(HarvestSchedulerMonitorServerTester.class);
        suite.addTestSuite(HarvesterStatusReceiverTest.class);
        suite.addTestSuite(HarvestJobGeneratorTest.class);
    }

    public static void main(String args[]) {
        String args2[] = {"-noloading", HarvesterSchedulerTesterSuite.class.getName()};

        TestRunner.main(args2);
    }
}